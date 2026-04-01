## 1. OpenSpec Alignment

- [x] 1.1 新增 `work-calendar-management` 主規格，定義 A014 行事曆查詢、編輯、整年預產與 CSV 預覽匯入契約
- [x] 1.2 更新 `frontend-tab-navigation-and-message-box`，加入管理員「行事曆設定」主 TAB 與未儲存提醒規則

## 2. Backend Calendar APIs

- [x] 2.1 建立 `work_calendar` 對應的 domain model、repository 與 migration，至少包含 `date` 與 `is_workday`
- [x] 2.2 新增 `A014Controller`、`WorkCalendarService` 與 DTO record，實作 `GET /api/admin/calendar` 與 `PUT /api/admin/calendar`
- [x] 2.3 實作 `POST /api/admin/calendar/generate`，以指定年份覆蓋產生整年資料，預設週六、週日為非上班日
- [x] 2.4 實作 `POST /api/admin/calendar/import`，支援 CSV 解析預覽、確認匯入、錯誤處理與 exception log
- [x] 2.5 補上管理員權限、參數驗證與 controller/service/repository 測試

## 3. Frontend Calendar UI

- [x] 3.1 在管理員主導覽加入「行事曆設定」TAB，並建立行事曆設定頁面
- [x] 3.2 實作年月切換、唯讀行事曆顯示與非上班日紅色標註
- [x] 3.3 實作編輯模式、日期切換、未儲存變更提示與更新送出流程
- [x] 3.4 實作預產整年確認提示與成功後重載行事曆
- [x] 3.5 實作 CSV 上傳、預覽確認、正式匯入與結果提示

## 4. Verification

- [x] 4.1 驗證管理員登入後可看到並切換到「行事曆設定」主 TAB
- [x] 4.2 驗證指定年月查詢、逐日編輯與更新後資料正確保存
- [x] 4.3 驗證切換月份時未儲存提醒、預產整年覆蓋確認與 CSV 預覽確認流程
- [x] 4.4 驗證既有管理員主 TAB 與其他設定頁功能不因 A014 產生回歸
