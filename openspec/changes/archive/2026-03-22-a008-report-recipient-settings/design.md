## Context

A005 已定義月結報表除了寄送給供應商外，還要寄送給 A008 報表收件信箱清單，但目前系統內尚未提供這份清單的實際設定來源。若沒有 A008，月結報表流程就只能依賴空 provider、stub 或寫死在程式中的收件者，無法讓管理員自行維護。

目前專案已具備 A004 系統設定型能力、管理員 JWT 權限、JDBC repository 與 A005 的 billing recipient provider 邊界，因此 A008 的主要工作是補 `report_recipient_emails` 資料模型、查詢/新增/刪除 API，以及把 A005 的 provider 接到這份正式設定資料上。

## Goals / Non-Goals

**Goals:**
- 定義管理員查詢、新增與刪除月結報表收件信箱的能力。
- 定義 `report_recipient_emails` 的資料邊界、Email 格式驗證與唯一性規則。
- 讓 A005 月結報表流程可直接讀取這份收件清單作為管理端 recipients。
- 沿用既有系統設定頁、管理員權限與 API 錯誤格式。

**Non-Goals:**
- 不在 A008 內重做 A005 月結報表本身的寄送流程。
- 不提供編輯既有收件信箱內容的能力，第一版僅支援新增與刪除。
- 不把 A004 與 A008 抽象化成更通用的設定框架。

## Decisions

### 1. 以獨立資料表維護月結報表收件信箱
- 決策：新增 `report_recipient_emails` 資料表，保存 email、建立者與建立時間。
- 原因：這與需求文件直接對齊，也能提供 A005 後續查詢與稽核所需資料。
- 替代方案：直接沿用 A004 的錯誤通知信箱表。未採用，因為兩者用途不同，管理上應分開。

### 2. API 介面比照 A004 的查詢/新增/刪除模式
- 決策：提供 `GET /api/settings/report-emails`、`POST /api/settings/report-emails`、`DELETE /api/settings/report-emails/{id}` 三個管理員 API，不新增 PATCH。
- 原因：A004 已建立一致的設定管理風格，可降低前後端複雜度與實作風險。
- 替代方案：加入修改 API。未採用，因為需求只要求查詢、新增、刪除。

### 3. 相同 Email 不可重複新增
- 決策：在資料表與 service 層同時保護 email 唯一性。
- 原因：避免 A005 月結報表寄送時，同一管理端收件者收到重複信件。
- 替代方案：允許重複資料，由 A005 寄送前再做 distinct。未採用，因為資料層應保持乾淨。

### 4. A005 recipient provider 切到 A008 正式資料來源
- 決策：沿用 A005 設計中的 provider 介面，A008 實作其 JDBC-backed provider，讓月結報表讀取真實收件清單。
- 原因：這能維持 A005 與 A008 之間的清楚邊界，避免 billing service 直接依賴 controller。
- 替代方案：A005 在寄送時直接 query A008 controller。未採用，因為 service/repository 整合更自然且耦合較低。

## Risks / Trade-offs

- [若未限制唯一性，管理端收件者可能重複收到月結報表] → 在 schema 與 service 都加上 email 唯一保護。
- [若清單為空，A005 將只寄給供應商] → A008 只負責設定來源，是否需要空清單警示留待實作時在 UI 補提示。
- [A004 與 A008 流程高度相似，可能出現重複程式碼] → 第一版先接受有限重複，優先交付需求；若後續再有更多設定類型再抽共用層。

## Migration Plan

- 新增 `report_recipient_emails` schema、model、repository 與 service。
- 新增管理員查詢、新增、刪除 API 與前端系統設定頁入口。
- 將 A005 billing recipient provider 從 stub 切換為讀取 `report_recipient_emails`。
- 如需 rollback，可暫時保留 provider 回傳空清單或 stub，但 A008 API 與資料表可獨立停用。

## Open Questions

- 當清單為空時，管理頁是否需要顯示明確提醒「月結報表目前只會寄給供應商」。
- 是否需要在 A008 第一版就加入收件信箱描述欄位；目前需求未提及，先維持最小欄位集。
