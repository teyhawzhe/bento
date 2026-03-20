## ADDED Requirements

### Requirement: 員工登入與登出
系統 SHALL 提供專屬的員工登入入口 `/login`，並透過 `POST /api/auth/login` 驗證員工帳號密碼。當驗證成功時，系統 SHALL 發放 `role=employee` 的 JWT。系統 SHALL 提供登出動作，在前端清除目前的登入狀態，並讓使用者返回登入頁。

#### Scenario: 啟用中的員工登入成功
- **WHEN** 員工從員工入口送出正確的帳號與密碼
- **THEN** 系統回傳 `role=employee` 的 JWT，且介面導向登入後頁面

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
系統 SHALL 提供專屬的管理員登入入口 `/admin/login`，並透過 `POST /api/admin/auth/login` 驗證管理員憑證。管理員登入成功後，系統 SHALL 發放 `role=admin` 的 JWT。當登入入口與帳號身分不一致時，系統 MUST 拒絕請求。

#### Scenario: 管理員登入成功
- **WHEN** 啟用中的管理員從管理員入口送出正確憑證
- **THEN** 系統回傳 `role=admin` 的 JWT，且介面導向管理後台

#### Scenario: 一般員工誤用管理員入口
- **WHEN** 非管理員員工透過 `POST /api/admin/auth/login` 送出有效員工憑證
- **THEN** 系統拒絕請求並回傳角色不符錯誤

#### Scenario: 管理員誤用員工入口
- **WHEN** 管理員透過 `POST /api/auth/login` 送出有效憑證
- **THEN** 系統拒絕請求並提示應使用管理員入口

### Requirement: 忘記密碼會寄送臨時密碼
系統 SHALL 允許員工透過 `POST /api/auth/forgot-password` 與員工 Email 發起忘記密碼流程。若 Email 存在，系統 SHALL 產生臨時密碼、更新密碼雜湊，並將臨時密碼寄送至該員工信箱；若 Email 不存在，系統 MUST 回傳錯誤訊息。

#### Scenario: 忘記密碼成功
- **WHEN** 員工送出存在於系統中的 Email
- **THEN** 系統產生臨時密碼、更新密碼雜湊、寄出通知信，並回傳成功訊息

#### Scenario: 查無 Email 時忘記密碼失敗
- **WHEN** 員工送出不存在的 Email
- **THEN** 系統拒絕請求並顯示查無此 Email

### Requirement: 員工登入後可修改密碼
系統 SHALL 允許已登入員工透過 `PATCH /api/auth/change-password` 修改目前密碼。系統 MUST 要求舊密碼、驗證新密碼格式為 8 到 16 碼且同時包含英文大小寫，並僅在舊密碼正確時更新密碼雜湊。修改成功後，系統 SHALL 要求員工重新登入。

#### Scenario: 修改密碼成功
- **WHEN** 已登入員工送出正確的舊密碼與符合規範的新密碼
- **THEN** 系統更新密碼雜湊、回傳成功，且前端清除登入狀態並導回登入頁

#### Scenario: 新密碼格式不符
- **WHEN** 已登入員工送出的新密碼不符合密碼規則
- **THEN** 系統拒絕請求並顯示密碼格式錯誤

#### Scenario: 舊密碼錯誤
- **WHEN** 已登入員工送出錯誤的舊密碼
- **THEN** 系統拒絕請求並顯示舊密碼錯誤

### Requirement: 受保護 API 必須驗證 JWT 角色權限
系統 MUST 驗證受保護 API 請求中的 JWT，並依 `role` 欄位執行授權判斷。當請求缺少有效 Bearer Token 時，系統 SHALL 拒絕請求；當角色與目標 API 不符時，系統 SHALL 回傳禁止存取。

#### Scenario: 缺少 Token 會被拒絕
- **WHEN** 用戶端呼叫受保護 API 時未提供有效 Bearer Token
- **THEN** 系統以未授權回應拒絕請求

#### Scenario: 角色不符會被拒絕
- **WHEN** 用戶端以 `role=employee` 的 Token 呼叫管理員限定 API
- **THEN** 系統以禁止存取回應拒絕請求
