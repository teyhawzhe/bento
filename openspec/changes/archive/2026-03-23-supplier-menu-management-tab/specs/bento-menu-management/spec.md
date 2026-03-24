## ADDED Requirements

### Requirement: 新建菜單 SHALL 自動帶入所選供應商 id
菜單建立流程 SHALL 使用建立菜單 tab 目前選取的供應商作為所屬供應商，並在送出新建菜單請求時自動帶入該供應商 id，而非要求使用者手動輸入 supplier id。

#### Scenario: 選定供應商後建立菜單
- **WHEN** 管理者已在建立菜單 tab 選擇供應商並送出新建菜單
- **THEN** 系統以目前選取的供應商 id 作為新建菜單 payload 的 supplier id

#### Scenario: 切換供應商後建立菜單
- **WHEN** 管理者在建立菜單 tab 切換到另一個供應商後送出新建菜單
- **THEN** 系統以切換後的供應商 id 作為新建菜單 payload 的 supplier id

### Requirement: 菜單清單 SHALL 依所選供應商過濾
建立菜單 tab 內的菜單清單 SHALL 依目前下拉式選項所選的供應商顯示對應菜單，避免不同供應商的菜單混雜顯示。

#### Scenario: 已選擇供應商時查看菜單清單
- **WHEN** 管理者在建立菜單 tab 選擇某一個供應商
- **THEN** 系統只顯示該供應商的菜單清單

#### Scenario: 尚未選擇供應商時查看菜單清單
- **WHEN** 管理者尚未在建立菜單 tab 選擇供應商
- **THEN** 系統顯示空狀態或提示，而不顯示混合的全量菜單清單
