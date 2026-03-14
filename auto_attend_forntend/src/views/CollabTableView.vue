<template>
  <div class="collab-table-page">
    <div class="table-header">
      <div class="header-left">
        <router-link to="/collab/projects" class="back-link">{{ $t('collabTable.backToList') }}</router-link>
        <h2 class="table-title">{{ tableName }}</h2>
      </div>
      <button class="primary-button" @click="openAddRecord">{{ $t('collabTable.newRecord') }}</button>
    </div>

    <div v-if="tableLoading" class="placeholder">{{ $t('collabTable.loadingTable') }}</div>
    <div v-else-if="!columns.length" class="placeholder">{{ $t('collabTable.noColumns') }}</div>
    <div v-else class="table-wrapper">
      <table class="data-table">
        <thead>
          <tr>
            <th v-for="col in columns" :key="col.id" class="col-header">{{ col.name }}</th>
            <th width="80">{{ $t('collabTable.operations') }}</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="row in records" :key="row.id" @click="openRecord(row)">
            <td v-for="col in columns" :key="col.id" class="cell">
              {{ formatCell(row['c' + col.id], col) }}
            </td>
            <td>
              <button class="link-button" @click.stop="openRecord(row)">{{ $t('collabTable.detail') }}</button>
            </td>
          </tr>
        </tbody>
      </table>
      <div v-if="!records.length && !recordsLoading" class="empty-tip">{{ $t('collabTable.noRecords') }}</div>
      <div v-if="recordsLoading" class="loading-tip">{{ $t('collabTable.loadingRecords') }}</div>
    </div>

    <!-- 记录详情抽屉 -->
    <div v-if="drawerRecord" class="drawer-mask" @click="drawerRecord = null">
      <div class="drawer" @click.stop>
        <div class="drawer-header">
          <h3>{{ $t('collabTable.recordDetail') }}</h3>
          <button class="close-btn" @click="drawerRecord = null">×</button>
        </div>
        <div class="drawer-tabs">
          <button :class="{ active: drawerTab === 'fields' }" @click="drawerTab = 'fields'">{{ $t('collabTable.fields') }}</button>
          <button :class="{ active: drawerTab === 'comments' }" @click="drawerTab = 'comments'; loadComments()">{{ $t('collabTable.discussion') }}</button>
          <button :class="{ active: drawerTab === 'attachments' }" @click="drawerTab = 'attachments'; loadAttachments()">{{ $t('collabTable.attachments') }}</button>
        </div>
        <div class="drawer-body">
          <div v-if="drawerTab === 'fields'" class="field-list">
            <div v-for="col in columns" :key="col.id" class="field-row">
              <span class="field-label">{{ col.name }}</span>
              <span class="field-value">{{ formatCell(drawerRecord['c' + col.id], col) || '—' }}</span>
            </div>
          </div>
          <div v-if="drawerTab === 'comments'" class="comment-panel">
            <div class="comment-list">
              <div v-for="c in comments" :key="c.id" class="comment-item">
                <span class="comment-user">{{ c.userName || c.userEmail }}</span>
                <span class="comment-time">{{ formatTime(c.createdAt) }}</span>
                <p class="comment-content">{{ c.content }}</p>
              </div>
            </div>
            <div class="comment-form">
              <textarea v-model="newComment" :placeholder="$t('collabTable.inputComment')" rows="2"></textarea>
              <button class="primary-button small" @click="submitComment">{{ $t('collabTable.send') }}</button>
            </div>
          </div>
          <div v-if="drawerTab === 'attachments'" class="attachment-panel">
            <div class="upload-area">
              <input ref="fileInput" type="file" @change="onFileSelected" style="display:none">
              <button class="primary-button small" @click="$refs.fileInput.click()">{{ $t('collabTable.uploadAttachment') }}</button>
            </div>
            <ul class="attachment-list">
              <li v-for="a in attachments" :key="a.id">
                <button class="link-button" @click="downloadAttachment(a)">{{ a.fileName }}</button>
                <span class="file-size">({{ formatSize(a.fileSize) }})</span>
              </li>
            </ul>
          </div>
        </div>
      </div>
    </div>

    <!-- 新建记录弹窗（简化：只提交空记录） -->
    <div v-if="showAddModal" class="drawer-mask" @click="showAddModal = false">
      <div class="drawer add-modal" @click.stop>
        <div class="drawer-header">
          <h3>{{ $t('collabTable.newRecordTitle') }}</h3>
          <button class="close-btn" @click="showAddModal = false">×</button>
        </div>
        <div class="drawer-body">
          <p class="tip">{{ $t('collabTable.createThenEdit') }}</p>
          <button class="primary-button" @click="createRecord">{{ $t('collabTable.create') }}</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'CollabTableView',
  data () {
    return {
      projectId: null,
      tableName: '',
      columns: [],
      records: [],
      recordsLoading: false,
      tableLoading: true,
      drawerRecord: null,
      drawerTab: 'fields',
      comments: [],
      attachments: [],
      newComment: '',
      showAddModal: false
    }
  },
  created () {
    this.projectId = Number(this.$route.params.projectId)
    this.loadTable()
    this.loadRecords()
  },
  methods: {
    async loadTable () {
      this.tableLoading = true
      try {
        const resp = await this.$http.get(`/collab/projects/${this.projectId}/table`)
        if (resp.data && resp.data.code === 0) {
          const d = resp.data.data
          this.tableName = d.name || this.$t('collabTable.defaultTableName')
          this.columns = (d.columns || []).sort((a, b) => (a.sortOrder || 0) - (b.sortOrder || 0))
        }
      } catch (e) {
        if (e.response && e.response.status === 401) {
          this.$router.push({ name: 'collab-login' })
        }
      } finally {
        this.tableLoading = false
      }
    },
    async loadRecords () {
      this.recordsLoading = true
      try {
        const resp = await this.$http.get(`/collab/projects/${this.projectId}/records`, {
          params: { page: 1, pageSize: 100 }
        })
        if (resp.data && resp.data.code === 0) {
          this.records = resp.data.data.items || []
        }
      } finally {
        this.recordsLoading = false
      }
    },
    formatCell (val, col) {
      if (val == null) return ''
      if (col.columnType === 'datetime' || col.columnType === 'date') {
        return String(val).slice(0, 19).replace('T', ' ')
      }
      return String(val)
    },
    openRecord (row) {
      this.drawerRecord = row
      this.drawerTab = 'fields'
      this.comments = []
      this.attachments = []
      this.newComment = ''
    },
    async loadComments () {
      if (!this.drawerRecord) return
      try {
        const resp = await this.$http.get(`/collab/records/${this.drawerRecord.id}/comments`)
        if (resp.data && resp.data.code === 0) {
          this.comments = resp.data.data.items || []
        }
      } catch (e) { /* ignore */ }
    },
    async loadAttachments () {
      if (!this.drawerRecord) return
      try {
        const resp = await this.$http.get(`/collab/records/${this.drawerRecord.id}/attachments`)
        if (resp.data && resp.data.code === 0) {
          this.attachments = resp.data.data.items || []
        }
      } catch (e) { /* ignore */ }
    },
    async submitComment () {
      if (!this.drawerRecord || !this.newComment.trim()) return
      try {
        await this.$http.post(`/collab/records/${this.drawerRecord.id}/comments`, {
          content: this.newComment.trim()
        })
        this.newComment = ''
        this.loadComments()
      } catch (e) { /* ignore */ }
    },
    onFileSelected (e) {
      const file = e.target.files && e.target.files[0]
      if (!file || !this.drawerRecord) return
      const form = new FormData()
      form.append('file', file)
      this.$http.post(`/collab/records/${this.drawerRecord.id}/attachments`, form, {
        headers: { 'Content-Type': 'multipart/form-data' }
      }).then(() => {
        this.loadAttachments()
        e.target.value = ''
      }).catch(() => { /* ignore */ })
    },
    downloadAttachment (a) {
      const token = window.localStorage.getItem('autoattend_collab_token')
      const url = (this.$http.defaults.baseURL || '/api') + '/collab/attachments/' + a.id + '/download'
      fetch(url, { headers: { Authorization: 'Bearer ' + token } })
        .then(r => r.blob())
        .then(blob => {
          const link = document.createElement('a')
          link.href = URL.createObjectURL(blob)
          link.download = a.fileName || 'download'
          link.click()
          URL.revokeObjectURL(link.href)
        })
        .catch(() => { /* ignore */ })
    },
    formatSize (bytes) {
      if (bytes < 1024) return bytes + ' B'
      if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
      return (bytes / (1024 * 1024)).toFixed(1) + ' MB'
    },
    formatTime (t) {
      if (!t) return ''
      return String(t).slice(0, 19).replace('T', ' ')
    },
    openAddRecord () {
      this.showAddModal = true
    },
    async createRecord () {
      try {
        await this.$http.post(`/collab/projects/${this.projectId}/records`, { fields: {} })
        this.showAddModal = false
        this.loadRecords()
      } catch (e) { /* ignore */ }
    }
  }
}
</script>

