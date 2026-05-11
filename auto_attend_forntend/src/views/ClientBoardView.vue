<template>
  <div class="lb-root">
    <!-- Loading -->
    <div v-if="loading" class="lb-state">
      <div class="lb-spinner"></div>
      <p>加载中…</p>
    </div>

    <!-- Error -->
    <div v-else-if="error" class="lb-state lb-err">
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" width="48" height="48"><circle cx="12" cy="12" r="10"/><path d="M12 8v4M12 16h.01"/></svg>
      <p>{{ error }}</p>
    </div>

    <!-- Main Content -->
    <template v-else>
      <!-- ===== Top Navigation Bar ===== -->
      <nav class="lb-nav">
        <div class="lb-nav-inner">
          <router-link class="lb-nav-left lb-nav-brand" :to="{ name: 'landing' }" title="返回平台首页">
            <div class="lb-nav-logo" aria-hidden="true">
              <img class="lb-nav-logo-img" src="/brand-logo.svg" width="24" height="24" alt="">
            </div>
            <span class="lb-nav-title">流帮 Project</span>
          </router-link>
          <div class="lb-nav-right">
            <span class="lb-nav-tenant" v-if="tenantName">{{ tenantName }}</span>
          </div>
        </div>
      </nav>

      <!-- ===== Project Hero ===== -->
      <header class="lb-hero">
        <div class="lb-hero-inner">
          <div class="lb-hero-meta">
            <span class="lb-tag">项目看板</span>
          </div>
          <h1 class="lb-hero-title">{{ projectName || '未命名项目' }}</h1>
          <p v-if="repoId" class="lb-hero-repo">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="14" height="14"><path d="M9 19c-5 1.5-5-2.5-7-3m14 6v-3.87a3.37 3.37 0 00-.94-2.61c3.14-.35 6.44-1.54 6.44-7A5.44 5.44 0 0020 4.77 5.07 5.07 0 0019.91 1S18.73.65 16 2.48a13.38 13.38 0 00-7 0C6.27.65 5.09 1 5.09 1A5.07 5.07 0 005 4.77a5.44 5.44 0 00-1.5 3.78c0 5.42 3.3 6.61 6.44 7A3.37 3.37 0 009 18.13V22"/></svg>
            {{ repoId }}
          </p>
        </div>
      </header>

      <!-- ===== Tab Bar ===== -->
      <div class="lb-tabs-bar">
        <div class="lb-tabs-inner">
          <button
            v-for="tab in visibleTabs"
            :key="tab.key"
            class="lb-tab"
            :class="{ 'lb-tab--active': activeTab === tab.key }"
            @click="activeTab = tab.key"
          >
            <svg v-if="tab.icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="15" height="15" v-html="tab.icon"></svg>
            {{ tab.label }}
          </button>
        </div>
      </div>

      <!-- ===== Tab Content ===== -->
      <main class="lb-main">

        <!-- ========== Tab: 项目概览 ========== -->
        <template v-if="activeTab === 'overview'">
          <!-- KPI Cards -->
          <section v-if="showProgress && progress && !progress.empty" class="lb-kpi-row">
            <div class="lb-kpi">
              <div class="lb-kpi-icon lb-kpi-icon--total">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="20" height="20"><rect x="3" y="3" width="7" height="7"/><rect x="14" y="3" width="7" height="7"/><rect x="14" y="14" width="7" height="7"/><rect x="3" y="14" width="7" height="7"/></svg>
              </div>
              <div class="lb-kpi-body">
                <div class="lb-kpi-value">{{ progress.totalRecords || 0 }}</div>
                <div class="lb-kpi-label">总任务</div>
              </div>
            </div>
            <div class="lb-kpi">
              <div class="lb-kpi-icon lb-kpi-icon--done">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="20" height="20"><path d="M22 11.08V12a10 10 0 11-5.93-9.14"/><polyline points="22 4 12 14.01 9 11.01"/></svg>
              </div>
              <div class="lb-kpi-body">
                <div class="lb-kpi-value lb-kpi-value--green">{{ progress.resolved || 0 }}</div>
                <div class="lb-kpi-label">已完成</div>
              </div>
            </div>
            <div class="lb-kpi">
              <div class="lb-kpi-icon lb-kpi-icon--open">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="20" height="20"><circle cx="12" cy="12" r="10"/><polyline points="12 6 12 12 16 14"/></svg>
              </div>
              <div class="lb-kpi-body">
                <div class="lb-kpi-value lb-kpi-value--orange">{{ progress.unresolved || 0 }}</div>
                <div class="lb-kpi-label">进行中</div>
              </div>
            </div>
            <div class="lb-kpi" v-if="resolveRate != null">
              <div class="lb-kpi-icon lb-kpi-icon--rate">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="20" height="20"><line x1="18" y1="20" x2="18" y2="10"/><line x1="12" y1="20" x2="12" y2="4"/><line x1="6" y1="20" x2="6" y2="14"/></svg>
              </div>
              <div class="lb-kpi-body">
                <div class="lb-kpi-value lb-kpi-value--blue">{{ resolveRate }}%</div>
                <div class="lb-kpi-label">完成率</div>
              </div>
            </div>
          </section>

          <!-- Progress Dashboard -->
          <section v-if="showProgress" class="lb-section">
            <div v-if="!progress || progress.empty" class="lb-empty-card">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" width="48" height="48" style="color:#c0c4cc"><rect x="3" y="3" width="18" height="18" rx="2"/><path d="M3 9h18M9 21V9"/></svg>
              <p class="lb-empty-title">{{ progress && progress.message ? progress.message : '暂无项目调整数据' }}</p>
              <p class="lb-empty-desc">启用项目调整表并添加记录后，此处将自动展示开发进度。</p>
            </div>
            <template v-else>
              <!-- Completion Overview -->
              <div class="lb-card" v-if="totalTasks > 0">
                <div class="lb-card-head">
                  <h3 class="lb-card-title">
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="18" height="18"><path d="M22 11.08V12a10 10 0 11-5.93-9.14"/><polyline points="22 4 12 14.01 9 11.01"/></svg>
                    完成概览
                  </h3>
                </div>
                <div class="lb-completion-row">
                  <div class="lb-completion-chart">
                    <canvas ref="pieRef" />
                  </div>
                  <div class="lb-completion-info">
                    <div class="lb-completion-bar-wrap">
                      <div class="lb-completion-bar-label">
                        <span>整体进度</span>
                        <span class="lb-completion-pct">{{ resolveRate != null ? resolveRate + '%' : '—' }}</span>
                      </div>
                      <div class="lb-completion-bar">
                        <div class="lb-completion-bar-fill" :style="{ width: (resolveRate || 0) + '%' }"></div>
                      </div>
                    </div>
                    <div class="lb-completion-legend">
                      <span class="lb-legend-item"><i class="lb-legend-dot lb-legend-dot--green"></i>已完成 {{ progress.resolved || 0 }}</span>
                      <span class="lb-legend-item"><i class="lb-legend-dot lb-legend-dot--gray"></i>进行中 {{ progress.unresolved || 0 }}</span>
                    </div>
                  </div>
                </div>
              </div>

              <!-- Charts Grid -->
              <div class="lb-chart-grid">
                <!-- Weekly Trend -->
                <div class="lb-card lb-card-wide" v-if="hasWeeklyData">
                  <div class="lb-card-head">
                    <h3 class="lb-card-title">
                      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="18" height="18"><polyline points="22 12 18 12 15 21 9 3 6 12 2 12"/></svg>
                      周趋势
                    </h3>
                  </div>
                  <div class="lb-chart-wrap">
                    <canvas ref="trendRef" />
                  </div>
                </div>

                <!-- Importance Distribution -->
                <div class="lb-card" v-if="importanceLabels.length && importanceCounts.some(v => v > 0)">
                  <div class="lb-card-head">
                    <h3 class="lb-card-title">
                      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="18" height="18"><path d="M4 15s1-1 4-1 5 2 8 2 4-1 4-1V3s-1 1-4 1-5-2-8-2-4 1-4 1z"/><line x1="4" y1="22" x2="4" y2="15"/></svg>
                      优先级分布
                    </h3>
                  </div>
                  <div class="lb-chart-wrap">
                    <canvas ref="barImpRef" />
                  </div>
                </div>
              </div>
            </template>
          </section>

          <!-- Feature Backlog -->
          <section v-if="showFeatures && featureRows.length" class="lb-section">
            <div class="lb-card">
              <div class="lb-card-head">
                <h3 class="lb-card-title">
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="18" height="18"><path d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2"/></svg>
                  功能清单
                </h3>
                <span class="lb-badge">{{ featureRows.length }} 项</span>
              </div>
              <div class="lb-table-wrap">
                <table class="lb-table">
                  <thead><tr><th>功能</th><th>进度</th></tr></thead>
                  <tbody>
                    <tr v-for="(row, i) in featureRows" :key="'fr-' + i">
                      <td>{{ row.featureName }}</td>
                      <td><span class="lb-pill" :class="pillCls(row.progress)">{{ row.progress }}</span></td>
                    </tr>
                  </tbody>
                </table>
              </div>
            </div>
          </section>
        </template>

        <!-- ========== Tab: 开发日报 ========== -->
        <template v-if="activeTab === 'daily'">
          <div v-if="dailyLoading" class="lb-state" style="padding:60px 32px">
            <div class="lb-spinner"></div><p>加载日报…</p>
          </div>
          <div v-else-if="dailyError" class="lb-empty-card">
            <p class="lb-empty-title">{{ dailyError }}</p>
          </div>
          <template v-else>
            <!-- Daily Summary List -->
            <div v-if="!dailyDetail" class="lb-daily-list">
              <div v-if="!dailyItems.length" class="lb-empty-card">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" width="48" height="48" style="color:#c0c4cc"><path d="M14 2H6a2 2 0 00-2 2v16a2 2 0 002 2h12a2 2 0 002-2V8z"/><polyline points="14 2 14 8 20 8"/><line x1="16" y1="13" x2="8" y2="13"/><line x1="16" y1="17" x2="8" y2="17"/></svg>
                <p class="lb-empty-title">暂无开发日报</p>
                <p class="lb-empty-desc">日报将在每日自动生成，展示前一天的代码提交与开发进展。</p>
              </div>
              <div v-for="item in dailyItems" :key="item.id" class="lb-daily-item" @click="openDailyDetail(item.id)">
                <div class="lb-daily-item-left">
                  <div class="lb-daily-date">{{ formatDailyDate(item.summaryDate) }}</div>
                  <div class="lb-daily-title">{{ item.title || '每日进展总结' }}</div>
                </div>
                <div class="lb-daily-item-right">
                  <span class="lb-daily-commits">{{ item.commitCount || 0 }} 次提交</span>
                  <span class="lb-daily-status" :class="item.status === 'success' ? 'lb-daily-status--ok' : 'lb-daily-status--fail'">
                    {{ item.status === 'success' ? '已生成' : '生成失败' }}
                  </span>
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="16" height="16" style="color:var(--lb-text-4);flex-shrink:0"><polyline points="9 18 15 12 9 6"/></svg>
                </div>
              </div>
              <!-- Pagination -->
              <div v-if="dailyTotalPages > 1" class="lb-pagination">
                <button class="lb-page-btn" :disabled="dailyPage <= 1" @click="dailyPage--; loadDailySummaries()">上一页</button>
                <span class="lb-page-info">{{ dailyPage }} / {{ dailyTotalPages }}</span>
                <button class="lb-page-btn" :disabled="dailyPage >= dailyTotalPages" @click="dailyPage++; loadDailySummaries()">下一页</button>
              </div>
            </div>

            <!-- Daily Detail -->
            <div v-else class="lb-daily-detail">
              <button class="lb-back-btn" @click="dailyDetail = null">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="16" height="16"><polyline points="15 18 9 12 15 6"/></svg>
                返回列表
              </button>
              <div class="lb-card lb-daily-content">
                <div class="lb-card-head">
                  <h3 class="lb-card-title">{{ dailyDetail.title || '每日进展总结' }}</h3>
                  <span class="lb-badge">{{ formatDailyDate(dailyDetail.summaryDate) }}</span>
                </div>
                <div class="lb-daily-meta">
                  <span>{{ dailyDetail.commitCount || 0 }} 次提交</span>
                  <span v-if="dailyDetail.model">模型: {{ dailyDetail.model }}</span>
                </div>
                <div class="lb-md-body" v-html="dailyRenderedHtml"></div>
              </div>
            </div>
          </template>
        </template>

        <!-- ========== Tab: 多维表格 ========== -->
        <template v-if="activeTab === 'table'">
          <div v-if="tableLoading" class="lb-state" style="padding:60px 32px">
            <div class="lb-spinner"></div><p>加载表格…</p>
          </div>
          <div v-else-if="tableError" class="lb-empty-card">
            <p class="lb-empty-title">{{ tableError }}</p>
          </div>
          <template v-else-if="tableColumns.length">
            <!-- Table Purpose Selector -->
            <div class="lb-table-selector" v-if="tablePurposes.length > 1">
              <button
                v-for="tp in tablePurposes"
                :key="tp.value"
                class="lb-purpose-btn"
                :class="{ 'lb-purpose-btn--active': tablePurpose === tp.value }"
                @click="switchTablePurpose(tp.value)"
              >{{ tp.label }}</button>
            </div>
            <!-- Desktop Table -->
            <div class="lb-table-outer lb-table-outer--desktop">
              <table class="lb-table lb-table--full">
                <thead>
                  <tr>
                    <th v-for="col in visibleColumns" :key="col.id">{{ col.name }}</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="(row, ri) in tableRecords" :key="'tr-' + ri">
                    <td v-for="col in visibleColumns" :key="col.id">
                      <template v-if="col.columnType === 'attachment'">
                        <div class="lb-cell-attachments">
                          <template v-if="getAttachmentUrls(row, col.id).length">
                            <img
                              v-for="(url, ai) in getAttachmentUrls(row, col.id).slice(0, 3)"
                              :key="ai"
                              :src="url"
                              class="lb-cell-thumb"
                              @click="previewImage(url)"
                            />
                            <span v-if="getAttachmentUrls(row, col.id).length > 3" class="lb-cell-more">
                              +{{ getAttachmentUrls(row, col.id).length - 3 }}
                            </span>
                          </template>
                          <span v-else class="lb-cell-empty">—</span>
                        </div>
                      </template>
                      <template v-else-if="col.columnType === 'status'">
                        <span class="lb-status-tag" :class="statusCls(getCellValue(row, col.id))">{{ getCellValue(row, col.id) || '—' }}</span>
                      </template>
                      <template v-else>
                        {{ getCellValue(row, col.id) || '—' }}
                      </template>
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>
            <!-- Mobile Card View -->
            <div class="lb-mobile-cards">
              <div v-for="(row, ri) in tableRecords" :key="'mc-' + ri" class="lb-mobile-card">
                <div v-for="col in visibleColumns" :key="col.id" class="lb-mobile-row">
                  <span class="lb-mobile-label">{{ col.name }}</span>
                  <span class="lb-mobile-value">
                    <template v-if="col.columnType === 'attachment'">
                      <div class="lb-cell-attachments">
                        <template v-if="getAttachmentUrls(row, col.id).length">
                          <img
                            v-for="(url, ai) in getAttachmentUrls(row, col.id).slice(0, 3)"
                            :key="ai"
                            :src="url"
                            class="lb-cell-thumb"
                            @click="previewImage(url)"
                          />
                        </template>
                        <span v-else>—</span>
                      </div>
                    </template>
                    <template v-else-if="col.columnType === 'status'">
                      <span class="lb-status-tag" :class="statusCls(getCellValue(row, col.id))">{{ getCellValue(row, col.id) || '—' }}</span>
                    </template>
                    <template v-else>{{ getCellValue(row, col.id) || '—' }}</template>
                  </span>
                </div>
              </div>
            </div>
            <!-- Table Pagination -->
            <div v-if="tableTotalPages > 1" class="lb-pagination">
              <button class="lb-page-btn" :disabled="tablePage <= 1" @click="tablePage--; loadTableData()">上一页</button>
              <span class="lb-page-info">{{ tablePage }} / {{ tableTotalPages }}</span>
              <button class="lb-page-btn" :disabled="tablePage >= tableTotalPages" @click="tablePage++; loadTableData()">下一页</button>
            </div>
          </template>
          <div v-else class="lb-empty-card">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" width="48" height="48" style="color:#c0c4cc"><rect x="3" y="3" width="18" height="18" rx="2"/><path d="M3 9h18M9 21V9"/></svg>
            <p class="lb-empty-title">暂无表格数据</p>
          </div>
        </template>

        <!-- ========== Tab: AI 需求录入 ========== -->
        <template v-if="activeTab === 'ai'">
          <div class="lb-card lb-card--accent">
            <div class="lb-card-head">
              <h3 class="lb-card-title">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="18" height="18"><polygon points="13 2 3 14 12 14 11 22 21 10 12 10 13 2"/></svg>
                AI 需求录入
              </h3>
            </div>
            <p class="lb-ai-desc">描述您的需求、问题或变更，AI 将自动解析并生成结构化任务草稿。支持粘贴或上传图片辅助说明。</p>
            <div class="lb-ai-split-mode" role="tablist" aria-label="AI 录入模式">
              <button
                type="button"
                role="tab"
                :aria-selected="aiSplitMode === 'single'"
                :class="{ 'lb-ai-split-tab--active': aiSplitMode === 'single' }"
                class="lb-ai-split-tab"
                @click="aiSplitMode = 'single'"
              >
                单条任务录入
              </button>
              <button
                type="button"
                role="tab"
                :aria-selected="aiSplitMode === 'multi'"
                :class="{ 'lb-ai-split-tab--active': aiSplitMode === 'multi' }"
                class="lb-ai-split-tab"
                @click="aiSplitMode = 'multi'"
              >
                智能拆分为多条任务
              </button>
            </div>
            <textarea
              v-model="aiText"
              class="lb-textarea"
              rows="4"
              placeholder="请描述需求、问题或变更…"
              @paste="onAiPaste"
            />
            <!-- Attachment Upload -->
            <div class="lb-ai-upload">
              <input ref="aiFileInput" type="file" multiple accept="image/*,video/*,.pdf,.doc,.docx,.xls,.xlsx,.ppt,.pptx,.txt" style="display:none" @change="onAiFilesSelected">
              <button type="button" class="lb-btn lb-btn--secondary" @click="triggerAiUpload">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="14" height="14"><path d="M21 15v4a2 2 0 01-2 2H5a2 2 0 01-2-2v-4"/><polyline points="17 8 12 3 7 8"/><line x1="12" y1="3" x2="12" y2="15"/></svg>
                上传附件
              </button>
              <span class="lb-ai-paste-hint">支持 Ctrl+V 粘贴图片</span>
            </div>
            <!-- Attachment List -->
            <div v-if="aiAttachments.length" class="lb-ai-att-list">
              <div v-for="att in aiAttachments" :key="att.id" class="lb-ai-att-item">
                <label class="lb-ai-att-check">
                  <input type="checkbox" :value="att.id" v-model="aiSelectedAttIds">
                  <div class="lb-ai-att-preview" :title="att.fileName">
                    <img v-if="att.isImage && att.previewUrl" :src="att.previewUrl" class="lb-ai-att-thumb" alt="">
                    <span v-else class="lb-ai-att-fileicon">{{ getFileIcon(att.fileName) }}</span>
                  </div>
                  <div class="lb-ai-att-meta">
                    <span class="lb-ai-att-name">{{ att.fileName }}</span>
                    <span class="lb-ai-att-size">{{ formatFileSize(att.fileSize) }}</span>
                  </div>
                </label>
                <button type="button" class="lb-ai-att-remove" @click.stop="removeAiAtt(att)" title="删除">×</button>
              </div>
            </div>
            <!-- Generate Button -->
            <div class="lb-ai-footer">
              <button type="button" class="lb-btn" :disabled="aiBusy || (!aiText.trim() && !aiSelectedAttIds.length)" @click="runAiPreview">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="14" height="14" v-if="!aiBusy"><polygon points="13 2 3 14 12 14 11 22 21 10 12 10 13 2"/></svg>
                {{ aiBusy ? '生成中…' : '生成草稿' }}
              </button>
            </div>
            <!-- Drafts -->
            <div v-if="aiDrafts.length" class="lb-drafts">
              <div class="lb-drafts-head">AI 草稿（{{ aiDrafts.length }} 条）</div>
              <div v-for="(d, i) in aiDrafts" :key="'d-' + i" class="lb-draft">
                <div class="lb-draft-toolbar">
                  <span class="lb-draft-index">{{ i + 1 }}</span>
                  <button
                    type="button"
                    class="lb-draft-remove"
                    title="删除此条草稿"
                    @click="removeAiDraft(i)"
                  >×</button>
                </div>
                <input
                  v-model="d.title"
                  type="text"
                  class="lb-draft-input"
                  placeholder="标题"
                >
                <textarea
                  v-model="d.description"
                  class="lb-draft-textarea"
                  rows="3"
                  placeholder="描述（可在此修改后再提交）"
                />
              </div>
              <div class="lb-drafts-foot">
                <button type="button" class="lb-btn" :disabled="aiCommitting" @click="commitAi">
                  {{ aiCommitting ? '提交中…' : '确认提交' }}
                </button>
              </div>
            </div>
          </div>
        </template>

        <!-- ===== Footer ===== -->
        <footer class="lb-footer">
          <span class="lb-footer-brand">
            <span class="lb-footer-icon" aria-hidden="true">
              <img class="lb-footer-icon-img" src="/brand-logo.svg" width="16" height="16" alt="">
            </span>
            流帮 Project
          </span>
          <span class="lb-footer-sep">·</span>
          <span>客户项目阅览看板</span>
        </footer>
      </main>
    </template>

    <!-- Image Preview Modal -->
    <div v-if="previewImageUrl" class="lb-img-modal" @click="previewImageUrl = null">
      <img :src="previewImageUrl" class="lb-img-modal-content" @click.stop>
    </div>
  </div>
