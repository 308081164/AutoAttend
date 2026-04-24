<template>
  <div class="quote-wizard-page">
    <div class="page-head">
      <router-link to="/quote" class="back-link">← 报价项目</router-link>
      <h1>解决方案级报价 · 智能规划</h1>
      <p class="lead">
        通过 Agent 对话梳理需求 → 自动拆分交付物 → 进入编辑页自动生成功能清单。全程 AI 驱动，减少手工操作。
      </p>
    </div>

    <!-- Step 1: 项目概况 -->
    <div class="card">
      <div class="card-step-header">
        <span class="step-num">1</span>
        <h2>项目概况</h2>
      </div>
      <div class="grid">
        <label class="full">项目名称 <input v-model="form.name" class="inp wide" placeholder="必填" /></label>
        <label>项目类型
          <select v-model="form.projectType" class="inp">
            <option value="website">企业官网</option>
            <option value="ecommerce_miniprogram">电商小程序</option>
            <option value="admin">管理后台</option>
            <option value="app">APP</option>
            <option value="other">其他</option>
          </select>
        </label>
        <label>默认技术栈
          <select v-model="form.techStack" class="inp">
            <option value="vue_node">Vue+Node</option>
            <option value="react_java">React+Java</option>
            <option value="miniprogram">小程序原生</option>
            <option value="flutter">Flutter</option>
            <option value="other">其他</option>
          </select>
        </label>
      </div>
      <div class="actions-row" style="margin-top:16px">
        <button type="button" class="btn primary" :disabled="!form.name.trim() || step1Saving" @click="saveAndCreateAgent">
          {{ step1Saving ? '创建中…' : '保存并进入需求分析' }}
        </button>
        <p v-if="step1Msg" :class="step1Ok ? 'ok' : 'err'">{{ step1Msg }}</p>
      </div>
    </div>

    <!-- Step 2: Agent 需求分析（项目保存后显示） -->
    <div v-if="wizardProjectId" class="card">
      <div class="card-step-header">
        <span class="step-num">2</span>
        <h2>需求分析 Agent</h2>
        <span v-if="agentSessionStatus" class="agent-badge" :class="'agent-badge--' + agentSessionStatus">
          {{ agentSessionStatus === 'active' ? '进行中' : '已结束' }}
        </span>
      </div>
      <p class="hint">通过 Agent 对话引导客户描述需求，系统将自动提炼需求摘要用于后续步骤。</p>

      <!-- Agent 操作区 -->
      <div v-if="!activeAgentSession" class="actions-row">
        <button type="button" class="btn primary" :disabled="agentCreating" @click="createAgentSession">
          {{ agentCreating ? '创建中…' : '创建 Agent 会话' }}
        </button>
        <p v-if="agentMsg" :class="agentMsgOk ? 'ok' : 'err'">{{ agentMsg }}</p>
      </div>

      <!-- 活跃会话 -->
      <div v-if="activeAgentSession" class="agent-session-info">
        <div class="agent-info-row">
          <span>会话 ID: {{ activeAgentSession.id }}</span>
          <span :class="activeAgentSession.status === 'active' ? 'ok' : 'hint'">
            {{ activeAgentSession.status === 'active' ? '● 进行中' : '● 已结束' }}
          </span>
        </div>
        <div class="agent-info-row">
          <span>分享链接：</span>
          <a v-if="activeAgentSession.clientUrl" :href="activeAgentSession.clientUrl" target="_blank" rel="noopener" class="agent-link">
            {{ activeAgentSession.clientUrl }}
          </a>
          <span v-else class="hint">加载中…</span>
        </div>
        <div class="actions-row" style="margin-top:12px">
          <button v-if="activeAgentSession.status === 'active'" type="button" class="btn secondary" @click="refreshAgentSession">刷新状态</button>
          <button v-if="activeAgentSession.summaryText" type="button" class="btn primary" @click="applySummaryAndProceed">
            ✓ 需求已确认，进入下一步
          </button>
          <p v-if="activeAgentSession.status === 'active'" class="hint">等待客户完成需求描述后，终止会话即可生成需求摘要。</p>
        </div>
      </div>

      <!-- 已结束但无摘要 -->
      <div v-else-if="endedAgentSessions.length" class="agent-session-info">
        <p class="hint">已有 {{ endedAgentSessions.length }} 个已结束的会话。</p>
        <div class="actions-row">
          <button type="button" class="btn primary" @click="createAgentSession">创建新会话</button>
        </div>
      </div>
    </div>

    <!-- Step 3: 交付物拆分（Agent摘要确认后显示） -->
    <div v-if="confirmedSummary" class="card">
      <div class="card-step-header">
        <span class="step-num">3</span>
        <h2>交付物拆分</h2>
        <span v-if="outlineAutoDone" class="ok" style="font-size:13px">✓ 已自动拆分</span>
      </div>
      <p class="hint">系统已基于需求摘要自动拆分交付物，请核对后进入下一步。也可手动调整或重新拆分。</p>
      <div class="actions-row">
        <button type="button" class="btn secondary" :disabled="outlineParsing" @click="runOutlineAi">
          {{ outlineParsing ? '分析中…' : '重新 AI 拆分' }}
        </button>
        <button type="button" class="btn secondary" @click="addRow">＋ 添加交付物</button>
      </div>
      <p v-if="outlineMsg" :class="outlineOk ? 'ok' : 'err'">{{ outlineMsg }}</p>

      <table v-if="rows.length" class="data-table deliver-table">
        <thead>
          <tr>
            <th>键（英文短码）</th>
            <th>显示名</th>
            <th>计价技术栈</th>
            <th>范围摘要</th>
            <th></th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="(row, idx) in rows" :key="'r-' + idx">
            <td><input v-model="row.deliverableKey" class="inp narrow" placeholder="web" /></td>
            <td><input v-model="row.deliverableLabel" class="inp" placeholder="Web 管理端" /></td>
            <td>
              <select v-model="row.techStack" class="inp">
                <option value="vue_node">Vue+Node</option>
                <option value="react_java">React+Java</option>
                <option value="miniprogram">小程序原生</option>
                <option value="flutter">Flutter</option>
                <option value="other">其他</option>
              </select>
            </td>
            <td><input v-model="row.scopeSummary" class="inp wide" placeholder="可选" /></td>
            <td><button type="button" class="btn-sm danger" :disabled="rows.length <= 2" @click="removeRow(idx)">删除</button></td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- Step 4: 进入编辑页 -->
    <div v-if="confirmedSummary" class="card actions-footer">
      <div class="card-step-header" style="justify-content:center">
        <span class="step-num">4</span>
        <h2>进入功能清单与报价</h2>
      </div>
      <p class="hint" style="margin-bottom:16px">
        系统将自动基于需求摘要和已确认的交付物，AI 生成功能清单。进入后请耐心等待解析完成。
      </p>
      <button type="button" class="btn primary large" :disabled="!canContinue" @click="goEditor">进入功能清单与报价</button>
      <p v-if="!canContinue" class="hint">请确认至少 2 个有效交付物（键唯一）。</p>
    </div>
  </div>
