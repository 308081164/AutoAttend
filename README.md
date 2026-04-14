# 流帮Project

**Slogan：软件流程，帮你搞定！**

> 说明：本仓库目录与代码工程名仍为历史名称（如 `AutoAttend` / `auto_attend_forntend`），**仅产品对外展示名**使用「流帮Project」与上述 Slogan；数据库、包名、接口路径等未改。

基于 GitHub Webhook 的开发状态追踪与 **多租户** 项目协作平台：**租户管理员**可查看提交与 Diff、看板统计、单次提交 AI 分析、**需求→报价→合同半自动化**（结构化/AI 辅助录入、人天报价、合同 AI 与多格式导出），并按订阅档位使用客户看板、Agent、快捷运维等能力；**员工**可登录后查看工作台、参与项目与多维表格协作（任务/需求管理、讨论、附件预览与下载）。

## 在线体验（测试环境）

- 在线地址：<http://autoattend.广厦智汇.com>
- 测试页面：<http://autoattend.广厦智汇.com/test>
- 说明：可先访问首页体验管理端与协作端主流程，再进入测试页面查看项目效果。

**近期已落地能力摘要**：多租户 **SaaS 订阅与配额**（免费版 / 尝鲜版 / 专业版 / 专业增强版，模拟支付续期）；**会员与计费**页、工作台 **权益展示**；**邀请码与会员积分、推荐返利（首年消费 10% 积分）**；租户管理员 **阿里云短信验证码**（可选，未配置时仍为密码登录）；独立 **平台监测台**（全站租户与用量、暂停/恢复、一键开通专业增强版、**全局官方邀请码**管理）；报价 **功能模块 AI 智能录入**；**报价单抬头**与 **乙方主体模板**；**合同补充信息**与 **附件一/三 HTML**；生产 **push 到 main/master 自动构建部署**（可选 ghcr 私有镜像登录）。详见 §1、§5 与 [docs/Docker推送后自动部署说明.md](docs/Docker推送后自动部署说明.md)。

---

## 1. 功能概览

### 1.1 登录与角色

- **租户管理员**（组织账号，多租户 SaaS）：在 **`/register`** 注册组织（名称、slug、手机号、密码），在 **`/login`** 使用 **手机号 + 密码** 登录。若服务端配置了 **阿里云短信**，登录/注册需填写 **短信验证码**（发送频率与有效期见环境变量说明）。注册时可填 **邀请码**（绑定推荐关系）。登录后自动具备**项目协作权限**，无需再次登录即可进入「项目协作」与全部多维表。
- **员工登录**（登录页「员工登录」→ `/collab-login`）：commit 作者邮箱 + 密码（默认 123456，可由管理员重置）。登录后进入**工作台**（`/member`），可查看参与项目、本人提交统计，并进入项目协作多维表。

### 1.2 租户控制台（租户管理员登录后）

