<template>
  <div class="login-page">
    <aside class="login-brand" aria-hidden="false">
      <div class="login-brand-inner">
        <div class="login-logo">{{ brandInitial }}</div>
        <h2 class="login-brand-title">{{ $t('app.title') }}</h2>
        <p class="login-brand-slogan">{{ $t('app.slogan') }}</p>
        <ul class="login-points">
          <li>{{ $t('landing.featConsoleTitle') }}：{{ $t('landing.featConsoleDesc') }}</li>
          <li>{{ $t('landing.featCollabTitle') }}：{{ $t('landing.featCollabDesc') }}</li>
          <li>{{ $t('landing.featAiTitle') }}：{{ $t('landing.featAiDesc') }}</li>
        </ul>
      </div>
    </aside>
    <div class="login-form-wrap">
      <div class="login-top">
        <select v-model="locale" class="login-lang" @change="onLocaleChange">
          <option v-for="opt in localeOptions" :key="opt.value" :value="opt.value">{{ opt.label }}</option>
        </select>
      </div>
      <div class="login-card">
        <h1 class="login-title">{{ $t('login.title') }}</h1>
        <p class="login-sub">{{ $t('login.subtitle') }}</p>
        <form @submit.prevent="onSubmit">
          <div class="form-item">
            <label>{{ $t('login.phone') }}</label>
            <input
              v-model="form.phone"
              type="text"
              autocomplete="username"
              :placeholder="$t('login.phoneHint')"
              required
            >
          </div>
          <div class="form-item">
            <label>{{ $t('login.password') }}</label>
            <input
              v-model="form.password"
              type="password"
              autocomplete="current-password"
              required
            >
          </div>
          <div v-if="error" class="error-text">{{ error }}</div>
          <button class="primary-button" type="submit" :disabled="loading">
            {{ loading ? $t('login.submitting') : $t('login.submit') }}
          </button>
          <p class="register-hint">
            <router-link to="/register">{{ $t('login.registerLink') }}</router-link>
          </p>
          <p class="back-landing">
            <router-link :to="{ name: 'landing' }">← {{ $t('login.backLanding') }}</router-link>
          </p>
        </form>
      </div>
    </div>
  </div>
</template>

<script>
import { localeOptions, setLocale } from '../locales'

export default {
  name: 'LoginView',
  data () {
    return {
      localeOptions,
      locale: this.$i18n.locale,
      form: {
        phone: '',
        password: ''
      },
      loading: false,
      error: ''
    }
  },
  computed: {
    brandInitial () {
      const t = this.$t('app.title')
      return t && t.length ? t.charAt(0) : '—'
    }
  },
  methods: {
    onLocaleChange () {
      setLocale(this.locale)
    },
    async onSubmit () {
      this.error = ''
      this.loading = true
      const phone = this.form.phone.trim()
      try {
        const adminResp = await this.$http.post('/admin/auth/login', {
          phone,
          password: this.form.password
        })
        if (adminResp.data && adminResp.data.code === 0) {
          const data = adminResp.data.data
          window.localStorage.setItem('autoattend_token', data.token)
          window.localStorage.setItem('autoattend_username', phone)
          if (data.collabToken) {
            window.localStorage.setItem('autoattend_collab_token', data.collabToken)
          }
          this.$router.push({ name: 'dashboard' })
          return
        }
        const collabResp = await this.$http.post('/collab/auth/login', {
          email: phone,
          password: this.form.password
        })
        if (collabResp.data && collabResp.data.code === 0) {
          window.localStorage.setItem('autoattend_collab_token', collabResp.data.data.token)
          this.$router.push({ name: 'member-home' })
          return
        }
        this.error = (adminResp.data && adminResp.data.message) || this.$t('login.failed')
      } catch (e) {
        if (e.response && e.response.status === 401) {
          this.error = this.$t('login.failed')
        } else {
          this.error = this.$t('login.failedBackend')
        }
      } finally {
        this.loading = false
      }
    }
  }
}
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  display: grid;
  grid-template-columns: minmax(280px, 1fr) minmax(360px, 1.1fr);
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', 'Noto Sans SC', sans-serif;
}

