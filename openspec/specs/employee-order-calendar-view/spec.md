## Requirements
### Requirement: 員工可查詢指定年月的訂餐行事曆基礎資料
系統 SHALL 提供 `GET /api/calendar?year={year}&month={month}`，讓已驗證的員工查詢指定年月的上班日行事曆資料，供 A002 訂餐頁渲染月視圖。此 API MUST 以 A014 `work_calendar` 作為資料來源，並以 `status=success` 與 `data` 陣列回傳；每筆資料 MUST 至少包含 `date` 與 `is_workday`。

#### Scenario: 員工查詢指定年月行事曆成功
- **WHEN** 已驗證的員工以有效的 `year` 與 `month` 呼叫 `GET /api/calendar`
- **THEN** 系統以 `status=success` 回傳該年月的 `date` / `is_workday` 陣列

### Requirement: 員工訂餐行事曆必須依上班日與訂單狀態顯示日期格
系統 SHALL 讓員工端以月行事曆視圖呈現 A002 訂餐狀態。日期格 MUST 依 A014 上班日資料與員工當月訂單資料呈現三種狀態：`is_workday=false` 的日期為非上班日且不可互動；上班日且當日無訂單時顯示空白可訂狀態；上班日且當日已有訂單時顯示已訂便當名稱並允許進一步管理。

#### Scenario: 非上班日顯示為不可互動
- **WHEN** 員工端行事曆中的某日期 `is_workday=false`
- **THEN** 系統將該日期顯示為非上班日，且不得提供訂餐、修改或取消互動

#### Scenario: 上班日無訂單顯示為可訂日期
- **WHEN** 員工端行事曆中的某日期為上班日，且該員工於當日尚無有效訂單
- **THEN** 系統將該日期顯示為可點擊訂餐的空白日期格

#### Scenario: 上班日已有訂單顯示已訂便當名稱
- **WHEN** 員工端行事曆中的某日期為上班日，且該員工於當日已有有效訂單
- **THEN** 系統在日期格中顯示已訂便當名稱，並允許點擊進入管理互動
