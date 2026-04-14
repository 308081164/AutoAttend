<template>
  <div class="member-home">
    <div class="page-header">
      <h1 class="page-title">{{ $t('memberHome.title') }}</h1>
      <div class="header-actions">
        <span class="user-info">{{ userEmail }}</span>
        <button class="link-button" @click="logout">{{ $t('app.logout') }}</button>
      </div>
    </div>

    <section class="section account-section">
      <h2 class="section-title">{{ $t('memberHome.accountSettings') }}</h2>
      <div class="account-card">
        <div class="account-row">
          <span class="account-label">{{ $t('memberHome.phoneStatus') }}</span>
          <span v-if="phoneBound" class="account-value account-ok">{{ $t('memberHome.phoneBound') }}</span>
          <span v-else class="account-value account-warn">{{ $t('memberHome.phoneNotBound') }}</span>
        </div>
        <template v-if="smsEnabled && !phoneBound">
          <p class="section-desc">{{ $t('memberHome.bindPhoneHint') }}</p>
          <div class="bind-row">
            <input v-model="bindForm.phone" type="text" class="bind-input" :placeholder="$t('dashboard.bindPhonePlaceholder')">
            <input v-model="bindForm.smsCode" type="text" maxlength="6" class="bind-input bind-code" :placeholder="$t('dashboard.bindPhoneCodePlaceholder')">
            <button type="button" class="secondary-btn" :disabled="bindCooldown > 0 || bindSending" @click="sendBindSms">
              {{ bindCooldown > 0 ? $t('login.sendSmsWait', { n: bindCooldown }) : $t('login.sendSms') }}
            </button>
            <button type="button" class="primary-btn" :disabled="bindSaving" @click="submitBind">{{ $t('dashboard.bindPhoneSubmit') }}</button>
          </div>
          <p v-if="bindMsg" class="bind-msg">{{ bindMsg }}</p>
        </template>
        <div class="pwd-block">
          <h3 class="pwd-title">{{ $t('memberHome.changePassword') }}</h3>
          <div class="pwd-row">
            <input v-model="pwdForm.current" type="password" class="bind-input" :placeholder="$t('memberHome.currentPassword')">
            <input v-model="pwdForm.next" type="password" class="bind-input" :placeholder="$t('memberHome.newPassword')">
            <button type="button" class="primary-btn" :disabled="pwdSaving" @click="changePassword">{{ $t('memberHome.savePassword') }}</button>
          </div>
          <p v-if="pwdMsg" class="bind-msg">{{ pwdMsg }}</p>
        </div>
      </div>
    </section>

    <section class="section stats-section">
      <h2 class="section-title">{{ $t('memberHome.myStats') }}</h2>
      <div v-if="statsLoading" class="placeholder">{{ $t('collab.loading') }}</div>
      <div v-else class="stats-cards">
        <div class="stat-card">
          <div class="stat-value">{{ overview.projectCount ?? 0 }}</div>
          <div class="stat-label">{{ $t('memberHome.projectCount') }}</div>
        </div>
        <div class="stat-card">
          <div class="stat-value">{{ overview.commitCountTotal ?? 0 }}</div>
          <div class="stat-label">{{ $t('memberHome.commitCountTotal') }}</div>
        </div>
        <div class="stat-card">
          <div class="stat-value">{{ overview.commitCountLast7Days ?? 0 }}</div>
          <div class="stat-label">{{ $t('memberHome.commitCountLast7Days') }}</div>
        </div>
      </div>
    </section>

    <section class="section">
      <h2 class="section-title">{{ $t('memberHome.myProjects') }}</h2>
      <p class="section-desc">{{ $t('collab.selectProject') }}</p>
      <router-link to="/collab/projects" class="primary-button">{{ $t('memberHome.goToProjects') }}</router-link>
    </section>
  </div>
</template>

