## ADDED Requirements

### Requirement: 管理員取消指定員工訂單必須遵守 16:30 截止規則
系統 SHALL 對 `DELETE /api/admin/orders/{id}` 套用逐筆訂單的取消截止時間。管理員取消指定員工訂餐的截止時間 MUST 為該訂餐日前一日 16:30，也就是 A003 排程執行前 30 分鐘；超過截止時間後，系統 MUST 拒絕取消請求。

#### Scenario: 管理員在截止前取消員工訂單
- **WHEN** 管理員送出 `DELETE /api/admin/orders/{id}`，且當前時間仍在該訂餐日前一日 16:30 之前
- **THEN** 系統刪除指定員工訂單並回傳成功結果

#### Scenario: 管理員在截止後無法取消員工訂單
- **WHEN** 管理員送出 `DELETE /api/admin/orders/{id}`，且當前時間已超過該訂餐日前一日 16:30
- **THEN** 系統 MUST 拒絕請求，且不得刪除該筆訂單
