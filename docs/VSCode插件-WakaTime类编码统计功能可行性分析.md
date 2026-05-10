# VS Code 插件 — WakaTime 类编码统计功能可行性分析

## 1. 现有 VS Code 插件定位

### 1.1 搜索结论

- **`.vscode/`**：仓库根目录存在 `.vscode/settings.json`（编辑器/工具链配置，**非**扩展源码）。
- **`*vscode*` / 扩展工程**：存在独立目录 **`vscode_autoattend_plugin/`**（协作工作台相关扩展），未发现 `extensions/` 等第二套扩展根目录。
- **`package.json` 中 `engines.vscode`**：见 `vscode_autoattend_plugin/package.json`。

### 1.2 插件标识与工程元数据

| 项 | 值 |
|----|-----|
| **npm 包名** | `autoattend-vscode-plugin` |
| **显示名** | AutoAttend |
| **publisher** | `autoattend` |
| **版本** | `0.0.1` |
| **VS Code 引擎** | `^1.90.0` |
| **入口** | `./dist/extension.js`（由 TypeScript 编译） |

### 1.3 激活事件（`activationEvents`）

- `onView:autoAttend.sidebarView` — 打开侧栏 Webview 视图时激活
- `onCommand:autoAttend.openPanel` — 执行「打开工作台」命令时激活
- `onCommand:autoAttend.logout` — 执行「退出登录」命令时激活

> 说明：当前**未**使用 `*` 或 `onStartupFinished` 等全局激活；若增加「后台持续统计」，通常需评估是否改为更宽激活或增加 `workspaceContains` 等，以在无需打开侧栏时仍能采集（见第 5、6 节）。

### 1.4 已有贡献点（`contributes`）

- **commands**
  - `autoAttend.openPanel` — 「AutoAttend: 打开工作台」
  - `autoAttend.logout` — 「AutoAttend: 退出登录」
- **viewsContainers.activitybar**
  - 容器 id：`autoAttend`，标题 AutoAttend，图标 `resources/icon.svg`
- **views**
  - Webview 视图 id：`autoAttend.sidebarView`，名称「协作工作台」
- **configuration**
  - `autoAttend.baseUrl`：后端地址，默认 `http://localhost:8080`

### 1.5 运行时能力摘要（与统计相关的现状）

- 扩展内已实现：`SecretStorage` 存 admin/collab token、`globalState` 存 baseUrl、`fetch` 调用 `/api/admin/auth/*` 与 **`/api/collab/projects`** 等。
- **尚无** 编辑器活动/文件切换监听、心跳上报、离线队列等与 WakaTime 同类能力。

### 1.6 相关既有文档（可选参考）

- `docs/VSCode插件-MVP任务拆解与接口清单.md` — 协作多维表格/看板 MVP，可与本统计能力分期并行规划。

---

## 2. WakaTime 类产品能力拆解

### 2.1 能力清单与归属

| 能力 | 扩展主机内 | 建议/必须后端 | VS Code API 与边界说明 |
|------|-------------|----------------|------------------------|
| 编辑器焦点 / 活动编辑器变化 | **可**：`window.onDidChangeWindowState`、`window.onDidChangeActiveTextEditor`、`workspace.onDidChangeTextDocument`（编辑信号）等 | 后端仅收聚合后事件即可 | **无法**获知 OS 级「用户是否真的在看屏幕」；最小化/失焦依赖窗口状态 API，精度低于专用桌面代理 |
| 文件切换 / 语言识别 | **可**：`TextDocument.languageId`、`document.fileName` / `uri` | 后端可做规范化与脱敏策略 | **Remote/SSH/WSL** 路径形态不同；部分 scheme（如 `vscode-remote`）需在扩展内统一处理 |
| 按「本地文件夹」聚合 | **可**：`workspace.workspaceFolders` | 后端映射到业务「项目」需规则或用户绑定 | 多根工作区需显式建模 |
| 按 **AutoAttend 业务项目 / 租户** 聚合 | **可（弱）**：依赖用户选择或「文件夹 ↔ 项目」映射配置 | **宜**：服务端按 `tenantId` / `projectId` / `userId` 持久化与权限校验 | 扩展无法凭空知道磁盘目录对应哪条 `BizProject`，需映射或显式选择 |
| 心跳上报 | **可**：`setInterval` / `Disposable` 与激活生命周期绑定 | **宜**：鉴权、限流、幂等、合并窗口 | 心跳过密增加耗电与网络策略触达概率（见第 5 节） |
| 离线队列 | **可**：`globalState` / 本地文件 / SQLite（若引入） | 服务端需容忍乱序与重复提交 | 扩展存储有大小限制，大批量需落盘或压缩 |
| 服务端 API、仪表盘、团队/账单 | **否** | **必须** | Webview 可展示嵌入式报表，复杂 BI 仍宜 Web 前端 |
| 隐私与配置（不上报路径、仅语言统计等） | **可**：设置项 + 客户端脱敏 | **宜**：服务端再次校验字段白名单 | 完全「零出站」模式可只做本地，则与 WakaTime 云模式不同 |

### 2.2 受 API 限制的要点（设计时需预期）

1. **「编码时间」是推断量**：基于活动编辑器 + 文档变更 + 窗口焦点等启发式，不等同于考勤级准确工时。
2. **未激活扩展则无采集**：当前激活较「懒」，若统计要覆盖「只写代码不看侧栏」，需调整激活策略或提供显式「启用编码统计」开关并触发激活。
3. **Webview 内时间**：iframe 内活动**不一定**反映为 VS Code 文本编辑器事件，若团队主要在嵌入页操作，需单独产品决策是否统计（通常不纳入经典 WakaTime 模型）。
4. **Git 分支/远程**：可通过 `git` 扩展 API 或执行 `git` 子进程获取，但增加依赖与权限说明成本。

