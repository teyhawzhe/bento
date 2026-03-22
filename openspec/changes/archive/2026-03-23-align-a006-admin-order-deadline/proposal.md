## Why

A006 最新文件已把管理員代訂與代取消的截止規則統一為「該訂餐日前一日 16:30」，但目前 OpenSpec 主規格與實作仍把代訂寫成「當日 16:30」。這會讓管理員補單的可操作時間比文件多出一天，必須盡快同步，避免系統行為與需求定義持續偏離。

## What Changes

- 將管理員代替員工新增隔日訂餐的截止規則，從當日 `16:30` 修正為該訂餐日前一日 `16:30`。
- 同步更新 A006 的 OpenSpec delta spec、後端 deadline 驗證、管理員頁提示文案與相關測試。
- 保持管理員查詢訂單的日期區間查詢與今天～今天預設值不變，本 change 只修正截止時間規則漂移。

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `admin-order-management`: 管理員代訂隔日便當的截止時間改為該訂餐日前一日 `16:30`，並與管理員代取消規則一致。

## Impact

- 受影響程式碼：backend `OrderDeadlineService`、`OrderService` 與對應測試；frontend 管理員訂單頁提示文案。
- 受影響 API：`POST /api/admin/orders` 的截止驗證行為。
- 受影響規則：A006 的管理員代訂時間邊界將回到 UML 最新文件定義，與管理員代取消保持一致。
