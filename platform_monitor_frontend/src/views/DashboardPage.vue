<template>
  <div class="wrap">
    <h2 class="title">平台监测看板</h2>

    <p v-if="loading" class="muted">加载中…</p>
    <p v-else-if="error" class="err">{{ error }}</p>

    <template v-else>
      <section class="cards">
        <div class="card">
          <div class="card-label">DAU（今日）</div>
          <div class="card-value">{{ overview && overview.dauToday != null ? overview.dauToday : '—' }}</div>
        </div>
        <div class="card">
          <div class="card-label">MAU（月活）</div>
          <div class="card-value">{{ overview && overview.mauMonth != null ? overview.mauMonth : '—' }}</div>
        </div>
        <div class="card">
          <div class="card-label">总用户数量</div>
          <div class="card-value">{{ overview && overview.totalUsers != null ? overview.totalUsers : '—' }}</div>
        </div>
        <div class="card">
          <div class="card-label">APIKey配置占比</div>
          <div class="card-value">
            {{ overview && overview.apiKeyConfiguredTenants != null ? overview.apiKeyConfiguredTenants : '—' }} /
            {{ overview && overview.totalTenants != null ? overview.totalTenants : '—' }}
          </div>
          <div class="card-sub">{{ formatRatio(overview && overview.apiKeyConfiguredRatioPercent != null ? overview.apiKeyConfiguredRatioPercent : null) }}</div>
        </div>
        <div class="card">
          <div class="card-label">GitHubToken配置占比</div>
          <div class="card-value">
            {{ overview && overview.githubTokenConfiguredTenants != null ? overview.githubTokenConfiguredTenants : '—' }} /
            {{ overview && overview.totalTenants != null ? overview.totalTenants : '—' }}
          </div>
          <div class="card-sub">{{ formatRatio(overview && overview.githubTokenConfiguredRatioPercent != null ? overview.githubTokenConfiguredRatioPercent : null) }}</div>
        </div>
      </section>

      <section class="section">
        <h3 class="section-title">DAU变化折线图</h3>
        <div v-if="dauTrend.length">
          <LineChart :labels="dauTrend.map(x => x.date)" :values="dauTrend.map(x => x.count)" />
        </div>
        <p v-else class="muted">暂无趋势数据</p>
      </section>

      <section class="section">
        <h3 class="section-title">账号活跃排行（今日提交数）</h3>
        <table class="table" v-if="activeAuthors.length">
          <thead>
            <tr>
              <th>邮箱</th>
              <th>名称</th>
              <th>提交数</th>
              <th>最后提交</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="r in activeAuthors" :key="r.authorEmail + '_' + r.lastCommittedAt">
              <td>{{ r.authorEmail || '—' }}</td>
              <td>{{ r.authorName || '—' }}</td>
              <td>{{ r.commitCount }}</td>
              <td class="mono">{{ r.lastCommittedAt || '—' }}</td>
            </tr>
          </tbody>
        </table>
        <p v-else class="muted">暂无排行数据</p>
      </section>

      <section class="section">
        <h3 class="section-title">用户组件点击 / 功能使用统计</h3>
        <p class="muted small-hint">组件名称与主应用首页「能力枢纽」卡片及控制台文案对齐（与 <code>zh.js</code> / 页面标题一致）。</p>
        <table class="table" v-if="componentUsage && componentUsage.components && componentUsage.components.length">
          <thead>
            <tr>
              <th>组件（首页 / 控制台）</th>
              <th>点击数</th>
              <th>核心接口调用数（usage）</th>
              <th>核心接口明细</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="c in componentUsage.components" :key="c.componentKey">
              <td>
                <div class="comp-title">{{ componentLabel(c.componentKey) }}</div>
                <div class="muted mono comp-key">{{ c.componentKey }}</div>
              </td>
              <td>{{ c.clickCount }}</td>
              <td>{{ c.usageCount }}</td>
              <td>
                <div class="core-apis">
                  <div v-for="api in c.coreApis" :key="api.coreApiKey" class="core-api-row">
                    <span>{{ coreApiLabel(api.coreApiKey) }}</span>
                    <span class="muted mono tiny">{{ api.coreApiKey }}</span>
                    <span class="muted">click={{ api.clickCount }}</span>
                    <span class="muted">usage={{ api.usageCount }}</span>
                  </div>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
        <p v-else class="muted">暂无组件事件数据（需要先在主应用触发组件操作）</p>
      </section>
    </template>
  </div>
</template>

<script>
import { http } from '../api/http'
import LineChart from '../components/LineChart.vue'

/** 与主应用 Dashboard 首页 hub 卡片标题、控制台区块标题一致（见 auto_attend_forntend zh.js / DashboardView.vue） */
const COMPONENT_LABELS = {
  hub_quote: '报价系统',
  hub_prototype: '快原型',
  ai_commit_analysis: 'AI 分析（单次提交）',
  // 历史埋点 key，便于旧数据仍可读
  quote: '报价系统',
  ui_prototype: '快原型',
  ai_analysis: 'AI 分析（单次提交）'
}

const CORE_API_LABELS = {
  quote_provision: '报价项目创建（Provision）',
  quote_calculate: '报价计算（Calculate）',
  ui_prototype_generate_spec: '生成规格（Spec）',
  ui_prototype_generate_mockup: '生成页面原型（Mockup）',
  ai_analysis_run: '运行单次提交 AI 分析'
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
      componentUsage: { components: [] }
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
        // 只影响图表展示
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
      } finally {
        this.loading = false
      }
    }
  }
}
</script>

<style scoped>
.wrap {
  max-width: 1200px;
  margin: 0 auto;
}
.title {
  margin: 0 0 16px;
  font-size: 22px;
  font-weight: 700;
}
.muted { color: #94a3b8; }
.small-hint { font-size: 13px; margin: 0 0 10px; }
.comp-title { font-weight: 600; }
.comp-key { font-size: 12px; margin-top: 4px; }
.tiny { font-size: 11px; }
.err { color: #fca5a5; }
.mono { font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, "Liberation Mono", "Courier New", monospace; }

.cards {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 16px;
}
.card {
  background: #1e293b;
  border: 1px solid #334155;
  border-radius: 12px;
  padding: 14px 14px 12px;
}
.card-label {
  color: #94a3b8;
  font-size: 13px;
  margin-bottom: 8px;
}
.card-value {
  font-size: 20px;
  font-weight: 700;
}
.card-sub {
  margin-top: 6px;
  color: #60a5fa;
  font-size: 13px;
}

.section {
  margin-top: 18px;
  padding: 16px;
  background: #0b1224;
  border: 1px solid #1f2a44;
  border-radius: 12px;
}
.section-title {
  margin: 0 0 12px;
  font-size: 16px;
  font-weight: 700;
}

.table {
  width: 100%;
  border-collapse: collapse;
  font-size: 14px;
}
th, td {
  text-align: left;
  padding: 10px 12px;
  border-bottom: 1px solid #334155;
}
th {
  color: #94a3b8;
  font-weight: 500;
}

.core-apis {
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.core-api-row {
  display: flex;
  gap: 8px;
  align-items: center;
  flex-wrap: wrap;
}
</style>

