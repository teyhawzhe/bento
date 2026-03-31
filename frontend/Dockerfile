FROM node:24-alpine AS builder
WORKDIR /app

ARG VITE_API_BASE_URL

COPY package.json package-lock.json* ./
RUN if [ -f package-lock.json ]; then npm ci; else npm install; fi

COPY . .
RUN VITE_API_BASE_URL="${VITE_API_BASE_URL:-/api}" npm run build

FROM nginx:1.29-alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=builder /app/dist /usr/share/nginx/html

EXPOSE 80
