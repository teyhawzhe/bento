## MODIFIED Requirements

### Requirement: 管理員可透過 CSV 批次建立菜單
系統 SHALL 允許已驗證的管理員透過 `GET /api/admin/import/template/menus` 下載菜單範本，並透過 `POST /api/admin/import/menus` 批次匯入菜單資料。菜單 CSV 表頭 MUST 為 `supplier_id,name,category,description,price,valid_from,valid_to`；同一 `supplier_id + name` 組合 MUST 不得與既有資料或同批次資料重複。

#### Scenario: 成功匯入菜單 CSV
- **WHEN** 管理員上傳合法的 menus CSV
- **THEN** 系統建立菜單資料並回傳本次成功匯入的菜單清單

#### Scenario: 菜單供應商不存在時匯入失敗
- **WHEN** 管理員上傳的 CSV 某列 `supplier_id` 不存在
- **THEN** 系統 rollback 該批次並回傳該列失敗原因
