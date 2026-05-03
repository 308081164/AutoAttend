<template>
  <div class="tenants-page">
    <PlatformOfficialInvitePanel />
    
    <el-card shadow="hover" class="main-card">
      <div slot="header" class="card-header">
        <span>租户列表</span>
        <div class="header-tools">
          <el-input
            v-model.trim="q"
            size="small"
            placeholder="搜索名称、slug、手机…"
            prefix-icon="el-icon-search"
            clearable
            style="width: 220px"
          />
          <el-select v-model="sortKey" size="small" style="width: 140px">
            <el-option label="按 ID" value="id" />
            <el-option label="按成员数" value="members" />
            <el-option label="按 7 日提交" value="commits7d" />
            <el-option label="按存储近似" value="storage" />
          </el-select>
          <el-checkbox v-model="desc">降序</el-checkbox>
        </div>
      </div>

      <el-table
        v-loading="loading"
        :data="filteredRows"
        stripe
        size="small"
        style="width: 100%"
        @row-click="goDetail"
      >
        <el-table-column prop="tenantId" label="ID" width="70" align="center" />
        <el-table-column prop="tenantName" label="名称" min-width="120">
          <template slot-scope="{ row }">
            <el-link type="primary" :underline="false">{{ row.tenantName }}</el-link>
          </template>
        </el-table-column>
        <el-table-column label="Slug" width="120">
          <template slot-scope="{ row }">
            <el-tag size="mini" effect="plain">{{ row.slug }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="planCode" label="套餐" width="90" align="center" />
        <el-table-column prop="billingBaselinePlanCode" label="基线" width="80" align="center">
          <template slot-scope="{ row }">
            <span class="muted-text">{{ row.billingBaselinePlanCode || '—' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="subscriptionEndsAt" label="权益截止" width="100">
          <template slot-scope="{ row }">
            <span class="small-text">{{ row.subscriptionEndsAt || '—' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="80" align="center">
          <template slot-scope="{ row }">
            <el-tag :type="row.status === 'active' ? 'success' : 'danger'" size="mini" effect="plain">
              {{ row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="memberCount" label="成员" width="60" align="center" />
        <el-table-column prop="githubLinkedProjects" label="GH 项目" width="80" align="center" />
        <el-table-column prop="commits24h" label="24h提交" width="80" align="center" />
        <el-table-column prop="commits7d" label="7d提交" width="80" align="center" />
        <el-table-column prop="dauEmails24h" label="DAU≈" width="70" align="center" />
        <el-table-column label="存储≈" width="90" align="center">
          <template slot-scope="{ row }">
            <span class="small-text">{{ formatBytes(row.storageDiffBytesApprox) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="adminPhone" label="手机" width="120">
          <template slot-scope="{ row }">
            {{ row.adminPhone || '—' }}
          </template>
        </el-table-column>
        <el-table-column prop="tenantCreatedAt" label="创建" width="100">
          <template slot-scope="{ row }">
            <span class="small-text">{{ row.tenantCreatedAt || '—' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="80" fixed="right">
          <template slot-scope="{ row }">
            <el-button type="text" size="small" @click.stop="goDetail(row)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
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
        let va, vb
        if (key === 'id') { va = num(a.tenantId); vb = num(b.tenantId) }
        else if (key === 'members') { va = num(a.memberCount); vb = num(b.memberCount) }
        else if (key === 'commits7d') { va = num(a.commits7d); vb = num(b.commits7d) }
        else if (key === 'storage') { va = num(a.storageDiffBytesApprox); vb = num(b.storageDiffBytesApprox) }
        else { va = num(a.tenantId); vb = num(b.tenantId) }
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
    goDetail (row) {
      this.$router.push({ name: 'tenant-detail', params: { id: row.tenantId } })
    },
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
.tenants-page {
  max-width: 1400px;
  margin: 0 auto;
}
.main-card {
  margin-top: 16px;
}
.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 12px;
  font-size: 15px;
  font-weight: 600;
}
.header-tools {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}
.muted-text {
  color: #c0c4cc;
}
.small-text {
  font-size: 12px;
  color: #909399;
}
</style>
