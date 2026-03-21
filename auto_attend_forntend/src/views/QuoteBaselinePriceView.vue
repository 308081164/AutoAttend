<template>
  <div class="baseline-price-page">
    <div class="page-head">
      <router-link to="/quote" class="back-link">← {{ $t('quote.backList') }}</router-link>
      <h1>{{ $t('quote.baselinePriceTitle') }}</h1>
      <p class="desc">{{ $t('quote.baselinePriceDesc') }}</p>
    </div>

    <div v-if="loading" class="placeholder">{{ $t('quote.loading') }}</div>
    <template v-else>
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
  name: 'QuoteBaselinePriceView',
  data () {
    return {
      loading: true,
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
        const [rb, rp] = await Promise.all([
          this.$http.get('/admin/quote/baselines'),
          this.$http.get('/admin/quote/price-config', { params: { all: true } })
        ])
        if (rb.data && rb.data.code === 0) {
          this.baselines = (rb.data.data || []).map(x => this.normBaseline(x))
        } else this.baselines = []
        if (rp.data && rp.data.code === 0) {
          this.prices = (rp.data.data || []).map(x => this.normPrice(x))
        } else this.prices = []
      } catch (e) {
        if (e.response && e.response.status === 401) this.$router.push({ name: 'login' })
        this.baselines = []
        this.prices = []
      } finally {
        this.loading = false
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
.baseline-price-page { max-width: 1000px; margin: 0 auto; padding: 16px; }
.page-head { margin-bottom: 20px; }
.back-link { font-size: 13px; color: #2563eb; text-decoration: none; }
.page-head h1 { margin: 8px 0; font-size: 22px; }
.desc { color: #6b7280; font-size: 14px; margin: 0; }
.card { background: #fff; border: 1px solid #e5e7eb; border-radius: 8px; padding: 16px; margin-bottom: 20px; }
.card h2 { margin: 0 0 8px; font-size: 16px; }
.hint { font-size: 12px; color: #6b7280; margin: 0 0 12px; }
.table-wrap { overflow-x: auto; }
.data-table { width: 100%; border-collapse: collapse; font-size: 13px; }
.data-table th, .data-table td { border: 1px solid #e5e7eb; padding: 8px; text-align: left; vertical-align: middle; }
.data-table th { background: #f9fafb; }
.inp { padding: 6px 8px; border: 1px solid #e5e7eb; border-radius: 6px; font-size: 13px; }
.inp.num { width: 88px; }
.inp.wide { min-width: 160px; width: 100%; max-width: 280px; }
.inp.cur { width: 64px; text-transform: uppercase; }
.cen { text-align: center; }
.actions { white-space: nowrap; }
.btn-sm { padding: 4px 10px; font-size: 12px; border-radius: 4px; border: 1px solid #e5e7eb; background: #fff; cursor: pointer; margin-right: 4px; }
.btn-sm.danger { color: #b91c1c; }
.add-row { display: flex; flex-wrap: wrap; gap: 8px; align-items: center; margin-top: 12px; }
.primary-button {
  display: inline-block; padding: 8px 16px; background: #2563eb; color: #fff;
  border-radius: 6px; border: none; cursor: pointer; font-size: 14px;
}
.chk { display: inline-flex; align-items: center; gap: 6px; font-size: 13px; }
.placeholder { padding: 24px; color: #6b7280; }
.ok { color: #059669; font-size: 13px; }
.err { color: #dc2626; font-size: 13px; }
</style>
