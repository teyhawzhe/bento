## Context

目前 backend 只有單一組 `application.yml`，尚未明確區分 `dev`、`staging`、`production` 三種環境，也沒有一致的 build / startup 規則。Email 相關流程雖然已集中透過 `EmailService` 呼叫，但實作仍是記憶體 mock，無法透過設定切換成 SMTP，也無法在不改碼的前提下做真實寄信驗證。

這次變更同時橫跨：
- Spring Boot 設定載入方式
- Email delivery 抽象與實作切換
- Docker / build 啟動參數
- README 與操作流程

因此需要先明確設計，避免後續把環境設定與 mail 邏輯硬綁在一起。

## Goals / Non-Goals

**Goals:**
- 讓 backend 明確支援 `dev`、`staging`、`production` 三組設定。
- 讓 build / 啟動流程可明確指定目標環境。
- 讓 mail delivery 採 `config-based` 切換，至少支援 `mock` 與 `smtp`。
- 集中管理 SMTP 欄位與敏感設定，避免硬寫在程式碼內。
- 保持既有業務服務呼叫方式穩定，降低對 `AuthService`、`EmployeeService`、`SupplierOrderNotificationService`、`MonthlyBillingService` 的衝擊。

**Non-Goals:**
- 不在本 change 內導入第三方 Email API provider 專屬 SDK。
- 不在本 change 內處理非 SMTP 的寄信管道，例如 HTTP API 模式。
- 不在本 change 內重新設計 A003 / A005 的通知內容格式。
- 不在本 change 內處理完整 CI/CD pipeline，只定義專案內可支援的環境 build / startup 方式。

## Decisions

### 1. 採用 Spring Boot profile 對應三組環境
系統將以 `dev`、`staging`、`production` 三個 profile 管理環境差異，並以：
- `application.yml` 放共用基底
- `application-dev.yml`
- `application-staging.yml`
- `application-production.yml`

作為環境覆寫層。

原因：
- Spring Boot 原生支援，學習成本最低。
- 對 Docker、IDE、CLI 啟動方式都一致。
- 適合把 DB、JWT、mail mode、logging 等差異集中管理。

替代方案：
- 單一檔案配大量環境變數：彈性高，但可讀性差，容易失去預設值與文件化能力。
- build-time 直接替換整份配置：可做，但比 profile 更難維護與本機測試。

### 2. Mail delivery 採 config-based mode，而不是只靠環境綁死
系統將使用統一的 mail configuration，例如：
- `app.mail.mode=mock|smtp`
- `app.mail.from`
- `app.mail.smtp.host`
- `app.mail.smtp.port`
- `app.mail.smtp.username`
- `app.mail.smtp.password`
- `app.mail.smtp.auth`
- `app.mail.smtp.starttls`

原因：
- 同一個環境也可能需要切換寄信模式，例如 `dev` 想測真 SMTP。
- 避免把 `dev=mock`、`production=smtp` 寫死，保留測試彈性。
- 後續要從 Gmail SMTP 換成 Resend / Brevo / SES SMTP 時，只要換設定值。

替代方案：
- 僅用 profile 決定 mail mode：實作簡單，但不符合目前想在測試環境直接切真 SMTP 的需求。

### 3. 保留 EmailService 的業務語意，將發信方式下沉
現有業務層已使用 `EmailService.sendPasswordEmail(...)` 與 `EmailService.sendEmail(...)`。設計上將保留這個 service 作為業務入口，再把實際送信責任下沉到可切換的 sender 抽象，例如：
- `MockMailSender`
- `SmtpMailSender`

原因：
- 降低對既有業務服務與測試的改動面。
- 讓「寄什麼」與「怎麼寄」分離。
- 保持未來擴充其他發信提供者的空間。

替代方案：
- 直接在 EmailService 內用 `if mode == ...` 分支：短期可行，但長期會讓 mock/smtp 邏輯耦合。

### 4. 敏感資訊 MUST 由環境變數或部署設定注入
`staging` 與 `production` 的 SMTP 帳密、寄件人、正式 DB 參數等敏感資訊 MUST 不進版本庫，應透過環境變數或部署平台 secret 注入。設定檔只保留 key 與必要預設值策略。

原因：
- 避免憑證外洩。
- 符合 `dev/staging/production` 常見部署方式。

### 5. Build / startup 文件化，不強制產生三份不同二進位
本 change 將把「依環境 build / 啟動」定義為可明確指定目標 profile 與設定來源，而非一定要輸出三個完全不同的 jar。專案可透過啟動參數、環境變數或容器變數決定目前環境。

原因：
- Spring Boot 慣例是同一個 artifact 搭配不同 profile 執行。
- 比維護三套獨立 artifact 更簡單。

替代方案：
- 為每個環境製作獨立 build artifact：可行，但對目前專案過重。

## Risks / Trade-offs

- [Risk] `mock` 與 `smtp` 切換後，寄信失敗會讓既有流程第一次暴露真實外部依賴問題
  → Mitigation: 在 design 與 tasks 中要求補上 SMTP 模式測試與失敗路徑驗證。

- [Risk] `dev`、`staging`、`production` 設定差異過大，可能導致「只在某環境出錯」
  → Mitigation: 保持共用基底設定，環境檔只覆寫必要差異，並在 README 中列出必填環境變數。

- [Risk] 如果把 Gmail SMTP 當作長期正式方案，可能遇到額度與政策限制
  → Mitigation: 設計保持 SMTP 通用，不把 Gmail 寫死為唯一 provider。

- [Risk] 既有測試依賴記憶體寄信行為，切換抽象後可能需要調整測試方式
  → Mitigation: 保留 mock sender 與可觀測的測試介面，降低測試改寫成本。

## Migration Plan

1. 先建立三組環境設定檔與共用設定模型。
2. 導入 mail configuration 與 sender 抽象，保留 mock 模式為預設開發選項。
3. 補上 SMTP 依賴與 SMTP mode 實作。
4. 更新 Docker / 啟動文件，讓環境與 mail mode 都可由設定選擇。
5. 在 `dev` 或 `staging` 以測試帳號驗證 SMTP 真實寄信流程。

Rollback strategy:
- 若 SMTP 導入後出現問題，可把 `app.mail.mode` 切回 `mock`，不必立即回退整個版本。
- 若某組環境設定不穩定，可先保留共用基底並只回退該 profile 覆寫檔。

## Open Questions

- `staging` 的預設 mail mode 要用 `mock` 還是 `smtp`？
- SMTP 模式下，是否需要保留最近寄出信件的 debug / audit 能力供測試觀察？
- 寄信失敗時，建立員工、forgot password、A003、A005 是否都維持目前「拋出錯誤」策略，或要分流程定義不同容錯行為？
