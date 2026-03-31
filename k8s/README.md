# Kubernetes Deployment Structure

This project uses a `base + overlays` layout so `dev`, `staging`, and `production`
share the same deployment model while only overriding environment-specific values.

## Layout

- `k8s/base`: shared Deployments, Services, and Ingress
- `k8s/overlays/dev`: local Kubernetes target for minikube
- `k8s/overlays/staging`: staging deployment settings
- `k8s/overlays/production`: production deployment settings

## Environment Model

- `dev`: targets minikube and includes an in-cluster MySQL dependency
- `staging`: assumes an external database and externally managed secrets
- `production`: assumes an external database and externally managed secrets

Backend keeps using Spring profiles:

- `dev`
- `staging`
- `production`

Frontend keeps using `VITE_API_BASE_URL`.

For Kubernetes, the recommended approach is:

- build the frontend image with `VITE_API_BASE_URL=/api`
- expose frontend and backend behind one Ingress host
- route `/api` to the backend service and `/` to the frontend service

That allows a single routing shape across all Kubernetes environments even if image
tags differ per environment.

## Secrets

Each overlay references these Secret names:

- `backend-secrets`
- `mysql-secrets` for `dev` only

The repository intentionally does not store real secret values. Use the checked-in
`*.example.env` files as templates and create real Secrets in your target cluster.

For backend secrets, `APP_JWT_SECRET` must be long enough for the JWT HMAC key.
Use at least 32 bytes for HS256-compatible deployments.

## Validation

Run:

```bash
./scripts/validate-k8s.sh
```

If `kubectl` or `kustomize` is available, the script will also build each overlay.
If not, it still performs structural checks so this change remains usable on machines
without a local Kubernetes toolchain.

## Minikube Notes

`dev` is designed for minikube, but this repo does not require every developer to
install or run it locally.

The fastest way to deploy `dev` to minikube is:

```bash
cp k8s/overlays/dev/backend-secrets.example.env k8s/overlays/dev/backend-secrets.env
cp k8s/overlays/dev/mysql-secrets.example.env k8s/overlays/dev/mysql-secrets.env
./scripts/minikube-dev.sh
```

The script will:

- start minikube
- enable ingress
- build backend and frontend images inside minikube
- create or update the `bento-dev` namespace and Secrets
- apply `k8s/overlays/dev`
- wait for `mysql`, `backend`, and `frontend` rollouts

To clean up the `dev` deployment later:

```bash
./scripts/minikube-dev-cleanup.sh
```

If you also want to stop minikube:

```bash
STOP_MINIKUBE=1 ./scripts/minikube-dev-cleanup.sh
```

When you do have minikube available, a typical flow is:

```bash
minikube start
eval "$(minikube docker-env)"
docker build -t lovius/bento-backend:dev ./backend
docker build --build-arg VITE_API_BASE_URL=/api -t lovius/bento-frontend:dev ./frontend
kubectl create namespace bento-dev
kubectl create secret generic backend-secrets \
  --namespace bento-dev \
  --from-literal=SPRING_DATASOURCE_PASSWORD=bento \
  --from-literal=APP_MAIL_SMTP_USERNAME='' \
  --from-literal=APP_MAIL_SMTP_PASSWORD='' \
  --from-literal=APP_JWT_SECRET='change-this-before-sharing'
kubectl create secret generic mysql-secrets \
  --namespace bento-dev \
  --from-literal=MYSQL_ROOT_PASSWORD=root \
  --from-literal=MYSQL_PASSWORD=bento
kubectl apply -k k8s/overlays/dev
```

For minikube ingress access, you may also need:

- `minikube addons enable ingress`
- a local hosts entry for `bento-dev.local`

## Verification Record

### 2026-03-31 Minikube Validation

- `minikube start` completed successfully and `kubectl` context was refreshed back to `minikube`
- `kubectl apply -k k8s/overlays/dev` applied successfully in namespace `bento-dev`
- `backend`, `frontend`, and `mysql` pods all reached `Running`
- `frontend` responded with `200` through the local minikube service tunnel
- `backend` received requests through the local minikube service tunnel after replacing the dev `APP_JWT_SECRET` with a 32+ byte value
- a direct GET to `/api/admin/employees` without `Authorization` reached the backend but returned an application error response, so this validation confirms deployment reachability rather than authenticated business-flow correctness