- **GitHub Webhook**：接入仓库 Push 等事件，自动拉取 commit 并生成完整 `git diff` 存档。
- **状态看板与统计**：按时间范围与仓库查看提交趋势、各仓库占比、开发者排名；资源总览（仓库数、总提交数、开发者数）。
- **仓库与提交**：仓库列表、提交列表，单条提交的 **Diff（代码变更）** 查看。
- **单次提交 AI 分析（DeepSeek）**：在「AI 配置」页配置 DeepSeek API Key 与开关；在看板中针对单条 commit 可「运行 AI 分析」，获得工作内容摘要、有效性、代码质量等结构化结果（详见 [docs/AI分析功能说明与测试指南.md](docs/AI分析功能说明与测试指南.md)）。
- **项目每日进展总结（DeepSeek）**：按**仓库 + 业务日**汇总「昨日」有提交时的 commit 信息与已有单次提交 AI 分析结果，调用 DeepSeek 生成一篇 Markdown 日报（进展、工作量、成员贡献等）。「立即生成昨日总结」在 **AI 配置**（全部昨日有提交的仓库）与 **看板**（当前选中仓库）均可触发；**看板**还提供历史总结分页与点开全文。定时任务默认每天 **04:00（Asia/Shanghai）** 处理「昨天」，需在 AI 配置中开启「每日总结」并填写 DeepSeek Key；与「单次提交 AI 分析」开关相互独立。**手动触发**（页面按钮或 `POST /api/admin/ai-analysis/daily-summary/run`）仅需有效 DeepSeek Key，可不开启每日总结开关。数据库迁移：`schema_ai_daily_summary_migration.sql`（生产若走 CI/CD 迁移清单会自动执行）。
- **AI 配置中心**：统一管理 DeepSeek 文本模型与通义·千问（Qwen）多模态模型的 API Key、启用开关、模型名称，并对 AI 调用 Token 用量进行统计与展示（含每日总结相关消耗）。
- **需求 → 报价 → 合同（半自动化，已实现）**：管理员在「报价与合同」中创建报价项目，录入项目类型/技术栈/非功能维度与 **功能模块 + 功能点**（每条含复杂度、数量）。录入方式包括：**手工维护**、按模块 **从预设功能点库添加**（在 **报价配置** `/quote/config` 维护库、风险系数、人天基准、人天单价），以及 **AI 智能录入**：粘贴或上传 `.txt`/`.md` 自然语言需求，由 **DeepSeek** 解析为结构化模块清单（可 **替换全部** 或 **追加** 到现有模块），填入后须人工核对再保存。系统按人天基准与勾选的风险系数计算报价、置信度与审核清单；报价单可导出 **HTML / PDF / Word(.docx)**。**报价单抬头**：支持按系统 **乙方主体模板**（法人/组织或自然人）自动带出出具方名称与联系方式，或本项目 **自定义抬头**；可在报价页编辑 **乙方主体模板**（与配置页同源）。**合同**：可维护 **合同补充信息**（付款计划、含税/发票说明、质保月数、交付物勾选、验收与异议期、里程碑、争议解决方式等），并生成 **附件一（功能清单）**、**附件三（里程碑）** HTML；在配置 DeepSeek 的前提下 **AI 生成合同正文**，在线编辑后导出 **HTML / PDF / Word**。PDF 中文建议在服务器放置中文字体（见 `atuo_attend_backend/src/main/resources/fonts/README.md` 或 `quote.export.cjk-font-path`）。**接口示例**：`POST /api/admin/quote/ai/parse-modules`（AI 解析模块，不落库）。完整设计与合同要素对照见 [docs/需求-报价-合同半自动化产出-功能设计文档.md](docs/需求-报价-合同半自动化产出-功能设计文档.md)。**数据库**：新环境 `schema_quote_mysql.sql`；增量见 `migrate_manifest.txt`（含报价抬头、乙方模板、合同上下文等迁移）。
- **项目协作入口**：从看板页进入「项目协作」，直接访问全部项目与多维表（与员工协作模块共用，权限为 super_admin）。

### 1.3 员工工作台与项目协作（员工或管理员登录协作后）

- **工作台**（`/member`）：展示参与项目数、总提交数、近 7 天提交数；入口进入「项目协作」多维表。
- **项目与多维表**：项目与仓库一一对应、随仓库自动创建；每个项目一张多维表/工作看板。
- **多维表能力**：记录增删改、负责人多选、状态/优先级等选项；**附件上传**（MinIO），**图片预览与下载**（正确 Content-Type，支持 PNG/JPEG/GIF/WebP 等）；**记录级讨论/留言**。
- **任务表 AI 录入模式（Qwen 多模态）**：在项目协作任务表中，通过「AI 录入模式」将客户的自然语言描述 + 选中的项目附件（截图/文件）交给通义·千问多模态模型，由 AI 拆分生成结构化任务草稿，产品可在前端编辑、确认后一键写入多维表（设计详见 [docs/项目协作-AI录入模式-功能设计.md](docs/项目协作-AI录入模式-功能设计.md)）。
- **CSV + AI 智能导入（DeepSeek）**：上传 `.csv` 后不直接按列落库；后端按 **每批最多 30 行数据** 调用 **DeepSeek**，结合当前表的列与选项组将各行清洗为与 AI 录入一致的任务草稿，用户核对后仍走 **`/ai-tasks/commit`** 批量插入。需配置 **DeepSeek API Key**（与报价等共用「AI 配置」）。详见 [docs/多维表格协作管理-功能设计文档.md](docs/多维表格协作管理-功能设计文档.md) §4.8。
- **权限**：超级管理员可看全部项目；子管理员/项目管理员仅被分配的项目；普通成员仅能查看与操作自己参与的项目。

