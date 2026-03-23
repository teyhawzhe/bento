## Why

目前新增單一員工與 CSV 匯入員工的流程都只收 `username`、`name`、`email`，無法在建立帳號時指定員工所屬部門，導致 A010 部門資料無法與 A001 員工管理流程連動。此外，系統也尚未明確定義完整的部門維護後台 CRUD，讓部門資料無法由管理者自行維護。現在需要同時補上部門維護能力與員工流程的部門欄位，讓管理者能建立、修改、查詢部門，並在建立與查看員工資料時直接看到 `department.name`。

## What Changes

- 新增 A010 部門資料需求，定義完整的部門維護後台 CRUD，以及可供員工建立與顯示使用的 department 主檔能力。
- 修改單一新增員工流程，建立時必須帶入 `department`，並在回傳與畫面顯示 `department.name`。
- 修改 CSV 匯入員工流程，每列資料需包含 `department` 欄位，系統需驗證部門是否存在後才建立帳號。
- 修改員工清單與相關管理畫面，顯示員工所屬部門名稱。

## Capabilities

### New Capabilities
- `department-management`: 定義部門主檔的後台 CRUD、使用限制，以及供員工建立與顯示流程使用的部門資料能力。

### Modified Capabilities
- `employee-account-administration`: 新增與匯入員工時需帶入部門，且員工清單與建立結果需顯示 `department.name`。

## Impact

- Affected specs: `openspec/specs/employee-account-administration/spec.md`、新增 `openspec/specs/department-management/spec.md`
- Affected backend: 部門 CRUD API、員工建立 API、CSV 匯入 API、員工清單查詢、員工/部門 DTO、Repository、Service
- Affected frontend: 部門管理畫面、管理員新增員工表單、CSV 匯入格式說明、員工清單顯示、部門下拉選單資料載入
- Data impact: 員工資料需關聯 department，部門資料需有可維護主檔，CSV 欄位格式需同步調整
