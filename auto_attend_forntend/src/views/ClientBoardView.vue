<template>
  <div class="cb-root">
    <!-- Loading -->
    <div v-if="loading" class="cb-state">
      <div class="cb-spinner"></div>
      <p>{{ $t('clientBoardPage.loading') }}</p>
    </div>

    <!-- Error -->
    <div v-else-if="error" class="cb-state cb-err">
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" width="48" height="48"><circle cx="12" cy="12" r="10"/><path d="M12 8v4M12 16h.01"/></svg>
      <p>{{ error }}</p>
    </div>

    <!-- Main Content -->
    <template v-else>
      <!-- Hero Header -->
      <header class="cb-hero">
        <div class="cb-hero-inner">
          <div class="cb-hero-top">
            <div class="cb-brand-badge">
              <span class="cb-brand-icon">流</span>
              <span class="cb-brand-text">流帮 Project</span>
            </div>
            <span class="cb-date" v-if="lastUpdated">{{ lastUpdated }}</span>
          </div>
          <h1 class="cb-title">{{ projectName || $t('clientBoardPage.untitled') }}</h1>
          <p v-if="repoId" class="cb-repo">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="14" height="14"><path d="M9 19c-5 1.5-5-2.5-7-3m14 6v-3.87a3.37 3.37 0 00-.94-2.61c3.14-.35 6.44-1.54 6.44-7A5.44 5.44 0 0020 4.77 5.07 5.07 0 0019.91 1S18.73.65 16 2.48a13.38 13.38 0 00-7 0C6.27.65 5.09 1 5.09 1A5.07 5.07 0 005 4.77a5.44 5.44 0 00-1.5 3.78c0 5.42 3.3 6.61 6.44 7A3.37 3.37 0 009 18.13V22"/></svg>
            {{ repoId }}
          </p>
        </div>
      </header>

      <main class="cb-main">
        <!-- ===== Progress Dashboard ===== -->
        <section v-if="showProgress" class="cb-card">
          <div class="cb-section-head">
            <h2 class="cb-h2">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="20" height="20"><path d="M18 20V10M12 20V4M6 20v-6"/></svg>
              {{ $t('clientBoardPage.progressSection') }}
            </h2>
          </div>

          <!-- Empty state for progress -->
          <div v-if="!progress || progress.empty" class="cb-empty">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" width="40" height="40"><rect x="3" y="3" width="18" height="18" rx="2"/><path d="M3 9h18M9 21V9"/></svg>
            <p class="cb-empty-title">{{ progress && progress.message ? progress.message : $t('clientBoardPage.noIssueTable') }}</p>
            <p class="cb-empty-desc">启用项目调整表并添加记录后，此处将自动展示进度数据。</p>
          </div>

          <!-- Charts -->
          <template v-else>
            <!-- Summary stats -->
            <div class="cb-stats-row">
              <div class="cb-stat">
                <span class="cb-stat-value cb-stat-value--green">{{ progress.resolved || 0 }}</span>
                <span class="cb-stat-label">{{ $t('clientBoardPage.legendResolved') }}</span>
              </div>
              <div class="cb-stat">
                <span class="cb-stat-value cb-stat-value--gray">{{ progress.unresolved || 0 }}</span>
                <span class="cb-stat-label">{{ $t('clientBoardPage.legendUnresolved') }}</span>
              </div>
              <div class="cb-stat" v-if="progress.totalCount">
                <span class="cb-stat-value">{{ progress.totalCount }}</span>
                <span class="cb-stat-label">总任务数</span>
              </div>
              <div class="cb-stat" v-if="progress.resolveRate != null">
                <span class="cb-stat-value cb-stat-value--blue">{{ progress.resolveRate }}%</span>
                <span class="cb-stat-label">完成率</span>
              </div>
            </div>

            <div class="cb-chart-grid">
              <!-- Pie chart -->
              <div class="cb-chart-box" v-if="(progress.resolved || 0) + (progress.unresolved || 0) > 0">
                <div class="cb-chart-title">{{ $t('clientBoardPage.chartCompletion') }}</div>
                <div class="cb-chart-canvas-wrap">
                  <canvas ref="pieRef" />
                </div>
              </div>

              <!-- Weekly created line -->
              <div class="cb-chart-box" :class="{ 'cb-chart-wide': (progress.resolved || 0) + (progress.unresolved || 0) > 0 }" v-if="weeklyCreated.length">
                <div class="cb-chart-title">{{ $t('clientBoardPage.chartCreated') }}</div>
                <div class="cb-chart-canvas-wrap">
                  <canvas ref="lineCreatedRef" />
                </div>
              </div>

              <!-- Weekly resolved line -->
              <div class="cb-chart-box cb-chart-wide" v-if="weeklyResolved.length">
                <div class="cb-chart-title">{{ $t('clientBoardPage.chartResolved') }}</div>
                <div class="cb-chart-canvas-wrap">
                  <canvas ref="lineResolvedRef" />
                </div>
              </div>

              <!-- Importance bar -->
              <div class="cb-chart-box" v-if="importanceLabels.length">
                <div class="cb-chart-title">{{ $t('clientBoardPage.chartImportance') }}</div>
                <div class="cb-chart-canvas-wrap">
                  <canvas ref="barImpRef" />
                </div>
              </div>

              <!-- Word cloud -->
              <div class="cb-chart-box" v-if="wordCloudItems.length">
                <div class="cb-chart-title">{{ $t('clientBoardPage.wordCloud') }}</div>
                <div class="cb-wc-body">
                  <span
                    v-for="(it, idx) in wordCloudItems"
                    :key="'wc-' + idx"
                    class="cb-wc-token"
                    :style="wordCloudStyle(it)"
                  >{{ it.text }}</span>
                </div>
              </div>

              <!-- Avg resolve time -->
              <div class="cb-chart-box cb-chart-wide" v-if="avgResolveHours.length">
                <div class="cb-chart-title">{{ $t('clientBoardPage.chartAvgResolve') }}</div>
                <div class="cb-chart-canvas-wrap">
                  <canvas ref="lineAvgRef" />
                </div>
              </div>
            </div>

            <!-- No chart data hint -->
            <div v-if="!hasAnyChartData" class="cb-empty cb-empty--small">
              <p class="cb-empty-desc">暂无足够的统计数据生成图表，随着项目推进将自动展示。</p>
            </div>
          </template>
        </section>

        <!-- ===== Feature Summary ===== -->
        <section v-if="showFeatures && featureRows.length" class="cb-card">
          <div class="cb-section-head">
            <h2 class="cb-h2">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="20" height="20"><path d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2"/></svg>
              {{ $t('clientBoardPage.featureSection') }}
            </h2>
            <span class="cb-count-badge">{{ featureRows.length }} 项</span>
          </div>
          <div class="cb-table-wrap">
            <table class="cb-table">
              <thead>
                <tr>
                  <th>{{ $t('clientBoardPage.colFeature') }}</th>
                  <th>{{ $t('clientBoardPage.colProgress') }}</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="(row, i) in featureRows" :key="'fr-' + i">
                  <td>{{ row.featureName }}</td>
                  <td>
                    <span class="cb-pill" :class="progressPillClass(row.progress)">{{ row.progress }}</span>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </section>

        <!-- ===== AI Entry ===== -->
        <section v-if="showAi" class="cb-card cb-ai">
          <div class="cb-section-head">
            <h2 class="cb-h2">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="20" height="20"><polygon points="13 2 3 14 12 14 11 22 21 10 12 10 13 2"/></svg>
              {{ $t('clientBoardPage.aiSection') }}
            </h2>
          </div>
          <p class="cb-ai-hint">{{ $t('clientBoardPage.aiHint') }}</p>
          <textarea v-model="aiText" class="cb-textarea" rows="4" :placeholder="$t('clientBoardPage.aiPlaceholder')" />
          <div class="cb-ai-actions">
            <button type="button" class="cb-btn cb-btn-primary" :disabled="aiBusy || !aiText.trim()" @click="runAiPreview">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="14" height="14" v-if="!aiBusy"><polygon points="13 2 3 14 12 14 11 22 21 10 12 10 13 2"/></svg>
              {{ aiBusy ? $t('clientBoardPage.aiRunning') : $t('clientBoardPage.aiPreview') }}
            </button>
          </div>
          <div v-if="aiDrafts.length" class="cb-drafts">
            <div class="cb-drafts-title">AI 生成的草稿（{{ aiDrafts.length }} 条）</div>
            <div v-for="(d, i) in aiDrafts" :key="'d-' + i" class="cb-draft">
              <div class="cb-draft-header">
                <strong>{{ d.title || '—' }}</strong>
              </div>
              <p class="cb-draft-desc">{{ d.description }}</p>
            </div>
            <div class="cb-drafts-actions">
              <button type="button" class="cb-btn cb-btn-primary" :disabled="aiCommitting" @click="commitAi">
                {{ aiCommitting ? $t('clientBoardPage.aiCommitting') : $t('clientBoardPage.aiCommit') }}
              </button>
            </div>
          </div>
        </section>

        <!-- ===== Powered By Footer ===== -->
        <div class="cb-powered">
          <span>Powered by</span>
          <span class="cb-powered-brand">
            <span class="cb-powered-icon">流</span>
            流帮 Project
          </span>
        </div>
      </main>

      <footer class="cb-foot">
        <span>{{ $t('clientBoardPage.footer') }}</span>
      </footer>
    </template>
  </div>
