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

## 10. 与多维表格联动（增强）：由 AI 推断开发状态并回写看板

### 10.1 背景与价值

- **现状**：单次提交 AI 分析仅针对「commit + diff」做工作内容/有效性/代码质量推断，结果独立落库，与需求/任务看板（多维表格）无联动，整体能力偏单薄。
- **多维表格**：每个项目一张表，每条记录代表一条需求或任务，具备「问题描述、归属模块、重要程度、解决情况、验收结果」等列，且项目与仓库一一对应。
- **增强目标**：将 **commit + diff + 当前项目多维表中相关记录的关键信息** 一并提交给 AI，由 AI：
  1. **推断本次提交对应的功能/需求**（关联到多维表中的哪条或哪几条记录）；  
  2. **推断这些需求/任务的开发状态**（如：已实现、待审核、待验收等）；  
  3. **输出结构化建议**，由系统**自动或经确认后**更新多维表对应记录的字段（如「解决情况」「验收结果」），实现 **提交 → 需求状态** 的闭环。

这样，单次提交分析从「单点分析」升级为「与需求看板联动」，看板能随提交自动反映开发进度，减少人工维护成本。

### 10.2 可行性分析

| 维度 | 说明 |
|------|------|
| **输入可行** | 同一仓库对应唯一项目与多维表；在构造 AI 请求时，可根据 `repo_full_name` 查到 `biz_project` 与 `biz_project_table`，再取该表下**未关闭或待更新**的记录（如「解决情况 ≠ 通过任务关闭」），抽取每条记录的 **record_id、问题描述（摘要）、归属模块、重要程度、解决情况、验收结果** 等关键字段，组装为「候选需求列表」一并发给 AI。可限制条数（如最近 50 条或仅未完成）与单条描述长度，避免 token 爆炸。 |
| **推断可行** | AI 同时看到：本次改动的 diff、commit message、以及「当前有哪些需求/任务」。结合 diff 中的模块路径、关键词、message 中的 issue/需求描述，可以合理推断「本次提交是在做哪条需求、修哪个 bug」，并给出状态建议（如：解决情况 → 已解决待审核、验收结果 → 待验收）。一次提交可能关联 0/1/多条记录，AI 可返回 `related_record_ids` + 每条的状态更新建议。 |
| **回写可行** | 后端解析 AI 返回的「记录 ID + 字段更新建议」后，可：**方案 A**：直接写回 `biz_record_field`（自动更新）；**方案 B**：先写入「待确认更新」表，管理员在 commit 详情页一键采纳后再写回多维表；**方案 C**：仅展示建议，不自动写回，由人在多维表中手动改。建议 MVP 采用 **B**，兼顾自动化与可控性。 |
| **风险与缓解** | 匹配可能不准（误关联、漏关联）：通过「仅建议 + 人工确认」或「高置信度才自动写回」控制；多维表记录过多时需做筛选与摘要，避免超长输入。 |

**结论**：在现有「单次提交 AI 分析」与「多维表格」均已落地的前提下，**把 commit、diff、多维表候选记录关键信息一起交给 AI，由 AI 推断对应功能的开发状态并输出结构化更新建议、再由系统写回（或经确认后写回）多维表，在技术与产品上均可行**，且能显著增强「单次提交分析」的价值。

### 10.3 输入扩展（发给 AI 的 payload）

在 4.2 节现有内容基础上，**可选增加**（当且仅当该仓库已绑定项目且启用「多维表联动」时）：

- **project_id**、**table_id**：当前仓库对应项目与表。
- **collab_records**：当前项目多维表中「待关联」记录的摘要列表，例如：
  - `record_id`：记录 ID，用于回写时定位。
  - `problem_summary`：问题描述前 N 字（如 200 字）。
  - `module`：归属模块。
  - `importance`：重要程度。
  - `resolve_status`：当前解决情况。
  - `accept_status`：当前验收结果。

