<template>
  <div class="wrap">
    <h2 class="title">系统配置</h2>
    <p class="lead">平台级设置（与租户无关）：SMTP 发信、日报邮件定时任务、官方 API 池。租户控制台不再提供邮件表单；未配置 SMTP 时相关能力不可用，不影响其他功能。</p>

    <section class="panel">
      <h3>壳客户端（Flutter / WebView）</h3>
      <p class="muted small">最低版本、黑名单与下载直链保存在平台配置（tenant_id=0）。壳应用请求 API 时若携带 <code class="mono">X-Client-Version</code> 且命中策略，将返回 HTTP 426，官网与纯浏览器无该头不受影响。</p>
      <div v-if="csLoading" class="muted">加载中…</div>
      <form v-else class="form" @submit.prevent="saveClientShell">
        <h4 class="subh">版本策略</h4>
        <label>最低支持版本（semver，如 1.0.0；留空表示不限制）</label>
        <input v-model="csForm.minSupportedVersion" type="text" class="inp mono" placeholder="1.0.0">
        <label>禁止的版本号（逗号分隔，如 0.9.0,1.0.0）</label>
        <input v-model="csForm.blockedVersionsCsv" type="text" class="inp mono" placeholder="">
        <label>禁止的构建号（逗号分隔整数）</label>
        <input v-model="csForm.blockedBuildsCsv" type="text" class="inp mono" placeholder="">
        <label>拦截提示文案</label>
        <input v-model="csForm.blockMessage" type="text" class="inp">
        <label>升级包 / Release 页面 URL</label>
        <input v-model="csForm.upgradeUrl" type="text" class="inp" placeholder="https://github.com/.../releases/...">
        <h4 class="subh">官网下载信息（GitHub Releases）</h4>
        <label>GitHub 仓库（owner/repo）</label>
        <input v-model="csForm.githubRepo" type="text" class="inp mono" placeholder="308081164/AutoAttend">
        <label>Release 标签前缀</label>
        <input v-model="csForm.releaseTagPrefix" type="text" class="inp mono" placeholder="app-v">
        <label>当前展示版本号（与 tag 中版本一致，如 1.0.0）</label>
        <input v-model="csForm.latestVersion" type="text" class="inp mono">
        <label>Windows 安装包直链（可空，则官网仅展示 Release 页）</label>
        <input v-model="csForm.windowsUrl" type="text" class="inp mono">
        <label>Linux deb 直链</label>
        <input v-model="csForm.linuxDebUrl" type="text" class="inp mono">
        <label>Android APK 直链</label>
        <input v-model="csForm.androidApkUrl" type="text" class="inp mono">
        <label>iOS 说明页 / TestFlight 链接</label>
        <input v-model="csForm.iosNoteUrl" type="text" class="inp mono">
        <div class="btn-row">
          <button type="submit" class="btn primary" :disabled="csSaving">{{ csSaving ? '保存中…' : '保存壳客户端配置' }}</button>
          <span v-if="csMsg" :class="csOk ? 'ok' : 'err'">{{ csMsg }}</span>
        </div>
      </form>
    </section>

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
      <h3>📬 来单邮件通知</h3>
      <p class="muted small">客户通过「团队专属 Agent 链接」创建报价项目后，系统自动发送邮件通知管理员。</p>
      <div v-if="notifyLoading" class="muted">加载中…</div>
      <form v-else class="form" @submit.prevent="saveQuickQuoteNotify">
        <label class="chk"><input v-model="notifyForm.enabled" type="checkbox"> 启用来单邮件通知</label>
        <label>通知邮箱</label>
        <input v-model="notifyForm.email" type="email" class="inp" placeholder="admin@company.com" :disabled="!notifyForm.enabled">
        <div class="btn-row">
          <button type="submit" class="btn primary" :disabled="notifySaving">{{ notifySaving ? '保存中…' : '保存通知设置' }}</button>
          <span v-if="notifyMsg" :class="notifyOk ? 'ok' : 'err'">{{ notifyMsg }}</span>
        </div>
      </form>
    </section>

    <section class="panel">
      <h3>🏢 团队展示区配置</h3>
      <p class="muted small">配置客户通过 Agent 链接进入时看到的团队介绍/专业能力展示区域。支持 3 套官方模板和自定义 HTML 模式。</p>
      <div v-if="scLoading" class="muted">加载中…</div>
      <form v-else class="form" @submit.prevent="saveShowcase">
        <label class="chk"><input v-model="scForm.enabled" type="checkbox"> 启用团队展示区</label>
        <label>展示模式</label>
        <select v-model="scForm.mode" class="inp" :disabled="!scForm.enabled">
          <option value="template">官方模板</option>
          <option value="custom_html">自定义 HTML</option>
        </select>
        <template v-if="scForm.mode === 'template'">
          <label>模板选择</label>
          <select v-model="scForm.templateId" class="inp" :disabled="!scForm.enabled">
            <option value="enterprise">企业版（蓝色商务风）</option>
            <option value="studio">工作室版（紫色创意风）</option>
            <option value="freelancer">自由职业版（青色简约风）</option>
          </select>
          <label>展示内容 JSON</label>
          <p class="hint">JSON 格式，包含 teamName、slogan、description、services、team、works、reviews 等字段。留空使用模板默认内容。</p>
          <textarea v-model="scForm.contentJson" class="inp textarea" rows="10" placeholder='{"teamName":"示例科技","slogan":"专业软件定制","services":[...]}' :disabled="!scForm.enabled"></textarea>
        </template>
        <template v-if="scForm.mode === 'custom_html'">
          <label>自定义 HTML 代码</label>
          <p class="hint">完整的 HTML 代码，将在 iframe 中以 sandbox 模式渲染。最大 50000 字符。</p>
          <textarea v-model="scForm.customHtml" class="inp textarea" rows="14" placeholder="<html><body>...</body></html>" :disabled="!scForm.enabled"></textarea>
          <p v-if="scForm.customHtml && scForm.customHtml.length > 50000" class="err">HTML 内容超出 50000 字符限制</p>
        </template>
        <div class="btn-row">
          <button type="submit" class="btn primary" :disabled="scSaving || !scForm.enabled">{{ scSaving ? '保存中…' : '保存展示配置' }}</button>
          <span v-if="scMsg" :class="scOk ? 'ok' : 'err'">{{ scMsg }}</span>
        </div>
      </form>
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

    <section class="panel">
      <h3>项目信息发布</h3>
      <p class="muted small">控制租户控制台「项目信息发布」模块是否可用；配置存于全局 <code class="mono">tenant_id=0</code>。</p>
      <div v-if="pmLoading" class="muted">加载中…</div>
      <form v-else class="form" @submit.prevent="saveProjectMarketplace">
        <label class="chk"><input v-model="pmForm.enabled" type="checkbox"> 启用模块总开关</label>
        <label>范围 scope</label>
        <select v-model="pmForm.scope" class="inp">
          <option value="off">off（关闭）</option>
          <option value="all">all（全平台，仍可按下方用户白名单限制）</option>
          <option value="tenant_whitelist">tenant_whitelist（仅白名单租户）</option>
          <option value="user_whitelist">user_whitelist（仅白名单管理员用户 ID）</option>
        </select>
        <label>租户 ID 白名单（逗号分隔）</label>
        <input v-model="pmForm.tenantIdsStr" type="text" class="inp mono" placeholder="例如：1,2,3">
        <label>管理员用户 ID 白名单（逗号分隔，空表示不额外限制用户）</label>
        <input v-model="pmForm.userIdsStr" type="text" class="inp mono" placeholder="可选">
        <label class="chk"><input v-model="pmForm.allowGuestBrowseList" type="checkbox"> 允许未登录访客调用公开状态接口（仅探测，不含列表数据）</label>
        <label class="chk"><input v-model="pmForm.requireContentReview" type="checkbox"> 先审后发（新发布进入待审核）</label>
        <label>免责文案版本号</label>
        <input v-model="pmForm.disclaimerVersion" type="text" class="inp mono">
        <p v-if="pmCounts" class="hint">当前白名单规模：租户 {{ pmCounts.tenantWhitelistCount }}，用户 {{ pmCounts.userWhitelistCount }}</p>
        <div class="btn-row">
          <button type="submit" class="btn primary" :disabled="pmSaving">{{ pmSaving ? '保存中…' : '保存' }}</button>
          <span v-if="pmMsg" :class="pmOk ? 'ok' : 'err'">{{ pmMsg }}</span>
        </div>
      </form>
    </section>

    <section class="panel">
      <h3>官方 API 池</h3>
      <p class="muted small">为租户代付 DeepSeek / 通义（DashScope）调用：开启后，在租户有余额时优先走平台密钥；计费按下方单价从租户「官方额度」扣减。新用户注册默认赠送额度见应用配置 <code class="mono">app.official-ai.registration-grant-cny</code>。</p>
      <div v-if="aiPoolLoading" class="muted">加载中…</div>
      <form v-else class="form" @submit.prevent="saveAiPool">
        <label class="chk"><input v-model="aiPoolForm.platformPoolEnabled" type="checkbox"> 启用平台官方代付（总开关，与后端 <code class="mono">app.official-ai.enabled</code> 同时生效）</label>
        <label>DeepSeek API Key（平台）</label>
        <input v-model="aiPoolForm.deepseekApiKey" type="password" class="inp mono" autocomplete="off" placeholder="留空则不修改已保存密钥">
        <p v-if="aiPool.deepseekApiKeyMasked" class="hint">已设置：{{ aiPool.deepseekApiKeyMasked }}</p>
        <label>通义 / DashScope API Key（平台）</label>
        <input v-model="aiPoolForm.qwenApiKey" type="password" class="inp mono" autocomplete="off" placeholder="留空则不修改已保存密钥">
        <p v-if="aiPool.qwenApiKeyMasked" class="hint">已设置：{{ aiPool.qwenApiKeyMasked }}</p>
        <div class="btn-row">
          <button type="submit" class="btn primary" :disabled="aiPoolSaving">{{ aiPoolSaving ? '保存中…' : '保存官方池' }}</button>
          <span v-if="aiPoolMsg" :class="aiPoolOk ? 'ok' : 'err'">{{ aiPoolMsg }}</span>
        </div>
      </form>

      <h4 class="subh">生成额度兑换码</h4>
      <p class="muted small">生成后仅显示一次明文码；租户在控制台「会员与计费」中兑换。</p>
      <form class="form form-row" @submit.prevent="generateRedeemCode">
        <label>面额（元）</label>
        <input v-model.number="redeemForm.grantCny" type="number" step="0.01" min="0.01" class="inp narrow">
        <label>最大使用次数</label>
        <input v-model.number="redeemForm.maxUses" type="number" min="1" class="inp narrow">
        <label>过期时间（ISO，可空）</label>
        <input v-model="redeemForm.expiresAt" type="text" class="inp mono" placeholder="2026-12-31T23:59:59">
        <label>备注</label>
        <input v-model="redeemForm.note" type="text" class="inp">
        <div class="btn-row">
          <button type="submit" class="btn" :disabled="redeemGenerating">{{ redeemGenerating ? '生成中…' : '生成兑换码' }}</button>
        </div>
      </form>
      <p v-if="lastPlainCode" class="ok mono">明文码（请立即复制）：{{ lastPlainCode }}</p>

      <h4 class="subh">租户官方消耗排行（近 {{ usageDays }} 天）</h4>
      <div class="btn-row">
        <button type="button" class="btn" @click="loadUsageByTenant">刷新排行</button>
      </div>
      <table v-if="usageByTenant.length" class="tbl">
        <thead>
          <tr><th>租户 ID</th><th>官方消耗（元）</th></tr>
        </thead>
        <tbody>
          <tr v-for="row in usageByTenant" :key="row.tenantId">
            <td>{{ row.tenantId }}</td>
            <td>{{ formatYuan(row.officialCostYuan) }}</td>
          </tr>
        </tbody>
      </table>
      <p v-else class="muted small">暂无数据或尚未产生官方池调用。</p>

      <h4 class="subh">最近兑换码（脱敏）</h4>
      <table v-if="redeemCodes.length" class="tbl">
        <thead>
          <tr><th>ID</th><th>面额</th><th>已用/上限</th><th>过期</th><th>创建时间</th></tr>
        </thead>
        <tbody>
          <tr v-for="c in redeemCodes" :key="c.id">
            <td>{{ c.id }}</td>
            <td>{{ c.grantCny }}</td>
            <td>{{ c.usedCount }} / {{ c.maxUses }}</td>
            <td>{{ c.expiresAt || '—' }}</td>
            <td>{{ c.createdAt }}</td>
          </tr>
        </tbody>
      </table>
      <p v-else class="muted small">暂无记录。</p>
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
      notifyLoading: true,
      notifySaving: false,
      notifyMsg: '',
      notifyOk: true,
      notifyForm: {
        enabled: false,
        email: ''
      },
      rmLoading: true,
      rmSaving: false,
      rmMsg: '',
      rmOk: true,
      rmForm: {
        enabled: true,
        cron: '0 30 4 * * *',
        timezone: 'Asia/Shanghai'
      },
      aiPoolLoading: true,
      aiPoolSaving: false,
      aiPoolMsg: '',
      aiPoolOk: true,
      aiPool: {},
      aiPoolForm: {
        platformPoolEnabled: true,
        deepseekApiKey: '',
        qwenApiKey: ''
      },
      redeemForm: {
        grantCny: 20,
        maxUses: 1,
        expiresAt: '',
        note: ''
      },
      redeemGenerating: false,
      lastPlainCode: '',
      redeemCodes: [],
      usageByTenant: [],
      usageDays: 30,
      pmLoading: true,
      pmSaving: false,
      pmMsg: '',
      pmOk: true,
      pmCounts: null,
      pmForm: {
        enabled: false,
        scope: 'off',
        tenantIdsStr: '',
        userIdsStr: '',
        allowGuestBrowseList: false,
        requireContentReview: true,
        disclaimerVersion: '2026-04-01'
      },

      csLoading: true,
      csSaving: false,
      csMsg: '',
      csOk: true,
      csForm: {
        minSupportedVersion: '',
        blockedVersionsCsv: '',
        blockedBuildsCsv: '',
        blockMessage: '',
        upgradeUrl: '',
        githubRepo: '',
        releaseTagPrefix: 'app-v',
        latestVersion: '',
        windowsUrl: '',
        linuxDebUrl: '',
        androidApkUrl: '',
        iosNoteUrl: ''
      },

      scLoading: true,
      scSaving: false,
      scMsg: '',
      scOk: true,
      scForm: {
        enabled: false,
        mode: 'template',
        templateId: 'enterprise',
        contentJson: '',
        customHtml: ''
      }
    }
  },
  mounted () {
    this.loadAll()
  },
  methods: {
    async loadAll () {
      await Promise.all([
        this.loadMail(),
        this.loadQuickQuoteNotify(),
        this.loadReportMail(),
        this.loadProjectMarketplace(),
        this.loadAiPool(),
        this.loadRedeemCodes(),
        this.loadUsageByTenant(),
        this.loadClientShell(),
        this.loadShowcase()
      ])
    },
    parseIdList (s) {
      if (!s || !String(s).trim()) return []
      return String(s).split(/[,，\s]+/).map(x => x.trim()).filter(Boolean).map(x => {
        const n = Number(x)
        return Number.isFinite(n) ? n : null
      }).filter(x => x != null)
    },
    async loadProjectMarketplace () {
      this.pmLoading = true
      try {
        const { data } = await http.get('/platform/settings/project-marketplace')
        if (data.code === 0 && data.data) {
          const d = data.data
          this.pmCounts = { tenantWhitelistCount: d.tenantWhitelistCount, userWhitelistCount: d.userWhitelistCount }
          this.pmForm.enabled = !!d.enabled
          this.pmForm.scope = d.scope || 'off'
          const tids = Array.isArray(d.tenantIds) ? d.tenantIds : []
          const uids = Array.isArray(d.userIds) ? d.userIds : []
          this.pmForm.tenantIdsStr = tids.join(', ')
          this.pmForm.userIdsStr = uids.join(', ')
          this.pmForm.allowGuestBrowseList = !!d.allowGuestBrowseList
          this.pmForm.requireContentReview = d.requireContentReview !== false
          this.pmForm.disclaimerVersion = d.disclaimerVersion || '2026-04-01'
        }
      } catch (e) {
        this.pmMsg = '加载失败'
        this.pmOk = false
      } finally {
        this.pmLoading = false
      }
    },
    async saveProjectMarketplace () {
      this.pmSaving = true
      this.pmMsg = ''
      try {
        const body = {
          enabled: !!this.pmForm.enabled,
          scope: this.pmForm.scope,
          tenantIds: this.parseIdList(this.pmForm.tenantIdsStr),
          userIds: this.parseIdList(this.pmForm.userIdsStr),
          allowGuestBrowseList: !!this.pmForm.allowGuestBrowseList,
          requireContentReview: !!this.pmForm.requireContentReview,
          disclaimerVersion: (this.pmForm.disclaimerVersion || '').trim() || '2026-04-01'
        }
        const { data } = await http.put('/platform/settings/project-marketplace', body)
        if (data.code === 0) {
          this.pmMsg = '已保存'
          this.pmOk = true
          await this.loadProjectMarketplace()
        } else {
          this.pmMsg = data.message || '保存失败'
          this.pmOk = false
        }
      } catch (e) {
        this.pmMsg = '请求失败'
        this.pmOk = false
      } finally {
        this.pmSaving = false
      }
    },
    async loadClientShell () {
      this.csLoading = true
      try {
        const { data } = await http.get('/platform/settings/client-shell')
        if (data.code === 0 && data.data) {
          const pol = data.data.policy || {}
          const dl = data.data.downloads || {}
          this.csForm.minSupportedVersion = pol.minSupportedVersion || ''
          this.csForm.blockedVersionsCsv = Array.isArray(pol.blockedVersions) ? pol.blockedVersions.join(',') : ''
          this.csForm.blockedBuildsCsv = Array.isArray(pol.blockedBuilds) ? pol.blockedBuilds.join(',') : ''
          this.csForm.blockMessage = pol.blockMessage || ''
          this.csForm.upgradeUrl = pol.upgradeUrl || ''
          this.csForm.githubRepo = dl.githubRepo || ''
          this.csForm.releaseTagPrefix = dl.releaseTagPrefix || 'app-v'
          this.csForm.latestVersion = dl.latestVersion || ''
          this.csForm.windowsUrl = dl.windowsUrl || ''
          this.csForm.linuxDebUrl = dl.linuxDebUrl || ''
          this.csForm.androidApkUrl = dl.androidApkUrl || ''
          this.csForm.iosNoteUrl = dl.iosNoteUrl || ''
        }
      } catch (e) {
        this.csMsg = '壳客户端配置加载失败'
        this.csOk = false
      } finally {
        this.csLoading = false
      }
    },
    async saveClientShell () {
      this.csSaving = true
      this.csMsg = ''
      const blockedVersions = (this.csForm.blockedVersionsCsv || '').split(/[,，]/).map(s => s.trim()).filter(Boolean)
      const blockedBuilds = (this.csForm.blockedBuildsCsv || '').split(/[,，]/).map(s => s.trim()).filter(Boolean).map(s => parseInt(s, 10)).filter(n => !isNaN(n))
      const policy = {
        minSupportedVersion: this.csForm.minSupportedVersion || '',
        blockedVersions,
        blockedBuilds,
        blockMessage: this.csForm.blockMessage || '',
        upgradeUrl: this.csForm.upgradeUrl || ''
      }
      const downloads = {
        githubRepo: this.csForm.githubRepo || '',
        releaseTagPrefix: this.csForm.releaseTagPrefix || 'app-v',
        latestVersion: this.csForm.latestVersion || '',
        windowsUrl: this.csForm.windowsUrl || '',
        linuxDebUrl: this.csForm.linuxDebUrl || '',
        androidApkUrl: this.csForm.androidApkUrl || '',
        iosNoteUrl: this.csForm.iosNoteUrl || ''
      }
      try {
        const { data } = await http.put('/platform/settings/client-shell', { policy, downloads })
        if (data.code === 0) {
          this.csMsg = '已保存'
          this.csOk = true
          await this.loadClientShell()
        } else {
          this.csMsg = data.message || '保存失败'
          this.csOk = false
        }
      } catch (e) {
        this.csMsg = '请求失败'
        this.csOk = false
      } finally {
        this.csSaving = false
      }
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
    // ===== 来单邮件通知 =====
    async loadQuickQuoteNotify () {
      this.notifyLoading = true
      try {
        const { data } = await http.get('/platform/settings/quick-quote-notify')
        if (data.code === 0 && data.data) {
          this.notifyForm.enabled = !!data.data.enabled
          this.notifyForm.email = data.data.email || ''
        }
      } catch (e) { void e }
      finally { this.notifyLoading = false }
    },
    async saveQuickQuoteNotify () {
      this.notifySaving = true
      this.notifyMsg = ''
      try {
        const { data } = await http.put('/platform/settings/quick-quote-notify', {
          enabled: this.notifyForm.enabled,
          email: this.notifyForm.email
        })
        if (data.code === 0) {
          this.notifyMsg = '已保存'
          this.notifyOk = true
        } else {
          this.notifyMsg = (data && data.message) || '保存失败'
          this.notifyOk = false
        }
      } catch (e) {
        this.notifyMsg = '请求失败'
        this.notifyOk = false
      } finally {
        this.notifySaving = false
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
    async loadAiPool () {
      this.aiPoolLoading = true
      try {
        const { data } = await http.get('/platform/official-ai/pool')
        if (data.code === 0 && data.data) {
          this.aiPool = data.data
          this.aiPoolForm.platformPoolEnabled = data.data.platformPoolEnabled !== false
          this.aiPoolForm.deepseekApiKey = ''
          this.aiPoolForm.qwenApiKey = ''
        }
      } catch (e) {
        this.aiPoolMsg = '官方池加载失败'
        this.aiPoolOk = false
      } finally {
        this.aiPoolLoading = false
      }
    },
    async saveAiPool () {
      this.aiPoolSaving = true
      this.aiPoolMsg = ''
      try {
        const payload = {
          platformPoolEnabled: this.aiPoolForm.platformPoolEnabled
        }
        if (this.aiPoolForm.deepseekApiKey && !this.aiPoolForm.deepseekApiKey.includes('****')) {
          payload.deepseekApiKey = this.aiPoolForm.deepseekApiKey
        }
        if (this.aiPoolForm.qwenApiKey && !this.aiPoolForm.qwenApiKey.includes('****')) {
          payload.qwenApiKey = this.aiPoolForm.qwenApiKey
        }
        const { data } = await http.put('/platform/official-ai/pool', payload)
        if (data.code === 0) {
          this.aiPoolMsg = '已保存'
          this.aiPoolOk = true
          this.aiPoolForm.deepseekApiKey = ''
          this.aiPoolForm.qwenApiKey = ''
          await this.loadAiPool()
        } else {
          this.aiPoolMsg = data.message || '保存失败'
          this.aiPoolOk = false
        }
      } catch (e) {
        this.aiPoolMsg = '请求失败'
        this.aiPoolOk = false
      } finally {
        this.aiPoolSaving = false
      }
    },
    async loadRedeemCodes () {
      try {
        const { data } = await http.get('/platform/official-ai/redeem-codes', { params: { limit: 30 } })
        if (data.code === 0 && Array.isArray(data.data)) {
          this.redeemCodes = data.data
        }
      } catch (e) { /* ignore */ }
    },
    async generateRedeemCode () {
      this.redeemGenerating = true
      this.lastPlainCode = ''
      try {
        const body = {
          grantCny: this.redeemForm.grantCny,
          maxUses: this.redeemForm.maxUses,
          note: this.redeemForm.note || ''
        }
        if ((this.redeemForm.expiresAt || '').trim()) {
          body.expiresAt = this.redeemForm.expiresAt.trim()
        }
        const { data } = await http.post('/platform/official-ai/redeem-codes', body)
        if (data.code === 0 && data.data && data.data.code) {
          this.lastPlainCode = data.data.code
          await this.loadRedeemCodes()
        } else {
          this.lastPlainCode = ''
          alert((data && data.message) || '生成失败')
        }
      } catch (e) {
        alert('请求失败')
      } finally {
        this.redeemGenerating = false
      }
    },
    async loadUsageByTenant () {
      try {
        const { data } = await http.get('/platform/official-ai/usage/by-tenant', { params: { days: this.usageDays, limit: 50 } })
        if (data.code === 0 && Array.isArray(data.data)) {
          this.usageByTenant = data.data
        }
      } catch (e) {
        this.usageByTenant = []
      }
    },
    formatYuan (v) {
      if (v == null || v === '') return '—'
      const n = Number(v)
      if (!Number.isFinite(n)) return '—'
      return n.toFixed(4)
    },
    // ===== 团队展示区配置 =====
    async loadShowcase () {
      this.scLoading = true
      try {
        const { data } = await http.get('/platform/settings/showcase')
        if (data.code === 0 && data.data) {
          const d = data.data
          this.scForm.enabled = !!d.enabled
          this.scForm.mode = d.mode || 'template'
          this.scForm.templateId = d.templateId || 'enterprise'
          this.scForm.contentJson = d.contentJson || ''
          this.scForm.customHtml = d.customHtml || ''
        }
      } catch (e) { void e }
      finally { this.scLoading = false }
    },
    async saveShowcase () {
      this.scSaving = true
      this.scMsg = ''
      try {
        const body = {
          enabled: !!this.scForm.enabled,
          mode: this.scForm.mode,
          templateId: this.scForm.templateId,
          contentJson: (this.scForm.contentJson || '').trim(),
          customHtml: (this.scForm.customHtml || '').trim()
        }
        const { data } = await http.put('/platform/settings/showcase', body)
        if (data.code === 0) {
          this.scMsg = '已保存'
          this.scOk = true
        } else {
          this.scMsg = (data && data.message) || '保存失败'
          this.scOk = false
        }
      } catch (e) {
        this.scMsg = '请求失败'
        this.scOk = false
      } finally {
        this.scSaving = false
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
.subh { margin: 16px 0 8px; font-size: 14px; font-weight: 600; }
.tbl { width: 100%; border-collapse: collapse; font-size: 13px; margin-top: 8px; }
.tbl th, .tbl td { padding: 6px 8px; border-bottom: 1px solid #1e293b; text-align: left; }
.form-row { display: grid; grid-template-columns: repeat(auto-fit, minmax(160px, 1fr)); gap: 8px; align-items: end; }
.textarea { min-height: 120px; resize: vertical; font-family: ui-monospace, monospace; font-size: 13px; line-height: 1.5; }
</style>
