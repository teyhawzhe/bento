## 1. Department CRUD Backend

- [x] 1.1 新增部門主檔的建立、查詢、修改、刪除/停用 API 與 service
- [x] 1.2 定義部門 DTO，確保至少包含 `id`、`name` 與管理所需狀態欄位
- [x] 1.3 補上部門名稱唯一性與被員工引用時不可刪除的驗證

## 2. Employee Create / Import Backend

- [x] 2.1 修改單一新增員工 API，要求 `departmentId` 為必填並驗證部門存在
- [x] 2.2 修改員工 CSV 匯入流程，要求 `department` 欄位並驗證每列部門值
- [x] 2.3 修改員工查詢與建立回應 DTO，回傳 `department.name`

## 3. Frontend Department / Employee Management

- [x] 3.1 新增部門管理畫面，支援部門清單、建立、編輯與刪除/停用操作
- [x] 3.2 在新增員工表單載入部門下拉選單並送出 `departmentId`
- [x] 3.3 調整 CSV 匯入說明與格式，讓管理者能提供 `department` 欄位
- [x] 3.4 在員工清單與建立結果畫面顯示 `department.name`

## 4. Verification

- [x] 4.1 補上部門 CRUD 與刪除限制的前後端測試
- [x] 4.2 補上單一新增員工與 CSV 匯入含 department 的驗證測試
- [x] 4.3 驗證部門管理畫面、員工清單與建立結果都能正確顯示部門名稱