</template>

<script>
export default {
  name: 'QuoteSolutionWizardView',
  data () {
    return {
      form: {
        name: '',
        projectType: 'other',
        techStack: 'vue_node'
      },
      // Step 1
      step1Saving: false,
      step1Msg: '',
      step1Ok: false,
      wizardProjectId: null,
      // Step 2: Agent
      agentCreating: false,
      agentMsg: '',
      agentMsgOk: false,
      activeAgentSession: null,
      endedAgentSessions: [],
      confirmedSummary: '',
      // Step 3: 交付物拆分
      demandText: '',
      rows: [],
      outlineParsing: false,
      outlineMsg: '',
      outlineOk: false,
      outlineAutoDone: false
    }
  },
  computed: {
    canContinue () {
      const name = (this.form.name || '').trim()
      if (!name) return false
      const keys = new Set()
      let ok = 0
      for (const r of this.rows) {
        const k = (r.deliverableKey || '').trim().toLowerCase()
        if (!k || keys.has(k)) continue
        keys.add(k)
        ok++
      }
      return ok >= 2
    },
    agentSessionStatus () {
      if (this.activeAgentSession) return this.activeAgentSession.status
      return null
    }
  },
  methods: {
    // ---- Step 1: 保存项目并准备 Agent ----
    async saveAndCreateAgent () {
      const name = (this.form.name || '').trim()
      if (!name) return
      this.step1Saving = true
      this.step1Msg = ''
      try {
        const resp = await this.$http.post('/admin/quote/projects', {
          name,
          projectType: this.form.projectType,
          techStack: this.form.techStack,
          quoteKind: 'solution'
        })
        if (resp.data && resp.data.code === 0 && resp.data.data && resp.data.data.id) {
          this.wizardProjectId = resp.data.data.id
          this.step1Msg = '项目已创建'
          this.step1Ok = true
        } else {
          this.step1Msg = (resp.data && resp.data.message) || '创建失败'
          this.step1Ok = false
        }
      } catch (e) {
        this.step1Msg = (e.response && e.response.data && e.response.data.message) || '请求失败'
        this.step1Ok = false
      } finally {
        this.step1Saving = false
      }
    },

    // ---- Step 2: Agent 会话管理 ----
    async fetchAgentSessions () {
      if (!this.wizardProjectId) return
      try {
        const resp = await this.$http.get(`/admin/agent/quote/projects/${this.wizardProjectId}/agent-sessions`)
        if (resp.data && resp.data.code === 0 && resp.data.data) {
          const items = resp.data.data.items || []
          this.activeAgentSession = items.find(s => s.status === 'active') || null
          this.endedAgentSessions = items.filter(s => s.status !== 'active')
        }
      } catch (e) { void e }
    },
    async createAgentSession () {
      if (!this.wizardProjectId) return
      this.agentCreating = true
      this.agentMsg = ''
      try {
        const body = {}
        const name = (this.form.name || '').trim()
        if (name) {
          body.backgroundTexts = [{ label: '项目名称', content: name }]
        }
        const resp = await this.$http.post(`/admin/agent/quote/projects/${this.wizardProjectId}/agent-sessions`, body)
        if (resp.data && resp.data.code === 0 && resp.data.data) {
          await this.fetchAgentSessions()
          this.agentMsg = 'Agent 会话已创建，请将分享链接发送给客户'
          this.agentMsgOk = true
        } else {
          this.agentMsg = (resp.data && resp.data.message) || '创建失败'
          this.agentMsgOk = false
        }
      } catch (e) {
        this.agentMsg = (e.response && e.response.data && e.response.data.message) || '创建失败'
        this.agentMsgOk = false
      } finally {
        this.agentCreating = false
      }
    },
    async refreshAgentSession () {
      await this.fetchAgentSessions()
      // 如果会话刚结束且有摘要，自动提示
      if (this.activeAgentSession && this.activeAgentSession.status === 'ended' && this.activeAgentSession.summaryText) {
        this.agentMsg = '会话已结束，需求摘要已生成，请点击「需求已确认」进入下一步'
        this.agentMsgOk = true
      }
    },

    // Agent 摘要确认 → 自动拆分交付物
    async applySummaryAndProceed () {
      if (!this.activeAgentSession || !this.activeAgentSession.summaryText) return
      this.confirmedSummary = this.activeAgentSession.summaryText
      this.demandText = this.activeAgentSession.summaryText
      // 自动触发 AI 拆分交付物
      await this.runOutlineAi(true)
    },

    // ---- Step 3: 交付物拆分 ----
    async runOutlineAi (silent) {
      const text = (this.demandText || '').trim()
      if (!text) {
        if (!silent) {
          this.outlineMsg = '请先填写需求与范围说明'
          this.outlineOk = false
        }
        return
      }
      this.outlineParsing = true
      if (!silent) this.outlineMsg = ''
      try {
        const resp = await this.$http.post('/admin/quote/ai/parse-deliverables-outline', {
          requirementText: text,
          projectType: this.form.projectType,
          techStack: this.form.techStack,
          prdSummary: ''
        })
        if (resp.data && resp.data.code === 0 && resp.data.data && resp.data.data.deliverables) {
          this.rows = resp.data.data.deliverables.map(d => ({
            deliverableKey: d.deliverableKey || '',
            deliverableLabel: d.deliverableLabel || '',
            techStack: d.techStack || this.form.techStack,
            scopeSummary: d.scopeSummary || ''
          }))
          this.outlineMsg = '已生成交付物骨架，请核对后进入下一步'
          this.outlineOk = true
          this.outlineAutoDone = true
        } else {
          this.outlineMsg = (resp.data && resp.data.message) || '拆分失败'
          this.outlineOk = false
        }
      } catch (e) {
        this.outlineMsg = (e.response && e.response.data && e.response.data.message) || '请求失败'
        this.outlineOk = false
      } finally {
        this.outlineParsing = false
      }
    },

    addRow () {
      this.rows.push({
        deliverableKey: 'part_' + (this.rows.length + 1),
        deliverableLabel: '新交付物',
        techStack: this.form.techStack || 'vue_node',
        scopeSummary: ''
      })
    },
    removeRow (idx) {
      if (this.rows.length <= 2) return
      this.rows.splice(idx, 1)
    },

    // ---- Step 4: 进入编辑页 ----
    goEditor () {
      const name = (this.form.name || '').trim()
      if (!name || !this.canContinue) return
      const modules = []
      let sort = 0
      const demand = (this.confirmedSummary || this.demandText || '').trim()
      let prdExtra = ''
      for (const r of this.rows) {
        const dk = (r.deliverableKey || '').trim().toLowerCase()
        if (!dk) continue
        const label = (r.deliverableLabel || '').trim() || dk
        const ts = r.techStack || this.form.techStack
        const scope = (r.scopeSummary || '').trim()
        if (scope) {
          prdExtra += `【${label}】${scope}\n`
        }
        modules.push({
          name: scope ? `${label}（${scope}）` : `${label} 范围待填`,
          sortOrder: sort++,
          deliverableKey: dk,
          deliverableLabel: label,
          techStack: ts,
          items: [{ name: '', complexity: 'standard', quantity: 1, excludedFromScale: false }]
        })
      }
      const prdSummary = [demand, prdExtra ? '\n—— 分交付物说明 ——\n' + prdExtra : ''].join('').trim()
      const payload = {
        form: {
          name,
          projectType: this.form.projectType,
          techStack: this.form.techStack,
          quoteKind: 'solution',
          prdSummary
        },
        modules,
        aiRequirementText: demand,
        // 标记来自向导，进入编辑页后自动触发 AI 解析功能清单
        autoAiParseModules: true
      }
      try {
        sessionStorage.setItem('quote_solution_payload', JSON.stringify(payload))
      } catch (e) {
        void e
      }
      // 跳转到已创建的项目编辑页（非新建页）
      this.$router.push({ path: '/quote/' + this.wizardProjectId, query: { fromWizard: '1' } })
    }
  },
  watch: {
    wizardProjectId (val) {
      if (val) {
        this.fetchAgentSessions()
      }
    }
  },
  created () {
    if (!this.rows.length) {
      this.rows = [
        { deliverableKey: 'web', deliverableLabel: 'Web 管理端', techStack: 'vue_node', scopeSummary: '' },
        { deliverableKey: 'api', deliverableLabel: '后端服务', techStack: 'react_java', scopeSummary: '' }
      ]
    }
  }
}
</script>

