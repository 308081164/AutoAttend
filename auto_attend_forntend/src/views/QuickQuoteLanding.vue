<template>
  <div class="quick-quote-page">
    <!-- 加载/错误状态 -->
    <div v-if="pageState === 'loading'" class="center-box">
      <div class="spinner"></div>
      <p>正在加载团队信息…</p>
    </div>
    <div v-else-if="pageState === 'error'" class="center-box">
      <div class="error-icon">✕</div>
      <p class="err">{{ errorMsg }}</p>
    </div>

    <!-- Step 1: 输入信息 -->
    <div v-else-if="pageState === 'form'" class="form-card">
      <div class="brand" v-if="teamName">
        <span class="brand-label">{{ teamName }}</span>
      </div>
      <h1>快速创建报价</h1>
      <p class="lead">为您的项目取一个代号，选择报价模式，系统将自动创建 AI 需求分析会话。</p>

      <div class="form-group">
        <label>项目名称 / 代号 <span class="required">*</span></label>
        <input
          v-model="projectName"
          class="inp"
          placeholder="例如：张总-企业官网、李经理-电商小程序"
          maxlength="100"
          @keyup.enter="submit"
        />
      </div>

      <div class="form-group">
        <label>报价模式</label>
        <div class="mode-selector">
          <label class="mode-option" :class="{ active: quoteKind === 'single' }">
            <input type="radio" v-model="quoteKind" value="single" />
            <div class="mode-content">
              <strong>单体应用</strong>
              <span>一个端 / 一个系统</span>
            </div>
          </label>
          <label class="mode-option" :class="{ active: quoteKind === 'solution' }">
            <input type="radio" v-model="quoteKind" value="solution" />
            <div class="mode-content">
              <strong>解决方案级</strong>
              <span>多端 / 多交付物</span>
            </div>
          </label>
        </div>
      </div>

      <button class="btn primary btn-block" :disabled="submitting || !projectName.trim()" @click="submit">
        {{ submitting ? '创建中…' : '创建 AI 需求分析会话' }}
      </button>
      <p v-if="submitMsg" :class="submitMsgOk ? 'ok' : 'err'">{{ submitMsg }}</p>
    </div>

    <!-- Step 2: 成功 - 展示链接 -->
    <div v-else-if="pageState === 'success'" class="form-card success-card">
      <div class="success-icon">✓</div>
      <h1>会话已创建</h1>
      <p class="lead">项目「{{ resultData.projectName }}」已创建，AI 需求分析会话已就绪。</p>

      <div class="result-box">
        <div class="result-row">
          <span class="result-label">项目名称</span>
          <span class="result-value">{{ resultData.projectName }}</span>
        </div>
        <div class="result-row">
          <span class="result-label">报价模式</span>
          <span class="result-value">{{ resultData.quoteKind === 'solution' ? '解决方案级' : '单体应用' }}</span>
        </div>
        <div class="result-row">
          <span class="result-label">Agent 链接</span>
          <div class="result-link-row">
            <code class="result-link">{{ fullAgentUrl }}</code>
            <button class="btn-sm primary" @click="copyLink">复制</button>
          </div>
        </div>
      </div>

      <div class="actions-row">
        <button class="btn primary large" @click="openAgent">立即进入 AI 对话</button>
        <button class="btn secondary" @click="copyLink">复制链接到剪贴板</button>
      </div>
      <p v-if="copyMsg" class="ok">{{ copyMsg }}</p>
    </div>
  </div>
</template>

<script>
import axios from 'axios'

export default {
  name: 'QuickQuoteLanding',
  data () {
    return {
      teamName: '',
      pageState: 'loading', // loading | form | error | success
      errorMsg: '',
      projectName: '',
      quoteKind: 'single',
      submitting: false,
      submitMsg: '',
      submitMsgOk: false,
      resultData: {},
      copyMsg: ''
    }
  },
  computed: {
    fullAgentUrl () {
      if (!this.resultData.publicToken) return ''
      return window.location.origin + '/agent/' + this.resultData.publicToken
    }
  },
  methods: {
    async checkSlug () {
      const slug = this.$route.params.slug
      if (!slug) {
        this.pageState = 'error'
        this.errorMsg = '链接无效：缺少团队标识'
        return
      }
      try {
        const resp = await axios.get('/api/public/agent/quick-start/' + encodeURIComponent(slug) + '/check')
        if (resp.data && resp.data.code === 0 && resp.data.data) {
          this.teamName = resp.data.data.teamName || ''
          this.pageState = 'form'
        } else {
          this.pageState = 'error'
          this.errorMsg = (resp.data && resp.data.message) || '团队不存在'
        }
      } catch (e) {
        this.pageState = 'error'
        this.errorMsg = '请求失败，请检查网络'
      }
    },
    async submit () {
      const name = this.projectName.trim()
      if (!name) return
      this.submitting = true
      this.submitMsg = ''
      const slug = this.$route.params.slug
      try {
        const resp = await axios.post('/api/public/agent/quick-start/' + encodeURIComponent(slug), {
          projectName: name,
          quoteKind: this.quoteKind
        })
        if (resp.data && resp.data.code === 0 && resp.data.data) {
          this.resultData = resp.data.data
          this.pageState = 'success'
        } else {
          this.submitMsg = (resp.data && resp.data.message) || '创建失败'
          this.submitMsgOk = false
        }
      } catch (e) {
        this.submitMsg = (e.response && e.response.data && e.response.data.message) || '请求失败'
        this.submitMsgOk = false
      } finally {
        this.submitting = false
      }
    },
    async copyLink () {
      const url = this.fullAgentUrl
      if (!url) return
      try {
        await navigator.clipboard.writeText(url)
        this.copyMsg = '✓ 已复制到剪贴板'
        setTimeout(() => { this.copyMsg = '' }, 3000)
      } catch (e) {
        // fallback
        const ta = document.createElement('textarea')
        ta.value = url
        document.body.appendChild(ta)
        ta.select()
        document.execCommand('copy')
        document.body.removeChild(ta)
        this.copyMsg = '✓ 已复制到剪贴板'
        setTimeout(() => { this.copyMsg = '' }, 3000)
      }
    },
    openAgent () {
      if (this.resultData.publicToken) {
        window.open('/agent/' + this.resultData.publicToken, '_blank')
      }
    }
  },
  created () {
    this.checkSlug()
  }
}
</script>

