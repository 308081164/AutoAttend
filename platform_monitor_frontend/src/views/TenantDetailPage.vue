<template>
  <div class="tenant-detail-page">
    <div class="page-header">
      <el-button type="text" @click="$router.push({ name: 'tenants' })" icon="el-icon-arrow-left" class="back-btn">
        返回租户列表
      </el-button>
      <h2 class="page-title">租户 #{{ tenantId }}</h2>
    </div>

    <el-skeleton :loading="loading" animated :count="4" v-if="loading" />

    <template v-else-if="error">
      <el-alert :title="error" type="error" show-icon :closable="false" />
    </template>

    <template v-else-if="tenant">
      <!-- 概览卡片 -->
      <el-row :gutter="16" class="stat-cards">
        <el-col :xs="12" :sm="6">
          <el-card shadow="hover" class="stat-card">
            <div class="stat-label">MRR 近似（近30天模拟收入）</div>
            <div class="stat-value">{{ formatYuan(mrrApproxCents) }}</div>
          </el-card>
        </el-col>
        <el-col :xs="12" :sm="6">
          <el-card shadow="hover" class="stat-card">
            <div class="stat-label">当前权益档位</div>
            <div class="stat-value mono-text">{{ ops.planCode || '—' }}</div>
          </el-card>
        </el-col>
        <el-col :xs="12" :sm="6">
          <el-card shadow="hover" class="stat-card">
            <div class="stat-label">到期回退档位</div>
            <div class="stat-value mono-text">{{ ops.billingBaselinePlanCode || '—' }}</div>
          </el-card>
        </el-col>
        <el-col :xs="12" :sm="6">
          <el-card shadow="hover" class="stat-card">
            <div class="stat-label">权益截止</div>
            <div class="stat-value small-text">{{ ops.subscriptionEndsAt || '—' }}</div>
          </el-card>
        </el-col>
      </el-row>

      <el-row :gutter="16">
        <el-col :span="12">
          <!-- 基本信息 -->
          <el-card shadow="hover" class="info-card">
            <div slot="header" class="card-header">
              <span>基本信息</span>
            </div>
            <el-descriptions :column="1" size="small" border>
              <el-descriptions-item label="名称">{{ ops.tenantName }}</el-descriptions-item>
              <el-descriptions-item label="Slug">
                <el-tag size="mini" effect="plain">{{ ops.slug }}</el-tag>
              </el-descriptions-item>
              <el-descriptions-item label="状态">
                <el-tag :type="ops.status === 'active' ? 'success' : 'danger'" size="mini" effect="plain">
                  {{ ops.status }}
                </el-tag>
              </el-descriptions-item>
              <el-descriptions-item label="管理员手机">{{ ops.adminPhone || '—' }}</el-descriptions-item>
              <el-descriptions-item label="创建时间">{{ ops.tenantCreatedAt || '—' }}</el-descriptions-item>
              <el-descriptions-item label="官方 API 额度（元）">{{ formatOfficialYuan(ops.officialApiCnyBalance) }}</el-descriptions-item>
            </el-descriptions>
          </el-card>
        </el-col>

        <el-col :span="12">
          <!-- 用量与活跃 -->
          <el-card shadow="hover" class="info-card">
            <div slot="header" class="card-header">
              <span>用量与活跃</span>
            </div>
            <el-descriptions :column="1" size="small" border>
              <el-descriptions-item label="成员数">{{ ops.memberCount }}</el-descriptions-item>
              <el-descriptions-item label="GitHub 已绑定项目">{{ ops.githubLinkedProjects }}</el-descriptions-item>
              <el-descriptions-item label="24h 提交">{{ ops.commits24h }}</el-descriptions-item>
              <el-descriptions-item label="7d 提交">{{ ops.commits7d }}</el-descriptions-item>
              <el-descriptions-item label="DAU 近似（24h 邮箱）">{{ ops.dauEmails24h }}</el-descriptions-item>
              <el-descriptions-item label="Diff 存储近似">{{ formatBytes(ops.storageDiffBytesApprox) }}</el-descriptions-item>
            </el-descriptions>
          </el-card>
        </el-col>
      </el-row>

      <!-- 项目信息发布 -->
      <el-card shadow="hover" class="info-card">
        <div slot="header" class="card-header">
          <span>项目信息发布（租户级）</span>
          <el-tag size="small" type="info" effect="plain">控制该租户管理员控制台是否出现「项目信息」入口</el-tag>
        </div>
        <el-form :model="mpForm" label-width="0" size="small">
          <el-form-item>
            <el-checkbox v-model="mpForm.browseEnabled" :disabled="mpSaving">对本租户开放浏览（列表/详情）</el-checkbox>
          </el-form-item>
          <el-form-item>
            <el-checkbox v-model="mpForm.allowPublish" :disabled="mpSaving || !mpForm.browseEnabled">允许在本租户发布与审核（需同时开启浏览）</el-checkbox>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" size="small" :loading="mpSaving" @click="saveProjectMarketplace">
              保存项目信息设置
            </el-button>
            <span v-if="mpMsg" class="success-text" style="margin-left: 12px;">{{ mpMsg }}</span>
          </el-form-item>
        </el-form>
      </el-card>

      <!-- 运维动作 -->
      <el-card shadow="hover" class="info-card">
        <div slot="header" class="card-header">
          <span>运维动作（需审计）</span>
          <el-tag size="small" type="warning" effect="plain">暂停后该租户管理员无法登录控制台</el-tag>
        </div>
        <div class="action-buttons">
          <el-button type="success" size="small" :loading="acting" @click="doGrantProPlus">
            <i class="el-icon-top"></i> 一键开通专业增强版（365天）
          </el-button>
          <el-button type="danger" size="small" :loading="acting" @click="doSuspend">
            <i class="el-icon-video-pause"></i> 暂停租户
          </el-button>
          <el-button type="primary" size="small" :loading="acting" @click="doResume">
            <i class="el-icon-video-play"></i> 恢复服务
          </el-button>
          <el-button size="small" :loading="acting" @click="doRevoke">
            <i class="el-icon-refresh"></i> 强制下线管理员会话
          </el-button>
        </div>
        <p v-if="actionMsg" class="success-text">{{ actionMsg }}</p>
      </el-card>
    </template>
  </div>
