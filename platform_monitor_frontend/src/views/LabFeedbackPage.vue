<template>
  <div class="lab-feedback-page">
    <div class="page-header">
      <h2 class="page-title">实验室反馈</h2>
      <p class="page-desc">查看用户提交的实验室功能反馈</p>
    </div>

    <el-card shadow="hover">
      <el-table v-loading="loading" :data="items" stripe size="small" style="width: 100%">
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column prop="tenantId" label="租户 ID" width="80" />
        <el-table-column prop="userId" label="用户 ID" width="80" />
        <el-table-column prop="category" label="分类" width="100" />
        <el-table-column prop="content" label="内容" min-width="300" show-overflow-tooltip />
        <el-table-column prop="createdAt" label="提交时间" min-width="150" />
      </el-table>
      <el-empty v-if="!loading && !items.length" description="暂无反馈数据" :image-size="80" />
    </el-card>
  </div>
</template>

<script>
import { http } from '../api/http'

export default {
  name: 'LabFeedbackPage',
  data () {
    return { loading: true, items: [] }
  },
  mounted () { this.load() },
  methods: {
    async load () {
      this.loading = true
      try {
        const { data } = await http.get('/platform/ops/lab-feedback')
        if (data.code === 0 && Array.isArray(data.data)) this.items = data.data
      } catch (e) { /* ignore */ }
      finally { this.loading = false }
    }
  }
}
</script>

<style scoped>
.lab-feedback-page {
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
