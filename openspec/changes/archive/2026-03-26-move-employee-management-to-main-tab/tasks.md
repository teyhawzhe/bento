## 1. OpenSpec Alignment

- [x] 1.1 更新 `frontend-tab-navigation-and-message-box`，將管理員主 TAB 明確改為包含「員工管理」的六個主頁籤
- [x] 1.2 更新 `employee-account-administration`，補上管理員透過「員工管理」主 TAB 進入建立、查詢與維護員工的情境

## 2. Frontend Navigation Refactor

- [x] 2.1 在管理員主導覽新增「員工管理」TAB，並維持既有登入後預設主 TAB 不變
- [x] 2.2 將新增員工表單從「系統設定」子頁搬到「員工管理」主 TAB
- [x] 2.3 將員工清單、部門篩選、編輯、啟用/停用與重設密碼操作搬到「員工管理」主 TAB
- [x] 2.4 收斂「系統設定」內容，移除員工帳號工具相關子頁與不再需要的 state

## 3. Verification

- [x] 3.1 驗證管理員登入後看到六個主 TAB，且可直接切到「員工管理」
- [x] 3.2 驗證「員工管理」TAB 中的新增、查詢、篩選、編輯、啟停與重設密碼流程皆正常
- [x] 3.3 驗證「系統設定」移除員工帳號工具後，剩餘設定功能與 CSV 匯入主 TAB 不受影響
