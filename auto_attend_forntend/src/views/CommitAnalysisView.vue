<template>
  <div class="commit-analysis-page">
    <div class="page-header">
      <router-link to="/" class="back-link">← {{ $t('dashboard.commitAnalysisBoardBack') }}</router-link>
      <h1 class="page-title">{{ $t('dashboard.commitAnalysisBoardTitle') }}：{{ shortSha(commitSha) }}</h1>
    </div>

    <div v-if="loading" class="placeholder">{{ $t('collab.loading') }}</div>
    <div v-else-if="error" class="error-box">{{ error }}</div>
    <template v-else-if="detail">
      <!-- 提交基础信息 -->
      <section class="section commit-meta-section">
        <h2 class="section-title">{{ $t('dashboard.commitAnalysisMetaTitle') }}</h2>
        <dl class="meta-dl">
          <dt>{{ $t('dashboard.repo') }}</dt>
          <dd>
            <a v-if="githubCommitUrl" :href="githubCommitUrl" target="_blank" rel="noopener noreferrer" class="repo-link">{{ detail.commit.repoFullName }}</a>
            <span v-else>{{ detail.commit.repoFullName }}</span>
          </dd>
          <dt>{{ $t('dashboard.commit') }}</dt>
          <dd class="mono">{{ detail.commit.commitSha }}</dd>
          <dt v-if="detail.commit.parentSha">Parent</dt>
          <dd v-if="detail.commit.parentSha" class="mono">{{ detail.commit.parentSha }}</dd>
          <dt>{{ $t('dashboard.author') }}</dt>
          <dd>{{ detail.commit.authorName || '—' }} <span v-if="detail.commit.authorEmail" class="email">&lt;{{ detail.commit.authorEmail }}&gt;</span></dd>
          <dt>{{ $t('dashboard.time') }}</dt>
          <dd>{{ formatTime(detail.commit.committedAt) }}</dd>
          <dt>{{ $t('dashboard.message') }}</dt>
          <dd class="message-cell"><pre class="message-pre">{{ detail.commit.message || '—' }}</pre></dd>
          <dt>{{ $t('dashboard.filesChanged') }}</dt>
          <dd>{{ detail.commit.filesChanged != null ? detail.commit.filesChanged : '—' }}</dd>
          <dt>{{ $t('dashboard.insertions') }}</dt>
          <dd class="num add">{{ detail.commit.insertions != null ? detail.commit.insertions : '—' }}</dd>
          <dt>{{ $t('dashboard.deletions') }}</dt>
          <dd class="num del">{{ detail.commit.deletions != null ? detail.commit.deletions : '—' }}</dd>
          <dt>Diff 可用</dt>
          <dd>{{ detail.commit.hasDiff ? '是' : '否' }}</dd>
          <dt v-if="detail.commit.validReason !== undefined">提交有效性</dt>
          <dd v-if="detail.commit.validReason !== undefined">{{ detail.commit.validCommit ? '有效' : '无效' }} <span v-if="detail.commit.validReason" class="valid-reason">（{{ detail.commit.validReason }}）</span></dd>
        </dl>
        <div class="commit-actions">
          <router-link :to="{ path: '/', query: {} }" class="link-button">{{ $t('dashboard.collapse') }}</router-link>
          <a v-if="githubCommitUrl" :href="githubCommitUrl" target="_blank" rel="noopener noreferrer" class="link-button">{{ $t('dashboard.viewOnGitHub') }}</a>
          <router-link v-if="detail.commit.hasDiff" :to="{ path: '/', query: { commitSha: detail.commit.commitSha, repoFullName: detail.commit.repoFullName } }" class="link-button">{{ $t('dashboard.viewDiff') }}</router-link>
        </div>
      </section>

      <!-- AI 分析结果 -->
      <section class="section ai-section">
        <h2 class="section-title">{{ $t('dashboard.aiAnalysisTitle') }}</h2>
        <div v-if="detail.aiResult" class="ai-result">
          <div class="ai-row">
            <span class="ai-label">{{ $t('dashboard.aiWorkSummary') }}</span>
            <span>{{ detail.aiResult.workSummary || '—' }}</span>
          </div>
          <div class="ai-row">
            <span class="ai-label">{{ $t('dashboard.aiWorkType') }}</span>
            <span>{{ detail.aiResult.workType || '—' }}</span>
          </div>
          <div class="ai-row" v-if="detail.aiResult.mainArea">
            <span class="ai-label">主要涉及</span>
            <span>{{ detail.aiResult.mainArea }}</span>
          </div>
          <div class="ai-row">
            <span class="ai-label">{{ $t('dashboard.aiIsEffective') }}</span>
            <span>{{ detail.aiResult.isEffective || '—' }}</span>
          </div>
          <div class="ai-row" v-if="detail.aiResult.effectiveReason">
            <span class="ai-label">{{ $t('dashboard.aiEffectiveReason') }}</span>
            <span>{{ detail.aiResult.effectiveReason }}</span>
          </div>
          <div class="ai-row" v-if="detail.aiResult.invalidReasonTag">
            <span class="ai-label">无效原因标签</span>
            <span>{{ detail.aiResult.invalidReasonTag }}</span>
          </div>
          <div class="ai-row">
            <span class="ai-label">{{ $t('dashboard.aiQualityLevel') }}</span>
            <span>{{ detail.aiResult.qualityLevel || '—' }}</span>
          </div>
          <div class="ai-row" v-if="detail.aiResult.qualityComment">
            <span class="ai-label">{{ $t('dashboard.aiQualityComment') }}</span>
            <span>{{ detail.aiResult.qualityComment }}</span>
          </div>
          <div class="ai-row" v-if="parsedRiskFlags.length">
            <span class="ai-label">风险标签</span>
            <span><span v-for="tag in parsedRiskFlags" :key="tag" class="tag">{{ tag }}</span></span>
          </div>
          <div class="ai-row" v-if="parsedSuggestions.length">
            <span class="ai-label">改进建议</span>
            <ul class="suggestions-list">
              <li v-for="(s, i) in parsedSuggestions" :key="i">{{ s }}</li>
            </ul>
          </div>
          <div class="ai-row meta">
            <span class="ai-label">Prompt 版本</span>
            <span>{{ detail.aiResult.promptVersion || '—' }}</span>
          </div>
          <div class="ai-row meta">
            <span class="ai-label">分析时间</span>
            <span>{{ formatTime(detail.aiResult.createdAt) }}</span>
          </div>
          <details v-if="detail.aiResult.rawResponse" class="raw-response-details">
            <summary>原始 AI 响应</summary>
            <pre class="raw-response-pre">{{ detail.aiResult.rawResponse }}</pre>
          </details>
        </div>
        <div v-else class="ai-empty">
          <p>{{ $t('dashboard.commitAnalysisNoResult') }}</p>
          <p class="hint">{{ $t('dashboard.aiAnalysisHint') }}</p>
          <router-link to="/" class="link-button">{{ $t('dashboard.commitAnalysisBoardBack') }}</router-link>
        </div>
      </section>
    </template>
  </div>
