# AutoAttend VSCode Plugin (MVP)

在 VSCode 内通过 Webview iframe 直接接入 AutoAttend 网页，实现“一套前端多端复用”。

## 已实现功能（Webview 嵌入模式）

- 登录（`/api/admin/auth/login`）
- 拉取项目列表并选择项目
- Webview 内 iframe 直接打开网页：
  - 项目列表页：`/collab/projects`
  - 项目协作页：`/collab/projects/{id}/table`
- 自动通过 URL 向网页注入 `adminToken` / `collabToken`，网页端接收后写入 localStorage
- 网页支持 `embed=1`，可隐藏外层壳，减少插件内冗余 UI

## 本地运行

1. `cd vscode_autoattend_plugin`
2. `npm install`
3. `npm run compile`
4. 在 VSCode 打开本仓库，按 `F5` 启动 Extension Development Host
5. 在左侧 Activity Bar 点击 `AutoAttend` 图标

## 配置

- `autoAttend.baseUrl`：后端地址，默认 `http://localhost:8080`

## 说明

- 该模式的核心价值：后续你只维护网页前端，插件无需同步重写业务界面。
- 若 iframe 被拦截（如 `X-Frame-Options` / CSP `frame-ancestors`），需在网关或后端放开嵌入策略。

