<template>
  <div class="nexus-console-page">
    <div class="page-head">
      <h1>快捷运维（Nexus）</h1>
      <div class="head-actions">
        <router-link to="/" class="secondary-button">返回首页</router-link>
      </div>
    </div>

    <section class="block">
      <h2 class="block-title">云账号接入（阿里云）</h2>

      <div v-if="accountsLoading" class="placeholder">加载中...</div>
      <div v-else>
        <div v-if="accounts.length" class="select-row">
          <label class="select-label">已配置账号：</label>
          <select v-model="selectedAccountId" @change="onAccountChange" class="select">
            <option v-for="a in accounts" :key="a.id" :value="a.id">
              {{ a.displayName }}（{{ a.regionId }}）
            </option>
          </select>
        </div>
        <div v-else class="placeholder">暂无账号。请先添加一个阿里云 AccessKey。</div>
      </div>

      <div class="form-card">
        <div class="form-title">添加阿里云账号</div>
        <form class="form" @submit.prevent="createAccount">
          <div class="form-row">
            <label>展示名称</label>
            <input v-model="form.displayName" placeholder="例如：本公司生产环境" required />
          </div>

          <div class="form-row">
            <label>地域（regionId）</label>
            <input v-model="form.regionId" placeholder="例如：cn-hangzhou" required />
          </div>

          <div class="form-row">
            <label>AccessKeyId</label>
            <input v-model="form.accessKeyId" placeholder="输入 AccessKeyId" required />
          </div>

          <div class="form-row">
            <label>AccessKeySecret</label>
            <input v-model="form.accessKeySecret" type="password" placeholder="输入 AccessKeySecret" required />
          </div>

          <div class="form-row">
            <label>自动巡检间隔（秒，选填）</label>
            <input v-model="form.autoSyncIntervalSeconds" type="number" min="10" placeholder="例如：60（留空则用全局默认）" />
          </div>

          <button class="primary-button" type="submit" :disabled="creatingAccount">
            {{ creatingAccount ? '保存中...' : '保存账号' }}
          </button>
        </form>
      </div>
    </section>

    <section class="block" v-if="selectedAccountId != null">
      <div class="block-head">
        <h2 class="block-title">实例列表</h2>
        <div class="block-actions">
          <button class="secondary-button" :disabled="syncing" @click="manualSync">同步元数据 + CPU 指标</button>
        </div>
      </div>

      <div v-if="instancesLoading" class="placeholder">加载中...</div>
      <div v-else>
        <div v-if="!instances.length" class="placeholder">暂无实例数据。</div>
        <table v-else class="data-table">
          <thead>
            <tr>
              <th>实例 ID</th>
              <th>名称</th>
              <th>状态</th>
              <th>规格</th>
              <th>公网 IP</th>
              <th>内网 IP</th>
              <th>内存(MB)</th>
            </tr>
          </thead>
          <tbody>
            <tr
              v-for="ins in instances"
              :key="ins.instanceId"
              :class="{ active: ins.instanceId === selectedInstanceId }"
              @click="selectInstance(ins.instanceId)"
            >
              <td>{{ ins.instanceId }}</td>
              <td>{{ ins.instanceName || '-' }}</td>
              <td>{{ ins.status || '-' }}</td>
              <td>{{ ins.instanceType || '-' }}</td>
              <td>{{ ins.publicIp || '-' }}</td>
              <td>{{ ins.privateIp || '-' }}</td>
              <td>{{ ins.memoryMb ?? '-' }}</td>
            </tr>
          </tbody>
        </table>
      </div>
    </section>

    <section class="block metrics-block" v-if="selectedAccountId != null && selectedInstanceId != null">
      <div class="block-head">
        <h2 class="block-title">监控指标（最近 {{ metricLimit }} 点）</h2>
        <div class="block-actions">
          <div class="metric-tabs">
            <button
              class="tab-button"
              :class="{ active: metricType === 'cpu' }"
              :disabled="metricsLoading"
              @click="metricType = 'cpu'; loadMetricChart()"
            >CPU 利用率</button>
            <button
              class="tab-button"
              :class="{ active: metricType === 'memory' }"
              :disabled="metricsLoading"
              @click="metricType = 'memory'; loadMetricChart()"
            >内存利用率</button>
          </div>
          <button class="secondary-button" @click="loadMetricChart" :disabled="metricsLoading">刷新图表</button>
        </div>
      </div>
      <div v-if="metricsLoading" class="placeholder">加载中...</div>
      <div v-else class="chart-wrap">
        <canvas ref="cpuChart"></canvas>
      </div>
    </section>

    <section class="block ssh-block" v-if="selectedAccountId != null && selectedInstanceId != null">
      <div class="block-head">
        <h2 class="block-title">本地 SSH 快捷入口（复制 / 唤起）</h2>
        <div class="block-actions">
          <button class="secondary-button" @click="copySshCommand" :disabled="sshCopying || !sshHost">
            {{ sshCopying ? '复制中...' : '复制 SSH 命令' }}
          </button>
          <button class="secondary-button" @click="openSshUri" :disabled="sshOpening || !sshHost">
            {{ sshOpening ? '唤起中...' : '唤起（ssh://）' }}
          </button>
        </div>
      </div>

      <div class="ssh-hint">
        说明：平台不会保存或代管私钥。所谓“免密”取决于你本机是否已配置好 key-based 登录（`~/.ssh/config` 或本机 key）。
      </div>

      <div class="ssh-grid">
        <div class="ssh-row">
          <label>目标主机</label>
          <div class="ssh-host">{{ sshHost || '-' }}</div>
        </div>
        <div class="ssh-row">
          <label>用户名</label>
          <input v-model="sshUser" />
        </div>
        <div class="ssh-row">
          <label>端口</label>
          <input v-model.number="sshPort" type="number" min="1" />
        </div>
        <div class="ssh-row">
          <label>本地私钥路径（可选）</label>
          <input v-model="sshKeyPath" placeholder="/path/to/id_rsa（不填则依赖本机默认 key）" />
        </div>
      </div>

      <div class="command-box">
        <div class="command-label">生成命令预览</div>
        <pre class="command-pre"><code>{{ sshCommand }}</code></pre>
      </div>
    </section>
  </div>