</template>

<script>
import { Chart as ChartJS, registerables } from 'chart.js'
import MarkdownIt from 'markdown-it'
import { shouldCompressAsRasterImage, compressImageFile } from '@/utils/imageCompress'
import trackClickMixin from '@/mixins/trackClickMixin'

ChartJS.register(...registerables)
const md = new MarkdownIt({ html: false, linkify: true, breaks: true })

export default {
  name: 'ClientBoardView',
  mixins: [trackClickMixin],
  data () {
    return {
      loading: true,
      error: '',
      token: '',
      tenantName: '',
      projectName: '',
      repoId: '',
      showProgress: false,
      showFeatures: false,
      showAi: false,
      progress: null,
      featureRows: [],
      charts: {},
      // Tab
      activeTab: 'overview',
      // Daily
      dailyLoading: false,
      dailyError: '',
      dailyItems: [],
      dailyTotal: 0,
      dailyPage: 1,
      dailyPageSize: 10,
      dailyDetail: null,
      // Table
      tableLoading: false,
      tableError: '',
      tableColumns: [],
      tableRecords: [],
      tableTotal: 0,
      tablePage: 1,
      tablePageSize: 20,
      tablePurpose: 'issue_tracking',
      tablePurposes: [],
      // AI
      aiText: '',
      aiDrafts: [],
      aiBusy: false,
      aiCommitting: false,
      aiAttachments: [],
      aiSelectedAttIds: [],
      aiUploading: false,
      /** 与协作端多维表一致：single 仅保留首条草稿，multi 允许多条 */
      aiSplitMode: 'multi',
      // Image preview
      previewImageUrl: ''
    }
  },
  computed: {
    apiBase () {
      return (this.$http && this.$http.defaults && this.$http.defaults.baseURL) ? this.$http.defaults.baseURL : '/api'
    },
    visibleTabs () {
      const tabs = [
        { key: 'overview', label: '项目概览', icon: '<rect x="3" y="3" width="7" height="7"/><rect x="14" y="3" width="7" height="7"/><rect x="14" y="14" width="7" height="7"/><rect x="3" y="14" width="7" height="7"/>' }
      ]
      tabs.push({ key: 'daily', label: '开发日报', icon: '<path d="M14 2H6a2 2 0 00-2 2v16a2 2 0 002 2h12a2 2 0 002-2V8z"/><polyline points="14 2 14 8 20 8"/>' })
      tabs.push({ key: 'table', label: '多维表格', icon: '<rect x="3" y="3" width="18" height="18" rx="2"/><path d="M3 9h18M9 21V9"/>' })
      if (this.showAi) {
        tabs.push({ key: 'ai', label: 'AI 录入', icon: '<polygon points="13 2 3 14 12 14 11 22 21 10 12 10 13 2"/>' })
      }
      return tabs
    },
    resolveRate () {
      const p = this.progress
      if (!p) return null
      const t = (p.resolved || 0) + (p.unresolved || 0)
      if (t === 0) return null
      return Math.round((p.resolved || 0) / t * 100)
    },
    totalTasks () {
      return (this.progress && this.progress.totalRecords) || 0
    },
    weekLabels () { return (this.progress && this.progress.weekLabels) || [] },
    weeklyCreated () { return (this.progress && this.progress.weeklyCreated) || [] },
    weeklyResolved () { return (this.progress && this.progress.weeklyResolved) || [] },
    importanceLabels () { return (this.progress && this.progress.importanceLabels) || [] },
    importanceCounts () { return (this.progress && this.progress.importanceCounts) || [] },
    hasWeeklyData () {
      return this.weekLabels.length > 0 &&
        (this.weeklyCreated.some(v => v > 0) || this.weeklyResolved.some(v => v > 0))
    },
    dailyTotalPages () {
      return Math.max(1, Math.ceil(this.dailyTotal / this.dailyPageSize))
    },
    tableTotalPages () {
      return Math.max(1, Math.ceil(this.tableTotal / this.tablePageSize))
    },
    visibleColumns () {
      return this.tableColumns.filter(c => c.columnType !== 'multi_user')
    },
    dailyRenderedHtml () {
      if (!this.dailyDetail || !this.dailyDetail.content) return ''
      return md.render(this.dailyDetail.content)
    }
  },
  watch: {
    activeTab (val) {
      if (val === 'daily' && !this.dailyItems.length && !this.dailyError) {
        this.loadDailySummaries()
      }
      if (val === 'table' && !this.tableColumns.length && !this.tableError) {
        this.loadTableData()
      }
      if (val === 'overview') {
        this.$nextTick(() => this.renderCharts())
      }
    }
  },
  mounted () {
    this.token = String(this.$route.params.token || '').trim()
    if (!this.token) { this.error = '无效的看板链接'; this.loading = false; return }
    this.fetchBoard()
  },
  beforeDestroy () {
    this.destroyCharts()
    this.revokeAiAttachmentBlobs()
  },
  methods: {
    // ===== Board Init =====
    async fetchBoard () {
      this.loading = true; this.error = ''
      try {
        const resp = await this.$http.get(`/public/client-board/${encodeURIComponent(this.token)}`)
        if (!resp.data || resp.data.code !== 0 || !resp.data.data) {
          this.error = (resp.data && resp.data.message) || '加载失败'
          return
        }
        const d = resp.data.data
        this.tenantName = d.tenantName || ''
        this.projectName = d.projectName || ''
        this.repoId = d.repoId || ''
        this.showProgress = !!d.showProgressDashboard
        this.showFeatures = !!d.showFeatureBacklog
        this.showAi = !!d.showAiTableEntry
        this.progress = d.progress || null
        this.featureRows = Array.isArray(d.featureSummary) ? d.featureSummary : []
        // Build table purposes
        const purposes = []
        if (this.showProgress) purposes.push({ value: 'issue_tracking', label: '项目调整' })
        if (this.showFeatures) purposes.push({ value: 'feature_backlog', label: '需求 Backlog' })
        this.tablePurposes = purposes
      } catch (e) { this.error = '加载失败，请稍后重试' }
      finally {
        this.loading = false
        // 须在 loading=false 之后渲染：首屏时 v-if="loading" 会卸载主内容，若在仍 loading 时 $nextTick，
        // canvas 尚未挂载，$refs.pieRef 等为 undefined，Chart 不会创建；切换 tab 会再次 renderCharts 才正常。
        if (!this.error) {
          this.$nextTick(() => {
            this.$nextTick(() => this.renderCharts())
          })
        }
      }
    },

    // ===== Charts =====
    destroyCharts () {
      Object.values(this.charts).forEach(c => { if (c && typeof c.destroy === 'function') c.destroy() })
      this.charts = {}
    },
    renderCharts () {
      this.destroyCharts()
      const p = this.progress
      if (!p || p.empty || !this.showProgress) return
      const resolved = Number(p.resolved) || 0
      const unresolved = Number(p.unresolved) || 0
      const total = resolved + unresolved

      // Pie
      if (this.$refs.pieRef && total > 0) {
        this.charts.pie = new ChartJS(this.$refs.pieRef, {
          type: 'doughnut',
          data: {
            labels: ['已完成', '进行中'],
            datasets: [{ data: [resolved, unresolved], backgroundColor: ['#3370ff', '#e5e6eb'], borderWidth: 0, hoverOffset: 4 }]
          },
          options: { responsive: true, maintainAspectRatio: false, cutout: '72%', plugins: { legend: { display: false } } }
        })
      }

      // Trend
      if (this.$refs.trendRef && this.hasWeeklyData && this.weekLabels.length) {
        this.charts.trend = new ChartJS(this.$refs.trendRef, {
          type: 'line',
          data: {
            labels: this.weekLabels.map(l => l.slice(5)),
            datasets: [
              { label: '新建', data: this.weeklyCreated, borderColor: '#3370ff', backgroundColor: 'rgba(51,112,255,0.06)', fill: true, tension: 0.35, borderWidth: 2, pointRadius: 3, pointHoverRadius: 5 },
              { label: '完成', data: this.weeklyResolved, borderColor: '#00b42a', backgroundColor: 'rgba(0,180,42,0.06)', fill: true, tension: 0.35, borderWidth: 2, pointRadius: 3, pointHoverRadius: 5 }
            ]
          },
          options: {
            responsive: true, maintainAspectRatio: false,
            interaction: { mode: 'index', intersect: false },
            plugins: { legend: { position: 'top', align: 'end', labels: { boxWidth: 12, boxHeight: 2, padding: 16, font: { size: 12 } } } },
            scales: {
              y: { beginAtZero: true, grid: { color: '#f2f3f5' }, ticks: { font: { size: 11 }, precision: 0 } },
              x: { grid: { display: false }, ticks: { font: { size: 11 } } }
            }
          }
        })
      }

      // Importance
      if (this.$refs.barImpRef && this.importanceLabels.length && this.importanceCounts.some(v => v > 0)) {
        const colors = ['#f54a45', '#ff8800', '#ffcd00', '#3370ff', '#8f959e']
        this.charts.bar = new ChartJS(this.$refs.barImpRef, {
          type: 'bar',
          data: {
            labels: this.importanceLabels,
            datasets: [{ data: this.importanceCounts, backgroundColor: this.importanceLabels.map((_, i) => colors[i % colors.length]), borderRadius: 6, barPercentage: 0.5 }]
          },
          options: {
            responsive: true, maintainAspectRatio: false, indexAxis: 'y',
            plugins: { legend: { display: false } },
            scales: {
              x: { beginAtZero: true, grid: { color: '#f2f3f5' }, ticks: { stepSize: 1, font: { size: 11 } } },
              y: { grid: { display: false }, ticks: { font: { size: 12 } } }
            }
          }
        })
      }
    },

    // ===== Daily Summaries =====
    async loadDailySummaries () {
      this.dailyLoading = true; this.dailyError = ''
      try {
        const resp = await this.$http.get(`/public/client-board/${encodeURIComponent(this.token)}/daily-summaries`, { params: { page: this.dailyPage, pageSize: this.dailyPageSize } })
        if (resp.data && resp.data.code === 0 && resp.data.data) {
          const d = resp.data.data
          this.dailyItems = d.items || []
          this.dailyTotal = d.total || 0
        } else {
          this.dailyError = (resp.data && resp.data.message) || '加载日报失败'
        }
      } catch (e) { this.dailyError = '加载日报失败，请稍后重试' }
      finally { this.dailyLoading = false }
    },
    async openDailyDetail (id) {
      try {
        const resp = await this.$http.get(`/public/client-board/${encodeURIComponent(this.token)}/daily-summaries/${id}`)
        if (resp.data && resp.data.code === 0 && resp.data.data) {
          this.dailyDetail = resp.data.data
        } else {
          window.alert((resp.data && resp.data.message) || '加载详情失败')
        }
      } catch (e) { window.alert('加载详情失败，请稍后重试') }
    },
    formatDailyDate (d) {
      if (!d) return ''
      return String(d).slice(0, 10)
    },

    // ===== Table Data =====
    async loadTableData () {
      this.tableLoading = true; this.tableError = ''
      try {
        const resp = await this.$http.get(`/public/client-board/${encodeURIComponent(this.token)}/table-data`, {
          params: { purpose: this.tablePurpose, page: this.tablePage, pageSize: this.tablePageSize }
        })
        if (resp.data && resp.data.code === 0 && resp.data.data) {
          const d = resp.data.data
          this.tableColumns = d.columns || []
          this.tableRecords = d.records || []
          this.tableTotal = d.total || 0
        } else {
          this.tableError = (resp.data && resp.data.message) || '加载表格失败'
        }
      } catch (e) { this.tableError = '加载表格失败，请稍后重试' }
      finally { this.tableLoading = false }
    },
    switchTablePurpose (purpose) {
      this.tablePurpose = purpose
      this.tablePage = 1
      this.loadTableData()
    },
    getCellValue (row, colId) {
      const v = row['c' + colId]
      if (v == null) return ''
      return String(v)
    },
    publicAttachmentPreviewUrl (attachmentId) {
      const id = Number(attachmentId)
      if (Number.isNaN(id)) return ''
      return `${this.apiBase}/public/client-board/${encodeURIComponent(this.token)}/attachments/${id}/preview`
    },
    getAttachmentUrls (row, colId) {
      const v = row['c' + colId]
      if (!v) return []
      try {
        const arr = typeof v === 'string' ? JSON.parse(v) : v
        if (!Array.isArray(arr)) return []
        return arr.map(a => {
          if (typeof a === 'number') return this.publicAttachmentPreviewUrl(a)
          if (a && a.id) return this.publicAttachmentPreviewUrl(a.id)
          return ''
        }).filter(Boolean)
      } catch (e) { return [] }
    },
    previewImage (url) {
      this.previewImageUrl = url
    },

    // ===== AI Entry =====
    triggerAiUpload () {
      if (this.$refs.aiFileInput) this.$refs.aiFileInput.click()
    },
    async onAiFilesSelected (e) {
      const files = Array.from(e.target.files || [])
      if (files.length) await this.uploadAiFiles(files)
      e.target.value = ''
    },
    async onAiPaste (e) {
      const items = (e.clipboardData || {}).items || []
      const imageFiles = []
      for (const item of items) {
        if (item.type && item.type.startsWith('image/')) {
          const f = item.getAsFile()
          if (f) imageFiles.push(f)
        }
      }
      if (imageFiles.length) {
        e.preventDefault()
        await this.uploadAiFiles(imageFiles)
      }
    },
    revokeAiAttachmentBlobs () {
      this.aiAttachments.forEach(a => {
        if (a && a.previewUrl && String(a.previewUrl).startsWith('blob:')) {
          try { URL.revokeObjectURL(a.previewUrl) } catch (e) { /* ignore */ }
        }
      })
    },
    async uploadAiFiles (files) {
      if (this.aiUploading) return
      this.aiUploading = true
      try {
        for (const file of files) {
          let uploadFile = file
          if (shouldCompressAsRasterImage(file)) {
            try {
              uploadFile = await compressImageFile(file, { maxWidth: 1920, maxHeight: 1920, quality: 0.82 })
            } catch (_err) { /* 使用原文件 */ }
          }
          const fd = new FormData()
          fd.append('file', uploadFile)
          const resp = await this.$http.post(`/public/client-board/${encodeURIComponent(this.token)}/attachments`, fd, {
            headers: { 'Content-Type': 'multipart/form-data' }
          })
          if (resp.data && resp.data.code === 0 && resp.data.data) {
            const att = resp.data.data
            const isImg = !!att.isImage
            this.aiAttachments.push({
              id: att.id,
              fileName: att.fileName || 'file',
              fileSize: att.fileSize || 0,
              isImage: isImg,
              previewUrl: isImg ? URL.createObjectURL(uploadFile) : null
            })
            this.aiSelectedAttIds.push(att.id)
          }
        }
      } catch (e) { window.alert('上传失败，请稍后重试') }
      finally { this.aiUploading = false }
    },
    removeAiAtt (att) {
      if (att && att.previewUrl && String(att.previewUrl).startsWith('blob:')) {
        try { URL.revokeObjectURL(att.previewUrl) } catch (e) { /* ignore */ }
      }
      const idx = this.aiAttachments.indexOf(att)
      if (idx >= 0) this.aiAttachments.splice(idx, 1)
      const sidIdx = this.aiSelectedAttIds.indexOf(att.id)
      if (sidIdx >= 0) this.aiSelectedAttIds.splice(sidIdx, 1)
    },
    getFileIcon (name) {
      if (!name) return '📄'
      const n = name.toLowerCase()
      if (n.endsWith('.pdf')) return '📕'
      if (n.endsWith('.doc') || n.endsWith('.docx')) return '📘'
      if (n.endsWith('.xls') || n.endsWith('.xlsx')) return '📗'
      if (n.endsWith('.ppt') || n.endsWith('.pptx')) return '📙'
      if (n.endsWith('.txt')) return '📄'
      if (n.endsWith('.mp4') || n.endsWith('.mov')) return '🎬'
      return '📎'
    },
    formatFileSize (bytes) {
      if (!bytes) return '0 B'
      if (bytes < 1024) return bytes + ' B'
      if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
      return (bytes / (1024 * 1024)).toFixed(1) + ' MB'
    },
    removeAiDraft (idx) {
      if (idx < 0 || idx >= this.aiDrafts.length) return
      this.aiDrafts.splice(idx, 1)
    },
    async runAiPreview () {
      this.$trackClick('client_board.ai.generate_draft')
      const raw = (this.aiText || '').trim()
      if (!raw && !this.aiSelectedAttIds.length) return
      this.aiBusy = true; this.aiDrafts = []
      try {
        const resp = await this.$http.post(`/public/client-board/${encodeURIComponent(this.token)}/ai-preview`, {
          rawText: raw,
          attachmentIds: this.aiSelectedAttIds
        })
        if (resp.data && resp.data.code === 0 && resp.data.data && Array.isArray(resp.data.data.items)) {
          let items = resp.data.data.items || []
          if (Array.isArray(items) && this.aiSelectedAttIds && this.aiSelectedAttIds.length) {
            const baseIds = Array.from(new Set(this.aiSelectedAttIds.map(Number).filter(n => !Number.isNaN(n))))
            items.forEach(t => {
              if (!t) return
              const existing = Array.isArray(t.attachmentIds) ? t.attachmentIds : []
              const merged = Array.from(new Set([...existing, ...baseIds].map(Number).filter(n => !Number.isNaN(n))))
              t.attachmentIds = merged
            })
          }
          if (this.aiSplitMode === 'single' && Array.isArray(items) && items.length > 1) {
            items = [items[0]]
          }
          this.aiDrafts = items
        } else { window.alert((resp.data && resp.data.message) || 'AI 预览失败') }
      } catch (e) { window.alert('请求失败，请稍后重试') }
      finally { this.aiBusy = false }
    },
    async commitAi () {
      this.$trackClick('client_board.ai.commit')
      if (!this.aiDrafts.length) return
      this.aiCommitting = true
      try {
        const resp = await this.$http.post(`/public/client-board/${encodeURIComponent(this.token)}/ai-commit`, { tasks: this.aiDrafts })
        if (resp.data && resp.data.code === 0) {
          const n = resp.data.data && resp.data.data.createdCount != null ? resp.data.data.createdCount : 0
          window.alert('提交成功，已创建 ' + n + ' 条任务')
          this.revokeAiAttachmentBlobs()
          this.aiDrafts = []; this.aiText = ''; this.aiAttachments = []; this.aiSelectedAttIds = []
          await this.fetchBoard()
        } else { window.alert((resp.data && resp.data.message) || '提交失败') }
      } catch (e) { window.alert('请求失败，请稍后重试') }
      finally { this.aiCommitting = false }
    },

    // ===== Helpers =====
    pillCls (p) {
      if (!p) return ''
      const v = String(p)
      if (v.includes('100') || v.includes('完成') || v.includes('已上线')) return 'lb-pill--done'
      if (v.includes('进行') || v.includes('开发')) return 'lb-pill--wip'
      if (v.includes('待') || v.includes('规划') || v.includes('0')) return 'lb-pill--todo'
      return ''
    },
    statusCls (v) {
      if (!v) return ''
      const s = String(v)
      if (s.includes('已解决') || s.includes('已完成') || s.includes('通过') || s.includes('关闭')) return 'lb-status-tag--done'
      if (s.includes('进行') || s.includes('开发') || s.includes('处理')) return 'lb-status-tag--wip'
      return ''
    }
  }
}
</script>

