<template>
  <div class="sub-page sub-page--billing">
    <section class="sub-hero">
      <p class="sub-eyebrow">{{ $t('subscriptionPage.eyebrow') }}</p>
      <h1 class="sub-title">{{ $t('subscriptionPage.title') }}</h1>
      <p class="sub-lead">{{ $t('subscriptionPage.lead') }}</p>
    </section>

    <section v-if="loading" class="sub-card sub-card--panel sub-loading">{{ $t('dashboard.loading') }}…</section>
    <section v-else-if="error" class="sub-card sub-card--panel sub-error">{{ error }}</section>

    <section v-else class="sub-status sub-card sub-card--panel">
      <h2 class="sub-h2">{{ $t('subscriptionPage.statusTitle') }}</h2>
      <dl class="sub-dl">
        <div>
          <dt>{{ $t('subscriptionPage.currentPlan') }}</dt>
          <dd>
            <span class="sub-pill">{{ planLabel(status.planCode) }}</span>
          </dd>
        </div>
        <div>
          <dt>{{ $t('subscriptionPage.baseline') }}</dt>
          <dd>
            <span class="sub-pill sub-pill--muted">{{ planLabel(status.billingBaselinePlanCode) }}</span>
          </dd>
        </div>
        <div>
          <dt>{{ $t('subscriptionPage.endsAt') }}</dt>
          <dd class="sub-dd-strong">{{ status.subscriptionEndsAt || $t('subscriptionPage.noActiveWindow') }}</dd>
        </div>
      </dl>
      <p class="sub-effective-hint">{{ $t('subscriptionPage.effectiveLimitsHint') }}</p>
      <ul class="sub-mini-list sub-mini-list--slash">
        <li v-for="row in usageSlashRows" :key="row.key">{{ row.text }}</li>
      </ul>
    </section>

    <section class="sub-compare sub-card sub-card--panel">
      <h2 class="sub-h2">{{ $t('subscriptionPage.compareTitle') }}</h2>
      <div class="sub-compare-grid sub-compare-grid--4">
        <div class="sub-compare-col">
          <div class="sub-compare-name">{{ $t('subscriptionPage.planFree') }}</div>
          <ul class="sub-compare-list">
            <li v-for="(line, idx) in quotaLines(quotaFree)" :key="'f-' + idx">{{ line }}</li>
          </ul>
        </div>
        <div class="sub-compare-col">
          <div class="sub-compare-name">{{ $t('subscriptionPage.planTeam') }}</div>
          <ul class="sub-compare-list">
            <li v-for="(line, idx) in quotaLines(quotaTeam)" :key="'t-' + idx">{{ line }}</li>
          </ul>
        </div>
        <div class="sub-compare-col">
          <div class="sub-compare-name">{{ $t('subscriptionPage.planPro') }}</div>
          <ul class="sub-compare-list">
            <li v-for="(line, idx) in quotaLines(quotaPro)" :key="'p-' + idx">{{ line }}</li>
          </ul>
        </div>
        <div class="sub-compare-col">
          <div class="sub-compare-name">{{ $t('subscriptionPage.planProPlus') }}</div>
          <ul class="sub-compare-list">
            <li v-for="(line, idx) in quotaLines(quotaProPlus)" :key="'pp-' + idx">{{ line }}</li>
          </ul>
        </div>
      </div>
      <p class="sub-compare-note">{{ $t('subscriptionPage.compareNote') }}</p>
    </section>

    <section class="sub-plans sub-plans--3">
      <article class="sub-plan sub-card sub-card--panel">
        <h3 class="sub-h3">{{ $t('subscriptionPage.teamCard') }}</h3>
        <p class="sub-price">{{ formatMoney(status.teamPriceCents) }}</p>
        <p class="sub-price-note">{{ $t('subscriptionPage.teamPriceNote') }}</p>
        <p class="sub-desc">{{ $t('subscriptionPage.teamDesc') }}</p>
        <ul class="sub-benefits">
          <li v-for="(line, idx) in quotaLines(quotaTeam)" :key="'bt-' + idx">{{ line }}</li>
        </ul>
        <button
          type="button"
          class="sub-btn primary"
          :disabled="paying"
          @click="mockPay('team')"
        >
          {{ paying === 'team' ? $t('subscriptionPage.paying') : $t('subscriptionPage.mockPayTeam') }}
        </button>
      </article>
      <article class="sub-plan sub-card sub-card--panel sub-plan--pro">
        <h3 class="sub-h3">{{ $t('subscriptionPage.proCard') }}</h3>
        <p class="sub-price">{{ formatAnnual(status.proAnnualPriceCents) }}</p>
        <p class="sub-desc">{{ $t('subscriptionPage.proDesc') }}</p>
        <ul class="sub-benefits">
          <li v-for="(line, idx) in quotaLines(quotaPro)" :key="'bp-' + idx">{{ line }}</li>
        </ul>
        <button
          type="button"
          class="sub-btn primary"
          :disabled="paying"
          @click="mockPay('pro')"
        >
          {{ paying === 'pro' ? $t('subscriptionPage.paying') : $t('subscriptionPage.mockPayAnnual') }}
        </button>
      </article>
      <article class="sub-plan sub-card sub-card--panel sub-plan--ent">
        <h3 class="sub-h3">{{ $t('subscriptionPage.proPlusCard') }}</h3>
        <p class="sub-price">{{ formatAnnual(status.proPlusAnnualPriceCents) }}</p>
        <p class="sub-desc">{{ $t('subscriptionPage.proPlusDesc') }}</p>
        <ul class="sub-benefits">
          <li v-for="(line, idx) in quotaLines(quotaProPlus)" :key="'bpp-' + idx">{{ line }}</li>
        </ul>
        <button
          type="button"
          class="sub-btn primary"
          :disabled="paying"
          @click="mockPay('pro_plus')"
        >
          {{ paying === 'pro_plus' ? $t('subscriptionPage.paying') : $t('subscriptionPage.mockPayAnnual') }}
        </button>
      </article>
    </section>

    <p v-if="toast" class="sub-toast">{{ toast }}</p>
    <p class="sub-footnote">{{ $t('subscriptionPage.footnote') }}</p>
  </div>
