## Context

目前系統尚未提供管理員維護上班日行事曆的能力，但 A014 已在 UML 與 OpenAPI 中定義完整流程：管理員可查詢指定年月行事曆、切換編輯模式逐日調整、預產整年資料，以及透過 CSV 先預覽再確認匯入。這項能力會同時影響管理員前端主導覽、後端 API、資料表與匯入流程，因此需要先明確定義技術邊界。

依 `openspec/config.yaml` 的專案規則，後端 controller 命名需以需求編號為主，因此本需求的 controller 應採 `A014Controller.java`；service 與 repository 則以主表 `work_calendar` 命名，分別對應 `WorkCalendarService` 與 `WorkCalendarRepository`。需求文件也已指向 `../uml/input/A014.md`、`../uml/output/A014/*` 與 `../uml/openapi.yaml` 作為主要契約來源。

## Goals / Non-Goals

**Goals:**
- 提供管理員可在前端主 TAB 進入的行事曆設定頁。
- 支援 `GET /api/admin/calendar`、`PUT /api/admin/calendar`、`POST /api/admin/calendar/generate`、`POST /api/admin/calendar/import` 四組 A014 API。
- 以 `work_calendar` 作為工作日資料來源，支援逐日修改、整年預產與 CSV 預覽匯入。
- 明確定義未儲存月份切換提醒、二次確認與 CSV 預覽確認等互動規則。

**Non-Goals:**
- 不修改 `../uml` 內的需求文件、OpenAPI 或 UML 圖。
- 不在本 change 內擴充國定假日自動匯入、外部行事曆串接或批次排程同步。
- 不新增管理員子角色或額外授權模型，維持現有管理員權限邊界。
- 不要求 CSV 匯入支援多種格式，僅支援 `yyyymmdd,y/n`。

## Decisions

### 1. 使用獨立 `work_calendar` 主表保存每日狀態
- 決策：以 `work_calendar(date PK, is_workday)` 作為唯一資料來源，逐日保存工作日狀態。
- 理由：A014 ER 已明確定義此表，且每日一筆資料可同時支援月份查詢、單筆切換與整年覆蓋。
- 替代方案：僅儲存例外日，未設定視為工作日。未採用，因為會讓查詢與整年預產邏輯變得隱性，也不符合 A014 已定義的資料模型。

### 2. 預產整年採覆蓋寫入，預設週六與週日為非上班日
- 決策：`POST /api/admin/calendar/generate` 以指定年份為範圍，重建該年 1 月 1 日到 12 月 31 日的資料，週六、週日寫入 `is_workday=false`，其餘寫入 `true`。
- 理由：需求已明確要求「確認後整年覆蓋」，且這是建立初始行事曆最快且最可預測的方式。
- 替代方案：只補不存在日期、保留既有人工調整。未採用，因為與「整年覆蓋」語意不符，容易讓使用者誤判結果。

### 3. 月份編輯 API 以 `WorkCalendarDay[]` 全量送出當前異動
- 決策：前端在編輯模式內維護當前月份的暫存狀態，按下「更新」後以 `PUT /api/admin/calendar` 送出 `[{date, is_workday}]` 陣列，由後端逐筆 upsert。
- 理由：這與 OpenAPI 契約一致，前端也能清楚區分「尚未儲存」與「已提交」狀態。
- 替代方案：逐日即時 PATCH。未採用，因為需求明確要求先編輯再按更新，且逐日即時寫入會讓「切換月份前提醒先更新」失去意義。

### 4. CSV 匯入採兩階段同端點流程
- 決策：`POST /api/admin/calendar/import` 以 `multipart/form-data` 接收 `file` 與 `confirm`。`confirm=false` 時僅解析並回傳預覽資料；`confirm=true` 時以同一份上傳檔內容執行批次更新。
- 理由：這與 OpenAPI 已定義的契約一致，也能避免額外引入 preview token 或暫存檔案識別碼。
- 替代方案：預覽先寫入暫存表，再以 token 確認匯入。未採用，因為目前需求規模較小，增加儲存與清理成本。

### 5. 行事曆設定作為管理員獨立主 TAB
- 決策：在現有管理員主導覽新增「行事曆設定」主 TAB，而不是併入「系統設定」或「報表設定」。
- 理由：A014.md 已明確標註此頁位於主 TAB；這項能力也有獨立的瀏覽、編輯、預產與匯入流程，適合作為單獨入口。
- 替代方案：放入系統設定子頁。未採用，因為與需求描述不符，也會讓管理頁資訊架構不一致。

### 6. 錯誤處理與可觀測性集中在 service / controller
- 決策：後端在 CSV 解析失敗、非法日期、參數不合法與匯入例外時，需保留明確錯誤回應，並在 exception 路徑補上 application log。
- 理由：這符合專案規則中「後端需要 LOG 尤其是有 exception」的要求，也有助於匯入問題排查。
- 替代方案：僅回傳通用錯誤訊息、不記錄細節。未採用，因為會降低維運可觀測性。

## Risks / Trade-offs

- [整年覆蓋可能抹除既有人工調整] → 前端必須在預產前顯示明確確認提示，並在文案中說明會覆蓋整年資料。
- [CSV 兩階段匯入要求確認時重新上傳同一份檔案] → 前端需保留使用者選定檔案直到確認送出，避免預覽與匯入內容不一致。
- [月份切換前的未儲存提醒若僅在前端處理，可能因重新整理而失去暫存] → 將「未儲存變更」明確視為前端頁面內狀態，不提供跨重新整理保存。
- [行事曆資料未建立時，前端可能拿不到完整月份資料] → 查詢 API 需定義明確策略，回傳目標月份每一天的資料；若資料表缺漏，可由後端補齊預設值或要求先預產。

## Migration Plan

- 新增 `work-calendar-management` 主規格，定義查詢、編輯、整年預產與 CSV 預覽匯入契約。
- 更新 `frontend-tab-navigation-and-message-box`，加入管理員「行事曆設定」主 TAB 與該頁互動規則。
- 實作 `work_calendar` 資料存取與 A014 controller / service / repository。
- 補上前端行事曆頁、編輯狀態管理、確認對話與 CSV 預覽流程。
- 驗證主 TAB 導覽、API 契約、月份切換提醒與 CSV 預覽確認流程。

## Open Questions

- 查詢尚未建立資料的年份時，後端要直接回傳空陣列，還是依平日預設值補滿整月資料？目前先傾向由實作時定義成一致可預測的回傳策略。
