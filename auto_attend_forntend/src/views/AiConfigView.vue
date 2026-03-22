<template>
  <div class="ai-config-page">
    <div class="page-header">
      <router-link to="/" class="back-link">← {{ $t('aiConfig.backToHome') }}</router-link>
      <h1 class="page-title">{{ $t('aiConfig.title') }}</h1>
    </div>
    <p class="page-desc">{{ $t('aiConfig.desc') }}</p>

    <div class="provider-grid">
      <div class="provider-panel">
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
              <label class="form-label">{{ $t('aiConfig.dailySummaryEnabled') }}</label>
              <label class="checkbox-label">
                <input type="checkbox" v-model="form.dailySummaryEnabled">
                <span>{{ $t('aiConfig.dailySummaryEnabledLabel') }}</span>
              </label>
              <p class="form-hint">{{ $t('aiConfig.dailySummaryHint') }}</p>
              <p class="form-hint subtle">{{ $t('aiConfig.toggleAutoSaveHint') }}</p>
            </div>
            <div class="form-row daily-run-row">
              <button type="button" class="link-button" :disabled="dailyRunLoading" @click="runDailySummaryNow">
                {{ dailyRunLoading ? '…' : $t('aiConfig.runDailySummaryNow') }}
              </button>
              <span v-if="dailyRunMessage" class="save-message inline-msg" :class="dailyRunOk ? 'success' : 'error'">{{ dailyRunMessage }}</span>
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
          <h2 class="config-card-title">DeepSeek {{ $t('aiConfig.tokenUsageTitle') }}</h2>
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
              <select v-model.number="usageDays" @change="onUsageDaysChange('deepseek')" class="usage-days-select">
                <option :value="7">7 {{ $t('aiConfig.usageDays') }}</option>
                <option :value="30">30 {{ $t('aiConfig.usageDays') }}</option>
                <option :value="90">90 {{ $t('aiConfig.usageDays') }}</option>
              </select>
              <button type="button" class="link-button" @click="refreshUsageDeepseek">{{ $t('dashboard.refresh') }}</button>
            </div>
            <div class="usage-charts">
              <div class="usage-chart-block">
                <div class="usage-chart-title">API 请求次数</div>
                <div class="usage-chart-wrap">
                  <canvas ref="deepseekCall"></canvas>
                </div>
              </div>
              <div class="usage-chart-block">
                <div class="usage-chart-title">{{ $t('aiConfig.usageTotalTokens') }}</div>
                <div class="usage-chart-wrap">
                  <canvas ref="deepseekToken"></canvas>
                </div>
              </div>
            </div>
            <div class="usage-detail-actions">
              <button type="button" class="primary-button primary-button-outline" @click="openUsageDetail('deepseek', usageDays)">查看详情</button>
            </div>
          </template>
        </div>
      </div>

      <div class="provider-panel">
        <div class="config-card qwen-config-card" v-if="qwenConfig">
          <h2 class="config-card-title">Qwen 多模态（协作 AI 录入）</h2>
          <p class="config-card-desc">用于「项目协作 → 任务表」的 AI 录入模式，多模态理解文字+附件。</p>
          <form @submit.prevent="saveQwenConfig">
            <div class="form-row">
              <label class="form-label">提供商</label>
              <span class="form-value">Qwen</span>
            </div>
            <div class="form-row">
              <label class="form-label">Qwen API Key</label>
              <p v-if="qwenConfig.apiKeyMasked" class="api-key-set">已设置（{{ qwenConfig.apiKeyMasked }}）</p>
              <input
                v-model="qwenForm.apiKey"
                type="password"
                autocomplete="off"
                placeholder="不填则保持原值"
                class="form-input"
              >
            </div>
            <div class="form-row">
              <label class="form-label">启用千问多模态</label>
              <label class="checkbox-label">
                <input type="checkbox" v-model="qwenForm.enabled">
                <span>用于协作任务表 AI 录入模式</span>
              </label>
            </div>
            <div class="form-row">
              <label class="form-label">模型</label>
              <input v-model="qwenForm.model" type="text" class="form-input" placeholder="qwen-omni">
            </div>
            <div class="form-actions">
              <button type="submit" class="primary-button" :disabled="qwenSaving">{{ qwenSaving ? $t('aiConfig.saving') : $t('aiConfig.save') }}</button>
              <span v-if="qwenSaveMessage" class="save-message" :class="qwenSaveSuccess ? 'success' : 'error'">{{ qwenSaveMessage }}</span>
            </div>
          </form>
        </div>

        <div class="config-card token-usage-card qwen-usage-card">
          <h2 class="config-card-title">千问多模态 {{ $t('aiConfig.tokenUsageTitle') }}</h2>
          <p class="config-card-desc">协作任务表 AI 录入产生的 Token 消耗。</p>
          <div v-if="usageQwenLoading" class="placeholder small">{{ $t('collab.loading') }}</div>
          <template v-else-if="usageQwen">
            <div class="usage-summary">
              <div class="usage-summary-item">
                <span class="usage-summary-value">{{ formatNum(usageQwen.summary.callCount) }}</span>
                <span class="usage-summary-label">{{ $t('aiConfig.usageCallCount') }}</span>
              </div>
              <div class="usage-summary-item">
                <span class="usage-summary-value">{{ formatNum(usageQwen.summary.inputTokens) }}</span>
                <span class="usage-summary-label">{{ $t('aiConfig.usageInputTokens') }}</span>
              </div>
              <div class="usage-summary-item">
                <span class="usage-summary-value">{{ formatNum(usageQwen.summary.outputTokens) }}</span>
                <span class="usage-summary-label">{{ $t('aiConfig.usageOutputTokens') }}</span>
              </div>
              <div class="usage-summary-item">
                <span class="usage-summary-value">{{ formatNum(usageQwen.summary.totalTokens) }}</span>
                <span class="usage-summary-label">{{ $t('aiConfig.usageTotalTokens') }}</span>
              </div>
            </div>
            <p class="usage-since">{{ $t('aiConfig.usageSince', { days: usageDaysQwen }) }}</p>
            <div class="usage-actions">
              <select v-model.number="usageDaysQwen" @change="onUsageDaysChange('qwen')" class="usage-days-select">
                <option :value="7">7 {{ $t('aiConfig.usageDays') }}</option>
                <option :value="30">30 {{ $t('aiConfig.usageDays') }}</option>
                <option :value="90">90 {{ $t('aiConfig.usageDays') }}</option>
              </select>
              <button type="button" class="link-button" @click="refreshUsageQwen">{{ $t('dashboard.refresh') }}</button>
            </div>
            <div class="usage-charts">
              <div class="usage-chart-block">
                <div class="usage-chart-title">API 请求次数</div>
                <div class="usage-chart-wrap">
                  <canvas ref="qwenCall"></canvas>
                </div>
              </div>
              <div class="usage-chart-block">
                <div class="usage-chart-title">{{ $t('aiConfig.usageTotalTokens') }}</div>
                <div class="usage-chart-wrap">
                  <canvas ref="qwenToken"></canvas>
                </div>
              </div>
            </div>
            <div class="usage-detail-actions">
              <button type="button" class="primary-button primary-button-outline" @click="openUsageDetail('qwen', usageDaysQwen)">查看详情</button>
            </div>
          </template>
        </div>
      </div>
    </div>

    <div class="usage-detail-modal" v-if="usageDetailOpen" @click.self="closeUsageDetail">
      <div class="usage-detail-dialog">
        <div class="usage-detail-header">
          <h3>{{ usageDetailTitle }}</h3>
          <button type="button" class="usage-detail-close" @click="closeUsageDetail" aria-label="关闭">×</button>
        </div>
        <div v-if="usageDetailLoading" class="placeholder small">加载中…</div>
        <template v-else>
          <div class="usage-table-wrap">
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
                <tr v-for="(row, i) in usageDetailItems" :key="'detail-' + i">
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
          <p v-if="!usageDetailItems.length" class="usage-empty">{{ $t('aiConfig.usageEmpty') }}</p>
          <div v-else class="usage-pagination">
            <button type="button" class="link-button" :disabled="usageDetailPage <= 1" @click="goUsageDetailPage(usageDetailPage - 1)">上一页</button>
            <span class="usage-pagination-info">第 {{ usageDetailPage }} / {{ usageDetailTotalPages }} 页，共 {{ usageDetailTotal }} 条</span>
            <button type="button" class="link-button" :disabled="usageDetailPage >= usageDetailTotalPages" @click="goUsageDetailPage(usageDetailPage + 1)">下一页</button>
          </div>
        </template>
      </div>
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
import { Chart as ChartJS, registerables } from 'chart.js'

