<template>
  <div class="prototype-project-page">
    <div class="page-head">
      <div class="head-left">
        <h1 class="title">{{ projectName || '快原型项目' }}</h1>
      </div>
      <div class="head-right">
        <button type="button" class="secondary-button" @click="renameProject" :disabled="!projectId">
          重命名
        </button>
      </div>
    </div>

    <div v-if="loading" class="placeholder">加载中…</div>
    <div v-else-if="!projectId" class="placeholder">项目不存在</div>
    <div v-else>
      <div class="layout">
        <div class="left">
          <div class="section-card">
            <div class="section-title">
              <span>生成</span>
              <span class="mode-toggle" role="tablist" aria-label="模式切换">
                <button
                  type="button"
                  class="mode-btn"
                  :class="{ active: mode === 'mockup' }"
                  @click="switchMode('mockup')"
                >HTML+CSS</button>
                <button
                  type="button"
                  class="mode-btn"
                  :class="{ active: mode === 'spec' }"
                  @click="switchMode('spec')"
                >Spec（结构化）</button>
                <button
                  v-if="penpotUiAvailable"
                  type="button"
                  class="mode-btn"
                  :class="{ active: mode === 'penpot' }"
                  @click="switchMode('penpot')"
                >Penpot Beta</button>
              </span>
            </div>
            <div class="section-body">
              <template v-if="mode === 'spec'">
                <label class="label">页面需求 / 设计文档</label>
                <textarea
                  v-model="prompt"
                  class="prompt-textarea"
                  placeholder="可直接写自然语言需求；从报价导入时会生成《页面设计文档》式蓝图（原文为主、清单在附录）。智能体将据此编排页面与组件。"
                ></textarea>
                <div class="actions-row">
                  <button type="button" class="primary-button" :disabled="generating" @click="generateSpec">
                    {{ generating ? '生成中…' : '生成并预览' }}
                  </button>
                  <router-link to="/api-config" class="secondary-button">{{ $t('aiConfig.navTitle') }}</router-link>
                </div>
              </template>

              <template v-else>
                <label class="label">对话输入（中文需求）</label>
                <textarea
                  v-model="mockupPrompt"
                  class="prompt-textarea"
                  placeholder="输入你的中文需求，比如：生成一个现代登录页（包含按钮、输入框、布局说明）"
                ></textarea>
                <div class="actions-row mockup-actions">
                  <label class="mockup-model">
                    <span class="mockup-model-label">模型</span>
                    <input v-model="selectedModel" class="mockup-model-input" placeholder="deepseek-chat" />
                  </label>
                  <button type="button" class="primary-button" :disabled="!canSendMockup" @click="sendMockup">
                    {{ mockupGenerating ? '生成中…' : '发送' }}
                  </button>
                  <button type="button" class="secondary-button" :disabled="mockupGenerating || mockupMessages.length <= 1" @click="clearMockup">
                    清空
                  </button>
                  <router-link to="/api-config" class="secondary-button">{{ $t('aiConfig.navTitle') }}</router-link>
                </div>
              </template>
              <div class="actions-row import-row">
                <select v-model="selectedQuoteProjectId" :disabled="importingQuoteRequirement || quoteLoading">
                  <option :value="null">{{ quoteLoading ? '报价项目加载中…' : '选择报价项目' }}</option>
                  <option v-for="qp in quoteProjects" :key="qp.id" :value="qp.id">
                    {{ qp.name || ('报价项目 #' + qp.id) }}
                  </option>
                </select>
                <button
                  type="button"
                  class="secondary-button"
                  :disabled="importingQuoteRequirement || !selectedQuoteProjectId"
                  @click="importQuoteRequirement"
                >
                  {{ importingQuoteRequirement ? '导入中…' : '导入为页面设计文档' }}
                </button>
              </div>
              <p class="section-hint import-doc-hint">将同步报价中的<strong>需求原文</strong>（PRD / AI 录入）与项目维度，并生成标准结构的《页面设计文档》；报价功能清单仅出现在文末附录，供智能体做漏项核对，不强制「一模块一 Tab」。</p>
              <div v-if="importHint" class="section-hint">{{ importHint }}</div>
              <div v-if="genError" class="error-msg">{{ genError }}</div>

              <template v-if="mode === 'penpot'">
                <p v-if="penpotBootstrapping" class="section-hint">正在为组织开通 Penpot 工作区并保存凭证，请稍候…</p>
                <p class="section-hint">
                  使用<strong>平台 AI 配置</strong>生成布局计划并写入 Penpot 画布；每个组织使用独立 Penpot 账号与 Token（凭证由服务端加密保存）。
                  外链打开工作区编辑；本页不嵌入 Penpot。
                </p>
                <label class="label">页面需求 / 设计说明（将传给 AI 规划）</label>
                <textarea
                  v-model="penpotPrompt"
                  class="prompt-textarea"
                  placeholder="描述页面目标、模块、关键文案等；可与左侧 Spec 模式使用相同需求文本。"
                ></textarea>
                <label class="label">备注（可选，审计用）</label>
                <textarea
                  v-model="penpotNote"
                  class="prompt-textarea muted-area"
                  placeholder="可选：内部备注，不参与主要规划"
                ></textarea>
                <div class="actions-row">
                  <button
                    type="button"
                    class="primary-button"
                    :disabled="penpotGenerating || penpotBootstrapping || !penpotReady || !String(penpotPrompt || '').trim()"
                    @click="createPenpotFile"
                  >
                    {{ penpotGenerating ? '生成中…' : '生成 Penpot 初稿' }}
                  </button>
                  <button type="button" class="secondary-button" :disabled="penpotProbeLoading || penpotBootstrapping || !penpotReady" @click="checkPenpotReachable">
                    {{ penpotProbeLoading ? '检测中…' : '检测 Penpot 连通' }}
                  </button>
                </div>
                <div v-if="penpotStatusHint" class="section-hint">{{ penpotStatusHint }}</div>
                <div v-if="penpotError" class="error-msg">{{ penpotError }}</div>
              </template>
            </div>
          </div>

          <div v-if="mode === 'spec'" class="section-card">
            <div class="section-title">项目版本</div>
            <div class="section-body">
              <select v-if="specs.length" v-model="activeSpecId" @change="onSelectSpec">
                <option v-for="s in specs" :key="s.id" :value="s.id">
                  v{{ s.version }}（{{ formatTime(s.updatedAt) }}）
                </option>
              </select>
              <div v-else class="muted">尚未生成 spec。</div>
              <div class="section-hint">生成新版本后会自动切换预览。</div>
            </div>
          </div>

          <div v-else-if="mode === 'penpot'" class="section-card">
            <div class="section-title">Penpot 绑定</div>
            <div class="section-body">
              <div v-if="penpotFileId" class="penpot-bind">
                <p><strong>文件 ID：</strong><code>{{ penpotFileId }}</code></p>
                <p v-if="penpotPreviewUrl">
                  <a :href="penpotPreviewUrl" target="_blank" rel="noopener">在 Penpot 中打开工作区</a>
                  <span class="section-hint">（公网访问请在部署环境配置 PENPOT_PUBLIC_URI；否则链接可能指向容器内网地址）</span>
                </p>
                <p v-if="penpotExportUrl">
                  <a :href="penpotExportUrl" target="_blank" rel="noopener">下载 .penpot 源文件</a>
                  <button type="button" class="secondary-button penpot-refetch" @click="refreshPenpotExport">刷新导出链接</button>
                </p>
              </div>
              <div v-else class="muted">尚未创建 Penpot 文件。点击左侧「创建 Penpot 文件」。</div>
            </div>
          </div>

          <div v-else class="section-card">
            <div class="section-title">对话记录</div>
            <div class="section-body">
              <div class="chat-log" role="log" aria-live="polite">
                <div v-for="(m, idx) in mockupMessages" :key="idx" class="chat-msg" :class="{ user: m.role === 'user' }">
                  <div class="chat-role">{{ m.role === 'user' ? '你' : 'DeepSeek' }}</div>
                  <div class="chat-content">{{ m.content }}</div>
                </div>
                <div v-if="mockupGenerating" class="chat-msg">
                  <div class="chat-role">DeepSeek</div>
                  <div class="chat-content">正在生成中……</div>
                </div>
              </div>
              <div class="section-hint">提示：HTML+CSS 模式不做版本控制，只保留当前产物。</div>
            </div>
          </div>

        </div>

        <div class="main">
          <div class="section-card">
            <div class="section-head">
              <div class="section-title">
                {{ mode === 'spec' ? '预览（MVP：点击态/切换面板/Tabs）' : mode === 'penpot' ? 'Penpot（外链打开）' : 'Mockup 预览（HTML+CSS）' }}
              </div>
              <div class="head-actions">
                <button
                  v-if="mode === 'spec'"
                  type="button"
                  class="primary-button"
                  :disabled="!activeSpecJson"
                  @click="exportSpecAndPreview"
                >
                  导出 spec + preview
                </button>
                <button
                  v-else-if="mode === 'mockup'"
                  type="button"
                  class="primary-button"
                  :disabled="!latestDesign"
                  @click="downloadLatestMockup"
                >
                  下载 mockup（HTML+CSS）
                </button>
                <button
                  v-else-if="mode === 'penpot' && penpotPreviewUrl"
                  type="button"
                  class="primary-button"
                  @click="openPenpotExternal"
                >
                  打开 Penpot 工作区
                </button>
                <button
                  v-if="mode === 'penpot' && penpotExportUrl"
                  type="button"
                  class="secondary-button"
                  @click="openPenpotExport"
                >
                  下载 .penpot
                </button>
              </div>
            </div>
            <div class="section-body">
              <template v-if="mode === 'spec'">
                <div ref="previewRoot" class="preview-root"></div>
                <div v-if="specParseError" class="error-msg">{{ specParseError }}</div>
                <div v-if="exportError" class="error-msg">{{ exportError }}</div>
              </template>
              <template v-else-if="mode === 'penpot'">
                <div v-if="penpotPreviewUrl" class="penpot-preview-hint">
                  <p>预览与编辑请在 Penpot 网页中完成（不在此页嵌入 iframe）。</p>
                  <p class="muted">工作区：<code>{{ penpotPreviewUrl }}</code></p>
                  <p v-if="penpotJobStage" class="muted">任务阶段：{{ penpotJobStage }} · 进度 {{ penpotJobProgress }}%</p>
                </div>
                <div v-else class="muted">提交生成后将显示工作区与导出链接。</div>
                <div v-if="penpotError" class="error-msg">{{ penpotError }}</div>
              </template>
              <template v-else>
                <iframe
                  v-if="latestDesign"
                  :srcdoc="iframeSrcdoc"
                  sandbox=""
                  class="mockup-iframe"
                  title="mockup-preview"
                />
                <div v-else class="muted">暂无可预览产物</div>
                <div v-if="mockupError" class="error-msg">{{ mockupError }}</div>
              </template>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { renderUiPrototypeSpecToDom } from '../utils/uiPrototypeSpecDomRenderer'
