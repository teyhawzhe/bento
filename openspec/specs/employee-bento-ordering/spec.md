## Purpose
定義員工查詢可訂菜單、建立訂單與查詢個人訂單時，需符合的 OpenAPI 契約與欄位限制。

## Requirements
### Requirement: 員工訂餐 API 必須符合 OpenAPI 回應契約且維持不顯示價格
系統 SHALL 讓 `/api/orders/menu`、`/api/orders`、`/api/orders/{id}` 與 `/api/orders/me` 的成功回應符合 `status/data` 契約。員工端菜單資料 MUST 不包含價格欄位，即使管理員端與資料庫內保留價格資訊。

#### Scenario: 取得員工菜單時不回傳價格
- **WHEN** 員工成功呼叫 `GET /api/orders/menu`
- **THEN** 系統以 `status=success` 回傳可訂菜單，且每筆資料不包含價格欄位

#### Scenario: 查詢個人訂單回應符合 envelope
- **WHEN** 員工成功呼叫 `GET /api/orders/me`
- **THEN** 系統以 `status=success` 與 `data` 陣列回傳個人訂單清單
