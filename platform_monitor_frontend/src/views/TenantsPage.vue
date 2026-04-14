<template>
  <div>
    <PlatformOfficialInvitePanel />
    <h2 class="title">租户列表</h2>
    <div class="toolbar">
      <input v-model.trim="q" type="search" class="search" placeholder="搜索名称、slug、手机…" aria-label="filter">
      <select v-model="sortKey" class="select">
        <option value="id">按 ID</option>
        <option value="members">按成员数</option>
        <option value="commits7d">按 7 日提交</option>
        <option value="storage">按存储近似</option>
      </select>
      <label class="chk"><input v-model="desc" type="checkbox"> 降序</label>
    </div>
    <p v-if="loading" class="muted">加载中…</p>
    <p v-else-if="error" class="err">{{ error }}</p>
    <div v-else class="table-wrap">
      <table class="table">
        <thead>
          <tr>
            <th>ID</th>
            <th>名称</th>
            <th>Slug</th>
            <th>套餐</th>
            <th>基线</th>
            <th>权益截止</th>
            <th>状态</th>
            <th>成员</th>
            <th>GH 项目</th>
            <th>24h提交</th>
            <th>7d提交</th>
            <th>DAU≈</th>
            <th>存储≈</th>
            <th>手机</th>
            <th>创建</th>
            <th></th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="t in filteredRows" :key="t.tenantId">
            <td>{{ t.tenantId }}</td>
            <td>{{ t.tenantName }}</td>
            <td><code>{{ t.slug }}</code></td>
            <td class="mono">{{ t.planCode }}</td>
            <td class="mono muted-sm">{{ t.billingBaselinePlanCode || '—' }}</td>
            <td class="small">{{ t.subscriptionEndsAt || '—' }}</td>
            <td>{{ t.status }}</td>
            <td>{{ t.memberCount }}</td>
            <td>{{ t.githubLinkedProjects }}</td>
            <td>{{ t.commits24h }}</td>
            <td>{{ t.commits7d }}</td>
            <td>{{ t.dauEmails24h }}</td>
            <td class="small">{{ formatBytes(t.storageDiffBytesApprox) }}</td>
            <td>{{ t.adminPhone || '—' }}</td>
            <td class="small">{{ t.tenantCreatedAt || '—' }}</td>
            <td>
              <router-link class="link" :to="{ name: 'tenant-detail', params: { id: t.tenantId } }">详情</router-link>
            </td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>

<script>
import { http } from '../api/http'
import PlatformOfficialInvitePanel from '../components/PlatformOfficialInvitePanel.vue'

export default {
  name: 'TenantsPage',
  components: { PlatformOfficialInvitePanel },
  data () {
    return {
      rows: [],
      loading: true,
      error: '',
      q: '',
      sortKey: 'id',
      desc: false
    }
  },
  computed: {
    filteredRows () {
      let list = this.rows.slice()
      const qq = (this.q || '').toLowerCase()
      if (qq) {
        list = list.filter(t => {
          const name = (t.tenantName || '').toLowerCase()
          const slug = (t.slug || '').toLowerCase()
          const phone = (t.adminPhone || '').toLowerCase()
          return name.includes(qq) || slug.includes(qq) || phone.includes(qq)
        })
      }
      const key = this.sortKey
      const dir = this.desc ? -1 : 1
      const num = (v) => (v == null || Number.isNaN(Number(v)) ? 0 : Number(v))
      list.sort((a, b) => {
        let va
        let vb
        if (key === 'id') {
          va = num(a.tenantId); vb = num(b.tenantId)
        } else if (key === 'members') {
          va = num(a.memberCount); vb = num(b.memberCount)
        } else if (key === 'commits7d') {
          va = num(a.commits7d); vb = num(b.commits7d)
        } else if (key === 'storage') {
          va = num(a.storageDiffBytesApprox); vb = num(b.storageDiffBytesApprox)
        } else {
          va = num(a.tenantId); vb = num(b.tenantId)
        }
        return (va - vb) * dir
      })
      return list
    }
  },
  async mounted () {
    try {
      const { data } = await http.get('/platform/ops/reports/tenants')
      if (data.code === 0 && data.data) {
        this.rows = data.data
      } else {
        this.error = (data && data.message) || '加载失败'
      }
    } catch (e) {
      this.error = '请求失败'
    } finally {
      this.loading = false
    }
  },
  methods: {
    formatBytes (b) {
      if (b == null) return '—'
      const n = Number(b)
      if (!Number.isFinite(n) || n < 0) return '—'
      if (n < 1024) return `${n} B`
      if (n < 1024 * 1024) return `${(n / 1024).toFixed(1)} KB`
      if (n < 1024 * 1024 * 1024) return `${(n / 1024 / 1024).toFixed(1)} MB`
      return `${(n / 1024 / 1024 / 1024).toFixed(2)} GB`
    }
  }
}
</script>

<style scoped>
.title {
  margin: 0 0 16px;
  font-size: 18px;
  font-weight: 600;
}
.toolbar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px;
  margin-bottom: 12px;
}
.search {
  flex: 1;
  min-width: 200px;
  padding: 8px 10px;
  border-radius: 8px;
  border: 1px solid #334155;
  background: #0f172a;
  color: #e2e8f0;
  font-size: 14px;
}
.select {
  padding: 8px 10px;
  border-radius: 8px;
  border: 1px solid #334155;
  background: #1e293b;
  color: #e2e8f0;
  font-size: 14px;
}
.chk { font-size: 13px; color: #94a3b8; }
.muted { color: #94a3b8; }
.muted-sm { color: #64748b; font-size: 12px; }
.err { color: #fca5a5; }
.table-wrap {
  overflow-x: auto;
}
.table {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
  min-width: 1100px;
}
th,
td {
  text-align: left;
  padding: 8px 10px;
  border-bottom: 1px solid #334155;
  vertical-align: top;
}
th {
  color: #94a3b8;
  font-weight: 500;
  white-space: nowrap;
}
code {
  font-size: 12px;
  background: #1e293b;
  padding: 2px 6px;
  border-radius: 4px;
}
.small { font-size: 12px; white-space: nowrap; }
.mono { font-family: ui-monospace, monospace; }
.link {
  color: #93c5fd;
  text-decoration: none;
  white-space: nowrap;
}
.link:hover { text-decoration: underline; }
</style>
