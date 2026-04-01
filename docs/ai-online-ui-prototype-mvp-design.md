# 功能设计文档：AI 在线 UI 原型生成（MVP：导出 `spec + preview`）

> 文档目标：让用户在你的平台“在线用 AI 快速生成可供客户观看的 UI 原型”，并可导出 `spec + preview`，用于后续开发指导。

## 1. 目标与范围

### 1.1 目标（必须具备）
1. 用户在网页端输入“页面需求”（自然语言 + 可选约束）。
2. 系统调用 LLM 生成 **UI 规范 `spec`（JSON）**。
3. 系统基于 `spec` 在前端渲染 **可交互原型预览**。
4. 支持用户导出：
   - `spec.json`
   - `preview`（静态可运行原型，客户无需接入你的后端即可查看）

### 1.2 非目标（明确不做，避免范围蔓延）
1. MVP 不生成“可直接运行的 Vue 组件骨架”（后续版本再做）。
2. MVP 不支持复杂业务数据联动（只实现点击态 / 切换面板 / Tabs 切换等 UI 交互容器级别）。
3. MVP 不允许模型输出任意 HTML/CSS 并注入到 DOM（避免安全与样式不可控）。

## 2. 用户流程（确定且可实现）

1. 用户进入“AI 原型工坊”页面。
2. 用户配置/选择 LLM provider（MVP 支持单或多 provider）。
3. 用户进行 **API Key 自助配置**（见第 7 节）。
4. （可选）用户点击“导入报价单需求”
   - 选择要导入的报价项目
   - 系统读取报价中的“模块 + 功能点（复杂度/数量）”，并用 AI 梳理成适合“快原型”的“页面需求”
   - 前端自动填充页面需求对话框，并提示“结构化需求已导入”
5. 用户在“页面需求”对话框中继续添加/补充其他要求，并提交生成请求。
6. 后端返回 `spec`，前端自动渲染预览。
7. 用户迭代反馈，生成下一版 `spec`（保留版本号）。
8. 用户导出：
   - `spec.json`
   - `preview/index.html`（内部内嵌 spec 或携带在 zip 中）

## 3. 总体架构

### 3.1 关键模块
1. 前端 Web：
   - 原型生成表单（输入需求、选择约束）
   - “导入报价单需求”入口（按钮 + 选择报价项目 + 回填 prompt）
   - `SpecRenderer`：spec -> 原型 UI（受控渲染器）
   - 导出器：打包 `spec + preview`
2. 后端 API：
   - `AI UI Spec Service`：负责组装系统提示词、调用 LLM、接收输出
   - `SpecValidator`：校验 spec 的结构、token、type 白名单
   - （新增可选）“报价需求导入服务”：读取报价结构化数据 -> AI 梳理 -> 输出 prompt 文本
   - （可选）版本存储：保存历史 specVersion 与导出记录
3. LLM 输出层：
   - 强约束：只输出严格 JSON（不可输出解释文本）
   - 强约束：token/type/children/interactions 必须满足 schema

### 3.2 最优雅的落点（为什么推荐 spec + 受控渲染器）
- 你不需要把 LLM 输出的“自由 HTML/CSS”直接渲染。
- 你的样式与交互由平台固定实现，从而“导出与预览一致”，后续开发指导也更可信。

### 3.3 “导入报价单需求”方案（可落地）
为了让用户更少手动编写自然语言需求，本功能将“智能报价”的结构化数据转写为“快原型”的可理解输入。

#### 3.3.1 输入数据来源（结合“智能报价”）
- 报价项目在系统中已形成结构化功能清单：`modules` 与其下的 `items`
- `items` 通常包含：功能点名称、复杂度、数量、以及（可选）已计算的人天估算
- 导入时建议优先使用：
  - 模块名称（用于 UI 分组）
  - 功能点名称（用于 UI 文案/卡片内容）
  - 复杂度/数量（可作为“描述粒度/信息密度”的提示）
- 不建议直接把报价金额、最终总价等财务数值写入“快原型 prompt”（除非用户明确要求在原型中展示财务信息）

#### 3.3.2 AI 梳理目标（输出给“页面需求”框）
AI 的任务不是生成 UI，而是把报价的“模块 + 功能点”重写为“页面要做什么”的自然语言输入，并满足本 MVP 的交互限制：
- 需要明确“页面要包含哪些模块区域”（用于生成 Panel/Tabs/Grid/Card）
- 需要明确“每个模块区域内部应出现哪些要点/功能点描述”（用于生成 Text/Button/Badge 等）
- 需要给出交互建议，但仍限定在本 MVP 允许的交互集合内：
  - `togglePanel`：用按钮/入口控制展开/收起某个模块面板
  - `setTab`：用 Tabs 切换不同模块区域