可配置：仅包含「解决情况 ≠ 通过任务关闭」或「验收结果 ≠ 通过任务关闭」的记录，并限制条数（如 30 条）与单条摘要长度，避免请求体过大。

### 10.4 输出扩展（AI 返回的 JSON）

在 4.3 / 6.2 节现有三项推断之外，**可选增加**（当请求中带有 `collab_records` 时）：

- **related_records**：本次提交关联的多维表记录及状态建议，数组，每项包含：
  - `record_id`：关联的记录 ID（须在请求的 `collab_records` 中）。
  - `confidence`：可选，关联置信度（如 high / medium / low）。
  - **状态更新建议**（仅当 AI 认为应更新时给出）：
    - `resolve_status`：建议的「解决情况」取值（与多维表选项组一致，如「已解决待审核」「未解决」等）。
    - `accept_status`：建议的「验收结果」取值。
    - `comment`：可选，简短理由（如「本次提交实现了该需求的登录接口」）。

若 AI 认为本次提交不关联任何需求，则 `related_records` 为空数组。

### 10.5 系统回写与配置

- **回写策略**（可配置）：  
  - **auto**：解析到 `related_records` 后，对每条建议直接更新 `biz_record_field`（需限定仅更新「解决情况」「验收结果」等约定字段）。  
  - **confirm**：将建议写入「待确认表」，在管理员/commit 详情页展示「AI 建议更新：记录 xxx → 解决情况=已解决待审核」，提供「采纳」「忽略」；采纳后再写回多维表。  
  - **suggest_only**：仅落库为「AI 建议」，不写回多维表，仅在 commit 的 AI 分析结果中展示，由人在多维表中手动操作。
- **数据落库**：  
  - 可将 `related_records` 存于 `aa_ai_analysis_result` 的扩展字段（如 JSON 列 `collab_suggestions`），或单独表 `aa_ai_collab_update`（job_id、record_id、建议字段、采纳状态、采纳时间等），便于审计与回滚。
- **配置项**：  
  - 是否启用「多维表联动」；  
  - 回写策略：auto / confirm / suggest_only；  
  - 候选记录筛选条件（如仅未关闭）与条数/摘要长度上限。

### 10.6 与现有设计的关系

- **单次提交分析**：仍以「每 commit 一条请求、三项推断」为主；多维表联动为**可选增强**，在配置开启且仓库已绑定项目时，在输入中追加 `collab_records`，在输出中解析并处理 `related_records`。
- **多维表格**：不改变其权限、表结构、前端交互；仅通过本增强**接收来自 AI 分析结果的字段更新**（自动或经确认），实现「提交 → 需求状态」的闭环。

---

## 11. Webhook 信息极限与国内无 Diff 场景下的降级方案

### 11.1 服务器仅凭 Webhook 能拿到的信息极限

Push 事件是 **GitHub 主动推送到你服务器** 的，不要求服务器能访问外网。Payload 中与单次提交相关的信息**上限**如下（均来自请求体，无需再调 GitHub API）：

| 来源 | 字段 | 说明 |
|------|------|------|
| **push.commits[]** | `id` | 完整 commit SHA |
| | `message` | 提交说明全文 |
| | `timestamp` | 提交时间（ISO 8601） |
| | `url` | 该 commit 在 GitHub 上的链接（如 `https://github.com/owner/repo/commit/xxx`） |
| | `author.name`, `author.email`, `author.username` | 作者信息 |
| | `committer` | 同结构，提交者信息 |
| | **`added[]`** | **本次 commit 新增文件的路径列表**（仅路径，无内容） |
| | **`modified[]`** | **本次 commit 修改文件的路径列表**（仅路径，无内容） |
| | **`removed[]`** | **本次 commit 删除文件的路径列表**（仅路径） |
| **push** | `repository.full_name`, `ref`, `before`, `after`, `compare` | 仓库、分支、前后 SHA、对比页 URL |

