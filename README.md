# AutoAttend（MVP）：基于 GitHub Webhook 的开发状态推断（管理员后台）

## 1. 项目简介
AutoAttend 的 MVP 目标是：接入 **GitHub Webhook**，自动抓取提交记录并生成/存档 **完整 `git diff`**，在 **仅管理员可见** 的后台中查看各员工的开发状态、活动证据与预警；同时为后续接入 DeepSeek 等 AI 模型分析（代码质量/有效工作）预留结构，但 **MVP 不调用 AI**。

- **前端**：`auto_attend_forntend`（Vue 2，管理员后台）
- **后端**：`atuo_attend_backend`（Spring Boot，接收Webhook、拉取diff、存储与查询）
- **接口文档**：`docs/MVP接口文档.md`
- **产品文档**：`产品设计_v2.md`

## 2. 目录结构
```
AutoAttend/
  atuo_attend_backend/        # Spring Boot 后端
  auto_attend_forntend/       # Vue 前端（管理员后台）
  docs/
    MVP接口文档.md
  产品设计_v2.md
```

## 3. 本地开发运行
### 3.1 后端（Spring Boot）
前置：
- JDK 17
- Maven 3.8+
- MySQL（MVP需要）
- 服务器需要安装 `git`（用于 `git clone/fetch/diff`）

#### 推荐：用 docker-compose 一键启动 MySQL（含自动建表）
在项目根目录执行：

```bash
docker compose up -d
```

默认会启动一个 MySQL 8.4：
- host：`127.0.0.1`
- port：`3306`
- db：`autoattend`
- user：`root`
- password：`root`

并自动导入建表脚本：`atuo_attend_backend/src/main/resources/db/schema_mysql.sql`

启动（示例）：
```bash
cd atuo_attend_backend
mvn spring-boot:run
```

如果你要按端口规划启动（后端 8848）：

```bash
cd atuo_attend_backend
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.arguments=--server.port=8848"
```

### 3.2 前端（Vue）
前置：
- Node.js 16+（建议）

启动：
```bash
cd auto_attend_forntend
npm install
npm run serve
```

## 4. 生产部署（前后端同机）
建议用 Nginx 统一入口：
- `/`：前端静态文件
- `/api`：反向代理到后端（如 `127.0.0.1:8080`）

### 4.1 前端构建
```bash
cd auto_attend_forntend
npm run build
```
构建产物通常在 `auto_attend_forntend/dist/`，复制到服务器 Nginx 的静态目录。

### 4.2 Nginx 示例（参考）
> 仅示例，路径与端口按你的服务器调整。

```nginx
server {
  listen 80;
  server_name your-domain.com;

  root /var/www/autoattend/dist;
  index index.html;

  location / {
    try_files $uri $uri/ /index.html;
  }

  location /api/ {
    proxy_pass http://127.0.0.1:8080/;
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
  }
}
```

部署后的访问形态建议：
- 管理后台：`http://your-domain.com/`
- 后端API：`http://your-domain.com/api/...`
- Webhook 回调地址：`http://your-domain.com/api/webhooks/github`

## 5. GitHub Webhook 配置（MVP）
在目标仓库（或组织）中配置 Webhook：
- **Payload URL**：`http(s)://<域名>/api/webhooks/github`
- **Content type**：`application/json`
- **Secret**：配置一个随机字符串（与后端配置一致）
- **Events**：至少勾选 `push`（建议再勾选 `pull_request`、`workflow_run`/`check_suite`）

后端必须校验：
- `X-Hub-Signature-256`
- `X-GitHub-Delivery` 幂等去重

## 6. 配置清单（建议以环境变量/配置文件实现）
> 具体键名以你后端实现为准；下面是MVP建议项，便于一次性考虑到位。

- **GitHub**
  - `GITHUB_WEBHOOK_SECRET`：Webhook secret
  - `GITHUB_TOKEN`：访问私有仓库/补拉信息用（可选，但私有仓库建议必配）
- **仓库存储**
  - `REPO_BASE_DIR`：本地仓库存放根目录（如 `D:/data/autoattend/repos`）
  - `REPO_CLONE_TIMEOUT_SEC`、`REPO_FETCH_TIMEOUT_SEC`：git操作超时
- **Diff存储**
  - `DIFF_STORAGE_MODE`：`INLINE`（单字段大文本）或 `CHUNK`（分块）
  - `DIFF_COMPRESS`：是否gzip压缩（建议开启）
  - `DIFF_ENCRYPT`：是否应用层加密（建议开启）
- **数据库**
  - `DB_HOST`、`DB_PORT`、`DB_NAME`
  - `DB_USER`、`DB_PASSWORD`
- **安全**
  - `JWT_SECRET`、`JWT_EXPIRES_IN`
  - 管理员初始账号密码（仅本地/首次启动用，生产请改）

## 7. 安全提醒（MVP必须重视）
因为 MVP 需要 **完整保存并可查看 diff**，这属于高敏数据：
- 仅管理员可访问（RBAC）
- 对 diff 查看与导出做审计
- 建议启用加密存储与保留期清理策略
- 前端页面禁止将 diff 长期缓存到本地（避免泄露）

## 8. 文档
- **MVP接口文档**：`docs/MVP接口文档.md`
- **产品设计**：`产品设计_v2.md`

