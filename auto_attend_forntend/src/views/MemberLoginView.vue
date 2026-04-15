<template>
  <div class="login-page member-login-page">
    <aside class="login-left" aria-hidden="true">
      <div class="login-left-bg"></div>
      <div class="login-left-content">
        <div class="brand-logo-row">
          <div class="brand-icon-box" aria-hidden="true">
            <img class="brand-icon-img" src="/brand-logo.svg" width="40" height="40" alt="">
          </div>
          <span class="brand-name">流帮 Project</span>
        </div>
        <p class="brand-tagline">{{ $t('memberLogin.tagline') }}</p>
      </div>
    </aside>

    <main class="login-right">
      <div class="login-top-bar">
        <select v-model="locale" class="lang-select" @change="onLocaleChange">
          <option v-for="opt in localeOptions" :key="opt.value" :value="opt.value">{{ opt.label }}</option>
        </select>
      </div>

      <div class="login-form-area">
        <div class="login-form-card">
          <h1 class="form-heading">{{ $t('memberLogin.title') }}</h1>
          <p class="form-subtitle">{{ loginSubtitle }}</p>

          <div v-if="smsEnabled" class="login-method-tabs" role="tablist">
            <button
              type="button"
              role="tab"
              :aria-selected="loginMethod === 'password'"
              class="method-tab"
              :class="{ active: loginMethod === 'password' }"
              @click="setLoginMethod('password')"
            >
              {{ $t('memberLogin.tabEmailPassword') }}
            </button>
            <button
              type="button"
              role="tab"
              :aria-selected="loginMethod === 'sms'"
              class="method-tab"
              :class="{ active: loginMethod === 'sms' }"
              @click="setLoginMethod('sms')"
            >
              {{ $t('memberLogin.tabSms') }}
            </button>
          </div>

          <form @submit.prevent="onSubmit" class="login-form">
            <template v-if="!smsEnabled || loginMethod === 'password'">
              <div class="field-group">
                <label class="field-label">{{ $t('memberLogin.email') }}</label>
                <input
                  v-model="form.email"
                  type="email"
                  class="phone-input"
                  style="width:100%;box-sizing:border-box;padding:10px 12px;border-radius:8px;border:1px solid var(--border-primary);"
                  autocomplete="username"
                  :placeholder="$t('memberLogin.emailHint')"
                  required
                >
              </div>
              <div class="field-group">
                <label class="field-label">{{ $t('login.password') }}</label>
                <input
                  v-model="form.password"
                  type="password"
                  class="password-input"
                  style="width:100%;box-sizing:border-box;padding:10px 12px;border-radius:8px;border:1px solid var(--border-primary);"
                  autocomplete="current-password"
                  required
                >
              </div>
            </template>

            <template v-else>
              <div class="field-group">
                <label class="field-label">{{ $t('login.phone') }}</label>
                <div class="phone-input-wrap">
                  <span class="phone-prefix">+86</span>
                  <input
                    v-model="form.phone"
                    type="text"
                    class="phone-input"
                    autocomplete="tel"
                    :placeholder="$t('login.phoneHint')"
                    required
                  >
                </div>
              </div>
              <div class="field-group">
                <label class="field-label">{{ $t('login.smsCode') }}</label>
                <div class="sms-row">
                  <input
                    v-model="form.smsCode"
                    type="text"
                    inputmode="numeric"
                    maxlength="6"
                    class="sms-input"
                    :placeholder="$t('login.smsCodeHint')"
                  >
                  <button
                    type="button"
                    class="sms-send-btn"
                    :disabled="smsCooldown > 0 || smsSending"
                    @click="sendLoginSms"
                  >
                    {{ smsCooldown > 0 ? $t('login.sendSmsWait', { n: smsCooldown }) : (smsSending ? '…' : $t('login.sendSms')) }}
                  </button>
                </div>
              </div>
            </template>

            <div v-if="error" class="error-msg">{{ error }}</div>
            <button type="submit" class="submit-btn" :disabled="loading">
              <span v-if="loading" class="spinner"></span>
              <span>{{ loading ? $t('login.submitting') : $t('login.submit') }}</span>
            </button>
          </form>

          <div class="divider-row">
            <span class="divider-line"></span>
            <span class="divider-text">{{ $t('memberLogin.or') }}</span>
            <span class="divider-line"></span>
          </div>
          <div class="alt-action">
            <router-link :to="{ name: 'login' }" class="register-link">{{ $t('memberLogin.goAdminLogin') }}</router-link>
          </div>
          <div class="back-link-row">
            <router-link :to="{ name: 'landing' }" class="back-link">&larr; {{ $t('login.backLanding') }}</router-link>
          </div>
        </div>
      </div>
    </main>
  </div>