import JSZip from 'jszip'

export default {
  name: 'PrototypeProjectView',
  data () {
    return {
      loading: true,
      projectId: null,
      projectName: '',
      mode: 'mockup',
      specs: [],
      activeSpecId: null,
      prompt: '',
      generating: false,
      quoteLoading: false,
      quoteProjects: [],
      selectedQuoteProjectId: null,
      importingQuoteRequirement: false,
      importHint: '',
      genError: '',
      specParseError: '',
      exportError: '',

      // HTML+CSS mockup mode (mvp-vue style)
      mockupPrompt: '',
      selectedModel: 'deepseek-chat',
      mockupGenerating: false,
      mockupError: '',
      mockupHtml: '',
      mockupCss: '',
      mockupRawAiContent: '',
      mockupMessages: [
        {
          role: 'assistant',
          content: '你好！你可以直接输入中文需求（例如：生成一个登录页 mockup）。我会自动解析为 HTML + CSS 并预览。'
        }
      ],

      // Penpot Beta（方案 A）
      penpotUiAvailable: false,
      penpotReady: false,
      penpotReachable: null,
      penpotNote: '',
      penpotGenerating: false,
      penpotError: '',
      penpotStatusHint: '',
      penpotProbeLoading: false,
      penpotFileId: '',
      penpotPreviewUrl: '',
      penpotExportUrl: '',
      penpotPrompt: '',
      penpotJobStage: '',
      penpotJobProgress: 0,
      penpotMustBootstrap: false,
      penpotBootstrapping: false
    }
  },
  computed: {
    activeSpec () {
      return this.specs.find(s => s.id === this.activeSpecId) || null
    },
    activeSpecJson () {
      return this.activeSpec ? this.activeSpec.specJson : ''
    },
    activeSpecParsed () {
      if (!this.activeSpecJson) return null
      try {
        return JSON.parse(this.activeSpecJson)
      } catch (e) {
        return null
      }
    },
    canSendMockup () {
      return !this.mockupGenerating && String(this.mockupPrompt || '').trim().length > 0
    },
    latestDesign () {
      const html = String(this.mockupHtml || '')
      const css = String(this.mockupCss || '')
      if (!html && !css) return null
      return { html, css }
    },
    iframeSrcdoc () {
      if (!this.latestDesign) return ''
      const css = this.latestDesign.css || ''
      const html = this.latestDesign.html || ''
      return `<!doctype html>
<html lang="zh-CN">
<head>
  <meta charset="UTF-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1.0" />
  <style>${css}</style>
</head>
<body>
${html}
</body>
</html>`
    }
  },
  created () {
    this.projectId = this.$route && this.$route.params ? this.$route.params.projectId : null
    // 进入页面默认使用 HTML+CSS 模式
    this.mode = 'mockup'
    this.load()
    this.loadQuoteProjects()
    this.loadMockup()
    this.loadPenpotStatus()
  },
  watch: {
    activeSpecId () {
      this.renderActiveSpec()
    }
  },
  methods: {
    async sendMockupText (text) {
      const content = String(text || '').trim()
      if (!content) return

      this.mockupMessages.push({ role: 'user', content })
      this.mockupGenerating = true
      this.mockupError = ''
      this.genError = ''

      try {
        const designPrompt = this.buildDesignPrompt(content)
        // 组件点击埋点：由“生成 HTML/CSS mockup”触发
        this.$http.post('/admin/ops/events/component-click', {
          componentKey: 'hub_prototype',
          coreApiKey: 'ui_prototype_generate_mockup'
        }).catch(() => {})
        const resp = await this.$http.post(
          `/admin/ui-prototype/projects/${this.projectId}/mockups/generate`,
          {
            prompt: designPrompt,
            model: this.selectedModel || undefined,
            messagesJson: JSON.stringify(this.mockupMessages)
          }
        )
        if (resp.data && resp.data.code === 0 && resp.data.data && resp.data.data.jobId != null) {
          await this.pollMockupJob(resp.data.data.jobId)
          // 复刻 mvp-vue 的“节选原始输出”体验（后端已落库 rawAiContent）
          const raw = String(this.mockupRawAiContent || '(空回复)')
          const hasDesign = !!this.latestDesign
          const assistantContent = hasDesign
            ? `已生成 mockup（HTML/CSS），已尝试解析并在下方预览。\n\n原始输出（节选）：\n${raw.substring(0, 500)}${raw.length > 500 ? '...' : ''}`
            : raw
          this.mockupMessages.push({ role: 'assistant', content: assistantContent })

          // 同步保存对话消息（不触发生成）
          try {
            await this.$http.post(
              `/admin/ui-prototype/projects/${this.projectId}/mockup/messages`,
              { messagesJson: JSON.stringify(this.mockupMessages) }
            )
          } catch (e) { /* ignore */ }
        } else {
          this.mockupError = (resp.data && resp.data.message) || '生成失败'
        }
      } catch (e) {
        this.mockupError = (e.response && e.response.data && e.response.data.message) || '生成失败'
      } finally {
        this.mockupGenerating = false
      }
    },
    async switchMode (m) {
      if (m !== 'spec' && m !== 'mockup' && m !== 'penpot') return
      this.mode = m
      this.genError = ''
      this.specParseError = ''
      this.exportError = ''
      this.mockupError = ''
      if (m === 'spec') {
        this.$nextTick(() => this.renderActiveSpec())
      }
      if (m === 'penpot' && this.penpotUiAvailable && this.penpotMustBootstrap) {
        await this.bootstrapPenpotTenant()
      }
    },
    async load () {
      this.loading = true
      this.genError = ''
      this.specParseError = ''
      this.exportError = ''
      try {
        const resp = await this.$http.get('/admin/ui-prototype/projects/' + this.projectId)
        if (resp.data && resp.data.code === 0 && resp.data.data) {
          const d = resp.data.data
          this.projectName = d.name || ''
          this.penpotFileId = d.penpotFileId ? String(d.penpotFileId) : ''
          this.penpotPreviewUrl = d.penpotPreviewUrl ? String(d.penpotPreviewUrl) : ''
          this.penpotExportUrl = d.penpotExportUrl ? String(d.penpotExportUrl) : ''
          this.specs = d.specs || []
          // 默认选中当前版本：如果有 currentSpecVersion，则匹配；否则选第一个
          const cur = d.currentSpecVersion
          const match = this.specs.find(s => s.version === cur)
          this.activeSpecId = match ? match.id : (this.specs[0] ? this.specs[0].id : null)
        } else {
          this.specs = []
        }
      } catch (e) {
        this.specs = []
      } finally {
        this.loading = false
        this.$nextTick(() => this.renderActiveSpec())
      }
    },
    async loadMockup () {
      this.mockupError = ''
      if (!this.projectId) return
      try {
        const resp = await this.$http.get(`/admin/ui-prototype/projects/${this.projectId}/mockup`)
        if (resp.data && resp.data.code === 0 && resp.data.data) {
          const d = resp.data.data
          this.mockupHtml = String(d.html || '')
          this.mockupCss = String(d.css || '')
          this.mockupRawAiContent = String(d.rawAiContent || '')
          if (d.messagesJson) {
            try {
              const arr = JSON.parse(String(d.messagesJson))
              if (Array.isArray(arr) && arr.length) this.mockupMessages = arr
            } catch (e) {
              // ignore invalid messagesJson
            }
          }
          if (d.modelUsed) this.selectedModel = String(d.modelUsed)
        }
      } catch (e) {
        // ignore
      }
    },
    onSelectSpec () {
      this.specParseError = ''
      this.renderActiveSpec()
    },
    formatTime (t) {
      if (!t) return ''
      try {
        const s = String(t)
        return s.length > 16 ? s.slice(0, 16) : s
      } catch (e) {
        return ''
      }
    },
    renderActiveSpec () {
      const root = this.$refs.previewRoot
      if (!root) return
      root.innerHTML = ''
      if (!this.activeSpecJson) return
      try {
        const parsed = JSON.parse(this.activeSpecJson)
        renderUiPrototypeSpecToDom(root, parsed)
        this.specParseError = ''
      } catch (e) {
        this.specParseError = 'spec JSON 解析失败：' + (e && e.message ? e.message : '')
      }
    },
    sleep (ms) {
      return new Promise(resolve => setTimeout(resolve, ms))
    },
    async pollGenerateJob (jobId) {
      const max = 120
      for (let i = 0; i < max; i++) {
        await this.sleep(1500)
        const r = await this.$http.get(`/admin/ui-prototype/projects/${this.projectId}/specs/jobs/${jobId}`)
        if (!r.data || r.data.code !== 0 || !r.data.data) {
          this.genError = (r.data && r.data.message) || '查询任务失败'
          return
        }
        const st = r.data.data.status
        if (st === 'success') {
          await this.load()
          return
        }
        if (st === 'failed') {
          this.genError = r.data.data.errorMessage || '生成失败'
          return
        }
      }
      this.genError = '等待超时：生成可能仍在后台进行，请稍后刷新页面查看版本列表。'
    },
    async loadPenpotStatus () {
      try {
        const resp = await this.$http.get('/admin/ui-prototype/penpot/status')
        if (resp.data && resp.data.code === 0 && resp.data.data) {
          const d = resp.data.data
          this.penpotUiAvailable = !!d.enabled
          this.penpotReady = !!d.penpotReady
          this.penpotMustBootstrap = !!d.mustBootstrap
          if (!this.penpotUiAvailable) {
            this.penpotStatusHint = 'Penpot Beta 未启用：请在部署环境设置 PENPOT_ENABLED=true（无需再配置平台级 PENPOT_ACCESS_TOKEN）。'
          } else if (this.penpotMustBootstrap) {
            this.penpotStatusHint = '首次使用需由租户管理员在本页开通 Penpot 工作区（自动创建独立账号并保存凭证）。切换到「Penpot Beta」标签时将自动执行。'
          } else {
            this.penpotStatusHint = ''
          }
        }
      } catch (e) {
        this.penpotUiAvailable = false
      }
    },
    async checkPenpotReachable () {
      this.penpotProbeLoading = true
      this.penpotError = ''
      try {
        const resp = await this.$http.get('/admin/ui-prototype/penpot/status', { params: { probe: 1 } })
        if (resp.data && resp.data.code === 0 && resp.data.data) {
          const ok = !!resp.data.data.reachable
          this.penpotReachable = ok
          this.penpotStatusHint = ok ? 'Penpot API 连通正常（已使用本组织独立 Token）。' : '无法连接 Penpot：请确认已开通工作区且 Penpot 容器可达。'
        }
      } catch (e) {
        this.penpotError = (e.response && e.response.data && e.response.data.message) || '检测失败'
      } finally {
        this.penpotProbeLoading = false
      }
    },
    async bootstrapPenpotTenant () {
      if (!this.penpotMustBootstrap || this.penpotBootstrapping) return
      this.penpotBootstrapping = true
      this.penpotError = ''
      try {
        const resp = await this.$http.post('/admin/ui-prototype/penpot/bootstrap', {})
        if (resp.data && resp.data.code === 0) {
          await this.loadPenpotStatus()
        } else {
          this.penpotError = (resp.data && resp.data.message) || '开通失败'
        }
      } catch (e) {
        this.penpotError = (e.response && e.response.data && e.response.data.message) || '开通失败'
      } finally {
        this.penpotBootstrapping = false
      }
    },
    async createPenpotFile () {
      if (!this.penpotReady) return
      this.penpotGenerating = true
      this.penpotError = ''
      try {
        const resp = await this.$http.post(`/admin/ui-prototype/projects/${this.projectId}/penpot/jobs`, {
          prompt: String(this.penpotPrompt || '').trim(),
          note: this.penpotNote || undefined
        })
        if (resp.data && resp.data.code === 0 && resp.data.data && resp.data.data.jobId != null) {
          await this.pollPenpotJob(resp.data.data.jobId)
          await this.load()
        } else {
          this.penpotError = (resp.data && resp.data.message) || '创建失败'
        }
      } catch (e) {
        this.penpotError = (e.response && e.response.data && e.response.data.message) || '创建失败'
      } finally {
        this.penpotGenerating = false
      }
    },
    async pollPenpotJob (jobId) {
      const max = 120
      for (let i = 0; i < max; i++) {
        await this.sleep(1500)
        const r = await this.$http.get(`/admin/ui-prototype/projects/${this.projectId}/penpot/jobs/${jobId}`)
        if (!r.data || r.data.code !== 0 || !r.data.data) {
          this.penpotError = (r.data && r.data.message) || '查询任务失败'
          return
        }
        const d = r.data.data
        this.penpotJobStage = d.stage || ''
        this.penpotJobProgress = typeof d.progress === 'number' ? d.progress : 0
        const st = d.status
        if (st === 'success') {
          await this.load()
          return
        }
        if (st === 'failed') {
          this.penpotError = d.errorMessage || '生成失败'
          return
        }
      }
      this.penpotError = '等待超时：请稍后刷新页面查看 Penpot 绑定。'
    },
    openPenpotExternal () {
      if (this.penpotPreviewUrl) {
        window.open(this.penpotPreviewUrl, '_blank', 'noopener')
      }
    },
    openPenpotExport () {
      if (this.penpotExportUrl) {
        window.open(this.penpotExportUrl, '_blank', 'noopener')
      }
    },
    async refreshPenpotExport () {
      this.penpotError = ''
      try {
        const r = await this.$http.get(`/admin/ui-prototype/projects/${this.projectId}/penpot/export-binfile`)
        if (r.data && r.data.code === 0 && r.data.data && r.data.data.exportBinfileUrl) {
          this.penpotExportUrl = String(r.data.data.exportBinfileUrl)
        }
      } catch (e) {
        this.penpotError = (e.response && e.response.data && e.response.data.message) || '刷新导出失败'
      }
    },
    async pollMockupJob (jobId) {
      const max = 120
      for (let i = 0; i < max; i++) {
        await this.sleep(1500)
        const r = await this.$http.get(`/admin/ui-prototype/projects/${this.projectId}/mockups/jobs/${jobId}`)
        if (!r.data || r.data.code !== 0 || !r.data.data) {
          this.mockupError = (r.data && r.data.message) || '查询任务失败'
          return
        }
        const st = r.data.data.status
        if (st === 'success') {
          await this.loadMockup()
          return
        }
        if (st === 'failed') {
          this.mockupError = r.data.data.errorMessage || '生成失败'
          return
        }
      }
      this.mockupError = '等待超时：生成可能仍在后台进行，请稍后刷新页面查看预览。'
    },
    async generateSpec () {
      this.generating = true
      this.genError = ''
      this.specParseError = ''
      this.exportError = ''
      try {
        // 组件点击埋点：由“生成结构化 spec”触发
        this.$http.post('/admin/ops/events/component-click', {
          componentKey: 'hub_prototype',
          coreApiKey: 'ui_prototype_generate_spec'
        }).catch(() => {})
        const resp = await this.$http.post(`/admin/ui-prototype/projects/${this.projectId}/specs/generate`, { prompt: this.prompt })
        if (resp.data && resp.data.code === 0 && resp.data.data && resp.data.data.jobId != null) {
          await this.pollGenerateJob(resp.data.data.jobId)
        } else {
          this.genError = (resp.data && resp.data.message) || '生成失败'
        }
      } catch (e) {
        const st = e.response && e.response.status
        if (st === 504) {
          this.genError = '网关超时：请确认已部署支持异步生成的后端版本；若已部署仍超时，请联系运维调大 nginx 超时或检查网络。'
        } else {
          this.genError = (e.response && e.response.data && e.response.data.message) || '生成失败'
        }
      } finally {
        this.generating = false
      }
    },
    buildDesignPrompt (userInput) {
      return `你是一名专业前端工程师。请生成一个“可直接在浏览器预览”的前端 mockup，用于本地 MVP 演示。

要求：
1. 只允许输出一个 JSON 对象，不要输出任何多余文字。
2. JSON 结构固定：{ "html": "...", "css": "..." }
3. html：只包含 body 内所需的元素（可以包含 class/id），不要包含 <html>, <head>, <body>，不要包含 <style> 标签。
4. css：只包含 CSS 内容（不要包含 <style> 标签）。
5. 需要支持中文显示（使用系统字体栈：sans-serif 即可），不要引用外部图片/外链资源。
6. 不要包含 <script>，不要包含事件处理属性（如 onclick=...）。
7. 尽量保证响应式（移动端也要好看）。

用户需求（中文）：
${userInput}`
    },
    async sendMockup () {
      const content = String(this.mockupPrompt || '').trim()
      if (!content) return
      this.mockupPrompt = ''
      await this.sendMockupText(content)
    },
    clearMockup () {
      this.mockupMessages = [
        { role: 'assistant', content: '已清空对话。你可以继续输入中文需求，我会继续调用 DeepSeek。' }
      ]
      this.mockupError = ''
      try {
        this.$http.post(
          `/admin/ui-prototype/projects/${this.projectId}/mockup/messages`,
          { messagesJson: JSON.stringify(this.mockupMessages) }
        )
      } catch (e) {
        // ignore
      }
    },
    downloadLatestMockup () {
      if (!this.latestDesign) return
      const css = this.latestDesign.css || ''
      const html = this.latestDesign.html || ''
      const doc = `<!doctype html>
<html lang="zh-CN">
<head>
  <meta charset="UTF-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1.0" />
  <style>${css}</style>
</head>
<body>
${html}
</body>
</html>`
      const blob = new Blob([doc], { type: 'text/html;charset=utf-8' })
      const url = URL.createObjectURL(blob)
      const a = document.createElement('a')
      a.href = url
      a.download = 'prototype-mockup.html'
      a.click()
      URL.revokeObjectURL(url)
    },
    async loadQuoteProjects () {
      this.quoteLoading = true
      try {
        const resp = await this.$http.get('/admin/quote/projects', { params: { page: 1, pageSize: 100 } })
        if (resp.data && resp.data.code === 0 && resp.data.data) {
          this.quoteProjects = Array.isArray(resp.data.data.items) ? resp.data.data.items : []
          if (!this.selectedQuoteProjectId && this.quoteProjects[0]) {
            this.selectedQuoteProjectId = this.quoteProjects[0].id
          }
        } else {
          this.quoteProjects = []
        }
      } catch (e) {
        this.quoteProjects = []
      } finally {
        this.quoteLoading = false
      }
    },
    async importQuoteRequirement () {
      if (!this.selectedQuoteProjectId) {
        this.importHint = '请先选择报价项目'
        return
      }
      this.importingQuoteRequirement = true
      this.importHint = ''
      this.genError = ''
      try {
        const resp = await this.$http.post(
          `/admin/ui-prototype/projects/${this.projectId}/import-quote-requirement`,
          { quoteProjectId: this.selectedQuoteProjectId }
        )
        if (resp.data && resp.data.code === 0 && resp.data.data && resp.data.data.requirementText) {
          const t = String(resp.data.data.requirementText || '').trim()
          if (this.mode === 'spec') {
            this.prompt = t
            this.importHint = '《页面设计文档》已写入需求框（用户叙述为主、报价清单在附录），可补充后生成'
          } else {
            // HTML+CSS 模式：导入后自动触发一次生成并预览
            this.mockupPrompt = t
            await this.sendMockupText(t)
            this.importHint = '《页面设计文档》已导入并已触发生成预览；可补充说明后再次发送'
          }
        } else {
          this.genError = (resp.data && resp.data.message) || '导入报价需求失败'
        }
      } catch (e) {
        this.genError = (e.response && e.response.data && e.response.data.message) || '导入报价需求失败'
      } finally {
        this.importingQuoteRequirement = false
      }
    },
    renameProject () {
      const name = window.prompt('请输入新项目名称', this.projectName)
      if (!name) return
      this.$http.put('/admin/ui-prototype/projects/' + this.projectId, { name }).then(async () => {
        await this.load()
      }).catch(e => {
        alert((e.response && e.response.data && e.response.data.message) || '重命名失败')
      })
    },
    async exportSpecAndPreview () {
      this.exportError = ''
      if (!this.activeSpecParsed) {
        this.exportError = '没有可导出的 spec'
        return
      }

      try {
        const spec = this.activeSpecParsed
        const specStr = JSON.stringify(spec, null, 2)
        const previewHtml = this.buildPreviewHtml(spec)

        const zip = new JSZip()
        zip.file('spec/spec.json', specStr)
        zip.file('preview/index.html', previewHtml)

        const zipBlob = await zip.generateAsync({ type: 'blob' })
        this.downloadBlob(zipBlob, `${this.projectName || 'prototype'}-prototype.zip`)
      } catch (e) {
        this.exportError = (e && e.message ? e.message : '导出失败')
      }
    },
    downloadTextFile (content, filename, mime) {
      const blob = new Blob([content], { type: mime || 'text/plain;charset=utf-8' })
      const url = URL.createObjectURL(blob)
      const a = document.createElement('a')
      a.href = url
      a.download = filename
      document.body.appendChild(a)
      a.click()
      document.body.removeChild(a)
      URL.revokeObjectURL(url)
    },
    downloadBlob (blob, filename) {
      const url = URL.createObjectURL(blob)
      const a = document.createElement('a')
      a.href = url
      a.download = filename
      document.body.appendChild(a)
      a.click()
      document.body.removeChild(a)
      URL.revokeObjectURL(url)
    },
    buildPreviewHtml (spec) {
      // 防止 LLM 输出中出现结束脚本标记导致预览页面内嵌脚本被提前截断
      const safeSpec = JSON.stringify(spec).replace(/<\/script>/gi, '<\\/script>')
      // 预览页面：提供一个最小的 DOM renderer（与 app renderer 同一输出结构）
      return `<!doctype html>
<html>
  <head>
    <meta charset="utf-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1" />
    <title>UI Prototype Preview</title>
    <style>
      body { margin: 0; padding: 12px; font-family: -apple-system,BlinkMacSystemFont,Segoe UI,Roboto,Helvetica,Arial,'Noto Sans','PingFang SC','Hiragino Sans GB','Microsoft YaHei',sans-serif; background: #f1f5f9; color:#0f172a; }
      button { font-family: inherit; }
    </style>
  </head>
  <body>
    <div id="app"></div>
    <script>
      const SPEC = ${safeSpec};

      const SPACING_TOKENS = { 'space-4': 4, 'space-8': 8, 'space-12': 12, 'space-16': 16, 'space-24': 24 };
      const RADIUS_TOKENS = { 'r-8': 8, 'r-10': 10, 'r-12': 12 };
      const COLOR_TOKENS = { primary: '#2563eb', 'primary-strong': '#1d4ed8', text: '#0f172a', 'text-muted': '#64748b', bg: '#ffffff', 'border-muted': '#e5e7eb', success: '#22c55e', info: '#0ea5e9', warn: '#f59e0b' };
      const SHADOW_TOKENS = { 'shadow-soft': '0 4px 24px rgba(15, 23, 42, 0.06)', 'shadow-none': 'none' };

      function applyTokenStyle(el, style) {
        if (!style || typeof style !== 'object') return;
        if (style.padding && SPACING_TOKENS[style.padding] != null) el.style.padding = SPACING_TOKENS[style.padding] + 'px';
        if (style.radius && RADIUS_TOKENS[style.radius] != null) el.style.borderRadius = RADIUS_TOKENS[style.radius] + 'px';
        if (style.bg && COLOR_TOKENS[style.bg]) el.style.background = COLOR_TOKENS[style.bg];
        if (style.border && COLOR_TOKENS[style.border]) el.style.border = '1px solid ' + COLOR_TOKENS[style.border];
        if (style.shadow && SHADOW_TOKENS[style.shadow]) el.style.boxShadow = SHADOW_TOKENS[style.shadow];
      }

      function createBaseNodeStyle(type, el) {
        if (type === 'Card') {
          el.style.background = '#ffffff';
          el.style.border = '1px solid #e5e7eb';
          el.style.borderRadius = '12px';
          el.style.padding = '16px';
          el.style.boxShadow = 'none';
        } else if (type === 'Panel') {
          el.style.border = '1px solid #e5e7eb';
          el.style.borderRadius = '12px';
          el.style.padding = '12px';
          el.style.background = '#ffffff';
        } else if (type === 'Button') {
          el.style.border = '1px solid #1d4ed8';
          el.style.background = '#1d4ed8';
          el.style.color = '#ffffff';
          el.style.padding = '8px 12px';
          el.style.borderRadius = '10px';
          el.style.cursor = 'pointer';
          el.style.fontWeight = '700';
          el.style.fontSize = '13px';
        } else if (type === 'Badge') {
          el.style.display = 'inline-block';
          el.style.padding = '2px 8px';
          el.style.borderRadius = '999px';
          el.style.background = '#eef2ff';
          el.style.color = '#3730a3';
          el.style.fontWeight = '800';
          el.style.fontSize = '11px';
        }
      }

      function renderNodeDom(spec, state, interactionsBySource, nodeId, rerender) {
        const node = spec.nodes[nodeId];
        if (!node) return document.createTextNode('—');

        const type = node.type;
        const props = node.props || {};
        const style = node.style || {};
        const children = Array.isArray(node.children) ? node.children : [];

        if (type === 'Page') {
          const root = document.createElement('div');
          root.style.width = '100%';
          root.style.boxSizing = 'border-box';
          root.style.padding = '12px';
          children.forEach(cid => root.appendChild(renderNodeDom(spec, state, interactionsBySource, cid, rerender)));
          return root;
        }

        if (type === 'Container' || type === 'Card') {
          const el = document.createElement('div');
          el.style.boxSizing = 'border-box';
          createBaseNodeStyle(type, el);
          applyTokenStyle(el, style);
          children.forEach(cid => el.appendChild(renderNodeDom(spec, state, interactionsBySource, cid, rerender)));
          return el;
        }

        if (type === 'Grid') {
          const el = document.createElement('div');
          el.style.display = 'grid';
          el.style.boxSizing = 'border-box';
          const cols = props.columns && Number.isInteger(props.columns) ? props.columns : 2;
          el.style.gridTemplateColumns = 'repeat(' + cols + ', minmax(0, 1fr))';
          el.style.gap = '12px';
          applyTokenStyle(el, style);
          children.forEach(cid => el.appendChild(renderNodeDom(spec, state, interactionsBySource, cid, rerender)));
          return el;
        }

        if (type === 'Text') {
          const el = document.createElement('div');
          el.textContent = props.text != null ? String(props.text) : '';
          el.style.color = '#0f172a';
          el.style.fontSize = '14px';
          applyTokenStyle(el, style);
          return el;
        }

        if (type === 'Button') {
          const el = document.createElement('button');
          el.type = 'button';
          el.textContent = props.label != null ? String(props.label) : '';
          createBaseNodeStyle(type, el);
          applyTokenStyle(el, style);
          el.addEventListener('click', () => {
            const interactions = interactionsBySource[nodeId] || [];
            interactions.forEach(it => {
              if (it.type === 'togglePanel') state.panelOpen[it.targetId] = !!it.params.open;
              if (it.type === 'setTab') state.tabsActive[it.targetId] = String(it.params.tabKey);
            });
            rerender();
          });
          return el;
        }

        if (type === 'Badge') {
          const el = document.createElement('span');
          el.textContent = props.text != null ? String(props.text) : '';
          createBaseNodeStyle(type, el);
          applyTokenStyle(el, style);
          return el;
        }

        if (type === 'Panel') {
          const el = document.createElement('div');
          createBaseNodeStyle(type, el);
          applyTokenStyle(el, style);
          const open = state.panelOpen[nodeId] != null ? state.panelOpen[nodeId] : !!props.defaultOpen;
          el.style.display = open ? 'block' : 'none';
          children.forEach(cid => el.appendChild(renderNodeDom(spec, state, interactionsBySource, cid, rerender)));
          return el;
        }

        if (type === 'Tabs') {
          const el = document.createElement('div');
          el.style.boxSizing = 'border-box';
          const tabItems = Array.isArray(props.tabItems) ? props.tabItems : [];
          const activeKey = state.tabsActive[nodeId] != null
            ? state.tabsActive[nodeId]
            : (tabItems[0] && tabItems[0].key ? tabItems[0].key : '');

          const header = document.createElement('div');
          header.style.display = 'flex';
          header.style.flexWrap = 'wrap';
          header.style.gap = '8px';
          header.style.marginBottom = '10px';

          tabItems.forEach(ti => {
            const key = ti.key;
            const btn = document.createElement('button');
            btn.type = 'button';
            btn.textContent = ti.label != null ? String(ti.label) : '';
            btn.style.border = '1px solid #e5e7eb';
            btn.style.background = key === activeKey ? '#eef2ff' : '#f8fafc';
            btn.style.color = key === activeKey ? '#3730a3' : '#334155';
            btn.style.padding = '8px 10px';
            btn.style.borderRadius = '10px';
            btn.style.cursor = 'pointer';
            btn.style.fontWeight = '800';
            btn.style.fontSize = '13px';
            btn.addEventListener('click', () => {
              state.tabsActive[nodeId] = String(key);
              rerender();
            });
            header.appendChild(btn);
          });

          const body = document.createElement('div');
          tabItems.forEach(ti => {
            if (String(ti.key) === String(activeKey)) {
              body.appendChild(renderNodeDom(spec, state, interactionsBySource, ti.contentId, rerender));
            }
          });
          el.appendChild(header);
          el.appendChild(body);
          return el;
        }

        return document.createTextNode('—');
      }

      function renderUiPrototypeSpecToDom (container, spec) {
        if (!container || !spec || !spec.nodes || !spec.layout || !spec.layout.root) return;

        const interactions = Array.isArray(spec.interactions) ? spec.interactions : [];
        const interactionsBySource = {};
        interactions.forEach(it => {
          if (!it || typeof it !== 'object') return;
          const sourceId = it.sourceId;
          if (!sourceId) return;
          if (!interactionsBySource[sourceId]) interactionsBySource[sourceId] = [];
          interactionsBySource[sourceId].push(it);
        });

        const state = { panelOpen: {}, tabsActive: {} };
        Object.keys(spec.nodes).forEach(id => {
          const n = spec.nodes[id];
          if (!n || !n.type) return;
          if (n.type === 'Panel') state.panelOpen[id] = !!(n.props && n.props.defaultOpen);
          if (n.type === 'Tabs') {
            const tabItems = n.props && Array.isArray(n.props.tabItems) ? n.props.tabItems : [];
            if (n.props && n.props.defaultTabKey) state.tabsActive[id] = String(n.props.defaultTabKey);
            else if (tabItems[0] && tabItems[0].key) state.tabsActive[id] = String(tabItems[0].key);
          }
        });

        const renderAll = () => {
          container.innerHTML = '';
          container.appendChild(renderNodeDom(spec, state, interactionsBySource, spec.layout.root, renderAll));
        }
        renderAll();
      }

      renderUiPrototypeSpecToDom(document.getElementById('app'), SPEC);
    </scr\u0069pt>
  </body>
</html>`
    }
  }
}
</script>

