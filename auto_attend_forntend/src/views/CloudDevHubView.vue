<template>
  <div class="cloud-dev-page" :class="{ 'is-embed-active': isEmbedLayout }">
    <div class="cloud-dev-open-mode" role="radiogroup" :aria-label="$t('cloudDev.openModeLabel')">
      <span class="cloud-dev-open-mode-label">{{ $t('cloudDev.openModeLabel') }}</span>
      <div class="cloud-dev-open-mode-seg">
        <button
          v-for="opt in openModeOptions"
          :key="opt.value"
          type="button"
          role="radio"
          :aria-checked="openMode === opt.value"
          class="cloud-dev-open-mode-btn"
          :class="{ 'is-active': openMode === opt.value }"
          @click="setOpenMode(opt.value)"
        >{{ $t(opt.labelKey) }}</button>
      </div>
      <p class="cloud-dev-open-mode-hint">{{ $t('cloudDev.openModeHint') }}</p>
    </div>

    <header v-if="!isEmbedLayout" class="cloud-dev-hero">
      <h1 class="cloud-dev-title">{{ $t('cloudDev.pageTitle') }}</h1>
      <p class="cloud-dev-lead">{{ $t('cloudDev.pageLead') }}</p>
      <p class="cloud-dev-disclaimer">{{ $t('cloudDev.disclaimer') }}</p>
    </header>

    <div class="cloud-dev-layout" :class="{ 'cloud-dev-layout--embed': isEmbedLayout }">
      <aside v-if="!isEmbedLayout" class="cloud-dev-sidebar">
        <button
          v-for="p in providers"
          :key="p.id"
          type="button"
          class="cloud-dev-tab"
          :class="{ 'is-active': selectedId === p.id }"
          @click="selectProvider(p.id)"
        >
          <span class="cloud-dev-tab-name">{{ $t(p.nameKey) }}</span>
        </button>
      </aside>
      <div v-else class="cloud-dev-tabs-top" role="tablist" :aria-label="$t('cloudDev.pageTitle')">
        <button
          v-for="p in providers"
          :key="'top-' + p.id"
          type="button"
          class="cloud-dev-tab cloud-dev-tab--top"
          :class="{ 'is-active': selectedId === p.id }"
          role="tab"
          :aria-selected="selectedId === p.id"
          @click="selectProvider(p.id)"
        >
          <span class="cloud-dev-tab-name">{{ $t(p.nameKey) }}</span>
        </button>
      </div>

      <main class="cloud-dev-main">
        <section v-if="current" class="cloud-dev-panel" :class="{ 'cloud-dev-panel--embed': isEmbedLayout }">
          <template v-if="!isEmbedLayout">
            <h2 class="cloud-dev-panel-title">{{ $t(current.nameKey) }}</h2>
            <p class="cloud-dev-panel-desc">{{ $t(current.descKey) }}</p>
          </template>
          <div v-else class="cloud-dev-embed-toolbar">
            <span class="cloud-dev-embed-toolbar-title">{{ $t(current.nameKey) }}</span>
          </div>

          <div class="cloud-dev-actions">
            <template v-if="current.url">
              <button
                v-if="!isEmbedLayout"
                type="button"
                class="cloud-dev-btn cloud-dev-btn--primary"
                @click="openByMode(current.url)"
              >{{ primaryOpenLabel }}</button>
            </template>
            <template v-else-if="current.links && current.links.length && !isEmbedLayout">
              <button
                v-for="(lnk, idx) in current.links"
                :key="idx"
                type="button"
                class="cloud-dev-btn"
                :class="{ 'cloud-dev-btn--primary': idx === 0 }"
                @click="openByMode(lnk.url)"
              >{{ $t(lnk.labelKey) }}</button>
            </template>
            <a
              v-if="current.docUrl"
              :href="current.docUrl"
              class="cloud-dev-btn cloud-dev-btn--ghost"
              target="_blank"
              rel="noopener noreferrer"
            >{{ $t('cloudDev.openDocs') }}</a>
            <a
              v-if="isEmbedLayout && (current.url || (current.links && current.links.length))"
              :href="embedLinkHref"
              class="cloud-dev-btn cloud-dev-btn--secondary"
              target="_blank"
              rel="noopener noreferrer"
            >{{ $t('cloudDev.openNewTab') }}</a>
          </div>

          <p v-if="isEmbedLayout" class="cloud-dev-embed-hint">{{ $t('cloudDev.embedHint') }}</p>

          <div
            v-if="isEmbedLayout && current.id === 'trae'"
            class="cloud-dev-trae-pick"
            role="group"
            :aria-label="$t('cloudDev.traeEmbedPickHint')"
          >
            <span class="cloud-dev-trae-pick-label">{{ $t('cloudDev.traeEmbedPickHint') }}</span>
            <button
              type="button"
              class="cloud-dev-seg"
              :class="{ 'is-active': traeEmbedPick === 0 }"
              @click="traeEmbedPick = 0"
            >{{ $t('cloudDev.traeCn') }}</button>
            <button
              type="button"
              class="cloud-dev-seg"
              :class="{ 'is-active': traeEmbedPick === 1 }"
              @click="traeEmbedPick = 1"
            >{{ $t('cloudDev.traeIntl') }}</button>
          </div>

          <div v-if="isEmbedLayout && embedSrc" class="cloud-dev-frame-wrap">
            <iframe
              :key="embedSrc"
              class="cloud-dev-frame"
              :class="{ 'cloud-dev-frame--max': isEmbedLayout }"
              :src="embedSrc"
              :title="$t(current.nameKey)"
              sandbox="allow-scripts allow-same-origin allow-forms allow-popups allow-popups-to-escape-sandbox allow-downloads"
              referrerpolicy="no-referrer-when-downgrade"
            />
            <p class="cloud-dev-embed-fallback">{{ $t('cloudDev.embedFallback') }}</p>
          </div>
        </section>
      </main>
    </div>
  </div>
