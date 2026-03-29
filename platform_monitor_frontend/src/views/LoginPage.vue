<template>
  <div class="wrap">
    <div class="card">
      <h1>监测台登录</h1>
      <form @submit.prevent="submit">
        <label>密码</label>
        <input v-model="password" type="password" autocomplete="current-password" required>
        <p v-if="error" class="err">{{ error }}</p>
        <button type="submit" :disabled="loading">{{ loading ? '登录中…' : '登录' }}</button>
      </form>
    </div>
  </div>
</template>

<script>
import { http, TOKEN_KEY } from '../api/http'

export default {
  name: 'LoginPage',
  data () {
    return {
      password: '',
      loading: false,
      error: ''
    }
  },
  methods: {
    async submit () {
      this.error = ''
      this.loading = true
      try {
        const { data } = await http.post('/platform/auth/login', { password: this.password })
        if (data.code === 0 && data.data && data.data.token) {
          localStorage.setItem(TOKEN_KEY, data.data.token)
          const redirect = this.$route.query.redirect || '/'
          this.$router.replace(redirect)
          return
        }
        this.error = (data && data.message) || '登录失败'
      } catch (e) {
        this.error = '请求失败，请确认后端已启动'
      } finally {
        this.loading = false
      }
    }
  }
}
</script>

<style scoped>
.wrap {
  max-width: 400px;
  margin: 48px auto;
}
.card {
  background: #1e293b;
  border: 1px solid #334155;
  border-radius: 12px;
  padding: 28px;
}
h1 {
  margin: 0 0 8px;
  font-size: 20px;
}
label {
  display: block;
  font-size: 13px;
  margin-bottom: 6px;
  color: #cbd5e1;
}
input {
  width: 100%;
  padding: 10px 12px;
  border-radius: 8px;
  border: 1px solid #475569;
  background: #0f172a;
  color: #f1f5f9;
  font-size: 15px;
  margin-bottom: 16px;
}
input:focus {
  outline: none;
  border-color: #60a5fa;
}
button {
  width: 100%;
  padding: 10px;
  border: none;
  border-radius: 8px;
  background: #3b82f6;
  color: #fff;
  font-size: 15px;
  cursor: pointer;
}
button:disabled {
  opacity: 0.6;
  cursor: default;
}
.err {
  color: #fca5a5;
  font-size: 13px;
  margin: -8px 0 12px;
}
</style>
