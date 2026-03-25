## MODIFIED Requirements

### Requirement: 管理員可透過 CSV 批次建立供應商
系統 SHALL 允許已驗證的管理員透過 `GET /api/admin/import/template/suppliers` 下載供應商範本，並透過 `POST /api/admin/import/suppliers` 批次匯入供應商資料。供應商 CSV 表頭 MUST 為 `name,email,phone,contact_person,business_registration_no`；`business_registration_no` MUST 在既有資料與同批次內保持唯一。

#### Scenario: 成功匯入供應商 CSV
- **WHEN** 管理員上傳合法的 suppliers CSV
- **THEN** 系統建立供應商資料並回傳本次成功匯入的供應商清單

#### Scenario: 匯入重複統編
- **WHEN** 管理員上傳的 CSV 某列 `business_registration_no` 已存在或同批次內重複
- **THEN** 系統 rollback 該批次並回傳重複錯誤
