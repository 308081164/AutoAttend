# 快原型 Penpot：Docker 部署与排查

本文档说明仓库内 **Penpot 与 AutoAttend 同栈部署** 的方式及常见问题，对应 `docker-compose.penpot.yml`（由 `docker-compose.yml` / `docker-compose.prod.yml` 引用）。

## 1. 架构说明

- **合并方式**：根目录 `docker-compose.yml` 与 `docker-compose.prod.yml` 使用 `include: docker-compose.penpot.yml`，使 `penpot-*` 与 `mysql`、`backend` 等处于 **同一默认网络**，无需额外 `networks` 声明。
- **端口**：
  - **宿主机**：`PENPOT_HTTP_PORT`（默认 **9001**）→ 容器 `penpot-frontend:8080`。
  - **容器内互访**：`backend` 使用 `PENPOT_INTERNAL_URI`（默认 `http://penpot-frontend:8080`）。
- **对外 URL（`PENPOT_PUBLIC_URI`）**：**可选**。不配时，应用生成的「工作区链接」与内网 RPC 基址一致，**只适合调试**；若终端用户需在浏览器打开 Penpot，请设为 **https + 公网域名**（与反代一致）。可另设 `PENPOT_API_BASE_URL` 专用于后端 RPC（一般等于 `PENPOT_INTERNAL_URI`）。

### 获取 Personal Access Token（推荐：自动化，无需打开网页）

在 **不修改 Penpot 源码** 的前提下，可用脚本通过官方 HTTP RPC 完成 **`login-with-password` → `create-access-token`**，将返回的明文 Token 写入部署环境（CI Secret、`.env`），**无需**在浏览器里手动复制。

