<template>
  <div class="detail-page">
    <!-- 背景层 -->
    <div class="detail-bg" aria-hidden="true">
      <div class="detail-bg__mesh"></div>
      <div class="detail-bg__glow"></div>
    </div>

    <!-- 顶部导航 -->
    <header class="detail-header">
      <div class="detail-header-inner">
        <a href="/" class="detail-back">← 返回首页</a>
        <div class="detail-header-title">{{ feature.title }}</div>
        <a href="/login" class="detail-cta">立即体验</a>
      </div>
    </header>

    <!-- Hero -->
    <section class="detail-hero">
      <div class="detail-hero-badge">{{ feature.badge }}</div>
      <h1>{{ feature.title }}</h1>
      <p class="detail-hero-desc">{{ feature.subtitle }}</p>
      <div class="detail-hero-tags">
        <span v-for="tag in feature.tags" :key="tag" class="detail-tag">{{ tag }}</span>
      </div>
    </section>

    <!-- 模拟截图区 -->
    <section class="detail-screenshot-section">
      <div class="detail-screenshot-label">产品预览</div>
      <div class="detail-screenshot-frame" :class="{ 'detail-screenshot-frame--animate': visible }">
        <!-- 模拟浏览器顶栏 -->
        <div class="mock-browser-bar">
          <span class="mock-dot mock-dot--r"></span>
          <span class="mock-dot mock-dot--y"></span>
          <span class="mock-dot mock-dot--g"></span>
          <span class="mock-url">{{ feature.mockUrl }}</span>
        </div>
        <!-- 模拟页面内容 -->
        <div class="mock-page" v-html="feature.mockHtml"></div>
      </div>
    </section>

    <!-- 功能亮点 -->
    <section class="detail-features">
      <h2 class="detail-section-title">核心能力</h2>
      <div class="detail-features-grid">
        <div v-for="(f, i) in feature.highlights" :key="i"
             class="detail-highlight-card"
             :style="{ animationDelay: (i * 0.1) + 's' }">
          <div class="detail-highlight-icon">{{ f.icon }}</div>
          <h3>{{ f.title }}</h3>
          <p>{{ f.desc }}</p>
        </div>
      </div>
    </section>

    <!-- 数据指标 -->
    <section v-if="feature.metrics" class="detail-metrics">
      <h2 class="detail-section-title">关键指标</h2>
      <div class="detail-metrics-grid">
        <div v-for="(m, i) in feature.metrics" :key="i" class="detail-metric-card">
          <div class="detail-metric-value">{{ m.value }}</div>
          <div class="detail-metric-label">{{ m.label }}</div>
        </div>
      </div>
    </section>

    <!-- 使用流程 -->
    <section v-if="feature.steps" class="detail-steps">
      <h2 class="detail-section-title">使用流程</h2>
      <div class="detail-steps-track">
        <div v-for="(s, i) in feature.steps" :key="i" class="detail-step">
          <div class="detail-step-num">{{ i + 1 }}</div>
          <div class="detail-step-content">
            <h3>{{ s.title }}</h3>
            <p>{{ s.desc }}</p>
          </div>
        </div>
      </div>
    </section>

    <!-- 底部 CTA -->
    <section class="detail-bottom-cta">
      <h2>准备好开始了吗？</h2>
      <p>{{ feature.ctaDesc }}</p>
      <div class="detail-bottom-actions">
        <a href="/login" class="detail-btn detail-btn--primary">免费注册</a>
        <a href="/" class="detail-btn detail-btn--outline">了解更多功能</a>
      </div>
    </section>

    <!-- 页脚 -->
    <footer class="detail-footer">
      <span>© {{ new Date().getFullYear() }} 流帮 Project</span>
    </footer>
  </div>
</template>