</template>

<script>
import { setStoredCollabActingUserId } from '@/utils/collabActingUser'
import { localeOptions, setLocale } from '../locales'

export default {
  name: 'MemberLoginView',
  data () {
    return {
      localeOptions,
      locale: this.$i18n.locale,
      form: {
        email: '',
        password: '',
        phone: '',
        smsCode: ''
      },
      loading: false,
      error: '',
      smsEnabled: false,
      smsResendInterval: 60,
      smsCooldown: 0,
      smsSending: false,
      smsTimer: null,
      loginMethod: 'password'
    }
  },
  computed: {
    loginSubtitle () {
      if (!this.smsEnabled) return this.$t('memberLogin.subtitlePasswordOnly')
      return this.loginMethod === 'sms'
        ? this.$t('memberLogin.subtitleSms')
        : this.$t('memberLogin.subtitleEmail')
    }
  },
  mounted () {
    this.loadSmsConfig()
  },
  beforeDestroy () {
    if (this.smsTimer) clearInterval(this.smsTimer)
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
          if (!this.smsEnabled) this.loginMethod = 'password'
        }
      } catch (e) {
        this.smsEnabled = false
        this.loginMethod = 'password'
      }
    },
    setLoginMethod (m) {
      if (m === this.loginMethod) return
      this.loginMethod = m
      this.error = ''
      if (m === 'password') {
        this.form.smsCode = ''
        this.form.phone = ''
      } else {
        this.form.password = ''
        this.form.email = ''
      }
    },
    sendLoginSms () {
      if (this.smsCooldown > 0 || this.smsSending) return
      const phone = (this.form.phone || '').trim()
      if (!phone) {
        this.error = this.$t('login.phoneHint')
        return
      }
      this.error = ''
      this.smsSending = true
      this.$http.post('/collab/auth/sms/send-login', { phone })
        .then(resp => {
          if (resp.data && resp.data.code === 0) {
            this.startSmsCooldown()
          } else {
            this.error = (resp.data && resp.data.message) || this.$t('login.smsSendFailed')
          }
        })
        .catch(e => {
          const msg = e.response && e.response.data && e.response.data.message
          this.error = msg || this.$t('login.smsSendFailed')
        })
        .finally(() => { this.smsSending = false })
    },
    startSmsCooldown () {
      this.smsCooldown = this.smsResendInterval
      if (this.smsTimer) clearInterval(this.smsTimer)
      this.smsTimer = setInterval(() => {
        this.smsCooldown--
        if (this.smsCooldown <= 0) {
          clearInterval(this.smsTimer)
          this.smsTimer = null
          this.smsCooldown = 0
        }
      }, 1000)
    },
    onLocaleChange () {
      setLocale(this.locale)
    },
    async onSubmit () {
      this.error = ''
      const useSms = this.smsEnabled && this.loginMethod === 'sms'
      if (useSms) {
        const sc = (this.form.smsCode || '').trim()
        if (!/^\d{6}$/.test(sc)) {
          this.error = this.$t('login.smsRequired')
          return
        }
      } else {
        const em = (this.form.email || '').trim()
        const pwd = (this.form.password || '').trim()
        if (!em || !pwd) {
          this.error = this.$t('memberLogin.needEmailPassword')
          return
        }
      }
      this.loading = true
      try {
        let payload
        if (useSms) {
          payload = { email: this.form.phone.trim(), smsCode: this.form.smsCode.trim() }
        } else {
          payload = { email: this.form.email.trim(), password: this.form.password }
        }
        const resp = await this.$http.post('/collab/auth/login', payload)
        if (resp.data && resp.data.code === 0) {
          window.localStorage.setItem('autoattend_collab_token', resp.data.data.token)
          setStoredCollabActingUserId(null)
          window.localStorage.removeItem('autoattend_token')
          this.$router.push({ name: 'member-home' })
          return
        }
        this.error = (resp.data && resp.data.message) || this.$t('login.failed')
      } catch (e) {
        this.error = this.$t('login.failed')
      } finally {
        this.loading = false
      }
    }
  }
}
</script>

