<template>
  <div class="quote-project-page">
    <div class="page-head">
      <div class="head-links">
        <router-link to="/quote" class="back-link">← {{ $t('quote.backList') }}</router-link>
        <span class="head-sep" aria-hidden="true">|</span>
        <router-link to="/quote/baseline-price" class="head-link-inline">{{ $t('quote.baselinePriceNav') }}</router-link>
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
        <h2>{{ $t('quote.presetAndRiskTitle') }}</h2>
        <p class="section-hint">{{ $t('quote.presetAndRiskHint') }}</p>

        <h3 class="sub-h3">{{ $t('quote.riskConfigTitle') }}</h3>
        <p class="section-hint small">{{ $t('quote.riskConfigHint') }}</p>
        <div class="table-wrap">
          <table class="cfg-table">
            <thead>
              <tr>
                <th>{{ $t('quote.riskKeyCol') }}</th>
                <th>{{ $t('quote.riskLabelCol') }}</th>
                <th>{{ $t('quote.riskPctCol') }}</th>
                <th>{{ $t('quote.enabledCol') }}</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="r in riskConfigs" :key="r.id">
                <td><code class="rk">{{ r.riskKey }}</code></td>
                <td><input v-model="r.label" class="inp compact" /></td>
                <td><input v-model.number="r.defaultPct" type="number" step="0.01" class="inp pct-inp" /></td>
                <td class="cen"><input type="checkbox" v-model="r.enabled" /></td>
              </tr>
            </tbody>
          </table>
        </div>
        <button type="button" class="btn secondary" :disabled="riskSaving" @click="saveRiskConfig">{{ riskSaving ? '…' : $t('quote.saveRiskConfig') }}</button>
        <span v-if="riskSaveMsg" :class="riskSaveOk ? 'ok' : 'err'">{{ riskSaveMsg }}</span>

        <h3 class="sub-h3">{{ $t('quote.presetLibTitle') }}</h3>
        <p class="section-hint small">{{ $t('quote.presetLibHint') }}</p>
        <div class="table-wrap">
          <table class="cfg-table">
            <thead>
              <tr>
                <th>{{ $t('quote.presetNameCol') }}</th>
                <th>{{ $t('quote.complexity') }}</th>
                <th>{{ $t('quote.presetCategoryCol') }}</th>
                <th>{{ $t('quote.presetSortCol') }}</th>
                <th>{{ $t('quote.enabledCol') }}</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="p in presetItemsAll" :key="p.id">
                <td><input v-model="p.name" class="inp compact" /></td>
                <td>
                  <select v-model="p.complexity" class="inp narrow">
                    <option value="simple">简单</option>
                    <option value="standard">标准</option>
                    <option value="medium">中等</option>
                    <option value="complex">复杂</option>
                    <option value="extreme">极复杂</option>
                  </select>
                </td>
                <td><input v-model="p.category" class="inp compact" placeholder="—" /></td>
                <td><input v-model.number="p.sortOrder" type="number" class="inp pct-inp" /></td>
                <td class="cen"><input type="checkbox" v-model="p.enabled" /></td>
                <td class="row-actions">
                  <button type="button" class="btn-sm" @click="savePresetRow(p)">{{ $t('quote.savePresetRow') }}</button>
                  <button type="button" class="btn-sm danger" @click="deletePresetRow(p)">{{ $t('quote.deletePresetRow') }}</button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
        <div class="new-preset-grid">
          <input v-model="newPreset.name" class="inp" :placeholder="$t('quote.newPresetNamePh')" />
          <select v-model="newPreset.complexity" class="inp narrow">
            <option value="simple">简单</option>
            <option value="standard">标准</option>
            <option value="medium">中等</option>
            <option value="complex">复杂</option>
            <option value="extreme">极复杂</option>
          </select>
          <input v-model="newPreset.category" class="inp" :placeholder="$t('quote.newPresetCatPh')" />
          <input v-model.number="newPreset.sortOrder" type="number" class="inp pct-inp" :placeholder="$t('quote.presetSortCol')" />
          <label class="chk inline"><input type="checkbox" v-model="newPreset.enabled" /> {{ $t('quote.enabledCol') }}</label>
          <button type="button" class="btn secondary" :disabled="presetAdding" @click="addPresetRow">{{ presetAdding ? '…' : $t('quote.addPresetBtn') }}</button>
        </div>
        <span v-if="presetLibMsg" :class="presetLibOk ? 'ok' : 'err'">{{ presetLibMsg }}</span>
      </section>

      <section class="card">
        <h2>{{ $t('quote.modules') }}</h2>
        <div v-for="(mod, mi) in modules" :key="'m-' + mi" class="module-block">
          <div class="mod-head">
            <input v-model="mod.name" class="inp mod-name" placeholder="模块名称" />
            <select class="inp preset-select" @change="applyPresetToModule(mi, $event)">
              <option value="">{{ $t('quote.addFromPreset') }}</option>
              <option v-for="p in presetPickerList" :key="'ps-' + p.id" :value="p.id">{{ formatPresetOption(p) }}</option>
            </select>
            <button type="button" class="btn-sm" @click="addItem(mi)">{{ $t('quote.addItem') }}</button>
            <button type="button" class="btn-sm danger" @click="removeModule(mi)">删模块</button>
          </div>
          <div v-for="(it, ii) in mod.items" :key="'i-' + mi + '-' + ii" class="item-row">
            <input v-model="it.name" class="inp" :placeholder="$t('quote.itemName')" />
            <select v-model="it.complexity" class="inp narrow">
              <option value="simple">简单</option>
              <option value="standard">标准</option>
              <option value="medium">中等</option>
              <option value="complex">复杂</option>
              <option value="extreme">极复杂</option>
            </select>
            <input v-model.number="it.quantity" type="number" min="1" class="inp qty" />
            <button type="button" class="btn-sm danger" @click="removeItem(mi, ii)">×</button>
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
      presetItemsAll: [],
      riskSaving: false,
      riskSaveMsg: '',
      riskSaveOk: false,
      presetAdding: false,
      presetLibMsg: '',
      presetLibOk: false,
      newPreset: { name: '', complexity: 'standard', category: '', sortOrder: 0, enabled: true }
    }
  },
  computed: {
    riskConfigsForCalculator () {
      return (this.riskConfigs || []).filter(r => r.riskKey !== 'urgency_rush' && r.enabled)
    },
    urgencyRiskMeta () {
      return (this.riskConfigs || []).find(r => r.riskKey === 'urgency_rush')
    },
    presetPickerList () {
      return (this.presetItemsAll || []).filter(p => p.enabled)
    }
  },
  created () {
    this.init()
  },
  watch: {
    '$route.params.id' () {
      this.init()
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
    applyPresetPayload (arr) {
      this.presetItemsAll = (arr || []).map(p => ({
        id: p.id,
        name: p.name,
        complexity: p.complexity || 'standard',
        category: p.category || '',
        sortOrder: p.sortOrder != null ? p.sortOrder : 0,
        enabled: p.enabled === true || p.enabled === 1
      }))
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
    applyPresetToModule (mi, ev) {
      const v = ev.target.value
      ev.target.value = ''
      if (!v) return
      const p = this.presetPickerList.find(x => String(x.id) === String(v))
      if (!p) return
      this.modules[mi].items.push({
        name: p.name,
        complexity: p.complexity || 'standard',
        quantity: 1
      })
    },
    async saveRiskConfig () {
      this.riskSaving = true
      this.riskSaveMsg = ''
      try {
        const items = this.riskConfigs.map(r => ({
          id: r.id,
          label: r.label,
          defaultPct: r.defaultPct,
          enabled: r.enabled
        }))
        const resp = await this.$http.put('/admin/quote/risk-config', { items })
        if (resp.data && resp.data.code === 0) {
          this.riskSaveOk = true
          this.riskSaveMsg = this.$t('quote.saveRiskOk')
          const r1 = await this.$http.get('/admin/quote/risk-config')
          if (r1.data && r1.data.code === 0) this.applyRiskConfigPayload(r1.data.data)
        } else {
          this.riskSaveOk = false
          this.riskSaveMsg = (resp.data && resp.data.message) || this.$t('quote.saveRiskFail')
        }
      } catch (e) {
        this.riskSaveOk = false
        this.riskSaveMsg = (e.response && e.response.data && e.response.data.message) || this.$t('quote.saveRiskFail')
      } finally {
        this.riskSaving = false
      }
    },
    async savePresetRow (p) {
      this.presetLibMsg = ''
      try {
        const body = {
          name: p.name,
          complexity: p.complexity,
          category: p.category || null,
          sortOrder: p.sortOrder != null ? p.sortOrder : 0,
          enabled: p.enabled
        }
        const resp = await this.$http.put('/admin/quote/preset-items/' + p.id, body)
        if (resp.data && resp.data.code === 0) {
          this.presetLibOk = true
          this.presetLibMsg = this.$t('quote.savePresetOk')
        } else {
          this.presetLibOk = false
          this.presetLibMsg = (resp.data && resp.data.message) || this.$t('quote.savePresetFail')
        }
      } catch (e) {
        this.presetLibOk = false
        this.presetLibMsg = (e.response && e.response.data && e.response.data.message) || this.$t('quote.savePresetFail')
      }
    },
    async deletePresetRow (p) {
      if (!confirm(this.$t('quote.confirmDeletePreset'))) return
      this.presetLibMsg = ''
      try {
        const resp = await this.$http.delete('/admin/quote/preset-items/' + p.id)
        if (resp.data && resp.data.code === 0) {
          this.presetLibOk = true
          this.presetLibMsg = this.$t('quote.deletePresetOk')
          const r3 = await this.$http.get('/admin/quote/preset-items', { params: { all: true } })
          if (r3.data && r3.data.code === 0) this.applyPresetPayload(r3.data.data)
        } else {
          this.presetLibOk = false
          this.presetLibMsg = (resp.data && resp.data.message) || this.$t('quote.savePresetFail')
        }
      } catch (e) {
        this.presetLibOk = false
        this.presetLibMsg = (e.response && e.response.data && e.response.data.message) || this.$t('quote.savePresetFail')
      }
    },
    async addPresetRow () {
      if (!this.newPreset.name || !String(this.newPreset.name).trim()) {
        this.presetLibOk = false
        this.presetLibMsg = this.$t('quote.newPresetNameRequired')
        return
      }
      this.presetAdding = true
      this.presetLibMsg = ''
      try {
        const body = {
          name: String(this.newPreset.name).trim(),
          complexity: this.newPreset.complexity || 'standard',
          category: this.newPreset.category ? String(this.newPreset.category).trim() : null,
          sortOrder: this.newPreset.sortOrder != null ? this.newPreset.sortOrder : 0,
          enabled: this.newPreset.enabled !== false
        }
        const resp = await this.$http.post('/admin/quote/preset-items', body)
        if (resp.data && resp.data.code === 0) {
          this.presetLibOk = true
          this.presetLibMsg = this.$t('quote.addPresetOk')
          this.newPreset = { name: '', complexity: 'standard', category: '', sortOrder: 0, enabled: true }
          const r3 = await this.$http.get('/admin/quote/preset-items', { params: { all: true } })
          if (r3.data && r3.data.code === 0) this.applyPresetPayload(r3.data.data)
        } else {
          this.presetLibOk = false
          this.presetLibMsg = (resp.data && resp.data.message) || this.$t('quote.savePresetFail')
        }
      } catch (e) {
        this.presetLibOk = false
        this.presetLibMsg = (e.response && e.response.data && e.response.data.message) || this.$t('quote.savePresetFail')
      } finally {
        this.presetAdding = false
      }
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
          this.$http.get('/admin/quote/preset-items', { params: { all: true } })
        ])
        if (r1.data && r1.data.code === 0) this.applyRiskConfigPayload(r1.data.data)
        if (r2.data && r2.data.code === 0) {
          this.priceConfigs = r2.data.data || []
          if (this.priceConfigs.length && !this.priceConfigId) this.priceConfigId = this.priceConfigs[0].id
        }
        if (r3.data && r3.data.code === 0) this.applyPresetPayload(r3.data.data)
        if (this.isNew) {
          this.projectId = null
          this.form = { ...this.form, name: '', prdSummary: '' }
          this.modules = [emptyModule()]
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
        if (d.latestResult) {
          this.calcResult = { ...d.latestResult, riskHints: [], confidenceLevel: '' }
        }
        await this.loadContractIfAny()
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
        })).filter(m => m.name && String(m.name).trim() && m.items.length)
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
.quote-project-page { max-width: 960px; margin: 0 auto; padding: 16px; }
.head-links { display: flex; flex-wrap: wrap; align-items: center; gap: 8px 12px; margin-bottom: 4px; }
.head-sep { color: #d1d5db; font-size: 13px; user-select: none; }
.head-link-inline { font-size: 13px; color: #2563eb; text-decoration: none; }
.head-link-inline:hover { text-decoration: underline; }
.back-link { font-size: 13px; color: #2563eb; text-decoration: none; }
.back-link:hover { text-decoration: underline; }
.page-head h1 { margin: 8px 0 16px; font-size: 22px; }
.card { background: #fff; border: 1px solid #e5e7eb; border-radius: 8px; padding: 16px; margin-bottom: 16px; }
.card h2 { margin: 0 0 12px; font-size: 16px; }
.card h3 { margin: 16px 0 8px; font-size: 14px; }
.grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(220px, 1fr)); gap: 12px; margin-bottom: 12px; }
label { display: flex; flex-direction: column; gap: 4px; font-size: 13px; color: #374151; }
label.block { display: block; margin-top: 8px; }
.inp { padding: 6px 10px; border: 1px solid #e5e7eb; border-radius: 6px; font-size: 14px; }
.inp.narrow { max-width: 140px; }
.inp.qty { max-width: 72px; }
.textarea { width: 100%; box-sizing: border-box; padding: 8px; border: 1px solid #e5e7eb; border-radius: 6px; font-size: 14px; }
.contract-ta { font-family: ui-monospace, monospace; font-size: 13px; }
.module-block { border: 1px dashed #e5e7eb; padding: 12px; margin-bottom: 12px; border-radius: 6px; }
.mod-head { display: flex; flex-wrap: wrap; gap: 8px; align-items: center; margin-bottom: 8px; }
.mod-name { flex: 1; min-width: 160px; }
.item-row { display: flex; flex-wrap: wrap; gap: 8px; align-items: center; margin-bottom: 6px; }
.btn { padding: 8px 16px; border-radius: 6px; border: none; cursor: pointer; font-size: 14px; margin-right: 8px; margin-top: 8px; }
.btn.primary { background: #2563eb; color: #fff; }
.btn.secondary { background: #f3f4f6; color: #111; }
.btn-sm { padding: 4px 10px; font-size: 12px; border-radius: 4px; border: 1px solid #e5e7eb; background: #fff; cursor: pointer; }
.btn-sm.danger { color: #b91c1c; }
.actions { margin-bottom: 16px; }
.risk-grid { display: flex; flex-wrap: wrap; gap: 12px; margin: 12px 0; }
.chk { flex-direction: row; align-items: center; }
.result-box { margin-top: 16px; padding: 12px; background: #f0fdf4; border-radius: 6px; font-size: 14px; }
.btn-row { margin-top: 8px; }
.export-row { display: flex; flex-wrap: wrap; gap: 8px; align-items: center; }
.export-row .btn { margin-top: 0; }
.ok { color: #059669; font-size: 13px; }
.err { color: #dc2626; font-size: 13px; }
.placeholder { padding: 24px; color: #6b7280; }
.section-hint { color: #6b7280; font-size: 13px; margin: 0 0 12px; }
.section-hint.small { font-size: 12px; margin-bottom: 8px; }
.sub-h3 { margin: 20px 0 8px; font-size: 14px; color: #374151; }
.sub-h3:first-of-type { margin-top: 0; }
.table-wrap { overflow-x: auto; margin-bottom: 12px; }
.cfg-table { width: 100%; border-collapse: collapse; font-size: 13px; }
.cfg-table th, .cfg-table td { border: 1px solid #e5e7eb; padding: 8px; text-align: left; vertical-align: middle; }
.cfg-table th { background: #f9fafb; }
.cfg-table .cen { text-align: center; }
.cfg-table .inp.compact { width: 100%; min-width: 120px; box-sizing: border-box; }
.cfg-table .pct-inp { width: 72px; }
code.rk { font-size: 12px; background: #f3f4f6; padding: 2px 6px; border-radius: 4px; }
.row-actions { white-space: nowrap; }
.row-actions .btn-sm { margin-right: 4px; }
.new-preset-grid { display: flex; flex-wrap: wrap; gap: 8px; align-items: center; margin-top: 12px; }
.chk.inline { flex-direction: row; align-items: center; }
.preset-select { min-width: 180px; max-width: 280px; font-size: 13px; }
</style>
