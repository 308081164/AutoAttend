# 推送代码后是否自动部署为最新版本？

**可以。** 仓库已配置 GitHub Actions（`.github/workflows/deploy.yml`）：向 **`main` 或 `master`** 分支 **push** 后，会自动完成：

1. 在 GitHub 云端构建 **backend**、**frontend** 镜像并推送到 **ghcr.io**（`latest` 标签）。
2. 通过 **SSH** 将 `docker-compose.prod.yml` 与整个 **`atuo_attend_backend`** 目录同步到服务器上的 **`DEPLOY_PATH`**（与仓库内迁移脚本、清单一致）。
3. 在服务器执行 **`docker compose pull`** → **`up -d`** → **强制重建 `backend` 容器**，从而：
   - 拉取刚构建的新镜像；
   - 每次部署 backend 都会启动一次，**按 `migrate_manifest.txt` 执行数据库迁移**（见 [Docker与CI-CD-数据库迁移.md](./Docker与CI-CD-数据库迁移.md)）。

> **说明**：部署**不是**在服务器上执行 `git pull`；代码与 SQL 以 **SCP 同步的 `atuo_attend_backend`** 为准，镜像以 **ghcr** 为准。

若你希望「推送到其他分支也部署」，可在 `deploy.yml` 里修改 `on.push.branches`。

---

## 一、你需要满足的条件（清单）

| 项 | 说明 |
|----|------|
| 分支 | 合并并 push 到 **`main`** 或 **`master`**。 |
| Actions | 仓库 **Settings → Actions → General** 允许执行 Workflow；fork 仓库需在 **Actions** 里启用。 |
| Secrets | 已配置 `SSH_HOST`、`SSH_USER`、`SSH_PRIVATE_KEY`（必配）；可选 `SSH_PORT`、`DEPLOY_PATH`。 |
| 服务器 | 已安装 **Docker** 与 **Docker Compose v2**；SSH 用户有权执行 `docker`（通常加入 `docker` 组或 root）。 |
| ghcr 拉取 | 镜像包为**公开**时，一般无需额外配置；为**私有**时须在 Secrets 中配置 **`GHCR_USERNAME` + `GHCR_READ_TOKEN`**（见下文）。 |

全部满足后，**每次 push 到 main 即一次完整自动部署**（构建 + 同步 + 拉镜像 + 重启 + 迁移）。

---

## 二、GitHub Secrets 一览

### 必填（SSH 部署）

| Secret | 说明 |
|--------|------|
| `SSH_PRIVATE_KEY` | 登录服务器的私钥全文（含 `BEGIN/END`）。 |
| `SSH_HOST` | 服务器 IP 或域名。 |
| `SSH_USER` | SSH 用户名（如 `root`）。 |

### 可选

| Secret | 说明 |
|--------|------|
| `SSH_PORT` | SSH 端口，默认 **22**。 |
| `DEPLOY_PATH` | 服务器上的项目目录，默认 **`/mnt/newdisk/app/AutoAttend`**。须与首次在服务器上准备的目录一致；workflow 中的 **SCP** 与本脚本 **`cd`** 都使用该路径。 |
| `GHCR_USERNAME` | GitHub 用户名或组织名（小写与镜像路径一致即可）。 |
| `GHCR_READ_TOKEN` | 用于在**服务器**上 `docker login ghcr.io` 的 **Personal Access Token**（Classic：**read:packages**；或 Fine-grained：对对应包只读）。 |

> 若 `docker compose pull` 报 **`unauthorized`**、**`denied`**：多为 ghcr 包设为私有且服务器未登录。请配置上述两个 GHCR 相关 Secret，重新跑一次 workflow。

---

## 三、服务器首次准备（只做一次）

1. **创建目录**（与 `DEPLOY_PATH` 一致），无需先 `git clone` 也可（首次部署时 SCP 会写入 `atuo_attend_backend` 与 compose 文件）：
   ```bash
   sudo mkdir -p /mnt/newdisk/app/AutoAttend/git-repos
   sudo chown -R "$USER:$USER" /mnt/newdisk/app/AutoAttend
   ```

