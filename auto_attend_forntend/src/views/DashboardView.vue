<template>
  <div class="dashboard">
    <section class="section">
      <div class="section-header">
        <h2 class="section-title">状态总览（Status Overview）</h2>
        <div class="repo-filter">
          <label>项目（Project）：</label>
          <select v-model="selectedRepo" @change="onRepoChange">
            <option value="">全部项目（All Projects）</option>
            <option v-for="repo in repos" :key="repo" :value="repo">
              {{ repo }}
            </option>
          </select>
        </div>
      </div>
      <div class="summary-grid" v-if="dashboard">
        <div class="summary-card">
          <div class="label">活跃开发（Active Coding）</div>
          <div class="value">{{ dashboard.summary.activeCoding || 0 }}</div>
        </div>
        <div class="summary-card">
          <div class="label">评审中（In Review）</div>
          <div class="value">{{ dashboard.summary.inReview || 0 }}</div>
        </div>
        <div class="summary-card">
          <div class="label">评审他人（Reviewing Others）</div>
          <div class="value">{{ dashboard.summary.reviewingOthers || 0 }}</div>
        </div>
        <div class="summary-card">
          <div class="label">修复构建（CI Fixing）</div>
          <div class="value">{{ dashboard.summary.ciFixing || 0 }}</div>
        </div>
        <div class="summary-card">
          <div class="label">阻塞（Blocked）</div>
          <div class="value">{{ dashboard.summary.blocked || 0 }}</div>
        </div>
        <div class="summary-card">
          <div class="label">无信号（Idle）</div>
          <div class="value">{{ dashboard.summary.idle || 0 }}</div>
        </div>
      </div>
      <div v-else class="placeholder">加载中...</div>
    </section>

    <section class="section">
      <div class="section-header">
        <h2 class="section-title">提交列表（最近 / Recent Commits）</h2>
        <button class="link-button" @click="loadCommits" :disabled="commitsLoading">
          刷新
        </button>
      </div>
      <div class="table-wrapper" v-if="commits.length">
        <table class="table">
          <thead>
          <tr>
            <th>仓库（Repo）</th>
            <th>提交哈希（Commit）</th>
            <th>作者（Author）</th>
            <th>时间（Time）</th>
            <th>提交说明（Message）</th>
            <th>操作</th>
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
              <button class="link-button" @click="viewDiff(item)">查看 Diff（代码变更）</button>
            </td>
          </tr>
          </tbody>
        </table>
      </div>
      <div v-else class="placeholder">
        暂无提交记录，可先配置 GitHub Webhook 并推送一次代码。
      </div>
    </section>

    <section class="section" v-if="authors.length">
      <h2 class="section-title">开发者统计（当前项目 / Developers by Project）</h2>
      <div class="table-wrapper">
        <table class="table">
          <thead>
          <tr>
            <th>开发者（Author）</th>
            <th>邮箱（Email）</th>
            <th>提交次数（Commits）</th>
            <th>最近提交时间（Last Commit）</th>
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
          Diff 详情（代码变更）：{{ selectedCommit.repoFullName }} @ {{ shortSha(selectedCommit.commitSha) }}
        </h2>
        <button class="link-button" @click="selectedCommit = null">收起</button>
      </div>
      <div class="diff-box" v-if="diffLoading">
        加载 diff 中...
      </div>
      <pre v-else class="diff-box"><code>{{ diffText || '（当前 diff 为空，后端尚未生成实际 diff 文本）' }}</code></pre>
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
      diffLoading: false,
      repos: [],
      selectedRepo: '',
      authors: []
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
    async viewDiff (item) {
      this.selectedCommit = item
      this.diffText = ''
      this.diffLoading = true
      try {
        const resp = await this.$http.get(`/admin/commits/${item.commitSha}/diff`, {
          params: {
            repoFullName: item.repoFullName,
            mode: 'raw'
          }
        })
        if (resp.data && resp.data.code === 0) {
          this.diffText = (resp.data.data && resp.data.data.diffText) || ''
        } else {
          this.diffText = '加载 diff 失败'
        }
      } catch (e) {
        this.diffText = '加载 diff 失败'
      } finally {
        this.diffLoading = false
      }
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
</style>

