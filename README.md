# AutoAttend

基于 GitHub Webhook 的开发状态追踪与项目协作平台：管理员可查看提交与 Diff、看板统计；成员可登录后按项目使用多维表格协作（任务/需求管理、讨论、附件）。

---

## 1. 功能概览

### 1.1 管理员后台（需管理员登录）

- **GitHub Webhook**：接入仓库 Push 等事件，自动拉取 commit 并生成完整 `git diff` 存档。
- **状态看板**：按时间范围与仓库查看「活跃开发 / 评审中 / 修复构建 / 阻塞 / 无信号」等统计。
- **仓库与提交**：仓库列表、提交列表、单条提交的 **Diff（代码变更）** 查看。
- **项目协作入口**：从看板页进入「项目协作」多维表（见下）。

### 1.2 项目协作（多维表格，成员/管理员）

- **自有账号登录**：邮箱 + 密码；账号来源于 Webhook 推送中的 commit 作者（默认密码 123456）。
- **项目与多维表**：项目与仓库一一对应、随仓库自动创建；每个项目一张多维表/工作看板。
- **多维表能力**：记录增删改、负责人多选、状态/优先级等选项、**附件上传与下载**（MinIO）、**记录级讨论/留言**。
- **权限**：超级管理员可看全部项目、创建子管理员、分配项目权限；子管理员/项目管理员仅管理被分配的项目；普通成员仅能查看与操作自己参与的项目。

### 1.3 接口与鉴权

- **管理员 API**：`/api/admin/*`（登录、看板、仓库、提交、Diff 等），JWT 鉴权。
- **协作 API**：`/api/collab/*`（登录、项目、表、记录、讨论、附件），JWT 鉴权。
- **Webhook**：`POST /api/webhooks/github`，签名校验与幂等去重。
- **国际化**：前端支持中文、English、Русский、日本語、Français，页头下拉切换，语言偏好存入 localStorage。

---

## 2. 技术栈

| 层级     | 技术 |
|----------|------|
| 前端     | Vue 2、Vue Router、Vue I18n、Axios |
| 后端     | Spring Boot（Java 17） |
| 数据库   | MySQL 8.4 |
| 对象存储 | MinIO（附件） |
| 部署     | Docker、Docker Compose；CI/CD 为 GitHub Actions 构建镜像并 SSH 部署 |

---

## 3. 目录结构

```
AutoAttend/
  atuo_attend_backend/     # Spring Boot 后端
  auto_attend_forntend/    # Vue 前端（管理后台 + 项目协作）
  docs/                    # 接口文档、功能设计、部署说明
  docker-compose.yml       # 本地/开发：MySQL + MinIO + backend + frontend
  docker-compose.prod.yml  # 生产：仅镜像拉取，不 build
  .github/workflows/       # CI/CD：push 触发构建与部署
```

---

## 4. 本地开发

### 4.1 一键启动依赖（MySQL + MinIO + 后端 + 前端）

在项目根目录执行：

```bash
docker compose up -d
```

- **MySQL**：`127.0.0.1:3306`，库 `autoattend`，用户 `autoattend` / `autoattend_pwd`；自动执行建表脚本（含协作表）。
- **MinIO**：API `9000`，控制台 `9002`（浏览器 `http://localhost:9002`）。
- **后端**：`http://localhost:8848`。
- **前端**：`http://localhost:8849`（页面内请求 `/api` 会由前端容器 Nginx 转发到 backend:8848）。

### 4.2 仅本地跑后端（不用 Docker 跑 backend）

需本地 JDK 17、Maven、MySQL、MinIO（或仍用 Docker 只起 mysql + minio）。后端端口 8848：

```bash
cd atuo_attend_backend
mvn spring-boot:run "-Dspring-boot.run.arguments=--server.port=8848"
```

### 4.3 仅本地跑前端

```bash
cd auto_attend_forntend
npm install
npm run serve
```

前端 devServer 会将 `/api` 代理到后端（默认 `localhost:8848`，见 `vue.config.js`）。

---

## 5. 生产部署

- **推荐**：使用 GitHub Actions 在 push 到 `main`/`master` 时自动构建镜像、推送到 ghcr.io，并 SSH 到服务器执行 `docker compose -f docker-compose.prod.yml pull && up -d`。
- **部署路径**：默认服务器项目目录为 `/mnt/newdisk/app/AutoAttend`（可在仓库 Secrets 中设置 `DEPLOY_PATH` 覆盖）。
- **详细步骤**：见 [docs/CI-CD-部署说明.md](docs/CI-CD-部署说明.md)（Secrets 配置、首次克隆、常见问题如 502、磁盘满、/api 502 等）。

部署后：

- 管理后台 / 项目协作前端：`http(s)://<域名>/`
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
| 管理员   | `ADMIN_USERNAME`, `ADMIN_PASSWORD`（默认 admin / admin123） |
| GitHub   | `GITHUB_WEBHOOK_SECRET`；`GITHUB_TOKEN`（可选，私有仓建议配） |
| MinIO    | `MINIO_ENDPOINT`, `MINIO_ACCESS_KEY`, `MINIO_SECRET_KEY`, `MINIO_BUCKET` |
| JWT      | `JWT_SECRET`, `JWT_EXPIRES_IN`（协作登录等） |
| 仓库存储 | `REPO_BASE_DIR`，以及 clone/fetch 超时等（若使用） |

生产环境请修改默认管理员密码与 JWT Secret。

---

## 8. 文档索引

| 文档 | 说明 |
|------|------|
| [docs/MVP接口文档.md](docs/MVP接口文档.md) | 管理员与 Webhook 接口说明 |
| [docs/CI-CD-部署说明.md](docs/CI-CD-部署说明.md) | 构建、部署、Secrets、常见问题 |
| [docs/多维表格协作管理-功能设计文档.md](docs/多维表格协作管理-功能设计文档.md) | 项目协作、多维表、权限、MinIO 等设计 |
| [docs/工作日报邮件自动推送-功能设计文档.md](docs/工作日报邮件自动推送-功能设计文档.md) | 工作日报邮件功能设计 |
| [docs/单次提交AI分析-功能设计文档.md](docs/单次提交AI分析-功能设计文档.md) | 单次提交 AI 分析设计 |
| [docs/需求-报价-合同半自动化产出-功能设计文档.md](docs/需求-报价-合同半自动化产出-功能设计文档.md) | 需求/报价/合同半自动化产出设计 |
| [docs/员工端资源与项目版本管理-功能设计文档.md](docs/员工端资源与项目版本管理-功能设计文档.md) | 员工端资源与版本管理设计 |
| 产品设计.md / 产品设计_v2.md | 产品设计概要 |

---

## 9. 安全与合规提示

- 管理员后台与 Diff 数据仅限授权管理员访问；生产环境务必改默认密码、使用 HTTPS。
- 协作模块使用自有账号（邮箱+密码），默认密码 123456 应在首次登录或由管理员重置后修改。
- Diff 与附件为敏感数据，建议做好访问审计、存储加密与保留期策略；前端避免长期缓存敏感内容。