</template>

<script>
const DEFAULT_QUOTAS = {
  free: {
    maxMembers: 20,
    maxGithubRepos: 3,
    maxQuoteProjects: 3,
    maxClientBoardsEnabled: 3,
    maxAgentSessions: 3,
    maxCollabProjects: 5,
    maxNexusAccounts: 2
  },
  team: {
    maxMembers: 100,
    maxGithubRepos: 20,
    maxQuoteProjects: 50,
    maxClientBoardsEnabled: 20,
    maxAgentSessions: 200,
    maxCollabProjects: 30,
    maxNexusAccounts: 20
  },
  pro: {
    maxMembers: 500,
    maxGithubRepos: 80,
    maxQuoteProjects: 400,
    maxClientBoardsEnabled: 80,
    maxAgentSessions: 1500,
    maxCollabProjects: 150,
    maxNexusAccounts: 40
  },
  pro_plus: {
    maxMembers: 10000,
    maxGithubRepos: 500,
    maxQuoteProjects: 2000,
    maxClientBoardsEnabled: 500,
    maxAgentSessions: 5000,
    maxCollabProjects: 500,
    maxNexusAccounts: 100
  }
}

export default {
  name: 'SubscriptionBillingView',
  data () {
    return {
      loading: true,
      error: '',
      paying: '',
      toast: '',
      status: {
        planCode: 'free',
        billingBaselinePlanCode: 'free',
        subscriptionEndsAt: null,
        teamPriceCents: 0,
        proAnnualPriceCents: 0,
        proPlusAnnualPriceCents: 0,
        planQuotas: null,
        usage: null,
        effectivePlan: null
      }
    }
  },
  computed: {
    quotaFree () {
      return this.normQ(this.status.planQuotas && this.status.planQuotas.free, DEFAULT_QUOTAS.free)
    },
    quotaTeam () {
      return this.normQ(this.status.planQuotas && this.status.planQuotas.team, DEFAULT_QUOTAS.team)
    },
    quotaPro () {
      return this.normQ(this.status.planQuotas && this.status.planQuotas.pro, DEFAULT_QUOTAS.pro)
    },
    quotaProPlus () {
      return this.normQ(this.status.planQuotas && this.status.planQuotas.pro_plus, DEFAULT_QUOTAS.pro_plus)
    },
    effectiveQuota () {
      if (this.status.effectivePlan) {
        return this.normQ(this.status.effectivePlan, this.quotaFree)
      }
      const code = (this.status.planCode || 'free').toLowerCase()
      if (code === 'team') return this.quotaTeam
      if (code === 'pro') return this.quotaPro
      if (code === 'pro_plus' || code === 'enterprise') return this.quotaProPlus
      return this.quotaFree
    },
    usageSlashRows () {
      const u = this.status.usage || {}
      const q = this.effectiveQuota
      const rows = [
        { key: 'm', label: this.$t('subscriptionPage.quotaLblMembers'), used: this.num(u.collabMembers), cap: q.maxMembers },
        { key: 'g', label: this.$t('subscriptionPage.quotaLblGithub'), used: this.num(u.githubLinkedQuotes), cap: q.maxGithubRepos },
        { key: 'q', label: this.$t('subscriptionPage.quotaLblQuotes'), used: this.num(u.quoteProjects), cap: q.maxQuoteProjects },
        { key: 'b', label: this.$t('subscriptionPage.quotaLblBoards'), used: this.num(u.clientBoardsEnabled), cap: q.maxClientBoardsEnabled },
        { key: 'a', label: this.$t('subscriptionPage.quotaLblAgent'), used: this.num(u.agentSessions), cap: q.maxAgentSessions },
        { key: 'c', label: this.$t('subscriptionPage.quotaLblCollab'), used: this.num(u.collabProjects), cap: q.maxCollabProjects },
        { key: 'n', label: this.$t('subscriptionPage.quotaLblNexus'), used: this.num(u.nexusAccounts), cap: q.maxNexusAccounts }
      ]
      return rows.map(r => ({
        key: r.key,
        text: this.$t('subscriptionPage.usageSlash', {
          label: r.label,
          used: r.used,
          cap: r.cap
        })
      }))
    }
  },
  async mounted () {
    await this.load()
  },
  methods: {
    num (v) {
      const n = Number(v)
      return Number.isFinite(n) ? n : 0
    },
    normQ (src, fallback) {
      if (!src || typeof src !== 'object') return { ...fallback }
      return {
        maxMembers: this.num(src.maxMembers) || fallback.maxMembers,
        maxGithubRepos: this.num(src.maxGithubRepos) || fallback.maxGithubRepos,
        maxQuoteProjects: this.num(src.maxQuoteProjects) || fallback.maxQuoteProjects,
        maxClientBoardsEnabled: this.num(src.maxClientBoardsEnabled) || fallback.maxClientBoardsEnabled,
        maxAgentSessions: this.num(src.maxAgentSessions) || fallback.maxAgentSessions,
        maxCollabProjects: this.num(src.maxCollabProjects) || fallback.maxCollabProjects,
        maxNexusAccounts: this.num(src.maxNexusAccounts) || fallback.maxNexusAccounts
      }
    },
    quotaLines (q) {
      return [
        this.$t('subscriptionPage.quotaLineMembers', { n: q.maxMembers }),
        this.$t('subscriptionPage.quotaLineRepos', { n: q.maxGithubRepos }),
        this.$t('subscriptionPage.quotaLineQuotes', { n: q.maxQuoteProjects }),
        this.$t('subscriptionPage.quotaLineBoards', { n: q.maxClientBoardsEnabled }),
        this.$t('subscriptionPage.quotaLineAgent', { n: q.maxAgentSessions }),
        this.$t('subscriptionPage.quotaLineCollab', { n: q.maxCollabProjects }),
        this.$t('subscriptionPage.quotaLineNexus', { n: q.maxNexusAccounts })
      ]
    },
    planLabel (code) {
      const c = (code || 'free').toLowerCase()
      if (c === 'team') return this.$t('subscriptionPage.planTeam')
      if (c === 'pro') return this.$t('subscriptionPage.planPro')
      if (c === 'pro_plus' || c === 'enterprise') return this.$t('subscriptionPage.planProPlus')
      return this.$t('subscriptionPage.planFree')
    },
    formatMoney (cents) {
      const n = Number(cents)
      if (!Number.isFinite(n) || n <= 0) return '—'
      return `¥${(n / 100).toFixed(2)}`
    },
    formatAnnual (cents) {
      const m = this.formatMoney(cents)
      if (m === '—') return '—'
      return this.$t('subscriptionPage.pricePerYear', { price: m })
    },
    async load () {
      this.loading = true
      this.error = ''
      try {
        const { data } = await this.$http.get('/admin/billing/status')
        if (data.code === 0 && data.data) {
          this.status = { ...this.status, ...data.data }
        } else {
          this.error = (data && data.message) || '加载失败'
        }
      } catch (e) {
        this.error = (e.response && e.response.data && e.response.data.message) || '网络错误'
      } finally {
        this.loading = false
      }
    },
    async mockPay (planCode) {
      this.paying = planCode
      this.toast = ''
      try {
        const { data } = await this.$http.post('/admin/billing/mock-pay', { planCode })
        if (data.code === 0 && data.data) {
          this.toast = data.data.message || this.$t('subscriptionPage.success')
          await this.load()
        } else {
          this.toast = (data && data.message) || '操作失败'
        }
      } catch (e) {
        const msg = e.response && e.response.data && e.response.data.message
        this.toast = msg || '请求失败'
      } finally {
        this.paying = ''
      }
    }
  }
}
</script>

