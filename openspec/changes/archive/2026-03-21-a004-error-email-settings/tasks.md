## 1. 後端資料模型與規則

- [x] 1.1 建立 `error_notification_emails` 資料表、domain model、DTO 與 repository 介面
- [x] 1.2 實作 Email 格式與唯一性驗證規則，避免重複收件信箱
- [x] 1.3 將錯誤通知信箱讀取能力抽成可供 A003 重用的 provider / service

## 2. 後端 A004 API

- [x] 2.1 實作管理員查詢錯誤通知信箱清單 API `GET /api/settings/error-emails`
- [x] 2.2 實作管理員新增錯誤通知信箱 API `POST /api/settings/error-emails`
- [x] 2.3 實作管理員刪除錯誤通知信箱 API `DELETE /api/settings/error-emails/{id}`
- [x] 2.4 將 A004 API 接入既有 JWT role 驗證、權限檢查與錯誤回應格式

## 3. 整合與前端

- [x] 3.1 將 A003 的錯誤通知 recipients 來源切換為 A004 service，而非固定值或 stub 清單
- [x] 3.2 建立管理員系統設定頁面，支援查詢、新增與刪除錯誤通知信箱
- [x] 3.3 補齊 demo 資料與 README 說明，讓本地可演示 A004 與 A003 的銜接邊界

## 4. 驗證

- [x] 4.1 驗證管理員查詢、新增、刪除錯誤通知信箱的主要情境
- [x] 4.2 驗證重複 Email、格式錯誤與刪除不存在資料等錯誤情境
- [x] 4.3 確認 A004 設定可被 A003 直接讀取，且能延伸到後續更多系統通知設定