</template>

<script>
export default {
  name: 'CommitAnalysisView',
  data () {
    return {
      detail: null,
      loading: true,
      error: ''
    }
  },
  computed: {
    commitSha () {
      return this.$route.params.commitSha || ''
    },
    repoFullName () {
      return this.$route.query.repoFullName || ''
    },
    githubCommitUrl () {
      if (!this.detail || !this.detail.commit) return ''
      const repo = this.detail.commit.repoFullName || ''
      const sha = this.detail.commit.commitSha || ''
      if (!repo || !sha) return ''
      return `https://github.com/${repo}/commit/${sha}`
    },
    parsedRiskFlags () {
      if (!this.detail || !this.detail.aiResult || !this.detail.aiResult.riskFlags) return []
      try {
        const arr = JSON.parse(this.detail.aiResult.riskFlags)
        return Array.isArray(arr) ? arr : []
      } catch (e) {
        return []
      }
    },
    parsedSuggestions () {
      if (!this.detail || !this.detail.aiResult || !this.detail.aiResult.suggestions) return []
      try {
        const arr = JSON.parse(this.detail.aiResult.suggestions)
        return Array.isArray(arr) ? arr : []
      } catch (e) {
        return []
      }
    }
  },
  watch: {
    '$route' () {
      this.loadDetail()
    }
  },
  created () {
    this.loadDetail()
  },
  methods: {
    shortSha (sha) {
      if (!sha || sha.length < 8) return sha
      return sha.substring(0, 7)
    },
    formatTime (t) {
      if (!t) return '—'
      try {
        return new Date(t).toLocaleString()
      } catch (e) {
        return t
      }
    },
    async loadDetail () {
      this.loading = true
      this.error = ''
      this.detail = null
      try {
        const params = {}
        if (this.repoFullName) params.repoFullName = this.repoFullName
        const resp = await this.$http.get(`/admin/ai-analysis/commits/${this.commitSha}/detail`, { params })
        if (resp.data && resp.data.code === 0 && resp.data.data) {
          this.detail = resp.data.data
        } else {
          this.error = (resp.data && resp.data.message) || '加载失败'
        }
      } catch (e) {
        this.error = (e.response && e.response.data && e.response.data.message) || e.message || '请求失败'
      } finally {
        this.loading = false
      }
    }
  }
}
</script>

