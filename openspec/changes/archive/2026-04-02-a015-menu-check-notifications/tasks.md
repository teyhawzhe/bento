## 1. OpenSpec Alignment

- [x] 1.1 新增 `menu-check-notification` 主規格，定義 A015 每日檢查、Email 提醒、即時查詢與 dismiss 契約
- [x] 1.2 更新 `error-email-settings`，補上 A015 菜單缺漏提醒共用錯誤通知信箱的用途
- [x] 1.3 更新 `frontend-tab-navigation-and-message-box`，加入登入後導向菜單設定頁、彈窗與頂部警示規則

## 2. Backend Menu Check Notification

- [x] 2.1 建立 `menu_notification_log`、`menu_notification_dismiss` 對應的 migration、domain model 與 repository
- [x] 2.2 建立 A015 缺漏日期計算 service，串接 `work_calendar`、menu 資料與錯誤通知信箱來源
- [x] 2.3 新增 `A015Controller` 與 DTO record，實作 `GET /api/admin/notifications/menu-check` 與 `POST /api/admin/notifications/menu-check/dismiss`
- [x] 2.4 新增每日 8:00 排程，對同日缺漏菜單只發送一封 Email，並記錄通知結果與 exception log
- [x] 2.5 補上管理員權限、通知日誌/dismiss 邏輯與 controller/service/repository 測試

## 3. Frontend Reminder Flow

- [x] 3.1 在管理員登入後加入 A015 即時檢查流程，取得缺漏日期清單
- [x] 3.2 若存在缺漏日期且當日未 dismiss，自動切換到既有菜單設定頁
- [x] 3.3 顯示列出缺漏日期的彈出視窗與頂部警示橫幅
- [x] 3.4 實作 dismiss 流程，呼叫 API 後在同日內不再顯示提醒
- [x] 3.5 確保管理員補完菜單後重新進入系統時，已 dismiss 狀態可正確抑制重複提醒

## 4. Verification

- [x] 4.1 驗證每日排程在存在缺漏菜單日期時只發送一封提醒信，並寫入通知日誌
- [x] 4.2 驗證管理員登入後可看到缺漏日期提醒、彈窗、頂部橫幅與自動導頁
- [x] 4.3 驗證 dismiss 後同日不再重複顯示提醒
- [x] 4.4 驗證既有菜單設定、錯誤通知信箱與管理員其他主 TAB 功能不因 A015 產生回歸
