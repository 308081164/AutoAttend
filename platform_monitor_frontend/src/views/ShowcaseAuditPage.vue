<template>
  <div class="showcase-audit-page">
    <div class="page-header">
      <h2 class="page-title">展示审核</h2>
      <p class="page-desc">审核租户提交的项目信息发布内容</p>
    </div>

    <el-card shadow="hover">
      <el-table v-loading="loading" :data="items" stripe size="small" style="width: 100%">
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column prop="tenantId" label="租户 ID" width="80" />
        <el-table-column prop="title" label="标题" min-width="160" />
        <el-table-column prop="status" label="状态" width="90" align="center">
          <template slot-scope="{ row }">
            <el-tag :type="row.status === 'approved' ? 'success' : row.status === 'rejected' ? 'danger' : 'warning'" size="mini" effect="plain">
              {{ row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="submittedAt" label="提交时间" min-width="150" />
        <el-table-column label="操作" width="180" fixed="right">
          <template slot-scope="{ row }">
            <el-button type="success" size="mini" :disabled="row.status !== 'pending'" @click="approve(row)">通过</el-button>
            <el-button type="danger" size="mini" :disabled="row.status !== 'pending'" @click="reject(row)">驳回</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!loading && !items.length" description="暂无待审核内容" :image-size="80" />
    </el-card>
  </div>
</template>

<script>
import { http } from '../api/http'

export default {
  name: 'ShowcaseAuditPage',
  data () {
    return { loading: true, items: [] }
  },
  mounted () { this.load() },
  methods: {
    async load () {
      this.loading = true
      try {
        const { data } = await http.get('/platform/ops/showcase/audit-list')
        if (data.code === 0 && Array.isArray(data.data)) this.items = data.data
      } catch (e) { /* ignore */ }
      finally { this.loading = false }
    },
    async approve (row) {
      try {
        await http.post(`/platform/ops/showcase/${row.id}/approve`)
        row.status = 'approved'
      } catch (e) { /* ignore */ }
    },
    async reject (row) {
      try {
        await http.post(`/platform/ops/showcase/${row.id}/reject`)
        row.status = 'rejected'
      } catch (e) { /* ignore */ }
    }
  }
}
</script>

<style scoped>
.showcase-audit-page {
  max-width: 1200px;
  margin: 0 auto;
}
.page-header {
  margin-bottom: 16px;
}
.page-title {
  margin: 0 0 6px;
  font-size: 20px;
  font-weight: 600;
  color: #303133;
}
.page-desc {
  margin: 0;
  font-size: 13px;
  color: #909399;
}
</style>
