<template>
  <div class="wrap">
    <h2 class="title">系统配置</h2>
    <p class="lead">平台级设置（与租户无关）：SMTP 发信、日报邮件定时任务。租户控制台不再提供邮件表单；未配置 SMTP 时相关能力不可用，不影响其他功能。</p>

    <section class="panel">
      <h3>SMTP 发信</h3>
      <p class="muted small">用于项目日报、开发者日报等。配置保存在全局库（tenant_id=0）。</p>
      <div v-if="mailLoading" class="muted">加载中…</div>
      <form v-else class="form" @submit.prevent="saveMail">
        <label>系统公网基址（邮件内链接）</label>
        <input v-model="mailForm.publicBaseUrl" type="text" class="inp" placeholder="https://your-domain.com">
        <label>SMTP Host</label>
        <input v-model="mailForm.smtpHost" type="text" class="inp" placeholder="smtp.example.com">
        <label>SMTP Port</label>
        <input v-model.number="mailForm.smtpPort" type="number" class="inp narrow" placeholder="587">
        <label>SMTP 用户名</label>
        <input v-model="mailForm.smtpUsername" type="text" class="inp" autocomplete="off">
        <label>SMTP 密码</label>
        <p v-if="mail.smtpPasswordMasked" class="hint">已设置密码，留空表示不修改</p>
        <input v-model="mailForm.smtpPassword" type="password" class="inp" autocomplete="off" placeholder="留空则不修改">
        <label>发件人邮箱</label>
        <input v-model="mailForm.fromAddress" type="text" class="inp" placeholder="noreply@example.com">
        <label>发件人显示名</label>
        <input v-model="mailForm.fromName" type="text" class="inp">
        <div class="btn-row">
          <button type="submit" class="btn primary" :disabled="mailSaving">{{ mailSaving ? '保存中…' : '保存 SMTP' }}</button>
          <span v-if="mailMsg" :class="mailOk ? 'ok' : 'err'">{{ mailMsg }}</span>
          <span v-if="mail.configured" class="ok small">当前 SMTP 已配齐</span>
        </div>
      </form>
      <div class="test-block">
        <label>测试收件邮箱</label>
        <div class="test-row">
          <input v-model="testToEmail" type="email" class="inp flex" placeholder="your@email.com">
          <button type="button" class="btn" :disabled="testSending" @click="sendTestMail">{{ testSending ? '发送中…' : '发送测试邮件' }}</button>
        </div>
        <p v-if="testMsg" :class="testOk ? 'ok' : 'err'">{{ testMsg }}</p>
      </div>
    </section>

    <section class="panel">
      <h3>日报邮件调度</h3>
      <p class="muted small">Cron 使用 Spring 6 域表达式（秒 分 时 日 月 周），保存后立即重载调度。</p>
      <div v-if="rmLoading" class="muted">加载中…</div>
      <form v-else class="form" @submit.prevent="saveReportMail">
        <label class="chk"><input v-model="rmForm.enabled" type="checkbox"> 启用定时发送昨天日报</label>
        <label>Cron</label>
        <input v-model="rmForm.cron" type="text" class="inp mono" placeholder="0 30 4 * * *">
        <label>时区</label>
        <input v-model="rmForm.timezone" type="text" class="inp" placeholder="Asia/Shanghai">
        <div class="btn-row">
          <button type="submit" class="btn primary" :disabled="rmSaving">{{ rmSaving ? '保存中…' : '保存调度' }}</button>
          <span v-if="rmMsg" :class="rmOk ? 'ok' : 'err'">{{ rmMsg }}</span>
        </div>
      </form>
    </section>
  </div>
</template>

<script>
import { http } from '../api/http'

