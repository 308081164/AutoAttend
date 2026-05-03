<template>
  <el-card shadow="hover" class="invite-card">
    <div slot="header" class="card-header">
      <span>📨 官方邀请码</span>
      <el-tag size="small" type="info" effect="plain">新租户注册时填写此码可自动获得官方 API 额度</el-tag>
    </div>
    <div v-if="!loading" class="invite-body">
      <div class="invite-row">
        <span class="invite-label">邀请码：</span>
        <el-tag type="success" size="medium" class="invite-code">{{ code }}</el-tag>
        <el-button size="small" @click="copyCode">复制</el-button>
      </div>
      <p class="hint-text">新租户在注册页面的「邀请码」字段输入此码，注册成功后自动获得 {{ grantCny }} 元官方 API 额度。</p>
    </div>
    <el-skeleton v-else :count="1" animated />
  </el-card>
</template>

<script>
import { http } from '../api/http'

export default {
  name: 'PlatformOfficialInvitePanel',
  data () {
    return { loading: true, code: '', grantCny: 0 }
  },
  async mounted () {
    try {
      const { data } = await http.get('/platform/settings/official-invite-code')
      if (data.code === 0 && data.data) {
        this.code = data.data.code || ''
        this.grantCny = data.data.grantCny || 0
      }
    } catch (e) { /* ignore */ }
    finally { this.loading = false }
  },
  methods: {
    copyCode () {
      if (!this.code) return
      navigator.clipboard.writeText(this.code).then(() => {
        this.$message.success('已复制邀请码')
      }).catch(() => {
        this.$message.warning('复制失败，请手动复制')
      })
    }
  }
}
</script>

<style scoped>
.invite-card {
  margin-bottom: 0;
}
.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 8px;
  font-size: 15px;
  font-weight: 600;
}
.invite-body {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.invite-row {
  display: flex;
  align-items: center;
  gap: 12px;
}
.invite-label {
  font-weight: 500;
  font-size: 14px;
}
.invite-code {
  font-family: 'SF Mono', 'Fira Code', 'Consolas', monospace;
  font-size: 16px;
  letter-spacing: 1px;
}
.hint-text {
  margin: 0;
  font-size: 12px;
  color: #909399;
}
</style>
