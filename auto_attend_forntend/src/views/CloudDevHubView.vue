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
              <div class="cloud-dev-actions-row">
                <button
                  type="button"
                  class="cloud-dev-btn cloud-dev-btn--primary"
                  @click="openInNewTab(current.url)"
                >{{ $t('cloudDev.openNewTab') }}</button>
                <button
                  type="button"
                  class="cloud-dev-btn cloud-dev-btn--secondary"
                  @click="openSameWindow(current.url)"
                >{{ $t('cloudDev.openSameWindow') }}</button>
              </div>
            </template>
            <template v-else-if="current.links && current.links.length">
              <div
                v-for="(lnk, idx) in current.links"
                :key="idx"
                class="cloud-dev-link-row"
              >
                <span class="cloud-dev-link-label">{{ $t(lnk.labelKey) }}</span>
                <div class="cloud-dev-link-row-actions">
                  <button
                    type="button"
                    class="cloud-dev-btn cloud-dev-btn--primary"
                    @click="openInNewTab(lnk.url)"
                  >{{ $t('cloudDev.openNewTab') }}</button>
                  <button
                    type="button"
                    class="cloud-dev-btn cloud-dev-btn--secondary"
                    @click="openSameWindow(lnk.url)"
                  >{{ $t('cloudDev.openSameWindow') }}</button>
                </div>
              </div>
            </template>
            <a
              v-if="current.docUrl"
              :href="current.docUrl"
              class="cloud-dev-btn cloud-dev-btn--ghost"
              target="_blank"
              rel="noopener noreferrer"
            >{{ $t('cloudDev.openDocs') }}</a>
          </div>
        </section>
      </main>
    </div>
  </div>
</template>

<script>
/** 第三方云 Agent 入口（官方网页） */
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
      selectedId: 'cursor'
    }
  },
  computed: {
    current () {
      return this.providers.find(p => p.id === this.selectedId) || null
    }
  },
  methods: {
    openInNewTab (url) {
      if (!url) return
      window.open(url, '_blank', 'noopener,noreferrer')
    },
    openSameWindow (url) {
      if (!url) return
      window.location.assign(url)
    },
    selectProvider (id) {
      this.selectedId = id
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
  flex-direction: column;
  gap: 14px;
  align-items: stretch;
}
.cloud-dev-actions-row {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  align-items: center;
}
.cloud-dev-link-row {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 12px 0;
  border-bottom: 1px solid var(--border-primary, #eef0f3);
}
.cloud-dev-link-row:last-child {
  border-bottom: none;
  padding-bottom: 0;
}
.cloud-dev-link-label {
  font-size: 13px;
  font-weight: 600;
  color: var(--text-primary, #1F2329);
  min-width: 0;
}
.cloud-dev-link-row-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
.cloud-dev-actions > .cloud-dev-btn,
.cloud-dev-actions > a.cloud-dev-btn {
  align-self: flex-start;
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
.cloud-dev-btn--secondary {
  background: var(--bg-hover, #f5f6f7);
}
.cloud-dev-btn--ghost {
  background: transparent;
}
</style>
