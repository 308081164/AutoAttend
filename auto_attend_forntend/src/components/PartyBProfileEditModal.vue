<template>
  <div v-show="visible" class="pbp-overlay" @click.self="onClose">
    <div class="pbp-dialog" role="dialog" aria-modal="true" @click.stop>
      <div class="pbp-head">
        <h3>{{ $t('quote.partyBModalTitle') }}</h3>
        <button type="button" class="pbp-x" aria-label="Close" @click="onClose">×</button>
      </div>
      <p class="pbp-hint">{{ $t('quote.partyBModalHint') }}</p>

      <div class="pbp-block">
        <h4 class="pbp-sub">{{ $t('quote.partyBLegalSubtitle') }}</h4>
        <div class="pbp-grid">
          <label>{{ $t('quote.partyBLegalName') }} <input v-model="partyB.legalName" class="inp" /></label>
          <label>{{ $t('quote.partyBCreditCode') }} <input v-model="partyB.creditCode" class="inp" /></label>
          <label class="full">{{ $t('quote.partyBAddress') }} <input v-model="partyB.address" class="inp" /></label>
          <label>{{ $t('quote.partyBContactName') }} <input v-model="partyB.contactName" class="inp" /></label>
          <label>{{ $t('quote.partyBContactPhone') }} <input v-model="partyB.contactPhone" class="inp" /></label>
          <label>{{ $t('quote.partyBBankName') }} <input v-model="partyB.bankName" class="inp" /></label>
          <label>{{ $t('quote.partyBBankAccount') }} <input v-model="partyB.bankAccount" class="inp" /></label>
        </div>
        <button type="button" class="btn secondary" :disabled="savingLegal" @click="saveLegal">{{ savingLegal ? '…' : $t('quote.partyBSave') }}</button>
        <span v-if="msgLegal" :class="okLegal ? 'ok' : 'err'">{{ msgLegal }}</span>
      </div>

      <div class="pbp-block pbp-block--sep">
        <h4 class="pbp-sub">{{ $t('quote.partyBNaturalSectionTitle') }}</h4>
        <p class="pbp-mini">{{ $t('quote.partyBNaturalSectionHint') }}</p>
        <div class="pbp-grid">
          <label>{{ $t('quote.partyBNaturalFullName') }} <input v-model="partyBNatural.fullName" class="inp" /></label>
          <label>{{ $t('quote.partyBNaturalIdNumber') }} <input v-model="partyBNatural.idNumber" class="inp" /></label>
          <label class="full">{{ $t('quote.partyBNaturalAddress') }} <input v-model="partyBNatural.address" class="inp" /></label>
          <label>{{ $t('quote.partyBNaturalContactPhone') }} <input v-model="partyBNatural.contactPhone" class="inp" /></label>
          <label>{{ $t('quote.partyBNaturalEmail') }} <input v-model="partyBNatural.email" class="inp" type="email" autocomplete="off" /></label>
          <label>{{ $t('quote.partyBBankName') }} <input v-model="partyBNatural.bankName" class="inp" /></label>
          <label>{{ $t('quote.partyBBankAccount') }} <input v-model="partyBNatural.bankAccount" class="inp" /></label>
        </div>
        <button type="button" class="btn secondary" :disabled="savingNat" @click="saveNatural">{{ savingNat ? '…' : $t('quote.partyBNaturalSave') }}</button>
        <span v-if="msgNat" :class="okNat ? 'ok' : 'err'">{{ msgNat }}</span>
      </div>

      <div class="pbp-foot">
        <button type="button" class="btn secondary" @click="onClose">{{ $t('quote.partyBModalClose') }}</button>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'PartyBProfileEditModal',
  props: {
    visible: { type: Boolean, default: false }
  },
  data () {
    return {
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
      savingLegal: false,
      savingNat: false,
      msgLegal: '',
      msgNat: '',
      okLegal: false,
      okNat: false
    }
  },
  watch: {
    visible (v) {
      if (v) {
        this.msgLegal = ''
        this.msgNat = ''
        this.loadFromServer()
      }
    }
  },
  methods: {
    onClose () {
      this.$emit('close')
    },
    applyPayload (d) {
      if (!d || typeof d !== 'object') return
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
    },
    async loadFromServer () {
      try {
        const resp = await this.$http.get('/admin/quote/party-b-profile')
        if (resp.data && resp.data.code === 0 && resp.data.data) {
          this.applyPayload(resp.data.data)
        }
      } catch (e) { /* 忽略 */ }
    },
    legalPayload () {
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
    async saveLegal () {
      this.savingLegal = true
      this.msgLegal = ''
      try {
        const resp = await this.$http.put('/admin/quote/party-b-profile', this.legalPayload())
        if (resp.data && resp.data.code === 0) {
          this.okLegal = true
          this.msgLegal = this.$t('quote.partyBSaveOk')
          this.$emit('saved')
        } else {
          this.okLegal = false
          this.msgLegal = (resp.data && resp.data.message) || this.$t('quote.partyBSaveFail')
        }
      } catch (e) {
        this.okLegal = false
        this.msgLegal = (e.response && e.response.data && e.response.data.message) || this.$t('quote.partyBSaveFail')
      } finally {
        this.savingLegal = false
      }
    },
    async saveNatural () {
      this.savingNat = true
      this.msgNat = ''
      try {
        const resp = await this.$http.put('/admin/quote/party-b-profile', {
          naturalPerson: { ...this.partyBNatural }
        })
        if (resp.data && resp.data.code === 0) {
          this.okNat = true
          this.msgNat = this.$t('quote.partyBSaveOk')
          this.$emit('saved')
        } else {
          this.okNat = false
          this.msgNat = (resp.data && resp.data.message) || this.$t('quote.partyBSaveFail')
        }
      } catch (e) {
        this.okNat = false
        this.msgNat = (e.response && e.response.data && e.response.data.message) || this.$t('quote.partyBSaveFail')
      } finally {
        this.savingNat = false
      }
    }
  }
}
</script>

