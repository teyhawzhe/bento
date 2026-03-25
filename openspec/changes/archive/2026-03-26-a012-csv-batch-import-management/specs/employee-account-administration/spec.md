## MODIFIED Requirements

### Requirement: 員工批次匯入必須支援 A012 CSV 契約
系統 SHALL 支援管理員透過 `POST /api/admin/import/employees` 以上傳員工 CSV 進行批次匯入。員工 CSV 的表頭 MUST 為 `username,name,email,department_id`；系統建立的員工 MUST 一律為 `is_admin = false`、`is_active = true`，且 MUST 驗證 `username`、`email` 不得與既有資料或同批次內其他列重複。

#### Scenario: 使用 department_id 匯入員工
- **WHEN** 管理員上傳表頭為 `username,name,email,department_id` 的合法員工 CSV
- **THEN** 系統建立員工並回傳匯入成功員工清單，且每筆結果包含 `department_id`、`is_admin=false`、`is_active=true`

#### Scenario: 員工 CSV 指定重複 username
- **WHEN** 管理員上傳的員工 CSV 中某一列 `username` 與既有資料或同批次資料重複
- **THEN** 系統 rollback 該批次並回傳失敗行號與重複原因
