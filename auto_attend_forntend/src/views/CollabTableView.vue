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
            <th v-for="col in columns" :key="col.id" class="col-header" :class="getColumnHeaderClass(col)">{{ getColumnDisplayName(col) }}</th>
            <th class="col-ops">{{ $t('collabTable.operations') }}</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="row in records" :key="row.id" @click="openRecord(row)">
            <td v-for="col in columns" :key="col.id" class="cell" :class="getCellClass(col)">
              <template v-if="isAttachmentColumn(col)">
                <div class="list-thumbs">
                  <template v-for="aid in getAttachmentIdsFromField(row['c' + col.id])">
                    <div v-if="getListAttachmentById(row.id, aid)" :key="aid" class="list-thumb-wrap">
                      <img v-if="getListAttachmentById(row.id, aid).isImage && listAttachmentPreviewUrls[aid]" :src="listAttachmentPreviewUrls[aid]" class="list-thumb-img" alt="">
                      <span v-else class="list-thumb-name">{{ getListAttachmentById(row.id, aid).fileName }}</span>
                    </div>
                  </template>
                </div>
              </template>
              <template v-else>
                {{ formatCell(row['c' + col.id], col) }}
              </template>
            </td>
            <td class="ops-cell">
              <button class="link-button" @click.stop="openRecord(row)">{{ $t('collabTable.detail') }}</button>
              <button class="link-button danger" @click.stop="confirmDeleteRow(row)" :title="$t('collabTable.deleteRecord')">{{ $t('collabTable.delete') }}</button>
            </td>
          </tr>
        </tbody>
      </table>
      <div v-if="!records.length && !recordsLoading" class="empty-tip">{{ $t('collabTable.noRecords') }}</div>
      <div v-if="recordsLoading" class="loading-tip">{{ $t('collabTable.loadingRecords') }}</div>
    </div>

    <!-- 记录详情抽屉 -->
    <div v-if="drawerRecord" class="drawer-mask" @click="closeDrawer">
      <div class="drawer" @click.stop>
        <div class="drawer-header">
          <h3>{{ $t('collabTable.recordDetail') }}</h3>
          <button class="close-btn" @click="closeDrawer">×</button>
        </div>
        <div class="drawer-tabs">
          <button :class="{ active: drawerTab === 'fields' }" @click="drawerTab = 'fields'">{{ $t('collabTable.fields') }}</button>
          <button :class="{ active: drawerTab === 'comments' }" @click="drawerTab = 'comments'; loadComments()">{{ $t('collabTable.discussion') }}</button>
          <button :class="{ active: drawerTab === 'attachments' }" @click="drawerTab = 'attachments'; loadAttachments()">{{ $t('collabTable.attachments') }}</button>
        </div>
        <div class="drawer-body">
          <div v-if="drawerTab === 'fields'" class="field-list">
            <div v-for="col in columns" :key="col.id" class="field-row">
              <span class="field-label">{{ getColumnDisplayName(col) }}</span>
              <div class="field-input-wrap">
                <textarea v-if="isProblemDescColumn(col)" :placeholder="$t('collabTable.fieldPlaceholder')"
                  :value="drawerEditValues['c' + col.id]"
                  @input="setDrawerField(col.id, $event.target.value)"
                  class="field-input field-textarea"
                  rows="3"
                ></textarea>
                <input v-else-if="isTextLike(col)" type="text" :placeholder="$t('collabTable.fieldPlaceholder')"
                  :value="drawerEditValues['c' + col.id]"
                  @input="setDrawerField(col.id, $event.target.value)"
                  class="field-input"
                >
                <select v-else-if="isSingleSelect(col)" :value="drawerEditValues['c' + col.id]"
                  @change="setDrawerField(col.id, $event.target.value)"
                  class="field-input"
                >
                  <option value="">{{ $t('collabTable.pleaseSelect') }}</option>
                  <option v-for="opt in getSelectOptions(col)" :key="opt" :value="opt">{{ opt }}</option>
                </select>
                <input v-else-if="col.columnType === 'number'" type="number" step="any"
                  :value="drawerEditValues['c' + col.id]"
                  @input="setDrawerField(col.id, $event.target.value === '' ? null : Number($event.target.value))"
                  class="field-input"
                >
                <input v-else-if="col.columnType === 'date'" type="date"
                  :value="formatDateForInput(drawerEditValues['c' + col.id])"
                  @input="setDrawerField(col.id, $event.target.value || null)"
                  class="field-input"
                >
                <input v-else-if="col.columnType === 'datetime'" type="datetime-local"
                  :value="formatDateTimeForInput(drawerEditValues['c' + col.id])"
                  @input="setDrawerField(col.id, $event.target.value || null)"
                  class="field-input"
                >
                <!-- 负责人等多选人 -->
                <div v-else-if="isMultiUserColumn(col)" class="multi-select-wrap">
                  <label v-for="m in projectMembers" :key="m.userId" class="multi-select-item">
                    <input type="checkbox" :value="m.userId"
                      :checked="isDrawerResponsibleSelected(col.id, m.userId)"
                      @change="toggleDrawerResponsible(col.id, m.userId, $event.target.checked)"
                    >
                    <span>{{ m.name || m.email || ('ID ' + m.userId) }}</span>
                  </label>
                  <span v-if="!projectMembers.length" class="text-muted">{{ $t('collabTable.noMembers') }}</span>
                </div>
                <!-- 图像展示：附件预览 + 上传 -->
                <div v-else-if="isAttachmentColumn(col)" class="attachment-field-preview">
                  <div class="attachment-thumb-list">
                    <template v-for="aid in getAttachmentIdsFromField(drawerEditValues['c' + col.id])">
                      <div v-if="getAttachmentById(aid)" :key="aid" class="thumb-wrap">
                        <img v-if="getAttachmentById(aid).isImage && attachmentPreviewUrls[aid]" :src="attachmentPreviewUrls[aid]" class="thumb-img" alt="">
                        <span v-else class="thumb-name">{{ getAttachmentById(aid).fileName }}</span>
                      </div>
                    </template>
                  </div>
                  <input ref="drawerFileInput" type="file" accept="image/*" style="display:none" @change="onDrawerFileSelected">
                  <button type="button" class="primary-button small" @click="triggerDrawerUpload(col.id)">{{ $t('collabTable.uploadImage') }}</button>
                </div>
                <input v-else type="text" :placeholder="$t('collabTable.fieldPlaceholder')"
                  :value="drawerEditValues['c' + col.id]"
                  @input="setDrawerField(col.id, $event.target.value)"
                  class="field-input"
                >
              </div>
            </div>
            <div class="drawer-actions">
              <button class="primary-button" @click="saveRecord" :disabled="drawerSaving">{{ $t('collabTable.save') }}</button>
              <button class="danger-button" @click="confirmDeleteRecord">{{ $t('collabTable.deleteRecord') }}</button>
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
              <li v-for="a in attachments" :key="a.id" class="attachment-item">
                <template v-if="a.isImage && attachmentPreviewUrls[a.id]">
                  <div class="attachment-preview-wrap">
                    <img :src="attachmentPreviewUrls[a.id]" class="attachment-preview-img" alt="" @click="downloadAttachment(a)">
                    <span class="attachment-meta">
                      <button class="link-button" @click="downloadAttachment(a)">{{ a.fileName }}</button>
                      <span class="file-size">({{ formatSize(a.fileSize) }})</span>
                    </span>
                  </div>
                </template>
                <template v-else>
                  <button class="link-button" @click="downloadAttachment(a)">{{ a.fileName }}</button>
                  <span class="file-size">({{ formatSize(a.fileSize) }})</span>
                </template>
              </li>
            </ul>
          </div>
        </div>
      </div>
    </div>

    <!-- 新建记录弹窗：可编辑各字段 -->
    <div v-if="showAddModal" class="drawer-mask" @click="showAddModal = false">
      <div class="drawer add-modal" @click.stop>
        <div class="drawer-header">
          <h3>{{ $t('collabTable.newRecordTitle') }}</h3>
          <button class="close-btn" @click="showAddModal = false">×</button>
        </div>
        <div class="drawer-body">
          <input ref="newRecordFileInput" type="file" accept="image/*" style="display:none" @change="onNewRecordFile">
          <div class="field-list">
            <div v-for="col in columns" :key="'new-' + col.id" class="field-row">
              <span class="field-label">{{ getColumnDisplayName(col) }}</span>
              <div class="field-input-wrap">
                <!-- 创建人：只读，默认当前登录用户 -->
                <input v-if="isCreatorColumn(col)" type="text" :value="newRecordFields['c' + col.id]"
                  readonly class="field-input field-readonly"
                >
                <!-- 负责人：多选，数据来自项目成员 -->
                <div v-else-if="isMultiUserColumn(col)" class="multi-select-wrap">
                  <label v-for="m in projectMembers" :key="m.userId" class="multi-select-item">
                    <input type="checkbox" :value="m.userId"
                      :checked="isResponsibleSelected(col.id, m.userId)"
                      @change="toggleResponsible(col.id, m.userId, $event.target.checked)"
                    >
                    <span>{{ m.name || m.email || ('ID ' + m.userId) }}</span>
                  </label>
                  <span v-if="!projectMembers.length" class="text-muted">{{ $t('collabTable.noMembers') }}</span>
                </div>
                <!-- 图像展示：上传附件 + 实时预览 -->
                <div v-else-if="isAttachmentColumn(col)" class="upload-field">
                  <div v-if="newRecordAttachmentColId === col.id && newRecordImageFile" class="new-record-preview">
                    <img :src="newRecordImagePreviewUrl" class="new-record-preview-img" alt="">
                    <span class="file-name-tag">{{ newRecordImageFileName }}</span>
                  </div>
                  <button type="button" class="primary-button small" @click="triggerNewRecordFile(col.id)">
                    {{ $t('collabTable.uploadImage') }}
                  </button>
                  <span v-if="newRecordAttachmentColId === col.id && newRecordImageFileName && !newRecordImageFile" class="file-name-tag">{{ newRecordImageFileName }}</span>
                </div>
                <input v-else-if="isTextLike(col)" type="text" :placeholder="$t('collabTable.fieldPlaceholder')"
                  :value="newRecordFields['c' + col.id]"
                  @input="setNewRecordField(col.id, $event.target.value)"
                  class="field-input"
                >
                <select v-else-if="isSingleSelect(col)" :value="newRecordFields['c' + col.id]"
                  @change="setNewRecordField(col.id, $event.target.value)"
                  class="field-input"
                >
                  <option value="">{{ $t('collabTable.pleaseSelect') }}</option>
                  <option v-for="opt in getSelectOptions(col)" :key="opt" :value="opt">{{ opt }}</option>
                </select>
                <input v-else-if="col.columnType === 'number'" type="number" step="any"
                  :value="newRecordFields['c' + col.id]"
                  @input="setNewRecordField(col.id, $event.target.value === '' ? null : Number($event.target.value))"
                  class="field-input"
                >
                <input v-else-if="col.columnType === 'date'" type="date"
                  :value="formatDateForInput(newRecordFields['c' + col.id])"
                  @input="setNewRecordField(col.id, $event.target.value || null)"
                  class="field-input"
                >
                <input v-else-if="col.columnType === 'datetime'" type="datetime-local"
                  :value="formatDateTimeForInput(newRecordFields['c' + col.id])"
                  @input="setNewRecordField(col.id, $event.target.value || null)"
                  class="field-input"
                >
                <input v-else type="text" :placeholder="$t('collabTable.fieldPlaceholder')"
                  :value="newRecordFields['c' + col.id]"
                  @input="setNewRecordField(col.id, $event.target.value)"
                  class="field-input"
                >
              </div>
            </div>
          </div>
          <button class="primary-button" @click="createRecord" :disabled="createSaving">{{ $t('collabTable.create') }}</button>
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
      drawerEditValues: {},
      drawerTab: 'fields',
      drawerSaving: false,
      comments: [],
      attachments: [],
      newComment: '',
      showAddModal: false,
      newRecordFields: {},
      createSaving: false,
      currentUser: null,
      projectMembers: [],
      newRecordImageFile: null,
      newRecordImageFileName: '',
      newRecordAttachmentColId: null,
      newRecordImagePreviewUrl: '',
      attachmentPreviewUrls: {},
      drawerUploadColId: null,
      recordAttachmentsMap: {},
      listAttachmentPreviewUrls: {}
    }
  },
  created () {
    this.projectId = Number(this.$route.params.projectId)
    this.loadTable()
    this.loadRecords()
    this.loadProjectMembers()
  },
  beforeDestroy () {
    this.revokeListAttachmentPreviewUrls()
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
        if (e.response && e.response.status === 401) this.$router.push({ name: 'login' })
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
          this.revokeListAttachmentPreviewUrls()
          this.loadListAttachmentPreviews()
        }
      } finally {
        this.recordsLoading = false
      }
    },
    getColumnHeaderClass (col) {
      if ((col.name || '').trim() === '问题描述') return 'col-problem'
      if (this.isAttachmentColumn(col)) return 'col-attachment'
      return ''
    },
    getCellClass (col) {
      if ((col.name || '').trim() === '问题描述') return 'cell-problem'
      if (this.isAttachmentColumn(col)) return 'cell-attachment'
      return ''
    },
    getListAttachmentById (recordId, attachmentId) {
      const list = this.recordAttachmentsMap[recordId]
      if (!list) return null
      const id = Number(attachmentId)
      return list.find(a => a.id === id || a.id === attachmentId)
    },
    revokeListAttachmentPreviewUrls () {
      Object.values(this.listAttachmentPreviewUrls).forEach(u => {
        try { URL.revokeObjectURL(u) } catch (_e) { /* ignore */ }
      })
      this.listAttachmentPreviewUrls = {}
      this.recordAttachmentsMap = {}
    },
    async loadListAttachmentPreviews () {
      const token = window.localStorage.getItem('autoattend_collab_token')
      const base = this.$http.defaults.baseURL || '/api'
      for (const row of this.records) {
        const ids = []
        this.columns.forEach(col => {
          if (!this.isAttachmentColumn(col)) return
          ids.push(...this.getAttachmentIdsFromField(row['c' + col.id]))
        })
        if (!ids.length) continue
        try {
          const resp = await this.$http.get(`/collab/records/${row.id}/attachments`)
          if (resp.data && resp.data.code === 0 && resp.data.data && resp.data.data.items) {
            this.$set(this.recordAttachmentsMap, row.id, resp.data.data.items)
            for (const a of resp.data.data.items) {
              if (!a.isImage) continue
              try {
                const r = await fetch(base + '/collab/attachments/' + a.id + '/preview', {
                  headers: { Authorization: 'Bearer ' + token }
                })
                if (r.ok) {
                  const blob = await r.blob()
                  if (blob.type && blob.type.startsWith('image/')) {
                    this.$set(this.listAttachmentPreviewUrls, a.id, URL.createObjectURL(blob))
                  }
                }
              } catch (e) { /* ignore */ }
            }
          }
        } catch (e) { /* ignore */ }
      }
    },
    formatCell (val, col) {
      if (val == null) return ''
      if ((col.columnType || '').toLowerCase() === 'attachment') return ''
      if (col.columnType === 'datetime' || col.columnType === 'date') {
        return String(val).slice(0, 19).replace('T', ' ')
      }
      if ((col.columnType || '').toLowerCase() === 'multi_user') {
        return this.formatMultiUserCell(val)
      }
      return String(val)
    },
    /** 将负责人等 multi_user 的 ID 数组转为名称展示 */
    formatMultiUserCell (val) {
      let ids = []
      try {
        ids = typeof val === 'string' ? JSON.parse(val) : (Array.isArray(val) ? val : [])
      } catch (e) {
        return String(val)
      }
      if (!ids.length) return ''
      const names = ids.map(id => {
        const m = this.projectMembers.find(mem => mem.userId === id || mem.userId === Number(id))
        return m ? (m.name || m.email || m.userName || '') || ('ID ' + id) : ('ID ' + id)
      }).filter(Boolean)
      return names.length ? names.join('、') : ''
    },
    loadProjectMembers () {
      this.$http.get(`/collab/projects/${this.projectId}/members`).then(resp => {
        if (resp.data && resp.data.code === 0 && resp.data.data && resp.data.data.items) {
          this.projectMembers = resp.data.data.items
        }
      }).catch(() => {})
    },
    async openRecord (row) {
      this.drawerRecord = row
      this.drawerEditValues = {}
      this.columns.forEach(col => {
        let v = row['c' + col.id]
        if (v != null && (col.columnType === 'datetime' || col.columnType === 'date')) {
          v = String(v).slice(0, 19).replace('T', ' ')
        }
        this.$set(this.drawerEditValues, 'c' + col.id, v != null && v !== '' ? v : null)
      })
      this.drawerTab = 'fields'
      this.comments = []
      this.attachments = []
      this.newComment = ''
      await this.loadAttachments()
      this.loadProjectMembersForDrawer()
    },
    closeDrawer () {
      this.revokeAttachmentPreviewUrls()
      this.drawerRecord = null
    },
    loadProjectMembersForDrawer () {
      if (this.projectMembers.length) return
      this.$http.get(`/collab/projects/${this.projectId}/members`).then(resp => {
        if (resp.data && resp.data.code === 0 && resp.data.data && resp.data.data.items) {
          this.projectMembers = resp.data.data.items
        }
      }).catch(() => {})
    },
    revokeAttachmentPreviewUrls () {
      Object.values(this.attachmentPreviewUrls).forEach(u => {
        try { URL.revokeObjectURL(u) } catch (_e) { /* revoke may throw in edge cases */ }
      })
      this.attachmentPreviewUrls = {}
    },
    isTextLike (col) {
      const t = (col.columnType || 'text').toLowerCase()
      return t === 'text' || (t === 'single_select' && !(col.optionGroup && col.optionGroup.options && col.optionGroup.options.length))
    },
    isSingleSelect (col) {
      return (col.columnType || '').toLowerCase() === 'single_select' && col.optionGroup && col.optionGroup.options && col.optionGroup.options.length
    },
    getSelectOptions (col) {
      if (!col.optionGroup || !col.optionGroup.options) return []
      return col.optionGroup.options
    },
    tryParseJson (v) {
      if (v == null) return null
      try { return typeof v === 'string' ? JSON.parse(v) : v } catch (e) { return null }
    },
    getColumnDisplayName (col) {
      if (col.name === '解决情况') return this.$t('collabTable.currentStatus')
      return col.name
    },
    isCreatorColumn (col) {
      return (col.name || '').trim() === '创建人'
    },
    isProblemDescColumn (col) {
      return (col.name || '').trim() === '问题描述'
    },
    isMultiUserColumn (col) {
      return (col.columnType || '').toLowerCase() === 'multi_user'
    },
    isAttachmentColumn (col) {
      return (col.columnType || '').toLowerCase() === 'attachment'
    },
    getAttachmentIdsFromField (val) {
      if (val == null) return []
      try {
        const arr = typeof val === 'string' ? JSON.parse(val) : val
        return Array.isArray(arr) ? arr : []
      } catch (e) {
        return []
      }
    },
    getAttachmentById (id) {
      const n = Number(id)
      return this.attachments.find(a => a.id === n || a.id === id)
    },
    isDrawerResponsibleSelected (colId, userId) {
      const raw = this.drawerEditValues['c' + colId]
      if (raw == null) return false
      let arr = []
      try {
        arr = typeof raw === 'string' ? JSON.parse(raw) : raw
      } catch (e) { return false }
      return Array.isArray(arr) && arr.indexOf(userId) !== -1
    },
    toggleDrawerResponsible (colId, userId, checked) {
      const raw = this.drawerEditValues['c' + colId]
      let arr = []
      try {
        arr = (raw != null && typeof raw === 'string') ? JSON.parse(raw) : (Array.isArray(raw) ? raw : [])
      } catch (e) { arr = [] }
      if (!Array.isArray(arr)) arr = []
      const idx = arr.indexOf(userId)
      if (checked && idx === -1) arr.push(userId)
      if (!checked && idx !== -1) arr.splice(idx, 1)
      this.$set(this.drawerEditValues, 'c' + colId, arr.length ? arr : null)
    },
    triggerDrawerUpload (colId) {
      this.drawerUploadColId = colId
      this.$nextTick(() => {
        const el = this.$refs.drawerFileInput
        if (el) (el.click && el.click()) || (el[0] && el[0].click())
      })
    },
    async onDrawerFileSelected (e) {
      const file = e.target.files && e.target.files[0]
      e.target.value = ''
      if (!file || !this.drawerRecord || this.drawerUploadColId == null) return
      const colId = this.drawerUploadColId
      try {
        const form = new FormData()
        form.append('file', file)
        const upResp = await this.$http.post(`/collab/records/${this.drawerRecord.id}/attachments`, form, {
          headers: { 'Content-Type': 'multipart/form-data' }
        })
        const attId = upResp.data && upResp.data.data && upResp.data.data.id
        if (attId != null) {
          const ids = this.getAttachmentIdsFromField(this.drawerEditValues['c' + colId])
          ids.push(attId)
          this.$set(this.drawerEditValues, 'c' + colId, ids)
          await this.loadAttachments()
          await this.saveRecord()
        }
      } catch (err) { /* ignore */ } finally {
        this.drawerUploadColId = null
      }
    },
    isResponsibleSelected (colId, userId) {
      const raw = this.newRecordFields['c' + colId]
      if (raw == null) return false
      let arr = []
      try {
        arr = typeof raw === 'string' ? JSON.parse(raw) : raw
      } catch (e) { return false }
      return Array.isArray(arr) && arr.indexOf(userId) !== -1
    },
    toggleResponsible (colId, userId, checked) {
      const raw = this.newRecordFields['c' + colId]
      let arr = []
      try {
        arr = (raw != null && typeof raw === 'string') ? JSON.parse(raw) : (Array.isArray(raw) ? raw : [])
      } catch (e) { arr = [] }
      if (!Array.isArray(arr)) arr = []
      const idx = arr.indexOf(userId)
      if (checked && idx === -1) arr.push(userId)
      if (!checked && idx !== -1) arr.splice(idx, 1)
      this.$set(this.newRecordFields, 'c' + colId, arr.length ? arr : null)
    },
    triggerNewRecordFile (colId) {
      this.newRecordAttachmentColId = colId
      this.$nextTick(() => {
        const el = this.$refs.newRecordFileInput
        if (el) (el.click && el.click()) || (el[0] && el[0].click())
      })
    },
    onNewRecordFile (e) {
      const file = e.target.files && e.target.files[0]
      if (!file) return
      if (this.newRecordImagePreviewUrl) {
        try { URL.revokeObjectURL(this.newRecordImagePreviewUrl) } catch (_err) { /* ignore */ }
      }
      this.newRecordImageFile = file
      this.newRecordImageFileName = file.name
      this.newRecordImagePreviewUrl = URL.createObjectURL(file)
      e.target.value = ''
    },
    getDefaultForSingleSelect (col) {
      const opts = this.getSelectOptions(col)
      if (!opts.length) return null
      const name = String((col.optionGroup && col.optionGroup.name) || col.name || '').trim()
      if (name === '当前状态' || name === '解决情况') return opts.indexOf('已创建') !== -1 ? '已创建' : opts[0]
      if (name === '验收结果') return opts.indexOf('未验收') !== -1 ? '未验收' : (opts.indexOf('待验收') !== -1 ? '待验收' : opts[0])
      return null
    },
    getDefaultCreateTime () {
      const now = new Date()
      const y = now.getFullYear()
      const m = String(now.getMonth() + 1).padStart(2, '0')
      const d = String(now.getDate()).padStart(2, '0')
      const h = String(now.getHours()).padStart(2, '0')
      const min = String(now.getMinutes()).padStart(2, '0')
      return `${y}-${m}-${d}T${h}:${min}`
    },
    setDrawerField (colId, value) {
      this.$set(this.drawerEditValues, 'c' + colId, value === '' ? null : value)
    },
    setNewRecordField (colId, value) {
      this.$set(this.newRecordFields, 'c' + colId, value === '' ? null : value)
    },
    formatDateForInput (val) {
      if (val == null || val === '') return ''
      const s = String(val).slice(0, 10)
      return s
    },
    formatDateTimeForInput (val) {
      if (val == null || val === '') return ''
      const s = String(val).slice(0, 19).replace(' ', 'T')
      return s
    },
    normalizeFieldValue (col, v) {
      if (v === undefined || v === null || v === '') return null
      if (col.columnType === 'date' && typeof v === 'string') return v.length === 10 ? v + 'T00:00:00' : v
      if (col.columnType === 'datetime' && typeof v === 'string' && v.length === 16) return v + ':00'
      return v
    },
    async saveRecord () {
      if (!this.drawerRecord || this.drawerSaving) return
      this.drawerSaving = true
      try {
        const fields = {}
        this.columns.forEach(col => {
          let v = this.drawerEditValues['c' + col.id]
          if (col.columnType === 'multi_user' && v != null) {
            v = Array.isArray(v) ? v : (typeof v === 'string' ? this.tryParseJson(v) : v)
          }
          v = this.normalizeFieldValue(col, v)
          if (v !== null) fields['c' + col.id] = v
        })
        await this.$http.put(`/collab/records/${this.drawerRecord.id}`, { fields })
        Object.assign(this.drawerRecord, this.drawerEditValues)
        this.loadRecords()
      } catch (e) { /* ignore */ } finally {
        this.drawerSaving = false
      }
    },
    confirmDeleteRecord () {
      if (!this.drawerRecord) return
      if (!window.confirm(this.$t('collabTable.confirmDelete'))) return
      this.doDeleteRecord(this.drawerRecord.id)
      this.drawerRecord = null
    },
    confirmDeleteRow (row) {
      if (!window.confirm(this.$t('collabTable.confirmDelete'))) return
      this.doDeleteRecord(row.id)
    },
    async doDeleteRecord (recordId) {
      try {
        await this.$http.delete(`/collab/records/${recordId}`)
        if (this.drawerRecord && this.drawerRecord.id === recordId) this.drawerRecord = null
        this.loadRecords()
      } catch (e) { /* ignore */ }
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
      this.revokeAttachmentPreviewUrls()
      try {
        const resp = await this.$http.get(`/collab/records/${this.drawerRecord.id}/attachments`)
        if (resp.data && resp.data.code === 0) {
          this.attachments = resp.data.data.items || []
          const token = window.localStorage.getItem('autoattend_collab_token')
          const base = this.$http.defaults.baseURL || '/api'
          for (const a of this.attachments) {
            if (!a.isImage) continue
            try {
              const r = await fetch(base + '/collab/attachments/' + a.id + '/preview', {
                headers: { Authorization: 'Bearer ' + token }
              })
              if (r.ok) {
                const blob = await r.blob()
                if (blob.type && blob.type.startsWith('image/')) {
                  this.$set(this.attachmentPreviewUrls, a.id, URL.createObjectURL(blob))
                }
              }
            } catch (e) { /* ignore */ }
          }
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
        .then(r => {
          if (!r.ok) return Promise.reject(new Error(r.statusText))
          return r.blob()
        })
        .then(blob => {
          const link = document.createElement('a')
          link.href = URL.createObjectURL(blob)
          link.download = a.fileName || 'download'
          link.click()
          setTimeout(() => URL.revokeObjectURL(link.href), 200)
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
    async openAddRecord () {
      if (this.newRecordImagePreviewUrl) {
        try { URL.revokeObjectURL(this.newRecordImagePreviewUrl) } catch (_err) { /* ignore */ }
      }
      this.newRecordFields = {}
      this.newRecordImageFile = null
      this.newRecordImageFileName = ''
      this.newRecordImagePreviewUrl = ''
      this.newRecordAttachmentColId = null
      this.showAddModal = true
      try {
        const meResp = await this.$http.get('/collab/auth/me')
        if (meResp.data && meResp.data.code === 0 && meResp.data.data) {
          this.currentUser = meResp.data.data
        }
      } catch (e) { this.currentUser = null }
      try {
        const memResp = await this.$http.get(`/collab/projects/${this.projectId}/members`)
        if (memResp.data && memResp.data.code === 0 && memResp.data.data && memResp.data.data.items) {
          this.projectMembers = memResp.data.data.items
        } else {
          this.projectMembers = []
        }
      } catch (e) { this.projectMembers = [] }
      this.$nextTick(() => {
        this.columns.forEach(col => {
          if (this.isCreatorColumn(col)) {
            const name = this.currentUser ? (this.currentUser.name || this.currentUser.email || '') : ''
            this.$set(this.newRecordFields, 'c' + col.id, name)
          } else if (col.columnType === 'datetime' && (col.name || '').trim() === '创建时间') {
            this.$set(this.newRecordFields, 'c' + col.id, this.getDefaultCreateTime())
          } else if (this.isSingleSelect(col)) {
            const def = this.getDefaultForSingleSelect(col)
            if (def != null) this.$set(this.newRecordFields, 'c' + col.id, def)
          }
        })
      })
    },
    async createRecord () {
      if (this.createSaving) return
      this.createSaving = true
      try {
        const fields = {}
        this.columns.forEach(col => {
          let v = this.newRecordFields['c' + col.id]
          if (col.columnType === 'multi_user' && v != null) {
            v = Array.isArray(v) ? v : (typeof v === 'string' ? (this.tryParseJson(v) || v) : v)
          }
          v = this.normalizeFieldValue(col, v)
          if (v !== null && v !== undefined) fields['c' + col.id] = v
        })
        const resp = await this.$http.post(`/collab/projects/${this.projectId}/records`, { fields })
        const recordId = resp.data && resp.data.data && resp.data.data.id
        if (recordId && this.newRecordImageFile && this.newRecordAttachmentColId) {
          const form = new FormData()
          form.append('file', this.newRecordImageFile)
          const upResp = await this.$http.post(`/collab/records/${recordId}/attachments`, form, {
            headers: { 'Content-Type': 'multipart/form-data' }
          })
          const attId = upResp.data && upResp.data.data && upResp.data.data.id
          if (attId != null) {
            await this.$http.put(`/collab/records/${recordId}`, {
              fields: { ['c' + this.newRecordAttachmentColId]: [attId] }
            })
          }
        }
        this.showAddModal = false
        this.newRecordFields = {}
        this.newRecordImageFile = null
        this.newRecordImageFileName = ''
        this.newRecordAttachmentColId = null
        this.loadRecords()
      } catch (e) { /* ignore */ } finally {
        this.createSaving = false
      }
    }
  }
}
</script>

<style scoped>
.collab-table-page {
  max-width: 98%;
  width: 100%;
  margin: 0 auto;
  padding: 0 12px;
  box-sizing: border-box;
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
  width: 100%;
}

.data-table {
  width: 100%;
  min-width: 900px;
  border-collapse: collapse;
}

.data-table th, .data-table td {
  padding: 10px 12px;
  text-align: left;
  border-bottom: 1px solid #e5e7eb;
  vertical-align: middle;
}

.data-table th {
  background: #f9fafb;
  font-weight: 600;
  font-size: 13px;
}

.data-table th.col-problem,
.data-table td.cell-problem { min-width: 200px; max-width: 380px; }
.data-table th.col-attachment,
.data-table td.cell-attachment { width: 120px; }
.data-table th.col-ops { width: 100px; }

.data-table tbody tr {
  cursor: pointer;
}

.data-table tbody tr:hover {
  background: #f9fafb;
}

.cell {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.cell-problem {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.cell-attachment {
  white-space: normal;
  width: 120px;
  vertical-align: middle;
}

.list-thumbs {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
  align-items: center;
}

.list-thumb-wrap {
  flex-shrink: 0;
}

.list-thumb-img {
  width: 40px;
  height: 40px;
  object-fit: cover;
  border-radius: 4px;
  display: block;
  border: 1px solid #e5e7eb;
}

.list-thumb-name {
  font-size: 11px;
  color: #6b7280;
  display: block;
  max-width: 56px;
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
  width: 540px;
  max-width: 96vw;
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
  padding: 20px 24px;
  max-width: 100%;
  box-sizing: border-box;
}

.field-list {
  width: 100%;
  max-width: 100%;
  box-sizing: border-box;
}

.field-list .field-row {
  margin-bottom: 14px;
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

.field-input-wrap {
  margin-top: 4px;
  max-width: 100%;
}

.field-input {
  width: 100%;
  max-width: 100%;
  padding: 8px 10px;
  border: 1px solid #e5e7eb;
  border-radius: 4px;
  font-size: 14px;
  box-sizing: border-box;
}

.field-input:focus {
  outline: none;
  border-color: #2563eb;
}

.field-input.field-textarea {
  min-height: 64px;
  resize: vertical;
  white-space: pre-wrap;
}

.drawer-actions {
  display: flex;
  gap: 10px;
  margin-top: 16px;
  padding-top: 12px;
  border-top: 1px solid #e5e7eb;
}

.danger-button {
  padding: 8px 16px;
  border-radius: 6px;
  border: none;
  background: #dc2626;
  color: #fff;
  cursor: pointer;
  font-size: 14px;
}

.danger-button:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.ops-cell {
  white-space: nowrap;
}

.ops-cell .link-button {
  margin-right: 8px;
}

.ops-cell .link-button.danger {
  color: #dc2626;
}

.field-readonly {
  background: #f3f4f6;
  color: #374151;
  cursor: default;
}

.multi-select-wrap {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 16px;
}

.multi-select-item {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
  cursor: pointer;
}

.multi-select-item input {
  margin: 0;
}

.text-muted {
  font-size: 13px;
  color: #9ca3af;
}

.upload-field {
  display: flex;
  align-items: center;
  gap: 8px;
}

.file-name-tag {
  font-size: 13px;
  color: #059669;
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

.attachment-item .attachment-preview-wrap {
  display: flex;
  align-items: center;
  gap: 10px;
}

.attachment-preview-img {
  width: 64px;
  height: 64px;
  object-fit: cover;
  border-radius: 6px;
  cursor: pointer;
  border: 1px solid #e5e7eb;
}

.attachment-meta {
  display: flex;
  align-items: center;
  gap: 4px;
  flex-wrap: wrap;
}

.attachment-list a,
.attachment-list .link-button {
  color: #2563eb;
}

.attachment-field-preview {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px;
}

.attachment-thumb-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.thumb-wrap {
  flex-shrink: 0;
}

.thumb-img {
  width: 56px;
  height: 56px;
  object-fit: cover;
  border-radius: 6px;
  display: block;
  border: 1px solid #e5e7eb;
}

.thumb-name {
  font-size: 12px;
  color: #6b7280;
  display: block;
  max-width: 80px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.new-record-preview {
  display: flex;
  align-items: center;
  gap: 8px;
}

.new-record-preview-img {
  width: 64px;
  height: 64px;
  object-fit: cover;
  border-radius: 6px;
  border: 1px solid #e5e7eb;
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
