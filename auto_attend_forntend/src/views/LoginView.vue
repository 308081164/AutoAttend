<template>
  <div class="login-page">
    <!-- ===== LEFT PANEL ===== -->
    <aside class="login-left" aria-hidden="true">
      <div class="login-left-bg"></div>

      <!-- Floating decorations -->
      <div class="float-deco deco-circle-1"></div>
      <div class="float-deco deco-circle-2"></div>
      <div class="float-deco deco-square-1"></div>
      <div class="float-deco deco-square-2"></div>
      <div class="float-deco deco-circle-3"></div>

      <div class="login-left-content">
        <!-- Logo -->
        <div class="brand-logo-row">
          <div class="brand-icon-box" aria-hidden="true">
            <img class="brand-icon-img" src="/brand-logo.svg" width="40" height="40" alt="">
          </div>
          <span class="brand-name">流帮 Project</span>
        </div>

        <!-- Tagline -->
        <p class="brand-tagline">软件流程，帮你搞定！</p>

        <!-- Feature cards -->
        <div class="feat-cards">
          <div class="feat-card">
            <div class="feat-icon-wrap">
              <span class="feat-icon">📋</span>
            </div>
            <div class="feat-body">
              <h3 class="feat-title">智能报价系统</h3>
              <p class="feat-desc">{{ $t('landing.featConsoleDesc') }}</p>
            </div>
          </div>

          <div class="feat-card">
            <div class="feat-icon-wrap">
              <span class="feat-icon">👥</span>
            </div>
            <div class="feat-body">
              <h3 class="feat-title">团队协作空间</h3>
              <p class="feat-desc">{{ $t('landing.featCollabDesc') }}</p>
            </div>
          </div>

          <div class="feat-card">
            <div class="feat-icon-wrap">
              <span class="feat-icon">🤖</span>
            </div>
            <div class="feat-body">
              <h3 class="feat-title">AI 能力集成</h3>
              <p class="feat-desc">{{ $t('landing.featAiDesc') }}</p>
            </div>
          </div>
        </div>
      </div>
    </aside>

    <!-- ===== RIGHT PANEL ===== -->
    <main class="login-right">
      <!-- Language selector -->
      <div class="login-top-bar">
        <select v-model="locale" class="lang-select" @change="onLocaleChange">
          <option v-for="opt in localeOptions" :key="opt.value" :value="opt.value">{{ opt.label }}</option>
        </select>
      </div>

      <!-- Form area -->
      <div class="login-form-area">
        <div class="login-form-card">
          <h1 class="form-heading">{{ $t('login.title') }}</h1>
          <p class="form-subtitle">{{ $t('login.subtitle') }}</p>

          <form @submit.prevent="onSubmit" class="login-form">
            <!-- Phone -->
            <div class="field-group">
              <label class="field-label">{{ $t('login.phone') }}</label>
              <div class="phone-input-wrap">
                <span class="phone-prefix">+86</span>
                <input
                  v-model="form.phone"
                  type="text"
                  class="phone-input"
                  autocomplete="username"
                  :placeholder="$t('login.phoneHint')"
                  required
                >
              </div>
            </div>

            <!-- Password -->
            <div class="field-group">
              <label class="field-label">{{ $t('login.password') }}</label>
              <div class="password-input-wrap">
                <input
                  v-model="form.password"
                  :type="showPassword ? 'text' : 'password'"
                  class="password-input"
                  autocomplete="current-password"
                  required
                >
                <button
                  type="button"
                  class="eye-toggle"
                  @click="showPassword = !showPassword"
                  tabindex="-1"
                  :aria-label="showPassword ? 'Hide password' : 'Show password'"
                >
                  <!-- Eye open icon -->
                  <svg v-if="!showPassword" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
                    <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/>
                    <circle cx="12" cy="12" r="3"/>
                  </svg>
                  <!-- Eye closed icon -->
                  <svg v-else width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
                    <path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94"/>
                    <path d="M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19"/>
                    <path d="M14.12 14.12a3 3 0 1 1-4.24-4.24"/>
                    <line x1="1" y1="1" x2="23" y2="23"/>
                  </svg>
                </button>
              </div>
            </div>

            <!-- Error -->
            <div v-if="error" class="error-msg">{{ error }}</div>

            <!-- Submit -->
            <button type="submit" class="submit-btn" :disabled="loading">
              <span v-if="loading" class="spinner"></span>
              <span>{{ loading ? $t('login.submitting') : $t('login.submit') }}</span>
            </button>
          </form>

          <!-- Divider -->
          <div class="divider-row">
            <span class="divider-line"></span>
            <span class="divider-text">或</span>
            <span class="divider-line"></span>
          </div>

          <!-- Register link -->
          <div class="alt-action">
            <router-link to="/register" class="register-link">{{ $t('login.registerLink') }}</router-link>
          </div>

          <!-- Back to landing -->
          <div class="back-link-row">
            <router-link :to="{ name: 'landing' }" class="back-link">&larr; {{ $t('login.backLanding') }}</router-link>
          </div>
        </div>
      </div>

      <!-- Footer -->
      <footer class="login-footer">
        <span>&copy; 2026 流帮Project</span>
      </footer>
    </main>
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
      error: '',
      showPassword: false
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
/* ============================================================
   LAYOUT
   ============================================================ */