<style scoped>
.pbp-overlay {
  position: fixed;
  inset: 0;
  z-index: 1000;
  background: rgba(15, 23, 42, 0.45);
  display: flex;
  align-items: flex-start;
  justify-content: center;
  padding: 24px 16px;
  overflow-y: auto;
  box-sizing: border-box;
}
.pbp-dialog {
  width: 100%;
  max-width: 640px;
  background: #fff;
  border-radius: 12px;
  border: 1px solid #cbd5e1;
  box-shadow: 0 20px 50px rgba(15, 23, 42, 0.15);
  padding: 18px 20px 16px;
  margin-bottom: 40px;
}
.pbp-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 8px;
}
.pbp-head h3 {
  margin: 0;
  font-size: 1.1rem;
  font-weight: 700;
  color: #020617;
}
.pbp-x {
  border: none;
  background: #f1f5f9;
  width: 36px;
  height: 36px;
  border-radius: 8px;
  font-size: 22px;
  line-height: 1;
  cursor: pointer;
  color: #475569;
}
.pbp-x:hover { background: #e2e8f0; }
.pbp-hint {
  font-size: 13px;
  color: #64748b;
  margin: 0 0 14px;
  line-height: 1.45;
}
.pbp-block { margin-top: 4px; }
.pbp-block--sep {
  margin-top: 18px;
  padding-top: 16px;
  border-top: 1px solid #e2e8f0;
}
.pbp-sub { margin: 0 0 8px; font-size: 0.95rem; font-weight: 700; color: #0f172a; }
.pbp-mini { font-size: 12px; color: #64748b; margin: -4px 0 10px; line-height: 1.4; }
.pbp-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 10px 12px;
  margin-bottom: 12px;
}
.pbp-grid label {
  display: flex;
  flex-direction: column;
  gap: 4px;
  font-size: 13px;
  font-weight: 600;
  color: #334155;
}
.pbp-grid label.full { grid-column: 1 / -1; }
.inp {
  padding: 8px 10px;
  border: 1px solid #94a3b8;
  border-radius: 6px;
  font-size: 14px;
}
.pbp-foot { margin-top: 16px; padding-top: 12px; border-top: 1px solid #e2e8f0; }
.btn {
  padding: 8px 14px;
  border-radius: 8px;
  border: none;
  cursor: pointer;
  font-size: 14px;
  font-weight: 600;
}
.btn.secondary { background: #e2e8f0; color: #0f172a; border: 1px solid #94a3b8; }
.btn.secondary:hover { background: #cbd5e1; }
.ok { color: #047857; font-size: 13px; margin-left: 8px; font-weight: 600; }
.err { color: #b91c1c; font-size: 13px; margin-left: 8px; font-weight: 600; }
</style>
