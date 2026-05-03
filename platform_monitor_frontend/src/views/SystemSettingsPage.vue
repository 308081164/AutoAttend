<template>
  <div class="system-settings-page">
    <div class="page-header">
      <h2 class="page-title">系统配置</h2>
      <p class="page-desc">平台级设置（与租户无关）：SMTP 发信、日报邮件定时任务、官方 API 池。租户控制台不再提供邮件表单；未配置 SMTP 时相关能力不可用，不影响其他功能。</p>
    </div>

    <!-- 壳客户端配置 -->
    <el-card shadow="hover" class="section-card">
      <div slot="header" class="section-header">
        <span>壳客户端（Flutter / WebView）</span>
        <el-tag size="small" type="info" effect="plain">最低版本、黑名单与下载直链保存在平台配置</el-tag>
      </div>
      <el-form v-if="!csLoading" :model="csForm" label-position="top" size="small" @submit.native.prevent="saveClientShell">
        <el-divider content-position="left">版本策略</el-divider>
        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item label="最低支持版本（semver，如 1.0.0；留空表示不限制）">
              <el-input v-model="csForm.minSupportedVersion" placeholder="1.0.0" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="禁止的版本号（逗号分隔）">
              <el-input v-model="csForm.blockedVersionsCsv" placeholder="0.9.0,1.0.0" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="禁止的构建号（逗号分隔整数）">
              <el-input v-model="csForm.blockedBuildsCsv" placeholder="" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="拦截提示文案">
              <el-input v-model="csForm.blockMessage" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="升级包 / Release 页面 URL">
              <el-input v-model="csForm.upgradeUrl" placeholder="https://github.com/.../releases/..." />
            </el-form-item>
          </el-col>
        </el-row>
        <el-divider content-position="left">官网下载信息（GitHub Releases）</el-divider>
        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item label="GitHub 仓库（owner/repo）">
              <el-input v-model="csForm.githubRepo" placeholder="308081164/AutoAttend" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="Release 标签前缀">
              <el-input v-model="csForm.releaseTagPrefix" placeholder="app-v" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="当前展示版本号">
              <el-input v-model="csForm.latestVersion" placeholder="1.0.0" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="6">
            <el-form-item label="Windows 安装包直链">
              <el-input v-model="csForm.windowsUrl" placeholder="可选" />
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="Linux deb 直链">
              <el-input v-model="csForm.linuxDebUrl" placeholder="可选" />
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="Android APK 直链">
              <el-input v-model="csForm.androidApkUrl" placeholder="可选" />
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="iOS 说明页 / TestFlight">
              <el-input v-model="csForm.iosNoteUrl" placeholder="可选" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item>
          <el-button type="primary" :loading="csSaving" @click="saveClientShell">保存壳客户端配置</el-button>
          <span v-if="csMsg" :class="csOk ? 'success-text' : 'error-text'" style="margin-left: 12px;">{{ csMsg }}</span>
        </el-form-item>
      </el-form>
      <el-skeleton v-else :count="3" animated />
    </el-card>

    <!-- SMTP 发信 -->
    <el-card shadow="hover" class="section-card">
      <div slot="header" class="section-header">
        <span>SMTP 发信</span>
        <el-tag size="small" type="info" effect="plain">用于项目日报、开发者日报等</el-tag>
      </div>
      <el-form v-if="!mailLoading" :model="mailForm" label-position="top" size="small" @submit.native.prevent="saveMail">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="系统公网基址（邮件内链接）">
              <el-input v-model="mailForm.publicBaseUrl" placeholder="https://your-domain.com" />
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="SMTP Host">
              <el-input v-model="mailForm.smtpHost" placeholder="smtp.example.com" />
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="SMTP Port">
              <el-input-number v-model="mailForm.smtpPort" :min="1" :max="65535" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item label="SMTP 用户名">
              <el-input v-model="mailForm.smtpUsername" autocomplete="off" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="SMTP 密码">
              <el-input v-model="mailForm.smtpPassword" type="password" autocomplete="off" placeholder="留空则不修改" />
              <span v-if="mail.smtpPasswordMasked" class="hint-text">已设置密码，留空表示不修改</span>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="发件人邮箱">
              <el-input v-model="mailForm.fromAddress" placeholder="noreply@example.com" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item label="发件人显示名">
              <el-input v-model="mailForm.fromName" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item>
          <el-button type="primary" :loading="mailSaving" @click="saveMail">保存 SMTP</el-button>
          <span v-if="mailMsg" :class="mailOk ? 'success-text' : 'error-text'" style="margin-left: 12px;">{{ mailMsg }}</span>
          <el-tag v-if="mail.configured" type="success" size="small" style="margin-left: 8px;">当前 SMTP 已配齐</el-tag>
        </el-form-item>
      </el-form>
      <el-skeleton v-else :count="3" animated />

      <el-divider />
      <div class="test-block">
        <span class="test-label">测试收件邮箱</span>
        <div class="test-row">
          <el-input v-model="testToEmail" placeholder="your@email.com" style="width: 300px;" size="small" />
          <el-button :loading="testSending" size="small" @click="sendTestMail">发送测试邮件</el-button>
        </div>
        <p v-if="testMsg" :class="testOk ? 'success-text' : 'error-text'" style="margin-top: 8px;">{{ testMsg }}</p>
      </div>
    </el-card>

    <!-- 来单邮件通知 -->
    <el-card shadow="hover" class="section-card">
      <div slot="header" class="section-header">
        <span>📬 来单邮件通知</span>
        <el-tag size="small" type="info" effect="plain">客户通过 Agent 链接创建报价项目后自动通知管理员</el-tag>
      </div>
      <el-form v-if="!notifyLoading" :model="notifyForm" label-position="top" size="small" @submit.native.prevent="saveQuickQuoteNotify">
        <el-form-item>
          <el-switch v-model="notifyForm.enabled" active-text="启用来单邮件通知" />
        </el-form-item>
        <el-form-item label="通知邮箱">
          <el-input v-model="notifyForm.email" placeholder="admin@company.com" :disabled="!notifyForm.enabled" style="max-width: 360px;" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="notifySaving" @click="saveQuickQuoteNotify">保存通知设置</el-button>
          <span v-if="notifyMsg" :class="notifyOk ? 'success-text' : 'error-text'" style="margin-left: 12px;">{{ notifyMsg }}</span>
        </el-form-item>
      </el-form>
      <el-skeleton v-else :count="2" animated />
    </el-card>

    <!-- 团队展示区配置 -->
    <el-card shadow="hover" class="section-card">
      <div slot="header" class="section-header">
        <span>🏢 团队展示区配置</span>
        <el-tag size="small" type="info" effect="plain">支持 3 套官方模板和自定义 HTML 模式</el-tag>
      </div>
      <el-form v-if="!scLoading" :model="scForm" label-position="top" size="small" @submit.native.prevent="saveShowcase">
        <el-form-item>
          <el-switch v-model="scForm.enabled" active-text="启用团队展示区" />
        </el-form-item>
        <el-form-item label="展示模式" v-if="scForm.enabled">
          <el-radio-group v-model="scForm.mode">
            <el-radio label="template">官方模板</el-radio>
            <el-radio label="custom_html">自定义 HTML</el-radio>
          </el-radio-group>
        </el-form-item>
        <template v-if="scForm.enabled && scForm.mode === 'template'">
          <el-form-item label="模板选择">
            <el-select v-model="scForm.templateId" style="width: 240px;">
              <el-option label="企业版（蓝色商务风）" value="enterprise" />
              <el-option label="工作室版（紫色创意风）" value="studio" />
              <el-option label="自由职业版（青色简约风）" value="freelancer" />
            </el-select>
          </el-form-item>
          <el-form-item label="展示内容 JSON">
            <p class="hint-text">JSON 格式，包含 teamName、slogan、description、services、team、works、reviews 等字段。留空使用模板默认内容。</p>
            <el-input v-model="scForm.contentJson" type="textarea" :rows="6" placeholder='{"teamName":"示例科技","slogan":"专业软件定制","services":[...]}' />
          </el-form-item>
        </template>
        <template v-if="scForm.enabled && scForm.mode === 'custom_html'">
          <el-form-item label="自定义 HTML 代码">
            <p class="hint-text">完整的 HTML 代码，将在 iframe 中以 sandbox 模式渲染。最大 50000 字符。</p>
            <el-input v-model="scForm.customHtml" type="textarea" :rows="10" placeholder="<html><body>...</body></html>" />
            <p v-if="scForm.customHtml && scForm.customHtml.length > 50000" class="error-text">HTML 内容超出 50000 字符限制</p>
          </el-form-item>
        </template>
        <el-form-item>
          <el-button type="primary" :loading="scSaving" :disabled="!scForm.enabled" @click="saveShowcase">保存展示配置</el-button>
          <span v-if="scMsg" :class="scOk ? 'success-text' : 'error-text'" style="margin-left: 12px;">{{ scMsg }}</span>
        </el-form-item>
      </el-form>
      <el-skeleton v-else :count="2" animated />
    </el-card>

    <!-- 日报邮件调度 -->
    <el-card shadow="hover" class="section-card">
      <div slot="header" class="section-header">
        <span>日报邮件调度</span>
        <el-tag size="small" type="info" effect="plain">Cron 使用 Spring 6 域表达式，保存后立即重载调度</el-tag>
      </div>
      <el-form v-if="!rmLoading" :model="rmForm" label-position="top" size="small" @submit.native.prevent="saveReportMail">
        <el-form-item>
          <el-switch v-model="rmForm.enabled" active-text="启用定时发送昨天日报" />
        </el-form-item>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="Cron">
              <el-input v-model="rmForm.cron" placeholder="0 30 4 * * *" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="时区">
              <el-input v-model="rmForm.timezone" placeholder="Asia/Shanghai" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item>
          <el-button type="primary" :loading="rmSaving" @click="saveReportMail">保存调度</el-button>
          <span v-if="rmMsg" :class="rmOk ? 'success-text' : 'error-text'" style="margin-left: 12px;">{{ rmMsg }}</span>
        </el-form-item>
      </el-form>
      <el-skeleton v-else :count="2" animated />
    </el-card>

    <!-- 项目信息发布 -->
    <el-card shadow="hover" class="section-card">
      <div slot="header" class="section-header">
        <span>项目信息发布</span>
        <el-tag size="small" type="info" effect="plain">控制租户控制台「项目信息发布」模块是否可用</el-tag>
      </div>
      <el-form v-if="!pmLoading" :model="pmForm" label-position="top" size="small" @submit.native.prevent="saveProjectMarketplace">
        <el-form-item>
          <el-switch v-model="pmForm.enabled" active-text="启用模块总开关" />
        </el-form-item>
        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item label="范围 scope">
              <el-select v-model="pmForm.scope" style="width: 100%;">
                <el-option label="off（关闭）" value="off" />
                <el-option label="all（全平台）" value="all" />
                <el-option label="tenant_whitelist（仅白名单租户）" value="tenant_whitelist" />
                <el-option label="user_whitelist（仅白名单管理员用户 ID）" value="user_whitelist" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="租户 ID 白名单（逗号分隔）">
              <el-input v-model="pmForm.tenantIdsStr" placeholder="例如：1,2,3" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="管理员用户 ID 白名单（逗号分隔）">
              <el-input v-model="pmForm.userIdsStr" placeholder="可选" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item>
              <el-checkbox v-model="pmForm.allowGuestBrowseList">允许未登录访客调用公开状态接口</el-checkbox>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item>
              <el-checkbox v-model="pmForm.requireContentReview">先审后发（新发布进入待审核）</el-checkbox>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="免责文案版本号">
              <el-input v-model="pmForm.disclaimerVersion" />
            </el-form-item>
          </el-col>
        </el-row>
        <p v-if="pmCounts" class="hint-text">当前白名单规模：租户 {{ pmCounts.tenantWhitelistCount }}，用户 {{ pmCounts.userWhitelistCount }}</p>
        <el-form-item>
          <el-button type="primary" :loading="pmSaving" @click="saveProjectMarketplace">保存</el-button>
          <span v-if="pmMsg" :class="pmOk ? 'success-text' : 'error-text'" style="margin-left: 12px;">{{ pmMsg }}</span>
        </el-form-item>
      </el-form>
      <el-skeleton v-else :count="3" animated />
    </el-card>

    <!-- 官方 API 池 -->
    <el-card shadow="hover" class="section-card">
      <div slot="header" class="section-header">
        <span>官方 API 池</span>
        <el-tag size="small" type="info" effect="plain">为租户代付 DeepSeek / 通义（DashScope）调用</el-tag>
      </div>
      <el-form v-if="!aiPoolLoading" :model="aiPoolForm" label-position="top" size="small" @submit.native.prevent="saveAiPool">
        <el-form-item>
          <el-switch v-model="aiPoolForm.platformPoolEnabled" active-text="启用平台官方代付（总开关）" />
        </el-form-item>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="DeepSeek API Key（平台）">
              <el-input v-model="aiPoolForm.deepseekApiKey" type="password" autocomplete="off" placeholder="留空则不修改已保存密钥" />
              <p v-if="aiPool.deepseekApiKeyMasked" class="hint-text">已设置：{{ aiPool.deepseekApiKeyMasked }}</p>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="通义 / DashScope API Key（平台）">
              <el-input v-model="aiPoolForm.qwenApiKey" type="password" autocomplete="off" placeholder="留空则不修改已保存密钥" />
              <p v-if="aiPool.qwenApiKeyMasked" class="hint-text">已设置：{{ aiPool.qwenApiKeyMasked }}</p>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item>
          <el-button type="primary" :loading="aiPoolSaving" @click="saveAiPool">保存官方池</el-button>
          <span v-if="aiPoolMsg" :class="aiPoolOk ? 'success-text' : 'error-text'" style="margin-left: 12px;">{{ aiPoolMsg }}</span>
        </el-form-item>
      </el-form>
      <el-skeleton v-else :count="2" animated />

      <el-divider />
      <h4 class="sub-title">生成额度兑换码</h4>
      <p class="hint-text">生成后仅显示一次明文码；租户在控制台「会员与计费」中兑换。</p>
      <el-form :model="redeemForm" label-position="top" size="small" class="redeem-form">
        <el-row :gutter="16">
          <el-col :span="6">
            <el-form-item label="面额（元）">
              <el-input-number v-model="redeemForm.grantCny" :min="0.01" :step="1" :precision="2" style="width: 100%;" />
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="最大使用次数">
              <el-input-number v-model="redeemForm.maxUses" :min="1" style="width: 100%;" />
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="过期时间（ISO，可空）">
              <el-input v-model="redeemForm.expiresAt" placeholder="2026-12-31T23:59:59" />
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="备注">
              <el-input v-model="redeemForm.note" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item>
          <el-button :loading="redeemGenerating" @click="generateRedeemCode">生成兑换码</el-button>
        </el-form-item>
      </el-form>
      <p v-if="lastPlainCode" class="success-text mono-text">明文码（请立即复制）：{{ lastPlainCode }}</p>

      <el-divider />
      <h4 class="sub-title">租户官方消耗排行（近 {{ usageDays }} 天）</h4>
      <el-button size="small" style="margin-bottom: 12px;" @click="loadUsageByTenant">刷新排行</el-button>
      <el-table v-if="usageByTenant.length" :data="usageByTenant" stripe size="small" style="width: 100%; max-width: 400px;">
        <el-table-column prop="tenantId" label="租户 ID" width="100" />
        <el-table-column label="官方消耗（元）" min-width="120">
          <template slot-scope="{ row }">{{ formatYuan(row.officialCostYuan) }}</template>
        </el-table-column>
      </el-table>
      <p v-else class="hint-text">暂无数据或尚未产生官方池调用。</p>

      <el-divider />
      <h4 class="sub-title">最近兑换码（脱敏）</h4>
      <el-table v-if="redeemCodes.length" :data="redeemCodes" stripe size="small" style="width: 100%;">
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column prop="grantCny" label="面额" width="80" />
        <el-table-column label="已用/上限" width="100">
          <template slot-scope="{ row }">{{ row.usedCount }} / {{ row.maxUses }}</template>
        </el-table-column>
        <el-table-column prop="expiresAt" label="过期" min-width="140">
          <template slot-scope="{ row }">{{ row.expiresAt || '—' }}</template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" min-width="140" />
      </el-table>
      <p v-else class="hint-text">暂无记录。</p>
    </el-card>
  </div>
