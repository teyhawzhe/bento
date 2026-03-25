## 1. Deadline And Date Window Alignment

- [x] 1.1 盤點現有員工訂餐與管理員取消流程中使用的截止時間與可訂日期計算，確認需調整的共用 helper 或 service
- [x] 1.2 實作員工新增/修改訂單使用本週星期五 12:00 的截止判斷，並套用到 `POST /api/orders` 與 `PATCH /api/orders/{id}`
- [x] 1.3 實作員工與管理員共用的「訂餐日前一日 16:30」取消截止判斷，並套用到 `DELETE /api/orders/{id}` 與 `DELETE /api/admin/orders/{id}`
- [x] 1.4 調整員工可訂菜單查詢邏輯，讓 `GET /api/orders/menu` 只回傳 A002 最新規格可訂區間內且仍在有效期間內的菜單，並保留週末有效菜單

## 2. Frontend Ordering Behavior

- [x] 2.1 更新員工訂餐畫面，使截止後禁止新增與修改，並顯示對應提示
- [x] 2.2 更新員工個人訂單清單，依各筆 `order_date` 隱藏已逾期訂單的取消按鈕，並在送出取消前再做一次前端時間檢查
- [x] 2.3 更新管理員訂單管理畫面，使已超過 16:30 截止的訂單不可取消，並顯示明確錯誤回饋

## 3. API And Contract Verification

- [x] 3.1 確認員工端 `GET /api/orders/menu`、`GET /api/orders/me` 在調整日期與截止邏輯後仍符合 `status/data` 契約且不回傳價格欄位
- [x] 3.2 對齊 `GET /api/menus` 與管理員菜單頁，移除或停用 `include_history` /「顯示歷史菜單」這類已不屬於最新版 A002 的查詢語意
- [x] 3.3 確認管理員端 `DELETE /api/admin/orders/{id}` 在期限內/逾期兩種情境下的 API 回應與錯誤碼符合規格
- [x] 3.4 盤點並清理這輪審計發現的非契約殘留，例如前端 `deleteDepartment` helper 或 README 中仍引用舊語意的內容
- [x] 3.5 補齊或更新前後端測試，覆蓋星期五 12:00、前一日 16:30、週末有效菜單、逾期取消 403 與菜單查詢不再依賴歷史切換等情境

## 4. Final Validation

- [x] 4.1 執行相關 backend 與 frontend 驗證，確認員工訂餐、員工取消、管理員取消流程沒有回歸
- [x] 4.2 更新必要的開發文件或操作說明，讓團隊知道 A002 對齊後的最新截止規則與測試重點