#### 3.3.3 输出回填策略（前端体验）
- 点击“导入报价单需求”后，系统生成一段“页面需求”文本 prompt
- 前端将其填入 `页面需求` 对话框，并提示“结构化需求已导入”
- 用户可继续在该文本基础上追加其他要求，再点击“生成并预览”

## 4. MVP 交互能力（必须支持）

### 4.1 允许的交互类型（只实现这两类）
1. `togglePanel`：展开/收起面板容器（如“抢先实验室”展开组件）
2. `setTab`：Tabs 切换面板内容（如四个标签的内容展示切换）

### 4.2 交互实现规则（确定且可控）
- 交互 target 必须指向一个已存在的节点 id（如 `panelId` / `tabsId`）。
- 交互仅影响 UI 状态，不触发任何后端请求。
- 交互不会改变布局结构树，只切换可见性/内容索引。

## 5. UI Spec 规范（高确定性：类型白名单 + token + 必填字段）

### 5.1 组件类型集合（MVP 固定白名单）
模型只能生成以下节点类型；其它类型全部拒绝并触发重试：
- `Page`（根节点）
- `Container`（布局容器）
- `Card`（卡片容器）
- `Text`（文本节点）
- `Button`（按钮节点）
- `Badge`（徽标/标签）
- `Tabs`（Tabs 容器）
- `Panel`（可展开/折叠面板）
- `Grid`（网格布局容器，用于“4 个一排”等）

### 5.2 token（不允许自由 CSS 字符串）
spec 的 style 只能引用 tokens，而不能输出任意 CSS 值。

#### 5.2.1 spacing tokens（确定性：px 映射）
- `space-4` = `4px`
- `space-8` = `8px`
- `space-12` = `12px`
- `space-16` = `16px`
- `space-24` = `24px`

#### 5.2.2 radius tokens
- `r-8` = `8px`
- `r-10` = `10px`
- `r-12` = `12px`

#### 5.2.3 color tokens（示例：可根据你项目主题微调，但需固定表）
- `primary` = `#2563eb`
- `primary-strong` = `#1d4ed8`
- `text` = `#0f172a`
- `text-muted` = `#64748b`
- `bg` = `#ffffff`
- `border-muted` = `#e5e7eb`
- `success` = `#22c55e`
- `info` = `#0ea5e9`
- `warn` = `#f59e0b`

#### 5.2.4 shadow tokens
- `shadow-soft` = `0 4px 24px rgba(15, 23, 42, 0.06)`
- `shadow-none` = `none`

### 5.3 Spec 必填结构（强制字段）
`spec.json` 最小结构必须满足下述结构（字段类型与含义按此文档实现）：

```json
{
  "meta": { "version": 1, "name": "string", "viewport": ["desktop","mobile"] },
  "theme": { "primaryColorToken": "primary" },
  "tokens": { "spacingScale": "px4", "radiusScale": "px8System" },
  "layout": { "root": "nodeId" },
  "nodes": {
    "nodeId": {
      "type": "Page|Container|Card|Text|Button|Badge|Tabs|Panel|Grid",
      "props": {},
      "style": {
        "padding": "space-16",
        "radius": "r-12",
        "bg": "bg",
        "border": "border-muted",
        "shadow": "shadow-none"
      },
      "children": ["childId1","childId2"]
    }
  },
  "interactions": [
    {
      "type": "togglePanel|setTab",
      "sourceId": "string",
      "targetId": "string",
      "params": { "open": true }
    }
  ]
}
```

### 5.4 交互节点约束（MVP）
1. `togglePanel`
   - `targetId` 必须是 `Panel` 节点
   - `params` 结构：`{ "open": true|false }` 或等价形式
2. `setTab`
   - `targetId` 必须是 `Tabs` 节点
   - `params` 结构：`{ "tabKey": "string" }`

## 6. MVP 预览渲染（方案1：纯原生 CSS，自包含）

### 6.1 选择原因
- 更稳定：不依赖 Tailwind CDN / 外部资源加载
- 更可控：你的 token 映射表是“唯一视觉来源”，导出与预览一致性更高

### 6.2 渲染器实现要点（确定性）
1. `SpecRenderer` 内置映射表：
   - node type -> 组件渲染函数
   - style token -> CSS 变量或固定 class
2. `Button`、`Card`、`Badge` 等统一实现 hover/active/pressed 等视觉状态。
3. Tabs：
   - Tabs 节点内部必须包含若干 Tab 子节点（或通过 props 提供 tab list）
   - 仅切换显示索引
4. Panel：
   - 初始状态由 spec 的 panel props 或 interactions 决定
   - 展开/收起仅影响可见性与高度过渡（MVP 可简单用 `display`/`max-height`）

