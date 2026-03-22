## Context

目前 `bento-supplier-management` 只定義了管理員新增供應商的能力，但營運上供應商資料會持續變動，管理員需要查詢既有供應商、檢視單筆細節並更新聯絡資訊與啟用狀態。A007 會同時涉及前端管理頁、後端供應商 API、repository 查詢條件與欄位保護規則，因此需要先固定查詢行為與更新邊界。

既有系統已經有 `suppliers` 資料表與新增供應商 API，A007 應優先重用現有供應商資料模型與 A001 管理員角色驗證，不新增新的供應商資料結構，只補上查詢與修改的能力。需求文件已明確指定 `id` 與 `business_registration_no` 不可修改，因此本次設計的核心是把這兩個欄位視為唯讀欄位，並讓前後端都一致遵守。

## Goals / Non-Goals

**Goals:**
- 定義供應商清單查詢 API，支援名稱精確與模糊查詢。
- 定義單一供應商詳細資料 API，供管理頁載入編輯前內容。
- 定義供應商修改 API，限制可編輯欄位為 `name`、`email`、`phone`、`contact_person`、`is_active`。
- 在前端管理頁提供供應商搜尋、檢視與修改流程，讓管理員可持續維護既有資料。

**Non-Goals:**
- 不新增新的供應商欄位或改變 `suppliers` 主鍵設計。
- 不支援修改 `business_registration_no` 或供應商 `id`。
- 不在 A007 內新增刪除供應商能力。

## Decisions

### 1. 延用既有 `bento-supplier-management` 模組擴充查詢與更新
- 決策：在現有供應商 controller/service/repository 上新增 `GET /api/suppliers`、`GET /api/suppliers/{id}` 與 `PATCH /api/suppliers/{id}`，與既有建立供應商 API 保持同一模組邊界。
- 原因：新增、查詢、更新都是同一份供應商資料的管理操作，集中實作可減少驗證與 DTO 分散。
- 替代方案：拆成新的 capability 或獨立 controller。未採用，因為資料模型完全相同，拆分只會增加維護成本。

### 2. 清單查詢以 `name + search_type` 控制精確或模糊搜尋
- 決策：`GET /api/suppliers` 支援可選 `name` 與 `search_type` 參數；`search_type=exact` 時做完全比對，`search_type=fuzzy` 時做包含關鍵字查詢；若未提供 `name` 則回傳全部供應商。
- 原因：這與需求文件完全對齊，也能讓管理頁在單一入口覆蓋瀏覽與搜尋兩種場景。
- 替代方案：固定採模糊搜尋。未採用，因為需求明確需要精確與模糊兩種模式。

### 3. 修改 API 使用白名單欄位更新
- 決策：`PATCH /api/suppliers/{id}` 的 request DTO 只接受可編輯欄位，service 層忽略或拒絕任何企圖修改 `business_registration_no` 或 `id` 的內容。
- 原因：以 DTO 限縮輸入欄位最直接，也能讓不可編輯規則在 API contract 就明確可見。
- 替代方案：接受完整 supplier payload 後再於 service 手動排除唯讀欄位。未採用，因為較容易產生誤更新或前後端認知不一致。

### 4. 前端管理頁採「搜尋列表 + 單筆編輯表單」模式
- 決策：管理頁先顯示供應商搜尋條件與列表，選取單筆供應商後載入詳細資料到編輯表單；唯讀欄位顯示但不可編輯。
- 原因：這與需求流程一致，且能讓管理員在同一頁面完成搜尋與更新，不需要跳轉。
- 替代方案：直接在列表內做 inline edit。未採用，因為會讓唯讀欄位與欄位驗證提示較難呈現。

## Risks / Trade-offs

- [模糊搜尋與資料量增加可能影響效能] → 第一版先以名稱條件查詢，不加複雜排序與分頁；後續如清單成長再補分頁。
- [前端與後端對唯讀欄位理解不一致] → 由 backend DTO 白名單做最終保護，前端只作展示限制。
- [查詢 API 行為不清楚] → 在 spec 明確定義未帶 `name` 時回傳全部供應商，避免不同實作解讀。

## Migration Plan

- 不需資料表 migration，直接部署後端查詢/更新 API 與前端管理頁即可。
- 先部署 backend API，再部署 frontend 搜尋與編輯介面，避免前端先上線卻缺少查詢或更新端點。

## Open Questions

- 第一版供應商清單是否需要顯示建立時間；目前需求未強制，預計保留詳細資料與列表共用既有回傳欄位。
- 若 `search_type` 缺省時預設精確或模糊搜尋；目前設計採未帶 `name` 回全部，帶 `name` 但未帶 `search_type` 時由實作決定預設值為 `exact` 或明確要求參數。
