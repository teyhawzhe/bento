## Purpose
定義管理員維護供應商資料時，需符合的 API 契約與回應格式。

## Requirements
### Requirement: 供應商管理 API 必須符合 OpenAPI 契約
系統 SHALL 讓 `/api/suppliers` 與 `/api/suppliers/{id}` 的查詢、建立與修改行為符合 `../uml/openapi.yaml` 定義的 request/response 契約，並以 `status/data` 格式回傳。

#### Scenario: 供應商清單查詢符合 envelope
- **WHEN** 管理員成功呼叫 `GET /api/suppliers`
- **THEN** 系統以 `status=success` 與 `data` 陣列回傳供應商清單

#### Scenario: 單一供應商查詢符合 envelope
- **WHEN** 管理員成功呼叫 `GET /api/suppliers/{id}`
- **THEN** 系統以 `status=success` 與 `data` 物件回傳供應商詳細資料