.login-page {
  min-height: 100vh;
  display: flex;
  font-family: var(--font-family);
}

/* ============================================================
   LEFT PANEL (58%)
   ============================================================ */
.login-left {
  position: relative;
  width: 58%;
  min-height: 100vh;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
}

.login-left-bg {
  position: absolute;
  inset: 0;
  background: linear-gradient(160deg, #0F172A 0%, #1456F0 45%, #7C3AED 100%);
  z-index: 0;
}

/* Floating decorations */
.float-deco {
  position: absolute;
  border-radius: 50%;
  z-index: 1;
  pointer-events: none;
}

.deco-circle-1 {
  width: 320px;
  height: 320px;
  top: -80px;
  right: -60px;
  background: rgba(255, 255, 255, 0.04);
  animation: float 8s ease-in-out infinite;
}

.deco-circle-2 {
  width: 200px;
  height: 200px;
  bottom: 10%;
  left: -40px;
  background: rgba(255, 255, 255, 0.05);
  animation: float 10s ease-in-out infinite 1s;
}

.deco-circle-3 {
  width: 120px;
  height: 120px;
  top: 40%;
  right: 15%;
  background: rgba(255, 255, 255, 0.03);
  animation: float 7s ease-in-out infinite 2s;
}

.deco-square-1 {
  width: 80px;
  height: 80px;
  border-radius: 16px;
  bottom: 25%;
  right: 10%;
  background: rgba(255, 255, 255, 0.04);
  transform: rotate(25deg);
  animation: float 9s ease-in-out infinite 0.5s;
}

.deco-square-2 {
  width: 50px;
  height: 50px;
  border-radius: 10px;
  top: 20%;
  left: 10%;
  background: rgba(255, 255, 255, 0.05);
  transform: rotate(15deg);
  animation: float 6s ease-in-out infinite 3s;
}

@keyframes float {
  0%, 100% {
    transform: translateY(0) rotate(0deg);
  }
  50% {
    transform: translateY(-18px) rotate(3deg);
  }
}

/* Left content */
.login-left-content {
  position: relative;
  z-index: 2;
  max-width: 420px;
  padding: 48px 40px;
}

.brand-logo-row {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-bottom: 16px;
}

.brand-icon-box {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.15);
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.2);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  overflow: hidden;
}

.brand-icon-img {
  width: 100%;
  height: 100%;
  display: block;
  object-fit: cover;
}

.brand-name {
  font-size: 22px;
  font-weight: 600;
  color: #fff;
  letter-spacing: -0.01em;
}

.brand-tagline {
  font-size: 15px;
  line-height: 1.6;
  color: rgba(255, 255, 255, 0.65);
  margin: 0 0 36px;
}

