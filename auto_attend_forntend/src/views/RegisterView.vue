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
  min-height: calc(100vh - 56px);
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: #f3f4f6;
  padding: 24px 0;
}

.register-card {
  width: 360px;
  padding: 24px;
  border-radius: 8px;
  background-color: #ffffff;
  box-shadow: 0 10px 15px -3px rgba(15, 23, 42, 0.15);
}

.register-title {
  margin: 0 0 16px;
  font-size: 18px;
  text-align: center;
}

.form-item {
  margin-bottom: 12px;
  display: flex;
  flex-direction: column;
}

label {
  font-size: 13px;
  margin-bottom: 4px;
  color: #374151;
}

input {
  padding: 8px 10px;
  border-radius: 4px;
  border: 1px solid #d1d5db;
  font-size: 14px;
}

input:focus {
  outline: none;
  border-color: #2563eb;
  box-shadow: 0 0 0 1px rgba(37, 99, 235, 0.2);
}

.primary-button {
  width: 100%;
  margin-top: 8px;
  padding: 8px 10px;
  border-radius: 4px;
  border: none;
  background-color: #2563eb;
  color: #ffffff;
  font-size: 14px;
  cursor: pointer;
}

.primary-button:disabled {
  opacity: 0.7;
  cursor: default;
}

.error-text {
  margin-bottom: 4px;
  font-size: 12px;
  color: #dc2626;
}

.login-hint {
  margin-top: 16px;
  font-size: 13px;
  text-align: center;
}

.login-hint a {
  color: #2563eb;
  text-decoration: none;
}

.login-hint a:hover {
  text-decoration: underline;
}
</style>
