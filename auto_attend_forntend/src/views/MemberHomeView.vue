<template>
  <div class="member-home">
    <section class="mh-hero">
      <div class="mh-hero-text">
        <p class="mh-eyebrow">{{ $t('memberHome.heroEyebrow') }}</p>
        <h1 class="mh-title">{{ $t('memberHome.heroTitle') }}</h1>
        <p class="mh-lead">{{ $t('memberHome.heroLead') }}</p>
      </div>
      <div class="mh-hero-meta">
        <div class="mh-email-pill">
          <span class="mh-email-label">{{ userEmail || '—' }}</span>
        </div>
        <button type="button" class="mh-logout" @click="logout">{{ $t('app.logout') }}</button>
      </div>
    </section>

    <div class="mh-grid">
      <section class="mh-card mh-card--account">
        <div class="mh-card-head">
          <h2 class="mh-card-title">{{ $t('memberHome.accountSettings') }}</h2>
        </div>
        <div class="mh-account-inner">
          <div class="mh-phone-row">
            <span class="mh-muted">{{ $t('memberHome.phoneStatus') }}</span>
            <span v-if="phoneBound" class="mh-badge mh-badge--ok">{{ $t('memberHome.phoneBound') }}</span>
            <span v-else class="mh-badge mh-badge--warn">{{ $t('memberHome.phoneNotBound') }}</span>
          </div>
          <template v-if="smsEnabled && !phoneBound">
            <p class="mh-hint">{{ $t('memberHome.bindPhoneHint') }}</p>
            <div class="mh-form-row">
              <input v-model="bindForm.phone" type="text" class="mh-input" :placeholder="$t('dashboard.bindPhonePlaceholder')">
              <input v-model="bindForm.smsCode" type="text" maxlength="6" class="mh-input mh-input--code" :placeholder="$t('dashboard.bindPhoneCodePlaceholder')">
              <button type="button" class="mh-btn mh-btn--ghost" :disabled="bindCooldown > 0 || bindSending" @click="sendBindSms">
                {{ bindCooldown > 0 ? $t('login.sendSmsWait', { n: bindCooldown }) : $t('login.sendSms') }}
              </button>
              <button type="button" class="mh-btn mh-btn--primary" :disabled="bindSaving" @click="submitBind">{{ $t('dashboard.bindPhoneSubmit') }}</button>
            </div>
            <p v-if="bindMsg" class="mh-msg">{{ bindMsg }}</p>
          </template>
          <div class="mh-divider" />
          <h3 class="mh-subtitle">{{ $t('memberHome.changePassword') }}</h3>
          <div class="mh-form-row mh-form-row--pwd">
            <input v-model="pwdForm.current" type="password" class="mh-input" :placeholder="$t('memberHome.currentPassword')">
            <input v-model="pwdForm.next" type="password" class="mh-input" :placeholder="$t('memberHome.newPassword')">
            <button type="button" class="mh-btn mh-btn--primary" :disabled="pwdSaving" @click="changePassword">{{ $t('memberHome.savePassword') }}</button>
          </div>
          <p v-if="pwdMsg" class="mh-msg">{{ pwdMsg }}</p>
        </div>
      </section>

      <section class="mh-card mh-card--stats">
        <div class="mh-card-head">
          <h2 class="mh-card-title">{{ $t('memberHome.myStats') }}</h2>
          <p class="mh-card-sub">{{ $t('memberHome.statsHint') }}</p>
        </div>
        <div v-if="linkedIdentities.length > 1" class="mh-identity-bar">
          <label class="mh-identity-label" for="mh-acting-select">{{ $t('collab.identityLabel') }}</label>
          <select
            id="mh-acting-select"
            v-model.number="actingUserId"
            class="mh-identity-select"
            @change="onActingIdentityChange"
          >
            <option v-for="it in linkedIdentities" :key="it.id" :value="it.id">
              {{ formatIdentityOption(it) }}
            </option>
          </select>
        </div>
        <div v-if="statsLoading" class="mh-placeholder">{{ $t('collab.loading') }}</div>
        <div v-else class="mh-stat-grid">
          <div class="mh-stat">
            <div class="mh-stat-icon mh-stat-icon--a" aria-hidden="true">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M22 19a2 2 0 01-2 2H4a2 2 0 01-2-2V5a2 2 0 012-2h5l2 3h9a2 2 0 012 2z"/></svg>
            </div>
            <div class="mh-stat-body">
              <div class="mh-stat-value">{{ overview.projectCount ?? 0 }}</div>
              <div class="mh-stat-label">{{ $t('memberHome.projectCount') }}</div>
            </div>
          </div>
          <div class="mh-stat">
            <div class="mh-stat-icon mh-stat-icon--b" aria-hidden="true">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="16 18 22 12 16 6"/><polyline points="8 6 2 12 8 18"/></svg>
            </div>
            <div class="mh-stat-body">
              <div class="mh-stat-value">{{ overview.commitCountTotal ?? 0 }}</div>
              <div class="mh-stat-label">{{ $t('memberHome.commitCountTotal') }}</div>
            </div>
          </div>
          <div class="mh-stat">
            <div class="mh-stat-icon mh-stat-icon--c" aria-hidden="true">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M3 3v18h18"/><path d="M18 17V9"/><path d="M13 17V5"/><path d="M8 17v-3"/></svg>
            </div>
            <div class="mh-stat-body">
              <div class="mh-stat-value">{{ overview.commitCountLast7Days ?? 0 }}</div>
              <div class="mh-stat-label">{{ $t('memberHome.commitCountLast7Days') }}</div>
            </div>
          </div>
        </div>
      </section>
    </div>

    <section class="mh-cta">
      <div class="mh-cta-inner">
        <div>
          <h2 class="mh-cta-title">{{ $t('memberHome.projectsCardTitle') }}</h2>
          <p class="mh-cta-desc">{{ $t('memberHome.projectsCardDesc') }}</p>
        </div>
        <router-link to="/collab/projects" class="mh-btn mh-btn--light">{{ $t('memberHome.goToProjects') }}</router-link>
      </div>
    </section>
  </div>
