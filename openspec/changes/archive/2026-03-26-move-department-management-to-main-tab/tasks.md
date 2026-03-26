## 1. OpenSpec Alignment

- [x] 1.1 更新 `frontend-tab-navigation-and-message-box`，將管理員主 TAB 明確改為包含「部門管理」的七個主頁籤
- [x] 1.2 更新 `department-management`，補上管理員透過「部門管理」主 TAB 進入查詢、新增與修改流程的情境

## 2. Frontend Navigation Refactor

- [x] 2.1 在管理員主導覽新增「部門管理」TAB，並維持既有登入後預設主 TAB 不變
- [x] 2.2 將部門管理的建立與修改區塊從「系統設定」搬到「部門管理」主 TAB
- [x] 2.3 收斂「系統設定」內容，移除部門管理區塊並保留設定型功能

## 3. Verification

- [x] 3.1 驗證管理員登入後看到七個主 TAB，且可直接切到「部門管理」
- [x] 3.2 驗證「部門管理」TAB 中的查詢、新增、修改流程皆正常
- [x] 3.3 驗證「系統設定」移除部門管理後，剩餘設定功能與其他主 TAB 不受影響
