<template>
  <div class="register-page">
    <div class="register-card">
      <h2 class="register-title">{{ $t('register.title') }}</h2>
      <form @submit.prevent="onSubmit">
        <div class="form-item">
          <label>{{ $t('register.phone') }}</label>
          <input v-model="form.phone" type="text" autocomplete="tel" required>
        </div>
        <div v-if="smsEnabled" class="form-item sms-row">
          <label>{{ $t('register.smsCode') }}</label>
          <div class="sms-inline">
            <input
              v-model="form.smsCode"
              type="text"
              inputmode="numeric"
              maxlength="6"
              autocomplete="one-time-code"
              :placeholder="$t('register.smsCodeHint')"
            >
            <button
              type="button"
              class="sms-btn"
              :disabled="smsCooldown > 0 || smsSending"
              @click="sendRegisterSms"
            >
              {{ smsCooldown > 0 ? $t('register.sendSmsWait', { n: smsCooldown }) : (smsSending ? '…' : $t('register.sendSms')) }}
            </button>
          </div>
        </div>
        <div class="form-item">
          <label>{{ $t('register.inviteCode') }}</label>
          <input v-model="form.inviteCode" type="text" autocomplete="off" :placeholder="$t('register.inviteCodeHint')">
        </div>
        <div class="form-item">
          <label>{{ $t('register.orgName') }}</label>
          <input v-model="form.orgName" type="text" :placeholder="$t('register.orgNameHint')" required>
        </div>
        <div class="form-item">
          <label>{{ $t('register.slug') }}</label>
          <input v-model="form.slug" type="text" :placeholder="$t('register.slugHint')" required>
        </div>
        <div class="form-item">
          <label>{{ $t('register.password') }}</label>
          <input v-model="form.password" type="password" autocomplete="new-password" maxlength="24" required>
        </div>
        <div v-if="error" class="error-text">{{ error }}</div>
        <button class="primary-button" type="submit" :disabled="loading">
          {{ loading ? $t('register.submitting') : $t('register.submit') }}
        </button>
        <p class="login-hint">
          <router-link to="/login">{{ $t('register.loginLink') }}</router-link>
        </p>
      </form>
    </div>
  </div>
</template>

