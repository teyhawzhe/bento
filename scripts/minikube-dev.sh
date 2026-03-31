#!/bin/sh
set -eu

SCRIPT_DIR="$(CDPATH= cd -- "$(dirname "$0")" && pwd)"
PROJECT_ROOT="$(CDPATH= cd -- "$SCRIPT_DIR/.." && pwd)"

MINIKUBE_PROFILE="${MINIKUBE_PROFILE:-minikube}"
NAMESPACE="${K8S_NAMESPACE:-bento-dev}"
BACKEND_IMAGE="${BACKEND_IMAGE:-lovius/bento-backend:dev}"
FRONTEND_IMAGE="${FRONTEND_IMAGE:-lovius/bento-frontend:dev}"
VITE_API_BASE_URL="${VITE_API_BASE_URL:-/api}"
ENABLE_INGRESS="${ENABLE_INGRESS:-1}"
BACKEND_SECRET_NAME="${BACKEND_SECRET_NAME:-backend-secrets}"
MYSQL_SECRET_NAME="${MYSQL_SECRET_NAME:-mysql-secrets}"

BACKEND_SECRETS_FILE="${BACKEND_SECRETS_FILE:-$PROJECT_ROOT/k8s/overlays/dev/backend-secrets.env}"
MYSQL_SECRETS_FILE="${MYSQL_SECRETS_FILE:-$PROJECT_ROOT/k8s/overlays/dev/mysql-secrets.env}"

if [ ! -f "$BACKEND_SECRETS_FILE" ]; then
  BACKEND_SECRETS_FILE="$PROJECT_ROOT/k8s/overlays/dev/backend-secrets.example.env"
fi

if [ ! -f "$MYSQL_SECRETS_FILE" ]; then
  MYSQL_SECRETS_FILE="$PROJECT_ROOT/k8s/overlays/dev/mysql-secrets.example.env"
fi

require_command() {
  if ! command -v "$1" >/dev/null 2>&1; then
    echo "Missing required command: $1" >&2
    exit 1
  fi
}

require_file() {
  if [ ! -f "$1" ]; then
    echo "Missing required file: $1" >&2
    exit 1
  fi
}

extract_env_value() {
  key="$1"
  file="$2"
  awk -F= -v key="$key" '
    $0 ~ "^[[:space:]]*" key "=" {
      sub(/^[^=]*=/, "", $0)
      print $0
      exit
    }
  ' "$file"
}

require_command minikube
require_command kubectl

require_file "$PROJECT_ROOT/k8s/overlays/dev/kustomization.yaml"
require_file "$BACKEND_SECRETS_FILE"
require_file "$MYSQL_SECRETS_FILE"

JWT_SECRET="$(extract_env_value APP_JWT_SECRET "$BACKEND_SECRETS_FILE" || true)"
if [ -z "$JWT_SECRET" ]; then
  echo "Missing APP_JWT_SECRET in $BACKEND_SECRETS_FILE" >&2
  exit 1
fi

JWT_SECRET_LENGTH="$(printf '%s' "$JWT_SECRET" | wc -c | tr -d ' ')"
if [ "$JWT_SECRET_LENGTH" -lt 32 ]; then
  echo "APP_JWT_SECRET in $BACKEND_SECRETS_FILE must be at least 32 characters." >&2
  exit 1
fi

if [ "$BACKEND_SECRETS_FILE" = "$PROJECT_ROOT/k8s/overlays/dev/backend-secrets.example.env" ]; then
  echo "Warning: using example backend secrets file. Create k8s/overlays/dev/backend-secrets.env for real values." >&2
fi

if [ "$MYSQL_SECRETS_FILE" = "$PROJECT_ROOT/k8s/overlays/dev/mysql-secrets.example.env" ]; then
  echo "Warning: using example mysql secrets file. Create k8s/overlays/dev/mysql-secrets.env for real values." >&2
fi

cd "$PROJECT_ROOT"

echo "Starting minikube profile: $MINIKUBE_PROFILE"
minikube start -p "$MINIKUBE_PROFILE"
minikube update-context -p "$MINIKUBE_PROFILE"

if [ "$ENABLE_INGRESS" = "1" ]; then
  echo "Ensuring ingress addon is enabled"
  minikube addons enable ingress -p "$MINIKUBE_PROFILE"
fi

echo "Building backend image: $BACKEND_IMAGE"
minikube image build -p "$MINIKUBE_PROFILE" -t "$BACKEND_IMAGE" "$PROJECT_ROOT/backend"

echo "Building frontend image: $FRONTEND_IMAGE"
minikube image build -p "$MINIKUBE_PROFILE" \
  --build-opt "build-arg=VITE_API_BASE_URL=$VITE_API_BASE_URL" \
  -t "$FRONTEND_IMAGE" \
  "$PROJECT_ROOT/frontend"

echo "Creating namespace: $NAMESPACE"
kubectl create namespace "$NAMESPACE" --dry-run=client -o yaml | kubectl apply -f -

echo "Applying backend secret from $BACKEND_SECRETS_FILE"
kubectl create secret generic "$BACKEND_SECRET_NAME" \
  --namespace "$NAMESPACE" \
  --from-env-file="$BACKEND_SECRETS_FILE" \
  --dry-run=client -o yaml | kubectl apply -f -

echo "Applying mysql secret from $MYSQL_SECRETS_FILE"
kubectl create secret generic "$MYSQL_SECRET_NAME" \
  --namespace "$NAMESPACE" \
  --from-env-file="$MYSQL_SECRETS_FILE" \
  --dry-run=client -o yaml | kubectl apply -f -

echo "Applying dev overlay"
kubectl apply -k "$PROJECT_ROOT/k8s/overlays/dev"

echo "Waiting for deployments to become ready"
kubectl rollout status deployment/mysql -n "$NAMESPACE" --timeout=180s
kubectl rollout status deployment/backend -n "$NAMESPACE" --timeout=180s
kubectl rollout status deployment/frontend -n "$NAMESPACE" --timeout=180s

echo
echo "Deployment complete."
echo "Namespace: $NAMESPACE"
echo "Ingress host: bento-dev.local"
echo
echo "Current resources:"
kubectl get pods,svc,ingress -n "$NAMESPACE"
echo
echo "Frontend tunnel URL:"
minikube service frontend -n "$NAMESPACE" --url -p "$MINIKUBE_PROFILE"
echo
echo "Backend tunnel URL:"
minikube service backend -n "$NAMESPACE" --url -p "$MINIKUBE_PROFILE"
