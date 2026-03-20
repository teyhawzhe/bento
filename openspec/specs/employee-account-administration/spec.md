## ADDED Requirements

### Requirement: 管理員可以查詢員工清單
系統 SHALL 允許已驗證的管理員透過 `GET /api/admin/employees` 取得全部員工帳號。回傳資料 SHALL 包含員工識別資訊、Email、管理員身分、啟用狀態與稽核時間，讓管理員可以決定是否停用、啟用或重設密碼。

#### Scenario: 管理員成功取得員工清單
- **WHEN** 已登入的管理員呼叫 `GET /api/admin/employees`
- **THEN** 系統回傳包含帳號狀態資訊的員工清單

### Requirement: 管理員可以新增單一員工帳號
系統 SHALL 允許已驗證的管理員透過 `POST /api/admin/employees` 以 `username`、`name` 與 `email` 建立單一員工帳號。系統 MUST 確保 `username` 與 `email` 皆唯一，產生初始密碼、儲存密碼雜湊，並將初始密碼寄送至員工 Email。

#### Scenario: 單一員工建立成功
- **WHEN** 管理員送出唯一的 `username`、`name` 與 `email`
- **THEN** 系統建立員工帳號、寄送初始密碼通知，並回傳成功結果

#### Scenario: 帳號或 Email 重複時建立失敗
- **WHEN** 管理員送出的 `username` 或 `email` 已存在
- **THEN** 系統拒絕請求並回傳唯一性衝突

### Requirement: 管理員可以透過 CSV 匯入員工帳號
系統 SHALL 允許已驗證的管理員透過 `POST /api/admin/employees/import` 上傳 CSV 檔。每一列資料 MUST 包含 `username`、`name` 與 `email`。系統 MUST 逐列驗證資料、略過不合法列、為合法列建立員工帳號與初始密碼、寄送通知信，並回傳成功筆數、失敗筆數與逐列錯誤資訊。

#### Scenario: CSV 匯入部分成功
- **WHEN** 管理員上傳同時包含合法與不合法資料列的 CSV 檔
- **THEN** 系統建立合法資料列的帳號、略過不合法列，並回傳筆數與逐列錯誤明細

#### Scenario: 空白 CSV 檔無法匯入
- **WHEN** 管理員上傳空白 CSV 檔
- **THEN** 系統拒絕請求並提示必須提供 CSV 檔案

### Requirement: 管理員可以停用或啟用員工帳號
系統 SHALL 允許已驗證的管理員透過 `PATCH /api/admin/employees/{id}/status` 變更員工帳號的啟用狀態。停用操作 MUST 將員工標記為未啟用，啟用操作 MUST 將員工標記為啟用。

#### Scenario: 管理員停用員工帳號
- **WHEN** 管理員對既有員工送出 `is_active=false`
- **THEN** 系統將員工帳號更新為停用，並回傳更新後狀態

#### Scenario: 管理員啟用員工帳號
- **WHEN** 管理員對既有停用員工送出 `is_active=true`
- **THEN** 系統將員工帳號更新為啟用，並回傳更新後狀態

### Requirement: 管理員可以重設員工密碼
系統 SHALL 允許已驗證的管理員透過 `PATCH /api/admin/employees/{id}/reset-password` 重設員工密碼。新密碼 MUST 符合與員工修改密碼相同的密碼規則。重設成功後，系統 SHALL 更新密碼雜湊並寄送通知信給員工。

#### Scenario: 重設密碼成功
- **WHEN** 管理員為既有員工送出符合規則的新密碼
- **THEN** 系統更新員工密碼雜湊、寄送重設通知信，並回傳成功

#### Scenario: 重設密碼格式不符
- **WHEN** 管理員送出的新密碼不符合密碼規則
- **THEN** 系統拒絕請求並顯示密碼格式錯誤
