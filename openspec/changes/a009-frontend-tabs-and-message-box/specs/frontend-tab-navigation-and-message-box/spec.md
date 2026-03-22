## ADDED Requirements

### Requirement: 登入後系統必須依角色顯示對應 TAB 頁籤
系統 SHALL 在使用者登入成功後，依登入角色初始化對應的 TAB 頁籤集合。員工角色 MUST 只看到「訂便當」與「修改密碼」兩個頁籤；管理員角色 MUST 只看到「訂單管理」「供應商管理」「報表設定」「系統設定」四個頁籤。

#### Scenario: 員工登入後看到員工頁籤
- **WHEN** 已驗證的員工登入前端系統
- **THEN** 系統顯示「訂便當」與「修改密碼」TAB，且不顯示管理員頁籤

#### Scenario: 管理員登入後看到管理頁籤
- **WHEN** 已驗證的管理員登入前端系統
- **THEN** 系統顯示「訂單管理」「供應商管理」「報表設定」「系統設定」TAB，且不顯示員工頁籤

### Requirement: 登入成功後系統必須直接導向對應 TAB 主頁
系統 SHALL 在登入成功後依 JWT 或登入回應中的角色資訊直接導向對應的 TAB 主頁。系統 MUST 不再顯示「員工入口」與「管理員入口」選擇頁。員工登入後 MUST 預設顯示「訂便當」頁籤；管理員登入後 MUST 預設顯示「訂單管理」頁籤。

#### Scenario: 員工登入後直接進入員工主頁
- **WHEN** 員工登入成功
- **THEN** 系統直接顯示員工 TAB 主頁，且預設停留在「訂便當」頁籤

#### Scenario: 管理員登入後直接進入管理員主頁
- **WHEN** 管理員登入成功
- **THEN** 系統直接顯示管理員 TAB 主頁，且預設停留在「訂單管理」頁籤

#### Scenario: 登入前不顯示角色入口切換
- **WHEN** 使用者開啟登入頁面但尚未登入
- **THEN** 系統只顯示單一登入表單，不顯示「員工入口」或「管理員入口」切換按鈕

### Requirement: 使用者必須能透過 TAB 切換功能內容而不重整整頁
系統 SHALL 允許已登入使用者點擊 TAB 切換對應功能內容，且切換過程 MUST 在前端單頁狀態內完成，不得重新整理整頁。每個 TAB 對應的內容 MUST 承載既有功能模組，而不是導向空白頁或暫時頁。

#### Scenario: 員工切換到修改密碼
- **WHEN** 員工在登入後點擊「修改密碼」TAB
- **THEN** 系統在不重新整理整頁的情況下顯示修改密碼內容區塊

#### Scenario: 管理員切換到供應商管理
- **WHEN** 管理員在登入後點擊「供應商管理」TAB
- **THEN** 系統在不重新整理整頁的情況下顯示供應商管理內容區塊

### Requirement: 員工在訂便當 TAB 內必須同步看到個人訂單
系統 SHALL 在員工的「訂便當」TAB 中同時顯示下單區與個人訂單清單。下單區 MUST 保留既有 A002 訂餐邏輯；個人訂單區 MUST 同步顯示目前員工的訂單清單，且取消訂單流程 MUST 維持既有的確認 MessageBox 與刪除邏輯。

#### Scenario: 員工在同一畫面查看下單區與我的訂單
- **WHEN** 員工進入「訂便當」TAB
- **THEN** 系統在同一個 TAB 內同時顯示下單區與個人訂單清單

#### Scenario: 員工送出訂單後右側同步顯示結果
- **WHEN** 員工在「訂便當」TAB 完成送單或更新訂單
- **THEN** 系統更新個人訂單清單，讓使用者可在右側立即看到最新訂單結果

#### Scenario: 員工在訂便當 TAB 內取消訂單
- **WHEN** 員工在「訂便當」TAB 右側的個人訂單清單點擊取消
- **THEN** 系統仍先顯示確認 MessageBox，確認後才執行取消請求

### Requirement: 系統必須以 MessageBox 顯示操作成功、失敗與警告提示
系統 SHALL 以統一的 MessageBox 元件顯示操作成功、操作失敗與表單驗證失敗提示。成功情境 MUST 顯示 success 類型訊息；API 或系統錯誤 MUST 顯示 error 類型訊息；前端表單驗證失敗 MUST 顯示 warning 類型訊息。使用者點擊確認按鈕後，MessageBox MUST 關閉。

#### Scenario: 一般操作成功後顯示成功 MessageBox
- **WHEN** 使用者完成新增、修改、查詢或送出等操作且系統回應成功
- **THEN** 系統顯示 success MessageBox，並在使用者按下確認後關閉彈窗

#### Scenario: API 回應錯誤時顯示錯誤 MessageBox
- **WHEN** 使用者送出操作後收到 4xx 或 5xx 錯誤回應
- **THEN** 系統顯示 error MessageBox，內容包含失敗原因，並在使用者按下確認後關閉彈窗

#### Scenario: 前端驗證失敗時顯示警告 MessageBox
- **WHEN** 使用者送出表單但缺少必要欄位或欄位格式不正確
- **THEN** 系統顯示 warning MessageBox，內容說明驗證失敗原因，且不送出後端請求

### Requirement: 刪除類操作必須先經過確認 MessageBox
系統 SHALL 在執行刪除或取消等破壞性操作前先顯示 confirm MessageBox，並提供「確認」與「取消」兩個按鈕。只有在使用者明確按下確認後，系統才可執行實際刪除請求；若使用者取消，系統 MUST 中止該操作且不改變原資料。

#### Scenario: 使用者確認刪除後才執行
- **WHEN** 使用者點擊刪除按鈕並在 confirm MessageBox 中選擇確認
- **THEN** 系統執行刪除請求，並在成功後顯示 success MessageBox

#### Scenario: 使用者取消刪除時不送出請求
- **WHEN** 使用者點擊刪除按鈕並在 confirm MessageBox 中選擇取消
- **THEN** 系統關閉確認彈窗且不送出刪除請求
