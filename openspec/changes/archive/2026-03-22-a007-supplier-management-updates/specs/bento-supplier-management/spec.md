## ADDED Requirements

### Requirement: 管理員可以查詢供應商清單
系統 SHALL 允許已驗證的管理員查詢供應商清單。系統 MUST 支援以 `name` 與 `search_type` 作為查詢條件，其中 `search_type` 為 `exact` 時必須做名稱完全比對，為 `fuzzy` 時必須做名稱關鍵字模糊查詢；若未提供名稱條件，系統 MUST 回傳全部供應商清單。

#### Scenario: 管理員查詢全部供應商
- **WHEN** 管理員未提供名稱條件呼叫供應商清單查詢功能
- **THEN** 系統回傳全部供應商清單

#### Scenario: 管理員以精確名稱查詢供應商
- **WHEN** 管理員提供 `name` 與 `search_type=exact` 呼叫供應商清單查詢功能
- **THEN** 系統僅回傳名稱完全符合的供應商資料

#### Scenario: 管理員以模糊名稱查詢供應商
- **WHEN** 管理員提供 `name` 與 `search_type=fuzzy` 呼叫供應商清單查詢功能
- **THEN** 系統回傳名稱包含關鍵字的供應商資料

### Requirement: 管理員可以查詢單一供應商詳細資料
系統 SHALL 允許已驗證的管理員以供應商編號查詢單一供應商詳細資料，供管理頁顯示完整內容與後續編輯使用。

#### Scenario: 管理員成功查詢單一供應商
- **WHEN** 管理員以存在的供應商編號呼叫單一供應商查詢功能
- **THEN** 系統回傳該供應商的完整資料

#### Scenario: 查詢不存在的供應商失敗
- **WHEN** 管理員以不存在的供應商編號呼叫單一供應商查詢功能
- **THEN** 系統拒絕請求並提示查無供應商

### Requirement: 管理員可以修改供應商可編輯欄位
系統 SHALL 允許已驗證的管理員修改供應商的 `name`、`email`、`phone`、`contact_person`、`is_active`。系統 MUST 禁止修改 `id` 與 `business_registration_no`，且欄位格式驗證失敗時 MUST 拒絕更新。

#### Scenario: 管理員成功修改供應商資料
- **WHEN** 管理員提交合法的可編輯欄位更新內容
- **THEN** 系統更新供應商資料並回傳修改結果

#### Scenario: 管理員嘗試修改唯讀欄位被拒絕
- **WHEN** 管理員嘗試修改供應商的 `business_registration_no` 或 `id`
- **THEN** 系統拒絕請求並維持原始資料不變

#### Scenario: 管理員提交無效格式資料更新失敗
- **WHEN** 管理員提交格式錯誤的 `email` 或其他不合法欄位內容
- **THEN** 系統拒絕請求並提示欄位驗證失敗
