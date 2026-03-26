## CSV Import Samples

這裡提供可供 A012 CSV 匯入功能測試的範例資料：

- `departments.csv`
- `employees.csv`
- `suppliers.csv`
- `menus.csv`

建議匯入順序：

1. `departments.csv`
2. `suppliers.csv`
3. `employees.csv`
4. `menus.csv`

## Assumption

`employees.csv` 內的 `department_id` 與 `menus.csv` 內的 `supplier_id`
是以「空資料庫或新環境首次匯入」為前提設計，假設部門與廠商匯入後
ID 會從 `1` 開始依序建立。

如果你的資料庫中已經有既有部門或廠商，請先依實際 ID 調整：

- `employees.csv` 的 `department_id`
- `menus.csv` 的 `supplier_id`
