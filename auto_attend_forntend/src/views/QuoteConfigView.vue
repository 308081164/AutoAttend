<template>
  <div class="quote-config-page">
    <div class="page-head">
      <h1>{{ $t('quote.quoteConfigTitle') }}</h1>
      <p class="desc">{{ $t('quote.quoteConfigDesc') }}</p>
    </div>

    <div v-if="loading" class="placeholder">{{ $t('quote.loading') }}</div>
    <template v-else>
      <!-- 风险系数 -->
      <section class="card">
        <h2>{{ $t('quote.riskConfigTitle') }}</h2>
        <p class="hint">{{ $t('quote.riskConfigHint') }}</p>
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
      </section>

      <!-- 合同乙方（我方）主体模板：法人/组织 + 自然人 -->
      <section class="card">
        <h2>{{ $t('quote.partyBSectionTitle') }}</h2>
        <p class="hint">{{ $t('quote.partyBSectionHint') }}</p>

        <div class="party-b-subsection">
          <h3 class="party-b-subtitle">{{ $t('quote.partyBLegalSubtitle') }}</h3>
          <div class="party-b-grid">
            <label>{{ $t('quote.partyBLegalName') }} <input v-model="partyB.legalName" class="inp wide" /></label>
            <label>{{ $t('quote.partyBCreditCode') }} <input v-model="partyB.creditCode" class="inp wide" /></label>
            <label class="full">{{ $t('quote.partyBAddress') }} <input v-model="partyB.address" class="inp wide" /></label>
            <label>{{ $t('quote.partyBContactName') }} <input v-model="partyB.contactName" class="inp" /></label>
            <label>{{ $t('quote.partyBContactPhone') }} <input v-model="partyB.contactPhone" class="inp" /></label>
            <label>{{ $t('quote.partyBBankName') }} <input v-model="partyB.bankName" class="inp wide" /></label>
            <label>{{ $t('quote.partyBBankAccount') }} <input v-model="partyB.bankAccount" class="inp wide" /></label>
          </div>
          <button type="button" class="btn secondary" :disabled="partyBSaving" @click="savePartyB">{{ partyBSaving ? '…' : $t('quote.partyBSave') }}</button>
          <span v-if="partyBMsg" :class="partyBOk ? 'ok' : 'err'">{{ partyBMsg }}</span>
        </div>

        <div class="party-b-subsection party-b-subsection--natural">
          <h3 class="party-b-subtitle">{{ $t('quote.partyBNaturalSectionTitle') }}</h3>
          <p class="hint">{{ $t('quote.partyBNaturalSectionHint') }}</p>
          <div class="party-b-grid">
            <label>{{ $t('quote.partyBNaturalFullName') }} <input v-model="partyBNatural.fullName" class="inp wide" /></label>
            <label>{{ $t('quote.partyBNaturalIdNumber') }} <input v-model="partyBNatural.idNumber" class="inp wide" /></label>
            <label class="full">{{ $t('quote.partyBNaturalAddress') }} <input v-model="partyBNatural.address" class="inp wide" /></label>
            <label>{{ $t('quote.partyBNaturalContactPhone') }} <input v-model="partyBNatural.contactPhone" class="inp" /></label>
            <label>{{ $t('quote.partyBNaturalEmail') }} <input v-model="partyBNatural.email" class="inp wide" type="email" autocomplete="off" /></label>
            <label>{{ $t('quote.partyBBankName') }} <input v-model="partyBNatural.bankName" class="inp wide" /></label>
            <label>{{ $t('quote.partyBBankAccount') }} <input v-model="partyBNatural.bankAccount" class="inp wide" /></label>
          </div>
          <button type="button" class="btn secondary" :disabled="partyBNaturalSaving" @click="savePartyBNatural">{{ partyBNaturalSaving ? '…' : $t('quote.partyBNaturalSave') }}</button>
          <span v-if="partyBNaturalMsg" :class="partyBNaturalOk ? 'ok' : 'err'">{{ partyBNaturalMsg }}</span>
        </div>
      </section>

      <!-- 预设功能点 -->
      <section class="card">
        <h2>{{ $t('quote.presetLibTitle') }}</h2>
        <p class="hint">{{ $t('quote.presetLibHintConfig') }}</p>
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

      <!-- 人天基准 -->
      <section class="card">
        <h2>{{ $t('quote.baselineSection') }}</h2>
        <p class="hint">{{ $t('quote.baselineHint') }}</p>
        <div class="table-wrap">
          <table class="data-table">
            <thead>
              <tr>
                <th>ID</th>
                <th>{{ $t('quote.techStack') }}</th>
                <th>{{ $t('quote.complexity') }}</th>
                <th>{{ $t('quote.baselineDaysCol') }}</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="b in baselines" :key="b.id">
                <td>{{ b.id }}</td>
                <td>
                  <select v-model="b.techStack" class="inp">
                    <option v-for="o in techOptionsForRow(b)" :key="o.v" :value="o.v">{{ o.l }}</option>
                  </select>
                </td>
                <td>
                  <select v-model="b.complexity" class="inp">
                    <option v-for="o in complexityOptionsForRow(b)" :key="o.v" :value="o.v">{{ o.l }}</option>
                  </select>
                </td>
                <td><input v-model.number="b.days" type="number" step="0.01" min="0.01" class="inp num" /></td>
                <td class="actions">
                  <button type="button" class="btn-sm" @click="saveBaseline(b)">{{ $t('quote.saveRow') }}</button>
                  <button type="button" class="btn-sm danger" @click="removeBaseline(b)">{{ $t('quote.deletePresetRow') }}</button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
        <div class="add-row">
          <select v-model="newBaseline.techStack" class="inp">
            <option v-for="o in techOptions" :key="'n-' + o.v" :value="o.v">{{ o.l }}</option>
          </select>
          <select v-model="newBaseline.complexity" class="inp">
            <option v-for="o in complexityOptions" :key="'nc-' + o.v" :value="o.v">{{ o.l }}</option>
          </select>
          <input v-model.number="newBaseline.days" type="number" step="0.01" min="0.01" class="inp num" :placeholder="$t('quote.baselineDaysCol')" />
          <button type="button" class="primary-button" :disabled="baselineAdding" @click="addBaseline">{{ baselineAdding ? '…' : $t('quote.addBaselineBtn') }}</button>
        </div>
        <p v-if="baselineMsg" :class="baselineOk ? 'ok' : 'err'">{{ baselineMsg }}</p>
      </section>

      <!-- 人天单价 -->
      <section class="card">
        <h2>{{ $t('quote.priceSection') }}</h2>
        <p class="hint">{{ $t('quote.priceHint') }}</p>
        <div class="table-wrap">
          <table class="data-table">
            <thead>
              <tr>
                <th>ID</th>
                <th>{{ $t('quote.regionLabelCol') }}</th>
                <th>{{ $t('quote.pricePerDayCol') }}</th>
                <th>{{ $t('quote.durationCoefficientCol') }}</th>
                <th>{{ $t('quote.currencyCol') }}</th>
                <th>{{ $t('quote.enabledCol') }}</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="p in prices" :key="p.id">
                <td>{{ p.id }}</td>
                <td><input v-model="p.regionLabel" class="inp wide" /></td>
                <td><input v-model.number="p.pricePerDay" type="number" step="0.01" min="1" class="inp num" /></td>
                <td><input v-model.number="p.durationCoefficient" type="number" step="0.0001" min="0.01" max="100" class="inp num narrow" :title="$t('quote.durationCoefficientHint')" /></td>
                <td><input v-model="p.currency" class="inp cur" maxlength="8" /></td>
                <td class="cen"><input type="checkbox" v-model="p.enabled" /></td>
                <td class="actions">
                  <button type="button" class="btn-sm" @click="savePrice(p)">{{ $t('quote.saveRow') }}</button>
                  <button type="button" class="btn-sm danger" @click="removePrice(p)">{{ $t('quote.deletePresetRow') }}</button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
        <div class="add-row">
          <input v-model="newPrice.regionLabel" class="inp wide" :placeholder="$t('quote.regionLabelCol')" />
          <input v-model.number="newPrice.pricePerDay" type="number" step="0.01" min="1" class="inp num" :placeholder="$t('quote.pricePerDayCol')" />
          <input v-model.number="newPrice.durationCoefficient" type="number" step="0.0001" min="0.01" max="100" class="inp num narrow" :placeholder="$t('quote.durationCoefficientCol')" :title="$t('quote.durationCoefficientHint')" />
          <input v-model="newPrice.currency" class="inp cur" maxlength="8" placeholder="CNY" />
          <label class="chk"><input type="checkbox" v-model="newPrice.enabled" /> {{ $t('quote.enabledCol') }}</label>
          <button type="button" class="primary-button" :disabled="priceAdding" @click="addPrice">{{ priceAdding ? '…' : $t('quote.addPriceBtn') }}</button>
        </div>
        <p v-if="priceMsg" :class="priceOk ? 'ok' : 'err'">{{ priceMsg }}</p>
      </section>
    </template>
  </div>
