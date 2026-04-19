<template>
  <div class="quote-list-page">
    <div class="page-head">
      <h1>{{ $t('quote.listTitle') }}</h1>
      <p class="desc">{{ $t('quote.listDesc') }}</p>
      <p class="desc desc-sub">每个项目即一次<strong>解决方案报价</strong>：可在编辑页按<strong>交付物</strong>（Web / App / 后端等）分组维护功能清单，统一计算总价与商务调价。</p>
      <div class="head-actions">
        <router-link to="/quote/config" class="secondary-button">{{ $t('quote.quoteConfigNav') }}</router-link>
        <router-link to="/quote/new" class="primary-button">{{ $t('quote.newProject') }}</router-link>
      </div>
    </div>
    <div v-if="loading" class="placeholder">{{ $t('quote.loading') }}</div>
    <div v-else-if="!items.length" class="placeholder">{{ $t('quote.emptyList') }}</div>
    <table v-else class="data-table">
      <thead>
        <tr>
          <th>ID</th>
          <th>{{ $t('quote.name') }}</th>
          <th>{{ $t('quote.techStack') }}</th>
          <th>操作</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="row in items" :key="row.id">
          <td>{{ row.id }}</td>
          <td>{{ row.name }}</td>
          <td>{{ row.techStack }}</td>
          <td>
            <router-link :to="'/quote/' + row.id">{{ $t('quote.open') }}</router-link>
            <button
              class="danger-button"
              :disabled="deletingId === row.id"
              @click="deleteProject(row.id)"
            >{{ $t('quote.deleteProject') }}</button>
          </td>
        </tr>
      </tbody>
    </table>
  </div>
</template>

<script>
export default {
  name: 'QuoteListView',
  data () {
    return { items: [], loading: true, deletingId: null }
  },
  created () {
    this.load()
  },
  methods: {
    async load () {
      this.loading = true
      try {
        const resp = await this.$http.get('/admin/quote/projects', { params: { page: 1, pageSize: 100 } })
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
    async deleteProject (id) {
      if (!confirm(this.$t('quote.confirmDeleteQuoteProject'))) return
      this.deletingId = id
      try {
        const resp = await this.$http.delete('/admin/quote/projects/' + id)
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
.quote-list-page {
  max-width: 900px;
  margin: 0 auto;
  padding: var(--space-xl);
  color: var(--text-primary);
  background: var(--bg-page);
  min-height: 100%;
  box-sizing: border-box;
}
.page-head {
  margin-bottom: var(--space-xl);
}
.page-head h1 {
  margin: 0 0 var(--space-sm);
  font-size: var(--font-size-xxl);
  font-weight: var(--font-weight-bold);
  color: var(--text-primary);
}
.desc {
  color: var(--text-secondary);
  font-size: var(--font-size-base);
  margin-bottom: var(--space-md);
  line-height: var(--line-height-normal);
  font-weight: var(--font-weight-medium);
}
.desc-sub {
  margin-top: -8px;
  margin-bottom: var(--space-md);
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-normal);
  opacity: 0.95;
}
.head-actions {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-sm);
  align-items: center;
  margin-bottom: var(--space-md);
}
.primary-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: var(--space-sm) var(--space-lg);
  background: var(--brand-blue);
  color: #fff;
  border-radius: var(--radius-sm);
  text-decoration: none;
  font-size: var(--font-size-base);
  font-weight: var(--font-weight-semibold);
  transition: var(--transition-fast);
  box-shadow: var(--shadow-btn);
}
.primary-button:hover {
  background: var(--brand-blue-hover);
}
.secondary-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: var(--space-sm) var(--space-lg);
  background: var(--bg-card);
  color: var(--text-primary);
  border-radius: var(--radius-sm);
  text-decoration: none;
  font-size: var(--font-size-base);
  border: 1px solid var(--border-primary);
  font-weight: var(--font-weight-medium);
  transition: var(--transition-fast);
}
.secondary-button:hover {
  background: var(--bg-hover);
  border-color: var(--border-input-hover);
}
.danger-button {
  margin-left: var(--space-sm);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: var(--space-xs) var(--space-md);
  background: var(--danger);
  color: #fff;
  border-radius: var(--radius-sm);
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-semibold);
  border: 1px solid var(--danger);
  cursor: pointer;
  transition: var(--transition-fast);
}
.danger-button:hover {
  background: #E03E3A;
  border-color: #E03E3A;
}
.danger-button:disabled {
  opacity: 0.45;
  cursor: not-allowed;
}
.data-table {
  width: 100%;
  border-collapse: collapse;
  font-size: var(--font-size-base);
  background: var(--bg-card);
  border: 1px solid var(--border-primary);
  border-radius: var(--radius-md);
  overflow: hidden;
}
.data-table th,
.data-table td {
  padding: var(--space-sm) var(--space-md);
  text-align: left;
  color: var(--text-primary);
  border-bottom: 1px solid var(--border-primary);
}
.data-table th {
  background: var(--bg-hover);
  color: var(--text-primary);
  font-weight: var(--font-weight-semibold);
  font-size: var(--font-size-sm);
}
.data-table tbody tr:hover {
  background: var(--bg-hover);
}
.data-table tbody tr:nth-child(even) {
  background: #FAFBFC;
}
.data-table tbody tr:nth-child(even):hover {
  background: var(--bg-hover);
}
.placeholder {
  padding: var(--space-xxl);
  color: var(--text-secondary);
  font-weight: var(--font-weight-medium);
  text-align: center;
}
</style>
