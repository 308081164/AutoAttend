# 码云 / Gitee / 自建 Git 与 AutoAttend 集成可行性分析

## 一、你的目标归纳

1. **部署即自带 Git 服务**：部署 AutoAttend 时，同时带上一套与项目关联好的「码云」（或等价 Git 服务），不依赖公网 GitHub。
2. **前端创建仓库**：在 AutoAttend 前端里能「创建仓库」，且自动完成：
   - 在 Git 服务上创建仓库；
   - 在 AutoAttend 内自动关联项目、多维表格等。
3. **更高集成度**：代码托管与看板/协作在同一套部署内闭环管理。

---

## 二、整体可行性结论

| 维度 | 结论 | 说明 |
|------|------|------|
| **技术可行性** | **高** | 现有架构已具备「仓库 → 项目 → 多维表」的自动联动（Webhook + CollabSyncService）；只需把「Git 来源」从 GitHub 扩展为 Gitee/自建 Git，并增加「由本系统创建仓库」的入口。 |
| **「部署即自带码云」** | **取决于方案** | 若指「我服务器上跑一套 Git 服务」：用 **Gitea** 与 AutoAttend 一起 Docker 编排即可实现；若指「码云品牌且私有化」：需 Gitee 企业版/私有化（商务报价）。 |
| **前端创建仓库并自动关联** | **能实现** | 后端调 Git 服务 API 创建仓库 → 在本系统内创建/关联项目与多维表 → 为该仓库注册 Webhook，流程可完全打通。 |

**结论：整体可行性高。** 推荐路径是「自建 Git（Gitea）+ AutoAttend 双服务编排 + 后端创建仓库/Webhook + 前端创建仓库入口」，即可达到「部署即自带 Git、前端创建即自动关联项目与多维表」的目标；若必须用「码云」品牌且私有化，再考虑 Gitee 企业版。

---

## 三、两种实现路径对比

### 3.1 方案 A：对接 Gitee.com（公有云）

