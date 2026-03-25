## Context

`A011.md` 定義了一個小而明確的擴充：管理員在員工清單頁面可用部門下拉選單篩選員工，選擇「全部」時回傳全部員工，選擇特定部門時只顯示該部門員工。`../uml/openapi.yaml` 也已將這個能力合併在 `GET /api/admin/employees`，以可選的 `department_id` 查詢參數表示。因此這次 change 的核心不是新增新 API，而是把 A011 對 A001 / A010 的交界補齊，讓員工清單查詢、部門清單提供與前端篩選互動有一致定義。

目前 OpenSpec 主規格中，`employee-account-administration` 只保證員工清單回應 envelope 與其他管理能力；`department-management` 則只描述部門清單可供後台維護與員工建立流程使用，尚未點出 A011 的員工篩選用途。這代表若直接進入實作，後端可能忽略 `department_id`，或前端自行在本地過濾既有列表，兩者都不符合 UML 與 OpenAPI 期望，因此需要先用 design 固定設計邊界。

## Goals / Non-Goals

**Goals:**
- 讓 `GET /api/admin/employees` 明確支援可選 `department_id` 參數。
- 明確定義管理員頁面會先載入部門清單，並以「全部 + 部門列表」作為篩選選單。
- 保持未帶 `department_id` 時回傳全部員工，帶入有效部門 ID 時僅回傳該部門員工。
- 讓 A011 與既有 A001 員工管理查詢、A010 部門清單契約保持一致。

**Non-Goals:**
- 不新增新的員工查詢 API path。
- 不新增資料表、欄位或索引。
- 不變更員工建立、修改、停用、重設密碼等其他 A001 能力。
- 不引入前端本地端快取或複雜搜尋條件組合。

## Decisions

### 1. 以既有 `GET /api/admin/employees` 承載全部與篩選兩種查詢語意
- 理由：OpenAPI 已明確定義 `department_id` 為可選 query parameter，A011 也說明不帶參數時回傳全部，帶參數時依部門篩選。沿用同一個 endpoint 可避免重複 API。
- 替代方案：新增 `/api/admin/departments/{id}/employees`。未採用，因為這不在 UML 與 OpenAPI 契約內，且會讓 A001 / A011 契約分裂。

### 2. 「全部」由前端以不帶 `department_id` 呼叫 API 表示
- 理由：A011 流程已明確指定「全部」時呼叫 `GET /api/admin/employees`，因此不應以 `department_id=0`、空字串或其他哨兵值代表全部。
- 替代方案：傳固定特殊值代表全部。未採用，因為會擴張契約，增加後端解讀分支。

### 3. 部門選單資料一律來自 `GET /api/admin/departments`
- 理由：A011 指定這支 API 供下拉選單使用；而 `department-management` 既有規格也已定義管理員可查詢部門清單。重用同一資料來源可避免前端硬編碼部門名稱。
- 替代方案：從員工清單反推部門。未採用，因為無法保證完整部門主檔，也會漏掉目前尚無員工的部門。

### 4. 篩選責任在後端，不在前端本地列表過濾
- 理由：A011 流程明確要求前端帶 query parameter 呼叫 API，由後端依 `department_id` 篩選後回傳。這樣可以保證資料來源一致，避免大型清單被前端自行過濾造成權責不清。
- 替代方案：先抓全部員工後由前端過濾。未採用，因為不符合需求流程，也會讓資料量與一致性風險增加。

## Risks / Trade-offs

- [現有後端若尚未實作 `department_id` 篩選，OpenSpec 與實作會短期不一致] → tasks 需明確拆出 controller / service / repository 與驗證測試。
- [前端若把「全部」實作成傳空字串，可能造成 query parser 行為不穩定] → 明確規定全部狀態不帶 `department_id`。
- [部門清單與員工清單載入順序不同步，可能讓篩選 UI 閃動] → 前端先載入部門選單，再依目前選定值查詢或重新查詢員工。
- [若傳入不存在的部門 ID，行為可能被不同層各自解讀] → 實作時應統一決定是回傳空陣列或驗證錯誤；基於目前 A011 / OpenAPI 未額外定義錯誤契約，先以回傳空陣列為預設設計假設。

## Migration Plan

- 先更新 `employee-account-administration` 與 `department-management` 的 delta specs，固定 A011 契約。
- 後端補上 `department_id` 的查詢參數解析與 repository 篩選條件。
- 前端在員工管理頁加入部門下拉選單，支援切換部門後重新查詢。
- 補上 API、service 與前端流程測試，確認切回「全部」時仍可取得完整員工清單。

## Open Questions

- 傳入不存在的 `department_id` 時，是否要回傳空陣列或 `400`：目前先依需求最小化假設為空陣列，若後續 UML 補充錯誤語意，再同步調整。
- 員工清單頁是否已有其他查詢條件需要與部門篩選組合：目前依 A011 假設只有部門單一篩選條件。
