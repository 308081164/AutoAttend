<template>
  <div class="prototype-page">
    <div class="page-head">
      <h1>快原型</h1>
      <p class="desc">创建多个原型项目，用 AI 生成 spec，并为客户提供可交互的 preview。</p>
      <div class="head-actions">
        <button type="button" class="primary-button" @click="createProject">
          新建项目
        </button>
        <router-link to="/" class="secondary-button">{{ $t('collab.backHome') || '返回首页' }}</router-link>
      </div>
    </div>

    <div v-if="loading" class="placeholder">加载中…</div>
    <div v-else-if="!items.length" class="placeholder">暂无项目，点击「新建项目」开始。</div>

    <table v-else class="data-table">
      <thead>
        <tr>
          <th>ID</th>
          <th>项目名称</th>
          <th>当前版本</th>
          <th>操作</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="row in items" :key="row.id">
          <td>{{ row.id }}</td>
          <td>{{ row.name }}</td>
          <td>{{ row.currentSpecVersion || '—' }}</td>
          <td>
            <router-link :to="'/prototype/' + row.id" class="link-button">进入</router-link>
            <button
              type="button"
              class="link-button danger"
              :disabled="deletingId === row.id"
              @click="deleteProject(row.id)"
            >
              删除
            </button>
          </td>
        </tr>
      </tbody>
    </table>
  </div>
</template>

<script>
export default {
  name: 'PrototypeListView',
  data () {
    return { items: [], loading: true, deletingId: null }
  },
  async created () {
    await this.load()
    if (this.$route.query && String(this.$route.query.create) === '1') {
      this.$router.replace({ path: '/prototype', query: {} })
      this.$nextTick(() => this.createProject())
    }
  },
  methods: {
    async load () {
      this.loading = true
      try {
        const resp = await this.$http.get('/admin/ui-prototype/projects')
        if (resp.data && resp.data.code === 0 && resp.data.data) {
          this.items = resp.data.data.items || []
        } else {
          this.items = []
        }
      } catch (e) {
        this.items = []
      } finally {
        this.loading = false
      }
    },
    async createProject () {
      const name = window.prompt('请输入项目名称', '新原型项目')
      if (!name) return
      try {
        const resp = await this.$http.post('/admin/ui-prototype/projects', { name })
        if (resp.data && resp.data.code === 0 && resp.data.data && resp.data.data.id) {
          await this.load()
          this.$router.push({ path: '/prototype/' + resp.data.data.id })
        } else {
          alert((resp.data && resp.data.message) || '创建失败')
        }
      } catch (e) {
        alert((e.response && e.response.data && e.response.data.message) || '创建失败')
      }
    },
    async deleteProject (id) {
      if (!confirm('确认删除该原型项目？')) return
      this.deletingId = id
      try {
        const resp = await this.$http.delete('/admin/ui-prototype/projects/' + id)
        if (resp.data && resp.data.code === 0) {
          await this.load()
        } else {
          alert((resp.data && resp.data.message) || '删除失败')
        }
      } catch (e) {
        alert((e.response && e.response.data && e.response.data.message) || '删除失败')
      } finally {
        this.deletingId = null
      }
    }
  }
}
</script>

<style scoped>
.prototype-page {
  max-width: 1000px;
  margin: 0 auto;
  padding: 16px;
  color: #0f172a;
}
.page-head {
  margin-bottom: 16px;
}
.page-head h1 {
  margin: 0 0 8px;
  font-size: 22px;
  font-weight: 800;
}
.desc {
  color: #475569;
  font-size: 14px;
  margin: 0 0 12px;
  line-height: 1.5;
}
.head-actions {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  align-items: center;
}
.primary-button {
  display: inline-block;
  padding: 8px 16px;
  background: #1d4ed8;
  color: #fff;
  border-radius: 8px;
  text-decoration: none;
  font-size: 14px;
  font-weight: 700;
  border: 1px solid #1d4ed8;
  cursor: pointer;
}
.secondary-button {
  display: inline-block;
  padding: 8px 16px;
  background: #e2e8f0;
  color: #0f172a;
  border-radius: 8px;
  text-decoration: none;
  font-size: 14px;
  border: 1px solid #94a3b8;
  font-weight: 600;
}
.placeholder {
  padding: 24px;
  color: #475569;
  font-weight: 600;
  background: #ffffff;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
}
.data-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 14px;
  background: #ffffff;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  overflow: hidden;
}
.data-table th, .data-table td {
  border-bottom: 1px solid #e5e7eb;
  padding: 10px;
  text-align: left;
  color: #1e293b;
}
.data-table th {
  background: #f1f5f9;
  font-weight: 800;
}
.data-table td {
  background: #fff;
}
.link-button {
  margin-right: 10px;
  color: #2563eb;
  font-weight: 700;
  background: transparent;
  border: none;
  cursor: pointer;
  padding: 0;
  font-size: 13px;
}
.link-button:hover {
  text-decoration: underline;
}
.link-button.danger {
  color: #dc2626;
}
.link-button.danger:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}
</style>

