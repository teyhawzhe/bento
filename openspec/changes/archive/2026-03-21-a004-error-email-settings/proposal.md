## Why

A003 已明確需要在通知失敗或排程發生系統錯誤時，將錯誤通知寄送到指定管理信箱，但目前系統尚未提供這些信箱的設定來源。A004 需要補上錯誤通知信箱的管理能力，讓後續排程與營運告警可以有穩定、可維護的收件名單。

## What Changes

- 新增管理員查詢目前錯誤通知信箱清單的能力。
- 新增管理員新增錯誤通知信箱的能力，並驗證 Email 格式後寫入資料庫。
- 新增管理員刪除既有錯誤通知信箱的能力。
- 建立 A004 API 契約、資料模型與權限邊界，供 A003 錯誤通知流程直接讀取。

## Capabilities

### New Capabilities
- `error-email-settings`: 管理員查詢、新增與刪除系統錯誤通知信箱清單。

### Modified Capabilities
- None.

## Impact

- 受影響程式碼：backend 系統設定模組、A003 錯誤通知 recipients provider、frontend 系統設定頁面。
- 受影響 API：新增 `/api/settings/error-emails` 與 `/api/settings/error-emails/{id}`。
- 受影響資料：新增 `error_notification_emails` 資料表，保存收件信箱、建立者與建立時間。
- 相依項目：需沿用 A001/A002 的管理員 JWT 權限，並與 A003 錯誤通知流程整合。
