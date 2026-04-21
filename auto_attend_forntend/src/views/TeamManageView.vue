<template>
  <div class="team-manage">
    <div class="page-header">
      <h1 class="page-title">{{ $t('teamManage.title') }}</h1>
      <button class="primary-button" @click="openCreate">{{ $t('teamManage.createMember') }}</button>
    </div>

    <div v-if="loading" class="placeholder">{{ $t('teamManage.loading') }}</div>
    <div v-else-if="!members.length" class="placeholder">{{ $t('teamManage.noMembers') }}</div>
    <!-- PC端表格 -->
    <div v-else class="table-wrapper table-wrapper--pc">
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
              <img v-if="u.avatar" :src="avatarDisplayUrl(u.avatar)" class="avatar-img" alt="">
              <span v-else class="avatar-placeholder">{{ (u.remarkName || u.name || u.email || '?').slice(0, 1) }}</span>
            </td>
            <td>{{ u.email }}</td>
            <td>{{ u.name }}</td>
            <td>{{ u.remarkName || '—' }}</td>
            <td>{{ u.jobTitle || '—' }}</td>
            <td>{{ roleLabel(u.role) }}</td>
            <td>
              <button class="link-button" @click="openEdit(u)">{{ $t('teamManage.editMember') }}</button>
              <button class="link-button" @click="openProjectPermissions(u)">{{ $t('teamManage.projectPermissions') }}</button>
            </td>
          </tr>
        </tbody>
      </table>
    </div>
    <!-- 移动端卡片列表 -->
    <div v-else class="member-list member-list--mobile">
      <div v-for="u in members" :key="u.id" class="member-card">
        <div class="member-card__main">
          <div class="member-card__avatar">
            <img v-if="u.avatar" :src="avatarDisplayUrl(u.avatar)" class="avatar-img" alt="">
            <span v-else class="avatar-placeholder">{{ (u.remarkName || u.name || u.email || '?').slice(0, 1) }}</span>
          </div>
          <div class="member-card__info">
            <div class="member-card__name">{{ u.remarkName || u.name || u.email }}</div>
            <div class="member-card__email">{{ u.email }}</div>
            <div class="member-card__meta">
              <span v-if="u.jobTitle" class="member-card__job">{{ u.jobTitle }}</span>
              <span class="member-card__role" :class="'member-card__role--' + u.role">{{ roleLabel(u.role) }}</span>
            </div>
          </div>
        </div>
        <div class="member-card__actions">
          <button class="member-card__btn" @click="openEdit(u)">编辑</button>
          <button class="member-card__btn" @click="openProjectPermissions(u)">权限</button>
        </div>
      </div>
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
        <div class="form-row"><label>{{ $t('teamManage.name') }}</label><span class="form-readonly">{{ editForm.name || '—' }}</span><p class="form-hint">{{ $t('teamManage.nameAdminReadOnlyHint') }}</p></div>
        <div class="form-row"><label>{{ $t('teamManage.remarkName') }}</label><input v-model="editForm.remarkName" type="text"></div>
        <div class="form-row"><label>{{ $t('teamManage.jobTitle') }}</label>
          <select v-model="editForm.jobTitle">
            <option v-for="t in jobTitles" :key="t" :value="t">{{ t }}</option>
          </select>
        </div>
        <div class="form-row">
          <label>{{ $t('teamManage.avatar') }}</label>
          <div class="avatar-edit avatar-edit--readonly">
            <img v-if="editForm.avatar" :src="avatarDisplayUrl(editForm.avatar)" class="avatar-preview" alt="">
            <span v-else class="avatar-placeholder avatar-preview">{{ (editForm.remarkName || editForm.name || '?').slice(0, 1) }}</span>
            <p class="form-hint">{{ $t('teamManage.avatarAdminReadOnlyHint') }}</p>
          </div>
        </div>
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
      showProjectsModal: false,
      createForm: { email: '', name: '', password: '', remarkName: '', jobTitle: '开发工程师' },
      editTarget: null,
      editForm: { name: '', remarkName: '', jobTitle: '开发工程师', avatar: '', role: 'member' },
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
    /** 头像展示 URL：MinIO key（avatars/xxx）走后端代理，否则视为外链 */
    avatarDisplayUrl (avatar) {
      if (!avatar || !avatar.trim()) return ''
      const s = avatar.trim()
      if (s.startsWith('http://') || s.startsWith('https://')) return s
      const base = this.$http.defaults.baseURL || '/api'
      return base + '/admin/team/avatar?key=' + encodeURIComponent(s)
    },
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
          const data = r.data.data || {}
          if (data.blocked === true) {
            alert(data.message || '当前套餐上限已达，暂不可新增成员，请及时升级/续费。')
            return
          }
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
        const body = {
          remarkName: this.editForm.remarkName != null ? String(this.editForm.remarkName).trim() : '',
          jobTitle: this.editForm.jobTitle || '开发工程师',
          role: this.editForm.role || 'member'
        }
        const r = await this.$http.put('/admin/team/members/' + this.editTarget.id, body)
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
.team-manage {
  max-width: 100%;
}
.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: var(--space-xl);
}
.page-title {
  margin: 0;
  font-size: var(--font-size-xxl);
  font-weight: var(--font-weight-bold);
  color: var(--text-primary);
}
.primary-button {
  padding: var(--space-sm) var(--space-lg);
  border-radius: var(--radius-sm);
  border: none;
  background: var(--brand-blue);
  color: #fff;
  cursor: pointer;
  font-size: var(--font-size-base);
  font-weight: var(--font-weight-medium);
  transition: var(--transition-fast);
  box-shadow: var(--shadow-btn);
}
.primary-button:hover {
  background: var(--brand-blue-hover);
}
.primary-button:disabled {
  opacity: 0.45;
  cursor: not-allowed;
}
.secondary-button {
  padding: var(--space-sm) var(--space-lg);
  border-radius: var(--radius-sm);
  border: 1px solid var(--border-primary);
  background: var(--bg-card);
  cursor: pointer;
  font-size: var(--font-size-base);
  margin-left: var(--space-sm);
  color: var(--text-primary);
  font-weight: var(--font-weight-medium);
  transition: var(--transition-fast);
}
.secondary-button:hover {
  background: var(--bg-hover);
  border-color: var(--border-input-hover);
}
.secondary-button:disabled {
  opacity: 0.45;
  cursor: not-allowed;
}
.placeholder {
  padding: var(--space-xxl);
  color: var(--text-secondary);
  text-align: center;
  font-weight: var(--font-weight-medium);
}
.table-wrapper {
  overflow-x: auto;
}
.table {
  width: 100%;
  border-collapse: collapse;
  background: var(--bg-card);
  border: 1px solid var(--border-primary);
  border-radius: var(--radius-md);
  overflow: hidden;
}
.table th,
.table td {
  padding: var(--space-sm) var(--space-md);
  text-align: left;
  border-bottom: 1px solid var(--border-primary);
  color: var(--text-primary);
}
.table th {
  background: var(--bg-hover);
  font-weight: var(--font-weight-semibold);
  font-size: var(--font-size-sm);
  color: var(--text-secondary);
}
.table td {
  font-size: var(--font-size-base);
}
.table tbody tr:hover {
  background: var(--bg-hover);
}
.table tbody tr:nth-child(even) {
  background: #FAFBFC;
}
.table tbody tr:nth-child(even):hover {
  background: var(--bg-hover);
}
.avatar-img {
  width: 32px;
  height: 32px;
  border-radius: var(--radius-full);
  object-fit: cover;
  border: 1px solid var(--border-primary);
}
.avatar-placeholder {
  width: 32px;
  height: 32px;
  border-radius: var(--radius-full);
  background: var(--bg-hover);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: var(--font-size-base);
  color: var(--text-secondary);
  font-weight: var(--font-weight-medium);
}
.avatar-edit {
  display: flex;
  align-items: center;
  gap: var(--space-md);
  flex-wrap: wrap;
}
.avatar-preview {
  width: 48px;
  height: 48px;
  border-radius: var(--radius-full);
  object-fit: cover;
  border: 1px solid var(--border-primary);
}
.avatar-edit .avatar-placeholder.avatar-preview {
  width: 48px;
  height: 48px;
  font-size: var(--font-size-xl);
}
.avatar-upload-actions {
  display: flex;
  align-items: center;
  gap: var(--space-sm);
  flex-wrap: wrap;
}
.avatar-file-input {
  position: absolute;
  width: 0;
  height: 0;
  opacity: 0;
  overflow: hidden;
}
.link-button {
  background: none;
  border: none;
  color: var(--text-link);
  cursor: pointer;
  font-size: var(--font-size-sm);
  padding: 0 var(--space-xs);
  margin-right: var(--space-sm);
  transition: var(--transition-fast);
}
.link-button:hover {
  text-decoration: underline;
  color: var(--text-link-hover);
}
.modal-mask {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.4);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: var(--z-modal);
}
.modal-card {
  background: var(--bg-card);
  border-radius: var(--radius-md);
  padding: var(--space-xl);
  min-width: 360px;
  max-width: 90vw;
  box-shadow: var(--shadow-lg);
}
.modal-card-wide {
  min-width: 480px;
  max-height: 80vh;
  overflow-y: auto;
}
.modal-card h3 {
  margin: 0 0 var(--space-lg);
  font-size: var(--font-size-xl);
  color: var(--text-primary);
  font-weight: var(--font-weight-semibold);
}
.form-row {
  margin-bottom: var(--space-md);
}
.form-row label {
  display: block;
  font-size: var(--font-size-sm);
  margin-bottom: var(--space-xs);
  color: var(--text-secondary);
  font-weight: var(--font-weight-medium);
}
.form-row input,
.form-row select {
  width: 100%;
  padding: var(--space-sm) var(--space-md);
  border: 1px solid var(--border-input);
  border-radius: var(--radius-sm);
  font-size: var(--font-size-base);
  box-sizing: border-box;
  color: var(--text-primary);
  background: var(--bg-input);
  transition: var(--transition-fast);
}
.form-row input:hover,
.form-row select:hover {
  border-color: var(--border-input-hover);
}
.form-row input:focus,
.form-row select:focus {
  outline: none;
  border-color: var(--border-input-focus);
  box-shadow: 0 0 0 2px rgba(20, 86, 240, 0.15);
}
.form-readonly {
  font-size: var(--font-size-base);
  color: var(--text-secondary);
}
.form-hint {
  font-size: var(--font-size-sm);
  color: var(--text-secondary);
  margin-bottom: var(--space-md);
}
.modal-actions {
  margin-top: var(--space-xl);
  display: flex;
  gap: var(--space-sm);
}
.project-check-list {
  max-height: 300px;
  overflow-y: auto;
}
.project-check-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: var(--space-sm) 0;
  border-bottom: 1px solid var(--bg-hover);
}
.project-check-item label {
  display: flex;
  align-items: center;
  gap: var(--space-sm);
  cursor: pointer;
  font-size: var(--font-size-base);
  color: var(--text-primary);
}
.role-select {
  width: 120px;
  padding: var(--space-xs) var(--space-sm);
  border-radius: var(--radius-sm);
  border: 1px solid var(--border-input);
  font-size: var(--font-size-sm);
  color: var(--text-primary);
  background: var(--bg-input);
  transition: var(--transition-fast);
}
.role-select:hover {
  border-color: var(--border-input-hover);
}
.role-select:focus {
  outline: none;
  border-color: var(--border-input-focus);
  box-shadow: 0 0 0 2px rgba(20, 86, 240, 0.15);
}