<style scoped>
.quick-quote-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
  color: #1a1a2e;
  box-sizing: border-box;
}
.center-box {
  text-align: center;
  color: #fff;
}
.spinner {
  width: 36px;
  height: 36px;
  border: 3px solid rgba(255,255,255,0.3);
  border-top-color: #fff;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
  margin: 0 auto 16px;
}
@keyframes spin { to { transform: rotate(360deg); } }
.error-icon {
  width: 56px;
  height: 56px;
  border-radius: 50%;
  background: rgba(255,255,255,0.2);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28px;
  margin: 0 auto 16px;
}
.form-card {
  background: #fff;
  border-radius: 16px;
  padding: 40px 32px;
  max-width: 460px;
  width: 100%;
  box-shadow: 0 20px 60px rgba(0,0,0,0.15);
}
.form-card h1 {
  margin: 0 0 8px;
  font-size: 24px;
  text-align: center;
}
.lead {
  color: #666;
  text-align: center;
  margin-bottom: 28px;
  font-size: 14px;
  line-height: 1.6;
}
.brand {
  text-align: center;
  margin-bottom: 20px;
}
.brand-label {
  display: inline-block;
  padding: 4px 14px;
  background: #f0f0ff;
  border-radius: 20px;
  font-size: 13px;
  color: #667eea;
  font-weight: 500;
}
.form-group {
  margin-bottom: 20px;
}
.form-group label {
  display: block;
  font-size: 14px;
  font-weight: 500;
  margin-bottom: 6px;
}
.required { color: #e74c3c; }
.inp {
  width: 100%;
  padding: 10px 14px;
  border: 1px solid #ddd;
  border-radius: 8px;
  font-size: 15px;
  box-sizing: border-box;
  transition: border-color 0.2s;
}
.inp:focus {
  outline: none;
  border-color: #667eea;
  box-shadow: 0 0 0 3px rgba(102,126,234,0.1);
}
.mode-selector {
  display: flex;
  gap: 12px;
}
.mode-option {
  flex: 1;
  cursor: pointer;
  border: 2px solid #e5e7eb;
  border-radius: 10px;
  padding: 14px;
  transition: all 0.2s;
  display: flex;
  align-items: flex-start;
  gap: 8px;
}
.mode-option input { display: none; }
.mode-option.active {
  border-color: #667eea;
  background: #f8f7ff;
}
.mode-content strong {
  display: block;
  font-size: 14px;
  margin-bottom: 2px;
}
.mode-content span {
  font-size: 12px;
  color: #999;
}
.btn {
  padding: 10px 20px;
  border-radius: 8px;
  border: none;
  cursor: pointer;
  font-size: 15px;
  font-weight: 500;
  transition: opacity 0.2s;
}
.btn:disabled { opacity: 0.5; cursor: not-allowed; }
.btn.primary { background: #667eea; color: #fff; }
.btn.primary:hover:not(:disabled) { background: #5a6fd6; }
.btn.secondary { background: #f3f4f6; color: #333; border: 1px solid #ddd; }
.btn-block { width: 100%; }
.btn.large { padding: 14px 28px; font-size: 16px; }
.btn-sm {
  padding: 5px 12px;
  font-size: 12px;
  border-radius: 6px;
  border: none;
  cursor: pointer;
}
.btn-sm.primary { background: #667eea; color: #fff; }
.ok { color: #059669; font-size: 13px; margin-top: 8px; }
.err { color: #dc2626; font-size: 13px; margin-top: 8px; }
.hint { color: #999; font-size: 13px; margin-top: 8px; }

/* 成功页 */
.success-card { text-align: center; }
.success-icon {
  width: 64px;
  height: 64px;
  border-radius: 50%;
  background: #dcfce7;
  color: #059669;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 32px;
  margin: 0 auto 20px;
}
.result-box {
  background: #f9fafb;
  border-radius: 10px;
  padding: 16px;
  margin: 20px 0;
  text-align: left;
}
.result-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 0;
  border-bottom: 1px solid #eee;
}
.result-row:last-child { border-bottom: none; }
.result-label { font-size: 13px; color: #666; flex-shrink: 0; }
.result-value { font-size: 13px; font-weight: 500; }
.result-link-row {
  display: flex;
  gap: 8px;
  align-items: center;
  flex: 1;
  margin-left: 12px;
}
.result-link {
  font-size: 12px;
  background: #fff;
  padding: 4px 8px;
  border-radius: 4px;
  border: 1px solid #ddd;
  word-break: break-all;
  flex: 1;
  color: #667eea;
}
.actions-row {
  display: flex;
  gap: 12px;
  justify-content: center;
  flex-wrap: wrap;
  margin-top: 20px;
}
</style>