<script>
export default {
  name: 'MemberHomeView',
  data () {
    return {
      userEmail: '',
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
    this.loadMe()
    this.loadOverview()
  },
  beforeDestroy () {
    if (this.bindTimer) clearInterval(this.bindTimer)
  },
  methods: {
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
  max-width: 640px;
  margin: 0 auto;
  padding: var(--space-xl);
  background: var(--bg-page);
  min-height: 100vh;
  box-sizing: border-box;
}

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: var(--space-xxl);
}

.page-title {
  margin: 0;
  font-size: 22px;
  font-weight: 600;
  color: var(--text-primary);
}

.header-actions {
  display: flex;
  align-items: center;
  gap: var(--space-md);
}

.user-info {
  font-size: 13px;
  color: var(--text-tertiary);
}

.link-button {
  border: none;
  background: transparent;
  color: var(--brand-blue);
  cursor: pointer;
  font-size: 13px;
  padding: 4px var(--space-sm);
  transition: opacity 0.2s;
}

.link-button:hover {
  opacity: 0.75;
  text-decoration: underline;
}

.section {
  margin-bottom: var(--space-xxl);
}

.section-title {
  margin: 0 0 var(--space-md);
  font-size: 16px;
  font-weight: 600;
  color: var(--text-primary);
}

.section-desc {
  margin: 0 0 var(--space-md);
  font-size: 14px;
  color: var(--text-secondary);
}

.account-card {
  padding: var(--space-lg);
  border-radius: var(--radius-lg);
  background: var(--bg-card);
  border: 1px solid var(--border-primary);
}
.account-row {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 8px;
}
.account-label {
  font-size: 13px;
  color: var(--text-secondary);
}
.account-value { font-size: 14px; }
.account-ok { color: #0a7a2f; }
.account-warn { color: #b45309; }
.bind-row, .pwd-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
  margin-top: 8px;
}
.bind-input {
  padding: 8px 12px;
  border-radius: 8px;
  border: 1px solid var(--border-primary);
  font-size: 14px;
}
.bind-code { width: 100px; }
.primary-btn, .secondary-btn {
  padding: 8px 14px;
  border-radius: 8px;
  font-size: 14px;
  cursor: pointer;
  border: 1px solid var(--brand-blue);
}
.primary-btn {
  background: var(--brand-blue);
  color: #fff;
}
.secondary-btn {
  background: #fff;
  color: var(--brand-blue);
}
.bind-msg { margin: 8px 0 0; font-size: 13px; color: var(--text-secondary); }
.pwd-block { margin-top: var(--space-lg); padding-top: var(--space-lg); border-top: 1px solid var(--border-primary); }
.pwd-title { margin: 0 0 8px; font-size: 14px; font-weight: 600; }

.stats-cards {
  display: flex;
  gap: var(--space-lg);
  flex-wrap: wrap;
}

.stat-card {
  flex: 1;
  min-width: 120px;
  padding: var(--space-lg);
  border-radius: var(--radius-lg);
  background: var(--bg-card);
  border: 1px solid var(--border-primary);
  box-shadow: var(--shadow-sm);
  transition: box-shadow 0.2s;
}

.stat-card:hover {
  box-shadow: var(--shadow-md);
}

.stat-value {
  font-size: 24px;
  font-weight: 600;
  color: var(--text-primary);
}

.stat-label {
  font-size: 13px;
  color: var(--text-tertiary);
  margin-top: 4px;
}

.primary-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: var(--space-sm) var(--space-xl);
  border-radius: var(--radius-md);
  background: var(--brand-blue);
  color: #fff;
  text-decoration: none;
  font-size: 14px;
  font-weight: 500;
  transition: opacity 0.2s;
  box-shadow: var(--shadow-sm);
}

.primary-button:hover {
  opacity: 0.85;
}

.placeholder {
  padding: var(--space-lg);
  color: var(--text-tertiary);
  font-size: 14px;
}
</style>
