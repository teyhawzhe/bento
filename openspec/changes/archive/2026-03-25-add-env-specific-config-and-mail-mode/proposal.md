## Why

目前專案只有單一組後端設定，尚未清楚區分 `dev`、`staging`、`production` 三種部署環境，也無法在不同 build 或執行環境下安全地切換設定。另一方面，Email 功能目前僅有記憶體 mock 實作，缺少可由設定控制的 `mock` / `smtp` 切換能力，無法在不改程式碼的前提下進行真實寄信測試。

## What Changes

- 新增三組環境設定模型，明確支援 `dev`、`staging`、`production` 的執行配置與 build/啟動方式。
- 新增 config-based mail delivery 設計，讓系統可透過設定切換 `mock` 與 `smtp` 發信模式。
- 集中定義 SMTP 設定欄位，例如 `host`、`port`、`username`、`password`、`from`、`auth`、`starttls`。
- 明確規範不同環境下的預設 mail mode 與敏感設定注入方式，避免把正式憑證寫死在程式或版本庫。
- 補齊 README / 操作說明，讓開發者知道如何用不同環境設定 build 與啟動專案。

## Capabilities

### New Capabilities
- `runtime-environment-configuration`: 定義系統如何支援 `dev`、`staging`、`production` 三種環境設定與對應的 build / startup 行為。
- `mail-delivery-configuration`: 定義系統如何以設定控制 `mock` 或 `smtp` 發信模式，以及 SMTP 欄位與安全邊界。

### Modified Capabilities
- `employee-authentication`: 更新認證相關流程對 Email 傳遞能力的依賴，要求忘記密碼與臨時密碼通知可由設定決定使用 `mock` 或 `smtp`。
- `employee-account-administration`: 更新建立員工、匯入員工與重設密碼通知的寄信行為，使其透過統一 mail delivery configuration 執行。
- `supplier-order-notification`: 更新 A003 通知流程對寄信通道的要求，讓供應商通知與錯誤通知可透過統一 mail delivery configuration 發送。
- `monthly-billing-reporting`: 更新 A005 月結通知對寄信通道的要求，讓月結通知可透過統一 mail delivery configuration 發送。

## Impact

- Affected code: `backend/src/main/resources/application*.yml`、`backend/src/main/java/com/lovius/bento/service/EmailService.java` 及其依賴服務、Docker / build 啟動設定、README。
- Affected systems: backend configuration loading、email delivery、deployment workflow。
- Likely dependencies: Spring Boot mail starter 或等效 SMTP 實作、環境變數管理、容器或 CI/CD build 參數。
