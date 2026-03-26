## Why

目前「部門管理」仍放在管理員的「系統設定」主 TAB 內，雖然功能上可用，但它本質上是高頻主檔維護，而不是單純的系統設定。隨著「員工管理」與「CSV 匯入」都已經被提升成主 TAB，部門管理繼續留在系統設定底下，會讓資訊架構顯得不一致，也增加管理者進入部門維護流程的層級。

既然 A010 的核心就是管理員可查詢、新增、修改部門，而且部門也直接支撐員工建立、員工篩選與 CSV 匯入等流程，那麼部門管理更適合作為獨立主 TAB，而不是系統設定子區塊。

## What Changes

- 將管理員端的「部門管理」從「系統設定」中獨立出來，提升為主 TAB。
- 將部門清單、建立部門、修改部門名稱等既有部門管理內容移到新主 TAB。
- 收斂「系統設定」主 TAB 的內容，讓它聚焦在真正的設定型功能。
- 對齊前端主導覽規格，讓管理員主 TAB 再增加一個「部門管理」。

## Capabilities

### Modified Capabilities

- `frontend-tab-navigation-and-message-box`: 管理員主導覽需新增「部門管理」主 TAB，並將部門維護內容移出系統設定。
- `department-management`: 補上管理員透過主 TAB 進入部門管理畫面的導覽與頁面承載情境。

## Impact

- Affected code: `frontend/src/App.tsx` 的管理員主 TAB 定義、系統設定區塊與部門管理區塊。
- UI: 管理員主導覽會新增「部門管理」TAB；系統設定將移除部門維護內容。
- API: 不新增後端 API，沿用既有 A010 的 `GET` / `POST` / `PATCH` 部門 API。
- Specs: 需同步更新管理員主 TAB 規格與 `department-management` capability 的導覽敘述。
