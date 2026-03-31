#!/bin/sh
set -eu

SCRIPT_DIR="$(CDPATH= cd -- "$(dirname "$0")" && pwd)"
PROJECT_ROOT="$(CDPATH= cd -- "$SCRIPT_DIR/.." && pwd)"

require_file() {
  if [ ! -f "$1" ]; then
    echo "Missing required file: $1" >&2
    exit 1
  fi
}

echo "Checking Kubernetes file layout..."

require_file "$PROJECT_ROOT/k8s/base/kustomization.yaml"
require_file "$PROJECT_ROOT/k8s/base/backend-deployment.yaml"
require_file "$PROJECT_ROOT/k8s/base/frontend-deployment.yaml"
require_file "$PROJECT_ROOT/k8s/base/ingress.yaml"

for overlay in dev staging production; do
  require_file "$PROJECT_ROOT/k8s/overlays/$overlay/kustomization.yaml"
  require_file "$PROJECT_ROOT/k8s/overlays/$overlay/backend-config.yaml"
  require_file "$PROJECT_ROOT/k8s/overlays/$overlay/frontend-config.yaml"
  require_file "$PROJECT_ROOT/k8s/overlays/$overlay/ingress-patch.yaml"
done

require_file "$PROJECT_ROOT/k8s/overlays/dev/mysql-deployment.yaml"
require_file "$PROJECT_ROOT/k8s/overlays/dev/mysql-service.yaml"
require_file "$PROJECT_ROOT/k8s/overlays/dev/mysql-pvc.yaml"

echo "Checking Secret template files..."
require_file "$PROJECT_ROOT/k8s/overlays/dev/backend-secrets.example.env"
require_file "$PROJECT_ROOT/k8s/overlays/dev/mysql-secrets.example.env"
require_file "$PROJECT_ROOT/k8s/overlays/staging/backend-secrets.example.env"
require_file "$PROJECT_ROOT/k8s/overlays/production/backend-secrets.example.env"

if command -v kubectl >/dev/null 2>&1; then
  echo "kubectl detected, building overlays with kubectl kustomize..."
  kubectl kustomize "$PROJECT_ROOT/k8s/overlays/dev" >/dev/null
  kubectl kustomize "$PROJECT_ROOT/k8s/overlays/staging" >/dev/null
  kubectl kustomize "$PROJECT_ROOT/k8s/overlays/production" >/dev/null
elif command -v kustomize >/dev/null 2>&1; then
  echo "kustomize detected, building overlays..."
  kustomize build "$PROJECT_ROOT/k8s/overlays/dev" >/dev/null
  kustomize build "$PROJECT_ROOT/k8s/overlays/staging" >/dev/null
  kustomize build "$PROJECT_ROOT/k8s/overlays/production" >/dev/null
else
  echo "kubectl/kustomize not installed; skipped overlay build validation."
fi

echo "Kubernetes structure validation passed."
