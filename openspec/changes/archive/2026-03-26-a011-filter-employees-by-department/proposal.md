## Why

`../uml/input/A011.md` 新增了「管理員以部門篩選員工清單」需求，但目前 OpenSpec 主規格僅描述管理員可查詢員工清單與部門清單，尚未明確把 `department_id` 查詢參數、`全部` 選項語意，以及員工頁面的篩選互動固定下來。若不先補齊這個 change，後續前後端可能各自實作不同的篩選方式，導致 A011 與 A001 / A010 交界持續模糊。

## What Changes

- 明確定義管理員可透過 `GET /api/admin/employees?department_id={id}` 依部門篩選員工，未帶參數時回傳全部員工。
- 明確定義管理員員工清單頁需載入 `GET /api/admin/departments` 作為篩選下拉選單資料來源，並提供「全部」選項。
- 補上部門篩選流程的前後端行為與驗證責任，確保 A011 與既有員工管理能力一致。
- 將 A011 影響範圍限制在查詢與畫面互動，不新增資料表欄位，也不變更員工建立、修改、停用或重設密碼流程。

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `employee-account-administration`: 補上員工清單依 `department_id` 篩選的 API 契約與管理員畫面互動。
- `department-management`: 補上部門清單作為員工篩選下拉資料來源的使用情境。

## Impact

- Affected code: A001 員工查詢 controller/service/repository、管理員員工列表前端頁面、相關 API client 與測試。
- API: 沿用既有 `GET /api/admin/employees`，但明確要求支援可選 `department_id` 查詢參數；沿用 `GET /api/admin/departments` 作為篩選選單資料來源。
- Data model: 無新增資料表或欄位，沿用 `employees.department_id` 與 `departments.id` 關聯。
- UI: 員工清單頁需提供部門篩選下拉選單與切換回「全部」的互動。
