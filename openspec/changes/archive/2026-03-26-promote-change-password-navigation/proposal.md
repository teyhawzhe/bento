## Why

最新版 A001 明確要求員工與系統管理員都必須有「修改自己的密碼」頁面，但目前前端導覽沒有完整反映這件事。員工端雖然仍有修改密碼功能，卻被放在「我的訂單」頁內的次要區塊；管理員端目前也缺少清楚可見的修改自己密碼入口。這讓功能並非真正消失，卻很容易讓使用者以為它被拿掉。

這次需要補一個導覽層級的 change，把「修改密碼」從隱藏區塊提升成更清楚的入口，先滿足 A001 的頁面可達性，再避免管理員主 TAB 無限制膨脹。

## What Changes

- 將員工端的「修改密碼」從「我的訂單」頁內區塊提升成獨立主 TAB。
- 在管理員端補上修改自己密碼入口，但先放在「系統設定」內，而不是再增加新的管理員主 TAB。
- 更新前端主導覽規格與認證規格，讓「修改密碼頁面」的可達性與目前 A001 需求一致。

## Capabilities

### Modified Capabilities

- `frontend-tab-navigation-and-message-box`: 員工主導覽需加入獨立的「修改密碼」TAB，並定義管理員在系統設定中的密碼修改入口。
- `employee-authentication`: 補上員工與管理員在前端實際進入修改密碼頁面的導覽情境。

## Impact

- Affected code: `frontend/src/App.tsx` 的員工 TAB 定義、員工「我的訂單」頁與管理員系統設定頁。
- UI: 員工端主 TAB 會從 2 個變成 3 個；管理員端不新增主 TAB，但系統設定會補上修改自己密碼區塊。
- API: 不新增 API，沿用 `PATCH /api/auth/change-password` 與 `PATCH /api/admin/auth/change-password`。
- Specs: 需同步更新前端導覽與認證主規格。
