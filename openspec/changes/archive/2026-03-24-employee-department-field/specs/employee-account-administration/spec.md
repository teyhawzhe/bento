## MODIFIED Requirements

### Requirement: 管理員可以查詢員工清單
系統 SHALL 允許已驗證的管理員透過 `GET /api/admin/employees` 取得全部員工帳號。回傳資料 SHALL 包含員工識別資訊、Email、部門摘要資訊、管理員身分、啟用狀態與稽核時間，讓管理員可以決定是否停用、啟用或重設密碼。部門摘要資訊 SHALL 至少包含 `department.id` 與 `department.name`。

#### Scenario: 管理員成功取得員工清單
- **WHEN** 已登入的管理員呼叫 `GET /api/admin/employees`
- **THEN** 系統回傳包含帳號狀態與 `department.name` 的員工清單

### Requirement: 管理員可以新增單一員工帳號
系統 SHALL 允許已驗證的管理員透過 `POST /api/admin/employees` 以 `username`、`name`、`email` 與 `departmentId` 建立單一員工帳號。系統 MUST 確保 `username` 與 `email` 皆唯一，驗證 `departmentId` 對應的部門存在，產生初始密碼、儲存密碼雜湊，並將初始密碼寄送至員工 Email。成功回應 SHALL 包含可供畫面顯示的 `department.name`。

#### Scenario: 單一員工建立成功
- **WHEN** 管理員送出唯一的 `username`、`name`、`email` 與有效的 `departmentId`
- **THEN** 系統建立員工帳號、關聯部門、寄送初始密碼通知，並回傳包含 `department.name` 的成功結果

#### Scenario: 帳號或 Email 重複時建立失敗
- **WHEN** 管理員送出的 `username` 或 `email` 已存在
- **THEN** 系統拒絕請求並回傳唯一性衝突

#### Scenario: 未提供部門時建立失敗
- **WHEN** 管理員建立員工時未提供 `departmentId`
- **THEN** 系統拒絕請求並提示必須指定部門

### Requirement: 管理員可以透過 CSV 匯入員工帳號
系統 SHALL 允許已驗證的管理員透過 `POST /api/admin/employees/import` 上傳 CSV 檔。每一列資料 MUST 包含 `username`、`name`、`email` 與 `department`。系統 MUST 逐列驗證資料、略過不合法列、驗證部門存在、為合法列建立員工帳號與初始密碼、寄送通知信，並回傳成功筆數、失敗筆數與逐列錯誤資訊。成功建立的員工資料 SHALL 可供後續員工清單顯示 `department.name`。

#### Scenario: CSV 匯入部分成功
- **WHEN** 管理員上傳同時包含合法與不合法資料列的 CSV 檔，且合法列都帶有有效部門
- **THEN** 系統建立合法資料列的帳號、略過不合法列，並回傳筆數與逐列錯誤明細

#### Scenario: 空白 CSV 檔無法匯入
- **WHEN** 管理員上傳空白 CSV 檔
- **THEN** 系統拒絕請求並提示必須提供 CSV 檔案

#### Scenario: CSV 缺少 department 欄位時匯入失敗
- **WHEN** 管理員上傳的 CSV 缺少 `department` 欄位，或某一列未提供部門值
- **THEN** 系統拒絕整體匯入或將該列標記失敗，並提示部門欄位為必填
