## MODIFIED Requirements

### Requirement: 員工登入與登出
系統 SHALL 提供專屬的員工登入入口 `/login`，並透過 `POST /api/auth/login` 驗證員工帳號密碼。當驗證成功時，系統 SHALL 同時發放 `role=employee` 的 access token 與 refresh token，並以 OpenAPI 定義的 `status/data` 格式回傳。系統 SHALL 提供 `POST /api/auth/refresh` 讓員工以有效 refresh token 換發新的 access token 與新的 refresh token，且舊 refresh token MUST 立即作廢。系統 SHALL 提供登出動作，於 `POST /api/auth/logout` 作廢目前 refresh token，並讓前端清除登入狀態後返回登入頁。當員工成功登入後，前端 SHALL 導向 A009 定義的員工 TAB 主頁。

#### Scenario: 啟用中的員工登入成功
- **WHEN** 員工從員工入口送出正確的帳號與密碼
- **THEN** 系統以 `status=success` 回傳 access token 與 refresh token，且介面導向員工 TAB 主頁

#### Scenario: 有效 refresh token 換發成功
- **WHEN** 員工呼叫 `POST /api/auth/refresh` 並提供有效且未作廢的 refresh token
- **THEN** 系統回傳新的 access token 與新的 refresh token，且舊 refresh token 立即失效

#### Scenario: 員工登出會作廢 refresh token
- **WHEN** 已登入的員工選擇登出
- **THEN** 系統作廢目前 refresh token，且前端清除本地登入狀態並回到登入頁

### Requirement: 管理員登入使用獨立入口與角色限制
系統 SHALL 提供專屬的管理員登入入口 `/admin/login`，並透過 `POST /api/admin/auth/login` 驗證管理員憑證。管理員登入成功後，系統 SHALL 同時發放 `role=admin` 的 access token 與 refresh token。系統 SHALL 提供 `POST /api/admin/auth/refresh` 讓管理員刷新 access token，並要求 refresh token 僅能使用一次。當登入入口與帳號身分不一致時，系統 MUST 拒絕請求。當管理員成功登入後，前端 SHALL 導向 A009 定義的管理員 TAB 主頁。

#### Scenario: 管理員登入成功
- **WHEN** 啟用中的管理員從管理員入口送出正確憑證
- **THEN** 系統以 `status=success` 回傳 `role=admin` 的 access token 與 refresh token，且介面導向管理員 TAB 主頁

#### Scenario: 管理員 refresh token 換發成功
- **WHEN** 管理員呼叫 `POST /api/admin/auth/refresh` 並提供有效 refresh token
- **THEN** 系統回傳新的 access token 與 refresh token，且舊 refresh token 作廢

#### Scenario: 一般員工誤用管理員入口
- **WHEN** 非管理員員工透過 `POST /api/admin/auth/login` 送出有效員工憑證
- **THEN** 系統拒絕請求並回傳角色不符錯誤

### Requirement: 員工登入後可修改密碼
系統 SHALL 允許已登入員工透過 `PATCH /api/auth/change-password` 修改目前密碼。系統 MUST 要求舊密碼、驗證新密碼格式為 8 到 16 碼且同時包含英文大小寫，並僅在舊密碼正確時更新密碼雜湊。修改成功後，系統 SHALL 作廢該員工所有未失效 refresh token，並要求員工重新登入。

#### Scenario: 修改密碼成功後所有 refresh token 失效
- **WHEN** 已登入員工送出正確的舊密碼與符合規範的新密碼
- **THEN** 系統更新密碼雜湊、作廢該員工所有 refresh token，並要求重新登入

### Requirement: 認證相關密碼通知必須透過統一 mail delivery configuration 發送
系統 SHALL 讓忘記密碼與臨時密碼通知透過統一 mail delivery configuration 發送。當 `mail mode = mock` 時，系統 MUST 不連線外部 SMTP；當 `mail mode = smtp` 時，系統 MUST 使用配置好的 SMTP 設定執行寄信。

#### Scenario: forgot password 在 mock mode 下執行
- **WHEN** 使用者觸發 forgot password，且系統設定 `mail mode = mock`
- **THEN** 系統完成密碼更新與通知流程，且不連線外部 SMTP

#### Scenario: forgot password 在 smtp mode 下執行
- **WHEN** 使用者觸發 forgot password，且系統設定 `mail mode = smtp`
- **THEN** 系統使用 SMTP 設定寄送臨時密碼通知
