<template>
  <div class="showcase-config-page">
    <div class="page-header">
      <button class="btn back-btn" @click="$router.push('/quote')">← 返回报价列表</button>
      <h2>🏢 团队展示页配置</h2>
      <p class="muted">配置客户通过 Agent 链接进入时看到的团队介绍/专业能力展示区域。</p>
    </div>

    <div v-if="loading" class="muted">加载中…</div>
    <form v-else class="form" @submit.prevent="save">
      <label class="chk"><input v-model="form.enabled" type="checkbox"> 启用团队展示区</label>

      <label>展示模式</label>
      <select v-model="form.mode" class="inp" :disabled="!form.enabled">
        <option value="template">官方模板</option>
        <option value="custom_html">自定义 HTML</option>
      </select>

      <!-- ========== 官方模板：结构化表单 ========== -->
      <template v-if="form.mode === 'template'">
        <label>模板选择</label>
        <select v-model="form.templateId" class="inp" :disabled="!form.enabled">
          <option value="enterprise">企业版（蓝色商务风）</option>
          <option value="studio">工作室版（紫色创意风）</option>
          <option value="freelancer">自由职业版（青色简约风）</option>
        </select>
        <p class="hint">不同模板对同一字段有不同的展示风格，所有字段均可留空使用默认值。</p>

        <!-- 基本信息 -->
        <div class="section-title">📋 基本信息</div>

        <label>一句话定位（Slogan）</label>
        <input v-model="content.slogan" class="inp" placeholder="例如：专业软件解决方案提供商" :disabled="!form.enabled" />

        <label>团队简介</label>
        <textarea v-model="content.description" class="inp textarea-sm" rows="3"
          placeholder="简要介绍你的团队/个人，1-3句话即可" :disabled="!form.enabled"></textarea>

        <!-- 核心数据 -->
        <div class="section-title">📊 核心数据亮点</div>
        <p class="hint">展示在页面醒目位置的数据卡片，建议 2-4 项。</p>
        <div v-for="(s, i) in content.stats" :key="'s'+i" class="sub-item">
          <div class="sub-item-header">
            <span class="sub-item-label">数据 {{ i + 1 }}</span>
            <button type="button" class="btn-sm danger" @click="content.stats.splice(i, 1)" :disabled="!form.enabled">删除</button>
          </div>
          <div class="sub-item-row">
            <div class="sub-item-field half">
              <label>标签</label>
              <input v-model="s.label" class="inp" placeholder="如：完成项目" :disabled="!form.enabled" />
            </div>
            <div class="sub-item-field half">
              <label>数值</label>
              <input v-model="s.value" class="inp" placeholder="如：500+" :disabled="!form.enabled" />
            </div>
          </div>
        </div>
        <button type="button" class="btn add-btn" @click="content.stats.push({label:'',value:''})" :disabled="!form.enabled">+ 添加数据</button>

        <!-- 核心能力 -->
        <div class="section-title">⚡ 核心能力 / 服务范围</div>
        <p class="hint">{{ form.templateId === 'freelancer' ? '你的服务项目列表' : '你擅长的技术领域和服务类型' }}</p>
        <div v-for="(c, i) in content.capabilities" :key="'c'+i" class="sub-item">
          <div class="sub-item-header">
            <span class="sub-item-label">能力 {{ i + 1 }}</span>
            <button type="button" class="btn-sm danger" @click="content.capabilities.splice(i, 1)" :disabled="!form.enabled">删除</button>
          </div>
          <div class="sub-item-row">
            <div class="sub-item-field">
              <label>标题</label>
              <input v-model="c.title" class="inp" placeholder="如：Web应用开发" :disabled="!form.enabled" />
            </div>
          </div>
          <div class="sub-item-row">
            <div class="sub-item-field">
              <label>描述（可选）</label>
              <input v-model="c.desc" class="inp" placeholder="如：企业级Web系统、SaaS平台" :disabled="!form.enabled" />
            </div>
          </div>
        </div>
        <button type="button" class="btn add-btn" @click="content.capabilities.push({title:'',desc:''})" :disabled="!form.enabled">+ 添加能力</button>

        <!-- 案例 / 评价 -->
        <div class="section-title">{{ form.templateId === 'freelancer' ? '💬 客户评价' : '🏆 成功案例 / 精选作品' }}</div>
        <p class="hint">{{ form.templateId === 'freelancer' ? '客户对你的评价，会显示五星评分' : '展示你的代表性项目或作品' }}</p>
        <div v-for="(cs, i) in content.cases" :key="'cs'+i" class="sub-item">
          <div class="sub-item-header">
            <span class="sub-item-label">{{ form.templateId === 'freelancer' ? '评价' : '案例' }} {{ i + 1 }}</span>
            <button type="button" class="btn-sm danger" @click="content.cases.splice(i, 1)" :disabled="!form.enabled">删除</button>
          </div>
          <div class="sub-item-row">
            <div class="sub-item-field">
              <label>标题</label>
              <input v-model="cs.title" class="inp" :placeholder="form.templateId === 'freelancer' ? '如：技术扎实，沟通顺畅' : '如：某大型电商平台'" :disabled="!form.enabled" />
            </div>
          </div>
          <div class="sub-item-row">
            <div class="sub-item-field half">
              <label>{{ form.templateId === 'freelancer' ? '评价者（可选）' : '行业标签（可选）' }}</label>
              <input v-model="cs.industry" class="inp" :placeholder="form.templateId === 'freelancer' ? '如：张总' : '如：电商、金融'" :disabled="!form.enabled" />
            </div>
          </div>
          <div class="sub-item-row">
            <div class="sub-item-field">
              <label>描述（可选）</label>
              <textarea v-model="cs.desc" class="inp textarea-sm" rows="2"
                :placeholder="form.templateId === 'freelancer' ? '客户评价内容' : '项目简介与成果'" :disabled="!form.enabled"></textarea>
            </div>
          </div>
        </div>
        <button type="button" class="btn add-btn" @click="content.cases.push({title:'',industry:'',desc:''})" :disabled="!form.enabled">+ 添加{{ form.templateId === 'freelancer' ? '评价' : '案例' }}</button>

        <!-- 技术栈（仅企业版和自由职业版显示） -->
        <template v-if="form.templateId !== 'studio'">
          <div class="section-title">🔧 {{ form.templateId === 'freelancer' ? '专业技能' : '技术栈' }}</div>
          <p class="hint">以标签形式展示，用逗号或回车分隔。</p>
          <input v-model="techStackText" class="inp" :placeholder="'Vue.js, React, Java, Python'" :disabled="!form.enabled" />
        </template>

        <!-- 高级：JSON 模式 -->
        <details class="json-toggle">
          <summary>🔧 高级：直接编辑 JSON</summary>
          <p class="hint">仅在需要精细控制时使用。表单内容会自动同步到此处，反之亦然。</p>
          <textarea v-model="form.contentJson" class="inp textarea" rows="10"
            placeholder='{ "slogan": "...", "stats": [...] }' :disabled="!form.enabled"></textarea>
        </details>
      </template>

      <!-- ========== 自定义 HTML ========== -->
      <template v-if="form.mode === 'custom_html'">
        <label>自定义 HTML 代码</label>
        <p class="hint">完整的 HTML 代码，将在 iframe 中以 sandbox 模式渲染。最大 50000 字符。</p>
        <textarea v-model="form.customHtml" class="inp textarea" rows="14"
          placeholder="<html><body>...</body></html>" :disabled="!form.enabled"></textarea>
        <p v-if="form.customHtml && form.customHtml.length > 50000" class="err">HTML 内容超出 50000 字符限制</p>
      </template>

      <div class="btn-row">
        <button type="submit" class="btn primary" :disabled="saving || !form.enabled">{{ saving ? '保存中…' : '保存配置' }}</button>
        <span v-if="msg" :class="ok ? 'ok' : 'err'">{{ msg }}</span>
      </div>
    </form>

    <!-- 预览区 -->
    <div v-if="form.enabled && previewUrl" class="preview-section">
      <h3>预览效果</h3>
      <p class="hint muted">以下为客户通过 Agent 链接看到的展示页效果</p>
      <div class="preview-frame-wrap">
        <iframe :src="previewUrl" class="preview-frame" />
      </div>
    </div>
  </div>
