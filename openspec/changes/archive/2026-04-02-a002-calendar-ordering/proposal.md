## Why

最新版 `../uml/input/A002.md` 已把員工訂餐主流程改寫為「以行事曆視圖瀏覽整月訂餐狀態，並透過日期格互動完成訂餐、修改與取消」。目前前端仍以「訂餐日期下拉 + 便當下拉 + 送出按鈕」為主，雖然沿用了部分 A002 截止規則，但互動模型已與現行需求來源分岐，也尚未提供員工端專用的上班日行事曆查詢 API。

## What Changes

- 將員工「訂便當」主流程改為月行事曆視圖，取代目前的訂餐日期下拉方式。
- 新增員工端行事曆讀取能力，提供 `GET /api/calendar` 讓前端載入指定年月的上班日資料，並以 A014 的 `work_calendar` 為來源。
- 明確定義員工端行事曆日期狀態：非上班日不可互動、上班日無訂單可點擊訂餐、上班日已有訂單可點擊管理。
- 明確定義行事曆互動行為：點擊空白可訂日期開啟訂餐視窗；點擊已有訂單日期開啟管理視窗；修改與取消前需顯示確認提示。
- 保留現有 A002 的核心業務規則，包括星期五 12:00 新增/修改截止、前一日 16:30 取消截止、同日訂單覆蓋與員工端不顯示價格。
- **BREAKING**: 員工端不再以訂餐日期下拉作為主要或必要互動模型，既有下拉式訂餐流程自規格層移除。

## Capabilities

### New Capabilities
- `employee-order-calendar-view`: 定義員工端讀取指定年月上班日資料，以及在月行事曆中顯示日期狀態的契約。

### Modified Capabilities
- `employee-bento-ordering`: 將員工訂餐主流程改為行事曆互動，補上便當選項顯示格式、行事曆訂餐/改單/取消流程與確認提示邊界。
- `frontend-tab-navigation-and-message-box`: 調整員工「訂便當」TAB 的呈現模式，明確要求以行事曆承接主流程，並在日期互動與修改/取消前使用既有 MessageBox。

## Impact

- Affected code: `frontend/src/App.tsx` 的員工訂餐頁、狀態管理、日期互動與彈窗；backend 員工行事曆 controller/service；既有 A002 controller/service 的回傳組合。
- APIs: 新增 `GET /api/calendar`；既有 `GET /api/orders/menu`、`POST /api/orders`、`PATCH /api/orders/{id}`、`DELETE /api/orders/{id}`、`GET /api/orders/me` 需配合行事曆互動流程驗證。
- Dependencies: 員工端行事曆需重用 A014 `work_calendar` 資料；員工訂餐視圖仍需與菜單有效期間、A003 取消/通知截止規則一致。
- UX: 員工端「訂便當」頁從雙下拉表單轉為月行事曆與日期彈窗；「我的訂單」TAB 保留個人訂單清單角色，但不再承擔主要訂餐入口。
