# Commit ↔ 多维表联动：候选集与 Prompt 优化设计（评审稿）

> 状态：设计评审用，**尚未开发**。与已实现的三档 `automation_mode`、双表候选、仅自动写状态列等变更正交；本稿聚焦「候选质量、提示词、性能与成本」的后续迭代方案。

## 1. 背景与目标

当前实现（评审基线）：

- `auto_status` 下从 **issue_tracking**、**feature_backlog** 各取最多 40 条开放记录，按 `id DESC` 分页拉取后过滤「已关闭」行，拼入 `collab_records` JSON，与 **截断后的 diff** 一并送入 DeepSeek。
- 模型输出 `related_records`，服务端按 `table_kind` 仅写入 **「当前状态」** 或 **「开发进度」**。

**目标**：在不大改产品形态的前提下，降低无关 token、提高关联准确率、控制单次与月度费用，并保留可观测与可回滚空间。

## 2. 候选集质量优化

### 2.1 与 Commit 的粗相关预筛（推荐优先级：高）

**思路**：在调用大模型前，用确定性规则缩小 `record_id` 集合，再查库组装候选（或仍查 80 条但在内存丢弃）。

可选信号（可组合，权重可调）：

| 信号 | 适用表 | 说明 |
|------|----------|------|
| `files_changed_list` 与「归属模块」选项标签的字符串包含/前缀 | 两表 | 低成本，适合模块名与路径片段一致的场景 |
| commit message / 路径 token 与 `summary` 的简易交集（分词或 bigram） | 两表 | 无需向量库即可去掉明显无关行 |
| 仅保留 `updated_at` 在最近 N 天的记录（可配置） | 两表 | 避免陈旧需求占满 40 席位 |

**输出**：仍输出与现结构兼容的 `collab_records` 列表，但条数可低于上限，以省 token。

### 2.2 两阶段推理（可选，优先级：中）

**阶段 A**：极小 prompt，只让模型从候选 `record_id` 中输出 Top-K id 列表（或 JSON 数组），不做状态建议。  
**阶段 B**：仅对 Top-K 拉全字段摘要 + diff 再生成 `suggested_status`。

**代价**：两次 API 调用或一次长 prompt；适合大仓库、候选噪声极高的租户开关控制。

### 2.3 向量检索 / 嵌入（长期，优先级：低）

对 `summary`（及可选的最近评论）做离线或近实时嵌入，用 commit diff 摘要或 message 的嵌入做近邻检索，取 Top-N 再进主 prompt。需引入索引任务、维度与刷新策略，运维成本较高，建议有明确 ROI 再上。

### 2.4 「关闭」定义的租户可配置化

当前硬编码：`issue_tracking` 为「当前状态=已验收」或「验收结果=通过任务关闭」；`feature_backlog` 为「开发进度=已完成」。  

**建议**：在 `aa_project_ai_linkage_config` 或独立 JSON 配置中允许覆盖「结案」判定（选项值列表），减少与自定义选项组不一致导致的误筛或漏筛。

## 3. Prompt 与模型输出契约

### 3.1 结构化输出

- 若 DeepSeek API 支持 JSON Schema / `response_format`，对根对象约束字段类型，减少 `parseResult` / `related_records` 解析失败。
- 对 `related_records` 单项约束：`record_id`（整数）、`table_kind`（枚举）、`confidence`（枚举）、`suggested_status`（短字符串）、`match_reason`（长度上限）。

### 3.2 状态值白名单校验（服务端）

在 `applyLinkageSuggestion` 写入前，将 `suggested_status` 与对应列的 **选项组 JSON** 做校验；不在枚举内则丢弃并打 metrics，避免脏值进表（可与置信度并行作为硬闸门）。

### 3.3 Prompt 分层版本

维护 `prompt_version` 与联动附录文本版本号，便于 A/B 与回归；重大策略变更只升附录版本，不动基础 code review JSON 说明时可减少回归面。

## 4. 性能与成本

### 4.1 Token 与延迟

| 手段 | 效果 |
|------|------|
| 预筛后减少 `collab_records` 条数 | 直接降低 input tokens |
| diff 与候选分块：先摘要 diff 再关联（慎用，可能丢信息） | 大 commit 显著降费，需评估准确率 |
| 对同一 `repo@sha` 的请求结果短期缓存（已有分析结果则跳过） | 已有；可扩展到「候选构建结果」短期缓存（分钟级），注意表数据变更失效 |

### 4.2 调用频率

- 调度任务 `fixed-delay` 与每轮处理条数上限保持可配置，避免与「自动写表」高峰叠加大面积触发。
- `disabled` 项目在调度层短路（可选优化）：当前已在 `runAnalysis` 内短路，若调度侧能按 `repo -> project` 预过滤可减少 DB 读。

### 4.3 成本归因

- 在 usage 记录中增加 `linkage_candidate_count`、`automation_mode` 等标签（若表结构允许扩展 metadata），便于按项目统计联动带来的增量 token。

## 5. 可观测性与安全

- 指标：`linkage_prompt_tokens`、`linkage_apply_success`、`linkage_apply_reject_invalid_status`、`linkage_parse_error`。
- 日志：单次分析关联 `projectId`、`commitSha`、采纳条数（不落敏感 diff 全文）。
- 人工复核队列（若未来恢复「建议模式」）：本评审稿不包含实现，仅预留与 `related_records` 结构化落库表的衔接说明。

## 6. 建议实施顺序（供排期）

1. **服务端状态值白名单** + **预筛（路径/模块/message 交集）** — 收益/成本比最高。  
2. **租户可配置结案规则** — 减少与自定义选项不一致。  
3. **metrics + 日志字段** — 便于验证 1、2 的效果。  
4. 再评估 **两阶段推理** 或 **向量检索**。

## 7. 开放问题（需产品/你方拍板）

- 预筛过强导致「关联过少」时，是否在 UI 提示管理员调低强度或关闭预筛？  
- `feature_backlog` 是否参与联动写表在部分团队是否应可关闭（仅 issue 写表）？当前产品为双表一致策略。

---

**文档版本**：2026-05-09 评审稿 v1  
