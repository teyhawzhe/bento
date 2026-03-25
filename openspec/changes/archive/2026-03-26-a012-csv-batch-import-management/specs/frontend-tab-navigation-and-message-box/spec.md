## MODIFIED Requirements

### Requirement: 管理員主導覽需提供與系統設定同層級的 CSV 匯入 TAB
系統 SHALL 在管理員後台主導覽提供與「系統設定」同層級的 CSV 匯入 TAB，集中顯示 employees、departments、suppliers、menus 四種資料類型的範本下載與上傳入口。切換到該 TAB 時 MUST 不重新整理整頁。

#### Scenario: 管理員切換到 CSV 匯入主 TAB
- **WHEN** 管理員在主導覽中點擊 CSV 匯入頁籤
- **THEN** 系統在單頁狀態中顯示四種資料類型的範本下載與上傳區塊

### Requirement: CSV 匯入流程需以 MessageBox 呈現成功與失敗結果
系統 SHALL 在管理員執行 CSV 匯入後，以既有 MessageBox 顯示成功、格式錯誤或資料驗證失敗提示。若匯入成功，畫面 MUST 同時顯示最近一次匯入成功資料清單；若匯入失敗，畫面 MUST 顯示失敗行號與修正提示。

#### Scenario: 匯入成功後顯示 success MessageBox 與成功清單
- **WHEN** 管理員成功完成任一資料類型的 CSV 匯入
- **THEN** 系統顯示 success MessageBox，並在對應區塊顯示最近一次成功匯入資料清單

#### Scenario: 匯入失敗後顯示 error MessageBox 與失敗行號
- **WHEN** 管理員上傳的 CSV 發生表頭錯誤或批次驗證失敗
- **THEN** 系統顯示 error MessageBox，並在畫面中提示失敗行號與錯誤原因