### 1.4 多租户 SaaS：订阅、配额与引流（已实现）

- **档位**：`free`（免费版）、`team`（尝鲜版）、`pro`（专业版，年费）、`pro_plus`（专业增强版，年费）。历史 `enterprise` 在系统中已映射为 `pro_plus`。
- **配额**：按档位限制协作成员、GitHub 绑定报价项目数、**报价项目总数**、**已启用客户看板**项目数、**Agent 会话**累计、**协作项目**总数、**快捷运维（Nexus）云账号**数等；具体数值见 `TenantPlanCatalog`。规则：**已超过上限的历史资源可继续使用，仅禁止继续新建**对应类型资源。
- **会员与计费**（`/subscription`）：展示当前档位、模拟支付续期（不产生真实支付页面）、尝鲜版首月价与年费专业档价格文案、**会员积分**；可 **兑换邀请福利**（尝鲜版试用，每组织限一次）、查看 **本人永久邀请码**（用户码不设次数上限；**官方邀请码**由平台在监测台 **统一发放**，与具体租户无关，可设有效期与总可用次数）。
- **邀请与推荐**：新用户可用邀请码绑定推荐方；被推荐方首年模拟订单消费可按规则产生 **推荐积分**；积分可按产品规则用于兑换（具体以后端实现为准）。

### 1.5 平台监测台（运维专用）

- 代码目录：**`platform_monitor_frontend`**（独立前端工程，与主站 Vue 应用分离）。
- 能力概览：租户列表顶部 **官方邀请码**（全局发放，与租户无关）、**系统配置**（全局 SMTP、日报邮件定时 Cron/时区/开关）、按租户查看用量与订阅、**暂停 / 恢复**、清除管理员会话、**一键开通专业增强版（pro+）** 指定期限权益。
- 鉴权：后端为 `/api/platform/*`；需配置环境变量 **`PLATFORM_OPS_PASSWORD`**（未配置则平台登录不可用）。部署方式可与主站同源反代或单独子域，视运维环境而定。

### 1.6 接口与鉴权

- **租户管理员 API**：`/api/admin/*`（登录/注册、看板、仓库、提交、Diff、报价与合同、Nexus、Agent、客户看板配置、**会员与计费与邀请码**等），登录后返回 token，请求头带 `Authorization: Bearer <token>`。
- **协作 API**：`/api/collab/*`（登录、项目、表、记录、讨论、附件列表/上传/预览/下载），JWT 鉴权；租户管理员登录后台时一并下发 `collabToken`，员工通过协作登录页获取。
- **平台监测 API**：`/api/platform/*`（需平台运维密码登录后的 token）；官方邀请码 **`GET/POST /api/platform/invite-codes`**；系统配置（SMTP、日报调度）**`GET/PUT /api/platform/settings/mail`**、**`GET/PUT /api/platform/settings/report-mail`**、**`POST /api/platform/settings/mail/test`**。
- **Webhook**：`POST /api/webhooks/github`，签名校验与幂等去重。
- **国际化**：前端支持中文、English、Русский、日本語、Français，页头下拉切换，语言偏好存入 localStorage。

---

## 2. 技术栈