<style scoped>
/* ================================================================
   Client Board — 字节系设计风格 v2 (Tab-based layout)
   ================================================================ */

/* --- Design Tokens --- */
.lb-root {
  --lb-blue: #3370ff;
  --lb-blue-light: #e8f0ff;
  --lb-blue-hover: #2860e1;
  --lb-green: #00b42a;
  --lb-green-light: #e8ffea;
  --lb-orange: #ff8800;
  --lb-orange-light: #fff3e8;
  --lb-red: #f54a45;
  --lb-bg: #f5f6f7;
  --lb-bg-card: #ffffff;
  --lb-border: #e5e6eb;
  --lb-text-1: #1f2329;
  --lb-text-2: #646a73;
  --lb-text-3: #8f959e;
  --lb-text-4: #c0c4cc;
  --lb-radius: 8px;
  --lb-radius-lg: 12px;
  --lb-shadow: 0 1px 4px rgba(0,0,0,0.04);
  --lb-shadow-hover: 0 4px 16px rgba(0,0,0,0.06);
  min-height: 100vh;
  background: var(--lb-bg);
  color: var(--lb-text-1);
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', 'PingFang SC', 'Hiragino Sans GB', 'Microsoft YaHei', sans-serif;
  -webkit-font-smoothing: antialiased;
}