/* Feature cards */
.feat-cards {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.feat-card {
  display: flex;
  align-items: flex-start;
  gap: 14px;
  padding: 16px;
  border-radius: var(--radius-lg);
  background: rgba(255, 255, 255, 0.08);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  border: 1px solid rgba(255, 255, 255, 0.12);
  transition: background 0.25s ease, border-color 0.25s ease;
}

.feat-card:hover {
  background: rgba(255, 255, 255, 0.13);
  border-color: rgba(255, 255, 255, 0.2);
}

.feat-icon-wrap {
  width: 40px;
  height: 40px;
  border-radius: 10px;
  background: rgba(255, 255, 255, 0.12);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.feat-icon {
  font-size: 18px;
  line-height: 1;
}

.feat-body {
  flex: 1;
  min-width: 0;
}

.feat-title {
  font-size: 14px;
  font-weight: 600;
  color: #fff;
  margin: 0 0 4px;
  line-height: 1.4;
}

.feat-desc {
  font-size: 12px;
  line-height: 1.55;
  color: rgba(255, 255, 255, 0.55);
  margin: 0;
}

/* ============================================================
   RIGHT PANEL (42%)
   ============================================================ */
.login-right {
  width: 42%;
  min-height: 100vh;
  background: #fff;
  display: flex;
  flex-direction: column;
}

.login-top-bar {
  display: flex;
  justify-content: flex-end;
  padding: 20px 32px 0;
}

.lang-select {
  padding: 6px 12px;
  border: 1px solid var(--border-primary);
  border-radius: var(--radius-sm);
  font-size: var(--font-size-sm);
  background: var(--bg-card);
  color: var(--text-primary);
  cursor: pointer;
  transition: var(--transition-fast);
  outline: none;
}

.lang-select:hover {
  border-color: var(--border-input-hover);
}

.lang-select:focus {
  border-color: var(--brand-blue);
  box-shadow: 0 0 0 2px rgba(20, 86, 240, 0.12);
}

/* Form area */
.login-form-area {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px 32px;
}

.login-form-card {
  width: 100%;
  max-width: 380px;
}

.form-heading {
  font-size: 26px;
  font-weight: 700;
  color: var(--text-primary);
  margin: 0 0 8px;
  letter-spacing: -0.02em;
}

.form-subtitle {
  font-size: var(--font-size-sm);
  line-height: 1.6;
  color: var(--text-secondary);
  margin: 0 0 32px;
}

/* Form */
.login-form {
  display: flex;
  flex-direction: column;
}

.field-group {
  margin-bottom: 20px;
}

.field-label {
  display: block;
  font-size: var(--font-size-xs);
  font-weight: 500;
  color: var(--text-secondary);
  margin-bottom: 6px;
}

/* Phone input */
.phone-input-wrap {
  display: flex;
  align-items: center;
  height: 44px;
  border: 1px solid var(--border-input);
  border-radius: var(--radius-md);
  overflow: hidden;
  transition: var(--transition-fast);
  background: var(--bg-input);
}

.phone-input-wrap:hover {
  border-color: var(--border-input-hover);
}

.phone-input-wrap:focus-within {
  border-color: var(--brand-blue);
  box-shadow: 0 0 0 3px rgba(20, 86, 240, 0.12);
}

.phone-prefix {
  display: flex;
  align-items: center;
  height: 100%;
  padding: 0 12px;
  font-size: var(--font-size-base);
  font-weight: 500;
  color: var(--text-primary);
  background: var(--bg-page);
  border-right: 1px solid var(--border-primary);
  flex-shrink: 0;
  user-select: none;
}

.phone-input {
  flex: 1;
  height: 100%;
  border: none;
  outline: none;
  padding: 0 12px;
  font-size: var(--font-size-base);
  color: var(--text-primary);
  background: transparent;
}

.phone-input::placeholder {
  color: var(--text-tertiary);
}

/* Password input */
.password-input-wrap {
  display: flex;
  align-items: center;
  height: 44px;
  border: 1px solid var(--border-input);
  border-radius: var(--radius-md);
  overflow: hidden;
  transition: var(--transition-fast);
  background: var(--bg-input);
}

.password-input-wrap:hover {
  border-color: var(--border-input-hover);
}

.password-input-wrap:focus-within {
  border-color: var(--brand-blue);
  box-shadow: 0 0 0 3px rgba(20, 86, 240, 0.12);
}

.password-input {
  flex: 1;
  height: 100%;
  border: none;
  outline: none;
  padding: 0 12px;
  font-size: var(--font-size-base);
  color: var(--text-primary);
  background: transparent;
}

.password-input::placeholder {
  color: var(--text-tertiary);
}

.eye-toggle {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 100%;
  background: none;
  border: none;
  color: var(--text-tertiary);
  cursor: pointer;
  flex-shrink: 0;
  transition: color 0.15s;
  padding: 0;
}

.eye-toggle:hover {
  color: var(--text-secondary);
}

/* Error */
.error-msg {
  margin-bottom: 12px;
  padding: 10px 12px;
  font-size: var(--font-size-xs);
  color: var(--danger);
  background: var(--danger-bg);
  border-radius: var(--radius-sm);
  line-height: 1.5;
}

/* Submit button */
.submit-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  width: 100%;
  height: 44px;
  margin-top: 4px;
  border: none;
  border-radius: var(--radius-md);
  background: var(--brand-blue);
  color: #fff;
  font-size: var(--font-size-base);
  font-weight: 600;
  cursor: pointer;
  transition: background 0.2s ease, transform 0.15s ease, box-shadow 0.2s ease;
  box-shadow: var(--shadow-btn);
  outline: none;
}

.submit-btn:hover:not(:disabled) {
  background: var(--brand-blue-hover);
  transform: translateY(-1px);
  box-shadow: 0 4px 14px rgba(20, 86, 240, 0.35);
}

.submit-btn:active:not(:disabled) {
  background: var(--brand-blue-active);
  transform: translateY(0);
}

.submit-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
  transform: none;
}

