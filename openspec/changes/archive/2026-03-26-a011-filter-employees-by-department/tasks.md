## 1. OpenSpec Alignment

- [x] 1.1 在 `employee-account-administration` delta spec 補上 `GET /api/admin/employees` 支援可選 `department_id` 的需求與情境
- [x] 1.2 在 `department-management` delta spec 補上 `GET /api/admin/departments` 可作為員工篩選下拉資料來源的情境

## 2. Backend Employee Filtering

- [x] 2.1 在 A001 員工查詢 controller 加入 `department_id` query parameter 解析
- [x] 2.2 在 service / repository 實作依部門篩選員工，未帶參數時回傳全部員工
- [x] 2.3 確保員工清單回應仍符合既有 `status/data` envelope，且不影響其他員工管理 API

## 3. Frontend Employee List Filtering

- [x] 3.1 在員工管理頁載入 `GET /api/admin/departments`，渲染含「全部」的部門下拉選單
- [x] 3.2 切換部門時以 query parameter 重新呼叫 `GET /api/admin/employees`
- [x] 3.3 切回「全部」時不帶 `department_id`，並重新顯示完整員工清單

## 4. Verification

- [x] 4.1 補上後端員工清單 API 測試，涵蓋未帶參數與指定部門兩種情境
- [x] 4.2 補上 service / repository 篩選測試，確認只回傳指定部門員工
- [x] 4.3 驗證前端部門下拉與員工清單互動流程可正常切換與回復全部資料