/* --- Loading / Error --- */
.lb-state { padding: 120px 32px; text-align: center; display: flex; flex-direction: column; align-items: center; gap: 16px; color: var(--lb-text-2); font-size: 14px; }
.lb-err { color: var(--lb-red); }
.lb-err svg { color: var(--lb-red); opacity: 0.5; }
.lb-spinner { width: 28px; height: 28px; border: 2.5px solid var(--lb-border); border-top-color: var(--lb-blue); border-radius: 50%; animation: lb-spin .7s linear infinite; }
@keyframes lb-spin { to { transform: rotate(360deg); } }

/* --- Top Nav --- */
.lb-nav { background: var(--lb-bg-card); border-bottom: 1px solid var(--lb-border); position: sticky; top: 0; z-index: 100; }
.lb-nav-inner { max-width: 1120px; margin: 0 auto; padding: 0 32px; height: 52px; display: flex; align-items: center; justify-content: space-between; }
.lb-nav-left { display: flex; align-items: center; gap: 10px; }
.lb-nav-brand { text-decoration: none; color: inherit; cursor: pointer; border-radius: 8px; padding: 2px 4px; margin: -2px -4px; transition: background .15s; }
.lb-nav-brand:hover { background: rgba(51, 112, 255, 0.08); }
.lb-nav-brand:focus-visible { outline: 2px solid var(--lb-blue); outline-offset: 2px; }
.lb-nav-logo { width: 28px; height: 28px; border-radius: 6px; display: flex; align-items: center; justify-content: center; overflow: hidden; flex-shrink: 0; box-shadow: 0 1px 2px rgba(0,0,0,.08); }
.lb-nav-logo-img { width: 100%; height: 100%; display: block; object-fit: cover; }
.lb-nav-title { font-size: 15px; font-weight: 600; color: var(--lb-text-1); }
.lb-nav-tenant { font-size: 13px; color: var(--lb-text-2); padding: 4px 12px; background: var(--lb-bg); border-radius: 100px; }

