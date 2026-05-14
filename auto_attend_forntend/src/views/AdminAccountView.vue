<template>
  <div class="account-page">
    <h1 class="account-title">{{ $t('accountPage.title') }}</h1>
    <p class="account-lead">{{ $t('accountPage.lead') }}</p>

    <section class="account-card" v-if="me">
      <h2 class="account-h2">{{ $t('accountPage.profileSection') }}</h2>
      <dl class="account-dl">
        <div><dt>{{ $t('accountPage.phone') }}</dt><dd>{{ me.phone || '—' }}</dd></div>
        <div><dt>{{ $t('accountPage.tenant') }}</dt><dd>{{ me.tenantName || '—' }}</dd></div>
        <div><dt>slug</dt><dd>{{ me.slug || '—' }}</dd></div>
      </dl>
      <router-link to="/tenant-admins" class="secondary-button small">{{ $t('accountPage.manageTenantAdmins') }}</router-link>
    </section>

    <section class="account-card">
      <h2 class="account-h2">{{ $t('accountPage.passwordSection') }}</h2>
      <div class="form-row">
        <label class="form-label">{{ $t('accountPage.currentPassword') }}</label>
        <input v-model="pwdForm.current" type="password" class="form-input" autocomplete="current-password">
      </div>
      <div class="form-row">
        <label class="form-label">{{ $t('accountPage.newPassword') }}</label>
        <input v-model="pwdForm.next" type="password" class="form-input" autocomplete="new-password">
      </div>
      <div class="form-row">
        <label class="form-label">{{ $t('accountPage.newPasswordConfirm') }}</label>
        <input v-model="pwdForm.next2" type="password" class="form-input" autocomplete="new-password">
      </div>
      <div class="form-actions">
        <button type="button" class="primary-button" :disabled="pwdSaving" @click="changePassword">
          {{ pwdSaving ? '…' : $t('accountPage.savePassword') }}
        </button>
        <span v-if="pwdMsg" class="save-message" :class="pwdOk ? 'success' : 'error'">{{ pwdMsg }}</span>
      </div>
    </section>

    <section class="account-card account-danger">
      <h2 class="account-h2">{{ $t('accountPage.sessionSection') }}</h2>
      <p class="text-muted small">{{ $t('accountPage.logoutHint') }}</p>
      <button type="button" class="danger-button" @click="doLogout">{{ $t('app.logout') }}</button>
    </section>
  </div>
</template>

<script>
import { notifyAuthSessionChanged } from '../utils/authSession'

export default {
  name: 'AdminAccountView',
  data () {
    return {
      me: null,
      pwdForm: { current: '', next: '', next2: '' },
      pwdSaving: false,
      pwdMsg: '',
      pwdOk: false
    }
  },
  created () {
    this.loadMe()
  },
  methods: {
    async loadMe () {
      try {
        const resp = await this.$http.get('/admin/auth/me')
        if (resp.data && resp.data.code === 0 && resp.data.data) {
          this.me = resp.data.data
        }
      } catch (e) {
        this.me = null
      }
    },
    async changePassword () {
      this.pwdMsg = ''
      this.pwdSaving = true
      try {
        const resp = await this.$http.post('/admin/auth/change-password', {
          currentPassword: this.pwdForm.current,
          newPassword: this.pwdForm.next,
          newPasswordConfirm: this.pwdForm.next2
        })
        if (resp.data && resp.data.code === 0) {
          this.pwdOk = true
          this.pwdMsg = this.$t('accountPage.passwordOk')
          this.pwdForm = { current: '', next: '', next2: '' }
        } else {
          this.pwdOk = false
          this.pwdMsg = (resp.data && resp.data.message) || this.$t('accountPage.passwordFail')
        }
      } catch (e) {
        this.pwdOk = false
        this.pwdMsg = (e.response && e.response.data && e.response.data.message) || this.$t('accountPage.passwordFail')
      } finally {
        this.pwdSaving = false
      }
    },
    async doLogout () {
      const adminTok = window.localStorage.getItem('autoattend_token')
      try {
        if (adminTok) {
          await this.$http.post('/admin/auth/logout', null, {
            headers: { Authorization: 'Bearer ' + adminTok }
          })
        }
      } catch (e) { /* ignore */ }
      window.localStorage.removeItem('autoattend_token')
      window.localStorage.removeItem('autoattend_username')
      notifyAuthSessionChanged()
      this.$router.replace({ name: 'login' })
    }
  }
}
</script>

<style scoped>
.account-page {
  max-width: 640px;
  margin: 0 auto;
}
.account-title {
  margin: 0 0 8px;
  font-size: 22px;
  font-weight: 700;
}
.account-lead {
  margin: 0 0 24px;
  color: var(--text-secondary, #646a73);
  font-size: 14px;
}
.account-card {
  background: var(--bg-card, #fff);
  border: 1px solid var(--border-primary, #e5e7eb);
  border-radius: 10px;
  padding: 20px 22px;
  margin-bottom: 16px;
}
.account-h2 {
  margin: 0 0 14px;
  font-size: 15px;
  font-weight: 600;
}
.account-dl {
  margin: 0 0 14px;
}
.account-dl > div {
  margin-bottom: 10px;
}
.account-dl dt {
  font-size: 12px;
  color: var(--text-tertiary, #8f959e);
  margin: 0 0 2px;
}
.account-dl dd {
  margin: 0;
  font-size: 14px;
}
.form-row {
  margin-bottom: 12px;
}
.form-label {
  display: block;
  font-size: 13px;
  margin-bottom: 6px;
}
.form-input {
  width: 100%;
  max-width: 360px;
  box-sizing: border-box;
  padding: 8px 10px;
  border-radius: 6px;
  border: 1px solid var(--border-primary, #dee0e3);
  font: inherit;
}
.form-actions {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 12px;
  margin-top: 8px;
}
.account-danger .danger-button {
  margin-top: 8px;
}
.danger-button {
  padding: 8px 16px;
  border-radius: 8px;
  border: 1px solid #fecaca;
  background: #fef2f2;
  color: #b91c1c;
  cursor: pointer;
  font: inherit;
}
.danger-button:hover {
  background: #fee2e2;
}
</style>
