<template>
  <div class="quote-list-page">
    <div class="page-head">
      <h1>{{ $t('quote.listTitle') }}</h1>
      <p class="desc">{{ $t('quote.listDesc') }}</p>
      <router-link to="/quote/new" class="primary-button">{{ $t('quote.newProject') }}</router-link>
    </div>
    <div v-if="loading" class="placeholder">{{ $t('quote.loading') }}</div>
    <div v-else-if="!items.length" class="placeholder">{{ $t('quote.emptyList') }}</div>
    <table v-else class="data-table">
      <thead>
        <tr>
          <th>ID</th>
          <th>{{ $t('quote.name') }}</th>
          <th>{{ $t('quote.techStack') }}</th>
          <th></th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="row in items" :key="row.id">
          <td>{{ row.id }}</td>
          <td>{{ row.name }}</td>
          <td>{{ row.techStack }}</td>
          <td><router-link :to="'/quote/' + row.id">{{ $t('quote.open') }}</router-link></td>
        </tr>
      </tbody>
    </table>
    <div class="back">
      <router-link to="/">{{ $t('collab.backHome') }}</router-link>
    </div>
  </div>
</template>

<script>
export default {
  name: 'QuoteListView',
  data () {
    return { items: [], loading: true }
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
    }
  }
}
</script>

<style scoped>
.quote-list-page { max-width: 900px; margin: 0 auto; padding: 16px; }
.page-head { margin-bottom: 20px; }
.page-head h1 { margin: 0 0 8px; font-size: 22px; }
.desc { color: #6b7280; font-size: 14px; margin-bottom: 12px; }
.primary-button {
  display: inline-block; padding: 8px 16px; background: #2563eb; color: #fff;
  border-radius: 6px; text-decoration: none; font-size: 14px;
}
.data-table { width: 100%; border-collapse: collapse; font-size: 14px; }
.data-table th, .data-table td { border: 1px solid #e5e7eb; padding: 10px; text-align: left; }
.data-table th { background: #f9fafb; }
.placeholder { padding: 24px; color: #6b7280; }
.back { margin-top: 24px; font-size: 13px; }
.back a { color: #2563eb; }
</style>
