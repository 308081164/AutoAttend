<template>
  <div class="sub-page sub-page--billing">
    <transition name="sub-toast-fade">
      <div
        v-if="toastMessage"
        class="sub-toast-bubble"
        :class="'sub-toast-bubble--' + toastKind"
        role="status"
        aria-live="polite"
      >
        {{ toastMessage }}
      </div>
    </transition>

    <section class="sub-hero">
      <p class="sub-eyebrow">{{ $t('subscriptionPage.eyebrow') }}</p>
      <h1 class="sub-title">{{ $t('subscriptionPage.title') }}</h1>
      <p class="sub-lead">{{ $t('subscriptionPage.lead') }}</p>
    </section>

    <section v-if="loading" class="sub-card sub-card--panel sub-loading">{{ $t('dashboard.loading') }}…</section>
    <section v-else-if="error" class="sub-card sub-card--panel sub-error">{{ error }}</section>

    <template v-else>
    <section class="sub-status sub-card sub-card--panel">
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
        <div>
          <dt>{{ $t('subscriptionPage.memberPoints') }}</dt>
          <dd class="sub-dd-strong">{{ num(status.memberPoints) }}</dd>
        </div>
      </dl>
      <p class="sub-points-hint">{{ $t('subscriptionPage.pointsRule') }}</p>
      <p class="sub-effective-hint">{{ $t('subscriptionPage.effectiveLimitsHint') }}</p>
      <ul class="sub-mini-list sub-mini-list--slash">
        <li v-for="row in usageSlashRows" :key="row.key">{{ row.text }}</li>
      </ul>
    </section>

    <nav class="sub-billing-tabstrip" role="tablist" aria-label="会员与计费">
      <button
        type="button"
        role="tab"
        :aria-selected="billingTab === 'purchase'"
        class="sub-billing-tab"
        :class="{ 'sub-billing-tab--active': billingTab === 'purchase' }"
        @click="billingTab = 'purchase'"
      >
        {{ $t('subscriptionPage.tabPurchase') }}
      </button>
      <button
        type="button"
        role="tab"
        :aria-selected="billingTab === 'referral'"
        class="sub-billing-tab"
        :class="{ 'sub-billing-tab--active': billingTab === 'referral' }"
        @click="onReferralTab"
      >
        {{ $t('subscriptionPage.tabReferral') }}
      </button>
    </nav>

    <div v-show="billingTab === 'purchase'">
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
        <p class="sub-price">{{ formatMoney(status.teamPriceCents) }}{{ $t('subscriptionPage.perMonth') }}</p>
        <p class="sub-price-sub">{{ $t('subscriptionPage.teamFirstMonth') }}：{{ formatMoney(status.teamFirstMonthPriceCents) }}{{ $t('subscriptionPage.perMonth') }}</p>
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
    </div>

    <div v-show="billingTab === 'referral'" class="sub-referral-wrap">
    <section class="sub-invite sub-card sub-card--panel">
      <h2 class="sub-h2">{{ $t('subscriptionPage.inviteRedeemTitle') }}</h2>
      <p class="sub-desc">{{ $t('subscriptionPage.inviteRedeemHint') }}</p>
      <div class="sub-invite-row">
        <input v-model="redeemCode" type="text" class="sub-invite-input" :disabled="status.inviteCodeRedeemed" :placeholder="'ABC12345'">
        <button type="button" class="sub-btn primary" :disabled="redeeming || status.inviteCodeRedeemed" @click="redeemInvite">
          {{ redeeming ? $t('subscriptionPage.paying') : $t('subscriptionPage.inviteRedeemBtn') }}
        </button>
      </div>
      <p v-if="status.inviteCodeRedeemed" class="sub-muted">{{ $t('subscriptionPage.inviteRedeemedNote') }}</p>
    </section>

    <section class="sub-official sub-card sub-card--panel">
      <h2 class="sub-h2">{{ $t('subscriptionPage.officialApiTitle') }}</h2>
      <p class="sub-desc">{{ $t('subscriptionPage.officialApiHint') }}</p>
      <dl class="sub-dl">
        <div>
          <dt>{{ $t('subscriptionPage.officialApiBalance') }}</dt>
          <dd class="sub-dd-strong">{{ formatOfficialYuan(status.officialApiCnyBalance) }}</dd>
        </div>
      </dl>
      <div class="sub-invite-row">
        <input
          v-model="officialRedeemCode"
          type="text"
          class="sub-invite-input"
          :placeholder="$t('subscriptionPage.officialApiPlaceholder')"
        >
        <button
          type="button"
          class="sub-btn primary"
          :disabled="officialRedeeming"
          @click="redeemOfficialApi"
        >
          {{ officialRedeeming ? $t('subscriptionPage.paying') : $t('subscriptionPage.officialApiRedeemBtn') }}
        </button>
      </div>
    </section>

    <section class="sub-invite sub-card sub-card--panel">
      <h3 class="sub-h3 sub-h3--sm">{{ $t('subscriptionPage.myInviteCode') }}</h3>
      <p class="sub-invite-user-hint">{{ $t('subscriptionPage.myInviteCodeHint') }}</p>
      <p v-if="myInviteLoading" class="sub-muted">…</p>
      <template v-else>
        <div class="sub-my-code-box">
          <span class="sub-my-code-text">{{ myInviteCode || '—' }}</span>
          <button
            v-if="myInviteCode"
            type="button"
            class="sub-btn sub-btn-copy"
            @click="copyMyInvite"
          >
            {{ copyInviteLabel }}
          </button>
        </div>
        <button type="button" class="sub-btn secondary small" @click="loadMyInvite">{{ $t('subscriptionPage.refreshInvite') }}</button>
      </template>
    </section>

      <section class="sub-card sub-card--panel sub-referral-block">
        <h2 class="sub-h2">{{ $t('subscriptionPage.cofounderTitle') }}</h2>
        <p class="sub-desc">{{ $t('subscriptionPage.cofounderHint') }}</p>
        <p v-if="referralLoading" class="sub-muted">…</p>
        <div v-else-if="referralInvitees.length" class="sub-table-scroll">
          <table class="sub-data-table">
            <thead>
              <tr>
                <th>{{ $t('subscriptionPage.colOrgName') }}</th>
                <th>slug</th>
                <th>{{ $t('subscriptionPage.colRegisteredAt') }}</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="row in referralInvitees" :key="row.tenantId">
                <td>{{ row.name || '—' }}</td>
                <td><code class="sub-mono">{{ row.slug || '—' }}</code></td>
                <td>{{ formatReferralDate(row.createdAt) }}</td>
              </tr>
            </tbody>
          </table>
        </div>
        <p v-else class="sub-muted">{{ $t('subscriptionPage.cofounderEmpty') }}</p>
      </section>
      <section class="sub-card sub-card--panel sub-referral-block">
        <h2 class="sub-h2">{{ $t('subscriptionPage.pointsLedgerTitle') }}</h2>
        <p class="sub-desc">{{ $t('subscriptionPage.pointsLedgerHint') }}</p>
        <p v-if="referralLoading" class="sub-muted">…</p>
        <div v-else-if="pointsLedger.length" class="sub-table-scroll">
          <table class="sub-data-table">
            <thead>
              <tr>
                <th>{{ $t('subscriptionPage.colSourceOrg') }}</th>
                <th>{{ $t('subscriptionPage.colOrderAmount') }}</th>
                <th>{{ $t('subscriptionPage.colPointsEarned') }}</th>
                <th>{{ $t('subscriptionPage.colRecordedAt') }}</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="row in pointsLedger" :key="row.id">
                <td>{{ row.sourceTenantName || ('#' + row.sourceTenantId) }}</td>
                <td>{{ formatMoney(row.orderAmountCents) }}</td>
                <td class="sub-dd-strong">{{ num(row.commissionCents) }}</td>
                <td>{{ formatReferralDate(row.createdAt) }}</td>
              </tr>
            </tbody>
          </table>
        </div>
        <p v-else class="sub-muted">{{ $t('subscriptionPage.pointsLedgerEmpty') }}</p>
      </section>
    </div>

    <p class="sub-footnote">{{ $t('subscriptionPage.footnote') }}</p>
    </template>
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
      billingTab: 'purchase',
      referralInvitees: [],
      pointsLedger: [],
      referralLoading: false,
      loading: true,
      error: '',
      paying: '',
      toastMessage: '',
      toastKind: 'success',
      toastDismissTimer: null,
      redeemCode: '',
      redeeming: false,
      officialRedeemCode: '',
      officialRedeeming: false,
      myInviteCode: '',
      myInviteLoading: false,
      copyInviteHintKey: '',
      status: {
        planCode: 'free',
        billingBaselinePlanCode: 'free',
        subscriptionEndsAt: null,
        teamPriceCents: 0,
        teamFirstMonthPriceCents: 0,
        memberPoints: 0,
        inviteCodeRedeemed: false,
        proAnnualPriceCents: 0,
        proPlusAnnualPriceCents: 0,
        planQuotas: null,
        usage: null,
        effectivePlan: null,
        officialApiCnyBalance: null,
        officialAiPoolEnabled: false
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
    copyInviteLabel () {
      if (this.copyInviteHintKey === 'copied') return this.$t('subscriptionPage.inviteCopied')
      if (this.copyInviteHintKey === 'fail') return this.$t('subscriptionPage.inviteCopyFail')
      return this.$t('subscriptionPage.copyInvite')
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
  beforeDestroy () {
    if (this.toastDismissTimer) {
      clearTimeout(this.toastDismissTimer)
      this.toastDismissTimer = null
    }
  },
  methods: {
    showToast (message, kind = 'success') {
      const k = kind === 'error' ? 'error' : 'success'
      const text = (message != null && String(message).trim() !== '') ? String(message).trim() : (
        k === 'error' ? this.$t('subscriptionPage.toastGenericError') : this.$t('subscriptionPage.toastGenericOk')
      )
      if (this.toastDismissTimer) {
        clearTimeout(this.toastDismissTimer)
        this.toastDismissTimer = null
      }
      this.toastKind = k
      this.toastMessage = text
      this.toastDismissTimer = setTimeout(() => {
        this.toastMessage = ''
        this.toastDismissTimer = null
      }, 3800)
    },
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
    formatOfficialYuan (v) {
      if (v == null || v === '') return '—'
      const n = Number(v)
      if (!Number.isFinite(n)) return '—'
      return `¥${n.toFixed(2)}`
    },
    async copyMyInvite () {
      const t = (this.myInviteCode || '').trim()
      if (!t) return
      try {
        await navigator.clipboard.writeText(t)
        this.copyInviteHintKey = 'copied'
        setTimeout(() => { this.copyInviteHintKey = '' }, 2500)
      } catch (e) {
        this.copyInviteHintKey = 'fail'
        setTimeout(() => { this.copyInviteHintKey = '' }, 3500)
      }
    },
    onReferralTab () {
      this.billingTab = 'referral'
      this.loadReferralData()
    },
    async loadReferralData () {
      this.referralLoading = true
      try {
        const [inv, led] = await Promise.all([
          this.$http.get('/admin/billing/referral/invitees'),
          this.$http.get('/admin/billing/referral/points-ledger')
        ])
        if (inv.data && inv.data.code === 0 && inv.data.data) {
          this.referralInvitees = Array.isArray(inv.data.data.items) ? inv.data.data.items : []
        }
        if (led.data && led.data.code === 0 && led.data.data) {
          this.pointsLedger = Array.isArray(led.data.data.items) ? led.data.data.items : []
        }
      } catch (e) {
        this.referralInvitees = []
        this.pointsLedger = []
      } finally {
        this.referralLoading = false
      }
    },
    formatReferralDate (v) {
      if (v == null || v === '') return '—'
      try {
        const d = new Date(v)
        if (Number.isNaN(d.getTime())) return String(v)
        return d.toLocaleString(this.$i18n.locale === 'en' ? 'en-US' : 'zh-CN', {
          year: 'numeric',
          month: '2-digit',
          day: '2-digit',
          hour: '2-digit',
          minute: '2-digit'
        })
      } catch (e) {
        return String(v)
      }
    },
    async load () {
      this.loading = true
      this.error = ''
      try {
        const { data } = await this.$http.get('/admin/billing/status')
        if (data.code === 0 && data.data) {
          this.status = { ...this.status, ...data.data }
          this.loadMyInvite()
        } else {
          this.error = (data && data.message) || '加载失败'
        }
      } catch (e) {
        this.error = (e.response && e.response.data && e.response.data.message) || '网络错误'
      } finally {
        this.loading = false
      }
    },
    async loadMyInvite () {
      this.myInviteLoading = true
      try {
        const { data } = await this.$http.get('/admin/billing/invite/my-code')
        if (data.code === 0 && data.data && data.data.code) {
          this.myInviteCode = data.data.code
        }
      } catch (e) {
        this.myInviteCode = ''
      } finally {
        this.myInviteLoading = false
      }
    },
    async redeemOfficialApi () {
      const c = (this.officialRedeemCode || '').trim()
      if (!c) return
      this.officialRedeeming = true
      try {
        const { data } = await this.$http.post('/admin/billing/official-api/redeem', { code: c })
        if (data.code === 0) {
          this.showToast((data.data && data.data.message) || this.$t('subscriptionPage.officialApiRedeemOk'), 'success')
          this.officialRedeemCode = ''
          await this.load()
        } else {
          this.showToast((data && data.message) || this.$t('subscriptionPage.toastRedeemFail'), 'error')
        }
      } catch (e) {
        const msg = e.response && e.response.data && e.response.data.message
        this.showToast(msg || this.$t('subscriptionPage.toastRedeemFail'), 'error')
      } finally {
        this.officialRedeeming = false
      }
    },
    async redeemInvite () {
      const c = (this.redeemCode || '').trim()
      if (!c || this.status.inviteCodeRedeemed) return
      this.redeeming = true
      try {
        const { data } = await this.$http.post('/admin/billing/invite/redeem', { inviteCode: c })
        if (data.code === 0) {
          this.showToast((data.data && data.data.message) || this.$t('subscriptionPage.inviteRedeemOk'), 'success')
          await this.load()
        } else {
          this.showToast((data && data.message) || this.$t('subscriptionPage.toastRedeemFail'), 'error')
        }
      } catch (e) {
        const msg = e.response && e.response.data && e.response.data.message
        this.showToast(msg || this.$t('subscriptionPage.toastRedeemFail'), 'error')
      } finally {
        this.redeeming = false
      }
    },
    async mockPay (planCode) {
      this.paying = planCode
      try {
        const { data } = await this.$http.post('/admin/billing/mock-pay', { planCode, useMemberPoints: true })
        if (data.code === 0 && data.data) {
          this.showToast(data.data.message || this.$t('subscriptionPage.success'), 'success')
          await this.load()
        } else {
          this.showToast((data && data.message) || this.$t('subscriptionPage.toastPayFail'), 'error')
        }
      } catch (e) {
        const msg = e.response && e.response.data && e.response.data.message
        this.showToast(msg || this.$t('subscriptionPage.toastRequestFail'), 'error')
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
.sub-points-hint {
  margin: 10px 0 0;
  font-size: 12px;
  color: var(--sub-on-panel-dim);
  line-height: 1.45;
}
.sub-invite {
  margin-top: 16px;
}
.sub-invite-row {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  align-items: center;
  margin: 10px 0 16px;
}
.sub-invite-input {
  flex: 1;
  min-width: 160px;
  padding: 10px 12px;
  border-radius: 8px;
  border: 1px solid var(--sub-panel-border);
  background: #1e293b;
  color: var(--sub-on-panel);
  font-size: 15px;
  letter-spacing: 0.06em;
}
.sub-h3--sm {
  margin-top: 12px;
  font-size: 15px;
}
.sub-invite-user-hint {
  margin: 4px 0 10px;
  font-size: 13px;
  line-height: 1.45;
  color: var(--sub-on-panel-muted);
}
.sub-my-code-box {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 12px;
  margin: 0 0 12px;
  padding: 14px 16px;
  border-radius: 10px;
  border: 1px solid rgba(148, 163, 184, 0.35);
  background: linear-gradient(135deg, #0f172a 0%, #1e293b 100%);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.06);
}
.sub-my-code-text {
  font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
  font-size: 20px;
  font-weight: 800;
  letter-spacing: 0.14em;
  color: #f8fafc;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.45);
  word-break: break-all;
}
.sub-btn-copy {
  flex-shrink: 0;
  width: auto;
  padding: 8px 14px;
  font-size: 13px;
  font-weight: 600;
  background: #334155;
  border: 1px solid #64748b;
  color: #f1f5f9;
}
.sub-btn-copy:hover {
  background: #475569;
}
.sub-mono {
  font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
  font-size: 18px;
  font-weight: 600;
  letter-spacing: 0.08em;
  margin: 6px 0;
}
.sub-muted {
  font-size: 13px;
  color: var(--sub-on-panel-dim);
  margin: 6px 0;
}
.sub-btn.secondary {
  background: transparent;
  border: 1px solid #64748b;
  color: #e2e8f0;
}
.sub-btn.small {
  padding: 6px 12px;
  font-size: 13px;
}
.sub-price-sub {
  margin: 0 0 6px;
  font-size: 14px;
  color: #a5b4fc;
  font-weight: 600;
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
.sub-toast-bubble {
  position: fixed;
  top: 16px;
  left: 50%;
  transform: translateX(-50%);
  z-index: 10050;
  max-width: min(420px, calc(100vw - 32px));
  padding: 12px 18px;
  border-radius: 999px;
  font-size: 14px;
  font-weight: 500;
  line-height: 1.4;
  text-align: center;
  box-shadow: 0 10px 40px rgba(15, 23, 42, 0.35), 0 0 0 1px rgba(255, 255, 255, 0.06) inset;
  pointer-events: none;
}
.sub-toast-bubble--success {
  background: linear-gradient(145deg, #14532d, #166534);
  color: #ecfdf5;
  border: 1px solid rgba(74, 222, 128, 0.45);
}
.sub-toast-bubble--error {
  background: linear-gradient(145deg, #7f1d1d, #991b1b);
  color: #fef2f2;
  border: 1px solid rgba(252, 165, 165, 0.45);
}
.sub-toast-fade-enter-active,
.sub-toast-fade-leave-active {
  transition: opacity 0.22s ease, transform 0.22s ease;
}
.sub-toast-fade-enter,
.sub-toast-fade-leave-to {
  opacity: 0;
  transform: translateX(-50%) translateY(-8px);
}
.sub-billing-tabstrip {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  margin: 16px 0 12px;
}
.sub-billing-tab {
  padding: 10px 18px;
  border-radius: 999px;
  border: 1px solid #cbd5e1;
  background: #fff;
  color: #334155;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.15s, border-color 0.15s, color 0.15s;
}
.sub-billing-tab:hover {
  border-color: #94a3b8;
}
.sub-billing-tab--active {
  background: #0f172a;
  color: #f8fafc;
  border-color: #0f172a;
}
.sub-referral-wrap {
  display: flex;
  flex-direction: column;
  gap: 16px;
  margin-top: 4px;
}
.sub-referral-block .sub-data-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
}
.sub-data-table th,
.sub-data-table td {
  padding: 10px 12px;
  text-align: left;
  border-bottom: 1px solid var(--sub-panel-border);
  color: var(--sub-on-panel);
}
.sub-data-table th {
  font-size: 12px;
  color: var(--sub-on-panel-dim);
  font-weight: 600;
}
.sub-table-scroll {
  overflow-x: auto;
  margin-top: 8px;
}
.sub-mono {
  font-size: 12px;
  color: var(--sub-on-panel-muted);
}
.sub-footnote {
  margin-top: 20px;
  font-size: 12px;
  color: #64748b;
  line-height: 1.55;
  max-width: 820px;
}
</style>
