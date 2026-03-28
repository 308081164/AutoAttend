<template>
  <div class="collab-table-page">
    <div class="table-header">
      <div class="header-left">
        <router-link to="/collab/projects" class="back-link">{{ $t('collabTable.backToList') }}</router-link>
        <div class="title-block">
          <div class="title-row">
            <h2 class="table-title">{{ pageTableTitle }}</h2>
            <button type="button" class="secondary-button filter-title-button" @click="openFilterModal" :title="$t('collabTable.filterHint')">
              {{ $t('collabTable.filter') }}<span v-if="activeFilters && activeFilters.length">({{ activeFilters.length }})</span>
            </button>
          </div>
          <div v-if="activeFilters && activeFilters.length" class="active-filter-chips">
            <span v-for="(f, idx) in activeFilters" :key="'af-' + idx" class="filter-chip">
              <span class="filter-chip-text">{{ formatActiveFilterLabel(f) }}</span>
              <button
                type="button"
                class="filter-chip-remove"
                @click.stop="removeActiveFilterAt(idx)"
                :aria-label="$t('collabTable.filterRemoveRule')"
                :title="$t('collabTable.filterRemoveRule')"
              >×</button>
            </span>
          </div>
        </div>
      </div>
      <div class="header-actions">
        <button
          type="button"
          class="secondary-button"
          @click="toggleDashboardView"
          :title="showDashboard ? $t('collabTable.backToTableView') : $t('collabTable.dashboardHint')"
        >
          {{ showDashboard ? $t('collabTable.backToTableView') : $t('collabTable.dashboardSwitch') }}
        </button>
        <button type="button" class="ai-magic-button" @click="openAiInput('text')" :title="$t('collabTable.aiInputModeHint')">
          <span class="ai-magic-icon" aria-hidden="true">✦</span>
          <span class="ai-magic-label">{{ $t('collabTable.aiInputMode') }}</span>
        </button>
        <button type="button" class="ai-csv-button" @click="openAiInput('csv')" :title="$t('collabTable.csvAiHint')">
          <span class="ai-magic-icon" aria-hidden="true">📄</span>
          <span class="ai-magic-label">{{ $t('collabTable.csvAiMode') }}</span>
        </button>
        <button type="button" class="primary-button" @click="openAddRecord">{{ $t('collabTable.newRecord') }}</button>
      </div>
    </div>

    <div v-if="tableLoading" class="placeholder">{{ $t('collabTable.loadingTable') }}</div>
    <div v-else-if="showDashboard" class="dashboard-wrapper">
      <div v-if="recordsLoading" class="loading-tip">{{ $t('collabTable.loadingRecords') }}</div>
      <div v-else-if="!records.length" class="empty-tip">{{ $t('collabTable.dashboardNoData') }}</div>
      <div v-else class="dashboard-grid">
        <div class="dashboard-card">
          <div class="dashboard-card-title">{{ $t('collabTable.dashboardTaskCompletion') }}</div>
          <canvas ref="taskCompletionChart"></canvas>
        </div>
        <div class="dashboard-card">
          <div class="dashboard-card-title">{{ $t('collabTable.dashboardWeeklyCreated') }}</div>
          <canvas ref="weeklyCreatedChart"></canvas>
        </div>
        <div class="dashboard-card">
          <div class="dashboard-card-title">{{ $t('collabTable.dashboardWeeklyResolved') }}</div>
          <canvas ref="weeklyResolvedChart"></canvas>
        </div>
        <div class="dashboard-card">
          <div class="dashboard-card-title">{{ $t('collabTable.dashboardImportantDistribution') }}</div>
          <canvas ref="importanceChart"></canvas>
        </div>
        <div class="dashboard-card dashboard-card-placeholder">
          <div class="dashboard-card-title">{{ $t('collabTable.dashboardWordCloud') }}</div>
          <div class="dashboard-placeholder">{{ $t('collabTable.dashboardComingSoon') }}</div>
        </div>
        <div class="dashboard-card dashboard-card-placeholder">
          <div class="dashboard-card-title">{{ $t('collabTable.dashboardAvgResolveDuration') }}</div>
          <div class="dashboard-placeholder">{{ $t('collabTable.dashboardComingSoon') }}</div>
        </div>
      </div>
    </div>
    <div v-else-if="!columns.length" class="placeholder">{{ $t('collabTable.noColumns') }}</div>
    <div v-else class="table-wrapper">
      <div v-if="selectedCount > 0" class="batch-toolbar">
        <span class="batch-toolbar-text">{{ $t('collabTable.batchSelected', { n: selectedCount }) }}</span>
        <button type="button" class="secondary-button small" :disabled="batchDeleting" @click="clearRowSelection">
          {{ $t('collabTable.batchClearSelection') }}
        </button>
        <button type="button" class="danger-button small" :disabled="batchDeleting" @click="confirmBatchDelete">
          {{ batchDeleting ? $t('collabTable.batchDeleting') : $t('collabTable.batchDelete') }}
        </button>
      </div>
      <table class="data-table">
        <thead>
          <tr>
            <th class="col-check" @click.stop>
              <input
                ref="selectAllCheckbox"
                type="checkbox"
                class="row-check-input"
                :disabled="!records.length || recordsLoading"
                :checked="allRowsSelected"
                @change="onToggleSelectAll($event)"
              >
            </th>
            <th class="col-serial">{{ $t('collabTable.serialNo') }}</th>
            <th v-for="col in columns" :key="col.id" class="col-header" :class="getColumnHeaderClass(col)">{{ getColumnDisplayName(col) }}</th>
            <th class="col-ops">{{ $t('collabTable.operations') }}</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="(row, idx) in records" :key="row.id" @click="openRecord(row)">
            <td class="cell col-check" @click.stop>
              <input
                type="checkbox"
                class="row-check-input"
                :checked="!!rowSelection[row.id]"
                @change="toggleRowSelection(row, $event.target.checked)"
              >
            </td>
            <td class="cell col-serial">{{ idx + 1 }}</td>
            <td v-for="col in columns" :key="col.id" class="cell" :class="getCellClass(col)">
              <template v-if="isAttachmentColumn(col)">
                <div class="list-thumbs">
                  <template v-for="aid in getAttachmentIdsFromField(row['c' + col.id])">
                    <div v-if="getListAttachmentById(row.id, aid)" :key="aid" class="list-thumb-wrap">
                      <img
                        v-if="getListAttachmentById(row.id, aid).isImage && listAttachmentPreviewUrls[aid]"
                        :src="listAttachmentPreviewUrls[aid]"
                        class="list-thumb-img list-thumb-img-clickable"
                        alt=""
                        @click.stop="openImagePreview(getListAttachmentById(row.id, aid), listAttachmentPreviewUrls[aid])"
                      >
                      <span v-else class="list-thumb-name">{{ getListAttachmentById(row.id, aid).fileName }}</span>
                    </div>
                  </template>
                </div>
              </template>
              <template v-else>
                <div
                  v-if="isStatusTagColumn(col) && isSingleSelect(col)"
                  class="status-inline-editor"
                  @click.stop
                >
                  <button
                    type="button"
                    class="status-select status-picker-trigger"
                    :class="getStatusTagClass(col, row['c' + col.id])"
                    :disabled="isStatusUpdating(row.id, col.id)"
                    @click.stop="toggleStatusPicker(row.id, col.id, $event)"
                  >
                    <span class="status-picker-text">{{ row['c' + col.id] || $t('collabTable.pleaseSelect') }}</span>
                  </button>
                  <div
                    v-if="isStatusPickerOpen(row.id, col.id)"
                    class="status-picker-menu"
                    :style="statusPickerMenuStyle"
                    @click.stop
                  >
                    <button
                      type="button"
                      class="status-picker-item"
                      :class="{ active: !row['c' + col.id] }"
                      @click.stop="onInlineStatusChange(row, col, ''); closeStatusPicker()"
                    >
                      {{ $t('collabTable.pleaseSelect') }}
                    </button>
                    <button
                      v-for="opt in getStatusOptions(col)"
                      :key="'status-opt-' + row.id + '-' + col.id + '-' + opt"
                      type="button"
                      class="status-picker-item"
                      :class="[getStatusTagClass(col, opt), { active: row['c' + col.id] === opt }]"
                      @click.stop="onInlineStatusChange(row, col, opt); closeStatusPicker()"
                    >
                      {{ opt }}
                    </button>
                  </div>
                </div>
                <span
                  v-else-if="isStatusTagColumn(col)"
                  class="status-tag"
                  :class="getStatusTagClass(col, row['c' + col.id])"
                >
                  {{ formatCell(row['c' + col.id], col) }}
                </span>
                <template v-else>
                  {{ formatCell(row['c' + col.id], col) }}
                </template>
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
          <button :class="{ active: drawerTab === 'history' }" @click="drawerTab = 'history'; loadHistory()">{{ $t('collabTable.editHistory') }}</button>
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
                        <img
                          v-if="getAttachmentById(aid).isImage && attachmentPreviewUrls[aid]"
                          :src="attachmentPreviewUrls[aid]"
                          class="thumb-img thumb-img-clickable"
                          alt=""
                          @click.stop="openImagePreview(getAttachmentById(aid), attachmentPreviewUrls[aid])"
                        >
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
                    <img
                      :src="attachmentPreviewUrls[a.id]"
                      class="attachment-preview-img attachment-preview-img-clickable"
                      alt=""
                      @click="openImagePreview(a, attachmentPreviewUrls[a.id])"
                    >
                    <span class="attachment-meta">
                      <button type="button" class="link-button" @click="openImagePreview(a, attachmentPreviewUrls[a.id])">{{ a.fileName }}</button>
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
          <div v-if="drawerTab === 'history'" class="history-panel">
            <div v-if="historyLoading" class="text-muted">{{ $t('collabTable.loadingRecords') }}</div>
            <div v-else-if="!historyItems.length" class="text-muted">{{ $t('collabTable.historyEmpty') }}</div>
            <div v-else class="history-list">
              <div v-for="h in historyItems" :key="'h-' + h.id" class="history-item">
                <div class="history-topline">
                  <span class="history-action">{{ formatHistoryAction(h.action) }}</span>
                  <span class="history-time">{{ formatTime(h.createdAt) }}</span>
                </div>
                <div class="history-meta">
                  <span>{{ h.operatorName || h.operatorEmail || ('ID ' + h.operatorUserId) }}</span>
                  <span v-if="h.operatorSystemRole" class="history-role">{{ h.operatorSystemRole }}</span>
                  <span v-if="h.operatorProjectRole" class="history-role">{{ h.operatorProjectRole }}</span>
                </div>
                <div class="history-field">
                  <strong>{{ h.columnName || $t('collabTable.historyUnknownField') }}</strong>
                </div>
                <div class="history-values">
                  <div class="history-old">
                    <span class="text-muted">{{ $t('collabTable.historyOldValue') }}:</span>
                    <span>{{ h.oldValue == null || h.oldValue === '' ? '-' : h.oldValue }}</span>
                  </div>
                  <div class="history-new">
                    <span class="text-muted">{{ $t('collabTable.historyNewValue') }}:</span>
                    <span>{{ h.newValue == null || h.newValue === '' ? '-' : h.newValue }}</span>
                  </div>
                </div>
              </div>
            </div>
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

    <!-- AI 录入模式弹窗 -->
    <div v-if="showAiModal" class="drawer-mask" @click="closeAiModal">
      <div class="drawer ai-modal" @click.stop>
        <div class="drawer-header">
          <h3>{{ aiModalMode === 'csv' ? $t('collabTable.csvAiTitle') : $t('collabTable.aiInputTitle') }}</h3>
          <button class="close-btn" @click="closeAiModal">×</button>
        </div>
        <div class="drawer-body ai-body">
          <div class="ai-mode-tabs" role="tablist">
            <button type="button" role="tab" :aria-selected="aiModalMode === 'text'" :class="{ active: aiModalMode === 'text' }" @click="aiModalMode = 'text'">
              {{ $t('collabTable.aiTabText') }}
            </button>
            <button type="button" role="tab" :aria-selected="aiModalMode === 'csv'" :class="{ active: aiModalMode === 'csv' }" @click="aiModalMode = 'csv'">
              {{ $t('collabTable.aiTabCsv') }}
            </button>
          </div>
          <div v-show="aiModalMode === 'csv'" class="ai-csv-section">
            <div class="csv-import-mode-tabs" role="tablist">
              <button type="button" role="tab" :aria-selected="csvImportMode === 'ai'" :class="{ active: csvImportMode === 'ai' }" @click="csvImportMode = 'ai'">
                {{ $t('collabTable.csvImportModeAi') }}
              </button>
              <button type="button" role="tab" :aria-selected="csvImportMode === 'quick'" :class="{ active: csvImportMode === 'quick' }" @click="csvImportMode = 'quick'">
                {{ $t('collabTable.csvImportModeQuick') }}
              </button>
            </div>
            <p class="hint-text" v-if="csvImportMode === 'ai'">{{ $t('collabTable.csvAiHint') }}</p>
            <p class="hint-text" v-else>{{ $t('collabTable.csvQuickHint') }}</p>
            <div class="ai-upload-row">
              <input ref="csvAiFileInput" type="file" accept=".csv,text/csv" style="display:none" @change="onCsvAiFileSelected">
              <button type="button" class="secondary-button small" @click="$refs.csvAiFileInput && $refs.csvAiFileInput.click()">
                {{ $t('collabTable.csvAiPickFile') }}
              </button>
              <span v-if="csvAiFileName" class="csv-file-name">{{ csvAiFileName }}</span>
            </div>
            <button
              v-if="csvImportMode === 'ai'"
              type="button"
              class="primary-button"
              @click="runCsvAiPreview"
              :disabled="csvAiLoading || !csvAiFile"
            >
              {{ csvAiLoading ? $t('collabTable.csvAiParsing') : $t('collabTable.csvAiParseBtn') }}
            </button>
            <button
              v-else
              type="button"
              class="primary-button"
              @click="analyzeQuickCsv"
              :disabled="csvAiLoading || !csvAiFile"
            >
              {{ csvAiLoading ? $t('collabTable.csvQuickAnalyzing') : $t('collabTable.csvQuickAnalyze') }}
            </button>
            <p v-if="csvAiProgress" class="csv-progress-text">{{ csvAiProgress }}</p>
            <ul v-if="csvAiWarnings.length" class="csv-warn-list">
              <li v-for="(w, wi) in csvAiWarnings" :key="'cw-' + wi">{{ w }}</li>
            </ul>
            <div v-if="csvImportMode === 'quick' && quickCsvHeaders.length" class="quick-csv-mapping-panel">
              <h4 class="quick-csv-title">{{ $t('collabTable.csvQuickMappingTitle') }}</h4>
              <div class="quick-csv-sample">
                <span class="text-muted small">{{ $t('collabTable.csvQuickSampleHint') }}</span>
                <div v-if="quickCsvSampleRows.length" class="quick-csv-sample-box">
                  <pre>{{ JSON.stringify(quickCsvSampleRows[0], null, 2) }}</pre>
                </div>
              </div>
              <div class="quick-map-list">
                <div v-for="m in quickCsvMappings" :key="'qm-' + m.targetColumnId" class="quick-map-row">
                  <div class="quick-map-target">{{ getColumnById(m.targetColumnId) ? getColumnById(m.targetColumnId).name : ('#' + m.targetColumnId) }}</div>
                  <div class="quick-map-source">
                    <select v-model="m.sourceHeaders" class="field-input quick-multi-select" multiple>
                      <option v-for="h in quickCsvHeaders" :key="'qh-' + h" :value="h">{{ h }}</option>
                    </select>
                    <input v-model="m.joinWith" type="text" class="field-input quick-join-input" :placeholder="$t('collabTable.csvQuickJoinPlaceholder')">
                  </div>
                </div>
              </div>
              <button
                type="button"
                class="primary-button"
                @click="commitQuickCsvImport"
                :disabled="quickCsvImporting"
              >
                {{ quickCsvImporting ? $t('collabTable.csvQuickImporting') : $t('collabTable.csvQuickImport') }}
              </button>
            </div>
          </div>
          <div v-show="aiModalMode === 'text'" class="ai-input-section">
            <label class="field-label">{{ $t('collabTable.aiInputRawText') }}</label>
            <textarea
              v-model="aiInputText"
              rows="4"
              class="field-input"
              :placeholder="$t('collabTable.aiInputPlaceholder')"
            ></textarea>
          </div>
          <div v-show="aiModalMode === 'text'" class="ai-attachments-section">
            <label class="field-label">{{ $t('collabTable.aiInputAttachments') }}</label>
            <div class="ai-upload-row">
              <input ref="aiFileInput" type="file" multiple style="display:none" @change="onAiFilesSelected">
              <button type="button" class="secondary-button small" @click="triggerAiUpload">
                {{ $t('collabTable.uploadAttachment') }}
              </button>
            </div>
            <div v-if="aiSessionAttachments.length" class="ai-attachment-list">
              <label v-for="a in aiSessionAttachments" :key="a.id" class="multi-select-item ai-attachment-item">
                <input
                  type="checkbox"
                  :value="a.id"
                  v-model="aiSelectedAttachmentIds"
                >
                <div class="ai-attachment-preview">
                  <img v-if="a.isImage && a.previewUrl" :src="a.previewUrl" class="ai-thumb-img" alt="">
                  <span v-else class="ai-file-icon">📎</span>
                </div>
                <div class="ai-attachment-meta">
                  <span class="ai-file-name">{{ a.fileName }}</span>
                  <span class="ai-file-size">{{ formatSize(a.fileSize) }}</span>
                </div>
              </label>
            </div>
            <div v-else class="text-muted small">{{ $t('collabTable.noAiAttachments') }}</div>
          </div>
          <div v-show="aiModalMode === 'text'" class="ai-actions">
            <button class="primary-button" @click="runAiPreview" :disabled="aiLoading">
              {{ aiLoading ? $t('collabTable.aiGenerating') : $t('collabTable.aiGenerateTasks') }}
            </button>
          </div>
          <div v-if="aiTasks.length" class="ai-result-section">
            <h4>{{ $t('collabTable.aiTaskDrafts') }}</h4>
            <div v-for="(t, idx) in aiTasks" :key="idx" class="ai-task-item">
              <input
                v-model="t.title"
                class="field-input"
                :placeholder="$t('collabTable.aiTaskTitlePlaceholder')"
              >
              <textarea
                v-model="t.description"
                rows="3"
                class="field-input"
                :placeholder="$t('collabTable.aiTaskDescPlaceholder')"
              ></textarea>
              <button class="link-button danger" @click="removeAiTask(idx)">
                {{ $t('collabTable.removeDraft') }}
              </button>
            </div>
            <div class="ai-commit-actions">
              <button class="primary-button" @click="commitAiTasks" :disabled="aiCommitting || !aiTasks.length">
                {{ aiCommitting ? $t('collabTable.aiCommitting') : $t('collabTable.aiInsertToTable') }}
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 组合筛选弹窗 -->
    <div v-if="showFilterModal" class="drawer-mask" @click="closeFilterModal">
      <div class="drawer filter-modal" @click.stop>
        <div class="drawer-header">
          <h3>{{ $t('collabTable.filterTitle') }}</h3>
          <button class="close-btn" @click="closeFilterModal">×</button>
        </div>
        <div class="drawer-body">
          <div class="field-list">
            <div v-if="!filterRules.length" class="filter-empty-block">
              <p class="text-muted small">{{ $t('collabTable.filterEmptyHint') }}</p>
              <button type="button" class="secondary-button small" @click="addFilterRule">
                {{ $t('collabTable.filterAddRule') }}
              </button>
            </div>
            <template v-else>
              <div v-for="(r, idx) in filterRules" :key="'fr-' + idx" class="filter-rule-row">
                <div class="filter-rule-col">
                  <label class="field-label">{{ $t('collabTable.filterColumn') }}</label>
                  <select class="field-input" v-model="r.columnId" @change="onFilterRuleColumnChanged(idx)">
                    <option v-for="c in filterableColumns" :key="c.id" :value="c.id">
                      {{ c.name }}
                    </option>
                  </select>
                </div>
                <div class="filter-rule-col">
                  <label class="field-label">{{ $t('collabTable.filterOperator') }}</label>
                  <select class="field-input" v-model="r.op">
                    <option value="eq">{{ $t('collabTable.filterOpEq') }}</option>
                    <option value="ne">{{ $t('collabTable.filterOpNe') }}</option>
                    <option value="contains">{{ $t('collabTable.filterOpContains') }}</option>
                    <option value="not_contains">{{ $t('collabTable.filterOpNotContains') }}</option>
                    <option value="empty">{{ $t('collabTable.filterOpEmpty') }}</option>
                    <option value="not_empty">{{ $t('collabTable.filterOpNotEmpty') }}</option>
                  </select>
                </div>
                <div class="filter-rule-col filter-rule-value">
                  <label class="field-label">{{ $t('collabTable.filterValue') }}</label>
                  <select
                    v-if="requiresFilterValue(r.op)"
                    class="field-input"
                    v-model="r.value"
                  >
                    <option value="">{{ $t('collabTable.pleaseSelect') }}</option>
                    <option v-for="v in getFilterValueOptions(r.columnId)" :key="v" :value="v">
                      {{ v }}
                    </option>
                  </select>
                  <div v-else class="text-muted small">{{ $t('collabTable.filterValueNotNeeded') }}</div>
                </div>
                <div class="filter-rule-actions">
                  <button type="button" class="filter-rule-remove-btn" @click="removeFilterRule(idx)">
                    {{ $t('collabTable.filterRemoveRule') }}
                  </button>
                </div>
              </div>
            </template>
          </div>

          <div class="drawer-actions filter-actions">
            <button type="button" class="secondary-button small" @click="addFilterRule">
              {{ $t('collabTable.filterAddRule') }}
            </button>
            <div style="flex: 1"></div>
            <button type="button" class="link-button" @click="clearFilterRules">
              {{ $t('collabTable.filterClearAll') }}
            </button>
            <button type="button" class="primary-button" @click="applyFilters">
              {{ $t('collabTable.filterApply') }}
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- 图片在线预览（字段 / 附件 / 列表缩略图） -->
    <div v-if="imagePreviewOpen" class="image-preview-backdrop" @click.self="closeImagePreview">
      <div class="image-preview-modal" @click.stop>
        <div class="image-preview-toolbar">
          <div class="image-preview-toolbar-left">
            <button type="button" class="image-tool-btn" disabled :title="$t('collabTable.imagePreviewComingSoon')">−</button>
            <button type="button" class="image-tool-btn" disabled :title="$t('collabTable.imagePreviewComingSoon')">+</button>
            <button type="button" class="image-tool-btn image-tool-wide" disabled :title="$t('collabTable.imagePreviewComingSoon')">1:1</button>
            <button type="button" class="image-tool-btn" disabled :title="$t('collabTable.imagePreviewComingSoon')">↻</button>
            <button type="button" class="image-tool-btn" disabled :title="$t('collabTable.imagePreviewComingSoon')">✎</button>
          </div>
          <div class="image-preview-toolbar-right">
            <button type="button" class="image-tool-btn image-tool-download" :title="$t('collabTable.imagePreviewDownload')" @click="downloadAttachment(imagePreviewAttachment)">
              ⬇
            </button>
            <button type="button" class="image-tool-btn image-tool-close" :title="$t('collabTable.imagePreviewClose')" @click="closeImagePreview">×</button>
          </div>
        </div>
        <div class="image-preview-body">
          <img v-if="imagePreviewUrl" :src="imagePreviewUrl" class="image-preview-main" :alt="imagePreviewTitle">
        </div>
        <div v-if="imagePreviewTitle" class="image-preview-footer">{{ imagePreviewTitle }}</div>
      </div>
    </div>
  </div>