<style scoped>
.member-login-page {
  min-height: 100vh;
  display: flex;
  font-family: var(--font-family);
}
.member-login-page .login-left {
  position: relative;
  width: 42%;
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
}
.member-login-page .login-left-bg {
  position: absolute;
  inset: 0;
  background: linear-gradient(160deg, #0f172a 0%, #1456f0 50%, #7c3aed 100%);
  z-index: 0;
}
.member-login-page .login-left-content {
  position: relative;
  z-index: 1;
  padding: 40px;
  color: #fff;
}
.member-login-page .brand-logo-row {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}
.member-login-page .brand-name {
  font-size: 20px;
  font-weight: 600;
}
.member-login-page .brand-tagline {
  margin: 0;
  opacity: 0.9;
  font-size: 14px;
}
.member-login-page .login-right {
  flex: 1;
  min-height: 100vh;
  background: var(--bg-page, #f8fafc);
  display: flex;
  flex-direction: column;
}
.member-login-page .login-top-bar {
  display: flex;
  justify-content: flex-end;
  padding: 16px 24px;
}
.member-login-page .login-form-area {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
}
.member-login-page .login-form-card {
  width: 100%;
  max-width: 420px;
  padding: 28px;
  background: var(--bg-card, #fff);
  border-radius: 12px;
  box-shadow: 0 4px 24px rgba(15, 23, 42, 0.08);
  border: 1px solid var(--border-primary, #e2e8f0);
}
.member-login-page .form-heading {
  margin: 0 0 8px;
  font-size: 22px;
}
.member-login-page .form-subtitle {
  margin: 0 0 20px;
  font-size: 14px;
  color: var(--text-secondary, #64748b);
}
@media (max-width: 900px) {
  .member-login-page .login-left { display: none; }
}
.member-login-page .login-form-card {
  max-width: 420px;
}
.login-method-tabs {
  display: flex;
  gap: 8px;
  margin-bottom: 16px;
}
.method-tab {
  flex: 1;
  padding: 8px 12px;
  border: 1px solid var(--border-primary);
  background: var(--bg-card);
  border-radius: 8px;
  cursor: pointer;
  font-size: 14px;
}
.method-tab.active {
  border-color: var(--brand-blue);
  background: #f0f5ff;
  color: var(--brand-blue);
}
.phone-input-wrap {
  display: flex;
  align-items: center;
  border: 1px solid var(--border-primary);
  border-radius: 8px;
  overflow: hidden;
}
.phone-prefix {
  padding: 10px 12px;
  background: var(--bg-muted);
  font-size: 14px;
}
.phone-input-wrap .phone-input {
  flex: 1;
  border: none;
  padding: 10px 12px;
}
.sms-row {
  display: flex;
  gap: 8px;
  align-items: center;
}
.sms-input {
  flex: 1;
  padding: 10px 12px;
  border-radius: 8px;
  border: 1px solid var(--border-primary);
}
.sms-send-btn {
  white-space: nowrap;
  padding: 10px 12px;
  border-radius: 8px;
  border: 1px solid var(--brand-blue);
  background: #fff;
  color: var(--brand-blue);
  cursor: pointer;
}
.field-group {
  margin-bottom: 14px;
}
.field-label {
  display: block;
  margin-bottom: 6px;
  font-size: 13px;
  color: var(--text-secondary);
}
.error-msg {
  color: #c00;
  font-size: 13px;
  margin-bottom: 8px;
}
.submit-btn {
  width: 100%;
  padding: 12px;
  margin-top: 8px;
  border: none;
  border-radius: 8px;
  background: var(--brand-blue);
  color: #fff;
  font-size: 15px;
  cursor: pointer;
}
.submit-btn:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}
.divider-row {
  display: flex;
  align-items: center;
  gap: 12px;
  margin: 20px 0 12px;
  color: var(--text-tertiary);
  font-size: 12px;
}
.divider-line {
  flex: 1;
  height: 1px;
  background: var(--border-primary);
}
.alt-action {
  text-align: center;
}
.register-link {
  color: var(--brand-blue);
  text-decoration: none;
}
.back-link-row {
  text-align: center;
  margin-top: 12px;
}
.back-link {
  color: var(--text-tertiary);
  font-size: 13px;
  text-decoration: none;
}
</style>
