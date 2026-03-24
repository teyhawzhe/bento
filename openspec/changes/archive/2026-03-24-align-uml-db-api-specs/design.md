## Context

目前專案已有可運作的前後端骨架，但規格來源同時包含 `../uml/input/*.md`、`../uml/openapi.yaml`、`openspec/specs/*` 與實際程式碼，這四者之間已經出現明顯分歧。最嚴重的差異集中在 A001 認證模型、共用 API 回應格式、A003 供應商通知排程資料表、A010 部門管理資料模型，以及 A009 前端 TAB 規範。

這次變更是跨前端、後端、資料庫與排程的整體對齊工作，且包含安全性與資料遷移議題，因此需要先定義技術決策，再進入實作。

## Goals / Non-Goals

**Goals:**

- 讓現有後端 API 路徑、request/response 契約與 `../uml/openapi.yaml` 一致。
- 讓認證流程符合 A001 的 access token + refresh token 規格，並可安全作廢 refresh token。
- 讓資料庫 schema 補齊 A001、A003 所需表結構，並修正與 UML 衍生資料模型不一致之處。
- 讓前端登入後導向、TAB 結構與 MessageBox 行為對齊 A009。
- 讓 A003 與 A005 的排程與記錄責任清楚分離，便於後續驗證與維護。

**Non-Goals:**

- 不重新設計整體 UI 風格或大幅改寫前端畫面架構。
- 不在本次變更中新增 UML 以外的新業務功能。
- 不導入外部身分驗證服務或更換 JWT 技術棧。
- 不修改 `../uml` 內的需求、PUML 或 OpenAPI 原始文件。

## Decisions

### 1. API 以 `../uml/openapi.yaml` 為主要契約來源

後端 controller 與前端 API client 以 `../uml/openapi.yaml` 定義的 path、request body 與 response envelope 為優先依據；`openspec/specs/*` 若與 OpenAPI 不一致，則在本 change 中同步修正。

Rationale:
- 這份 OpenAPI 已經明確定義 request/response shape，是前後端對接最直接的契約。
- 目前最大痛點是 API shape 漂移，先收斂到單一契約來源最能降低後續歧義。

Alternatives considered:
- 以現有實作為準，反向修改 OpenAPI：會放大與 UML 原始需求的差距，因此不採用。
- 只對齊後端、不調整前端：會留下前後端資料結構不一致問題，因此不採用。

### 2. 導入統一的 API response envelope

後端新增共用成功/失敗回應模型，將既有裸 DTO 回傳統一包成 `status` 與 `data`。前端 `api.ts` 與型別層同步調整，只在 client 層拆封資料，避免 UI 到處處理 envelope。

Rationale:
- OpenAPI 已明確定義 `ApiSuccess` / `ApiFailed`。
- 在 client 層集中拆封可以降低 UI 重構範圍。

Alternatives considered:
- 僅更新文件不改實作：無法真正達成對齊。
- 在每個頁面元件自行拆封：重複程式會變多，因此不採用。

### 3. 認證改為 access token + refresh token 持久化模式

新增 `refresh_tokens` 資料表與對應 repository/service，登入時同時發 access token 與 refresh token；refresh token 需儲存 hash、到期時間、作廢狀態。logout、change-password、reset-password 時，需依規則作廢相關 refresh token。

Rationale:
- 這是 A001 與 OpenAPI 都明確要求的能力。
- 只有持久化 refresh token，才能支持單次使用、登出作廢與修改密碼後全部失效。

Alternatives considered:
- 僅用無狀態 JWT：無法實作 refresh token 單次使用與作廢。
- 直接存明文 refresh token：安全性較差，因此不採用。

### 4. schema 以「需求最小必要集」對齊 UML，避免保留未定義行為

`schema.sql` 補上 `refresh_tokens` 與 `notification_logs`。`departments` 若現有欄位超出需求，優先評估是否保留為內部實作欄位；若保留，API 與 spec 不暴露額外行為。A010 不再提供不在 UML/OpenAPI 內的刪除 API。

Rationale:
- 需求需要的資料必須補齊。
- 額外欄位若不影響外部契約，可暫時保留以降低 migration 風險，但不能再暴露出額外 API 行為。

Alternatives considered:
- 完全重建所有表：風險較高，且會增加不必要 migration 成本。
- 保留所有現況行為不處理：無法通過規格對齊。

### 5. A003 與 A005 排程分開建模與記錄

A003 使用 `notification_logs` 記錄每日供應商通知結果；A005 繼續使用 `monthly_billing_logs` 記錄月結發送結果。兩者共用 Email service 與錯誤通知來源，但不共用資料表。

Rationale:
- 兩者觸發頻率、資料內容與驗收條件不同。
- 分表後查詢、錯誤排除與驗證都更清楚。

Alternatives considered:
- 共用單一 logs 表：欄位意義會混亂，查詢條件也不清楚，因此不採用。

### 6. 前端以既有單頁結構調整 TAB，而非重寫畫面

保留現有 `App.tsx` 的單頁狀態管理方式，但調整 role 對應的主 TAB 與子內容。員工端回到「訂便當 / 我的訂單」規格；管理員端維持「訂單管理 / 供應商管理 / 報表設定 / 系統設定」主分頁，功能內容在各 TAB 內呈現。

Rationale:
- 能最大化重用現有 UI 與資料載入邏輯。
- 可在不重寫頁面架構下對齊 A009。

Alternatives considered:
- 改為完整 router-based 多頁架構：超出此次對齊變更範圍。

## Risks / Trade-offs

- [認證流程變更影響前後端登入狀態] → 以 client API 層集中處理 token 保存、refresh 與 envelope 拆封，降低 UI 受影響範圍。
- [既有測試大量依賴裸 DTO] → 先建立共用 response wrapper 測試輔助方法，再逐步調整 controller/service test。
- [資料表調整牽動 demo data 與初始化流程] → 同步更新 schema initializer 與 demo data initializer，避免啟動失敗。
- [部門表是否保留 `is_active` 存在解讀差異] → 對外契約只暴露 UML 定義行為；若保留欄位，視為內部實作細節，不再提供刪除/停用 API。
- [A003 排程補齊後可能暴露 Email stub 與真實排程差異] → 先以可測試的 service + scheduler 分層，Email 仍可保留 stub，但記錄與錯誤流程必須完整。

## Migration Plan

1. 新增與更新 schema：補 `refresh_tokens`、`notification_logs`，並調整部門與相關初始化資料。
2. 調整 backend DTO、controller、service 與例外回應，先完成 A001 與共用 response envelope。
3. 調整 frontend API client 與 session/token 管理，再更新 TAB 與 MessageBox 對應畫面。
4. 補齊 A003 scheduler、log repository、錯誤通知串接。
5. 更新測試與 README 說明，確認對外行為與規格一致。

Rollback strategy:
- 若 refresh token 持久化上線失敗，可暫時回退到原本單 token 實作，但必須連同前端 client 一起回退。
- 若 A003 排程影響既有批次穩定性，可先停用 scheduler bean，但保留資料表與 service。

## Open Questions

- `departments.is_active` 與 `updated_at` 是否完全移除，還是保留為內部欄位但不暴露於 API？
- A005 `POST /api/admin/reports/monthly` 是否維持現有較詳細的成功回應，還是嚴格收斂為 OpenAPI 的 `SuccessNoData`？
- `employee-account-administration` 現有 spec 檔內容已出現亂碼，實作前是否要一併在 spec delta 中以正確中文 requirement 完整覆寫？