</template>

<script>
import { getStoredCollabActingUserId, setStoredCollabActingUserId } from '@/utils/collabActingUser'

export default {
  name: 'MemberHomeView',
  data () {
    return {
      userEmail: '',
      sessionUserId: null,
      linkedIdentities: [],
      actingUserId: null,
      phoneBound: false,
      overview: {},
      statsLoading: true,
      smsEnabled: false,
      smsResendInterval: 60,
      bindForm: { phone: '', smsCode: '' },
      bindMsg: '',
      bindSaving: false,
      bindSending: false,
      bindCooldown: 0,
      bindTimer: null,
      pwdForm: { current: '', next: '' },
      pwdMsg: '',
      pwdSaving: false
    }
  },
  created () {
    this.loadSmsConfig()
    this.loadMe().then(() => {
      this.loadLinkedIdentities().then(() => this.loadOverview())
    })
  },
  beforeDestroy () {
    if (this.bindTimer) clearInterval(this.bindTimer)
  },
  methods: {
    formatIdentityOption (it) {
      const em = (it.email || '').trim()
      const name = (it.name || '').trim()
      if (name && em && name !== em) return `${name} (${em})`
      return em || name || ('#' + it.id)
    },
    async loadLinkedIdentities () {
      try {
        const resp = await this.$http.get('/collab/auth/linked-identities')
        if (resp.data && resp.data.code === 0) {
          const d = resp.data.data || {}
          this.linkedIdentities = Array.isArray(d.items) ? d.items : []
          const apiActing = d.actingUserId != null ? Number(d.actingUserId) : null
          const stored = getStoredCollabActingUserId()
          const storedNum = stored != null ? Number(stored) : null
          const ids = new Set(this.linkedIdentities.map(x => x.id))
          if (storedNum != null && ids.has(storedNum)) {
            this.actingUserId = storedNum
          } else if (apiActing != null && ids.has(apiActing)) {
            this.actingUserId = apiActing
          } else if (this.sessionUserId != null && ids.has(this.sessionUserId)) {
            this.actingUserId = this.sessionUserId
          } else if (this.linkedIdentities.length) {
            this.actingUserId = this.linkedIdentities[0].id
          }
          if (this.actingUserId != null) {
            setStoredCollabActingUserId(this.actingUserId)
          }
        }
      } catch (e) { /* ignore */ }
    },
    onActingIdentityChange () {
      setStoredCollabActingUserId(this.actingUserId)
      this.loadOverview()
    },
    async loadSmsConfig () {
      try {
        const resp = await this.$http.get('/collab/auth/sms/config')
        if (resp.data && resp.data.code === 0 && resp.data.data) {
          this.smsEnabled = !!resp.data.data.enabled
          if (resp.data.data.resendIntervalSeconds != null) {
            this.smsResendInterval = Number(resp.data.data.resendIntervalSeconds) || 60
          }
        }
      } catch (e) { this.smsEnabled = false }
    },
    async loadMe () {
      try {
        const resp = await this.$http.get('/collab/auth/me')
        if (resp.data && resp.data.code === 0) {
          const d = resp.data.data || {}
          this.userEmail = d.email || ''
          this.sessionUserId = d.id != null ? Number(d.id) : null
          this.phoneBound = d.phoneBound === true
        }
      } catch (e) {
        if (e.response && e.response.status === 401) this.$router.push({ name: 'member-login' })
      }
    },
    async loadOverview () {
      this.statsLoading = true
      try {
        const resp = await this.$http.get('/collab/stats/overview')
        if (resp.data && resp.data.code === 0) {
          this.overview = resp.data.data || {}
        }
      } catch (e) {
        if (e.response && e.response.status === 401) this.$router.push({ name: 'member-login' })
      } finally {
        this.statsLoading = false
      }
    },
    startBindCooldown () {
      this.bindCooldown = this.smsResendInterval
      if (this.bindTimer) clearInterval(this.bindTimer)
      this.bindTimer = setInterval(() => {
        this.bindCooldown--
        if (this.bindCooldown <= 0) {
          clearInterval(this.bindTimer)
          this.bindTimer = null
          this.bindCooldown = 0
        }
      }, 1000)
    },
    async sendBindSms () {
      if (this.bindCooldown > 0 || this.bindSending) return
      const phone = (this.bindForm.phone || '').trim()
      if (!phone) {
        this.bindMsg = this.$t('dashboard.bindPhoneNeedPhone')
        return
      }
      this.bindMsg = ''
      this.bindSending = true
      try {
        const r = await this.$http.post('/collab/auth/sms/send', { phone, purpose: 'bind_phone' })
        if (r.data && r.data.code === 0) {
          this.startBindCooldown()
        } else {
          this.bindMsg = (r.data && r.data.message) || this.$t('login.smsSendFailed')
        }
      } catch (e) {
        this.bindMsg = (e.response && e.response.data && e.response.data.message) || this.$t('login.smsSendFailed')
      } finally {
        this.bindSending = false
      }
    },
    async submitBind () {
      const phone = (this.bindForm.phone || '').trim()
      const code = (this.bindForm.smsCode || '').trim()
      if (!phone || !/^\d{6}$/.test(code)) {
        this.bindMsg = this.$t('dashboard.bindPhoneNeedCode')
        return
      }
      this.bindSaving = true
      this.bindMsg = ''
      try {
        const r = await this.$http.post('/collab/auth/bind-phone', { phone, smsCode: code })
        if (r.data && r.data.code === 0) {
          this.bindMsg = this.$t('dashboard.bindPhoneOk')
          await this.loadMe()
        } else {
          this.bindMsg = (r.data && r.data.message) || this.$t('dashboard.bindPhoneFail')
        }
      } catch (e) {
        this.bindMsg = (e.response && e.response.data && e.response.data.message) || this.$t('dashboard.bindPhoneFail')
      } finally {
        this.bindSaving = false
      }
    },
    async changePassword () {
      const cur = (this.pwdForm.current || '').trim()
      const n = (this.pwdForm.next || '').trim()
      if (!cur || !n) {
        this.pwdMsg = this.$t('memberHome.passwordBothRequired')
        return
      }
      this.pwdSaving = true
      this.pwdMsg = ''
      try {
        const r = await this.$http.post('/collab/auth/password', { currentPassword: cur, newPassword: n })
        if (r.data && r.data.code === 0) {
          this.pwdMsg = this.$t('memberHome.passwordChanged')
          this.pwdForm = { current: '', next: '' }
        } else {
          this.pwdMsg = (r.data && r.data.message) || this.$t('memberHome.passwordFail')
        }
      } catch (e) {
        this.pwdMsg = (e.response && e.response.data && e.response.data.message) || this.$t('memberHome.passwordFail')
      } finally {
        this.pwdSaving = false
      }
    },
    logout () {
      window.localStorage.removeItem('autoattend_collab_token')
      window.localStorage.removeItem('autoattend_token')
      this.$router.push({ name: 'member-login' })
    }
  }
}
</script>