2. **（推荐）先起 MySQL，初始化数据卷**（空卷时自动执行 `01`–`04` 建表脚本）：
   ```bash
   cd /mnt/newdisk/app/AutoAttend
   # 首次若尚无 compose 文件，可先手动从本机 scp 一份 docker-compose.prod.yml 与 atuo_attend_backend，或等第一次 CI 成功后再执行：
   docker compose -f docker-compose.prod.yml up -d mysql
   ```

3. **首次拉镜像**（在已配置好 `IMAGE_*` 或由第一次成功的 Actions 部署自动设置后）：
   - 第一次 CI 会写入 `IMAGE_BACKEND` / `IMAGE_FRONTEND` 环境变量并 `pull`；若手动调试：
   ```bash
   export IMAGE_BACKEND=ghcr.io/<你的GitHub用户名小写>/autoattend-backend:latest
   export IMAGE_FRONTEND=ghcr.io/<你的GitHub用户名小写>/autoattend-frontend:latest
   docker compose -f docker-compose.prod.yml pull
   docker compose -f docker-compose.prod.yml up -d
   ```

4. **（私有 ghcr）在服务器手动登录一次验证**（可选）：
   ```bash
   echo "<PAT>" | docker login ghcr.io -u "<GitHub用户名>" --password-stdin
   ```

更多故障排查（502、磁盘满、反代）见 [CI-CD-部署说明.md](./CI-CD-部署说明.md)。

---

## 四、如何确认「已是当前推送的版本」

1. **GitHub**：打开仓库 **Actions**，查看最新 **Build and Deploy** 运行是否**绿色成功**。
2. **镜像**：在服务器执行：
   ```bash
   docker images | grep autoattend
   ```
   查看 **CREATED** 是否为刚才构建时间附近。
3. **应用**：访问前端页面或调用带版本说明的接口（若有）；后端日志中可见新代码行为。

---

## 五、与「仅数据库迁移」的关系

- **表结构变更**：在 `atuo_attend_backend/src/main/resources/db/` 增加 SQL，并**追加**到 **`migrate_manifest.txt`**，与代码一并 push。
- 部署时 **backend 容器每次强制重建**，entrypoint 会按清单执行 SQL（幂等脚本可重复执行）。

详见 [Docker与CI-CD-数据库迁移.md](./Docker与CI-CD-数据库迁移.md)。

---

## 六、不通过 GitHub 时的手动部署（备用）

在能访问 ghcr 的机器上构建/拉取镜像后，将 `docker-compose.prod.yml` 与 `atuo_attend_backend`（含 `db` 目录）放到服务器 `DEPLOY_PATH`，执行：

```bash
export IMAGE_BACKEND=ghcr.io/<owner>/autoattend-backend:latest
export IMAGE_FRONTEND=ghcr.io/<owner>/autoattend-frontend:latest
docker compose -f docker-compose.prod.yml pull
docker compose -f docker-compose.prod.yml up -d
docker compose -f docker-compose.prod.yml up -d --no-deps --force-recreate backend
```

---

## 七、相关文件

| 文件 | 作用 |
|------|------|
| `.github/workflows/deploy.yml` | push → 构建、推镜像、SCP、compose pull/up、重建 backend |
| `docker-compose.prod.yml` | 生产编排；backend 挂载 `./atuo_attend_backend/.../db` → `/app/db` |
| `atuo_attend_backend/docker-entrypoint.sh` | 等待 MySQL、执行迁移清单、启动 Spring Boot |

---

**结论**：配置好 Secrets 与服务器 Docker 环境后，**推送到 `main`/`master` 即可自动部署为当前提交对应的最新镜像与最新迁移脚本**；若拉镜像失败，优先检查 ghcr 可见性与 **`GHCR_USERNAME` / `GHCR_READ_TOKEN`**。
