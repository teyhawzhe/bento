## MODIFIED Requirements

### Requirement: 員工登入與登出
系統 SHALL 提供專屬的員工登入入口 `/login`，並透過 `POST /api/auth/login` 驗證員工帳號密碼。當驗證成功時，系統 SHALL 發放 `role=employee` 的 JWT。系統 SHALL 提供登出動作，在前端清除目前的登入狀態，並讓使用者返回登入頁。當員工成功登入後，前端 SHALL 導向 A002 的個人訂餐頁，並顯示目前可訂購菜單或無菜單提示。

#### Scenario: 啟用中的員工登入成功
- **WHEN** 員工從員工入口送出正確的帳號與密碼
- **THEN** 系統回傳 `role=employee` 的 JWT，且介面導向個人訂餐頁

#### Scenario: 查無帳號時登入失敗
- **WHEN** 員工透過 `POST /api/auth/login` 送出不存在的帳號
- **THEN** 系統拒絕請求並顯示查無帳號的錯誤訊息

#### Scenario: 密碼錯誤時登入失敗
- **WHEN** 員工送出存在的帳號但密碼錯誤
- **THEN** 系統拒絕請求並顯示密碼錯誤訊息

#### Scenario: 停用帳號無法登入
- **WHEN** 已停用的員工帳號送出正確憑證
- **THEN** 系統拒絕請求並顯示帳號已停用

#### Scenario: 員工登出會清除登入狀態
- **WHEN** 已登入的員工選擇登出
- **THEN** 系統清除已儲存的 JWT，並回到登入頁

### Requirement: 管理員登入使用獨立入口與角色限制
系統 SHALL 提供專屬的管理員登入入口 `/admin/login`，並透過 `POST /api/admin/auth/login` 驗證管理員憑證。管理員登入成功後，系統 SHALL 發放 `role=admin` 的 JWT。當登入入口與帳號身分不一致時，系統 MUST 拒絕請求。當管理員成功登入後，前端 SHALL 導向 A002 的菜單管理頁。

#### Scenario: 管理員登入成功
- **WHEN** 啟用中的管理員從管理員入口送出正確憑證
- **THEN** 系統回傳 `role=admin` 的 JWT，且介面導向菜單管理頁

#### Scenario: 一般員工誤用管理員入口
- **WHEN** 非管理員員工透過 `POST /api/admin/auth/login` 送出有效員工憑證
- **THEN** 系統拒絕請求並回傳角色不符錯誤

#### Scenario: 管理員誤用員工入口
- **WHEN** 管理員透過 `POST /api/auth/login` 送出有效憑證
- **THEN** 系統拒絕請求並提示應使用管理員入口