.login-brand {
  background: linear-gradient(165deg, #f0f4ff 0%, #f7f8fa 45%, #e9f0ff 100%);
  border-right: 1px solid #dee0e3;
  padding: 48px 40px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.login-brand-inner {
  max-width: 400px;
}

.login-logo {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  background: linear-gradient(135deg, #3370ff 0%, #5b9cff 100%);
  color: #fff;
  font-weight: 700;
  font-size: 20px;
  display: grid;
  place-items: center;
  margin-bottom: 20px;
}

.login-brand-title {
  margin: 0 0 8px;
  font-size: 24px;
  font-weight: 600;
  color: #1f2329;
  letter-spacing: -0.02em;
}

.login-brand-slogan {
  margin: 0 0 28px;
  font-size: 15px;
  line-height: 1.6;
  color: #646a73;
}

.login-points {
  margin: 0;
  padding-left: 18px;
  color: #646a73;
  font-size: 14px;
  line-height: 1.65;
}

.login-points li {
  margin-bottom: 10px;
}

.login-form-wrap {
  background: #fff;
  display: flex;
  flex-direction: column;
  min-height: 100vh;
}

.login-top {
  display: flex;
  justify-content: flex-end;
  padding: 16px 24px 0;
}

.login-lang {
  padding: 6px 10px;
  border: 1px solid #dee0e3;
  border-radius: 8px;
  font-size: 13px;
  background: #fff;
  color: #1f2329;
  cursor: pointer;
}

.login-card {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: center;
  max-width: 400px;
  width: 100%;
  margin: 0 auto;
  padding: 24px 24px 48px;
}

.login-title {
  margin: 0 0 8px;
  font-size: 22px;
  font-weight: 600;
  color: #1f2329;
}

.login-sub {
  margin: 0 0 24px;
  font-size: 13px;
  line-height: 1.55;
  color: #646a73;
}

.form-item {
  margin-bottom: 16px;
  display: flex;
  flex-direction: column;
}

label {
  font-size: 12px;
  margin-bottom: 6px;
  color: #646a73;
}

input {
  height: 40px;
  padding: 0 12px;
  border-radius: 8px;
  border: 1px solid #dee0e3;
  font-size: 14px;
  transition: border-color 0.15s, box-shadow 0.15s;
}

input:hover {
  border-color: #c9cdd4;
}

input:focus {
  outline: none;
  border-color: #3370ff;
  box-shadow: 0 0 0 3px rgba(51, 112, 255, 0.15);
}

.primary-button {
  width: 100%;
  margin-top: 8px;
  height: 42px;
  border-radius: 8px;
  border: none;
  background: #3370ff;
  color: #fff;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: background 0.15s;
}

.primary-button:hover:not(:disabled) {
  background: #2860e1;
}

.primary-button:disabled {
  opacity: 0.65;
  cursor: default;
}

.error-text {
  margin-bottom: 8px;
  font-size: 12px;
  color: #f53f3f;
}

.register-hint {
  margin-top: 18px;
  font-size: 14px;
  text-align: center;
}

.register-hint a {
  color: #3370ff;
  text-decoration: none;
  font-weight: 500;
}

.register-hint a:hover {
  text-decoration: underline;
}

.back-landing {
  margin-top: 16px;
  font-size: 13px;
  text-align: center;
}

.back-landing a {
  color: #646a73;
  text-decoration: none;
}

.back-landing a:hover {
  color: #3370ff;
}

@media (max-width: 900px) {
  .login-page {
    grid-template-columns: 1fr;
  }

  .login-brand {
    border-right: none;
    border-bottom: 1px solid #dee0e3;
    padding: 32px 24px;
    min-height: auto;
  }

  .login-form-wrap {
    min-height: auto;
  }
}
</style>
