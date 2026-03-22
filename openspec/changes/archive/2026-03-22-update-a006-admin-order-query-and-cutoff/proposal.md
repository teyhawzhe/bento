## Why

A006 最新文件已把管理員代訂截止時間改為當日 16:30，並將管理員訂單查詢升級為日期區間查詢且預設今天～今天。現在主規格、後端與前端仍停留在舊版的單日查詢與 17:00 截止，會讓管理頁行為與文件不一致，因此需要補一個同步 change。

## What Changes

- 將管理員代替員工新增隔日訂餐的截止時間，從前一日 17:00 改為當日 16:30。
- 將管理員查詢所有員工訂餐記錄的 API 與前端篩選，從單一 `date` 改為 `date_from`、`date_to`、`employee_id`。
- 將管理員訂單頁的預設查詢條件改為今天～今天，並在進入頁面時自動查詢。
- 更新管理員頁面與 API 的提示、查詢參數與截止驗證，讓 A006、A003 邊界與實際操作一致。

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `admin-order-management`: 管理員查詢改為日期區間模式，代訂截止時間改為當日 16:30，並要求管理頁預設以今天～今天查詢。

## Impact

- 受影響程式碼：backend 管理員訂單 controller/service/repository、frontend 管理員訂單總覽頁與代訂表單。
- 受影響 API：`GET /api/admin/orders` 需改用 `date_from`、`date_to`、`employee_id`；`POST /api/admin/orders` 的截止判斷改為 16:30。
- 受影響規則：管理員代訂與 A003 17:00 批次通知之間需維持 30 分鐘緩衝。
- 相依項目：沿用既有 `admin-order-management` capability、A003 排程時點與 A002 訂單資料模型。
