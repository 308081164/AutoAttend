<template>
  <div class="sub-page">
    <section class="sub-hero">
      <p class="sub-eyebrow">{{ $t('subscriptionPage.eyebrow') }}</p>
      <h1 class="sub-title">{{ $t('subscriptionPage.title') }}</h1>
      <p class="sub-lead">{{ $t('subscriptionPage.lead') }}</p>
    </section>

    <section v-if="loading" class="sub-card muted">{{ $t('dashboard.loading') }}…</section>
    <section v-else-if="error" class="sub-card err">{{ error }}</section>

    <section v-else class="sub-status sub-card">
      <h2 class="sub-h2">{{ $t('subscriptionPage.statusTitle') }}</h2>
      <dl class="sub-dl">
        <div>
          <dt>{{ $t('subscriptionPage.currentPlan') }}</dt>
          <dd><code>{{ planLabel(status.planCode) }}</code></dd>
        </div>
        <div>
          <dt>{{ $t('subscriptionPage.baseline') }}</dt>
          <dd><code>{{ planLabel(status.billingBaselinePlanCode) }}</code></dd>
        </div>
        <div>
          <dt>{{ $t('subscriptionPage.endsAt') }}</dt>
          <dd>{{ status.subscriptionEndsAt || '—' }}</dd>
        </div>
      </dl>
    </section>

    <section class="sub-plans">
      <article class="sub-plan sub-card">
        <h3 class="sub-h3">{{ $t('subscriptionPage.teamCard') }}</h3>
        <p class="sub-price">{{ formatMoney(status.teamPriceCents) }}</p>
        <p class="sub-desc">{{ $t('subscriptionPage.teamDesc') }}</p>
        <button
          type="button"
          class="sub-btn primary"
          :disabled="paying"
          @click="mockPay('team')"
        >
          {{ paying === 'team' ? $t('subscriptionPage.paying') : $t('subscriptionPage.mockPay') }}
        </button>
      </article>
      <article class="sub-plan sub-card sub-plan--pro">
        <h3 class="sub-h3">{{ $t('subscriptionPage.proCard') }}</h3>
        <p class="sub-price">{{ formatMoney(status.proPriceCents) }}</p>
        <p class="sub-desc">{{ $t('subscriptionPage.proDesc') }}</p>
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
        proPriceCents: 0
      }
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
.sub-page {
  max-width: 960px;
  margin: 0 auto;
  padding: 8px 4px 32px;
}
.sub-hero {
  margin-bottom: 20px;
}
.sub-eyebrow {
  font-size: 12px;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: var(--muted, #64748b);
  margin: 0 0 8px;
}
.sub-title {
  margin: 0 0 8px;
  font-size: 24px;
  font-weight: 700;
}
.sub-lead {
  margin: 0;
  color: var(--muted, #64748b);
  line-height: 1.5;
  max-width: 720px;
}
.sub-card {
  background: var(--panel, #0f172a);
  border: 1px solid var(--border, #1e293b);
  border-radius: 12px;
  padding: 16px 18px;
}
.sub-h2 {
  margin: 0 0 12px;
  font-size: 16px;
}
.sub-h3 {
  margin: 0 0 8px;
  font-size: 18px;
}
.sub-dl {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 12px 20px;
  margin: 0;
}
.sub-dl dt {
  font-size: 12px;
  color: var(--muted, #94a3b8);
  margin-bottom: 4px;
}
.sub-dl dd {
  margin: 0;
  font-size: 15px;
}
.sub-plans {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(260px, 1fr));
  gap: 16px;
  margin-top: 16px;
}
.sub-plan--pro {
  border-color: #334155;
  box-shadow: 0 0 0 1px rgba(96, 165, 250, 0.15);
}
.sub-price {
  font-size: 22px;
  font-weight: 700;
  margin: 0 0 8px;
}
.sub-desc {
  margin: 0 0 16px;
  font-size: 14px;
  color: var(--muted, #94a3b8);
  line-height: 1.45;
}
.sub-btn {
  width: 100%;
  padding: 10px 14px;
  border-radius: 8px;
  border: 1px solid transparent;
  font-size: 14px;
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
.muted { color: var(--muted, #94a3b8); }
.err { color: #f87171; }
.sub-toast {
  margin-top: 14px;
  color: #86efac;
  font-size: 14px;
}
.sub-footnote {
  margin-top: 20px;
  font-size: 12px;
  color: var(--muted, #64748b);
  line-height: 1.5;
}
code {
  font-size: 13px;
  padding: 2px 6px;
  border-radius: 4px;
  background: #1e293b;
}
</style>
