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
      <ul class="sub-mini-list">
        <li>{{ $t('subscriptionPage.quotaLineMembers', { n: effectiveQuota.maxMembers }) }}</li>
        <li>{{ $t('subscriptionPage.quotaLineRepos', { n: effectiveQuota.maxGithubRepos }) }}</li>
      </ul>
    </section>

    <section class="sub-compare sub-card sub-card--panel">
      <h2 class="sub-h2">{{ $t('subscriptionPage.compareTitle') }}</h2>
      <div class="sub-compare-grid">
        <div class="sub-compare-col">
          <div class="sub-compare-name">{{ $t('subscriptionPage.planFree') }}</div>
          <ul class="sub-compare-list">
            <li>{{ $t('subscriptionPage.quotaLineMembers', { n: quotaFree.maxMembers }) }}</li>
            <li>{{ $t('subscriptionPage.quotaLineRepos', { n: quotaFree.maxGithubRepos }) }}</li>
          </ul>
        </div>
        <div class="sub-compare-col">
          <div class="sub-compare-name">{{ $t('subscriptionPage.planTeam') }}</div>
          <ul class="sub-compare-list">
            <li>{{ $t('subscriptionPage.quotaLineMembers', { n: quotaTeam.maxMembers }) }}</li>
            <li>{{ $t('subscriptionPage.quotaLineRepos', { n: quotaTeam.maxGithubRepos }) }}</li>
          </ul>
        </div>
        <div class="sub-compare-col">
          <div class="sub-compare-name">{{ $t('subscriptionPage.planPro') }}</div>
          <ul class="sub-compare-list">
            <li>{{ $t('subscriptionPage.quotaLineMembers', { n: quotaPro.maxMembers }) }}</li>
            <li>{{ $t('subscriptionPage.quotaLineRepos', { n: quotaPro.maxGithubRepos }) }}</li>
          </ul>
        </div>
      </div>
      <p class="sub-compare-note">{{ $t('subscriptionPage.compareNote') }}</p>
    </section>

    <section class="sub-plans">
      <article class="sub-plan sub-card sub-card--panel">
        <h3 class="sub-h3">{{ $t('subscriptionPage.teamCard') }}</h3>
        <p class="sub-price">{{ formatMoney(status.teamPriceCents) }}</p>
        <p class="sub-desc">{{ $t('subscriptionPage.teamDesc') }}</p>
        <ul class="sub-benefits">
          <li>{{ $t('subscriptionPage.quotaLineMembers', { n: quotaTeam.maxMembers }) }}</li>
          <li>{{ $t('subscriptionPage.quotaLineRepos', { n: quotaTeam.maxGithubRepos }) }}</li>
        </ul>
        <button
          type="button"
          class="sub-btn primary"
          :disabled="paying"
          @click="mockPay('team')"
        >
          {{ paying === 'team' ? $t('subscriptionPage.paying') : $t('subscriptionPage.mockPay') }}
        </button>
      </article>
      <article class="sub-plan sub-card sub-card--panel sub-plan--pro">
        <h3 class="sub-h3">{{ $t('subscriptionPage.proCard') }}</h3>
        <p class="sub-price">{{ formatMoney(status.proPriceCents) }}</p>
        <p class="sub-desc">{{ $t('subscriptionPage.proDesc') }}</p>
        <ul class="sub-benefits">
          <li>{{ $t('subscriptionPage.quotaLineMembers', { n: quotaPro.maxMembers }) }}</li>
          <li>{{ $t('subscriptionPage.quotaLineRepos', { n: quotaPro.maxGithubRepos }) }}</li>
        </ul>
        <button
          type="button"
          class="sub-btn primary"
          :disabled="paying"
          @click="mockPay('pro')"
        >
          {{ paying === 'pro' ? $t('subscriptionPage.paying') : $t('subscriptionPage.mockPay') }}
        </button>
      </article>
    </section>

    <p v-if="toast" class="sub-toast">{{ toast }}</p>
    <p class="sub-footnote">{{ $t('subscriptionPage.footnote') }}</p>
  </div>
</template>

<script>
const DEFAULT_QUOTAS = {
  free: { maxMembers: 20, maxGithubRepos: 3 },
  team: { maxMembers: 100, maxGithubRepos: 20 },
  pro: { maxMembers: 10000, maxGithubRepos: 500 }
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
        proPriceCents: 0,
        planQuotas: null
      }
    }
  },
  computed: {
    quotaFree () {
      return (this.status.planQuotas && this.status.planQuotas.free) || DEFAULT_QUOTAS.free
    },
    quotaTeam () {
      return (this.status.planQuotas && this.status.planQuotas.team) || DEFAULT_QUOTAS.team
    },
    quotaPro () {
      return (this.status.planQuotas && this.status.planQuotas.pro) || DEFAULT_QUOTAS.pro
    },
    effectiveQuota () {
      const code = (this.status.planCode || 'free').toLowerCase()
      if (code === 'pro') return this.quotaPro
      if (code === 'team') return this.quotaTeam
      return this.quotaFree
    }
  },
  async mounted () {
    await this.load()
  },
  methods: {
    planLabel (code) {
      const c = (code || 'free').toLowerCase()
      if (c === 'team') return this.$t('subscriptionPage.planTeam')
      if (c === 'pro') return this.$t('subscriptionPage.planPro')
      return this.$t('subscriptionPage.planFree')
    },
    formatMoney (cents) {
      const n = Number(cents)
      if (!Number.isFinite(n) || n <= 0) return '—'
      return `¥${(n / 100).toFixed(2)}`
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
/* 本页深色卡片内强制高对比文字（避免继承全局浅底上的棕色字） */
.sub-page--billing {
  --sub-on-panel: #f8fafc;
  --sub-on-panel-muted: #cbd5e1;
  --sub-on-panel-dim: #94a3b8;
  --sub-panel-bg: #0f172a;
  --sub-panel-border: #334155;
  max-width: 960px;
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
  max-width: 720px;
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
.sub-mini-list li {
  margin-bottom: 4px;
}
.sub-compare {
  margin-top: 16px;
}
.sub-compare-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}
@media (max-width: 720px) {
  .sub-compare-grid {
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
  font-size: 13px;
  color: var(--sub-on-panel-muted);
  line-height: 1.5;
}
.sub-compare-note {
  margin: 12px 0 0;
  font-size: 12px;
  color: var(--sub-on-panel-dim);
  line-height: 1.45;
}
.sub-plans {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(260px, 1fr));
  gap: 16px;
  margin-top: 16px;
}
.sub-plan--pro.sub-card--panel {
  border-color: #3b82f6;
  box-shadow: 0 0 0 1px rgba(59, 130, 246, 0.25);
}
.sub-price {
  font-size: 26px;
  font-weight: 800;
  margin: 0 0 8px;
  color: var(--sub-on-panel);
  letter-spacing: 0.02em;
}
.sub-desc {
  margin: 0 0 10px;
  font-size: 14px;
  line-height: 1.45;
}
.sub-benefits {
  margin: 0 0 16px;
  padding-left: 1.15em;
  font-size: 14px;
  line-height: 1.55;
  color: var(--sub-on-panel-muted);
}
.sub-benefits li {
  margin-bottom: 4px;
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
  max-width: 720px;
}
</style>