</template>

<script>
const EMPTY_CONTENT = {
  slogan: '',
  description: '',
  stats: [],
  capabilities: [],
  cases: [],
  techStack: []
}

export default {
  name: 'ShowcaseConfigView',
  data () {
    return {
      loading: true,
      saving: false,
      msg: '',
      ok: true,
      form: {
        enabled: false,
        mode: 'template',
        templateId: 'enterprise',
        contentJson: '',
        customHtml: ''
      },
      content: { ...EMPTY_CONTENT, stats: [], capabilities: [], cases: [] },
      syncLock: false
    }
  },
  computed: {
    previewUrl () {
      const base = window.location.origin
      return base + '/quick-quote/default'
    },
    techStackText: {
      get () {
        return (this.content.techStack || []).join(', ')
      },
      set (val) {
        this.content.techStack = val.split(/[,，\n]+/).map(s => s.trim()).filter(Boolean)
      }
    }
  },
  watch: {
    // 表单 → JSON 同步
    content: {
      handler () {
        if (this.syncLock) return
        this.syncLock = true
        try {
          this.form.contentJson = JSON.stringify(this.content, null, 2)
        } catch (e) { void e }
        this.$nextTick(() => { this.syncLock = false })
      },
      deep: true
    },
    // JSON → 表单同步
    'form.contentJson' (val) {
      if (this.syncLock) return
      if (!val || !val.trim()) {
        this.content = { ...EMPTY_CONTENT, stats: [], capabilities: [], cases: [] }
        return
      }
      try {
        const parsed = JSON.parse(val)
        this.syncLock = true
        this.content = {
          slogan: parsed.slogan || '',
          description: parsed.description || '',
          stats: Array.isArray(parsed.stats) ? parsed.stats.map(s => ({ label: s.label || '', value: s.value || '' })) : [],
          capabilities: Array.isArray(parsed.capabilities) ? parsed.capabilities.map(c => ({ title: c.title || '', desc: c.desc || '' })) : [],
          cases: Array.isArray(parsed.cases) ? parsed.cases.map(c => ({ title: c.title || '', industry: c.industry || '', desc: c.desc || '' })) : [],
          techStack: Array.isArray(parsed.techStack) ? parsed.techStack : []
        }
        this.$nextTick(() => { this.syncLock = false })
      } catch (e) { void e }
    }
  },
  async mounted () {
    await this.load()
  },
  methods: {
    async load () {
      this.loading = true
      try {
        const { data } = await this.$http.get('/admin/quote/showcase')
        if (data.code === 0 && data.data) {
          const d = data.data
          this.form.enabled = !!d.enabled
          this.form.mode = d.mode || 'template'
          this.form.templateId = d.templateId || 'enterprise'
          this.form.contentJson = d.contentJson || ''
          this.form.customHtml = d.customHtml || ''
        }
      } catch (e) { void e }
      finally { this.loading = false }
    },
    async save () {
      this.saving = true
      this.msg = ''
      try {
        const body = {
          enabled: !!this.form.enabled,
          mode: this.form.mode,
          templateId: this.form.templateId,
          contentJson: (this.form.contentJson || '').trim(),
          customHtml: (this.form.customHtml || '').trim()
        }
        const { data } = await this.$http.put('/admin/quote/showcase', body)
        if (data.code === 0) {
          this.msg = '已保存'
          this.ok = true
        } else {
          this.msg = (data && data.message) || '保存失败'
          this.ok = false
        }
      } catch (e) {
        this.msg = '请求失败'
        this.ok = false
      } finally {
        this.saving = false
      }
    }
  }
}
</script>

