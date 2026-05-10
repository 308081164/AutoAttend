/**
 * 静态 Demo 写死数据 — 迁移 Vue 时可替换为 API / props
 * @see ../docs/项目协作UI优化方案.md
 * @see ../auto_attend_forntend/src/views/CollabTableView.vue
 */
window.COLLAB_DEMO = {
  project: {
    name: 'AutoAttend 协作看板',
    repoFullName: 'github.com/guangsha/AutoAttend',
    branch: 'main',
    phase: '迭代 · Sprint 12',
    owner: '张敏（PM）',
    createdAt: '2025-11-08',
    envLabel: '生产稳定',
    intro:
      '面向企业的考勤与协作一体化产品：支持多维表驱动的问题跟踪、客户阅览看板与控制台指标对齐，本仓库为前后端单体协作示例。',
    developers: ['张敏（PM）', '李工（后端）', '赵前端', '王采购（业务）'],
    techStack: ['Vue 3', 'Spring Boot', 'MySQL', 'Redis', 'Vite']
  },
  /** 与轮播「风险与阻塞」共用数据源（原 L1 右侧列表迁入） */
  risks: [
    { level: 'high', text: '短信配额审批阻塞联调窗口' },
    { level: 'med', text: '预发回归清单与业务口径待对齐' },
    { level: 'low', text: '客户阅览看板 HTTPS 复制提示待优化' }
  ],
  portalLinks: [
    { id: 1, label: '需求文档', url: 'https://example.com/wiki', sortOrder: 1 },
    { id: 2, label: '代码仓库', url: 'https://github.com/guangsha/AutoAttend', sortOrder: 2 },
    { id: 3, label: 'CI 流水线', url: 'https://github.com/guangsha/AutoAttend/actions', sortOrder: 3 },
    { id: 4, label: 'Figma 设计', url: 'https://figma.com/file/mock', sortOrder: 4 },
    { id: 5, label: '控制台看板', url: '/admin/repos', sortOrder: 5 }
  ],
  mailNotify: {
    smtpHint: '当前 SMTP 已在监测台配置（Demo 文案）。',
    repoFullName: 'guangsha/AutoAttend',
    enabled: true,
    sendToManagers: true,
    sendToDevelopers: true,
    managerEmailsText: 'pm@example.com\ndelivery@example.com'
  },
  aiLinkage: {
    automationMode: 'auto_status',
    minConfidence: 'medium'
  },
  clientBoard: {
    enabled: true,
    showProgressDashboard: true,
    showFeatureBacklog: true,
    showAiTableEntry: false,
    aiPurpose: 'issue_tracking',
    publicUrl: 'https://app.example.com/public/client-board/demo-token-8f3a2b',
    token: 'demo-token-8f3a2b'
  },
  issueRows: [
    { id: 101, problem: '里程碑 M2 验收：多维表筛选与附件缓存', module: '项目协作', status: '测试中', accept: '待验收', important: '阶段性优先解决', owner: '张敏 / 李工' },
    { id: 102, problem: '预发回归清单对齐', module: '质量', status: '开发中', accept: '未验收', important: '严重紧急', owner: '李工' },
    { id: 103, problem: '短信配额审批阻塞联调', module: '运维', status: '修复中', accept: '未验收', important: '严重紧急', owner: '王采购' },
    { id: 104, problem: '客户阅览看板 HTTPS 复制提示', module: '前端', status: '已创建', accept: '未验收', important: '下一阶段待办', owner: '赵前端' }
  ],
  featureRows: [
    { id: 201, name: '顶栏 Tab 替代侧栏导航', desc: '与数据看板/双表切换一致', module: '协作 UI', progress: '开发中', important: '严重紧急', owner: '赵前端' },
    { id: 202, name: '协作配置折叠收纳', desc: '邮件 / AI / 客户看板', module: '协作 UI', progress: '待开发', important: '阶段性优先解决', owner: '张敏' },
    { id: 203, name: '控制台数据看板嵌入', desc: 'DashboardView compact 模式', module: '数据', progress: '联调中', important: '阶段性优先解决', owner: '李工' },
    { id: 204, name: '传送门排序与空态', desc: 'portal CRUD', module: '协作', progress: '已完成', important: '等待排期', owner: '张敏' }
  ],
  embedDashboardTitles: [
    '任务完成情况',
    '本周新建任务',
    '本周解决任务',
    '重要程度分布',
    '问题描述词云',
    '平均解决耗时'
  ],
  dailySummaries: [
    { date: '2026-05-10', title: '联调窗口开启', summary: '预发部署完成，等待业务验收清单。' },
    { date: '2026-05-09', title: '聚合 KPI 对齐', summary: '与控制台指标口径统一，文档已更新。' },
    { date: '2026-05-08', title: 'PR 合并节奏正常', summary: '4 个 PR 合并，无阻塞缺陷。' },
    { date: '2026-05-07', title: '考勤边界修复', summary: '跨日班次统计与前端展示一致。' },
    { date: '2026-05-06', title: '评审通过 UI 方案', summary: '看板信息分层与 Tab 导航定稿。' },
    { date: '2026-05-05', title: '协作 JWT 续期', summary: '成员会话与管理员切换策略评审。' }
  ],
  kpis: [
    { label: '本周提交数', value: '47' },
    { label: '活跃成员', value: '8' },
    { label: '未关闭任务', value: '23' },
    { label: '本周代码行数', value: '+3.2k' }
  ]
}
