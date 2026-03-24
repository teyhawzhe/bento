## MODIFIED Requirements

### Requirement: 系統必須持久化供應商通知 refresh 與錯誤記錄所需資料
系統 SHALL 建立 `notification_logs` 資料表，用於記錄 A003 每日供應商通知的通知日期、收件人、通知內容、發送狀態、錯誤訊息與建立時間。該資料表 MUST 與 A005 的 `monthly_billing_logs` 分離。

#### Scenario: A003 發送成功寫入通知記錄
- **WHEN** 系統成功寄送某供應商的每日通知 Email
- **THEN** 系統在 `notification_logs` 中建立一筆成功記錄

#### Scenario: A003 發送失敗寫入通知記錄
- **WHEN** 系統寄送某供應商的每日通知 Email 失敗或發生異常
- **THEN** 系統在 `notification_logs` 中建立一筆失敗或異常記錄

### Requirement: A003 通知必須透過統一 mail delivery configuration 發送
系統 SHALL 讓 A003 每日供應商通知與錯誤通知流程透過統一 mail delivery configuration 發送。當 `mail mode = mock` 時，系統 MUST 保留既有通知流程的可觀測性；當 `mail mode = smtp` 時，系統 MUST 使用配置好的 SMTP 設定寄送通知。

#### Scenario: A003 在 mock mode 下發送
- **WHEN** 每日供應商通知排程執行且系統設定 `mail mode = mock`
- **THEN** 系統完成通知流程、保留可觀測的寄信紀錄，且不連線外部 SMTP

#### Scenario: A003 在 smtp mode 下發送
- **WHEN** 每日供應商通知排程執行且系統設定 `mail mode = smtp`
- **THEN** 系統使用 SMTP 設定寄送供應商通知與錯誤通知