</template>

<script>
import { http } from '../api/http'

export default {
  name: 'SystemSettingsPage',
  data () {
    return {
      mailLoading: true, mailSaving: false, mailMsg: '', mailOk: true, mail: {},
      mailForm: { publicBaseUrl: '', smtpHost: '', smtpPort: 587, smtpUsername: '', smtpPassword: '', fromAddress: '', fromName: '' },
      testToEmail: '', testSending: false, testMsg: '', testOk: true,
      notifyLoading: true, notifySaving: false, notifyMsg: '', notifyOk: true, notifyForm: { enabled: false, email: '' },
      rmLoading: true, rmSaving: false, rmMsg: '', rmOk: true, rmForm: { enabled: true, cron: '0 30 4 * * *', timezone: 'Asia/Shanghai' },
      aiPoolLoading: true, aiPoolSaving: false, aiPoolMsg: '', aiPoolOk: true, aiPool: {},
      aiPoolForm: { platformPoolEnabled: true, deepseekApiKey: '', qwenApiKey: '' },
      redeemForm: { grantCny: 20, maxUses: 1, expiresAt: '', note: '' },
      redeemGenerating: false, lastPlainCode: '', redeemCodes: [], usageByTenant: [], usageDays: 30,
      pmLoading: true, pmSaving: false, pmMsg: '', pmOk: true, pmCounts: null,
      pmForm: { enabled: false, scope: 'off', tenantIdsStr: '', userIdsStr: '', allowGuestBrowseList: false, requireContentReview: true, disclaimerVersion: '2026-04-01' },
      csLoading: true, csSaving: false, csMsg: '', csOk: true,
      csForm: { minSupportedVersion: '', blockedVersionsCsv: '', blockedBuildsCsv: '', blockMessage: '', upgradeUrl: '', githubRepo: '', releaseTagPrefix: 'app-v', latestVersion: '', windowsUrl: '', linuxDebUrl: '', androidApkUrl: '', iosNoteUrl: '' },
      scLoading: true, scSaving: false, scMsg: '', scOk: true,
      scForm: { enabled: false, mode: 'template', templateId: 'enterprise', contentJson: '', customHtml: '' }
    }
  },
  mounted () { this.loadAll() },
  methods: {
    async loadAll () {
      await Promise.all([
        this.loadMail(), this.loadQuickQuoteNotify(), this.loadReportMail(),
        this.loadProjectMarketplace(), this.loadAiPool(), this.loadRedeemCodes(),
        this.loadUsageByTenant(), this.loadClientShell(), this.loadShowcase()
      ])
    },
    parseIdList (s) {
      if (!s || !String(s).trim()) return []
      return String(s).split(/[,，\s]+/).map(x => x.trim()).filter(Boolean).map(x => {
        const n = Number(x); return Number.isFinite(n) ? n : null
      }).filter(x => x != null)
    },
    async loadProjectMarketplace () {
      this.pmLoading = true
      try {
        const { data } = await http.get('/platform/settings/project-marketplace')
        if (data.code === 0 && data.data) {
          const d = data.data
          this.pmCounts = { tenantWhitelistCount: d.tenantWhitelistCount, userWhitelistCount: d.userWhitelistCount }
          this.pmForm.enabled = !!d.enabled; this.pmForm.scope = d.scope || 'off'
          this.pmForm.tenantIdsStr = (Array.isArray(d.tenantIds) ? d.tenantIds : []).join(', ')
          this.pmForm.userIdsStr = (Array.isArray(d.userIds) ? d.userIds : []).join(', ')
          this.pmForm.allowGuestBrowseList = !!d.allowGuestBrowseList
          this.pmForm.requireContentReview = d.requireContentReview !== false
          this.pmForm.disclaimerVersion = d.disclaimerVersion || '2026-04-01'
        }
      } catch (e) { this.pmMsg = '加载失败'; this.pmOk = false }
      finally { this.pmLoading = false }
    },
    async saveProjectMarketplace () {
      this.pmSaving = true; this.pmMsg = ''
      try {
        const { data } = await http.put('/platform/settings/project-marketplace', {
          enabled: !!this.pmForm.enabled, scope: this.pmForm.scope,
          tenantIds: this.parseIdList(this.pmForm.tenantIdsStr),
          userIds: this.parseIdList(this.pmForm.userIdsStr),
          allowGuestBrowseList: !!this.pmForm.allowGuestBrowseList,
          requireContentReview: !!this.pmForm.requireContentReview,
          disclaimerVersion: (this.pmForm.disclaimerVersion || '').trim() || '2026-04-01'
        })
        if (data.code === 0) { this.pmMsg = '已保存'; this.pmOk = true; await this.loadProjectMarketplace() }
        else { this.pmMsg = data.message || '保存失败'; this.pmOk = false }
      } catch (e) { this.pmMsg = '请求失败'; this.pmOk = false }
      finally { this.pmSaving = false }
    },
    async loadClientShell () {
      this.csLoading = true
      try {
        const { data } = await http.get('/platform/settings/client-shell')
        if (data.code === 0 && data.data) {
          const pol = data.data.policy || {}; const dl = data.data.downloads || {}
          this.csForm.minSupportedVersion = pol.minSupportedVersion || ''
          this.csForm.blockedVersionsCsv = Array.isArray(pol.blockedVersions) ? pol.blockedVersions.join(',') : ''
          this.csForm.blockedBuildsCsv = Array.isArray(pol.blockedBuilds) ? pol.blockedBuilds.join(',') : ''
          this.csForm.blockMessage = pol.blockMessage || ''; this.csForm.upgradeUrl = pol.upgradeUrl || ''
          this.csForm.githubRepo = dl.githubRepo || ''; this.csForm.releaseTagPrefix = dl.releaseTagPrefix || 'app-v'
          this.csForm.latestVersion = dl.latestVersion || ''; this.csForm.windowsUrl = dl.windowsUrl || ''
          this.csForm.linuxDebUrl = dl.linuxDebUrl || ''; this.csForm.androidApkUrl = dl.androidApkUrl || ''
          this.csForm.iosNoteUrl = dl.iosNoteUrl || ''
        }
      } catch (e) { this.csMsg = '壳客户端配置加载失败'; this.csOk = false }
      finally { this.csLoading = false }
    },
    async saveClientShell () {
      this.csSaving = true; this.csMsg = ''
      const blockedVersions = (this.csForm.blockedVersionsCsv || '').split(/[,，]/).map(s => s.trim()).filter(Boolean)
      const blockedBuilds = (this.csForm.blockedBuildsCsv || '').split(/[,，]/).map(s => s.trim()).filter(Boolean).map(s => parseInt(s, 10)).filter(n => !isNaN(n))
      try {
        const { data } = await http.put('/platform/settings/client-shell', {
          policy: { minSupportedVersion: this.csForm.minSupportedVersion || '', blockedVersions, blockedBuilds, blockMessage: this.csForm.blockMessage || '', upgradeUrl: this.csForm.upgradeUrl || '' },
          downloads: { githubRepo: this.csForm.githubRepo || '', releaseTagPrefix: this.csForm.releaseTagPrefix || 'app-v', latestVersion: this.csForm.latestVersion || '', windowsUrl: this.csForm.windowsUrl || '', linuxDebUrl: this.csForm.linuxDebUrl || '', androidApkUrl: this.csForm.androidApkUrl || '', iosNoteUrl: this.csForm.iosNoteUrl || '' }
        })
        if (data.code === 0) { this.csMsg = '已保存'; this.csOk = true; await this.loadClientShell() }
        else { this.csMsg = data.message || '保存失败'; this.csOk = false }
      } catch (e) { this.csMsg = '请求失败'; this.csOk = false }
      finally { this.csSaving = false }
    },
    async loadMail () {
      this.mailLoading = true
      try {
        const { data } = await http.get('/platform/settings/mail')
        if (data.code === 0 && data.data) {
          this.mail = data.data; const d = data.data
          this.mailForm.publicBaseUrl = d.publicBaseUrl || ''
          this.mailForm.smtpHost = d.smtpHost || ''
          this.mailForm.smtpPort = d.smtpPort != null ? Number(d.smtpPort) : 587
          this.mailForm.smtpUsername = d.smtpUsername || ''
          this.mailForm.smtpPassword = ''
          this.mailForm.fromAddress = d.fromAddress || ''
          this.mailForm.fromName = d.fromName || ''
        }
      } catch (e) { this.mailMsg = '加载失败'; this.mailOk = false }
      finally { this.mailLoading = false }
    },
    async saveMail () {
      this.mailSaving = true; this.mailMsg = ''
      try {
        const payload = { publicBaseUrl: this.mailForm.publicBaseUrl || '', smtpHost: this.mailForm.smtpHost || '', smtpPort: this.mailForm.smtpPort != null ? Number(this.mailForm.smtpPort) : null, smtpUsername: this.mailForm.smtpUsername || '', fromAddress: this.mailForm.fromAddress || '', fromName: this.mailForm.fromName || '' }
        if (this.mailForm.smtpPassword && !this.mailForm.smtpPassword.includes('****')) { payload.smtpPassword = this.mailForm.smtpPassword }
        const { data } = await http.put('/platform/settings/mail', payload)
        if (data.code === 0) { this.mailMsg = '已保存'; this.mailOk = true; this.mailForm.smtpPassword = ''; await this.loadMail() }
        else { this.mailMsg = data.message || '保存失败'; this.mailOk = false }
      } catch (e) { this.mailMsg = '请求失败'; this.mailOk = false }
      finally { this.mailSaving = false }
    },
    async sendTestMail () {
      const to = (this.testToEmail || '').trim()
      if (!to) { this.testMsg = '请填写收件邮箱'; this.testOk = false; return }
      this.testSending = true; this.testMsg = ''
      try {
        const { data } = await http.post('/platform/settings/mail/test', { toEmail: to })
        if (data.code === 0 && data.data) { this.testOk = !!data.data.ok; this.testMsg = data.data.message || '' }
        else { this.testOk = false; this.testMsg = (data && data.message) || '失败' }
      } catch (e) { this.testOk = false; this.testMsg = '请求失败' }
      finally { this.testSending = false }
    },
    async loadQuickQuoteNotify () {
      this.notifyLoading = true
      try {
        const { data } = await http.get('/platform/settings/quick-quote-notify')
        if (data.code === 0 && data.data) {
          this.notifyForm.enabled = !!data.data.enabled
          this.notifyForm.email = data.data.email || ''
        }
      } catch (e) { /* ignore */ }
      finally { this.notifyLoading = false }
    },
    async saveQuickQuoteNotify () {
      this.notifySaving = true; this.notifyMsg = ''
      try {
        const { data } = await http.put('/platform/settings/quick-quote-notify', {
          enabled: !!this.notifyForm.enabled,
          email: this.notifyForm.email || ''
        })
        if (data.code === 0) { this.notifyMsg = '已保存'; this.notifyOk = true; await this.loadQuickQuoteNotify() }
        else { this.notifyMsg = data.message || '保存失败'; this.notifyOk = false }
      } catch (e) { this.notifyMsg = '请求失败'; this.notifyOk = false }
      finally { this.notifySaving = false }
    },
    async loadReportMail () {
      this.rmLoading = true
      try {
        const { data } = await http.get('/platform/settings/report-mail')
        if (data.code === 0 && data.data) {
          this.rmForm.enabled = data.data.enabled !== false
          this.rmForm.cron = data.data.cron || '0 30 4 * * *'
          this.rmForm.timezone = data.data.timezone || 'Asia/Shanghai'
        }
      } catch (e) { /* ignore */ }
      finally { this.rmLoading = false }
    },
    async saveReportMail () {
      this.rmSaving = true; this.rmMsg = ''
      try {
        const { data } = await http.put('/platform/settings/report-mail', {
          enabled: !!this.rmForm.enabled,
          cron: this.rmForm.cron || '0 30 4 * * *',
          timezone: this.rmForm.timezone || 'Asia/Shanghai'
        })
        if (data.code === 0) { this.rmMsg = '已保存'; this.rmOk = true; await this.loadReportMail() }
        else { this.rmMsg = data.message || '保存失败'; this.rmOk = false }
      } catch (e) { this.rmMsg = '请求失败'; this.rmOk = false }
      finally { this.rmSaving = false }
    },
    async loadAiPool () {
      this.aiPoolLoading = true
      try {
        const { data } = await http.get('/platform/settings/ai-pool')
        if (data.code === 0 && data.data) {
          this.aiPool = data.data
          this.aiPoolForm.platformPoolEnabled = data.data.platformPoolEnabled !== false
          this.aiPoolForm.deepseekApiKey = ''
          this.aiPoolForm.qwenApiKey = ''
        }
      } catch (e) { /* ignore */ }
      finally { this.aiPoolLoading = false }
    },
    async saveAiPool () {
      this.aiPoolSaving = true; this.aiPoolMsg = ''
      try {
        const payload = { platformPoolEnabled: !!this.aiPoolForm.platformPoolEnabled }
        if (this.aiPoolForm.deepseekApiKey && !this.aiPoolForm.deepseekApiKey.includes('****')) { payload.deepseekApiKey = this.aiPoolForm.deepseekApiKey }
        if (this.aiPoolForm.qwenApiKey && !this.aiPoolForm.qwenApiKey.includes('****')) { payload.qwenApiKey = this.aiPoolForm.qwenApiKey }
        const { data } = await http.put('/platform/settings/ai-pool', payload)
        if (data.code === 0) { this.aiPoolMsg = '已保存'; this.aiPoolOk = true; await this.loadAiPool() }
        else { this.aiPoolMsg = data.message || '保存失败'; this.aiPoolOk = false }
      } catch (e) { this.aiPoolMsg = '请求失败'; this.aiPoolOk = false }
      finally { this.aiPoolSaving = false }
    },
    async generateRedeemCode () {
      this.redeemGenerating = true; this.lastPlainCode = ''
      try {
        const { data } = await http.post('/platform/settings/ai-pool/redeem-codes', {
          grantCny: this.redeemForm.grantCny,
          maxUses: this.redeemForm.maxUses,
          expiresAt: this.redeemForm.expiresAt || null,
          note: this.redeemForm.note || ''
        })
        if (data.code === 0 && data.data) {
          this.lastPlainCode = data.data.plainCode || ''
          await this.loadRedeemCodes()
        } else {
          this.lastPlainCode = '生成失败: ' + (data && data.message)
        }
      } catch (e) { this.lastPlainCode = '请求失败' }
      finally { this.redeemGenerating = false }
    },
    async loadRedeemCodes () {
      try {
        const { data } = await http.get('/platform/settings/ai-pool/redeem-codes')
        if (data.code === 0 && Array.isArray(data.data)) { this.redeemCodes = data.data }
      } catch (e) { /* ignore */ }
    },
    async loadUsageByTenant () {
      try {
        const { data } = await http.get('/platform/settings/ai-pool/usage-by-tenant', { params: { days: this.usageDays } })
        if (data.code === 0 && Array.isArray(data.data)) { this.usageByTenant = data.data }
      } catch (e) { /* ignore */ }
    },
    async loadShowcase () {
      this.scLoading = true
      try {
        const { data } = await http.get('/platform/settings/showcase')
        if (data.code === 0 && data.data) {
          this.scForm.enabled = !!data.data.enabled
          this.scForm.mode = data.data.mode || 'template'
          this.scForm.templateId = data.data.templateId || 'enterprise'
          this.scForm.contentJson = data.data.contentJson || ''
          this.scForm.customHtml = data.data.customHtml || ''
        }
      } catch (e) { /* ignore */ }
      finally { this.scLoading = false }
    },
    async saveShowcase () {
      this.scSaving = true; this.scMsg = ''
      try {
        const { data } = await http.put('/platform/settings/showcase', {
          enabled: !!this.scForm.enabled,
          mode: this.scForm.mode || 'template',
          templateId: this.scForm.templateId || 'enterprise',
          contentJson: this.scForm.contentJson || '',
          customHtml: this.scForm.customHtml || ''
        })
        if (data.code === 0) { this.scMsg = '已保存'; this.scOk = true; await this.loadShowcase() }
        else { this.scMsg = data.message || '保存失败'; this.scOk = false }
      } catch (e) { this.scMsg = '请求失败'; this.scOk = false }
      finally { this.scSaving = false }
    },
    formatYuan (cents) {
      if (cents == null) return '—'
      const n = Number(cents)
      if (!Number.isFinite(n)) return '—'
      return `¥${(n / 100).toFixed(2)}`
    }
  }
}
</script>