<style scoped>
.collab-table-page {
  max-width: 1200px;
  margin: 0 auto;
}

.table-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.back-link {
  font-size: 13px;
  color: #2563eb;
}

.table-title {
  margin: 0;
  font-size: 18px;
}

.primary-button {
  padding: 8px 16px;
  border-radius: 6px;
  border: none;
  background: #2563eb;
  color: #fff;
  cursor: pointer;
  font-size: 14px;
}

.primary-button.small {
  padding: 4px 12px;
  font-size: 13px;
}

.table-wrapper {
  overflow-x: auto;
  background: #fff;
  border-radius: 8px;
  border: 1px solid #e5e7eb;
}

.data-table {
  width: 100%;
  border-collapse: collapse;
}

.data-table th, .data-table td {
  padding: 10px 12px;
  text-align: left;
  border-bottom: 1px solid #e5e7eb;
}

.data-table th {
  background: #f9fafb;
  font-weight: 600;
  font-size: 13px;
}

.data-table tbody tr {
  cursor: pointer;
}

.data-table tbody tr:hover {
  background: #f9fafb;
}

.cell {
  max-width: 200px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.placeholder, .empty-tip, .loading-tip {
  padding: 24px;
  text-align: center;
  color: #6b7280;
}

.drawer-mask {
  position: fixed;
  inset: 0;
  background: rgba(0,0,0,0.4);
  display: flex;
  align-items: center;
  justify-content: flex-end;
  z-index: 100;
}

.drawer {
  width: 420px;
  max-width: 100%;
  height: 100%;
  background: #fff;
  box-shadow: -4px 0 16px rgba(0,0,0,0.1);
  display: flex;
  flex-direction: column;
}

.drawer-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px;
  border-bottom: 1px solid #e5e7eb;
}

