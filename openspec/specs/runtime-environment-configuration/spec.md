## Purpose
定義 backend 在 `dev`、`staging`、`production` 三種環境下的設定載入、啟動方式與敏感資訊管理規則。

## Requirements
### Requirement: 系統必須支援 dev、staging、production 三組環境設定
系統 SHALL 提供 `dev`、`staging`、`production` 三組可明確選擇的執行環境設定。每組環境 MUST 可透過啟動參數、環境變數或容器設定指定，且未指定時 MUST 有明確的預設策略。

#### Scenario: 以 dev 環境啟動
- **WHEN** 開發者以 `dev` 環境設定啟動 backend
- **THEN** 系統載入 `dev` 對應的設定覆寫並成功啟動

#### Scenario: 以 staging 環境啟動
- **WHEN** 系統以 `staging` 環境設定啟動
- **THEN** 系統載入 `staging` 對應的設定覆寫並成功啟動

#### Scenario: 以 production 環境啟動
- **WHEN** 系統以 `production` 環境設定啟動
- **THEN** 系統載入 `production` 對應的設定覆寫並成功啟動

### Requirement: 不同環境的 build 與 startup 方式必須可文件化且可重現
系統 SHALL 為 `dev`、`staging`、`production` 提供明確且可重現的 build / startup 方式，讓開發者與部署流程可選擇目標環境並套用對應設定。

#### Scenario: 文件中可找到 dev 啟動方式
- **WHEN** 開發者查閱專案說明文件
- **THEN** 文件明確描述如何以 `dev` 環境 build 或啟動系統

#### Scenario: 文件中可找到 production 啟動方式
- **WHEN** 維運人員查閱專案說明文件
- **THEN** 文件明確描述如何以 `production` 環境 build 或啟動系統

### Requirement: 敏感設定不得寫死於版本庫
系統 SHALL 允許 `staging` 與 `production` 使用外部注入方式提供敏感設定，例如資料庫密碼、SMTP 帳密或正式寄件者資訊。這些敏感值 MUST 不作為固定明文寫入版本庫。

#### Scenario: production 使用外部 SMTP 密碼
- **WHEN** production 環境需要 SMTP 密碼
- **THEN** 系統透過外部設定注入該值，而非依賴版本庫內固定明文
