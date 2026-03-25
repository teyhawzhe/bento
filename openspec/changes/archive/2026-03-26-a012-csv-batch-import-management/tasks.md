## 1. OpenSpec Alignment

- [x] 1.1 新增 A012 專屬 capability / delta spec，定義 CSV 範本下載、四種批次匯入與成功結果清單契約
- [x] 1.2 在 `employee-account-administration` 補上 A012 員工 CSV 匯入欄位、批次規則與回傳語意
- [x] 1.3 在 `department-management`、`bento-supplier-management`、`bento-menu-management` 補上各自 CSV 匯入與範本下載情境
- [x] 1.4 在 `frontend-tab-navigation-and-message-box` 補上管理員獨立 CSV 匯入 TAB 的導覽要求

## 2. Backend Import Framework

- [x] 2.1 新增 `GET /api/admin/import/template/{type}`，可下載 employees / departments / suppliers / menus 四種 CSV 範本
- [x] 2.2 建立共用 CSV 匯入框架，統一處理 UTF-8 驗證、表頭比對、必填檢查、5000 筆 transaction 批次與 rollback 錯誤回傳
- [x] 2.3 實作員工與部門 CSV 匯入，包含既有資料衝突與同批次重複檢查
- [x] 2.4 實作供應商與菜單 CSV 匯入，包含 business registration number、`supplier_id + name` 與跨表有效性檢查
- [x] 2.5 讓四種匯入成功時回傳本次成功匯入資料清單，並保持 OpenAPI 定義的 `status/data` envelope

## 3. Frontend CSV Import Tab

- [x] 3.1 在管理員後台新增獨立 CSV 匯入 TAB，集中顯示四種資料類型的範本下載與上傳入口
- [x] 3.2 為四種資料類型串接 template download 與 CSV upload API，並顯示成功 / 失敗 MessageBox
- [x] 3.3 在每種資料類型區塊顯示最近一次成功匯入的資料清單，並在資料錯誤時顯示失敗行號與修正提示

## 4. Verification

- [x] 4.1 補上後端 controller / service 測試，涵蓋範本下載、表頭錯誤、資料驗證失敗 rollback 與成功匯入
- [x] 4.2 驗證四種 CSV 在 UTF-8、必填欄位與重複資料規則下都符合 A012 / OpenAPI
- [x] 4.3 驗證前端 CSV 匯入 TAB 可下載範本、上傳 CSV、顯示成功清單，且不影響既有管理員 tab 流程
