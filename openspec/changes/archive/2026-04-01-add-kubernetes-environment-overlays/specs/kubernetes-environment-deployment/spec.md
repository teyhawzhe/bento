## ADDED Requirements

### Requirement: 系統必須提供 Kubernetes base 與環境 overlays 結構
系統 SHALL 提供一套可維護的 Kubernetes 部署結構，至少包含共用 base 與 `dev`、`staging`、`production` 三組環境 overlays。base MUST 承載共用資源定義；各 overlay MUST 只覆寫必要的環境差異。

#### Scenario: 開發者檢視 Kubernetes 設定結構
- **WHEN** 開發者查看專案內的 Kubernetes 設定
- **THEN** 可辨識共用 base 與 `dev`、`staging`、`production` overlays 的分層方式

#### Scenario: staging 與 production 沿用同一套基底部署模型
- **WHEN** 維運比較 `staging` 與 `production` 的 Kubernetes 設定
- **THEN** 兩者共用同一套 base，只保留必要的環境差異

### Requirement: dev 環境的 Kubernetes 目標必須定義為 minikube
系統 SHALL 將 `dev` overlay 的 Kubernetes 目標環境定義為 minikube，並在文件中說明其用途與限制。本需求 MUST 不要求每位開發者本機都安裝或執行 minikube，仍可先完成結構與設定層級的準備工作。

#### Scenario: 本機未安裝 minikube 的開發者閱讀部署說明
- **WHEN** 開發者尚未安裝 minikube 或本機資源不足
- **THEN** 文件仍可讓其理解 `dev` overlay 的用途、必要設定與可先完成的檢查項目

#### Scenario: 有 minikube 的環境套用 dev overlay
- **WHEN** 團隊成員在具備 minikube 的環境中部署 `dev` overlay
- **THEN** 系統可依 `dev` 環境設定部署 frontend、backend 與相依資源

### Requirement: Kubernetes 部署必須明確區分 ConfigMap 與 Secret 責任邊界
系統 SHALL 清楚區分哪些設定可透過 ConfigMap 或可提交的 overlay 管理，哪些敏感資訊 MUST 經由 Secret 或等效外部注入機制提供。此邊界 MUST 適用於 `dev`、`staging`、`production` 三組環境。

#### Scenario: backend 部署需要 profile 與 mail 設定
- **WHEN** backend Kubernetes deployment 需要 `SPRING_PROFILES_ACTIVE`、mail host 與其他一般設定
- **THEN** 系統可透過 ConfigMap 或等效非敏感設定來源提供這些值

#### Scenario: backend 部署需要資料庫與 SMTP 密碼
- **WHEN** backend Kubernetes deployment 需要資料庫密碼、SMTP 密碼或 JWT secret
- **THEN** 系統透過 Secret 或等效外部注入機制提供這些敏感值

### Requirement: frontend 與 backend 的 Kubernetes 環境設定必須與既有應用層環境模型對齊
系統 SHALL 讓 Kubernetes overlays 與既有應用層環境模型保持一致。backend MUST 能透過部署設定指定對應的 Spring profile；frontend MUST 有明確策略對應各環境的 API base URL 或等效設定來源。

#### Scenario: dev overlay 部署 backend
- **WHEN** 團隊套用 `dev` overlay 部署 backend
- **THEN** backend 使用 `dev` 對應的環境設定啟動

#### Scenario: staging overlay 部署 frontend
- **WHEN** 團隊套用 `staging` overlay 部署 frontend
- **THEN** frontend 會以明確定義的方式對應 `staging` API 端點，而不是依賴模糊的預設值
