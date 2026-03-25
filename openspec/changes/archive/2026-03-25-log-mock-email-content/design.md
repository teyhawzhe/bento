## Context

目前 `MockMailSender` 只把寄信資料保存在記憶體清單中，方便測試透過程式讀取 `sentEmails()` 驗證，但對本機開發、手動測試與排查排程通知來說，這種可觀測性不夠直接。使用者希望在 `mail mode = mock` 時，能直接從 application log 查到 email 的收件人、主旨與內容，而不需要額外透過 debugger 或測試程式檢查記憶體狀態。

現有架構已將 mock 與 smtp 發信路徑分離，因此這次變更可以集中在 mock sender 的 observability，不需要改動業務服務如何呼叫 `EmailService`。

## Goals / Non-Goals

**Goals:**
- 讓 `mock` mode 每次寄信都產生可查驗的 log。
- 讓 log 至少包含收件人、主旨與內容，支援手動驗證通知內容。
- 保持 `EmailService` 的呼叫方式與現有業務流程不變。
- 確保 `smtp` mode 行為完全不受影響。

**Non-Goals:**
- 不修改實際 email 模板內容或寄送條件。
- 不引入新的 audit table、外部 log sink 或通知追蹤系統。
- 不改變既有 `sentEmails()` 測試介面，除非為了配合 log 行為補強測試。

## Decisions

### 1. 在 `MockMailSender` 內集中輸出 mock email log
由 `MockMailSender.send(...)` 在保留既有記憶體紀錄的同時，新增 logger 輸出 email payload。這樣所有透過 `EmailService` 進入 mock 寄信的流程，都能自動取得一致的 log 行為。

原因：
- 變更點最小，且可覆蓋所有 mock 寄信入口。
- 不需要在 `EmailService` 或各業務服務重複加 log。
- 能保留現有測試仍使用 `sentEmails()` 驗證的能力。

替代方案：
- 在 `EmailService` 加 log：也能達成，但會把 mock-specific 行為放進共用 service。
- 在各業務流程個別加 log：重複且容易遺漏。

### 2. Log 內容固定包含 recipient、subject、body
mock 寄信 log 將明確輸出收件人、主旨與內容，必要時以多行格式或清楚分隔符呈現，避免 body 與其他系統 log 混在一起難以閱讀。

原因：
- 使用者的主要目標是直接查驗 email 內容，而不只是知道有沒有觸發寄信。
- 固定欄位比只輸出拼接字串更利於閱讀與測試比對。

替代方案：
- 只記錄「已發送」事件：不足以驗證實際內容。
- 僅記錄 subject 不記錄 body：仍無法確認模板或變數代入是否正確。

### 3. 僅在 `mock` mode 記錄完整內容，`smtp` mode 維持現狀
完整 email payload 的 application log 僅限 `MockMailSender`。`SmtpMailSender` 不新增對應 log，避免真實寄信環境暴露敏感內容。

原因：
- 某些 mock email 可能包含臨時密碼、重設資訊或通知明細。
- 使用者需求是查驗 mock email，不是改變正式寄信的稽核策略。

替代方案：
- 在所有 mode 都記錄 body：風險過高，不符合最小必要揭露原則。
- 對 body 做部分遮罩：會降低手動驗證價值，無法滿足這次需求。

## Risks / Trade-offs

- [Risk] mock email body 可能包含敏感資訊，例如臨時密碼或 reset 內容
  → Mitigation: 明確把完整內容 log 限制在 `mock` mode，並在 spec 中禁止延伸到 `smtp` mode。

- [Risk] 多行 body 可能讓 log 可讀性下降
  → Mitigation: 使用固定前綴與清楚欄位標記，讓開發者能快速定位每封 mock email。

- [Risk] 既有測試只驗證記憶體紀錄，未覆蓋 log 行為
  → Mitigation: 補上 `MockMailSender` 或 mail flow 的測試，驗證寄信後會產生包含關鍵欄位的 log。

## Migration Plan

1. 在 `MockMailSender` 增加 logger，於 `send(...)` 寫出收件人、主旨與內容。
2. 保留 `sentEmails()` 現有行為，避免破壞既有測試與呼叫端。
3. 新增或調整測試，驗證 mock 寄信同時保留記憶體紀錄與可查驗 log。
4. 在本機以 `mail mode = mock` 觸發一條實際寄信流程，確認 log 可直接查驗 email 內容。

Rollback strategy:
- 若 log 格式造成測試或閱讀問題，可回退為既有記憶體紀錄模式，不影響其他業務邏輯。
- 若未來評估敏感資訊風險過高，可限制 mock mode 的使用環境或調整 log 等級與輸出格式。

## Open Questions

- mock email log 是否需要固定前綴關鍵字，例如 `MOCK_EMAIL`，方便日後搜尋？
- 是否要把 recipient、subject、body 拆成結構化欄位，或先以易讀字串格式落地即可？
