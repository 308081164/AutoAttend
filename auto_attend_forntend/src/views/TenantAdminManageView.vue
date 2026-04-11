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
.tenant-admin-manage {
  max-width: 960px;
  margin: 0 auto;
  padding: var(--space-xl);
  background: var(--bg-page);
  min-height: 100vh;
  box-sizing: border-box;
}
.page-header {
  margin-bottom: var(--space-xl);
}
.page-title {
  margin: 0 0 var(--space-sm);
  font-size: 22px;
  font-weight: 600;
  color: var(--text-primary);
}
.page-desc {
  margin: 0;
  font-size: 14px;
  color: var(--text-secondary);
  line-height: 1.6;
}
.primary-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: var(--space-sm) var(--space-lg);
  border-radius: var(--radius-md);
  border: none;
  background: var(--brand-blue);
  color: #fff;
  cursor: pointer;
  font-size: 14px;
  font-weight: 500;
  transition: opacity 0.2s;
  box-shadow: var(--shadow-sm);
}
.primary-button:hover {
  opacity: 0.85;
}
.primary-button:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
.secondary-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: var(--space-sm) var(--space-lg);
  border-radius: var(--radius-md);
  border: 1px solid var(--border-primary);
  background: var(--bg-card);
  cursor: pointer;
  font-size: 14px;
  font-weight: 500;
  color: var(--text-primary);
  margin-left: var(--space-sm);
  transition: border-color 0.2s;
}
.secondary-button:hover {
  border-color: var(--brand-blue);
  color: var(--brand-blue);
}
.placeholder {
  padding: var(--space-xxl);
  color: var(--text-tertiary);
  text-align: center;
  background: var(--bg-card);
  border: 1px solid var(--border-primary);
  border-radius: var(--radius-lg);
}
.table-wrapper {
  overflow-x: auto;
}
.table {
  width: 100%;
  border-collapse: separate;
  border-spacing: 0;
  background: var(--bg-card);
  border: 1px solid var(--border-primary);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-sm);
}
.table th,
.table td {
  padding: var(--space-sm) var(--space-md);
  text-align: left;
  border-bottom: 1px solid var(--border-primary);
}
.table th {
  background: var(--bg-page);
  font-weight: 600;
  font-size: 13px;
  color: var(--text-secondary);
}
.table td {
  font-size: 14px;
  color: var(--text-primary);
}
.table tr:last-child td {
  border-bottom: none;
}
.table tr:hover td {
  background: var(--bg-page);
}
.mono {
  font-family: 'SF Mono', 'Menlo', 'Monaco', 'Consolas', ui-monospace, monospace;
  font-size: 13px;
  color: var(--text-secondary);
}
.link-button {
  background: none;
  border: none;
  color: var(--brand-blue);
  cursor: pointer;
  font-size: 13px;
  padding: 0 4px;
  transition: opacity 0.2s;
}
.link-button:hover {
  opacity: 0.75;
  text-decoration: underline;
}
.modal-mask {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.45);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}
.modal-card {
  background: var(--bg-card);
  border-radius: var(--radius-xl);
  padding: var(--space-xl);
  min-width: 360px;
  max-width: 90vw;
  box-shadow: var(--shadow-lg);
}
.modal-card h3 {
  margin: 0 0 var(--space-md);
  font-size: 18px;
  font-weight: 600;
  color: var(--text-primary);
}
.form-row {
  margin-bottom: var(--space-md);
}
.form-row label {
  display: block;
  font-size: 13px;
  margin-bottom: 4px;
  color: var(--text-secondary);
  font-weight: 500;
}
.form-row input {
  width: 100%;
  padding: var(--space-sm) var(--space-md);
  border: 1px solid var(--border-primary);
  border-radius: var(--radius-sm);
  font-size: 14px;
  box-sizing: border-box;
  background: var(--bg-card);
  color: var(--text-primary);
  transition: border-color 0.2s;
}
.form-row input:focus {
  border-color: var(--brand-blue);
  outline: none;
}
.form-hint {
  font-size: 13px;
  color: var(--text-tertiary);
  margin-bottom: var(--space-md);
  line-height: 1.5;
}
.modal-actions {
  margin-top: var(--space-xl);
}
</style>
