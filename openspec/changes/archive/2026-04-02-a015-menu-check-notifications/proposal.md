## Why

UML 的 A015 已定義系統需要主動提醒管理員「後一個月內哪些上班日尚未設定菜單」，避免管理端漏設菜單而影響後續訂餐流程。現況系統雖已有 A004 錯誤通知信箱、A014 上班日行事曆與管理員菜單設定頁，但尚未把這三者串成每日通知與登入即時提醒機制。

這次 change 要把 A015 納入既有產品結構，補齊每日排程檢查、提醒日誌、當日 dismiss、登入自動導流與前端警示呈現，讓菜單缺漏可以被及早發現且避免重複提醒。

## What Changes

- 新增 A015 菜單設定提醒能力，支援每日 8:00 排程檢查未來一個月上班日的缺漏菜單，必要時寄送一封提醒 Email 並記錄通知狀態。
- 新增 `GET /api/admin/notifications/menu-check` 與 `POST /api/admin/notifications/menu-check/dismiss` API，支援管理員登入後即時檢查與當日標記已處理。
- 在管理員前端登入後加入菜單缺漏提醒流程：若存在未設定日期且當日尚未 dismiss，自動導向菜單設定頁並顯示彈出視窗與頂部警示橫幅。
- 更新錯誤通知信箱與前端導覽相關規格，明確說明 A004 錯誤通知收件來源也承接 A015 的提醒用途。

## Capabilities

### New Capabilities
- `menu-check-notification`: 定義 A015 每日檢查、Email 提醒、即時查詢與 dismiss 契約。

### Modified Capabilities
- `error-email-settings`: 錯誤通知信箱的用途需擴充為 A003 供應商通知異常與 A015 菜單缺漏提醒共用。
- `frontend-tab-navigation-and-message-box`: 管理員登入後若存在未設定菜單的上班日，需自動導向菜單設定頁並顯示彈窗與頂部警示。

## Impact

- Affected code: 後端排程、通知 service / repository、錯誤通知收件來源、管理員登入後前端初始化流程、供應商/菜單設定頁提醒 UI。
- API: 新增 `/api/admin/notifications/menu-check` 與 `/api/admin/notifications/menu-check/dismiss`。
- Data: 需新增 A015 提醒日誌與 dismiss 記錄資料結構，以避免當日重複發送或重複提醒。
- Integrations: 串接 A014 `work_calendar`、既有 menu 資料、A004 錯誤通知信箱與現有 mail delivery configuration。