| 层级     | 技术 |
|----------|------|
| 前端     | Vue 2、Vue Router、Vue I18n、Axios、Chart.js；平台监测台为 **Vite + Vue 2.7** |
| 后端     | Spring Boot（Java 17） |
| 数据库   | MySQL 8.4 |
| 对象存储 | MinIO（协作附件） |
| 部署     | Docker、Docker Compose；CI/CD 为 GitHub Actions 构建镜像并 SSH 部署 |

---

## 3. 目录结构

```
AutoAttend/
  atuo_attend_backend/        # Spring Boot 后端
  auto_attend_forntend/       # Vue 前端（租户控制台 + 员工工作台 + 项目协作）
  platform_monitor_frontend/  # 平台监测台（独立 Vite 前端）
  docs/                       # 接口文档、功能设计、部署说明、规划与可行性分析
  docker-compose.yml          # 本地/开发：MySQL + MinIO + backend + frontend
  docker-compose.prod.yml     # 生产：仅镜像拉取，不 build
  .github/workflows/          # CI/CD：push 触发构建与部署
```

---

## 4. 本地开发

### 4.1 一键启动（MySQL + MinIO + 后端 + 前端）

在项目根目录执行：

```bash
docker compose up -d
```

- **MySQL**：`127.0.0.1:3306`，库 `autoattend`，用户 `autoattend` / `autoattend_pwd`；自动执行建表脚本（含协作表、AI 分析表）。
- **MinIO**：API `9000`，控制台 `9002`（浏览器 `http://localhost:9002`）。
- **后端**：`http://localhost:8848`。
- **前端**：`http://localhost:8849`（请求 `/api` 由前端容器 Nginx 转发到 backend:8848）。

### 4.2 仅本地跑后端

需本地 JDK 17、Maven、MySQL、MinIO（或仅用 Docker 起 mysql + minio）。后端端口 8848：

```bash
cd atuo_attend_backend
./mvnw spring-boot:run "-Dspring-boot.run.arguments=--server.port=8848"
```
（若本机已安装 Maven，也可使用 `mvn`。）

### 4.3 仅本地跑前端（主站）

```bash
cd auto_attend_forntend
npm install
npm run serve
```

前端 devServer 会将 `/api` 代理到后端（默认 `localhost:8848`，见 `vue.config.js`）。

### 4.4 仅本地跑平台监测台前端

需已启动后端（默认 `8848`），并在后端配置 `PLATFORM_OPS_PASSWORD` 以便登录 `/api/platform/*`。导航含 **系统配置**（`/system`），用于维护全局 SMTP 与日报邮件调度。

```bash
cd platform_monitor_frontend
npm install
npm run dev
```

默认开发端口为 **8850**；`vite.config.js` 已将 `/api` **代理到** `http://127.0.0.1:8848`，与本地后端默认端口一致。

---

## 5. 生产部署

- **推送即自动部署（推荐）**：向 **`main`/`master` push** 后，GitHub Actions 会构建 backend/frontend 镜像并推送到 **ghcr.io**，再通过 SSH **同步** `docker-compose.prod.yml` 与 **`atuo_attend_backend`**（含 `db` 与 **`migrate_manifest.txt`**），最后在服务器 **`docker compose pull` → `up -d` → 强制重建 backend**。因此**无需在服务器上 `git pull`**；线上版本 = 本次流水线构建的镜像 + SCP 过去的迁移脚本。前提：在仓库 **Actions Secrets** 中配置 `SSH_*`（及可选 `DEPLOY_PATH`、私有镜像时的 **`GHCR_USERNAME` + `GHCR_READ_TOKEN`**）。**一键对照清单与常见问题**见 [**docs/Docker推送后自动部署说明.md**](docs/Docker推送后自动部署说明.md)。
- **数据库迁移**：后端容器 entrypoint 按挂载的 **`migrate_manifest.txt`** 执行 SQL；详见 [docs/Docker与CI-CD-数据库迁移.md](docs/Docker与CI-CD-数据库迁移.md)。
- **部署路径**：默认 `/mnt/newdisk/app/AutoAttend`（Secret `DEPLOY_PATH` 可覆盖）。
- **更多**：Secrets 细节、首次起 MySQL、502/反代排查见 [docs/CI-CD-部署说明.md](docs/CI-CD-部署说明.md)。