<style scoped>
.commit-analysis-page {
  max-width: 900px;
  margin: 0 auto;
  padding: 16px;
}
.page-header {
  margin-bottom: 16px;
}
.back-link {
  font-size: 13px;
  color: #2563eb;
  text-decoration: none;
}
.page-title {
  font-size: 20px;
  margin: 8px 0 0;
  word-break: break-all;
}
.placeholder, .error-box {
  padding: 24px;
  text-align: center;
  border-radius: 8px;
}
.error-box {
  background: #fef2f2;
  color: #dc2626;
}
.section {
  background: #fff;
  border-radius: 8px;
  padding: 20px;
  margin-bottom: 20px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.08);
}
.section-title {
  font-size: 16px;
  margin: 0 0 16px;
  color: #1f2937;
}
.meta-dl {
  display: grid;
  grid-template-columns: 140px 1fr;
  gap: 8px 16px;
  margin: 0;
  font-size: 14px;
}
.meta-dl dt {
  color: #6b7280;
  font-weight: 500;
}
.meta-dl dd {
  margin: 0;
  word-break: break-all;
}
.mono {
  font-family: ui-monospace, monospace;
  font-size: 13px;
}
.email {
  color: #6b7280;
}
.message-cell {
  white-space: pre-wrap;
}
.message-pre {
  margin: 0;
  font-family: inherit;
  font-size: 13px;
  white-space: pre-wrap;
  word-break: break-word;
}
.num.add { color: #059669; }
.num.del { color: #dc2626; }
.valid-reason { color: #6b7280; font-size: 13px; }
.repo-link {
  color: #2563eb;
  text-decoration: none;
}
.commit-actions {
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid #e5e7eb;
  display: flex;
  gap: 12px;
}
.link-button {
  font-size: 13px;
  color: #2563eb;
  background: none;
  border: none;
  cursor: pointer;
  text-decoration: none;
  padding: 4px 0;
}
.ai-result {
  font-size: 14px;
}
.ai-row {
  margin-bottom: 12px;
  display: flex;
  gap: 12px;
  align-items: flex-start;
}
.ai-row.meta { color: #6b7280; font-size: 13px; }
.ai-label {
  flex-shrink: 0;
  width: 120px;
  font-weight: 500;
  color: #374151;
}
.tag {
  display: inline-block;
  margin-right: 6px;
  padding: 2px 8px;
  background: #f3f4f6;
  border-radius: 4px;
  font-size: 12px;
}
.suggestions-list {
  margin: 0;
  padding-left: 20px;
}
.raw-response-details {
  margin-top: 16px;
  font-size: 13px;
}
.raw-response-pre {
  margin: 8px 0 0;
  padding: 12px;
  background: #f9fafb;
  border-radius: 6px;
  overflow: auto;
  max-height: 300px;
  white-space: pre-wrap;
  word-break: break-word;
}
.ai-empty {
  padding: 24px;
  text-align: center;
  color: #6b7280;
}
.ai-empty .hint {
  font-size: 13px;
  margin-top: 8px;
}
.ai-empty .link-button {
  margin-top: 12px;
  display: inline-block;
}
</style>