/* --- Hero --- */
.lb-hero { background: var(--lb-bg-card); border-bottom: 1px solid var(--lb-border); }
.lb-hero-inner { max-width: 1120px; margin: 0 auto; padding: 20px 32px 16px; }
.lb-hero-meta { display: flex; align-items: center; gap: 12px; margin-bottom: 8px; }
.lb-tag { display: inline-flex; align-items: center; padding: 2px 8px; background: var(--lb-blue-light); color: var(--lb-blue); font-size: 12px; font-weight: 500; border-radius: 4px; }
.lb-hero-title { margin: 0 0 6px; font-size: 22px; font-weight: 700; color: var(--lb-text-1); line-height: 1.3; }
.lb-hero-repo { margin: 0; font-size: 13px; color: var(--lb-text-2); font-family: 'SF Mono', Menlo, Monaco, Consolas, monospace; display: inline-flex; align-items: center; gap: 6px; padding: 3px 10px; background: var(--lb-bg); border-radius: 4px; }

/* --- Tab Bar --- */
.lb-tabs-bar { background: var(--lb-bg-card); border-bottom: 1px solid var(--lb-border); position: sticky; top: 52px; z-index: 99; overflow-x: auto; -webkit-overflow-scrolling: touch; }
.lb-tabs-bar::-webkit-scrollbar { display: none; }
.lb-tabs-inner { max-width: 1120px; margin: 0 auto; padding: 0 32px; display: flex; gap: 0; }
.lb-tab { display: inline-flex; align-items: center; gap: 6px; padding: 12px 20px; font-size: 14px; font-weight: 500; color: var(--lb-text-2); background: none; border: none; border-bottom: 2px solid transparent; cursor: pointer; white-space: nowrap; transition: color .15s, border-color .15s; outline: none; }
.lb-tab:hover { color: var(--lb-text-1); }
.lb-tab--active { color: var(--lb-blue); border-bottom-color: var(--lb-blue); }
.lb-tab svg { flex-shrink: 0; }

