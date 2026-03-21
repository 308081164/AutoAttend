<template>
  <div class="quote-project-page">
    <div class="page-head">
      <div class="head-links">
        <router-link to="/quote" class="back-link">← {{ $t('quote.backList') }}</router-link>
        <span class="head-sep" aria-hidden="true">|</span>
        <router-link to="/quote/config" class="head-link-inline">{{ $t('quote.quoteConfigNav') }}</router-link>
      </div>
      <h1>{{ isNew ? $t('quote.newProject') : $t('quote.editTitle') }}</h1>
    </div>

    <div v-if="pageLoading" class="placeholder">{{ $t('quote.loading') }}</div>
    <template v-else>
      <section class="card">
        <h2>项目基础</h2>
        <div class="grid">
          <label>{{ $t('quote.name') }} <input v-model="form.name" class="inp" /></label>
          <label>{{ $t('quote.projectType') }}
            <select v-model="form.projectType" class="inp">
              <option value="website">企业官网</option>
              <option value="ecommerce_miniprogram">电商小程序</option>
              <option value="admin">管理后台</option>
              <option value="app">APP</option>
              <option value="other">其他</option>
            </select>
          </label>
          <label>{{ $t('quote.techStack') }}
            <select v-model="form.techStack" class="inp">
              <option value="vue_node">Vue+Node</option>
              <option value="react_java">React+Java</option>
              <option value="miniprogram">小程序原生</option>
              <option value="flutter">Flutter</option>
              <option value="other">其他</option>
            </select>
          </label>
          <label>{{ $t('quote.designType') }}
            <select v-model="form.designType" class="inp">
              <option value="has_design">已有设计稿</option>
              <option value="need_design">需 UI 设计</option>
              <option value="template">套用模板</option>
            </select>
          </label>
          <label>{{ $t('quote.dataMigration') }}
            <select v-model="form.dataMigration" class="inp">
              <option value="none">无</option>
              <option value="migrate">需从旧系统迁移</option>
              <option value="third_party">需第三方接口对接</option>
            </select>
          </label>
          <label>{{ $t('quote.concurrency') }}
            <select v-model="form.concurrency" class="inp">
              <option value="lt100">&lt;100</option>
              <option value="r100_1000">100–1000</option>
              <option value="r1000_10000">1000–10000</option>
              <option value="gt10000">10000+</option>
            </select>
          </label>
          <label>{{ $t('quote.securityLevel') }}
            <select v-model="form.securityLevel" class="inp">
              <option value="normal">普通</option>
              <option value="financial">金融级</option>
              <option value="government">政府级</option>
            </select>
          </label>
          <label>{{ $t('quote.deployType') }}
            <select v-model="form.deployType" class="inp">
              <option value="single">单机</option>
              <option value="cloud">云服务器</option>
              <option value="k8s">容器化/K8s</option>
            </select>
          </label>
        </div>
        <label class="block">{{ $t('quote.prdSummary') }}</label>
        <textarea v-model="form.prdSummary" class="textarea" rows="4" placeholder="PRD 或需求摘要，有助于提高置信度"></textarea>
      </section>

      <section class="card">
        <h2>{{ $t('quote.modules') }}</h2>
        <div v-for="(mod, mi) in modules" :key="'m-' + mi" class="module-block">
          <div class="mod-head">
            <input v-model="mod.name" class="inp mod-name" placeholder="模块名称" />
            <select class="inp preset-select" @change="applyPresetToModule(mi, $event)">
              <option value="">{{ $t('quote.addFromPreset') }}</option>
              <option v-for="p in presetItems" :key="'ps-' + p.id" :value="p.id">{{ formatPresetOption(p) }}</option>
            </select>
            <button type="button" class="btn-sm" @click="addItem(mi)">{{ $t('quote.addItem') }}</button>
            <button type="button" class="btn-sm danger" @click="removeModule(mi)">删模块</button>
          </div>
          <div class="item-table-head" role="row">
            <div class="item-col item-col-name th">{{ $t('quote.itemName') }}</div>
            <div class="item-col item-col-complexity th">
              {{ $t('quote.complexity') }}
              <span class="th-sub">{{ $t('quote.complexityHint') }}</span>
            </div>
            <div class="item-col item-col-qty th">
              {{ $t('quote.quantity') }}
              <span class="th-sub">{{ $t('quote.quantityHint') }}</span>
            </div>
            <div class="item-col item-col-actions th" aria-hidden="true"></div>
          </div>
          <div v-for="(it, ii) in mod.items" :key="'i-' + mi + '-' + ii" class="item-row">
            <input v-model="it.name" class="inp item-col-name" :placeholder="$t('quote.itemName')" />
            <select v-model="it.complexity" class="inp narrow item-col-complexity">
              <option value="simple">简单</option>
              <option value="standard">标准</option>
              <option value="medium">中等</option>
              <option value="complex">复杂</option>
              <option value="extreme">极复杂</option>
            </select>
            <input v-model.number="it.quantity" type="number" min="1" class="inp qty item-col-qty" />
            <div class="item-col-actions">
              <button type="button" class="btn-sm danger" @click="removeItem(mi, ii)">×</button>
            </div>
          </div>
        </div>
        <button type="button" class="btn secondary" @click="addModule">{{ $t('quote.addModule') }}</button>
      </section>

      <div class="actions">
        <button type="button" class="btn primary" :disabled="saving" @click="saveProject">{{ saving ? '…' : $t('quote.save') }}</button>
        <span v-if="saveMsg" :class="saveOk ? 'ok' : 'err'">{{ saveMsg }}</span>
      </div>

      <section v-if="projectId" class="card">
        <h2>报价计算</h2>
        <div class="risk-grid">
          <label v-for="r in riskConfigsForCalculator" :key="r.riskKey" class="chk">
            <input type="checkbox" :value="r.riskKey" v-model="selectedRisks" />
            {{ r.label }} ({{ formatRiskPct(r.defaultPct) }})
          </label>
          <label v-if="urgencyRiskMeta && urgencyRiskMeta.enabled" class="chk">
            <input type="checkbox" v-model="urgencyRush" />
            {{ urgencyRiskMeta.label }} ({{ formatRiskPct(urgencyRiskMeta.defaultPct) }})
          </label>
        </div>
        <label>{{ $t('quote.priceTier') }}
          <select v-model.number="priceConfigId" class="inp">
            <option v-for="p in priceConfigs" :key="p.id" :value="p.id">{{ p.regionLabel }} — ¥{{ p.pricePerDay }}/天</option>
          </select>
        </label>
        <h3>{{ $t('quote.auditTitle') }}</h3>
        <div class="risk-grid">
          <label class="chk"><input type="checkbox" v-model="audit.changeLimit" /> 限制需求变更次数</label>
          <label class="chk"><input type="checkbox" v-model="audit.acceptanceClear" /> 验收标准明确</label>
          <label class="chk"><input type="checkbox" v-model="audit.paymentNodes" /> 付款节点约定</label>
          <label class="chk"><input type="checkbox" v-model="audit.deployFee" /> 含部署上线费用</label>
          <label class="chk"><input type="checkbox" v-model="audit.maintenanceScope" /> 维护期及范围明确</label>
        </div>
        <button type="button" class="btn primary" :disabled="calculating" @click="runCalculate">{{ $t('quote.calculate') }}</button>

        <div v-if="calcResult" class="result-box">
          <h3>{{ $t('quote.resultTitle') }}</h3>
          <p>{{ $t('quote.totalDays') }}：{{ calcResult.totalDays }} &nbsp; {{ $t('quote.baseAmount') }}：¥{{ calcResult.baseAmount }}</p>
          <p>{{ $t('quote.riskPct') }}：{{ calcResult.riskPctTotal }} &nbsp; 风险金额：¥{{ calcResult.riskAmount }} &nbsp; {{ $t('quote.finalAmount') }}：<strong>¥{{ calcResult.finalAmount }}</strong></p>
          <p>{{ $t('quote.confidence') }}：{{ calcResult.confidenceScore }}（{{ calcResult.confidenceLevel }}）</p>
          <ul v-if="calcResult.riskHints && calcResult.riskHints.length"><li v-for="(h, i) in calcResult.riskHints" :key="i">{{ h }}</li></ul>
          <div class="btn-row export-row">
            <button type="button" class="btn secondary" @click="downloadQuote">{{ $t('quote.genQuote') }}</button>
            <button type="button" class="btn secondary" @click="downloadQuotePdf">{{ $t('quote.genQuotePdf') }}</button>
            <button type="button" class="btn secondary" @click="downloadQuoteDocx">{{ $t('quote.genQuoteWord') }}</button>
          </div>
        </div>
      </section>

      <section v-if="calcResult && calcResult.id" class="card">
        <h2>{{ $t('quote.contractTitle') }}</h2>
        <div class="grid">
          <label>{{ $t('quote.clientName') }} <input v-model="contract.clientName" class="inp" /></label>
          <label>{{ $t('quote.companyName') }} <input v-model="contract.companyName" class="inp" /></label>
          <label>{{ $t('quote.templateType') }}
            <select v-model="contract.templateType" class="inp">
              <option value="software_dev">软件开发合同</option>
              <option value="maintenance">维护服务合同</option>
            </select>
          </label>
        </div>
        <button type="button" class="btn primary" :disabled="contractGenLoading" @click="runGenContract">{{ $t('quote.genContract') }}</button>
        <label class="block">合同正文（可编辑）</label>
        <textarea v-model="contract.editedContent" class="textarea contract-ta" rows="16"></textarea>
        <div class="btn-row export-row">
          <button type="button" class="btn secondary" @click="saveContractBody">{{ $t('quote.saveContract') }}</button>
          <button type="button" class="btn secondary" @click="exportContractFile">{{ $t('quote.exportContract') }}</button>
          <button type="button" class="btn secondary" @click="exportContractPdf">{{ $t('quote.exportContractPdf') }}</button>
          <button type="button" class="btn secondary" @click="exportContractDocx">{{ $t('quote.exportContractWord') }}</button>
        </div>
        <p v-if="contractMsg" :class="contractOk ? 'ok' : 'err'">{{ contractMsg }}</p>
      </section>
    </template>
  </div>