<style scoped>
.prototype-project-page {
  max-width: none;
  width: 100%;
  margin: 0;
  padding: var(--space-lg) var(--space-xl);
  color: var(--text-primary);
  background: var(--bg-page);
  min-height: 100vh;
  box-sizing: border-box;
}
.page-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-md);
  margin-bottom: var(--space-lg);
}
.head-left {
  display: flex;
  align-items: baseline;
  gap: var(--space-md);
  flex-wrap: wrap;
}
.title {
  margin: 0;
  font-size: 22px;
  font-weight: 600;
  color: var(--text-primary);
}
.layout {
  display: grid;
  grid-template-columns: 320px minmax(0, 1fr);
  gap: var(--space-lg);
}
.left .section-card {
  margin-bottom: var(--space-lg);
}
.main .section-card {
  min-height: calc(100vh - 170px);
}
.section-card {
  background: var(--bg-card);
  border: 1px solid var(--border-primary);
  border-radius: var(--radius-lg);
  padding: var(--space-lg);
  box-shadow: var(--shadow-sm);
}
.section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-md);
  margin-bottom: var(--space-sm);
}
.section-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: var(--space-sm);
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-sm);
}
.section-head .section-title {
  margin-bottom: 0;
}
.head-actions {
  display: flex;
  gap: var(--space-sm);
  align-items: center;
}
.mode-toggle {
  display: inline-flex;
  gap: 4px;
  padding: 3px;
  border: 1px solid var(--border-primary);
  border-radius: var(--radius-md);
  background: var(--bg-page);
}
.mode-btn {
  border: 1px solid transparent;
  background: transparent;
  padding: 5px 10px;
  border-radius: var(--radius-sm);
  cursor: pointer;
  font-weight: 500;
  font-size: 13px;
  color: var(--text-secondary);
  transition: all 0.2s;
}
.mode-btn.active {
  background: var(--bg-card);
  border-color: var(--brand-blue);
  color: var(--brand-blue);
  box-shadow: var(--shadow-sm);
}
.muted-area {
  opacity: 0.92;
  min-height: 56px;
}
.penpot-refetch {
  margin-left: 8px;
}
.section-body select,
.section-body textarea {
  width: 100%;
  box-sizing: border-box;
}
.section-hint {
  margin-top: var(--space-sm);
  color: var(--text-tertiary);
  font-size: 12px;
  font-weight: 400;
  line-height: 1.5;
}
.import-doc-hint {
  font-weight: 400;
  line-height: 1.5;
}
.import-doc-hint strong {
  font-weight: 600;
  color: var(--text-secondary);
}
.label {
  display: block;
  color: var(--text-secondary);
  font-size: 13px;
  font-weight: 500;
  margin-bottom: 6px;
}
.prompt-textarea {
  min-height: 120px;
  border: 1px solid var(--border-primary);
  border-radius: var(--radius-md);
  padding: var(--space-sm) var(--space-md);
  font-size: 14px;
  resize: vertical;
  background: var(--bg-card);
  color: var(--text-primary);
  transition: border-color 0.2s;
  box-sizing: border-box;
}
.prompt-textarea:focus {
  border-color: var(--brand-blue);
  outline: none;
}
.actions-row {
  margin-top: var(--space-sm);
}
.mockup-actions {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-sm);
  align-items: center;
}
.mockup-model {
  display: flex;
  align-items: center;
  gap: var(--space-sm);
  flex: 1;
  min-width: 240px;
}
.mockup-model-label {
  font-size: 12px;
  font-weight: 500;
  color: var(--text-secondary);
  white-space: nowrap;
}
.mockup-model-input {
  flex: 1;
  min-width: 0;
  border: 1px solid var(--border-primary);
  border-radius: var(--radius-sm);
  padding: 6px var(--space-sm);
  font-size: 13px;
  font-weight: 400;
  background: var(--bg-card);
  color: var(--text-primary);
  transition: border-color 0.2s;
}
.mockup-model-input:focus {
  border-color: var(--brand-blue);
  outline: none;
}
.import-row {
  display: flex;
  gap: var(--space-sm);
}
.import-row select {
  flex: 1;
  min-width: 0;
}
.primary-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: var(--space-sm) var(--space-lg);
  background: var(--brand-blue);
  color: #ffffff;
  border: none;
  border-radius: var(--radius-md);
  cursor: pointer;
  font-weight: 500;
  font-size: 14px;
  transition: opacity 0.2s;
  box-shadow: var(--shadow-sm);
}
.primary-button:hover {
  opacity: 0.85;
}
.primary-button:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
.secondary-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: var(--space-sm) var(--space-md);
  border: 1px solid var(--border-primary);
  background: var(--bg-card);
  border-radius: var(--radius-md);
  cursor: pointer;
  font-weight: 500;
  font-size: 14px;
  color: var(--text-primary);
  text-decoration: none;
  transition: border-color 0.2s;
}
.secondary-button:hover {
  border-color: var(--brand-blue);
  color: var(--brand-blue);
}
.secondary-button:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
.preview-root {
  min-height: calc(100vh - 260px);
  border: 1px solid var(--border-primary);
  border-radius: var(--radius-lg);
  background: var(--bg-page);
  padding: var(--space-sm);
  overflow: auto;
}
.mockup-iframe {
  width: 100%;
  height: calc(100vh - 260px);
  border-radius: var(--radius-lg);
  border: 1px solid var(--border-primary);
  background: var(--bg-card);
}
.chat-log {
  max-height: 420px;
  overflow: auto;
  border: 1px solid var(--border-primary);
  border-radius: var(--radius-lg);
  background: var(--bg-page);
  padding: var(--space-sm);
}
.chat-msg {
  border: 1px solid var(--border-primary);
  border-radius: var(--radius-md);
  background: var(--bg-card);
  padding: var(--space-sm) var(--space-md);
  margin-bottom: var(--space-sm);
  box-shadow: var(--shadow-sm);
}
.chat-msg.user {
  background: #F0F5FF;
  border-color: #D4E3FF;
}
.chat-role {
  font-size: 12px;
  font-weight: 600;
  color: var(--text-tertiary);
  margin-bottom: 4px;
}
.chat-content {
  white-space: pre-wrap;
  font-size: 13px;
  font-weight: 400;
  color: var(--text-primary);
  line-height: 1.6;
}
.muted {
  color: var(--text-tertiary);
  font-weight: 400;
  font-size: 13px;
}
.placeholder {
  padding: var(--space-xxl);
  border: 1px solid var(--border-primary);
  border-radius: var(--radius-lg);
  background: var(--bg-card);
  font-weight: 500;
  color: var(--text-tertiary);
  text-align: center;
}
.error-msg {
  margin-top: var(--space-sm);
  color: var(--danger);
  font-weight: 500;
  font-size: 13px;
}
@media (max-width: 900px) {
  .layout { grid-template-columns: 1fr; }
}
</style>

