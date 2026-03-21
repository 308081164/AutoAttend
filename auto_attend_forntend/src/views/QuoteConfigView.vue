<template>
  <div class="quote-config-page">
    <div class="page-head">
      <router-link to="/quote" class="back-link">← {{ $t('quote.backList') }}</router-link>
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
      newBaseline: { techStack: 'vue_node', complexity: 'standard', days: 1.5 },
      newPrice: { regionLabel: '', pricePerDay: 1500, currency: 'CNY', enabled: true },
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
      return {
        id: row.id,
        regionLabel: row.regionLabel || '',
        pricePerDay: row.pricePerDay != null ? Number(row.pricePerDay) : 0,
        currency: (row.currency || 'CNY').toUpperCase(),
        enabled: row.enabled === true || row.enabled === 1
      }
    },
    async load () {
      this.loading = true
      try {
        const [r1, r2, rb, rp] = await Promise.all([
          this.$http.get('/admin/quote/risk-config'),
          this.$http.get('/admin/quote/preset-items', { params: { all: true } }),
          this.$http.get('/admin/quote/baselines'),
          this.$http.get('/admin/quote/price-config', { params: { all: true } })
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
          currency: this.newPrice.currency || 'CNY',
          enabled: this.newPrice.enabled !== false
        })
        if (resp.data && resp.data.code === 0) {
          this.priceOk = true
          this.priceMsg = this.$t('quote.addPriceOk')
          this.newPrice = { regionLabel: '', pricePerDay: 1500, currency: 'CNY', enabled: true }
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
  padding: clamp(16px, 2vw, 28px) clamp(16px, 3vw, 40px);
  box-sizing: border-box;
  color: #0f172a;
  background: #f1f5f9;
}
.page-head { margin-bottom: 20px; }
.back-link { font-size: 15px; color: #1d4ed8; text-decoration: none; font-weight: 500; }
.back-link:hover { text-decoration: underline; color: #1e40af; }
.page-head h1 { margin: 8px 0 10px; font-size: clamp(1.35rem, 2vw, 1.75rem); font-weight: 700; color: #020617; }
.desc { color: #475569; font-size: 15px; margin: 0; line-height: 1.5; font-weight: 500; }
.card {
  background: #fff;
  border: 1px solid #cbd5e1;
  border-radius: 10px;
  padding: clamp(18px, 2vw, 24px);
  margin-bottom: 20px;
  box-shadow: 0 1px 2px rgba(15, 23, 42, 0.06);
}
.card h2 { margin: 0 0 10px; font-size: 1.125rem; font-weight: 700; color: #020617; }
.hint { font-size: 14px; color: #475569; margin: 0 0 12px; line-height: 1.5; font-weight: 500; }
.table-wrap { overflow-x: auto; margin-bottom: 12px; }
.data-table { width: 100%; border-collapse: collapse; font-size: 15px; background: #fff; }
.data-table th, .data-table td { border: 1px solid #cbd5e1; padding: 10px 12px; text-align: left; vertical-align: middle; color: #1e293b; }
.data-table th { background: #e2e8f0; font-weight: 700; color: #0f172a; }
.cfg-table { width: 100%; border-collapse: collapse; font-size: 15px; background: #fff; }
.cfg-table th, .cfg-table td { border: 1px solid #cbd5e1; padding: 10px 12px; text-align: left; vertical-align: middle; color: #1e293b; }
.cfg-table th { background: #e2e8f0; font-weight: 700; color: #0f172a; }
.cfg-table .cen { text-align: center; }
.cfg-table .inp.compact { width: 100%; min-width: 140px; box-sizing: border-box; font-size: 15px; }
.cfg-table .pct-inp { width: 88px; font-size: 15px; }
.cfg-table .inp.narrow { max-width: 160px; }
code.rk { font-size: 13px; background: #e2e8f0; color: #0f172a; padding: 3px 8px; border-radius: 4px; border: 1px solid #cbd5e1; }
.row-actions { white-space: nowrap; }
.row-actions .btn-sm { margin-right: 6px; }
.new-preset-grid { display: flex; flex-wrap: wrap; gap: 10px; align-items: center; margin-top: 14px; }
.chk.inline { display: inline-flex; flex-direction: row; align-items: center; gap: 6px; font-size: 15px; font-weight: 500; color: #1e293b; }
.inp {
  padding: 8px 12px;
  border: 1px solid #94a3b8;
  border-radius: 8px;
  font-size: 15px;
  color: #0f172a;
  background: #fff;
}
.inp:focus { outline: 2px solid #3b82f6; outline-offset: 1px; border-color: #2563eb; }
.inp.num { width: 96px; }
.inp.wide { min-width: 160px; width: 100%; max-width: 300px; }
.inp.cur { width: 72px; text-transform: uppercase; }
.cen { text-align: center; }
.actions { white-space: nowrap; }
.btn-sm {
  padding: 6px 12px;
  font-size: 14px;
  border-radius: 6px;
  border: 1px solid #64748b;
  background: #fff;
  color: #1e293b;
  cursor: pointer;
  margin-right: 4px;
  font-weight: 500;
}
.btn-sm:hover { background: #f1f5f9; }
.btn-sm.danger { color: #991b1b; border-color: #fca5a5; }
.btn-sm.danger:hover { background: #fef2f2; }
.add-row { display: flex; flex-wrap: wrap; gap: 10px; align-items: center; margin-top: 12px; }
.primary-button {
  display: inline-block; padding: 10px 18px; background: #1d4ed8; color: #fff;
  border-radius: 8px; border: none; cursor: pointer; font-size: 16px; font-weight: 600;
}
.primary-button:hover { background: #1e40af; }
.btn { padding: 10px 18px; border-radius: 8px; border: none; cursor: pointer; font-size: 16px; margin-right: 8px; margin-top: 8px; }
.btn.secondary { background: #e2e8f0; color: #0f172a; border: 1px solid #94a3b8; font-weight: 500; }
.btn.secondary:hover { background: #cbd5e1; }
.chk { display: inline-flex; align-items: center; gap: 6px; font-size: 15px; font-weight: 500; color: #1e293b; }
.placeholder { padding: 28px; color: #475569; font-size: 16px; font-weight: 500; }
.ok { color: #047857; font-size: 15px; font-weight: 600; }
.err { color: #b91c1c; font-size: 15px; font-weight: 600; }
</style>