</template>

<script>
function emptyModule () {
  return { name: '', sortOrder: 0, items: [{ name: '', complexity: 'standard', quantity: 1 }] }
}

export default {
  name: 'QuoteProjectView',
  data () {
    return {
      pageLoading: true,
      isNew: false,
      projectId: null,
      form: {
        name: '',
        projectType: 'website',
        techStack: 'vue_node',
        designType: 'need_design',
        dataMigration: 'none',
        concurrency: 'lt100',
        securityLevel: 'normal',
        deployType: 'cloud',
        status: 'draft',
        linkTableId: null,
        prdSummary: ''
      },
      modules: [emptyModule()],
      riskConfigs: [],
      priceConfigs: [],
      selectedRisks: [],
      urgencyRush: false,
      priceConfigId: null,
      audit: {
        changeLimit: false,
        acceptanceClear: false,
        paymentNodes: false,
        deployFee: false,
        maintenanceScope: false
      },
      calcResult: null,
      calculating: false,
      saving: false,
      saveMsg: '',
      saveOk: false,
      contract: { clientName: '', companyName: '', templateType: 'software_dev', editedContent: '' },
      contractGenLoading: false,
      contractMsg: '',
      contractOk: false,
      /** 仅启用项，供「从预设添加」下拉（在「报价配置」页维护） */
      presetItems: [],
      restoringCalcPrefs: false,
      calcPrefsDebounceTimer: null
    }
  },
  computed: {
    /** 用于侦听报价偏好变化并防抖落库 */
    calcPrefsSnapshot () {
      return JSON.stringify({
        rk: this.selectedRisks,
        ur: this.urgencyRush,
        pc: this.priceConfigId,
        au: this.audit
      })
    },
    riskConfigsForCalculator () {
      return (this.riskConfigs || []).filter(r => r.riskKey !== 'urgency_rush' && r.enabled)
    },
    urgencyRiskMeta () {
      return (this.riskConfigs || []).find(r => r.riskKey === 'urgency_rush')
    }
  },
  created () {
    this.init()
  },
  beforeDestroy () {
    if (this.calcPrefsDebounceTimer) clearTimeout(this.calcPrefsDebounceTimer)
  },
  watch: {
    '$route.params.id' () {
      this.init()
    },
    calcPrefsSnapshot () {
      if (this.pageLoading || this.restoringCalcPrefs) return
      this.scheduleQuoteCalcPrefsSave()
    }
  },
  methods: {
    applyRiskConfigPayload (arr) {
      this.riskConfigs = (arr || []).map(r => ({
        id: r.id,
        riskKey: r.riskKey,
        label: r.label,
        defaultPct: Number(r.defaultPct),
        enabled: r.enabled === true || r.enabled === 1
      }))
    },
    applyPresetPickerPayload (arr) {
      this.presetItems = (arr || []).map(p => ({
        id: p.id,
        name: p.name,
        complexity: p.complexity || 'standard',
        category: p.category || '',
        enabled: p.enabled === true || p.enabled === 1
      })).filter(p => p.enabled)
    },
    formatRiskPct (n) {
      const x = Number(n)
      if (Number.isNaN(x)) return '0%'
      if (x >= 0) return '+' + x + '%'
      return x + '%'
    },
    formatPresetOption (p) {
      const c = p.category ? p.category + ' · ' : ''
      return c + p.name
    },
    resetCalcPrefsUi () {
      this.selectedRisks = []
      this.urgencyRush = false
      this.audit = {
        changeLimit: false,
        acceptanceClear: false,
        paymentNodes: false,
        deployFee: false,
        maintenanceScope: false
      }
      if (this.priceConfigs.length) this.priceConfigId = this.priceConfigs[0].id
      else this.priceConfigId = null
    },
    buildQuoteCalcPrefsPayload () {
      return {
        riskKeys: Array.isArray(this.selectedRisks) ? [...this.selectedRisks] : [],
        urgencyRush: !!this.urgencyRush,
        priceConfigId: this.priceConfigId,
        auditChecklist: { ...this.audit }
      }
    },
    applyQuoteCalcPrefs (prefs) {
      if (!prefs || typeof prefs !== 'object') return
      if (Array.isArray(prefs.riskKeys)) {
        this.selectedRisks = prefs.riskKeys.filter(k => typeof k === 'string')
      }
      if (typeof prefs.urgencyRush === 'boolean') this.urgencyRush = prefs.urgencyRush
      const pid = prefs.priceConfigId
      if (pid != null && this.priceConfigs.some(p => Number(p.id) === Number(pid))) {
        this.priceConfigId = Number(pid)
      }
      const ac = prefs.auditChecklist
      if (ac && typeof ac === 'object') {
        const keys = ['changeLimit', 'acceptanceClear', 'paymentNodes', 'deployFee', 'maintenanceScope']
        keys.forEach(k => {
          if (Object.prototype.hasOwnProperty.call(ac, k)) this.audit[k] = !!ac[k]
        })
      }
    },
    scheduleQuoteCalcPrefsSave () {
      if (this.calcPrefsDebounceTimer) clearTimeout(this.calcPrefsDebounceTimer)
      this.calcPrefsDebounceTimer = setTimeout(() => this.flushQuoteCalcPrefs(), 650)
    },
    async flushQuoteCalcPrefs () {
      if (this.pageLoading || this.restoringCalcPrefs || !this.projectId) return
      const body = this.buildQuoteCalcPrefsPayload()
      try {
        await this.$http.patch('/admin/quote/projects/' + this.projectId + '/calc-prefs', body)
      } catch (e) { /* 静默失败，避免打断编辑 */ }
    },
    applyPresetToModule (mi, ev) {
      const v = ev.target.value
      ev.target.value = ''
      if (!v) return
      const p = this.presetItems.find(x => String(x.id) === String(v))
      if (!p) return
      this.modules[mi].items.push({
        name: p.name,
        complexity: p.complexity || 'standard',
        quantity: 1
      })
    },
    async init () {
      const raw = this.$route.params.id
      this.isNew = raw === 'new'
      this.pageLoading = true
      this.calcResult = null
      this.saveMsg = ''
      try {
        const [r1, r2, r3] = await Promise.all([
          this.$http.get('/admin/quote/risk-config'),
          this.$http.get('/admin/quote/price-config'),
          this.$http.get('/admin/quote/preset-items')
        ])
        if (r1.data && r1.data.code === 0) this.applyRiskConfigPayload(r1.data.data)
        if (r2.data && r2.data.code === 0) {
          this.priceConfigs = r2.data.data || []
          if (this.priceConfigs.length && !this.priceConfigId) this.priceConfigId = this.priceConfigs[0].id
        }
        if (r3.data && r3.data.code === 0) this.applyPresetPickerPayload(r3.data.data)
        else this.presetItems = []
        if (this.isNew) {
          this.projectId = null
          this.form = { ...this.form, name: '', prdSummary: '' }
          this.modules = [emptyModule()]
          this.resetCalcPrefsUi()
        } else {
          this.projectId = Number(raw)
          await this.loadProject(this.projectId)
        }
      } catch (e) {
        if (e.response && e.response.status === 401) this.$router.push({ name: 'login' })
      } finally {
        this.pageLoading = false
      }
    },
    async loadProject (id) {
      const resp = await this.$http.get('/admin/quote/projects/' + id)
      if (resp.data && resp.data.code === 0 && resp.data.data) {
        const d = resp.data.data
        this.restoringCalcPrefs = true
        try {
          this.form.name = d.name || ''
          this.form.projectType = d.projectType || 'other'
          this.form.techStack = d.techStack || 'vue_node'
          this.form.designType = d.designType || 'need_design'
          this.form.dataMigration = d.dataMigration || 'none'
          this.form.concurrency = d.concurrency || 'lt100'
          this.form.securityLevel = d.securityLevel || 'normal'
          this.form.deployType = d.deployType || 'cloud'
          this.form.status = d.status || 'draft'
          this.form.linkTableId = d.linkTableId
          this.form.prdSummary = d.prdSummary || ''
          const mods = d.modules || []
          if (!mods.length) this.modules = [emptyModule()]
          else {
            this.modules = mods.map((m, idx) => ({
              name: m.name,
              sortOrder: m.sortOrder != null ? m.sortOrder : idx,
              items: (m.items || []).map(it => ({
                name: it.name,
                complexity: it.complexity || 'standard',
                quantity: it.quantity || 1
              }))
            }))
          }
          if (d.quoteCalcPrefs) {
            this.applyQuoteCalcPrefs(d.quoteCalcPrefs)
          }
          if (d.latestResult) {
            this.calcResult = { ...d.latestResult, riskHints: [], confidenceLevel: '' }
          }
          await this.loadContractIfAny()
        } finally {
          this.$nextTick(() => { this.restoringCalcPrefs = false })
        }
      }
    },
    async loadContractIfAny () {
      if (!this.calcResult || !this.calcResult.id) return
      try {
        const resp = await this.$http.get('/admin/quote/results/' + this.calcResult.id + '/contract')
        if (resp.data && resp.data.code === 0 && resp.data.data) {
          const c = resp.data.data
          this.contract.clientName = c.clientName || ''
          this.contract.companyName = c.companyName || ''
          this.contract.templateType = c.templateType || 'software_dev'
          this.contract.editedContent = c.editedContent || c.aiRawResponse || ''
        }
      } catch (e) { /* 404 ok */ }
    },
    payload () {
      return {
        name: this.form.name,
        projectType: this.form.projectType,
        techStack: this.form.techStack,
        designType: this.form.designType,
        dataMigration: this.form.dataMigration,
        concurrency: this.form.concurrency,
        securityLevel: this.form.securityLevel,
        deployType: this.form.deployType,
        status: this.form.status,
        linkTableId: this.form.linkTableId,
        prdSummary: this.form.prdSummary,
        modules: this.modules.map((m, mi) => ({
          name: m.name,
          sortOrder: m.sortOrder != null ? m.sortOrder : mi,
          items: (m.items || []).filter(it => it.name && String(it.name).trim()).map(it => ({
            name: String(it.name).trim(),
            complexity: it.complexity || 'standard',
            quantity: Math.max(1, Number(it.quantity) || 1)
          }))
        })).filter(m => m.name && String(m.name).trim() && m.items.length),
        quoteCalcPrefs: this.buildQuoteCalcPrefsPayload()
      }
    },
    async saveProject () {
      this.saving = true
      this.saveMsg = ''
      try {
        const body = this.payload()
        if (this.isNew) {
          const resp = await this.$http.post('/admin/quote/projects', body)
          if (resp.data && resp.data.code === 0 && resp.data.data && resp.data.data.id) {
            this.saveOk = true
            this.saveMsg = '已创建'
            const newId = resp.data.data.id
            this.projectId = newId
            this.isNew = false
            await this.$router.replace({ name: 'quote-project', params: { id: String(newId) } })
          } else {
            this.saveOk = false
            this.saveMsg = (resp.data && resp.data.message) || '失败'
          }
        } else {
          const resp = await this.$http.put('/admin/quote/projects/' + this.projectId, body)
          if (resp.data && resp.data.code === 0) {
            this.saveOk = true
            this.saveMsg = '已保存'
          } else {
            this.saveOk = false
            this.saveMsg = (resp.data && resp.data.message) || '失败'
          }
        }
      } catch (e) {
        this.saveOk = false
        this.saveMsg = (e.response && e.response.data && e.response.data.message) || '网络错误'
      } finally {
        this.saving = false
      }
    },
    addModule () {
      this.modules.push(emptyModule())
    },
    removeModule (mi) {
      if (this.modules.length <= 1) return
      this.modules.splice(mi, 1)
    },
    addItem (mi) {
      this.modules[mi].items.push({ name: '', complexity: 'standard', quantity: 1 })
    },
    removeItem (mi, ii) {
      if (this.modules[mi].items.length <= 1) return
      this.modules[mi].items.splice(ii, 1)
    },
    async runCalculate () {
      let pid = this.projectId || (this.$route.params.id !== 'new' ? Number(this.$route.params.id) : null)
      if (!pid) {
        await this.saveProject()
        pid = this.projectId || (this.$route.params.id !== 'new' ? Number(this.$route.params.id) : null)
      }
      if (!pid) {
        alert('请先填写项目名称并保存')
        return
      }
      this.projectId = pid
      this.calculating = true
      try {
        const body = {
          riskKeys: this.selectedRisks,
          urgencyRush: this.urgencyRush,
          priceConfigId: this.priceConfigId,
          auditChecklist: { ...this.audit }
        }
        const resp = await this.$http.post('/admin/quote/projects/' + pid + '/calculate', body)
        if (resp.data && resp.data.code === 0 && resp.data.data) {
          this.calcResult = resp.data.data
          await this.loadContractIfAny()
        } else {
          alert((resp.data && resp.data.message) || '计算失败')
        }
      } catch (e) {
        alert((e.response && e.response.data && e.response.data.message) || '计算失败')
      } finally {
        this.calculating = false
      }
    },
    async downloadQuote () {
      if (!this.projectId || !this.calcResult) return
      try {
        const resp = await this.$http.post('/admin/quote/projects/' + this.projectId + '/quote-doc', {
          quoteResultId: this.calcResult.id
        })
        if (resp.data && resp.data.code === 0 && resp.data.data && resp.data.data.html) {
          const blob = new Blob([resp.data.data.html], { type: 'text/html;charset=utf-8' })
          const a = document.createElement('a')
          a.href = URL.createObjectURL(blob)
          a.download = resp.data.data.filename || 'quote.html'
          a.click()
          URL.revokeObjectURL(a.href)
        }
      } catch (e) {
        alert('生成失败')
      }
    },
    async downloadBinaryGet (path, defaultFilename) {
      try {
        const resp = await this.$http.get(path, {
          params: { quoteResultId: this.calcResult.id },
          responseType: 'blob'
        })
        const blob = resp.data
        const a = document.createElement('a')
        a.href = URL.createObjectURL(blob)
        a.download = defaultFilename
        a.click()
        URL.revokeObjectURL(a.href)
      } catch (e) {
        alert((e.response && e.response.data && e.response.data.message) || this.$t('quote.exportFail'))
      }
    },
    async downloadQuotePdf () {
      if (!this.projectId || !this.calcResult) return
      await this.downloadBinaryGet(
        '/admin/quote/projects/' + this.projectId + '/quote-doc.pdf',
        'quote-' + this.projectId + '.pdf'
      )
    },
    async downloadQuoteDocx () {
      if (!this.projectId || !this.calcResult) return
      await this.downloadBinaryGet(
        '/admin/quote/projects/' + this.projectId + '/quote-doc.docx',
        'quote-' + this.projectId + '.docx'
      )
    },
    async runGenContract () {
      if (!this.calcResult || !this.calcResult.id) return
      this.contractGenLoading = true
      this.contractMsg = ''
      try {
        const resp = await this.$http.post('/admin/quote/results/' + this.calcResult.id + '/contract/generate', {
          clientName: this.contract.clientName,
          companyName: this.contract.companyName,
          templateType: this.contract.templateType
        })
        if (resp.data && resp.data.code === 0 && resp.data.data) {
          this.contract.editedContent = resp.data.data.editedContent || ''
          this.contractOk = true
          this.contractMsg = '已生成'
        } else {
          this.contractOk = false
          this.contractMsg = (resp.data && resp.data.message) || '失败'
        }
      } catch (e) {
        this.contractOk = false
        this.contractMsg = (e.response && e.response.data && e.response.data.message) || '失败'
      } finally {
        this.contractGenLoading = false
      }
    },
    async saveContractBody () {
      if (!this.calcResult || !this.calcResult.id) return
      this.contractMsg = ''
      try {
        const resp = await this.$http.put('/admin/quote/results/' + this.calcResult.id + '/contract', {
          editedContent: this.contract.editedContent
        })
        if (resp.data && resp.data.code === 0) {
          this.contractOk = true
          this.contractMsg = '已保存'
        } else {
          this.contractOk = false
          this.contractMsg = (resp.data && resp.data.message) || '失败'
        }
      } catch (e) {
        this.contractOk = false
        this.contractMsg = '失败'
      }
    },
    async exportContractFile () {
      if (!this.calcResult || !this.calcResult.id) return
      try {
        const resp = await this.$http.post('/admin/quote/results/' + this.calcResult.id + '/contract/export')
        if (resp.data && resp.data.code === 0 && resp.data.data && resp.data.data.html) {
          const blob = new Blob([resp.data.data.html], { type: 'text/html;charset=utf-8' })
          const a = document.createElement('a')
          a.href = URL.createObjectURL(blob)
          a.download = resp.data.data.filename || 'contract.html'
          a.click()
          URL.revokeObjectURL(a.href)
        }
      } catch (e) {
        alert('导出失败')
      }
    },
    async exportContractPdf () {
      if (!this.calcResult || !this.calcResult.id) return
      try {
        const resp = await this.$http.get('/admin/quote/results/' + this.calcResult.id + '/contract.pdf', {
          responseType: 'blob'
        })
        const blob = resp.data
        const a = document.createElement('a')
        a.href = URL.createObjectURL(blob)
        a.download = 'contract-' + this.calcResult.id + '.pdf'
        a.click()
        URL.revokeObjectURL(a.href)
      } catch (e) {
        alert((e.response && e.response.data && e.response.data.message) || this.$t('quote.exportFail'))
      }
    },
    async exportContractDocx () {
      if (!this.calcResult || !this.calcResult.id) return
      try {
        const resp = await this.$http.get('/admin/quote/results/' + this.calcResult.id + '/contract.docx', {
          responseType: 'blob'
        })
        const blob = resp.data
        const a = document.createElement('a')
        a.href = URL.createObjectURL(blob)
        a.download = 'contract-' + this.calcResult.id + '.docx'
        a.click()
        URL.revokeObjectURL(a.href)
      } catch (e) {
        alert((e.response && e.response.data && e.response.data.message) || this.$t('quote.exportFail'))
      }
    }
  }
}
</script>