<style scoped>
.quote-wizard-page {
  max-width: 960px;
  margin: 0 auto;
  padding: var(--space-xl);
  color: var(--text-primary);
  background: var(--bg-page);
  min-height: 100%;
  box-sizing: border-box;
}
.back-link { display: inline-block; margin-bottom: 12px; color: var(--text-secondary); text-decoration: none; }
.page-head h1 { margin: 0 0 8px; font-size: var(--font-size-xxl); }
.lead { color: var(--text-secondary); line-height: 1.6; margin-bottom: 24px; }
.card {
  background: var(--bg-card, #fff);
  border: 1px solid var(--border-color, #e5e7eb);
  border-radius: 12px;
  padding: 20px;
  margin-bottom: 20px;
}
.card h2 { margin: 0; font-size: 18px; }
.card-step-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 16px;
}
.step-num {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: var(--color-primary, #2563eb);
  color: #fff;
  font-size: 14px;
  font-weight: 600;
  flex-shrink: 0;
}
.grid { display: flex; flex-wrap: wrap; gap: 12px; margin-bottom: 12px; }
.grid label { display: flex; flex-direction: column; gap: 4px; font-size: 13px; }
.grid .full { flex: 1 1 100%; }
.inp.wide { min-width: 240px; }
.textarea { width: 100%; box-sizing: border-box; padding: 10px; border-radius: 8px; border: 1px solid var(--border-color, #ddd); font-family: inherit; }
.actions-row { display: flex; gap: 12px; flex-wrap: wrap; align-items: center; }
.btn { padding: 8px 16px; border-radius: 8px; border: none; cursor: pointer; font-size: 14px; }
.btn.primary { background: var(--color-primary, #2563eb); color: #fff; }
.btn.secondary { background: var(--bg-muted, #f3f4f6); color: var(--text-primary); border: 1px solid var(--border-color); }
.btn:disabled { opacity: 0.6; cursor: not-allowed; }
.btn-sm { padding: 4px 10px; font-size: 12px; border-radius: 6px; border: 1px solid #ddd; background: #fff; cursor: pointer; }
.btn-sm.danger { color: #b91c1c; border-color: #fecaca; }
.ok { color: #059669; margin-top: 8px; }
.err { color: #dc2626; margin-top: 8px; }
.hint { color: var(--text-secondary); font-size: 13px; margin: 8px 0 0; }
.block { display: block; margin-bottom: 6px; font-size: 14px; }
.data-table { width: 100%; border-collapse: collapse; margin-top: 12px; font-size: 13px; }
.data-table th, .data-table td { border: 1px solid #e5e7eb; padding: 8px; text-align: left; vertical-align: top; }
.data-table th { background: #f9fafb; }
.inp.narrow { width: 100px; }
.actions-footer { text-align: center; }
.btn.large { padding: 12px 28px; font-size: 16px; }
/* Agent 会话信息 */
.agent-session-info {
  margin-top: 16px;
  padding: 16px;
  background: var(--bg-muted, #f9fafb);
  border-radius: 8px;
  border: 1px solid var(--border-color, #e5e7eb);
}
.agent-info-row {
  display: flex;
  gap: 12px;
  align-items: center;
  margin-bottom: 8px;
  font-size: 13px;
}
.agent-link {
  color: var(--color-primary, #2563eb);
  word-break: break-all;
}
.agent-badge {
  display: inline-block;
  padding: 2px 8px;
  border-radius: 10px;
  font-size: 12px;
  font-weight: 500;
}
.agent-badge--active {
  background: #dcfce7;
  color: #059669;
}
.agent-badge--ended {
  background: #f3f4f6;
  color: #6b7280;
}
</style>
