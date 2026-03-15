<template>
  <div class="ai-config-page">
    <div class="page-header">
      <router-link to="/" class="back-link">← {{ $t('aiConfig.backToHome') }}</router-link>
      <h1 class="page-title">{{ $t('aiConfig.title') }}</h1>
    </div>
    <p class="page-desc">{{ $t('aiConfig.desc') }}</p>

    <div class="config-card" v-if="config">
      <p v-if="configLoadedHint" class="config-loaded-hint">{{ $t('aiConfig.configLoadedHint') }}</p>
      <form @submit.prevent="saveConfig">
        <div class="form-row">
          <label class="form-label">{{ $t('aiConfig.provider') }}</label>
          <span class="form-value">DeepSeek</span>
        </div>
        <div class="form-row">
          <label class="form-label">{{ $t('aiConfig.apiKey') }}</label>
          <p v-if="config.apiKeyMasked" class="api-key-set">{{ $t('aiConfig.apiKeySetHint') }}（{{ config.apiKeyMasked }}）</p>
          <input
            v-model="form.apiKey"
            type="password"
            autocomplete="off"
            :placeholder="config.apiKeyMasked ? $t('aiConfig.apiKeyPlaceholderKeep') : $t('aiConfig.apiKeyPlaceholder')"
            class="form-input"
          >
          <p class="form-hint">{{ $t('aiConfig.apiKeyHint') }}</p>
        </div>
        <div class="form-row">
          <label class="form-label">{{ $t('aiConfig.enabled') }}</label>
          <label class="checkbox-label">
            <input type="checkbox" v-model="form.enabled">
            <span>{{ $t('aiConfig.enabledLabel') }}</span>
          </label>
        </div>
        <div class="form-row">
          <label class="form-label">{{ $t('aiConfig.model') }}</label>
          <input v-model="form.model" type="text" class="form-input" placeholder="deepseek-chat">
        </div>
        <div class="form-actions">
          <button type="submit" class="primary-button" :disabled="saving">{{ saving ? $t('aiConfig.saving') : $t('aiConfig.save') }}</button>
          <span v-if="saveMessage" class="save-message" :class="saveSuccess ? 'success' : 'error'">{{ saveMessage }}</span>
        </div>
      </form>
    </div>
    <div v-else class="placeholder">{{ $t('aiConfig.loading') }}</div>

    <div class="config-card token-usage-card">
      <h2 class="config-card-title">{{ $t('aiConfig.tokenUsageTitle') }}</h2>
      <p class="config-card-desc">{{ $t('aiConfig.tokenUsageDesc') }}</p>
      <div v-if="usageLoading" class="placeholder small">{{ $t('collab.loading') }}</div>
      <template v-else-if="usage">
        <div class="usage-summary">
          <div class="usage-summary-item">
            <span class="usage-summary-value">{{ formatNum(usage.summary.callCount) }}</span>
            <span class="usage-summary-label">{{ $t('aiConfig.usageCallCount') }}</span>
          </div>
          <div class="usage-summary-item">
            <span class="usage-summary-value">{{ formatNum(usage.summary.inputTokens) }}</span>
            <span class="usage-summary-label">{{ $t('aiConfig.usageInputTokens') }}</span>
          </div>
          <div class="usage-summary-item">
            <span class="usage-summary-value">{{ formatNum(usage.summary.outputTokens) }}</span>
            <span class="usage-summary-label">{{ $t('aiConfig.usageOutputTokens') }}</span>
          </div>
          <div class="usage-summary-item">
            <span class="usage-summary-value">{{ formatNum(usage.summary.totalTokens) }}</span>
            <span class="usage-summary-label">{{ $t('aiConfig.usageTotalTokens') }}</span>
          </div>
        </div>
        <p class="usage-since">{{ $t('aiConfig.usageSince', { days: usageDays }) }}</p>
        <div class="usage-actions">
          <select v-model.number="usageDays" @change="loadUsage" class="usage-days-select">
            <option :value="7">7 {{ $t('aiConfig.usageDays') }}</option>
            <option :value="30">30 {{ $t('aiConfig.usageDays') }}</option>
            <option :value="90">90 {{ $t('aiConfig.usageDays') }}</option>
          </select>
          <button type="button" class="link-button" @click="loadUsage">{{ $t('dashboard.refresh') }}</button>
        </div>
        <div v-if="usage.items && usage.items.length" class="usage-table-wrap">
          <table class="usage-table">
            <thead>
              <tr>
                <th>{{ $t('aiConfig.usageTime') }}</th>
                <th>Repo</th>
                <th>Commit</th>
                <th>Model</th>
                <th class="num">{{ $t('aiConfig.usageInputTokens') }}</th>
                <th class="num">{{ $t('aiConfig.usageOutputTokens') }}</th>
                <th class="num">{{ $t('aiConfig.usageTotalTokens') }}</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="(row, i) in usage.items" :key="i">
                <td>{{ formatUsageTime(row.usedAt) }}</td>
                <td class="repo-cell">{{ row.repoFullName || '—' }}</td>
                <td class="mono">{{ shortSha(row.commitSha) }}</td>
                <td>{{ row.model || '—' }}</td>
                <td class="num">{{ formatNum(row.inputTokens) }}</td>
                <td class="num">{{ formatNum(row.outputTokens) }}</td>
                <td class="num">{{ formatNum(row.totalTokens) }}</td>
              </tr>
            </tbody>
          </table>
        </div>
        <p v-else class="usage-empty">{{ $t('aiConfig.usageEmpty') }}</p>
      </template>
    </div>

    <div class="config-card github-config-card" v-if="githubConfig !== undefined">
      <h2 class="config-card-title">{{ $t('githubConfig.title') }}</h2>
      <p class="config-card-desc">{{ $t('githubConfig.desc') }}</p>
      <form @submit.prevent="saveGitHubConfig">
        <div class="form-row">
          <label class="form-label">{{ $t('githubConfig.tokenLabel') }}</label>
          <p v-if="githubConfig.githubTokenMasked" class="api-key-set">{{ $t('githubConfig.tokenSetHint') }}（{{ githubConfig.githubTokenMasked }}）</p>
          <input
            v-model="githubForm.githubToken"
            type="password"
            autocomplete="off"
            :placeholder="githubConfig.hasGitHubToken ? $t('githubConfig.tokenPlaceholderKeep') : $t('githubConfig.tokenPlaceholder')"
            class="form-input"
          >
          <p class="form-hint">{{ $t('githubConfig.tokenHint') }}</p>
        </div>
        <div class="form-row">
          <label class="form-label">{{ $t('githubConfig.proxyLabel') }}</label>
          <input
            v-model="githubForm.githubApiProxy"
            type="text"
            autocomplete="off"
            :placeholder="$t('githubConfig.proxyPlaceholder')"
            class="form-input"
          >
        </div>
        <div class="form-actions">
          <button type="submit" class="primary-button" :disabled="githubSaving">{{ githubSaving ? $t('githubConfig.saving') : $t('githubConfig.save') }}</button>
          <span v-if="githubSaveMessage" class="save-message" :class="githubSaveSuccess ? 'success' : 'error'">{{ githubSaveMessage }}</span>
        </div>
      </form>
    </div>
  </div>
