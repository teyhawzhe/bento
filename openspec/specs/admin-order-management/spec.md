## Purpose
定義管理員查詢、代訂與取消員工訂餐時，需對齊的 API 契約與預設查詢規則。

## Requirements
### Requirement: 管理員訂單 API 必須符合 OpenAPI 回應契約
系統 SHALL 讓 `/api/admin/orders`、`/api/admin/orders/{id}` 與 `/api/admin/suppliers` 的成功與失敗回應符合 `status/data` 契約。管理員查單 API 預設查詢區間 MUST 為今天到今天。

#### Scenario: 管理員查詢訂單回應符合 envelope
- **WHEN** 管理員成功呼叫 `GET /api/admin/orders`
- **THEN** 系統以 `status=success` 與 `data` 陣列回傳查詢結果

#### Scenario: 管理員查詢供應商與便當選項符合 envelope
- **WHEN** 管理員成功呼叫 `GET /api/admin/suppliers`
- **THEN** 系統以 `status=success` 回傳供應商及其便當選項資料
