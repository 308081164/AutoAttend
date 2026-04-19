<template>
  <div class="quote-wizard-page">
    <div class="page-head">
      <router-link to="/quote" class="back-link">← 报价项目</router-link>
      <h1>解决方案级报价 · 交付物规划</h1>
      <p class="lead">
        先确认<strong>多个交付物</strong>及其<strong>计价技术栈</strong>，再进入功能清单页用 AI 填充模块与功能点。代码仓库仍与单体报价相同，仅创建<strong>一个</strong>共用仓库。
      </p>
    </div>

    <div class="card">
      <h2>1. 项目概况</h2>
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
      <label class="block">需求与范围说明（将参与 AI 拆分与后续填功能）</label>
      <textarea v-model="demandText" class="textarea" rows="10" placeholder="描述整体系统：含哪些端、与谁集成、关键业务等。" />
    </div>

    <div class="card">
      <h2>2. 交付物拆分</h2>
      <p class="hint">可先点「AI 智能拆分」，再手工增删改键名、显示名、技术栈与范围摘要。</p>
      <div class="actions-row">
        <button type="button" class="btn primary" :disabled="outlineParsing" @click="runOutlineAi">
          {{ outlineParsing ? '分析中…' : 'AI 智能拆分交付物' }}
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

    <div class="card actions-footer">
      <button type="button" class="btn primary large" :disabled="!canContinue" @click="goEditor">进入功能清单与报价</button>
      <p v-if="!canContinue" class="hint">请填写项目名称、至少 2 个有效交付物（键唯一）。</p>
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
      demandText: '',
      rows: [],
      outlineParsing: false,
      outlineMsg: '',
      outlineOk: false
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
    }
  },
  methods: {
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
    async runOutlineAi () {
      const text = (this.demandText || '').trim()
      if (!text) {
        this.outlineMsg = '请先填写需求与范围说明'
        this.outlineOk = false
        return
      }
      this.outlineParsing = true
      this.outlineMsg = ''
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
    goEditor () {
      const name = (this.form.name || '').trim()
      if (!name || !this.canContinue) return
      const modules = []
      let sort = 0
      const demand = (this.demandText || '').trim()
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
        aiRequirementText: demand
      }
      try {
        sessionStorage.setItem('quote_solution_payload', JSON.stringify(payload))
      } catch (e) {
        void e
      }
      this.$router.push({ path: '/quote/new', query: { mode: 'solution', fromWizard: '1' } })
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
.card h2 { margin: 0 0 16px; font-size: 18px; }
.grid { display: flex; flex-wrap: wrap; gap: 12px; margin-bottom: 12px; }
.grid label { display: flex; flex-direction: column; gap: 4px; font-size: 13px; }
.grid .full { flex: 1 1 100%; }
.inp.wide { min-width: 240px; }
.textarea { width: 100%; box-sizing: border-box; padding: 10px; border-radius: 8px; border: 1px solid var(--border-color, #ddd); font-family: inherit; }
.actions-row { display: flex; gap: 12px; flex-wrap: wrap; margin-bottom: 12px; }
.btn { padding: 8px 16px; border-radius: 8px; border: none; cursor: pointer; font-size: 14px; }
.btn.primary { background: var(--color-primary, #2563eb); color: #fff; }
.btn.secondary { background: var(--bg-muted, #f3f4f6); color: var(--text-primary); border: 1px solid var(--border-color); }
.btn:disabled { opacity: 0.6; cursor: not-allowed; }
.btn-sm { padding: 4px 10px; font-size: 12px; border-radius: 6px; border: 1px solid #ddd; background: #fff; cursor: pointer; }
.btn-sm.danger { color: #b91c1c; border-color: #fecaca; }
.ok { color: #059669; margin-top: 8px; }
.err { color: #dc2626; margin-top: 8px; }
.data-table { width: 100%; border-collapse: collapse; margin-top: 12px; font-size: 13px; }
.data-table th, .data-table td { border: 1px solid #e5e7eb; padding: 8px; text-align: left; vertical-align: top; }
.data-table th { background: #f9fafb; }
.inp.narrow { width: 100px; }
.actions-footer { text-align: center; }
.btn.large { padding: 12px 28px; font-size: 16px; }
.hint { color: var(--text-secondary); font-size: 13px; margin: 8px 0 0; }
.block { display: block; margin-bottom: 6px; font-size: 14px; }
</style>
