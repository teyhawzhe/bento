## Context

目前系統中的「報表設定」主 TAB 主要承載 A005 月結報表觸發、發送記錄與 A008 收件信箱設定，性質偏向帳單報表與寄送管理。UML 的 A013 則是另一條獨立需求，目標是讓管理員查詢員工訂餐明細，並支援 PDF 匯出。這代表系統需要補上一條新的查詢型報表能力，而不是去擴充 `monthly_billing_logs` 或複用 A005 的資料模型。

A013 的資料來源來自既有訂單與主檔關聯：`orders JOIN employees JOIN departments JOIN menus JOIN suppliers`。因此本 change 不新增資料表，而是在現有資料基礎上新增查詢 API、前端報表畫面與 PDF 輸出。

## Goals / Non-Goals

**Goals:**
- 在管理員「報表設定」主 TAB 中提供 A013 員工訂餐報表查詢入口。
- 支援日期起訖、排序條件與畫面預覽。
- 支援以相同查詢條件下載 PDF。
- 釐清 A013 與 A005 月結報表在 API、資料來源與畫面位置上的差異。

**Non-Goals:**
- 不新增新的管理員主 TAB。
- 不改變 A005 月結報表的寄送流程或 `monthly_billing_logs` 結構。
- 不新增新的資料表。
- 不在本 change 內擴充 XLSX、CSV 匯出等額外格式。

## Decisions

### 1. A013 放在既有「報表設定」主 TAB 內
- 決策：不新增新的管理員主 TAB，而是在 `admin-reports` 頁下新增「員工訂餐報表」區塊或次分頁。
- 理由：A013 與 A005/A008 同屬報表領域，集中在既有報表入口較符合目前資訊架構，也避免管理員主導覽持續膨脹。
- 替代方案：新增獨立主 TAB「訂餐報表」。未採用，因為目前主導覽已包含多個業務面主 TAB。

### 2. A013 建立獨立 capability，而不是併入月結報表 spec
- 決策：新增 `employee-order-reporting` spec，並只在 `monthly-billing-reporting` 補上邊界說明。
- 理由：A013 是查詢型報表與 PDF 匯出，A005 是月結帳單產生與寄送記錄，兩者使用情境、資料格式與 API 都不同。
- 替代方案：直接把 A013 併入 `monthly-billing-reporting`。未採用，因為會讓 spec 職責過度混雜。

### 3. JSON 預覽與 PDF 匯出共用同一組查詢條件
- 決策：`date_from`、`date_to`、`sort_by` 在預覽與 PDF 下載時完全一致。
- 理由：避免前端出現「畫面看到一份、下載得到另一份」的不一致體驗，也可降低後端邏輯分歧。
- 替代方案：PDF 使用獨立參數或固定排序。未採用，因為會增加理解與維護成本。

### 4. 排序選項先對齊 UML，使用有限 enum
- 決策：排序先固定支援 `date`、`department`、`employee`、`supplier` 四種選項，預設為 `date`。
- 理由：這與 UML A013 一致，也能讓前後端契約保持簡單明確。
- 替代方案：開放任意欄位排序。未採用，因為超出目前需求且容易增加查詢與驗證複雜度。

## Risks / Trade-offs

- [同一個「報表設定」頁同時放 A005 與 A013，若版面分區不清楚，使用者容易混淆] → 實作時需用明確標題或次分頁切開「員工訂餐報表」與「月結報表」。
- [PDF 產生需要引入新的後端輸出能力] → 先採最小可行的伺服器端 PDF 產法，重點放在內容正確與可下載。
- [若 `openapi.yaml` 尚未完整補上 A013 路徑，OpenSpec 與 UML 仍可能短暫不同步] → 本 change 應同步檢查 OpenAPI 與主 specs 契約。

## Migration Plan

- 新增 `employee-order-reporting` 主規格，定義 JSON 預覽與 PDF 下載契約。
- 更新前端導覽 spec，明確說明管理員「報表設定」主 TAB 內含多類報表能力。
- 補充 `monthly-billing-reporting` spec，強調 A005 與 A013 為不同 API。
- 實作後端查詢與 PDF API，再擴充前端 `admin-reports` 畫面。
- 驗證查詢結果、排序、PDF 下載與既有 A005 區塊共存。

## Open Questions

- PDF 的版型是否只需表格列印版即可。目前先假設以簡潔列印格式為主，不做品牌化排版。
