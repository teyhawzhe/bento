## Historical Note

這份 archived change 建立於較早的 A010 討論階段，部分 proposal / design / delta spec / tasks 仍保留了「完整 CRUD」與「刪除或停用部門」的歷史假設。

目前的現行需求來源請以以下文件為準：
- `../uml/input/A010.md`
- `../uml/openapi.yaml`
- `openspec/specs/department-management/spec.md`

現行 A010 對外契約只包含：
- `GET /api/admin/departments`
- `POST /api/admin/departments`
- `PATCH /api/admin/departments/{id}`

不包含 delete 或 disable 部門 API。
