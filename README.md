# 公司員工訂便當系統

目前已完成 A001「內部員工登入驗證」與 A002「員工訂便當」第一版，包含前端登入後訂餐流程、管理員菜單維護、供應商建立，以及後續可銜接 Docker Compose 與 A003 排程的執行邊界。

## 專案結構

- `frontend`: React + TypeScript + Vite + Tailwind 的登入、訂餐與管理介面
- `backend`: Java 21 + Spring Boot 的 A001/A002 API
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
- 員工查詢下週便當 `GET /api/orders/menu`
- 員工新增訂餐 `POST /api/orders`
- 員工更新訂餐 `PATCH /api/orders/{id}`
- 員工取消訂餐 `DELETE /api/orders/{id}`
- 員工查詢個人訂餐記錄 `GET /api/orders/me`
- 管理員查詢菜單清單 `GET /api/menus`
- 管理員建立菜單 `POST /api/menus`
- 管理員編輯菜單 `PATCH /api/menus/{id}`
- 管理員新增供應商 `POST /api/suppliers`
- 管理員取消指定員工訂餐 `DELETE /api/admin/orders/{id}`

## Demo 帳號

- 員工: `alice / WelcomeA1`
- 管理員: `admin / AdminPassA1`
- 停用帳號: `disabled.user / DisabledA1`

## 目前假設

- A001/A002 目前採 JDBC + `schema.sql` 管理 `employees`、`suppliers`、`menus`、`orders`
- Email 發送先以服務 stub 模擬，尚未串 SMTP
- 管理員取消訂餐截止時間先固定為訂餐日前一日 17:00，供後續 A003 排程沿用
- 真實部署設定、供應商正式通知與月結報表可在 A003/A005 之後接續補強

## 啟動方式

目前專案已包含 `backend/build.gradle` 與 Gradle Wrapper，可直接在 `backend` 透過 `./gradlew` 執行建置；前端相依套件仍尚未安裝，因此這一版先提供完整原始碼結構與容器化配置。

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

目前後端已改為 JDBC + MySQL schema 邊界，`mysql` 服務可直接承接 `employees`、`suppliers`、`menus`、`orders` 等資料表。

## 驗證紀錄

- 已依 OpenSpec A001 規格逐項對照員工登入、管理員登入、忘記密碼、修改密碼、查詢員工、新增員工、CSV 匯入、停用啟用與重設密碼流程。
- 已依 A002 規格補上員工查詢下週便當、訂餐/改單/取消、個人訂餐記錄、管理員新增供應商、建立菜單、查詢含歷史菜單與編輯菜單流程。
- 已修正狀態切換 API 的請求格式相容性，後端可同時接受 `is_active` 與 `isActive`。
- 已補齊前端登入後分流：員工進入訂餐頁、管理員進入菜單管理頁，且員工端不顯示價格資訊。
- `docker compose config` 已通過，可確認 compose 結構正確。
- 已補上 Gradle Wrapper，並確認 `cd backend && ./gradlew test` 可成功執行。
- 目前尚未完成真正的瀏覽器互動驗證與完整 API 情境驗證，原因是 workspace 尚未完成前端依賴安裝，且本機尚未啟動可供 Spring Boot 連線的 MySQL 服務。

## A003 / A006 邊界

- `suppliers.email`、`menus.supplier_id` 與 `orders.order_date` 已保留 A003 依供應商彙整隔日訂單並寄信所需欄位。
- `orders.created_by` 已保留 A006 管理員代替員工新增訂餐時的操作者資訊。
- `orders.employee_id` 與 `orders.order_date` 的唯一鍵，讓未來 A006 代訂仍可沿用同一日覆蓋規則。

## CSV 格式

匯入檔案需包含 header：

```csv
username,name,email
jane.hsu,Jane Hsu,jane@company.local
tom.lee,Tom Lee,tom@company.local
```