**前提**：已存在可登录的 Penpot 账号（邮箱 + 密码）。首次部署可由运维在 Penpot 注册页注册 **平台服务账号**，或通过官方 `manage.py` / 邀请流程创建（见 [Penpot 自托管文档](https://help.penpot.app/technical-guide/getting-started/docker/)）。

```bash
cd /path/to/AutoAttend
export PENPOT_BASE_URL=http://127.0.0.1:9001   # 或部署机可访问的 Penpot 根 URL
export PENPOT_EMAIL=your-service@example.com
export PENPOT_PASSWORD='********'
./scripts/penpot-bootstrap-token.sh
# 将输出的单行 token 配置为 PENPOT_ACCESS_TOKEN（勿提交 Git）
```

可选环境变量：`PENPOT_TOKEN_NAME`（默认 `autoattend-bootstrap`）。

**手动方式（备选）**：浏览器登录 → **Your account** → **Access tokens** → Generate（与自动化等价，仅适合排查）。

### 「一账号一 Token」与租户强隔离

| 模式 | 含义 | 能否强隔离 | 说明 |
|------|------|------------|------|
| **平台单 Token** | 全站共用一个 `PENPOT_ACCESS_TOKEN`（服务账号） | **数据层靠你们业务映射** | Penpot 侧文件都挂在同一 profile；隔离依赖你们在 **AutoAttend 租户 ↔ Penpot project/file** 的映射与 API 侧校验，**不是** Penpot 多用户天然隔离。 |
| **每租户管理员一 Token** | 每个租户在 Penpot 有 **独立 profile**，各自 `create-access-token`，存 `aa_tenant` 或密钥表 | **可做到「一账号一 token」** | 需：**(1)** 为该租户在 Penpot **注册/预创建用户**（邮箱可用 `penpot+{tenantId}@你的域` 或 OIDC）；**(2)** 后端用 **该租户对应 Token** 调 RPC；**(3)** 轮换/吊销时按租户更新 DB。**仍不强迫客户手抄**：Token 可由 **你们后端** 在管理员首次绑定 Penpot 时代为调用 `create-access-token` 并加密存储，或使用本脚本在 **开通租户时 CI/运维批量执行**。 |
| **每终端用户一 Token** | 每个业务用户一个 Penpot 账号 + Token | 一般 **不推荐** | 成本高；Penpot 报价/团队模型与你们租户模型要对齐，运维复杂。 |

**结论**：自动化脚本与「一账号一 Token」**不矛盾**。强隔离取决于是否在 Penpot 侧为 **每个隔离单元（建议：每个租户）** 使用 **不同 profile + 不同 Token**，并由后端 **按租户路由** 调用；平台级单 Token 则隔离在 **应用层**，Penpot 仍为共享技术账号。

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

1. **`PENPOT_ENABLED`**：`docker-compose.prod.yml` 中 backend 默认 **`true`**（与 Penpot 栈一起部署时快原型入口可见）。若暂不跑 Penpot，在 `.env` 中显式设 `PENPOT_ENABLED=false`。
2. 在服务器 `DEPLOY_PATH` 下配置 `.env`（与 compose 同目录），建议包含：
   - **`NEXUS_CRYPTO_DEV_KEY`**：租户 Penpot 凭证（Token/密码）用 NexusCryptoService 加密落库；**必须与首次写入时一致**。迁移服务器或重建容器后若未设置或与旧环境不同，会导致 **解密失败**。生产请使用长随机串并长期固定。
   - **`PENPOT_TENANT_CREDENTIAL_PEPPER`**（可选但推荐生产设置）：派生租户 Penpot 登录密码的盐。
   - **`IMAGE_*`**：指向 ghcr 上实际镜像，避免 compose 占位符导致拉取失败（见主 README / 部署说明）。
   - `PENPOT_PUBLIC_URI`：用户浏览器访问 Penpot 的完整 URL（可选；不配则工作区链接可能仅容器内可打开）。
   - `PENPOT_SECRET_KEY`：**勿使用** 仓库默认值。
3. **租户级 Token（当前默认）**：管理员在快原型页首次开通即可，**无需**在服务端配置 `PENPOT_ACCESS_TOKEN`。可选：运维脚本 `scripts/penpot-bootstrap-token.sh` 生成平台级 Token，或 `PENPOT_EMAIL`+`PENPOT_PASSWORD`（兼容旧方式）。
4. CI 会同步 `docker-compose.prod.yml` 与 **`docker-compose.penpot.yml`**；部署脚本会先 `up -d penpot-frontend` 再启动 `backend`。
5. 若经 **反向代理** 仅暴露 443，请把 `PENPOT_PUBLIC_URI` 设为 `https://...`，并在代理层转发到 `127.0.0.1:9001`（或你映射的端口）。

## 4. 常见问题

| 现象 | 可能原因 | 处理 |
|------|----------|------|
| 快原型页没有 **Penpot Beta** 标签 | 后端 `app.penpot.enabled=false`（如旧 `.env` 写死 `PENPOT_ENABLED=false`） | 去掉或改为 `PENPOT_ENABLED=true`，`docker compose up -d --force-recreate backend`；或确认已拉取含 `PENPOT_ENABLED` 默认 true 的 compose |
| `prepare-register-profile HTTP 404` | ① Penpot **2.12+** RPC 路径变更；② 或 **未带 `x-client` 头**（启用 `client-header-check` 时后端拒 RPC，部分反代会映射成 404） | ① **`PENPOT_RPC_PATH_STYLE=auto`（默认）** 自动探测 `/api/main/methods` 与 `/api/rpc/command`；② 后端已默认发送 **`x-client: penpot-backend`**（可用 `PENPOT_CLIENT_HEADER` 覆盖）。部署含该修复的后端镜像并 `docker compose up -d --force-recreate backend` |
| `backend` 起不来 / 一直等待 | `depends_on` 等待 `penpot-frontend` | `docker compose ps`、`docker logs penpot-postgres`、`docker logs penpot-frontend`；先单独 `up -d penpot-frontend` 看依赖是否 healthy |
| 浏览器打开 Penpot 空白或资源 404 | `PENPOT_PUBLIC_URI` 与浏览器地址不一致 | 将 `PENPOT_PUBLIC_URI` 改为实际访问 URL（含协议与端口） |
| 宿主机 9001 已被占用 | 端口冲突 | 设置 `PENPOT_HTTP_PORT=其他端口`，并同步修改 `PENPOT_PUBLIC_URI` 中的端口 |
| 与 MinIO 控制台端口混淆 | 本仓库 MinIO 控制台映射为 **9002** | Penpot 默认 **9001**，一般不与 API 的 9000 冲突 |
| 邮件调试端口冲突 | mailcatcher 默认 **1080** | 设置 `PENPOT_MAILCATCHER_HTTP_PORT` |

## 5. 安全提示

- **生产**务必关闭或收紧 Penpot 注册策略，并按 [Penpot 配置文档](https://help.penpot.app/technical-guide/configuration/) 启用 `secure-session-cookies` 等（当前 compose 片段沿用官方开发向 flags，上线前需评审）。
- `PENPOT_SECRET_KEY` 泄露会导致会话风险，仅通过环境变量注入，勿提交 Git。
