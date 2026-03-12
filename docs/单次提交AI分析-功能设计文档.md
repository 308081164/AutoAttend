# 单次提交 AI 分析（工作内容 / 有效性 / 代码质量）— 功能设计文档

## 1. 文档说明

- **功能名称**：单次提交 AI 分析（工作内容推断、有效性推断、代码质量推断）
- **版本**：v0.1
- **目标**：对每一次开发推送产生的 **单条 commit** 及其 **完整 diff**、commit 元信息，单独构造一条请求发送给 AI，由 AI 进行工作内容推断、有效性推断、代码质量推断，并将结构化结果落库，供管理员在看板与详情中查看；与《产品设计_v2》中「附：AI 模型接入预留设计」及 6.4「有效提交」识别、7.3「Diff 查看」等能力对齐。
- **依赖**：现有 AutoAttend 后端、`aa_commit` / `aa_commit_diff` 数据、管理员后台；需接入大模型 API（如 DeepSeek / OpenAI 等，可配置）。

---

## 2. 功能目标与范围

### 2.1 目标

- **一次 commit = 一次 AI 请求**：每次开发推送中的每个 commit，在 diff 已落库的前提下，由系统自动生成一条「分析任务」，将**该 commit 的 diff + 元信息**作为输入发送给 AI，不合并多条 commit 为一条请求。
- **AI 产出三项推断**：  
  - **工作内容推断**：本 commit 主要在做什么（如：新增登录接口、修复某 bug、格式化/重构、仅改注释等），简短摘要与可选标签。  
  - **有效性推断**：是否为有效代码（有效 / 弱有效 / 无效），并给出理由（如：仅空白与注释、仅 lock 文件、无业务逻辑变更等）。  
  - **代码质量推断**：代码风格与健康度（命名、结构、风险点等），可选评分与改进建议。
- **结果可追溯、可配置**：分析结果结构化落库，与 commit 一一对应；管理员可查看、可配置开关与模型/提示词版本；支持失败重试与限流。

### 2.2 范围（本期）

| 在范围内 | 不在范围内（可后续迭代） |
|----------|---------------------------|
| 每 commit 一条分析任务，触发时机可配置（推送后异步 / 定时补齐） | 多条 commit 合并为一次请求 |
| 输入：diff 文本 + commit 元信息（message、author、repo、时间等） | 输入中包含 PR/Issue 正文、CI 日志等 |
| 输出：工作内容摘要、有效性结论+理由、代码质量结论+评分/建议，严格 JSON | 自然语言长报告、多轮对话 |
| 分析任务表 + 分析结果表落库，与现有 commit/diff 关联 | 实时流式输出到前端 |
| 管理员查看某 commit 的 AI 分析结果（与 diff 同屏或独立 Tab） | 员工端查看、争议申诉工作流 |
| 全局开关 + 模型/API 配置 + 提示词版本管理 | 多模型 A/B 对比、自定义规则覆盖 AI 结论 |

---

## 3. 用户角色与场景

| 角色 | 操作 |
|------|------|
| **管理员** | 开启/关闭「单次提交 AI 分析」；配置模型类型、API Key、提示词版本；查看 commit 列表时查看每条 commit 的 AI 分析结果（工作内容/有效性/代码质量）；可选：按有效性/质量筛选、导出。 |
| **系统** | 在 commit 及 diff 就绪后创建分析任务；按策略调用 AI API；解析返回 JSON 写入分析结果表；失败时按策略重试或标记失败。 |

---

## 4. 功能需求详述

### 4.1 触发时机（何时创建「一条请求」）

- **推荐**：在 **diff 已成功拉取并写入 `aa_commit_diff` 之后**，由同一流程（如 Webhook 异步处理或定时任务扫描「有 diff 且尚未分析」的 commit）创建一条 **AI 分析任务**，对应**该 commit 唯一**。
- **可选策略**（可配置）：  
  - **推送后异步**：Webhook 处理 push 时，对每个 commit 在落库 diff 后投递一条分析任务（或写入任务表，由后台 worker 消费）。  
  - **定时补齐**：定时任务扫描 `aa_commit` + `aa_commit_diff` 中「存在 diff 且无对应分析结果」的 commit，批量创建任务，避免与 Webhook 强耦合。
- **约束**：同一 `(repo_full_name, commit_sha)` 只对应**一条**分析任务；若任务已存在且状态为 success，则不再重复创建（幂等）。

### 4.2 单条请求的输入内容（发给 AI）

每条请求的 payload 建议包含（与产品设计 v2 附录一致）：

- **diff 文本**：来自 `aa_commit_diff.diff_text`（可截断/分块策略见 4.5）。
- **commit 元信息**：  
  - `repo_full_name`、`commit_sha`、`parent_sha`（可选）  
  - `author_name`、`author_email`  
  - `committed_at`、`message`  
  - 可选：`files_changed`、`insertions`、`deletions`（若已有）
