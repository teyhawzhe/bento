# 公司員工訂便當系統

目前已完成 A001「內部員工登入驗證」、A002「員工訂便當」、A003「每日供應商訂單通知」、A004「錯誤通知信箱設定」、A005「月結帳單報表與發信」、A006「管理員代替員工新增訂餐與查詢訂單」、A007「管理供應商」、A008「月結報表收件信箱設定」與 A010「部門管理」對齊版，包含前後端 `status/data` API 契約、refresh token 認證模型、員工與管理員主頁導向、供應商通知排程、月結報表與發送記錄查詢。

## 專案結構

- `frontend`: React + TypeScript + Vite + Tailwind 的登入、訂餐與管理介面
- `backend`: Java 21 + Spring Boot 的 A001/A002 API
- `docker-compose.yml`: 前端、後端、MySQL 的容器編排檔
- `openspec`: OpenSpec 設定

## 已實作功能

- 員工登入 `POST /api/auth/login`
- 員工 refresh token 換發 `POST /api/auth/refresh`
- 員工登出 `POST /api/auth/logout`
- 管理員登入 `POST /api/admin/auth/login`
- 管理員 refresh token 換發 `POST /api/admin/auth/refresh`
- 忘記密碼 `POST /api/auth/forgot-password`
- 修改密碼 `PATCH /api/auth/change-password`
- 查詢員工 `GET /api/admin/employees`
- 新增員工 `POST /api/admin/employees`
- CSV 匯入員工 `POST /api/admin/employees/import`
- 啟用或停用員工 `PATCH /api/admin/employees/{id}/status`
- 管理員重設員工密碼 `PATCH /api/admin/employees/{id}/reset-password`
- 員工查詢下週便當 `GET /api/orders/menu`
- 員工新增訂餐 `POST /api/orders`
- 員工更新訂餐 `PATCH /api/orders/{id}`
- 員工取消訂餐 `DELETE /api/orders/{id}`
- 員工查詢個人訂餐記錄 `GET /api/orders/me`
- 管理員查詢菜單清單 `GET /api/menus`
- 管理員建立菜單 `POST /api/menus`
- 管理員編輯菜單 `PATCH /api/menus/{id}`
- 管理員新增供應商 `POST /api/suppliers`
- 管理員查詢供應商 `GET /api/suppliers`
- 管理員查詢單一供應商 `GET /api/suppliers/{id}`
- 管理員更新供應商 `PATCH /api/suppliers/{id}`
- 管理員查詢部門 `GET /api/admin/departments`
- 管理員建立部門 `POST /api/admin/departments`
- 管理員更新部門 `PATCH /api/admin/departments/{id}`
- 管理員查詢員工訂餐 `GET /api/admin/orders`
- 管理員代替員工新增訂餐 `POST /api/admin/orders`
- 管理員取消指定員工訂餐 `DELETE /api/admin/orders/{id}`
- 管理員查詢供應商與便當選項 `GET /api/admin/suppliers`
- 管理員查詢錯誤通知信箱 `GET /api/settings/error-emails`
- 管理員新增錯誤通知信箱 `POST /api/settings/error-emails`
- 管理員刪除錯誤通知信箱 `DELETE /api/settings/error-emails/{id}`
- 管理員查詢報表收件信箱 `GET /api/settings/report-emails`
- 管理員新增報表收件信箱 `POST /api/settings/report-emails`
- 管理員刪除報表收件信箱 `DELETE /api/settings/report-emails/{id}`
- 每日供應商通知排程 `A003 / 每日 17:00`
- 管理員手動觸發月結報表 `POST /api/admin/reports/monthly`
- 管理員查詢月結發送記錄 `GET /api/admin/reports/monthly`

## Demo 帳號

- 員工: `alice / WelcomeA1`
- 管理員: `admin / AdminPassA1`
- 停用帳號: `disabled.user / DisabledA1`

## 目前假設

- 後端目前採 JDBC + `schema.sql` 管理 `departments`、`employees`、`refresh_tokens`、`suppliers`、`menus`、`orders`、`error_notification_emails`、`report_recipient_emails`、`notification_logs`、`monthly_billing_logs`
- Email 發送已支援 `config-based` 的 `mock` / `smtp` 兩種模式，`dev` 預設使用 `mock`，`staging` / `production` 預設使用 `smtp`
- 管理員代訂與取消截止時間為訂餐日前一日 16:30；A003 供應商通知排程為每日 17:00
- API JSON 欄位已統一對齊 `snake_case`，前端內部則維持 `camelCase` 使用
- A004 已提供 `ErrorNotificationRecipientProvider`，A003 排程會直接讀取錯誤通知收件清單
- A005 已提供 `BillingReportRecipientProvider`，A008 報表收件清單已接入月結發信流程

## 啟動方式

