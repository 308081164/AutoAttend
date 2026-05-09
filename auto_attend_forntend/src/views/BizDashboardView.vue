<template>
  <div class="biz-page">
    <div class="page-head">
      <router-link to="/quote" class="head-back">← 报价列表</router-link>
      <h1>商务数据看板</h1>
      <p class="desc">客户、商机与报价项目汇总（Phase A）</p>
    </div>
    <div v-if="loading" class="placeholder">加载中…</div>
    <div v-else class="biz-cards">
      <div class="biz-card">
        <div class="biz-card__label">客户数</div>
        <div class="biz-card__value">{{ summary.customerCount }}</div>
      </div>
      <div class="biz-card">
        <div class="biz-card__label">商机数</div>
        <div class="biz-card__value">{{ summary.opportunityCount }}</div>
      </div>
      <div class="biz-card">
        <div class="biz-card__label">报价项目</div>
        <div class="biz-card__value">{{ summary.quoteCount }}</div>
      </div>
      <div class="biz-card">
        <div class="biz-card__label">在途商机金额</div>
        <div class="biz-card__value">{{ fmtMoney(summary.totalPipeline) }}</div>
      </div>
      <div class="biz-card">
        <div class="biz-card__label">已赢单金额</div>
        <div class="biz-card__value">{{ fmtMoney(summary.wonAmount) }}</div>
      </div>
    </div>
    <section v-if="!loading && stageRows.length" class="biz-section">
      <h2>商机阶段分布</h2>
      <table class="data-table">
        <thead>
          <tr>
            <th>阶段</th>
            <th>数量</th>
            <th>金额合计</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="(row, i) in stageRows" :key="i">
            <td>{{ stageLabel(row.stage) }}</td>
            <td>{{ row.cnt }}</td>
            <td>{{ fmtMoney(row.totalAmount) }}</td>
          </tr>
        </tbody>
      </table>
    </section>
  </div>
</template>

<script>
const STAGE_LABELS = {
  discovery: '发现',
  qualification: '确认需求',
  proposal: '方案/报价',
  negotiation: '谈判',
  closed_won: '赢单',
  closed_lost: '输单'
}

export default {
  name: 'BizDashboardView',
  data () {
    return {
      loading: true,
      summary: {
        customerCount: 0,
        opportunityCount: 0,
        quoteCount: 0,
        totalPipeline: 0,
        wonAmount: 0,
        stageGroups: []
      }
    }
  },
  computed: {
    stageRows () {
      return this.summary.stageGroups || []
    }
  },
  mounted () {
    this.load()
  },
  methods: {
    stageLabel (s) {
      return STAGE_LABELS[s] || s || '—'
    },
    fmtMoney (n) {
      const v = Number(n)
      if (!Number.isFinite(v)) return '—'
      return '¥' + v.toLocaleString('zh-CN', { minimumFractionDigits: 0, maximumFractionDigits: 2 })
    },
    async load () {
      this.loading = true
      try {
        const resp = await this.$http.get('/admin/biz/dashboard/summary')
        if (resp.data && resp.data.code === 0 && resp.data.data) {
          this.summary = { ...this.summary, ...resp.data.data }
        }
      } catch (e) {
        console.error(e)
      } finally {
        this.loading = false
      }
    }
  }
}
</script>

<style scoped>
.biz-page {
  max-width: 960px;
  margin: 0 auto;
  padding: var(--space-lg, 24px);
}
.page-head h1 {
  margin: var(--space-sm, 8px) 0;
  font-size: var(--font-size-2xl, 1.5rem);
}
.desc {
  color: var(--text-secondary, #666);
  margin: 0 0 var(--space-md, 16px);
}
.head-back {
  font-size: var(--font-size-sm, 14px);
  color: var(--brand-blue, #2563eb);
  text-decoration: none;
}
.biz-cards {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(160px, 1fr));
  gap: var(--space-md, 16px);
  margin-bottom: var(--space-xl, 32px);
}
.biz-card {
  background: var(--surface-card, #fff);
  border: 1px solid var(--border-subtle, #e5e7eb);
  border-radius: var(--radius-md, 8px);
  padding: var(--space-md, 16px);
}
.biz-card__label {
  font-size: var(--font-size-sm, 13px);
  color: var(--text-secondary, #666);
}
.biz-card__value {
  font-size: 1.5rem;
  font-weight: 600;
  margin-top: 4px;
}
.biz-section h2 {
  font-size: 1.1rem;
  margin-bottom: var(--space-sm, 8px);
}
.data-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 14px;
}
.data-table th,
.data-table td {
  border: 1px solid var(--border-subtle, #e5e7eb);
  padding: 8px 12px;
  text-align: left;
}
.data-table th {
  background: var(--surface-muted, #f9fafb);
}
.placeholder {
  padding: 48px;
  text-align: center;
  color: #888;
}
</style>
