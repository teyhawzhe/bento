## 1. 資料模型與 A005 整合

- [x] 1.1 建立 `report_recipient_emails` 資料表、domain model、DTO 與 repository 介面
- [x] 1.2 實作 A008 報表收件者 provider，讓 A005 月結報表流程讀取正式設定清單

## 2. 後端設定 API

- [x] 2.1 實作 `GET /api/settings/report-emails`，回傳收件信箱清單並驗證管理員權限
- [x] 2.2 實作 `POST /api/settings/report-emails`，補上 Email 格式與重複值驗證
- [x] 2.3 實作 `DELETE /api/settings/report-emails/{id}`，處理刪除成功與不存在資料情境

## 3. 前端系統設定頁

- [x] 3.1 在管理員設定頁新增月結報表收件信箱區塊與清單展示
- [x] 3.2 新增報表收件信箱建立與刪除操作流程
- [x] 3.3 顯示空清單提示、建立成功訊息與欄位驗證錯誤

## 4. 驗證

- [x] 4.1 驗證查詢、新增、刪除 API 與非管理員拒絕情境
- [x] 4.2 驗證重複 Email、格式錯誤與刪除不存在資料的錯誤處理
- [x] 4.3 驗證 A005 會讀取 A008 收件清單，以及前端設定頁操作流程
