## Why

目前月結報表流程已經在 A005 定義會寄送給供應商與 A008 收件清單，但系統內還沒有真正可管理這份清單的設定能力。A008 要把這個預留邊界補齊，讓管理員可以自行維護月結報表的內部收件者，而不需要修改程式或依賴暫時的 stub。

## What Changes

- 新增管理員查詢月結報表收件信箱清單的能力。
- 新增管理員新增月結報表收件信箱的能力，包含 Email 格式與重複值驗證。
- 新增管理員刪除月結報表收件信箱的能力。
- 建立 `report_recipient_emails` 的資料模型、API 契約與管理頁設定入口。
- 將 A005 月結報表流程使用的報表收件者 provider 接到實際設定資料來源。

## Capabilities

### New Capabilities
- `billing-report-recipient-settings`: 管理員查詢、新增與刪除月結報表收件信箱清單的能力。

### Modified Capabilities

## Impact

- 受影響程式碼：backend 系統設定 controller/service/repository、A005 收件者 provider、frontend 系統設定頁。
- 受影響 API：新增 `GET /api/settings/report-emails`、`POST /api/settings/report-emails`、`DELETE /api/settings/report-emails/{id}`。
- 受影響資料：需新增 `report_recipient_emails` 資料表，保存 Email、建立者與建立時間。
- 相依項目：A005 月結報表流程會從此設定清單讀取內部收件者，並與既有供應商收件邏輯共同使用。
