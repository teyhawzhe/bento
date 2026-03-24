## ADDED Requirements

### Requirement: 菜單管理 API 必須符合 OpenAPI 契約
系統 SHALL 讓 `/api/menus` 與 `/api/menus/{id}` 的查詢、建立與修改行為符合 `../uml/openapi.yaml` 定義的 request/response 契約，並以 `status/data` 格式回傳。

#### Scenario: 菜單清單查詢符合 envelope
- **WHEN** 管理員成功呼叫 `GET /api/menus`
- **THEN** 系統以 `status=success` 與 `data` 陣列回傳菜單清單

#### Scenario: 建立菜單符合 envelope
- **WHEN** 管理員成功呼叫 `POST /api/menus`
- **THEN** 系統以 `201` 與 `status=success` 回傳新建菜單資料