</template>

<script>
const OPEN_MODE_STORAGE_KEY = 'autoattend-clouddev-open-mode'
const OPEN_MODES = ['newTab', 'sameWindow', 'embed']

/** 第三方云 Agent 入口（官方网页）；内嵌受 X-Frame-Options 等限制可能为空白 */
const PROVIDERS = [
  {
    id: 'cursor',
    nameKey: 'cloudDev.cursorName',
    descKey: 'cloudDev.cursorDesc',
    url: 'https://cursor.com/agents',
    docUrl: 'https://cursor.com/docs/cloud-agent/web-and-mobile'
  },
  {
    id: 'trae',
    nameKey: 'cloudDev.traeName',
    descKey: 'cloudDev.traeDesc',
    links: [
      { labelKey: 'cloudDev.traeCn', url: 'https://solo.trae.cn' },
      { labelKey: 'cloudDev.traeIntl', url: 'https://solo.trae.ai' }
    ],
    docUrl: 'https://docs.trae.cn/'
  },
  {
    id: 'codex',
    nameKey: 'cloudDev.codexName',
    descKey: 'cloudDev.codexDesc',
    url: 'https://chatgpt.com/codex',
    docUrl: 'https://developers.openai.com/codex/'
  }
]

export default {
  name: 'CloudDevHubView',
  data () {
    return {
      providers: PROVIDERS,
      selectedId: 'cursor',
      openMode: 'newTab',
      traeEmbedPick: 0,
      openModeOptions: [
        { value: 'newTab', labelKey: 'cloudDev.openModeNewTab' },
        { value: 'sameWindow', labelKey: 'cloudDev.openModeSameWindow' },
        { value: 'embed', labelKey: 'cloudDev.openModeEmbed' }
      ]
    }
  },
  computed: {
    current () {
      return this.providers.find(p => p.id === this.selectedId) || null
    },
    isEmbedLayout () {
      return this.openMode === 'embed'
    },
    embedSrc () {
      if (!this.current || this.openMode !== 'embed') return ''
      if (this.current.url) return this.current.url
      if (this.current.links && this.current.links[this.traeEmbedPick]) {
        return this.current.links[this.traeEmbedPick].url
      }
      return ''
    },
    embedLinkHref () {
      if (!this.current) return '#'
      if (this.current.url) return this.current.url
      if (this.current.links && this.current.links[this.traeEmbedPick]) {
        return this.current.links[this.traeEmbedPick].url
      }
      return '#'
    },
    primaryOpenLabel () {
      return this.openMode === 'sameWindow'
        ? this.$t('cloudDev.openSameWindow')
        : this.$t('cloudDev.openNewTab')
    }
  },
  created () {
    this.openMode = this.readStoredOpenMode()
  },
  watch: {
    openMode (v) {
      this.persistOpenMode(v)
      this.notifyAppEmbed(v === 'embed')
    }
  },
  mounted () {
    this.persistOpenMode(this.openMode)
    this.notifyAppEmbed(this.openMode === 'embed')
  },
  beforeDestroy () {
    this.notifyAppEmbed(false)
  },
  methods: {
    readStoredOpenMode () {
      try {
        const raw = window.localStorage.getItem(OPEN_MODE_STORAGE_KEY)
        if (raw && OPEN_MODES.includes(raw)) return raw
      } catch (e) { /* ignore */ }
      return 'newTab'
    },
    persistOpenMode (mode) {
      try {
        window.localStorage.setItem(OPEN_MODE_STORAGE_KEY, mode)
      } catch (e) { /* ignore */ }
    },
    setOpenMode (mode) {
      if (!OPEN_MODES.includes(mode)) return
      this.openMode = mode
    },
    openByMode (url) {
      if (!url) return
      if (this.openMode === 'sameWindow') {
        window.location.assign(url)
        return
      }
      window.open(url, '_blank', 'noopener,noreferrer')
    },
    notifyAppEmbed (active) {
      try {
        window.dispatchEvent(new CustomEvent('autoattend-clouddev-embed', { detail: { active } }))
      } catch (e) {
        /* ignore */
      }
    },
    selectProvider (id) {
      this.selectedId = id
      if (id !== 'trae') this.traeEmbedPick = 0
    }
  }
}
</script>

