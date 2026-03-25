## Why

`../uml/input/A012.md` 新增了「CSV 批次匯入管理」需求，要求管理後台提供獨立的 CSV 匯入 TAB，集中處理員工、部門、供應商與便當菜單四種類型的範本下載與批次匯入。目前系統只有員工 CSV 匯入能力，而且仍沿用較早期的欄位與回傳格式，尚未覆蓋 A012 定義的統一範本下載、UTF-8 / 表頭驗證、5000 筆 transaction 批次處理，以及匯入成功後顯示成功資料清單等行為。

如果不先整理成一個獨立 change，後續實作很容易只補部分 API 或只新增前端入口，讓員工、部門、供應商、菜單四條匯入流程各自分岐，難以維持一致的驗證與錯誤回應模式。

## What Changes

- 新增 A012 CSV 批次匯入管理能力，定義管理員可下載四種 CSV 範本並上傳對應 CSV 進行批次匯入。
- 明確規範四種 CSV 的表頭、必填欄位、重複資料檢查與 UTF-8 驗證。
- 明確規範匯入流程以 5000 筆為一個 transaction 批次，任一批次失敗時整批 rollback，並回傳處理到的 CSV 行號與錯誤原因。
- 新增管理後台的獨立 CSV 匯入主 TAB，與「系統設定」處於同一層級，集中呈現範本下載、檔案上傳與成功匯入結果清單。
- 對齊既有員工、部門、供應商、菜單主檔能力，讓匯入成功後資料契約與既有查詢/建立流程維持一致。

## Capabilities

### New Capabilities

- `csv-batch-import-management`: 定義管理員下載範本、上傳四種 CSV、進行批次驗證與 transaction 匯入，並查看成功匯入結果清單的能力。

### Modified Capabilities

- `employee-account-administration`: 對齊 A012 的員工 CSV 匯入欄位、回傳結果與批次驗證規則。
- `department-management`: 補上部門 CSV 批次匯入與範本下載的使用情境。
- `bento-supplier-management`: 補上供應商 CSV 批次匯入與範本下載的使用情境。
- `bento-menu-management`: 補上菜單 CSV 批次匯入與範本下載的使用情境。
- `frontend-tab-navigation-and-message-box`: 管理員端需新增與「系統設定」同層級的 CSV 匯入主 TAB，並以既有 MessageBox 呈現成功、失敗與驗證提示。

## Impact

- Affected code: backend import controller/service/repository、CSV parsing 與 transaction handling、frontend 管理員匯入頁與 API client。
- API: 新增 `GET /api/admin/import/template/{type}`、`POST /api/admin/import/employees`、`POST /api/admin/import/departments`、`POST /api/admin/import/suppliers`、`POST /api/admin/import/menus`。
- Data validation: 需補上四種 CSV 的表頭驗證、UTF-8 驗證、批次內重複檢查，以及與既有資料的衝突檢查。
- UI: 管理員後台需新增與「系統設定」同層級的 CSV 匯入主 TAB，支援範本下載、檔案選擇、送出匯入與成功清單顯示。
