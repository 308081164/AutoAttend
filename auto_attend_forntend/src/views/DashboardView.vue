<template>
  <div class="dashboard">
    <section class="section section-header-bar">
      <div class="header-actions">
        <router-link to="/team" class="link-button">{{ $t('teamManage.navTitle') }}</router-link>
        <router-link to="/ai-config" class="link-button">{{ $t('aiConfig.navTitle') }}</router-link>
        <router-link to="/test" class="link-button test-entry">{{ $t('test.title') }}</router-link>
        <router-link to="/collab/projects" class="link-button collab-entry">{{ $t('dashboard.collabEntry') }}</router-link>
        <div class="repo-filter">
          <label>{{ $t('dashboard.project') }}：</label>
          <select v-model="selectedRepo" @change="onRepoChange">
            <option value="">{{ $t('dashboard.allProjects') }}</option>
            <option v-for="repo in repos" :key="repo" :value="repo">{{ repo }}</option>
          </select>
        </div>
      </div>
    </section>

    <!-- 顶部：选中项目时展示项目基本信息，否则展示资源总览 -->
    <section class="section overview-section">
      <h2 class="section-title">{{ selectedRepo ? $t('dashboard.projectInfoTitle') : $t('dashboard.overviewTitle') }}</h2>
      <!-- 已选项目：项目名称、简介、开发者、技术栈 -->
      <div v-if="selectedRepo" class="project-info-cards">
        <div v-if="repoInfoLoading" class="placeholder">{{ $t('collab.loading') }}</div>
        <template v-else>
          <div class="project-info-row">
            <span class="project-info-label">{{ $t('dashboard.projectName') }}：</span>
            <a v-if="repoInfo.htmlUrl" :href="repoInfo.htmlUrl" target="_blank" rel="noopener noreferrer" class="project-info-name">{{ repoInfo.name || repoInfo.fullName || selectedRepo }}</a>
            <span v-else class="project-info-name">{{ repoInfo.name || repoInfo.fullName || selectedRepo }}</span>
          </div>
          <div v-if="repoInfo.description" class="project-info-row">
            <span class="project-info-label">{{ $t('dashboard.about') }}：</span>
            <span class="project-info-desc">{{ repoInfo.description }}</span>
          </div>
          <div class="project-info-row">
            <span class="project-info-label">{{ $t('dashboard.projectDevelopers') }}：</span>
            <span class="project-info-developers">{{ developerListText }}</span>
          </div>
          <div v-if="languageList.length" class="project-info-row">
            <span class="project-info-label">{{ $t('dashboard.techStack') }}：</span>
            <span class="project-info-languages">{{ languageList.join('、') }}</span>
          </div>
        </template>
      </div>
      <!-- 未选项目：资源总览三卡片或加载占位 -->
      <template v-else>
        <div v-if="statsOverview" class="overview-cards">
          <div class="overview-card">
            <div class="overview-value">{{ statsOverview.repoCount ?? 0 }}</div>
            <div class="overview-label">{{ $t('dashboard.repoCount') }}</div>
          </div>
          <div class="overview-card">
            <div class="overview-value">{{ statsOverview.totalCommits ?? 0 }}</div>
            <div class="overview-label">{{ $t('dashboard.totalCommits') }}</div>
          </div>
          <div class="overview-card">
            <div class="overview-value">{{ statsOverview.authorCount ?? 0 }}</div>
            <div class="overview-label">{{ $t('dashboard.authorCount') }}</div>
          </div>
        </div>
        <div v-else class="placeholder">{{ $t('collab.loading') }}</div>
      </template>
    </section>

    <!-- 图表区（仅提交趋势 + 开发者排名，已移除各仓库提交占比） -->
    <div class="charts-row">
      <section class="section chart-section chart-section-full">
        <h2 class="section-title">{{ $t('dashboard.commitTrend') }}</h2>
        <div class="chart-wrap">
          <canvas ref="trendChart"></canvas>
        </div>
        <div class="chart-legend">
          <button :class="{ active: trendRange === '7d' }" @click="trendRange = '7d'; loadStatsCommitsByDay()">{{ $t('dashboard.commitTrendRange') }}</button>
          <button :class="{ active: trendRange === '30d' }" @click="trendRange = '30d'; loadStatsCommitsByDay()">{{ $t('dashboard.commitTrendRange30') }}</button>
        </div>
      </section>
    </div>
    <div class="charts-row">
      <section class="section chart-section chart-section-wide">
        <h2 class="section-title">{{ $t('dashboard.authorRanking') }}</h2>
        <div class="chart-wrap chart-wrap-bar">
          <canvas ref="authorChart"></canvas>
        </div>
      </section>
    </div>

    <!-- 最近提交表格 -->
    <section class="section">
      <div class="section-header">
        <h2 class="section-title">{{ $t('dashboard.recentCommits') }}</h2>
        <button class="link-button" @click="loadCommits" :disabled="commitsLoading">{{ $t('dashboard.refresh') }}</button>
      </div>
      <div class="table-wrapper" v-if="commits.length">
        <table class="table">
          <thead>
            <tr>
              <th>{{ $t('dashboard.repo') }}</th>
              <th>{{ $t('dashboard.commit') }}</th>
              <th>{{ $t('dashboard.author') }}</th>
              <th>{{ $t('dashboard.time') }}</th>
              <th class="num">{{ $t('dashboard.filesChanged') }}</th>
              <th class="num">{{ $t('dashboard.insertions') }}</th>
              <th class="num">{{ $t('dashboard.deletions') }}</th>
              <th>{{ $t('dashboard.message') }}</th>
              <th>{{ $t('dashboard.actions') }}</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="item in commits" :key="item.commitSha">
              <td>{{ item.repoFullName }}</td>
              <td class="mono">{{ shortSha(item.commitSha) }}</td>
              <td>{{ item.authorName || '-' }}</td>
              <td>{{ formatTime(item.committedAt) }}</td>
              <td class="num">{{ item.filesChanged != null ? item.filesChanged : '-' }}</td>
              <td class="num add">{{ item.insertions != null ? item.insertions : '-' }}</td>
              <td class="num del">{{ item.deletions != null ? item.deletions : '-' }}</td>
              <td>{{ item.message }}</td>
              <td>
                <button class="link-button" @click="viewDiff(item)">{{ $t('dashboard.viewDiff') }}</button>
                <router-link :to="commitAnalysisRoute(item)" class="link-button commit-analysis-link">{{ $t('dashboard.commitAnalysisBoard') }}</router-link>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
      <div v-else class="placeholder">{{ $t('dashboard.noCommits') }}</div>
    </section>

    <section class="section" v-if="authors.length">
      <h2 class="section-title">{{ $t('dashboard.developers') }}</h2>
      <div class="table-wrapper">
        <table class="table">
          <thead>
          <tr>
            <th>{{ $t('dashboard.author') }}</th>
            <th>{{ $t('dashboard.email') }}</th>
            <th>{{ $t('dashboard.commits') }}</th>
            <th>{{ $t('dashboard.lastCommit') }}</th>
          </tr>
          </thead>
          <tbody>
          <tr v-for="a in authors" :key="a.authorEmail || a.authorName">
            <td>{{ a.authorName || '-' }}</td>
            <td>{{ a.authorEmail || '-' }}</td>
            <td>{{ a.commitCount }}</td>
            <td>{{ formatTime(a.lastCommittedAt) }}</td>
          </tr>
          </tbody>
        </table>
      </div>
    </section>

    <section v-if="selectedCommit" class="section">
      <div class="section-header">
        <h2 class="section-title">
          {{ $t('dashboard.diffTitle') }}：{{ selectedCommit.repoFullName }} @ {{ shortSha(selectedCommit.commitSha) }}
        </h2>
        <div class="diff-actions">
          <router-link v-if="selectedCommit" :to="commitAnalysisRoute(selectedCommit)" class="link-button">{{ $t('dashboard.commitAnalysisBoard') }}</router-link>
          <button v-if="diffText" class="link-button" @click="copyDiffForAi">{{ $t('dashboard.copyForAi') }}</button>
          <button class="link-button" @click="selectedCommit = null">{{ $t('dashboard.collapse') }}</button>
        </div>
      </div>
      <div class="diff-box" v-if="diffLoading">
        {{ $t('dashboard.loadingDiff') }}
      </div>
      <div v-else-if="diffText" class="diff-box-wrapper">
        <div class="diff-box diff-content" v-html="diffHtml"></div>
        <div v-if="isDiffPlaceholder(diffText)" class="diff-placeholder-actions">
          <button type="button" class="primary-button small" :disabled="diffLoading" @click="retryFetchDiff">{{ $t('dashboard.retryFetchDiff') }}</button>
          <a :href="githubCommitUrl" target="_blank" rel="noopener noreferrer" class="view-on-github-link">{{ $t('dashboard.viewOnGitHub') }}</a>
        </div>
      </div>
      <pre v-else class="diff-box"><code>{{ $t('dashboard.diffNotReady') }}</code></pre>

      <div class="ai-analysis-section">
        <h3 class="ai-analysis-title">{{ $t('dashboard.aiAnalysisTitle') }}</h3>
        <div v-if="aiAnalysisLoading" class="ai-analysis-loading">{{ $t('dashboard.loading') }}...</div>
        <div v-else-if="aiAnalysisResult" class="ai-analysis-result">
          <div class="ai-analysis-row">
            <span class="ai-analysis-label">{{ $t('dashboard.aiWorkSummary') }}</span>
            <span>{{ aiAnalysisResult.workSummary || '—' }}</span>
          </div>
          <div class="ai-analysis-row">
            <span class="ai-analysis-label">{{ $t('dashboard.aiWorkType') }}</span>
            <span>{{ aiAnalysisResult.workType || '—' }}</span>
          </div>
          <div class="ai-analysis-row">
            <span class="ai-analysis-label">{{ $t('dashboard.aiIsEffective') }}</span>
            <span>{{ aiAnalysisResult.isEffective || '—' }}</span>
          </div>
          <div class="ai-analysis-row" v-if="aiAnalysisResult.effectiveReason">
            <span class="ai-analysis-label">{{ $t('dashboard.aiEffectiveReason') }}</span>
            <span>{{ aiAnalysisResult.effectiveReason }}</span>
          </div>
          <div class="ai-analysis-row">
            <span class="ai-analysis-label">{{ $t('dashboard.aiQualityLevel') }}</span>
            <span>{{ aiAnalysisResult.qualityLevel || '—' }}</span>
          </div>
          <div class="ai-analysis-row" v-if="aiAnalysisResult.qualityComment">
            <span class="ai-analysis-label">{{ $t('dashboard.aiQualityComment') }}</span>
            <span>{{ aiAnalysisResult.qualityComment }}</span>
          </div>
        </div>
        <div v-else class="ai-analysis-actions">
          <button type="button" class="primary-button small" :disabled="aiAnalysisRunning" @click="runAiAnalysis">
            {{ aiAnalysisRunning ? $t('dashboard.aiRunning') : $t('dashboard.runAiAnalysis') }}
          </button>
          <p class="ai-analysis-hint">{{ $t('dashboard.aiAnalysisHint') }}</p>
        </div>
      </div>
    </section>
  </div>