<script>
const FEATURES = {
  console: {
    title: '控制台总览',
    badge: '指挥中心',
    subtitle: '一目了然掌握团队动态：API 健康状态、AI 用量、成员活跃度、仓库趋势，所有关键信息汇聚一屏。',
    tags: ['实时监控', 'AI 用量', '团队管理', '仓库集成'],
    mockUrl: 'autoattend.xn--gnr55wfngz8f.com/console',
    mockHtml: `
      <div class="mp mp--dashboard">
        <div class="mp-sidebar"><div class="mp-sidebar-item active">📊 控制台</div><div class="mp-sidebar-item">📋 协作表</div><div class="mp-sidebar-item">💰 报价</div><div class="mp-sidebar-item">🤖 AI 配置</div><div class="mp-sidebar-item">🧪 快原型</div><div class="mp-sidebar-item">👥 团队</div></div>
        <div class="mp-main">
          <div class="mp-status-bar"><span class="mp-chip mp-chip--ok">● DeepSeek 已连接</span><span class="mp-chip mp-chip--ok">● Qwen 已连接</span><span class="mp-chip">👥 5 人在线</span><span class="mp-chip">📦 3 仓库</span></div>
          <div class="mp-cards-row"><div class="mp-kpi"><div class="mp-kpi-val">¥128.50</div><div class="mp-kpi-label">AI 本月消耗</div></div><div class="mp-kpi"><div class="mp-kpi-val">12,450</div><div class="mp-kpi-label">Token 用量</div></div><div class="mp-kpi"><div class="mp-kpi-val">86</div><div class="mp-kpi-label">提交事件</div></div><div class="mp-kpi"><div class="mp-kpi-val">3</div><div class="mp-kpi-label">活跃仓库</div></div></div>
          <div class="mp-chart-placeholder"><div class="mp-chart-bar" style="height:60%"></div><div class="mp-chart-bar" style="height:80%"></div><div class="mp-chart-bar" style="height:45%"></div><div class="mp-chart-bar" style="height:90%"></div><div class="mp-chart-bar" style="height:70%"></div><div class="mp-chart-bar" style="height:55%"></div><div class="mp-chart-bar" style="height:85%"></div></div>
        </div>
      </div>`,
    highlights: [
      { icon: '🟢', title: 'API 实时健康检测', desc: '自动检测 DeepSeek、通义千问等 AI 模型的连通性，异常即时告警' },
      { icon: '📊', title: 'AI 用量可视化', desc: '按模型、按时间维度展示 Token 消耗与调用次数，成本一目了然' },
      { icon: '👥', title: '团队活跃度', desc: '实时显示在线成员数、最近提交记录与开发者排名' },
      { icon: '🔗', title: '仓库趋势图', desc: '绑定 GitHub/GitLab 后自动同步提交趋势与代码活跃度' }
    ],
    metrics: [
      { value: '< 3s', label: 'API 响应延迟' },
      { value: '99.5%', label: '服务可用性' },
      { value: '∞', label: '仓库接入数' },
      { value: '实时', label: '数据刷新' }
    ],
    steps: [
      { title: '配置 AI 密钥', desc: '在 AI 配置页填入 DeepSeek/通义千问的 API Key' },
      { title: '绑定代码仓库', desc: '添加 GitHub/GitLab 仓库 Webhook，自动同步提交事件' },
      { title: '查看实时数据', desc: '控制台自动展示 API 状态、提交趋势和 AI 用量' }
    ],
    ctaDesc: '注册即可免费体验控制台全部功能，无需绑定信用卡。'
  },
  collab: {
    title: '协作多维表',
    badge: '项目管理',
    subtitle: '像使用飞书多维表一样管理项目功能清单，支持 AI 智能录入、CSV 批量导入，与代码仓库深度联动。',
    tags: ['多维表格', 'AI 录入', 'CSV 导入', '权限管理', '公开阅览'],
    mockUrl: 'autoattend.xn--gnr55wfngz8f.com/collab/projects',
    mockHtml: `
      <div class="mp mp--collab">
        <div class="mp-sidebar"><div class="mp-sidebar-item active">🏠 首页看板</div><div class="mp-sidebar-item">🐛 问题追踪</div><div class="mp-sidebar-item">✅ 功能待办</div></div>
        <div class="mp-main">
          <div class="mp-toolbar"><span class="mp-toolbar-title">功能清单</span><div class="mp-chip-group"><span class="mp-chip mp-chip--active">全部</span><span class="mp-chip">P0</span><span class="mp-chip">P1</span><span class="mp-chip">进行中</span></div><span class="mp-ai-btn">✦ AI 录入</span></div>
          <div class="mp-table"><div class="mp-table-head"><span class="mp-th" style="width:40px">☐</span><span class="mp-th">ID</span><span class="mp-th">功能名称</span><span class="mp-th">优先级</span><span class="mp-th">状态</span><span class="mp-th">负责人</span></div>
          <div class="mp-table-row"><span class="mp-td">☐</span><span class="mp-td mp-muted">#001</span><span class="mp-td">用户登录与注册</span><span class="mp-td"><span class="mp-pill mp-pill--red">P0</span></span><span class="mp-td"><span class="mp-pill mp-pill--green">已完成</span></span><span class="mp-td">张三</span></div>
          <div class="mp-table-row"><span class="mp-td">☐</span><span class="mp-td mp-muted">#002</span><span class="mp-td">商品搜索与筛选</span><span class="mp-td"><span class="mp-pill mp-pill--red">P0</span></span><span class="mp-td"><span class="mp-pill mp-pill--blue">进行中</span></span><span class="mp-td">李四</span></div>
          <div class="mp-table-row"><span class="mp-td">☐</span><span class="mp-td mp-muted">#003</span><span class="mp-td">订单管理后台</span><span class="mp-td"><span class="mp-pill mp-pill--yellow">P1</span></span><span class="mp-td"><span class="mp-pill mp-pill--gray">待开始</span></span><span class="mp-td">王五</span></div>
          <div class="mp-table-row"><span class="mp-td">☐</span><span class="mp-td mp-muted">#004</span><span class="mp-td">支付对接</span><span class="mp-td"><span class="mp-pill mp-pill--yellow">P1</span></span><span class="mp-td"><span class="mp-pill mp-pill--gray">待开始</span></span><span class="mp-td">赵六</span></div>
          <div class="mp-table-row"><span class="mp-td">☐</span><span class="mp-td mp-muted">#005</span><span class="mp-td">数据报表导出</span><span class="mp-td"><span class="mp-pill mp-pill--blue">P2</span></span><span class="mp-td"><span class="mp-pill mp-pill--gray">待开始</span></span><span class="mp-td">—</span></div>
          </div>
        </div>
      </div>`,
    highlights: [
      { icon: '✦', title: 'AI 智能录入', desc: '用自然语言描述功能需求，AI 自动解析为结构化功能点并填入表格' },
      { icon: '📄', title: 'CSV 批量导入', desc: '支持从 Excel/CSV 批量导入功能清单，AI 自动识别列映射' },
      { icon: '🔗', title: '仓库联动', desc: '与 Git 仓库绑定后，提交记录自动关联到功能点' },
      { icon: '👁', title: '客户公开阅览', desc: '生成只读阅览链接，客户可实时查看项目进度' }
    ],
    metrics: [
      { value: '10万+', label: '单次 AI 录入字数' },
      { value: '3000行', label: 'CSV 批量导入' },
      { value: '实时', label: '阅览同步' },
      { value: '角色级', label: '权限粒度' }
    ],
    steps: [
      { title: '创建协作项目', desc: '新建项目并选择模板（问题追踪 / 功能待办）' },
      { title: '录入功能清单', desc: '手动录入、AI 智能生成或 CSV 批量导入' },
      { title: '分配与跟踪', desc: '设置优先级、负责人，实时跟踪完成进度' },
      { title: '共享给客户', desc: '生成公开阅览链接，客户实时查看项目状态' }
    ],
    ctaDesc: '从零开始管理你的下一个项目功能清单。'
  },
  quote: {
    title: '报价与商务',
    badge: '智能报价',
    subtitle: '从需求到报价单的全流程自动化：AI 辅助需求分析、结构化功能清单、智能计算报价、自动生成合同。',
    tags: ['需求分析', 'AI 报价', '合同生成', 'Agent 引导'],
    mockUrl: 'autoattend.xn--gnr55wfngz8f.com/quote',
    mockHtml: `
      <div class="mp mp--quote">
        <div class="mp-main">
          <div class="mp-quote-header"><span class="mp-quote-title">报价项目</span><span class="mp-btn-mp">+ 新建报价</span></div>
          <div class="mp-quote-cards">
            <div class="mp-quote-card"><div class="mp-quote-card-id">#QT-001</div><div class="mp-quote-card-name">企业电商平台</div><div class="mp-quote-card-meta"><span class="mp-pill mp-pill--blue">单体项目</span><span class="mp-muted">Vue + Node</span></div><div class="mp-quote-card-price">¥ 285,000</div></div>
            <div class="mp-quote-card"><div class="mp-quote-card-id">#QT-002</div><div class="mp-quote-card-name">金融风控系统</div><div class="mp-quote-card-meta"><span class="mp-pill mp-pill--purple">解决方案</span><span class="mp-muted">Java + Spring</span></div><div class="mp-quote-card-price">¥ 520,000</div></div>
          </div>
          <div class="mp-agent-box"><div class="mp-agent-badge">🤖 Agent 专属链接</div><div class="mp-agent-url">http://autoattend.../agent/abc123</div><span class="mp-btn-mp-sm">复制链接</span></div>
        </div>
      </div>`,
    highlights: [
      { icon: '🤖', title: '需求顾问 Agent', desc: '通过对话式 Agent 引导客户自助描述需求，AI 自动整理为结构化功能清单' },
      { icon: '📐', title: '智能报价计算', desc: '基于功能点、技术栈、风险系数自动计算人天、工期和报价金额' },
      { icon: '📝', title: '合同自动生成', desc: 'AI 根据报价结果自动生成合同正文，支持付款计划配置' },
      { icon: '📊', title: '多维度置信度', desc: '报价结果附带置信度评分和风险提示，辅助商务决策' }
    ],
    metrics: [
      { value: '3 分钟', label: 'Agent 收集需求' },
      { value: '自动', label: '报价计算' },
      { value: 'AI 生成', label: '合同正文' },
      { value: '±15%', label: '报价精度' }
    ],
    steps: [
      { title: '创建报价项目', desc: '选择单体项目或解决方案模式，填写基础信息' },
      { title: 'Agent 收集需求', desc: '生成专属链接发给客户，Agent 对话引导需求描述' },
      { title: 'AI 计算报价', desc: '一键生成报价：人天、工期、风险系数、置信度评分' },
      { title: '导出合同', desc: 'AI 自动生成合同正文，配置付款计划后导出' }
    ],
    ctaDesc: '让报价从 3 天缩短到 30 分钟。'
  },
  ai: {
    title: 'API 配置与智能分析',
    badge: 'AI 引擎',
    subtitle: '一站式管理 DeepSeek、通义千问等 AI 模型密钥，为提交分析、日报生成、多维表 AI 录入提供底层能力。',
    tags: ['DeepSeek', '通义千问', '提交分析', '日报生成'],
    mockUrl: 'autoattend.xn--gnr55wfngz8f.com/api-config',
    mockHtml: `
      <div class="mp mp--ai">
        <div class="mp-main">
          <div class="mp-ai-hero"><h3>配置你的 AI 模型</h3><p>4 步完成配置，立即解锁全部 AI 功能</p></div>
          <div class="mp-ai-steps"><div class="mp-ai-step"><div class="mp-ai-step-num">1</div><div>注册 API Key</div></div><div class="mp-ai-step"><div class="mp-ai-step-num">2</div><div>填入密钥</div></div><div class="mp-ai-step"><div class="mp-ai-step-num">3</div><div>测试连通</div></div><div class="mp-ai-step"><div class="mp-ai-step-num">4</div><div>开始使用</div></div></div>
          <div class="mp-ai-providers"><div class="mp-ai-provider"><div class="mp-ai-provider-icon">🧠</div><div class="mp-ai-provider-name">DeepSeek</div><div class="mp-ai-provider-status"><span class="mp-chip mp-chip--ok">● 已配置</span></div></div><div class="mp-ai-provider"><div class="mp-ai-provider-icon">🔮</div><div class="mp-ai-provider-name">通义千问</div><div class="mp-ai-provider-status"><span class="mp-chip mp-chip--ok">● 已配置</span></div></div></div>
        </div>
      </div>`,
    highlights: [
      { icon: '🔑', title: '多模型密钥管理', desc: '支持 DeepSeek、通义千问等多个 AI 提供商，密钥安全存储' },
      { icon: '🔍', title: '提交级 AI 分析', desc: '每次代码提交自动触发 AI 分析，生成代码审查建议' },
      { icon: '📋', title: 'AI 日报生成', desc: '自动汇总当日提交活动，生成结构化开发日报' },
      { icon: '🛡', title: '数据导出守卫', desc: '敏感数据导出需密码验证，防止误操作泄露' }
    ],
    metrics: [
      { value: '3+', label: 'AI 提供商' },
      { value: '秒级', label: '分析响应' },
      { value: '自动', label: '日报生成' },
      { value: '加密', label: '密钥存储' }
    ],
    steps: [
      { title: '获取 API Key', desc: '前往 DeepSeek 或通义千问官网注册并获取 API Key' },
      { title: '填入配置', desc: '在 AI 配置页粘贴 API Key，系统自动验证连通性' },
      { title: '解锁功能', desc: '配置完成后自动解锁提交分析、日报、AI 录入等功能' }
    ],
    ctaDesc: '配置 AI 模型只需 2 分钟，立即解锁全部智能功能。'
  },
  prototype: {
    title: '快原型',
    badge: 'AI 原型',
    subtitle: '用 AI 快速生成交互原型：输入需求描述，自动生成 UI 规格文档，并提供可交互的在线预览。',
    tags: ['AI 生成', '在线预览', '规格文档', 'Penpot 集成'],
    mockUrl: 'autoattend.xn--gnr55wfngz8f.com/prototype',
    mockHtml: `
      <div class="mp mp--proto">
        <div class="mp-main">
          <div class="mp-proto-header"><h3>快原型</h3><p>创建多个原型项目，用 AI 生成 spec，并为客户提供可交互的 preview</p><span class="mp-btn-mp">+ 新建项目</span></div>
          <div class="mp-proto-list"><div class="mp-proto-item"><div class="mp-proto-item-icon">🎨</div><div class="mp-proto-item-info"><div class="mp-proto-item-name">企业官网 V2</div><div class="mp-proto-item-meta">版本 v3 · 更新于 2 小时前</div></div><span class="mp-btn-mp-sm">进入</span></div><div class="mp-proto-item"><div class="mp-proto-item-icon">📱</div><div class="mp-proto-item-info"><div class="mp-proto-item-name">电商 APP</div><div class="mp-proto-item-meta">版本 v1 · 更新于 1 天前</div></div><span class="mp-btn-mp-sm">进入</span></div></div>
        </div>
      </div>`,
    highlights: [
      { icon: '🤖', title: 'AI 生成 UI 规格', desc: '输入需求描述，AI 自动生成详细的 UI 规格文档' },
      { icon: '👁', title: '可交互预览', desc: '基于规格文档自动渲染可交互的原型预览' },
      { icon: '🔗', title: 'Penpot 集成', desc: '与 Penpot 设计工具深度集成，支持导出为设计稿' },
      { icon: '📱', title: '多端适配', desc: '自动生成桌面端和移动端两套原型方案' }
    ],
    metrics: [
      { value: '5 分钟', label: '生成原型' },
      { value: '多端', label: '适配方案' },
      { value: 'AI', label: '规格生成' },
      { value: '在线', label: '交互预览' }
    ],
    steps: [
      { title: '新建原型项目', desc: '创建项目并输入产品名称和需求描述' },
      { title: 'AI 生成规格', desc: 'AI 自动生成详细的 UI 组件规格文档' },
      { title: '在线预览', desc: '实时查看可交互的原型效果，支持多端切换' }
    ],
    ctaDesc: '从需求描述到可交互原型，只需 5 分钟。'
  },
  team: {
    title: '团队与账号',
    badge: '组织管理',
    subtitle: '灵活的团队协作体系：多租户隔离、角色权限管理、成员邀请，适配从个人到企业的各种规模。',
    tags: ['多租户', '角色权限', '成员管理', '组织隔离'],
    mockUrl: 'autoattend.xn--gnr55wfngz8f.com/team',
    mockHtml: `
      <div class="mp mp--team">
        <div class="mp-main">
          <div class="mp-team-header"><h3>团队成员</h3><span class="mp-btn-mp">+ 邀请成员</span></div>
          <div class="mp-team-list"><div class="mp-team-member"><div class="mp-avatar" style="background:#4f8cff">张</div><div class="mp-team-info"><div class="mp-team-name">张三</div><div class="mp-team-role">管理员</div></div><span class="mp-chip mp-chip--ok">活跃</span></div><div class="mp-team-member"><div class="mp-avatar" style="background:#a855f7">李</div><div class="mp-team-info"><div class="mp-team-name">李四</div><div class="mp-team-role">开发者</div></div><span class="mp-chip mp-chip--ok">活跃</span></div><div class="mp-team-member"><div class="mp-avatar" style="background:#f59e0b">王</div><div class="mp-team-info"><div class="mp-team-name">王五</div><div class="mp-team-role">观察者</div></div><span class="mp-chip">离线</span></div></div>
        </div>
      </div>`,
    highlights: [
      { icon: '🏢', title: '多租户隔离', desc: '每个组织独立租户，数据完全隔离，互不可见' },
      { icon: '🔑', title: '角色权限', desc: '管理员、开发者、观察者等多级角色，精细化权限控制' },
      { icon: '📧', title: '成员邀请', desc: '通过链接或手机号邀请成员加入团队' },
      { icon: '📱', title: '手机号登录', desc: '支持阿里云短信验证码登录，安全便捷' }
    ],
    metrics: [
      { value: '∞', label: '租户数量' },
      { value: '4 级', label: '角色体系' },
      { value: '独立', label: '数据隔离' },
      { value: '短信', label: '安全登录' }
    ],
    steps: [
      { title: '注册组织', desc: '手机号 + 验证码快速注册，自动创建组织租户' },
      { title: '邀请成员', desc: '通过链接邀请团队成员，设置角色权限' },
      { title: '开始协作', desc: '成员加入后即可参与项目协作和功能开发' }
    ],
    ctaDesc: '从个人开发者到企业团队，灵活适配。'
  },
  nexus: {
    title: '快捷运维（Nexus）',
    badge: '云运维',
    subtitle: '多云账号统一管理：实例监控、成本分析、告警规则，让运维工作一目了然。',
    tags: ['多云管理', '成本分析', '告警规则', '实例监控'],
    mockUrl: 'autoattend.xn--gnr55wfngz8f.com/nexus',
    mockHtml: `
      <div class="mp mp--nexus">
        <div class="mp-main">
          <div class="mp-nexus-cards"><div class="mp-nexus-card"><div class="mp-nexus-card-icon">☁️</div><div class="mp-nexus-card-val">3</div><div class="mp-nexus-card-label">云账号</div></div><div class="mp-nexus-card"><div class="mp-nexus-card-icon">🖥</div><div class="mp-nexus-card-val">12</div><div class="mp-nexus-card-label">实例数</div></div><div class="mp-nexus-card"><div class="mp-nexus-card-icon">💰</div><div class="mp-nexus-card-val">¥2,340</div><div class="mp-nexus-card-label">本月成本</div></div></div>
          <div class="mp-nexus-table"><div class="mp-table-head"><span class="mp-th">实例</span><span class="mp-th">状态</span><span class="mp-th">CPU</span><span class="mp-th">内存</span></div><div class="mp-table-row"><span class="mp-td">web-prod-01</span><span class="mp-td"><span class="mp-chip mp-chip--ok">运行中</span></span><span class="mp-td">32%</span><span class="mp-td">4.2 / 8 GB</span></div><div class="mp-table-row"><span class="mp-td">db-master</span><span class="mp-td"><span class="mp-chip mp-chip--ok">运行中</span></span><span class="mp-td">68%</span><span class="mp-td">14 / 16 GB</span></div></div>
        </div>
      </div>`,
    highlights: [
      { icon: '☁', title: '多云统一管理', desc: '一个面板管理阿里云、AWS 等多个云平台的资源' },
      { icon: '📊', title: '成本分析', desc: '按账号、按实例维度展示云资源成本，支持趋势分析' },
      { icon: '🔔', title: '告警规则', desc: '自定义 CPU、内存、磁盘等指标的告警阈值' },
      { icon: '📈', title: '指标巡检', desc: '定时采集实例指标，异常自动通知' }
    ],
    metrics: [
      { value: '多云', label: '平台支持' },
      { value: '实时', label: '指标采集' },
      { value: '自定义', label: '告警规则' },
      { value: '趋势', label: '成本分析' }
    ],
    steps: [
      { title: '接入云账号', desc: '添加阿里云/AWS 等云平台账号，授权后自动同步资源' },
      { title: '查看仪表盘', desc: '统一查看所有实例状态、指标和成本' },
      { title: '配置告警', desc: '设置告警规则，异常情况自动通知' }
    ],
    ctaDesc: '多云资源统一管理，运维效率提升 10 倍。'
  },
  cloud: {
    title: '云开发中心',
    badge: '智能体聚合',
    subtitle: '聚合 Cursor Cloud Agent、TRAE SOLO、OpenAI Codex 等云端智能体入口，一站式访问所有开发工具。',
    tags: ['Cursor', 'TRAE', 'Codex', '智能体'],
    mockUrl: 'autoattend.xn--gnr55wfngz8f.com/cloud-dev',
    mockHtml: `
      <div class="mp mp--cloud">
        <div class="mp-main">
          <div class="mp-cloud-hero"><h3>云开发中心</h3><p>聚合主流云端智能体，一键跳转</p></div>
          <div class="mp-cloud-grid"><div class="mp-cloud-card"><div class="mp-cloud-icon">🎯</div><div class="mp-cloud-name">Cursor Cloud Agent</div><div class="mp-cloud-desc">AI 辅助编码</div><span class="mp-btn-mp-sm">打开</span></div><div class="mp-cloud-card"><div class="mp-cloud-icon">⚡</div><div class="mp-cloud-name">TRAE SOLO</div><div class="mp-cloud-desc">全栈开发助手</div><span class="mp-btn-mp-sm">打开</span></div><div class="mp-cloud-card"><div class="mp-cloud-icon">🤖</div><div class="mp-cloud-name">OpenAI Codex</div><div class="mp-cloud-desc">代码生成</div><span class="mp-btn-mp-sm">打开</span></div></div>
        </div>
      </div>`,
    highlights: [
      { icon: '🎯', title: 'Cursor Cloud Agent', desc: '集成 Cursor 的 AI 辅助编码能力，云端直接使用' },
      { icon: '⚡', title: 'TRAE SOLO', desc: '全栈开发助手，支持从需求到部署的完整流程' },
      { icon: '🤖', title: 'OpenAI Codex', desc: 'OpenAI 代码生成模型，支持多种编程语言' },
      { icon: '🔌', title: '统一入口', desc: '一个页面访问所有云端开发工具，无需逐个切换' }
    ],
    metrics: [
      { value: '3+', label: '智能体集成' },
      { value: '一键', label: '快速访问' },
      { value: '统一', label: '管理入口' },
      { value: '持续', label: '更新迭代' }
    ],
    steps: [
      { title: '浏览智能体', desc: '查看所有可用的云端开发智能体列表' },
      { title: '一键打开', desc: '点击即可跳转到对应的云端开发工具' }
    ],
    ctaDesc: '所有云端开发工具，一个入口搞定。'
  },
  client: {
    title: '客户阅览看板',
    badge: '透明交付',
    subtitle: '面向甲方的只读看板：功能进度、开发日报、多维表阅览，让项目交付过程完全透明。',
    tags: ['只读看板', '进度追踪', '日报阅览', '多维表'],
    mockUrl: 'autoattend.xn--gnr55wfngz8f.com/board',
    mockHtml: `
      <div class="mp mp--client">
        <div class="mp-main">
          <div class="mp-client-hero"><div class="mp-client-badge">📊 项目看板</div><div class="mp-client-name">企业电商平台 V2</div></div>
          <div class="mp-kpi-row"><div class="mp-kpi"><div class="mp-kpi-val">24</div><div class="mp-kpi-label">总任务</div></div><div class="mp-kpi"><div class="mp-kpi-val" style="color:#22c55e">18</div><div class="mp-kpi-label">已完成</div></div><div class="mp-kpi"><div class="mp-kpi-val" style="color:#f59e0b">4</div><div class="mp-kpi-label">进行中</div></div><div class="mp-kpi"><div class="mp-kpi-val" style="color:#3b82f6">75%</div><div class="mp-kpi-label">完成率</div></div></div>
          <div class="mp-client-chart"><div class="mp-chart-bar" style="height:40%;background:#22c55e"></div><div class="mp-chart-bar" style="height:65%;background:#22c55e"></div><div class="mp-chart-bar" style="height:80%;background:#3b82f6"></div><div class="mp-chart-bar" style="height:55%;background:#3b82f6"></div><div class="mp-chart-bar" style="height:90%;background:#22c55e"></div><div class="mp-chart-bar" style="height:70%;background:#22c55e"></div></div>
        </div>
      </div>`,
    highlights: [
      { icon: '📊', title: 'KPI 仪表盘', desc: '总任务、已完成、进行中、完成率四大核心指标一目了然' },
      { icon: '📈', title: '趋势图表', desc: '周趋势折线图和优先级分布图，直观展示项目进展' },
      { icon: '📋', title: '功能清单', desc: '只读查看功能清单和进度状态，彩色标签区分优先级' },
      { icon: '🔒', title: '安全只读', desc: '客户只能查看，无法修改任何数据，确保信息安全' }
    ],
    metrics: [
      { value: '实时', label: '进度同步' },
      { value: '只读', label: '安全模式' },
      { value: '4 维', label: 'KPI 指标' },
      { value: '链接', label: '免登录访问' }
    ],
    steps: [
      { title: '生成阅览链接', desc: '在协作项目中一键生成公开阅览链接' },
      { title: '发送给客户', desc: '客户通过链接免登录直接访问看板' },
      { title: '实时查看进度', desc: '功能清单和 KPI 数据实时同步更新' }
    ],
    ctaDesc: '让客户随时了解项目进展，减少沟通成本。'
  },
  agent: {
    title: '需求顾问 Agent',
    badge: 'AI 对话',
    subtitle: '基于大模型的需求收集智能体：通过自然对话引导客户描述需求，AI 自动整理为结构化功能清单。',
    tags: ['自然对话', '需求分析', '文件上传', '自动摘要'],
    mockUrl: 'autoattend.xn--gnr55wfngz8f.com/agent',
    mockHtml: `
      <div class="mp mp--agent">
        <div class="mp-main">
          <div class="mp-agent-header"><span class="mp-agent-logo">🤖</span><span class="mp-agent-title">流帮 Project</span><span class="mp-agent-tenant">示例科技</span></div>
          <div class="mp-agent-chat">
            <div class="mp-msg mp-msg--bot"><div class="mp-msg-avatar">🤖</div><div class="mp-msg-bubble">你好！我是需求顾问助手。请告诉我你的项目需求，我会帮你整理为结构化功能清单。</div></div>
            <div class="mp-msg mp-msg--user"><div class="mp-msg-bubble">我需要一个电商平台，支持商品管理、订单处理和支付功能。</div></div>
            <div class="mp-msg mp-msg--bot"><div class="mp-msg-avatar">🤖</div><div class="mp-msg-bubble">好的，我来帮你梳理。初步整理为以下功能模块：<br>1. <b>用户管理</b>：注册、登录、个人中心<br>2. <b>商品管理</b>：商品列表、搜索、详情<br>3. <b>订单管理</b>：购物车、下单、支付<br>4. <b>后台管理</b>：数据统计、权限管理<br><br>需要我进一步细化某个模块吗？</div></div>
          </div>
          <div class="mp-agent-input"><span class="mp-agent-attach">📎</span><div class="mp-agent-input-box">输入你的需求...</div><span class="mp-agent-send">发送</span></div>
        </div>
      </div>`,
    highlights: [
      { icon: '💬', title: '自然对话', desc: '客户用日常语言描述需求，无需理解技术术语' },
      { icon: '📎', title: '文件上传', desc: '支持上传需求文档、图片、视频等多模态文件' },
      { icon: '📋', title: '自动摘要', desc: '对话结束后 AI 自动生成结构化需求摘要' },
      { icon: '🔗', title: '一键应用', desc: '摘要可直接应用到报价项目的功能清单中' }
    ],
    metrics: [
      { value: '3 分钟', label: '平均对话时长' },
      { value: '多模态', label: '文件支持' },
      { value: '自动', label: '摘要生成' },
      { value: '一键', label: '应用到报价' }
    ],
    steps: [
      { title: '生成 Agent 链接', desc: '在报价项目中创建 Agent 会话，生成专属链接' },
      { title: '发送给客户', desc: '客户通过链接直接打开对话界面，无需注册登录' },
      { title: 'AI 引导需求', desc: 'Agent 通过多轮对话引导客户完整描述需求' },
      { title: '自动生成摘要', desc: '对话结束后 AI 自动整理为结构化功能清单' }
    ],
    ctaDesc: '让客户自己说需求，AI 帮你整理。'
  }
}

