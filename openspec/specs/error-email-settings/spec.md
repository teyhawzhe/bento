## ADDED Requirements

### Requirement: 管理員可以查詢錯誤通知信箱清單
系統 SHALL 允許已驗證的管理員透過 `GET /api/settings/error-emails` 查詢目前設定的錯誤通知信箱清單。回傳資料 SHALL 包含信箱識別、Email、建立者與建立時間。

#### Scenario: 管理員成功查詢錯誤通知信箱清單
- **WHEN** 已登入的管理員查詢錯誤通知信箱清單
- **THEN** 系統回傳目前所有錯誤通知信箱資料

### Requirement: 管理員可以新增錯誤通知信箱
系統 SHALL 允許已驗證的管理員透過 `POST /api/settings/error-emails` 新增錯誤通知信箱。系統 MUST 驗證 Email 格式正確，且相同 Email 不可重複建立。

#### Scenario: 管理員成功新增錯誤通知信箱
- **WHEN** 管理員送出格式正確且尚未存在的 Email
- **THEN** 系統建立錯誤通知信箱資料並回傳新增結果

#### Scenario: Email 格式錯誤時新增失敗
- **WHEN** 管理員送出格式不正確的 Email
- **THEN** 系統拒絕請求並提示 Email 格式錯誤

#### Scenario: 重複 Email 時新增失敗
- **WHEN** 管理員送出已存在於清單中的 Email
- **THEN** 系統拒絕請求並提示該 Email 已存在

### Requirement: 管理員可以刪除錯誤通知信箱
系統 SHALL 允許已驗證的管理員透過 `DELETE /api/settings/error-emails/{id}` 刪除既有錯誤通知信箱。

#### Scenario: 管理員成功刪除錯誤通知信箱
- **WHEN** 管理員刪除既有的錯誤通知信箱
- **THEN** 系統移除該筆資料並回傳成功

#### Scenario: 刪除不存在的錯誤通知信箱時失敗
- **WHEN** 管理員刪除不存在的錯誤通知信箱識別
- **THEN** 系統拒絕請求並回傳查無資料