---

## 3. 与 AutoAttend 仓库的结合

### 3.1 后端现状（Spring）

- 后端模块路径：`atuo_attend_backend/`，典型 REST 前缀如 **`/api/collab/*`**、**`/api/admin/*`**。
- **租户 / 项目模型**：协作侧已有项目列表接口，例如 `CollabProjectController` 映射 **`/api/collab/projects`**，返回项含 `id`、`tenantId`、`name`、`repoId` 等（与扩展内 `listProjects()` 调用一致）。
- **鉴权**：插件已使用 **协作 JWT**（`Authorization: Bearer`）访问 collab API；管理端 token 存于 SecretStorage。新增「编码心跳」接口可优先挂在 **collab** 路径下，以便与当前用户、租户上下文一致。

### 3.2 「按 AutoAttend 项目维度」上报与展示的对接点

| 对接点 | 说明 |
|--------|------|
| **身份** | 沿用 collab token，后端从 JWT 解析 `sessionUserId`、租户/项目范围，避免插件伪造用户 |
| **projectId** | 插件侧增加「当前统计绑定项目」：用户从已有列表选择，或「自动建议」本地 git remote 与 `BizProject.repoId` 匹配（后者开发量更高） |
| **展示** | 管理端/协作 Web 已有项目维度页面时，可嵌入「本项目编码时长」卡片；或独立报表路由 |
| **与现有协作数据关系** | 编码统计与多维表格/看板数据正交，宜独立表，通过 `tenant_id`、`project_id`、`user_id`、日期聚合查询 |

### 3.3 若无现成 API — 粗粒度新增接口清单（建议）

以下均为 **粗粒度**，命名可按现有 `ApiResponse` 风格调整。

1. **`POST /api/collab/coding-time/heartbeats`（或 `/events`）**
   - 批量接受心跳：时间戳、时长片段、语言、`projectId`（可选）、**脱敏后**文件指纹或仅语言维度。
   - 行为：鉴权、幂等键（如 client_event_id）、限流。

2. **`GET /api/collab/coding-time/summary`**
   - 查询参数：`projectId`、`from`、`to`、粒度 `day|week`。
   - 返回：按用户或按项目的聚合秒数/分钟数。

3. **（可选）`PUT /api/collab/coding-time/preferences`**
   - 用户级：是否上报路径、采样间隔、绑定项目策略。

4. **（管理端，可选）`GET /api/admin/.../coding-time/reports`**
   - 租户管理员查看多成员汇总、导出 CSV。

5. **数据库（若持久化）**
   - 原始事件表 + 按日汇总表（物化或定时任务），避免报表扫描全量事件。

---

## 4. 实现难度分级

| 范围 | 难度 | 说明 |
|------|------|------|
| **纯本地统计 MVP** | **低** | `onDidChangeActiveTextEditor` + 定时器 + `OutputChannel`/状态栏展示；无后端、无多设备同步 |
| **本地统计 + 简单 REST 上报** | **中** | 增加脱敏、离线队列、重试、与 collab 登录打通、`projectId` 绑定；后端 1～2 个写入/查询接口 + 表结构 |
| **完整 WakaTime 级**（仪表盘、团队排行、账单/配额、多 IDE 同步、精细权限与 SLA） | **高** | 需完整产品化、运营与合规流程、高可用 ingest、报表管道、冲突合并策略、多编辑器插件矩阵 |

---

## 5. 风险与合规

| 类别 | 风险 | 缓解建议 |
|------|------|----------|
| **隐私** | 文件路径、分支名、仓库 URL 属于敏感元数据 | 默认仅上报 **语言 + 哈希化路径** 或不上报路径；配置项分级；隐私政策与用户同意 |
| **性能** | 心跳过密导致 CPU/网络/日志膨胀 | 建议 **30s～120s** 级心跳或「有编辑才刷新」；批量上传 |
| **企业网络** | 出站 HTTPS 被拦 | 支持可配置 baseUrl（已有）、代理环境变量、离线队列与可观测失败原因 |
| **插件冲突** | 多个统计扩展重复钩子 | 文档说明；避免过度监听 `type`/`change` 全量；提供禁用开关 |
| **安全** | Token 与 URL 拼接（现有嵌入页模式） | 新接口优先 **Header** 传 JWT；避免在 query 中长期附带 token（与现有 `openEmbed` 设计长期演进一并评估） |

---

## 6. 结论与建议

1. **是否建议做 MVP**：**建议**。与现有 AutoAttend 插件、Spring 后端、**`/api/collab/projects`** 模型契合度高，价值闭环清晰（个人/项目维度「大致编码投入」），不必一次做到 WakaTime 全量能力。
2. **推荐技术路线（MVP）**：
   - **`window.onDidChangeActiveTextEditor` + `onDidChangeWindowState`** 维护「当前片段」起止；
   - **`setInterval` 或基于编辑事件的防抖心跳**（避免空转）；
   - **`fetch` POST** 至自建 `POST .../heartbeats`（collab 鉴权）；
   - **本地队列**：`globalState` 或小型本地 JSON 文件缓冲失败请求。
3. **分阶段交付**：
   - **P0**：侧栏或命令中「绑定项目」+ 本地汇总 + 可选上报开关；
   - **P1**：后端持久化 + 日汇总 + Web 简易图表；
   - **P2**：路径/分支脱敏策略、管理报表、限流与审计。

---

## 修订记录

| 日期 | 说明 |
|------|------|
| 2026-05-10 | 初版：基于仓库 `vscode_autoattend_plugin` 与 `atuo_attend_backend` 协作项目 API 的可行性分析 |