</template>

<script>
import { Chart as ChartJS, registerables } from 'chart.js'

ChartJS.register(...registerables)

export default {
  name: 'CollabTableView',
  data () {
    return {
      projectId: null,
      projectName: '',
      tableBaseName: '',
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
      historyItems: [],
      historyLoading: false,
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
      listAttachmentPreviewUrls: {},
      showAiModal: false,
      aiModalMode: 'text',
      aiInputText: '',
      aiTasks: [],
      aiLoading: false,
      showFilterModal: false,
      filterRules: [],
      activeFilters: [],
      csvAiFile: null,
      csvAiFileName: '',
      csvAiLoading: false,
      csvAiProgress: '',
      csvAiWarnings: [],
      csvImportMode: 'ai',
      quickCsvSessionId: '',
      quickCsvHeaders: [],
      quickCsvSampleRows: [],
      quickCsvMappings: [],
      quickCsvImporting: false,
      aiCommitting: false,
      aiSessionAttachments: [],
      aiSelectedAttachmentIds: [],
      imagePreviewOpen: false,
      imagePreviewUrl: '',
      imagePreviewTitle: '',
      imagePreviewAttachment: null,
      imagePreviewEscapeHandler: null,
      statusUpdatingMap: {},
      statusPickerOpenKey: '',
      /** 下拉锚点（viewport），用于 fixed 定位，避免被表格 overflow 裁剪 */
      statusPickerAnchor: null,
      /** 批量选择：recordId -> true */
      rowSelection: {},
      batchDeleting: false,
      showDashboard: false,
      dashboardWeeks: 8,
      dashboardCharts: {
        taskCompletion: null,
        weeklyCreated: null,
        weeklyResolved: null,
        importance: null
      }
    }
  },
  computed: {
    pageTableTitle () {
      const base = (this.tableBaseName || '').trim() || this.$t('collabTable.defaultTableName')
      const pn = (this.projectName || '').trim()
      if (!pn) return base
      return this.$t('collabTable.pageTitleWithProject', { project: pn, table: base })
    },
    filterableColumns () {
      const allowed = ['重要程度', '当前状态', '验收结果']
      return Array.isArray(this.columns) ? this.columns.filter(c => allowed.includes(c.name)) : []
    },
    selectedCount () {
      return Object.keys(this.rowSelection).length
    },
    allRowsSelected () {
      if (!this.records.length) return false
      return this.records.every(r => this.rowSelection[r.id])
    },
    statusPickerMenuStyle () {
      const a = this.statusPickerAnchor
      if (!a) return {}
      const vw = typeof window !== 'undefined' ? window.innerWidth : 1200
      const vh = typeof window !== 'undefined' ? window.innerHeight : 800
      let left = a.left
      const minW = Math.max(a.width || 0, 240)
      const maxW = Math.min(440, vw - 16)
      const panelW = Math.min(Math.max(minW, 240), maxW)
      if (left + panelW > vw - 8) {
        left = Math.max(8, vw - 8 - panelW)
      }
      const maxH = Math.min(480, Math.max(200, vh - a.top - 12))
      return {
        position: 'fixed',
        top: a.top + 'px',
        left: left + 'px',
        minWidth: panelW + 'px',
        maxWidth: maxW + 'px',
        maxHeight: maxH + 'px',
        zIndex: '200',
        overflow: 'auto'
      }
    }
  },
  updated () {
    this.$nextTick(() => {
      const el = this.$refs.selectAllCheckbox
      if (el) {
        el.indeterminate = this.selectedCount > 0 && !this.allRowsSelected
      }
    })
  },
  created () {
    this.projectId = Number(this.$route.params.projectId)
    this.loadProjectSummary()
    this.loadTable()
    this.loadRecords()
    this.loadProjectMembers()
  },
  beforeDestroy () {
    this.detachImagePreviewEscape()
    this.detachStatusPickerOutside()
    this.detachStatusPickerScrollClose()
    this.closeImagePreview()
    this.revokeListAttachmentPreviewUrls()
    this.destroyDashboardCharts()
  },
  methods: {
    toggleDashboardView () {
      this.showDashboard = !this.showDashboard
      if (this.showDashboard) this.$nextTick(() => this.renderDashboardCharts())
    },
    destroyDashboardCharts () {
      Object.keys(this.dashboardCharts).forEach(k => {
        const chart = this.dashboardCharts[k]
        if (chart && typeof chart.destroy === 'function') chart.destroy()
        this.dashboardCharts[k] = null
      })
    },
    getColumnIdByName (name) {
      const target = String(name || '').trim()
      const col = (this.columns || []).find(c => String(c && c.name ? c.name : '').trim() === target)
      return col ? col.id : null
    },
    getRecordStringValue (row, columnName) {
      const colId = this.getColumnIdByName(columnName)
      if (!colId || !row) return ''
      const raw = row['c' + colId]
      return raw == null ? '' : String(raw).trim()
    },
    isResolvedRecord (row) {
      const text = [
        this.getRecordStringValue(row, '解决情况'),
        this.getRecordStringValue(row, '验收结果'),
        this.getRecordStringValue(row, '当前状态')
      ].join(' ')
      const hitWords = ['已解决', '已完成', '通过任务关闭', '通过', '完成', '关闭', '已验收']
      return hitWords.some(w => text.includes(w))
    },
    weekStartDate (input) {
      const d = input instanceof Date ? new Date(input.getTime()) : new Date(input)
      if (Number.isNaN(d.getTime())) return null
      d.setHours(0, 0, 0, 0)
      const day = d.getDay()
      const diff = day === 0 ? 6 : day - 1
      d.setDate(d.getDate() - diff)
      return d
    },
    weekLabel (dateObj) {
      if (!dateObj) return ''
      const y = dateObj.getFullYear()
      const m = String(dateObj.getMonth() + 1).padStart(2, '0')
      const d = String(dateObj.getDate()).padStart(2, '0')
      return `${y}-${m}-${d}`
    },
    buildWeeklySeries (rows, filterFn) {
      const weeks = Number(this.dashboardWeeks) > 0 ? Number(this.dashboardWeeks) : 8
      const nowWeek = this.weekStartDate(new Date())
      const labels = []
      const counts = []
      const weekMap = {}
      for (let i = weeks - 1; i >= 0; i--) {
        const d = new Date(nowWeek.getTime())
        d.setDate(d.getDate() - i * 7)
        const key = this.weekLabel(d)
        labels.push(key)
        weekMap[key] = 0
      }
      const safeRows = Array.isArray(rows) ? rows : []
      safeRows.forEach(row => {
        if (typeof filterFn === 'function' && !filterFn(row)) return
        const t = row && row.createdAt ? new Date(row.createdAt) : null
        if (!t || Number.isNaN(t.getTime())) return
        const wk = this.weekLabel(this.weekStartDate(t))
        if (Object.prototype.hasOwnProperty.call(weekMap, wk)) weekMap[wk] += 1
      })
      labels.forEach(l => counts.push(weekMap[l] || 0))
      return { labels, counts }
    },
    getImportanceDistribution () {
      const colId = this.getColumnIdByName('重要程度')
      if (!colId) return { labels: [], counts: [] }
      const map = {}
      const safeRecords = Array.isArray(this.records) ? this.records : []
      safeRecords.forEach(row => {
        const raw = row['c' + colId]
        const key = raw == null || String(raw).trim() === '' ? this.$t('collabTable.dashboardUnknown') : String(raw).trim()
        map[key] = (map[key] || 0) + 1
      })
      const labels = Object.keys(map)
      const counts = labels.map(k => map[k])
      return { labels, counts }
    },
    renderDashboardCharts () {
      if (!this.showDashboard) return
      this.destroyDashboardCharts()
      const total = (this.records || []).length
      const resolved = (this.records || []).filter(r => this.isResolvedRecord(r)).length
      const unresolved = Math.max(total - resolved, 0)
      const createdSeries = this.buildWeeklySeries(this.records)
      const resolvedSeries = this.buildWeeklySeries(this.records, r => this.isResolvedRecord(r))
      const importance = this.getImportanceDistribution()

      if (this.$refs.taskCompletionChart) {
        this.dashboardCharts.taskCompletion = new ChartJS(this.$refs.taskCompletionChart, {
          type: 'pie',
          data: {
            labels: [this.$t('collabTable.dashboardResolved'), this.$t('collabTable.dashboardUnresolved')],
            datasets: [{ data: [resolved, unresolved], backgroundColor: ['#22c55e', '#94a3b8'] }]
          },
          options: { responsive: true, maintainAspectRatio: false }
        })
      }
      if (this.$refs.weeklyCreatedChart) {
        this.dashboardCharts.weeklyCreated = new ChartJS(this.$refs.weeklyCreatedChart, {
          type: 'line',
          data: {
            labels: createdSeries.labels,
            datasets: [{ label: this.$t('collabTable.dashboardWeeklyCreated'), data: createdSeries.counts, borderColor: '#3b82f6', backgroundColor: 'rgba(59,130,246,0.2)', fill: true, tension: 0.25 }]
          },
          options: { responsive: true, maintainAspectRatio: false }
        })
      }
      if (this.$refs.weeklyResolvedChart) {
        this.dashboardCharts.weeklyResolved = new ChartJS(this.$refs.weeklyResolvedChart, {
          type: 'line',
          data: {
            labels: resolvedSeries.labels,
            datasets: [{ label: this.$t('collabTable.dashboardWeeklyResolved'), data: resolvedSeries.counts, borderColor: '#10b981', backgroundColor: 'rgba(16,185,129,0.2)', fill: true, tension: 0.25 }]
          },
          options: { responsive: true, maintainAspectRatio: false }
        })
      }
      if (this.$refs.importanceChart) {
        this.dashboardCharts.importance = new ChartJS(this.$refs.importanceChart, {
          type: 'bar',
          data: {
            labels: importance.labels,
            datasets: [{ label: this.$t('collabTable.dashboardImportantDistribution'), data: importance.counts, backgroundColor: '#f59e0b' }]
          },
          options: { responsive: true, maintainAspectRatio: false }
        })
      }
    },
    openAiInput (mode) {
      this.aiModalMode = mode === 'csv' ? 'csv' : 'text'
      this.showAiModal = true
      this.csvImportMode = 'ai'
      this.aiInputText = ''
      this.aiTasks = []
      this.aiSelectedAttachmentIds = []
      this.clearAiSessionAttachments()
      this.csvAiFile = null
      this.csvAiFileName = ''
      this.csvAiWarnings = []
      this.csvAiProgress = ''
      this.clearQuickCsvState()
      if (this.$refs.csvAiFileInput) this.$refs.csvAiFileInput.value = ''
    },
    closeAiModal () {
      if (this.quickCsvSessionId) {
        this.$http.delete(`/collab/projects/${this.projectId}/csv-quick-import/session/${this.quickCsvSessionId}`, { timeout: 15000 }).catch(() => {})
      }
      this.showAiModal = false
      this.clearQuickCsvState()
    },
    openFilterModal () {
      // 从当前激活的筛选规则恢复编辑
      this.filterRules = Array.isArray(this.activeFilters) ? this.activeFilters.map(r => ({ ...r })) : []
      if (!this.filterRules.length) {
        const defCol = this.filterableColumns[0]
        this.filterRules = [{
          columnId: defCol ? defCol.id : null,
          op: 'eq',
          value: ''
        }]
      }
      this.showFilterModal = true
    },
    closeFilterModal () {
      this.showFilterModal = false
    },
    createEmptyFilterRule () {
      const defCol = this.filterableColumns[0]
      return {
        columnId: defCol ? defCol.id : null,
        op: 'eq',
        value: ''
      }
    },
    addFilterRule () {
      this.filterRules.push(this.createEmptyFilterRule())
    },
    removeFilterRule (idx) {
      this.filterRules.splice(idx, 1)
    },
    async removeActiveFilterAt (idx) {
      if (!Array.isArray(this.activeFilters) || idx < 0 || idx >= this.activeFilters.length) return
      const next = this.activeFilters.filter((_, i) => i !== idx)
      this.activeFilters = next
      await this.loadRecords(next)
    },
    formatActiveFilterLabel (f) {
      if (!f) return ''
      const col = this.getColumnById(f.columnId)
      const colName = col ? (col.name || '') : ('#' + f.columnId)
      const opKeys = {
        eq: 'filterOpEq',
        ne: 'filterOpNe',
        contains: 'filterOpContains',
        not_contains: 'filterOpNotContains',
        empty: 'filterOpEmpty',
        not_empty: 'filterOpNotEmpty'
      }
      const opLabel = this.$t('collabTable.' + (opKeys[f.op] || 'filterOpEq'))
      if (!this.requiresFilterValue(f.op)) return `${colName} · ${opLabel}`
      const v = f.value != null && String(f.value).trim() !== '' ? String(f.value) : '—'
      return `${colName} · ${opLabel} · ${v}`
    },
    requiresFilterValue (op) {
      return op !== 'empty' && op !== 'not_empty'
    },
    getColumnById (colId) {
      const id = Number(colId)
      return this.columns.find(c => Number(c.id) === id)
    },
    getFilterValueOptions (colId) {
      const col = this.getColumnById(colId)
      if (!col || !col.optionGroup || !Array.isArray(col.optionGroup.options)) return []
      return col.optionGroup.options
    },
    onFilterRuleColumnChanged (idx) {
      const r = this.filterRules[idx]
      if (!r) return
      if (!this.requiresFilterValue(r.op)) return
      const opts = this.getFilterValueOptions(r.columnId)
      if (!opts.length) {
        r.value = ''
        return
      }
      // 若当前 value 不在新列的可选项中，则重置为第一个
      if (!opts.includes(r.value)) r.value = opts[0]
    },
    clearFilterRules () {
      this.activeFilters = []
      this.filterRules = []
      this.closeFilterModal()
      this.loadRecords([])
    },
    buildFiltersPayload () {
      const payload = []
      for (const r of this.filterRules) {
        if (!r || r.columnId == null) continue
        if (!r.op) continue
        const rule = { columnId: r.columnId, op: r.op }
        if (this.requiresFilterValue(r.op)) {
          rule.value = r.value
        }
        payload.push(rule)
      }
      return payload
    },
    async applyFilters () {
      if (!this.filterRules.length) {
        this.activeFilters = []
        this.closeFilterModal()
        await this.loadRecords([])
        return
      }
      // 基本校验：value 必须存在（除 empty/not_empty）
      for (const r of this.filterRules) {
        if (!r || r.columnId == null || !r.op) continue
        if (this.requiresFilterValue(r.op)) {
          if (r.value == null || String(r.value).trim() === '') {
            alert(this.$t('collabTable.filterValueRequired') || '请选择匹配值')
            return
          }
        }
      }
      const payload = this.buildFiltersPayload()
      this.activeFilters = payload
      this.closeFilterModal()
      await this.loadRecords(payload)
    },
    onCsvAiFileSelected (e) {
      if (this.quickCsvSessionId) {
        this.$http.delete(`/collab/projects/${this.projectId}/csv-quick-import/session/${this.quickCsvSessionId}`, { timeout: 15000 }).catch(() => {})
      }
      const f = e.target && e.target.files && e.target.files[0]
      this.csvAiFile = f || null
      this.csvAiFileName = f ? f.name : ''
      this.csvAiWarnings = []
      this.csvAiProgress = ''
      this.quickCsvSessionId = ''
      this.quickCsvHeaders = []
      this.quickCsvSampleRows = []
      this.quickCsvMappings = []
    },
    clearQuickCsvState () {
      this.quickCsvSessionId = ''
      this.quickCsvHeaders = []
      this.quickCsvSampleRows = []
      this.quickCsvMappings = []
      this.quickCsvImporting = false
    },
    initQuickCsvMappings (headers) {
      const headerSet = new Set((headers || []).map(h => String(h || '').trim()))
      const out = []
      for (const col of this.columns || []) {
        const name = String(col.name || '').trim()
        if (!name) continue
        out.push({
          targetColumnId: col.id,
          sourceHeaders: headerSet.has(name) ? [name] : [],
          joinWith: '\n'
        })
      }
      this.quickCsvMappings = out
    },
    async analyzeQuickCsv () {
      if (!this.csvAiFile) return
      this.csvAiLoading = true
      this.csvAiWarnings = []
      try {
        const fd = new FormData()
        fd.append('file', this.csvAiFile)
        const resp = await this.$http.post(`/collab/projects/${this.projectId}/csv-quick-import/session`, fd, {
          headers: { 'Content-Type': 'multipart/form-data' },
          timeout: 120000
        })
        if (!resp.data || resp.data.code !== 0 || !resp.data.data) {
          alert((resp.data && resp.data.message) || this.$t('collabTable.csvQuickAnalyzeFailed'))
          return
        }
        const d = resp.data.data
        this.quickCsvSessionId = d.sessionId || ''
        this.quickCsvHeaders = Array.isArray(d.headers) ? d.headers : []
        this.quickCsvSampleRows = Array.isArray(d.sampleRows) ? d.sampleRows : []
        this.initQuickCsvMappings(this.quickCsvHeaders)
      } catch (e) {
        const msg = (e.response && e.response.data && e.response.data.message) || ''
        alert(msg || this.$t('collabTable.csvQuickAnalyzeFailed'))
      } finally {
        this.csvAiLoading = false
      }
    },
    async commitQuickCsvImport () {
      if (!this.quickCsvSessionId) {
        alert(this.$t('collabTable.csvQuickNeedAnalyze'))
        return
      }
      const mappings = (this.quickCsvMappings || [])
        .map(m => ({
          targetColumnId: m.targetColumnId,
          sourceHeaders: Array.isArray(m.sourceHeaders) ? m.sourceHeaders.filter(Boolean) : [],
          joinWith: m.joinWith != null ? String(m.joinWith) : '\n'
        }))
        .filter(m => m.sourceHeaders.length > 0)
      if (!mappings.length) {
        alert(this.$t('collabTable.csvQuickNeedMapping'))
        return
      }
      this.quickCsvImporting = true
      try {
        const resp = await this.$http.post(`/collab/projects/${this.projectId}/csv-quick-import/commit`, {
          sessionId: this.quickCsvSessionId,
          mappings
        }, { timeout: 180000 })
        if (!resp.data || resp.data.code !== 0 || !resp.data.data) {
          alert((resp.data && resp.data.message) || this.$t('collabTable.csvQuickImportFailed'))
          return
        }
        const d = resp.data.data
        alert(this.$t('collabTable.csvQuickImportDone', { created: d.createdCount || 0, skipped: d.skippedCount || 0 }))
        this.showAiModal = false
        this.clearQuickCsvState()
        this.csvAiFile = null
        this.csvAiFileName = ''
        await this.loadRecords()
      } catch (e) {
        const msg = (e.response && e.response.data && e.response.data.message) || ''
        alert(msg || this.$t('collabTable.csvQuickImportFailed'))
      } finally {
        this.quickCsvImporting = false
      }
    },
    async runCsvAiPreview () {
      if (!this.csvAiFile) return
      this.csvAiLoading = true
      this.csvAiWarnings = []
      this.csvAiProgress = ''
      const chunkTimeout = 360000
      const sessionTimeout = 120000
      let sessionId = null
      try {
        const fd = new FormData()
        fd.append('file', this.csvAiFile)
        const initResp = await this.$http.post(`/collab/projects/${this.projectId}/csv-ai-import/session`, fd, {
          headers: { 'Content-Type': 'multipart/form-data' },
          timeout: sessionTimeout
        })
        if (!initResp.data || initResp.data.code !== 0 || !initResp.data.data) {
          alert((initResp.data && initResp.data.message) || this.$t('collabTable.csvAiPreviewFailed'))
          return
        }
        const { sessionId: sid, chunksTotal } = initResp.data.data
        sessionId = sid
        const total = Number(chunksTotal) || 0
        if (!sid || total < 1) {
          alert(this.$t('collabTable.csvAiPreviewFailed'))
          return
        }
        const merged = []
        const allWarnings = []
        for (let i = 0; i < total; i++) {
          this.csvAiProgress = this.$t('collabTable.csvAiProgress', { current: i + 1, total })
          const chunkResp = await this.$http.post(
            `/collab/projects/${this.projectId}/csv-ai-import/session/${sid}/chunk`,
            { chunkIndex: i },
            { timeout: chunkTimeout, headers: { 'Content-Type': 'application/json' } }
          )
          if (!chunkResp.data || chunkResp.data.code !== 0 || !chunkResp.data.data) {
            throw new Error((chunkResp.data && chunkResp.data.message) || 'chunk failed')
          }
          const d = chunkResp.data.data
          if (Array.isArray(d.items) && d.items.length) {
            merged.push(...d.items)
          }
          if (Array.isArray(d.warnings)) {
            allWarnings.push(...d.warnings)
          }
        }
        this.csvAiProgress = ''
        if (!merged.length) {
          alert(this.$t('collabTable.csvAiNoDrafts'))
          this.csvAiWarnings = allWarnings
          return
        }
        this.aiTasks = merged
        this.csvAiWarnings = allWarnings
        try {
          await this.$http.delete(`/collab/projects/${this.projectId}/csv-ai-import/session/${sid}`, { timeout: 15000 })
        } catch (e) {
          // 忽略清理失败，会话会 TTL 过期
        }
      } catch (err) {
        this.csvAiProgress = ''
        const msg = (err.response && err.response.data && err.response.data.message) || err.message || ''
        if (sessionId) {
          try {
            await this.$http.delete(`/collab/projects/${this.projectId}/csv-ai-import/session/${sessionId}`, { timeout: 15000 })
          } catch (e) { /* ignore */ }
        }
        alert(msg || this.$t('collabTable.csvAiPreviewFailed'))
      } finally {
        this.csvAiLoading = false
      }
    },
    async runAiPreview () {
      if (!this.aiInputText || !this.aiInputText.trim()) return
      this.aiLoading = true
      try {
        const resp = await this.$http.post(`/collab/projects/${this.projectId}/ai-tasks/preview`, {
          rawText: this.aiInputText,
          attachmentIds: this.aiSelectedAttachmentIds
        })
        if (resp.data && resp.data.code === 0 && resp.data.data && resp.data.data.items) {
          this.aiTasks = resp.data.data.items
        } else {
          alert((resp.data && resp.data.message) || this.$t('collabTable.aiPreviewFailed'))
        }
      } catch (e) {
        alert(this.$t('collabTable.aiPreviewFailed'))
      } finally {
        this.aiLoading = false
      }
    },
    removeAiTask (idx) {
      this.aiTasks.splice(idx, 1)
    },
    async commitAiTasks () {
      if (!this.aiTasks.length) return
      this.aiCommitting = true
      try {
        const resp = await this.$http.post(`/collab/projects/${this.projectId}/ai-tasks/commit`, {
          tasks: this.aiTasks
        })
        if (resp.data && resp.data.code === 0) {
          this.showAiModal = false
          this.aiInputText = ''
          this.aiTasks = []
          await this.loadRecords()
          this.resyncDrawerAfterRecordsReload()
        } else {
          alert((resp.data && resp.data.message) || this.$t('collabTable.aiCommitFailed'))
        }
      } catch (e) {
        alert(this.$t('collabTable.aiCommitFailed'))
      } finally {
        this.aiCommitting = false
      }
    },
    triggerAiUpload () {
      if (this.$refs.aiFileInput) this.$refs.aiFileInput.click()
    },
    clearAiSessionAttachments () {
      this.aiSessionAttachments.forEach(a => {
        if (a.previewUrl) {
          try {
            URL.revokeObjectURL(a.previewUrl)
          } catch (e) {
            // ignore revoke error
          }
        }
      })
      this.aiSessionAttachments = []
      this.aiSelectedAttachmentIds = []
    },
    async onAiFilesSelected (e) {
      const files = Array.from(e.target.files || [])
      if (!files.length) return
      for (const file of files) {
        const fd = new FormData()
        fd.append('file', file)
        try {
          const resp = await this.$http.post(`/collab/projects/${this.projectId}/attachments`, fd, {
            headers: { 'Content-Type': 'multipart/form-data' }
          })
          if (resp.data && resp.data.code === 0 && resp.data.data) {
            const a = resp.data.data
            const item = {
              id: a.id,
              fileName: a.fileName,
              fileSize: a.fileSize,
              isImage: a.isImage === true,
              previewUrl: a.isImage ? URL.createObjectURL(file) : null
            }
            this.aiSessionAttachments.push(item)
          }
        } catch (err) {
          // 简单提示即可
          alert(this.$t('collabTable.aiUploadFailed') || '上传附件失败')
        }
      }
      e.target.value = ''
    },
    async loadProjectSummary () {
      try {
        const resp = await this.$http.get(`/collab/projects/${this.projectId}`)
        if (resp.data && resp.data.code === 0 && resp.data.data) {
          this.projectName = resp.data.data.name != null ? String(resp.data.data.name) : ''
        }
      } catch (e) {
        if (e.response && e.response.status === 401) this.$router.push({ name: 'login' })
      }
    },
    async loadTable () {
      this.tableLoading = true
      try {
        const resp = await this.$http.get(`/collab/projects/${this.projectId}/table`)
        if (resp.data && resp.data.code === 0) {
          const d = resp.data.data
          this.tableBaseName = d.name != null && String(d.name).trim() !== ''
            ? String(d.name).trim()
            : this.$t('collabTable.defaultTableName')
          this.columns = (d.columns || []).sort((a, b) => (a.sortOrder || 0) - (b.sortOrder || 0))
        }
      } catch (e) {
        if (e.response && e.response.status === 401) this.$router.push({ name: 'login' })
      } finally {
        this.tableLoading = false
      }
    },
    /**
     * 拉取当前项目下全部记录（含筛选条件）：分页请求直至取完，避免单页 100 条截断。
     */
    async loadRecords (filters) {
      this.recordsLoading = true
      try {
        const effectiveFilters = filters !== undefined ? filters : this.activeFilters
        const baseParams = { _t: Date.now() }
        if (Array.isArray(effectiveFilters) && effectiveFilters.length) {
          baseParams.filters = JSON.stringify(effectiveFilters)
        }
        const pageSize = 500
        const maxPages = 10000
        const all = []
        for (let page = 1; page <= maxPages; page++) {
          const resp = await this.$http.get(`/collab/projects/${this.projectId}/records`, {
            params: { ...baseParams, page, pageSize }
          })
          if (!resp.data || resp.data.code !== 0) break
          const data = resp.data.data || {}
          const batch = Array.isArray(data.items) ? data.items : []
          all.push(...batch)
          const total = data.total != null ? Number(data.total) : NaN
          if (batch.length < pageSize) break
          if (!Number.isNaN(total) && all.length >= total) break
        }
        this.records = all
        this.pruneRowSelectionAfterLoad()
        this.revokeListAttachmentPreviewUrls()
        await this.loadListAttachmentPreviews()
        if (this.showDashboard) this.$nextTick(() => this.renderDashboardCharts())
      } finally {
        this.recordsLoading = false
      }
    },
    pruneRowSelectionAfterLoad () {
      const idSet = new Set(this.records.map(r => r.id))
      Object.keys(this.rowSelection).forEach(k => {
        if (!idSet.has(Number(k))) this.$delete(this.rowSelection, k)
      })
    },
    onToggleSelectAll (e) {
      const on = e.target.checked
      if (on) {
        this.records.forEach(r => this.$set(this.rowSelection, r.id, true))
      } else {
        this.rowSelection = {}
      }
    },
    toggleRowSelection (row, checked) {
      if (checked) this.$set(this.rowSelection, row.id, true)
      else this.$delete(this.rowSelection, row.id)
    },
    clearRowSelection () {
      this.rowSelection = {}
    },
    confirmBatchDelete () {
      const ids = Object.keys(this.rowSelection).map(Number)
      if (!ids.length) return
      if (!window.confirm(this.$t('collabTable.confirmBatchDelete', { n: ids.length }))) return
      this.batchDeleteRecords(ids)
    },
    async batchDeleteRecords (ids) {
      this.batchDeleting = true
      try {
        for (const id of ids) {
          await this.$http.delete(`/collab/records/${id}`)
        }
        if (this.drawerRecord && ids.includes(this.drawerRecord.id)) {
          this.revokeAttachmentPreviewUrls()
          this.drawerRecord = null
          this.drawerEditValues = {}
        }
        this.rowSelection = {}
        await this.loadRecords()
      } catch (e) {
        alert(this.$t('collabTable.batchDeleteFailed'))
        await this.loadRecords()
      } finally {
        this.batchDeleting = false
      }
    },
    getColumnHeaderClass (col) {
      if ((col.name || '').trim() === '问题描述') return 'col-problem'
      if (this.isAttachmentColumn(col)) return 'col-attachment'
      return ''
    },
    getCellClass (col) {
      const classes = []
      if ((col.name || '').trim() === '问题描述') classes.push('cell-problem')
      if (this.isAttachmentColumn(col)) classes.push('cell-attachment')
      if (this.isStatusTagColumn(col)) classes.push('cell-status')
      return classes.join(' ')
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
    /** 将列表行数据同步到抽屉编辑表单（与接口返回字段一致） */
    syncDrawerEditValuesFromRow (row) {
      if (!row) return
      this.columns.forEach(col => {
        let v = row['c' + col.id]
        if (v != null && (col.columnType === 'datetime' || col.columnType === 'date')) {
          v = String(v).slice(0, 19).replace('T', ' ')
        }
        this.$set(this.drawerEditValues, 'c' + col.id, v != null && v !== '' ? v : null)
      })
    },
    /**
     * 列表重新拉取后，抽屉若仍打开则绑定到 records 中的新行引用，避免仍指向旧对象导致列表与详情不一致。
     */
    resyncDrawerAfterRecordsReload () {
      if (!this.drawerRecord) return
      const recordId = this.drawerRecord.id
      const fresh = this.records.find(r => r.id === recordId)
      if (!fresh) {
        this.revokeAttachmentPreviewUrls()
        this.drawerRecord = null
        this.drawerEditValues = {}
        return
      }
      this.drawerRecord = fresh
      this.syncDrawerEditValuesFromRow(fresh)
    },
    async openRecord (row) {
      this.drawerRecord = row
      this.drawerEditValues = {}
      this.syncDrawerEditValuesFromRow(row)
      this.drawerTab = 'fields'
      this.comments = []
      this.attachments = []
      this.historyItems = []
      this.newComment = ''
      await this.loadAttachments()
      this.loadProjectMembersForDrawer()
    },
    closeDrawer () {
      this.closeImagePreview()
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
    isStatusTagColumn (col) {
      const n = String(col && col.name ? col.name : '').trim()
      return n === '重要程度' || n === '当前状态' || n === '解决情况' || n === '验收结果'
    },
    getStatusTagClass (col, rawValue) {
      const name = String(col && col.name ? col.name : '').trim()
      const v = String(rawValue == null ? '' : rawValue).trim()
      const levelMap = {
        严重紧急: 'status-tag-red',
        紧急重要: 'status-tag-red',
        高: 'status-tag-red',
        中: 'status-tag-amber',
        低: 'status-tag-slate',
        下一阶段待办: 'status-tag-amber',
        待规划: 'status-tag-slate'
      }
      const statusMap = {
        已创建: 'status-tag-blue',
        待处理: 'status-tag-blue',
        待排期: 'status-tag-amber',
        处理中: 'status-tag-violet',
        开发中: 'status-tag-violet',
        测试中: 'status-tag-cyan',
        待验收: 'status-tag-cyan',
        已验收: 'status-tag-green',
        已完成: 'status-tag-green',
        已关闭: 'status-tag-slate',
        已驳回: 'status-tag-orange',
        驳回: 'status-tag-orange'
      }
      const acceptMap = {
        待验收: 'status-tag-cyan',
        未验收: 'status-tag-cyan',
        验收中: 'status-tag-blue',
        已验收: 'status-tag-green',
        驳回: 'status-tag-orange',
        '驳回，需要重新': 'status-tag-orange'
      }
      let cls = ''
      if (name === '重要程度') cls = levelMap[v] || ''
      if (name === '当前状态' || name === '解决情况') cls = statusMap[v] || ''
      if (name === '验收结果') cls = acceptMap[v] || ''
      return cls || 'status-tag-slate'
    },
    statusUpdateKey (recordId, colId) {
      return `${recordId}:${colId}`
    },
    isStatusUpdating (recordId, colId) {
      return !!this.statusUpdatingMap[this.statusUpdateKey(recordId, colId)]
    },
    statusPickerKey (recordId, colId) {
      return `${recordId}:${colId}`
    },
    isStatusPickerOpen (recordId, colId) {
      return this.statusPickerOpenKey === this.statusPickerKey(recordId, colId)
    },
    toggleStatusPicker (recordId, colId, evt) {
      const key = this.statusPickerKey(recordId, colId)
      const opening = this.statusPickerOpenKey !== key
      this.statusPickerOpenKey = opening ? key : ''
      if (opening && evt && evt.currentTarget && typeof evt.currentTarget.getBoundingClientRect === 'function') {
        const r = evt.currentTarget.getBoundingClientRect()
        this.statusPickerAnchor = {
          top: r.bottom + 6,
          left: r.left,
          width: r.width
        }
      } else {
        this.statusPickerAnchor = null
      }
      if (this.statusPickerOpenKey) {
        this.attachStatusPickerOutside()
        this.attachStatusPickerScrollClose()
      } else {
        this.detachStatusPickerOutside()
        this.detachStatusPickerScrollClose()
      }
    },
    closeStatusPicker () {
      this.statusPickerOpenKey = ''
      this.statusPickerAnchor = null
      this.detachStatusPickerOutside()
      this.detachStatusPickerScrollClose()
    },
    attachStatusPickerOutside () {
      if (this._statusPickerOutsideHandler) return
      this._statusPickerOutsideHandler = () => {
        this.closeStatusPicker()
      }
      document.addEventListener('click', this._statusPickerOutsideHandler)
    },
    detachStatusPickerOutside () {
      if (!this._statusPickerOutsideHandler) return
      document.removeEventListener('click', this._statusPickerOutsideHandler)
      this._statusPickerOutsideHandler = null
    },
    attachStatusPickerScrollClose () {
      if (this._statusPickerScrollHandler) return
      this._statusPickerScrollHandler = () => {
        this.closeStatusPicker()
      }
      window.addEventListener('scroll', this._statusPickerScrollHandler, true)
      window.addEventListener('resize', this._statusPickerScrollHandler)
    },
    detachStatusPickerScrollClose () {
      if (!this._statusPickerScrollHandler) return
      window.removeEventListener('scroll', this._statusPickerScrollHandler, true)
      window.removeEventListener('resize', this._statusPickerScrollHandler)
      this._statusPickerScrollHandler = null
    },
    getStatusOptions (col) {
      return this.getSelectOptions(col) || []
    },
    async onInlineStatusChange (row, col, nextValue) {
      if (!row || !col) return
      const fieldKey = 'c' + col.id
      const prev = row[fieldKey] == null ? '' : String(row[fieldKey])
      const next = nextValue == null ? '' : String(nextValue)
      if (prev === next) return
      const key = this.statusUpdateKey(row.id, col.id)
      this.$set(this.statusUpdatingMap, key, true)
      this.$set(row, fieldKey, next || null)
      try {
        await this.$http.put(`/collab/records/${row.id}`, { fields: { [fieldKey]: next || null } })
      } catch (e) {
        this.$set(row, fieldKey, prev || null)
      } finally {
        this.$delete(this.statusUpdatingMap, key)
      }
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
    buildCreatorFieldLabel () {
      const u = this.currentUser
      if (!u) return ''
      const base = String(u.name || u.email || '').trim()
      const role = String(u.role || '').trim()
      if (base && role) return base + '（' + role + '）'
      return base || role || ''
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
        await this.loadRecords()
        this.resyncDrawerAfterRecordsReload()
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
        if (this.drawerRecord && this.drawerRecord.id === recordId) {
          this.revokeAttachmentPreviewUrls()
          this.drawerRecord = null
          this.drawerEditValues = {}
        }
        await this.loadRecords()
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
    async loadHistory () {
      if (!this.drawerRecord) return
      this.historyLoading = true
      try {
        const resp = await this.$http.get(`/collab/records/${this.drawerRecord.id}/history`, {
          params: { page: 1, pageSize: 100 }
        })
        if (resp.data && resp.data.code === 0 && resp.data.data) {
          this.historyItems = Array.isArray(resp.data.data.items) ? resp.data.data.items : []
        } else {
          this.historyItems = []
        }
      } catch (e) {
        this.historyItems = []
      } finally {
        this.historyLoading = false
      }
    },
    formatHistoryAction (action) {
      if (action === 'create') return this.$t('collabTable.historyActionCreate')
      if (action === 'delete') return this.$t('collabTable.historyActionDelete')
      return this.$t('collabTable.historyActionUpdate')
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
    openImagePreview (att, objectUrl) {
      if (!att || !objectUrl) return
      this.imagePreviewAttachment = att
      this.imagePreviewUrl = objectUrl
      this.imagePreviewTitle = att.fileName || ''
      this.imagePreviewOpen = true
      document.body.style.overflow = 'hidden'
      this.attachImagePreviewEscape()
    },
    closeImagePreview () {
      this.imagePreviewOpen = false
      this.imagePreviewAttachment = null
      this.imagePreviewUrl = ''
      this.imagePreviewTitle = ''
      document.body.style.overflow = ''
      this.detachImagePreviewEscape()
    },
    attachImagePreviewEscape () {
      this.detachImagePreviewEscape()
      this.imagePreviewEscapeHandler = (e) => {
        if (e.key === 'Escape') this.closeImagePreview()
      }
      document.addEventListener('keydown', this.imagePreviewEscapeHandler)
    },
    detachImagePreviewEscape () {
      if (this.imagePreviewEscapeHandler) {
        document.removeEventListener('keydown', this.imagePreviewEscapeHandler)
        this.imagePreviewEscapeHandler = null
      }
    },
    downloadAttachment (a) {
      if (!a || a.id == null) return
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
            this.$set(this.newRecordFields, 'c' + col.id, this.buildCreatorFieldLabel())
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
        await this.loadRecords()
        this.resyncDrawerAfterRecordsReload()
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
  align-items: flex-start;
  gap: 16px;
  margin-bottom: 16px;
}

.header-left {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  flex: 1;
  min-width: 0;
}

.title-block {
  flex: 1;
  min-width: 0;
}

.title-row {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
}

.filter-title-button {
  flex-shrink: 0;
}

.active-filter-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 10px;
}

.filter-chip {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  max-width: 100%;
  padding: 4px 6px 4px 10px;
  font-size: 12px;
  color: #334155;
  background: #f1f5f9;
  border: 1px solid #e2e8f0;
  border-radius: 999px;
}

.filter-chip-text {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.filter-chip-remove {
  flex-shrink: 0;
  width: 22px;
  height: 22px;
  padding: 0;
  border: none;
  border-radius: 50%;
  background: #e2e8f0;
  color: #475569;
  font-size: 16px;
  line-height: 1;
  cursor: pointer;
}

.filter-chip-remove:hover {
  background: #fecaca;
  color: #b91c1c;
}

.filter-empty-block {
  padding: 8px 0 16px;
}

.filter-rule-remove-btn {
  padding: 6px 10px;
  font-size: 12px;
  color: #b91c1c;
  background: #fff;
  border: 1px solid #fecaca;
  border-radius: 6px;
  cursor: pointer;
  white-space: nowrap;
}

.filter-rule-remove-btn:hover {
  background: #fef2f2;
}

.back-link {
  font-size: 13px;
  color: #2563eb;
}

.table-title {
  margin: 0;
  font-size: 18px;
  line-height: 1.3;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
  flex-shrink: 0;
}

.ai-magic-button {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 9px 16px;
  font-size: 14px;
  font-weight: 600;
  color: #4338ca;
  background: linear-gradient(145deg, #eef2ff 0%, #e0e7ff 55%, #ddd6fe 100%);
  border: 1px solid #c7d2fe;
  border-radius: 10px;
  cursor: pointer;
  box-shadow: 0 1px 2px rgba(67, 56, 202, 0.06), inset 0 1px 0 rgba(255, 255, 255, 0.65);
  transition: transform 0.15s ease, box-shadow 0.2s ease, filter 0.2s ease;
}

.ai-magic-button:hover {
  filter: brightness(1.02);
  box-shadow: 0 4px 14px rgba(99, 102, 241, 0.18);
  transform: translateY(-1px);
}

.ai-magic-button:active {
  transform: translateY(0);
  box-shadow: 0 1px 3px rgba(67, 56, 202, 0.12);
}

.ai-magic-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 22px;
  height: 22px;
  font-size: 13px;
  line-height: 1;
  color: #6366f1;
  background: rgba(255, 255, 255, 0.75);
  border-radius: 6px;
  border: 1px solid rgba(199, 210, 254, 0.9);
}

.ai-magic-label {
  letter-spacing: 0.02em;
}

.ai-csv-button {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 9px 16px;
  font-size: 14px;
  font-weight: 600;
  color: #0f766e;
  background: linear-gradient(145deg, #ecfdf5 0%, #d1fae5 100%);
  border: 1px solid #5eead4;
  border-radius: 10px;
  cursor: pointer;
  margin-right: 8px;
  transition: transform 0.15s ease, box-shadow 0.2s ease;
}

.ai-csv-button:hover {
  box-shadow: 0 4px 12px rgba(13, 148, 136, 0.15);
  transform: translateY(-1px);
}

.ai-mode-tabs {
  display: flex;
  gap: 8px;
  margin-bottom: 14px;
  border-bottom: 1px solid #e2e8f0;
  padding-bottom: 8px;
}

.ai-mode-tabs button {
  padding: 8px 14px;
  border: none;
  background: #f1f5f9;
  color: #475569;
  border-radius: 8px;
  cursor: pointer;
  font-size: 14px;
  font-weight: 600;
}

.ai-mode-tabs button.active {
  background: #2563eb;
  color: #fff;
}

.ai-csv-section {
  margin-bottom: 16px;
}

.csv-import-mode-tabs {
  display: flex;
  gap: 8px;
  margin-bottom: 10px;
}

.csv-import-mode-tabs button {
  padding: 6px 12px;
  border: 1px solid #cbd5e1;
  border-radius: 8px;
  background: #f8fafc;
  color: #334155;
  cursor: pointer;
  font-size: 13px;
}

.csv-import-mode-tabs button.active {
  border-color: #2563eb;
  background: #dbeafe;
  color: #1d4ed8;
}

.ai-csv-section .hint-text {
  font-size: 13px;
  color: #64748b;
  line-height: 1.5;
  margin: 0 0 10px;
}

.quick-csv-mapping-panel {
  margin-top: 14px;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  padding: 12px;
  background: #f8fafc;
}

.quick-csv-title {
  margin: 0 0 10px;
  font-size: 14px;
}

.quick-csv-sample-box {
  margin-top: 6px;
  max-height: 130px;
  overflow: auto;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
  background: #fff;
}

.quick-csv-sample-box pre {
  margin: 0;
  padding: 8px;
  font-size: 12px;
  line-height: 1.4;
  white-space: pre-wrap;
  word-break: break-word;
}

.quick-map-list {
  margin: 10px 0 12px;
}

.quick-map-row {
  display: grid;
  grid-template-columns: 140px 1fr;
  gap: 10px;
  align-items: start;
  margin-bottom: 10px;
}

.quick-map-target {
  font-size: 13px;
  color: #334155;
  line-height: 30px;
}

.quick-map-source {
  display: grid;
  grid-template-columns: 1fr 110px;
  gap: 8px;
}

.quick-multi-select {
  min-height: 80px;
}

.quick-join-input {
  min-height: 32px;
}

.csv-file-name {
  font-size: 13px;
  color: #334155;
  margin-left: 8px;
}

.csv-progress-text {
  font-size: 13px;
  color: #475569;
  margin: 8px 0 0;
}

.csv-warn-list {
  margin: 10px 0 0;
  padding-left: 1.2rem;
  font-size: 13px;
  color: #b45309;
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
  overflow-y: visible;
  background: #fff;
  border-radius: 8px;
  border: 1px solid #e5e7eb;
  width: 100%;
}

.dashboard-wrapper {
  background: #fff;
  border-radius: 8px;
  border: 1px solid #e5e7eb;
  padding: 14px;
}

.dashboard-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.dashboard-card {
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  padding: 10px;
  background: #ffffff;
  min-height: 280px;
  display: flex;
  flex-direction: column;
}

.dashboard-card canvas {
  width: 100% !important;
  height: 240px !important;
}

.dashboard-card-title {
  font-size: 14px;
  font-weight: 600;
  color: #1f2937;
  margin-bottom: 8px;
}

.dashboard-card-placeholder {
  justify-content: center;
  align-items: center;
}

.dashboard-placeholder {
  color: #64748b;
  font-size: 13px;
}

.batch-toolbar {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px 14px;
  padding: 10px 14px;
  background: #eff6ff;
  border-bottom: 1px solid #bfdbfe;
}

.batch-toolbar-text {
  font-size: 14px;
  font-weight: 600;
  color: #1e40af;
  margin-right: auto;
}

.data-table {
  width: 100%;
  min-width: 940px;
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
.data-table th.col-check,
.data-table td.col-check {
  width: 44px;
  text-align: center;
  vertical-align: middle;
}

.row-check-input {
  width: 16px;
  height: 16px;
  cursor: pointer;
  accent-color: #2563eb;
}

.data-table th.col-serial,
.data-table td.col-serial { width: 60px; text-align: center; font-variant-numeric: tabular-nums; }
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

.data-table td.cell-status {
  overflow: visible;
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

.status-tag {
  display: inline-flex;
  align-items: center;
  max-width: 100%;
  padding: 2px 10px;
  border-radius: 999px;
  font-size: 12px;
  line-height: 18px;
  font-weight: 500;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.status-tag-blue { color: #0c4a6e; background: #dbeafe; }
.status-tag-cyan { color: #155e75; background: #cffafe; }
.status-tag-green { color: #166534; background: #dcfce7; }
.status-tag-violet { color: #5b21b6; background: #ede9fe; }
.status-tag-amber { color: #92400e; background: #fef3c7; }
.status-tag-orange { color: #9a3412; background: #ffedd5; }
.status-tag-red { color: #991b1b; background: #fee2e2; }
.status-tag-slate { color: #334155; background: #e2e8f0; }

.status-inline-editor {
  display: inline-flex;
  position: relative;
  max-width: 100%;
  z-index: 30;
}

.status-select {
  display: inline-flex;
  align-items: center;
  max-width: 100%;
  min-width: 108px;
  padding: 2px 28px 2px 10px;
  border: 1px solid transparent;
  border-radius: 999px;
  font-size: 12px;
  line-height: 18px;
  font-weight: 500;
  cursor: pointer;
  appearance: none;
  -webkit-appearance: none;
  -moz-appearance: none;
  background-image:
    linear-gradient(45deg, transparent 50%, currentColor 50%),
    linear-gradient(135deg, currentColor 50%, transparent 50%);
  background-position:
    calc(100% - 16px) calc(50% - 1px),
    calc(100% - 11px) calc(50% - 1px);
  background-size: 5px 5px, 5px 5px;
  background-repeat: no-repeat;
}

.status-select:focus {
  outline: none;
  border-color: #60a5fa;
  box-shadow: 0 0 0 2px rgba(96, 165, 250, 0.25);
}

.status-select:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}

.status-picker-trigger {
  justify-content: flex-start;
}

.status-picker-text {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.status-picker-menu {
  /* 具体位置与 max-height 由 statusPickerMenuStyle（fixed）覆盖，避免被 .table-wrapper overflow 裁剪 */
  box-sizing: border-box;
  background: #fff;
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  box-shadow: 0 10px 24px rgba(15, 23, 42, 0.16);
  padding: 8px;
  -webkit-overflow-scrolling: touch;
}

.status-picker-item {
  display: block;
  width: 100%;
  text-align: left;
  border: 1px solid transparent;
  background: #fff;
  color: #334155;
  border-radius: 8px;
  padding: 8px 12px;
  margin-bottom: 4px;
  cursor: pointer;
  font-size: 13px;
  line-height: 1.45;
  white-space: normal;
  word-break: break-word;
}

.status-picker-item:last-child {
  margin-bottom: 0;
}

.status-picker-item.active {
  border-color: #93c5fd;
  box-shadow: inset 0 0 0 1px rgba(37, 99, 235, 0.2);
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

.filter-rule-row {
  display: flex;
  flex-direction: column;
  align-items: stretch;
  gap: 10px;
  margin-bottom: 14px;
  padding: 12px;
  background: #f9fafb;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  box-sizing: border-box;
}

.filter-rule-col {
  flex: none;
  min-width: 0;
  width: 100%;
}

.filter-rule-value {
  min-width: 0;
}

.filter-rule-actions {
  flex-shrink: 0;
  align-self: flex-end;
  padding-bottom: 0;
}

.filter-actions {
  margin-top: 16px;
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

.danger-button.small {
  padding: 4px 12px;
  font-size: 13px;
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

.history-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.history-item {
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  padding: 10px 12px;
  background: #fff;
}

.history-topline {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 6px;
}

.history-action {
  font-size: 12px;
  color: #0f766e;
  background: #ecfeff;
  border: 1px solid #a5f3fc;
  border-radius: 999px;
  padding: 1px 8px;
}

.history-time {
  font-size: 12px;
  color: #64748b;
}

.history-meta {
  font-size: 12px;
  color: #334155;
  margin-bottom: 6px;
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}

.history-role {
  color: #4f46e5;
}

.history-field {
  margin-bottom: 6px;
  font-size: 13px;
}

.history-values {
  font-size: 12px;
  display: grid;
  grid-template-columns: 1fr;
  gap: 4px;
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

/* 图片在线预览 */
.image-preview-backdrop {
  position: fixed;
  inset: 0;
  z-index: 3000;
  background: rgba(15, 23, 42, 0.92);
  display: flex;
  align-items: stretch;
  justify-content: stretch;
}
.image-preview-modal {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-height: 0;
  background: #0f172a;
}
.image-preview-toolbar {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 10px 14px;
  background: #f8fafc;
  border-bottom: 1px solid #e2e8f0;
}
.image-preview-toolbar-left,
.image-preview-toolbar-right {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
}
.image-tool-btn {
  min-width: 36px;
  height: 36px;
  padding: 0 8px;
  border: 1px solid #cbd5e1;
  border-radius: 6px;
  background: #fff;
  color: #334155;
  font-size: 16px;
  line-height: 1;
  cursor: pointer;
  transition: background 0.15s, border-color 0.15s;
}
.image-tool-btn:hover:not(:disabled) {
  background: #f1f5f9;
  border-color: #94a3b8;
}
.image-tool-btn:disabled {
  opacity: 0.45;
  cursor: not-allowed;
}
.image-tool-wide {
  min-width: 44px;
  font-size: 12px;
  font-weight: 600;
}
.image-tool-download {
  color: #1d4ed8;
  border-color: #93c5fd;
}
.image-tool-download:hover:not(:disabled) {
  background: #eff6ff;
}
.image-tool-close {
  font-size: 22px;
  font-weight: 300;
  line-height: 0.9;
}
.image-preview-body {
  flex: 1;
  min-height: 0;
  overflow: auto;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
  -webkit-overflow-scrolling: touch;
}
.image-preview-main {
  max-width: 100%;
  max-height: min(85vh, 900px);
  width: auto;
  height: auto;
  object-fit: contain;
  border-radius: 4px;
  box-shadow: 0 8px 40px rgba(0, 0, 0, 0.35);
}
.image-preview-footer {
  flex-shrink: 0;
  padding: 10px 16px 14px;
  text-align: center;
  font-size: 13px;
  color: #cbd5e1;
  background: #0f172a;
  border-top: 1px solid #1e293b;
  word-break: break-all;
}
.list-thumb-img-clickable,
.thumb-img-clickable,
.attachment-preview-img-clickable {
  cursor: zoom-in;
}
</style>
