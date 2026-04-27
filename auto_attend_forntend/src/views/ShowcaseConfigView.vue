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

      <template v-if="form.mode === 'template'">
        <label>模板选择</label>
        <select v-model="form.templateId" class="inp" :disabled="!form.enabled">
          <option value="enterprise">企业版（蓝色商务风）</option>
          <option value="studio">工作室版（紫色创意风）</option>
          <option value="freelancer">自由职业版（青色简约风）</option>
        </select>
        <label>展示内容 JSON</label>
        <p class="hint">JSON 格式，包含 teamName、slogan、description、services、team、works、reviews 等字段。留空使用模板默认内容。</p>
        <textarea v-model="form.contentJson" class="inp textarea" rows="12"
          placeholder='{"teamName":"示例科技","slogan":"专业软件定制","services":[...]}' :disabled="!form.enabled"></textarea>
      </template>

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
      <p class="hint muted">以下为客户通过 Agent 链接看到的展示页效果（仅展示区部分）</p>
      <div class="preview-frame-wrap">
        <iframe :src="previewUrl" class="preview-frame" sandbox="allow-scripts" />
      </div>
    </div>
  </div>
</template>

<script>
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
      }
    }
  },
  computed: {
    previewUrl () {
      // 使用公开接口预览
      const base = window.location.origin
      return base + '/quick-quote/default'
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
  max-width: 720px;
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
  gap: 12px;
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
  min-height: 120px;
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