</template>

<script>
export default {
  name: 'AiConfigView',
  data () {
    return {
      config: null,
      form: {
        apiKey: '',
        enabled: false,
        model: 'deepseek-chat'
      },
      saving: false,
      saveMessage: '',
      saveSuccess: false,
      githubConfig: undefined,
      githubForm: {
        githubToken: '',
        githubApiProxy: ''
      },
      githubSaving: false,
      githubSaveMessage: '',
      githubSaveSuccess: false,
      usage: null,
      usageLoading: false,
      usageDays: 30
    }
  },
  created () {
    this.loadConfig()
    this.loadGitHubConfig()
    this.loadUsage()
  },
  computed: {
    configLoadedHint () {
      if (!this.config) return false
      const hasKey = this.config.hasApiKey === true || !!(this.config.apiKeyMasked && String(this.config.apiKeyMasked).trim())
      const isEnabled = this.config.enabled === true || this.config.enabled === 'true' || this.config.enabled === 1
      return hasKey || isEnabled
    }
  },
  methods: {
    async loadConfig () {
      try {
        const resp = await this.$http.get('/admin/ai-analysis/config')
        if (resp.data && resp.data.code === 0 && resp.data.data) {
          const data = resp.data.data
          this.config = data
          this.form.enabled = data.enabled === true || data.enabled === 'true' || data.enabled === 1
          this.form.model = (data.model && String(data.model).trim()) || 'deepseek-chat'
          this.form.apiKey = ''
        }
      } catch (e) {
        if (e.response && e.response.status === 401) {
          this.$router.push({ name: 'login' })
        }
      }
    },
    async saveConfig () {
      this.saving = true
      this.saveMessage = ''
      try {
        const payload = {
          enabled: this.form.enabled,
          model: this.form.model || 'deepseek-chat'
        }
        if (this.form.apiKey && !this.form.apiKey.includes('****')) {
          payload.apiKey = this.form.apiKey
        }
        const resp = await this.$http.put('/admin/ai-analysis/config', payload)
        if (resp.data && resp.data.code === 0) {
          this.saveMessage = this.$t('aiConfig.saveSuccess')
          this.saveSuccess = true
          this.loadConfig()
          this.form.apiKey = ''
        } else {
          this.saveMessage = (resp.data && resp.data.message) || this.$t('aiConfig.saveFailed')
          this.saveSuccess = false
        }
      } catch (e) {
        this.saveMessage = (e.response && e.response.data && e.response.data.message) || this.$t('aiConfig.saveFailed')
        this.saveSuccess = false
      } finally {
        this.saving = false
      }
    },
    async loadGitHubConfig () {
      try {
        const resp = await this.$http.get('/admin/config/github')
        if (resp.data && resp.data.code === 0 && resp.data.data) {
          const d = resp.data.data
          this.githubConfig = d
          this.githubForm.githubToken = ''
          this.githubForm.githubApiProxy = d.githubApiProxy || ''
        } else {
          this.githubConfig = {}
        }
      } catch (e) {
        if (e.response && e.response.status === 401) this.$router.push({ name: 'login' })
        else this.githubConfig = {}
      }
    },
    async loadUsage () {
      this.usageLoading = true
      try {
        const resp = await this.$http.get('/admin/ai-analysis/usage', { params: { days: this.usageDays } })
        if (resp.data && resp.data.code === 0 && resp.data.data) {
          this.usage = resp.data.data
          if (!this.usage.summary) this.usage.summary = {}
        } else {
          this.usage = { summary: {}, items: [] }
        }
      } catch (e) {
        this.usage = { summary: {}, items: [] }
      } finally {
        this.usageLoading = false
      }
    },
    formatNum (v) {
      if (v == null || v === '') return '0'
      const n = Number(v)
      if (isNaN(n)) return String(v)
      return n.toLocaleString()
    },
    formatUsageTime (t) {
      if (!t) return '—'
      try {
        return new Date(t).toLocaleString()
      } catch (e) {
        return String(t)
      }
    },
    shortSha (sha) {
      if (!sha || sha.length < 8) return sha || '—'
      return sha.substring(0, 7)
    },
    async saveGitHubConfig () {
      this.githubSaving = true
      this.githubSaveMessage = ''
      try {
        const payload = {
          githubApiProxy: this.githubForm.githubApiProxy || ''
        }
        if (this.githubForm.githubToken && !this.githubForm.githubToken.includes('****')) {
          payload.githubToken = this.githubForm.githubToken
        }
        const resp = await this.$http.put('/admin/config/github', payload)
        if (resp.data && resp.data.code === 0) {
          this.githubSaveMessage = this.$t('githubConfig.saveSuccess')
          this.githubSaveSuccess = true
          this.loadGitHubConfig()
          this.githubForm.githubToken = ''
        } else {
          this.githubSaveMessage = (resp.data && resp.data.message) || this.$t('githubConfig.saveFailed')
          this.githubSaveSuccess = false
        }
      } catch (e) {
        this.githubSaveMessage = (e.response && e.response.data && e.response.data.message) || this.$t('githubConfig.saveFailed')
        this.githubSaveSuccess = false
      } finally {
        this.githubSaving = false
      }
    }
  }
}
</script>

