<template>
  <div class="prototype-project-page">
    <div class="page-head">
      <div class="head-left">
        <router-link to="/prototype" class="back-link">← 返回</router-link>
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
                  <router-link to="/ai-config" class="secondary-button">AI 配置</router-link>
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
                  <router-link to="/ai-config" class="secondary-button">AI 配置</router-link>
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
                {{ mode === 'spec' ? '预览（MVP：点击态/切换面板/Tabs）' : 'Mockup 预览（HTML+CSS）' }}
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
                  v-else
                  type="button"
                  class="primary-button"
                  :disabled="!latestDesign"
                  @click="downloadLatestMockup"
                >
                  下载 mockup（HTML+CSS）
                </button>
              </div>
            </div>
            <div class="section-body">
              <template v-if="mode === 'spec'">
                <div ref="previewRoot" class="preview-root"></div>
                <div v-if="specParseError" class="error-msg">{{ specParseError }}</div>
                <div v-if="exportError" class="error-msg">{{ exportError }}</div>
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
      ]
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
    switchMode (m) {
      if (m !== 'spec' && m !== 'mockup') return
      this.mode = m
      this.genError = ''
      this.specParseError = ''
      this.exportError = ''
      this.mockupError = ''
      if (m === 'spec') {
        this.$nextTick(() => this.renderActiveSpec())
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
  padding: 16px 24px;
  color: #0f172a;
}
.page-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 14px;
}
.head-left {
  display: flex;
  align-items: baseline;
  gap: 12px;
  flex-wrap: wrap;
}
.back-link {
  color: #2563eb;
  font-weight: 700;
  text-decoration: none;
}
.title {
  margin: 0;
  font-size: 22px;
  font-weight: 900;
}
.layout {
  display: grid;
  grid-template-columns: 300px minmax(0, 1fr);
  gap: 14px;
}
.left .section-card {
  margin-bottom: 14px;
}
.main .section-card {
  min-height: calc(100vh - 170px);
}
.section-card {
  background: #ffffff;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  padding: 12px;
}
.section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 10px;
}
.section-title {
  font-size: 14px;
  font-weight: 900;
  color: #0f172a;
  margin-bottom: 10px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}
.section-head .section-title {
  margin-bottom: 0;
}
.head-actions {
  display: flex;
  gap: 8px;
  align-items: center;
}
.mode-toggle {
  display: inline-flex;
  gap: 6px;
  padding: 2px;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  background: #f8fafc;
}
.mode-btn {
  border: 1px solid transparent;
  background: transparent;
  padding: 6px 10px;
  border-radius: 10px;
  cursor: pointer;
  font-weight: 900;
  color: #334155;
}
.mode-btn.active {
  background: #eef2ff;
  border-color: #c7d2fe;
  color: #3730a3;
}
.section-body select, .section-body textarea {
  width: 100%;
}
.section-hint {
  margin-top: 8px;
  color: #64748b;
  font-size: 12px;
  font-weight: 600;
}
.import-doc-hint {
  font-weight: 500;
  line-height: 1.45;
}
.import-doc-hint strong {
  font-weight: 700;
}
.label {
  display: block;
  color: #334155;
  font-size: 13px;
  font-weight: 800;
  margin-bottom: 6px;
}
.prompt-textarea {
  min-height: 120px;
  border: 1px solid #e5e7eb;
  border-radius: 10px;
  padding: 10px;
  font-size: 14px;
  resize: vertical;
}
.actions-row {
  margin-top: 10px;
}
.mockup-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
}
.mockup-model {
  display: flex;
  align-items: center;
  gap: 8px;
  flex: 1;
  min-width: 240px;
}
.mockup-model-label {
  font-size: 12px;
  font-weight: 900;
  color: #334155;
}
.mockup-model-input {
  flex: 1;
  min-width: 0;
  border: 1px solid #e5e7eb;
  border-radius: 10px;
  padding: 8px 10px;
  font-weight: 800;
}
.import-row {
  display: flex;
  gap: 8px;
}
.import-row select {
  flex: 1;
  min-width: 0;
}
.primary-button {
  display: inline-block;
  padding: 10px 14px;
  background: #1d4ed8;
  color: #ffffff;
  border: 1px solid #1d4ed8;
  border-radius: 10px;
  cursor: pointer;
  font-weight: 900;
}
.primary-button:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}
.secondary-button {
  padding: 8px 12px;
  border: 1px solid #94a3b8;
  background: #e2e8f0;
  border-radius: 10px;
  cursor: pointer;
  font-weight: 800;
}
.secondary-button:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}
.preview-root {
  min-height: calc(100vh - 260px);
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  background: #f8fafc;
  padding: 10px;
  overflow: auto;
}
.mockup-iframe {
  width: 100%;
  height: calc(100vh - 260px);
  border-radius: 12px;
  border: 1px solid #e5e7eb;
  background: #ffffff;
}
.chat-log {
  max-height: 420px;
  overflow: auto;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  background: #f8fafc;
  padding: 10px;
}
.chat-msg {
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  background: #ffffff;
  padding: 10px;
  margin-bottom: 10px;
}
.chat-msg.user {
  background: #eff6ff;
  border-color: #bfdbfe;
}
.chat-role {
  font-size: 12px;
  font-weight: 900;
  color: #475569;
  margin-bottom: 6px;
}
.chat-content {
  white-space: pre-wrap;
  font-size: 13px;
  font-weight: 700;
  color: #0f172a;
  line-height: 1.55;
}
.muted {
  color: #64748b;
  font-weight: 700;
  font-size: 13px;
}
.placeholder {
  padding: 24px;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  background: #ffffff;
  font-weight: 800;
  color: #475569;
}
.error-msg {
  margin-top: 10px;
  color: #dc2626;
  font-weight: 900;
  font-size: 13px;
}
@media (max-width: 900px) {
  .layout { grid-template-columns: 1fr; }
}
</style>

