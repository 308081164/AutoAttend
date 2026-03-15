# Diff 拉取失败排查说明

当页面显示「Diff 暂不可用」时，系统会先尝试 **GitHub API**，再尝试 **本地 Git 兜底**。任一路失败都可能出现该提示。

## 一、可能原因概览

| 环节 | 可能原因 |
|------|----------|
| **方式 1：GitHub API** | 1) 未在管理后台「AI 配置」页填写并保存 GitHub Token（Token 仅存数据库，不通过环境变量传入）<br>2) 网络/代理：容器访问不了 `api.github.com`，或 `GITHUB_API_PROXY` 配置错误<br>3) API 限流（Token 权限不足或未保存成功） |
| **方式 3：本地 Git** | 1) 容器内访问不了 `github.com`（国内常见，clone/fetch 超时或失败）<br>2) `--depth 100` 浅克隆，该 commit 不在最近 100 条内则 `git show` 会失败<br>3) 首次 clone 失败后未留下有效仓库目录，后续只做 fetch 仍拿不到更早的 commit |

## 二、在服务器上执行的 Docker 命令（用于排查）

在服务器上依次执行，并把**关键输出**提供给排查方。

### 1. 看后端最近日志（含 Diff/API/git 相关）

```bash
docker logs --tail 200 autoattend-backend 2>&1 | grep -E -i "diff|fetch|clone|git|retries|GITHUB|proxy|308081164"
```

- 若出现 **`Fetch diff failed for repo=308081164/AutoAttend`**：API 请求失败（网络/代理/限流/Token）。
- 若出现 **`git clone failed for 308081164/AutoAttend (exit code ...)`**：本地 clone 失败（多为网络或 Token）。
- 若出现 **`git show failed for ... (exit ...), commit may be outside shallow clone`**：该 commit 不在当前浅克隆的 100 条内。
- 若出现 **`Diff unavailable after 6 retries`**：两路都失败。

### 2. 确认容器内环境变量（代理、GIT_WORKSPACE）

```bash
docker exec autoattend-backend env | grep -E "GIT_|proxy"
```

- 期望：`GIT_WORKSPACE=/data/git-repos`（兜底已启用）。GitHub Token 不在环境变量中，仅通过管理后台「AI 配置」存入数据库。

### 3. 确认本地 Git 兜底目录与仓库是否存在

```bash
docker exec autoattend-backend sh -c "ls -la /data/git-repos/ 2>/dev/null || echo 'dir missing'"
```

- 若有 `308081164_AutoAttend` 且内有 `.git`，说明曾 clone 过；否则可能是首次 clone 失败或未触发。

### 4. 在容器内手动测 API 连通性（可选）

```bash
docker exec autoattend-backend sh -c "wget -q -O - --timeout=5 https://api.github.com/repos/308081164/AutoAttend/commits/fe8be47 2>&1 | head -5"
```

- 若超时或连接错误，说明容器访问 GitHub API 有问题（需检查代理或网络）。

### 5. 在容器内手动测 git clone/show（可选）

```bash
docker exec autoattend-backend sh -c "cd /tmp && git clone --depth 50 https://github.com/308081164/AutoAttend.git test-repo 2>&1; ls test-repo/.git 2>/dev/null && (cd test-repo && git show fe8be47 --format= --no-color 2>&1 | head -20)"
```

- 若 clone 超时或失败：容器访问 github.com 有问题（国内常见）。
- 若 clone 成功但 `git show fe8be47` 报错：该 commit 可能不在最近 50 条内（与当前 `--depth 100` 同理）。

## 三、根据结果的建议

- **API 和本地 Git 在容器内都访问不了 GitHub**  
  在服务器或网络侧为**该容器**配置 HTTP/SOCKS 代理（或能访问外网的网关），并让 API 请求与 `git` 都走代理；或在宿主机先 clone 再挂载进容器（需改部署方式）。
- **API 失败但容器内 git 能访问 GitHub**  
  检查 `GITHUB_TOKEN` 是否传入容器、是否在「AI 配置」保存到 DB；检查 `GITHUB_API_PROXY` 是否正确。
- **只有 `git show` 报 commit 不在 shallow clone**  
  增大 clone/fetch 的 `--depth`（需改代码或配置中的 depth 参数），或对该仓库做一次完整 clone（无 `--depth`），再重试。

将上述命令的输出（尤其是 1、2、3）提供给支持方，便于进一步精确定位。