部署后：

- 管理后台 / 员工工作台 / 项目协作前端：`http(s)://<域名>/`
- 后端 API：`http(s)://<域名>/api/...`
- Webhook：`http(s)://<域名>/api/webhooks/github`

---

## 6. GitHub Webhook 配置

在目标仓库（或组织）的 Webhook 中：

- **Payload URL**：`http(s)://<你的域名>/api/webhooks/github`
- **Content type**：`application/json`
- **Secret**：与后端配置的 `GITHUB_WEBHOOK_SECRET` 一致
- **Events**：至少勾选 `push`（建议再选 `pull_request` 等）

后端会校验 `X-Hub-Signature-256` 并做 delivery 幂等去重。

---

## 7. 配置项（环境变量 / 配置文件）

后端主要配置（示例以环境变量形式，具体以 `application.properties` 为准）：

| 类别     | 配置项示例 |
|----------|------------|
| 数据库   | `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD` |
| 旧版单用户管理员（兼容） | `ADMIN_USERNAME`, `ADMIN_PASSWORD`（多租户模式下以租户管理员为准；详见 `application.properties`） |
| 租户管理员短信（可选） | `ALIYUN_ACCESS_KEY_ID`, `ALIYUN_ACCESS_KEY_SECRET`, `ALIYUN_SMS_SIGN_NAME`, `ALIYUN_SMS_TEMPLATE_CODE`；模板参数名用环境变量配置（勿在 properties 中自引用同名占位符）；`ADMIN_SMS_CODE_TTL_MINUTES`, `ADMIN_SMS_RESEND_INTERVAL_SECONDS` |
| 平台监测台 | `PLATFORM_OPS_PASSWORD`（不配则 `/api/platform/auth/login` 不可用） |
| GitHub   | `GITHUB_WEBHOOK_SECRET`；`GITHUB_TOKEN`（可选，私有仓拉取 Diff 建议配） |
| MinIO    | `MINIO_ENDPOINT`, `MINIO_ACCESS_KEY`, `MINIO_SECRET_KEY`, `MINIO_BUCKET` |
| 协作 JWT | `collab.jwt.secret`, `collab.jwt.expireSeconds`（协作登录 token） |
| AI 分析  | 在「AI 配置」页填写 DeepSeek API Key / Qwen API Key、启用开关、模型（存库），支持单次提交分析、**项目每日进展总结**、协作任务表多模态录入，以及 **报价功能模块 AI 智能录入**（与合同生成共用 DeepSeek Key） |
| 每日总结调度 | `app.daily-summary.enabled`（默认 `true`）、`app.daily-summary.cron`（默认 `0 0 4 * * *`）、`app.daily-summary.timezone`（默认 `Asia/Shanghai`）；关闭 JVM 侧定时任务可设 `app.daily-summary.enabled=false`（业务上仍须在 AI 配置中开启「每日总结」且配置 Key，定时才会真正生成） |

生产环境请修改默认密码、协作 JWT Secret，按需开启短信与平台监测密码。

---

## 8. 文档索引

### 8.1 已实现功能与接口

