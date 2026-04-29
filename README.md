# 流帮 Project（AutoAttend）

**Slogan：软件流程，帮你搞定！**

> 仓库与工程目录仍沿用历史名称（`AutoAttend`、`atuo_attend_backend`、`auto_attend_forntend`）；**产品对外**可使用「流帮 Project」与上述 Slogan。数据库表名、Java 包名、REST 路径等未改。

基于 GitHub Webhook 的**多租户**研发协作与交付平台：租户管理员可进行提交追踪、Diff、看板与 AI 分析、**需求 → 报价 → 合同**半自动化、**快原型（含 Penpot Beta）**、订阅与计费、平台监测台等；员工通过协作端参与项目与**多维表格**（任务、附件、AI/CSV 导入等）。

---

## 目录

- [在线体验](#在线体验测试环境)
- [核心能力一览](#核心能力一览)
- [功能概览](#功能概览)
- [技术栈](#技术栈)
- [仓库结构](#仓库结构)
- [本地开发](#本地开发)
- [生产部署与 CI/CD](#生产部署与-cicd)
- [环境变量与配置要点](#环境变量与配置要点)
- [GitHub Webhook](#github-webhook-配置)
- [文档索引](#文档索引)
- [安全提示](#安全与合规提示)

---

## 在线体验

- 在线地址：<https://liubangproject.tech>
- 旧域名（已重定向）：<http://autoattend.广厦智汇.com>

---

## 核心能力一览

| 领域 | 说明 |
|------|------|
| **租户与协作** | 管理员注册/登录（可选阿里云短信）、员工协作登录；项目与仓库联动，多维表、附件（MinIO）、讨论区 |
| **研发可视化** | Webhook 拉取提交与 Diff；看板统计；单次提交 **DeepSeek** 分析；**每日进展总结**（按业务日） |
| **报价与合同** | 功能模块与人天报价、预设库与风险系数；**AI 解析**自然语言需求；报价/合同 **HTML、PDF、Word** 导出；乙方主体模板与抬头 |
| **快原型** | 结构化 Spec、HTML/CSS Mockup；**Penpot Beta**：平台 AI 规划布局并写入 **Penpot** 画布、导出 `.penpot`；**每租户独立 Penpot 账号与加密 Token**（管理员首次开通，无需在服务端配置平台级 `PENPOT_ACCESS_TOKEN`） |
| **SaaS 与计费** | 多档位订阅与配额、会员积分、邀请码与推荐；**平台监测台**（租户运维、官方邀请码、邮件/日报调度等） |
| **交付与运维** | 可选 **Nexus** 快捷运维扩展；平台监测独立前端 |

更细的设计与排障见 `docs/` 下各文档。

---

## 功能概览

### 登录与角色

- **租户管理员**：`/register`、`/login`（手机号 + 密码；可配置短信验证码）。注册可填邀请码。登录后可进管理端与「项目协作」。
- **员工**：登录页「员工登录」→ `/collab-login`，进入工作台 `/member` 与项目协作。

### 租户控制台（管理员）

- **GitHub Webhook**、看板与仓库/提交/**Diff**
- **AI 配置**：DeepSeek / 通义千问 Key、用量统计；单次提交分析、每日总结、报价 AI 等共用配置
- **报价与合同**：见 [docs/需求-报价-合同半自动化产出-功能设计文档.md](docs/需求-报价-合同半自动化产出-功能设计文档.md)
- **快原型**：Spec / Mockup / **Penpot Beta**；Penpot 与主栈一并 **Docker Compose** 编排（`docker-compose.penpot.yml`）。产品说明与部署排障：[docs/快原型-Penpot-Beta-功能设计.md](docs/快原型-Penpot-Beta-功能设计.md)、[docs/快原型-Penpot-Docker部署与排查.md](docs/快原型-Penpot-Docker部署与排查.md)
- **项目协作入口**、**会员与计费**、**邀请码** 等（详见原功能文档与 `TenantPlanCatalog` 配额规则）

### 员工协作

- 多维表、附件、讨论、**AI 录入（Qwen）**、**CSV + DeepSeek 批量导入** 等  
  详见 [docs/多维表格协作管理-功能设计文档.md](docs/多维表格协作管理-功能设计文档.md)

### 平台监测台

- 独立前端工程 **`platform_monitor_frontend`**，访问 `/api/platform/*`，需配置 **`PLATFORM_OPS_PASSWORD`**

### API 约定（摘要）

- 管理员：`/api/admin/*`，`Authorization: Bearer <token>`
- 协作：`/api/collab/*`，JWT
- 平台：`/api/platform/*`
- Webhook：`POST /api/webhooks/github`

---

## 技术栈

| 层级 | 技术 |
|------|------|
| 主前端 | Vue 2、Vue Router、Vue I18n、Axios |
| 监测台前端 | Vite + Vue 2.7 |
| 后端 | Spring Boot 3、Java 17 |
| 数据库 | MySQL 8.4 |
| 对象存储 | MinIO |
| 可选组件 | Penpot（开源设计协作）、Valkey、PostgreSQL（Penpot 专用） |
| 部署 | Docker Compose；CI/CD：GitHub Actions → ghcr.io → SSH 部署 |

---

## 仓库结构

```
AutoAttend/
  atuo_attend_backend/           # Spring Boot 后端
  auto_attend_forntend/          # Vue 主前端（管理端 + 协作端）
  platform_monitor_frontend/     # 平台监测台（Vite）
  docs/                          # 功能设计、部署、接口说明
  docker-compose.yml             # 本地开发：MySQL + MinIO + Penpot + backend + frontend
  docker-compose.prod.yml        # 生产：拉取镜像（含 include Penpot）
  docker-compose.penpot.yml      # Penpot 服务栈（被 compose include）
  .github/workflows/deploy.yml   # push main：构建镜像、SCP、compose 部署
```

---

## 本地开发

### 一键 Docker（推荐）

```bash
docker compose up -d
```

- MySQL：`127.0.0.1:3306`，库 `autoattend`，用户 `autoattend` / `autoattend_pwd`
- MinIO：API `9000`，控制台 `9002`
- 后端：`http://localhost:8848`
- 主前端：`http://localhost:8849`（Nginx 将 `/api` 转发到 backend）
- Penpot：默认宿主机端口 **9001**（见 `.env.example` 中 `PENPOT_HTTP_PORT`）

### 仅后端

```bash
cd atuo_attend_backend
./mvnw spring-boot:run "-Dspring-boot.run.arguments=--server.port=8848"
```

### 仅主前端

```bash
cd auto_attend_forntend
npm ci
npm run serve
```

### 仅监测台前端

```bash
cd platform_monitor_frontend
npm ci
npm run dev
# 默认端口 8850，代理 /api → 8848
```

---

## 生产部署与 CI/CD

### 自动部署（推荐）

向 **`main` 或 `master`** **push** 后，`.github/workflows/deploy.yml` 会：

1. 校验数据库迁移清单、构建主前端 sanity check  
2. 构建并推送 **`autoattend-backend`**、**`autoattend-frontend`**、**`platform-monitor-frontend`** 到 **ghcr.io**（`latest`）  
3. SSH 将 **`docker-compose.prod.yml`**、**`docker-compose.penpot.yml`**、**整个 `atuo_attend_backend/`**（含 `db` 与 `migrate_manifest.txt`）同步到服务器 **`DEPLOY_PATH`**  
4. 服务器执行 **`docker compose pull`**、分步 **`up -d`**、强制重建 **backend**（执行迁移）、等待 **8848** 可用后重建前端容器  

**线上代码与 SQL 以 SCP 到服务器的 `atuo_attend_backend` 为准**；镜像以 ghcr 为准。无需在服务器 `git pull`。

详细 Secrets、首次起服、ghcr 私有包登录等见：

- [docs/Docker推送后自动部署说明.md](docs/Docker推送后自动部署说明.md)  
- [docs/CI-CD-部署说明.md](docs/CI-CD-部署说明.md)  
- [docs/Docker与CI-CD-数据库迁移.md](docs/Docker与CI-CD-数据库迁移.md)

### 服务器目录与 Secret

| 项 | 说明 |
|----|------|
| **`DEPLOY_PATH`** | GitHub Secret，默认 **`/mnt/newdisk/app/AutoAttend`**；须与实际部署目录一致 |
| **同目录 `.env`** | **强烈建议**配置 **`IMAGE_*`**（见上），否则会出现 **`manifest unknown`**。另建议配置 **`NEXUS_CRYPTO_DEV_KEY`**：与加密租户 Penpot 等凭证时一致，**迁移或换机后若改变会导致库内密文无法解密**；生产用长随机串并长期固定。 |
| **`PENPOT_ENABLED`** | `docker-compose.prod.yml` 中 backend 默认 **`true`**（与 Penpot 栈一起用时快原型 Penpot 入口可见）；若 `.env` 里写死 `false` 会覆盖默认并隐藏功能。 |
| **阿里云短信** | `ALIYUN_*` 见 `.env.example`；写入部署目录 `.env`，**勿提交 Git** |

### Docker 数据目录（运维可选）

镜像与命名卷默认在 Docker **`data-root`**（常见为 `/var/lib/docker`）。若需将**全部** Docker 数据放到独立数据盘（例如 **`/mnt/newdisk/docker`**），在服务器 **`/etc/docker/daemon.json`** 中设置 **`"data-root"`**，迁移步骤与注意点需单独规划（磁盘挂载、空间、备份）。**CI 脚本不依赖**该路径，仅改守护进程配置即可。

---

## 环境变量与配置要点

根目录 **`.env.example`** 汇总了 Compose 常用变量（Penpot、短信、可选镜像名等）。生产在 **`DEPLOY_PATH` 下**维护 **`.env`**，`chmod 600 .env`。

后端详细项见 `atuo_attend_backend/src/main/resources/application.properties`，主要包括：

| 类别 | 示例 |
|------|------|
| 数据库 | `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD` |
| MinIO | `MINIO_ENDPOINT`, `MINIO_ACCESS_KEY`, `MINIO_SECRET_KEY`, `MINIO_BUCKET` |
| 短信（可选） | `ALIYUN_ACCESS_KEY_ID`, `ALIYUN_ACCESS_KEY_SECRET`, `ALIYUN_SMS_SIGN_NAME`, `ALIYUN_SMS_TEMPLATE_CODE` |
| 平台监测 | `PLATFORM_OPS_PASSWORD` |
| GitHub | `GITHUB_TOKEN`（可选）；Webhook 密钥等 |
| Penpot（后端 `app.penpot.*`） | `PENPOT_ENABLED`、内网/公网 URI、租户邮箱域与 **`PENPOT_TENANT_CREDENTIAL_PEPPER`** 等；详见 `.env.example` 与快原型文档 |
| AI | 多在管理后台「AI 配置」落库，部分默认值在 `application.properties` |

---

## GitHub Webhook 配置

- **URL**：`https://<你的域名>/api/webhooks/github`  
- **Content type**：`application/json`  
- **Secret**：与后端配置一致（`GITHUB_WEBHOOK_SECRET` / 环境变量）  
- **Events**：至少 `push`

---

## 文档索引

| 文档 | 内容 |
|------|------|
| [docs/Docker推送后自动部署说明.md](docs/Docker推送后自动部署说明.md) | push 是否自动上线、Secrets、`DEPLOY_PATH`、验证步骤 |
| [docs/Docker与CI-CD-数据库迁移.md](docs/Docker与CI-CD-数据库迁移.md) | `migrate_manifest.txt` 与增量迁移规范 |
| [docs/快原型-Penpot-Beta-功能设计.md](docs/快原型-Penpot-Beta-功能设计.md) | Penpot Beta 能力与租户 Token 说明 |
| [docs/快原型-Penpot-Docker部署与排查.md](docs/快原型-Penpot-Docker部署与排查.md) | Penpot Compose、端口、自动化 Token、排障 |
| [docs/需求-报价-合同半自动化产出-功能设计文档.md](docs/需求-报价-合同半自动化产出-功能设计文档.md) | 报价与合同业务设计 |
| [docs/多维表格协作管理-功能设计文档.md](docs/多维表格协作管理-功能设计文档.md) | 协作与多维表 |
| [docs/MVP接口文档.md](docs/MVP接口文档.md) | 管理员与 Webhook 接口摘要 |

更多规划类、可行性文档见 `docs/` 目录内其他文件。

---

## 安全与合规提示

- 生产务必使用 **HTTPS**，修改默认密码与 **JWT/协作密钥**；**AccessKey、数据库密码、`.env` 勿提交 Git**。  
- 协作端默认初始密码等策略请以实际部署配置为准，并引导用户修改。  
- Diff、附件与业务数据属敏感信息，注意访问控制与留存策略。

---

*文档版本随仓库更新；具体行为以代码与 `application.properties` 为准。*
