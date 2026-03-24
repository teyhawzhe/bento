## ADDED Requirements

### Requirement: 月結報表收件信箱 API 必須符合 OpenAPI 契約
系統 SHALL 讓報表收件信箱的查詢、新增與刪除 API 以 `status/data` 格式回傳，並作為 A005 月結報表發信時的管理端收件來源。

#### Scenario: 查詢報表收件信箱符合 envelope
- **WHEN** 管理員成功呼叫 `GET /api/settings/report-emails`
- **THEN** 系統以 `status=success` 與 `data` 陣列回傳報表收件信箱清單

#### Scenario: 月結報表發信使用既有收件清單
- **WHEN** 系統執行月結報表發送
- **THEN** 系統將每筆報表同步寄送給供應商與報表收件信箱清單中的所有信箱

