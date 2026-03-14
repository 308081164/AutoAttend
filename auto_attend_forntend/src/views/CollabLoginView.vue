<template>
  <div class="login-page">
    <div class="login-card">
      <h2 class="login-title">项目协作登录</h2>
      <p class="login-desc">使用 commit 作者邮箱 + 默认密码 123456 登录（可由管理员重置）</p>
      <form @submit.prevent="onSubmit">
        <div class="form-item">
          <label>邮箱</label>
          <input
            v-model="form.email"
            type="email"
            autocomplete="email"
            placeholder="author@example.com"
            required
          >
        </div>
        <div class="form-item">
          <label>密码</label>
          <input
            v-model="form.password"
            type="password"
            autocomplete="current-password"
            placeholder="默认 123456"
            required
          >
        </div>
        <div v-if="error" class="error-text">{{ error }}</div>
        <button class="primary-button" type="submit" :disabled="loading">
          {{ loading ? '登录中...' : '登录' }}
        </button>
      </form>
      <div class="back-link">
        <router-link to="/">返回首页</router-link>
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
          this.$router.push({ name: 'collab-projects' })
        } else {
          this.error = (resp.data && resp.data.message) || '登录失败'
        }
      } catch (e) {
        this.error = e.response && e.response.data && e.response.data.message
          ? e.response.data.message
          : '登录失败，请检查后端服务'
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
