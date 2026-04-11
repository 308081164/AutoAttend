<template>
  <div class="nexus-page">
    <!-- ===== Top Bar: Account Selector + Actions ===== -->
    <div class="nexus-topbar">
      <div class="nexus-topbar-left">
        <h1 class="nexus-title">快捷运维</h1>
        <div class="nexus-account-selector">
          <select v-model="selectedAccountId" @change="onAccountChange" class="nx-select" v-if="accounts.length">
            <option v-for="a in accounts" :key="a.id" :value="a.id">
              {{ a.displayName }}（{{ a.regionId }}）
            </option>
          </select>
          <span v-else class="nx-no-account">暂无云账号</span>
        </div>
      </div>
      <div class="nexus-topbar-right">
        <button class="nx-btn nx-btn--primary" @click="showAddAccount = !showAddAccount" v-if="!showAddAccount">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="14" height="14"><path d="M12 5v14M5 12h14"/></svg>
          添加账号
        </button>
        <button class="nx-btn nx-btn--secondary" @click="manualSync" :disabled="syncing || !selectedAccountId" v-if="selectedAccountId">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="14" height="14"><path d="M1 4v6h6M23 20v-6h-6"/><path d="M20.49 9A9 9 0 005.64 5.64L1 10m22 4l-4.64 4.36A9 9 0 013.51 15"/></svg>
          {{ syncing ? '同步中...' : '同步' }}
        </button>
      </div>
    </div>

    <!-- ===== Add Account Panel (collapsible) ===== -->
    <div class="nx-panel nx-panel--add" v-if="showAddAccount">
      <div class="nx-panel-header">
        <h3 class="nx-panel-title">添加阿里云账号</h3>
        <button class="nx-icon-btn" @click="showAddAccount = false">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="16" height="16"><path d="M18 6L6 18M6 6l12 12"/></svg>
        </button>
      </div>
      <form class="nx-form" @submit.prevent="createAccount">
        <div class="nx-form-grid">
          <div class="nx-form-item">
            <label>展示名称</label>
            <input v-model="form.displayName" placeholder="例如：生产环境" required />
          </div>
          <div class="nx-form-item">
            <label>地域 (regionId)</label>
            <input v-model="form.regionId" placeholder="例如：cn-hangzhou" required />
          </div>
          <div class="nx-form-item">
            <label>AccessKeyId</label>
            <input v-model="form.accessKeyId" placeholder="输入 AccessKeyId" required />
          </div>
          <div class="nx-form-item">
            <label>AccessKeySecret</label>
            <input v-model="form.accessKeySecret" type="password" placeholder="输入 AccessKeySecret" required />
          </div>
          <div class="nx-form-item">
            <label>自动巡检间隔（秒）</label>
            <input v-model="form.autoSyncIntervalSeconds" type="number" min="10" placeholder="默认 60" />
          </div>
        </div>
        <div class="nx-form-actions">
          <button type="button" class="nx-btn nx-btn--ghost" @click="showAddAccount = false">取消</button>
          <button type="submit" class="nx-btn nx-btn--primary" :disabled="creatingAccount">
            {{ creatingAccount ? '保存中...' : '保存账号' }}
          </button>
        </div>
      </form>
    </div>

    <!-- ===== No Account Empty State ===== -->
    <div class="nx-empty-state" v-if="!accountsLoading && !accounts.length && !showAddAccount">
      <div class="nx-empty-icon">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" width="48" height="48"><path d="M5 12h14M12 5l7 7-7 7"/></svg>
      </div>
      <h3 class="nx-empty-title">还没有接入云账号</h3>
      <p class="nx-empty-desc">添加阿里云 AccessKey 后，即可查看和管理 ECS 实例。</p>
      <button class="nx-btn nx-btn--primary" @click="showAddAccount = true">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="14" height="14"><path d="M12 5v14M5 12h14"/></svg>
        添加第一个账号
      </button>
    </div>

    <!-- ===== Main Content: Master-Detail ===== -->
    <div class="nexus-master-detail" v-if="accounts.length">
      <!-- Left: Instance List -->
      <div class="nexus-master">
        <div class="nexus-master-header">
          <span class="nexus-master-title">实例列表</span>
          <span class="nexus-master-count" v-if="instances.length">{{ instances.length }} 台</span>
        </div>
        <div class="nexus-master-search">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="14" height="14" class="nx-search-icon"><circle cx="11" cy="11" r="8"/><path d="M21 21l-4.35-4.35"/></svg>
          <input v-model="instanceSearch" placeholder="搜索实例 ID 或名称..." class="nx-search-input" />
        </div>
        <div class="nexus-master-list" v-if="!instancesLoading && filteredInstances.length">
          <div
            v-for="ins in filteredInstances"
            :key="ins.instanceId"
            class="nexus-instance-card"
            :class="{ 'is-active': ins.instanceId === selectedInstanceId }"
            @click="selectInstance(ins.instanceId)"
          >
            <div class="nx-inst-header">
              <span class="nx-inst-name">{{ ins.instanceName || ins.instanceId }}</span>
              <span class="nx-inst-status" :class="'nx-inst-status--' + (ins.status || 'unknown')">
                {{ statusLabel(ins.status) }}
              </span>
            </div>
            <div class="nx-inst-meta">
              <span class="nx-inst-id">{{ ins.instanceId }}</span>
              <span class="nx-inst-type" v-if="ins.instanceType">{{ ins.instanceType }}</span>
            </div>
            <div class="nx-inst-ips">
              <span class="nx-inst-ip" v-if="ins.publicIp" :title="'公网: ' + ins.publicIp">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="12" height="12"><path d="M5 12.55a11 11 0 0114.08 0M1.42 9a16 16 0 0121.16 0M8.53 16.11a6 6 0 016.95 0M12 20h.01"/></svg>
                {{ ins.publicIp }}
              </span>
              <span class="nx-inst-ip nx-inst-ip--private" v-if="ins.privateIp" :title="'内网: ' + ins.privateIp">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="12" height="12"><rect x="2" y="2" width="20" height="8" rx="2"/><rect x="2" y="14" width="20" height="8" rx="2"/><circle cx="6" cy="6" r="1"/><circle cx="6" cy="18" r="1"/></svg>
                {{ ins.privateIp }}
              </span>
            </div>
          </div>
        </div>
        <div class="nexus-master-empty" v-else-if="!instancesLoading">
          <p v-if="instanceSearch">没有匹配的实例</p>
          <p v-else>暂无实例数据，点击「同步」获取</p>
        </div>
        <div class="nexus-master-loading" v-if="instancesLoading">
          <div class="nx-spinner"></div>
          <span>加载实例中...</span>
        </div>
      </div>

      <!-- Right: Detail Panel -->
      <div class="nexus-detail" v-if="selectedInstance">
        <!-- Instance Info Bar -->
        <div class="nx-detail-header">
          <div class="nx-detail-info">
            <h2 class="nx-detail-name">{{ selectedInstance.instanceName || selectedInstance.instanceId }}</h2>
            <div class="nx-detail-tags">
              <span class="nx-tag">{{ selectedInstance.instanceType || '-' }}</span>
              <span class="nx-tag nx-tag--mem" v-if="selectedInstance.memoryMb">{{ selectedInstance.memoryMb }} MB</span>
              <span class="nx-tag nx-tag--status" :class="'nx-tag--status-' + (selectedInstance.status || 'unknown')">{{ statusLabel(selectedInstance.status) }}</span>
            </div>
          </div>
        </div>

        <!-- Metrics Section -->
        <div class="nx-section">
          <div class="nx-section-header">
            <h3 class="nx-section-title">监控指标</h3>
            <div class="nx-section-actions">
              <div class="nx-tabs">
                <button
                  class="nx-tab"
                  :class="{ 'is-active': metricType === 'cpu' }"
                  :disabled="metricsLoading"
                  @click="metricType = 'cpu'; loadMetricChart()"
                >CPU</button>
                <button
                  class="nx-tab"
                  :class="{ 'is-active': metricType === 'memory' }"
                  :disabled="metricsLoading"
                  @click="metricType = 'memory'; loadMetricChart()"
                >内存</button>
              </div>
              <button class="nx-btn nx-btn--ghost nx-btn--sm" @click="loadMetricChart" :disabled="metricsLoading">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="12" height="12"><path d="M1 4v6h6M23 20v-6h-6"/><path d="M20.49 9A9 9 0 005.64 5.64L1 10m22 4l-4.64 4.36A9 9 0 013.51 15"/></svg>
                刷新
              </button>
            </div>
          </div>
          <div class="nx-chart-area" v-if="!metricsLoading">
            <canvas ref="cpuChart"></canvas>
          </div>
          <div class="nx-chart-loading" v-else>
            <div class="nx-spinner"></div>
          </div>
        </div>

        <!-- SSH Section -->
        <div class="nx-section">
          <div class="nx-section-header">
            <h3 class="nx-section-title">SSH 快捷入口</h3>
            <div class="nx-section-actions">
              <button class="nx-btn nx-btn--secondary nx-btn--sm" @click="copySshCommand" :disabled="sshCopying || !sshHost">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="12" height="12"><rect x="9" y="9" width="13" height="13" rx="2"/><path d="M5 15H4a2 2 0 01-2-2V4a2 2 0 012-2h9a2 2 0 012 2v1"/></svg>
                {{ sshCopying ? '复制中...' : '复制命令' }}
              </button>
              <button class="nx-btn nx-btn--secondary nx-btn--sm" @click="openSshUri" :disabled="sshOpening || !sshHost">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="12" height="12"><path d="M18 13v6a2 2 0 01-2 2H5a2 2 0 01-2-2V8a2 2 0 012-2h6M15 3h6v6M10 14L21 3"/></svg>
                {{ sshOpening ? '唤起中...' : 'SSH 客户端' }}
              </button>
            </div>
          </div>
          <div class="nx-ssh-config">
            <div class="nx-ssh-row">
              <label>主机</label>
              <span class="nx-ssh-value">{{ sshHost || '-' }}</span>
            </div>
            <div class="nx-ssh-row">
              <label>用户</label>
              <input v-model="sshUser" class="nx-ssh-input" />
            </div>
            <div class="nx-ssh-row">
              <label>端口</label>
              <input v-model.number="sshPort" type="number" min="1" class="nx-ssh-input nx-ssh-input--sm" />
            </div>
            <div class="nx-ssh-row">
              <label>密钥路径</label>
              <input v-model="sshKeyPath" placeholder="可选，默认使用本机 key" class="nx-ssh-input" />
            </div>
          </div>
          <div class="nx-command-preview">
            <div class="nx-command-label">命令预览</div>
            <pre class="nx-command-code"><code>{{ sshCommand }}</code></pre>
          </div>
          <div class="nx-ssh-hint">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="14" height="14"><circle cx="12" cy="12" r="10"/><path d="M12 16v-4M12 8h.01"/></svg>
            平台不保存私钥，依赖本机 key-based 登录。
          </div>
        </div>
      </div>

      <!-- No Instance Selected -->
      <div class="nexus-detail-empty" v-else-if="!instancesLoading && accounts.length">
        <div class="nx-detail-empty-icon">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" width="40" height="40"><rect x="2" y="3" width="20" height="14" rx="2"/><path d="M8 21h8M12 17v4"/></svg>
        </div>
        <p>从左侧选择一台实例查看详情</p>
      </div>
    </div>
  </div>
