## ADDED Requirements

### Requirement: 管理員菜單清單查詢需對齊最新版 A002 範圍
系統 SHALL 提供管理員查詢菜單清單的能力，以支援建立後檢視與後續編輯流程。最新版 A002 僅要求 `GET /api/menus` 回傳菜單清單本身，不再把「含歷史菜單」或 `include_history` 查詢模式視為 A002 的必要需求。

#### Scenario: 管理員可查詢菜單清單
- **WHEN** 管理員成功呼叫 `GET /api/menus`
- **THEN** 系統以 `status=success` 與 `data` 陣列回傳可供管理員檢視與編輯的菜單清單

#### Scenario: A002 驗收不再依賴歷史菜單切換
- **WHEN** 團隊依最新版 A002 驗證管理員菜單查詢流程
- **THEN** 驗收重點僅包含菜單清單查詢與編輯流程本身，而不要求額外的歷史菜單切換參數或畫面開關
