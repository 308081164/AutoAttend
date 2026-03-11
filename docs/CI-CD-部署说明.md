# AutoAttend CI/CD + 云端构建 + 自动部署说明

## 一、整体流程

```
开发者 push 代码到 GitHub (main/master)
    ↓
GitHub Actions 触发
    ↓
① 在 GitHub 云端构建 backend / frontend Docker 镜像（不占用你服务器）
    ↓
② 将镜像推送到 GitHub Container Registry (ghcr.io)
    ↓
③ 通过 SSH 登录你的服务器，执行：
      git pull  →  docker compose -f docker-compose.prod.yml pull  →  up -d
    ↓
服务器上只做「拉取已构建好的镜像 + 重启容器」，几十秒内完成更新
```

这样**构建在 GitHub 上完成**，**服务器不再执行 `compose build`**，部署会快很多。

---

## 二、前置条件

### 1. 仓库与权限
- 代码在 **GitHub** 上（本方案使用 GitHub Actions + GitHub Container Registry）。
- 部署分支建议为 `main` 或 `master`（可在 workflow 里改）。

### 2. 服务器端
- 已安装 **Docker**、**Docker Compose**。
- 服务器上有一份项目目录（用于 `docker-compose.prod.yml` 和 MySQL 建表脚本），例如：
  ```bash
  # 在服务器上执行一次
  git clone https://github.com/你的用户名/AutoAttend.git /opt/AutoAttend
  ```
- 为 GitHub Actions 准备一个 **SSH 登录方式**（见下文「配置 Secrets」）。

### 3. GitHub 仓库配置
- 在仓库 **Settings → Secrets and variables → Actions** 中配置以下 Secrets（见下一节）。

---

## 三、需要配置的 GitHub Secrets

| Secret 名称       | 说明 |
|-------------------|------|
| `SSH_PRIVATE_KEY` | 用于登录服务器的私钥内容（整段，含 `-----BEGIN ... END-----`）。 |
| `SSH_HOST`        | 服务器 IP 或域名，例如 `123.45.67.89`。 |
| `SSH_USER`        | SSH 登录用户名，例如 `root`。 |
| `SSH_PORT`        | （可选）SSH 端口，默认 22。 |
| `DEPLOY_PATH`     | （可选）服务器上项目目录，默认 `/opt/AutoAttend`。 |

### 如何生成并配置 SSH 公钥/私钥

1. **本机生成密钥对**（若已有可跳过）：
   ```bash
   ssh-keygen -t ed25519 -C "github-actions-deploy" -f deploy_key -N ""
   ```
   得到 `deploy_key`（私钥）和 `deploy_key.pub`（公钥）。

2. **服务器上导入公钥**：
   ```bash
   # 登录服务器后
   echo "这里粘贴 deploy_key.pub 内容" >> ~/.ssh/authorized_keys
   chmod 600 ~/.ssh/authorized_keys
   ```

3. **在 GitHub 仓库中**：Settings → Secrets → New repository secret：
   - 名称：`SSH_PRIVATE_KEY`
   - 值：`deploy_key` 文件的**完整内容**（复制整段，包括首尾行）。

4. 同样新建 `SSH_HOST`、`SSH_USER`；若 SSH 不用 22 端口，再建 `SSH_PORT`。

---

## 四、服务器上首次部署（仅一次）

1. **克隆仓库**（若未克隆）：
   ```bash
   git clone https://github.com/你的用户名/AutoAttend.git /opt/AutoAttend
   cd /opt/AutoAttend
   ```

2. **首次启动 MySQL 并建表**（只需一次）：
   ```bash
   docker compose -f docker-compose.prod.yml up -d mysql
   # 等待几秒后，若需执行建表脚本可进入 mysql 容器执行，或已通过 volume 挂载 init 脚本则自动完成
   ```

3. **配置镜像地址并启动全部服务**：  
   GitHub Actions 会在部署时设置 `IMAGE_BACKEND`、`IMAGE_FRONTEND`。  
   首次可手动指定（将 `你的GitHub用户名` 换为实际）：
   ```bash
   export IMAGE_BACKEND=ghcr.io/你的GitHub用户名/autoattend-backend:latest
   export IMAGE_FRONTEND=ghcr.io/你的GitHub用户名/autoattend-frontend:latest
   docker compose -f docker-compose.prod.yml pull
   docker compose -f docker-compose.prod.yml up -d
   ```

之后每次 **push 到 main**，由 GitHub Actions 自动构建镜像并 SSH 到服务器执行 `pull + up -d`，无需再在服务器上执行 `compose build`。

---

## 五、文件说明

| 文件 | 作用 |
|------|------|
| `.github/workflows/deploy.yml` | push 时在 GitHub 上构建 backend/frontend 镜像、推送到 ghcr.io、SSH 到服务器执行拉取与重启。 |
| `docker-compose.prod.yml` | 生产环境用：backend/frontend 使用镜像（不 build），MySQL 仍用本地挂载的建表脚本。 |
| `docker-compose.yml` | 本地/自建用：含 `build`，适合在未使用 CI/CD 时本地或服务器直接 build。 |

---

## 六、常见问题

- **镜像拉取慢**：服务器若在国内，ghcr.io 可能较慢，可考虑将镜像同步到阿里云 ACR 或在 workflow 中推送到阿里云，再在 `docker-compose.prod.yml` 中改用 ACR 地址。
- **SSH 连接失败**：检查 `SSH_HOST`、`SSH_USER`、`SSH_PRIVATE_KEY` 是否正确；服务器 `sshd` 是否允许密钥登录；防火墙是否放行 `SSH_PORT`。
- **权限错误**：若使用私有仓库，需在 workflow 中为 `GITHUB_TOKEN` 配置 `packages: write`（当前 workflow 已包含）。
