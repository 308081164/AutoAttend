<template>
  <div class="prototype-project-page">
    <div class="page-head">
      <div class="head-left">
        <h1 class="title">Penpot 诊断测试</h1>
      </div>
      <div class="head-right">
        <router-link to="/prototype" class="secondary-button">返回快原型列表</router-link>
      </div>
    </div>

    <div class="layout diagnose-layout">
      <div class="left">
        <div class="section-card">
          <div class="section-title">诊断操作</div>
          <div class="section-body">
            <p class="section-hint">
              逐步执行以下测试以定位 Penpot 连接问题。每个测试会返回详细的状态信息与耗时。
            </p>
            <div class="diagnose-actions">
              <button
                type="button"
                class="primary-button"
                :disabled="running"
                @click="runFullDiagnose"
              >
                {{ running ? '诊断中…' : '完整诊断（一键运行）' }}
              </button>
              <button
                type="button"
                class="secondary-button"
                :disabled="running"
                @click="clearResults"
              >
                清空结果
              </button>
            </div>
            <div class="diagnose-actions">
              <button
                type="button"
                class="secondary-button"
                :disabled="running"
                @click="runSingleTest('config')"
              >
                检查 Penpot 配置
              </button>
              <button
                type="button"
                class="secondary-button"
                :disabled="running"
                @click="runSingleTest('credential')"
              >
                检查租户凭证
              </button>
            </div>
            <div class="diagnose-actions">
              <button
                type="button"
                class="secondary-button"
                :disabled="running"
                @click="runSingleTest('backend')"
              >
                测试 Penpot 后端连通
              </button>
              <button
                type="button"
                class="secondary-button"
                :disabled="running"
                @click="runSingleTest('frontend')"
              >
                测试 Penpot 前端连通
              </button>
            </div>
            <div class="diagnose-actions">
              <button
                type="button"
                class="secondary-button"
                :disabled="running"
                @click="runSingleTest('rpc')"
              >
                测试 RPC 路径探测
              </button>
              <button
                type="button"
                class="secondary-button"
                :disabled="running"
                @click="runSingleTest('bootstrap')"
              >
                测试创建账号流程
              </button>
            </div>
            <div v-if="error" class="error-msg">{{ error }}</div>
          </div>
        </div>
      </div>

      <div class="main">
        <div class="section-card">
          <div class="section-title">诊断结果</div>
          <div class="section-body">
            <div v-if="!results.length && !running" class="muted">
              点击左侧按钮开始诊断，或使用「完整诊断」一键运行所有测试。
            </div>
            <div v-if="running && !results.length" class="muted">
              正在执行诊断，请稍候…
            </div>

            <div v-if="summary" class="diagnose-summary" :class="summaryClass">
              {{ summary }}
            </div>

            <div v-for="(item, idx) in results" :key="idx" class="check-item" :class="'check-' + item.status">
              <div class="check-header">
                <span class="check-status-icon">{{ statusIcon(item.status) }}</span>
                <span class="check-name">{{ item.name }}</span>
                <span class="check-duration">{{ item.durationMs != null ? item.durationMs + 'ms' : '' }}</span>
              </div>
              <div class="check-message">{{ item.message }}</div>
              <!-- 配置详情 -->
              <div v-if="item.config" class="check-detail">
                <table class="config-table">
                  <tr v-for="(val, key) in item.config" :key="key">
                    <td class="config-key">{{ key }}</td>
                    <td class="config-val">{{ val }}</td>
                  </tr>
                </table>
              </div>
              <!-- 警告信息 -->
              <div v-if="item.warnings && item.warnings.length" class="check-warnings">
                <div v-for="(w, wi) in item.warnings" :key="wi" class="check-warning-item">
                  {{ w }}
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'PenpotDiagnoseView',
  data () {
    return {
      running: false,
      error: '',
      results: [],
      summary: ''
    }
  },
  computed: {
    summaryClass () {
      if (!this.summary) return ''
      if (this.summary.indexOf('全部通过') >= 0 && this.summary.indexOf('警告') < 0) {
        return 'summary-ok'
      }
      if (this.summary.indexOf('警告') >= 0) {
        return 'summary-warn'
      }
      return 'summary-fail'
    }
  },
  methods: {
    statusIcon (status) {
      if (status === 'ok') return '[OK]'
      if (status === 'warn') return '[!]'
      if (status === 'fail') return '[X]'
      return '[?]'
    },
    async runFullDiagnose () {
      this.running = true
      this.error = ''
      this.results = []
      this.summary = ''
      try {
        const resp = await this.$http.get('/admin/ui-prototype/penpot/diagnose')
        if (resp.data && resp.data.code === 0 && resp.data.data) {
          const d = resp.data.data
          this.results = Array.isArray(d.checks) ? d.checks : []
          this.summary = d.summary || ''
        } else {
          this.error = (resp.data && resp.data.message) || '诊断接口返回异常'
        }
      } catch (e) {
        this.error = (e.response && e.response.data && e.response.data.message) || '诊断请求失败：' + (e.message || '')
      } finally {
        this.running = false
      }
    },
    async runSingleTest (type) {
      this.running = true
      this.error = ''
      try {
        if (type === 'config') {
          await this.runAndFilter('配置检查')
        } else if (type === 'credential') {
          await this.runAndFilter('租户凭证检查')
        } else if (type === 'backend') {
          await this.runAndFilter('DIRECT 基址连通')
        } else if (type === 'frontend') {
          await this.runAndFilter('PRIMARY 基址连通')
        } else if (type === 'rpc') {
          await this.runAndFilter('RPC')
        } else if (type === 'bootstrap') {
          await this.testBootstrap()
        }
      } catch (e) {
        this.error = (e.response && e.response.data && e.response.data.message) || '测试失败：' + (e.message || '')
      } finally {
        this.running = false
      }
    },
    async runAndFilter (keyword) {
      const resp = await this.$http.get('/admin/ui-prototype/penpot/diagnose')
      if (resp.data && resp.data.code === 0 && resp.data.data) {
        const d = resp.data.data
        const allChecks = Array.isArray(d.checks) ? d.checks : []
        // 过滤出匹配的检查项
        const filtered = allChecks.filter(c => c.name && c.name.indexOf(keyword) >= 0)
        // 追加到结果（去重）
        const existingNames = new Set(this.results.map(r => r.name))
        for (const item of filtered) {
          if (!existingNames.has(item.name)) {
            this.results.push(item)
          }
        }
        this.summary = d.summary || ''
      } else {
        this.error = (resp.data && resp.data.message) || '诊断接口返回异常'
      }
    },
    async testBootstrap () {
      try {
        const resp = await this.$http.post('/admin/ui-prototype/penpot/bootstrap', {})
        if (resp.data && resp.data.code === 0) {
          this.results.push({
            name: '创建账号流程 (bootstrap)',
            status: 'ok',
            message: 'Penpot 工作区开通成功',
            durationMs: null
          })
        } else {
          this.results.push({
            name: '创建账号流程 (bootstrap)',
            status: 'fail',
            message: (resp.data && resp.data.message) || '开通失败',
            durationMs: null
          })
        }
      } catch (e) {
        const msg = (e.response && e.response.data && e.response.data.message) || e.message || '未知错误'
        this.results.push({
          name: '创建账号流程 (bootstrap)',
          status: 'fail',
          message: '开通失败：' + msg,
          durationMs: null
        })
      }
    },
    clearResults () {
      this.results = []
      this.summary = ''
      this.error = ''
    }
  }
}
</script>