<style scoped>
.ai-config-page {
  max-width: 560px;
  margin: 0 auto;
}
.page-header {
  margin-bottom: 8px;
}
.back-link {
  font-size: 13px;
  color: #2563eb;
  text-decoration: none;
}
.page-title {
  font-size: 22px;
  margin: 12px 0 0;
}
.page-desc {
  color: #6b7280;
  font-size: 14px;
  margin-bottom: 24px;
}
.config-loaded-hint {
  font-size: 13px;
  color: #059669;
  margin: 0 0 16px 0;
  padding: 8px 12px;
  background: #ecfdf5;
  border-radius: 6px;
}
.api-key-set {
  font-size: 13px;
  color: #059669;
  margin: 0 0 8px 0;
}
.config-card {
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  padding: 24px;
}
.token-usage-card {
  margin-top: 24px;
}
.usage-summary {
  display: flex;
  flex-wrap: wrap;
  gap: 16px 24px;
  margin-bottom: 12px;
}
.usage-summary-item {
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.usage-summary-value {
  font-size: 18px;
  font-weight: 600;
  color: #1f2937;
}
.usage-summary-label {
  font-size: 12px;
  color: #6b7280;
}
.usage-since {
  font-size: 13px;
  color: #6b7280;
  margin: 0 0 12px;
}
.usage-actions {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}
.usage-days-select {
  padding: 6px 10px;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  font-size: 13px;
}
.usage-table-wrap {
  overflow-x: auto;
}
.usage-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
}
.usage-table th,
.usage-table td {
  padding: 8px 10px;
  text-align: left;
  border-bottom: 1px solid #e5e7eb;
}
.usage-table th.num,
.usage-table td.num {
  text-align: right;
}
.usage-table .mono {
  font-family: ui-monospace, monospace;
}
.repo-cell {
  max-width: 180px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.usage-empty {
  font-size: 13px;
  color: #6b7280;
  margin: 0;
}
.placeholder.small {
  padding: 12px;
}
.github-config-card {
  margin-top: 24px;
}
.config-card-title {
  font-size: 16px;
  margin: 0 0 8px;
  color: #1f2937;
}
.config-card-desc {
  font-size: 13px;
  color: #6b7280;
  margin: 0 0 20px;
}
.form-row {
  margin-bottom: 20px;
}
.form-label {
  display: block;
  font-size: 13px;
  font-weight: 500;
  color: #374151;
  margin-bottom: 6px;
}
.form-value {
  font-size: 14px;
  color: #6b7280;
}
.form-input {
  width: 100%;
  padding: 8px 12px;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  font-size: 14px;
  box-sizing: border-box;
}
.form-hint {
  font-size: 12px;
  color: #9ca3af;
  margin-top: 4px;
}
.checkbox-label {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  cursor: pointer;
}
.checkbox-label input {
  margin: 0;
}
.form-actions {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-top: 24px;
  padding-top: 16px;
  border-top: 1px solid #e5e7eb;
}
.primary-button {
  padding: 8px 20px;
  border-radius: 6px;
  border: none;
  background: #2563eb;
  color: #fff;
  font-size: 14px;
  cursor: pointer;
}
.primary-button:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}
.save-message {
  font-size: 13px;
}
.save-message.success { color: #059669; }
.save-message.error { color: #dc2626; }
.placeholder {
  padding: 24px;
  text-align: center;
  color: #6b7280;
}
</style>
