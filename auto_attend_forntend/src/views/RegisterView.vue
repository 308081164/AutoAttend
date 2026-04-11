<template>
  <div class="register-page">
    <div class="register-card">
      <h2 class="register-title">{{ $t('register.title') }}</h2>
      <form @submit.prevent="onSubmit">
        <div class="form-item">
          <label>{{ $t('register.phone') }}</label>
          <input v-model="form.phone" type="text" autocomplete="tel" required>
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
        password: ''
      },
      loading: false,
      error: ''
    }
  },
  methods: {
    async onSubmit () {
      this.error = ''
      this.loading = true
      try {
        const resp = await this.$http.post('/admin/auth/register', {
          phone: this.form.phone.trim(),
          orgName: this.form.orgName.trim(),
          slug: this.form.slug.trim(),
          password: this.form.password
        })
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
</style>
