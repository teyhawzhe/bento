## Why

A001 已完成登入驗證與員工帳號管理，但員工訂便當系統的核心價值仍未落地：員工尚無法瀏覽下週工作日便當並完成訂餐，管理員也無法建立供應商與維護菜單。現在需要補上 A002 的訂餐主流程，讓系統從「可登入」前進到「可實際管理菜單並完成員工訂餐」。

## What Changes

- 新增員工瀏覽下週工作日便當選項、建立訂單、更新訂單、取消訂單與查詢個人訂餐記錄的能力。
- 新增管理員建立供應商、依日期建立菜單、設定菜單有效期間、查詢含歷史的菜單清單與編輯菜單資料的能力。
- 新增管理員在指定截止時間前取消指定員工訂單的能力，並銜接 A003 每日供應商訂單通知的排程邊界。
- 建立前後端一致的 A002 API 契約、固定截止條件、資料模型與畫面流程。
- 延伸 A001 的 JWT 角色授權，讓管理員與員工在登入後進入不同的 A002 操作路徑。

## Capabilities

### New Capabilities
- `employee-bento-ordering`: 員工瀏覽下週工作日便當選項、建立或更新同日訂單、取消訂單與查詢個人訂餐記錄。
- `bento-menu-management`: 管理員查詢菜單清單、建立與編輯指定供應商的菜單，並設定菜單有效期間。
- `bento-supplier-management`: 管理員新增可供菜單使用的供應商基本資料。
- `admin-order-management`: 管理員在允許期限內取消指定員工的訂餐記錄。

### Modified Capabilities
- `employee-authentication`: 已登入員工與管理員在 A002 頁面需沿用既有 JWT session 與角色授權進入對應功能頁。

## Impact

- 受影響程式碼：frontend 訂餐頁與管理頁、backend 訂單/菜單/供應商模組、共用 API 型別與授權攔截。
- 受影響 API：新增 `/api/orders/menu`、`/api/orders`、`/api/orders/me`、`/api/menus`、`/api/suppliers`、`/api/admin/orders/{id}` 等 A002 端點。
- 受影響資料：需新增 suppliers、menus、orders 等資料模型，並與既有 employees 身分關聯。
- 相依項目：沿用 A001 JWT 驗證，並需保留與 A003「每日供應商訂單通知」截止時間的銜接規則。
