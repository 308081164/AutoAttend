<template>
  <div class="mp-wrap">
    <router-link to="/marketplace" class="back">{{ $t('marketplace.backList') }}</router-link>
    <h1 class="page-title">{{ $t('marketplace.pendingReview') }}</h1>
    <p class="page-desc">{{ $t('marketplace.pendingDesc') }}</p>
    <div v-if="loading" class="placeholder">{{ $t('marketplace.loading') }}</div>
    <table v-else-if="rows.length" class="tbl">
      <thead>
        <tr><th>ID</th><th>{{ $t('marketplace.fieldTitle') }}</th><th>{{ $t('marketplace.publisher') }}</th><th /></tr>
      </thead>
      <tbody>
        <tr v-for="r in rows" :key="r.projectId">
          <td>{{ r.projectId }}</td>
          <td>{{ r.title }}</td>
          <td>{{ r.publisherDisplayName }}</td>
          <td class="actions">
            <button type="button" class="btn-sm primary" @click="approve(r.projectId)">{{ $t('marketplace.approve') }}</button>
            <button type="button" class="btn-sm" @click="reject(r.projectId)">{{ $t('marketplace.reject') }}</button>
          </td>
        </tr>
      </tbody>
    </table>
    <p v-else class="placeholder">{{ $t('marketplace.empty') }}</p>
  </div>
</template>

<script>
export default {
  name: 'MarketplacePendingView',
  data () {
    return { loading: true, rows: [] }
  },
  async mounted () {
    await this.load()
  },
  methods: {
    async load () {
      this.loading = true
      try {
        const r = await this.$http.get('/admin/marketplace/projects/pending')
        if (r.data && r.data.code === 0 && r.data.data) {
          this.rows = r.data.data
        }
      } catch (e) {
        this.rows = []
      } finally {
        this.loading = false
      }
    },
    async approve (id) {
      try {
        const r = await this.$http.post('/admin/marketplace/projects/' + id + '/approve')
        if (r.data && r.data.code === 0) await this.load()
        else alert(r.data.message || 'Error')
      } catch (e) {
        alert('Error')
      }
    },
    async reject (id) {
      const reason = window.prompt(this.$t('marketplace.rejectReason'), '')
      if (reason === null) return
      try {
        const r = await this.$http.post('/admin/marketplace/projects/' + id + '/reject', { reason })
        if (r.data && r.data.code === 0) await this.load()
        else alert(r.data.message || 'Error')
      } catch (e) {
        alert('Error')
      }
    }
  }
}
</script>

<style scoped>
.mp-wrap { max-width: 960px; margin: 0 auto; padding: var(--space-xl); }
.back { display: inline-block; margin-bottom: 16px; font-size: 14px; }
.page-title { font-size: 20px; margin: 0 0 8px; }
.page-desc { font-size: 14px; color: var(--text-secondary); margin: 0 0 16px; }
.tbl { width: 100%; border-collapse: collapse; font-size: 14px; }
.tbl th, .tbl td { border: 1px solid var(--border-primary); padding: 8px 10px; text-align: left; }
.actions { white-space: nowrap; }
.btn-sm { margin-right: 6px; padding: 4px 10px; border-radius: 6px; border: 1px solid var(--border-primary); cursor: pointer; background: transparent; }
.btn-sm.primary { background: var(--brand-blue); color: #fff; border: none; }
.placeholder { color: var(--text-secondary); }
</style>
