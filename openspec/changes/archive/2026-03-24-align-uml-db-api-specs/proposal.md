## Why

目前專案已經完成多數 A001、A002、A004、A005、A006、A007、A008、A010 的第一版功能，但實作內容與 `../uml/input`、`../uml/openapi.yaml`、以及 UML 衍生的資料表規格仍有明顯落差。這些落差集中在認證 Token 模型、API 回應格式、部分資料表結構，以及前端分頁與流程行為，若不先對齊，後續功能擴充與驗收會持續出現規格分歧。

## What Changes

- 對齊 A001 員工與管理員登入驗證規格，補齊 refresh token、refresh API、logout 作廢機制，以及 change-password 後 token 作廢流程。
- 對齊 OpenAPI 的共用回應格式，讓既有 API 回傳結構與前端資料存取方式符合 `status` / `data` 契約。
- 對齊資料庫 schema，補上 A001 與 A003 所需資料表與欄位，並修正與 UML 規格不一致的部門資料結構。
- 對齊 A010 部門管理 API，移除不在 UML 與 OpenAPI 規格內的行為，讓 request/response 與需求一致。
- 對齊 A009 前端 TAB 與訊息提示規範，調整登入後預設導向、員工與管理員頁籤內容，以及既有 MessageBox 使用方式。
- 對齊 A003 與 A005 的排程與記錄邊界，明確區分供應商通知紀錄與月結報表紀錄。

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `employee-authentication`: 認證流程改為 access token + refresh token，並補齊 refresh、logout 作廢、change-password 後失效機制與回應契約。
- `employee-account-administration`: 員工管理相關 API 的 request/response 需對齊 OpenAPI 與共用回應格式。
- `employee-bento-ordering`: 訂餐相關 API 的回應契約需對齊 OpenAPI，並保留員工端不顯示價格的規則。
- `admin-order-management`: 管理員查單、代訂、代取消流程與回應格式需對齊 OpenAPI 與 A006 規格。
- `bento-supplier-management`: 供應商查詢、單筆查詢與修改 API 契約需與 OpenAPI 一致。
- `bento-menu-management`: 菜單查詢與維護 API 回應契約需與 OpenAPI 一致。
- `error-email-settings`: 錯誤通知信箱 API 回應格式需對齊 OpenAPI，並作為 A003 錯誤通知來源。
- `billing-report-recipient-settings`: 報表收件信箱 API 回應格式需對齊 OpenAPI，並作為 A005 收件來源。
- `monthly-billing-reporting`: 月結報表觸發與記錄回應格式需對齊 OpenAPI，並與 A003 記錄表分離。
- `supplier-order-notification`: 補齊每日 17:00 排程、通知記錄表與錯誤通知流程，讓現況從邊界保留提升為可驗收功能。
- `department-management`: 部門資料結構、更新 API 契約與允許操作需與 UML/OpenAPI 對齊。
- `frontend-tab-navigation-and-message-box`: 前端登入後導向、TAB 配置與 MessageBox 行為需回到 A009 規格。

## Impact

- Affected code: `backend/src/main/java/**`, `backend/src/main/resources/schema.sql`, `frontend/src/**`
- Affected APIs: `../uml/openapi.yaml` 中 A001、A002、A003、A004、A005、A006、A007、A008、A010 相關路徑的既有實作
- Affected systems: JWT 驗證流程、前端 session/token 管理、排程通知、月結發信、資料庫 migration/init
- Dependencies: 需新增 refresh token 持久化與 A003 通知記錄持久化；前後端都需同步調整回應格式
