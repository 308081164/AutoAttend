<template>
  <div class="quote-list-page">
    <div class="page-head">
      <h1>{{ $t('quote.listTitle') }}</h1>
      <p class="desc">{{ $t('quote.listDesc') }}</p>
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
    <div class="back">
      <router-link :to="{ name: 'dashboard' }">{{ $t('collab.backHome') }}</router-link>
    </div>
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
  padding: 16px;
  color: #0f172a;
  background: #f1f5f9;
  min-height: 100%;
  box-sizing: border-box;
}
.page-head { margin-bottom: 20px; }
.page-head h1 { margin: 0 0 8px; font-size: 22px; font-weight: 700; color: #020617; }
.desc { color: #475569; font-size: 14px; margin-bottom: 12px; line-height: 1.5; font-weight: 500; }
.head-actions { display: flex; flex-wrap: wrap; gap: 10px; align-items: center; margin-bottom: 12px; }
.primary-button {
  display: inline-block; padding: 8px 16px; background: #1d4ed8; color: #fff;
  border-radius: 6px; text-decoration: none; font-size: 14px; font-weight: 600;
}
.primary-button:hover { background: #1e40af; }
.secondary-button {
  display: inline-block; padding: 8px 16px; background: #e2e8f0; color: #0f172a;
  border-radius: 6px; text-decoration: none; font-size: 14px; border: 1px solid #94a3b8; font-weight: 500;
}
.secondary-button:hover { background: #cbd5e1; }
.danger-button {
  margin-left: 10px;
  display: inline-block;
  padding: 8px 12px;
  background: #dc2626;
  color: #fff;
  border-radius: 6px;
  font-size: 13px;
  font-weight: 600;
  border: 1px solid #b91c1c;
  cursor: pointer;
}
.danger-button:hover { background: #b91c1c; }
.danger-button:disabled { opacity: 0.6; cursor: not-allowed; }
.data-table { width: 100%; border-collapse: collapse; font-size: 14px; background: #fff; }
.data-table th, .data-table td { border: 1px solid #cbd5e1; padding: 10px; text-align: left; color: #1e293b; }
.data-table th { background: #e2e8f0; color: #0f172a; font-weight: 700; }
.data-table td { background: #fff; }
.placeholder { padding: 24px; color: #475569; font-weight: 500; }
.back { margin-top: 24px; font-size: 13px; }
.back a { color: #1d4ed8; font-weight: 500; }
</style>
