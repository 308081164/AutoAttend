# MVP 接口文档（仅管理员 + GitHub + Diff 全量存档）

## 1. 约定
- **Base URL**：`http://<host>:<port>`（生产建议通过 Nginx 统一入口）
- **数据格式**：除 webhook 外，默认 `application/json; charset=utf-8`
- **时间格式**：ISO 8601（如 `2026-03-11T10:20:30+08:00`）
- **鉴权方式（MVP）**：管理员登录获取 JWT，后续请求携带 `Authorization: Bearer <token>`
- **角色（MVP）**：仅 `ADMIN`

## 2. 通用响应与错误码
### 2.1 通用响应（成功）
```json
{
  "code": 0,
  "message": "ok",
  "data": {}
}
```

### 2.2 通用响应（失败）
```json
{
  "code": 40001,
  "message": "invalid signature",
  "data": null
}
```

### 2.3 错误码建议
- `40000`：参数错误
- `40001`：Webhook 签名/事件校验失败
- `40100`：未登录/Token无效
- `40300`：无权限
- `40400`：资源不存在
- `40900`：幂等冲突（重复delivery等）
- `50000`：服务端异常
- `50001`：Git 操作失败（clone/fetch/diff）
- `50002`：Diff 存储/读取失败

## 3. 管理员鉴权（MVP）
> 若你暂时不做登录，也可以先用固定 header 或 IP 白名单顶住；但接口文档按 JWT 给出，便于直接落地。

### 3.1 管理员登录
- **POST** `/api/admin/auth/login`

请求：
```json
{
  "username": "admin",
  "password": "admin123"
}
```

响应：
```json
{
  "code": 0,
  "message": "ok",
  "data": {
    "token": "<jwt>",
    "expiresIn": 7200
  }
}
```

### 3.2 获取当前管理员信息
- **GET** `/api/admin/auth/me`

响应：
```json
{
  "code": 0,
  "message": "ok",
  "data": {
    "userId": 1,
    "username": "admin",
    "role": "ADMIN"
  }
}
```

## 4. GitHub Webhook（MVP）
### 4.1 接收 GitHub Webhook
- **POST** `/api/webhooks/github`
- **Headers（GitHub 会带）**
  - `X-GitHub-Event`
  - `X-GitHub-Delivery`
  - `X-Hub-Signature-256`（若配置 secret）

说明：
- 必须做 **签名校验**、**delivery 幂等去重**。
- 建议快速返回 `200`，后续处理异步化（MVP可任务表/线程池）。

响应：
```json
{ "code": 0, "message": "ok", "data": { "deliveryId": "..." } }
```

### 4.2 Webhook 事件类型（MVP建议启用）
- `push`：用于抓 commit 与生成 diff（核心）
- `pull_request`：用于推断评审/合并状态（可选但推荐）
- `workflow_run` 或 `check_suite`：用于推断 CI 状态（可选）

## 5. 管理员看板与状态（MVP）
### 5.1 管理员总览看板
- **GET** `/api/admin/dashboard`
- Query：
  - `range`：`4h|24h|7d`（默认 `24h`）
  - `repoId`：可选

响应（示例）：
```json
{
  "code": 0,
  "message": "ok",
  "data": {
    "range": "24h",
    "summary": {
      "activeCoding": 5,
      "inReview": 2,
      "reviewingOthers": 1,
      "ciFixing": 1,
      "blocked": 0,
      "idle": 3
    },
    "users": [
      {
        "userId": 101,
        "displayName": "张三",
        "githubUsername": "zhangsan",
        "currentStatus": "ACTIVE_CODING",
        "confidence": 0.78,
        "lastActivityAt": "2026-03-11T10:02:03+08:00",
        "metrics": {
          "validCommits": 3,
          "invalidCommits": 1,
          "prsOpened": 0,
          "prsMerged": 0,
          "ciFailed": 1
        }
      }
    ],
    "alerts": [
      {
        "alertId": 9001,
        "type": "NO_SIGNAL",
        "level": "WARN",
        "title": "工作时间内6小时无信号",
        "userId": 109,
        "createdAt": "2026-03-11T11:00:00+08:00",
        "status": "OPEN"
      }
    ]
  }
}
```

### 5.2 查询某员工状态时间线
- **GET** `/api/admin/users/{userId}/status`
- Query：
  - `range`：默认 `7d`
  - `repoId`：可选

响应（示例）：
```json
{
  "code": 0,
  "message": "ok",
  "data": {
    "userId": 101,
    "range": "7d",
    "snapshots": [
      {
        "at": "2026-03-11T10:00:00+08:00",
        "status": "IN_REVIEW",
        "confidence": 0.71,
        "reasonSummary": "近4小时主要活动为PR更新与回复review",
        "evidence": [
          { "type": "PULL_REQUEST", "url": "https://github.com/...", "at": "2026-03-11T09:50:00+08:00" }
        ]
      }
    ]
  }
}
```

