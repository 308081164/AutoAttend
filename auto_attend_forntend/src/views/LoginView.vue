<template>
  <div class="login-page">
    <div class="login-card">
      <h2 class="login-title">{{ $t('login.title') }}</h2>
      <form @submit.prevent="onSubmit">
        <div class="form-item">
          <label>{{ $t('login.username') }}</label>
          <input
            v-model="form.username"
            type="text"
            autocomplete="username"
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
      </form>
    </div>
  </div>
</template>

<script>
export default {
  name: 'LoginView',
  data () {
    return {
      form: {
        username: 'admin',
        password: 'admin123'
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
        const adminResp = await this.$http.post('/admin/auth/login', this.form)
        if (adminResp.data && adminResp.data.code === 0) {
          const data = adminResp.data.data
          window.localStorage.setItem('autoattend_token', data.token)
          window.localStorage.setItem('autoattend_username', this.form.username)
          if (data.collabToken) {
            window.localStorage.setItem('autoattend_collab_token', data.collabToken)
          }
          this.$router.push({ name: 'dashboard' })
          return
        }
        const collabResp = await this.$http.post('/collab/auth/login', {
          email: this.form.username.trim(),
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
  width: 100%;
  height: calc(100vh - 56px);
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: #f3f4f6;
}

.login-card {
  width: 320px;
  padding: 24px;
  border-radius: 8px;
  background-color: #ffffff;
  box-shadow: 0 10px 15px -3px rgba(15, 23, 42, 0.15);
}

.login-title {
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

.employee-login-hint {
  margin-top: 16px;
  font-size: 13px;
  color: #6b7280;
  text-align: center;
}

.employee-login-hint a {
  color: #2563eb;
  text-decoration: none;
}

.employee-login-hint a:hover {
  text-decoration: underline;
}
</style>

