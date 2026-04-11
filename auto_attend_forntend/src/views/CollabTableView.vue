<template>
  <div class="collab-table-page collab-table-layout">
    <aside v-if="projectId" class="collab-sidebar" :class="{ collapsed: sidebarCollapsed }" aria-label="多维表切换">
      <button
        type="button"
        class="collab-sidebar-collapse"
        :title="sidebarCollapsed ? $t('collabTable.sidebarExpand') : $t('collabTable.sidebarCollapse')"
        @click="sidebarCollapsed = !sidebarCollapsed"
      >{{ sidebarCollapsed ? '▶' : '◀' }}</button>

      <nav class="collab-sidebar-nav">
        <router-link
          class="collab-sidebar-link collab-sidebar-link-btn"
          :class="{ active: showHomeDashboard }"
          :to="{ name: 'collab-table', params: { projectId: String(projectId) }, query: {} }"
        >
          <span v-if="!sidebarCollapsed">{{ $t('collabTable.sidebarHomeBoard') }}</span>
          <span v-else>B</span>
        </router-link>
        <router-link
          class="collab-sidebar-link"
          :class="{ active: !showHomeDashboard && tablePurpose === 'issue_tracking' }"
          :to="{ name: 'collab-table', params: { projectId: String(projectId) }, query: { purpose: 'issue_tracking' } }"
        >
          <span v-if="!sidebarCollapsed">{{ $t('collabTable.sidebarIssue') }}</span>
          <span v-else>I</span>
        </router-link>
        <router-link
          class="collab-sidebar-link"
          :class="{ active: !showHomeDashboard && tablePurpose === 'feature_backlog' }"
          :to="{ name: 'collab-table', params: { projectId: String(projectId) }, query: { purpose: 'feature_backlog' } }"
        >
          <span v-if="!sidebarCollapsed">{{ $t('collabTable.sidebarFeature') }}</span>
          <span v-else>F</span>
        </router-link>
      </nav>

      <p v-if="!sidebarCollapsed && !showHomeDashboard" class="collab-sidebar-hint">
        {{ tablePurpose === 'feature_backlog' ? $t('collabTable.sidebarHintFeature') : $t('collabTable.sidebarHintIssue') }}
      </p>
    </aside>
    <div class="collab-table-main">
    <div class="table-header">
      <div class="header-left">
        <router-link to="/collab/projects" class="back-link">{{ $t('collabTable.backToList') }}</router-link>
        <div class="title-block">
          <div class="title-row">
            <h2 class="table-title">{{ pageTableTitle }}</h2>
            <button
              v-if="!showHomeDashboard"
              type="button"
              class="secondary-button filter-title-button"
              @click="openFilterModal"
              :title="$t('collabTable.filterHint')"
            >
              {{ $t('collabTable.filter') }}<span v-if="activeFilters && activeFilters.length">({{ activeFilters.length }})</span>
            </button>
          </div>
          <div v-if="!showHomeDashboard && activeFilters && activeFilters.length" class="active-filter-chips">
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
      <div class="header-actions" v-if="!showHomeDashboard">
        <button
          v-if="tablePurpose === 'issue_tracking'"
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

    <div v-if="showHomeDashboard" class="collab-home-dashboard">
      <div class="portal-bar">
        <div class="portal-items">
          <a
            v-for="it in portalLinks"
            :key="it.id"
            class="portal-link"
            :href="it.url"
            target="_blank"
            rel="noopener noreferrer"
            :title="it.url"
          >
            <span class="portal-link-icon" aria-hidden="true">🔗</span>
            <span class="portal-link-label">{{ it.label }}</span>
          </a>
          <span v-if="!portalLinks.length" class="text-muted small">暂无传送门链接</span>
        </div>
        <button type="button" class="secondary-button small" @click="openPortalModal">配置传送门</button>
      </div>
      <div class="mail-notify-panel">
        <div class="mail-notify-head">
          <div class="mail-notify-title">邮件通知</div>
          <button type="button" class="secondary-button small" @click="toggleMailNotifyOpen">
            {{ mailNotifyOpen ? '收起' : '配置' }}
          </button>
        </div>
        <div v-if="mailNotifyOpen" class="mail-notify-body">
          <div v-if="mailNotifyLoading" class="text-muted small">加载中…</div>
          <template v-else>
            <div v-if="mailNotifyConfig && mailNotifyConfig.mailConfigured === false" class="text-muted small">
              当前未配置 SMTP 发信能力，请先到「API 配置与能力测试 → 邮件（SMTP）配置」完成配置。
            </div>
            <div class="form-row">
              <label class="form-label">绑定仓库（owner/repo）</label>
              <input v-model="mailNotifyForm.repoFullName" type="text" class="form-input" placeholder="owner/repo">
            </div>
            <div class="form-row">
              <label class="checkbox-label">
                <input type="checkbox" v-model="mailNotifyForm.enabled">
                <span>启用本项目日报邮件</span>
              </label>
            </div>
            <div class="form-row">
              <label class="checkbox-label">
                <input type="checkbox" v-model="mailNotifyForm.sendToManagers">
                <span>向项目负责人发送项目日报</span>
              </label>
            </div>
            <div class="form-row">
              <label class="checkbox-label">
                <input type="checkbox" v-model="mailNotifyForm.sendToDevelopers">
                <span>向开发者发送个人日报</span>
              </label>
            </div>
            <div class="form-row">
              <label class="form-label">项目负责人邮箱（逗号/换行分隔）</label>
              <textarea v-model="mailNotifyForm.managerEmailsText" rows="3" class="form-input" placeholder="pm@example.com"></textarea>
            </div>
            <div class="form-actions">
              <button type="button" class="primary-button" :disabled="mailNotifySaving" @click="saveMailNotifyConfig">
                {{ mailNotifySaving ? '保存中…' : '保存' }}
              </button>
              <button type="button" class="secondary-button" :disabled="mailNotifySendingTest" @click="sendMailNotifyTest">
                {{ mailNotifySendingTest ? '发送中…' : '发送测试邮件' }}
              </button>
              <span v-if="mailNotifyMessage" class="save-message" :class="mailNotifyMessageOk ? 'success' : 'error'">{{ mailNotifyMessage }}</span>
            </div>
          </template>
        </div>
      </div>
      <div class="mail-notify-panel">
        <div class="mail-notify-head">
          <div class="mail-notify-title">提交联动任务状态更新（AI）</div>
          <button type="button" class="secondary-button small" @click="toggleAiLinkageOpen">
            {{ aiLinkageOpen ? '收起' : '配置' }}
          </button>
        </div>
        <div v-if="aiLinkageOpen" class="mail-notify-body">
          <div v-if="aiLinkageLoading" class="text-muted small">加载中…</div>
          <template v-else>
            <div class="form-row">
              <label class="checkbox-label">
                <input type="checkbox" v-model="aiLinkageForm.enabled">
                <span>启用提交联动任务状态更新</span>
              </label>
            </div>
            <div class="form-row">
              <label class="form-label">联动模式</label>
              <select v-model="aiLinkageForm.mode" class="form-input">
                <option value="auto">自动更新任务状态</option>
                <option value="confirm">仅输出建议（待人工确认）</option>
                <option value="suggest_only">仅分析不更新</option>
              </select>
            </div>
            <div class="form-row">
              <label class="form-label">最小置信度</label>
              <select v-model="aiLinkageForm.minConfidence" class="form-input">
                <option value="high">高</option>
                <option value="medium">中</option>
                <option value="low">低</option>
              </select>
            </div>
            <div v-if="aiLinkageForm.enabled" class="text-muted small" style="color:#b91c1c;">
              AI的分析可能不准确，请仔细辨别。
            </div>
            <div class="form-actions">
              <button type="button" class="primary-button" :disabled="aiLinkageSaving" @click="saveAiLinkageConfig">
                {{ aiLinkageSaving ? '保存中…' : '保存' }}
              </button>
              <span v-if="aiLinkageMessage" class="save-message" :class="aiLinkageMessageOk ? 'success' : 'error'">{{ aiLinkageMessage }}</span>
            </div>
          </template>
        </div>
      </div>
      <div class="mail-notify-panel client-board-panel">
        <div class="mail-notify-head">
          <div class="mail-notify-title">客户项目阅览看板</div>
          <button type="button" class="secondary-button small" @click="toggleClientBoardOpen">
            {{ clientBoardOpen ? '收起' : '配置' }}
          </button>
        </div>
        <div v-if="clientBoardOpen" class="mail-notify-body">
          <div v-if="clientBoardLoading" class="text-muted small">加载中…</div>
          <template v-else>
            <p class="text-muted small" style="margin-bottom:10px">
              开启后生成独立网页链接，客户无需登录即可查看（内容由下方选项控制）。AI 录入使用企业「API 配置」中的通义 Key，费用由配置方承担。
            </p>
            <div class="form-row">
              <label class="checkbox-label">
                <input type="checkbox" v-model="clientBoardForm.enabled">
                <span>启用客户阅览看板</span>
              </label>
            </div>
            <div class="form-row">
              <label class="checkbox-label">
                <input type="checkbox" v-model="clientBoardForm.showProgressDashboard">
                <span>向客户展示功能进度仪表盘</span>
              </label>
            </div>
            <div class="form-row">
              <label class="checkbox-label">
                <input type="checkbox" v-model="clientBoardForm.showFeatureBacklog">
                <span>向客户展示待开发功能摘要</span>
              </label>
            </div>
            <div class="form-row">
              <label class="checkbox-label">
                <input type="checkbox" v-model="clientBoardForm.showAiTableEntry">
                <span>开放多维表 AI 录入（项目调整表）</span>
              </label>
            </div>
            <div class="form-row">
              <label class="form-label">AI 目标表</label>
              <select v-model="clientBoardForm.aiPurpose" class="form-input">
                <option value="issue_tracking">项目调整（issue_tracking）</option>
              </select>
            </div>
            <div v-if="clientBoardToken" class="form-row client-board-url-block">
              <label class="form-label">客户访问链接</label>
              <div class="client-board-url-row">
                <input
                  type="text"
                  readonly
                  class="form-input client-board-url-input"
                  :value="clientBoardPublicUrl"
                  @click="$event.target.select()"
                >
                <button type="button" class="secondary-button small" @click="copyClientBoardLink">
                  {{ clientBoardCopied === 'link' ? '已复制' : '复制链接' }}
                </button>
                <button type="button" class="primary-button small" @click="openClientBoardInNewTab">
                  打开阅览看板
                </button>
              </div>
              <p class="text-muted small client-board-url-hint">
                若一键复制无效，可点击输入框全选后 Ctrl+C。内网 HTTP 下浏览器常限制剪贴板 API；对外分享请在构建时配置环境变量 <code>VUE_APP_PUBLIC_ORIGIN</code>（例如 https://your-domain.com）。
              </p>
            </div>
            <div v-if="clientBoardToken" class="form-row client-board-token-row">
              <span class="text-muted small">浏览 ID：</span>
              <code class="client-board-code">{{ clientBoardToken }}</code>
              <button type="button" class="secondary-button small" @click="copyClientBoardId">
                {{ clientBoardCopied === 'id' ? '已复制' : '复制 ID' }}
              </button>
            </div>
            <div class="form-actions">
              <button type="button" class="primary-button" :disabled="clientBoardSaving" @click="saveClientBoard">
                {{ clientBoardSaving ? '保存中…' : '保存配置' }}
              </button>
              <button type="button" class="secondary-button" :disabled="clientBoardSaving" @click="regenerateClientBoardToken">
                重新生成令牌
              </button>
              <span v-if="clientBoardMessage" class="save-message" :class="clientBoardMessageOk ? 'success' : 'error'">{{ clientBoardMessage }}</span>
            </div>
          </template>
        </div>
      </div>
      <DashboardView
        :fixedRepoFullName="projectRepoId"
        :collab-data-board-only="true"
        :key="'home-board-' + projectId + '-' + projectRepoId"
      />
    </div>
    <div v-else-if="tableLoading" class="placeholder">{{ $t('collabTable.loadingTable') }}</div>
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
        <div class="dashboard-card dashboard-card-wordcloud">
          <div class="dashboard-card-title">{{ $t('collabTable.dashboardWordCloud') }}</div>
          <div v-if="!dashboardWordCloudItems.length" class="dashboard-empty-hint">{{ $t('collabTable.dashboardWordCloudEmpty') }}</div>
          <div v-else class="word-cloud-body">
            <span
              v-for="(it, idx) in dashboardWordCloudItems"
              :key="'wc-' + idx"
              class="word-cloud-token"
              :style="wordCloudTokenStyle(it)"
            >{{ it.text }}</span>
          </div>
          <p v-if="dashboardWordCloudItems.length" class="dashboard-word-foot">{{ $t('collabTable.dashboardWordCloudFootnote') }}</p>
        </div>
        <div class="dashboard-card">
          <div class="dashboard-card-title">{{ $t('collabTable.dashboardAvgResolveDuration') }}</div>
          <div v-if="!dashboardAvgResolveHasData" class="dashboard-empty-hint">{{ $t('collabTable.dashboardAvgResolveEmpty') }}</div>
          <canvas v-show="dashboardAvgResolveHasData" ref="avgResolveChart"></canvas>
        </div>
      </div>
    </div>
    <div v-else-if="!columns.length" class="placeholder">{{ $t('collabTable.noColumns') }}</div>
    <div v-else class="table-scroll-outer">
    <div class="table-wrapper">
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
          <tr
            v-for="(row, idx) in records"
            :key="row.id"
            class="data-table-row"
            :class="{ 'status-picker-row-elevated': statusPickerRecordId != null && Number(statusPickerRecordId) === Number(row.id) }"
            @click="openRecord(row)"
          >
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
              <button class="link-button" @click.stop="openRecord(row, 'history')">{{ $t('collabTable.history') }}</button>
              <button class="link-button danger" @click.stop="confirmDeleteRow(row)" :title="$t('collabTable.deleteRecord')">{{ $t('collabTable.delete') }}</button>
            </td>
          </tr>
        </tbody>
      </table>
      <div v-if="!records.length && !recordsLoading" class="empty-tip">{{ $t('collabTable.noRecords') }}</div>
      <div v-if="recordsLoading" class="loading-tip">{{ $t('collabTable.loadingRecords') }}</div>
    </div>
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
                  <input
                    ref="drawerFileInput"
                    type="file"
                    accept="image/*,video/*,.pdf,.doc,.docx,.xls,.xlsx,.ppt,.pptx,.txt"
                    style="display:none"
                    @change="onDrawerFileSelected"
                  >
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
              <input
                ref="fileInput"
                type="file"
                multiple
                accept="image/*,video/*,.pdf,.doc,.docx,.xls,.xlsx,.ppt,.pptx,.txt"
                @change="onFileSelected"
                style="display:none"
              >
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
                    <button type="button" class="link-button danger" @click="deleteDrawerAttachment(a)">{{ $t('collabTable.delete') }}</button>
                  </div>
                </template>
                <template v-else>
                  <button class="link-button" @click="downloadAttachment(a)">{{ a.fileName }}</button>
                  <span class="file-size">({{ formatSize(a.fileSize) }})</span>
                  <button type="button" class="link-button danger" @click="deleteDrawerAttachment(a)">{{ $t('collabTable.delete') }}</button>
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
          <div v-show="aiModalMode === 'text'" class="ai-split-mode-tabs" role="tablist">
            <button
              type="button"
              role="tab"
              :aria-selected="aiSplitMode === 'single'"
              :class="{ active: aiSplitMode === 'single' }"
              @click="aiSplitMode = 'single'"
            >
              {{ $t('collabTable.aiSplitSingle') }}
            </button>
            <button
              type="button"
              role="tab"
              :aria-selected="aiSplitMode === 'multi'"
              :class="{ active: aiSplitMode === 'multi' }"
              @click="aiSplitMode = 'multi'"
            >
              {{ $t('collabTable.aiSplitMulti') }}
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
              @paste="onAiPaste"
              rows="4"
              class="field-input"
              :placeholder="$t('collabTable.aiInputPlaceholder')"
            ></textarea>
          </div>
          <div v-show="aiModalMode === 'text'" class="ai-attachments-section">
            <label class="field-label">{{ $t('collabTable.aiInputAttachments') }}</label>
            <div class="ai-upload-row">
              <input
                ref="aiFileInput"
                type="file"
                multiple
                accept="image/*,video/*,.pdf,.doc,.docx,.xls,.xlsx,.ppt,.pptx,.txt"
                style="display:none"
                @change="onAiFilesSelected"
              >
              <button type="button" class="secondary-button small" @click="triggerAiUpload">
                {{ $t('collabTable.uploadAttachment') }}
              </button>
              <span class="text-muted small ai-paste-hint">{{ $t('collabTable.aiPasteHint') || '支持 Ctrl+V 粘贴图片上传' }}</span>
            </div>
            <div v-if="aiSessionAttachments.length" class="ai-attachment-list">
              <div v-for="a in aiSessionAttachments" :key="a.id" class="ai-attachment-item">
                <label class="ai-attachment-main">
                <input
                  type="checkbox"
                  :value="a.id"
                  v-model="aiSelectedAttachmentIds"
                >
                  <div class="ai-attachment-preview" :title="a.fileName">
                  <img v-if="a.isImage && a.previewUrl" :src="a.previewUrl" class="ai-thumb-img" alt="">
                    <span v-else class="ai-file-icon">{{ getAiAttachmentIcon(a.fileName) }}</span>
                </div>
                <div class="ai-attachment-meta">
                  <span class="ai-file-name">{{ a.fileName }}</span>
                  <span class="ai-file-size">{{ formatSize(a.fileSize) }}</span>
                </div>
              </label>
                <button
                  type="button"
                  class="ai-attachment-remove"
                  :title="$t('collabTable.removeAttachment') || '删除附件'"
                  @click.stop="removeAiAttachment(a)"
                >×</button>
              </div>
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

    <!-- 传送门配置弹窗 -->
    <div v-if="portalModalOpen" class="drawer-mask" @click="closePortalModal">
      <div class="drawer drawer-wide" @click.stop>
        <div class="drawer-header">
          <h3>传送门</h3>
          <button class="close-btn" @click="closePortalModal">×</button>
        </div>
        <div class="drawer-body">
          <div class="portal-modal-actions">
            <button type="button" class="primary-button small" @click="addPortalLink">添加链接</button>
            <div style="flex:1"></div>
            <select v-model.number="portalImportFromProjectId" class="field-input" style="max-width: 260px;">
              <option :value="0">从其他项目导入…</option>
              <option v-for="p in portalImportProjectOptions" :key="'pi-' + p.id" :value="p.id">
                {{ p.name || ('项目#' + p.id) }}
              </option>
            </select>
            <button type="button" class="secondary-button small" :disabled="portalImporting || portalImportFromProjectId<=0" @click="importPortalLinks">
              {{ portalImporting ? '导入中…' : '导入并覆盖' }}
            </button>
          </div>
          <div v-if="portalLoading" class="text-muted">加载中…</div>
          <div v-else class="portal-edit-list">
            <div v-for="(it, idx) in portalEditItems" :key="it._key" class="portal-edit-row">
              <input v-model="it.label" class="field-input" placeholder="展示文本（如：百度一下）">
              <input v-model="it.url" class="field-input" placeholder="https://example.com">
              <input v-model.number="it.sortOrder" type="number" class="field-input" style="max-width: 90px;" placeholder="排序">
              <button type="button" class="link-button danger" @click="removePortalLink(idx)">删除</button>
            </div>
            <div v-if="!portalEditItems.length" class="text-muted small">暂无链接，可点击上方「添加链接」</div>
          </div>
          <div class="drawer-actions">
            <button type="button" class="primary-button" :disabled="portalSaving" @click="savePortalLinks">
              {{ portalSaving ? '保存中…' : '保存' }}
            </button>
            <span v-if="portalMessage" class="save-message" :class="portalMessageOk ? 'success' : 'error'">{{ portalMessage }}</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { Chart as ChartJS, registerables } from 'chart.js'