export default {
  name: 'LandingDetailView',
  data () {
    return {
      visible: false,
      feature: FEATURES.console
    }
  },
  watch: {
    '$route.params.id': {
      immediate: true,
      handler (id) {
        this.feature = FEATURES[id] || FEATURES.console
        this.visible = false
        this.$nextTick(() => { this.visible = true })
        window.scrollTo(0, 0)
      }
    }
  }
}
</script>

<style scoped>
/* ===== 页面基础 ===== */
.detail-page {
  position: relative;
  min-height: 100vh;
  color: #e2e8f0;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
  overflow-x: hidden;
}

/* ===== 背景 ===== */
.detail-bg {
  position: fixed;
  inset: 0;
  z-index: -1;
  background: linear-gradient(165deg, #0a0e1a 0%, #0f172a 40%, #0c1222 100%);
}
.detail-bg__mesh {
  position: absolute;
  inset: 0;
  background-image:
    linear-gradient(rgba(56, 189, 248, 0.04) 1px, transparent 1px),
    linear-gradient(90deg, rgba(56, 189, 248, 0.04) 1px, transparent 1px);
  background-size: 48px 48px;
}
.detail-bg__glow {
  position: absolute;
  width: 600px;
  height: 600px;
  border-radius: 50%;
  top: -100px;
  right: -100px;
  background: radial-gradient(circle, rgba(59, 130, 246, 0.12), transparent 70%);
  filter: blur(80px);
}

/* ===== Header ===== */
.detail-header {
  position: sticky;
  top: 0;
  z-index: 100;
  backdrop-filter: blur(14px);
  background: rgba(10, 14, 26, 0.8);
  border-bottom: 1px solid rgba(255, 255, 255, 0.06);
}
.detail-header-inner {
  max-width: 1100px;
  margin: 0 auto;
  padding: 14px 24px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.detail-back {
  color: #94a3b8;
  text-decoration: none;
  font-size: 14px;
  transition: color 0.15s;
}
.detail-back:hover { color: #e2e8f0; }
.detail-header-title {
  font-size: 15px;
  font-weight: 600;
  color: #f1f5f9;
}
.detail-cta {
  background: #3b82f6;
  color: #fff;
  text-decoration: none;
  padding: 7px 18px;
  border-radius: 8px;
  font-size: 13px;
  font-weight: 600;
  transition: background 0.15s, transform 0.1s;
}
.detail-cta:hover { background: #2563eb; transform: translateY(-1px); }

/* ===== Hero ===== */
.detail-hero {
  text-align: center;
  padding: 80px 24px 48px;
  max-width: 720px;
  margin: 0 auto;
}
.detail-hero-badge {
  display: inline-block;
  padding: 4px 14px;
  border-radius: 20px;
  background: rgba(59, 130, 246, 0.12);
  color: #60a5fa;
  font-size: 13px;
  font-weight: 600;
  margin-bottom: 20px;
  border: 1px solid rgba(59, 130, 246, 0.2);
}
.detail-hero h1 {
  font-size: 42px;
  font-weight: 800;
  background: linear-gradient(135deg, #f1f5f9, #94a3b8, #60a5fa);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  margin: 0 0 16px;
  line-height: 1.2;
}
.detail-hero-desc {
  font-size: 17px;
  color: #94a3b8;
  line-height: 1.7;
  margin: 0 0 24px;
}
.detail-hero-tags {
  display: flex;
  justify-content: center;
  flex-wrap: wrap;
  gap: 8px;
}
.detail-tag {
  padding: 4px 12px;
  border-radius: 6px;
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.08);
  font-size: 13px;
  color: #cbd5e1;
}

/* ===== 截图区 ===== */
.detail-screenshot-section {
  max-width: 960px;
  margin: 0 auto 80px;
  padding: 0 24px;
}
.detail-screenshot-label {
  text-align: center;
  font-size: 13px;
  color: #64748b;
  margin-bottom: 12px;
  text-transform: uppercase;
  letter-spacing: 2px;
}
.detail-screenshot-frame {
  border-radius: 12px;
  border: 1px solid rgba(255, 255, 255, 0.08);
  overflow: hidden;
  background: #111827;
  box-shadow: 0 25px 60px rgba(0, 0, 0, 0.4);
  opacity: 0;
  transform: translateY(30px);
  transition: opacity 0.8s ease, transform 0.8s ease;
}
.detail-screenshot-frame--animate {
  opacity: 1;
  transform: translateY(0);
}
.mock-browser-bar {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 10px 14px;
  background: #1e293b;
  border-bottom: 1px solid rgba(255, 255, 255, 0.06);
}
.mock-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
}
.mock-dot--r { background: #ef4444; }
.mock-dot--y { background: #eab308; }
.mock-dot--g { background: #22c55e; }
.mock-url {
  flex: 1;
  text-align: center;
  font-size: 12px;
  color: #64748b;
  font-family: monospace;
}
.mock-page {
  padding: 0;
  min-height: 320px;
  font-size: 13px;
  color: #cbd5e1;
}

/* ===== 模拟页面通用样式 ===== */
.mp { display: flex; min-height: 320px; }
.mp-sidebar { width: 180px; background: #1e293b; padding: 12px 0; border-right: 1px solid rgba(255,255,255,0.06); flex-shrink: 0; }
.mp-sidebar-item { padding: 8px 16px; font-size: 13px; color: #94a3b8; cursor: default; }
.mp-sidebar-item.active { color: #60a5fa; background: rgba(59,130,246,0.08); border-right: 2px solid #3b82f6; }
.mp-main { flex: 1; padding: 16px; }
.mp-status-bar { display: flex; gap: 8px; flex-wrap: wrap; margin-bottom: 16px; }
.mp-chip { padding: 3px 10px; border-radius: 12px; font-size: 11px; background: rgba(255,255,255,0.06); color: #94a3b8; }
.mp-chip--ok { color: #22c55e; }
.mp-chip--active { background: rgba(59,130,246,0.12); color: #60a5fa; }
.mp-chip-group { display: flex; gap: 4px; }
.mp-cards-row { display: grid; grid-template-columns: repeat(4, 1fr); gap: 12px; margin-bottom: 16px; }
.mp-kpi { background: #1e293b; border-radius: 8px; padding: 14px; text-align: center; }
.mp-kpi-val { font-size: 22px; font-weight: 700; color: #f1f5f9; }
.mp-kpi-label { font-size: 11px; color: #64748b; margin-top: 4px; }
.mp-chart-placeholder { display: flex; align-items: flex-end; gap: 8px; height: 100px; padding: 12px; background: #1e293b; border-radius: 8px; }
.mp-chart-bar { flex: 1; background: linear-gradient(to top, #3b82f6, #60a5fa); border-radius: 4px 4px 0 0; min-width: 20px; transition: height 0.5s; }
.mp-toolbar { display: flex; align-items: center; gap: 12px; margin-bottom: 12px; flex-wrap: wrap; }
.mp-toolbar-title { font-weight: 600; font-size: 15px; }
.mp-ai-btn { padding: 4px 12px; border-radius: 6px; background: linear-gradient(135deg, #7c3aed, #a855f7); color: #fff; font-size: 12px; font-weight: 600; }
.mp-table { background: #1e293b; border-radius: 8px; overflow: hidden; }
.mp-table-head { display: flex; padding: 8px 12px; background: #0f172a; font-size: 11px; color: #64748b; font-weight: 600; }
.mp-table-row { display: flex; padding: 8px 12px; border-bottom: 1px solid rgba(255,255,255,0.04); font-size: 12px; align-items: center; }
.mp-th, .mp-td { flex: 1; }
.mp-th:first-child, .mp-td:first-child { flex: 0 0 40px; }
.mp-muted { color: #64748b; }
.mp-pill { display: inline-block; padding: 1px 8px; border-radius: 10px; font-size: 11px; font-weight: 600; }
.mp-pill--red { background: rgba(239,68,68,0.15); color: #f87171; }
.mp-pill--yellow { background: rgba(234,179,8,0.15); color: #fbbf24; }
.mp-pill--blue { background: rgba(59,130,246,0.15); color: #60a5fa; }
.mp-pill--green { background: rgba(34,197,94,0.15); color: #4ade80; }
.mp-pill--purple { background: rgba(168,85,247,0.15); color: #c084fc; }
.mp-pill--gray { background: rgba(255,255,255,0.06); color: #94a3b8; }
.mp-btn-mp { padding: 5px 14px; border-radius: 6px; background: #3b82f6; color: #fff; font-size: 12px; font-weight: 600; cursor: default; }
.mp-btn-mp-sm { padding: 3px 10px; border-radius: 4px; background: rgba(59,130,246,0.15); color: #60a5fa; font-size: 11px; cursor: default; }
.mp-quote-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.mp-quote-title { font-size: 18px; font-weight: 700; }
.mp-quote-cards { display: grid; grid-template-columns: repeat(2, 1fr); gap: 12px; margin-bottom: 16px; }
.mp-quote-card { background: #1e293b; border-radius: 8px; padding: 14px; }
.mp-quote-card-id { font-size: 11px; color: #64748b; }
.mp-quote-card-name { font-size: 14px; font-weight: 600; margin: 4px 0; }
.mp-quote-card-meta { display: flex; gap: 8px; margin-bottom: 8px; }
.mp-quote-card-price { font-size: 18px; font-weight: 700; color: #22c55e; }
.mp-agent-box { background: #1e293b; border-radius: 8px; padding: 14px; display: flex; align-items: center; gap: 12px; }
.mp-agent-badge { font-size: 12px; color: #a855f7; font-weight: 600; }
.mp-agent-url { flex: 1; font-size: 11px; color: #64748b; font-family: monospace; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.mp-ai-hero { text-align: center; padding: 24px 0 20px; }
.mp-ai-hero h3 { font-size: 20px; font-weight: 700; margin: 0 0 6px; }
.mp-ai-hero p { color: #94a3b8; font-size: 13px; margin: 0; }
.mp-ai-steps { display: flex; justify-content: center; gap: 24px; margin-bottom: 24px; }
.mp-ai-step { display: flex; align-items: center; gap: 8px; font-size: 13px; color: #94a3b8; }
.mp-ai-step-num { width: 24px; height: 24px; border-radius: 50%; background: #3b82f6; color: #fff; display: flex; align-items: center; justify-content: center; font-size: 12px; font-weight: 700; }
.mp-ai-providers { display: grid; grid-template-columns: repeat(2, 1fr); gap: 12px; }
.mp-ai-provider { background: #1e293b; border-radius: 8px; padding: 16px; display: flex; align-items: center; gap: 12px; }
.mp-ai-provider-icon { font-size: 24px; }
.mp-ai-provider-name { flex: 1; font-weight: 600; font-size: 14px; }
.mp-proto-header { margin-bottom: 16px; }
.mp-proto-header h3 { font-size: 18px; font-weight: 700; margin: 0 0 4px; }
.mp-proto-header p { color: #94a3b8; font-size: 13px; margin: 0 0 12px; }
.mp-proto-list { display: flex; flex-direction: column; gap: 8px; }
.mp-proto-item { background: #1e293b; border-radius: 8px; padding: 14px; display: flex; align-items: center; gap: 12px; }
.mp-proto-item-icon { font-size: 24px; }
.mp-proto-item-info { flex: 1; }
.mp-proto-item-name { font-weight: 600; font-size: 14px; }
.mp-proto-item-meta { font-size: 11px; color: #64748b; }
.mp-team-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.mp-team-header h3 { font-size: 18px; font-weight: 700; margin: 0; }
.mp-team-list { display: flex; flex-direction: column; gap: 8px; }
.mp-team-member { background: #1e293b; border-radius: 8px; padding: 12px; display: flex; align-items: center; gap: 12px; }
.mp-avatar { width: 36px; height: 36px; border-radius: 50%; display: flex; align-items: center; justify-content: center; color: #fff; font-size: 14px; font-weight: 700; }
.mp-team-info { flex: 1; }
.mp-team-name { font-weight: 600; font-size: 14px; }
.mp-team-role { font-size: 12px; color: #64748b; }
.mp-nexus-cards { display: grid; grid-template-columns: repeat(3, 1fr); gap: 12px; margin-bottom: 16px; }
.mp-nexus-card { background: #1e293b; border-radius: 8px; padding: 16px; text-align: center; }
.mp-nexus-card-icon { font-size: 24px; margin-bottom: 6px; }
.mp-nexus-card-val { font-size: 22px; font-weight: 700; color: #f1f5f9; }
.mp-nexus-card-label { font-size: 12px; color: #64748b; }
.mp-cloud-hero { text-align: center; padding: 20px 0 16px; }
.mp-cloud-hero h3 { font-size: 18px; font-weight: 700; margin: 0 0 4px; }
.mp-cloud-hero p { color: #94a3b8; font-size: 13px; margin: 0; }
.mp-cloud-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 12px; }
.mp-cloud-card { background: #1e293b; border-radius: 8px; padding: 16px; text-align: center; }
.mp-cloud-icon { font-size: 28px; margin-bottom: 8px; }
.mp-cloud-name { font-weight: 600; font-size: 14px; margin-bottom: 4px; }
.mp-cloud-desc { font-size: 12px; color: #64748b; margin-bottom: 10px; }
.mp-client-hero { text-align: center; margin-bottom: 16px; }
.mp-client-badge { display: inline-block; padding: 4px 12px; border-radius: 12px; background: rgba(59,130,246,0.12); color: #60a5fa; font-size: 12px; margin-bottom: 8px; }
.mp-client-name { font-size: 18px; font-weight: 700; }
.mp-kpi-row { display: grid; grid-template-columns: repeat(4, 1fr); gap: 12px; margin-bottom: 16px; }
.mp-client-chart { display: flex; align-items: flex-end; gap: 8px; height: 80px; padding: 12px; background: #1e293b; border-radius: 8px; }
.mp-agent-header { display: flex; align-items: center; gap: 10px; padding: 10px 14px; background: #1e293b; border-bottom: 1px solid rgba(255,255,255,0.06); }
.mp-agent-logo { font-size: 20px; }
.mp-agent-title { font-weight: 700; font-size: 14px; }
.mp-agent-tenant { margin-left: auto; font-size: 12px; color: #64748b; }
.mp-agent-chat { padding: 16px; display: flex; flex-direction: column; gap: 12px; min-height: 200px; }
.mp-msg { display: flex; gap: 8px; }
.mp-msg--bot { flex-direction: row; }
.mp-msg--user { flex-direction: row-reverse; }
.mp-msg-avatar { width: 28px; height: 28px; border-radius: 6px; background: #1e293b; display: flex; align-items: center; justify-content: center; font-size: 14px; flex-shrink: 0; }
.mp-msg-bubble { max-width: 75%; padding: 10px 14px; border-radius: 10px; font-size: 13px; line-height: 1.6; }
.mp-msg--bot .mp-msg-bubble { background: #1e293b; color: #e2e8f0; border-top-left-radius: 2px; }
.mp-msg--user .mp-msg-bubble { background: #3b82f6; color: #fff; border-top-right-radius: 2px; }
.mp-agent-input { display: flex; align-items: center; gap: 8px; padding: 10px; background: #1e293b; border-radius: 0 0 0 0; border-top: 1px solid rgba(255,255,255,0.06); }
.mp-agent-attach { font-size: 18px; cursor: default; }
.mp-agent-input-box { flex: 1; padding: 8px 12px; background: #0f172a; border-radius: 8px; font-size: 13px; color: #64748b; }
.mp-agent-send { padding: 6px 14px; border-radius: 8px; background: #3b82f6; color: #fff; font-size: 12px; font-weight: 600; cursor: default; }

/* ===== Section 标题 ===== */
.detail-section-title {
  text-align: center;
  font-size: 28px;
  font-weight: 700;
  margin-bottom: 36px;
  color: #f1f5f9;
}

/* ===== 功能亮点 ===== */
.detail-features {
  max-width: 1100px;
  margin: 0 auto 80px;
  padding: 0 24px;
}
.detail-features-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
  gap: 20px;
}
.detail-highlight-card {
  background: rgba(255, 255, 255, 0.03);
  border: 1px solid rgba(255, 255, 255, 0.06);
  border-radius: 12px;
  padding: 24px;
  transition: transform 0.25s, box-shadow 0.25s, border-color 0.25s;
  animation: fadeUp 0.6s ease both;
}
.detail-highlight-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 12px 40px rgba(0, 0, 0, 0.2);
  border-color: rgba(59, 130, 246, 0.2);
}
.detail-highlight-icon { font-size: 28px; margin-bottom: 12px; }
.detail-highlight-card h3 { font-size: 16px; font-weight: 700; margin: 0 0 8px; color: #f1f5f9; }
.detail-highlight-card p { font-size: 14px; color: #94a3b8; line-height: 1.6; margin: 0; }

/* ===== 指标 ===== */
.detail-metrics {
  max-width: 900px;
  margin: 0 auto 80px;
  padding: 0 24px;
}
.detail-metrics-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(160px, 1fr));
  gap: 16px;
}
.detail-metric-card {
  text-align: center;
  padding: 28px 16px;
  background: rgba(255, 255, 255, 0.03);
  border: 1px solid rgba(255, 255, 255, 0.06);
  border-radius: 12px;
  transition: transform 0.2s;
}
.detail-metric-card:hover { transform: translateY(-2px); }
.detail-metric-value {
  font-size: 32px;
  font-weight: 800;
  background: linear-gradient(135deg, #60a5fa, #a78bfa);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}
.detail-metric-label { font-size: 14px; color: #64748b; margin-top: 6px; }

/* ===== 步骤 ===== */
.detail-steps {
  max-width: 800px;
  margin: 0 auto 80px;
  padding: 0 24px;
}
.detail-steps-track {
  position: relative;
  padding-left: 40px;
}
.detail-steps-track::before {
  content: '';
  position: absolute;
  left: 15px;
  top: 0;
  bottom: 0;
  width: 2px;
  background: linear-gradient(to bottom, #3b82f6, rgba(59, 130, 246, 0.1));
}
.detail-step {
  position: relative;
  padding: 0 0 32px;
}
.detail-step-num {
  position: absolute;
  left: -40px;
  top: 0;
  width: 30px;
  height: 30px;
  border-radius: 50%;
  background: linear-gradient(135deg, #3b82f6, #8b5cf6);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  font-weight: 700;
  color: #fff;
  box-shadow: 0 4px 12px rgba(59, 130, 246, 0.3);
}
.detail-step-content h3 { font-size: 16px; font-weight: 700; margin: 0 0 6px; color: #f1f5f9; }
.detail-step-content p { font-size: 14px; color: #94a3b8; line-height: 1.6; margin: 0; }

/* ===== 底部 CTA ===== */
.detail-bottom-cta {
  text-align: center;
  padding: 80px 24px;
  max-width: 600px;
  margin: 0 auto;
}
.detail-bottom-cta h2 { font-size: 32px; font-weight: 800; color: #f1f5f9; margin: 0 0 12px; }
.detail-bottom-cta p { font-size: 16px; color: #94a3b8; margin: 0 0 28px; line-height: 1.6; }
.detail-bottom-actions { display: flex; justify-content: center; gap: 12px; flex-wrap: wrap; }
.detail-btn {
  padding: 12px 28px;
  border-radius: 10px;
  font-size: 15px;
  font-weight: 600;
  text-decoration: none;
  transition: transform 0.15s, box-shadow 0.15s;
}
.detail-btn:hover { transform: translateY(-2px); }
.detail-btn--primary { background: #3b82f6; color: #fff; box-shadow: 0 4px 20px rgba(59, 130, 246, 0.3); }
.detail-btn--outline { background: transparent; color: #94a3b8; border: 1px solid rgba(255, 255, 255, 0.15); }
.detail-btn--outline:hover { border-color: rgba(255, 255, 255, 0.3); color: #e2e8f0; }

/* ===== Footer ===== */
.detail-footer {
  text-align: center;
  padding: 32px 24px;
  color: #475569;
  font-size: 13px;
  border-top: 1px solid rgba(255, 255, 255, 0.04);
}

/* ===== 动画 ===== */
@keyframes fadeUp {
  from { opacity: 0; transform: translateY(20px); }
  to { opacity: 1; transform: translateY(0); }
}

/* ===== 响应式 ===== */
@media (max-width: 768px) {
  .detail-hero h1 { font-size: 28px; }
  .detail-hero-desc { font-size: 15px; }
  .detail-features-grid { grid-template-columns: 1fr; }
  .detail-metrics-grid { grid-template-columns: repeat(2, 1fr); }
  .mp-sidebar { display: none; }
  .mp-cards-row { grid-template-columns: repeat(2, 1fr); }
  .mp-nexus-cards { grid-template-columns: 1fr; }
  .mp-cloud-grid { grid-template-columns: 1fr; }
  .mp-kpi-row { grid-template-columns: repeat(2, 1fr); }
  .mp-quote-cards { grid-template-columns: 1fr; }
  .mp-ai-providers { grid-template-columns: 1fr; }
}
</style>