</template>

<script>
import { Chart as ChartJS, registerables } from 'chart.js'

ChartJS.register(...registerables)

export default {
  name: 'DashboardView',
  data () {
    return {
      dashboard: null,
      commits: [],
      commitsLoading: false,
      selectedCommit: null,
      diffText: '',
      diffMeta: null,
      diffLoading: false,
      repos: [],
      selectedRepo: '',
      authors: [],
      aiAnalysisResult: null,
      aiAnalysisLoading: false,
      aiAnalysisRunning: false,
      statsOverview: null,
      commitsByDay: [],
      authorsStats: [],
      trendRange: '7d',
      chartInstances: { trend: null, author: null },
      repoInfo: {},
      repoInfoLoading: false
    }
  },
  watch: {
    selectedCommit (v) {
      this.aiAnalysisResult = null
      if (v) this.loadAiAnalysisResult()
    },
    selectedRepo () {
      if (this.selectedRepo) this.loadRepoInfo()
      else this.repoInfo = {}
    },
    commitsByDay () { this.renderTrendChart() },
    authorsStats () { this.renderAuthorChart() }
  },
  computed: {
    developerListText () {
      const list = this.authors || []
      if (!list.length) return '—'
      return list.map(a => a.authorName || a.authorEmail || '—').join('、')
    },
    languageList () {
      const lang = this.repoInfo.languages
      if (!lang || typeof lang !== 'object') return []
      return Object.keys(lang)
    },
    githubCommitUrl () {
      if (!this.selectedCommit) return ''
      const repo = this.selectedCommit.repoFullName || ''
      const sha = this.selectedCommit.commitSha || ''
      if (!repo || !sha) return ''
      return `https://github.com/${repo}/commit/${sha}`
    },
    commitAnalysisRoute (item) {
      if (!item || !item.commitSha) return { path: '/' }
      return {
        path: `/commit/${item.commitSha}`,
        query: item.repoFullName ? { repoFullName: item.repoFullName } : {}
      }
    },
    diffHtml () {
      if (!this.diffText) return ''
      return this.diffText.split('\n').map(line => {
        const escaped = this.escapeHtml(line || ' ')
        if (line.startsWith('+') && !line.startsWith('+++')) return '<span class="diff-line diff-add">' + escaped + '</span>'
        if (line.startsWith('-') && !line.startsWith('---')) return '<span class="diff-line diff-del">' + escaped + '</span>'
        return '<span class="diff-line">' + escaped + '</span>'
      }).join('\n')
    }
  },
  created () {
    this.loadDashboard()
    this.loadCommits().then(() => this.tryOpenCommitFromQuery())
    this.loadStatsOverview()
    this.loadStatsCommitsByDay()
    this.loadStatsAuthors()
    this.loadRepos().then(() => {
      if (this.selectedRepo) this.loadRepoInfo()
    }).catch(() => {})
  },
  mounted () {
    this.$nextTick(() => {
      setTimeout(() => {
        this.renderTrendChart()
        this.renderAuthorChart()
      }, 100)
    })
  },
  beforeDestroy () {
    const chartKeys = ['trend', 'author']
    chartKeys.forEach(k => {
      if (this.chartInstances[k]) {
        this.chartInstances[k].destroy()
        this.chartInstances[k] = null
      }
    })
  },
  methods: {
    async loadStatsOverview () {
      try {
        const resp = await this.$http.get('/admin/stats/overview', {
          params: { repoFullName: this.selectedRepo || undefined }
        })
        if (resp.data && resp.data.code === 0) this.statsOverview = resp.data.data
      } catch (e) { console.error('loadStatsOverview failed', e) }
    },
    async loadRepoInfo () {
      if (!this.selectedRepo) return
      this.repoInfoLoading = true
      this.repoInfo = {}
      try {
        const resp = await this.$http.get('/admin/stats/repo-info', {
          params: { repoFullName: this.selectedRepo }
        })
        if (resp.data && resp.data.code === 0 && resp.data.data) this.repoInfo = resp.data.data
      } catch (e) { console.error('loadRepoInfo failed', e) }
      finally { this.repoInfoLoading = false }
    },
    async loadStatsCommitsByDay () {
      try {
        const resp = await this.$http.get('/admin/stats/commits-by-day', {
          params: { range: this.trendRange, repoFullName: this.selectedRepo || undefined }
        })
        if (resp.data && resp.data.code === 0) this.commitsByDay = resp.data.data || []
      } catch (e) { console.error('loadStatsCommitsByDay failed', e) }
    },
    async loadStatsAuthors () {
      try {
        const resp = await this.$http.get('/admin/stats/authors', {
          params: { repoFullName: this.selectedRepo || undefined }
        })
        if (resp.data && resp.data.code === 0) this.authorsStats = (resp.data.data || []).slice(0, 15)
      } catch (e) { console.error('loadStatsAuthors failed', e) }
    },
    renderTrendChart () {
      this.$nextTick(() => {
        const el = this.$refs.trendChart
        if (!el) return
        if (this.chartInstances.trend) this.chartInstances.trend.destroy()
        const days = this.trendRange === '30d' ? 30 : 7
        const formatDayLabel = (d) => {
          const v = d.date
          if (v == null) return ''
          if (typeof v === 'string') return v.slice(0, 10).slice(5)
          if (typeof v === 'number') {
            const t = new Date(v)
            return String(t.getMonth() + 1).padStart(2, '0') + '-' + String(t.getDate()).padStart(2, '0')
          }
          return String(v).slice(0, 10).slice(5)
        }
        let labels = (this.commitsByDay || []).map(formatDayLabel)
        let counts = (this.commitsByDay || []).map(d => d.count || 0)
        if (labels.length === 0) {
          const d = new Date()
          for (let i = days - 1; i >= 0; i--) {
            const t = new Date(d)
            t.setDate(t.getDate() - i)
            const m = String(t.getMonth() + 1).padStart(2, '0')
            const day = String(t.getDate()).padStart(2, '0')
            labels.push(m + '-' + day)
            counts.push(0)
          }
        }
        this.chartInstances.trend = new ChartJS(el, {
          type: 'bar',
          data: {
            labels,
            datasets: [
              { label: this.$t('dashboard.recentCommits'), data: counts, backgroundColor: 'rgba(37, 99, 235, 0.7)', borderColor: 'rgb(37, 99, 235)', borderWidth: 1 }
            ]
          },
          options: {
            responsive: true,
            maintainAspectRatio: true,
            plugins: { legend: { display: false } },
            scales: {
              y: { beginAtZero: true, ticks: { stepSize: 1 } }
            }
          }
        })
      })
    },
    renderAuthorChart () {
      this.$nextTick(() => {
        const el = this.$refs.authorChart
        if (!el) return
        if (this.chartInstances.author) this.chartInstances.author.destroy()
        const data = this.authorsStats || []
        const noDataLabel = '暂无数据'
        const labels = data.length ? data.map(a => a.authorName || a.authorEmail || '') : [noDataLabel]
        const counts = data.length ? data.map(a => a.commitCount || 0) : [0]
        this.chartInstances.author = new ChartJS(el, {
          type: 'bar',
          data: {
            labels,
            datasets: [{ label: this.$t('dashboard.commits'), data: counts, backgroundColor: 'rgba(5, 150, 105, 0.7)', borderColor: 'rgb(5, 150, 105)', borderWidth: 1 }]
          },
          options: {
            indexAxis: 'y',
            responsive: true,
            maintainAspectRatio: true,
            plugins: { legend: { display: false } },
            scales: {
              x: { beginAtZero: true, ticks: { stepSize: 1 } }
            }
          }
        })
      })
    },
    async loadRepos () {
      try {
        const resp = await this.$http.get('/admin/repos')
        if (resp.data && resp.data.code === 0) {
          this.repos = resp.data.data.items || []
        }
      } catch (e) {
        console.error('loadRepos failed', e)
      }
    },
    onRepoChange () {
      this.loadDashboard()
      this.loadCommits()
      this.loadStatsOverview()
      this.loadStatsCommitsByDay()
      this.loadStatsAuthors()
      if (this.selectedRepo) this.loadRepoInfo()
      else this.repoInfo = {}
    },
    async loadDashboard () {
      try {
        const resp = await this.$http.get('/admin/dashboard', {
          params: { range: '24h', repoFullName: this.selectedRepo || undefined }
        })
        const d = resp.data
        if (!d) return
        const ok = d.code === 0 || d.code === '0'
        const data = d.data || d
        if (!ok || !data) return
        this.dashboard = data
        this.authors = data.authors || []
        // 以 dashboard 的 data.commits 为主数据源，确保表格有数据（/admin/commits 为 data.items，两套接口字段名不同）
        if (data.commits && Array.isArray(data.commits)) {
          this.commits = data.commits
        }
      } catch (e) {
        console.error('loadDashboard failed', e)
      }
    },
    async loadCommits () {
      this.commitsLoading = true
      try {
        const resp = await this.$http.get('/admin/commits', {
          params: {
            page: 1,
            pageSize: 50,
            repoFullName: this.selectedRepo || undefined
          }
        })
        const d = resp.data
        if (!d) { this.commitsLoading = false; return }
        const ok = d.code === 0 || d.code === '0'
        const payload = d.data != null ? d.data : d
        const list = (payload && payload.items) || d.items
        if (ok && Array.isArray(list)) {
          this.commits = list
        }
      } catch (e) {
        console.error('loadCommits failed', e)
      } finally {
        this.commitsLoading = false
      }
    },
    isDiffPlaceholder (text) {
      return text && (text.indexOf('(Diff 暂不可用') !== -1 || text.indexOf('(Diff will be') !== -1)
    },
    shortSha (sha) {
      if (!sha) return ''
      return sha.substring(0, 8)
    },
    formatTime (t) {
      if (!t) return ''
      try {
        return new Date(t).toLocaleString()
      } catch (e) {
        return t
      }
    },
    tryOpenCommitFromQuery () {
      const q = this.$route.query
      if (!q || !q.commitSha || !q.repoFullName || !this.commits.length) return
      const item = this.commits.find(c => c.commitSha === q.commitSha && c.repoFullName === q.repoFullName)
      if (item) this.viewDiff(item)
    },
    retryFetchDiff () {
      if (this.selectedCommit) this.viewDiff(this.selectedCommit)
    },
    async loadAiAnalysisResult () {
      if (!this.selectedCommit) return
      this.aiAnalysisLoading = true
      this.aiAnalysisResult = null
      try {
        const params = {}
        if (this.selectedCommit.repoFullName) params.repoFullName = this.selectedCommit.repoFullName
        const resp = await this.$http.get(`/admin/ai-analysis/commits/${this.selectedCommit.commitSha}/result`, { params })
        if (resp.data && resp.data.code === 0 && resp.data.data) {
          this.aiAnalysisResult = resp.data.data
        }
      } catch (e) {
        if (e.response && e.response.status !== 404) console.error('loadAiAnalysisResult failed', e)
      } finally {
        this.aiAnalysisLoading = false
      }
    },
    async runAiAnalysis () {
      if (!this.selectedCommit || this.aiAnalysisRunning) return
      this.aiAnalysisRunning = true
      try {
        const params = {}
        if (this.selectedCommit.repoFullName) params.repoFullName = this.selectedCommit.repoFullName
        const resp = await this.$http.post(`/admin/ai-analysis/commits/${this.selectedCommit.commitSha}/run`, null, { params })
        if (resp.data && resp.data.code === 0 && resp.data.data) {
          this.aiAnalysisResult = resp.data.data
        } else {
          alert((resp.data && resp.data.message) || this.$t('dashboard.aiRunFailed'))
        }
      } catch (e) {
        alert((e.response && e.response.data && e.response.data.message) || this.$t('dashboard.aiRunFailed'))
      } finally {
        this.aiAnalysisRunning = false
      }
    },
    async viewDiff (item) {
      this.selectedCommit = item
      this.diffText = ''
      this.diffMeta = null
      this.diffLoading = true
      try {
        const params = { mode: 'raw' }
        if (item.repoFullName) params.repoFullName = item.repoFullName
        const resp = await this.$http.get(`/admin/commits/${item.commitSha}/diff`, { params })
        if (resp.data && resp.data.code === 0) {
          const d = resp.data.data
          this.diffText = (d && d.diffText) || ''
          this.diffMeta = d ? { repoFullName: d.repoFullName, commitSha: d.commitSha, message: d.message, authorName: d.authorName, authorEmail: d.authorEmail, committedAt: d.committedAt } : null
        } else {
          this.diffText = this.$t('dashboard.diffLoadFailed')
        }
      } catch (e) {
        this.diffText = this.$t('dashboard.diffLoadFailed')
      } finally {
        this.diffLoading = false
        this.loadAiAnalysisResult().then(() => {
          if (!this.aiAnalysisResult && this.diffText && !this.isDiffPlaceholder(this.diffText)) this.runAiAnalysis()
        })
      }
    },
    escapeHtml (s) {
      const div = document.createElement('div')
      div.textContent = s
      return div.innerHTML
    },
    copyDiffForAi () {
      const meta = this.diffMeta || {}
      const header = [
        `仓库: ${meta.repoFullName || this.selectedCommit?.repoFullName || ''}`,
        `提交: ${meta.commitSha || this.selectedCommit?.commitSha || ''}`,
        `说明: ${meta.message || ''}`,
        `作者: ${meta.authorName || ''} <${meta.authorEmail || ''}>`,
        `时间: ${meta.committedAt || ''}`,
        '--- diff ---'
      ].join('\n')
      const text = header + '\n' + (this.diffText || '')
      if (navigator.clipboard && navigator.clipboard.writeText) {
        navigator.clipboard.writeText(text).then(() => {
          alert(this.$t('dashboard.copied'))
        }).catch(() => {
          this.fallbackCopy(text)
        })
      } else {
        this.fallbackCopy(text)
      }
    },
    fallbackCopy (text) {
      const ta = document.createElement('textarea')
      ta.value = text
      document.body.appendChild(ta)
      ta.select()
      try {
        document.execCommand('copy')
        alert(this.$t('dashboard.copied'))
      } catch (e) {
        alert(this.$t('dashboard.copyFailed'))
      }
      document.body.removeChild(ta)
    }
  }
}
</script>

