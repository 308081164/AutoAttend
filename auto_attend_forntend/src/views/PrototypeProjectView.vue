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

          <div class="section-card">
            <div class="section-title">导出</div>
            <div class="section-body">
              <button type="button" class="primary-button" :disabled="!activeSpecJson" @click="exportSpecAndPreview">
                导出 spec + preview
              </button>
              <div v-if="exportError" class="error-msg">{{ exportError }}</div>
            </div>
          </div>
        </div>

        <div class="main">
          <div class="section-card">
            <div class="section-title">生成 spec</div>
            <div class="section-body">
              <label class="label">页面需求</label>
              <textarea
                v-model="prompt"
                class="prompt-textarea"
                placeholder="例如：制作一个可切换面板的登录页原型，包含卡片、按钮与 Tabs。"
              ></textarea>
              <div class="actions-row">
                <button type="button" class="primary-button" :disabled="generating" @click="generateSpec">
                  {{ generating ? '生成中…' : '生成并预览' }}
                </button>
                <router-link to="/ai-config" class="secondary-button">AI 配置</router-link>
              </div>
              <div v-if="genError" class="error-msg">{{ genError }}</div>
            </div>
          </div>

          <div class="section-card">
            <div class="section-title">预览（MVP：点击态/切换面板/Tabs）</div>
            <div class="section-body">
              <div ref="previewRoot" class="preview-root"></div>
              <div v-if="specParseError" class="error-msg">{{ specParseError }}</div>
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
      specs: [],
      activeSpecId: null,
      prompt: '',
      generating: false,
      genError: '',
      specParseError: '',
      exportError: ''
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
    }
  },
  created () {
    this.projectId = this.$route && this.$route.params ? this.$route.params.projectId : null
    this.load()
  },
  watch: {
    activeSpecId () {
      this.renderActiveSpec()
    }
  },
  methods: {
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
    async generateSpec () {
      this.generating = true
      this.genError = ''
      this.specParseError = ''
      this.exportError = ''
      try {
        const resp = await this.$http.post(`/admin/ui-prototype/projects/${this.projectId}/specs/generate`, { prompt: this.prompt })
        if (resp.data && resp.data.code === 0 && resp.data.data) {
          await this.load()
        } else {
          this.genError = (resp.data && resp.data.message) || '生成失败'
        }
      } catch (e) {
        this.genError = (e.response && e.response.data && e.response.data.message) || '生成失败'
      } finally {
        this.generating = false
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
  max-width: 1200px;
  margin: 0 auto;
  padding: 16px;
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
  grid-template-columns: 320px 1fr;
  gap: 14px;
}
.left .section-card {
  margin-bottom: 14px;
}
.section-card {
  background: #ffffff;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  padding: 12px;
}
.section-title {
  font-size: 14px;
  font-weight: 900;
  color: #0f172a;
  margin-bottom: 10px;
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
  min-height: 240px;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  background: #f8fafc;
  padding: 10px;
  overflow: auto;
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