</template>

<script>
import { Chart as ChartJS, registerables } from 'chart.js'
ChartJS.register(...registerables)

export default {
  name: 'NexusConsoleView',
  data () {
    return {
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
          // 清空敏感字段
          this.form.accessKeySecret = ''
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
          await this.loadMetricChart()
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

        const resp = await this.$http.get(apiPath, {
          params: { limit: this.metricLimit }
        })
        if (resp.data && resp.data.code === 0) {
          const points = resp.data.data || []
          const labels = points.map(p => (p.ts ? p.ts.toString().slice(5, 16).replace('T', ' ') : ''))
          const values = points.map(p => Number(p.value || 0))
          const title = this.metricType === 'cpu' ? 'CPU(%)' : '内存(%)'
          this.renderCpuChart(labels, values, title)
        } else {
          this.renderCpuChart([], [], this.metricType === 'cpu' ? 'CPU(%)' : '内存(%)')
        }
      } catch (e) {
        this.renderCpuChart([], [], this.metricType === 'cpu' ? 'CPU(%)' : '内存(%)')
      } finally {
        this.metricsLoading = false
      }
    },
    renderCpuChart (labels, values, label) {
      if (this.chart) {
        this.chart.destroy()
        this.chart = null
      }
      if (!this.$refs.cpuChart) return
      const ctx = this.$refs.cpuChart
      this.chart = new ChartJS(ctx, {
        type: 'line',
        data: {
          labels,
          datasets: [
            {
              label: label || 'CPU(%)',
              data: values,
              borderColor: '#2563eb',
              backgroundColor: 'rgba(37, 99, 235, 0.1)',
              pointRadius: 2,
              tension: 0.3
            }
          ]
        },
        options: {
          responsive: true,
          plugins: {
            legend: { display: true }
          },
          scales: {
            y: {
              min: 0,
              max: 100,
              title: { display: true, text: 'Percent' }
            }
          }
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
          {
            type,
            sshUser: this.sshUser,
            sshPort: this.sshPort,
            keyPath: this.sshKeyPath || null,
            host: this.sshHost,
            command: this.sshCommand
          }
        )
      } catch (e) {
        // 审计失败不阻断用户操作
      }
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
        alert('已复制到剪贴板：SSH 命令')
      } catch (e) {
        alert((e && e.message) ? e.message : '复制失败，请手动复制命令预览')
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
        alert('唤起失败（可能浏览器不支持 ssh:// 协议处理器）。你仍可复制命令手动登录。')
      } finally {
        this.sshOpening = false
      }
    }
  }
}
</script>