export default {
  name: 'SystemSettingsPage',
  data () {
    return {
      mailLoading: true,
      mailSaving: false,
      mailMsg: '',
      mailOk: true,
      mail: {},
      mailForm: {
        publicBaseUrl: '',
        smtpHost: '',
        smtpPort: 587,
        smtpUsername: '',
        smtpPassword: '',
        fromAddress: '',
        fromName: ''
      },
      testToEmail: '',
      testSending: false,
      testMsg: '',
      testOk: true,
      rmLoading: true,
      rmSaving: false,
      rmMsg: '',
      rmOk: true,
      rmForm: {
        enabled: true,
        cron: '0 30 4 * * *',
        timezone: 'Asia/Shanghai'
      }
    }
  },
  mounted () {
    this.loadAll()
  },
  methods: {
    async loadAll () {
      await Promise.all([this.loadMail(), this.loadReportMail()])
    },
    async loadMail () {
      this.mailLoading = true
      try {
        const { data } = await http.get('/platform/settings/mail')
        if (data.code === 0 && data.data) {
          this.mail = data.data
          const d = data.data
          this.mailForm.publicBaseUrl = d.publicBaseUrl || ''
          this.mailForm.smtpHost = d.smtpHost || ''
          this.mailForm.smtpPort = d.smtpPort != null ? Number(d.smtpPort) : 587
          this.mailForm.smtpUsername = d.smtpUsername || ''
          this.mailForm.smtpPassword = ''
          this.mailForm.fromAddress = d.fromAddress || ''
          this.mailForm.fromName = d.fromName || ''
        }
      } catch (e) {
        this.mailMsg = '加载失败'
        this.mailOk = false
      } finally {
        this.mailLoading = false
      }
    },
    async saveMail () {
      this.mailSaving = true
      this.mailMsg = ''
      try {
        const payload = {
          publicBaseUrl: this.mailForm.publicBaseUrl || '',
          smtpHost: this.mailForm.smtpHost || '',
          smtpPort: this.mailForm.smtpPort != null ? Number(this.mailForm.smtpPort) : null,
          smtpUsername: this.mailForm.smtpUsername || '',
          fromAddress: this.mailForm.fromAddress || '',
          fromName: this.mailForm.fromName || ''
        }
        if (this.mailForm.smtpPassword && !this.mailForm.smtpPassword.includes('****')) {
          payload.smtpPassword = this.mailForm.smtpPassword
        }
        const { data } = await http.put('/platform/settings/mail', payload)
        if (data.code === 0) {
          this.mailMsg = '已保存'
          this.mailOk = true
          this.mailForm.smtpPassword = ''
          await this.loadMail()
        } else {
          this.mailMsg = data.message || '保存失败'
          this.mailOk = false
        }
      } catch (e) {
        this.mailMsg = '请求失败'
        this.mailOk = false
      } finally {
        this.mailSaving = false
      }
    },
    async sendTestMail () {
      const to = (this.testToEmail || '').trim()
      if (!to) {
        this.testMsg = '请填写收件邮箱'
        this.testOk = false
        return
      }
      this.testSending = true
      this.testMsg = ''
      try {
        const { data } = await http.post('/platform/settings/mail/test', { toEmail: to })
        if (data.code === 0 && data.data) {
          this.testOk = !!data.data.ok
          this.testMsg = data.data.message || ''
        } else {
          this.testOk = false
          this.testMsg = (data && data.message) || '失败'
        }
      } catch (e) {
        this.testOk = false
        this.testMsg = '请求失败'
      } finally {
        this.testSending = false
      }
    },
    async loadReportMail () {
      this.rmLoading = true
      try {
        const { data } = await http.get('/platform/settings/report-mail')
        if (data.code === 0 && data.data) {
          const d = data.data
          this.rmForm.enabled = d.enabled !== false
          this.rmForm.cron = d.cron || '0 30 4 * * *'
          this.rmForm.timezone = d.timezone || 'Asia/Shanghai'
        }
      } catch (e) {
        this.rmMsg = '加载失败'
        this.rmOk = false
      } finally {
        this.rmLoading = false
      }
    },
    async saveReportMail () {
      this.rmSaving = true
      this.rmMsg = ''
      try {
        const { data } = await http.put('/platform/settings/report-mail', {
          enabled: this.rmForm.enabled,
          cron: this.rmForm.cron,
          timezone: this.rmForm.timezone
        })
        if (data.code === 0) {
          this.rmMsg = '已保存并重载调度'
          this.rmOk = true
        } else {
          this.rmMsg = data.message || '保存失败'
          this.rmOk = false
        }
      } catch (e) {
        this.rmMsg = '请求失败'
        this.rmOk = false
      } finally {
        this.rmSaving = false
      }
    }
  }
}
</script>

<style scoped>
.wrap { max-width: 720px; margin: 0 auto; }
.title { margin: 0 0 8px; font-size: 20px; font-weight: 700; }
.lead { margin: 0 0 20px; font-size: 14px; line-height: 1.55; color: #94a3b8; }
.panel {
  margin-bottom: 18px;
  padding: 16px;
  background: #0b1224;
  border: 1px solid #1f2a44;
  border-radius: 12px;
}
.panel h3 { margin: 0 0 10px; font-size: 16px; }
.muted { color: #94a3b8; }
.small { font-size: 13px; }
.form { display: flex; flex-direction: column; gap: 8px; }
.form label { font-size: 13px; color: #cbd5e1; margin-top: 6px; }
.inp {
  padding: 8px 10px;
  border-radius: 8px;
  border: 1px solid #334155;
  background: #0f172a;
  color: #f1f5f9;
  font-size: 14px;
}
.inp.narrow { max-width: 120px; }
.inp.flex { flex: 1; min-width: 0; }
.inp.mono { font-family: ui-monospace, monospace; }
.hint { margin: 0; font-size: 12px; color: #94a3b8; }
.btn-row { display: flex; flex-wrap: wrap; align-items: center; gap: 12px; margin-top: 12px; }
.btn {
  padding: 8px 14px;
  border-radius: 8px;
  border: 1px solid #475569;
  background: #1e293b;
  color: #e2e8f0;
  cursor: pointer;
  font-size: 14px;
}
.btn.primary {
  border-color: #2563eb;
  background: #1d4ed8;
  color: #fff;
}
.btn:disabled { opacity: 0.55; cursor: not-allowed; }
.ok { color: #86efac; font-size: 13px; }
.err { color: #fca5a5; font-size: 13px; }
.chk { display: flex; align-items: center; gap: 8px; cursor: pointer; }
.test-block { margin-top: 18px; padding-top: 14px; border-top: 1px solid #1e293b; }
.test-row { display: flex; flex-wrap: wrap; gap: 10px; align-items: center; margin-top: 8px; }
</style>
