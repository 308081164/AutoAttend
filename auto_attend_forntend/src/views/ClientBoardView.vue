<template>
  <div class="cb-root">
    <div v-if="loading" class="cb-state">{{ $t('clientBoardPage.loading') }}</div>
    <div v-else-if="error" class="cb-state cb-err">{{ error }}</div>
    <template v-else>
      <header class="cb-hero">
        <div class="cb-hero-inner">
          <p class="cb-kicker">{{ $t('clientBoardPage.kicker') }}</p>
          <h1 class="cb-title">{{ projectName || $t('clientBoardPage.untitled') }}</h1>
          <p v-if="repoId" class="cb-repo">{{ repoId }}</p>
        </div>
      </header>

      <main class="cb-main">
        <section v-if="showProgress && progress && !progress.empty" class="cb-card">
          <h2 class="cb-h2">{{ $t('clientBoardPage.progressSection') }}</h2>
          <div class="cb-chart-grid">
            <div class="cb-chart-box">
              <div class="cb-chart-title">{{ $t('clientBoardPage.chartCompletion') }}</div>
              <canvas ref="pieRef" />
            </div>
            <div class="cb-chart-box cb-chart-wide">
              <div class="cb-chart-title">{{ $t('clientBoardPage.chartCreated') }}</div>
              <canvas ref="lineCreatedRef" />
            </div>
            <div class="cb-chart-box cb-chart-wide">
              <div class="cb-chart-title">{{ $t('clientBoardPage.chartResolved') }}</div>
              <canvas ref="lineResolvedRef" />
            </div>
            <div class="cb-chart-box cb-chart-wide">
              <div class="cb-chart-title">{{ $t('clientBoardPage.chartImportance') }}</div>
              <canvas ref="barImpRef" />
            </div>
            <div class="cb-chart-box cb-wc">
              <div class="cb-chart-title">{{ $t('clientBoardPage.wordCloud') }}</div>
              <div v-if="!wordCloudItems.length" class="cb-muted">{{ $t('clientBoardPage.wordCloudEmpty') }}</div>
              <div v-else class="cb-wc-body">
                <span
                  v-for="(it, idx) in wordCloudItems"
                  :key="'wc-' + idx"
                  class="cb-wc-token"
                  :style="wordCloudStyle(it)"
                >{{ it.text }}</span>
              </div>
            </div>
            <div class="cb-chart-box cb-chart-wide">
              <div class="cb-chart-title">{{ $t('clientBoardPage.chartAvgResolve') }}</div>
              <canvas ref="lineAvgRef" />
            </div>
          </div>
        </section>

        <section v-if="showProgress && progress && progress.empty" class="cb-card cb-muted-box">
          {{ progress.message || $t('clientBoardPage.noIssueTable') }}
        </section>

        <section v-if="showFeatures && featureRows.length" class="cb-card">
          <h2 class="cb-h2">{{ $t('clientBoardPage.featureSection') }}</h2>
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
                  <td><span class="cb-pill">{{ row.progress }}</span></td>
                </tr>
              </tbody>
            </table>
          </div>
        </section>

        <section v-if="showAi" class="cb-card cb-ai">
          <h2 class="cb-h2">{{ $t('clientBoardPage.aiSection') }}</h2>
          <p class="cb-ai-hint">{{ $t('clientBoardPage.aiHint') }}</p>
          <textarea v-model="aiText" class="cb-textarea" rows="4" :placeholder="$t('clientBoardPage.aiPlaceholder')" />
          <div class="cb-ai-actions">
            <button type="button" class="cb-btn cb-btn-primary" :disabled="aiBusy" @click="runAiPreview">
              {{ aiBusy ? $t('clientBoardPage.aiRunning') : $t('clientBoardPage.aiPreview') }}
            </button>
          </div>
          <div v-if="aiDrafts.length" class="cb-drafts">
            <div v-for="(d, i) in aiDrafts" :key="'d-' + i" class="cb-draft">
              <strong>{{ d.title || '—' }}</strong>
              <p class="cb-draft-desc">{{ d.description }}</p>
            </div>
            <button type="button" class="cb-btn cb-btn-primary" :disabled="aiCommitting" @click="commitAi">
              {{ aiCommitting ? $t('clientBoardPage.aiCommitting') : $t('clientBoardPage.aiCommit') }}
            </button>
          </div>
        </section>

        <section class="cb-card cb-apk">
          <h2 class="cb-h2">{{ $t('clientBoardPage.apkSection') }}</h2>
          <p class="cb-muted">{{ $t('clientBoardPage.apkPlaceholder') }}</p>
          <button type="button" class="cb-btn cb-btn-ghost" disabled>{{ $t('clientBoardPage.apkSoon') }}</button>
        </section>
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
    wordCloudItems () {
      const p = this.progress
      if (!p || !Array.isArray(p.wordCloud)) return []
      return p.wordCloud
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
      const wc = p.weeklyCreated || []
      const wr = p.weeklyResolved || []
      const il = p.importanceLabels || []
      const ic = p.importanceCounts || []
      const avgs = p.avgResolveHours || []
      const resolved = Number(p.resolved) || 0
      const unresolved = Number(p.unresolved) || 0

      if (this.$refs.pieRef) {
        this.charts.pie = new ChartJS(this.$refs.pieRef, {
          type: 'pie',
          data: {
            labels: [this.$t('clientBoardPage.legendResolved'), this.$t('clientBoardPage.legendUnresolved')],
            datasets: [{ data: [resolved, unresolved], backgroundColor: ['#00b578', '#c9cdd4'] }]
          },
          options: { responsive: true, maintainAspectRatio: false, plugins: { legend: { position: 'bottom' } } }
        })
      }
      if (this.$refs.lineCreatedRef) {
        this.charts.lc = new ChartJS(this.$refs.lineCreatedRef, {
          type: 'line',
          data: {
            labels,
            datasets: [{
              label: this.$t('clientBoardPage.chartCreated'),
              data: wc,
              borderColor: '#3370ff',
              backgroundColor: 'rgba(51,112,255,0.12)',
              fill: true,
              tension: 0.25
            }]
          },
          options: { responsive: true, maintainAspectRatio: false, plugins: { legend: { display: false } } }
        })
      }
      if (this.$refs.lineResolvedRef) {
        this.charts.lr = new ChartJS(this.$refs.lineResolvedRef, {
          type: 'line',
          data: {
            labels,
            datasets: [{
              label: this.$t('clientBoardPage.chartResolved'),
              data: wr,
              borderColor: '#00b578',
              backgroundColor: 'rgba(0,181,120,0.12)',
              fill: true,
              tension: 0.25
            }]
          },
          options: { responsive: true, maintainAspectRatio: false, plugins: { legend: { display: false } } }
        })
      }
      if (this.$refs.barImpRef && il.length) {
        this.charts.bi = new ChartJS(this.$refs.barImpRef, {
          type: 'bar',
          data: {
            labels: il,
            datasets: [{ label: '', data: ic, backgroundColor: '#ff9f1a' }]
          },
          options: { responsive: true, maintainAspectRatio: false, plugins: { legend: { display: false } } }
        })
      }
      if (this.$refs.lineAvgRef && avgs.length) {
        this.charts.la = new ChartJS(this.$refs.lineAvgRef, {
          type: 'line',
          data: {
            labels,
            datasets: [{
              label: this.$t('clientBoardPage.avgHours'),
              data: avgs,
              borderColor: '#7c3aed',
              backgroundColor: 'rgba(124,58,237,0.1)',
              fill: true,
              tension: 0.2
            }]
          },
          options: { responsive: true, maintainAspectRatio: false, scales: { y: { beginAtZero: true } } }
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
      const palette = ['#1d2129', '#3370ff', '#00b578', '#ff9f1a', '#7c3aed']
      const idx = (it.text && it.text.charCodeAt(0) ? it.text.charCodeAt(0) : 0) % palette.length
      return {
        fontSize: fs + 'px',
        color: palette[idx],
        fontWeight: r >= 0.35 ? 600 : 400
      }
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
          window.alert((resp.data && resp.data.message) || 'AI 失败')
        }
      } catch (e) {
        window.alert('请求失败')
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
        window.alert('请求失败')
      } finally {
        this.aiCommitting = false
      }
    }
  }
}
</script>

