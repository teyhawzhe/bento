## Why

目前系統的供應商管理只支援新增資料，管理員無法查詢既有供應商、檢視單筆明細或更新聯絡資訊與啟用狀態。A007 要補齊這段維運流程，讓管理端可以持續維護供應商資料品質，支撐後續菜單管理與通知流程。

## What Changes

- 擴充管理員供應商管理能力，新增供應商清單查詢，並支援名稱精確查詢與模糊查詢。
- 新增查詢單一供應商詳細資料的能力，讓管理員可在編輯前載入既有資料。
- 新增修改供應商資料的能力，允許更新 `name`、`email`、`phone`、`contact_person`、`is_active`。
- 明確限制 `id` 與 `business_registration_no` 為唯讀欄位，不可透過更新 API 修改。
- 補齊前後端管理畫面、API 契約與欄位驗證，讓供應商查詢與編輯流程一致。

## Capabilities

### New Capabilities

### Modified Capabilities
- `bento-supplier-management`: 從僅支援新增供應商，擴充為支援供應商清單查詢、單筆查詢與受限欄位的資料更新。

## Impact

- 受影響程式碼：backend 供應商 controller/service/repository、frontend 管理員供應商管理頁。
- 受影響 API：新增或調整 `GET /api/suppliers`、`GET /api/suppliers/{id}`、`PATCH /api/suppliers/{id}`。
- 受影響資料：沿用既有 `suppliers` 資料表，不新增欄位，但需固定不可修改欄位與查詢條件。
- 相依項目：沿用 A001 JWT/角色授權，以及 A002 已存在的供應商資料模型與菜單建立流程。
