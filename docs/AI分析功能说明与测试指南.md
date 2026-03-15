# AI 分析功能说明与测试指南

## 一、当前已实现的功能

### 1.1 单次提交 AI 分析（DeepSeek）

对**单条 commit** 的 **diff + 元信息** 调用 DeepSeek，得到结构化结果并落库，供管理员在看板中查看。

| 能力 | 说明 |
|------|------|
| **工作内容推断** | `work_summary`（简短摘要）、`work_type`（如 feature/bugfix/refactor/format_only/doc_only 等）、`main_area`（主要涉及模块/路径）。 |
| **有效性推断** | `is_effective`（effective / weak_effective / ineffective）、`effective_reason`（理由）、`invalid_reason_tag`（如 only_whitespace、only_comment 等）。 |
| **代码质量推断** | `quality_level`（如 high/medium/low）、`quality_comment`（评语）、`risk_flags`、`suggestions`（改进建议）。 |
| **结果落库** | 与 `(repo_full_name, commit_sha)` 一一对应，存在 `aa_ai_analysis_result`；任务记录在 `aa_ai_analysis_job`。 |

### 1.2 管理员配置（AI 配置页）

- **入口**：管理后台顶部或首页「AI 配置」链接 → `/ai-config`。
- **可配置项**：  
  - **API Key**：DeepSeek API Key（必填才能分析）。  
  - **启用开关**：是否启用「单次提交 AI 分析」。  
  - **模型**：如 `deepseek-chat`（默认）。  
- **说明**：当前版本仅支持 DeepSeek；API Key 需在 [DeepSeek 开放平台](https://platform.deepseek.com) 获取。

### 1.3 看板中的 AI 分析（按 commit 手动触发）

- **入口**：管理后台首页 → 在「最近提交」表格中点击某条 commit 的 **「查看 Diff」** → 在展开的 Diff 区域下方有 **「AI 分析」** 区块。
- **行为**：  
  - 若该 commit 已有 AI 分析结果：直接展示**工作内容摘要、工作类型、有效性、有效性理由、代码质量、质量评语**等。  
  - 若没有：显示 **「运行 AI 分析」** 按钮；点击后后端对该 commit 执行一次分析（拉取 diff → 调用 DeepSeek → 解析 JSON 落库），成功后页面展示结果。
- **前提**：  
  - 已在「AI 配置」中填写 DeepSeek API Key 并**开启**分析；  
  - 该 commit **已有 Diff**（即系统已成功拉取并存储该 commit 的 diff，否则会提示 Diff 暂不可用）。

### 1.4 当前未实现的能力（设计文档中有、代码未做）

- **推送后自动分析**：Webhook 收到 push 后自动对每个 commit 创建 AI 分析任务并执行（当前仅支持管理员在看板点击「运行 AI 分析」手动触发）。  
- **定时补齐**：定时任务扫描「有 diff 但无分析结果」的 commit 并批量分析。  
- **多模型 / 提示词 A/B**：当前仅单模型、单版提示词。

---

## 二、如何测试并拿到结果

### 步骤 1：准备 DeepSeek API Key

1. 打开 [DeepSeek 开放平台](https://platform.deepseek.com)，注册/登录。  
2. 在控制台创建 API Key，并复制保存（仅显示一次）。  

### 步骤 2：配置 AI 分析

1. 启动项目（本地或 Docker），用**管理员账号**登录管理后台。  
2. 点击顶部或首页的 **「AI 配置」**，进入 `/ai-config`。  
3. 在 **API Key** 中粘贴 DeepSeek API Key。  
4. 勾选 **「启用单次提交 AI 分析」**。  
5. 模型可保持默认 `deepseek-chat`，点击 **「保存」**。  

### 步骤 3：确保有 commit 且该 commit 有 Diff

- 系统里的 commit 来自 **GitHub Webhook 推送**；Diff 在「查看 Diff」时会按需从 GitHub API 拉取并落库。  
- 若你还没有任何 commit：  
  - 在 GitHub 上对已配置 Webhook 的仓库做一次 **push**（至少一个 commit）；  
  - 等待 Webhook 处理完成后，在管理后台「最近提交」中应能看到该 commit。  
- 若 commit 已有但 Diff 未拉取：  
  - 在首页点击该 commit 的 **「查看 Diff」**，系统会自动尝试拉取并保存 Diff（需配置 `GITHUB_TOKEN` 且网络可达 GitHub）；  
  - 若 Diff 拉取成功，下方会显示 diff 内容；若失败，会显示占位提示（此时无法运行 AI 分析，需解决 Token/网络后再试）。  

### 步骤 4：在看板触发 AI 分析并查看结果

1. 在管理后台首页，找到 **「最近提交」** 表格。  
2. 点击某一行的 **「查看 Diff」**，展开该 commit 的 Diff 与 AI 区块。  
3. 若该 commit 尚未有 AI 结果：  
   - 点击 **「运行 AI 分析」**；  
   - 等待数秒（会显示「分析中...」），成功后页面会刷新并展示 AI 分析结果。  
4. 若已有结果：直接查看 **工作内容摘要、工作类型、有效性、有效性理由、代码质量、质量评语** 等字段。  

### 步骤 5：自检清单（拿不到结果时对照）

| 检查项 | 说明 |
|--------|------|
| 已登录管理员 | 未登录或未用管理员账号会 401，无法访问 AI 配置与接口。 |
| AI 配置已保存且已开启 | 「AI 配置」中 API Key 已填、已勾选「启用」并保存。 |
| 该 commit 已有 Diff | Diff 区域不是「Diff 暂不可用」；若不可用，需配置 `GITHUB_TOKEN` 并确保能访问 GitHub API。 |
| API Key 有效且额度充足 | DeepSeek 控制台可查看用量与额度；无效或超限会导致分析失败。 |
| 后端日志 | 若前端报错或一直「分析中」，可查看后端日志（如 `AiAnalysisService`、`DeepSeekClient`）是否有异常或 DeepSeek 返回错误。 |

---

## 三、接口一览（便于联调 / 自动化测试）

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/admin/ai-analysis/config` | 获取 AI 配置（API Key 脱敏）。 |
| PUT | `/api/admin/ai-analysis/config` | 更新 AI 配置（apiKey、enabled、model 等）。 |
| GET | `/api/admin/ai-analysis/commits/{commitSha}/result` | 查询某 commit 的 AI 分析结果；可选 query `repoFullName`（不传则按 commitSha 查一条）。 |
| POST | `/api/admin/ai-analysis/commits/{commitSha}/run` | 对某 commit 执行一次 AI 分析并返回结果；可选 query `repoFullName`。 |

以上接口均需**管理员 JWT**（登录后请求头带 `Authorization: Bearer <token>`）。

---

## 四、小结

- **已实现**：单 commit 的 AI 分析（工作内容 / 有效性 / 代码质量）、DeepSeek 配置、看板中按 commit 查看结果与**手动「运行 AI 分析」**。  
- **测试要点**：配置 DeepSeek API Key 并开启 → 确保 commit 有 Diff → 在看板中对该 commit 点击「查看 Diff」→ 点击「运行 AI 分析」→ 在页面查看结构化结果。  
- **未实现**：推送后自动分析、定时补齐、多模型等；若需要可后续按设计文档迭代。
