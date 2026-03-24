## Purpose
定義 A003 每日供應商通知的持久化記錄需求，以及與 A005 月結記錄的邊界。

## Requirements
### Requirement: 系統必須持久化供應商通知 refresh 與錯誤記錄所需資料
系統 SHALL 建立 `notification_logs` 資料表，用於記錄 A003 每日供應商通知的通知日期、收件人、通知內容、發送狀態、錯誤訊息與建立時間。該資料表 MUST 與 A005 的 `monthly_billing_logs` 分離。

#### Scenario: A003 發送成功寫入通知記錄
- **WHEN** 系統成功寄送某供應商的每日通知 Email
- **THEN** 系統在 `notification_logs` 中建立一筆成功記錄

#### Scenario: A003 發送失敗寫入通知記錄
- **WHEN** 系統寄送某供應商的每日通知 Email 失敗或發生異常
- **THEN** 系統在 `notification_logs` 中建立一筆失敗或異常記錄
