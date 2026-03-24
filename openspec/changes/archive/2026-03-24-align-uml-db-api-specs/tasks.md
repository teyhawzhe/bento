## 1. 資料庫與初始化調整

- [x] 1.1 補齊 `refresh_tokens` 與 `notification_logs` 資料表，並更新 `schema.sql`
- [x] 1.2 調整部門相關 schema 與初始化流程，避免對外暴露不在 UML/OpenAPI 內的刪除或停用行為
- [x] 1.3 更新 demo data 與 schema initializer，確認新表與既有資料能正常啟動

## 2. A001 認證流程對齊

- [x] 2.1 新增 refresh token 的 model、dao 與 service，支援建立、查詢、作廢與輪替
- [x] 2.2 調整登入流程，讓員工與管理員登入都回傳 access token + refresh token
- [x] 2.3 實作 `/api/auth/refresh` 與 `/api/admin/auth/refresh`，並強制 refresh token 單次使用
- [x] 2.4 調整 logout、change-password、reset-password 流程，補上 refresh token 作廢邏輯

## 3. API 契約與後端回應格式對齊

- [x] 3.1 建立共用 API success/failed envelope，統一 controller 回應格式
- [x] 3.2 對齊 A001、A002、A004、A005、A006、A007、A008、A010 的 request/response shape 與 OpenAPI 一致
- [x] 3.3 移除或封存不在 UML/OpenAPI 規格內的 A010 對外刪除 API
- [x] 3.4 補強全域錯誤處理，讓驗證與授權失敗回應符合共用契約

## 4. A003 與 A005 排程邊界對齊

- [x] 4.1 實作 A003 每日 17:00 供應商通知 service 與 scheduler
- [x] 4.2 建立 `notification_logs` 的持久化與查詢邏輯，區分成功、失敗、異常與系統錯誤
- [x] 4.3 串接 A004 錯誤通知信箱來源，補齊 A003 發送失敗的錯誤通知流程
- [x] 4.4 檢查 A005 月結報表仍使用 `monthly_billing_logs`，避免與 A003 共用記錄表

## 5. 前端 API Client 與畫面對齊

- [x] 5.1 調整 `frontend/src/api.ts` 與型別定義，統一處理 `status/data` envelope
- [x] 5.2 調整前端 session/token 管理，支援 refresh token 保存、刷新與失效處理
- [x] 5.3 依 A009 調整登入後預設導向與主 TAB 結構，讓員工顯示「訂便當」「我的訂單」
- [x] 5.4 檢查並修正 MessageBox 在成功、失敗、警告與確認情境下的使用一致性

## 6. 驗證與文件更新

- [x] 6.1 補齊或更新 backend 測試，涵蓋認證刷新、token 作廢、A003 記錄與 envelope 回應
- [x] 6.2 補齊或更新 frontend 行為驗證，確認 TAB、登入導向與 API client 契約正常
- [x] 6.3 更新 README 與相關說明，反映最新的認證模式、A003/A005 邊界與已對齊規格