<style scoped>
.member-home {
  max-width: 1100px;
  margin: 0 auto;
  padding: 24px 20px 48px;
  box-sizing: border-box;
}

.mh-hero {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-end;
  justify-content: space-between;
  gap: 20px;
  margin-bottom: 28px;
  padding: 28px 28px 24px;
  border-radius: 20px;
  background: linear-gradient(135deg, #0f172a 0%, #1e3a5f 45%, #172554 100%);
  color: #f8fafc;
  box-shadow: 0 20px 50px rgba(15, 23, 42, 0.25);
  border: 1px solid rgba(148, 163, 184, 0.15);
}

.mh-eyebrow {
  margin: 0 0 8px;
  font-size: 12px;
  font-weight: 600;
  letter-spacing: 0.12em;
  text-transform: uppercase;
  color: #7dd3fc;
  opacity: 0.95;
}

.mh-title {
  margin: 0 0 10px;
  font-size: 26px;
  font-weight: 700;
  letter-spacing: -0.02em;
  line-height: 1.2;
}

.mh-lead {
  margin: 0;
  max-width: 560px;
  font-size: 14px;
  line-height: 1.65;
  color: #cbd5e1;
}

.mh-hero-meta {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 10px;
}

.mh-email-pill {
  padding: 8px 14px;
  border-radius: 999px;
  background: rgba(15, 23, 42, 0.45);
  border: 1px solid rgba(148, 163, 184, 0.25);
  font-size: 13px;
  max-width: 280px;
  word-break: break-all;
  text-align: right;
}

.mh-email-label {
  color: #e2e8f0;
}

.mh-logout {
  border: 1px solid rgba(248, 250, 252, 0.35);
  background: transparent;
  color: #f1f5f9;
  padding: 8px 16px;
  border-radius: 10px;
  font-size: 13px;
  cursor: pointer;
  transition: background 0.2s, border-color 0.2s;
}
.mh-logout:hover {
  background: rgba(255, 255, 255, 0.08);
}

.mh-grid {
  display: grid;
  grid-template-columns: 1fr;
  gap: 20px;
}
@media (min-width: 960px) {
  .mh-grid {
    grid-template-columns: 1.15fr 0.85fr;
    align-items: stretch;
  }
}

.mh-card {
  border-radius: 16px;
  background: var(--bg-card, #fff);
  border: 1px solid var(--border-primary, #e2e8f0);
  box-shadow: 0 4px 24px rgba(15, 23, 42, 0.06);
  overflow: hidden;
}

.mh-card-head {
  padding: 18px 20px 0;
}

.mh-card-title {
  margin: 0;
  font-size: 17px;
  font-weight: 700;
  color: var(--text-primary, #0f172a);
}

.mh-card-sub {
  margin: 6px 0 0;
  font-size: 13px;
  color: var(--text-tertiary, #64748b);
  line-height: 1.45;
}

.mh-account-inner {
  padding: 16px 20px 22px;
}

.mh-phone-row {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px;
  margin-bottom: 8px;
}

.mh-muted {
  font-size: 13px;
  color: var(--text-secondary, #475569);
}

.mh-badge {
  font-size: 12px;
  padding: 4px 10px;
  border-radius: 999px;
  font-weight: 500;
}
.mh-badge--ok {
  background: #ecfdf5;
  color: #047857;
  border: 1px solid #a7f3d0;
}
.mh-badge--warn {
  background: #fffbeb;
  color: #b45309;
  border: 1px solid #fde68a;
}

.mh-hint {
  margin: 10px 0 12px;
  font-size: 13px;
  line-height: 1.55;
  color: var(--text-secondary, #475569);
}

.mh-form-row {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  align-items: center;
}

.mh-input {
  padding: 10px 12px;
  border-radius: 10px;
  border: 1px solid var(--border-primary, #e2e8f0);
  font-size: 14px;
  min-width: 160px;
  flex: 1;
}
.mh-input--code {
  min-width: 100px;
  flex: 0 0 100px;
}

.mh-form-row--pwd .mh-input {
  max-width: 220px;
}

.mh-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 10px 16px;
  border-radius: 10px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  border: none;
  text-decoration: none;
  transition: opacity 0.2s, transform 0.15s;
  white-space: nowrap;
}
.mh-btn:disabled {
  opacity: 0.55;
  cursor: not-allowed;
}
.mh-btn--primary {
  background: linear-gradient(135deg, #2563eb, #1d4ed8);
  color: #fff;
  box-shadow: 0 4px 14px rgba(37, 99, 235, 0.35);
}
.mh-btn--ghost {
  background: #fff;
  color: #1d4ed8;
  border: 1px solid #93c5fd;
}
.mh-btn--light {
  background: #fff;
  color: #1e3a8a;
  box-shadow: 0 8px 24px rgba(15, 23, 42, 0.12);
}
.mh-btn--light:hover {
  opacity: 0.95;
  transform: translateY(-1px);
}

.mh-msg {
  margin: 10px 0 0;
  font-size: 13px;
  color: var(--text-secondary, #64748b);
}

.mh-divider {
  height: 1px;
  background: var(--border-primary, #e2e8f0);
  margin: 20px 0;
}

.mh-subtitle {
  margin: 0 0 12px;
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary, #0f172a);
}

.mh-card--stats .mh-card-head {
  padding-bottom: 4px;
}

.mh-identity-bar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px;
  padding: 0 20px 12px;
  font-size: 13px;
}

.mh-identity-label {
  color: var(--text-secondary, #64748b);
  margin: 0;
}

.mh-identity-select {
  min-width: 200px;
  padding: 6px 10px;
  border-radius: 10px;
  border: 1px solid var(--border-primary, #e2e8f0);
  background: #fff;
  color: var(--text-primary, #0f172a);
}

.mh-placeholder {
  padding: 24px 20px;
  color: var(--text-tertiary, #94a3b8);
  font-size: 14px;
}

.mh-stat-grid {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 12px 16px 22px;
}

.mh-stat {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 14px 16px;
  border-radius: 14px;
  background: linear-gradient(145deg, #f8fafc, #f1f5f9);
  border: 1px solid #e2e8f0;
}

.mh-stat-icon {
  width: 44px;
  height: 44px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.mh-stat-icon svg {
  width: 22px;
  height: 22px;
}
.mh-stat-icon--a {
  background: linear-gradient(145deg, #dbeafe, #bfdbfe);
  color: #1d4ed8;
}
.mh-stat-icon--b {
  background: linear-gradient(145deg, #e0e7ff, #c7d2fe);
  color: #4338ca;
}
.mh-stat-icon--c {
  background: linear-gradient(145deg, #d1fae5, #a7f3d0);
  color: #047857;
}

.mh-stat-value {
  font-size: 22px;
  font-weight: 700;
  color: var(--text-primary, #0f172a);
  letter-spacing: -0.02em;
  line-height: 1.1;
}

.mh-stat-label {
  font-size: 12px;
  color: var(--text-tertiary, #64748b);
  margin-top: 2px;
}

.mh-cta {
  margin-top: 24px;
  border-radius: 16px;
  padding: 2px;
  background: linear-gradient(135deg, #3b82f6, #6366f1, #8b5cf6);
  box-shadow: 0 12px 40px rgba(79, 70, 229, 0.25);
}

.mh-cta-inner {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 22px 24px;
  border-radius: 14px;
  background: linear-gradient(180deg, #1e293b 0%, #0f172a 100%);
  color: #f1f5f9;
}

.mh-cta-title {
  margin: 0 0 8px;
  font-size: 18px;
  font-weight: 700;
}

.mh-cta-desc {
  margin: 0;
  font-size: 14px;
  line-height: 1.55;
  color: #cbd5e1;
  max-width: 560px;
}
</style>