## 7. API Key 自助配置（必须支持）

> 你的平台要让用户自定义 LLM 凭证，同时保证安全与可审计。

### 7.1 建议的后端接口（确定）
1. `POST /api/llm-keys/test`
   - 入参：`{ provider, apiKey, baseUrl? }`
   - 出参：`{ ok: boolean, modelCaps?: ... }`
   - 用途：让用户在保存前完成校验
2. `POST /api/llm-keys`
   - 入参：`{ provider, apiKey, baseUrl? }`
   - 出参：`{ keyId: string }`
   - 保存策略：服务器端加密存储（不落盘明文到前端/浏览器）
3. `POST /api/ai/ui-spec`
   - 入参：`{ keyId, prompt, constraints }`
   - 出参：`{ spec, specValidationResult, specVersion }`

### 7.2 安全要求（必须）
- 前端不得存明文 apiKey 到 `localStorage`（只存 `keyId` 或 token 引用）。
- 后端日志禁止输出 API Key。
- 数据库加密存储（至少对字段进行对称加密）。
- 限流与配额（按 userId + ip 双维度）。
- 错误码明确：
  - `401_KEY_INVALID`
  - `429_RATE_LIMIT`
  - `422_SPEC_INVALID`
  - `500_LLM_INTERNAL_ERROR`

## 8. LLM 生成策略（强约束 + 自我修复）

### 8.1 system prompt 强约束（确定性要求）
- 只允许输出严格 JSON（无前言无解释）
- 组件类型必须来自白名单
- style 只能使用 tokens
- children 必须引用已存在节点 id
- interactions 必须能在 nodes 中找到 `sourceId`/`targetId`

### 8.2 自动重试（失败可恢复）
1. 生成一次 -> 解析 JSON -> 校验 `SpecValidator`
2. 校验失败：
   - 将错误以结构化形式回填给模型
   - 要求模型“只修复 JSON，不改变语义目标”
3. 最多重试 N 次（建议 2 次）
4. 仍失败 -> 返回给前端“可读错误原因 + 原始校验摘要”

## 9. 导出格式（MVP 必须一致）

### 9.1 导出 zip 结构（固定）
- `/spec/spec.json`
- `/preview/index.html`
- `/preview/renderer.js`
- `/preview/styles.css`

### 9.2 preview 实现策略（确定）
- `index.html` 可选择：
  1. 运行时读取 `spec/spec.json`（若你允许静态服务器）
  2. 导出时把 spec inline 到 `index.html`（最简单自包含）

建议优先选项 2，以确保客户直接打开 zip 中 index.html 就可运行（无需跨域请求）。

## 10. 后续版本完善建议（不改变 MVP 的边界）

### 10.1 V1（紧跟 MVP，扩展交互与组件）
1. 扩展组件类型：
   - `Input/Select/FormField/List/Table`
2. 扩展交互：
   - 表单校验（仅前端规则，MVP 不联真实数据）
3. preview 支持更多交互：
   - 弹窗容器 `Modal`、下拉菜单 `Dropdown`（若需要）

### 10.2 V2（你提到的“可直接跑 Vue 组件骨架”）
1. 在后端或导出器中加入 `codegen`：
   - spec -> Vue SFC 骨架（template/props/events）
   - token -> CSS 变量或 style 片段
2. 增加 `interactionHandlers.ts`：
   - 将 `togglePanel/setTab` 等交互映射为事件处理逻辑

### 10.3 V3（接入你未来的 Design System）
- 即使你目前没有组件库，后续引入统一 Button/Card/Tabs 的视觉规则会显著降低“渲染一致性”风险。

## 11. 可行性落地清单（建议按顺序推进）
1. 实现 `SpecValidator`（schema + 白名单 + token 校验）
2. 实现 `SpecRenderer`（type -> 渲染；token -> CSS 映射）
3. 实现后端 `/api/ai/ui-spec`
4. 实现导出器（zip：spec + 自包含 preview）
5. UI 端接入生成/迭代与导出流程
6. （新增可选）实现“报价需求导入服务”
   - 读取智能报价的结构化模块/功能点
   - 调用 AI 梳理为“页面需求”prompt
   - 前端回填并提示“结构化需求已导入”

## 12. 附录：最小 spec 交互示例（MVP 验收用）

### 12.1 示例：Panel 展开/收起（伪结构）
- sourceId：某个 `Button` 节点
- targetId：某个 `Panel` 节点
- interactions：
  - `togglePanel`，`params: { "open": true }`

### 12.2 示例：Tabs 切换（伪结构）
- sourceId：Tabs 内 tab button
- targetId：Tabs 节点
- interactions：
  - `setTab`，`params: { "tabKey": "finance" }`

