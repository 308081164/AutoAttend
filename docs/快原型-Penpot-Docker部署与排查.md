# 快原型 Penpot：Docker 部署与排查

本文档说明仓库内 **Penpot 与 AutoAttend 同栈部署** 的方式及常见问题，对应 `docker-compose.penpot.yml`（由 `docker-compose.yml` / `docker-compose.prod.yml` 引用）。

## 1. 架构说明

- **合并方式**：根目录 `docker-compose.yml` 与 `docker-compose.prod.yml` 使用 `include: docker-compose.penpot.yml`，使 `penpot-*` 与 `mysql`、`backend` 等处于 **同一默认网络**，无需额外 `networks` 声明。
- **端口**：
  - **宿主机**：`PENPOT_HTTP_PORT`（默认 **9001**）→ 容器 `penpot-frontend:8080`。
  - **容器内互访**：`backend` 使用 `PENPOT_INTERNAL_URI`（默认 `http://penpot-frontend:8080`）。
- **对外 URL**：`PENPOT_PUBLIC_URI` 必须可被 **浏览器** 访问（用于 Penpot 前端与后端协同）；开发多为 `http://localhost:9001`，生产请改为 **https + 域名** 或经网关的路径。

## 2. 首次启动（本地）

```bash
cd /path/to/AutoAttend
cp .env.example .env
# 建议设置 PENPOT_SECRET_KEY（随机 512-bit，见 .env.example 注释）

docker compose -f docker-compose.yml pull penpot-frontend penpot-backend penpot-exporter penpot-postgres penpot-valkey penpot-mailcatch
docker compose -f docker-compose.yml up -d penpot-frontend
docker compose -f docker-compose.yml up -d backend frontend
```

连通性（在 **backend 容器内**）：

```bash
docker exec autoattend-backend sh -c 'curl -sS -o /dev/null -w "%{http_code}\n" --connect-timeout 5 "${PENPOT_INTERNAL_URI:-http://penpot-frontend:8080}/" || echo fail'
```

期望：HTTP 状态码为 **200** 或 **302**（Penpot 可能重定向到登录页）。

浏览器访问：`http://localhost:9001`（或你设置的 `PENPOT_HTTP_PORT`）。

## 3. 生产（docker-compose.prod.yml）

1. 在服务器 `DEPLOY_PATH` 下配置 `.env`（与 compose 同目录），至少设置：
   - `PENPOT_PUBLIC_URI`：用户浏览器访问 Penpot 的完整 URL。
   - `PENPOT_SECRET_KEY`：**勿使用** 仓库默认值。
2. **AutoAttend 后端调用 Penpot（方案 A）**：在 Penpot 网页 **设置 → Access tokens** 生成令牌，写入部署环境：
   - `PENPOT_ENABLED=true`
   - `PENPOT_ACCESS_TOKEN=<令牌>`
   - `PENPOT_INTERNAL_URI`：与 Compose 内 `penpot-frontend:8080` 一致（默认已配）。
   备选：配置 `PENPOT_EMAIL` + `PENPOT_PASSWORD`（内置账号密码，由后端登录换 Cookie）。
3. CI 会同步 `docker-compose.prod.yml` 与 **`docker-compose.penpot.yml`**；部署脚本会先 `up -d penpot-frontend` 再启动 `backend`。
4. 若经 **反向代理** 仅暴露 443，请把 `PENPOT_PUBLIC_URI` 设为 `https://...`，并在代理层转发到 `127.0.0.1:9001`（或你映射的端口）。

## 4. 常见问题

| 现象 | 可能原因 | 处理 |
|------|----------|------|
| `backend` 起不来 / 一直等待 | `depends_on` 等待 `penpot-frontend` | `docker compose ps`、`docker logs penpot-postgres`、`docker logs penpot-frontend`；先单独 `up -d penpot-frontend` 看依赖是否 healthy |
| 浏览器打开 Penpot 空白或资源 404 | `PENPOT_PUBLIC_URI` 与浏览器地址不一致 | 将 `PENPOT_PUBLIC_URI` 改为实际访问 URL（含协议与端口） |
| 宿主机 9001 已被占用 | 端口冲突 | 设置 `PENPOT_HTTP_PORT=其他端口`，并同步修改 `PENPOT_PUBLIC_URI` 中的端口 |
| 与 MinIO 控制台端口混淆 | 本仓库 MinIO 控制台映射为 **9002** | Penpot 默认 **9001**，一般不与 API 的 9000 冲突 |
| 邮件调试端口冲突 | mailcatcher 默认 **1080** | 设置 `PENPOT_MAILCATCHER_HTTP_PORT` |

## 5. 安全提示

- **生产**务必关闭或收紧 Penpot 注册策略，并按 [Penpot 配置文档](https://help.penpot.app/technical-guide/configuration/) 启用 `secure-session-cookies` 等（当前 compose 片段沿用官方开发向 flags，上线前需评审）。
- `PENPOT_SECRET_KEY` 泄露会导致会话风险，仅通过环境变量注入，勿提交 Git。
