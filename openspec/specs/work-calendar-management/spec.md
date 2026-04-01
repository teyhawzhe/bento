## Purpose
定義管理員查詢、編輯、預產與匯入上班日行事曆時，需遵循的 OpenAPI 契約與前後端互動邊界。

## Requirements
### Requirement: 管理員可查詢指定年月的上班日行事曆
系統 SHALL 提供 `GET /api/admin/calendar?year={year}&month={month}`，讓已驗證的管理員查詢指定年月的行事曆資料。成功回應 MUST 符合 OpenAPI 的 `status/data` envelope，且 `data` 中每筆資料 MUST 包含 `date` 與 `is_workday`。

#### Scenario: 管理員查詢指定年月行事曆成功
- **WHEN** 管理員以有效的 `year` 與 `month` 呼叫 `GET /api/admin/calendar`
- **THEN** 系統以 `status=success` 回傳該年月的 `WorkCalendarDay` 陣列

### Requirement: 管理員可編輯當月行事曆並儲存異動
系統 SHALL 允許管理員進入行事曆編輯模式，切換當前月份日期的 `is_workday` 狀態，並透過 `PUT /api/admin/calendar` 儲存異動。更新請求 MUST 以 `WorkCalendarDay[]` 作為 request body，且成功後 MUST 回傳 OpenAPI 定義的成功 envelope。

#### Scenario: 管理員儲存當月異動
- **WHEN** 管理員在編輯模式調整一個或多個日期後呼叫 `PUT /api/admin/calendar`
- **THEN** 系統更新對應日期的 `is_workday` 狀態，並以 `status=success` 回傳成功結果

#### Scenario: 切換月份前有未儲存異動
- **WHEN** 管理員在編輯模式中已修改當前月份資料且尚未執行更新
- **THEN** 系統 MUST 提醒「尚有修改未更新，請先按更新」，且不得直接切換到其他月份

### Requirement: 管理員可預產整年行事曆並覆蓋既有資料
系統 SHALL 提供 `POST /api/admin/calendar/generate`，讓管理員以指定 `year` 預產整年行事曆。確認執行後，系統 MUST 以週六、週日為非上班日，其餘日期為上班日，並覆蓋該年份既有資料。

#### Scenario: 管理員確認預產整年行事曆
- **WHEN** 管理員確認執行 `POST /api/admin/calendar/generate` 並提供有效年份
- **THEN** 系統覆蓋該年份的行事曆資料，且將週六、週日標記為 `is_workday=false`

### Requirement: 管理員可透過 CSV 預覽後確認匯入行事曆
系統 SHALL 提供 `POST /api/admin/calendar/import`，支援 `yyyymmdd,y/n` 格式 CSV 上傳，並以 `confirm=false` 先回傳預覽資料、`confirm=true` 再正式套用匯入結果。每筆預覽或匯入結果 MUST 轉換為 `WorkCalendarDay` 格式回傳。

#### Scenario: 管理員上傳 CSV 取得預覽結果
- **WHEN** 管理員上傳格式正確的行事曆 CSV，且以 `confirm=false` 呼叫 `POST /api/admin/calendar/import`
- **THEN** 系統解析 CSV 並以 `status=success` 回傳預覽用的 `WorkCalendarDay` 陣列，而不更新資料庫

#### Scenario: 管理員確認 CSV 匯入
- **WHEN** 管理員以上傳檔案內容呼叫 `POST /api/admin/calendar/import` 且 `confirm=true`
- **THEN** 系統批次更新對應日期的 `is_workday` 狀態，並以 `status=success` 回傳匯入結果
