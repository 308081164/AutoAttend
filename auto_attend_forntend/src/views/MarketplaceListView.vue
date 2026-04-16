<template>
  <div class="mp-wrap">
    <div class="page-header">
      <h1 class="page-title">{{ $t('marketplace.title') }}</h1>
      <p class="page-desc">{{ $t('marketplace.desc') }}</p>
      <p v-if="statusMsg" class="hint-warn">{{ statusMsg }}</p>
    </div>

    <div v-if="loadingStatus" class="placeholder">{{ $t('marketplace.loading') }}</div>
    <template v-else-if="moduleVisible">
      <div class="toolbar">
        <input v-model="filters.q" type="search" class="inp" :placeholder="$t('marketplace.searchPlaceholder')" @keyup.enter="loadList">
        <input v-model="filters.tech" type="text" class="inp narrow" :placeholder="$t('marketplace.techPlaceholder')">
        <select v-model="filters.sort" class="inp narrow" @change="loadList">
          <option value="newest">{{ $t('marketplace.sortNewest') }}</option>
          <option value="hot">{{ $t('marketplace.sortHot') }}</option>
          <option value="budget">{{ $t('marketplace.sortBudget') }}</option>
        </select>
        <button type="button" class="primary-button" @click="loadList">{{ $t('marketplace.search') }}</button>
        <button v-if="canPublish" type="button" class="secondary-button" @click="openCreate">{{ $t('marketplace.publish') }}</button>
        <router-link v-if="canPublish" to="/marketplace/mine" class="link-inline">{{ $t('marketplace.myPublished') }}</router-link>
        <router-link v-if="canPublish" to="/marketplace/pending" class="link-inline">{{ $t('marketplace.pendingReview') }}</router-link>
      </div>

      <div v-if="loadingList" class="placeholder">{{ $t('marketplace.loading') }}</div>
      <div v-else-if="!items.length" class="placeholder">{{ $t('marketplace.empty') }}</div>
      <ul v-else class="card-list">
        <li v-for="it in items" :key="it.projectId" class="card">
          <router-link :to="'/marketplace/' + it.projectId" class="card-title">{{ it.title }}</router-link>
          <div class="card-meta">
            <span>{{ $t('marketplace.publisher') }}：{{ it.publisherDisplayName || '—' }}</span>
            <span>{{ $t('marketplace.statusLabel') }}：{{ it.status }}</span>
            <span v-if="it.publishTime">{{ formatTime(it.publishTime) }}</span>
          </div>
          <p class="card-snippet">{{ snippet(it.description) }}</p>
        </li>
      </ul>

      <div v-if="total > pageSize" class="pager">
        <button type="button" class="secondary-button" :disabled="page <= 1" @click="page--; loadList()">{{ $t('marketplace.prev') }}</button>
        <span class="muted">{{ page }} / {{ totalPages }}</span>
        <button type="button" class="secondary-button" :disabled="page >= totalPages" @click="page++; loadList()">{{ $t('marketplace.next') }}</button>
      </div>
    </template>

    <div v-else class="placeholder">{{ $t('marketplace.moduleOff') }}</div>

    <div v-if="showForm" class="modal-mask" @click.self="showForm = false">
      <div class="modal-card wide">
        <h3>{{ $t('marketplace.publish') }}</h3>
        <div class="form-row">
          <label>{{ $t('marketplace.fieldTitle') }} *</label>
          <input v-model="form.title" type="text" class="inp">
        </div>
        <div class="form-row">
          <label>{{ $t('marketplace.fieldDesc') }}</label>
          <textarea v-model="form.description" rows="5" class="inp"></textarea>
        </div>
        <div class="form-row">
          <label>{{ $t('marketplace.fieldTech') }}</label>
          <input v-model="form.techStackStr" type="text" class="inp" placeholder="Vue, Java, …">
        </div>
        <div class="form-row two">
          <div>
            <label>{{ $t('marketplace.fieldBudget') }}</label>
            <input v-model="form.budgetRange" type="text" class="inp">
          </div>
          <div>
            <label>{{ $t('marketplace.fieldDuration') }}</label>
            <input v-model="form.duration" type="text" class="inp">
          </div>
        </div>
        <div class="form-row">
          <label>{{ $t('marketplace.fieldLocation') }}</label>
          <input v-model="form.location" type="text" class="inp">
        </div>
        <div class="form-row two">
          <div>
            <label>{{ $t('marketplace.fieldContactType') }}</label>
            <select v-model="form.contactType" class="inp">
              <option value="phone">{{ $t('marketplace.contactPhone') }}</option>
              <option value="email">{{ $t('marketplace.contactEmail') }}</option>
              <option value="internal">{{ $t('marketplace.contactInternal') }}</option>
            </select>
          </div>
          <div>
            <label>{{ $t('marketplace.fieldContact') }} *</label>
            <input v-model="form.contactValue" type="text" class="inp">
          </div>
        </div>
        <p class="form-hint">{{ $t('marketplace.disclaimerHint') }}</p>
        <div class="modal-actions">
          <button class="primary-button" :disabled="saving" @click="submitCreate">{{ saving ? '…' : $t('marketplace.submit') }}</button>
          <button class="secondary-button" @click="showForm = false">{{ $t('teamManage.cancel') }}</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'MarketplaceListView',
  data () {
    return {
      loadingStatus: true,
      loadingList: false,
      moduleVisible: false,
      canPublish: false,
      statusMsg: '',
      items: [],
      total: 0,
      page: 1,
      pageSize: 20,
      filters: { q: '', tech: '', sort: 'newest' },
      showForm: false,
      saving: false,
      form: {
        title: '',
        description: '',
        techStackStr: '',
        budgetRange: '',
        duration: '',
        location: '',
        contactType: 'phone',
        contactValue: ''
      }
    }
  },
  computed: {
    totalPages () {
      return Math.max(1, Math.ceil(this.total / this.pageSize))
    }
  },
  async mounted () {
    await this.loadStatus()
    if (this.moduleVisible) {
      await this.loadList()
    }
  },
  methods: {
    async loadStatus () {
      this.loadingStatus = true
      try {
        const r = await this.$http.get('/admin/marketplace/status')
        if (r.data && r.data.code === 0 && r.data.data) {
          this.moduleVisible = !!r.data.data.moduleVisible
          this.canPublish = !!r.data.data.canPublish
          if (!this.moduleVisible) {
            this.statusMsg = this.$t('marketplace.moduleOffHint')
          }
        }
      } catch (e) {
        this.moduleVisible = false
      } finally {
        this.loadingStatus = false
      }
    },
    async loadList () {
      this.loadingList = true
      try {
        const r = await this.$http.get('/admin/marketplace/projects', {
          params: {
            q: this.filters.q || undefined,
            tech: this.filters.tech || undefined,
            sort: this.filters.sort,
            page: this.page,
            pageSize: this.pageSize
          }
        })
        if (r.data && r.data.code === 0 && r.data.data) {
          this.items = r.data.data.items || []
          this.total = r.data.data.total || 0
        } else {
          this.items = []
          if (r.data && r.data.code === 40300) {
            this.statusMsg = r.data.message || ''
            this.moduleVisible = false
          }
        }
      } catch (e) {
        this.items = []
      } finally {
        this.loadingList = false
      }
    },
    snippet (t) {
      if (!t) return ''
      const s = String(t).replace(/\s+/g, ' ').trim()
      return s.length > 160 ? s.slice(0, 160) + '…' : s
    },
    formatTime (v) {
      if (!v) return ''
      return String(v).replace('T', ' ').slice(0, 19)
    },
    openCreate () {
      this.form = {
        title: '',
        description: '',
        techStackStr: '',
        budgetRange: '',
        duration: '',
        location: '',
        contactType: 'phone',
        contactValue: ''
      }
      this.showForm = true
    },
    parseTech () {
      const s = (this.form.techStackStr || '').trim()
      if (!s) return []
      return s.split(/[,，]/).map(x => x.trim()).filter(Boolean)
    },
    async submitCreate () {
      if (!(this.form.title || '').trim()) {
        alert(this.$t('marketplace.needTitle'))
        return
      }
      if (!(this.form.contactValue || '').trim()) {
        alert(this.$t('marketplace.needContact'))
        return
      }
      this.saving = true
      try {
        const body = {
          title: this.form.title.trim(),
          description: this.form.description || '',
          techStack: this.parseTech(),
          budgetRange: this.form.budgetRange || '',
          duration: this.form.duration || '',
          location: this.form.location || '',
          contactType: this.form.contactType,
          contactValue: this.form.contactValue.trim()
        }
        const r = await this.$http.post('/admin/marketplace/projects', body)
        if (r.data && r.data.code === 0) {
          this.showForm = false
          await this.loadList()
          alert(this.$t('marketplace.createOk'))
        } else {
          alert((r.data && r.data.message) || 'Error')
        }
      } catch (e) {
        alert(this.$t('marketplace.saveFailed'))
      } finally {
        this.saving = false
      }
    }
  }
}
</script>

