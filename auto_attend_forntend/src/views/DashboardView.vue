<template>
  <div class="dashboard">
    <section class="section">
      <div class="section-header">
        <h2 class="section-title">{{ $t('dashboard.statusOverview') }}</h2>
        <div class="header-actions">
          <router-link to="/test" class="link-button test-entry">{{ $t('test.title') }}</router-link>
          <router-link to="/collab/projects" class="link-button collab-entry">{{ $t('dashboard.collabEntry') }}</router-link>
          <div class="repo-filter">
            <label>{{ $t('dashboard.project') }}：</label>
            <select v-model="selectedRepo" @change="onRepoChange">
            <option value="">{{ $t('dashboard.allProjects') }}</option>
            <option v-for="repo in repos" :key="repo" :value="repo">
              {{ repo }}
            </option>
          </select>
          </div>
        </div>
      </div>
      <div class="summary-grid" v-if="dashboard">
        <div class="summary-card">
          <div class="label">{{ $t('dashboard.activeCoding') }}</div>
          <div class="value">{{ dashboard.summary.activeCoding || 0 }}</div>
        </div>
        <div class="summary-card">
          <div class="label">{{ $t('dashboard.inReview') }}</div>
          <div class="value">{{ dashboard.summary.inReview || 0 }}</div>
        </div>
        <div class="summary-card">
          <div class="label">{{ $t('dashboard.reviewingOthers') }}</div>
          <div class="value">{{ dashboard.summary.reviewingOthers || 0 }}</div>
        </div>
        <div class="summary-card">
          <div class="label">{{ $t('dashboard.ciFixing') }}</div>
          <div class="value">{{ dashboard.summary.ciFixing || 0 }}</div>
        </div>
        <div class="summary-card">
          <div class="label">{{ $t('dashboard.blocked') }}</div>
          <div class="value">{{ dashboard.summary.blocked || 0 }}</div>
        </div>
        <div class="summary-card">
          <div class="label">{{ $t('dashboard.idle') }}</div>
          <div class="value">{{ dashboard.summary.idle || 0 }}</div>
        </div>
      </div>
      <div v-else class="placeholder">{{ $t('collab.loading') }}</div>
    </section>

    <section class="section">
      <div class="section-header">
        <h2 class="section-title">{{ $t('dashboard.recentCommits') }}</h2>
        <button class="link-button" @click="loadCommits" :disabled="commitsLoading">
          {{ $t('dashboard.refresh') }}
        </button>
      </div>
      <div class="table-wrapper" v-if="commits.length">
        <table class="table">
          <thead>
          <tr>
            <th>{{ $t('dashboard.repo') }}</th>
            <th>{{ $t('dashboard.commit') }}</th>
            <th>{{ $t('dashboard.author') }}</th>
            <th>{{ $t('dashboard.time') }}</th>
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
            <td>{{ item.message }}</td>
            <td>
              <button class="link-button" @click="viewDiff(item)">{{ $t('dashboard.viewDiff') }}</button>
            </td>
          </tr>
          </tbody>
        </table>
      </div>
      <div v-else class="placeholder">
        {{ $t('dashboard.noCommits') }}
      </div>
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
    </section>
  </div>
</template>

<script>
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
      authors: []
    }
  },
  computed: {
    githubCommitUrl () {
      if (!this.selectedCommit) return ''
      const repo = this.selectedCommit.repoFullName || ''
      const sha = this.selectedCommit.commitSha || ''
      if (!repo || !sha) return ''
      return `https://github.com/${repo}/commit/${sha}`
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
    this.loadRepos().then(() => {
      this.loadDashboard()
      this.loadCommits()
    })
  },
  methods: {
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
    },
    async loadDashboard () {
      try {
        const resp = await this.$http.get('/admin/dashboard', {
          params: { range: '24h', repoFullName: this.selectedRepo || undefined }
        })
        if (resp.data && resp.data.code === 0) {
          this.dashboard = resp.data.data
          this.authors = resp.data.data.authors || []
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
        if (resp.data && resp.data.code === 0) {
          this.commits = resp.data.data.items || []
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
    retryFetchDiff () {
      if (this.selectedCommit) this.viewDiff(this.selectedCommit)
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
</style>

