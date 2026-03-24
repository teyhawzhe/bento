## Purpose
定義錯誤通知信箱設定 API，以及其在 A003 供應商通知異常流程中的收件用途。

## Requirements
### Requirement: 錯誤通知信箱 API 必須符合 OpenAPI 契約
系統 SHALL 讓錯誤通知信箱清單的查詢、新增與刪除 API 以 `status/data` 格式回傳，並作為 A003 供應商通知排程發生錯誤時的收件來源。

#### Scenario: 查詢錯誤通知信箱符合 envelope
- **WHEN** 管理員成功呼叫 `GET /api/settings/error-emails`
- **THEN** 系統以 `status=success` 與 `data` 陣列回傳錯誤通知信箱清單

#### Scenario: A003 錯誤通知使用既有收件清單
- **WHEN** 供應商通知排程發生發送失敗或系統錯誤
- **THEN** 系統使用錯誤通知信箱清單作為錯誤通知收件來源
