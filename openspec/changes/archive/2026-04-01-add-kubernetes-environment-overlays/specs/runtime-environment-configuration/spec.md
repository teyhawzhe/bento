## MODIFIED Requirements

### Requirement: 不同環境的 build 與 startup 方式必須可文件化且可重現
系統 SHALL 為 `dev`、`staging`、`production` 提供明確且可重現的 build / startup 方式，讓開發者與部署流程可選擇目標環境並套用對應設定。當系統以 Kubernetes 部署時，文件 MUST 說明各環境如何對應到 Kubernetes deployment structure、如何指定目標 profile，以及哪些步驟可在沒有本機叢集的情況下先完成。

#### Scenario: 文件中可找到 dev 的 Kubernetes 目標部署方式
- **WHEN** 開發者查閱專案說明文件
- **THEN** 文件明確描述 `dev` 環境以 minikube 為目標部署方式，以及本機未安裝 minikube 時的限制與替代檢查方法

#### Scenario: 文件中可找到 staging 或 production 的 Kubernetes 設定方式
- **WHEN** 維運人員查閱專案說明文件
- **THEN** 文件明確描述如何套用 `staging` 或 `production` 的部署設定與對應環境參數

### Requirement: 敏感設定不得寫死於版本庫
系統 SHALL 允許 `staging` 與 `production` 使用外部注入方式提供敏感設定，例如資料庫密碼、SMTP 帳密或正式寄件者資訊。這些敏感值 MUST 不作為固定明文寫入版本庫。當系統以 Kubernetes 部署時，敏感設定 MUST 透過 Secret 或等效外部注入機制提供，而非直接寫入可提交的 manifests。

#### Scenario: production 的 Kubernetes deployment 使用 Secret 提供敏感資訊
- **WHEN** production 環境需要資料庫密碼、JWT secret 或 SMTP 密碼
- **THEN** 系統透過 Kubernetes Secret 或等效外部機制注入該值，而非依賴版本庫內固定明文
