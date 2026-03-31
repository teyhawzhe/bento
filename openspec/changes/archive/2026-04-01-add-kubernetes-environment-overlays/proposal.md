## Why

目前專案已經有 backend 的 `dev`、`staging`、`production` profile，以及以 `docker-compose.yml` 為主的本機容器啟動方式，但部署層仍缺少對應 Kubernetes 的環境模型。這使得系統雖然能在應用程式設定上區分不同環境，卻沒有一致的 Kubernetes 結構去承接 `dev`、`staging`、`production` 的部署差異，也無法清楚說明哪些設定應放在 ConfigMap、哪些應放在 Secret。

這次 change 要補齊一套可維護的 Kubernetes 環境分層結構，讓 `dev` 以 minikube 為目標環境，並讓 `staging`、`production` 共用同一套基底部署模型。考量目前開發機器未安裝 Kubernetes 且資源有限，本 change 以「建立可部署結構、設定策略與文件」為主，不要求在本機實際跑起 minikube 或正式叢集。

## What Changes

- 新增 Kubernetes 部署結構，支援 `dev`、`staging`、`production` 三組環境的 base 與 overlay 分層。
- 定義 backend 與 frontend 在 Kubernetes 下的設定注入方式，區分 ConfigMap 與 Secret 的責任邊界。
- 明確規範 `dev` 以 minikube 為目標執行環境，但本次不強制要求開發者本機完成 minikube 驗證。
- 補齊 README / 部署文件，說明如何使用 Kubernetes overlays、如何準備環境設定，以及哪些驗證可在沒有本機 k8s 的情況下完成。
- 保留既有 Spring profile 與前端環境參數機制，讓應用層與部署層的環境切分能互相對齊。

## Capabilities

### New Capabilities

- `kubernetes-environment-deployment`: 定義系統如何以 Kubernetes base / overlay 模型支援 `dev`、`staging`、`production` 三組部署環境，以及 minikube 與正式叢集的使用邊界。

### Modified Capabilities

- `runtime-environment-configuration`: 補充環境設定在 Kubernetes 部署模型下的文件化、注入方式與敏感資訊管理規則。

## Impact

- Affected code: `k8s/` 或等效 Kubernetes 設定目錄、README、部署說明、可能的 frontend / backend build 與環境變數注入設定。
- Affected systems: deployment workflow、環境設定管理、frontend/backend container deployment。
- Likely dependencies: Kubernetes manifests 或 Kustomize/Helm 結構、ConfigMap、Secret、Ingress、image build 策略。
