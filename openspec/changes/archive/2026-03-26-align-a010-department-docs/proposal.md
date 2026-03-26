## Why

目前 A010 的現行需求來源已經收斂為「查詢、新增、修改部門」，不包含刪除部門；`../uml/input/A010.md`、`../uml/openapi.yaml`、主 OpenSpec 規格與後端 A010 controller 也都一致對齊這個範圍。不過較早歸檔的 A010 相關 change 仍保留「完整 CRUD」、「刪除或停用部門」等歷史描述，容易讓後續閱讀者誤以為 delete 仍是現行需求。

這次需要開一個小而明確的文檔對齊 change，把「A010 現行真相」固定下來，並標記哪些 archived change 文檔只是歷史脈絡，避免之後在 explore、proposal 或 implementation 階段再次把舊版本當成最新版需求。

## What Changes

- 明確記錄目前 A010 的有效需求邊界：只包含 `GET`、`POST`、`PATCH` 部門 API，不包含 delete。
- 對齊 `department-management` 與相關文檔敘事，避免混用過時的「完整 CRUD」描述。
- 補上一份文檔整理任務，指出 archived A010 change 中哪些內容屬於舊版本歷史說法。

## Capabilities

### Modified Capabilities

- `department-management`: 補強文檔敘事，明確說明現行 A010 對外契約只到查詢、新增、修改，不包含刪除或停用。

## Impact

- Affected docs: `openspec/specs/department-management/spec.md` 與 A010 歷史 change 的相關敘事檔案。
- Affected workflow: 後續 explore / propose 時，應以現行 A010 主來源為準，不再從舊 archived change 推導 delete 需求。
- Code impact: 無，不涉及前後端功能修改。
