## Why

`../uml/input/A002.md` 已更新員工訂便當規格，但目前 OpenSpec 主規格仍只覆蓋部分 A002 行為，特別是員工可訂日期範圍、員工與管理員取消截止時間，以及前端取消按鈕顯示規則尚未完整對齊。這輪進一步對照 `../uml` 全部需求文件、OpenSpec 主規格與實際專案後，也確認仍存在少量規格殘留，例如 A002 已移除的歷史菜單語意與前端殘留的非規格部門刪除 helper。現在需要補上一個對齊型 change，讓後續實作與驗證都能以最新版 UML 文件、OpenAPI 與專案現況的交集為單一依據。

## What Changes

- 對齊員工訂餐規格到最新版 A002：新增「本週星期五 12:00 前可新增/修改」與「截止日後到下下週五前依菜單有效期間可訂」的規則。
- 補充員工取消訂單規則：取消截止時間改為該訂餐日前一日 16:30，且前端需在載入訂單時隱藏已逾期訂單的取消按鈕。
- 補充管理員取消指定員工訂單規則：同樣以該訂餐日前一日 16:30 為截止時間，超過後拒絕取消。
- 對齊管理員菜單查詢規格：A002 最新版僅要求「查詢菜單清單」，不再要求「含歷史」查詢模式，因此需移除歷史菜單切換語意與 `include_history` 規格依賴。
- 明確化員工訂餐與個人訂單 API 需同時遵守最新版 A002 與 `../uml/openapi.yaml` 契約，持續維持員工端不顯示價格。
- 整理這輪規格審計發現的相鄰殘留差異，至少包含不再屬於任何 UML/API 契約的前端 `deleteDepartment` helper，避免 README、OpenSpec 與專案實作繼續分岐。
- 以 delta specs 方式更新既有能力，作為後續前後端調整與測試的規格基礎。

## Capabilities

### New Capabilities
<!-- None -->

### Modified Capabilities
- `employee-bento-ordering`: 補上員工可訂日期範圍、新增/修改截止時間、取消截止時間與前端取消按鈕顯示規則。
- `admin-order-management`: 補上管理員取消指定員工訂單的 16:30 截止規則與逾時拒絕行為。
- `bento-menu-management`: 對齊最新版 A002 的菜單查詢範圍，移除「含歷史菜單」作為 A002 要求的一部分。

## Impact

- 受影響規格：`openspec/specs/employee-bento-ordering/spec.md`、`openspec/specs/admin-order-management/spec.md`、`openspec/specs/bento-menu-management/spec.md`。
- 受影響 API：`GET /api/orders/menu`、`POST /api/orders`、`PATCH /api/orders/{id}`、`DELETE /api/orders/{id}`、`GET /api/orders/me`、`GET /api/menus`、`DELETE /api/admin/orders/{id}`。
- 受影響程式碼：frontend 員工訂餐頁、個人訂單清單、菜單管理頁與非規格殘留 helper、backend 訂單截止判斷、菜單查詢參數與 API 回應測試。
- 相依文件：`../uml/input/A002.md`、`../uml/output/A002/*.puml`、`../uml/openapi.yaml`。
