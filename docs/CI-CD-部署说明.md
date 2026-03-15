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
  git clone https://github.com/你的用户名/AutoAttend.git /mnt/newdisk/app/AutoAttend
  ```
  （若使用其他目录，需在仓库 Secrets 中设置 `DEPLOY_PATH` 与之一致。）
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
| `DEPLOY_PATH`     | （可选）服务器上项目目录，默认 `/mnt/newdisk/app/AutoAttend`。 |

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
   git clone https://github.com/你的用户名/AutoAttend.git /mnt/newdisk/app/AutoAttend
   cd /mnt/newdisk/app/AutoAttend
   ```

2. **首次启动 MySQL 并建表**（只需一次）：
   ```bash
   docker compose -f docker-compose.prod.yml up -d mysql
   ```
   等待几秒即可。MySQL 首次初始化时会自动执行 `01_schema.sql`、`02_schema_collab.sql`、`03_schema_ai.sql`（由 docker-compose 挂载到 `docker-entrypoint-initdb.d/`），无需手动执行任何建表脚本。

3. **配置镜像地址并启动全部服务**：  
   GitHub Actions 会在部署时设置 `IMAGE_BACKEND`、`IMAGE_FRONTEND`。  
   首次可手动指定（将 `你的GitHub用户名` 换为实际）：
   ```bash
   export IMAGE_BACKEND=ghcr.io/你的GitHub用户名/autoattend-backend:latest
   export IMAGE_FRONTEND=ghcr.io/你的GitHub用户名/autoattend-frontend:latest
   docker compose -f docker-compose.prod.yml pull
   docker compose -f docker-compose.prod.yml up -d
   ```

