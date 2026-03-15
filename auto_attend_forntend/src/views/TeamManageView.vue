<template>
  <div class="team-manage">
    <div class="page-header">
      <h1 class="page-title">{{ $t('teamManage.title') }}</h1>
      <button class="primary-button" @click="openCreate">{{ $t('teamManage.createMember') }}</button>
    </div>

    <div v-if="loading" class="placeholder">{{ $t('teamManage.loading') }}</div>
    <div v-else-if="!members.length" class="placeholder">{{ $t('teamManage.noMembers') }}</div>
    <div v-else class="table-wrapper">
      <table class="table">
        <thead>
          <tr>
            <th>{{ $t('teamManage.avatar') }}</th>
            <th>{{ $t('teamManage.email') }}</th>
            <th>{{ $t('teamManage.name') }}</th>
            <th>{{ $t('teamManage.remarkName') }}</th>
            <th>{{ $t('teamManage.jobTitle') }}</th>
            <th>{{ $t('teamManage.role') }}</th>
            <th>{{ $t('dashboard.actions') }}</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="u in members" :key="u.id">
            <td>
              <img v-if="u.avatar" :src="u.avatar" class="avatar-img" alt="">
              <span v-else class="avatar-placeholder">{{ (u.remarkName || u.name || u.email || '?').slice(0, 1) }}</span>
            </td>
            <td>{{ u.email }}</td>
            <td>{{ u.name }}</td>
            <td>{{ u.remarkName || '—' }}</td>
            <td>{{ u.jobTitle || '—' }}</td>
            <td>{{ roleLabel(u.role) }}</td>
            <td>
              <button class="link-button" @click="openEdit(u)">{{ $t('teamManage.editMember') }}</button>
              <button class="link-button" @click="openResetPassword(u)">{{ $t('teamManage.resetPassword') }}</button>
              <button class="link-button" @click="openProjectPermissions(u)">{{ $t('teamManage.projectPermissions') }}</button>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- 创建成员 -->
    <div v-if="showCreateModal" class="modal-mask" @click.self="showCreateModal = false">
      <div class="modal-card">
        <h3>{{ $t('teamManage.createMember') }}</h3>
        <div class="form-row"><label>{{ $t('teamManage.email') }}</label><input v-model="createForm.email" type="email" required></div>
        <div class="form-row"><label>{{ $t('teamManage.name') }}</label><input v-model="createForm.name" type="text"></div>
        <div class="form-row"><label>{{ $t('teamManage.password') }}</label><input v-model="createForm.password" type="password" :placeholder="$t('teamManage.defaultPasswordHint')"></div>
        <div class="form-row"><label>{{ $t('teamManage.remarkName') }}</label><input v-model="createForm.remarkName" type="text"></div>
        <div class="form-row"><label>{{ $t('teamManage.jobTitle') }}</label>
          <select v-model="createForm.jobTitle">
            <option v-for="t in jobTitles" :key="t" :value="t">{{ t }}</option>
          </select>
        </div>
        <div class="modal-actions">
          <button class="primary-button" @click="saveCreate" :disabled="saving">{{ $t('teamManage.save') }}</button>
          <button class="secondary-button" @click="showCreateModal = false">{{ $t('teamManage.cancel') }}</button>
        </div>
      </div>
    </div>

    <!-- 编辑成员 -->
    <div v-if="showEditModal" class="modal-mask" @click.self="showEditModal = false">
      <div class="modal-card">
        <h3>{{ $t('teamManage.editMember') }}</h3>
        <div class="form-row"><label>{{ $t('teamManage.email') }}</label><span class="form-readonly">{{ editTarget && editTarget.email }}</span></div>
        <div class="form-row"><label>{{ $t('teamManage.name') }}</label><input v-model="editForm.name" type="text"></div>
        <div class="form-row"><label>{{ $t('teamManage.remarkName') }}</label><input v-model="editForm.remarkName" type="text"></div>
        <div class="form-row"><label>{{ $t('teamManage.jobTitle') }}</label>
          <select v-model="editForm.jobTitle">
            <option v-for="t in jobTitles" :key="t" :value="t">{{ t }}</option>
          </select>
        </div>
        <div class="form-row"><label>{{ $t('teamManage.avatar') }}</label><input v-model="editForm.avatar" type="text" placeholder="https://..."></div>
        <div class="form-row"><label>{{ $t('teamManage.role') }}</label>
          <select v-model="editForm.role">
            <option value="member">{{ $t('teamManage.roleMember') }}</option>
            <option value="sub_admin">{{ $t('teamManage.roleSubAdmin') }}</option>
            <option value="super_admin">{{ $t('teamManage.roleSuperAdmin') }}</option>
          </select>
        </div>
        <div class="modal-actions">
          <button class="primary-button" @click="saveEdit" :disabled="saving">{{ $t('teamManage.save') }}</button>
          <button class="secondary-button" @click="showEditModal = false">{{ $t('teamManage.cancel') }}</button>
        </div>
      </div>
    </div>

    <!-- 重置密码 -->
    <div v-if="showPasswordModal" class="modal-mask" @click.self="showPasswordModal = false">
      <div class="modal-card">
        <h3>{{ $t('teamManage.resetPassword') }}</h3>
        <p class="form-hint">{{ passwordTarget ? passwordTarget.email : '' }}</p>
        <div class="form-row"><label>{{ $t('teamManage.newPassword') }}</label><input v-model="passwordForm.newPassword" type="password" required></div>
        <div class="modal-actions">
          <button class="primary-button" @click="saveResetPassword" :disabled="saving">{{ $t('teamManage.save') }}</button>
          <button class="secondary-button" @click="showPasswordModal = false">{{ $t('teamManage.cancel') }}</button>
        </div>
      </div>
    </div>

    <!-- 项目权限 -->
    <div v-if="showProjectsModal" class="modal-mask" @click.self="showProjectsModal = false">
      <div class="modal-card modal-card-wide">
        <h3>{{ $t('teamManage.projectPermissions') }}</h3>
        <p v-if="projectsTarget && projectsTarget.role === 'super_admin'" class="form-hint">{{ $t('teamManage.superAdminProjectsNote') }}</p>
        <div v-else class="project-check-list">
          <div v-for="p in allProjects" :key="p.id" class="project-check-item">
            <label>
              <input type="checkbox" :value="p.id" v-model="projectSelection">
              {{ p.name }} ({{ p.repoId }})
            </label>
            <select v-if="projectSelection.includes(p.id)" v-model="projectRoles[p.id]" class="role-select">
              <option value="member">{{ $t('teamManage.projectRoleMember') }}</option>
              <option value="admin">{{ $t('teamManage.projectRoleAdmin') }}</option>
            </select>
          </div>
        </div>
        <div class="modal-actions">
          <button class="primary-button" @click="saveProjectPermissions" :disabled="saving">{{ $t('teamManage.save') }}</button>
          <button class="secondary-button" @click="showProjectsModal = false">{{ $t('teamManage.cancel') }}</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'TeamManageView',
  data () {
    return {
      members: [],
      jobTitles: [],
      allProjects: [],
      loading: true,
      saving: false,
      showCreateModal: false,
      showEditModal: false,
      showPasswordModal: false,
      showProjectsModal: false,
      createForm: { email: '', name: '', password: '', remarkName: '', jobTitle: '开发工程师' },
      editTarget: null,
      editForm: { name: '', remarkName: '', jobTitle: '开发工程师', avatar: '', role: 'member' },
      passwordTarget: null,
      passwordForm: { newPassword: '' },
      projectsTarget: null,
      projectSelection: [],
      projectRoles: {}
    }
  },
  created () {
    this.loadJobTitles()
    this.loadMembers()
    this.loadProjects()
  },
  methods: {
    roleLabel (role) {
      if (!role) return '—'
      const map = { member: this.$t('teamManage.roleMember'), sub_admin: this.$t('teamManage.roleSubAdmin'), super_admin: this.$t('teamManage.roleSuperAdmin') }
      return map[role] || role
    },
    async loadJobTitles () {
      try {
        const r = await this.$http.get('/admin/team/job-titles')
        if (r.data && r.data.code === 0 && r.data.data) this.jobTitles = r.data.data
      } catch (e) { /* ignore */ }
    },
    async loadMembers () {
      this.loading = true
      try {
        const r = await this.$http.get('/admin/team/members')
        if (r.data && r.data.code === 0 && r.data.data) this.members = r.data.data
      } catch (e) {
        if (e.response && e.response.status === 401) this.$router.push({ name: 'login' })
      } finally {
        this.loading = false
      }
    },
    async loadProjects () {
      try {
        const r = await this.$http.get('/admin/team/projects')
        if (r.data && r.data.code === 0 && r.data.data) this.allProjects = r.data.data
      } catch (e) { /* ignore */ }
    },
    openCreate () {
      this.createForm = { email: '', name: '', password: '', remarkName: '', jobTitle: this.jobTitles[0] || '开发工程师' }
      this.showCreateModal = true
    },
    async saveCreate () {
      if (!this.createForm.email || !this.createForm.email.trim()) return
      this.saving = true
      try {
        const r = await this.$http.post('/admin/team/members', {
          email: this.createForm.email.trim(),
          name: this.createForm.name ? this.createForm.name.trim() : null,
          password: this.createForm.password || undefined,
          remarkName: this.createForm.remarkName ? this.createForm.remarkName.trim() : null,
          jobTitle: this.createForm.jobTitle || '开发工程师'
        })
        if (r.data && r.data.code === 0) {
          this.showCreateModal = false
          this.loadMembers()
        } else {
          alert(r.data.message || '创建失败')
        }
      } catch (e) {
        alert(e.response && e.response.data && e.response.data.message ? e.response.data.message : '创建失败')
      } finally {
        this.saving = false
      }
    },
    openEdit (u) {
      this.editTarget = u
      this.editForm = { name: u.name || '', remarkName: u.remarkName || '', jobTitle: u.jobTitle || '开发工程师', avatar: u.avatar || '', role: u.role || 'member' }
      this.showEditModal = true
    },
    async saveEdit () {
      if (!this.editTarget) return
      this.saving = true
      try {
        const r = await this.$http.put('/admin/team/members/' + this.editTarget.id, this.editForm)
        if (r.data && r.data.code === 0) {
          this.showEditModal = false
          this.loadMembers()
        } else {
          alert(r.data.message || '保存失败')
        }
      } catch (e) {
        alert(e.response && e.response.data && e.response.data.message ? e.response.data.message : '保存失败')
      } finally {
        this.saving = false
      }
    },
    openResetPassword (u) {
      this.passwordTarget = u
      this.passwordForm = { newPassword: '' }
      this.showPasswordModal = true
    },
    async saveResetPassword () {
      if (!this.passwordTarget || !this.passwordForm.newPassword || !this.passwordForm.newPassword.trim()) return
      this.saving = true
      try {
        const r = await this.$http.put('/admin/team/members/' + this.passwordTarget.id + '/password', { newPassword: this.passwordForm.newPassword })
        if (r.data && r.data.code === 0) {
          this.showPasswordModal = false
        } else {
          alert(r.data.message || '重置失败')
        }
      } catch (e) {
        alert(e.response && e.response.data && e.response.data.message ? e.response.data.message : '重置失败')
      } finally {
        this.saving = false
      }
    },
    async openProjectPermissions (u) {
      this.projectsTarget = u
      if (u.role === 'super_admin') {
        this.projectSelection = []
        this.projectRoles = {}
        this.showProjectsModal = true
        return
      }
      try {
        const r = await this.$http.get('/admin/team/members/' + u.id + '/projects')
        if (r.data && r.data.code === 0 && r.data.data) {
          const items = r.data.data.items || []
          this.projectSelection = items.map(i => i.projectId)
          this.projectRoles = {}
          items.forEach(i => { this.$set(this.projectRoles, i.projectId, i.role || 'member') })
        }
        this.showProjectsModal = true
      } catch (e) { /* ignore */ }
    },
    async saveProjectPermissions () {
      if (!this.projectsTarget) return
      if (this.projectsTarget.role === 'super_admin') {
        this.showProjectsModal = false
        return
      }
      this.saving = true
      try {
        const projects = this.projectSelection.map(pid => ({ projectId: pid, role: this.projectRoles[pid] || 'member' }))
        const r = await this.$http.put('/admin/team/members/' + this.projectsTarget.id + '/projects', { projects })
        if (r.data && r.data.code === 0) {
          this.showProjectsModal = false
        } else {
          alert(r.data.message || '保存失败')
        }
      } catch (e) {
        alert(e.response && e.response.data && e.response.data.message ? e.response.data.message : '保存失败')
      } finally {
        this.saving = false
      }
    }
  }
}
</script>

