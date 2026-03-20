# 公司員工訂便當系統

目前已完成 A001「內部員工登入驗證」第一版，包含前端簡約風格 UI、後端 API 骨架，以及後續可銜接 Docker Compose 的執行邊界。

## 專案結構

- `frontend`: React + TypeScript + Vite + Tailwind 的登入與管理介面
- `backend`: Java 21 + Spring Boot 的 A001 API
- `docker-compose.yml`: 前端、後端、MySQL 的容器編排檔
- `openspec`: OpenSpec 設定

## 已實作功能

- 員工登入 `POST /api/auth/login`
- 管理員登入 `POST /api/admin/auth/login`
- 忘記密碼 `POST /api/auth/forgot-password`
- 修改密碼 `PATCH /api/auth/change-password`
- 查詢員工 `GET /api/admin/employees`
- 新增員工 `POST /api/admin/employees`
- CSV 匯入員工 `POST /api/admin/employees/import`
- 啟用或停用員工 `PATCH /api/admin/employees/{id}/status`
- 管理員重設員工密碼 `PATCH /api/admin/employees/{id}/reset-password`

## Demo 帳號

- 員工: `alice / WelcomeA1`
- 管理員: `admin / AdminPassA1`
- 停用帳號: `disabled.user / DisabledA1`

## 目前假設

- 資料層先用記憶體 repository，方便快速驗證流程
- Email 發送先以服務 stub 模擬，尚未串 SMTP
- 真實資料庫、JWT 黑名單與正式部署設定可在 A002 之後接續補強

## 啟動方式

目前 workspace 尚未安裝 Gradle，前端相依套件也尚未安裝，因此這一版先提供完整原始碼結構與容器化配置。

建議啟動方式：

1. 在 `frontend` 安裝套件後執行 `npm run dev`
2. 在 `backend` 以 Gradle 啟動 Spring Boot
3. 或使用 `docker compose up --build` 啟動前端、後端與 MySQL

### 快速指令

在專案根目錄可直接執行：

```bash
./scripts/dev-frontend.sh
./scripts/dev-backend.sh
./scripts/dev-compose.sh
```

- `dev-frontend.sh`: 自動安裝前端依賴後啟動 Vite
- `dev-backend.sh`: 優先使用 `./gradlew`，其次使用系統 `gradle`
- `dev-compose.sh`: 直接執行 `docker compose up --build`

## Docker Compose

- `frontend`: 使用 Nginx 提供打包後的 React 靜態頁面，對外開放 `5173`
- `backend`: 以多階段 Dockerfile 建立 Spring Boot 映像，對外開放 `8080`
- `mysql`: 預留 MySQL 8.4 服務，對外開放 `3306`

目前後端資料層仍為記憶體 repository，`mysql` 服務主要是為後續切換正式資料庫時保留執行邊界。

## 驗證紀錄

- 已依 OpenSpec A001 規格逐項對照員工登入、管理員登入、忘記密碼、修改密碼、查詢員工、新增員工、CSV 匯入、停用啟用與重設密碼流程。
- 已修正狀態切換 API 的請求格式相容性，後端可同時接受 `is_active` 與 `isActive`。
- 已補齊前端登出時呼叫 `POST /api/auth/logout`，讓前後端流程與規格一致。
- `docker compose config` 已通過，可確認 compose 結構正確。
- 目前尚未完成真正的容器建置與瀏覽器互動驗證，原因是本機無法連線至 Docker daemon，且 workspace 尚未完成前端依賴安裝與 Gradle 執行環境配置。

## CSV 格式

匯入檔案需包含 header：

```csv
username,name,email
jane.hsu,Jane Hsu,jane@company.local
tom.lee,Tom Lee,tom@company.local
```
