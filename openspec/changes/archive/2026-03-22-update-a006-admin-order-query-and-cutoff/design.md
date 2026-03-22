## Context

A006 最新文件已更新兩個核心行為：管理員查詢所有員工訂單時，應支援 `date_from`、`date_to` 與 `employee_id`，且進入畫面時預設查詢今天～今天；另外，管理員代訂隔日便當的截止時間已從舊版 `17:00` 改成 `16:30`。目前主規格、後端 service 與前端管理頁仍停留在舊版假設，導致查詢條件與截止邏輯都與文件不一致。

這次 change 不新增 capability，而是修正既有 `admin-order-management` 的查詢契約與截止規則。影響面會跨 backend controller/service/repository、frontend 管理員訂單頁，以及 OpenSpec 主規格，因此先用 design 固定資料邊界與驗證方式。

## Goals / Non-Goals

**Goals:**
- 將管理員訂單查詢改成日期區間模型，支援 `date_from`、`date_to`、`employee_id`。
- 讓管理員頁首次載入時預設以今天～今天自動查詢，而不是依賴單一日期欄位。
- 將管理員代訂隔日便當的截止時間改為當日 `16:30`。
- 讓前端、後端與 OpenSpec 對 A006 的查詢參數與截止規則保持一致。

**Non-Goals:**
- 本 change 不調整員工端 A002 的下單或取消規則。
- 本 change 不重做管理員頁的整體資訊架構，只更新查詢條件與截止提示。
- 本 change 不處理分頁、排序或匯出需求。

## Decisions

### 1. 查詢 API 從單日 `date` 改為區間 `date_from` / `date_to`
- 決策：`GET /api/admin/orders` 改成接受 `date_from`、`date_to`、`employee_id`；若未提供日期，server 端以今天～今天作為預設值。
- 原因：這與最新 A006 文件一致，也能讓前端首次進入頁面時自動得到當日結果。
- 替代方案：保留單一 `date`，由前端自行拆成兩次查詢。未採用，因為查詢語意與文件不一致，且不利於篩選擴充。

### 2. 管理員頁以今天～今天作為預設查詢狀態
- 決策：前端管理員訂單頁初始化查詢條件時，`date_from` 與 `date_to` 都設成今天，並在登入後或切入管理員畫面時直接查詢。
- 原因：這符合 A006 的使用流程，也能避免管理頁一開始顯示空白或無篩選的全量資料。
- 替代方案：由後端預設今天～今天，但前端畫面欄位留空。未採用，因為畫面與實際查詢條件會脫節。

### 3. 管理員代訂截止改為當日 16:30
- 決策：`ensureAdminOrderCreationWindowOpen` 改成使用當日 `16:30`，與 A003 每日 `17:00` 批次之間保留 30 分鐘緩衝。
- 原因：A006 最新文件已明確指定新截止時間，且這是對管理員補單可否送出的唯一判斷。
- 替代方案：沿用 `17:00`。未採用，因為與文件不符，也會讓補單時間侵蝕 A003 的排程緩衝。

### 4. 管理員取消與代訂的截止規則分開維護
- 決策：雖然兩者目前都會落在 `16:30` / `17:00` 這類批次邊界附近，仍保留獨立的 deadline service 方法，避免未來 A002 與 A006 再次調整時互相牽動。
- 原因：A002 最近已更新管理員取消規則，A006 也更新了代訂規則，兩者需求來源不同，集中成同一個方法風險較高。
- 替代方案：所有管理員行為共用同一個截止檢查。未採用，因為容易在後續需求變更時造成規則漂移。

## Risks / Trade-offs

- [查詢參數從 `date` 轉成區間後，前後端若只改一邊會查不到資料] → 同步更新 controller、frontend API helper 與初始化查詢 state。
- [今天～今天的預設值若前後端時區不一致，可能查出錯誤日期] → 由前端與 backend 都以 `Asia/Taipei` 所在本地日期為基準。
- [代訂截止時間改成 16:30 後，既有測試與提示文案容易遺漏 `17:00`] → 補 deadline service、service、frontend 文案與測試案例的全套調整。
- [日期區間查詢可能比單日查詢回傳更多資料] → 第一版維持今天～今天預設值，降低首次載入資料量。

## Migration Plan

- 先更新 `admin-order-management` delta spec，固定日期區間查詢與 `16:30` 截止規則。
- 實作時先調整 backend controller/service/repository 與 deadline service，再更新 frontend 管理頁的查詢欄位和提示。
- rollout 後若需回滾，可暫時恢復單日 `date` 查詢與 `17:00` 截止邏輯；資料表本身不需要 migration。

## Open Questions

- `GET /api/admin/orders` 是否需要在本次就限制 `date_from <= date_to` 並回傳明確錯誤訊息，或先交由前端避免無效輸入。