4. **可选：配置 GitHub Token 与代理以拉取 Commit Diff**  
   管理后台「查看 Diff」、Webhook 拉取 diff、首页提交列表的「文件/新增/删除」统计均依赖 GitHub API；未配置 token 时易触发**匿名限流**（约 60 次/小时），拉取失败时页面会显示「Diff 暂不可用」占位。

   **GitHub Token：仅在管理后台「AI 配置」页填写（不写入服务器或 compose）**  
   - 为保护 Token 安全、避免泄露，**不在** `docker-compose.prod.yml` 或服务器 `.env` 中配置 `GITHUB_TOKEN`。
   - 请在 GitHub 创建 [Personal Access Token](https://github.com/settings/tokens)（权限勾选 `repo` 或 `public_repo`），然后在系统**管理后台 → AI 配置**页中填写并保存「GitHub Token」。Token 仅存入数据库，供后端拉取 Diff 与本地 Git 兜底使用。

   **大陆服务器无法直连 GitHub 时（可选）**  
   - 在项目目录的 **`.env`** 中增加（该文件已在 `.gitignore` 中，不会提交）：
     ```bash
     GITHUB_API_PROXY=http://127.0.0.1:7890
     # 或 SOCKS5：GITHUB_API_PROXY=socks5://127.0.0.1:1080
     ```
   - 执行 `docker compose -f docker-compose.prod.yml up -d backend` 使代理生效。若无代理，可点击「在 GitHub 上查看」在浏览器中打开该提交。

   **本地 Git 兜底目录**  
   - 当 GitHub API 拉取 diff 失败时，系统会自动用本地 `git clone/fetch + git show` 拉取。兜底仓库固定使用宿主机目录 `/mnt/newdisk/app/AutoAttend/git-repos` 挂载到容器的 `/data/git-repos`。**CI/CD 每次部署时会自动创建该目录，无需在服务器上手动操作。**

5. **（可选）启用「单次提交 AI 分析」**  
   AI 分析相关表（`aa_ai_analysis_config` 等）会随部署自动创建：**全新 MySQL** 由容器首次启动时执行 `03_schema_ai.sql`；**已有数据目录** 的实例会在后端首次启动时自动执行建表，无需手动跑 SQL。  
   启用方式：在管理后台 **「AI 配置」** 页填写 DeepSeek API Key（需自行到 [DeepSeek 平台](https://platform.deepseek.com) 获取）并开启分析。

之后每次 **push 到 main**，由 GitHub Actions 自动构建镜像并 SSH 到服务器执行 `pull + up -d`，无需再在服务器上执行 `compose build`。

---

## 五、文件说明

| 文件 | 作用 |
|------|------|
| `.github/workflows/deploy.yml` | push 时在 GitHub 上构建 backend/frontend 镜像、推送到 ghcr.io、SSH 到服务器执行拉取与重启。 |
| `docker-compose.prod.yml` | 生产环境用：backend/frontend 使用镜像（不 build），MySQL 使用本地挂载的建表脚本（01/02/03 含 MVP、协作、AI 分析表）。 |
| `docker-compose.yml` | 本地/自建用：含 `build`，适合在未使用 CI/CD 时本地或服务器直接 build。 |

---

## 六、常见问题

- **团队管理：已有库升级**  
  若在**本次更新前**已部署过 AutoAttend，MySQL 中的 `biz_user` 表可能缺少「头像、备注名、职务」列。**无需在服务器上手动执行**：下次通过 CI/CD 部署（push 到 main 后）会自动执行 `schema_collab_team_migration.sql`（在 `up -d` 之后由 workflow 挂载并运行）；若列已存在会报错，workflow 已忽略该错误，不影响部署。全新 MySQL 的建表脚本已包含这些列，同样无需额外操作。

- **镜像拉取慢**：服务器若在国内，ghcr.io 可能较慢，可考虑将镜像同步到阿里云 ACR 或在 workflow 中推送到阿里云，再在 `docker-compose.prod.yml` 中改用 ACR 地址。
- **SSH 连接失败**：检查 `SSH_HOST`、`SSH_USER`、`SSH_PRIVATE_KEY` 是否正确；服务器 `sshd` 是否允许密钥登录；防火墙是否放行 `SSH_PORT`。
- **权限错误**：若使用私有仓库，需在 workflow 中为 `GITHUB_TOKEN` 配置 `packages: write`（当前 workflow 已包含）。

### 整站 502（打开域名即 502）

按下面顺序在**服务器上**执行，定位是「前端容器未跑起来」还是「外层 Nginx 未反代到前端」：

1. **看三个容器是否都在运行**：
   ```bash
   docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}" | grep autoattend
   ```
   应看到 `autoattend-frontend`、`autoattend-backend`、`autoattend-mysql` 均为 **Up**。若 `autoattend-frontend` 没有或状态异常，执行下一步。

2. **看前端容器日志**（若上一步发现 frontend 未 Up 或反复重启）：
   ```bash
   docker logs --tail 80 autoattend-frontend
   ```
   若有启动报错（如 nginx 配置错误），需修复镜像或配置后重新部署。若日志正常但容器退出了，可尝试：
   ```bash
   cd /mnt/newdisk/app/AutoAttend   # 或你的 DEPLOY_PATH
   docker compose -f docker-compose.prod.yml up -d frontend
   ```

3. **在服务器本机测前端端口**：
   ```bash
   curl -s -o /dev/null -w "%{http_code}" http://127.0.0.1:8849/
   ```
   - 若返回 **200**：说明前端容器正常，502 来自**外层 Nginx/宝塔未把域名反代到 8849**，请按下一小节「502 Bad Gateway（宝塔 Nginx 反代前端）」配置反代。
   - 若返回 **000** 或连接失败：说明 8849 无进程监听，前端未启动或端口未映射，回到步骤 1、2 并确认 `docker compose -f docker-compose.prod.yml up -d` 已拉齐并启动 frontend。

4. **确认后端可访问**（可选，用于区分「整站 502」与「仅 /api 502」）：
   ```bash
   curl -s -o /dev/null -w "%{http_code}" http://127.0.0.1:8848/api/admin/repos
   ```
   返回 200/401 表示后端正常；若后端也 502，先看 `docker logs --tail 80 autoattend-backend` 和 MySQL 是否 Up。

### 502 Bad Gateway（宝塔 Nginx 反代前端）

若 `curl http://127.0.0.1:8849/` 在服务器上返回 200，但浏览器访问域名报 502，多半是 **Nginx 没有反代到前端容器**（例如站点被配成「网站目录」而不是「反向代理」）。

**处理步骤（宝塔）：**

1. 在宝塔里打开该站点 → **设置** → **反向代理** → **添加反向代理**。
2. 代理名称随意（如 `前端`）；**目标 URL** 填：`http://127.0.0.1:8849`。
3. 发送域名留空或填 `$host`；**提交** 后再 **重载 Nginx**。

若站点当前是「根目录」模式，可改为仅用反代提供内容：在 **配置文件** 里把 `location /` 改为走反代，例如：

```nginx
location / {
    proxy_pass http://127.0.0.1:8849;
    proxy_http_version 1.1;
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto $scheme;
}
```

删除或注释掉原来的 `root ...` 和 `index ...`（在该 `location /` 内）。保存后执行 **Nginx 重载**。

### /api 接口 502（前端正常、接口报 502）

页面能打开，但 `/api/admin/repos`、`/api/admin/dashboard` 等接口返回 **502**：说明请求已到达前端容器，但前端容器内 Nginx 把 `/api/` 转给 **backend:8848** 时失败（后端未响应或报错）。

**排查步骤：**

1. **确认后端与 MySQL 都在运行**：
   ```bash
   docker ps --format "table {{.Names}}\t{{.Status}}" | grep autoattend
   ```
   若 `autoattend-mysql` 为 Restarting 或 `autoattend-backend` 未 Up，先按上文「MySQL 容器反复重启」处理磁盘与 MySQL，再 `docker compose -f docker-compose.prod.yml up -d`。

2. **看后端日志**（常见为连不上 MySQL）：
   ```bash
   docker logs --tail 80 autoattend-backend
   ```
   若有数据库连接错误，先保证 MySQL 容器稳定 Up 后再重启 backend：  
   `docker compose -f docker-compose.prod.yml restart backend`

3. **确认本机访问后端**（可选）：
   ```bash
   curl -s -o /dev/null -w "%{http_code}" http://127.0.0.1:8848/api/admin/repos
   ```
   若返回 200/401 等非 502，说明后端正常，502 多半是前端容器内 Nginx 到 backend 的网络或超时；可重新部署一次前端镜像（已加强 /api 代理超时与转发头）后再试。

### 协作登录 500 / Table 'biz_user' doesn't exist

若调用 **POST /api/collab/auth/login** 返回 500，后端日志出现 `Table 'autoattend.biz_user' doesn't exist`：说明协作模块的数据库表未创建。MySQL 的 init 脚本只在**首次初始化空数据目录**时执行；若服务器上 MySQL 此前已跑过且未挂载协作建表脚本，需要**手动执行一次**协作表结构。

**一次性修复（在服务器项目目录下执行）：**

```bash
cd /mnt/newdisk/app/AutoAttend   # 或你的 DEPLOY_PATH
docker exec -i autoattend-mysql mysql -u root -proot autoattend < atuo_attend_backend/src/main/resources/db/schema_collab_mysql.sql
```

执行成功后，再尝试协作登录。之后新部署的服务器若使用当前 `docker-compose.prod.yml`（已挂载 `02_schema_collab.sql`），全新 MySQL 数据目录会自动建协作表；**已有数据目录的实例仍需上述手动执行一次**。

### MySQL 容器反复重启 / 502 / ERR_EMPTY_RESPONSE

若 `docker ps` 显示 `autoattend-mysql` 为 **Restarting**，或页面出现 **ERR_EMPTY_RESPONSE**，先查 MySQL 日志：

```bash
docker logs --tail 100 autoattend-mysql
```

**若日志出现 “No space left on device” / “out of disk space”**：说明**服务器磁盘已满**。MySQL 无法写 redo log 和临时表，会一直启动失败；磁盘满还会导致 Nginx 或系统异常，进而 502 或连接被关闭无数据返回。

**处理步骤：**

1. **在服务器上查看磁盘占用**：
   ```bash
   df -h
   du -sh /var/lib/docker/* 2>/dev/null
   du -sh /www/wwwroot/* 2>/dev/null
   ```
2. **腾出空间**（按需执行）：
   - 清理 Docker 未用镜像与容器：`docker system prune -a`（会删掉未使用的镜像，确认无再执行）
   - 或只删未用镜像：`docker image prune -a`
   - 清理 Docker 构建缓存：`docker builder prune -f`
   - 清理系统/宝塔日志、临时文件（如 `/www/server/panel/logs`、`/tmp`）
   - 删除其他不用的 Docker 卷或大文件
3. **确认有可用空间后**再重启 MySQL：
   ```bash
   cd /mnt/newdisk/app/AutoAttend   # 或你的 DEPLOY_PATH
   docker compose -f docker-compose.prod.yml up -d mysql
   ```
4. 等 MySQL 稳定（`docker ps` 显示 Up）后，再访问前端；若仍有问题可再查 `docker logs autoattend-backend`。

其他常见原因：数据目录权限、与宿主机已有 MySQL 冲突（端口/数据）、init 脚本报错。按日志修正后同样执行 `docker compose -f docker-compose.prod.yml up -d mysql`。
