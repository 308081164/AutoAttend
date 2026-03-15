<template>
  <div class="login-page">
    <div class="login-card">
      <h2 class="login-title">{{ $t('collabLogin.title') }}</h2>
      <p class="login-desc">{{ $t('collabLogin.desc') }}</p>
      <form @submit.prevent="onSubmit">
        <div class="form-item">
          <label>{{ $t('collabLogin.email') }}</label>
          <input
            v-model="form.email"
            type="email"
            autocomplete="email"
            :placeholder="$t('collabLogin.placeholderEmail')"
            required
          >
        </div>
        <div class="form-item">
          <label>{{ $t('collabLogin.password') }}</label>
          <input
            v-model="form.password"
            type="password"
            autocomplete="current-password"
            :placeholder="$t('collabLogin.placeholderPassword')"
            required
          >
        </div>
        <div v-if="error" class="error-text">{{ error }}</div>
        <button class="primary-button" type="submit" :disabled="loading">
          {{ loading ? $t('collabLogin.submitting') : $t('collabLogin.submit') }}
        </button>
      </form>
      <div class="back-link">
        <router-link to="/">{{ $t('collabLogin.backHome') }}</router-link>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'CollabLoginView',
  data () {
    return {
      form: {
        email: '',
        password: '123456'
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
        const resp = await this.$http.post('/collab/auth/login', this.form)
        if (resp.data && resp.data.code === 0) {
          const token = resp.data.data.token
          window.localStorage.setItem('autoattend_collab_token', token)
          this.$router.push({ name: 'member-home' })
        } else {
          this.error = (resp.data && resp.data.message) || this.$t('collabLogin.failed')
        }
      } catch (e) {
        this.error = e.response && e.response.data && e.response.data.message
          ? e.response.data.message
          : this.$t('collabLogin.failedBackend')
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
  min-height: calc(100vh - 56px);
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: #f3f4f6;
}

.login-card {
  width: 340px;
  padding: 24px;
  border-radius: 8px;
  background-color: #ffffff;
  box-shadow: 0 10px 15px -3px rgba(15, 23, 42, 0.15);
}

.login-title {
  margin: 0 0 8px;
  font-size: 18px;
  text-align: center;
}

.login-desc {
  margin: 0 0 16px;
  font-size: 12px;
  color: #6b7280;
  text-align: center;
}

.form-item {
  margin-bottom: 12px;
}

.form-item label {
  display: block;
  font-size: 13px;
  margin-bottom: 4px;
  color: #374151;
}

.form-item input {
  width: 100%;
  padding: 8px 10px;
  border-radius: 4px;
  border: 1px solid #d1d5db;
  font-size: 14px;
  box-sizing: border-box;
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
  margin-top: 4px;
  font-size: 12px;
  color: #dc2626;
}

.back-link {
  margin-top: 16px;
  text-align: center;
  font-size: 13px;
}

.back-link a {
  color: #2563eb;
}
</style>