/* ========== 移动端卡片列表 ========== */
.member-list--mobile {
  display: none;
}

.member-card {
  background: var(--bg-card);
  border: 1px solid var(--border-primary);
  border-radius: var(--radius-md);
  padding: var(--space-md);
  margin-bottom: var(--space-md);
}

.member-card__main {
  display: flex;
  align-items: flex-start;
  gap: var(--space-md);
}

.member-card__avatar .avatar-img,
.member-card__avatar .avatar-placeholder {
  width: 48px;
  height: 48px;
  border-radius: 50%;
}

.member-card__avatar .avatar-placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--brand-blue);
  color: #fff;
  font-size: var(--font-size-lg);
  font-weight: var(--font-weight-semibold);
}

.member-card__info {
  flex: 1;
  min-width: 0;
}

.member-card__name {
  font-size: var(--font-size-md);
  font-weight: var(--font-weight-semibold);
  color: var(--text-primary);
  margin-bottom: 2px;
}

.member-card__email {
  font-size: var(--font-size-sm);
  color: var(--text-secondary);
  margin-bottom: var(--space-xs);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.member-card__meta {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-xs);
}

.member-card__job {
  font-size: var(--font-size-xs);
  color: var(--text-secondary);
  background: var(--bg-hover);
  padding: 2px 6px;
  border-radius: 4px;
}

