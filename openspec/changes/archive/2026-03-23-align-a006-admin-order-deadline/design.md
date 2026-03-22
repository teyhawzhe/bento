## Context

A006 最新 UML 文件現在明確規定，管理員代訂與代取消的截止時間都必須是「該訂餐日前一日 16:30」。目前系統才剛完成一輪 A006 同步，但那一輪把代訂截止實作成「當日 16:30」，與最新文件不一致。查詢 API 的日期區間、今天～今天預設值與管理員代取消規則目前都已正確，因此這次 change 應聚焦在修正代訂 deadline 漂移，而不是再次擴大範圍。

## Goals / Non-Goals

**Goals:**
- 將管理員代訂隔日便當的 deadline 驗證改為「該訂餐日前一日 16:30」。
- 同步更新 `admin-order-management` 規格、前端提示文案與測試案例。
- 保持管理員代取消 deadline 與代訂 deadline 使用一致的時間邊界。

**Non-Goals:**
- 不調整管理員查詢訂單 API 的 `date_from`、`date_to`、`employee_id` 契約。
- 不更動管理頁整體版面與篩選互動，只修正文案中錯誤的截止描述。
- 不修改 A002 員工端截止規則。

## Decisions

### 1. 管理員代訂 deadline 回到「訂餐日前一日 16:30」
- 決策：`ensureAdminOrderCreationWindowOpen(orderDate)` 直接使用 `orderDate.minusDays(1)` 的 `16:30` 作為 cutoff。
- 原因：這與最新 A006 文字、API 說明與流程圖一致，也與管理員代取消規則完全對齊。
- 替代方案：沿用目前的「當日 16:30」。未採用，因為會讓隔日補單在訂餐日當天才截止，與最新需求衝突。

### 2. 保留代訂與代取消為獨立方法，但共用相同時間邊界
- 決策：保留 `ensureAdminOrderCreationWindowOpen(...)` 與 `ensureAdminCancellationWindowOpen(...)` 兩個公開方法，不直接合併 API；但兩者都遵循「前一日 16:30」。
- 原因：代訂與代取消雖然目前規則相同，但需求來源不同，保留分離能降低未來再次變更時的耦合風險。
- 替代方案：把代訂直接委派給取消 deadline helper。未採用，因為語意會被混在一起，後續維護不清楚。

### 3. 前端只修正提示，不預先做額外本地截止判斷
- 決策：前端管理員頁更新提示文字，讓使用者明白截止時間是「訂餐日前一日 16:30」；實際是否允許送出仍以 backend 驗證為準。
- 原因：目前管理員代訂已由 server 做最終驗證，前端不需要重複建立一套易漂移的本地時間規則。
- 替代方案：前端先依本機時間禁用代訂按鈕。未採用，因為容易與 server 時區或未來規則變更脫節。

## Risks / Trade-offs

- [舊測試仍以當日 16:30 為準] → 同步更新 deadline service 與 service/controller 驗證案例。
- [前端文案若未更新，使用者會誤以為可在當日補單] → 一起修正管理員頁 deadline 提示。
- [近期剛做過 A006，同檔案容易殘留上一輪假設] → 這次 change 僅鎖定代訂 deadline，避免查詢功能被誤改。

## Migration Plan

- 先更新 `admin-order-management` delta spec，固定代訂 deadline 的新舊差異。
- 實作時修改 backend deadline service 與相關測試，再調整 frontend 提示文案。
- rollout 後若要回滾，只需恢復代訂 deadline 的計算方式；資料表與 API 參數不需 migration。

## Open Questions

- 無。最新 A006 對代訂 deadline 的文字已足夠明確，本 change 可直接實作。