<style scoped>
.cloud-dev-page {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 var(--space-md, 16px) var(--space-xxl, 48px);
  box-sizing: border-box;
}
.cloud-dev-open-mode {
  margin-bottom: var(--space-lg, 24px);
  padding: 14px 16px;
  border-radius: 10px;
  border: 1px solid var(--border-primary, #dee0e3);
  background: var(--bg-hover, #f8f9fa);
}
.cloud-dev-open-mode-label {
  display: block;
  font-size: 13px;
  font-weight: 600;
  color: var(--text-primary, #1F2329);
  margin-bottom: 10px;
}
.cloud-dev-open-mode-seg {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
}
.cloud-dev-open-mode-btn {
  padding: 8px 14px;
  border-radius: 8px;
  border: 1px solid var(--border-primary, #dee0e3);
  background: #fff;
  font-size: 13px;
  font-weight: 500;
  color: var(--text-secondary, #646A73);
  cursor: pointer;
  transition: border-color 0.15s, background 0.15s, color 0.15s;
}
.cloud-dev-open-mode-btn:hover {
  border-color: var(--brand-blue, #1456F0);
  color: var(--brand-blue, #1456F0);
}
.cloud-dev-open-mode-btn.is-active {
  border-color: var(--brand-blue, #1456F0);
  background: #F0F5FF;
  color: var(--brand-blue, #1456F0);
}
.cloud-dev-open-mode-hint {
  margin: 10px 0 0;
  font-size: 12px;
  line-height: 1.5;
  color: var(--text-disabled, #8F959E);
}
.cloud-dev-page.is-embed-active {
  max-width: none;
  margin: 0;
  padding: 0 0 var(--space-md, 16px);
  display: flex;
  flex-direction: column;
  flex: 1;
  min-height: 0;
}
.cloud-dev-page.is-embed-active .cloud-dev-layout--embed {
  flex: 1;
  min-height: 0;
}
.cloud-dev-layout--embed .cloud-dev-main {
  flex: 1;
  min-width: 0;
  min-height: 0;
  display: flex;
  flex-direction: column;
}
.cloud-dev-hero {
  margin-bottom: var(--space-lg, 24px);
}
.cloud-dev-title {
  margin: 0 0 var(--space-sm, 8px);
  font-size: 22px;
  font-weight: 700;
  color: var(--text-primary, #1F2329);
}
.cloud-dev-lead {
  margin: 0 0 var(--space-sm, 8px);
  font-size: 14px;
  line-height: 1.55;
  color: var(--text-secondary, #646A73);
  max-width: 52rem;
}
.cloud-dev-disclaimer {
  margin: 0;
  font-size: 12px;
  color: var(--text-disabled, #8F959E);
  line-height: 1.5;
  max-width: 52rem;
}
.cloud-dev-layout {
  display: grid;
  grid-template-columns: 200px 1fr;
  gap: 20px;
  align-items: start;
}
.cloud-dev-layout--embed {
  display: flex;
  flex-direction: column;
  gap: 10px;
  align-items: stretch;
}
@media (max-width: 768px) {
  .cloud-dev-layout:not(.cloud-dev-layout--embed) {
    grid-template-columns: 1fr;
  }
}
.cloud-dev-tabs-top {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  padding: 0 2px;
}
.cloud-dev-tab--top {
  flex: 0 1 auto;
  min-width: 0;
}
.cloud-dev-embed-toolbar {
  margin: 0 0 10px;
  padding-bottom: 2px;
  border-bottom: 1px solid var(--border-primary, #dee0e3);
}
.cloud-dev-embed-toolbar-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--text-primary, #1F2329);
}
.cloud-dev-sidebar {
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.cloud-dev-tab {
  text-align: left;
  padding: 10px 14px;
  border-radius: 8px;
  border: 1px solid var(--border-primary, #dee0e3);
  background: #fff;
  font-size: 13px;
  font-weight: 500;
  color: var(--text-secondary, #646A73);
  cursor: pointer;
  transition: border-color 0.15s, background 0.15s;
}
.cloud-dev-tab:hover {
  border-color: var(--brand-blue, #1456F0);
  color: var(--brand-blue, #1456F0);
}
.cloud-dev-tab.is-active {
  border-color: var(--brand-blue, #1456F0);
  background: #F0F5FF;
  color: var(--brand-blue, #1456F0);
}
.cloud-dev-main {
  min-width: 0;
}
.cloud-dev-panel {
  background: #fff;
  border: 1px solid var(--border-primary, #dee0e3);
  border-radius: 12px;
  padding: 20px 22px;
}
.cloud-dev-panel--embed {
  padding: 12px 14px 14px;
  border-radius: 10px;
  display: flex;
  flex-direction: column;
  flex: 1;
  min-height: 0;
}
.cloud-dev-panel-title {
  margin: 0 0 8px;
  font-size: 17px;
  font-weight: 600;
}
.cloud-dev-panel-desc {
  margin: 0 0 16px;
  font-size: 13px;
  line-height: 1.55;
  color: var(--text-secondary, #646A73);
}
.cloud-dev-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  align-items: center;
  margin-bottom: 12px;
}
.cloud-dev-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 8px 16px;
  border-radius: 8px;
  font-size: 13px;
  font-weight: 500;
  text-decoration: none;
  border: 1px solid var(--border-primary, #dee0e3);
  background: #fff;
  color: var(--text-primary, #1F2329);
  cursor: pointer;
  transition: opacity 0.15s, border-color 0.15s;
}
.cloud-dev-btn:hover {
  border-color: var(--brand-blue, #1456F0);
  color: var(--brand-blue, #1456F0);
}
.cloud-dev-btn--primary {
  background: var(--brand-blue, #1456F0);
  border-color: var(--brand-blue, #1456F0);
  color: #fff;
}
.cloud-dev-btn--primary:hover {
  opacity: 0.9;
  color: #fff;
}
.cloud-dev-btn--ghost {
  background: transparent;
}
.cloud-dev-btn--secondary {
  background: var(--bg-hover, #f5f6f7);
}
.cloud-dev-btn--secondary.is-on {
  border-color: var(--brand-blue, #1456F0);
  color: var(--brand-blue, #1456F0);
}
.cloud-dev-embed-hint {
  font-size: 12px;
  color: var(--text-disabled, #8F959E);
  margin: 0 0 10px;
  line-height: 1.45;
}
.cloud-dev-trae-pick {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  margin-bottom: 10px;
}
.cloud-dev-trae-pick-label {
  font-size: 12px;
  color: var(--text-secondary, #646A73);
  margin-right: 4px;
}
.cloud-dev-seg {
  padding: 6px 12px;
  border-radius: 6px;
  border: 1px solid var(--border-primary, #dee0e3);
  background: #fff;
  font-size: 12px;
  color: var(--text-secondary, #646A73);
  cursor: pointer;
}
.cloud-dev-seg.is-active {
  border-color: var(--brand-blue, #1456F0);
  background: #F0F5FF;
  color: var(--brand-blue, #1456F0);
  font-weight: 500;
}
.cloud-dev-frame-wrap {
  position: relative;
  border-radius: 8px;
  overflow: hidden;
  border: 1px solid var(--border-primary, #dee0e3);
  background: var(--bg-page, #f0f2f5);
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
}
.cloud-dev-panel--embed .cloud-dev-frame-wrap {
  flex: 1 1 auto;
}
.cloud-dev-frame {
  display: block;
  width: 100%;
  height: min(72vh, 640px);
  border: none;
  background: #fff;
  flex: 1 1 auto;
  min-height: min(72vh, 640px);
}
.cloud-dev-frame--max {
  height: calc(100vh - 200px);
  min-height: 420px;
  max-height: none;
}
@media (max-width: 768px) {
  .cloud-dev-frame--max {
    height: calc(100vh - 220px);
    min-height: 320px;
  }
}
.cloud-dev-embed-fallback {
  margin: 0;
  padding: 8px 12px;
  font-size: 11px;
  color: var(--text-disabled, #8F959E);
  background: var(--bg-hover, #f5f6f7);
  border-top: 1px solid var(--border-primary, #dee0e3);
}
</style>