<style scoped>
/* 全宽占满可视区域（仅左右留少量边距），避免窄栏居中 */
.quote-project-page {
  width: 100%;
  max-width: none;
  margin: 0;
  padding: clamp(16px, 2vw, 28px) clamp(16px, 3vw, 40px);
  box-sizing: border-box;
  color: #0f172a;
  background: #f1f5f9;
}
.head-links { display: flex; flex-wrap: wrap; align-items: center; gap: 10px 14px; margin-bottom: 6px; }
.head-sep { color: #64748b; font-size: 15px; user-select: none; }
.head-link-inline { font-size: 15px; color: #1d4ed8; text-decoration: none; font-weight: 500; }
.head-link-inline:hover { text-decoration: underline; color: #1e40af; }
.back-link { font-size: 15px; color: #1d4ed8; text-decoration: none; font-weight: 500; }
.back-link:hover { text-decoration: underline; color: #1e40af; }
.page-head h1 { margin: 10px 0 20px; font-size: clamp(1.5rem, 2vw, 1.85rem); font-weight: 700; color: #020617; letter-spacing: -0.02em; }
.card {
  background: #fff;
  border: 1px solid #cbd5e1;
  border-radius: 10px;
  padding: clamp(18px, 2vw, 28px);
  margin-bottom: 20px;
  box-shadow: 0 1px 2px rgba(15, 23, 42, 0.06);
}
.card h2 { margin: 0 0 14px; font-size: 1.125rem; font-weight: 700; color: #020617; }
.card h3 { margin: 18px 0 10px; font-size: 1rem; font-weight: 600; color: #0f172a; }
/* 宽屏下多列铺开，提高横向利用率 */
.grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(min(100%, 260px), 1fr)); gap: 14px 18px; margin-bottom: 14px; }
label { display: flex; flex-direction: column; gap: 6px; font-size: 15px; color: #1e293b; font-weight: 500; }
label.block { display: block; margin-top: 10px; }
.inp {
  padding: 8px 12px;
  border: 1px solid #94a3b8;
  border-radius: 8px;
  font-size: 16px;
  line-height: 1.4;
  color: #0f172a;
  background: #fff;
}
.inp:focus,
.textarea:focus {
  outline: 2px solid #3b82f6;
  outline-offset: 1px;
  border-color: #2563eb;
}
.inp.narrow { max-width: 160px; }
.inp.qty { max-width: 88px; }
.textarea {
  width: 100%;
  box-sizing: border-box;
  padding: 12px;
  border: 1px solid #94a3b8;
  border-radius: 8px;
  font-size: 16px;
  line-height: 1.5;
  min-height: 6.5em;
  color: #0f172a;
  background: #fff;
}
.contract-ta { font-family: ui-monospace, monospace; font-size: 14px; }
/* 功能模块：区块与表头对比度加强，减轻视疲劳 */
.module-block {
  border: 1px solid #94a3b8;
  background: #f8fafc;
  padding: 16px;
  margin-bottom: 14px;
  border-radius: 8px;
}
.mod-head { display: flex; flex-wrap: wrap; gap: 10px; align-items: center; margin-bottom: 10px; }
.mod-name { flex: 1; min-width: 200px; }
.item-table-head,
.item-row {
  display: grid;
  grid-template-columns: minmax(140px, 1fr) minmax(160px, 200px) 88px 44px;
  gap: 10px;
  align-items: start;
  margin-bottom: 8px;
}
.item-table-head {
  margin-top: 6px;
  padding: 8px 4px 10px;
  border-bottom: 2px solid #64748b;
  background: linear-gradient(to bottom, #f1f5f9, #fff);
  border-radius: 6px 6px 0 0;
}
.th { font-size: 13px; font-weight: 700; color: #020617; line-height: 1.25; }
.th-sub { display: block; font-size: 11px; font-weight: 500; color: #475569; margin-top: 4px; line-height: 1.35; }
.item-row { padding: 2px 0; }
.item-col-actions { display: flex; align-items: flex-start; justify-content: center; padding-top: 2px; }
.item-row .inp.narrow { max-width: none; width: 100%; }
.item-row .inp { min-width: 0; }
.btn { padding: 10px 18px; border-radius: 8px; border: none; cursor: pointer; font-size: 16px; margin-right: 8px; margin-top: 8px; }
.btn.primary { background: #1d4ed8; color: #fff; }
.btn.primary:hover { background: #1e40af; }
.btn.secondary {
  background: #e2e8f0;
  color: #0f172a;
  border: 1px solid #94a3b8;
  font-weight: 500;
}
.btn.secondary:hover { background: #cbd5e1; }
.btn-sm {
  padding: 6px 12px;
  font-size: 14px;
  border-radius: 6px;
  border: 1px solid #64748b;
  background: #fff;
  color: #1e293b;
  cursor: pointer;
  font-weight: 500;
}
.btn-sm:hover { background: #f1f5f9; border-color: #475569; }
.btn-sm.danger { color: #991b1b; border-color: #fca5a5; background: #fff; }
.btn-sm.danger:hover { background: #fef2f2; border-color: #f87171; }
.actions { margin-bottom: 18px; }
.risk-grid { display: flex; flex-wrap: wrap; gap: 14px 18px; margin: 14px 0; font-size: 15px; color: #1e293b; }
.chk { flex-direction: row; align-items: center; font-weight: 500; }
.result-box {
  margin-top: 18px;
  padding: 16px;
  background: #ecfdf5;
  border: 1px solid #6ee7b7;
  border-radius: 8px;
  font-size: 16px;
  line-height: 1.55;
  color: #064e3b;
}
.result-box h3 { color: #022c22; }
.result-box strong { color: #022c22; }
.btn-row { margin-top: 10px; }
.export-row { display: flex; flex-wrap: wrap; gap: 10px; align-items: center; }
.export-row .btn { margin-top: 0; }
.ok { color: #047857; font-size: 15px; font-weight: 600; }
.err { color: #b91c1c; font-size: 15px; font-weight: 600; }
.placeholder { padding: 28px; color: #475569; font-size: 16px; font-weight: 500; }
.preset-select { min-width: 200px; max-width: min(100%, 400px); flex: 1; font-size: 15px; }
</style>
