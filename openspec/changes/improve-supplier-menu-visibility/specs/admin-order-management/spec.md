## ADDED Requirements

### Requirement: 管理員可以查詢供應商清單及其對應的便當選項
系統 SHALL 允許已驗證的管理員查詢供應商清單及其對應的便當選項，作為 A006 管理員查詢流程的一部分。系統 MUST 提供 `GET /api/admin/suppliers`，並回傳供應商清單；每筆供應商資料 MUST 同步包含其對應的便當選項，讓管理頁可直接顯示供應商與便當的關聯。

#### Scenario: 管理員成功查詢供應商與便當選項
- **WHEN** 管理員呼叫 `GET /api/admin/suppliers`
- **THEN** 系統回傳供應商清單，且每筆供應商資料都包含其對應的便當選項

#### Scenario: 供應商沒有任何便當選項時仍可查詢
- **WHEN** 管理員呼叫 `GET /api/admin/suppliers`，且其中某供應商目前沒有關聯便當選項
- **THEN** 系統仍回傳該供應商資料，並以空集合表示其目前沒有便當選項
