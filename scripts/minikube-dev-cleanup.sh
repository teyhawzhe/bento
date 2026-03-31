#!/bin/sh
set -eu

MINIKUBE_PROFILE="${MINIKUBE_PROFILE:-minikube}"
NAMESPACE="${K8S_NAMESPACE:-bento-dev}"
STOP_MINIKUBE="${STOP_MINIKUBE:-0}"

require_command() {
  if ! command -v "$1" >/dev/null 2>&1; then
    echo "Missing required command: $1" >&2
    exit 1
  fi
}

require_command minikube
require_command kubectl

echo "Deleting namespace: $NAMESPACE"
kubectl delete namespace "$NAMESPACE" --ignore-not-found=true

if [ "$STOP_MINIKUBE" = "1" ]; then
  echo "Stopping minikube profile: $MINIKUBE_PROFILE"
  minikube stop -p "$MINIKUBE_PROFILE"
else
  echo "Minikube profile left running: $MINIKUBE_PROFILE"
  echo "Set STOP_MINIKUBE=1 to stop it as part of cleanup."
fi
