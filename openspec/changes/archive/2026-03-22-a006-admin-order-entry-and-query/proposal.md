## Why

目前系統的管理員只能在截止前取消指定員工訂單，但當員工遺漏訂餐或需要協助補單時，管理端還缺少查詢全員訂餐紀錄與代為建立隔日訂單的能力。A006 要補齊這段營運流程，讓管理員能在 A003 每日通知批次執行前完成查詢、補單與責任追蹤。

## What Changes

- 擴充管理員訂單管理能力，支援查詢所有員工訂餐紀錄，並可依日期、員工條件篩選。
- 新增管理員代替員工建立隔日訂單的能力，沿用 A003 的 17:00 截止邊界。
- 明確定義代訂成功時 `orders.created_by` 的記錄規則，用來保存實際操作者為管理員。
- 補齊前後端管理頁、管理 API、資料欄位與驗證規則，讓查詢、截止檢查與代訂流程一致。

## Capabilities

### New Capabilities

### Modified Capabilities
- `admin-order-management`: 從僅支援管理員取消員工訂單，擴充為支援管理員查詢全員訂單與在期限內代替員工新增隔日訂單。

## Impact

- 受影響程式碼：backend 管理員訂單 controller/service/repository、frontend 管理員訂單總覽頁與新增代訂流程。
- 受影響 API：新增或調整 `GET /api/admin/orders`、`POST /api/admin/orders` 的查詢與建立契約。
- 受影響資料：`orders` 需新增 `created_by` 欄位並與 `employees` 關聯，查詢需 JOIN 員工與菜單資料。
- 相依項目：沿用 A001 JWT/角色授權、A002 訂單與菜單資料模型、A003 每日 17:00 供應商通知截止規則。