<style scoped>
.prototype-project-page {
  max-width: none;
  width: 100%;
  margin: 0;
  padding: var(--space-lg) var(--space-xl);
  color: var(--text-primary);
  background: var(--bg-page);
  min-height: 100vh;
  box-sizing: border-box;
}
.page-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-md);
  margin-bottom: var(--space-lg);
}
.head-left {
  display: flex;
  align-items: baseline;
  gap: var(--space-md);
  flex-wrap: wrap;
}
.title {
  margin: 0;
  font-size: 22px;
  font-weight: 600;
  color: var(--text-primary);
}
.diagnose-layout {
  display: grid;
  grid-template-columns: 320px minmax(0, 1fr);
  gap: var(--space-lg);
}
.left .section-card {
  margin-bottom: var(--space-lg);
}
.main .section-card {
  min-height: calc(100vh - 170px);
}
.section-card {
  background: var(--bg-card);
  border: 1px solid var(--border-primary);
  border-radius: var(--radius-lg);
  padding: var(--space-lg);
  box-shadow: var(--shadow-sm);
}
.section-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: var(--space-sm);
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-sm);
}
.section-hint {
  margin-top: var(--space-sm);
  color: var(--text-tertiary);
  font-size: 12px;
  font-weight: 400;
  line-height: 1.5;
}
.diagnose-actions {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-sm);
  margin-top: var(--space-sm);
}
.primary-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: var(--space-sm) var(--space-lg);
  background: var(--brand-blue);
  color: #ffffff;
  border: none;
  border-radius: var(--radius-md);
  cursor: pointer;
  font-weight: 500;
  font-size: 14px;
  transition: opacity 0.2s;
  box-shadow: var(--shadow-sm);
}
.primary-button:hover {
  opacity: 0.85;
}
.primary-button:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
.secondary-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: var(--space-sm) var(--space-md);
  border: 1px solid var(--border-primary);
  background: var(--bg-card);
  border-radius: var(--radius-md);
  cursor: pointer;
  font-weight: 500;
  font-size: 14px;
  color: var(--text-primary);
  text-decoration: none;
  transition: border-color 0.2s;
}
.secondary-button:hover {
  border-color: var(--brand-blue);
  color: var(--brand-blue);
}
.secondary-button:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
.muted {
  color: var(--text-tertiary);
  font-weight: 400;
  font-size: 13px;
}
.error-msg {
  margin-top: var(--space-sm);
  color: var(--danger);
  font-weight: 500;
  font-size: 13px;
}
.diagnose-summary {
  padding: var(--space-sm) var(--space-md);
  border-radius: var(--radius-md);
  font-weight: 500;
  font-size: 14px;
  margin-bottom: var(--space-md);
}
.summary-ok {
  background: #f0fdf4;
  color: #166534;
  border: 1px solid #bbf7d0;
}
.summary-warn {
  background: #fffbeb;
  color: #92400e;
  border: 1px solid #fde68a;
}
.summary-fail {
  background: #fef2f2;
  color: #991b1b;
  border: 1px solid #fecaca;
}
.check-item {
  border: 1px solid var(--border-primary);
  border-radius: var(--radius-md);
  padding: var(--space-sm) var(--space-md);
  margin-bottom: var(--space-sm);
  background: var(--bg-card);
}
.check-ok {
  border-left: 3px solid #22c55e;
}
.check-warn {
  border-left: 3px solid #f59e0b;
}
.check-fail {
  border-left: 3px solid #ef4444;
}
.check-header {
  display: flex;
  align-items: center;
  gap: var(--space-sm);
}
.check-status-icon {
  font-weight: 700;
  font-size: 13px;
  min-width: 30px;
}
.check-ok .check-status-icon {
  color: #22c55e;
}
.check-warn .check-status-icon {
  color: #f59e0b;
}
.check-fail .check-status-icon {
  color: #ef4444;
}
.check-name {
  font-weight: 600;
  font-size: 13px;
  color: var(--text-primary);
  flex: 1;
}
.check-duration {
  font-size: 12px;
  color: var(--text-tertiary);
  font-weight: 400;
}
.check-message {
  margin-top: 4px;
  font-size: 13px;
  color: var(--text-secondary);
  line-height: 1.5;
}
.check-detail {
  margin-top: var(--space-sm);
}
.config-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 12px;
}
.config-table td {
  padding: 3px 6px;
  border-bottom: 1px solid var(--border-primary);
  vertical-align: top;
}
.config-key {
  color: var(--text-tertiary);
  font-weight: 500;
  white-space: nowrap;
  width: 180px;
}
.config-val {
  color: var(--text-primary);
  word-break: break-all;
}
.check-warnings {
  margin-top: var(--space-xs);
}
.check-warning-item {
  font-size: 12px;
  color: #92400e;
  padding: 2px 0;
}
@media (max-width: 900px) {
  .diagnose-layout { grid-template-columns: 1fr; }
}
</style>
