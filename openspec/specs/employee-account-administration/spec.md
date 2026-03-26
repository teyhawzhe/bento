## Purpose
定義管理員建立、查詢、匯入、更新員工資料與重設密碼時，需符合的 OpenAPI 契約與 refresh token 作廢規則。

## Requirements
### Requirement: 員工管理 API 必須對齊 OpenAPI 回應契約
系統 SHALL 讓 `/api/admin/employees`、`/api/admin/employees/{id}`、`/api/admin/employees/import`、`/api/admin/employees/{id}/status` 與 `/api/admin/employees/{id}/reset-password` 的成功與失敗回應符合 `../uml/openapi.yaml` 定義的 `status/data` 契約。管理員重設密碼成功時，系統 MUST 作廢目標員工所有未失效的 refresh token。

#### Scenario: 查詢員工清單回應符合 envelope
- **WHEN** 管理員成功呼叫 `GET /api/admin/employees`
- **THEN** 系統以 `status=success` 與 `data` 陣列回傳員工清單

#### Scenario: 依部門篩選員工清單
- **WHEN** 管理員呼叫 `GET /api/admin/employees?department_id=2`
- **THEN** 系統只回傳 `department.id = 2` 的員工資料

#### Scenario: 查詢全部員工不帶 department_id
- **WHEN** 管理員在員工清單頁選擇「全部」並呼叫 `GET /api/admin/employees`
- **THEN** 系統回傳全部員工，而不是要求特定哨兵值代表全部

#### Scenario: 重設員工密碼後 refresh token 被作廢
- **WHEN** 管理員成功呼叫 `PATCH /api/admin/employees/{id}/reset-password`
- **THEN** 系統更新目標員工密碼並作廢該員工所有未失效 refresh token

### Requirement: 員工建立與密碼通知必須透過統一 mail delivery configuration 發送
系統 SHALL 讓單一建立員工、CSV 匯入員工與管理員重設員工密碼通知透過統一 mail delivery configuration 發送。相同業務流程在 `mock` 與 `smtp` mode 間 MUST 維持一致的呼叫入口與業務結果語意。

#### Scenario: 管理員透過員工管理主 TAB 進入建立與維護流程
- **WHEN** 管理員在前端切換到「員工管理」主 TAB
- **THEN** 系統顯示新增員工表單、員工清單、部門篩選、編輯入口、啟停操作與重設密碼操作

#### Scenario: 建立員工時使用 mock mode
- **WHEN** 管理員建立員工且系統設定 `mail mode = mock`
- **THEN** 系統建立員工帳號並以 mock 發信方式記錄通知

#### Scenario: 匯入員工時使用 smtp mode
- **WHEN** 管理員匯入員工且系統設定 `mail mode = smtp`
- **THEN** 系統使用 SMTP 設定寄送初始密碼通知給成功匯入的員工

#### Scenario: 管理員重設員工密碼時使用 smtp mode
- **WHEN** 管理員重設員工密碼且系統設定 `mail mode = smtp`
- **THEN** 系統使用 SMTP 設定寄送重設後密碼通知