<style scoped>
.team-manage { max-width: 100%; }
.page-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 20px; }
.page-title { margin: 0; font-size: 22px; }
.primary-button { padding: 8px 16px; border-radius: 6px; border: none; background: #2563eb; color: #fff; cursor: pointer; font-size: 14px; }
.secondary-button { padding: 8px 16px; border-radius: 6px; border: 1px solid #d1d5db; background: #fff; cursor: pointer; font-size: 14px; margin-left: 8px; }
.placeholder { padding: 24px; color: #6b7280; text-align: center; }
.table-wrapper { overflow-x: auto; }
.table { width: 100%; border-collapse: collapse; background: #fff; border: 1px solid #e5e7eb; border-radius: 8px; }
.table th, .table td { padding: 10px 12px; text-align: left; border-bottom: 1px solid #e5e7eb; }
.table th { background: #f9fafb; font-weight: 600; font-size: 13px; }
.table td { font-size: 14px; }
.avatar-img { width: 32px; height: 32px; border-radius: 50%; object-fit: cover; }
.avatar-placeholder { width: 32px; height: 32px; border-radius: 50%; background: #e5e7eb; display: inline-flex; align-items: center; justify-content: center; font-size: 14px; }
.link-button { background: none; border: none; color: #2563eb; cursor: pointer; font-size: 13px; padding: 0 4px; margin-right: 8px; }
.link-button:hover { text-decoration: underline; }
.modal-mask { position: fixed; inset: 0; background: rgba(0,0,0,0.5); display: flex; align-items: center; justify-content: center; z-index: 1000; }
.modal-card { background: #fff; border-radius: 8px; padding: 24px; min-width: 360px; max-width: 90vw; }
.modal-card-wide { min-width: 480px; max-height: 80vh; overflow-y: auto; }
.modal-card h3 { margin: 0 0 16px; font-size: 18px; }
.form-row { margin-bottom: 12px; }
.form-row label { display: block; font-size: 13px; margin-bottom: 4px; color: #374151; }
.form-row input, .form-row select { width: 100%; padding: 8px 10px; border: 1px solid #d1d5db; border-radius: 4px; font-size: 14px; box-sizing: border-box; }
.form-readonly { font-size: 14px; color: #6b7280; }
.form-hint { font-size: 13px; color: #6b7280; margin-bottom: 12px; }
.modal-actions { margin-top: 20px; }
.project-check-list { max-height: 300px; overflow-y: auto; }
.project-check-item { display: flex; align-items: center; justify-content: space-between; padding: 8px 0; border-bottom: 1px solid #f3f4f6; }
.project-check-item label { display: flex; align-items: center; gap: 8px; cursor: pointer; }
.role-select { width: 120px; padding: 4px 8px; border-radius: 4px; border: 1px solid #d1d5db; font-size: 13px; }
</style>
