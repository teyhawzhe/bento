## 1. OpenSpec Alignment

- [x] 1.1 新增 `kubernetes-environment-deployment` 主規格，定義 Kubernetes base / overlay、minikube 目標與三組環境責任邊界
- [x] 1.2 更新 `runtime-environment-configuration`，補上 Kubernetes 部署下的環境注入與敏感資訊管理要求

## 2. Kubernetes Deployment Structure

- [x] 2.1 建立專案內的 Kubernetes 設定目錄與共用 base 資源
- [x] 2.2 建立 `dev`、`staging`、`production` overlays，讓 backend / frontend 可對應不同環境設定
- [x] 2.3 定義 ConfigMap 與 Secret 的使用方式，清楚區分可進版本庫與不可進版本庫的設定
- [x] 2.4 為 `dev` overlay 說明 minikube 目標使用方式與必要限制

## 3. Application Deployment Mapping

- [x] 3.1 對齊 backend 的 `SPRING_PROFILES_ACTIVE` 與 Kubernetes overlays 設定
- [x] 3.2 對齊 frontend 的 API base URL 設定策略，明確說明 build-time 或 deploy-time 做法
- [x] 3.3 定義 database、service、ingress 與 image 使用策略，避免三環境部署模型失衡

## 4. Documentation And Verification

- [x] 4.1 更新 README 或部署文件，說明如何準備 `dev` / `staging` / `production` 的 Kubernetes 設定
- [x] 4.2 補上 Secret 準備方式與示例，避免敏感值寫入版本庫
- [x] 4.3 完成不依賴本機叢集的靜態驗證，例如 manifests 結構與設定檢查
- [x] 4.4 若有可用環境，補記錄 minikube 或其他 Kubernetes 叢集的實際部署驗證結果