</template>

<script>
import { Chart as ChartJS, registerables } from 'chart.js'
ChartJS.register(...registerables)

export default {
  name: 'NexusConsoleView',
  data () {
    return {
      showAddAccount: false,
      accountsLoading: false,
      accounts: [],
      selectedAccountId: null,
      creatingAccount: false,
      syncing: false,

      form: {
        displayName: '',
        regionId: '',
        accessKeyId: '',
        accessKeySecret: '',
        autoSyncIntervalSeconds: null
      },

      instanceSearch: '',
      instancesLoading: false,
      instances: [],
      selectedInstanceId: null,

      metricsLoading: false,
      metricLimit: 60,
      metricType: 'cpu',
      chart: null,

      sshCopying: false,
      sshOpening: false,
      sshUser: 'root',
      sshPort: 22,
      sshKeyPath: ''
    }
  },
  created () {
    this.loadAccounts()
  },
  computed: {
    selectedInstance () {
      if (!this.instances || !this.selectedInstanceId) return null
      return this.instances.find(i => i.instanceId === this.selectedInstanceId) || null
    },
    filteredInstances () {
      if (!this.instanceSearch) return this.instances
      const q = this.instanceSearch.toLowerCase()
      return this.instances.filter(ins =>
        (ins.instanceId && ins.instanceId.toLowerCase().includes(q)) ||
        (ins.instanceName && ins.instanceName.toLowerCase().includes(q))
      )
    },
    sshHost () {
      if (!this.selectedInstance) return null
      return this.selectedInstance.publicIp || this.selectedInstance.privateIp || null
    },
    sshCommand () {
      if (!this.sshHost) return ''
      const keyPath = this.sshKeyPath && String(this.sshKeyPath).trim()
      const keyPart = keyPath ? `-i ${this.shellQuote(keyPath)} ` : ''
      const userPart = this.sshUser && String(this.sshUser).trim() ? String(this.sshUser).trim() : 'root'
      const portPart = this.sshPort ? ` -p ${this.sshPort}` : ''
      return `ssh ${keyPart}${userPart}@${this.sshHost}${portPart}`.trim()
    }
  },
  methods: {
    statusLabel (status) {
      const map = { Running: '运行中', Stopped: '已停止', Starting: '启动中', Stopping: '停止中' }
      return map[status] || status || '未知'
    },
    async loadAccounts () {
      this.accountsLoading = true
      try {
        const resp = await this.$http.get('/admin/nexus/accounts')
        if (resp.data && resp.data.code === 0 && resp.data.data) {
          this.accounts = resp.data.data.items || []
          if (this.accounts.length && this.selectedAccountId == null) {
            this.selectedAccountId = this.accounts[0].id
            await this.loadInstances()
          }
        } else {
          this.accounts = []
        }
      } catch (e) {
        this.accounts = []
      } finally {
        this.accountsLoading = false
      }
    },
    async onAccountChange () {
      await this.loadInstances()
    },
    async createAccount () {
      this.creatingAccount = true
      try {
        const payload = {
          displayName: this.form.displayName,
          regionId: this.form.regionId,
          accessKeyId: this.form.accessKeyId,
          accessKeySecret: this.form.accessKeySecret,
          autoSyncIntervalSeconds: this.form.autoSyncIntervalSeconds === '' ? null : this.form.autoSyncIntervalSeconds
        }
        const resp = await this.$http.post('/admin/nexus/accounts', payload)
        if (resp.data && resp.data.code === 0 && resp.data.data && resp.data.data.id) {
          this.form.accessKeySecret = ''
          this.showAddAccount = false
          await this.loadAccounts()
          this.selectedAccountId = resp.data.data.id
          await this.loadInstances()
        } else {
          alert((resp.data && resp.data.message) || '保存失败')
        }
      } catch (e) {
        alert((e.response && e.response.data && e.response.data.message) || '保存失败')
      } finally {
        this.creatingAccount = false
      }
    },
    async loadInstances () {
      this.instancesLoading = true
      this.selectedInstanceId = null
      this.instances = []
      try {
        const resp = await this.$http.get(`/admin/nexus/accounts/${this.selectedAccountId}/instances`)
        if (resp.data && resp.data.code === 0 && resp.data.data) {
          this.instances = resp.data.data || []
        } else {
          this.instances = []
        }
      } catch (e) {
        this.instances = []
      } finally {
        this.instancesLoading = false
      }
    },
    async manualSync () {
      if (!confirm('将为当前账号执行：元数据同步 + CPU/内存指标拉取。继续吗？')) return
      this.syncing = true
      try {
        const resp = await this.$http.post(`/admin/nexus/accounts/${this.selectedAccountId}/sync`)
        if (resp.data && resp.data.code === 0) {
          await this.loadInstances()
          if (this.selectedInstanceId) await this.loadMetricChart()
        } else {
          alert((resp.data && resp.data.message) || '同步失败')
        }
      } catch (e) {
        alert((e.response && e.response.data && e.response.data.message) || '同步失败')
      } finally {
        this.syncing = false
      }
    },
    selectInstance (instanceId) {
      this.selectedInstanceId = instanceId
      this.loadMetricChart()
    },
    async loadMetricChart () {
      if (!this.selectedAccountId || !this.selectedInstanceId) return
      this.metricsLoading = true
      try {
        const apiPath =
          this.metricType === 'cpu'
            ? `/admin/nexus/accounts/${this.selectedAccountId}/instances/${this.selectedInstanceId}/cpu-metrics`
            : `/admin/nexus/accounts/${this.selectedAccountId}/instances/${this.selectedInstanceId}/memory-metrics`

        const resp = await this.$http.get(apiPath, { params: { limit: this.metricLimit } })
        if (resp.data && resp.data.code === 0) {
          const points = resp.data.data || []
          const labels = points.map(p => (p.ts ? p.ts.toString().slice(5, 16).replace('T', ' ') : ''))
          const values = points.map(p => Number(p.value || 0))
          const title = this.metricType === 'cpu' ? 'CPU(%)' : '内存(%)'
          this.renderChart(labels, values, title)
        } else {
          this.renderChart([], [], this.metricType === 'cpu' ? 'CPU(%)' : '内存(%)')
        }
      } catch (e) {
        this.renderChart([], [], this.metricType === 'cpu' ? 'CPU(%)' : '内存(%)')
      } finally {
        this.metricsLoading = false
      }
    },
    renderChart (labels, values, label) {
      if (this.chart) { this.chart.destroy(); this.chart = null }
      if (!this.$refs.cpuChart) return
      const ctx = this.$refs.cpuChart
      this.chart = new ChartJS(ctx, {
        type: 'line',
        data: {
          labels,
          datasets: [{
            label: label || 'CPU(%)',
            data: values,
            borderColor: '#1456F0',
            backgroundColor: 'rgba(20, 86, 240, 0.08)',
            pointRadius: 1.5,
            pointHoverRadius: 4,
            tension: 0.35,
            borderWidth: 2,
            fill: true
          }]
        },
        options: {
          responsive: true,
          maintainAspectRatio: false,
          plugins: {
            legend: { display: false }
          },
          scales: {
            y: { min: 0, max: 100, grid: { color: '#f0f0f0' }, ticks: { font: { size: 11 } } },
            x: { grid: { display: false }, ticks: { font: { size: 11 }, maxRotation: 0, maxTicksLimit: 8 } }
          },
          interaction: { intersect: false, mode: 'index' }
        }
      })
    },
    shellQuote (s) {
      const v = String(s)
      if (v.includes(' ') || v.includes('\t') || v.includes('"') || v.includes("'")) {
        return `"${v.replace(/"/g, '\\"')}"`
      }
      return v
    },
    async auditSshAction (type) {
      if (!this.selectedAccountId || !this.selectedInstanceId) return
      try {
        await this.$http.post(
          `/admin/nexus/accounts/${this.selectedAccountId}/instances/${this.selectedInstanceId}/ssh/action`,
          { type, sshUser: this.sshUser, sshPort: this.sshPort, keyPath: this.sshKeyPath || null, host: this.sshHost, command: this.sshCommand }
        )
      } catch (e) { /* audit failure does not block user */ }
    },
    async copyToClipboard (text) {
      if (navigator.clipboard && navigator.clipboard.writeText) {
        await navigator.clipboard.writeText(text)
        return
      }
      const ta = document.createElement('textarea')
      ta.value = text
      document.body.appendChild(ta)
      ta.select()
      document.execCommand('copy')
      document.body.removeChild(ta)
    },
    async copySshCommand () {
      if (!this.sshCommand) return
      try {
        this.sshCopying = true
        await this.copyToClipboard(this.sshCommand)
        await this.auditSshAction('copy')
        alert('已复制到剪贴板')
      } catch (e) {
        alert('复制失败，请手动复制命令预览')
      } finally {
        this.sshCopying = false
      }
    },
    async openSshUri () {
      if (!this.sshHost) return
      try {
        this.sshOpening = true
        await this.auditSshAction('open')
        const uri = `ssh://${encodeURIComponent(this.sshUser || 'root')}@${this.sshHost}${this.sshPort ? ':' + this.sshPort : ''}`
        window.location.href = uri
      } catch (e) {
        alert('唤起失败，请复制命令手动登录。')
      } finally {
        this.sshOpening = false
      }
    }
  }
}
</script>