<style scoped>
.cb-root {
  min-height: 100vh;
  background: linear-gradient(180deg, #f0f6ff 0%, #f7f8fa 32%, #f7f8fa 100%);
  color: #1d2129;
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "PingFang SC", "Hiragino Sans GB", "Microsoft YaHei", sans-serif;
}

.cb-state {
  padding: 48px 24px;
  text-align: center;
  font-size: 15px;
  color: #646a73;
}
.cb-err { color: #d0302f; }

.cb-hero {
  padding: 28px 20px 20px;
  border-bottom: 1px solid rgba(29, 33, 41, 0.08);
  background: #fff;
}
.cb-hero-inner {
  max-width: 1120px;
  margin: 0 auto;
}
.cb-kicker {
  margin: 0 0 8px;
  font-size: 12px;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: #86909c;
}
.cb-title {
  margin: 0;
  font-size: 22px;
  font-weight: 600;
  line-height: 1.35;
  letter-spacing: -0.02em;
}
.cb-repo {
  margin: 10px 0 0;
  font-size: 13px;
  color: #646a73;
  font-family: ui-monospace, monospace;
}

.cb-main {
  max-width: 1120px;
  margin: 0 auto;
  padding: 20px 16px 48px;
}

.cb-card {
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 1px 2px rgba(29, 33, 41, 0.06), 0 4px 16px rgba(51, 112, 255, 0.06);
  padding: 20px 18px;
  margin-bottom: 16px;
  border: 1px solid rgba(29, 33, 41, 0.06);
}
.cb-h2 {
  margin: 0 0 16px;
  font-size: 16px;
  font-weight: 600;
  color: #1d2129;
}

.cb-chart-grid {
  display: grid;
  grid-template-columns: 1fr;
  gap: 16px;
}
@media (min-width: 900px) {
  .cb-chart-grid {
    grid-template-columns: 1fr 1fr;
  }
  .cb-chart-wide {
    grid-column: span 2;
  }
}

.cb-chart-box {
  min-height: 220px;
  position: relative;
}
.cb-chart-title {
  font-size: 13px;
  font-weight: 500;
  color: #646a73;
  margin-bottom: 8px;
}
.cb-wc {
  min-height: 160px;
}
.cb-wc-body {
  line-height: 1.65;
  word-break: break-all;
}
.cb-wc-token {
  display: inline-block;
  margin: 2px 6px 2px 0;
}

.cb-muted {
  color: #86909c;
  font-size: 13px;
}
.cb-muted-box {
  font-size: 14px;
}

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
  border-bottom: 1px solid #eceff3;
  padding: 10px 8px;
  text-align: left;
}
.cb-table th {
  color: #646a73;
  font-weight: 500;
}
.cb-pill {
  display: inline-block;
  padding: 2px 8px;
  border-radius: 999px;
  background: #f2f3f5;
  font-size: 12px;
}

.cb-ai-hint {
  font-size: 12px;
  color: #86909c;
  margin: 0 0 10px;
}
.cb-textarea {
  width: 100%;
  border: 1px solid #e5e6eb;
  border-radius: 8px;
  padding: 10px 12px;
  font-size: 14px;
  resize: vertical;
  box-sizing: border-box;
}
.cb-ai-actions {
  margin-top: 10px;
}
.cb-drafts {
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid #f2f3f5;
}
.cb-draft {
  margin-bottom: 12px;
  padding: 10px;
  background: #fafbfc;
  border-radius: 8px;
}
.cb-draft-desc {
  margin: 6px 0 0;
  font-size: 13px;
  color: #4e5969;
  white-space: pre-wrap;
}

.cb-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 36px;
  padding: 0 16px;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  border: none;
}
.cb-btn-primary {
  background: #3370ff;
  color: #fff;
}
.cb-btn-primary:disabled {
  opacity: 0.55;
  cursor: not-allowed;
}
.cb-btn-ghost {
  background: #f2f3f5;
  color: #646a73;
}

.cb-apk {
  opacity: 0.95;
}

.cb-foot {
  text-align: center;
  padding: 16px;
  font-size: 12px;
  color: #c9cdd4;
}

@media (max-width: 600px) {
  .cb-title { font-size: 18px; }
  .cb-main { padding: 14px 12px 36px; }
  .cb-card { padding: 16px 14px; }
}
</style>
