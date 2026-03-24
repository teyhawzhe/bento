## Context

目前員工管理僅支援以 `username`、`name`、`email` 建立單一帳號或批次匯入帳號，員工資料沒有正式的部門關聯欄位。同時，A010 也尚未定義完整的部門維護後台 CRUD，導致部門資料無法由管理者直接建立、修改或停用。這讓 A010 部門主檔無法與 A001 員工管理流程連動，也使管理員在員工清單中無法直接辨識員工所屬部門。

這次變更同時影響部門主檔 CRUD、員工建立流程、CSV 匯入格式、員工清單查詢與前端顯示，因此需要先定義資料流、管理邊界與驗證策略，避免後續實作時前後端欄位名稱不一致。

## Goals / Non-Goals

**Goals:**
- 定義 A010 完整的部門維護後台 CRUD 與資料約束
- 定義 A010 部門資料如何提供給員工建立與顯示流程使用
- 讓單一新增員工與 CSV 匯入都必須帶入有效 department
- 讓員工清單與建立結果能顯示 `department.name`
- 明確定義前後端 payload、DTO 與 CSV 欄位規則

**Non-Goals:**
- 不調整員工登入、JWT 欄位或權限模型
- 不處理歷史員工資料的大量資料清洗策略以外的組織重編邏輯

## Decisions

### 1. 部門以獨立 capability 管理，並在此 change 內定義完整 CRUD
- 採用新增 `department-management` capability 的方式，定義部門主檔新增、查詢、修改、刪除/停用規則，避免把部門需求混入員工管理 spec。
- 員工建立與列表功能依賴 department-management 提供的有效部門資料，不自行維護部門主檔。
- 替代方案是直接把部門欄位寫進 `employee-account-administration` 而不新增 capability，但這會讓 A010 的責任邊界不清楚。

### 2. 建立與匯入員工都以 department id 作為寫入欄位，畫面顯示 department.name
- 後端寫入時以穩定識別值 `departmentId` 關聯員工與部門。
- 前端表單與列表則顯示 `department.name`，避免管理者需要記憶部門 id。
- CSV 匯入採用明確欄位 `department`，其值對應部門 id；若未來需要支援部門名稱匯入，可在後續 change 擴充。
- 替代方案是 CSV 直接用部門名稱比對，但名稱唯一性與大小寫/空白規則會提高匯入歧義。

### 3. 部門刪除採保守策略，避免破壞既有員工關聯
- 若部門已被員工資料引用，系統 SHOULD 拒絕硬刪除，或改以停用/不可選取方式處理，避免歷史員工資料失聯。
- 後台 CRUD 畫面需區分「可選用中的部門」與「已停用但仍被引用的部門」。
- 替代方案是允許直接刪除並將員工 department 清空，但這會造成員工資料語意不完整。

### 4. 員工查詢回傳展開的 department 摘要物件
- `GET /api/admin/employees`、單一建立成功回應、CSV 匯入成功項目都應回傳可供 UI 顯示的 department 摘要，至少包含 `id` 與 `name`。
- 前端不再自行拼接或額外查單筆部門名稱，降低畫面載入時的 API 次數。
- 替代方案是僅回傳 `departmentId`，由前端自行對照部門清單，但這會增加同步複雜度與畫面耦合。

## Risks / Trade-offs

- [既有員工資料缺少 department] → 需要在實作時決定 migration 預設值或補值策略，避免新查詢回傳不完整資料
- [部門被刪除後影響既有員工資料] → 對已被引用部門採拒絕刪除或停用策略，避免破壞員工關聯
- [CSV 來源檔未同步新增 department 欄位] → 在匯入驗證中明確回報缺欄或部門不存在的錯誤
- [部門名稱變更造成前端顯示差異] → 以前端一律依後端即時回傳的 `department.name` 為準
- [前後端欄位命名不一致] → 在 spec 中固定 `departmentId`（寫入）與 `department.name`（顯示）責任