<style scoped>
/* 本页深色卡片内强制高对比文字 */
.sub-page--billing {
  --sub-on-panel: #f8fafc;
  --sub-on-panel-muted: #cbd5e1;
  --sub-on-panel-dim: #94a3b8;
  --sub-panel-bg: #0f172a;
  --sub-panel-border: #334155;
  max-width: 1100px;
  margin: 0 auto;
  padding: 8px 4px 32px;
}
.sub-page--billing .sub-title {
  color: #0f172a;
}
.sub-page--billing .sub-eyebrow {
  color: #475569;
}
.sub-page--billing .sub-lead {
  color: #475569;
}
.sub-hero {
  margin-bottom: 20px;
}
.sub-eyebrow {
  font-size: 12px;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  margin: 0 0 8px;
}
.sub-title {
  margin: 0 0 8px;
  font-size: 24px;
  font-weight: 700;
}
.sub-lead {
  margin: 0;
  line-height: 1.55;
  max-width: 820px;
}
.sub-card--panel {
  background: var(--sub-panel-bg);
  border: 1px solid var(--sub-panel-border);
  color: var(--sub-on-panel);
  border-radius: 12px;
  padding: 16px 18px;
}
.sub-card--panel .sub-h2,
.sub-card--panel .sub-h3 {
  color: var(--sub-on-panel);
}
.sub-card--panel .sub-desc {
  color: var(--sub-on-panel-muted);
}
.sub-loading,
.sub-error {
  color: var(--sub-on-panel-muted);
}
.sub-error.sub-card--panel {
  color: #fecaca;
  border-color: #7f1d1d;
}
.sub-h2 {
  margin: 0 0 12px;
  font-size: 16px;
  font-weight: 700;
}
.sub-h3 {
  margin: 0 0 8px;
  font-size: 18px;
  font-weight: 700;
}
.sub-dl {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 12px 20px;
  margin: 0;
}
.sub-dl dt {
  font-size: 12px;
  color: var(--sub-on-panel-dim);
  margin-bottom: 6px;
  font-weight: 500;
}
.sub-dl dd {
  margin: 0;
  font-size: 16px;
  color: var(--sub-on-panel);
}
.sub-dd-strong {
  font-weight: 600;
  letter-spacing: 0.02em;
}
.sub-pill {
  display: inline-block;
  padding: 4px 10px;
  border-radius: 999px;
  font-size: 14px;
  font-weight: 600;
  color: #0f172a;
  background: #e2e8f0;
}
.sub-pill--muted {
  background: #64748b;
  color: #f8fafc;
}
.sub-effective-hint {
  margin: 14px 0 8px;
  font-size: 13px;
  color: var(--sub-on-panel-muted);
  line-height: 1.45;
}
.sub-mini-list {
  margin: 0;
  padding-left: 1.2em;
  color: var(--sub-on-panel-muted);
  font-size: 14px;
  line-height: 1.55;
}
.sub-mini-list--slash {
  list-style: none;
  padding-left: 0;
}
.sub-mini-list--slash li {
  margin-bottom: 6px;
  font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
  font-size: 13px;
  color: var(--sub-on-panel);
}
.sub-compare {
  margin-top: 16px;
}
.sub-compare-grid {
  display: grid;
  gap: 12px;
}
.sub-compare-grid--4 {
  grid-template-columns: repeat(4, minmax(0, 1fr));
}
@media (max-width: 1024px) {
  .sub-compare-grid--4 {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
@media (max-width: 560px) {
  .sub-compare-grid--4 {
    grid-template-columns: 1fr;
  }
}
.sub-compare-col {
  padding: 12px;
  border-radius: 8px;
  background: rgba(15, 23, 42, 0.6);
  border: 1px solid #1e293b;
}
.sub-compare-name {
  font-weight: 700;
  font-size: 14px;
  color: var(--sub-on-panel);
  margin-bottom: 8px;
}
.sub-compare-list {
  margin: 0;
  padding-left: 1.1em;
  font-size: 12px;
  color: var(--sub-on-panel-muted);
  line-height: 1.45;
}
.sub-compare-note {
  margin: 12px 0 0;
  font-size: 12px;
  color: var(--sub-on-panel-dim);
  line-height: 1.45;
}
.sub-plans {
  display: grid;
  gap: 16px;
  margin-top: 16px;
}
.sub-plans--3 {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}
@media (max-width: 900px) {
  .sub-plans--3 {
    grid-template-columns: 1fr;
  }
}
.sub-plan--pro.sub-card--panel {
  border-color: #3b82f6;
  box-shadow: 0 0 0 1px rgba(59, 130, 246, 0.25);
}
.sub-plan--ent.sub-card--panel {
  border-color: #a855f7;
  box-shadow: 0 0 0 1px rgba(168, 85, 247, 0.22);
}
.sub-price {
  font-size: 22px;
  font-weight: 800;
  margin: 0 0 4px;
  color: var(--sub-on-panel);
  letter-spacing: 0.02em;
}
.sub-price-unit {
  font-size: 13px;
  font-weight: 500;
}
.sub-price-note {
  margin: 0 0 8px;
  font-size: 12px;
  color: var(--sub-on-panel-dim);
}
.sub-desc {
  margin: 0 0 10px;
  font-size: 14px;
  line-height: 1.45;
}
.sub-benefits {
  margin: 0 0 16px;
  padding-left: 1.15em;
  font-size: 12px;
  line-height: 1.45;
  color: var(--sub-on-panel-muted);
}
.sub-benefits li {
  margin-bottom: 3px;
}
.sub-btn {
  width: 100%;
  padding: 10px 14px;
  border-radius: 8px;
  border: 1px solid transparent;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
}
.sub-btn.primary {
  background: linear-gradient(135deg, #2563eb, #1d4ed8);
  color: #fff;
}
.sub-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}
.sub-toast {
  margin-top: 14px;
  color: #4ade80;
  font-size: 14px;
}
.sub-footnote {
  margin-top: 20px;
  font-size: 12px;
  color: #64748b;
  line-height: 1.55;
  max-width: 820px;
}
</style>