### 5.3 查询某员工活动列表
- **GET** `/api/admin/users/{userId}/activities`
- Query：
  - `range`：默认 `7d`
  - `repoId`：可选
  - `page`：默认 1
  - `pageSize`：默认 20

## 6. Commit 与 Diff（MVP核心）
### 6.1 查询提交列表（按员工/仓库/时间）
- **GET** `/api/admin/commits`
- Query：
  - `userId`：可选
  - `repoId`：可选
  - `range`：`24h|7d|30d`（默认 `7d`）
  - `page`、`pageSize`

响应（示例）：
```json
{
  "code": 0,
  "message": "ok",
  "data": {
    "page": 1,
    "pageSize": 20,
    "total": 1,
    "items": [
      {
        "repoId": 1,
        "commitSha": "a1b2c3d4",
        "parentSha": "z9y8x7w6",
        "author": {
          "githubUsername": "zhangsan",
          "email": "zhangsan@example.com"
        },
        "committedAt": "2026-03-11T09:40:00+08:00",
        "message": "fix: login validation",
        "stats": {
          "filesChanged": 3,
          "insertions": 42,
          "deletions": 10
        },
        "validity": {
          "isValidCommit": true,
          "reason": "业务代码变更且规模超过阈值"
        },
        "diff": {
          "stored": true,
          "sizeBytes": 18324,
          "storageMode": "INLINE"
        }
      }
    ]
  }
}
```

### 6.2 获取某次提交的完整 Diff
- **GET** `/api/admin/commits/{commitSha}/diff`
- Query：
  - `repoId`：必填（避免 sha 跨仓库冲突）
  - `mode`：`raw|chunk`（默认 `raw`）
  - `chunk`：当 `mode=chunk` 时必填，从 1 开始

响应（raw 模式，示例）：
```json
{
  "code": 0,
  "message": "ok",
  "data": {
    "repoId": 1,
    "commitSha": "a1b2c3d4",
    "diffText": "diff --git a/src/... (完整diff文本)"
  }
}
```

响应（chunk 模式，示例）：
```json
{
  "code": 0,
  "message": "ok",
  "data": {
    "repoId": 1,
    "commitSha": "a1b2c3d4",
    "chunk": 1,
    "chunkCount": 5,
    "diffTextPart": "diff --git ... (第1块)"
  }
}
```

> 建议：后端存储可用 `LONGTEXT` + gzip 压缩（或分块表），响应端按需分块，避免一次性返回超大diff。

## 7. 配置与管理（管理员）
### 7.1 绑定仓库
- **POST** `/api/admin/repos/bind`

请求：
```json
{
  "name": "auto-attend",
  "fullName": "org/auto-attend",
  "cloneUrl": "https://github.com/org/auto-attend.git",
  "defaultBranch": "main",
  "accessToken": "ghp_xxx",
  "localPath": "D:/data/autoattend/repos/org_auto-attend"
}
```

响应：
```json
{ "code": 0, "message": "ok", "data": { "repoId": 1 } }
```

### 7.2 绑定员工身份（GitHub ↔ 系统用户）
- **POST** `/api/admin/users/identity/bind`

请求：
```json
{
  "userId": 101,
  "githubUsername": "zhangsan",
  "email": "zhangsan@example.com"
}
```

### 7.3 规则配置（有效提交阈值等）
- **GET** `/api/admin/rules`
- **PUT** `/api/admin/rules`

建议配置项：
- 工作时间窗口（用于“无信号”预警）
- 无效文件黑名单（如 `*.lock`、`package-lock.json` 等）
- 变更阈值（行数/文件数）
- 状态权重（commit/pr/ci）

## 8. 预警（管理员）
### 8.1 查询预警列表
- **GET** `/api/admin/alerts`
- Query：`range`、`status=open|closed`、`type`

### 8.2 关闭预警
- **POST** `/api/admin/alerts/{alertId}/close`

## 9. AI 分析预留（MVP不启用）
> 你后续会接 DeepSeek。建议接口先预留但返回 501 或受配置开关控制。

### 9.1 创建分析任务（预留）
- **POST** `/api/admin/ai/analysis/jobs`

请求：
```json
{
  "repoId": 1,
  "commitSha": "a1b2c3d4",
  "model": "deepseek-chat",
  "promptVersion": "v1"
}
```

响应：
```json
{
  "code": 0,
  "message": "ok",
  "data": { "jobId": "job_20260311_0001", "status": "PENDING" }
}
```

### 9.2 查询分析结果（预留）
- **GET** `/api/admin/ai/analysis/results?repoId=&commitSha=`

响应（示例）：
```json
{
  "code": 0,
  "message": "ok",
  "data": {
    "repoId": 1,
    "commitSha": "a1b2c3d4",
    "workload": "中",
    "isValidCommit": true,
    "qualityComment": "结构清晰，边界校验较完整；建议补充单测",
    "level": "中级",
    "riskFlags": ["NO_TESTS"],
    "rawResponse": { "model": "deepseek-chat", "tokens": 1234 }
  }
}
```

