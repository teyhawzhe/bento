## Why

目前 A001 只支援管理員新增、匯入、停用與重設員工帳號，當員工的姓名、帳號、Email、部門或管理員身分有異動時，管理員無法直接在系統內維護既有資料。`A001.md` 已新增「管理者可以修改員工資料」需求，因此需要補上對應的 API、驗證規則與後台操作流程。

## What Changes

- 新增管理員修改既有員工資料的能力，提供 `PATCH /api/admin/employees/{id}` 更新員工可編輯欄位。
- 允許管理員更新 `username`、`name`、`email`、`departmentId` 與 `isAdmin`，其中 `id` 仍為唯讀欄位。
- 新增員工資料更新時的唯一性驗證與部門有效性驗證，避免與其他員工帳號或 Email 衝突。
- 補上管理端員工清單中的編輯入口與編輯表單，讓管理員可在同一個 A001 管理流程內完成修改。

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `employee-account-administration`: 新增管理員修改員工資料的需求、API 契約、驗證規則與後台操作流程。

## Impact

- Affected code: A001 controller、employee service / repository、員工管理前端畫面、員工 DTO 與測試。
- API: 新增 `PATCH /api/admin/employees/{id}`。
- Data validation: 更新員工時需驗證 `username`、`email` 唯一性，以及 `departmentId` 是否存在且可用。
- UI: 員工管理畫面需支援載入既有員工資料並編輯送出。