.drawer-header h3 {
  margin: 0;
  font-size: 16px;
}

.close-btn {
  border: none;
  background: none;
  font-size: 24px;
  cursor: pointer;
  color: #6b7280;
}

.drawer-tabs {
  display: flex;
  padding: 0 16px;
  gap: 8px;
  border-bottom: 1px solid #e5e7eb;
}

.drawer-tabs button {
  padding: 10px 12px;
  border: none;
  background: none;
  cursor: pointer;
  font-size: 13px;
  color: #6b7280;
}

.drawer-tabs button.active {
  color: #2563eb;
  border-bottom: 2px solid #2563eb;
  margin-bottom: -1px;
}

.drawer-body {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
}

.field-list .field-row {
  margin-bottom: 12px;
}

.field-label {
  display: block;
  font-size: 12px;
  color: #6b7280;
  margin-bottom: 2px;
}

.field-value {
  font-size: 14px;
}

.comment-item {
  margin-bottom: 12px;
  padding-bottom: 12px;
  border-bottom: 1px solid #f3f4f6;
}

.comment-user {
  font-weight: 500;
  font-size: 13px;
}

.comment-time {
  font-size: 11px;
  color: #9ca3af;
  margin-left: 8px;
}

.comment-content {
  margin: 8px 0 0;
  font-size: 14px;
  white-space: pre-wrap;
}

.comment-form {
  margin-top: 16px;
}

.comment-form textarea {
  width: 100%;
  padding: 8px;
  border: 1px solid #e5e7eb;
  border-radius: 4px;
  font-size: 14px;
  margin-bottom: 8px;
  box-sizing: border-box;
}

.upload-area {
  margin-bottom: 16px;
}

.attachment-list {
  list-style: none;
  padding: 0;
  margin: 0;
}

.attachment-list li {
  padding: 6px 0;
  font-size: 14px;
}

.attachment-list a {
  color: #2563eb;
}

.file-size {
  font-size: 12px;
  color: #9ca3af;
  margin-left: 4px;
}

.add-modal .drawer-body .tip {
  margin: 0 0 12px;
  font-size: 13px;
  color: #6b7280;
}

.link-button {
  border: none;
  background: transparent;
  color: #2563eb;
  cursor: pointer;
  font-size: 13px;
}
</style>