目前專案已包含 `backend/build.gradle` 與 Gradle Wrapper，可直接在 `backend` 透過 `./gradlew` 執行建置；前端可在 `frontend` 以 `npm` 啟動或打包。

### 環境設定

backend 目前支援 3 組環境 profile：

- `dev`
- `staging`
- `production`

預設 profile：

- 本機直接啟動 backend：`dev`
- Docker image 預設：`production`
- `docker compose` 預設：`dev`

可透過 `SPRING_PROFILES_ACTIVE` 指定目標環境，例如：

```bash
SPRING_PROFILES_ACTIVE=dev ./scripts/backend-dev.sh
cd backend && SPRING_PROFILES_ACTIVE=staging ./gradlew bootRun
cd backend && SPRING_PROFILES_ACTIVE=production ./gradlew bootRun
```

也可以直接使用環境腳本：

```bash
./scripts/backend-dev.sh
./scripts/backend-staging.sh
./scripts/backend-production.sh
```

Windows 可使用：

```bat
scripts\backend-dev.bat
scripts\backend-staging.bat
scripts\backend-production.bat
```

### Mail 設定

mail delivery 採 `config-based` 切換：

- `APP_MAIL_MODE=mock`
- `APP_MAIL_MODE=smtp`

共用 SMTP 設定欄位：

- `APP_MAIL_FROM`
- `APP_MAIL_SMTP_HOST`
- `APP_MAIL_SMTP_PORT`
- `APP_MAIL_SMTP_USERNAME`
- `APP_MAIL_SMTP_PASSWORD`
- `APP_MAIL_SMTP_AUTH`
- `APP_MAIL_SMTP_STARTTLS`
- `APP_PDF_FONT_PATH`

Gmail SMTP 測試範例：

```bash
cd backend
SPRING_PROFILES_ACTIVE=staging \
APP_MAIL_MODE=smtp \
APP_MAIL_FROM=your-account@gmail.com \
APP_MAIL_SMTP_HOST=smtp.gmail.com \
APP_MAIL_SMTP_PORT=587 \
APP_MAIL_SMTP_USERNAME=your-account@gmail.com \
APP_MAIL_SMTP_PASSWORD=your-app-password \
./gradlew bootRun
```

PDF 中文字型：

- A013 員工訂餐報表 PDF 需要可顯示中文的字型檔
- 本機 macOS 可設為：`APP_PDF_FONT_PATH=/System/Library/Fonts/Supplemental/Arial Unicode.ttf`
- Docker Compose 預設會帶入：`/usr/share/fonts/opentype/noto/NotoSansCJK-Regular.ttc`

建議啟動方式：

1. 在 `frontend` 安裝套件後執行 `npm run dev`
2. 在 `backend` 以 Gradle 啟動 Spring Boot
3. 或使用 `docker compose up --build` 啟動前端、後端與 MySQL

### 快速指令

在專案根目錄可直接執行：

```bash
./scripts/dev-frontend.sh
./scripts/backend-dev.sh
./scripts/dev-compose.sh
```

- `dev-frontend.sh`: 自動安裝前端依賴後啟動 Vite
- `backend-dev.sh` / `backend-staging.sh` / `backend-production.sh`: 直接以指定 profile 啟動 backend
- `backend-dev.bat` / `backend-staging.bat` / `backend-production.bat`: Windows 版本的 backend 啟動腳本
- `dev-compose.sh`: 直接執行 `docker compose up --build`

前端 API 連線策略：

- `npm run dev` / `./scripts/dev-frontend.sh` 會透過 Vite proxy 將 `/api` 轉送到 `http://localhost:8080`
- Docker / Kubernetes 的 frontend Nginx 會將 `/api` 反向代理到 `backend:8080`
- 若需要覆寫，也可在 build 時提供 `VITE_API_BASE_URL`

## Docker Compose

- `frontend`: 使用 Nginx 提供打包後的 React 靜態頁面，對外開放 `5173`
- `backend`: 以多階段 Dockerfile 建立 Spring Boot 映像，對外開放 `8080`
- `mysql`: 預留 MySQL 8.4 服務，對外開放 `3306`

目前後端已改為 JDBC + MySQL schema 邊界，`mysql` 服務可直接承接 `employees`、`suppliers`、`menus`、`orders` 等資料表。
`docker compose` 會預設注入 `SPRING_PROFILES_ACTIVE=dev` 與 `APP_MAIL_MODE=mock`，也可在執行前自行覆寫。

## Kubernetes

專案現在也提供 Kubernetes 的 `base + overlays` 結構，位於 [`k8s/README.md`](./k8s/README.md)。

- `k8s/base`: 共用的 frontend、backend、service、ingress 定義
- `k8s/overlays/dev`: 以 minikube 為目標的開發環境，包含 in-cluster MySQL
- `k8s/overlays/staging`: staging 環境，預設連接外部資料庫
- `k8s/overlays/production`: production 環境，預設連接外部資料庫

