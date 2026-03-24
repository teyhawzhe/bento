## MODIFIED Requirements

### Requirement: 管理員可查詢員工帳號清單
系統 SHALL 允許已驗證的管理員透過 `GET /api/admin/employees` 取得全部員工帳號。回傳資料 SHALL 包含員工識別資訊、Email、部門摘要、管理員身分、啟用狀態與稽核時間，讓管理員可以決定是否修改、停用、啟用或重設密碼。

#### Scenario: 管理員查詢員工清單
- **WHEN** 已登入的管理員呼叫 `GET /api/admin/employees`
- **THEN** 系統回傳每位員工資料，且每筆資料包含 `department.name`

### Requirement: 管理員可建立單一員工帳號
系統 SHALL 允許已驗證的管理員透過 `POST /api/admin/employees` 以 `username`、`name`、`email` 與 `departmentId` 建立單一員工帳號。系統 MUST 確保 `username` 與 `email` 皆唯一，並驗證 `departmentId` 對應到有效部門。建立成功後，系統 SHALL 產生初始密碼、儲存密碼雜湊、寄送初始密碼至員工 Email，並在回應中包含 `department.name`。

#### Scenario: 成功建立員工帳號
- **WHEN** 管理員送出有效的 `username`、`name`、`email` 與 `departmentId`
- **THEN** 系統建立員工帳號、寄送初始密碼通知，並回傳包含 `department.name` 的員工摘要

#### Scenario: 帳號或 Email 已存在
- **WHEN** 管理員建立員工時提供已存在的 `username` 或 `email`
- **THEN** 系統拒絕請求並回傳唯一性衝突錯誤

#### Scenario: 部門不存在或不可用
- **WHEN** 管理員建立員工時提供不存在或已停用的 `departmentId`
- **THEN** 系統拒絕請求並回傳部門驗證失敗錯誤

### Requirement: 管理員可使用 CSV 匯入員工帳號
系統 SHALL 允許已驗證的管理員透過 `POST /api/admin/employees/import` 上傳 CSV 檔。每一列資料 MUST 包含 `username`、`name`、`email` 與 `department`。系統 MUST 逐列驗證資料、略過不合法列、為合法列建立員工帳號與初始密碼、寄送通知信，並回傳成功筆數、失敗筆數與逐列錯誤資訊。成功建立的員工資料 SHALL 可在後續員工清單中顯示 `department.name`。

#### Scenario: CSV 匯入成功與失敗並存
- **WHEN** 管理員上傳包含合法與不合法列的 CSV 檔
- **THEN** 系統建立所有合法列員工帳號，略過不合法列，並回傳成功筆數、失敗筆數與逐列錯誤原因

#### Scenario: CSV 檔案不存在
- **WHEN** 管理員未上傳 CSV 檔案即呼叫匯入 API
- **THEN** 系統拒絕請求並回傳缺少 CSV 檔案錯誤

#### Scenario: CSV 缺少 department 欄位或值
- **WHEN** 管理員上傳的 CSV 缺少 `department` 欄位，或某列 `department` 為空值
- **THEN** 系統回報該列驗證失敗，並在逐列錯誤資訊中指出 `department` 為必填

### Requirement: 管理員可修改員工資料
系統 SHALL 允許已驗證的管理員透過 `PATCH /api/admin/employees/{id}` 修改既有員工資料。系統 MUST 僅允許更新 `username`、`name`、`email`、`departmentId` 與 `isAdmin`，且 `id` MUST 維持唯讀不可修改。更新時系統 MUST 驗證 `username` 與 `email` 不可與其他員工重複，並驗證 `departmentId` 對應到有效部門。

#### Scenario: 成功修改員工資料
- **WHEN** 管理員對既有員工送出有效的 `username`、`name`、`email`、`departmentId` 與 `isAdmin`
- **THEN** 系統更新該員工資料並回傳更新後員工摘要

#### Scenario: 修改後帳號或 Email 與其他員工衝突
- **WHEN** 管理員更新員工資料時，送出的 `username` 或 `email` 已被其他員工使用
- **THEN** 系統拒絕請求並回傳唯一性衝突錯誤

#### Scenario: 嘗試修改無效部門
- **WHEN** 管理員更新員工資料時，送出的 `departmentId` 不存在或已停用
- **THEN** 系統拒絕請求並回傳部門驗證失敗錯誤

### Requirement: 管理員可變更員工帳號啟用狀態
系統 SHALL 允許已驗證的管理員透過 `PATCH /api/admin/employees/{id}/status` 變更員工帳號的啟用狀態。停用操作 MUST 將員工標記為未啟用；啟用操作 MUST 將員工標記為啟用。

#### Scenario: 停用員工帳號
- **WHEN** 管理員送出 `is_active=false`
- **THEN** 系統將該員工標記為未啟用並回傳更新後資料

#### Scenario: 啟用員工帳號
- **WHEN** 管理員送出 `is_active=true`
- **THEN** 系統將該員工標記為啟用並回傳更新後資料

### Requirement: 管理員可重設員工密碼
系統 SHALL 允許已驗證的管理員透過 `PATCH /api/admin/employees/{id}/reset-password` 重設員工密碼。新密碼 MUST 符合與員工修改密碼相同的密碼規則。重設成功後，系統 SHALL 更新密碼雜湊並寄送通知信給員工。

#### Scenario: 成功重設員工密碼
- **WHEN** 管理員提供符合規則的新密碼
- **THEN** 系統更新員工密碼雜湊並寄送通知信給該員工

#### Scenario: 新密碼格式不符
- **WHEN** 管理員提供不符合規則的新密碼
- **THEN** 系統拒絕請求並回傳密碼格式錯誤
