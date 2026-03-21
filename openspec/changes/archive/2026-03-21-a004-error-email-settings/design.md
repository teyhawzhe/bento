## Context

A003 已定義在供應商通知失敗或排程發生系統錯誤時，要將錯誤通知寄送至 A004 錯誤通知信箱清單。要讓這條流程正式落地，系統必須先提供一個由管理員維護的錯誤通知信箱來源，避免依賴寫死在程式中的固定 recipients。

目前專案已具備管理員 JWT 權限、JDBC 資料層與系統設定頁可延伸的前後端基礎，因此 A004 主要是補 `error_notification_emails` 資料模型、對應 CRUD 中的查詢/新增/刪除 API，以及讓 A003 後續可直接讀取的 provider 邊界。

## Goals / Non-Goals

**Goals:**
- 定義管理員查詢、新增與刪除錯誤通知信箱的能力。
- 定義 `error_notification_emails` 資料邊界與 Email 格式驗證規則。
- 讓 A003 可以直接讀取這份信箱清單作為錯誤通知 recipients。
- 沿用既有管理員 JWT 權限與 API 錯誤處理格式。

**Non-Goals:**
- 本 change 不處理寄信流程本身，那屬於 A003。
- 本 change 不提供修改既有錯誤通知信箱內容的能力，僅支援新增與刪除。
- 本 change 不擴充更一般化的系統設定框架或多種類型通知收件設定。
- 本 change 不處理 A008 月結報表收件信箱設定。

## Decisions

### 1. 以獨立資料表維護錯誤通知信箱
- 決策：新增 `error_notification_emails` 資料表，保存 email、建立者與建立時間。
- 原因：這能與 A003 的錯誤通知需求直接對接，也方便後續查詢與稽核。
- 替代方案：把信箱寫在設定檔或 `.env`。未採用，因為管理員無法透過系統自行維護。

### 2. 僅提供查詢、新增、刪除，不提供編輯
- 決策：A004 第一版不做更新 API；若信箱需變更，採刪除舊資料再新增新資料。
- 原因：這與目前需求完全一致，實作也更簡潔。
- 替代方案：加入 PATCH 更新信箱。未採用，因為不是當前需求必要範圍。

### 3. 以 email 唯一性避免重複收件
- 決策：相同 email 不可重複新增，後端需驗證唯一性。
- 原因：避免 A003 寄送錯誤通知時同一收件人重複收到多封相同通知。
- 替代方案：允許重複資料，由 A003 再做 distinct。未採用，因為資料層就應保持乾淨。

### 4. 將 A003 recipients 讀取抽成可重用 provider
- 決策：A004 提供 repository / service，可被 A003 當作錯誤通知 recipients provider 使用。
- 原因：能讓 A003 不再依賴 stub 或寫死值，並保留後續擴充設定來源的空間。
- 替代方案：A003 直接 query A004 controller。未採用，因為 service/repository 整合更自然且少一層 HTTP 耦合。

## Risks / Trade-offs

- [若未限制 email 唯一性，會造成錯誤通知重複寄送] → 在資料表與 service 層同時做唯一性保護。
- [若誤刪唯一的錯誤通知信箱，A003 可能失去錯誤通知對象] → 初版先允許刪除，但 README 與後續 UI 需提示風險。
- [僅支援新增/刪除，使用者若要修改會多一次操作] → 以需求範圍優先，後續若真有需要再加更新 API。

## Migration Plan

- 新增 `error_notification_emails` schema、model、repository 與 service。
- 新增管理員查詢、新增、刪除 API 與前端設定頁邊界。
- 將 A003 的錯誤通知 recipients provider 切換為讀取這份設定。
- 若需 rollback，可暫時停用 A004 API，A003 回退到 stub recipients。

## Open Questions

- 當信箱清單為空時，A003 發生錯誤是否僅記錄 log，或要額外觸發 fallback 行為。
- 是否需要在 A004 就加入備註欄位，用來標示信箱用途或負責人。