/* --- Main --- */
.lb-main { max-width: 1120px; margin: 0 auto; padding: 20px 32px 48px; }
.lb-section { margin-bottom: 20px; }

/* --- KPI Row --- */
.lb-kpi-row { display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px; margin-bottom: 20px; }
.lb-kpi { background: var(--lb-bg-card); border-radius: var(--lb-radius-lg); border: 1px solid var(--lb-border); padding: 20px; display: flex; align-items: center; gap: 16px; box-shadow: var(--lb-shadow); transition: box-shadow .2s; }
.lb-kpi:hover { box-shadow: var(--lb-shadow-hover); }
.lb-kpi-icon { width: 44px; height: 44px; border-radius: 10px; display: flex; align-items: center; justify-content: center; flex-shrink: 0; }
.lb-kpi-icon--total { background: var(--lb-blue-light); color: var(--lb-blue); }
.lb-kpi-icon--done { background: var(--lb-green-light); color: var(--lb-green); }
.lb-kpi-icon--open { background: var(--lb-orange-light); color: var(--lb-orange); }
.lb-kpi-icon--rate { background: #f3e8ff; color: #7c3aed; }
.lb-kpi-body { display: flex; flex-direction: column; gap: 2px; }
.lb-kpi-value { font-size: 28px; font-weight: 700; color: var(--lb-text-1); line-height: 1.1; }
.lb-kpi-value--green { color: var(--lb-green); }
.lb-kpi-value--orange { color: var(--lb-orange); }
.lb-kpi-value--blue { color: var(--lb-blue); }
.lb-kpi-label { font-size: 13px; color: var(--lb-text-2); font-weight: 400; }

/* --- Card --- */
.lb-card { background: var(--lb-bg-card); border-radius: var(--lb-radius-lg); border: 1px solid var(--lb-border); padding: 20px 24px; box-shadow: var(--lb-shadow); transition: box-shadow .2s; }
.lb-card:hover { box-shadow: var(--lb-shadow-hover); }
.lb-card--accent { border-left: 3px solid var(--lb-blue); }
.lb-card-head { display: flex; align-items: center; justify-content: space-between; margin-bottom: 16px; }
.lb-card-title { margin: 0; font-size: 15px; font-weight: 600; color: var(--lb-text-1); display: flex; align-items: center; gap: 8px; }
.lb-card-title svg { color: var(--lb-blue); flex-shrink: 0; }
.lb-badge { font-size: 12px; color: var(--lb-text-2); background: var(--lb-bg); padding: 2px 10px; border-radius: 100px; }

/* --- Completion Row --- */
.lb-completion-row { display: flex; align-items: center; gap: 32px; }
.lb-completion-chart { width: 140px; height: 140px; flex-shrink: 0; position: relative; }
.lb-completion-info { flex: 1; display: flex; flex-direction: column; gap: 16px; }
.lb-completion-bar-wrap { display: flex; flex-direction: column; gap: 8px; }
.lb-completion-bar-label { display: flex; justify-content: space-between; font-size: 13px; color: var(--lb-text-2); }
.lb-completion-pct { font-weight: 600; color: var(--lb-blue); }
.lb-completion-bar { height: 8px; background: var(--lb-bg); border-radius: 100px; overflow: hidden; }
.lb-completion-bar-fill { height: 100%; background: var(--lb-blue); border-radius: 100px; transition: width .6s ease; }
.lb-completion-legend { display: flex; gap: 20px; }
.lb-legend-item { display: flex; align-items: center; gap: 6px; font-size: 13px; color: var(--lb-text-2); }
.lb-legend-dot { width: 8px; height: 8px; border-radius: 50%; display: inline-block; }
.lb-legend-dot--green { background: var(--lb-green); }
.lb-legend-dot--gray { background: var(--lb-border); }

/* --- Chart Grid --- */
.lb-chart-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; margin-top: 16px; }
.lb-card-wide { grid-column: span 2; }
.lb-chart-wrap { height: 220px; position: relative; }

