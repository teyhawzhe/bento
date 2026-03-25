## Why

目前系統在 `mail mode = mock` 時雖然會保留記憶體中的寄信紀錄，但開發者在本機或測試環境排查問題時，無法直接從 application log 查驗實際寄出的收件人、主旨與內容。這讓驗證通知內容變得不夠直觀，也增加了排查 mock 寄信流程的成本。

## What Changes

- 明確規範 `mock` mail mode 在每次寄信時都要輸出可查驗的應用程式 log。
- 規範 log 內容至少包含收件人、主旨與信件內容，方便開發與測試直接比對。
- 要求 mock mode 的 log 行為不改變既有業務流程結果，也不得影響 `smtp` mode 的真實寄信路徑。
- 補齊對應測試與驗證步驟，確保 mock 寄信時能穩定觀察到信件內容。

## Capabilities

### New Capabilities

### Modified Capabilities
- `mail-delivery-configuration`: 將 mock mode 的可觀測性明確化為 application log 輸出，要求寄信時記錄收件人、主旨與內容，且不得影響 smtp mode 行為。

## Impact

- Affected code: `backend/src/main/java/com/lovius/bento/service/MockMailSender.java`、可能包含 `EmailService` 測試與 mail 相關整合測試。
- Affected systems: backend mock mail observability、開發與測試排查流程。
- No API contract changes are expected; this is a behavior and observability update for mock email delivery.