<style scoped>
.showcase-config-page {
  max-width: 760px;
  margin: 0 auto;
  padding: 24px 16px;
}
.page-header {
  margin-bottom: 24px;
}
.page-header h2 {
  margin: 8px 0 4px;
}
.back-btn {
  background: none;
  border: 1px solid #ddd;
  border-radius: 6px;
  padding: 4px 12px;
  cursor: pointer;
  font-size: 14px;
}
.back-btn:hover {
  background: #f5f5f5;
}
.form {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.form label {
  font-weight: 600;
  font-size: 14px;
}
.form .chk {
  font-weight: 400;
  display: flex;
  align-items: center;
  gap: 6px;
}
.inp {
  padding: 8px 10px;
  border: 1px solid #d0d0d0;
  border-radius: 6px;
  font-size: 14px;
  width: 100%;
  box-sizing: border-box;
}
.textarea {
  font-family: monospace;
  resize: vertical;
  min-height: 100px;
}
.textarea-sm {
  min-height: 60px;
  font-family: inherit;
}
.hint {
  font-size: 12px;
  color: #888;
  margin: 0;
}
.muted {
  color: #888;
}
.err {
  color: #e53935;
  font-size: 13px;
}
.ok {
  color: #43a047;
  font-size: 13px;
}
.btn-row {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-top: 8px;
}
.btn {
  padding: 8px 20px;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  font-size: 14px;
}
.btn.primary {
  background: #1976d2;
  color: #fff;
}
.btn.primary:hover {
  background: #1565c0;
}
.btn.primary:disabled {
  background: #90caf9;
  cursor: not-allowed;
}
.add-btn {
  background: #f5f5f5;
  border: 1px dashed #bbb;
  color: #555;
  font-size: 13px;
  padding: 6px 14px;
  margin-top: 4px;
}
.add-btn:hover {
  background: #eee;
}
.section-title {
  font-size: 15px;
  font-weight: 700;
  margin: 16px 0 6px;
  padding-bottom: 4px;
  border-bottom: 1px solid #eee;
}
.sub-item {
  background: #fafafa;
  border: 1px solid #eee;
  border-radius: 8px;
  padding: 10px 12px;
  margin-bottom: 8px;
}
.sub-item-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 6px;
}
.sub-item-label {
  font-size: 13px;
  font-weight: 600;
  color: #555;
}
.sub-item-row {
  display: flex;
  gap: 8px;
  margin-bottom: 4px;
}
.sub-item-field {
  flex: 1;
  min-width: 0;
}
.sub-item-field.half {
  flex: 0 0 calc(50% - 4px);
}
.sub-item-field label {
  font-size: 12px;
  font-weight: 400;
  color: #888;
}
.btn-sm {
  padding: 2px 8px;
  border: 1px solid #e0e0e0;
  border-radius: 4px;
  background: #fff;
  cursor: pointer;
  font-size: 12px;
  color: #e53935;
}
.btn-sm:hover {
  background: #ffebee;
}
.btn-sm:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}
.json-toggle {
  margin-top: 16px;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  padding: 8px 12px;
}
.json-toggle summary {
  cursor: pointer;
  font-size: 13px;
  color: #666;
  user-select: none;
}
.json-toggle .hint {
  margin-top: 4px;
}
.preview-section {
  margin-top: 32px;
}
.preview-section h3 {
  margin-bottom: 4px;
}
.preview-frame-wrap {
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  overflow: hidden;
  margin-top: 8px;
}
.preview-frame {
  width: 100%;
  height: 600px;
  border: none;
}
</style>