| 文档 | 说明 |
|------|------|
| [docs/MVP接口文档.md](docs/MVP接口文档.md) | 管理员与 Webhook 接口说明 |
| [docs/Docker推送后自动部署说明.md](docs/Docker推送后自动部署说明.md) | **推送 main 是否自动上线**、Secrets 清单、ghcr 私有镜像、首次服务器准备、验证方式 |
| [docs/CI-CD-部署说明.md](docs/CI-CD-部署说明.md) | 构建、部署、Secrets、常见问题 |
| [docs/Docker与CI-CD-数据库迁移.md](docs/Docker与CI-CD-数据库迁移.md) | Docker 与 CI/CD 下建表脚本 vs 迁移脚本、自动执行迁移、新增迁移规范 |
| [docs/多维表格协作管理-功能设计文档.md](docs/多维表格协作管理-功能设计文档.md) | 项目协作、多维表、权限、MinIO 等设计（已实现部分） |
| [docs/协作项目与表格创建逻辑-原因分析.md](docs/协作项目与表格创建逻辑-原因分析.md) | 解释协作项目与任务表如何随仓库自动创建，以及为何可能出现「有提交但无协作表」的情况 |
| [docs/项目协作-AI录入模式-功能设计.md](docs/项目协作-AI录入模式-功能设计.md) | 项目协作任务表的 AI 录入模式设计（Qwen 多模态 + DeepSeek 结合） |
| [docs/单次提交AI分析-功能设计文档.md](docs/单次提交AI分析-功能设计文档.md) | 单次提交 AI 分析设计（已实现手动触发 + 配置） |
| [docs/AI分析功能说明与测试指南.md](docs/AI分析功能说明与测试指南.md) | AI 分析已实现功能说明与测试步骤 |
| [docs/需求-报价-合同半自动化产出-功能设计文档.md](docs/需求-报价-合同半自动化产出-功能设计文档.md) | **已实现**：报价项目、预设库、AI 解析功能模块、计算与报价单导出、乙方/抬头、合同补充字段、合同 AI 与附件 HTML；文档中含标准合同要素对照与后续迭代项（如需求变更单、附件打包等） |
| [docs/平台监测台-SaaS化扩展设计-审阅稿.md](docs/平台监测台-SaaS化扩展设计-审阅稿.md) | 平台监测台与 SaaS 化扩展的设计与落地记录（订阅档位、配额、运维能力等） |
| `atuo_attend_backend/src/main/resources/db/schema_ai_daily_summary_migration.sql` | 每日进展总结：`aa_ai_analysis_config.daily_summary_enabled` 与表 `aa_project_daily_summary`；已纳入迁移清单时由部署流程自动执行 |

### 8.2 功能规划与可行性分析（见 docs）

以下为 **尚未实现** 或 **部分规划** 的文档，具体范围与实现顺序以各文档为准。

| 文档 | 说明 |
|------|------|
| [docs/工作日报邮件自动推送-功能设计文档.md](docs/工作日报邮件自动推送-功能设计文档.md) | **规划**：基于 commit 数据向员工自动发送工作日报邮件；管理员配置开关、周期、退订与黑名单。 |
| [docs/员工端资源与项目版本管理-功能设计文档.md](docs/员工端资源与项目版本管理-功能设计文档.md) | **规划**：员工端 OSS 资源区浏览/下载、类 Git 项目版本推送、推送记录与活动统计。 |
| [docs/码云-Gitee-自建Git-集成可行性分析.md](docs/码云-Gitee-自建Git-集成可行性分析.md) | **可行性分析**：Gitee/自建 Git（如 Gitea）与 AutoAttend 集成；前端创建仓库并自动关联项目与多维表。 |
| [docs/Code-Server在线开发环境集成-可行性分析报告.md](docs/Code-Server在线开发环境集成-可行性分析报告.md) | **可行性分析**：集成 code-server 在线 IDE；统一开发环境、Cline/DeepSeek 配置与用量管控。 |

产品设计概要见仓库根目录 `产品设计.md` / `产品设计_v2.md`（若有）。

---

## 9. 安全与合规提示

- 租户控制台、Diff 与平台监测数据仅限授权账号访问；生产环境务必修改默认/初始密码、使用 HTTPS；按需开启短信验证码与强密码策略。
- 协作模块使用自有账号（邮箱+密码），默认密码 123456 应在首次登录或由管理员重置后修改。
- Diff 与附件为敏感数据，建议做好访问审计、存储加密与保留期策略；前端避免长期缓存敏感内容。
