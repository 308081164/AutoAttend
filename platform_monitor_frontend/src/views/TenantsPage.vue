<template>
  <div>
    <h2 class="title">租户列表</h2>
    <p v-if="loading" class="muted">加载中…</p>
    <p v-else-if="error" class="err">{{ error }}</p>
    <table v-else class="table">
      <thead>
        <tr>
          <th>ID</th>
          <th>名称</th>
          <th>Slug</th>
          <th>联系方式</th>
          <th>创建时间</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="t in rows" :key="t.tenantId">
          <td>{{ t.tenantId }}</td>
          <td>{{ t.tenantName }}</td>
          <td><code>{{ t.slug }}</code></td>
          <td>{{ t.adminPhone || '—' }}</td>
          <td>{{ t.tenantCreatedAt || '—' }}</td>
        </tr>
      </tbody>
    </table>
  </div>
</template>

<script>
import { http } from '../api/http'

export default {
  name: 'TenantsPage',
  data () {
    return {
      rows: [],
      loading: true,
      error: ''
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
  }
}
</script>

<style scoped>
.title {
  margin: 0 0 16px;
  font-size: 18px;
  font-weight: 600;
}
.muted {
  color: #94a3b8;
}
.err {
  color: #fca5a5;
}
.table {
  width: 100%;
  border-collapse: collapse;
  font-size: 14px;
}
th,
td {
  text-align: left;
  padding: 10px 12px;
  border-bottom: 1px solid #334155;
}
th {
  color: #94a3b8;
  font-weight: 500;
}
code {
  font-size: 13px;
  background: #1e293b;
  padding: 2px 6px;
  border-radius: 4px;
}
</style>