- **含义**：不部署 Git 服务，AutoAttend 对接 [gitee.com](https://gitee.com) 的 API 与 Webhook。
- **是否「自带码云」**：否，依赖公网 Gitee，不是你服务器上的服务。
- **前端创建仓库**：可以。后端用 Gitee Open API 创建仓库、配置 Webhook，再在 AutoAttend 内创建项目并绑定 `owner/repo`。
- **优点**：无需自建 Git，开发量相对小（类似现有 GitHub 逻辑，换一套 API/Webhook 格式）。
- **缺点**：数据在 Gitee 云端；需 Gitee 账号与 Token；不是「部署即自带」的形态。

### 3.2 方案 B：自建 Git 服务（Gitea）+ Docker 与 AutoAttend 一起部署（推荐）

- **含义**：在**同一台或同一内网**服务器上，用 Docker 跑 **Gitea**（轻量、开源），与现有 MySQL / MinIO / Backend / Frontend 一起编排；AutoAttend 只与这台 Gitea 通信。
- **是否「自带码云」**：是「自带一套 Git 服务」，体验上类似「自带码云」；品牌是 Gitea 而非「码云」，但功能等价（仓库、Webhook、API 一应俱全）。
- **前端创建仓库**：可以。后端调 Gitea API 创建仓库、创建 Webhook（指向 AutoAttend），再在 AutoAttend 内 `ensureProjectAndTable(repoFullName)`，即自动关联项目与多维表。
- **优点**：
  - 代码与看板都在你自己服务器，数据可控、可内网访问；
  - 与现有「项目 = 仓库、一仓库一多维表」模型完全一致；
  - Gitea 提供完整 REST API（创建仓库、Webhook、获取 commit/diff），文档清晰；
  - Docker 镜像小、资源占用低，适合与 AutoAttend 同机部署。
- **缺点**：需要维护 Gitea 服务（备份、升级）；不是「码云」商标。

**若必须「码云」品牌且私有化**：需采用 **Gitee 企业版/私有化**（专业版或旗舰版），需联系 Gitee 商务；API 与 Webhook 与公有云类似，集成方式可参考方案 B 的「创建仓库 + Webhook + 项目关联」流程。

---

## 四、与现有 AutoAttend 架构的契合度

当前逻辑已支持「以仓库为中心」的集成：

| 现有能力 | 说明 |
|----------|------|
| **项目与仓库一一对应** | `BizProject.repoId` = 仓库 full_name（如 `owner/repo`）；`CollabSyncService.ensureProjectAndTable(repoFullName)` 在首次出现某仓库时创建项目 + 多维表 + 默认列。 |
| **Webhook 驱动** | GitHub Push → `GithubWebhookController` → 解析 `repo.full_name`、commits → 调 `ensureProjectAndTable`、`ensureUser`、`ensureProjectMember`、`CommitService.saveCommit` 等。 |
| **仓库来源** | 目前仅「GitHub Webhook 推送」触发；仓库列表来自已入库的 commit 去重。 |

要做的事本质是：

1. **抽象「Git 提供方」**：支持多种来源（GitHub / Gitee.com / Gitea）。
2. **统一「仓库标识」**：继续用 `owner/repo` 或 `repoFullName`，便于与现有项目/多维表/commit 表兼容。
3. **新增「由本系统创建仓库」**：调用对应 Git API 创建仓库 + 在本系统内创建/关联项目与多维表 + 注册 Webhook。
4. **Webhook 多源**：增加 Gitee/Gitea 的 Webhook 解析（payload 与 GitHub 不同），但后续处理可复用现有「按 repoFullName 同步项目与 commit」的逻辑。

因此，**在现有架构上扩展 Git 来源与「创建仓库」能力，改动范围清晰，可行性高。**

---

## 五、方案 B（Gitea 自建）落地要点

### 5.1 部署形态

- 在现有 `docker-compose.yml` / `docker-compose.prod.yml` 中增加 **Gitea** 服务（官方镜像 `gitea/gitea`）。
- Gitea 使用独立数据卷；端口例如 3000（或 8080），与现有 8848/8849 不冲突。
- AutoAttend 后端通过内网地址访问 Gitea（如 `http://gitea:3000`），无需公网。

### 5.2 后端需要实现的能力

| 能力 | 说明 |
|------|------|
| **Gitea API 客户端** | 创建仓库 `POST /api/v1/user/repos` 或 `POST /api/v1/org/{org}/repos`；创建 Webhook `POST /api/v1/repos/{owner}/{repo}/hooks`；获取 commit 列表与 diff（与现有 GithubDiffFetcher 类似，换 Gitea 的 API 路径）。 |
| **创建仓库并关联项目** | 管理员或授权用户在前端点击「创建仓库」→ 后端接收「仓库名、可见性等」→ 调 Gitea 创建仓库 → 得到 `owner/repo` → 调用现有 `collabSyncService.ensureProjectAndTable(repoFullName)` → 再为该仓库在 Gitea 注册 Webhook（URL 指向 AutoAttend 的 `/api/webhooks/gitea` 或 `/api/webhooks/gitea`）。 |
| **Gitea Webhook 处理** | 新增 `POST /api/webhooks/gitea`，解析 Gitea 的 push payload（结构不同于 GitHub），提取 `repo.full_name`、commits、author 等，然后复用现有「ensureProjectAndTable、ensureUser、ensureProjectMember、saveCommit、fetchAndSaveDiff」逻辑；Diff 拉取改为调 Gitea API。 |
| **配置** | 后端配置项：Gitee/Gitea 的 base URL、API Token（或 Gitea 的 admin token），以及「当前默认 Git 提供方」（GitHub / Gitee / Gitea）。 |

### 5.3 前端需要实现的能力

| 能力 | 说明 |
|------|------|
| **「创建仓库」入口** | 在管理后台（或项目协作入口）增加「创建仓库」按钮/表单：输入仓库名、可选描述、可见性等。 |
| **调用后端** | 提交后调用后端「创建仓库」接口；成功后在当前项目列表中出现新仓库，并可进入对应多维表。 |
| **仓库来源展示** | 若同时支持 GitHub / Gitee / Gitea，列表或详情中可展示「来源」（如图标或文字）。 |

### 5.4 数据流小结（前端创建仓库）

```
用户在前端点击「创建仓库」并填写名称
  → 后端调用 Gitea API 创建仓库
  → 后端 ensureProjectAndTable(owner/repo)  // 自动创建项目 + 多维表
  → 后端在 Gitea 为该仓库注册 Webhook，指向 AutoAttend
  → 前端刷新或跳转，即可看到新项目并进入多维表
之后该仓库任意 push → Gitea 发送 Webhook → AutoAttend 同步 commit、成员等（与现有 GitHub 流程一致）
```

---

## 六、风险与约束

| 项 | 说明 |
|----|------|
| **Gitea 与「码云」品牌** | 若必须「码云」品牌且自建，需用 Gitee 企业版/私有化（商务、授权与运维成本）；功能上 Gitea 可完全满足「自带 Git + 前端创建仓库 + 自动关联项目与多维表」。 |
| **多 Git 源并存** | 若未来同时支持 GitHub、Gitee.com、Gitea，需在仓库/项目上记录「来源」字段，Webhook、Diff 拉取按来源走不同逻辑；设计时预留枚举即可。 |
| **权限与 Token** | Gitea 创建仓库、创建 Webhook 需 API Token（如 Gitea 管理员或机器人账号）；需在部署说明中写清如何生成与配置。 |
| **首次部署** | Gitea 首次启动需完成「安装向导」（创建管理员等）；可脚本化或文档说明，与 AutoAttend 的「首次建表」类似，一次性操作。 |

---

## 七、建议实施顺序

1. **阶段一（推荐先做）**：**Gitea 自建 + Docker 编排**  
   - 在 docker-compose 中加入 Gitea 服务；  
   - 后端实现 Gitea API 客户端（创建仓库、创建 Webhook、拉取 commit/diff）；  
   - 新增 Gitea Webhook 接口，复用现有同步与 Diff 逻辑；  
   - 后端提供「创建仓库」API（入参：仓库名等），内部完成「Gitea 创建 → 项目/多维表关联 → Webhook 注册」；  
   - 前端增加「创建仓库」入口并调用该 API。  

2. **阶段二（可选）**：  
   - 支持 Gitee.com 作为另一 Git 源（API + Webhook 适配）；  
   - 或评估 Gitee 企业版/私有化，替代 Gitea 以满足「码云」品牌与合规需求。  

---

## 八、结论

- **整体可行性高**：现有「仓库 → 项目 → 多维表」的自动关联与 Webhook 驱动模型，可直接复用到「码云 / Gitee / 自建 Git」上。  
- **「部署即自带 Git」**：通过 **Gitea 与 AutoAttend 一起 Docker 部署**即可实现；若必须「码云」品牌，则需 Gitee 企业版/私有化。  
- **「前端创建仓库并自动关联项目、多维表」**：可以做到，且与现有设计一致——后端调 Git API 创建仓库并注册 Webhook，再调用现有 `ensureProjectAndTable` 即可完成项目与多维表的自动创建与绑定。

若你确定采用「Gitea 自建」方案，可从「docker-compose 加入 Gitea + 后端 Gitea 客户端与 Webhook」开始实现，再补前端「创建仓库」入口。