設計原則：

- backend 仍透過 `SPRING_PROFILES_ACTIVE` 對應 `dev`、`staging`、`production`
- frontend 仍透過 `VITE_API_BASE_URL` 決定 API 位置
- Kubernetes 主要負責注入對應環境值，而不是重新定義另一套環境模型
- 敏感資訊例如 DB 密碼、SMTP 密碼、JWT secret 不會直接寫進版本庫，改由 cluster Secret 或外部注入提供

### Frontend Image Build Strategy

frontend Docker image 現在可透過 build arg 注入 `VITE_API_BASE_URL`：

```bash
docker build --build-arg VITE_API_BASE_URL=/api -t lovius/bento-frontend:dev ./frontend
docker build --build-arg VITE_API_BASE_URL=/api -t lovius/bento-frontend:staging ./frontend
docker build --build-arg VITE_API_BASE_URL=/api -t lovius/bento-frontend:production ./frontend
```

若三個 Kubernetes 環境都採同一個 Ingress 路由模型，可共用 `/api`。若未來不同環境需要不同 API host，也可在 build 時改成絕對 URL。

### Minikube 邊界

- `dev` overlay 的目標環境是 minikube
- 但目前 repo 不要求每位開發者本機都安裝或實際執行 minikube
- 若本機資源不足，可先完成 manifests、ConfigMap/Secret 準備與靜態驗證

### Secret 準備

各 overlay 都提供 `*.example.env` 作為 Secret 範本，例如：

- `k8s/overlays/dev/backend-secrets.example.env`
- `k8s/overlays/dev/mysql-secrets.example.env`
- `k8s/overlays/staging/backend-secrets.example.env`
- `k8s/overlays/production/backend-secrets.example.env`

這些檔案只提供 key 與 placeholder，實際部署時請在目標 cluster 建立 Secret，不要把真實值提交進版本庫。

### 靜態驗證

在沒有本機 Kubernetes 叢集的情況下，可先執行：

```bash
./scripts/validate-k8s.sh
```

若系統上有 `kubectl` 或 `kustomize`，此腳本也會嘗試 build 各 overlay；若沒有，仍會完成結構檢查。

## 驗證紀錄

- 已依 OpenSpec A001 規格逐項對照員工登入、管理員登入、refresh token、忘記密碼、修改密碼、查詢員工、新增員工、CSV 匯入、停用啟用與重設密碼流程。
- 已依 A002 / A006 / A007 / A010 規格補上員工查詢便當、訂餐/改單/取消、個人訂餐記錄、管理員查單與代訂、供應商管理、部門管理、建立菜單、查詢菜單清單與編輯菜單流程。
- 已依 A003 規格補上每日 17:00 供應商通知排程、`notification_logs` 記錄、供應商通知失敗/異常/系統錯誤時的 A004 錯誤通知信箱發送。
- 已依 A004 規格補上管理員查詢、新增、刪除錯誤通知信箱 API 與前端設定頁。
- 已依 A005 / A008 規格補上月結期間計算、手動觸發月結報表、月結發送記錄查詢、報表收件信箱設定與管理員前端入口。
- 已補齊前端登入後分流：員工進入「訂便當 / 我的訂單」，管理員進入訂單管理頁，且員工端不顯示價格資訊。
- 已補上 `dev / staging / production` 三組 backend 環境設定與 `mock / smtp` mail mode 切換。
- 已確認 `frontend` 的 `npm run build` 與 `backend` 的 `./gradlew test` 可成功執行。
- `docker compose config` 已通過，可確認 compose 結構正確。
- 已於 2026-03-31 在本機 minikube 驗證 `k8s/overlays/dev`：`kubectl apply -k` 成功，`backend` / `frontend` / `mysql` 三個 pods 均達到 `Running`，且 frontend 可透過 minikube service tunnel 回應 `200`。
- 目前尚未完成真正的瀏覽器 E2E 驗證與實際外部 SMTP / MySQL 整合驗證。

## A003 / A005 / A006 邊界

- A003 會從隔日 `orders`、`menus`、`suppliers` 彙總供應商通知內容，並將結果寫入 `notification_logs`
- A003 發送失敗、異常或系統錯誤時，會透過 `ErrorNotificationRecipientProvider` 讀取 `error_notification_emails`
- A005 仍持續使用 `monthly_billing_logs`，沒有與 A003 共用記錄表
- A006 的 `orders.created_by` 已保留管理員代訂時的操作者資訊
- `orders.employee_id` 與 `orders.order_date` 的唯一鍵仍保留單一員工同日單筆訂單規則

## CSV 格式

匯入檔案需包含 header：

```csv
username,name,email,department
jane.hsu,Jane Hsu,jane@company.local,Operations
tom.lee,Tom Lee,tom@company.local,Finance
```
