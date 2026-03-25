## MODIFIED Requirements

### Requirement: 管理員可透過 CSV 批次建立部門
系統 SHALL 允許已驗證的管理員透過 `GET /api/admin/import/template/departments` 下載部門範本，並透過 `POST /api/admin/import/departments` 批次匯入部門資料。部門 CSV 表頭 MUST 為 `name`，且 `name` 不得與既有部門或同批次內其他列重複。

#### Scenario: 成功匯入部門 CSV
- **WHEN** 管理員上傳合法的 departments CSV
- **THEN** 系統建立部門資料並回傳本次成功匯入的部門清單

#### Scenario: 匯入重複部門名稱
- **WHEN** 管理員上傳的 CSV 某列部門名稱已存在或同批次內重複
- **THEN** 系統 rollback 該批次並回傳重複錯誤