- **可选上下文**：当前仓库语言/技术栈简述（如从 repo 名或配置读取），便于 AI 更准确定性「工作内容」与「代码质量」。

以上内容在发送前组装为**固定结构的 JSON 或文本块**（见 6 接口与数据格式），便于 AI 与后续版本管理。

### 4.3 AI 输出要求（三项推断）

要求 AI 返回**严格 JSON**，便于落库与展示。建议结构（字段名可微调）：

- **工作内容推断**  
  - `work_summary`：简短中文摘要（一句或几句）。  
  - `work_type`：枚举或标签，如：`feature` / `bugfix` / `refactor` / `format_only` / `doc_only` / `config_only` / `other`。  
  - `main_area`：可选，主要涉及模块/文件路径摘要。
- **有效性推断**  
  - `is_effective`：`effective` / `weak_effective` / `ineffective`。  
  - `reason`：理由说明（如：「仅修改空白与注释」「涉及业务逻辑新增」等）。  
  - 可选：`invalid_reason_tag`：如 `only_whitespace` / `only_comment` / `only_lockfile` / `no_logic_change` 等，便于统计与规则联动。
- **代码质量推断**  
  - `quality_level`：如 `high` / `medium` / `low` 或 1–10 分。  
  - `quality_comment`：简短评语与主要问题（命名、结构、风险等）。  
  - 可选：`risk_flags`：数组，如 `["magic_number", "long_method", "no_error_handling"]`。  
  - 可选：`suggestions`：改进建议列表（短条）。

产品设计 v2 附录中的 `workload(低/中/高)` 可映射到上述 `work_type` + `is_effective` 组合，或单独增加字段。

### 4.4 与现有「有效提交」规则的关系

- **MVP 规则（6.4）**：当前已有基于规则的「有效提交」识别（仅空白/仅注释/仅 lock 等），与 AI 的「有效性推断」可**并存**：  
  - 规则结果可写入 `commit_metric` 或现有表；AI 结果写入 `ai_analysis_result`。  
  - 管理员界面可同时展示「规则判定」与「AI 推断」，便于对比与后续用 AI 结果做统计（如无效提交占比、质量分布）。
- **后续**：若希望用 AI 覆盖规则，可在配置中增加「优先采用 AI 有效性结论」等策略。

### 4.5 风控与限制

- **Diff 截断**：单 commit 的 diff 超过约定长度（如 100KB 字符）时，可截断并注明「已截断」，或按文件/块拆分后只发送前 N 个块，避免超长导致超时或超费。  
- **调用限流**：按租户/仓库/全局配置 QPS 或并发数，避免瞬时大量推送打满 AI API。  
- **重试**：任务失败（网络/4xx/5xx）时，按退避策略重试（如 3 次，间隔递增），仍失败则标记任务状态为 failed，并记录错误信息。  
- **敏感信息**：若 diff 中含密钥、内部 URL 等，可在发送前做脱敏或仅对部分仓库启用 AI 分析（配置控制）。

### 4.6 配置项（管理员）

- **总开关**：是否启用「单次提交 AI 分析」（默认可关闭）。  
- **模型与 API**：模型类型（如 deepseek-chat）、API 端点、API Key（加密存储）。  
- **提示词版本**：当前使用的 prompt 版本号或标识，便于回溯与 A/B。  
- **触发策略**：推送后立即 / 仅定时补齐 / 两者都开。  
- **限流与截断**：单次 diff 最大字符数、单仓库/全局 QPS 上限。

---

## 5. 数据模型建议

### 5.1 分析任务表 `aa_ai_analysis_job`

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | 主键 |
| repo_full_name | VARCHAR(255) | 仓库全名 |
| commit_sha | VARCHAR(64) | 提交 SHA |
| status | VARCHAR(16) | pending / running / success / failed |
| model | VARCHAR(64) | 使用的模型标识 |
| prompt_version | VARCHAR(32) | 提示词版本 |
| retry_count | INT | 重试次数 |
| last_error | TEXT | 最后一次错误信息（若 failed） |
| created_at | DATETIME | 创建时间 |
| updated_at | DATETIME | 更新时间 |
| UNIQUE(repo_full_name, commit_sha) | - | 幂等 |