import { compressImageFile, IMAGE_COMPRESS_PRESETS, shouldCompressAsRasterImage } from '@/utils/imageCompress'
import { buildWordCloudItems } from '@/utils/collabDashboardText'
import DashboardView from './DashboardView.vue'

ChartJS.register(...registerables)

export default {
  name: 'CollabTableView',
  components: {
    DashboardView
  },
  data () {
    return {
      projectId: null,
      projectName: '',
      projectRepoId: '',
      showHomeDashboard: false,
      sidebarCollapsed: false,
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
      // aiSplitMode: 'single' | 'multi'（单条任务录入 / 智能拆分多条）
      aiSplitMode: 'multi',
      imagePreviewOpen: false,
      imagePreviewUrl: '',
      imagePreviewTitle: '',
      imagePreviewAttachment: null,
      imagePreviewEscapeHandler: null,
      statusUpdatingMap: {},
      statusPickerOpenKey: '',
      /** 打开下拉的行 id，用于提升整行层叠顺序，避免被下方行的状态标签盖住 */
      statusPickerRecordId: null,
      /** 下拉锚点（viewport），用于 fixed 定位，避免被表格 overflow 裁剪 */
      statusPickerAnchor: null,
      /** 批量选择：recordId -> true */
      rowSelection: {},
      batchDeleting: false,
      showDashboard: false,
      dashboardWeeks: 8,
      dashboardWordCloudItems: [],
      /** 仪表盘：平均解决时长按周序列（供图与 tooltip） */
      avgResolveSeries: null,
      dashboardCharts: {
        taskCompletion: null,
        weeklyCreated: null,
        weeklyResolved: null,
        importance: null,
        avgResolve: null
      },
      /** issue_tracking=项目调整；feature_backlog=待开发功能清单 */
      tablePurpose: 'issue_tracking',

      // ===== 项目级日报邮件通知（开发与数据看板）=====
      mailNotifyOpen: false,
      mailNotifyLoading: false,
      mailNotifySaving: false,
      mailNotifySendingTest: false,
      mailNotifyMessage: '',
      mailNotifyMessageOk: false,
      mailNotifyConfig: null,
      mailNotifyForm: {
        repoFullName: '',
        enabled: false,
        sendToManagers: true,
        sendToDevelopers: true,
        managerEmailsText: ''
      },
      aiLinkageOpen: false,
      aiLinkageLoading: false,
      aiLinkageSaving: false,
      aiLinkageMessage: '',
      aiLinkageMessageOk: false,
      aiLinkageForm: {
        enabled: false,
        mode: 'auto',
        minConfidence: 'medium'
      },

      // ===== 传送门（项目级链接）=====
      portalLinks: [],
      portalModalOpen: false,
      portalLoading: false,
      portalSaving: false,
      portalMessage: '',
      portalMessageOk: false,
      portalEditItems: [],
      portalImportFromProjectId: 0,
      portalImporting: false,
      portalImportProjectOptions: [],

      clientBoardOpen: false,
      clientBoardLoading: false,
      clientBoardSaving: false,
      clientBoardMessage: '',
      clientBoardMessageOk: false,
      clientBoardCopied: '',
      clientBoardToken: '',
      clientBoardForm: {
        enabled: false,
        showProgressDashboard: true,
        showFeatureBacklog: false,
        showAiTableEntry: false,
        aiPurpose: 'issue_tracking'
      }
    }
  },
  watch: {
    '$route.query.purpose' () {
      this.syncPurposeFromRoute()
      this.loadTable()
      this.loadRecords()
      if (this.drawerRecord) {
        this.drawerRecord = null
        this.drawerEditValues = {}
      }
      this.showDashboard = false
    }
  },
  computed: {
    dashboardAvgResolveHasData () {
      const s = this.avgResolveSeries
      if (!s || !Array.isArray(s.avgs)) return false
      return s.avgs.some((v) => Number(v) > 0)
    },
    pageTableTitle () {
      if (this.showHomeDashboard) {
        const pn = (this.projectName || '').trim()
        const board = this.$t('collabTable.sidebarHomeBoard')
        return pn ? `${pn} · ${board}` : board
      }
      const base = (this.tableBaseName || '').trim() || this.$t('collabTable.defaultTableName')
      const pn = (this.projectName || '').trim()
      if (!pn) return base
      return this.$t('collabTable.pageTitleWithProject', { project: pn, table: base })
    },
    /** 客户阅览看板完整 URL；优先 VUE_APP_PUBLIC_ORIGIN（与当前浏览器地址栏可能不同，如反代/公网域名） */
    clientBoardPublicUrl () {
      return this.getClientBoardPublicUrl()
    },
    filterableColumns () {
      const allowed = this.tablePurpose === 'feature_backlog'
        ? ['重要程度', '开发进度']
        : ['重要程度', '当前状态', '验收结果']
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
        zIndex: 20050,
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
    this.syncPurposeFromRoute()
    this.loadProjectSummary()
    this.loadTable()
    this.loadRecords()
    this.loadProjectMembers()
    this.loadMailNotifyConfig()
    this.loadAiLinkageConfig()
    this.loadPortalLinks()
    this.loadClientBoard()
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
    syncPurposeFromRoute () {
      const p = this.$route.query.purpose
      this.showDashboard = false
      if (p === 'feature_backlog' || p === 'issue_tracking') {
        this.showHomeDashboard = false
        this.tablePurpose = p
      } else {
        // 无 purpose：默认「开发与数据看板」（与侧栏第一项一致）
        this.showHomeDashboard = true
        this.tablePurpose = 'issue_tracking'
      }
    },
    toggleMailNotifyOpen () {
      this.mailNotifyOpen = !this.mailNotifyOpen
      if (this.mailNotifyOpen && !this.mailNotifyConfig) this.loadMailNotifyConfig()
    },
    async loadMailNotifyConfig () {
      if (!this.projectId) return
      this.mailNotifyLoading = true
      try {
        const resp = await this.$http.get(`/admin/report/projects/${this.projectId}/config`)
        if (resp.data && resp.data.code === 0 && resp.data.data) {
          const d = resp.data.data
          this.mailNotifyConfig = d
          this.mailNotifyForm.repoFullName = d.repoFullName || ''
          this.mailNotifyForm.enabled = d.enabled === true
          this.mailNotifyForm.sendToManagers = d.sendToManagers !== false
          this.mailNotifyForm.sendToDevelopers = d.sendToDevelopers !== false
          const emails = Array.isArray(d.managerEmails) ? d.managerEmails : []
          this.mailNotifyForm.managerEmailsText = emails.join('\n')
        } else {
          this.mailNotifyConfig = {}
        }
      } catch (e) {
        this.mailNotifyConfig = {}
      } finally {
        this.mailNotifyLoading = false
      }
    },
    async saveMailNotifyConfig () {
      if (!this.projectId) return
      this.mailNotifySaving = true
      this.mailNotifyMessage = ''
      try {
        const raw = this.mailNotifyForm.managerEmailsText || ''
        const list = raw.split(/[\n,\r\t ]+/).map(s => s.trim()).filter(Boolean)
        const payload = {
          repoFullName: this.mailNotifyForm.repoFullName || '',
          enabled: !!this.mailNotifyForm.enabled,
          sendToManagers: !!this.mailNotifyForm.sendToManagers,
          sendToDevelopers: !!this.mailNotifyForm.sendToDevelopers,
          managerEmails: list
        }
        const resp = await this.$http.put(`/admin/report/projects/${this.projectId}/config`, payload)
        if (resp.data && resp.data.code === 0) {
          this.mailNotifyMessageOk = true
          this.mailNotifyMessage = '已保存'
          await this.loadMailNotifyConfig()
        } else {
          this.mailNotifyMessageOk = false
          this.mailNotifyMessage = (resp.data && resp.data.message) || '保存失败'
        }
      } catch (e) {
        this.mailNotifyMessageOk = false
        this.mailNotifyMessage = '保存失败'
      } finally {
        this.mailNotifySaving = false
      }
    },
    async sendMailNotifyTest () {
      if (!this.projectId) return
      this.mailNotifySendingTest = true
      this.mailNotifyMessage = ''
      try {
        const resp = await this.$http.post(`/admin/report/projects/${this.projectId}/send-now`, null, {
          params: { includeDevelopers: false }
        })
        if (resp.data && resp.data.code === 0 && resp.data.data) {
          this.mailNotifyMessageOk = true
          this.mailNotifyMessage = `已发送 ${resp.data.data.sent || 0} 封`
        } else {
          this.mailNotifyMessageOk = false
          this.mailNotifyMessage = (resp.data && resp.data.message) || '发送失败'
        }
      } catch (e) {
        this.mailNotifyMessageOk = false
        this.mailNotifyMessage = '发送失败'
      } finally {
        this.mailNotifySendingTest = false
      }
    },
    toggleAiLinkageOpen () {
      this.aiLinkageOpen = !this.aiLinkageOpen
      if (this.aiLinkageOpen) this.loadAiLinkageConfig()
    },
    async loadAiLinkageConfig () {
      if (!this.projectId) return
      this.aiLinkageLoading = true
      try {
        const resp = await this.$http.get(`/admin/ai-analysis/projects/${this.projectId}/linkage-config`)
        if (resp.data && resp.data.code === 0 && resp.data.data) {
          const d = resp.data.data
          this.aiLinkageForm.enabled = d.enabled === true
          this.aiLinkageForm.mode = d.mode || 'auto'
          this.aiLinkageForm.minConfidence = d.minConfidence || 'medium'
        }
      } catch (e) {
        // ignore
      } finally {
        this.aiLinkageLoading = false
      }
    },
    async saveAiLinkageConfig () {
      if (!this.projectId) return
      this.aiLinkageSaving = true
      this.aiLinkageMessage = ''
      try {
        const payload = {
          enabled: !!this.aiLinkageForm.enabled,
          mode: this.aiLinkageForm.mode || 'auto',
          minConfidence: this.aiLinkageForm.minConfidence || 'medium'
        }
        const resp = await this.$http.put(`/admin/ai-analysis/projects/${this.projectId}/linkage-config`, payload)
        if (resp.data && resp.data.code === 0) {
          this.aiLinkageMessageOk = true
          this.aiLinkageMessage = '已保存'
        } else {
          this.aiLinkageMessageOk = false
          this.aiLinkageMessage = (resp.data && resp.data.message) || '保存失败'
        }
      } catch (e) {
        this.aiLinkageMessageOk = false
        this.aiLinkageMessage = '保存失败'
      } finally {
        this.aiLinkageSaving = false
      }
    },
    toggleClientBoardOpen () {
      this.clientBoardOpen = !this.clientBoardOpen
    },
    async loadClientBoard () {
      if (!this.projectId) return
      this.clientBoardLoading = true
      this.clientBoardMessage = ''
      try {
        const resp = await this.$http.get(`/collab/projects/${this.projectId}/client-board`)
        if (resp.data && resp.data.code === 0 && resp.data.data) {
          const d = resp.data.data
          this.clientBoardForm.enabled = !!d.enabled
          this.clientBoardForm.showProgressDashboard = d.showProgressDashboard !== false
          this.clientBoardForm.showFeatureBacklog = !!d.showFeatureBacklog
          this.clientBoardForm.showAiTableEntry = !!d.showAiTableEntry
          this.clientBoardForm.aiPurpose = d.aiPurpose || 'issue_tracking'
          this.clientBoardToken = d.publicToken || ''
        }
      } catch (e) {
        this.clientBoardMessage = '加载客户阅览看板配置失败（需协作登录且后端已升级）'
        this.clientBoardMessageOk = false
      } finally {
        this.clientBoardLoading = false
      }
    },
    async saveClientBoard () {
      if (!this.projectId) return
      this.clientBoardSaving = true
      this.clientBoardMessage = ''
      try {
        const resp = await this.$http.put(`/collab/projects/${this.projectId}/client-board`, {
          enabled: !!this.clientBoardForm.enabled,
          showProgressDashboard: !!this.clientBoardForm.showProgressDashboard,
          showFeatureBacklog: !!this.clientBoardForm.showFeatureBacklog,
          showAiTableEntry: !!this.clientBoardForm.showAiTableEntry,
          aiPurpose: this.clientBoardForm.aiPurpose || 'issue_tracking',
          regenerateToken: false
        })
        if (resp.data && resp.data.code === 0 && resp.data.data) {
          const d = resp.data.data
          this.clientBoardToken = d.publicToken || ''
          this.clientBoardMessageOk = true
          this.clientBoardMessage = '已保存'
        } else {
          this.clientBoardMessageOk = false
          this.clientBoardMessage = (resp.data && resp.data.message) || '保存失败'
        }
      } catch (e) {
        this.clientBoardMessageOk = false
        this.clientBoardMessage = '保存失败'
      } finally {
        this.clientBoardSaving = false
      }
    },
    async regenerateClientBoardToken () {
      if (!this.projectId) return
      if (!window.confirm('重新生成后旧链接将失效，确定继续？')) return
      this.clientBoardSaving = true
      this.clientBoardMessage = ''
      try {
        const resp = await this.$http.put(`/collab/projects/${this.projectId}/client-board`, {
          enabled: !!this.clientBoardForm.enabled,
          showProgressDashboard: !!this.clientBoardForm.showProgressDashboard,
          showFeatureBacklog: !!this.clientBoardForm.showFeatureBacklog,
          showAiTableEntry: !!this.clientBoardForm.showAiTableEntry,
          aiPurpose: this.clientBoardForm.aiPurpose || 'issue_tracking',
          regenerateToken: true
        })
        if (resp.data && resp.data.code === 0 && resp.data.data) {
          this.clientBoardToken = resp.data.data.publicToken || ''
          this.clientBoardMessageOk = true
          this.clientBoardMessage = '已重新生成令牌'
        } else {
          this.clientBoardMessageOk = false
          this.clientBoardMessage = (resp.data && resp.data.message) || '操作失败'
        }
      } catch (e) {
        this.clientBoardMessageOk = false
        this.clientBoardMessage = '操作失败'
      } finally {
        this.clientBoardSaving = false
      }
    },
    getClientBoardPublicUrl () {
      const token = (this.clientBoardToken || '').trim()
      if (!token) return ''
      let origin = ''
      const env = typeof process !== 'undefined' && process.env
        ? (process.env.VUE_APP_PUBLIC_ORIGIN || process.env.VUE_APP_CLIENT_BOARD_ORIGIN || '')
        : ''
      const envTrim = String(env || '').trim().replace(/\/+$/, '')
      if (envTrim) {
        origin = envTrim
      } else if (typeof window !== 'undefined' && window.location && window.location.origin) {
        origin = String(window.location.origin).replace(/\/+$/, '')
      }
      return `${origin}/client-board/${encodeURIComponent(token)}`
    },
    /**
     * 兼容非 HTTPS / 内网：clipboard API 常不可用，回退到 document.execCommand('copy')。
     */
    async copyTextToClipboard (text) {
      if (text == null || String(text) === '') return false
      const s = String(text)
      try {
        if (navigator.clipboard && typeof navigator.clipboard.writeText === 'function' && window.isSecureContext) {
          await navigator.clipboard.writeText(s)
          return true
        }
      } catch (e) {
        /* fall through */
      }
      try {
        const ta = document.createElement('textarea')
        ta.value = s
        ta.setAttribute('readonly', 'readonly')
        ta.style.cssText = 'position:fixed;top:0;left:0;width:2px;height:2px;padding:0;border:none;outline:none;box-shadow:none;background:transparent;opacity:0;'
        document.body.appendChild(ta)
        ta.focus()
        ta.select()
        ta.setSelectionRange(0, s.length)
        const ok = document.execCommand('copy')
        document.body.removeChild(ta)
        return !!ok
      } catch (e) {
        return false
      }
    },
    async copyClientBoardLink () {
      const url = this.getClientBoardPublicUrl()
      if (!url) return
      const ok = await this.copyTextToClipboard(url)
      if (ok) {
        this.clientBoardCopied = 'link'
        this.clientBoardMessageOk = true
        this.clientBoardMessage = '已复制客户访问链接'
        setTimeout(() => { this.clientBoardCopied = '' }, 2200)
      } else {
        this.clientBoardMessageOk = false
        this.clientBoardMessage = '复制失败：请点击上方链接框全选后手动复制'
      }
    },
    async copyClientBoardId () {
      const token = (this.clientBoardToken || '').trim()
      if (!token) return
      const ok = await this.copyTextToClipboard(token)
      if (ok) {
        this.clientBoardCopied = 'id'
        this.clientBoardMessageOk = true
        this.clientBoardMessage = '已复制浏览 ID'
        setTimeout(() => { this.clientBoardCopied = '' }, 2200)
      } else {
        this.clientBoardMessageOk = false
        this.clientBoardMessage = '复制失败：请手动选中 ID 文本复制'
      }
    },
    openClientBoardInNewTab () {
      const url = this.getClientBoardPublicUrl()
      if (!url) return
      try {
        window.open(url, '_blank', 'noopener,noreferrer')
      } catch (e) {
        this.clientBoardMessageOk = false
        this.clientBoardMessage = '无法打开新窗口，请检查浏览器弹窗拦截'
      }
    },
    toggleDashboardView () {
      if (this.tablePurpose !== 'issue_tracking') return
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
      if (this.tablePurpose === 'feature_backlog') {
        const st = this.getRecordStringValue(row, '开发进度')
        return st.includes('已完成')
      }
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
    /**
     * 已完成任务：按「最后更新周」分组，计算创建→更新 时长（小时）的周均值。
     * 完成态与 isResolvedRecord 一致；时长用 updatedAt - createdAt 近似。
     */
    buildAvgResolveWeeklySeries () {
      const weeks = Number(this.dashboardWeeks) > 0 ? Number(this.dashboardWeeks) : 8
      const nowWeek = this.weekStartDate(new Date())
      const labels = []
      const weekMap = {}
      for (let i = weeks - 1; i >= 0; i--) {
        const d = new Date(nowWeek.getTime())
        d.setDate(d.getDate() - i * 7)
        const key = this.weekLabel(d)
        labels.push(key)
        weekMap[key] = { sum: 0, count: 0 }
      }
      const avgs = []
      const counts = []
      const rows = Array.isArray(this.records) ? this.records : []
      rows.forEach((row) => {
        if (!this.isResolvedRecord(row)) return
        const end = row.updatedAt ? new Date(row.updatedAt) : null
        const start = row.createdAt ? new Date(row.createdAt) : null
        if (!end || !start || Number.isNaN(end.getTime()) || Number.isNaN(start.getTime())) return
        const ms = end.getTime() - start.getTime()
        if (ms <= 0 || ms > 366 * 24 * 3600 * 1000) return
        const hours = ms / 3600000
        const wk = this.weekLabel(this.weekStartDate(end))
        if (Object.prototype.hasOwnProperty.call(weekMap, wk)) {
          weekMap[wk].sum += hours
          weekMap[wk].count += 1
        }
      })
      labels.forEach((l) => {
        const x = weekMap[l]
        counts.push(x.count)
        avgs.push(x.count > 0 ? x.sum / x.count : 0)
      })
      return { labels, avgs, counts }
    },
    wordCloudTokenStyle (it) {
      const minPx = 12
      const maxPx = 26
      const max = it && it.max ? it.max : 1
      const w = it && it.weight != null ? it.weight : 0
      const r = max > 0 ? w / max : 0
      const fs = minPx + (maxPx - minPx) * r
      const palette = ['#1e40af', '#2563eb', '#0d9488', '#d97706', '#b45309', '#7c3aed', '#be185d', '#0f766e']
      const idx = (it && it.text ? String(it.text).charCodeAt(0) : 0) % palette.length
      return {
        fontSize: fs + 'px',
        color: palette[idx],
        fontWeight: r >= 0.35 ? 600 : 400
      }
    },
    renderDashboardCharts () {
      if (!this.showDashboard) return
      this.destroyDashboardCharts()
      this.dashboardWordCloudItems = buildWordCloudItems(this.records, this.columns, 48)
      this.avgResolveSeries = this.buildAvgResolveWeeklySeries()
      const total = (this.records || []).length
      const resolved = (this.records || []).filter(r => this.isResolvedRecord(r)).length
      const unresolved = Math.max(total - resolved, 0)
      const createdSeries = this.buildWeeklySeries(this.records)
      const resolvedSeries = this.buildWeeklySeries(this.records, r => this.isResolvedRecord(r))
      const importance = this.getImportanceDistribution()
      const avgSer = this.avgResolveSeries

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
      if (this.$refs.avgResolveChart && avgSer && avgSer.avgs && avgSer.avgs.some((v) => Number(v) > 0)) {
        const self = this
        this.dashboardCharts.avgResolve = new ChartJS(this.$refs.avgResolveChart, {
          type: 'bar',
          data: {
            labels: avgSer.labels,
            datasets: [{
              label: this.$t('collabTable.dashboardAvgResolveSeriesLabel'),
              data: avgSer.avgs,
              backgroundColor: '#6366f1'
            }]
          },
          options: {
            responsive: true,
            maintainAspectRatio: false,
            scales: {
              y: {
                beginAtZero: true,
                title: {
                  display: true,
                  text: this.$t('collabTable.dashboardAvgResolveYAxis')
                }
              }
            },
            plugins: {
              tooltip: {
                callbacks: {
                  label (ctx) {
                    const i = ctx.dataIndex
                    const h = ctx.parsed.y
                    const c = (avgSer.counts && avgSer.counts[i]) != null ? avgSer.counts[i] : 0
                    return self.$t('collabTable.dashboardAvgResolveTooltip', {
                      hours: Number(h).toFixed(2),
                      count: c
                    })
                  }
                }
              }
            }
          }
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
        }, { params: { purpose: this.tablePurpose } })
        if (resp.data && resp.data.code === 0 && resp.data.data && resp.data.data.items) {
          let items = resp.data.data.items || []
          // 同一张图生成多条记录：默认把当前选中的附件复制到每条草稿中
          if (Array.isArray(items) && this.aiSelectedAttachmentIds && this.aiSelectedAttachmentIds.length) {
            const base = Array.from(new Set(this.aiSelectedAttachmentIds.map(Number).filter(n => !Number.isNaN(n))))
            items.forEach(t => {
              if (!t) return
              const existing = Array.isArray(t.attachmentIds) ? t.attachmentIds : []
              const merged = Array.from(new Set([...existing, ...base].map(Number).filter(n => !Number.isNaN(n))))
              t.attachmentIds = merged
            })
          }
          // 单条任务录入模式：仅保留第一条草稿，避免拆分为多条
          if (this.aiSplitMode === 'single' && Array.isArray(items) && items.length > 1) {
            items = [items[0]]
          }
          this.aiTasks = items
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
        }, { params: { purpose: this.tablePurpose } })
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
    onAiPaste (e) {
      const cd = e && e.clipboardData
      if (!cd || !cd.items || !cd.items.length) return
      const imageFiles = []
      for (const it of Array.from(cd.items)) {
        if (!it) continue
        if (it.kind === 'file' && it.type && it.type.startsWith('image/')) {
          const f = it.getAsFile()
          if (f) imageFiles.push(f)
        }
      }
      if (!imageFiles.length) return
      // 当剪贴板里有图片时，拦截默认粘贴（避免把图片当成文本或污染输入框）
      e.preventDefault()
      this.uploadAiFiles(imageFiles)
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
      const files = Array.from((e && e.target && e.target.files) || [])
      if (!files.length) return
      await this.uploadAiFiles(files)
      if (e && e.target) e.target.value = ''
    },
    getAiAttachmentIcon (fileName) {
      const n = (fileName || '').toLowerCase()
      if (n.match(/\.(png|jpe?g|gif|webp|svg)$/)) return '🖼️'
      if (n.match(/\.(mp4|mov|m4v|avi|mkv|webm)$/)) return '🎬'
      if (n.match(/\.(pdf)$/)) return '📕'
      if (n.match(/\.(doc|docx)$/)) return '📄'
      if (n.match(/\.(xls|xlsx)$/)) return '📊'
      if (n.match(/\.(ppt|pptx)$/)) return '📑'
      if (n.match(/\.(txt|md)$/)) return '📝'
      return '📎'
    },
    async uploadAiFiles (files) {
      if (!Array.isArray(files) || !files.length) return
      for (const file of files) {
        if (!file) continue
        let uploadFile = file
        if (shouldCompressAsRasterImage(file)) {
          try {
            uploadFile = await compressImageFile(file, IMAGE_COMPRESS_PRESETS.attachment)
          } catch (_err) {
            /* 使用原文件 */
          }
        }
        const fd = new FormData()
        fd.append('file', uploadFile)
        try {
          const resp = await this.$http.post(`/collab/projects/${this.projectId}/attachments`, fd, {
            headers: { 'Content-Type': 'multipart/form-data' }
          })
          if (resp.data && resp.data.code === 0 && resp.data.data) {
            const a = resp.data.data
            const attId = a.id
            const item = {
              id: attId,
              fileName: a.fileName,
              fileSize: a.fileSize,
              isImage: a.isImage === true,
              previewUrl: a.isImage ? URL.createObjectURL(uploadFile) : null
            }
            this.aiSessionAttachments.push(item)
            // 上传后默认选中
            if (attId != null && !this.aiSelectedAttachmentIds.includes(attId)) {
              this.aiSelectedAttachmentIds.push(attId)
            }
          }
        } catch (err) {
          alert(this.$t('collabTable.aiUploadFailed') || '上传附件失败')
        }
      }
    },
    async removeAiAttachment (a) {
      if (!a || a.id == null) return
      try {
        await this.$http.delete(`/collab/attachments/${a.id}`)
      } catch (e) {
        // 允许本地先移除，避免卡住；后端失败由用户重新上传即可
      }
      const idx = this.aiSessionAttachments.findIndex(x => x && x.id === a.id)
      if (idx >= 0) {
        const item = this.aiSessionAttachments[idx]
        if (item && item.previewUrl) {
          try { URL.revokeObjectURL(item.previewUrl) } catch (_e) { /* ignore */ }
        }
        this.aiSessionAttachments.splice(idx, 1)
      }
      const idn = Number(a.id)
      this.aiSelectedAttachmentIds = this.aiSelectedAttachmentIds.filter(x => Number(x) !== idn)
    },
    async loadProjectSummary () {
      try {
        const resp = await this.$http.get(`/collab/projects/${this.projectId}`)
        if (resp.data && resp.data.code === 0 && resp.data.data) {
          this.projectName = resp.data.data.name != null ? String(resp.data.data.name) : ''
          this.projectRepoId = resp.data.data.repoId != null ? String(resp.data.data.repoId) : ''
        }
      } catch (e) {
        if (e.response && e.response.status === 401) this.$router.push({ name: 'login' })
      }
    },
    async loadPortalLinks () {
      if (!this.projectId) return
      try {
        const resp = await this.$http.get(`/collab/projects/${this.projectId}/portal-links`)
        if (resp.data && resp.data.code === 0 && resp.data.data && Array.isArray(resp.data.data.items)) {
          this.portalLinks = resp.data.data.items
        } else {
          this.portalLinks = []
        }
      } catch (e) {
        this.portalLinks = []
      }
    },
    async openPortalModal () {
      this.portalModalOpen = true
      this.portalMessage = ''
      this.portalMessageOk = false
      this.portalLoading = true
      try {
        await this.loadPortalLinks()
        this.portalEditItems = (this.portalLinks || []).map(it => ({
          _key: 'pl-' + it.id,
          id: it.id,
          label: it.label || '',
          url: it.url || '',
          sortOrder: it.sortOrder || 0
        }))
        const pr = await this.$http.get('/collab/projects')
        const items = pr.data && pr.data.data && Array.isArray(pr.data.data.items) ? pr.data.data.items : []
        this.portalImportProjectOptions = items.filter(p => p && Number(p.id) !== Number(this.projectId))
      } catch (e) {
        this.portalEditItems = []
        this.portalImportProjectOptions = []
      } finally {
        this.portalLoading = false
      }
    },
    closePortalModal () {
      this.portalModalOpen = false
      this.portalImportFromProjectId = 0
    },
    addPortalLink () {
      const key = 'new-' + Date.now() + '-' + Math.random().toString(16).slice(2)
      this.portalEditItems.push({ _key: key, id: null, label: '', url: '', sortOrder: this.portalEditItems.length })
    },
    removePortalLink (idx) {
      this.portalEditItems.splice(idx, 1)
    },
    async savePortalLinks () {
      if (!this.projectId) return
      this.portalSaving = true
      this.portalMessage = ''
      try {
        // 简单校验
        for (const it of this.portalEditItems) {
          if (!it.label || !String(it.label).trim()) throw new Error('展示文本不能为空')
          const url = String(it.url || '').trim()
          if (!url) throw new Error('URL 不能为空')
          if (!(url.startsWith('http://') || url.startsWith('https://'))) throw new Error('URL 仅支持 http/https')
        }
        // 先拉取当前服务端数据，用于计算删除
        const current = Array.isArray(this.portalLinks) ? this.portalLinks : []
        const currentIds = new Set(current.map(x => Number(x.id)).filter(n => !isNaN(n)))
        const nextIds = new Set(this.portalEditItems.map(x => Number(x.id)).filter(n => !isNaN(n)))
        // 删除
        for (const id of currentIds) {
          if (!nextIds.has(id)) {
            await this.$http.delete(`/collab/projects/${this.projectId}/portal-links/${id}`)
          }
        }
        // upsert（逐条）
        for (const it of this.portalEditItems) {
          const payload = { label: String(it.label).trim(), url: String(it.url).trim(), sortOrder: Number(it.sortOrder) || 0 }
          if (it.id) {
            await this.$http.put(`/collab/projects/${this.projectId}/portal-links/${it.id}`, payload)
          } else {
            const r = await this.$http.post(`/collab/projects/${this.projectId}/portal-links`, payload)
            const newId = r.data && r.data.data && r.data.data.id
            if (newId) it.id = newId
          }
        }
        this.portalMessageOk = true
        this.portalMessage = '已保存'
        await this.loadPortalLinks()
        this.portalEditItems = (this.portalLinks || []).map(it => ({
          _key: 'pl-' + it.id,
          id: it.id,
          label: it.label || '',
          url: it.url || '',
          sortOrder: it.sortOrder || 0
        }))
      } catch (e) {
        this.portalMessageOk = false
        this.portalMessage = (e && e.message) || '保存失败'
      } finally {
        this.portalSaving = false
      }
    },
    async importPortalLinks () {
      if (!this.projectId || !this.portalImportFromProjectId) return
      if (!window.confirm('导入将覆盖当前项目的全部传送门链接，是否继续？')) return
      this.portalImporting = true
      this.portalMessage = ''
      try {
        const resp = await this.$http.post(`/collab/projects/${this.projectId}/portal-links/import`, {
          fromProjectId: this.portalImportFromProjectId
        })
        if (resp.data && resp.data.code === 0) {
          await this.loadPortalLinks()
          this.portalEditItems = (this.portalLinks || []).map(it => ({
            _key: 'pl-' + it.id,
            id: it.id,
            label: it.label || '',
            url: it.url || '',
            sortOrder: it.sortOrder || 0
          }))
          this.portalMessageOk = true
          const n = resp.data.data && resp.data.data.importedCount != null ? resp.data.data.importedCount : 0
          this.portalMessage = `已导入 ${n} 条`
        } else {
          this.portalMessageOk = false
          this.portalMessage = (resp.data && resp.data.message) || '导入失败'
        }
      } catch (e) {
        this.portalMessageOk = false
        this.portalMessage = '导入失败'
      } finally {
        this.portalImporting = false
      }
    },
    async loadTable () {
      this.tableLoading = true
      try {
        const resp = await this.$http.get(`/collab/projects/${this.projectId}/table`, {
          params: { purpose: this.tablePurpose }
        })
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
        const baseParams = { _t: Date.now(), purpose: this.tablePurpose }
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
      const n = (col.name || '').trim()
      if (n === '问题描述' || n === '功能描述') return 'col-problem'
      if (this.isAttachmentColumn(col)) return 'col-attachment'
      return ''
    },
    getCellClass (col) {
      const classes = []
      const cn = (col.name || '').trim()
      if (cn === '问题描述' || cn === '功能描述') classes.push('cell-problem')
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
    async openRecord (row, initialTab = 'fields') {
      this.drawerRecord = row
      this.drawerEditValues = {}
      this.syncDrawerEditValuesFromRow(row)
      this.drawerTab = ['fields', 'comments', 'attachments', 'history'].includes(initialTab) ? initialTab : 'fields'
      this.comments = []
      this.attachments = []
      this.historyItems = []
      this.newComment = ''
      await this.loadAttachments()
      this.loadProjectMembersForDrawer()
      if (this.drawerTab === 'history') await this.loadHistory()
      else if (this.drawerTab === 'comments') await this.loadComments()
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
      return n === '重要程度' || n === '当前状态' || n === '解决情况' || n === '验收结果' || n === '开发进度'
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
      const devProgressMap = {
        待开发: 'status-tag-slate',
        开发中: 'status-tag-violet',
        联调中: 'status-tag-cyan',
        测试中: 'status-tag-cyan',
        已完成: 'status-tag-green',
        阻塞: 'status-tag-orange'
      }
      let cls = ''
      if (name === '重要程度') cls = levelMap[v] || ''
      if (name === '当前状态' || name === '解决情况') cls = statusMap[v] || ''
      if (name === '验收结果') cls = acceptMap[v] || ''
      if (name === '开发进度') cls = devProgressMap[v] || ''
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
      this.statusPickerRecordId = opening ? recordId : null
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
      this.statusPickerRecordId = null
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
      const n = (col.name || '').trim()
      return n === '问题描述' || n === '功能描述'
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
        let uploadFile = file
        if (shouldCompressAsRasterImage(file)) {
          try {
            uploadFile = await compressImageFile(file, IMAGE_COMPRESS_PRESETS.attachment)
          } catch (_err) {
            /* 原图上传 */
          }
        }
        const form = new FormData()
        form.append('file', uploadFile)
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
      if (name === '开发进度') return opts.indexOf('待开发') !== -1 ? '待开发' : opts[0]
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
    async onFileSelected (e) {
      const files = Array.from((e && e.target && e.target.files) || [])
      if (!files.length || !this.drawerRecord) return
      for (const file of files) {
        if (!file) continue
      let uploadFile = file
      if (file.type && file.type.startsWith('image/') && shouldCompressAsRasterImage(file)) {
        try {
          uploadFile = await compressImageFile(file, IMAGE_COMPRESS_PRESETS.attachment)
        } catch (_err) {
          /* 原图 */
        }
      }
      const form = new FormData()
      form.append('file', uploadFile)
      try {
        await this.$http.post(`/collab/records/${this.drawerRecord.id}/attachments`, form, {
          headers: { 'Content-Type': 'multipart/form-data' }
        })
      } catch (_err) { /* ignore */ }
      }
      this.loadAttachments()
      if (e && e.target) e.target.value = ''
    },
    async deleteDrawerAttachment (a) {
      if (!a || a.id == null) return
      if (!window.confirm(this.$t('collabTable.confirmDeleteAttachment') || '确定删除该附件吗？')) return
      try {
        await this.$http.delete(`/collab/attachments/${a.id}`)
      } catch (e) { /* ignore */ }
      this.loadAttachments()
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
        const resp = await this.$http.post(`/collab/projects/${this.projectId}/records`, { fields }, {
          params: { purpose: this.tablePurpose }
        })
        const recordId = resp.data && resp.data.data && resp.data.data.id
        if (recordId && this.newRecordImageFile && this.newRecordAttachmentColId) {
          let uploadFile = this.newRecordImageFile
          if (shouldCompressAsRasterImage(uploadFile)) {
            try {
              uploadFile = await compressImageFile(uploadFile, IMAGE_COMPRESS_PRESETS.attachment)
            } catch (_err) {
              /* 原图 */
            }
          }
          const form = new FormData()
          form.append('file', uploadFile)
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
/* ============================================================
   CollabTableView - Refactored Scoped Styles
   Uses CSS variables from theme.css design system
   ============================================================ */

/* ===================== Layout ===================== */

.collab-table-page {
  max-width: 98%;
  width: 100%;
  margin: 0 auto;
  padding: 0 var(--space-md);
  box-sizing: border-box;
}

.collab-table-layout {
  display: flex;
  align-items: stretch;
  min-height: 100vh;
}

.collab-table-main {
  flex: 1;
  min-width: 0;
}

/* ===================== Sidebar ===================== */

.collab-sidebar {
  width: 208px;
  flex-shrink: 0;
  padding: var(--space-lg) var(--space-md);
  border-right: 1px solid var(--border-primary);
  background: var(--bg-page);
}

.collab-sidebar.collapsed {
  width: 64px;
  padding: var(--space-lg) var(--space-sm);
}

.collab-sidebar-collapse {
  width: 100%;
  margin-bottom: 10px;
  border: 1px solid var(--border-primary);
  border-radius: var(--radius-md);
  background: var(--bg-card);
  color: var(--text-secondary);
  padding: 6px 0;
  font-size: var(--font-size-xs);
  font-weight: var(--font-weight-semibold);
  cursor: pointer;
  transition: var(--transition-fast);
}

.collab-sidebar-collapse:hover {
  background: var(--bg-hover);
}

.collab-sidebar-nav {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.collab-sidebar-link {
  display: block;
  padding: 10px var(--space-md);
  border-radius: var(--radius-md);
  color: var(--text-secondary);
  text-decoration: none;
  font-size: var(--font-size-base);
  border: 0;
  background: transparent;
  cursor: pointer;
  transition: var(--transition-fast);
}

.collab-sidebar-link:hover {
  background: var(--bg-hover);
  color: var(--text-primary);
}

.collab-sidebar-link.active {
  background: var(--brand-blue-light);
  color: var(--brand-blue);
  font-weight: var(--font-weight-semibold);
}

.collab-sidebar-hint {
  margin-top: var(--space-md);
  font-size: var(--font-size-xs);
  color: var(--text-tertiary);
  line-height: 1.4;
}

.collab-sidebar.collapsed .collab-sidebar-hint {
  display: none;
}

.collab-sidebar-link-btn {
  width: 100%;
  text-align: left;
}

.collab-sidebar.collapsed .collab-sidebar-link {
  padding: 10px var(--space-sm);
  font-size: var(--font-size-sm);
  text-align: center;
}

.collab-sidebar.collapsed .collab-sidebar-link-btn {
  text-align: center;
}

/* ===================== Table Header / Toolbar ===================== */

.table-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: var(--space-lg);
  margin-bottom: var(--space-lg);
}

.header-left {
  display: flex;
  align-items: flex-start;
  gap: var(--space-md);
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

.back-link {
  font-size: var(--font-size-sm);
  color: var(--brand-blue);
  text-decoration: none;
  transition: var(--transition-fast);
}

.back-link:hover {
  color: var(--brand-blue-hover);
  text-decoration: underline;
}

.table-title {
  margin: 0;
  font-size: var(--font-size-xl);
  line-height: var(--line-height-tight);
  color: var(--text-primary);
}

.header-actions {
  display: flex;
  align-items: center;
  gap: var(--space-md);
  flex-wrap: wrap;
  flex-shrink: 0;
}

/* ===================== Active Filter Chips ===================== */

.active-filter-chips {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-sm);
  margin-top: 10px;
}

.filter-chip {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  max-width: 100%;
  padding: var(--space-xs) 6px var(--space-xs) 10px;
  font-size: var(--font-size-xs);
  color: var(--text-secondary);
  background: var(--bg-page);
  border: 1px solid var(--border-primary);
  border-radius: var(--radius-full);
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
  background: var(--border-primary);
  color: var(--text-secondary);
  font-size: 16px;
  line-height: 1;
  cursor: pointer;
  transition: var(--transition-fast);
}

.filter-chip-remove:hover {
  background: var(--danger-bg);
  color: var(--danger);
}

/* ===================== Buttons ===================== */

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

.primary-button:hover:not(:disabled) {
  background: var(--brand-blue-hover);
}

.primary-button:active:not(:disabled) {
  background: var(--brand-blue-active);
}

.primary-button:disabled {
  opacity: 0.45;
  cursor: not-allowed;
}

.primary-button.small {
  padding: var(--space-xs) var(--space-md);
  font-size: var(--font-size-sm);
}

.secondary-button {
  padding: var(--space-sm) var(--space-lg);
  border-radius: var(--radius-sm);
  border: 1px solid var(--border-primary);
  background: var(--bg-card);
  color: var(--text-primary);
  cursor: pointer;
  font-size: var(--font-size-base);
  font-weight: var(--font-weight-medium);
  transition: var(--transition-fast);
}

.secondary-button:hover:not(:disabled) {
  background: var(--bg-hover);
  border-color: var(--border-input-hover);
}

.secondary-button:disabled {
  opacity: 0.45;
  cursor: not-allowed;
}

.secondary-button.small {
  padding: var(--space-xs) var(--space-md);
  font-size: var(--font-size-sm);
}

.danger-button {
  padding: var(--space-sm) var(--space-lg);
  border-radius: var(--radius-sm);
  border: none;
  background: var(--danger);
  color: #fff;
  cursor: pointer;
  font-size: var(--font-size-base);
  font-weight: var(--font-weight-medium);
  transition: var(--transition-fast);
}

.danger-button:hover:not(:disabled) {
  background: #E03E3A;
}

.danger-button:disabled {
  opacity: 0.45;
  cursor: not-allowed;
}

.danger-button.small {
  padding: var(--space-xs) var(--space-md);
  font-size: var(--font-size-sm);
}

.link-button {
  border: none;
  background: transparent;
  color: var(--brand-blue);
  cursor: pointer;
  font-size: var(--font-size-sm);
  transition: var(--transition-fast);
}

.link-button:hover {
  color: var(--brand-blue-hover);
  text-decoration: underline;
}

.link-button.danger {
  color: var(--danger);
}

.link-button.danger:hover {
  color: #E03E3A;
}

/* ===================== AI Magic / CSV Buttons ===================== */

.ai-magic-button {
  display: inline-flex;
  align-items: center;
  gap: var(--space-sm);
  padding: 9px var(--space-lg);
  font-size: var(--font-size-base);
  font-weight: var(--font-weight-semibold);
  color: var(--brand-purple);
  background: linear-gradient(145deg, var(--brand-blue-light) 0%, #ede9fe 100%);
  border: 1px solid #c7d2fe;
  border-radius: 10px;
  cursor: pointer;
  box-shadow: var(--shadow-sm), inset 0 1px 0 rgba(255, 255, 255, 0.65);
  transition: transform 0.15s ease, box-shadow 0.2s ease, filter 0.2s ease;
}

.ai-magic-button:hover {
  filter: brightness(1.02);
  box-shadow: var(--shadow-md);
  transform: translateY(-1px);
}

.ai-magic-button:active {
  transform: translateY(0);
  box-shadow: var(--shadow-sm);
}

.ai-magic-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 22px;
  height: 22px;
  font-size: 13px;
  line-height: 1;
  color: var(--brand-purple);
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
  gap: var(--space-sm);
  padding: 9px var(--space-lg);
  font-size: var(--font-size-base);
  font-weight: var(--font-weight-semibold);
  color: #0f766e;
  background: linear-gradient(145deg, var(--success-bg) 0%, #d1fae5 100%);
  border: 1px solid #5eead4;
  border-radius: 10px;
  cursor: pointer;
  margin-right: var(--space-sm);
  transition: transform 0.15s ease, box-shadow 0.2s ease;
}

.ai-csv-button:hover {
  box-shadow: 0 4px 12px rgba(13, 148, 136, 0.15);
  transform: translateY(-1px);
}

/* ===================== Table Wrapper & Data Table ===================== */

.table-scroll-outer {
  overflow-x: auto;
  background: var(--bg-card);
  border-radius: var(--radius-md);
  border: 1px solid var(--border-primary);
  width: 100%;
}

.table-wrapper {
  overflow: visible;
  background: var(--bg-card);
  border-radius: var(--radius-md);
  width: 100%;
}

.data-table {
  width: 100%;
  min-width: 940px;
  border-collapse: collapse;
}

.data-table th,
.data-table td {
  padding: 10px var(--space-md);
  text-align: left;
  border-bottom: 1px solid var(--border-primary);
  vertical-align: middle;
  font-size: var(--font-size-base);
}

.data-table th {
  background: var(--bg-page);
  font-weight: var(--font-weight-semibold);
  font-size: var(--font-size-sm);
  color: var(--text-secondary);
  white-space: nowrap;
}

.data-table th.col-problem,
.data-table td.cell-problem {
  min-width: 200px;
  max-width: 380px;
}

.data-table th.col-attachment,
.data-table td.cell-attachment {
  width: 120px;
}

.data-table th.col-check,
.data-table td.col-check {
  width: 44px;
  text-align: center;
  vertical-align: middle;
}

.data-table th.col-serial,
.data-table td.col-serial {
  width: 60px;
  text-align: center;
  font-variant-numeric: tabular-nums;
}

.data-table th.col-ops {
  width: 100px;
}

.data-table th.col-header {
  position: relative;
}

.row-check-input {
  width: 16px;
  height: 16px;
  cursor: pointer;
  accent-color: var(--brand-blue);
}

.data-table tbody tr.data-table-row {
  cursor: pointer;
  transition: background-color var(--transition-fast);
}

.data-table tbody tr.data-table-row:hover {
  background: var(--bg-hover);
}

/* 下拉打开时抬高整行，避免同列 z-index 与层叠顺序导致下方行的状态标签盖住 fixed 菜单 */
.data-table tbody tr.status-picker-row-elevated {
  position: relative;
  z-index: 20040;
}

/* ===================== Cell Types ===================== */

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

.ops-cell {
  white-space: nowrap;
}

.ops-cell .link-button {
  margin-right: var(--space-sm);
}

.ops-cell .link-button.danger {
  color: var(--danger);
}

/* ===================== Status Tags ===================== */

.status-tag {
  display: inline-flex;
  align-items: center;
  max-width: 100%;
  padding: 2px 10px;
  border-radius: var(--radius-full);
  font-size: var(--font-size-xs);
  line-height: 18px;
  font-weight: var(--font-weight-medium);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.status-tag-blue   { color: #0c4a6e; background: #dbeafe; }
.status-tag-cyan   { color: #155e75; background: #cffafe; }
.status-tag-green  { color: #166534; background: #dcfce7; }
.status-tag-violet { color: #5b21b6; background: #ede9fe; }
.status-tag-amber  { color: #92400e; background: #fef3c7; }
.status-tag-orange { color: #9a3412; background: #ffedd5; }
.status-tag-red    { color: #991b1b; background: #fee2e2; }
.status-tag-slate  { color: var(--text-secondary); background: var(--bg-page); }

/* ===================== Status Inline Editor / Picker ===================== */

/* 勿对容器设 z-index，否则会新建层叠上下文，fixed 下拉无法压过下方表格行内的状态标签 */
.status-inline-editor {
  display: inline-flex;
  position: relative;
  max-width: 100%;
  overflow: visible;
}

.status-select {
  display: inline-flex;
  align-items: center;
  max-width: 100%;
  min-width: 108px;
  padding: 2px 28px 2px 10px;
  border: 1px solid transparent;
  border-radius: var(--radius-full);
  font-size: var(--font-size-xs);
  line-height: 18px;
  font-weight: var(--font-weight-medium);
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
  transition: var(--transition-fast);
}

.status-select:focus {
  outline: none;
  border-color: var(--brand-blue);
  box-shadow: 0 0 0 2px rgba(20, 86, 240, 0.15);
}

.status-select:disabled {
  opacity: 0.6;
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
  box-sizing: border-box;
  position: fixed;
  z-index: 20050;
  opacity: 1;
  background: var(--bg-card);
  border: 1px solid var(--border-primary);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-lg);
  padding: var(--space-sm);
  -webkit-overflow-scrolling: touch;
}

.status-picker-item {
  display: block;
  width: 100%;
  text-align: left;
  border: 1px solid transparent;
  background: var(--bg-card);
  color: var(--text-secondary);
  border-radius: var(--radius-md);
  padding: var(--space-sm) var(--space-md);
  margin-bottom: var(--space-xs);
  cursor: pointer;
  font-size: var(--font-size-sm);
  line-height: 1.45;
  white-space: normal;
  word-break: break-word;
  transition: var(--transition-fast);
}

.status-picker-item:last-child {
  margin-bottom: 0;
}

.status-picker-item:hover {
  background: var(--bg-hover);
  color: var(--text-primary);
}

.status-picker-item.active {
  border-color: var(--brand-blue-light);
  box-shadow: inset 0 0 0 1px rgba(20, 86, 240, 0.2);
}

/* ===================== List Thumbnails ===================== */

.list-thumbs {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-xs);
  align-items: center;
}

.list-thumb-wrap {
  flex-shrink: 0;
}

.list-thumb-img {
  width: 40px;
  height: 40px;
  object-fit: cover;
  border-radius: var(--radius-xs);
  display: block;
  border: 1px solid var(--border-primary);
}

.list-thumb-name {
  font-size: 11px;
  color: var(--text-tertiary);
  display: block;
  max-width: 56px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.list-thumb-img-clickable,
.thumb-img-clickable,
.attachment-preview-img-clickable {
  cursor: zoom-in;
}

/* ===================== Batch Toolbar ===================== */

.batch-toolbar {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px 14px;
  padding: 10px var(--space-md);
  background: var(--info-bg);
  border-bottom: 1px solid var(--brand-blue-light);
}

.batch-toolbar-text {
  font-size: var(--font-size-base);
  font-weight: var(--font-weight-semibold);
  color: var(--brand-blue);
  margin-right: auto;
}

/* ===================== Placeholder / Empty / Loading ===================== */

.placeholder,
.empty-tip,
.loading-tip {
  padding: var(--space-xxl);
  text-align: center;
  color: var(--text-tertiary);
  font-size: var(--font-size-base);
}

/* ===================== Drawer / Modal ===================== */

.drawer-mask {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.4);
  display: flex;
  align-items: center;
  justify-content: flex-end;
  z-index: var(--z-modal);
}

.drawer {
  width: 540px;
  max-width: 96vw;
  height: 100%;
  background: var(--bg-card);
  box-shadow: var(--shadow-lg);
  display: flex;
  flex-direction: column;
}

.drawer-wide {
  max-width: 560px;
}

.drawer-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: var(--space-lg);
  border-bottom: 1px solid var(--border-primary);
}

.drawer-header h3 {
  margin: 0;
  font-size: var(--font-size-lg);
  color: var(--text-primary);
}

.close-btn {
  border: none;
  background: none;
  font-size: var(--font-size-xxl);
  cursor: pointer;
  color: var(--text-tertiary);
  line-height: 1;
  transition: var(--transition-fast);
  padding: var(--space-xs);
  border-radius: var(--radius-sm);
}

.close-btn:hover {
  color: var(--text-primary);
  background: var(--bg-hover);
}

.drawer-tabs {
  display: flex;
  flex-wrap: wrap;
  row-gap: var(--space-xs);
  padding: 0 var(--space-lg);
  gap: var(--space-sm);
  border-bottom: 1px solid var(--border-primary);
}

.drawer-tabs button {
  padding: 10px var(--space-md);
  border: none;
  background: none;
  cursor: pointer;
  font-size: var(--font-size-sm);
  color: var(--text-tertiary);
  transition: var(--transition-fast);
  position: relative;
}

.drawer-tabs button:hover {
  color: var(--text-secondary);
}

.drawer-tabs button.active {
  color: var(--brand-blue);
  font-weight: var(--font-weight-medium);
}

.drawer-tabs button.active::after {
  content: '';
  position: absolute;
  bottom: -1px;
  left: 0;
  right: 0;
  height: 2px;
  background: var(--brand-blue);
  border-radius: 1px;
}

.drawer-body {
  flex: 1;
  overflow-y: auto;
  padding: var(--space-xl);
  max-width: 100%;
  box-sizing: border-box;
}

.drawer-actions {
  display: flex;
  gap: 10px;
  margin-top: var(--space-lg);
  padding-top: var(--space-md);
  border-top: 1px solid var(--border-primary);
}

/* ===================== Field List / Form ===================== */

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
  font-size: var(--font-size-xs);
  color: var(--text-tertiary);
  margin-bottom: 2px;
  font-weight: var(--font-weight-medium);
}

.field-value {
  font-size: var(--font-size-base);
}

.field-input-wrap {
  margin-top: var(--space-xs);
  max-width: 100%;
}

.field-input {
  width: 100%;
  max-width: 100%;
  padding: var(--space-sm) var(--space-md);
  border: 1px solid var(--border-input);
  border-radius: var(--radius-sm);
  font-size: var(--font-size-base);
  color: var(--text-primary);
  background: var(--bg-input);
  box-sizing: border-box;
  transition: var(--transition-fast);
}

.field-input:hover {
  border-color: var(--border-input-hover);
}

.field-input:focus {
  outline: none;
  border-color: var(--border-input-focus);
  box-shadow: 0 0 0 2px rgba(20, 86, 240, 0.15);
}

.field-input.field-textarea {
  min-height: 64px;
  resize: vertical;
  white-space: pre-wrap;
}

.field-readonly {
  background: var(--bg-input-disabled);
  color: var(--text-disabled);
  cursor: default;
}

.form-row {
  margin-bottom: var(--space-md);
}

.form-label {
  display: block;
  font-size: var(--font-size-xs);
  color: var(--text-tertiary);
  margin-bottom: 2px;
  font-weight: var(--font-weight-medium);
}

.form-input {
  width: 100%;
  padding: var(--space-sm) var(--space-md);
  border: 1px solid var(--border-input);
  border-radius: var(--radius-sm);
  font-size: var(--font-size-base);
  color: var(--text-primary);
  background: var(--bg-input);
  box-sizing: border-box;
  transition: var(--transition-fast);
}

.form-input:focus {
  outline: none;
  border-color: var(--border-input-focus);
  box-shadow: 0 0 0 2px rgba(20, 86, 240, 0.15);
}

.checkbox-label {
  display: inline-flex;
  align-items: center;
  gap: var(--space-sm);
  font-size: var(--font-size-base);
  color: var(--text-primary);
  cursor: pointer;
}

.checkbox-label input[type="checkbox"] {
  width: 16px;
  height: 16px;
  accent-color: var(--brand-blue);
  cursor: pointer;
}

.form-actions {
  display: flex;
  align-items: center;
  gap: var(--space-sm);
  margin-top: var(--space-md);
}

.save-message {
  font-size: var(--font-size-sm);
  margin-left: var(--space-sm);
}

.save-message.success {
  color: var(--success);
}

.save-message.error {
  color: var(--danger);
}

/* ===================== Multi Select ===================== */

.multi-select-wrap {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-sm) var(--space-lg);
}

.multi-select-item {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: var(--font-size-base);
  cursor: pointer;
}

.multi-select-item input {
  margin: 0;
  accent-color: var(--brand-blue);
}

/* ===================== Text Muted / Utility ===================== */

.text-muted {
  font-size: var(--font-size-sm);
  color: var(--text-tertiary);
}

.small {
  font-size: var(--font-size-sm);
}

/* ===================== Upload / Attachment ===================== */

.upload-field {
  display: flex;
  align-items: center;
  gap: var(--space-sm);
}

.file-name-tag {
  font-size: var(--font-size-sm);
  color: var(--success);
}

.file-size {
  font-size: var(--font-size-xs);
  color: var(--text-tertiary);
  margin-left: var(--space-xs);
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
  gap: var(--space-sm);
}

.thumb-wrap {
  flex-shrink: 0;
}

.thumb-img {
  width: 56px;
  height: 56px;
  object-fit: cover;
  border-radius: var(--radius-md);
  display: block;
  border: 1px solid var(--border-primary);
}

.thumb-name {
  font-size: var(--font-size-xs);
  color: var(--text-tertiary);
  display: block;
  max-width: 80px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.new-record-preview {
  display: flex;
  align-items: center;
  gap: var(--space-sm);
}

.new-record-preview-img {
  width: 64px;
  height: 64px;
  object-fit: cover;
  border-radius: var(--radius-md);
  border: 1px solid var(--border-primary);
}

.upload-area {
  margin-bottom: var(--space-lg);
}

.attachment-list {
  list-style: none;
  padding: 0;
  margin: 0;
}

.attachment-list li {
  padding: 6px 0;
  font-size: var(--font-size-base);
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
  border-radius: var(--radius-md);
  cursor: pointer;
  border: 1px solid var(--border-primary);
  transition: var(--transition-fast);
}

.attachment-preview-img:hover {
  box-shadow: var(--shadow-md);
}

.attachment-meta {
  display: flex;
  align-items: center;
  gap: var(--space-xs);
  flex-wrap: wrap;
}

.attachment-list a,
.attachment-list .link-button {
  color: var(--brand-blue);
}

/* ===================== Comment Panel ===================== */

.comment-panel {
  padding: 0;
}

.comment-list {
  display: flex;
  flex-direction: column;
}

.comment-item {
  margin-bottom: var(--space-md);
  padding-bottom: var(--space-md);
  border-bottom: 1px solid var(--bg-page);
}

.comment-user {
  font-weight: var(--font-weight-medium);
  font-size: var(--font-size-sm);
  color: var(--text-primary);
}

.comment-time {
  font-size: 11px;
  color: var(--text-tertiary);
  margin-left: var(--space-sm);
}

.comment-content {
  margin: var(--space-sm) 0 0;
  font-size: var(--font-size-base);
  white-space: pre-wrap;
  color: var(--text-primary);
}

.comment-form {
  margin-top: var(--space-lg);
}

.comment-form textarea {
  width: 100%;
  padding: var(--space-sm);
  border: 1px solid var(--border-input);
  border-radius: var(--radius-sm);
  font-size: var(--font-size-base);
  margin-bottom: var(--space-sm);
  box-sizing: border-box;
  color: var(--text-primary);
  background: var(--bg-input);
  transition: var(--transition-fast);
}

.comment-form textarea:focus {
  outline: none;
  border-color: var(--border-input-focus);
  box-shadow: 0 0 0 2px rgba(20, 86, 240, 0.15);
}

/* ===================== History Panel ===================== */

.history-panel {
  padding: 0;
}

.history-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.history-item {
  border: 1px solid var(--border-primary);
  border-radius: var(--radius-md);
  padding: 10px var(--space-md);
  background: var(--bg-card);
}

.history-topline {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 6px;
}

.history-action {
  font-size: var(--font-size-xs);
  color: #0f766e;
  background: #ecfeff;
  border: 1px solid #a5f3fc;
  border-radius: var(--radius-full);
  padding: 1px var(--space-sm);
}

.history-time {
  font-size: var(--font-size-xs);
  color: var(--text-tertiary);
}

.history-meta {
  font-size: var(--font-size-xs);
  color: var(--text-secondary);
  margin-bottom: 6px;
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}

.history-role {
  color: var(--brand-purple);
  font-weight: var(--font-weight-medium);
}

.history-field {
  margin-bottom: 6px;
  font-size: var(--font-size-sm);
}

.history-values {
  font-size: var(--font-size-xs);
  display: grid;
  grid-template-columns: 1fr;
  gap: var(--space-xs);
}

.history-old,
.history-new {
  padding: var(--space-xs) var(--space-sm);
  border-radius: var(--radius-xs);
  background: var(--bg-page);
}

/* ===================== Dashboard ===================== */

.dashboard-wrapper {
  background: var(--bg-card);
  border-radius: var(--radius-md);
  border: 1px solid var(--border-primary);
  padding: var(--space-md);
}

.dashboard-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: var(--space-md);
}

.dashboard-card {
  border: 1px solid var(--border-primary);
  border-radius: var(--radius-md);
  padding: 10px;
  background: var(--bg-card);
  min-height: 280px;
  display: flex;
  flex-direction: column;
  transition: var(--transition-normal);
}

.dashboard-card:hover {
  box-shadow: var(--shadow-md);
}

.dashboard-card canvas {
  width: 100% !important;
  height: 240px !important;
}

.dashboard-card-title {
  font-size: var(--font-size-base);
  font-weight: var(--font-weight-semibold);
  color: var(--text-primary);
  margin-bottom: var(--space-sm);
}

.dashboard-card-placeholder {
  justify-content: center;
  align-items: center;
}

.dashboard-placeholder {
  color: var(--text-tertiary);
  font-size: var(--font-size-sm);
}

.dashboard-card-wordcloud {
  min-height: 280px;
}

.dashboard-empty-hint {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  text-align: center;
  padding: var(--space-md);
  color: var(--text-tertiary);
  font-size: var(--font-size-sm);
  min-height: 200px;
}

.word-cloud-body {
  flex: 1;
  display: flex;
  flex-wrap: wrap;
  align-content: flex-start;
  align-items: center;
  justify-content: center;
  gap: var(--space-sm) var(--space-md);
  padding: var(--space-sm) var(--space-xs);
  min-height: 200px;
  line-height: 1.35;
}

.word-cloud-token {
  display: inline-block;
  cursor: default;
  user-select: none;
  max-width: 100%;
  word-break: break-all;
}

.dashboard-word-foot {
  font-size: 11px;
  color: var(--text-tertiary);
  margin: 6px 0 0;
  line-height: 1.4;
}

/* ===================== Home Dashboard ===================== */

.collab-home-dashboard {
  width: 100%;
  min-width: 0;
}

.portal-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  padding: 10px var(--space-md);
  border: 1px solid var(--border-primary);
  border-radius: var(--radius-lg);
  background: var(--bg-card);
}

.portal-items {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-sm);
  align-items: center;
  min-width: 0;
}

.portal-link {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 10px;
  border-radius: var(--radius-full);
  border: 1px solid var(--border-primary);
  background: var(--bg-page);
  color: var(--text-primary);
  text-decoration: none;
  max-width: 260px;
  transition: var(--transition-fast);
}

.portal-link:hover {
  border-color: var(--brand-blue-light);
  background: var(--info-bg);
  color: var(--brand-blue);
}

.portal-link-icon {
  flex-shrink: 0;
  font-size: var(--font-size-base);
}

.portal-link-label {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* ===================== Mail Notify / Config Panels ===================== */

.mail-notify-panel {
  margin: var(--space-md) 0;
  border: 1px solid var(--border-primary);
  border-radius: var(--radius-lg);
  background: var(--bg-card);
  padding: var(--space-md);
}

.mail-notify-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.mail-notify-title {
  font-size: var(--font-size-base);
  font-weight: var(--font-weight-bold);
  color: var(--text-primary);
}

.mail-notify-body {
  margin-top: 10px;
}

.client-board-token-row {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: var(--space-sm);
}

.client-board-code {
  font-size: var(--font-size-xs);
  font-family: var(--font-mono);
  background: var(--bg-page);
  padding: var(--space-xs) var(--space-sm);
  border-radius: var(--radius-sm);
  word-break: break-all;
  flex: 1;
  min-width: 0;
  border: 1px solid var(--border-primary);
}

.client-board-url-block {
  margin-top: var(--space-xs);
}
.client-board-url-row {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: var(--space-sm);
}
.client-board-url-input {
  flex: 1;
  min-width: 200px;
  font-size: var(--font-size-xs);
  font-family: var(--font-mono);
}
.client-board-url-hint {
  margin-top: var(--space-sm);
  line-height: 1.45;
}
.client-board-url-hint code {
  font-size: 11px;
  background: var(--bg-page);
  padding: 1px 4px;
  border-radius: var(--radius-sm);
  border: 1px solid var(--border-primary);
}

/* ===================== Portal Modal ===================== */

.portal-modal-actions {
  display: flex;
  gap: 10px;
  align-items: center;
  margin-bottom: var(--space-md);
}

.portal-edit-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.portal-edit-row {
  display: grid;
  grid-template-columns: 160px 1fr 90px 64px;
  gap: 10px;
  align-items: center;
}

.link-table-list {
  max-height: 360px;
  overflow: auto;
  border: 1px solid var(--border-primary);
  border-radius: var(--radius-md);
  padding: var(--space-sm);
}

.link-table-row {
  display: flex;
  align-items: flex-start;
  gap: var(--space-sm);
  padding: var(--space-sm);
  border-bottom: 1px solid var(--bg-page);
  cursor: pointer;
  transition: var(--transition-fast);
}

.link-table-row:hover {
  background: var(--bg-hover);
}

.link-table-title {
  flex: 1;
  font-size: var(--font-size-base);
}

.link-table-mod {
  font-size: var(--font-size-xs);
  color: var(--text-tertiary);
}

/* ===================== Filter Modal ===================== */

.filter-empty-block {
  padding: var(--space-sm) 0 var(--space-lg);
}

.filter-rule-row {
  display: flex;
  flex-direction: column;
  align-items: stretch;
  gap: 10px;
  margin-bottom: 14px;
  padding: var(--space-md);
  background: var(--bg-page);
  border: 1px solid var(--border-primary);
  border-radius: var(--radius-md);
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

.filter-rule-remove-btn {
  padding: 6px 10px;
  font-size: var(--font-size-xs);
  color: var(--danger);
  background: var(--bg-card);
  border: 1px solid var(--danger-bg);
  border-radius: var(--radius-sm);
  cursor: pointer;
  white-space: nowrap;
  transition: var(--transition-fast);
}

.filter-rule-remove-btn:hover {
  background: var(--danger-bg);
}

.filter-actions {
  margin-top: var(--space-lg);
}

/* ===================== AI Modal ===================== */

.ai-body {
  padding: var(--space-xl);
}

.ai-mode-tabs {
  display: flex;
  gap: var(--space-sm);
  margin-bottom: 14px;
  border-bottom: 1px solid var(--border-primary);
  padding-bottom: var(--space-sm);
}

.ai-mode-tabs button {
  padding: var(--space-sm) var(--space-md);
  border: none;
  background: var(--bg-page);
  color: var(--text-secondary);
  border-radius: var(--radius-md);
  cursor: pointer;
  font-size: var(--font-size-base);
  font-weight: var(--font-weight-semibold);
  transition: var(--transition-fast);
}

.ai-mode-tabs button:hover {
  background: var(--bg-hover);
}

.ai-mode-tabs button.active {
  background: var(--brand-blue);
  color: #fff;
}

.ai-split-mode-tabs {
  display: flex;
  gap: var(--space-sm);
  margin-bottom: 14px;
}

.ai-split-mode-tabs button {
  padding: 6px var(--space-md);
  border: 1px solid var(--border-primary);
  border-radius: var(--radius-md);
  background: var(--bg-card);
  color: var(--text-secondary);
  cursor: pointer;
  font-size: var(--font-size-sm);
  transition: var(--transition-fast);
}

.ai-split-mode-tabs button:hover {
  background: var(--bg-hover);
}

.ai-split-mode-tabs button.active {
  border-color: var(--brand-blue);
  background: var(--brand-blue-light);
  color: var(--brand-blue);
}

.ai-csv-section {
  margin-bottom: var(--space-lg);
}

.csv-import-mode-tabs {
  display: flex;
  gap: var(--space-sm);
  margin-bottom: 10px;
}

.csv-import-mode-tabs button {
  padding: 6px var(--space-md);
  border: 1px solid var(--border-primary);
  border-radius: var(--radius-md);
  background: var(--bg-card);
  color: var(--text-secondary);
  cursor: pointer;
  font-size: var(--font-size-sm);
  transition: var(--transition-fast);
}

.csv-import-mode-tabs button:hover {
  background: var(--bg-hover);
}

.csv-import-mode-tabs button.active {
  border-color: var(--brand-blue);
  background: var(--brand-blue-light);
  color: var(--brand-blue);
}

.ai-csv-section .hint-text {
  font-size: var(--font-size-sm);
  color: var(--text-tertiary);
  line-height: var(--line-height-normal);
  margin: 0 0 10px;
}

.quick-csv-mapping-panel {
  margin-top: 14px;
  border: 1px solid var(--border-primary);
  border-radius: var(--radius-md);
  padding: var(--space-md);
  background: var(--bg-card);
}

.quick-csv-title {
  margin: 0 0 10px;
  font-size: var(--font-size-base);
  color: var(--text-primary);
}

.quick-csv-sample-box {
  margin-top: 6px;
  max-height: 130px;
  overflow: auto;
  border: 1px solid var(--border-primary);
  border-radius: var(--radius-sm);
  background: var(--bg-card);
}

.quick-csv-sample-box pre {
  margin: 0;
  padding: var(--space-sm);
  font-size: var(--font-size-xs);
  line-height: 1.4;
  white-space: pre-wrap;
  word-break: break-word;
  font-family: var(--font-mono);
  color: var(--text-secondary);
}

.quick-map-list {
  margin: 10px 0 var(--space-md);
}

.quick-map-row {
  display: grid;
  grid-template-columns: 140px 1fr;
  gap: 10px;
  align-items: start;
  margin-bottom: 10px;
}

.quick-map-target {
  font-size: var(--font-size-sm);
  color: var(--text-secondary);
  line-height: 30px;
}

.quick-map-source {
  display: grid;
  grid-template-columns: 1fr 110px;
  gap: var(--space-sm);
}

.quick-multi-select {
  min-height: 80px;
}

.quick-join-input {
  min-height: 32px;
}

.csv-file-name {
  font-size: var(--font-size-sm);
  color: var(--text-secondary);
  margin-left: var(--space-sm);
}

.csv-progress-text {
  font-size: var(--font-size-sm);
  color: var(--text-secondary);
  margin: var(--space-sm) 0 0;
}

.csv-warn-list {
  margin: 10px 0 0;
  padding-left: 1.2rem;
  font-size: var(--font-size-sm);
  color: var(--warning);
}

.ai-input-section {
  margin-bottom: var(--space-md);
}

.ai-attachments-section {
  margin-bottom: var(--space-md);
}

.ai-upload-row {
  display: flex;
  align-items: center;
  gap: var(--space-sm);
}

.ai-paste-hint {
  margin-left: var(--space-sm);
}

.ai-attachment-list {
  margin-top: 10px;
  display: flex;
  flex-direction: column;
  gap: var(--space-sm);
}

.ai-attachment-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  padding: var(--space-sm) 10px;
  border: 1px solid var(--border-primary);
  border-radius: var(--radius-lg);
  background: var(--bg-card);
  transition: var(--transition-fast);
}

.ai-attachment-item:hover {
  border-color: var(--border-input-hover);
}

.ai-attachment-main {
  display: grid;
  grid-template-columns: 18px 100px 1fr;
  align-items: center;
  gap: 10px;
  flex: 1;
  cursor: pointer;
}

.ai-attachment-preview {
  width: 100px;
  height: 100px;
  border-radius: var(--radius-lg);
  border: 1px solid var(--border-primary);
  background: var(--bg-page);
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
}

.ai-thumb-img {
  width: 100px;
  height: 100px;
  object-fit: cover;
  display: block;
}

.ai-file-icon {
  font-size: 28px;
  line-height: 1;
}

.ai-attachment-meta {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}

.ai-file-name {
  font-size: var(--font-size-sm);
  color: var(--text-secondary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.ai-file-size {
  font-size: var(--font-size-xs);
  color: var(--text-tertiary);
}

.ai-attachment-remove {
  width: 28px;
  height: 28px;
  border-radius: var(--radius-full);
  border: 1px solid var(--border-primary);
  background: var(--bg-card);
  color: var(--text-tertiary);
  cursor: pointer;
  font-size: 18px;
  line-height: 1;
  transition: var(--transition-fast);
  flex-shrink: 0;
}

.ai-attachment-remove:hover {
  color: var(--danger);
  border-color: var(--danger-bg);
  background: var(--danger-bg);
}

.ai-actions {
  margin-top: var(--space-md);
}

.ai-result-section {
  margin-top: var(--space-lg);
}

.ai-result-section h4 {
  margin: 0 0 var(--space-md);
  font-size: var(--font-size-base);
  color: var(--text-primary);
}

.ai-task-item {
  margin-bottom: var(--space-md);
  padding: var(--space-md);
  border: 1px solid var(--border-primary);
  border-radius: var(--radius-md);
  background: var(--bg-page);
}

.ai-commit-actions {
  margin-top: var(--space-md);
}

/* ===================== Image Preview ===================== */

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
  gap: var(--space-md);
  padding: 10px 14px;
  background: var(--bg-page);
  border-bottom: 1px solid var(--border-primary);
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
  padding: 0 var(--space-sm);
  border: 1px solid var(--border-primary);
  border-radius: var(--radius-sm);
  background: var(--bg-card);
  color: var(--text-secondary);
  font-size: var(--font-size-lg);
  line-height: 1;
  cursor: pointer;
  transition: background 0.15s, border-color 0.15s;
}

.image-tool-btn:hover:not(:disabled) {
  background: var(--bg-hover);
  border-color: var(--text-tertiary);
}

.image-tool-btn:disabled {
  opacity: 0.45;
  cursor: not-allowed;
}

.image-tool-wide {
  min-width: 44px;
  font-size: var(--font-size-xs);
  font-weight: var(--font-weight-semibold);
}

.image-tool-download {
  color: var(--brand-blue);
  border-color: var(--brand-blue-light);
}

.image-tool-download:hover:not(:disabled) {
  background: var(--info-bg);
}

.image-tool-close {
  font-size: var(--font-size-xxl);
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
  padding: var(--space-xl);
  -webkit-overflow-scrolling: touch;
}

.image-preview-main {
  max-width: 100%;
  max-height: min(85vh, 900px);
  width: auto;
  height: auto;
  object-fit: contain;
  border-radius: var(--radius-xs);
  box-shadow: 0 8px 40px rgba(0, 0, 0, 0.35);
}

.image-preview-footer {
  flex-shrink: 0;
  padding: 10px var(--space-lg) 14px;
  text-align: center;
  font-size: var(--font-size-sm);
  color: var(--text-tertiary);
  background: #0f172a;
  border-top: 1px solid #1e293b;
  word-break: break-all;
}

/* ===================== Add Modal ===================== */

.add-modal .drawer-body .tip {
  margin: 0 0 var(--space-md);
  font-size: var(--font-size-sm);
  color: var(--text-tertiary);
}

/* ===================== Client Board Panel ===================== */

.client-board-panel {
  /* inherits from .mail-notify-panel */
}

/* ===================== Hint Text ===================== */

.hint-text {
  font-size: var(--font-size-sm);
  color: var(--text-tertiary);
  line-height: var(--line-height-normal);
  margin: 0 0 10px;
}
</style>
