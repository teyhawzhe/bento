## MODIFIED Requirements

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