<style scoped>
.mp-wrap { max-width: 960px; margin: 0 auto; padding: var(--space-xl); min-height: 100vh; box-sizing: border-box; }
.page-title { font-size: 22px; margin: 0 0 8px; }
.page-desc { color: var(--text-secondary); margin: 0 0 12px; }
.hint-warn { color: var(--text-secondary); font-size: 13px; }
.toolbar { display: flex; flex-wrap: wrap; gap: 8px; align-items: center; margin-bottom: 16px; }
.inp { flex: 1; min-width: 120px; padding: 8px 10px; border-radius: 8px; border: 1px solid var(--border-primary); }
.inp.narrow { max-width: 200px; flex: none; }
.primary-button, .secondary-button { padding: 8px 16px; border-radius: 8px; border: none; cursor: pointer; }
.primary-button { background: var(--brand-blue); color: #fff; }
.secondary-button { background: transparent; border: 1px solid var(--border-primary); }
.link-inline { margin-left: 8px; font-size: 14px; }
.card-list { list-style: none; padding: 0; margin: 0; }
.card { border: 1px solid var(--border-primary); border-radius: 12px; padding: 16px; margin-bottom: 12px; }
.card-title { font-size: 17px; font-weight: 600; color: var(--brand-blue); text-decoration: none; }
.card-meta { font-size: 13px; color: var(--text-secondary); display: flex; flex-wrap: wrap; gap: 12px; margin: 8px 0; }
.card-snippet { margin: 0; font-size: 14px; color: var(--text-primary); line-height: 1.5; }
.pager { display: flex; align-items: center; gap: 12px; margin-top: 16px; }
.muted { color: var(--text-secondary); }
.modal-mask { position: fixed; inset: 0; background: rgba(0,0,0,.45); display: flex; align-items: center; justify-content: center; z-index: 50; padding: 16px; }
.modal-card { background: var(--bg-page); border-radius: 12px; padding: 20px; max-width: 520px; width: 100%; max-height: 90vh; overflow: auto; }
.modal-card.wide { max-width: 640px; }
.form-row { margin-bottom: 12px; }
.form-row.two { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; }
.form-row label { display: block; font-size: 13px; margin-bottom: 4px; }
.form-hint { font-size: 12px; color: var(--text-secondary); }
.modal-actions { margin-top: 16px; display: flex; gap: 8px; }
.placeholder { color: var(--text-secondary); padding: 24px 0; }
</style>
