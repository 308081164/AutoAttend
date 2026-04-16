<template>
  <div class="mp-wrap">
    <router-link to="/marketplace" class="back">{{ $t('marketplace.backList') }}</router-link>
    <h1 class="page-title">{{ $t('marketplace.myPublished') }}</h1>
    <div v-if="loading" class="placeholder">{{ $t('marketplace.loading') }}</div>
    <ul v-else class="card-list">
      <li v-for="it in items" :key="it.projectId" class="card">
        <router-link :to="'/marketplace/' + it.projectId" class="card-title">{{ it.title }}</router-link>
        <div class="card-meta">{{ it.status }} · {{ formatTime(it.publishTime || it.createdAt) }}</div>
      </li>
    </ul>
    <p v-if="!loading && !items.length" class="placeholder">{{ $t('marketplace.empty') }}</p>
  </div>
</template>

<script>
export default {
  name: 'MarketplaceMineView',
  data () {
    return { loading: true, items: [] }
  },
  async mounted () {
    await this.load()
  },
  methods: {
    async load () {
      this.loading = true
      try {
        const r = await this.$http.get('/admin/marketplace/me/published-projects')
        if (r.data && r.data.code === 0 && r.data.data) {
          this.items = r.data.data.items || []
        }
      } catch (e) {
        this.items = []
      } finally {
        this.loading = false
      }
    },
    formatTime (v) {
      if (!v) return '—'
      return String(v).replace('T', ' ').slice(0, 19)
    }
  }
}
</script>

<style scoped>
.mp-wrap { max-width: 960px; margin: 0 auto; padding: var(--space-xl); }
.back { display: inline-block; margin-bottom: 16px; font-size: 14px; }
.page-title { font-size: 20px; }
.card-list { list-style: none; padding: 0; margin: 0; }
.card { border: 1px solid var(--border-primary); border-radius: 12px; padding: 12px 16px; margin-bottom: 10px; }
.card-title { font-weight: 600; color: var(--brand-blue); text-decoration: none; }
.card-meta { font-size: 13px; color: var(--text-secondary); margin-top: 6px; }
.placeholder { color: var(--text-secondary); }
</style>