### 5.2 分析结果表 `aa_ai_analysis_result`

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | 主键 |
| repo_full_name | VARCHAR(255) | 仓库全名 |
| commit_sha | VARCHAR(64) | 提交 SHA |
| work_summary | VARCHAR(512) | 工作内容摘要 |
| work_type | VARCHAR(32) | 工作类型标签 |
| main_area | VARCHAR(255) | 可选，主要涉及模块/路径 |
| is_effective | VARCHAR(16) | effective / weak_effective / ineffective |
| effective_reason | TEXT | 有效性理由 |
| invalid_reason_tag | VARCHAR(64) | 可选，无效原因标签 |
| quality_level | VARCHAR(16) | 质量等级或分数档位 |
| quality_comment | TEXT | 质量评语 |
| risk_flags | JSON/VARCHAR(512) | 可选，风险标签数组 |
| suggestions | JSON/TEXT | 可选，改进建议 |
| raw_response | JSON/TEXT | 原始 AI 返回（便于排查与迭代） |
| prompt_version | VARCHAR(32) | 使用的提示词版本 |
| created_at | DATETIME | 创建时间 |
| UNIQUE(repo_full_name, commit_sha) | - | 与 commit 一一对应 |

### 5.3 与现有表关系

- **aa_commit**：分析任务/结果通过 `(repo_full_name, commit_sha)` 与 commit 关联；查询某 commit 的 AI 结果时 join 或按 commit_sha 查 `aa_ai_analysis_result`。  
- **aa_commit_diff**：分析任务消费的是已存在的 diff；任务创建前需保证该 commit 在 `aa_commit_diff` 中已有记录。

---

## 6. 接口与数据格式

### 6.1 调用 AI 的请求体示例（系统 → 大模型 API）

```json
{
  "repo_full_name": "owner/repo",
  "commit_sha": "abc123...",
  "author_name": "张三",
  "author_email": "zhangsan@example.com",
  "committed_at": "2025-03-11T10:00:00Z",
  "message": "feat: 增加管理员登录接口",
  "files_changed": 3,
  "insertions": 120,
  "deletions": 10,
  "diff_text": "--- a/src/...\n+++ b/src/...\n@@ ... @@\n...",
  "tech_hint": "Spring Boot, Vue"
}
```

- 实际请求格式需符合所选大模型的 **Chat/Completion API**（如 system + user message，将上述 JSON 或格式化文本放入 user content）；具体由「分析器」实现封装。

### 6.2 要求 AI 返回的 JSON 结构（与 4.3 对应）

```json
{
  "work_summary": "新增管理员登录接口与 JWT 校验",
  "work_type": "feature",
  "main_area": "admin/auth",
  "is_effective": "effective",
  "effective_reason": "包含业务逻辑新增与安全相关代码",
  "invalid_reason_tag": null,
  "quality_level": "medium",
  "quality_comment": "命名清晰，建议对密码做脱敏日志",
  "risk_flags": ["log_sensitive_data"],
  "suggestions": ["避免在日志中打印完整 password"]
}
```

- 需在**提示词**中明确要求只输出该 JSON，便于后端解析并写入 `aa_ai_analysis_result`；解析失败时记入 `last_error` 并将任务标为 failed。

### 6.3 后端对管理员的接口

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /api/admin/commits/{commitSha}/ai-analysis | 查询某 commit 的 AI 分析结果（与 getDiff 同 commit 维度）；若不存在则返回 404 或空结构。 |
| GET | /api/admin/ai-analysis/jobs | 可选，分页查询任务列表（status、repo、时间范围）。 |
| GET | /api/admin/ai-analysis/config | 查询当前 AI 分析配置（开关、模型、prompt 版本等，脱敏 API Key）。 |
| PUT | /api/admin/ai-analysis/config | 更新配置（开关、模型、API Key、prompt 版本、限流与截断参数）。 |
| POST | /api/admin/commits/{commitSha}/ai-analysis/retry | 可选，对失败任务手动触发重试。 |

---

## 7. 前端展示建议

- **Commit 列表/详情**：在现有「查看 Diff」旁或下方增加 **「AI 分析」** 区块（或 Tab），展示：  
  - 工作内容摘要、工作类型、主要涉及模块；  
  - 有效性结论与理由；  
  - 代码质量等级与评语、风险标签、改进建议。  
- **筛选与统计**：可选按「有效性」「质量等级」筛选 commit；后续可做「无效提交占比」「质量分布」等统计与产品设计 v2 中的预警联动。

---

## 8. 非功能要点

- **安全**：API Key 加密存储；仅管理员可查看分析结果与配置。  
- **可观测**：任务状态、失败原因、重试次数可查；便于排查 AI 超时/限流/解析错误。  
- **可解释**：结果表保留 `raw_response` 与 `prompt_version`，便于审计与提示词迭代。

---

## 9. 后续迭代可选

- 多模型 A/B 或降级策略（主模型不可用时切换备用模型）。  
- 管理员对 AI 结论的「采纳/驳回」与自定义规则覆盖。  
- 与「无效提交占比」「代码质量趋势」等预警规则联动。  
- 员工端查看本人 commit 的 AI 分析（含争议/补充说明）。

---

**文档结束。可按本设计实现「每 commit 单条请求 → AI 三项推断 → 结果落库与管理员查看」，并与《产品设计_v2》中 Diff 存档、有效提交识别、AI 预留设计保持一致。**
