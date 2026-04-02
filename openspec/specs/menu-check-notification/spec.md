## Purpose
定義 A015 菜單缺漏檢查、每日提醒信、管理員即時提醒查詢與 dismiss 流程。

## Requirements
### Requirement: 系統必須每日檢查未來一個月上班日的菜單缺漏
系統 SHALL 在每日固定時間檢查未來一個月內的上班日是否存在未設定菜單的日期。檢查邏輯 MUST 以 A014 的 `work_calendar` 中 `is_workday=true` 日期為基礎，再比對各日期是否至少有一筆有效菜單覆蓋。

#### Scenario: 每日排程找出缺漏日期
- **WHEN** 系統執行 A015 每日檢查排程
- **THEN** 系統回傳未來一個月內所有未設定菜單的上班日日期清單

### Requirement: 系統對同一日的缺漏菜單只發送一封提醒信
系統 SHALL 在檢查出缺漏菜單日期時，將提醒 Email 發送到既有錯誤通知信箱清單，且同一天 MUST 只發送一封提醒信。系統 MUST 將當日通知結果寫入 `menu_notification_log`，至少包含 `notify_date`、`missing_from`、`missing_to` 與 `status`。

#### Scenario: 當日首次檢查且存在缺漏日期
- **WHEN** 系統於某日首次檢查出未設定菜單的上班日
- **THEN** 系統發送一封列出缺漏日期的 Email，並寫入成功或失敗的通知日誌

#### Scenario: 同一日再次檢查
- **WHEN** 系統在同一天再次執行檢查且仍存在缺漏日期
- **THEN** 系統不得重複發送第二封提醒信

### Requirement: 管理員登入後可即時取得菜單缺漏提醒
系統 SHALL 提供 `GET /api/admin/notifications/menu-check`，讓已驗證的管理員在登入後即時查詢未來一個月未設定菜單的上班日。若當日已 dismiss，系統 MUST 回傳空缺漏清單；若當日未 dismiss 且存在缺漏，系統 MUST 以 `status=success` 回傳 `missing_dates`。

#### Scenario: 管理員登入後看到缺漏日期
- **WHEN** 管理員呼叫 `GET /api/admin/notifications/menu-check` 且存在未設定菜單日期，且當日尚未 dismiss
- **THEN** 系統以 `status=success` 回傳 `missing_dates`

#### Scenario: 當日已 dismiss 不再提醒
- **WHEN** 管理員呼叫 `GET /api/admin/notifications/menu-check` 且當日已標記 dismiss
- **THEN** 系統回傳空的 `missing_dates`

### Requirement: 管理員可標記當日提醒為已處理
系統 SHALL 提供 `POST /api/admin/notifications/menu-check/dismiss`，讓管理員將當日菜單缺漏提醒標記為已處理。系統 MUST 將當日日期寫入 `menu_notification_dismiss`，使同一天後續登入時不再顯示提醒。

#### Scenario: 管理員 dismiss 當日提醒
- **WHEN** 管理員成功呼叫 `POST /api/admin/notifications/menu-check/dismiss`
- **THEN** 系統記錄當日 dismiss 狀態，並回傳成功結果