<style scoped>
.system-settings-page {
  max-width: 1200px;
  margin: 0 auto;
}
.page-header {
  margin-bottom: 16px;
}
.page-title {
  margin: 0 0 6px;
  font-size: 20px;
  font-weight: 600;
  color: #303133;
}
.page-desc {
  margin: 0;
  font-size: 13px;
  color: #909399;
  line-height: 1.5;
}
.section-card {
  margin-bottom: 16px;
}
.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 8px;
  font-size: 15px;
  font-weight: 600;
}
.sub-title {
  margin: 0 0 8px;
  font-size: 14px;
  font-weight: 600;
  color: #303133;
}
.test-block {
  margin-top: 8px;
}
.test-label {
  display: block;
  font-size: 13px;
  font-weight: 500;
  color: #606266;
  margin-bottom: 8px;
}
.test-row {
  display: flex;
  align-items: center;
  gap: 10px;
}
.hint-text {
  margin: 4px 0 0;
  font-size: 12px;
  color: #909399;
}
.success-text {
  color: #67c23a;
  font-size: 13px;
}
.error-text {
  color: #f56c6c;
  font-size: 13px;
}
.mono-text {
  font-family: 'SF Mono', 'Fira Code', 'Consolas', monospace;
}
.redeem-form {
  margin-top: 8px;
}
</style>
