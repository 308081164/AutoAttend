<template>
  <div class="tenant-admin-manage">
    <div class="page-header">
      <h1 class="page-title">{{ $t('tenantAdminManage.title') }}</h1>
      <p class="page-desc">{{ $t('tenantAdminManage.desc') }}</p>
    </div>

    <div v-if="loading" class="placeholder">{{ $t('tenantAdminManage.loading') }}</div>
    <div v-else-if="!rows.length" class="placeholder">{{ $t('tenantAdminManage.empty') }}</div>
    <div v-else class="table-wrapper">
      <table class="table">
        <thead>
          <tr>
            <th>ID</th>
            <th>{{ $t('tenantAdminManage.phone') }}</th>
            <th>{{ $t('tenantAdminManage.createdAt') }}</th>
            <th>{{ $t('dashboard.actions') }}</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="u in rows" :key="u.id">
            <td>{{ u.id }}</td>
            <td><code class="mono">{{ u.phone }}</code></td>
            <td>{{ formatTime(u.createdAt) }}</td>
            <td>
              <button class="link-button" @click="openEdit(u)">{{ $t('tenantAdminManage.edit') }}</button>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <div v-if="showEditModal" class="modal-mask" @click.self="showEditModal = false">
      <div class="modal-card">
        <h3>{{ $t('tenantAdminManage.edit') }}</h3>
        <p v-if="editTarget" class="form-hint">{{ $t('tenantAdminManage.editHint') }}</p>
        <div class="form-row">
          <label>{{ $t('tenantAdminManage.phone') }}</label>
          <input v-model="editForm.phone" type="text" :placeholder="$t('login.phoneHint')">
        </div>
        <div class="form-row">
          <label>{{ $t('tenantAdminManage.newPasswordOptional') }}</label>
          <input v-model="editForm.newPassword" type="password" autocomplete="new-password" :placeholder="$t('tenantAdminManage.leaveBlank')">
        </div>
        <div class="modal-actions">
          <button class="primary-button" @click="saveEdit" :disabled="saving">{{ $t('teamManage.save') }}</button>
          <button class="secondary-button" @click="showEditModal = false">{{ $t('teamManage.cancel') }}</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'TenantAdminManageView',
  data () {
    return {
      rows: [],
      loading: true,
      currentUserId: null,
      showEditModal: false,
      editTarget: null,
      editForm: { phone: '', newPassword: '' },
      saving: false
    }
  },
  async mounted () {
    await this.loadMe()
    await this.loadList()
  },
  methods: {
    async loadMe () {
      try {
        const r = await this.$http.get('/admin/auth/me')
        if (r.data && r.data.code === 0 && r.data.data && r.data.data.userId != null) {
          this.currentUserId = r.data.data.userId
        }
      } catch (e) { /* ignore */ }
    },
    async loadList () {
      this.loading = true
      try {
        const r = await this.$http.get('/admin/tenant-admins')
        if (r.data && r.data.code === 0 && r.data.data) {
          this.rows = r.data.data
        } else {
          this.rows = []
        }
      } catch (e) {
        this.rows = []
        alert(this.$t('tenantAdminManage.loadFailed'))
      } finally {
        this.loading = false
      }
    },
    formatTime (v) {
      if (!v) return '—'
      return String(v).replace('T', ' ').slice(0, 19)
    },
    openEdit (u) {
      this.editTarget = u
      this.editForm = { phone: u.phone || '', newPassword: '' }
      this.showEditModal = true
    },
    async saveEdit () {
      if (!this.editTarget) return
      const phone = (this.editForm.phone || '').trim()
      const pwd = (this.editForm.newPassword || '').trim()
      if (!phone && !pwd) {
        alert(this.$t('tenantAdminManage.needOneField'))
        return
      }
      this.saving = true
      try {
        const body = {}
        if (phone) body.phone = phone
        if (pwd) body.newPassword = pwd
        const r = await this.$http.put('/admin/tenant-admins/' + this.editTarget.id, body)
        if (r.data && r.data.code === 0) {
          this.showEditModal = false
          await this.loadList()
          if (this.editTarget.id === this.currentUserId && phone) {
            try {
              const me = await this.$http.get('/admin/auth/me')
              if (me.data && me.data.code === 0 && me.data.data && me.data.data.phone) {
                window.localStorage.setItem('autoattend_username', me.data.data.phone)
              }
            } catch (e) { /* ignore */ }
          }
        } else {
          alert(r.data.message || this.$t('tenantAdminManage.saveFailed'))
        }
      } catch (e) {
        const msg = e.response && e.response.data && e.response.data.message
        alert(msg || this.$t('tenantAdminManage.saveFailed'))
      } finally {
        this.saving = false
      }
    }
  }
}
</script>

<style scoped>
.tenant-admin-manage { max-width: 960px; margin: 0 auto; padding: 24px; }
.page-header { margin-bottom: 20px; }
.page-title { margin: 0 0 8px; font-size: 22px; }
.page-desc { margin: 0; font-size: 14px; color: #6b7280; line-height: 1.5; }
.primary-button { padding: 8px 16px; border-radius: 6px; border: none; background: #2563eb; color: #fff; cursor: pointer; font-size: 14px; }
.secondary-button { padding: 8px 16px; border-radius: 6px; border: 1px solid #d1d5db; background: #fff; cursor: pointer; font-size: 14px; margin-left: 8px; }
.placeholder { padding: 24px; color: #6b7280; text-align: center; }
.table-wrapper { overflow-x: auto; }
.table { width: 100%; border-collapse: collapse; background: #fff; border: 1px solid #e5e7eb; border-radius: 8px; }
.table th, .table td { padding: 10px 12px; text-align: left; border-bottom: 1px solid #e5e7eb; }
.table th { background: #f9fafb; font-weight: 600; font-size: 13px; }
.table td { font-size: 14px; }
.mono { font-size: 13px; }
.link-button { background: none; border: none; color: #2563eb; cursor: pointer; font-size: 13px; padding: 0 4px; }
.link-button:hover { text-decoration: underline; }
.modal-mask { position: fixed; inset: 0; background: rgba(0,0,0,0.5); display: flex; align-items: center; justify-content: center; z-index: 1000; }
.modal-card { background: #fff; border-radius: 8px; padding: 24px; min-width: 360px; max-width: 90vw; }
.modal-card h3 { margin: 0 0 12px; font-size: 18px; }
.form-row { margin-bottom: 12px; }
.form-row label { display: block; font-size: 13px; margin-bottom: 4px; color: #374151; }
.form-row input { width: 100%; padding: 8px 10px; border: 1px solid #d1d5db; border-radius: 4px; font-size: 14px; box-sizing: border-box; }
.form-hint { font-size: 13px; color: #6b7280; margin-bottom: 12px; line-height: 1.45; }
.modal-actions { margin-top: 20px; }
</style>