.submit-btn:focus-visible {
  box-shadow: 0 0 0 3px rgba(20, 86, 240, 0.25);
}

/* Spinner */
.spinner {
  display: inline-block;
  width: 16px;
  height: 16px;
  border: 2px solid rgba(255, 255, 255, 0.3);
  border-top-color: #fff;
  border-radius: 50%;
  animation: spin 0.6s linear infinite;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

/* Divider */
.divider-row {
  display: flex;
  align-items: center;
  gap: 14px;
  margin: 24px 0 20px;
}

.divider-line {
  flex: 1;
  height: 1px;
  background: var(--border-primary);
}

.divider-text {
  font-size: var(--font-size-xs);
  color: var(--text-tertiary);
  user-select: none;
}

/* Register link */
.alt-action {
  text-align: center;
}

.register-link {
  font-size: var(--font-size-base);
  font-weight: 500;
  color: var(--brand-blue);
  text-decoration: none;
  transition: color 0.15s;
}

.register-link:hover {
  color: var(--brand-blue-hover);
  text-decoration: underline;
}

/* Back link */
.back-link-row {
  text-align: center;
  margin-top: 14px;
}

.back-link {
  font-size: var(--font-size-sm);
  color: var(--text-tertiary);
  text-decoration: none;
  transition: color 0.15s;
}

.back-link:hover {
  color: var(--brand-blue);
}

/* Footer */
.login-footer {
  padding: 20px 32px;
  text-align: center;
  font-size: var(--font-size-xs);
  color: var(--text-disabled);
}

/* ============================================================
   RESPONSIVE
   ============================================================ */
@media (max-width: 768px) {
  .login-page {
    flex-direction: column;
  }

  .login-left {
    width: 100%;
    min-height: auto;
    padding: 0;
  }

  .login-left-content {
    padding: 36px 24px 32px;
    max-width: 100%;
  }

  .feat-cards {
    display: none;
  }

  .brand-tagline {
    margin-bottom: 0;
  }

  .login-right {
    width: 100%;
    min-height: auto;
  }

  .login-form-area {
    padding: 24px 20px;
  }

  .login-form-card {
    max-width: 100%;
  }

  .login-top-bar {
    padding: 16px 20px 0;
  }

  .login-footer {
    padding: 16px 20px;
  }
}

@media (max-width: 480px) {
  .form-heading {
    font-size: 22px;
  }

  .phone-input {
    padding: 0 8px;
    font-size: var(--font-size-sm);
  }

  .password-input {
    padding: 0 8px;
    font-size: var(--font-size-sm);
  }
}
</style>
