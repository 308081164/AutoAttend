<template>
  <div class="ai-config-page">
    <div class="page-header">
      <router-link to="/" class="back-link">← {{ $t('aiConfig.backToHome') }}</router-link>
      <h1 class="page-title">{{ $t('aiConfig.title') }}</h1>
    </div>
    <p class="page-desc">{{ $t('aiConfig.desc') }}</p>

    <div class="config-card" v-if="config">
      <p v-if="configLoadedHint" class="config-loaded-hint">{{ $t('aiConfig.configLoadedHint') }}</p>
      <form @submit.prevent="saveConfig">
        <div class="form-row">
          <label class="form-label">{{ $t('aiConfig.provider') }}</label>
          <span class="form-value">DeepSeek</span>
        </div>
        <div class="form-row">
          <label class="form-label">{{ $t('aiConfig.apiKey') }}</label>
          <p v-if="config.apiKeyMasked" class="api-key-set">{{ $t('aiConfig.apiKeySetHint') }}（{{ config.apiKeyMasked }}）</p>
          <input
            v-model="form.apiKey"
            type="password"
            autocomplete="off"
            :placeholder="config.apiKeyMasked ? $t('aiConfig.apiKeyPlaceholderKeep') : $t('aiConfig.apiKeyPlaceholder')"
            class="form-input"
          >
          <p class="form-hint">{{ $t('aiConfig.apiKeyHint') }}</p>
        </div>
        <div class="form-row">
          <label class="form-label">{{ $t('aiConfig.enabled') }}</label>
          <label class="checkbox-label">
            <input type="checkbox" v-model="form.enabled">
            <span>{{ $t('aiConfig.enabledLabel') }}</span>
          </label>
        </div>
        <div class="form-row">
          <label class="form-label">{{ $t('aiConfig.model') }}</label>
          <input v-model="form.model" type="text" class="form-input" placeholder="deepseek-chat">
        </div>
        <div class="form-actions">
          <button type="submit" class="primary-button" :disabled="saving">{{ saving ? $t('aiConfig.saving') : $t('aiConfig.save') }}</button>
          <span v-if="saveMessage" class="save-message" :class="saveSuccess ? 'success' : 'error'">{{ saveMessage }}</span>
        </div>
      </form>
    </div>
    <div v-else class="placeholder">{{ $t('aiConfig.loading') }}</div>
  </div>
</template>

<script>
export default {
  name: 'AiConfigView',
  data () {
    return {
      config: null,
      form: {
        apiKey: '',
        enabled: false,
        model: 'deepseek-chat'
      },
      saving: false,
      saveMessage: '',
      saveSuccess: false
    }
  },
  created () {
    this.loadConfig()
  },
  computed: {
    configLoadedHint () {
      if (!this.config) return false
      const hasKey = this.config.hasApiKey === true || !!(this.config.apiKeyMasked && String(this.config.apiKeyMasked).trim())
      const isEnabled = this.config.enabled === true || this.config.enabled === 'true' || this.config.enabled === 1
      return hasKey || isEnabled
    }
  },
  methods: {
    async loadConfig () {
      try {
        const resp = await this.$http.get('/admin/ai-analysis/config')
        if (resp.data && resp.data.code === 0 && resp.data.data) {
          const data = resp.data.data
          this.config = data
          this.form.enabled = data.enabled === true || data.enabled === 'true' || data.enabled === 1
          this.form.model = (data.model && String(data.model).trim()) || 'deepseek-chat'
          this.form.apiKey = ''
        }
      } catch (e) {
        if (e.response && e.response.status === 401) {
          this.$router.push({ name: 'login' })
        }
      }
    },
    async saveConfig () {
      this.saving = true
      this.saveMessage = ''
      try {
        const payload = {
          enabled: this.form.enabled,
          model: this.form.model || 'deepseek-chat'
        }
        if (this.form.apiKey && !this.form.apiKey.includes('****')) {
          payload.apiKey = this.form.apiKey
        }
        const resp = await this.$http.put('/admin/ai-analysis/config', payload)
        if (resp.data && resp.data.code === 0) {
          this.saveMessage = this.$t('aiConfig.saveSuccess')
          this.saveSuccess = true
          this.loadConfig()
          this.form.apiKey = ''
        } else {
          this.saveMessage = (resp.data && resp.data.message) || this.$t('aiConfig.saveFailed')
          this.saveSuccess = false
        }
      } catch (e) {
        this.saveMessage = (e.response && e.response.data && e.response.data.message) || this.$t('aiConfig.saveFailed')
        this.saveSuccess = false
      } finally {
        this.saving = false
      }
    }
  }
}
</script>

<style scoped>
.ai-config-page {
  max-width: 560px;
  margin: 0 auto;
}
.page-header {
  margin-bottom: 8px;
}
.back-link {
  font-size: 13px;
  color: #2563eb;
  text-decoration: none;
}
.page-title {
  font-size: 22px;
  margin: 12px 0 0;
}
.page-desc {
  color: #6b7280;
  font-size: 14px;
  margin-bottom: 24px;
}
.config-loaded-hint {
  font-size: 13px;
  color: #059669;
  margin: 0 0 16px 0;
  padding: 8px 12px;
  background: #ecfdf5;
  border-radius: 6px;
}
.api-key-set {
  font-size: 13px;
  color: #059669;
  margin: 0 0 8px 0;
}
.config-card {
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  padding: 24px;
}
.form-row {
  margin-bottom: 20px;
}
.form-label {
  display: block;
  font-size: 13px;
  font-weight: 500;
  color: #374151;
  margin-bottom: 6px;
}
.form-value {
  font-size: 14px;
  color: #6b7280;
}
.form-input {
  width: 100%;
  padding: 8px 12px;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  font-size: 14px;
  box-sizing: border-box;
}
.form-hint {
  font-size: 12px;
  color: #9ca3af;
  margin-top: 4px;
}
.checkbox-label {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  cursor: pointer;
}
.checkbox-label input {
  margin: 0;
}
.form-actions {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-top: 24px;
  padding-top: 16px;
  border-top: 1px solid #e5e7eb;
}
.primary-button {
  padding: 8px 20px;
  border-radius: 6px;
  border: none;
  background: #2563eb;
  color: #fff;
  font-size: 14px;
  cursor: pointer;
}
.primary-button:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}
.save-message {
  font-size: 13px;
}
.save-message.success { color: #059669; }
.save-message.error { color: #dc2626; }
.placeholder {
  padding: 24px;
  text-align: center;
  color: #6b7280;
}
</style>