ChartJS.register(...registerables)

const PAGE_SIZE = 20

export default {
  name: 'AiConfigView',
  data () {
    return {
      config: null,
      form: {
        apiKey: '',
        enabled: false,
        dailySummaryEnabled: false,
        model: 'deepseek-chat'
      },
      dailyRunLoading: false,
      dailyRunMessage: '',
      dailyRunOk: false,
      saving: false,
      saveMessage: '',
      saveSuccess: false,
      qwenConfig: null,
      qwenForm: {
        apiKey: '',
        enabled: false,
        model: 'qwen-omni'
      },
      qwenSaving: false,
      qwenSaveMessage: '',
      qwenSaveSuccess: false,
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
      usageDays: 30,
      usageDaily: [],
      usageQwen: null,
      usageQwenLoading: false,
      usageDaysQwen: 30,
      usageQwenDaily: [],
      chartInstances: { deepseekCall: null, deepseekToken: null, qwenCall: null, qwenToken: null },
      usageDetailOpen: false,
      usageDetailProvider: null,
      usageDetailDays: 30,
      usageDetailPage: 1,
      usageDetailPageSize: PAGE_SIZE,
      usageDetailTotal: 0,
      usageDetailItems: [],
      usageDetailLoading: false,
      /** 从服务端回填表单时跳过「开关自动保存」 */
      aiConfigHydrating: false,
      aiConfigAutoSaveTimer: null
    }
  },
  created () {
    this.loadConfig()
    this.loadQwenConfig()
    this.loadGitHubConfig()
    this.loadUsage()
    this.loadUsageQwen()
    this.loadUsageDaily()
    this.loadUsageQwenDaily()
  },
  mounted () {
    this.$nextTick(() => {
      setTimeout(() => this.renderUsageCharts(), 150)
    })
  },
  beforeDestroy () {
    if (this.aiConfigAutoSaveTimer) {
      clearTimeout(this.aiConfigAutoSaveTimer)
      this.aiConfigAutoSaveTimer = null
    }
    const chartKeys = ['deepseekCall', 'deepseekToken', 'qwenCall', 'qwenToken']
    chartKeys.forEach(k => {
      if (this.chartInstances[k]) {
        this.chartInstances[k].destroy()
        this.chartInstances[k] = null
      }
    })
  },
  computed: {
    configLoadedHint () {
      if (!this.config) return false
      const hasKey = this.config.hasApiKey === true || !!(this.config.apiKeyMasked && String(this.config.apiKeyMasked).trim())
      const isEnabled = this.config.enabled === true || this.config.enabled === 'true' || this.config.enabled === 1
      return hasKey || isEnabled
    },
    usageDetailTitle () {
      const name = this.usageDetailProvider === 'qwen' ? '千问多模态' : 'DeepSeek'
      return `${name} 用量详情（近 ${this.usageDetailDays || 30} 天）`
    },
    usageDetailTotalPages () {
      if (this.usageDetailTotal <= 0) return 1
      return Math.ceil(this.usageDetailTotal / this.usageDetailPageSize) || 1
    }
  },
  watch: {
    /** Canvas 在 v-else-if="usage" 内，须等 summary 加载完成才挂载；仅监听 daily 会先渲染但 refs 为空 */
    usage () { this.$nextTick(() => this.renderUsageCharts()) },
    usageQwen () { this.$nextTick(() => this.renderUsageCharts()) },
    usageDaily () { this.$nextTick(() => this.renderUsageCharts()) },
    usageQwenDaily () { this.$nextTick(() => this.renderUsageCharts()) },
    'form.enabled' () { this.scheduleDeepSeekToggleAutoSave() },
    'form.dailySummaryEnabled' () { this.scheduleDeepSeekToggleAutoSave() }
  },
  methods: {
    coerceBool (v) {
      if (v === true || v === 1) return true
      if (v === false || v === 0) return false
      if (typeof v === 'string') {
        const s = v.trim().toLowerCase()
        if (s === 'true' || s === '1') return true
        if (s === 'false' || s === '0') return false
      }
      return false
    },
    buildDeepSeekSavePayload () {
      const payload = {
        enabled: !!this.form.enabled,
        dailySummaryEnabled: !!this.form.dailySummaryEnabled,
        model: (this.form.model && String(this.form.model).trim()) || 'deepseek-chat'
      }
      if (this.form.apiKey && !this.form.apiKey.includes('****')) {
        payload.apiKey = this.form.apiKey
      }
      return payload
    },
    scheduleDeepSeekToggleAutoSave () {
      if (this.aiConfigHydrating || !this.config) return
      if (this.aiConfigAutoSaveTimer) clearTimeout(this.aiConfigAutoSaveTimer)
      this.aiConfigAutoSaveTimer = setTimeout(() => this.persistDeepSeekTogglesSilent(), 450)
    },
    async persistDeepSeekTogglesSilent () {
      this.aiConfigAutoSaveTimer = null
      if (this.aiConfigHydrating || !this.config) return
      try {
        const resp = await this.$http.put('/admin/ai-analysis/config', this.buildDeepSeekSavePayload())
        if (resp.data && resp.data.code === 0) {
          await this.loadConfig()
        }
      } catch (e) { /* 静默：可稍后手动点保存 */ }
    },
    async loadConfig () {
      this.aiConfigHydrating = true
      try {
        const resp = await this.$http.get('/admin/ai-analysis/config')
        if (resp.data && resp.data.code === 0 && resp.data.data) {
          const data = resp.data.data
          this.config = data
          this.form.enabled = this.coerceBool(data.enabled)
          this.form.dailySummaryEnabled = this.coerceBool(data.dailySummaryEnabled)
          this.form.model = (data.model && String(data.model).trim()) || 'deepseek-chat'
          this.form.apiKey = ''
        }
      } catch (e) {
        if (e.response && e.response.status === 401) {
          this.$router.push({ name: 'login' })
        }
      } finally {
        this.$nextTick(() => { this.aiConfigHydrating = false })
      }
    },
    async loadQwenConfig () {
      try {
        const resp = await this.$http.get('/admin/ai-analysis/qwen-config')
        if (resp.data && resp.data.code === 0 && resp.data.data) {
          const data = resp.data.data
          this.qwenConfig = data
          this.qwenForm.enabled = data.enabled === true || data.enabled === 'true' || data.enabled === 1
          this.qwenForm.model = (data.model && String(data.model).trim()) || 'qwen-omni'
          this.qwenForm.apiKey = ''
        }
      } catch (e) {
        // 忽略，保留 qwenConfig 为空
      }
    },
    async saveConfig () {
      this.saving = true
      this.saveMessage = ''
      try {
        if (this.aiConfigAutoSaveTimer) {
          clearTimeout(this.aiConfigAutoSaveTimer)
          this.aiConfigAutoSaveTimer = null
        }
        const resp = await this.$http.put('/admin/ai-analysis/config', this.buildDeepSeekSavePayload())
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
    async runDailySummaryNow () {
      this.dailyRunLoading = true
      this.dailyRunMessage = ''
      try {
        const resp = await this.$http.post('/admin/ai-analysis/daily-summary/run')
        if (resp.data && resp.data.code === 0 && resp.data.data) {
          const n = resp.data.data.reposProcessed != null ? resp.data.data.reposProcessed : 0
          const d = resp.data.data.summaryDate || ''
          this.dailyRunOk = true
          this.dailyRunMessage = this.$t('aiConfig.runDailySummaryOk', { date: d, n })
        } else {
          this.dailyRunOk = false
          this.dailyRunMessage = (resp.data && resp.data.message) || this.$t('aiConfig.runDailySummaryFail')
        }
      } catch (e) {
        this.dailyRunOk = false
        this.dailyRunMessage = (e.response && e.response.data && e.response.data.message) || this.$t('aiConfig.runDailySummaryFail')
      } finally {
        this.dailyRunLoading = false
      }
    },
    async saveQwenConfig () {
      this.qwenSaving = true
      this.qwenSaveMessage = ''
      try {
        const payload = {
          enabled: this.qwenForm.enabled,
          model: this.qwenForm.model || 'qwen-omni'
        }
        if (this.qwenForm.apiKey && !this.qwenForm.apiKey.includes('****')) {
          payload.apiKey = this.qwenForm.apiKey
        }
        const resp = await this.$http.put('/admin/ai-analysis/qwen-config', payload)
        if (resp.data && resp.data.code === 0) {
          this.qwenSaveMessage = this.$t('aiConfig.saveSuccess')
          this.qwenSaveSuccess = true
          this.loadQwenConfig()
          this.qwenForm.apiKey = ''
        } else {
          this.qwenSaveMessage = (resp.data && resp.data.message) || this.$t('aiConfig.saveFailed')
          this.qwenSaveSuccess = false
        }
      } catch (e) {
        this.qwenSaveMessage = (e.response && e.response.data && e.response.data.message) || this.$t('aiConfig.saveFailed')
        this.qwenSaveSuccess = false
      } finally {
        this.qwenSaving = false
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
        const resp = await this.$http.get('/admin/ai-analysis/usage', { params: { days: this.usageDays, provider: 'deepseek' } })
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
    async loadUsageQwen () {
      this.usageQwenLoading = true
      try {
        const resp = await this.$http.get('/admin/ai-analysis/usage', { params: { days: this.usageDaysQwen, provider: 'qwen' } })
        if (resp.data && resp.data.code === 0 && resp.data.data) {
          this.usageQwen = resp.data.data
          if (!this.usageQwen.summary) this.usageQwen.summary = {}
        } else {
          this.usageQwen = { summary: {}, items: [] }
        }
      } catch (e) {
        this.usageQwen = { summary: {}, items: [] }
      } finally {
        this.usageQwenLoading = false
      }
    },
    async loadUsageDaily () {
      try {
        const resp = await this.$http.get('/admin/ai-analysis/usage/daily', { params: { days: this.usageDays, provider: 'deepseek' } })
        if (resp.data && resp.data.code === 0 && resp.data.data && Array.isArray(resp.data.data.daily)) {
          this.usageDaily = this.fillDailyRange(resp.data.data.daily, this.usageDays)
        } else {
          this.usageDaily = this.fillDailyRange([], this.usageDays)
        }
      } catch (e) {
        this.usageDaily = this.fillDailyRange([], this.usageDays)
      }
    },
    async loadUsageQwenDaily () {
      try {
        const resp = await this.$http.get('/admin/ai-analysis/usage/daily', { params: { days: this.usageDaysQwen, provider: 'qwen' } })
        if (resp.data && resp.data.code === 0 && resp.data.data && Array.isArray(resp.data.data.daily)) {
          this.usageQwenDaily = this.fillDailyRange(resp.data.data.daily, this.usageDaysQwen)
        } else {
          this.usageQwenDaily = this.fillDailyRange([], this.usageDaysQwen)
        }
      } catch (e) {
        this.usageQwenDaily = this.fillDailyRange([], this.usageDaysQwen)
      }
    },
    fillDailyRange (daily, days) {
      const map = {}
      ;(daily || []).forEach(d => {
        const dateStr = d.date != null ? String(d.date).slice(0, 10) : ''
        if (dateStr) map[dateStr] = { date: dateStr, callCount: Number(d.callCount) || 0, totalTokens: Number(d.totalTokens) || 0, inputTokens: Number(d.inputTokens) || 0, outputTokens: Number(d.outputTokens) || 0 }
      })
      const result = []
      const end = new Date()
      for (let i = days - 1; i >= 0; i--) {
        const d = new Date(end)
        d.setDate(d.getDate() - i)
        const dateStr = d.getFullYear() + '-' + String(d.getMonth() + 1).padStart(2, '0') + '-' + String(d.getDate()).padStart(2, '0')
        result.push(map[dateStr] || { date: dateStr, callCount: 0, totalTokens: 0, inputTokens: 0, outputTokens: 0 })
      }
      return result
    },
    onUsageDaysChange (provider) {
      if (provider === 'deepseek') {
        this.loadUsage()
        this.loadUsageDaily()
      } else {
        this.loadUsageQwen()
        this.loadUsageQwenDaily()
      }
    },
    refreshUsageDeepseek () {
      this.loadUsage()
      this.loadUsageDaily()
    },
    refreshUsageQwen () {
      this.loadUsageQwen()
      this.loadUsageQwenDaily()
    },
    openUsageDetail (provider, days) {
      this.usageDetailProvider = provider
      this.usageDetailDays = days
      this.usageDetailPage = 1
      this.usageDetailTotal = 0
      this.usageDetailItems = []
      this.usageDetailOpen = true
      this.loadUsageDetailPage()
    },
    closeUsageDetail () {
      this.usageDetailOpen = false
    },
    async loadUsageDetailPage () {
      if (!this.usageDetailProvider) return
      this.usageDetailLoading = true
      try {
        const resp = await this.$http.get('/admin/ai-analysis/usage', {
          params: { days: this.usageDetailDays, provider: this.usageDetailProvider, page: this.usageDetailPage, pageSize: this.usageDetailPageSize }
        })
        if (resp.data && resp.data.code === 0 && resp.data.data) {
          const d = resp.data.data
          this.usageDetailItems = d.items || []
          this.usageDetailTotal = Number(d.total) || 0
        } else {
          this.usageDetailItems = []
          this.usageDetailTotal = 0
        }
      } catch (e) {
        this.usageDetailItems = []
        this.usageDetailTotal = 0
      } finally {
        this.usageDetailLoading = false
      }
    },
    goUsageDetailPage (page) {
      if (page < 1 || page > this.usageDetailTotalPages) return
      this.usageDetailPage = page
      this.loadUsageDetailPage()
    },
    /** Vue 2 单节点 ref 为元素；极少数情况下为数组 */
    resolveChartCanvasRef (name) {
      const r = this.$refs[name]
      if (!r) return null
      return r instanceof HTMLCanvasElement ? r : (Array.isArray(r) ? r[0] : r)
    },
    renderUsageCharts () {
      this.renderOneUsageChart('deepseek', this.usageDaily, 'deepseekCall', 'deepseekToken')
      this.renderOneUsageChart('qwen', this.usageQwenDaily, 'qwenCall', 'qwenToken')
    },
    renderOneUsageChart (provider, daily, callRef, tokenRef) {
      this.$nextTick(() => {
        const days = daily || []
        const formatLabel = (d) => {
          const v = d.date
          if (!v) return ''
          const s = String(v).slice(0, 10)
          return s.length >= 10 ? s.slice(5) : s
        }
        const labels = days.map(formatLabel)
        const callData = days.map(d => d.callCount || 0)
        const tokenData = days.map(d => d.totalTokens || 0)
        const isQwen = provider === 'qwen'
        const blue = 'rgba(37, 99, 235, 0.7)'
        const blueBorder = 'rgb(37, 99, 235)'
        const teal = 'rgba(20, 184, 166, 0.7)'
        const tealBorder = 'rgb(20, 184, 166)'
        const callEl = this.resolveChartCanvasRef(callRef)
        const tokenEl = this.resolveChartCanvasRef(tokenRef)
        if (callEl) {
          if (this.chartInstances[callRef]) this.chartInstances[callRef].destroy()
          this.chartInstances[callRef] = new ChartJS(callEl, {
            type: 'line',
            data: { labels, datasets: [{ label: 'API 请求次数', data: callData, borderColor: blueBorder, backgroundColor: blue, fill: true, tension: 0.2 }] },
            options: {
              responsive: true,
              maintainAspectRatio: false,
              plugins: { legend: { display: false } },
              scales: { y: { beginAtZero: true } }
            }
          })
        }
        if (tokenEl) {
          if (this.chartInstances[tokenRef]) this.chartInstances[tokenRef].destroy()
          this.chartInstances[tokenRef] = new ChartJS(tokenEl, {
            type: 'bar',
            data: { labels, datasets: [{ label: 'Tokens', data: tokenData, backgroundColor: isQwen ? teal : blue, borderColor: isQwen ? tealBorder : blueBorder, borderWidth: 1 }] },
            options: {
              responsive: true,
              maintainAspectRatio: false,
              plugins: { legend: { display: false } },
              scales: { y: { beginAtZero: true } }
            }
          })
        }
      })
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
  max-width: 1100px;
  margin: 0 auto;
}
.provider-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 24px;
  margin-bottom: 24px;
}
@media (max-width: 768px) {
  .provider-grid {
    grid-template-columns: 1fr;
  }
}
.provider-panel {
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  overflow: hidden;
}
.provider-panel .config-card {
  border: none;
  border-radius: 0;
  margin-top: 0;
}
.provider-panel .placeholder {
  border: none;
  margin: 0;
  padding: 24px;
}
.provider-panel .config-loaded-hint {
  margin: 0 0 16px 0;
}
.config-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 24px;
  margin-bottom: 24px;
}
@media (max-width: 768px) {
  .config-row {
    grid-template-columns: 1fr;
  }
}
.usage-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 24px;
  margin-bottom: 24px;
}
@media (max-width: 768px) {
  .usage-row {
    grid-template-columns: 1fr;
  }
}
.token-usage-card {
  margin-top: 0;
}
.qwen-config-card {
  margin-top: 0;
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
.link-button {
  background: none;
  border: none;
  color: #2563eb;
  cursor: pointer;
  font-size: 13px;
  padding: 0;
}
.link-button:hover { text-decoration: underline; }
.link-button:disabled { color: #9ca3af; cursor: not-allowed; text-decoration: none; }
.usage-charts {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
  margin-bottom: 16px;
}
.usage-chart-block {
  min-height: 120px;
}
.usage-chart-title {
  font-size: 12px;
  color: #6b7280;
  margin-bottom: 6px;
}
.usage-chart-wrap {
  height: 120px;
  position: relative;
}
@media (max-width: 500px) {
  .usage-charts { grid-template-columns: 1fr; }
}
.usage-detail-actions {
  margin-top: 8px;
}
.primary-button-outline {
  background: #fff;
  color: #2563eb;
  border: 1px solid #2563eb;
}
.primary-button-outline:hover { background: #eff6ff; }
.usage-detail-modal {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.4);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}
.usage-detail-dialog {
  background: #fff;
  border-radius: 8px;
  max-width: 90vw;
  width: 800px;
  max-height: 85vh;
  display: flex;
  flex-direction: column;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.15);
}
.usage-detail-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px;
  border-bottom: 1px solid #e5e7eb;
}
.usage-detail-header h3 {
  margin: 0;
  font-size: 16px;
  color: #1f2937;
}
.usage-detail-close {
  background: none;
  border: none;
  font-size: 24px;
  line-height: 1;
  color: #6b7280;
  cursor: pointer;
  padding: 0 4px;
}
.usage-detail-close:hover { color: #1f2937; }
.usage-detail-dialog .usage-table-wrap {
  flex: 1;
  overflow: auto;
  padding: 16px 20px;
}
.usage-pagination {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 16px;
  padding: 12px 20px;
  border-top: 1px solid #e5e7eb;
}
.usage-pagination-info {
  font-size: 13px;
  color: #6b7280;
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
.form-hint.subtle {
  color: #b4bcc8;
  font-style: italic;
}
.daily-run-row {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
}
.daily-run-row .inline-msg {
  margin: 0;
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
