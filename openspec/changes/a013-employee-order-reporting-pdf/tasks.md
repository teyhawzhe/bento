## 1. OpenSpec Alignment

- [x] 1.1 新增 `employee-order-reporting` 主規格，定義員工訂餐報表查詢、排序與 PDF 下載契約
- [x] 1.2 更新 `frontend-tab-navigation-and-message-box`，明確說明管理員「報表設定」主 TAB 內包含員工訂餐報表能力
- [x] 1.3 更新 `monthly-billing-reporting`，補上 A005 月結報表與 A013 員工訂餐報表的邊界說明

## 2. Backend Reporting APIs

- [x] 2.1 新增 `GET /api/admin/reports/orders`，支援 `date_from`、`date_to`、`sort_by`
- [x] 2.2 建立員工訂餐報表查詢 service / repository，回傳日期、部門、員工姓名、便當名稱、廠商欄位
- [x] 2.3 新增 `GET /api/admin/reports/orders/pdf`，以相同條件輸出 PDF 檔案
- [x] 2.4 補上管理員權限、參數驗證與 API / service 測試

## 3. Frontend Report UI

- [x] 3.1 在既有 `admin-reports` 頁面加入「員工訂餐報表」區塊或次分頁
- [x] 3.2 新增日期區間、排序條件與查詢按鈕，並顯示畫面預覽結果
- [x] 3.3 新增 PDF 下載按鈕，下載目前條件對應的報表
- [x] 3.4 保持既有 A005 月結報表與 A008 收件信箱功能可用，不因 A013 產生回歸

## 4. Verification

- [x] 4.1 驗證管理員可在「報表設定」找到員工訂餐報表入口
- [x] 4.2 驗證查詢結果可依日期範圍與排序條件正確顯示
- [x] 4.3 驗證 PDF 下載內容與畫面查詢條件一致
- [x] 4.4 驗證既有月結報表觸發、發送記錄與收件信箱設定功能仍正常
