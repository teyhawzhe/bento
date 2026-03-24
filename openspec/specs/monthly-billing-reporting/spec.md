## Purpose
定義月結報表 API 與 A005 發送記錄的保存方式，並明確區隔 A003 與 A005 的記錄表。

## Requirements
### Requirement: 月結報表 API 與通知記錄必須與供應商通知分離
系統 SHALL 持續以 `monthly_billing_logs` 記錄 A005 月結報表發送結果，且 MUST 不與 A003 供應商每日通知共用記錄表。`/api/admin/reports/monthly` 的查詢與手動觸發行為 SHALL 符合 OpenAPI 定義的回應契約。

#### Scenario: 月結發送記錄維持獨立資料表
- **WHEN** 系統完成月結報表發送
- **THEN** 系統將結果寫入 `monthly_billing_logs`，且不寫入 `notification_logs`

#### Scenario: 查詢月結記錄符合 envelope
- **WHEN** 管理員成功呼叫 `GET /api/admin/reports/monthly`
- **THEN** 系統以 `status=success` 與 `data` 陣列回傳月結報表發送記錄
