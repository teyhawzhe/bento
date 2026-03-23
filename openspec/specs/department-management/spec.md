## ADDED Requirements

### Requirement: 管理員可建立部門主檔
系統 SHALL 允許已驗證的管理員建立部門主檔。建立部門時 SHALL 至少包含 `name`，且部門名稱 MUST 唯一。

#### Scenario: 成功建立部門
- **WHEN** 管理員送出有效且未重複的部門名稱
- **THEN** 系統建立部門並回傳部門摘要

#### Scenario: 部門名稱重複
- **WHEN** 管理員送出已存在的部門名稱
- **THEN** 系統拒絕請求並回傳名稱重複錯誤

### Requirement: 管理員可查詢部門清單
系統 SHALL 允許已驗證的管理員查詢部門清單，供後台維護與員工建立流程使用。回傳資料 SHALL 至少包含 `id`、`name` 與啟用狀態。

#### Scenario: 查詢部門清單
- **WHEN** 管理員呼叫部門清單 API
- **THEN** 系統回傳部門清單，且每筆資料包含 `id`、`name` 與 `isActive`

### Requirement: 管理員可修改部門資料
系統 SHALL 允許已驗證的管理員修改部門資料，包括部門名稱與啟用狀態。修改時 MUST 驗證名稱唯一性。

#### Scenario: 成功修改部門
- **WHEN** 管理員更新部門名稱或啟用狀態且資料有效
- **THEN** 系統儲存變更並回傳更新後部門資料

#### Scenario: 修改為重複名稱
- **WHEN** 管理員將部門名稱修改為另一筆已存在的名稱
- **THEN** 系統拒絕請求並回傳名稱重複錯誤

### Requirement: 被員工引用的部門不可刪除或停用
系統 SHALL 允許管理員對未被員工引用的部門執行刪除或停用。若部門仍被任何員工引用，系統 MUST 拒絕刪除或停用操作。

#### Scenario: 停用未被引用的部門
- **WHEN** 管理員停用一個沒有任何員工引用的部門
- **THEN** 系統將該部門標記為停用

#### Scenario: 刪除或停用被引用的部門
- **WHEN** 管理員嘗試刪除或停用一個仍被員工引用的部門
- **THEN** 系統拒絕請求並回傳不可刪除或停用的錯誤

### Requirement: 員工建立流程必須使用有效部門
系統 MUST 提供可供單一新增員工與 CSV 匯入員工使用的有效部門資料來源。系統 MUST 拒絕使用不存在或已停用的部門建立員工。

#### Scenario: 單一新增員工使用有效 departmentId
- **WHEN** 管理員建立員工時提供有效的 `departmentId`
- **THEN** 系統允許建立該員工帳號

#### Scenario: 單一新增員工使用無效 departmentId
- **WHEN** 管理員建立員工時提供不存在或已停用的 `departmentId`
- **THEN** 系統拒絕請求並回傳部門驗證失敗錯誤

#### Scenario: CSV 匯入使用無效 department
- **WHEN** 管理員匯入 CSV 時，某列 `department` 對應不到有效部門
- **THEN** 系統回報該列匯入失敗並附上錯誤原因