</template>

<script>
import { Chart as ChartJS, registerables } from 'chart.js'
ChartJS.register(...registerables)

export default {
  name: 'ClientBoardView',
  data () {
    return {
      loading: true,
      error: '',
      token: '',
      projectName: '',
      repoId: '',
      showProgress: false,
      showFeatures: false,
      showAi: false,
      progress: null,
      featureRows: [],
      aiText: '',
      aiDrafts: [],
      aiBusy: false,
      aiCommitting: false,
      charts: { pie: null, lc: null, lr: null, bi: null, la: null }
    }
  },
  computed: {
    lastUpdated () {
      const p = this.progress
      if (!p || !p.generatedAt) return ''
      return '数据更新于 ' + p.generatedAt
    },
    wordCloudItems () {
      const p = this.progress
      if (!p || !Array.isArray(p.wordCloud)) return []
      return p.wordCloud.slice(0, 40)
    },
    weeklyCreated () {
      return (this.progress && this.progress.weeklyCreated) || []
    },
    weeklyResolved () {
      return (this.progress && this.progress.weeklyResolved) || []
    },
    importanceLabels () {
      return (this.progress && this.progress.importanceLabels) || []
    },
    importanceCounts () {
      return (this.progress && this.progress.importanceCounts) || []
    },
    avgResolveHours () {
      return (this.progress && this.progress.avgResolveHours) || []
    },
    hasAnyChartData () {
      const p = this.progress
      if (!p) return false
      return (p.resolved || 0) + (p.unresolved || 0) > 0 ||
        this.weeklyCreated.length > 0 ||
        this.weeklyResolved.length > 0 ||
        this.importanceLabels.length > 0 ||
        this.wordCloudItems.length > 0 ||
        this.avgResolveHours.length > 0
    }
  },
  mounted () {
    this.token = String(this.$route.params.token || '').trim()
    if (!this.token) {
      this.error = this.$t('clientBoardPage.badToken')
      this.loading = false
      return
    }
    this.fetchBoard()
  },
  beforeDestroy () {
    Object.keys(this.charts).forEach(k => {
      const c = this.charts[k]
      if (c && typeof c.destroy === 'function') c.destroy()
      this.charts[k] = null
    })
  },
  methods: {
    async fetchBoard () {
      this.loading = true
      this.error = ''
      try {
        const resp = await this.$http.get(`/public/client-board/${encodeURIComponent(this.token)}`)
        if (!resp.data || resp.data.code !== 0 || !resp.data.data) {
          this.error = (resp.data && resp.data.message) || this.$t('clientBoardPage.loadFail')
          return
        }
        const d = resp.data.data
        this.projectName = d.projectName || ''
        this.repoId = d.repoId || ''
        this.showProgress = !!d.showProgressDashboard
        this.showFeatures = !!d.showFeatureBacklog
        this.showAi = !!d.showAiTableEntry
        this.progress = d.progress || null
        this.featureRows = Array.isArray(d.featureSummary) ? d.featureSummary : []
        this.$nextTick(() => this.renderCharts())
      } catch (e) {
        this.error = this.$t('clientBoardPage.loadFail')
      } finally {
        this.loading = false
      }
    },
    destroyCharts () {
      Object.keys(this.charts).forEach(k => {
        const c = this.charts[k]
        if (c && typeof c.destroy === 'function') c.destroy()
        this.charts[k] = null
      })
    },
    renderCharts () {
      this.destroyCharts()
      const p = this.progress
      if (!p || p.empty || !this.showProgress) return
      const labels = p.weekLabels || []
      const resolved = Number(p.resolved) || 0
      const unresolved = Number(p.unresolved) || 0
      const total = resolved + unresolved

      // Pie chart
      if (this.$refs.pieRef && total > 0) {
        this.charts.pie = new ChartJS(this.$refs.pieRef, {
          type: 'doughnut',
          data: {
            labels: [this.$t('clientBoardPage.legendResolved'), this.$t('clientBoardPage.legendUnresolved')],
            datasets: [{
              data: [resolved, unresolved],
              backgroundColor: ['#00b578', '#e5e6eb'],
              borderWidth: 0,
              hoverOffset: 4
            }]
          },
          options: {
            responsive: true,
            maintainAspectRatio: false,
            cutout: '65%',
            plugins: {
              legend: { position: 'bottom', labels: { padding: 16, usePointStyle: true, pointStyle: 'circle', font: { size: 12 } } }
            }
          }
        })
      }

      // Line: weekly created
      if (this.$refs.lineCreatedRef && this.weeklyCreated.length) {
        this.charts.lc = new ChartJS(this.$refs.lineCreatedRef, {
          type: 'line',
          data: {
            labels,
            datasets: [{
              label: this.$t('clientBoardPage.chartCreated'),
              data: this.weeklyCreated,
              borderColor: '#3370ff',
              backgroundColor: 'rgba(51,112,255,0.08)',
              fill: true,
              tension: 0.3,
              borderWidth: 2,
              pointRadius: 3,
              pointHoverRadius: 5
            }]
          },
          options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: { legend: { display: false } },
            scales: {
              y: { beginAtZero: true, grid: { color: '#f0f0f0' }, ticks: { font: { size: 11 } } },
              x: { grid: { display: false }, ticks: { font: { size: 11 }, maxRotation: 0, maxTicksLimit: 8 } }
            }
          }
        })
      }

      // Line: weekly resolved
      if (this.$refs.lineResolvedRef && this.weeklyResolved.length) {
        this.charts.lr = new ChartJS(this.$refs.lineResolvedRef, {
          type: 'line',
          data: {
            labels,
            datasets: [{
              label: this.$t('clientBoardPage.chartResolved'),
              data: this.weeklyResolved,
              borderColor: '#00b578',
              backgroundColor: 'rgba(0,181,120,0.08)',
              fill: true,
              tension: 0.3,
              borderWidth: 2,
              pointRadius: 3,
              pointHoverRadius: 5
            }]
          },
          options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: { legend: { display: false } },
            scales: {
              y: { beginAtZero: true, grid: { color: '#f0f0f0' }, ticks: { font: { size: 11 } } },
              x: { grid: { display: false }, ticks: { font: { size: 11 }, maxRotation: 0, maxTicksLimit: 8 } }
            }
          }
        })
      }

      // Bar: importance
      if (this.$refs.barImpRef && this.importanceLabels.length) {
        const colors = ['#f54a45', '#ff8800', '#ff9f1a', '#3370ff', '#8f959e']
        this.charts.bi = new ChartJS(this.$refs.barImpRef, {
          type: 'bar',
          data: {
            labels: this.importanceLabels,
            datasets: [{
              label: '',
              data: this.importanceCounts,
              backgroundColor: this.importanceLabels.map((_, i) => colors[i % colors.length]),
              borderRadius: 4,
              barPercentage: 0.6
            }]
          },
          options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: { legend: { display: false } },
            scales: {
              y: { beginAtZero: true, grid: { color: '#f0f0f0' }, ticks: { stepSize: 1, font: { size: 11 } } },
              x: { grid: { display: false }, ticks: { font: { size: 11 } } }
            }
          }
        })
      }

      // Line: avg resolve hours
      if (this.$refs.lineAvgRef && this.avgResolveHours.length) {
        this.charts.la = new ChartJS(this.$refs.lineAvgRef, {
          type: 'line',
          data: {
            labels,
            datasets: [{
              label: this.$t('clientBoardPage.avgHours'),
              data: this.avgResolveHours,
              borderColor: '#7c3aed',
              backgroundColor: 'rgba(124,58,237,0.08)',
              fill: true,
              tension: 0.2,
              borderWidth: 2,
              pointRadius: 3,
              pointHoverRadius: 5
            }]
          },
          options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: { legend: { display: false } },
            scales: {
              y: { beginAtZero: true, grid: { color: '#f0f0f0' }, ticks: { font: { size: 11 } } },
              x: { grid: { display: false }, ticks: { font: { size: 11 }, maxRotation: 0, maxTicksLimit: 8 } }
            }
          }
        })
      }
    },
    wordCloudStyle (it) {
      const max = it.max || 1
      const w = it.weight || 0
      const r = max > 0 ? w / max : 0
      const minPx = 12
      const maxPx = 24
      const fs = minPx + (maxPx - minPx) * r
      const palette = ['#1d2129', '#3370ff', '#00b578', '#ff9f1a', '#7c3aed', '#f54a45']
      const idx = (it.text && it.text.charCodeAt(0) ? it.text.charCodeAt(0) : 0) % palette.length
      return {
        fontSize: fs + 'px',
        color: palette[idx],
        fontWeight: r >= 0.35 ? 600 : 400
      }
    },
    progressPillClass (progress) {
      if (!progress) return ''
      const v = String(progress)
      if (v.includes('100') || v.includes('完成') || v.includes('已上线')) return 'cb-pill--done'
      if (v.includes('进行') || v.includes('开发')) return 'cb-pill--wip'
      if (v.includes('待') || v.includes('规划') || v.includes('0')) return 'cb-pill--todo'
      return ''
    },
    async runAiPreview () {
      const raw = (this.aiText || '').trim()
      if (!raw) return
      this.aiBusy = true
      this.aiDrafts = []
      try {
        const resp = await this.$http.post(`/public/client-board/${encodeURIComponent(this.token)}/ai-preview`, {
          rawText: raw,
          attachmentIds: []
        })
        if (resp.data && resp.data.code === 0 && resp.data.data && Array.isArray(resp.data.data.items)) {
          this.aiDrafts = resp.data.data.items
        } else {
          window.alert((resp.data && resp.data.message) || 'AI 预览失败')
        }
      } catch (e) {
        window.alert('请求失败，请稍后重试')
      } finally {
        this.aiBusy = false
      }
    },
    async commitAi () {
      if (!this.aiDrafts.length) return
      this.aiCommitting = true
      try {
        const resp = await this.$http.post(`/public/client-board/${encodeURIComponent(this.token)}/ai-commit`, {
          tasks: this.aiDrafts
        })
        if (resp.data && resp.data.code === 0) {
          const n = resp.data.data && resp.data.data.createdCount != null ? resp.data.data.createdCount : 0
          window.alert(this.$t('clientBoardPage.commitOk', { n }))
          this.aiDrafts = []
          this.aiText = ''
          await this.fetchBoard()
        } else {
          window.alert((resp.data && resp.data.message) || '提交失败')
        }
      } catch (e) {
        window.alert('请求失败，请稍后重试')
      } finally {
        this.aiCommitting = false
      }
    }
  }
}
</script>

