<template>
  <div class="cloud-dev-page">
    <header class="cloud-dev-hero">
      <h1 class="cloud-dev-title">{{ $t('cloudDev.pageTitle') }}</h1>
      <p class="cloud-dev-lead">{{ $t('cloudDev.pageLead') }}</p>
      <p class="cloud-dev-disclaimer">{{ $t('cloudDev.disclaimer') }}</p>
    </header>

    <div class="cloud-dev-layout">
      <aside class="cloud-dev-sidebar">
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

      <main class="cloud-dev-main">
        <section v-if="current" class="cloud-dev-panel">
          <h2 class="cloud-dev-panel-title">{{ $t(current.nameKey) }}</h2>
          <p class="cloud-dev-panel-desc">{{ $t(current.descKey) }}</p>

          <div class="cloud-dev-actions">
            <template v-if="current.url">
              <a
                :href="current.url"
                class="cloud-dev-btn cloud-dev-btn--primary"
                target="_blank"
                rel="noopener noreferrer"
              >{{ $t('cloudDev.openNewTab') }}</a>
            </template>
            <template v-else-if="current.links && current.links.length">
              <a
                v-for="(lnk, idx) in current.links"
                :key="idx"
                :href="lnk.url"
                class="cloud-dev-btn"
                :class="{ 'cloud-dev-btn--primary': idx === 0 }"
                target="_blank"
                rel="noopener noreferrer"
              >{{ $t(lnk.labelKey) }}</a>
            </template>
            <a
              v-if="current.docUrl"
              :href="current.docUrl"
              class="cloud-dev-btn cloud-dev-btn--ghost"
              target="_blank"
              rel="noopener noreferrer"
            >{{ $t('cloudDev.openDocs') }}</a>
            <button
              type="button"
              class="cloud-dev-btn cloud-dev-btn--secondary"
              :class="{ 'is-on': embedEnabled }"
              @click="toggleEmbed"
            >{{ embedEnabled ? $t('cloudDev.closeEmbed') : $t('cloudDev.tryEmbed') }}</button>
          </div>

          <p v-if="embedEnabled" class="cloud-dev-embed-hint">{{ $t('cloudDev.embedHint') }}</p>

          <div
            v-if="embedEnabled && current.id === 'trae'"
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

          <div v-if="embedEnabled && embedSrc" class="cloud-dev-frame-wrap">
            <iframe
              :key="embedSrc"
              class="cloud-dev-frame"
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
      embedEnabled: false,
      traeEmbedPick: 0
    }
  },
  computed: {
    current () {
      return this.providers.find(p => p.id === this.selectedId) || null
    },
    embedSrc () {
      if (!this.current || !this.embedEnabled) return ''
      if (this.current.url) return this.current.url
      if (this.current.links && this.current.links[this.traeEmbedPick]) {
        return this.current.links[this.traeEmbedPick].url
      }
      return ''
    }
  },
  methods: {
    selectProvider (id) {
      this.selectedId = id
      this.embedEnabled = false
      if (id !== 'trae') this.traeEmbedPick = 0
    },
    toggleEmbed () {
      this.embedEnabled = !this.embedEnabled
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
@media (max-width: 768px) {
  .cloud-dev-layout {
    grid-template-columns: 1fr;
  }
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
}
.cloud-dev-frame {
  display: block;
  width: 100%;
  height: min(72vh, 640px);
  border: none;
  background: #fff;
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
