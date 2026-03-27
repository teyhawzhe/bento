## Purpose
定義 A013 員工訂餐報表的查詢、排序與 PDF 下載契約。

## Requirements
### Requirement: 管理員可查詢員工訂餐報表預覽
系統 SHALL 提供 `GET /api/admin/reports/orders`，讓管理員以 `date_from`、`date_to` 與 `sort_by` 查詢員工訂餐報表，並以 `status=success` 與 `data` 陣列回傳報表資料。每筆資料 MUST 包含 `order_date`、`department_name`、`employee_name`、`menu_name`、`supplier_name`。

#### Scenario: 管理員依日期區間預覽報表
- **WHEN** 管理員以有效的日期起訖呼叫 `GET /api/admin/reports/orders`
- **THEN** 系統回傳符合日期區間的員工訂餐資料預覽

#### Scenario: 管理員以部門排序預覽報表
- **WHEN** 管理員以 `sort_by=department` 呼叫 `GET /api/admin/reports/orders`
- **THEN** 系統依部門名稱排序回傳資料

### Requirement: 管理員可下載與預覽一致的 PDF 報表
系統 SHALL 提供 `GET /api/admin/reports/orders/pdf`，以與 JSON 預覽相同的 `date_from`、`date_to`、`sort_by` 條件產生 PDF 報表並回傳下載檔案。

#### Scenario: 管理員下載 PDF
- **WHEN** 管理員以有效的日期起訖與排序條件呼叫 `GET /api/admin/reports/orders/pdf`
- **THEN** 系統回傳 PDF 檔案，且內容條件與畫面預覽一致

### Requirement: 員工訂餐報表排序選項受限且有預設值
系統 SHALL 僅接受 `date`、`department`、`employee`、`supplier` 四種排序條件；當未提供 `sort_by` 時 MUST 預設為 `date`。若日期區間不完整、起訖顛倒或排序條件不支援，系統 MUST 拒絕請求並回傳錯誤。

#### Scenario: 未提供 sort_by 時使用日期排序
- **WHEN** 管理員呼叫 A013 查詢 API 且未帶 `sort_by`
- **THEN** 系統以日期排序回傳結果

#### Scenario: 不支援的排序條件被拒絕
- **WHEN** 管理員以不支援的 `sort_by` 呼叫 A013 API
- **THEN** 系統回傳錯誤訊息並拒絕請求