<style scoped>
.dashboard {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.section {
  background-color: #ffffff;
  border-radius: 8px;
  padding: 16px 20px 20px;
  box-shadow: 0 1px 3px rgba(15, 23, 42, 0.1);
}

.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}

.section-title {
  margin: 0 0 12px;
  font-size: 16px;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.collab-entry {
  color: #2563eb;
  text-decoration: none;
  font-size: 14px;
}

.repo-filter {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
}

.repo-filter select {
  padding: 4px 6px;
  border-radius: 4px;
  border: 1px solid #d1d5db;
  font-size: 13px;
}

.section-header-bar {
  padding-top: 8px;
  padding-bottom: 8px;
}

.overview-section .section-title {
  margin-bottom: 12px;
}

.overview-cards {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
}

.overview-card {
  background: linear-gradient(135deg, #eff6ff 0%, #dbeafe 100%);
  border-radius: 10px;
  padding: 20px;
  text-align: center;
  border: 1px solid #bfdbfe;
}

.overview-card:nth-child(2) {
  background: linear-gradient(135deg, #ecfdf5 0%, #d1fae5 100%);
  border-color: #a7f3d0;
}

.overview-card:nth-child(3) {
  background: linear-gradient(135deg, #f5f3ff 0%, #ede9fe 100%);
  border-color: #c4b5fd;
}

.overview-value {
  font-size: 28px;
  font-weight: 700;
  color: #1e293b;
  line-height: 1.2;
}

.overview-label {
  font-size: 13px;
  color: #64748b;
  margin-top: 4px;
}

.project-info-cards {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.project-info-row {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  font-size: 14px;
}
.project-info-label {
  flex-shrink: 0;
  color: #64748b;
  min-width: 80px;
}
.project-info-name {
  font-weight: 600;
  color: #2563eb;
  text-decoration: none;
}
.project-info-name:hover {
  text-decoration: underline;
}
.project-info-desc {
  color: #475569;
  line-height: 1.5;
}
.project-info-developers,
.project-info-languages {
  color: #334155;
}

.charts-row {
  display: grid;
  grid-template-columns: 1fr;
  gap: 20px;
}

.chart-section-full {
  grid-column: 1 / -1;
}

@media (max-width: 900px) {
  .overview-cards {
    grid-template-columns: 1fr;
  }
}

.chart-section {
  min-height: 260px;
}

.chart-section-wide {
  grid-column: 1 / -1;
}

.chart-wrap {
  height: 220px;
  position: relative;
}

.chart-wrap-pie {
  height: 240px;
}

.chart-section-full .chart-wrap {
  height: 260px;
}

.chart-wrap-bar {
  height: 360px;
}

.chart-legend {
  margin-top: 8px;
  display: flex;
  gap: 8px;
}

.chart-legend button {
  padding: 4px 10px;
  font-size: 12px;
  border-radius: 4px;
  border: 1px solid #e2e8f0;
  background: #f8fafc;
  cursor: pointer;
}

.chart-legend button.active {
  background: #2563eb;
  color: #fff;
  border-color: #2563eb;
}

.table .num {
  text-align: right;
  font-variant-numeric: tabular-nums;
}

.table .num.add { color: #059669; }
.table .num.del { color: #dc2626; }

.summary-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.summary-card {
  border-radius: 6px;
  padding: 10px 12px;
  background-color: #f3f4f6;
}

.summary-card .label {
  font-size: 12px;
  color: #6b7280;
}

.summary-card .value {
  margin-top: 4px;
  font-size: 18px;
  font-weight: 600;
}

.table-wrapper {
  width: 100%;
  overflow-x: auto;
}

.table {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
}

.table th,
.table td {
  padding: 8px 10px;
  border-bottom: 1px solid #e5e7eb;
  text-align: left;
}

.table th {
  background-color: #f9fafb;
  font-weight: 500;
  color: #4b5563;
}

.table tbody tr:hover {
  background-color: #f3f4f6;
}

.mono {
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, "Liberation Mono", "Courier New", monospace;
}

.link-button {
  border: none;
  background: transparent;
  color: #2563eb;
  cursor: pointer;
  padding: 0;
  font-size: 13px;
}

.link-button:hover {
  text-decoration: underline;
}

.placeholder {
  font-size: 13px;
  color: #6b7280;
}

.diff-box-wrapper {
  margin-top: 8px;
}

.diff-placeholder-actions {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-top: 12px;
  flex-wrap: wrap;
}

.diff-box-wrapper .view-on-github-link {
  display: inline-block;
  font-size: 13px;
  color: #2563eb;
}

.diff-box-wrapper .view-on-github-link:hover {
  text-decoration: underline;
}

.diff-box {
  margin-top: 8px;
  max-height: 400px;
  overflow: auto;
  padding: 12px;
  border-radius: 4px;
  background-color: #111827;
  color: #e5e7eb;
  font-size: 12px;
}

.diff-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.diff-content {
  white-space: pre-wrap;
  word-break: break-all;
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, "Liberation Mono", "Courier New", monospace;
}

.diff-line {
  display: block;
}

.diff-add {
  color: #34d399;
  background-color: rgba(52, 211, 153, 0.08);
}

.diff-del {
  color: #f87171;
  background-color: rgba(248, 113, 113, 0.08);
}

.ai-analysis-section {
  margin-top: 24px;
  padding-top: 16px;
  border-top: 1px solid #e5e7eb;
}
.ai-analysis-title {
  font-size: 15px;
  margin: 0 0 12px;
  color: #374151;
}
.ai-analysis-loading {
  font-size: 13px;
  color: #6b7280;
}
.ai-analysis-result {
  background: #f9fafb;
  border-radius: 8px;
  padding: 12px 16px;
  font-size: 14px;
}
.ai-analysis-row {
  margin-bottom: 8px;
}
.ai-analysis-row:last-child {
  margin-bottom: 0;
}
.ai-analysis-label {
  display: inline-block;
  min-width: 100px;
  color: #6b7280;
  margin-right: 8px;
}
.ai-analysis-actions {
  margin-top: 8px;
}
.ai-analysis-hint {
  font-size: 12px;
  color: #9ca3af;
  margin: 8px 0 0;
}
</style>

