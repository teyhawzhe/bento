## ADDED Requirements

### Requirement: 管理員可下載四種 CSV 範本
系統 SHALL 允許已驗證的管理員透過 `GET /api/admin/import/template/{type}` 下載 employees、departments、suppliers、menus 四種類型的 CSV 範本。每份範本 MUST 只包含對應資料類型的正確表頭，且第一行 MUST 為表頭列。

#### Scenario: 下載員工 CSV 範本
- **WHEN** 管理員呼叫 `GET /api/admin/import/template/employees`
- **THEN** 系統回傳內容為 `username,name,email,department_id` 的 CSV 範本

#### Scenario: 下載菜單 CSV 範本
- **WHEN** 管理員呼叫 `GET /api/admin/import/template/menus`
- **THEN** 系統回傳內容為 `supplier_id,name,category,description,price,valid_from,valid_to` 的 CSV 範本

### Requirement: 管理員可依資料類型批次匯入 CSV
系統 SHALL 允許已驗證的管理員透過 `POST /api/admin/import/employees`、`/departments`、`/suppliers`、`/menus` 上傳對應 CSV。系統 MUST 驗證 UTF-8 編碼、表頭完全一致、必填欄位完整、與既有資料衝突，以及同批次內重複資料。

#### Scenario: 管理員成功匯入部門 CSV
- **WHEN** 管理員上傳表頭正確且資料合法的 departments CSV
- **THEN** 系統建立部門資料並回傳本次成功匯入的部門清單

#### Scenario: 管理員上傳錯誤表頭的 CSV
- **WHEN** 管理員上傳的 CSV 第一行表頭與規格不一致
- **THEN** 系統拒絕匯入並回傳 `400`

### Requirement: 批次匯入必須以 5000 筆 transaction 為單位
系統 SHALL 以每 5000 筆資料為一個 transaction 批次處理 CSV 匯入。同一批次內若任一資料列驗證失敗或寫入失敗，系統 MUST rollback 該批次，並以 `422` 回傳失敗行號與原因；已在前一批次成功提交的資料 MAY 保留。

#### Scenario: 第二批次發生驗證失敗
- **WHEN** 管理員上傳超過 5000 筆資料，且第二批次某一列驗證失敗
- **THEN** 系統 rollback 第二批次，保留第一批次已成功寫入資料，並回傳失敗行號與修正提示

#### Scenario: 同批次資料重複導致 rollback
- **WHEN** 同一批次內出現重複的唯一鍵資料
- **THEN** 系統 rollback 該批次並回傳對應 CSV 行號與錯誤原因
