## Purpose
定義部門主檔在 A010 管理流程中的查詢、新增與修改契約，並限制對外只暴露 UML/OpenAPI 定義的行為。

## Requirements
### Requirement: 管理員可查詢部門清單
系統 SHALL 允許已驗證的管理員查詢部門清單，供後台維護與員工建立流程使用。回傳資料 SHALL 至少包含 `id`、`name` 與 `created_at`，並符合 OpenAPI 定義的 `status/data` 契約。若系統內部保留其他欄位，MUST 視為內部實作細節，不得透過 A010 API 暴露額外行為。

#### Scenario: 查詢部門清單
- **WHEN** 管理員呼叫部門清單 API
- **THEN** 系統以 `status=success` 回傳部門清單，且每筆資料包含 `id`、`name` 與 `created_at`

#### Scenario: 部門清單可作為員工篩選下拉來源
- **WHEN** 管理員進入員工清單頁並需要依部門篩選員工
- **THEN** 系統可透過 `GET /api/admin/departments` 提供完整部門清單，供前端渲染「全部 + 部門列表」下拉選單

#### Scenario: 管理員透過部門管理主 TAB 進入維護畫面
- **WHEN** 管理員在前端切換到「部門管理」主 TAB
- **THEN** 系統顯示部門清單、建立部門輸入區與修改部門名稱入口

### Requirement: 管理員可新增部門資料
系統 SHALL 允許已驗證的管理員透過 `POST /api/admin/departments` 建立部門。建立請求 MUST 要求 `name`，且 MUST 驗證名稱唯一性。系統 MUST 僅暴露 UML 與 OpenAPI 已定義的建立、查詢與修改契約，不提供未定義的 delete 或 disable API。

#### Scenario: 成功新增部門
- **WHEN** 管理員送出合法且未重複的部門名稱
- **THEN** 系統建立部門並以 `status=success` 回傳新部門資料

#### Scenario: 新增為重複名稱
- **WHEN** 管理員建立部門時送出另一筆已存在的名稱
- **THEN** 系統拒絕請求並回傳名稱重複錯誤

### Requirement: 管理員可修改部門資料
系統 SHALL 允許已驗證的管理員透過 `PATCH /api/admin/departments/{id}` 修改部門名稱。修改請求 MUST 僅要求 `name`，且 MUST 驗證名稱唯一性。系統 MUST 不提供 UML 與 OpenAPI 未定義的部門刪除或停用 API 作為對外契約。

#### Scenario: 成功修改部門名稱
- **WHEN** 管理員送出有效且未重複的部門名稱
- **THEN** 系統儲存變更並以 `status=success` 回傳更新後部門資料

#### Scenario: 修改為重複名稱
- **WHEN** 管理員將部門名稱修改為另一筆已存在的名稱
- **THEN** 系統拒絕請求並回傳名稱重複錯誤