</template>

<script>
import { http } from '../api/http'

export default {
  name: 'TenantDetailPage',
  data () {
    return {
      loading: true,
      error: '',
      tenant: null,
      mrrApproxCents: 0,
      acting: false,
      actionMsg: '',
      mpForm: {
        browseEnabled: true,
        allowPublish: false
      },
      mpSaving: false,
      mpMsg: ''
    }
  },
  computed: {
    tenantId () {
      return Number(this.$route.params.id)
    },
    ops () {
      const t = this.tenant
      if (!t) return {}
      return t.ops || t
    }
  },
  watch: {
    '$route.params.id' () {
      this.load()
    }
  },
  mounted () {
    this.load()
  },
  methods: {
    formatYuan (cents) {
      if (cents == null) return '—'
      const n = Number(cents)
      if (!Number.isFinite(n)) return '—'
      return `¥${(n / 100).toFixed(2)}`
    },
    formatOfficialYuan (v) {
      if (v == null || v === '') return '—'
      const n = Number(v)
      if (!Number.isFinite(n)) return '—'
      return `¥${n.toFixed(2)}`
    },
    formatBytes (b) {
      if (b == null) return '—'
      const n = Number(b)
      if (!Number.isFinite(n) || n < 0) return '—'
      if (n < 1024) return `${n} B`
      if (n < 1024 * 1024) return `${(n / 1024).toFixed(1)} KB`
      if (n < 1024 * 1024 * 1024) return `${(n / 1024 / 1024).toFixed(1)} MB`
      return `${(n / 1024 / 1024 / 1024).toFixed(2)} GB`
    },
    async load () {
      this.loading = true
      this.error = ''
      this.actionMsg = ''
      try {
        const { data } = await http.get(`/platform/tenants/${this.tenantId}`)
        if (data.code === 0 && data.data) {
          this.tenant = data.data.tenant
          this.mrrApproxCents = data.data.mrrApproxCents30d != null ? data.data.mrrApproxCents30d : 0
          const tr = data.data.tenant || {}
          this.mpForm.browseEnabled = tr.projectMarketplaceEnabled !== false
          this.mpForm.allowPublish = tr.projectMarketplaceAllowPublish === true
        } else {
          this.error = (data && data.message) || '加载失败'
        }
      } catch (e) {
        this.error = '请求失败'
      } finally {
        this.loading = false
      }
    },
    async doSuspend () {
      try {
        await this.$confirm('确认暂停该租户？', '提示', { type: 'warning' })
        await this.postAction('suspend')
      } catch (e) { /* cancelled */ }
    },
    async doResume () {
      try {
        await this.$confirm('确认恢复该租户服务？', '提示', { type: 'info' })
        await this.postAction('resume')
      } catch (e) { /* cancelled */ }
    },
    async doRevoke () {
      try {
        await this.$confirm('确认清除该租户全部管理员会话？', '提示', { type: 'warning' })
        await this.postAction('revoke')
      } catch (e) { /* cancelled */ }
    },
    async doGrantProPlus () {
      try {
        await this.$confirm('确认为该租户开通专业增强版（pro+）权益 365 天？（不产生订单）', '提示', { type: 'info' })
        await this.postGrantProPlus()
      } catch (e) { /* cancelled */ }
    },
    async postGrantProPlus () {
      this.acting = true
      this.actionMsg = ''
      try {
        const { data } = await http.post(`/platform/tenants/${this.tenantId}/grant-pro-plus`, { days: 365 })
        if (data.code === 0) {
          this.actionMsg = '已开通专业增强版权益'
          await this.load()
        } else {
          this.actionMsg = (data && data.message) || '失败'
        }
      } catch (e) {
        this.actionMsg = '请求失败'
      } finally {
        this.acting = false
      }
    },
    async saveProjectMarketplace () {
      this.mpSaving = true
      this.mpMsg = ''
      try {
        const { data } = await http.put(`/platform/tenants/${this.tenantId}/project-marketplace`, {
          projectMarketplaceEnabled: this.mpForm.browseEnabled,
          projectMarketplaceAllowPublish: this.mpForm.allowPublish
        })
        if (data.code === 0) {
          this.mpMsg = '已保存'
          await this.load()
        } else {
          this.mpMsg = (data && data.message) || '保存失败'
        }
      } catch (e) {
        this.mpMsg = '请求失败'
      } finally {
        this.mpSaving = false
      }
    },
    async postAction (kind) {
      this.acting = true
      this.actionMsg = ''
      try {
        let path = ''
        if (kind === 'suspend') path = `/platform/tenants/${this.tenantId}/suspend`
        if (kind === 'resume') path = `/platform/tenants/${this.tenantId}/resume`
        if (kind === 'revoke') path = `/platform/tenants/${this.tenantId}/revoke-admin-sessions`
        const { data } = await http.post(path, {})
        if (data.code === 0) {
          this.actionMsg = kind === 'revoke'
            ? `已清除会话 ${data.data && data.data.deletedSessions != null ? data.data.deletedSessions : ''} 条`
            : '操作成功'
          await this.load()
        } else {
          this.actionMsg = (data && data.message) || '失败'
        }
      } catch (e) {
        this.actionMsg = '请求失败'
      } finally {
        this.acting = false
      }
    }
  }
}
</script>

<style scoped>
.tenant-detail-page {
  max-width: 1200px;
  margin: 0 auto;
}
.page-header {
  margin-bottom: 16px;
}
.back-btn {
  font-size: 13px;
  margin-bottom: 4px;
}
.page-title {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
  color: #303133;
}
.stat-cards {
  margin-bottom: 16px;
}
.stat-card {
  margin-bottom: 16px;
}
.stat-card .el-card__body {
  padding: 16px;
}
.stat-label {
  font-size: 12px;
  color: #909399;
  margin-bottom: 8px;
}
.stat-value {
  font-size: 20px;
  font-weight: 700;
  color: #303133;
}
.small-text {
  font-size: 14px;
  word-break: break-all;
}
.mono-text {
  font-family: 'SF Mono', 'Fira Code', 'Consolas', monospace;
}
.info-card {
  margin-bottom: 16px;
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
.action-buttons {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}
.success-text {
  color: #67c23a;
  font-size: 13px;
}
</style>