<script>
export default {
  name: 'RegisterView',
  data () {
    return {
      form: {
        phone: '',
        orgName: '',
        slug: '',
        password: '',
        smsCode: '',
        inviteCode: ''
      },
      loading: false,
      error: '',
      smsEnabled: false,
      smsResendInterval: 60,
      smsCooldown: 0,
      smsSending: false,
      smsTimer: null
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
        const resp = await this.$http.get('/admin/auth/sms/config')
        if (resp.data && resp.data.code === 0 && resp.data.data) {
          this.smsEnabled = !!resp.data.data.enabled
          if (resp.data.data.resendIntervalSeconds != null) {
            this.smsResendInterval = Number(resp.data.data.resendIntervalSeconds) || 60
          }
        }
      } catch (e) {
        this.smsEnabled = false
      }
    },
    sendRegisterSms () {
      if (this.smsCooldown > 0 || this.smsSending) return
      const phone = this.form.phone.trim()
      if (!phone) {
        this.error = this.$t('login.phoneHint')
        return
      }
      this.error = ''
      this.smsSending = true
      this.$http.post('/admin/auth/sms/send', { phone, purpose: 'register' })
        .then(resp => {
          if (resp.data && resp.data.code === 0) {
            this.startSmsCooldown()
          } else {
            this.error = (resp.data && resp.data.message) || this.$t('register.smsSendFailed')
          }
        })
        .catch(e => {
          const msg = e.response && e.response.data && e.response.data.message
          this.error = msg || this.$t('register.smsSendFailed')
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
    async onSubmit () {
      this.error = ''
      if (this.smsEnabled) {
        const sc = (this.form.smsCode || '').trim()
        if (!/^\d{6}$/.test(sc)) {
          this.error = this.$t('register.smsRequired')
          return
        }
      }
      this.loading = true
      try {
        const body = {
          phone: this.form.phone.trim(),
          orgName: this.form.orgName.trim(),
          slug: this.form.slug.trim(),
          password: this.form.password
        }
        if (this.smsEnabled) {
          body.smsCode = (this.form.smsCode || '').trim()
        }
        const inv = (this.form.inviteCode || '').trim()
        if (inv) {
          body.inviteCode = inv
        }
        const resp = await this.$http.post('/admin/auth/register', body)
        if (resp.data && resp.data.code === 0) {
          const data = resp.data.data
          window.localStorage.setItem('autoattend_token', data.token)
          window.localStorage.setItem('autoattend_username', this.form.phone.trim())
          if (data.collabToken) {
            window.localStorage.setItem('autoattend_collab_token', data.collabToken)
          }
          this.$router.push({ name: 'dashboard' })
          return
        }
        this.error = (resp.data && resp.data.message) || this.$t('register.failed')
      } catch (e) {
        const msg = e.response && e.response.data && e.response.data.message
        this.error = msg || this.$t('register.failed')
      } finally {
        this.loading = false
      }
    }
  }
}
</script>

<style scoped>
.register-page {
  width: 100%;
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--bg-page);
  padding: var(--space-xl) 0;
}

.register-card {
  width: 100%;
  max-width: 400px;
  padding: var(--space-xxl);
  border-radius: var(--radius-lg);
  background-color: var(--bg-card);
  border: 1px solid var(--border-primary);
  box-shadow: var(--shadow-lg);
}

.register-title {
  margin: 0 0 var(--space-lg);
  font-size: var(--font-size-xl);
  text-align: center;
  color: var(--text-primary);
  font-weight: var(--font-weight-bold);
}

.form-item {
  margin-bottom: var(--space-md);
  display: flex;
  flex-direction: column;
}

label {
  font-size: var(--font-size-sm);
  margin-bottom: var(--space-xs);
  color: var(--text-secondary);
  font-weight: var(--font-weight-medium);
}

input {
  padding: var(--space-sm) var(--space-md);
  border-radius: var(--radius-sm);
  border: 1px solid var(--border-input);
  font-size: var(--font-size-base);
  color: var(--text-primary);
  background: var(--bg-input);
  transition: var(--transition-fast);
}

input:hover {
  border-color: var(--border-input-hover);
}

input:focus {
  outline: none;
  border-color: var(--border-input-focus);
  box-shadow: 0 0 0 2px rgba(20, 86, 240, 0.15);
}

input::placeholder {
  color: var(--text-tertiary);
}

.primary-button {
  width: 100%;
  margin-top: var(--space-sm);
  padding: var(--space-sm) var(--space-md);
  border-radius: var(--radius-sm);
  border: none;
  background-color: var(--brand-blue);
  color: #ffffff;
  font-size: var(--font-size-base);
  cursor: pointer;
  font-weight: var(--font-weight-medium);
  transition: var(--transition-fast);
  box-shadow: var(--shadow-btn);
}

.primary-button:hover {
  background-color: var(--brand-blue-hover);
}

.primary-button:disabled {
  opacity: 0.45;
  cursor: not-allowed;
}

.error-text {
  margin-bottom: var(--space-xs);
  font-size: var(--font-size-xs);
  color: var(--danger);
}

.login-hint {
  margin-top: var(--space-lg);
  font-size: var(--font-size-sm);
  text-align: center;
  color: var(--text-secondary);
}

.login-hint a {
  color: var(--text-link);
  text-decoration: none;
  font-weight: var(--font-weight-medium);
  transition: var(--transition-fast);
}

.login-hint a:hover {
  color: var(--text-link-hover);
  text-decoration: underline;
}

.sms-row .sms-inline {
  display: flex;
  align-items: center;
  gap: 10px;
}
.sms-row .sms-inline input {
  flex: 1;
}
.sms-btn {
  flex-shrink: 0;
  padding: var(--space-sm) var(--space-md);
  border-radius: var(--radius-sm);
  border: 1px solid var(--border-input);
  background: var(--bg-card);
  color: var(--brand-blue);
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-medium);
  cursor: pointer;
  white-space: nowrap;
}
.sms-btn:hover:not(:disabled) {
  border-color: var(--brand-blue);
  background: rgba(20, 86, 240, 0.06);
}
.sms-btn:disabled {
  opacity: 0.55;
  cursor: not-allowed;
}
</style>
