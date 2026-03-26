## 1. OpenSpec Alignment

- [x] 1.1 更新 `frontend-tab-navigation-and-message-box`，將員工主 TAB 明確改為包含「修改密碼」的三個頁籤
- [x] 1.2 更新 `employee-authentication`，補上員工與管理員實際進入修改密碼頁面的前端導覽情境

## 2. Frontend Navigation Refactor

- [x] 2.1 在員工主導覽新增「修改密碼」TAB，並保留「訂便當」「我的訂單」
- [x] 2.2 將員工修改密碼表單從「我的訂單」頁搬到新的主 TAB
- [x] 2.3 在管理員「系統設定」加入修改自己密碼區塊，串接管理員修改密碼 API
- [x] 2.4 收斂「我的訂單」頁內容，只保留訂單查詢與取消相關區塊

## 3. Verification

- [x] 3.1 驗證員工登入後看到三個主 TAB，且可直接切到「修改密碼」
- [x] 3.2 驗證員工修改密碼成功後會要求重新登入
- [x] 3.3 驗證管理員可從「系統設定」修改自己的密碼，且成功後會要求重新登入
