<template>
  <div class="dashboard-page">
    <div class="page-header">
      <h2 class="page-title">平台监测看板</h2>
    </div>

    <el-skeleton :loading="loading" animated :count="6" v-if="loading" />

    <template v-else>
      <!-- 概览卡片 -->
      <el-row :gutter="16" class="stat-cards">
        <el-col :xs="12" :sm="8" :md="6" :lg="4">
          <el-card shadow="hover" class="stat-card">
            <div class="stat-label">DAU（今日）</div>
            <div class="stat-value">{{ overview && overview.dauToday != null ? overview.dauToday : '—' }}</div>
          </el-card>
        </el-col>
        <el-col :xs="12" :sm="8" :md="6" :lg="4">
          <el-card shadow="hover" class="stat-card">
            <div class="stat-label">MAU（月活）</div>
            <div class="stat-value">{{ overview && overview.mauMonth != null ? overview.mauMonth : '—' }}</div>
          </el-card>
        </el-col>
        <el-col :xs="12" :sm="8" :md="6" :lg="4">
          <el-card shadow="hover" class="stat-card">
            <div class="stat-label">总用户数量</div>
            <div class="stat-value">{{ overview && overview.totalUsers != null ? overview.totalUsers : '—' }}</div>
          </el-card>
        </el-col>
        <el-col :xs="12" :sm="8" :md="6" :lg="4">
          <el-card shadow="hover" class="stat-card">
            <div class="stat-label">APIKey 配置占比</div>
            <div class="stat-value">
              {{ overview && overview.apiKeyConfiguredTenants != null ? overview.apiKeyConfiguredTenants : '—' }}
              <span class="stat-divider">/</span>
              {{ overview && overview.totalTenants != null ? overview.totalTenants : '—' }}
            </div>
            <div class="stat-sub">{{ formatRatio(overview && overview.apiKeyConfiguredRatioPercent) }}</div>
          </el-card>
        </el-col>
        <el-col :xs="12" :sm="8" :md="6" :lg="4">
          <el-card shadow="hover" class="stat-card">
            <div class="stat-label">GitHubToken 配置占比</div>
            <div class="stat-value">
              {{ overview && overview.githubTokenConfiguredTenants != null ? overview.githubTokenConfiguredTenants : '—' }}
              <span class="stat-divider">/</span>
              {{ overview && overview.totalTenants != null ? overview.totalTenants : '—' }}
            </div>
            <div class="stat-sub">{{ formatRatio(overview && overview.githubTokenConfiguredRatioPercent) }}</div>
          </el-card>
        </el-col>
        <el-col :xs="12" :sm="8" :md="6" :lg="4">
          <el-card shadow="hover" class="stat-card">
            <div class="stat-label">MRR 近似（近30天）</div>
            <div class="stat-value">{{ formatMoney(overview && overview.mrrApproxCents) }}</div>
            <div class="stat-sub">有效订阅：{{ overview && overview.activePaidSubscriptions != null ? overview.activePaidSubscriptions : '—' }}</div>
          </el-card>
        </el-col>
        <el-col :xs="12" :sm="8" :md="6" :lg="4">
          <el-card shadow="hover" class="stat-card">
            <div class="stat-label">已暂停租户</div>
            <div class="stat-value">{{ overview && overview.suspendedTenants != null ? overview.suspendedTenants : '—' }}</div>
          </el-card>
        </el-col>
      </el-row>

      <!-- DAU 折线图 -->
      <el-card shadow="hover" class="section-card">
        <div slot="header" class="section-header">
          <span>DAU 变化折线图</span>
        </div>
        <div v-if="dauTrend.length">
          <LineChart :labels="dauTrend.map(x => x.date)" :values="dauTrend.map(x => x.count)" />
        </div>
        <el-empty v-else description="暂无趋势数据" :image-size="80" />
      </el-card>

      <!-- 账号活跃排行 -->
      <el-card shadow="hover" class="section-card">
        <div slot="header" class="section-header">
          <span>账号活跃排行（今日提交数）</span>
        </div>
        <el-table
          v-if="activeAuthors.length"
          :data="activeAuthors"
          stripe
          size="small"
          style="width: 100%"
        >
          <el-table-column prop="authorEmail" label="邮箱" min-width="180">
            <template slot-scope="{ row }">
              {{ row.authorEmail || '—' }}
            </template>
          </el-table-column>
          <el-table-column prop="authorName" label="名称" min-width="120">
            <template slot-scope="{ row }">
              {{ row.authorName || '—' }}
            </template>
          </el-table-column>
          <el-table-column prop="commitCount" label="提交数" width="100" align="center" />
          <el-table-column prop="lastCommittedAt" label="最后提交" min-width="160">
            <template slot-scope="{ row }">
              <span class="mono-text">{{ row.lastCommittedAt || '—' }}</span>
            </template>
          </el-table-column>
        </el-table>
        <el-empty v-else description="暂无排行数据" :image-size="80" />
      </el-card>

      <!-- 组件使用统计 -->
      <el-card shadow="hover" class="section-card">
        <div slot="header" class="section-header">
          <span>用户组件点击 / 功能使用统计</span>
          <el-tag size="small" type="info" effect="plain">组件名称与主应用首页「能力枢纽」卡片及控制台文案对齐</el-tag>
        </div>
        <el-table
          v-if="componentUsage && componentUsage.components && componentUsage.components.length"
          :data="componentUsage.components"
          stripe
          size="small"
          style="width: 100%"
        >
          <el-table-column label="组件（首页 / 控制台）" min-width="200">
            <template slot-scope="{ row }">
              <div class="comp-cell">
                <span class="comp-label">{{ componentLabel(row.componentKey) }}</span>
                <el-tag size="mini" type="info" effect="plain">{{ row.componentKey }}</el-tag>
              </div>
            </template>
          </el-table-column>
          <el-table-column prop="clickCount" label="点击数" width="90" align="center" />
          <el-table-column prop="usageCount" label="核心接口调用数" width="130" align="center" />
          <el-table-column label="核心接口明细" min-width="300">
            <template slot-scope="{ row }">
              <div v-if="row.coreApis && row.coreApis.length" class="core-apis">
                <div v-for="api in row.coreApis" :key="api.coreApiKey" class="core-api-item">
                  <span class="core-api-label">{{ coreApiLabel(api.coreApiKey) }}</span>
                  <el-tag size="mini" type="info" effect="plain">{{ api.coreApiKey }}</el-tag>
                  <span class="core-api-stat">click={{ api.clickCount }}</span>
                  <span class="core-api-stat">usage={{ api.usageCount }}</span>
                </div>
              </div>
              <span v-else class="muted-text">—</span>
            </template>
          </el-table-column>
        </el-table>
        <el-empty v-else description="暂无组件事件数据（需要先在主应用触发组件操作）" :image-size="80" />
      </el-card>

      <!-- 功能点击热度排行 -->
      <el-card shadow="hover" class="section-card">
        <div slot="header" class="section-header">
          <span>功能点击热度排行（跨组件 Top 50）</span>
          <el-tag size="small" type="success" effect="plain">按点击量降序，统计独立用户数与租户数</el-tag>
        </div>
        <el-table
          v-if="heatRank && heatRank.items && heatRank.items.length"
          :data="heatRank.items"
          stripe
          size="small"
          style="width: 100%"
          :default-sort="{ prop: 'clickCount', order: 'descending' }"
        >
          <el-table-column prop="rank" label="排名" width="60" align="center" />
          <el-table-column label="功能名称" min-width="260">
            <template slot-scope="{ row }">
              <div class="comp-cell">
                <span class="comp-label">{{ coreApiLabel(row.coreApiKey) }}</span>
                <el-tag size="mini" type="info" effect="plain">{{ row.coreApiKey }}</el-tag>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="所属模块" width="120" align="center">
            <template slot-scope="{ row }">
              <el-tag size="mini" :type="row.componentKey === 'collab' ? 'success' : ''" effect="plain">{{ componentLabel(row.componentKey) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="clickCount" label="点击数" width="90" align="center" sortable />
          <el-table-column prop="usageCount" label="使用数" width="90" align="center" sortable />
          <el-table-column prop="userCount" label="独立用户数" width="100" align="center" sortable />
          <el-table-column prop="tenantCount" label="涉及租户数" width="100" align="center" sortable />
        </el-table>
        <el-empty v-else description="暂无热度数据（需要先在主应用触发功能操作）" :image-size="80" />
      </el-card>
    </template>
  </div>
</template>

<script>
import { http } from '../api/http'
import LineChart from '../components/LineChart.vue'

const COMPONENT_LABELS = {
  hub_quote: '报价系统',
  hub_prototype: '快原型',
  ai_commit_analysis: 'AI 分析（单次提交）',
  quote: '报价系统',
  ui_prototype: '快原型',
  ai_analysis: 'AI 分析（单次提交）',
  collab: '项目协作'
}

const CORE_API_LABELS = {
  quote_provision: '报价项目创建（Provision）',
  quote_calculate: '报价计算（Calculate）',
  ui_prototype_generate_spec: '生成规格（Spec）',
  ui_prototype_generate_mockup: '生成页面原型（Mockup）',
  ai_analysis_run: '运行单次提交 AI 分析',
  // 项目协作 - 记录操作
  'collab.record.open_add_modal': '新建记录（打开弹窗）',
  'collab.record.create': '新建记录（确认创建）',
  'collab.record.open_detail': '打开记录详情',
  'collab.record.save': '保存记录修改',
  'collab.record.delete': '删除记录',
  'collab.record.batch_delete': '批量删除记录',
  'collab.record.submit_comment': '提交评论',
  // 项目协作 - 表格操作
  'collab.table.toggle_dashboard': '切换仪表盘/表格视图',
  'collab.filter.open_modal': '打开筛选弹窗',
  'collab.filter.apply': '应用筛选',
  // 项目协作 - AI 功能
  'collab.ai.open_input': '打开 AI 录入',
  'collab.ai.generate_drafts': 'AI 生成任务草稿',
  'collab.ai.commit_tasks': 'AI 任务提交到表格',
  'collab.csv.ai_parse': 'CSV AI 解析',
  'collab.csv.quick_analyze': 'CSV 快速分析',
  'collab.csv.quick_commit': 'CSV 快速导入提交',
  // 项目协作 - 首页配置
  'collab.home.save_mail_config': '保存邮件通知配置',
  'collab.home.send_test_mail': '发送测试邮件',
  'collab.home.save_ai_linkage': '保存 AI 联动配置',
  'collab.home.save_client_board': '保存客户看板配置',
  'collab.home.copy_client_link': '复制客户访问链接',
  'collab.home.open_board_tab': '打开客户看板',
  'collab.home.regenerate_token': '重新生成看板令牌',
  // 项目协作 - 传送门
  'collab.portal.open_modal': '打开传送门配置',
  'collab.portal.save': '保存传送门链接',
  'collab.portal.import': '导入传送门链接',
  // 客户看板
  'client_board.ai.generate_draft': '客户 AI 生成草稿',
  'client_board.ai.commit': '客户 AI 提交任务',
  'client_board.table.switch_purpose': '切换表格用途',
  'client_board.daily.open_detail': '打开日报详情'
}

export default {
  name: 'DashboardPage',
  components: { LineChart },
  data () {
    return {
      loading: false,
      error: '',
      overview: null,
      dauTrend: [],
      activeAuthors: [],
      componentUsage: { components: [] },
      heatRank: { items: [] }
    }
  },
  async mounted () {
    await this.load()
  },
  methods: {
    componentLabel (key) {
      if (!key) return '—'
      return COMPONENT_LABELS[key] || key
    },
    coreApiLabel (key) {
      if (!key) return '—'
      return CORE_API_LABELS[key] || key
    },
    formatRatio (v) {
      if (v == null || Number.isNaN(Number(v))) return '—'
      const n = Number(v)
      return `${n.toFixed(2)}%`
    },
    formatMoney (cents) {
      if (cents == null || Number.isNaN(Number(cents))) return '—'
      const n = Number(cents)
      return `¥${(n / 100).toFixed(2)}`
    },
    async load () {
      this.loading = true
      this.error = ''
      try {
        const { data: o } = await http.get('/platform/ops/metrics/overview')
        if (o && o.code === 0) this.overview = o.data || {}
      } catch (e) {
        this.error = '加载概览失败'
        this.loading = false
        return
      }

      try {
        const { data: t } = await http.get('/platform/ops/metrics/dau-trend', { params: { days: 30 } })
        if (t && t.code === 0) this.dauTrend = Array.isArray(t.data) ? t.data : []
      } catch (e) {
        this.dauTrend = []
      }

      try {
        const { data: r } = await http.get('/platform/ops/metrics/active-authors', { params: { limit: 20 } })
        if (r && r.code === 0) this.activeAuthors = Array.isArray(r.data) ? r.data : []
      } catch (e) {
        this.activeAuthors = []
      }

      try {
        const { data: c } = await http.get('/platform/ops/metrics/component-usage', { params: { days: 30 } })
        if (c && c.code === 0) this.componentUsage = c.data || { components: [] }
      } catch (e) {
        this.componentUsage = { components: [] }
      }

      try {
        const { data: h } = await http.get('/platform/ops/metrics/heat-rank', { params: { days: 30, limit: 50 } })
        if (h && h.code === 0) this.heatRank = h.data || { items: [] }
      } catch (e) {
        this.heatRank = { items: [] }
      } finally {
        this.loading = false
      }
    }
  }
}
</script>

<style scoped>
.dashboard-page {
  max-width: 1400px;
  margin: 0 auto;
}
.page-header {
  margin-bottom: 16px;
}
.page-title {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
  color: #303133;
}
.stat-cards {
  margin-bottom: 16px;
}
.stat-card {
  margin-bottom: 16px;
}
.stat-card .el-card__body {
  padding: 16px;
}
.stat-label {
  font-size: 12px;
  color: #909399;
  margin-bottom: 8px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.stat-value {
  font-size: 22px;
  font-weight: 700;
  color: #303133;
  line-height: 1.2;
}
.stat-divider {
  color: #dcdfe6;
  margin: 0 2px;
  font-weight: 300;
}
.stat-sub {
  margin-top: 6px;
  font-size: 12px;
  color: #409eff;
}
.section-card {
  margin-bottom: 16px;
}
.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 8px;
  font-size: 15px;
  font-weight: 600;
}
.comp-cell {
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.comp-label {
  font-weight: 600;
  font-size: 13px;
}
.core-apis {
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.core-api-item {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  font-size: 12px;
}
.core-api-label {
  font-weight: 500;
}
.core-api-stat {
  color: #909399;
  font-size: 11px;
}
.mono-text {
  font-family: 'SF Mono', 'Fira Code', 'Consolas', monospace;
  font-size: 12px;
}
.muted-text {
  color: #c0c4cc;
}
</style>
