## Why

UML 的 A013 已經定義新的管理員報表需求：可依日期範圍查詢員工訂餐資料，支援畫面預覽與 PDF 下載。但目前系統的「報表設定」主 TAB 只承載 A005 月結報表與 A008 報表收件信箱設定，尚未提供員工訂餐明細報表的 API、前端預覽畫面或 PDF 匯出能力。

這次 change 要把 A013 納入現行產品結構，補齊查詢型報表能力，同時避免再增加新的管理員主 TAB，讓報表相關功能維持在同一個資訊架構中。

## What Changes

- 在管理員既有「報表設定」主 TAB 中加入 A013「員工訂餐報表」區塊或次頁，支援日期區間查詢、排序與畫面預覽。
- 新增 `GET /api/admin/reports/orders` 與 `GET /api/admin/reports/orders/pdf` 兩個 API，回傳員工訂餐報表資料與 PDF 檔案。
- 更新 OpenSpec 報表與前端導覽規格，明確定義 A013 與既有 A005/A008 在同一主 TAB 內的分工。

## Capabilities

### Modified Capabilities

- `frontend-tab-navigation-and-message-box`: 管理員「報表設定」主 TAB 需包含月結報表與員工訂餐報表兩類能力。
- `monthly-billing-reporting`: 需明確界定 A005 月結報表與 A013 員工訂餐報表屬於不同 API 與不同用途，避免混淆。

### New Capabilities

- `employee-order-reporting`: 定義 A013 員工訂餐報表查詢、排序與 PDF 下載契約。

## Impact

- Affected code: 管理員報表頁 UI、前端 API client、後端報表 controller/service/repository、PDF 產出流程。
- API: 新增 `/api/admin/reports/orders` 與 `/api/admin/reports/orders/pdf`。
- UI: 不新增管理員主 TAB，改在現有「報表設定」下擴充員工訂餐報表區塊。
- Specs: 需新增員工訂餐報表主規格，並同步修正前端導覽與月結報表規格的責任邊界。
