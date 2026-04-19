<template>
  <div class="wrap">
    <p>
      <router-link :to="{ name: 'tenants' }" class="back">← 返回租户列表</router-link>
    </p>
    <h2 class="title">租户 #{{ tenantId }}</h2>
    <p v-if="loading" class="muted">加载中…</p>
    <p v-else-if="error" class="err">{{ error }}</p>
    <template v-else-if="tenant">
      <section class="cards">
        <div class="card">
          <div class="card-label">MRR 近似（近30天模拟收入，全站）</div>
          <div class="card-value">{{ formatYuan(mrrApproxCents) }}</div>
        </div>
        <div class="card">
          <div class="card-label">当前权益档位</div>
          <div class="card-value mono">{{ ops.planCode || '—' }}</div>
        </div>
        <div class="card">
          <div class="card-label">到期回退档位</div>
          <div class="card-value mono">{{ ops.billingBaselinePlanCode || '—' }}</div>
        </div>
        <div class="card">
          <div class="card-label">权益截止</div>
          <div class="card-value small">{{ ops.subscriptionEndsAt || '—' }}</div>
        </div>
      </section>

      <section class="panel">
        <h3>基本信息</h3>
        <table class="kv">
          <tr><td>名称</td><td>{{ ops.tenantName }}</td></tr>
          <tr><td>Slug</td><td><code>{{ ops.slug }}</code></td></tr>
          <tr><td>状态</td><td>{{ ops.status }}</td></tr>
          <tr><td>管理员手机</td><td>{{ ops.adminPhone || '—' }}</td></tr>
          <tr><td>创建时间</td><td>{{ ops.tenantCreatedAt || '—' }}</td></tr>
          <tr><td>官方 API 额度（元）</td><td>{{ formatOfficialYuan(ops.officialApiCnyBalance) }}</td></tr>
        </table>
      </section>

      <section class="panel">
        <h3>项目信息发布（租户级）</h3>
        <p class="muted small">控制该租户管理员控制台是否出现「项目信息」入口，以及是否允许在本租户内发布（仍须在租户管理中为账号勾选可发布）。</p>
        <div class="mp-row">
          <label class="mp-check">
            <input v-model="mpBrowseEnabled" type="checkbox" :disabled="mpSaving">
            <span>对本租户开放浏览（列表/详情）</span>
          </label>
        </div>
        <div class="mp-row">
          <label class="mp-check">
            <input v-model="mpAllowPublish" type="checkbox" :disabled="mpSaving || !mpBrowseEnabled">
            <span>允许在本租户发布与审核（需同时开启浏览）</span>
          </label>
        </div>
        <div class="btn-row" style="margin-top:12px">
          <button type="button" class="btn ok" :disabled="mpSaving" @click="saveProjectMarketplace">保存项目信息设置</button>
        </div>
        <p v-if="mpMsg" class="ok">{{ mpMsg }}</p>
      </section>

      <section class="panel">
        <h3>用量与活跃</h3>
        <table class="kv">
          <tr><td>成员数</td><td>{{ ops.memberCount }}</td></tr>
          <tr><td>GitHub 已绑定项目</td><td>{{ ops.githubLinkedProjects }}</td></tr>
          <tr><td>24h 提交</td><td>{{ ops.commits24h }}</td></tr>
          <tr><td>7d 提交</td><td>{{ ops.commits7d }}</td></tr>
          <tr><td>DAU 近似（24h 邮箱）</td><td>{{ ops.dauEmails24h }}</td></tr>
          <tr><td>Diff 存储近似</td><td>{{ formatBytes(ops.storageDiffBytesApprox) }}</td></tr>
        </table>
      </section>

      <section class="panel actions">
        <h3>运维动作（需审计）</h3>
        <p class="muted small">暂停后该租户管理员无法登录控制台；恢复后可继续使用。强制下线将清除该租户全部管理员会话。</p>
        <div class="btn-row">
          <button type="button" class="btn ok" :disabled="acting" @click="doGrantProPlus">一键开通专业增强版（365天）</button>
          <button type="button" class="btn danger" :disabled="acting" @click="doSuspend">暂停租户</button>
          <button type="button" class="btn" :disabled="acting" @click="doResume">恢复服务</button>
          <button type="button" class="btn warn" :disabled="acting" @click="doRevoke">强制下线管理员会话</button>
        </div>
        <p v-if="actionMsg" class="ok">{{ actionMsg }}</p>
      </section>
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
      mpBrowseEnabled: true,
      mpAllowPublish: false,
      mpSaving: false,
      mpMsg: ''
    }
  },
  computed: {
    tenantId () {
      return Number(this.$route.params.id)
    },
    /** 详情接口 tenant.ops 为运维报表行；兼容旧结构（字段在 tenant 根上） */
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
          const b = tr.projectMarketplaceEnabled
          const p = tr.projectMarketplaceAllowPublish
          this.mpBrowseEnabled = b !== false
          this.mpAllowPublish = p === true
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
      if (!window.confirm('确认暂停该租户？')) return
      await this.postAction('suspend')
    },
    async doResume () {
      if (!window.confirm('确认恢复该租户服务？')) return
      await this.postAction('resume')
    },
    async doRevoke () {
      if (!window.confirm('确认清除该租户全部管理员会话？')) return
      await this.postAction('revoke')
    },
    async doGrantProPlus () {
      if (!window.confirm('确认为该租户开通专业增强版（pro+）权益 365 天？（不产生订单）')) return
      await this.postGrantProPlus()
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
          projectMarketplaceEnabled: this.mpBrowseEnabled,
          projectMarketplaceAllowPublish: this.mpAllowPublish
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
.wrap { max-width: 1000px; margin: 0 auto; }
.back { color: #93c5fd; text-decoration: none; font-size: 14px; }
.back:hover { text-decoration: underline; }
.title { margin: 12px 0 16px; font-size: 22px; font-weight: 700; }
.muted { color: #94a3b8; }
.small { font-size: 14px; word-break: break-all; }
.err { color: #fca5a5; }
.ok { color: #86efac; margin-top: 10px; }
.mono { font-family: ui-monospace, monospace; }

.cards {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 16px;
}
.card {
  background: #1e293b;
  border: 1px solid #334155;
  border-radius: 12px;
  padding: 14px;
}
.card-label { color: #94a3b8; font-size: 12px; margin-bottom: 6px; }
.card-value { font-size: 18px; font-weight: 700; }

.panel {
  margin-top: 14px;
  padding: 16px;
  background: #0b1224;
  border: 1px solid #1f2a44;
  border-radius: 12px;
}
.panel h3 { margin: 0 0 12px; font-size: 16px; }
.kv { width: 100%; border-collapse: collapse; font-size: 14px; }
.kv td { padding: 8px 10px; border-bottom: 1px solid #334155; }
.kv td:first-child { color: #94a3b8; width: 180px; }
code { background: #1e293b; padding: 2px 6px; border-radius: 4px; }

.mp-row { margin-top: 10px; }
.mp-check { display: flex; align-items: flex-start; gap: 10px; cursor: pointer; color: #e2e8f0; font-size: 14px; }
.mp-check input { margin-top: 3px; }

.actions .btn-row { display: flex; flex-wrap: wrap; gap: 10px; margin-top: 10px; }
.btn {
  padding: 8px 14px;
  border-radius: 8px;
  border: 1px solid #475569;
  background: #1e293b;
  color: #e2e8f0;
  cursor: pointer;
  font-size: 14px;
}
.btn:disabled { opacity: 0.5; cursor: not-allowed; }
.btn.danger { border-color: #b91c1c; background: #7f1d1d; }
.btn.warn { border-color: #ca8a04; background: #713f12; }
.btn.ok { border-color: #15803d; background: #14532d; color: #ecfdf5; }
.small { font-size: 13px; }
@media (max-width: 900px) {
  .cards { grid-template-columns: repeat(2, 1fr); }
}
</style>