<style scoped>
.nexus-console-page {
  max-width: 1100px;
  margin: 0 auto;
  padding: 16px;
  color: #0f172a;
  background: #f1f5f9;
  min-height: 100%;
  box-sizing: border-box;
}
.page-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}
.page-head h1 {
  margin: 0;
  font-size: 22px;
  font-weight: 800;
}
.secondary-button {
  display: inline-block;
  padding: 8px 14px;
  background: #e2e8f0;
  color: #0f172a;
  border-radius: 6px;
  text-decoration: none;
  font-size: 14px;
  font-weight: 600;
  border: 1px solid #94a3b8;
  cursor: pointer;
}
.secondary-button:hover { background: #cbd5e1; }
.primary-button {
  padding: 10px 14px;
  background: #1d4ed8;
  color: #fff;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  font-size: 14px;
  font-weight: 700;
}
.primary-button:disabled { opacity: 0.7; cursor: not-allowed; }

.block {
  background: #ffffff;
  border: 1px solid #e5e7eb;
  border-radius: 10px;
  padding: 16px;
  margin-bottom: 16px;
}
.block-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 10px;
}
.block-title {
  margin: 0 0 10px;
  font-size: 16px;
  font-weight: 800;
}
.block-actions { display: flex; gap: 10px; align-items: center; }
.placeholder {
  padding: 16px;
  color: #64748b;
  font-weight: 600;
}
.select-row { display: flex; align-items: center; gap: 10px; margin-bottom: 14px; }
.select-label { font-weight: 700; font-size: 13px; color: #334155; }
.select {
  padding: 8px 10px;
  border-radius: 6px;
  border: 1px solid #cbd5e1;
  background: #fff;
  font-size: 14px;
  min-width: 320px;
}

.form-card {
  border-top: 1px solid #e5e7eb;
  padding-top: 14px;
}
.form-title {
  font-weight: 800;
  margin-bottom: 10px;
}
.form .form-row {
  display: flex;
  flex-direction: column;
  margin-bottom: 10px;
}
.form .form-row label {
  font-size: 13px;
  color: #475569;
  font-weight: 700;
  margin-bottom: 6px;
}
.form input {
  padding: 10px 10px;
  border-radius: 6px;
  border: 1px solid #cbd5e1;
  font-size: 14px;
}
.data-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 14px;
}
.data-table th, .data-table td {
  border: 1px solid #e5e7eb;
  padding: 10px;
  text-align: left;
}
.data-table th {
  background: #eef2ff;
  font-weight: 800;
}
.data-table tr {
  cursor: pointer;
}
.data-table tr.active {
  background: #eff6ff;
}

.metrics-block .chart-wrap {
  padding: 8px 4px 0;
}

.metric-tabs {
  display: flex;
  gap: 8px;
  align-items: center;
}

.tab-button {
  padding: 8px 12px;
  border-radius: 8px;
  border: 1px solid #cbd5e1;
  background: #fff;
  cursor: pointer;
  font-size: 13px;
  font-weight: 700;
  color: #0f172a;
}

.tab-button.active {
  background: #eff6ff;
  border-color: #93c5fd;
  color: #1d4ed8;
}
.tab-button:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}

.ssh-block { margin-top: 16px; }
.ssh-hint {
  color: #64748b;
  font-size: 13px;
  margin-bottom: 12px;
  font-weight: 600;
  line-height: 1.5;
}
.ssh-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px 18px;
  margin-bottom: 12px;
}
.ssh-row {
  display: flex;
  flex-direction: column;
}
.ssh-row label {
  font-size: 13px;
  color: #475569;
  font-weight: 800;
  margin-bottom: 6px;
}
.ssh-row input {
  padding: 10px 10px;
  border-radius: 6px;
  border: 1px solid #cbd5e1;
  font-size: 14px;
}
.ssh-host {
  padding: 10px 10px;
  border-radius: 6px;
  border: 1px solid #cbd5e1;
  background: #f8fafc;
  font-size: 14px;
  font-weight: 700;
  color: #0f172a;
  min-height: 40px;
  display: flex;
  align-items: center;
}
.command-box {
  border: 1px dashed #cbd5e1;
  border-radius: 10px;
  padding: 12px;
  background: #ffffff;
  margin-top: 12px;
}
.command-label {
  font-size: 13px;
  font-weight: 800;
  color: #334155;
  margin-bottom: 8px;
}
.command-pre {
  margin: 0;
  padding: 10px;
  background: #0b1220;
  color: #e2e8f0;
  border-radius: 8px;
  overflow: auto;
}
.command-pre code { white-space: pre; }
</style>

