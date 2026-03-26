## Context

重新檢查後，A010 的現行來源已經一致：
- `../uml/input/A010.md` 只定義查詢、新增、修改部門
- `../uml/openapi.yaml` 只定義 `GET /api/admin/departments`、`POST /api/admin/departments`、`PATCH /api/admin/departments/{id}`
- `openspec/specs/department-management/spec.md` 也明確限制不提供 UML / OpenAPI 未定義的刪除或停用 API
- `backend/src/main/java/com/lovius/bento/controller/A010Controller.java` 目前實作同樣只有 GET / POST / PATCH

真正過時的是 archived 的 `employee-department-field` change。那份歷史 change 仍保留「完整 CRUD」、「刪除/停用規則」、「被員工引用時不可刪除」等較早版本的思路。這本身不是程式錯誤，而是歷史文檔沒有被標示成舊版假設，容易在後續閱讀時造成誤解。

## Goals / Non-Goals

**Goals:**
- 固定 A010 現行需求邊界，讓後續討論都以查詢、新增、修改為準。
- 降低 archived change 舊敘事對現在規格理解造成的干擾。
- 若需要，補充主規格的說明文字，讓「不提供 delete」的原因更容易被看懂。

**Non-Goals:**
- 不新增任何部門刪除 API。
- 不修改後端、前端或資料表結構。
- 不重寫整份 archived change，只做必要的對齊說明或補充。

## Decisions

### 1. 以 `A010.md` + `openapi.yaml` + 主 OpenSpec spec 作為現行真相
- 決策：後續所有 A010 討論都以這三者為主，不再以 archived change 內容反向推導現行需求。
- 理由：archived change 代表歷史提案與當時決策過程，不一定等於最終持續有效的需求契約。
- 替代方案：把 archived change 視為與主 spec 同等權威。未採用，因為這會再次把舊版假設混入現在需求。

### 2. 對 archived A010 change 採「歷史描述」處理，而不是功能回補
- 決策：這次 change 聚焦於文檔對齊與註記，不把 delete 部門當成功能缺口。
- 理由：目前主來源沒有 delete，若直接補功能，反而會讓實作偏離現行需求。
- 替代方案：直接再開一個功能 change 去做 delete。未採用，因為沒有現行需求依據。

### 3. 主規格若需調整，只做敘事強化，不改變現有契約內容
- 決策：`department-management` 若要更新，應偏向補充「目前只暴露查詢、新增、修改」的可讀性，而不是改變 API 定義。
- 理由：主 spec 內容目前方向正確，問題主要在讀者可能被 archived change 誤導。
- 替代方案：完全不動主 spec。可行，但若要降低後續誤讀，補一點敘事會更穩。

## Risks / Trade-offs

- [archived change 本質上就是歷史資料，若過度修改可能失去追溯價值] → 應盡量採補充說明或新增對齊文檔，而不是大幅重寫歷史內容。
- [若只更新主 spec，不整理 archived change，未來 explore 時仍可能再次誤判] → tasks 應至少包含一項「列出 archived change 的過時描述點」。
- [若把 archived 文檔完全當錯誤處理，會掩蓋當初設計討論脈絡] → 應明確區分「歷史提案」與「現行契約」。

## Migration Plan

- 先整理 A010 現行來源與 archived A010 change 的差異清單。
- 視需要補強 `department-management` 的敘事說明，強化「不提供 delete / disable」的現行邊界。
- 產出一份簡潔的對齊結果，供後續 explore / propose 直接引用。

## Open Questions

- 是否需要在 archived A010 change 目錄內增加一份簡短說明，標記該 change 包含舊版 CRUD 假設？如果你希望保留最清楚的追溯路徑，這會是最穩的做法。
