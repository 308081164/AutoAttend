<template>
  <div class="prototype-page">
    <div class="page-head">
      <h1>快原型</h1>
      <p class="desc">创建多个原型项目，用 AI 生成 spec，并为客户提供可交互的 preview。</p>
      <div class="head-actions">
        <button type="button" class="primary-button" @click="createProject">
          新建项目
        </button>
        <router-link :to="{ name: 'dashboard' }" class="secondary-button">{{ $t('collab.backHome') || '返回首页' }}</router-link>
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
  padding: var(--space-xl);
  color: var(--text-primary);
  background: var(--bg-page);
  min-height: 100vh;
  box-sizing: border-box;
}
.page-head {
  margin-bottom: var(--space-xl);
}
.page-head h1 {
  margin: 0 0 var(--space-sm);
  font-size: 22px;
  font-weight: 600;
  color: var(--text-primary);
}
.desc {
  color: var(--text-secondary);
  font-size: 14px;
  margin: 0 0 var(--space-md);
  line-height: 1.6;
}
.head-actions {
  display: flex;
  gap: var(--space-sm);
  flex-wrap: wrap;
  align-items: center;
}
.primary-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: var(--space-sm) var(--space-lg);
  background: var(--brand-blue);
  color: #fff;
  border-radius: var(--radius-md);
  text-decoration: none;
  font-size: 14px;
  font-weight: 500;
  border: none;
  cursor: pointer;
  transition: opacity 0.2s;
  box-shadow: var(--shadow-sm);
}
.primary-button:hover {
  opacity: 0.85;
}
.secondary-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: var(--space-sm) var(--space-lg);
  background: var(--bg-card);
  color: var(--text-primary);
  border-radius: var(--radius-md);
  text-decoration: none;
  font-size: 14px;
  border: 1px solid var(--border-primary);
  font-weight: 500;
  transition: border-color 0.2s;
}
.secondary-button:hover {
  border-color: var(--brand-blue);
  color: var(--brand-blue);
}
.placeholder {
  padding: var(--space-xxl);
  color: var(--text-tertiary);
  font-weight: 500;
  background: var(--bg-card);
  border: 1px solid var(--border-primary);
  border-radius: var(--radius-lg);
  text-align: center;
  box-shadow: var(--shadow-sm);
}
.data-table {
  width: 100%;
  border-collapse: separate;
  border-spacing: 0;
  font-size: 14px;
  background: var(--bg-card);
  border: 1px solid var(--border-primary);
  border-radius: var(--radius-lg);
  overflow: hidden;
  box-shadow: var(--shadow-sm);
}
.data-table th,
.data-table td {
  border-bottom: 1px solid var(--border-primary);
  padding: var(--space-sm) var(--space-md);
  text-align: left;
  color: var(--text-primary);
}
.data-table th {
  background: var(--bg-page);
  font-weight: 600;
  color: var(--text-secondary);
  font-size: 13px;
}
.data-table td {
  background: var(--bg-card);
}
.data-table tr:last-child td {
  border-bottom: none;
}
.data-table tr:hover td {
  background: var(--bg-page);
}
.link-button {
  margin-right: var(--space-sm);
  color: var(--brand-blue);
  font-weight: 500;
  background: transparent;
  border: none;
  cursor: pointer;
  padding: 0;
  font-size: 13px;
  transition: opacity 0.2s;
}
.link-button:hover {
  opacity: 0.75;
  text-decoration: underline;
}
.link-button.danger {
  color: var(--danger);
}
.link-button.danger:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}
</style>