**服务器拿不到的（需访问 GitHub API 或 git 仓库）：**

- **Diff 文本**（行级增删内容）：需请求 `GET /repos/{owner}/{repo}/commits/{ref}` 且 `Accept: application/vnd.github.v3.diff`，境内服务器往往无法直连 api.github.com。
- **文件内容**：需再调 Contents API 或本地 clone 后读文件。
- **行级统计**（insertions/deletions）：部分在 payload 的 `head_commit` 或需 API。

因此：**在国内仅能收到 Webhook 且无法访问 GitHub API 的前提下，服务器能稳定拿到的是「commit 元信息 + 本 commit 的 added/modified/removed 文件路径列表」，拿不到 diff 与文件内容。**

### 11.2 国内场景下如何尽量实现「AI 分析」相关功能

在**无 diff、仅 commit 元信息 + 文件路径列表**的前提下，仍可做的能力与取舍如下。

| 能力 | 有 Diff 时 | 无 Diff（仅 Webhook）时 |
|------|------------|---------------------------|
| **工作内容推断** | 根据 message + diff 精确推断 | 根据 **message + added/modified/removed 路径** 推断（如：路径含 `frontend/src/views/Login.vue` → 登录相关；`*.lock` → 依赖更新）。准确度略降但可用。 |
| **有效性推断** | 根据 diff 内容判断仅空白/注释/lockfile | 根据 **路径与 message** 判断：路径仅含 `package-lock.json`、`yarn.lock`、`*.md`、`docs/` 等可标为弱有效/无效；结合 message 关键词（如 "chore", "bump"）提高准确度。 |
| **代码质量推断** | 可根据代码内容评分与建议 | **不可做**（无代码内容），对应字段留空或标记「仅元信息模式无此项」。 |
| **多维表联动（关联需求 + 状态建议）** | 根据 diff 与需求描述精确匹配 | 根据 **message + 路径 + 多维表候选记录摘要** 做关联与状态建议（如 message 含「登录」、路径含 `auth` → 关联到「登录需求」记录）。精确度略降，仍可落库并走「建议 + 人工确认」回写。 |

**实现要点：**

1. **Webhook 解析**：在接收 Push 时解析每个 commit 的 `added`、`modified`、`removed`（若当前 DTO 未包含则补充），与现有 `message/author/timestamp` 一并落库（如扩展 `aa_commit` 或单独表存「每 commit 的文件路径列表」），**不依赖** `aa_commit_diff` 是否有数据。
2. **AI 任务创建**：  
   - **有 diff**：沿用现有设计，创建「完整分析」任务（三项推断 + 可选多维表联动）。  
   - **无 diff**：创建「仅元信息 + 路径」任务，输入中**不包含** `diff_text`，仅包含 message、author、timestamp、added/modified/removed 路径列表；调用国内可达的大模型 API（如 DeepSeek、通义、文心等），输出中**工作内容**与**有效性**必填，**代码质量**为空或固定占位（如 `quality_level: "no_diff"`）。
3. **多维表联动**：输入中仍可带「当前项目多维表候选记录摘要」；AI 根据 message + 路径推断 `related_records` 与状态建议，回写策略与 10 节一致（建议采用「待确认后写回」）。
4. **结果落库**：统一写入 `aa_ai_analysis_result`，用字段（如 `input_mode` = `full_diff` / `metadata_only`）或「代码质量为空」区分来源，便于统计与展示时标注「仅基于提交说明与路径，未含代码内容」。

这样，**在国内仅能拿到 commit 信息与多维表数据的场景下**，仍可尽量实现：  
- 基于 message + 路径的**工作内容**与**有效性** AI 分析；  
- **多维表联动**（关联需求 + 状态建议 + 经确认回写）；  
- 与「阅读多维表格」一致的数据源与权限模型。  
仅「代码质量」一项在无 diff 时不做，其余能力可最大程度保留。
