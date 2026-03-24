## 1. 環境設定基礎

- [x] 1.1 建立 `application-dev.yml`、`application-staging.yml`、`application-production.yml`，並整理共用與環境差異設定
- [x] 1.2 為 mail configuration 建立集中設定結構，至少涵蓋 `mode`、`from`、`smtp.host`、`smtp.port`、`smtp.username`、`smtp.password`、`smtp.auth`、`smtp.starttls`
- [x] 1.3 更新 Docker / 啟動方式，讓 backend 可透過環境變數或參數指定目標環境

## 2. Mail Delivery 抽象與實作

- [x] 2.1 重構 `EmailService` 與相關抽象，保留既有業務入口並將實際送信責任下沉
- [x] 2.2 實作 `mock` mode 的 mail sender，保留既有可觀測的測試 / 開發行為
- [x] 2.3 實作 `smtp` mode 的 mail sender，支援通用 SMTP 設定與 Gmail SMTP 測試情境
- [x] 2.4 補上缺少的依賴與設定綁定，確認 `mock` / `smtp` 可由 config 切換

## 3. 業務流程串接

- [x] 3.1 調整認證相關寄信流程，讓 forgot password / 臨時密碼通知走統一 mail delivery configuration
- [x] 3.2 調整員工建立、匯入與重設密碼通知流程，讓其走統一 mail delivery configuration
- [x] 3.3 調整 A003 供應商通知與錯誤通知流程，讓其走統一 mail delivery configuration
- [x] 3.4 調整 A005 月結通知流程，讓其走統一 mail delivery configuration

## 4. 驗證與文件

- [x] 4.1 更新 backend 測試，涵蓋 `mock` mode、`smtp` mode 配置綁定與主要寄信流程
- [x] 4.2 驗證 `dev`、`staging`、`production` 三組環境至少可成功載入設定並啟動
- [x] 4.3 更新 README 與部署說明，補充三組環境的 build / startup 方式與 SMTP 設定需求