/* --- Empty State --- */
.lb-empty-card { text-align: center; padding: 56px 24px; background: var(--lb-bg-card); border-radius: var(--lb-radius-lg); border: 1px solid var(--lb-border); }
.lb-empty-card svg { opacity: .35; margin-bottom: 12px; }
.lb-empty-title { margin: 0 0 8px; font-size: 15px; font-weight: 500; color: var(--lb-text-2); }
.lb-empty-desc { margin: 0; font-size: 13px; color: var(--lb-text-3); line-height: 1.6; }

/* --- Table --- */
.lb-table-wrap { overflow-x: auto; }
.lb-table { width: 100%; border-collapse: collapse; font-size: 13px; }
.lb-table th, .lb-table td { border-bottom: 1px solid var(--lb-border); padding: 10px 14px; text-align: left; white-space: nowrap; }
.lb-table th { color: var(--lb-text-2); font-weight: 500; font-size: 12px; background: var(--lb-bg); position: sticky; top: 0; }
.lb-table tbody tr:hover { background: #fafbfc; }
.lb-table--full td { max-width: 260px; overflow: hidden; text-overflow: ellipsis; }
.lb-table-outer--desktop { overflow-x: auto; border-radius: var(--lb-radius-lg); border: 1px solid var(--lb-border); background: var(--lb-bg-card); }

/* --- Table Selector --- */
.lb-table-selector { display: flex; gap: 8px; margin-bottom: 16px; }
.lb-purpose-btn { padding: 6px 16px; border-radius: 100px; font-size: 13px; font-weight: 500; border: 1px solid var(--lb-border); background: var(--lb-bg-card); color: var(--lb-text-2); cursor: pointer; transition: all .15s; outline: none; }
.lb-purpose-btn:hover { border-color: var(--lb-blue); color: var(--lb-blue); }
.lb-purpose-btn--active { background: var(--lb-blue); color: #fff; border-color: var(--lb-blue); }

/* --- Status Tag --- */
.lb-status-tag { display: inline-block; padding: 2px 10px; border-radius: 100px; font-size: 12px; font-weight: 500; background: var(--lb-bg); color: var(--lb-text-2); }
.lb-status-tag--done { background: var(--lb-green-light); color: var(--lb-green); }
.lb-status-tag--wip { background: var(--lb-orange-light); color: var(--lb-orange); }

/* --- Cell Attachments --- */
.lb-cell-attachments { display: flex; align-items: center; gap: 6px; flex-wrap: wrap; }
.lb-cell-thumb { width: 36px; height: 36px; object-fit: cover; border-radius: 4px; cursor: pointer; border: 1px solid var(--lb-border); }
.lb-cell-thumb:hover { opacity: .8; }
.lb-cell-more { font-size: 12px; color: var(--lb-text-3); }
.lb-cell-empty { color: var(--lb-text-4); }

/* --- Mobile Cards --- */
.lb-mobile-cards { display: none; }

/* --- Image Preview Modal --- */
.lb-img-modal { position: fixed; inset: 0; background: rgba(0,0,0,0.7); z-index: 1000; display: flex; align-items: center; justify-content: center; padding: 24px; cursor: pointer; }
.lb-img-modal-content { max-width: 90vw; max-height: 90vh; border-radius: 8px; object-fit: contain; cursor: default; }

/* --- Pill --- */
.lb-pill { display: inline-block; padding: 2px 10px; border-radius: 100px; font-size: 12px; font-weight: 500; background: var(--lb-bg); color: var(--lb-text-2); }
.lb-pill--done { background: var(--lb-green-light); color: var(--lb-green); }
.lb-pill--wip { background: var(--lb-orange-light); color: var(--lb-orange); }
.lb-pill--todo { background: var(--lb-blue-light); color: var(--lb-blue); }

/* --- Daily Summary --- */
.lb-daily-list { display: flex; flex-direction: column; gap: 8px; }
.lb-daily-item { background: var(--lb-bg-card); border: 1px solid var(--lb-border); border-radius: var(--lb-radius-lg); padding: 16px 20px; display: flex; align-items: center; justify-content: space-between; cursor: pointer; transition: box-shadow .15s, border-color .15s; box-shadow: var(--lb-shadow); }
.lb-daily-item:hover { box-shadow: var(--lb-shadow-hover); border-color: var(--lb-blue); }
.lb-daily-item-left { display: flex; flex-direction: column; gap: 4px; min-width: 0; }
.lb-daily-date { font-size: 12px; color: var(--lb-text-3); font-family: 'SF Mono', Menlo, Monaco, Consolas, monospace; }
.lb-daily-title { font-size: 14px; font-weight: 600; color: var(--lb-text-1); overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.lb-daily-item-right { display: flex; align-items: center; gap: 12px; flex-shrink: 0; }
.lb-daily-commits { font-size: 12px; color: var(--lb-text-3); }
.lb-daily-status { font-size: 11px; padding: 2px 8px; border-radius: 100px; font-weight: 500; }
.lb-daily-status--ok { background: var(--lb-green-light); color: var(--lb-green); }
.lb-daily-status--fail { background: #fdecee; color: var(--lb-red); }
.lb-daily-detail { }
.lb-back-btn { display: inline-flex; align-items: center; gap: 4px; padding: 6px 12px; font-size: 13px; color: var(--lb-blue); background: none; border: none; cursor: pointer; border-radius: var(--lb-radius); transition: background .15s; margin-bottom: 16px; }
.lb-back-btn:hover { background: var(--lb-blue-light); }
.lb-daily-content { }
.lb-daily-meta { display: flex; gap: 16px; font-size: 12px; color: var(--lb-text-3); margin-bottom: 16px; }
.lb-md-body { font-size: 14px; line-height: 1.8; color: var(--lb-text-1); }
.lb-md-body >>> h1, .lb-md-body >>> h2, .lb-md-body >>> h3 { margin: 20px 0 8px; font-weight: 600; }
.lb-md-body >>> h1 { font-size: 18px; }
.lb-md-body >>> h2 { font-size: 16px; }
.lb-md-body >>> h3 { font-size: 15px; }
.lb-md-body >>> p { margin: 8px 0; }
.lb-md-body >>> ul, .lb-md-body >>> ol { padding-left: 20px; margin: 8px 0; }
.lb-md-body >>> li { margin: 4px 0; }
.lb-md-body >>> code { background: var(--lb-bg); padding: 2px 6px; border-radius: 4px; font-size: 13px; font-family: 'SF Mono', Menlo, Monaco, Consolas, monospace; }
.lb-md-body >>> pre { background: var(--lb-bg); padding: 12px 16px; border-radius: var(--lb-radius); overflow-x: auto; margin: 12px 0; }
.lb-md-body >>> pre code { background: none; padding: 0; }
.lb-md-body >>> strong { font-weight: 600; }
.lb-md-body >>> blockquote { border-left: 3px solid var(--lb-blue); padding-left: 12px; margin: 12px 0; color: var(--lb-text-2); }

/* --- Pagination --- */
.lb-pagination { display: flex; align-items: center; justify-content: center; gap: 16px; margin-top: 20px; }
.lb-page-btn { padding: 6px 16px; font-size: 13px; border: 1px solid var(--lb-border); background: var(--lb-bg-card); color: var(--lb-text-2); border-radius: var(--lb-radius); cursor: pointer; transition: all .15s; outline: none; }
.lb-page-btn:hover:not(:disabled) { border-color: var(--lb-blue); color: var(--lb-blue); }
.lb-page-btn:disabled { opacity: .4; cursor: not-allowed; }
.lb-page-info { font-size: 13px; color: var(--lb-text-3); }

/* --- AI Section --- */
.lb-ai-split-mode { display: flex; flex-wrap: wrap; gap: 8px; margin-bottom: 12px; }
.lb-ai-split-tab { padding: 6px 12px; border: 1px solid var(--lb-border); border-radius: 8px; background: var(--lb-bg-card); color: var(--lb-text-2); font-size: 13px; cursor: pointer; transition: border-color .15s, background .15s, color .15s; }
.lb-ai-split-tab:hover { background: var(--lb-bg); color: var(--lb-text-1); }
.lb-ai-split-tab--active { border-color: var(--lb-blue); background: var(--lb-blue-light); color: var(--lb-blue); font-weight: 500; }
.lb-ai-desc { margin: 0 0 12px; font-size: 13px; color: var(--lb-text-3); line-height: 1.6; }
.lb-textarea { width: 100%; border: 1px solid var(--lb-border); border-radius: var(--lb-radius); padding: 10px 14px; font-size: 14px; font-family: inherit; color: var(--lb-text-1); background: var(--lb-bg-card); resize: vertical; box-sizing: border-box; outline: none; transition: border-color .2s, box-shadow .2s; line-height: 1.6; }
.lb-textarea:focus { border-color: var(--lb-blue); box-shadow: 0 0 0 2px rgba(51,112,255,.12); }
.lb-ai-upload { display: flex; align-items: center; gap: 12px; margin-top: 12px; }
.lb-ai-paste-hint { font-size: 12px; color: var(--lb-text-4); }
.lb-ai-att-list { margin-top: 12px; display: flex; flex-direction: column; gap: 8px; }
.lb-ai-att-item { display: flex; align-items: center; justify-content: space-between; padding: 8px 12px; background: var(--lb-bg); border-radius: var(--lb-radius); border: 1px solid transparent; transition: border-color .15s; }
.lb-ai-att-item:hover { border-color: var(--lb-border); }
.lb-ai-att-check { display: flex; align-items: center; gap: 10px; cursor: pointer; flex: 1; min-width: 0; }
.lb-ai-att-check input[type="checkbox"] { flex-shrink: 0; width: 16px; height: 16px; accent-color: var(--lb-blue); }
.lb-ai-att-preview { width: 36px; height: 36px; border-radius: 4px; overflow: hidden; flex-shrink: 0; display: flex; align-items: center; justify-content: center; background: var(--lb-bg-card); border: 1px solid var(--lb-border); }
.lb-ai-att-thumb { width: 100%; height: 100%; object-fit: cover; }
.lb-ai-att-fileicon { font-size: 16px; }
.lb-ai-att-meta { display: flex; flex-direction: column; gap: 2px; min-width: 0; }
.lb-ai-att-name { font-size: 13px; color: var(--lb-text-1); overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.lb-ai-att-size { font-size: 11px; color: var(--lb-text-4); }
.lb-ai-att-remove { width: 24px; height: 24px; display: flex; align-items: center; justify-content: center; border: none; background: none; color: var(--lb-text-4); cursor: pointer; border-radius: 4px; font-size: 16px; flex-shrink: 0; }
.lb-ai-att-remove:hover { background: #fdecee; color: var(--lb-red); }
.lb-ai-footer { margin-top: 16px; }

/* --- AI Drafts --- */
.lb-drafts { margin-top: 16px; padding-top: 16px; border-top: 1px solid var(--lb-border); }
.lb-drafts-head { font-size: 13px; font-weight: 600; color: var(--lb-text-1); margin-bottom: 12px; }
.lb-draft { margin-bottom: 8px; padding: 12px 14px; background: var(--lb-bg); border-radius: var(--lb-radius); border: 1px solid transparent; transition: border-color .15s; }
.lb-draft:hover { border-color: var(--lb-blue); }
.lb-draft-toolbar { display: flex; align-items: center; justify-content: space-between; gap: 8px; margin-bottom: 8px; }
.lb-draft-index { font-size: 12px; font-weight: 600; color: var(--lb-text-3); }
.lb-draft-remove { width: 28px; height: 28px; display: inline-flex; align-items: center; justify-content: center; border: none; background: transparent; color: var(--lb-text-4); border-radius: 6px; font-size: 18px; line-height: 1; cursor: pointer; flex-shrink: 0; }
.lb-draft-remove:hover { background: #fdecee; color: var(--lb-red); }
.lb-draft-input { width: 100%; box-sizing: border-box; padding: 8px 10px; border: 1px solid var(--lb-border); border-radius: 8px; font-size: 14px; font-weight: 600; color: var(--lb-text-1); background: var(--lb-bg-card); margin-bottom: 8px; outline: none; transition: border-color .15s, box-shadow .15s; }
.lb-draft-input:focus { border-color: var(--lb-blue); box-shadow: 0 0 0 2px rgba(51,112,255,.12); }
.lb-draft-textarea { width: 100%; box-sizing: border-box; padding: 8px 10px; border: 1px solid var(--lb-border); border-radius: 8px; font-size: 13px; color: var(--lb-text-2); background: var(--lb-bg-card); resize: vertical; min-height: 72px; line-height: 1.6; white-space: pre-wrap; font-family: inherit; outline: none; transition: border-color .15s, box-shadow .15s; }
.lb-draft-textarea:focus { border-color: var(--lb-blue); box-shadow: 0 0 0 2px rgba(51,112,255,.12); }
.lb-drafts-foot { margin-top: 12px; display: flex; justify-content: flex-end; }

/* --- Button --- */
.lb-btn { display: inline-flex; align-items: center; justify-content: center; gap: 6px; min-height: 32px; padding: 0 16px; border-radius: var(--lb-radius); font-size: 14px; font-weight: 500; cursor: pointer; border: none; background: var(--lb-blue); color: #fff; transition: background .15s; outline: none; line-height: 1; }
.lb-btn:hover { background: var(--lb-blue-hover); }
.lb-btn:active { background: #1d56d0; }
.lb-btn:disabled { opacity: .4; cursor: not-allowed; }
.lb-btn:focus-visible { box-shadow: 0 0 0 2px rgba(51,112,255,.25); }
.lb-btn--secondary { background: var(--lb-bg-card); color: var(--lb-text-2); border: 1px solid var(--lb-border); }
.lb-btn--secondary:hover { border-color: var(--lb-blue); color: var(--lb-blue); }

/* --- Footer --- */
.lb-footer { text-align: center; padding: 32px 0 24px; font-size: 12px; color: var(--lb-text-3); display: flex; align-items: center; justify-content: center; gap: 8px; }
.lb-footer-brand { display: inline-flex; align-items: center; gap: 4px; font-weight: 600; color: var(--lb-text-2); }
.lb-footer-icon { width: 16px; height: 16px; border-radius: 3px; display: inline-flex; align-items: center; justify-content: center; overflow: hidden; flex-shrink: 0; }
.lb-footer-icon-img { width: 100%; height: 100%; display: block; object-fit: cover; }
.lb-footer-sep { color: var(--lb-text-4); }

/* --- Responsive --- */
@media (max-width: 768px) {
  .lb-nav-inner { padding: 0 16px; }
  .lb-hero-inner { padding: 16px 16px 12px; }
  .lb-hero-title { font-size: 18px; }
  .lb-tabs-inner { padding: 0 16px; }
  .lb-tab { padding: 10px 14px; font-size: 13px; }
  .lb-main { padding: 16px 16px 40px; }
  .lb-kpi-row { grid-template-columns: 1fr 1fr; gap: 10px; }
  .lb-kpi { padding: 14px; }
  .lb-kpi-value { font-size: 22px; }
  .lb-chart-grid { grid-template-columns: 1fr; }
  .lb-card-wide { grid-column: span 1; }
  .lb-completion-row { flex-direction: column; gap: 20px; }
  .lb-completion-chart { width: 120px; height: 120px; }
  /* Mobile: hide desktop table, show cards */
  .lb-table-outer--desktop { display: none; }
  .lb-mobile-cards { display: flex; flex-direction: column; gap: 12px; }
  .lb-mobile-card { background: var(--lb-bg-card); border: 1px solid var(--lb-border); border-radius: var(--lb-radius-lg); padding: 14px 16px; box-shadow: var(--lb-shadow); }
  .lb-mobile-row { display: flex; justify-content: space-between; align-items: flex-start; padding: 6px 0; border-bottom: 1px solid var(--lb-border); gap: 12px; }
  .lb-mobile-row:last-child { border-bottom: none; }
  .lb-mobile-label { font-size: 12px; color: var(--lb-text-3); flex-shrink: 0; min-width: 70px; }
  .lb-mobile-value { font-size: 13px; color: var(--lb-text-1); text-align: right; word-break: break-all; }
  .lb-daily-item { flex-direction: column; align-items: flex-start; gap: 8px; }
  .lb-daily-item-right { align-self: flex-end; }
}
</style>