.member-card__role {
  font-size: var(--font-size-xs);
  padding: 2px 6px;
  border-radius: 4px;
  font-weight: var(--font-weight-medium);
}

.member-card__role--super_admin {
  background: rgba(234, 88, 12, 0.1);
  color: #ea580c;
}

.member-card__role--sub_admin {
  background: rgba(20, 86, 240, 0.1);
  color: var(--brand-blue);
}

.member-card__role--member {
  background: var(--bg-hover);
  color: var(--text-secondary);
}

.member-card__actions {
  display: flex;
  gap: var(--space-sm);
  margin-top: var(--space-md);
  padding-top: var(--space-md);
  border-top: 1px solid var(--border-primary);
}

.member-card__btn {
  flex: 1;
  padding: var(--space-sm) var(--space-md);
  border-radius: var(--radius-sm);
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-medium);
  text-align: center;
  cursor: pointer;
  background: var(--bg-card);
  color: var(--text-primary);
  border: 1px solid var(--border-primary);
  transition: var(--transition-fast);
}

.member-card__btn:hover {
  background: var(--bg-hover);
  border-color: var(--border-input-hover);
}

/* ========== 响应式：移动端 ========== */
@media (max-width: 768px) {
  .team-manage {
    padding: var(--space-md);
    padding-bottom: calc(var(--space-xl) + env(safe-area-inset-bottom, 0px));
  }

  .page-header {
    flex-direction: column;
    align-items: stretch;
    gap: var(--space-md);
  }

  .page-title {
    font-size: var(--font-size-xl);
  }

  .primary-button {
    width: 100%;
    font-size: var(--font-size-sm);
    padding: var(--space-sm) var(--space-md);
  }

  .table-wrapper--pc {
    display: none;
  }

  .member-list--mobile {
    display: block;
  }

  /* 弹窗适配 */
  .modal-card {
    width: 95%;
    max-width: 400px;
    max-height: 90vh;
    overflow-y: auto;
    margin: var(--space-md);
  }

  .modal-card h3 {
    font-size: var(--font-size-lg);
  }

  .form-row {
    flex-direction: column;
    gap: var(--space-xs);
  }

  .form-row label {
    font-size: var(--font-size-sm);
  }

  .form-row input,
  .form-row select {
    font-size: var(--font-size-md);
  }

  .modal-actions {
    flex-direction: column;
  }

  .modal-actions button {
    width: 100%;
  }
}
</style>