<style scoped>
/* ============================================================
   ClientBoardView - Public-facing client board page
   Professional, clean design with brand identity
   ============================================================ */

/* --- Root --- */
.cb-root {
  min-height: 100vh;
  background: var(--bg-page, #f5f6f7);
  color: var(--text-primary, #1F2329);
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', 'PingFang SC', 'Hiragino Sans GB', 'Microsoft YaHei', sans-serif;
}

/* --- Loading / Error --- */
.cb-state {
  padding: 120px 32px;
  text-align: center;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
  color: var(--text-secondary, #646A73);
  font-size: 15px;
}

.cb-err {
  color: #f54a45;
}

.cb-err svg {
  color: #f54a45;
  opacity: 0.6;
}

.cb-spinner {
  width: 32px;
  height: 32px;
  border: 3px solid #e5e6eb;
  border-top-color: var(--brand-blue, #1456F0);
  border-radius: 50%;
  animation: cb-spin 0.7s linear infinite;
}

@keyframes cb-spin { to { transform: rotate(360deg); } }

/* --- Hero Header --- */
.cb-hero {
  background: #fff;
  border-bottom: 1px solid var(--border-primary, #dee0e3);
  padding: 32px 32px 28px;
}

.cb-hero-inner {
  max-width: 1080px;
  margin: 0 auto;
}

.cb-hero-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.cb-brand-badge {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 4px 12px 4px 4px;
  background: var(--bg-page, #f5f6f7);
  border-radius: 100px;
  font-size: 12px;
  color: var(--text-secondary, #646A73);
  font-weight: 500;
}

.cb-brand-icon {
  width: 22px;
  height: 22px;
  background: var(--brand-blue, #1456F0);
  color: #fff;
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 11px;
  font-weight: 700;
}

.cb-date {
  font-size: 12px;
  color: var(--text-disabled, #8F959E);
}

.cb-title {
  margin: 0 0 8px;
  font-size: 26px;
  font-weight: 700;
  color: var(--text-primary, #1F2329);
  letter-spacing: -0.02em;
  line-height: 1.3;
}

.cb-repo {
  margin: 0;
  font-size: 13px;
  color: var(--text-secondary, #646A73);
  font-family: 'SF Mono', 'Menlo', 'Monaco', 'Consolas', monospace;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 4px 10px;
  background: var(--bg-page, #f5f6f7);
  border-radius: 6px;
}

/* --- Main --- */
.cb-main {
  max-width: 1080px;
  margin: 0 auto;
  padding: 24px 32px 48px;
}

/* --- Card --- */
.cb-card {
  background: #fff;
  border-radius: 12px;
  border: 1px solid var(--border-primary, #dee0e3);
  padding: 24px;
  margin-bottom: 20px;
  transition: box-shadow 0.2s;
}

.cb-card:hover {
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
}

/* --- Section Head --- */
.cb-section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
}

.cb-h2 {
  margin: 0;
  font-size: 17px;
  font-weight: 600;
  color: var(--text-primary, #1F2329);
  display: flex;
  align-items: center;
  gap: 8px;
}

.cb-h2 svg {
  color: var(--brand-blue, #1456F0);
  flex-shrink: 0;
}

.cb-count-badge {
  font-size: 12px;
  font-weight: 500;
  color: var(--text-secondary, #646A73);
  background: var(--bg-page, #f5f6f7);
  padding: 2px 10px;
  border-radius: 100px;
}

/* --- Stats Row --- */
.cb-stats-row {
  display: flex;
  gap: 16px;
  margin-bottom: 24px;
  padding-bottom: 20px;
  border-bottom: 1px solid var(--border-primary, #dee0e3);
  flex-wrap: wrap;
}

.cb-stat {
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 80px;
}

.cb-stat-value {
  font-size: 28px;
  font-weight: 700;
  color: var(--text-primary, #1F2329);
  line-height: 1;
}

.cb-stat-value--green { color: #00b578; }
.cb-stat-value--blue { color: var(--brand-blue, #1456F0); }
.cb-stat-value--gray { color: #8F959E; }

.cb-stat-label {
  font-size: 12px;
  color: var(--text-secondary, #646A73);
  font-weight: 500;
}

/* --- Chart Grid --- */
.cb-chart-grid {
  display: grid;
  grid-template-columns: 1fr;
  gap: 20px;
}

@media (min-width: 768px) {
  .cb-chart-grid {
    grid-template-columns: 1fr 1fr;
  }
  .cb-chart-wide {
    grid-column: span 2;
  }
}

.cb-chart-box {
  min-height: 200px;
  position: relative;
}

.cb-chart-canvas-wrap {
  height: 200px;
  position: relative;
}

.cb-chart-title {
  font-size: 13px;
  font-weight: 500;
  color: var(--text-secondary, #646A73);
  margin-bottom: 8px;
}

/* --- Word Cloud --- */
.cb-wc-body {
  line-height: 2;
  word-break: break-all;
  padding: 8px 0;
}

.cb-wc-token {
  display: inline-block;
  margin: 2px 8px 2px 0;
  cursor: default;
  transition: opacity 0.15s;
}

.cb-wc-token:hover { opacity: 0.7; }

/* --- Empty State --- */
.cb-empty {
  text-align: center;
  padding: 48px 24px;
  color: var(--text-disabled, #8F959E);
}

.cb-empty svg {
  opacity: 0.4;
  margin-bottom: 12px;
}

.cb-empty-title {
  margin: 0 0 8px;
  font-size: 15px;
  font-weight: 500;
  color: var(--text-secondary, #646A73);
}

.cb-empty-desc {
  margin: 0;
  font-size: 13px;
  color: var(--text-disabled, #8F959E);
  line-height: 1.6;
}

.cb-empty--small {
  padding: 24px;
}

/* --- Feature Table --- */
.cb-table-wrap {
  overflow-x: auto;
}

.cb-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
}

.cb-table th,
.cb-table td {
  border-bottom: 1px solid var(--border-primary, #dee0e3);
  padding: 10px 12px;
  text-align: left;
}

.cb-table th {
  color: var(--text-secondary, #646A73);
  font-weight: 600;
  font-size: 12px;
  text-transform: uppercase;
  letter-spacing: 0.03em;
  background: var(--bg-page, #f5f6f7);
}

.cb-table th:first-child { border-radius: 6px 0 0 6px; }
.cb-table th:last-child { border-radius: 0 6px 6px 0; }

.cb-table tbody tr:hover { background: #fafbfc; }

/* --- Progress Pill --- */
.cb-pill {
  display: inline-block;
  padding: 3px 10px;
  border-radius: 100px;
  font-size: 12px;
  font-weight: 500;
  background: var(--bg-page, #f5f6f7);
  color: var(--text-secondary, #646A73);
}

.cb-pill--done { background: #ECFDF5; color: #059669; }
.cb-pill--wip { background: #FFF7ED; color: #D97706; }
.cb-pill--todo { background: #F0F5FF; color: var(--brand-blue, #1456F0); }

/* --- AI Section --- */
.cb-ai {
  border-left: 3px solid var(--brand-blue, #1456F0);
}

.cb-ai-hint {
  font-size: 12px;
  color: var(--text-disabled, #8F959E);
  margin: 0 0 12px;
  line-height: 1.6;
}

.cb-textarea {
  width: 100%;
  border: 1px solid var(--border-primary, #dee0e3);
  border-radius: 8px;
  padding: 10px 14px;
  font-size: 14px;
  font-family: inherit;
  color: var(--text-primary, #1F2329);
  background: #fff;
  resize: vertical;
  box-sizing: border-box;
  outline: none;
  transition: border-color 0.2s, box-shadow 0.2s;
  line-height: 1.6;
}

.cb-textarea:focus {
  border-color: var(--brand-blue, #1456F0);
  box-shadow: 0 0 0 2px rgba(20, 86, 240, 0.12);
}

.cb-ai-actions {
  margin-top: 12px;
}

/* --- AI Drafts --- */
.cb-drafts {
  margin-top: 20px;
  padding-top: 16px;
  border-top: 1px solid var(--border-primary, #dee0e3);
}

.cb-drafts-title {
  font-size: 13px;
  font-weight: 600;
  color: var(--text-primary, #1F2329);
  margin-bottom: 12px;
}

.cb-draft {
  margin-bottom: 10px;
  padding: 12px 14px;
  background: var(--bg-page, #f5f6f7);
  border-radius: 8px;
  border: 1px solid transparent;
  transition: border-color 0.15s;
}

.cb-draft:hover {
  border-color: var(--brand-blue, #1456F0);
}

.cb-draft-header strong {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary, #1F2329);
}

.cb-draft-desc {
  margin: 6px 0 0;
  font-size: 13px;
  color: var(--text-secondary, #646A73);
  white-space: pre-wrap;
  line-height: 1.6;
}

.cb-drafts-actions {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}

/* --- Buttons --- */
.cb-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  min-height: 36px;
  padding: 0 18px;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  border: none;
  transition: all 0.2s;
  outline: none;
  line-height: 1;
}

.cb-btn:focus-visible {
  box-shadow: 0 0 0 2px rgba(20, 86, 240, 0.25);
}

.cb-btn-primary {
  background: var(--brand-blue, #1456F0);
  color: #fff;
}

.cb-btn-primary:hover { background: #0E42D0; }
.cb-btn-primary:active { background: #0a35b0; }
.cb-btn-primary:disabled { opacity: 0.45; cursor: not-allowed; }

/* --- Powered By --- */
.cb-powered {
  text-align: center;
  padding: 24px;
  font-size: 12px;
  color: var(--text-disabled, #8F959E);
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
}

.cb-powered-brand {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-weight: 600;
  color: var(--text-secondary, #646A73);
}

.cb-powered-icon {
  width: 18px;
  height: 18px;
  background: var(--brand-blue, #1456F0);
  color: #fff;
  border-radius: 4px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 9px;
  font-weight: 700;
}

/* --- Footer --- */
.cb-foot {
  text-align: center;
  padding: 20px;
  font-size: 12px;
  color: var(--text-disabled, #8F959E);
  border-top: 1px solid var(--border-primary, #dee0e3);
}

/* --- Responsive --- */
@media (max-width: 640px) {
  .cb-hero { padding: 24px 16px 20px; }
  .cb-title { font-size: 20px; }
  .cb-main { padding: 16px 16px 40px; }
  .cb-card { padding: 16px; }
  .cb-stats-row { gap: 12px; }
  .cb-stat-value { font-size: 22px; }
  .cb-chart-grid { grid-template-columns: 1fr; }
  .cb-chart-wide { grid-column: span 1; }
}
</style>