</template>

<script>
export default {
  name: 'QuoteConfigView',
  data () {
    return {
      loading: true,
      riskConfigs: [],
      riskSaving: false,
      riskSaveMsg: '',
      riskSaveOk: false,
      presetItemsAll: [],
      presetAdding: false,
      presetLibMsg: '',
      presetLibOk: false,
      newPreset: { name: '', complexity: 'standard', category: '', sortOrder: 0, enabled: true },
      baselines: [],
      prices: [],
      baselineMsg: '',
      baselineOk: false,
      priceMsg: '',
      priceOk: false,
      baselineAdding: false,
      priceAdding: false,
      partyB: {
        legalName: '',
        creditCode: '',
        address: '',
        contactName: '',
        contactPhone: '',
        bankName: '',
        bankAccount: ''
      },
      partyBNatural: {
        fullName: '',
        idNumber: '',
        address: '',
        contactPhone: '',
        email: '',
        bankName: '',
        bankAccount: ''
      },
      partyBSaving: false,
      partyBMsg: '',
      partyBOk: false,
      partyBNaturalSaving: false,
      partyBNaturalMsg: '',
      partyBNaturalOk: false,
      newBaseline: { techStack: 'vue_node', complexity: 'standard', days: 1.5 },
      newPrice: { regionLabel: '', pricePerDay: 1500, durationCoefficient: 1.2, currency: 'CNY', enabled: true },
      techOptions: [
        { v: 'vue_node', l: 'Vue+Node' },
        { v: 'react_java', l: 'React+Java' },
        { v: 'miniprogram', l: '小程序原生' },
        { v: 'flutter', l: 'Flutter' },
        { v: 'other', l: '其他' }
      ],
      complexityOptions: [
        { v: 'simple', l: '简单' },
        { v: 'standard', l: '标准' },
        { v: 'medium', l: '中等' },
        { v: 'complex', l: '复杂' },
        { v: 'extreme', l: '极复杂' }
      ]
    }
  },
  created () {
    this.load()
  },
  methods: {
    techOptionsForRow (b) {
      const opts = [...this.techOptions]
      if (b.techStack && !opts.some(o => o.v === b.techStack)) {
        opts.push({ v: b.techStack, l: b.techStack })
      }
      return opts
    },
    complexityOptionsForRow (b) {
      const opts = [...this.complexityOptions]
      if (b.complexity && !opts.some(o => o.v === b.complexity)) {
        opts.push({ v: b.complexity, l: b.complexity })
      }
      return opts
    },
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
    normBaseline (row) {
      return {
        id: row.id,
        techStack: row.techStack || 'vue_node',
        complexity: row.complexity || 'standard',
        days: row.days != null ? Number(row.days) : 0
      }
    },
    normPrice (row) {
      const dc = row.durationCoefficient != null ? Number(row.durationCoefficient) : 1.2
      return {
        id: row.id,
        regionLabel: row.regionLabel || '',
        pricePerDay: row.pricePerDay != null ? Number(row.pricePerDay) : 0,
        durationCoefficient: Number.isFinite(dc) && dc > 0 ? dc : 1.2,
        currency: (row.currency || 'CNY').toUpperCase(),
        enabled: row.enabled === true || row.enabled === 1
      }
    },
    partyBLegalPayload () {
      return {
        legalName: this.partyB.legalName,
        creditCode: this.partyB.creditCode,
        address: this.partyB.address,
        contactName: this.partyB.contactName,
        contactPhone: this.partyB.contactPhone,
        bankName: this.partyB.bankName,
        bankAccount: this.partyB.bankAccount
      }
    },
    async savePartyB () {
      this.partyBSaving = true
      this.partyBMsg = ''
      try {
        const resp = await this.$http.put('/admin/quote/party-b-profile', this.partyBLegalPayload())
        if (resp.data && resp.data.code === 0) {
          this.partyBOk = true
          this.partyBMsg = this.$t('quote.partyBSaveOk')
        } else {
          this.partyBOk = false
          this.partyBMsg = (resp.data && resp.data.message) || this.$t('quote.partyBSaveFail')
        }
      } catch (e) {
        this.partyBOk = false
        this.partyBMsg = (e.response && e.response.data && e.response.data.message) || this.$t('quote.partyBSaveFail')
      } finally {
        this.partyBSaving = false
      }
    },
    async savePartyBNatural () {
      this.partyBNaturalSaving = true
      this.partyBNaturalMsg = ''
      try {
        const resp = await this.$http.put('/admin/quote/party-b-profile', {
          naturalPerson: { ...this.partyBNatural }
        })
        if (resp.data && resp.data.code === 0) {
          this.partyBNaturalOk = true
          this.partyBNaturalMsg = this.$t('quote.partyBSaveOk')
        } else {
          this.partyBNaturalOk = false
          this.partyBNaturalMsg = (resp.data && resp.data.message) || this.$t('quote.partyBSaveFail')
        }
      } catch (e) {
        this.partyBNaturalOk = false
        this.partyBNaturalMsg = (e.response && e.response.data && e.response.data.message) || this.$t('quote.partyBSaveFail')
      } finally {
        this.partyBNaturalSaving = false
      }
    },
    async load () {
      this.loading = true
      try {
        const [r1, r2, rb, rp, rpb] = await Promise.all([
          this.$http.get('/admin/quote/risk-config'),
          this.$http.get('/admin/quote/preset-items', { params: { all: true } }),
          this.$http.get('/admin/quote/baselines'),
          this.$http.get('/admin/quote/price-config', { params: { all: true } }),
          this.$http.get('/admin/quote/party-b-profile')
        ])
        if (r1.data && r1.data.code === 0) this.applyRiskConfigPayload(r1.data.data)
        else this.riskConfigs = []
        if (r2.data && r2.data.code === 0) this.applyPresetPayload(r2.data.data)
        else this.presetItemsAll = []
        if (rb.data && rb.data.code === 0) {
          this.baselines = (rb.data.data || []).map(x => this.normBaseline(x))
        } else this.baselines = []
        if (rp.data && rp.data.code === 0) {
          this.prices = (rp.data.data || []).map(x => this.normPrice(x))
        } else this.prices = []
        if (rpb.data && rpb.data.code === 0 && rpb.data.data) {
          const d = rpb.data.data
          this.partyB = {
            legalName: d.legalName || '',
            creditCode: d.creditCode || '',
            address: d.address || '',
            contactName: d.contactName || '',
            contactPhone: d.contactPhone || '',
            bankName: d.bankName || '',
            bankAccount: d.bankAccount || ''
          }
          const np = d.naturalPerson && typeof d.naturalPerson === 'object' ? d.naturalPerson : {}
          this.partyBNatural = {
            fullName: np.fullName || '',
            idNumber: np.idNumber || '',
            address: np.address || '',
            contactPhone: np.contactPhone || '',
            email: np.email || '',
            bankName: np.bankName || '',
            bankAccount: np.bankAccount || ''
          }
        }
      } catch (e) {
        if (e.response && e.response.status === 401) this.$router.push({ name: 'login' })
        this.riskConfigs = []
        this.presetItemsAll = []
        this.baselines = []
        this.prices = []
      } finally {
        this.loading = false
      }
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
          const r2 = await this.$http.get('/admin/quote/preset-items', { params: { all: true } })
          if (r2.data && r2.data.code === 0) this.applyPresetPayload(r2.data.data)
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
          const r2 = await this.$http.get('/admin/quote/preset-items', { params: { all: true } })
          if (r2.data && r2.data.code === 0) this.applyPresetPayload(r2.data.data)
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
          const r2 = await this.$http.get('/admin/quote/preset-items', { params: { all: true } })
          if (r2.data && r2.data.code === 0) this.applyPresetPayload(r2.data.data)
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
    async saveBaseline (b) {
      this.baselineMsg = ''
      try {
        const resp = await this.$http.put('/admin/quote/baselines/' + b.id, {
          techStack: b.techStack,
          complexity: b.complexity,
          days: b.days
        })
        if (resp.data && resp.data.code === 0) {
          this.baselineOk = true
          this.baselineMsg = this.$t('quote.saveBaselineOk')
          await this.load()
        } else {
          this.baselineOk = false
          this.baselineMsg = (resp.data && resp.data.message) || this.$t('quote.saveBaselineFail')
        }
      } catch (e) {
        this.baselineOk = false
        this.baselineMsg = (e.response && e.response.data && e.response.data.message) || this.$t('quote.saveBaselineFail')
      }
    },
    async removeBaseline (b) {
      if (!confirm(this.$t('quote.confirmDeleteBaseline'))) return
      this.baselineMsg = ''
      try {
        const resp = await this.$http.delete('/admin/quote/baselines/' + b.id)
        if (resp.data && resp.data.code === 0) {
          this.baselineOk = true
          this.baselineMsg = this.$t('quote.deleteBaselineOk')
          await this.load()
        } else {
          this.baselineOk = false
          this.baselineMsg = (resp.data && resp.data.message) || this.$t('quote.saveBaselineFail')
        }
      } catch (e) {
        this.baselineOk = false
        this.baselineMsg = (e.response && e.response.data && e.response.data.message) || this.$t('quote.saveBaselineFail')
      }
    },
    async addBaseline () {
      this.baselineMsg = ''
      this.baselineAdding = true
      try {
        const resp = await this.$http.post('/admin/quote/baselines', {
          techStack: this.newBaseline.techStack,
          complexity: this.newBaseline.complexity,
          days: this.newBaseline.days
        })
        if (resp.data && resp.data.code === 0) {
          this.baselineOk = true
          this.baselineMsg = this.$t('quote.addBaselineOk')
          this.newBaseline = { techStack: 'vue_node', complexity: 'standard', days: 1.5 }
          await this.load()
        } else {
          this.baselineOk = false
          this.baselineMsg = (resp.data && resp.data.message) || this.$t('quote.saveBaselineFail')
        }
      } catch (e) {
        this.baselineOk = false
        this.baselineMsg = (e.response && e.response.data && e.response.data.message) || this.$t('quote.saveBaselineFail')
      } finally {
        this.baselineAdding = false
      }
    },
    async savePrice (p) {
      this.priceMsg = ''
      try {
        const resp = await this.$http.put('/admin/quote/price-config/' + p.id, {
          regionLabel: p.regionLabel,
          pricePerDay: p.pricePerDay,
          durationCoefficient: p.durationCoefficient != null ? p.durationCoefficient : 1.2,
          currency: p.currency || 'CNY',
          enabled: p.enabled
        })
        if (resp.data && resp.data.code === 0) {
          this.priceOk = true
          this.priceMsg = this.$t('quote.savePriceOk')
          await this.load()
        } else {
          this.priceOk = false
          this.priceMsg = (resp.data && resp.data.message) || this.$t('quote.savePriceFail')
        }
      } catch (e) {
        this.priceOk = false
        this.priceMsg = (e.response && e.response.data && e.response.data.message) || this.$t('quote.savePriceFail')
      }
    },
    async removePrice (p) {
      if (!confirm(this.$t('quote.confirmDeletePrice'))) return
      this.priceMsg = ''
      try {
        const resp = await this.$http.delete('/admin/quote/price-config/' + p.id)
        if (resp.data && resp.data.code === 0) {
          this.priceOk = true
          this.priceMsg = this.$t('quote.deletePriceOk')
          await this.load()
        } else {
          this.priceOk = false
          this.priceMsg = (resp.data && resp.data.message) || this.$t('quote.savePriceFail')
        }
      } catch (e) {
        this.priceOk = false
        this.priceMsg = (e.response && e.response.data && e.response.data.message) || this.$t('quote.savePriceFail')
      }
    },
    async addPrice () {
      this.priceMsg = ''
      if (!this.newPrice.regionLabel || !String(this.newPrice.regionLabel).trim()) {
        this.priceOk = false
        this.priceMsg = this.$t('quote.regionRequired')
        return
      }
      this.priceAdding = true
      try {
        const resp = await this.$http.post('/admin/quote/price-config', {
          regionLabel: String(this.newPrice.regionLabel).trim(),
          pricePerDay: this.newPrice.pricePerDay,
          durationCoefficient: this.newPrice.durationCoefficient != null ? this.newPrice.durationCoefficient : 1.2,
          currency: this.newPrice.currency || 'CNY',
          enabled: this.newPrice.enabled !== false
        })
        if (resp.data && resp.data.code === 0) {
          this.priceOk = true
          this.priceMsg = this.$t('quote.addPriceOk')
          this.newPrice = { regionLabel: '', pricePerDay: 1500, durationCoefficient: 1.2, currency: 'CNY', enabled: true }
          await this.load()
        } else {
          this.priceOk = false
          this.priceMsg = (resp.data && resp.data.message) || this.$t('quote.savePriceFail')
        }
      } catch (e) {
        this.priceOk = false
        this.priceMsg = (e.response && e.response.data && e.response.data.message) || this.$t('quote.savePriceFail')
      } finally {
        this.priceAdding = false
      }
    }
  }
}
</script>

<style scoped>
.quote-config-page {
  width: 100%;
  max-width: none;
  margin: 0;
  padding: clamp(var(--space-lg), 2vw, var(--space-xxl)) clamp(var(--space-lg), 3vw, 40px);
  box-sizing: border-box;
  color: var(--text-primary);
  background: var(--bg-page);
}
.page-head {
  margin-bottom: var(--space-xl);
}
.page-head h1 {
  margin: 0 0 var(--space-md);
  font-size: clamp(1.35rem, 2vw, 1.75rem);
  font-weight: var(--font-weight-bold);
  color: var(--text-primary);
}
.desc {
  color: var(--text-secondary);
  font-size: var(--font-size-md);
  margin: 0;
  line-height: var(--line-height-normal);
  font-weight: var(--font-weight-medium);
}
.card {
  background: var(--bg-card);
  border: 1px solid var(--border-primary);
  border-radius: var(--radius-lg);
  padding: clamp(var(--space-lg), 2vw, var(--space-xl));
  margin-bottom: var(--space-xl);
  box-shadow: var(--shadow-sm);
  transition: var(--transition-normal);
}
.card:hover {
  box-shadow: var(--shadow-md);
}
.card h2 {
  margin: 0 0 var(--space-md);
  font-size: var(--font-size-lg);
  font-weight: var(--font-weight-bold);
  color: var(--text-primary);
}
.hint {
  font-size: var(--font-size-base);
  color: var(--text-secondary);
  margin: 0 0 var(--space-md);
  line-height: var(--line-height-normal);
  font-weight: var(--font-weight-medium);
}
.table-wrap {
  overflow-x: auto;
  margin-bottom: var(--space-md);
}
.data-table {
  width: 100%;
  border-collapse: collapse;
  font-size: var(--font-size-md);
  background: var(--bg-card);
}
.data-table th,
.data-table td {
  padding: var(--space-sm) var(--space-md);
  text-align: left;
  vertical-align: middle;
  color: var(--text-primary);
  border-bottom: 1px solid var(--border-primary);
}
.data-table th {
  background: var(--bg-hover);
  font-weight: var(--font-weight-semibold);
  color: var(--text-primary);
  font-size: var(--font-size-sm);
}
.data-table tbody tr:hover {
  background: var(--bg-hover);
}
.cfg-table {
  width: 100%;
  border-collapse: collapse;
  font-size: var(--font-size-md);
  background: var(--bg-card);
}
.cfg-table th,
.cfg-table td {
  padding: var(--space-sm) var(--space-md);
  text-align: left;
  vertical-align: middle;
  color: var(--text-primary);
  border-bottom: 1px solid var(--border-primary);
}
.cfg-table th {
  background: var(--bg-hover);
  font-weight: var(--font-weight-semibold);
  color: var(--text-primary);
  font-size: var(--font-size-sm);
}
.cfg-table tbody tr:hover {
  background: var(--bg-hover);
}
.cfg-table .cen {
  text-align: center;
}
.cfg-table .inp.compact {
  width: 100%;
  min-width: 140px;
  box-sizing: border-box;
  font-size: var(--font-size-md);
}
.cfg-table .pct-inp {
  width: 88px;
  font-size: var(--font-size-md);
}
.cfg-table .inp.narrow {
  max-width: 160px;
}
code.rk {
  font-size: var(--font-size-sm);
  background: var(--bg-hover);
  color: var(--text-primary);
  padding: var(--space-xs) var(--space-sm);
  border-radius: var(--radius-xs);
  border: 1px solid var(--border-primary);
  font-family: var(--font-mono);
}
.row-actions {
  white-space: nowrap;
}
.row-actions .btn-sm {
  margin-right: var(--space-xs);
}
.new-preset-grid {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-sm);
  align-items: center;
  margin-top: var(--space-md);
}
.chk.inline {
  display: inline-flex;
  flex-direction: row;
  align-items: center;
  gap: var(--space-xs);
  font-size: var(--font-size-md);
  font-weight: var(--font-weight-medium);
  color: var(--text-primary);
}
.inp {
  padding: var(--space-sm) var(--space-md);
  border: 1px solid var(--border-input);
  border-radius: var(--radius-sm);
  font-size: var(--font-size-md);
  color: var(--text-primary);
  background: var(--bg-input);
  transition: var(--transition-fast);
}
.inp:hover {
  border-color: var(--border-input-hover);
}
.inp:focus {
  outline: none;
  border-color: var(--border-input-focus);
  box-shadow: 0 0 0 2px rgba(20, 86, 240, 0.15);
}
.inp.num {
  width: 96px;
}
.inp.wide {
  min-width: 160px;
  width: 100%;
  max-width: 300px;
}
.party-b-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
  gap: var(--space-md) var(--space-lg);
  margin-bottom: var(--space-md);
}
.party-b-grid label {
  display: flex;
  flex-direction: column;
  gap: var(--space-xs);
  font-size: var(--font-size-base);
  font-weight: var(--font-weight-semibold);
  color: var(--text-secondary);
}
.party-b-grid label.full {
  grid-column: 1 / -1;
}
.party-b-grid .inp.wide {
  max-width: none;
}
.party-b-subsection {
  margin-top: var(--space-xs);
}
.party-b-subsection--natural {
  margin-top: var(--space-xl);
  padding-top: var(--space-lg);
  border-top: 1px solid var(--border-primary);
}
.party-b-subtitle {
  margin: 0 0 var(--space-md);
  font-size: var(--font-size-lg);
  font-weight: var(--font-weight-bold);
  color: var(--text-primary);
}
.inp.cur {
  width: 72px;
  text-transform: uppercase;
}
.cen {
  text-align: center;
}
.actions {
  white-space: nowrap;
}
.btn-sm {
  padding: var(--space-xs) var(--space-md);
  font-size: var(--font-size-base);
  border-radius: var(--radius-xs);
  border: 1px solid var(--border-primary);
  background: var(--bg-card);
  color: var(--text-primary);
  cursor: pointer;
  margin-right: var(--space-xs);
  font-weight: var(--font-weight-medium);
  transition: var(--transition-fast);
}
.btn-sm:hover {
  background: var(--bg-hover);
  border-color: var(--border-input-hover);
}
.btn-sm.danger {
  color: var(--danger);
  border-color: var(--danger);
  background: var(--bg-card);
}
.btn-sm.danger:hover {
  background: var(--danger-bg);
}
.add-row {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-sm);
  align-items: center;
  margin-top: var(--space-md);
}
.primary-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: var(--space-sm) var(--space-lg);
  background: var(--brand-blue);
  color: #fff;
  border-radius: var(--radius-sm);
  border: none;
  cursor: pointer;
  font-size: var(--font-size-lg);
  font-weight: var(--font-weight-semibold);
  transition: var(--transition-fast);
  box-shadow: var(--shadow-btn);
}
.primary-button:hover {
  background: var(--brand-blue-hover);
}
.primary-button:disabled {
  opacity: 0.45;
  cursor: not-allowed;
}
.btn {
  padding: var(--space-sm) var(--space-lg);
  border-radius: var(--radius-sm);
  border: none;
  cursor: pointer;
  font-size: var(--font-size-lg);
  margin-right: var(--space-sm);
  margin-top: var(--space-sm);
  transition: var(--transition-fast);
}
.btn.secondary {
  background: var(--bg-card);
  color: var(--text-primary);
  border: 1px solid var(--border-primary);
  font-weight: var(--font-weight-medium);
}
.btn.secondary:hover {
  background: var(--bg-hover);
  border-color: var(--border-input-hover);
}
.btn.secondary:disabled {
  opacity: 0.45;
  cursor: not-allowed;
}
.chk {
  display: inline-flex;
  align-items: center;
  gap: var(--space-xs);
  font-size: var(--font-size-md);
  font-weight: var(--font-weight-medium);
  color: var(--text-primary);
}
.placeholder {
  padding: var(--space-xxl);
  color: var(--text-secondary);
  font-size: var(--font-size-lg);
  font-weight: var(--font-weight-medium);
  text-align: center;
}
.ok {
  color: var(--success);
  font-size: var(--font-size-md);
  font-weight: var(--font-weight-semibold);
}
.err {
  color: var(--danger);
  font-size: var(--font-size-md);
  font-weight: var(--font-weight-semibold);
}
</style>
