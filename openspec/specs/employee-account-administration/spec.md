## Purpose
定義管理員建立、查詢、匯入、更新員工資料與重設密碼時，需符合的 OpenAPI 契約與 refresh token 作廢規則。

## Requirements
### Requirement: 員工管理 API 必須對齊 OpenAPI 回應契約
系統 SHALL 讓 `/api/admin/employees`、`/api/admin/employees/{id}`、`/api/admin/employees/import`、`/api/admin/employees/{id}/status` 與 `/api/admin/employees/{id}/reset-password` 的成功與失敗回應符合 `../uml/openapi.yaml` 定義的 `status/data` 契約。管理員重設密碼成功時，系統 MUST 作廢目標員工所有未失效的 refresh token。

#### Scenario: 查詢員工清單回應符合 envelope
- **WHEN** 管理員成功呼叫 `GET /api/admin/employees`
- **THEN** 系統以 `status=success` 與 `data` 陣列回傳員工清單

#### Scenario: 重設員工密碼後 refresh token 被作廢
- **WHEN** 管理員成功呼叫 `PATCH /api/admin/employees/{id}/reset-password`
- **THEN** 系統更新目標員工密碼並作廢該員工所有未失效 refresh token
