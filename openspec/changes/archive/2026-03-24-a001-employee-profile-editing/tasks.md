## 1. Backend API / Service

- [x] 1.1 新增 `PATCH /api/admin/employees/{id}` controller 與 request DTO
- [x] 1.2 在 service 層實作員工資料更新邏輯，支援 `username`、`name`、`email`、`departmentId`、`isAdmin`
- [x] 1.3 補上更新時的唯一性驗證，確保 `username`、`email` 只與其他員工比較
- [x] 1.4 驗證更新時 `departmentId` 必須指向有效部門

## 2. Frontend Employee Management

- [x] 2.1 在員工管理畫面加入編輯入口與表單
- [x] 2.2 顯示唯讀 `id`，並讓管理員可編輯 `username`、`name`、`email`、`department`、`isAdmin`
- [x] 2.3 送出更新成功後重新載入員工清單並顯示結果訊息

## 3. Verification

- [x] 3.1 補上 A001 controller 的員工更新 API 測試
- [x] 3.2 補上員工 service 更新邏輯的唯一性與部門驗證測試
- [x] 3.3 驗證前端員工編輯流程與既有停用 / 重設密碼流程可共存
