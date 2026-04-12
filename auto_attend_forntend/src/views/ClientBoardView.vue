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
          <div class="lb-nav-left">
            <div class="lb-nav-logo">流</div>
            <span class="lb-nav-title">流帮 Project</span>
          </div>
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
            <span class="lb-hero-date" v-if="generatedAt">{{ generatedAt }}</span>
          </div>
          <h1 class="lb-hero-title">{{ projectName || '未命名项目' }}</h1>
          <p v-if="repoId" class="lb-hero-repo">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="14" height="14"><path d="M9 19c-5 1.5-5-2.5-7-3m14 6v-3.87a3.37 3.37 0 00-.94-2.61c3.14-.35 6.44-1.54 6.44-7A5.44 5.44 0 0020 4.77 5.07 5.07 0 0019.91 1S18.73.65 16 2.48a13.38 13.38 0 00-7 0C6.27.65 5.09 1 5.09 1A5.07 5.07 0 005 4.77a5.44 5.44 0 00-1.5 3.78c0 5.42 3.3 6.61 6.44 7A3.37 3.37 0 009 18.13V22"/></svg>
            {{ repoId }}
          </p>
        </div>
      </header>

      <main class="lb-main">
        <!-- ===== KPI Summary Cards ===== -->
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

        <!-- ===== Progress Dashboard ===== -->
        <section v-if="showProgress" class="lb-section">
          <!-- Empty state -->
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
              <!-- Weekly Trend (combined created + resolved) -->
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

              <!-- Word Cloud -->
              <div class="lb-card" v-if="wordCloudItems.length">
                <div class="lb-card-head">
                  <h3 class="lb-card-title">
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="18" height="18"><path d="M21 15a2 2 0 01-2 2H7l-4 4V5a2 2 0 012-2h14a2 2 0 012 2z"/></svg>
                    需求关键词
                  </h3>
                </div>
                <div class="lb-wc-body">
                  <span v-for="(it, idx) in wordCloudItems" :key="'wc-' + idx" class="lb-wc-token" :style="wcStyle(it)">{{ it.text }}</span>
                </div>
              </div>
            </div>
          </template>
        </section>

        <!-- ===== Feature Backlog ===== -->
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

        <!-- ===== AI Entry ===== -->
        <section v-if="showAi" class="lb-section">
          <div class="lb-card lb-card--accent">
            <div class="lb-card-head">
              <h3 class="lb-card-title">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="18" height="18"><polygon points="13 2 3 14 12 14 11 22 21 10 12 10 13 2"/></svg>
                AI 需求录入
              </h3>
            </div>
            <p class="lb-ai-desc">描述您的需求、问题或变更，AI 将自动解析并生成结构化任务草稿。</p>
            <textarea v-model="aiText" class="lb-textarea" rows="4" placeholder="请描述需求、问题或变更…" />
            <div class="lb-ai-footer">
              <button type="button" class="lb-btn" :disabled="aiBusy || !aiText.trim()" @click="runAiPreview">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="14" height="14" v-if="!aiBusy"><polygon points="13 2 3 14 12 14 11 22 21 10 12 10 13 2"/></svg>
                {{ aiBusy ? '生成中…' : '生成草稿' }}
              </button>
            </div>
            <div v-if="aiDrafts.length" class="lb-drafts">
              <div class="lb-drafts-head">AI 草稿（{{ aiDrafts.length }} 条）</div>
              <div v-for="(d, i) in aiDrafts" :key="'d-' + i" class="lb-draft">
                <strong>{{ d.title || '—' }}</strong>
                <p>{{ d.description }}</p>
              </div>
              <div class="lb-drafts-foot">
                <button type="button" class="lb-btn" :disabled="aiCommitting" @click="commitAi">
                  {{ aiCommitting ? '提交中…' : '确认提交' }}
                </button>
              </div>
            </div>
          </div>
        </section>

        <!-- ===== Footer ===== -->
        <footer class="lb-footer">
          <span class="lb-footer-brand">
            <span class="lb-footer-icon">流</span>
            流帮 Project
          </span>
          <span class="lb-footer-sep">·</span>
          <span>客户项目阅览看板</span>
        </footer>
      </main>
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
      tenantName: '',
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
      charts: {}
    }
  },
  computed: {
    generatedAt () {
      const p = this.progress
      if (!p || !p.generatedAt) return ''
      return p.generatedAt
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
    wordCloudItems () {
      const p = this.progress
      if (!p || !Array.isArray(p.wordCloud)) return []
      return p.wordCloud.slice(0, 40)
    },
    hasWeeklyData () {
      return this.weekLabels.length > 0 &&
        (this.weeklyCreated.some(v => v > 0) || this.weeklyResolved.some(v => v > 0))
    }
  },
  mounted () {
    this.token = String(this.$route.params.token || '').trim()
    if (!this.token) { this.error = '无效的看板链接'; this.loading = false; return }
    this.fetchBoard()
  },
  beforeDestroy () { this.destroyCharts() },
  methods: {
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
        this.$nextTick(() => this.renderCharts())
      } catch (e) { this.error = '加载失败，请稍后重试' }
      finally { this.loading = false }
    },
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
      const labels = this.weekLabels

      // 1. Pie chart
      if (this.$refs.pieRef && total > 0) {
        this.charts.pie = new ChartJS(this.$refs.pieRef, {
          type: 'doughnut',
          data: {
            labels: ['已完成', '进行中'],
            datasets: [{ data: [resolved, unresolved], backgroundColor: ['#3370ff', '#e5e6eb'], borderWidth: 0, hoverOffset: 4 }]
          },
          options: {
            responsive: true, maintainAspectRatio: false, cutout: '72%',
            plugins: { legend: { display: false } }
          }
        })
      }

      // 2. Combined trend chart (created + resolved)
      if (this.$refs.trendRef && this.hasWeeklyData && labels.length) {
        this.charts.trend = new ChartJS(this.$refs.trendRef, {
          type: 'line',
          data: {
            labels: labels.map(l => l.slice(5)), // show MM-DD
            datasets: [
              {
                label: '新建',
                data: this.weeklyCreated,
                borderColor: '#3370ff',
                backgroundColor: 'rgba(51,112,255,0.06)',
                fill: true, tension: 0.35, borderWidth: 2, pointRadius: 3, pointHoverRadius: 5
              },
              {
                label: '完成',
                data: this.weeklyResolved,
                borderColor: '#00b42a',
                backgroundColor: 'rgba(0,180,42,0.06)',
                fill: true, tension: 0.35, borderWidth: 2, pointRadius: 3, pointHoverRadius: 5
              }
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

      // 3. Importance bar
      if (this.$refs.barImpRef && this.importanceLabels.length) {
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
    wcStyle (it) {
      const max = it.max || 1; const w = it.weight || 0; const r = max > 0 ? w / max : 0
      const fs = 12 + 14 * r
      const palette = ['#1f2329', '#3370ff', '#00b42a', '#ff8800', '#7c3aed', '#f54a45']
      const idx = (it.text && it.text.charCodeAt(0) ? it.text.charCodeAt(0) : 0) % palette.length
      return { fontSize: fs + 'px', color: palette[idx], fontWeight: r >= 0.35 ? 600 : 400 }
    },
    pillCls (p) {
      if (!p) return ''
      const v = String(p)
      if (v.includes('100') || v.includes('完成') || v.includes('已上线')) return 'lb-pill--done'
      if (v.includes('进行') || v.includes('开发')) return 'lb-pill--wip'
      if (v.includes('待') || v.includes('规划') || v.includes('0')) return 'lb-pill--todo'
      return ''
    },
    async runAiPreview () {
      const raw = (this.aiText || '').trim()
      if (!raw) return
      this.aiBusy = true; this.aiDrafts = []
      try {
        const resp = await this.$http.post(`/public/client-board/${encodeURIComponent(this.token)}/ai-preview`, { rawText: raw, attachmentIds: [] })
        if (resp.data && resp.data.code === 0 && resp.data.data && Array.isArray(resp.data.data.items)) {
          this.aiDrafts = resp.data.data.items
        } else { window.alert((resp.data && resp.data.message) || 'AI 预览失败') }
      } catch (e) { window.alert('请求失败，请稍后重试') }
      finally { this.aiBusy = false }
    },
    async commitAi () {
      if (!this.aiDrafts.length) return
      this.aiCommitting = true
      try {
        const resp = await this.$http.post(`/public/client-board/${encodeURIComponent(this.token)}/ai-commit`, { tasks: this.aiDrafts })
        if (resp.data && resp.data.code === 0) {
          const n = resp.data.data && resp.data.data.createdCount != null ? resp.data.data.createdCount : 0
          window.alert('提交成功，已创建 ' + n + ' 条任务')
          this.aiDrafts = []; this.aiText = ''; await this.fetchBoard()
        } else { window.alert((resp.data && resp.data.message) || '提交失败') }
      } catch (e) { window.alert('请求失败，请稍后重试') }
      finally { this.aiCommitting = false }
    }
  }
}
</script>

<style scoped>
/* ================================================================
   Client Board — 字节系设计风格 (Lark/飞书 inspired)
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
.lb-nav-logo { width: 28px; height: 28px; background: var(--lb-blue); color: #fff; border-radius: 6px; display: flex; align-items: center; justify-content: center; font-size: 13px; font-weight: 700; }
.lb-nav-title { font-size: 15px; font-weight: 600; color: var(--lb-text-1); }
.lb-nav-tenant { font-size: 13px; color: var(--lb-text-2); padding: 4px 12px; background: var(--lb-bg); border-radius: 100px; }

/* --- Hero --- */
.lb-hero { background: var(--lb-bg-card); border-bottom: 1px solid var(--lb-border); }
.lb-hero-inner { max-width: 1120px; margin: 0 auto; padding: 28px 32px 24px; }
.lb-hero-meta { display: flex; align-items: center; gap: 12px; margin-bottom: 12px; }
.lb-tag { display: inline-flex; align-items: center; padding: 2px 8px; background: var(--lb-blue-light); color: var(--lb-blue); font-size: 12px; font-weight: 500; border-radius: 4px; }
.lb-hero-date { font-size: 12px; color: var(--lb-text-3); }
.lb-hero-title { margin: 0 0 8px; font-size: 24px; font-weight: 700; color: var(--lb-text-1); line-height: 1.3; }
.lb-hero-repo { margin: 0; font-size: 13px; color: var(--lb-text-2); font-family: 'SF Mono', Menlo, Monaco, Consolas, monospace; display: inline-flex; align-items: center; gap: 6px; padding: 3px 10px; background: var(--lb-bg); border-radius: 4px; }

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
.lb-chart-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; }
.lb-card-wide { grid-column: span 2; }
.lb-chart-wrap { height: 220px; position: relative; }

/* --- Word Cloud --- */
.lb-wc-body { line-height: 2.2; padding: 8px 0; }
.lb-wc-token { display: inline-block; margin: 2px 10px 2px 0; cursor: default; transition: opacity .15s; }
.lb-wc-token:hover { opacity: .65; }

/* --- Empty State --- */
.lb-empty-card { text-align: center; padding: 56px 24px; background: var(--lb-bg-card); border-radius: var(--lb-radius-lg); border: 1px solid var(--lb-border); }
.lb-empty-card svg { opacity: .35; margin-bottom: 12px; }
.lb-empty-title { margin: 0 0 8px; font-size: 15px; font-weight: 500; color: var(--lb-text-2); }
.lb-empty-desc { margin: 0; font-size: 13px; color: var(--lb-text-3); line-height: 1.6; }

/* --- Table --- */
.lb-table-wrap { overflow-x: auto; }
.lb-table { width: 100%; border-collapse: collapse; font-size: 13px; }
.lb-table th, .lb-table td { border-bottom: 1px solid var(--lb-border); padding: 10px 14px; text-align: left; }
.lb-table th { color: var(--lb-text-2); font-weight: 500; font-size: 12px; background: var(--lb-bg); }
.lb-table tbody tr:hover { background: #fafbfc; }

/* --- Pill --- */
.lb-pill { display: inline-block; padding: 2px 10px; border-radius: 100px; font-size: 12px; font-weight: 500; background: var(--lb-bg); color: var(--lb-text-2); }
.lb-pill--done { background: var(--lb-green-light); color: var(--lb-green); }
.lb-pill--wip { background: var(--lb-orange-light); color: var(--lb-orange); }
.lb-pill--todo { background: var(--lb-blue-light); color: var(--lb-blue); }

/* --- AI Section --- */
.lb-ai-desc { margin: 0 0 12px; font-size: 13px; color: var(--lb-text-3); line-height: 1.6; }
.lb-textarea { width: 100%; border: 1px solid var(--lb-border); border-radius: var(--lb-radius); padding: 10px 14px; font-size: 14px; font-family: inherit; color: var(--lb-text-1); background: var(--lb-bg-card); resize: vertical; box-sizing: border-box; outline: none; transition: border-color .2s, box-shadow .2s; line-height: 1.6; }
.lb-textarea:focus { border-color: var(--lb-blue); box-shadow: 0 0 0 2px rgba(51,112,255,.12); }
.lb-ai-footer { margin-top: 12px; }

/* --- AI Drafts --- */
.lb-drafts { margin-top: 16px; padding-top: 16px; border-top: 1px solid var(--lb-border); }
.lb-drafts-head { font-size: 13px; font-weight: 600; color: var(--lb-text-1); margin-bottom: 12px; }
.lb-draft { margin-bottom: 8px; padding: 12px 14px; background: var(--lb-bg); border-radius: var(--lb-radius); border: 1px solid transparent; transition: border-color .15s; }
.lb-draft:hover { border-color: var(--lb-blue); }
.lb-draft strong { font-size: 14px; color: var(--lb-text-1); }
.lb-draft p { margin: 4px 0 0; font-size: 13px; color: var(--lb-text-2); white-space: pre-wrap; line-height: 1.6; }
.lb-drafts-foot { margin-top: 12px; display: flex; justify-content: flex-end; }

/* --- Button --- */
.lb-btn { display: inline-flex; align-items: center; justify-content: center; gap: 6px; min-height: 32px; padding: 0 16px; border-radius: var(--lb-radius); font-size: 14px; font-weight: 500; cursor: pointer; border: none; background: var(--lb-blue); color: #fff; transition: background .15s; outline: none; line-height: 1; }
.lb-btn:hover { background: var(--lb-blue-hover); }
.lb-btn:active { background: #1d56d0; }
.lb-btn:disabled { opacity: .4; cursor: not-allowed; }
.lb-btn:focus-visible { box-shadow: 0 0 0 2px rgba(51,112,255,.25); }

/* --- Footer --- */
.lb-footer { text-align: center; padding: 32px 0 24px; font-size: 12px; color: var(--lb-text-3); display: flex; align-items: center; justify-content: center; gap: 8px; }
.lb-footer-brand { display: inline-flex; align-items: center; gap: 4px; font-weight: 600; color: var(--lb-text-2); }
.lb-footer-icon { width: 16px; height: 16px; background: var(--lb-blue); color: #fff; border-radius: 3px; display: inline-flex; align-items: center; justify-content: center; font-size: 8px; font-weight: 700; }
.lb-footer-sep { color: var(--lb-text-4); }

/* --- Responsive --- */
@media (max-width: 768px) {
  .lb-nav-inner { padding: 0 16px; }
  .lb-hero-inner { padding: 20px 16px 16px; }
  .lb-hero-title { font-size: 20px; }
  .lb-main { padding: 16px 16px 40px; }
  .lb-kpi-row { grid-template-columns: 1fr 1fr; gap: 12px; }
  .lb-kpi { padding: 14px; }
  .lb-kpi-value { font-size: 22px; }
  .lb-chart-grid { grid-template-columns: 1fr; }
  .lb-card-wide { grid-column: span 1; }
  .lb-completion-row { flex-direction: column; gap: 20px; }
  .lb-completion-chart { width: 120px; height: 120px; }
}
</style>
