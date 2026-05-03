<template>
  <div class="login-page">
    <el-card class="login-card" shadow="always">
      <div class="login-header">
        <img class="login-logo" src="/brand-logo.svg" width="48" height="48" alt="" />
        <h1 class="login-title">监测台登录</h1>
        <p class="login-desc">AutoAttend 平台运营管理后台</p>
      </div>
      <el-form ref="form" :model="form" :rules="rules" @submit.native.prevent="submit">
        <el-form-item prop="password">
          <el-input
            v-model="form.password"
            type="password"
            placeholder="请输入登录密码"
            prefix-icon="el-icon-lock"
            show-password
            @keyup.enter.native="submit"
          ></el-input>
        </el-form-item>
        <el-form-item>
          <el-button
            type="primary"
            :loading="loading"
            class="login-btn"
            @click="submit"
          >{{ loading ? '登录中…' : '登 录' }}</el-button>
        </el-form-item>
        <p v-if="error" class="login-error">
          <i class="el-icon-warning"></i>
          {{ error }}
        </p>
      </el-form>
    </el-card>
  </div>
</template>

<script>
import { http, TOKEN_KEY } from '../api/http'

export default {
  name: 'LoginPage',
  data () {
    return {
      form: { password: '' },
      rules: {
        password: [
          { required: true, message: '请输入密码', trigger: 'blur' }
        ]
      },
      loading: false,
      error: ''
    }
  },
  methods: {
    async submit () {
      this.error = ''
      this.$refs.form.validate(async (valid) => {
        if (!valid) return
        this.loading = true
        try {
          const { data } = await http.post('/platform/auth/login', { password: this.form.password })
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
      })
    }
  }
}
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 20px;
}
.login-card {
  width: 400px;
  max-width: 100%;
  border-radius: 12px;
}
.login-header {
  text-align: center;
  margin-bottom: 28px;
}
.login-logo {
  border-radius: 12px;
  margin-bottom: 12px;
}
.login-title {
  margin: 0 0 6px;
  font-size: 22px;
  font-weight: 600;
  color: #303133;
}
.login-desc {
  margin: 0;
  font-size: 14px;
  color: #909399;
}
.login-btn {
  width: 100%;
  font-size: 15px;
  padding: 12px 0;
}
.login-error {
  margin: 0;
  font-size: 13px;
  color: #f56c6c;
  text-align: center;
}
</style>
