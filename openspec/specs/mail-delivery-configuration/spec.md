## Purpose
定義系統如何以設定控制 `mock` 與 `smtp` 寄信模式，並集中管理 SMTP 相關欄位與測試可觀測性。

## Requirements
### Requirement: 系統必須以設定控制 mail delivery mode
系統 SHALL 提供統一的 mail delivery configuration，至少支援 `mock` 與 `smtp` 兩種模式，並讓系統在不修改業務程式碼的前提下切換發信行為。

#### Scenario: 使用 mock mode 啟動
- **WHEN** 系統設定 `mail mode = mock`
- **THEN** 系統使用 mock 發信行為而不連線外部 SMTP 服務

#### Scenario: 使用 smtp mode 啟動
- **WHEN** 系統設定 `mail mode = smtp`
- **THEN** 系統使用 SMTP 設定將 Email 寄送到外部郵件服務

### Requirement: SMTP 設定欄位必須集中且可覆寫
系統 SHALL 集中定義 SMTP 相關設定欄位，至少包含 `host`、`port`、`username`、`password`、`from`、`auth` 與 `starttls`。這些欄位 MUST 可由環境設定或外部變數覆寫。

#### Scenario: 以 Gmail SMTP 測試寄信
- **WHEN** 開發者提供 Gmail SMTP 對應設定值並切換為 `smtp` mode
- **THEN** 系統使用該組 SMTP 設定執行寄信流程

#### Scenario: 切換 SMTP provider 不需修改業務流程
- **WHEN** 維運將 SMTP 設定從 Gmail 改為其他 SMTP provider
- **THEN** 業務服務維持原本寄信呼叫方式而不需修改流程邏輯

### Requirement: mock mode 必須保留開發與測試可觀測性
系統 SHALL 在 `mock` mode 下保留可供開發與測試觀察的寄信結果，讓既有測試與本機驗證能確認信件是否被觸發。每次以 `mock` mode 執行寄信流程時，系統 MUST 在 application log 中輸出可查驗的寄信內容，至少包含收件人、主旨與信件內容。此 log 行為 MUST 不改變既有業務流程結果，且 MUST 不延伸到 `smtp` mode 的真實寄信路徑。

#### Scenario: mock mode 可觀察已送出信件
- **WHEN** 系統以 `mock` mode 執行寄信流程
- **THEN** 開發者或測試可觀察到已觸發的信件紀錄

#### Scenario: mock mode 在 log 中輸出完整查驗資訊
- **WHEN** 系統以 `mock` mode 執行寄信流程
- **THEN** application log 會包含該封信的收件人、主旨與信件內容

#### Scenario: smtp mode 不輸出 mock email 內容 log
- **WHEN** 系統設定 `mail mode = smtp` 並執行寄信流程
- **THEN** 系統不會因本需求而額外輸出 mock email 內容 log，且維持既有 SMTP 寄信行為
