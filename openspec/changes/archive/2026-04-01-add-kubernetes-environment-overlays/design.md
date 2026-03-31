## Context

目前 repo 已具備兩塊基礎：

- backend 已用 Spring profile 支援 `dev`、`staging`、`production`
- frontend 已可透過 `VITE_API_BASE_URL` 指定 API 位址

但部署層仍停留在 `docker-compose.yml`，沒有 Kubernetes 的 base / overlay 結構，也沒有定義環境差異應落在哪一層。若直接為三個環境各寫一套獨立 YAML，後續很容易漂移；若把所有差異都塞進單一檔案與大量變數，又會失去可讀性與文件化能力。

本 change 要先把 Kubernetes 的結構與責任邊界定清楚，讓未來可以在有 minikube 或正式叢集的環境中部署，而不是要求目前這台開發機立刻跑起整套 k8s。

## Goals / Non-Goals

**Goals:**
- 建立可維護的 Kubernetes base / overlay 結構。
- 支援 `dev`、`staging`、`production` 三組環境部署設定。
- 定義 `dev` 以 minikube 為目標環境的定位。
- 明確區分 ConfigMap 與 Secret 的責任邊界。
- 讓 README / 部署文件能描述如何準備與套用不同環境設定。

**Non-Goals:**
- 不要求本機必須安裝或成功執行 minikube。
- 不在本 change 內建立正式雲端 Kubernetes 叢集。
- 不在本 change 內完成 CI/CD pipeline。
- 不強制在本 change 內完成 frontend runtime config 重構；若現況採 build-time 注入，允許先以環境別 build 參數維持。

## Decisions

### 1. 採用 Kubernetes base + overlays 結構，而不是三套獨立 manifests

系統將以共用 base 搭配 `dev`、`staging`、`production` overlays 管理環境差異。共用資源例如 deployment、service、ingress 基礎結構留在 base；環境差異例如 image tag、host、profile、ConfigMap/Secret 引用留在 overlays。

原因：
- 避免三套 YAML 長期漂移。
- `staging` 與 `production` 可保持高相似度。
- `dev` 仍可用自己的 overlay 對應 minikube。

替代方案：
- 為三個環境維護完全獨立的 manifests：短期直覺，但長期維護成本高。
- 改用單一 manifest + 大量變數：可行，但可讀性差，審查與追蹤變更困難。

### 2. `dev` 的 Kubernetes 目標環境定為 minikube，但本次不把本機驗證列為完成條件

設計上將 `dev` overlay 明確定位為可部署到 minikube，但考量目前開發機未安裝 Kubernetes 且資源有限，本次完成條件聚焦在：

- manifests / overlays 結構完整
- 設定注入策略清楚
- 文件可指引有資源的環境完成部署

原因：
- 先把部署模型設計正確，比在不合適的機器上硬跑更有價值。
- 可以降低 change 被本機環境卡住的風險。

### 3. 應用層繼續保留現有 profile / env 機制，Kubernetes 只負責注入

backend 持續使用 `SPRING_PROFILES_ACTIVE` 對應 `dev`、`staging`、`production`；frontend 保留現有 API base URL 設定方式。Kubernetes 層負責把對應環境值注入容器，而不是重新發明另一套環境命名。

原因：
- 已有 profile 設計可用，不需重做應用設定模型。
- 能讓 Docker Compose、CLI 啟動與 Kubernetes 部署共用同一組應用層語意。

### 4. ConfigMap 與 Secret 分層管理

非敏感設定如 profile 名稱、API host、一般 mail host、feature flags 等，放在 ConfigMap 或 overlay 參數中；敏感設定如資料庫密碼、SMTP 密碼、JWT secret 等，透過 Secret 或外部部署平台注入。

原因：
- 延續既有 runtime environment spec 的安全邊界。
- 讓 `staging`、`production` 的敏感資料不進版本庫。

### 5. frontend 先接受環境別 build / deploy 策略，不強迫單一 image runtime 注入

目前 frontend 的 `VITE_API_BASE_URL` 偏向 build-time 注入。這次 change 不強制把它重構為 runtime config，只要求部署文件與 overlays 能清楚定義 frontend 在各環境的 API 目標。若之後想提升為單一 image 多環境共用，可在後續 change 再處理。

原因：
- 讓本次範圍維持在中版本，不把前端配置體系重構一起打包。
- 先解決 k8s 環境結構與文件問題。

## Risks / Trade-offs

- [Risk] frontend 若維持 build-time API URL，可能需要不同環境產出不同 image
  → Mitigation: 在 README 與 tasks 中明確寫出這個策略，避免誤以為單一 image 可直接跨環境複用。

- [Risk] 沒有本機 minikube 驗證，可能讓 manifests 的實際部署問題延後暴露
  → Mitigation: 將可做的靜態驗證、結構檢查與文件化列為本次必做；真正 cluster 驗證列為可在有資源環境執行的後續驗證。

- [Risk] 若 dev overlay 包含 MySQL in-cluster，而 staging / production 使用外部 DB，差異可能過大
  → Mitigation: 在 design 與 tasks 中要求清楚文件化 DB 依賴邊界，並盡量只保留必要差異。

## Migration Plan

1. 建立 Kubernetes base 與 `dev` / `staging` / `production` overlays。
2. 定義 frontend、backend、database、service、ingress 與設定注入的責任邊界。
3. 將 `SPRING_PROFILES_ACTIVE` 與相關環境值映射到對應 overlays。
4. 補充 README 與部署指引，說明 minikube 目標、Secret 準備方式與本機未安裝 k8s 時的限制。
5. 進行 manifests 結構檢查與可執行的靜態驗證；若有可用叢集，再補做部署驗證。

## Open Questions

- dev overlay 是否要內含 MySQL deployment，還是預設連到外部資料庫？
- frontend 是否接受各環境單獨 build image，還是要在近期追加入 runtime config？
- ingress / host naming 在 staging 與 production 是否已有既定網域規則？
