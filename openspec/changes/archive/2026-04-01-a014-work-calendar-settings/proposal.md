## Why

UML 的 A014 已定義管理員需要維護上班日行事曆，作為後續訂餐與管理流程的日期基礎。但目前系統尚未提供行事曆查詢、逐月編輯、整年預產與 CSV 匯入能力，也沒有對應的管理頁面承接這項需求。

這次 change 要把 A014 正式納入現有產品結構，補齊管理員行事曆設定的 API、前端操作流程與規格邊界，讓工作日資料可以被一致地建立、檢視與維護。

## What Changes

- 新增管理員上班日行事曆設定能力，支援依年/月查詢行事曆、唯讀顯示非上班日標示，以及編輯模式下逐日切換上班日狀態。
- 新增整年行事曆預產流程，預設以週六、週日為非上班日，且執行前需二次確認，確認後覆蓋指定年份資料。
- 新增 CSV 匯入流程，支援 `yyyymmdd,y/n` 格式檔案上傳、預覽與確認後匯入。
- 更新管理員前端導覽規格，明確定義 A014 行事曆設定頁作為獨立主 TAB，並規範切換月份時的未儲存提醒。

## Capabilities

### New Capabilities
- `work-calendar-management`: 定義 A014 管理員行事曆查詢、編輯、整年預產與 CSV 預覽匯入契約。

### Modified Capabilities
- `frontend-tab-navigation-and-message-box`: 管理員主導覽需新增「行事曆設定」主 TAB，並支援行事曆頁的單頁切換與未儲存提醒流程。

## Impact

- Affected code: 管理員行事曆設定頁 UI、前端 API client、後端 calendar controller/service/repository、CSV 解析與匯入流程。
- API: 新增或落實 `/api/admin/calendar`、`/api/admin/calendar/generate`、`/api/admin/calendar/import`。
- Data: 需維護工作日資料結構，至少包含 `date` 與 `is_workday` 欄位。
- Specs: 需新增行事曆設定主規格，並同步更新前端主 TAB 導覽規格。