<style scoped>
.nexus-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
  height: calc(100vh - 56px - 48px);
  overflow: hidden;
}

/* ===== Top Bar ===== */
.nexus-topbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-shrink: 0;
  gap: 16px;
}
.nexus-topbar-left {
  display: flex;
  align-items: center;
  gap: 16px;
}
.nexus-title {
  margin: 0;
  font-size: 20px;
  font-weight: 700;
  color: var(--text-primary, #1F2329);
  white-space: nowrap;
}
.nexus-account-selector {
  display: flex;
  align-items: center;
}
.nexus-topbar-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

/* ===== Buttons ===== */
.nx-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 7px 14px;
  border-radius: 8px;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
  border: none;
  white-space: nowrap;
}
.nx-btn--primary {
  background: var(--brand-blue, #1456F0);
  color: #fff;
}
.nx-btn--primary:hover { opacity: 0.85; }
.nx-btn--primary:disabled { opacity: 0.45; cursor: not-allowed; }
.nx-btn--secondary {
  background: #fff;
  color: var(--text-primary, #1F2329);
  border: 1px solid var(--border-primary, #dee0e3);
}
.nx-btn--secondary:hover { border-color: var(--brand-blue, #1456F0); color: var(--brand-blue, #1456F0); }
.nx-btn--secondary:disabled { opacity: 0.45; cursor: not-allowed; }
.nx-btn--ghost {
  background: transparent;
  color: var(--text-secondary, #646A73);
}
.nx-btn--ghost:hover { background: var(--bg-hover, #f5f6f7); color: var(--text-primary, #1F2329); }
.nx-btn--sm { padding: 4px 10px; font-size: 12px; }
.nx-icon-btn {
  width: 28px; height: 28px; border: none; background: transparent; border-radius: 6px;
  cursor: pointer; display: flex; align-items: center; justify-content: center;
  color: var(--text-secondary, #646A73); transition: background 0.2s;
}
.nx-icon-btn:hover { background: var(--bg-hover, #f5f6f7); }

/* ===== Select ===== */
.nx-select {
  padding: 6px 32px 6px 12px;
  border-radius: 8px;
  border: 1px solid var(--border-primary, #dee0e3);
  background: #fff;
  font-size: 13px;
  color: var(--text-primary, #1F2329);
  cursor: pointer;
  appearance: auto;
  max-width: 280px;
}
.nx-select:focus { border-color: var(--brand-blue, #1456F0); outline: none; }
.nx-no-account { font-size: 13px; color: var(--text-disabled, #8F959E); }

/* ===== Add Account Panel ===== */
.nx-panel {
  background: #fff;
  border: 1px solid var(--border-primary, #dee0e3);
  border-radius: 12px;
  flex-shrink: 0;
  overflow: hidden;
}
.nx-panel--add { padding: 20px; }
.nx-panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}
.nx-panel-title {
  margin: 0;
  font-size: 15px;
  font-weight: 600;
  color: var(--text-primary, #1F2329);
}
.nx-form-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 12px 20px;
}
.nx-form-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.nx-form-item label {
  font-size: 12px;
  font-weight: 500;
  color: var(--text-secondary, #646A73);
}
.nx-form-item input {
  padding: 7px 10px;
  border-radius: 6px;
  border: 1px solid var(--border-primary, #dee0e3);
  font-size: 13px;
  color: var(--text-primary, #1F2329);
  transition: border-color 0.2s;
}
.nx-form-item input:focus { border-color: var(--brand-blue, #1456F0); outline: none; }
.nx-form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 16px;
}

/* ===== Empty State ===== */
.nx-empty-state {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  text-align: center;
}
.nx-empty-icon {
  width: 72px; height: 72px; border-radius: 50%;
  background: var(--bg-hover, #f5f6f7);
  display: flex; align-items: center; justify-content: center;
  color: var(--text-disabled, #8F959E);
}
.nx-empty-title { margin: 0; font-size: 16px; font-weight: 600; color: var(--text-primary, #1F2329); }
.nx-empty-desc { margin: 0; font-size: 13px; color: var(--text-secondary, #646A73); }

/* ===== Master-Detail Layout ===== */
.nexus-master-detail {
  flex: 1;
  display: flex;
  gap: 16px;
  min-height: 0;
  overflow: hidden;
}

/* ===== Master (Left) ===== */
.nexus-master {
  width: 340px;
  min-width: 280px;
  background: #fff;
  border: 1px solid var(--border-primary, #dee0e3);
  border-radius: 12px;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  flex-shrink: 0;
}
.nexus-master-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 16px 0;
  flex-shrink: 0;
}
.nexus-master-title { font-size: 14px; font-weight: 600; color: var(--text-primary, #1F2329); }
.nexus-master-count { font-size: 12px; color: var(--text-disabled, #8F959E); }
.nexus-master-search {
  padding: 10px 12px;
  flex-shrink: 0;
  position: relative;
}
.nx-search-icon {
  position: absolute;
  left: 22px;
  top: 50%;
  transform: translateY(-50%);
  color: var(--text-disabled, #8F959E);
  pointer-events: none;
}
.nx-search-input {
  width: 100%;
  padding: 6px 10px 6px 30px;
  border-radius: 6px;
  border: 1px solid var(--border-primary, #dee0e3);
  font-size: 13px;
  color: var(--text-primary, #1F2329);
  background: var(--bg-page, #f0f2f5);
  transition: border-color 0.2s;
}
.nx-search-input:focus { border-color: var(--brand-blue, #1456F0); outline: none; background: #fff; }
.nexus-master-list {
  flex: 1;
  overflow-y: auto;
  padding: 0 8px 8px;
}
.nexus-master-list::-webkit-scrollbar { width: 4px; }
.nexus-master-list::-webkit-scrollbar-thumb { background: #ddd; border-radius: 2px; }

/* ===== Instance Card ===== */
.nexus-instance-card {
  padding: 12px;
  border-radius: 8px;
  cursor: pointer;
  transition: background 0.15s, box-shadow 0.15s;
  margin-bottom: 4px;
  border: 1px solid transparent;
}
.nexus-instance-card:hover { background: var(--bg-hover, #f5f6f7); }
.nexus-instance-card.is-active {
  background: #F0F5FF;
  border-color: var(--brand-blue, #1456F0);
}
.nx-inst-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 4px;
}
.nx-inst-name {
  font-size: 13px;
  font-weight: 600;
  color: var(--text-primary, #1F2329);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.nx-inst-status {
  font-size: 11px;
  font-weight: 500;
  padding: 1px 6px;
  border-radius: 4px;
  white-space: nowrap;
  flex-shrink: 0;
}
.nx-inst-status--Running { background: #ECFDF5; color: #059669; }
.nx-inst-status--Stopped { background: #FEF2F2; color: #DC2626; }
.nx-inst-status--Starting, .nx-inst-status--Stopping { background: #FFF7ED; color: #D97706; }
.nx-inst-status--unknown { background: #f5f6f7; color: #8F959E; }
.nx-inst-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 6px;
}
.nx-inst-id { font-size: 11px; color: var(--text-disabled, #8F959E); font-family: monospace; }
.nx-inst-type { font-size: 11px; color: var(--text-secondary, #646A73); }
.nx-inst-ips {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}
.nx-inst-ip {
  display: inline-flex;
  align-items: center;
  gap: 3px;
  font-size: 11px;
  color: var(--text-secondary, #646A73);
  font-family: monospace;
}
.nx-inst-ip--private { color: var(--text-disabled, #8F959E); }

.nexus-master-empty {
  padding: 32px 16px;
  text-align: center;
  color: var(--text-disabled, #8F959E);
  font-size: 13px;
}
.nexus-master-loading {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  color: var(--text-disabled, #8F959E);
  font-size: 13px;
}

/* ===== Detail (Right) ===== */
.nexus-detail {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 16px;
  overflow-y: auto;
  min-width: 0;
}
.nexus-detail::-webkit-scrollbar { width: 4px; }
.nexus-detail::-webkit-scrollbar-thumb { background: #ddd; border-radius: 2px; }

.nx-detail-header {
  background: #fff;
  border: 1px solid var(--border-primary, #dee0e3);
  border-radius: 12px;
  padding: 16px 20px;
  flex-shrink: 0;
}
.nx-detail-name { margin: 0 0 8px; font-size: 16px; font-weight: 600; color: var(--text-primary, #1F2329); }
.nx-detail-tags { display: flex; gap: 6px; flex-wrap: wrap; }
.nx-tag {
  font-size: 11px;
  font-weight: 500;
  padding: 2px 8px;
  border-radius: 4px;
  background: var(--bg-hover, #f5f6f7);
  color: var(--text-secondary, #646A73);
}
.nx-tag--mem { background: #F0F5FF; color: var(--brand-blue, #1456F0); }
.nx-tag--status-Running { background: #ECFDF5; color: #059669; }
.nx-tag--status-Stopped { background: #FEF2F2; color: #DC2626; }

/* ===== Section ===== */
.nx-section {
  background: #fff;
  border: 1px solid var(--border-primary, #dee0e3);
  border-radius: 12px;
  padding: 16px 20px;
  flex-shrink: 0;
}
.nx-section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}
.nx-section-title { margin: 0; font-size: 14px; font-weight: 600; color: var(--text-primary, #1F2329); }
.nx-section-actions { display: flex; align-items: center; gap: 8px; }

/* ===== Tabs ===== */
.nx-tabs {
  display: flex;
  background: var(--bg-page, #f0f2f5);
  border-radius: 6px;
  padding: 2px;
}
.nx-tab {
  padding: 4px 12px;
  border: none;
  background: transparent;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 500;
  color: var(--text-secondary, #646A73);
  cursor: pointer;
  transition: all 0.2s;
}
.nx-tab.is-active { background: #fff; color: var(--brand-blue, #1456F0); box-shadow: 0 1px 2px rgba(0,0,0,0.06); }
.nx-tab:disabled { opacity: 0.5; cursor: not-allowed; }

/* ===== Chart ===== */
.nx-chart-area { height: 200px; position: relative; }
.nx-chart-loading { height: 200px; display: flex; align-items: center; justify-content: center; }

/* ===== SSH ===== */
.nx-ssh-config {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
  gap: 10px;
  margin-bottom: 12px;
}
.nx-ssh-row { display: flex; flex-direction: column; gap: 3px; }
.nx-ssh-row label { font-size: 11px; font-weight: 500; color: var(--text-secondary, #646A73); }
.nx-ssh-value {
  padding: 5px 8px;
  border-radius: 6px;
  background: var(--bg-page, #f0f2f5);
  font-size: 13px;
  font-family: monospace;
  color: var(--text-primary, #1F2329);
  min-height: 32px;
  display: flex;
  align-items: center;
}
.nx-ssh-input {
  padding: 5px 8px;
  border-radius: 6px;
  border: 1px solid var(--border-primary, #dee0e3);
  font-size: 13px;
  color: var(--text-primary, #1F2329);
  transition: border-color 0.2s;
}
.nx-ssh-input:focus { border-color: var(--brand-blue, #1456F0); outline: none; }
.nx-ssh-input--sm { max-width: 100px; }

.nx-command-preview {
  border-radius: 8px;
  overflow: hidden;
  border: 1px solid var(--border-primary, #dee0e3);
}
.nx-command-label {
  font-size: 11px;
  font-weight: 500;
  color: var(--text-secondary, #646A73);
  padding: 6px 10px;
  background: var(--bg-page, #f0f2f5);
  border-bottom: 1px solid var(--border-primary, #dee0e3);
}
.nx-command-code {
  margin: 0;
  padding: 10px 12px;
  background: #1F2329;
  color: #E8E8E8;
  font-family: 'SF Mono', 'Menlo', 'Monaco', 'Consolas', ui-monospace, monospace;
  font-size: 12px;
  line-height: 1.5;
  overflow-x: auto;
}
.nx-command-code code { white-space: pre; }

.nx-ssh-hint {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: var(--text-disabled, #8F959E);
  margin-top: 8px;
}

/* ===== Detail Empty ===== */
.nexus-detail-empty {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  color: var(--text-disabled, #8F959E);
  font-size: 14px;
}
.nx-detail-empty-icon {
  color: var(--text-disabled, #8F959E);
  opacity: 0.5;
}

/* ===== Spinner ===== */
.nx-spinner {
  width: 20px; height: 20px;
  border: 2px solid #e5e7eb;
  border-top-color: var(--brand-blue, #1456F0);
  border-radius: 50%;
  animation: nx-spin 0.6s linear infinite;
}
@keyframes nx-spin { to { transform: rotate(360deg); } }

/* ===== Responsive ===== */
@media (max-width: 900px) {
  .nexus-master-detail { flex-direction: column; overflow-y: auto; }
  .nexus-master { width: 100%; min-width: 0; max-height: 300px; }
  .nexus-detail { overflow-y: visible; }
  .nexus-page { height: auto; overflow-y: auto; }
}
</style>
