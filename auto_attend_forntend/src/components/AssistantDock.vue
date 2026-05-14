<template>
  <div class="assistant-root">
    <button
      type="button"
      class="assistant-fab"
      :aria-label="$t('assistant.openFab')"
      @click="open = true"
    >{{ $t('assistant.fabLabel') }}</button>

    <transition name="assistant-backdrop-fade">
      <div v-if="open" class="assistant-backdrop" @click.self="open = false" />
    </transition>

    <transition name="assistant-drawer-slide">
      <aside v-if="open" class="assistant-drawer" role="dialog" :aria-label="$t('assistant.title')">
        <header class="assistant-head">
          <h2 class="assistant-title">{{ $t('assistant.title') }}</h2>
          <button type="button" class="assistant-close" @click="open = false" :aria-label="$t('assistant.close')">×</button>
        </header>
        <p class="assistant-hint">{{ $t('assistant.hint') }}</p>
        <div class="assistant-messages" ref="scrollBox">
          <div
            v-for="(m, i) in messages"
            :key="'m-' + i"
            class="assistant-bubble"
            :class="'assistant-bubble--' + m.role"
          >
            <span class="assistant-bubble-role">{{ roleLabel(m.role) }}</span>
            <div class="assistant-bubble-text">{{ m.content }}</div>
          </div>
        </div>
        <div class="assistant-input-row">
          <textarea
            v-model="draft"
            rows="2"
            class="assistant-input"
            :placeholder="$t('assistant.placeholder')"
            @keydown.enter.exact.prevent="send"
          />
          <button type="button" class="primary-button assistant-send" :disabled="sending || !draft.trim()" @click="send">
            {{ sending ? '…' : $t('assistant.send') }}
          </button>
        </div>
        <div class="assistant-foot">
          <button type="button" class="link-button" @click="clearMemory">{{ $t('assistant.clearHistory') }}</button>
        </div>
      </aside>
    </transition>
  </div>
</template>

<script>
const STORAGE_KEY = 'autoattend-assistant-messages-v1'

export default {
  name: 'AssistantDock',
  data () {
    return {
      open: false,
      draft: '',
      sending: false,
      messages: []
    }
  },
  watch: {
    open (v) {
      if (v) {
        this.loadFromStorage()
        this.$nextTick(() => this.scrollBottom())
      }
    }
  },
  methods: {
    roleLabel (role) {
      if (role === 'assistant') return this.$t('assistant.roleAssistant')
      return this.$t('assistant.roleUser')
    },
    loadFromStorage () {
      try {
        const raw = window.localStorage.getItem(STORAGE_KEY)
        const arr = raw ? JSON.parse(raw) : null
        this.messages = Array.isArray(arr) && arr.length
          ? arr
          : [{ role: 'assistant', content: this.$t('assistant.welcome') }]
      } catch (e) {
        this.messages = [{ role: 'assistant', content: this.$t('assistant.welcome') }]
      }
    },
    persist () {
      try {
        window.localStorage.setItem(STORAGE_KEY, JSON.stringify(this.messages.slice(-80)))
      } catch (e) { /* ignore */ }
    },
    scrollBottom () {
      const el = this.$refs.scrollBox
      if (el) el.scrollTop = el.scrollHeight
    },
    clearMemory () {
      this.messages = [{ role: 'assistant', content: this.$t('assistant.cleared') }]
      this.persist()
    },
    async send () {
      const text = (this.draft || '').trim()
      if (!text || this.sending) return
      this.messages.push({ role: 'user', content: text })
      this.draft = ''
      this.persist()
      this.$nextTick(() => this.scrollBottom())
      this.sending = true
      try {
        const payload = {
          messages: this.messages.map(m => ({ role: m.role, content: m.content }))
        }
        const resp = await this.$http.post('/admin/assistant/qwen-chat', payload)
        if (resp.data && resp.data.code === 0 && resp.data.data) {
          const c = resp.data.data.content != null ? String(resp.data.data.content) : ''
          this.messages.push({ role: 'assistant', content: c || '—' })
        } else {
          this.messages.push({
            role: 'assistant',
            content: (resp.data && resp.data.message) || this.$t('assistant.errorGeneric')
          })
        }
      } catch (e) {
        const msg = (e.response && e.response.data && e.response.data.message) || this.$t('assistant.errorGeneric')
        this.messages.push({ role: 'assistant', content: msg })
      } finally {
        this.sending = false
        this.persist()
        this.$nextTick(() => this.scrollBottom())
      }
    }
  }
}
</script>

<style scoped>
.assistant-root {
  position: relative;
  z-index: 1200;
}
.assistant-fab {
  position: fixed;
  right: 20px;
  bottom: 22px;
  padding: 10px 14px;
  border-radius: 999px;
  border: none;
  background: var(--brand-blue, #1456f0);
  color: #fff;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  box-shadow: 0 4px 14px rgba(20, 86, 240, 0.35);
}
.assistant-fab:hover {
  filter: brightness(1.05);
}
.assistant-backdrop {
  position: fixed;
  inset: 0;
  background: rgba(15, 23, 42, 0.35);
}
.assistant-drawer {
  position: fixed;
  top: 0;
  right: 0;
  width: 30vw;
  min-width: 280px;
  max-width: 480px;
  height: 100vh;
  background: var(--bg-card, #fff);
  box-shadow: -4px 0 24px rgba(15, 23, 42, 0.12);
  display: flex;
  flex-direction: column;
  z-index: 1201;
}
.assistant-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 16px;
  border-bottom: 1px solid var(--border-primary, #e5e7eb);
}
.assistant-title {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
}
.assistant-close {
  border: none;
  background: transparent;
  font-size: 22px;
  line-height: 1;
  cursor: pointer;
  color: var(--text-secondary, #646a73);
}
.assistant-hint {
  margin: 0;
  padding: 10px 16px;
  font-size: 12px;
  color: var(--text-tertiary, #8f959e);
  border-bottom: 1px solid var(--border-primary, #eef0f3);
}
.assistant-messages {
  flex: 1;
  overflow-y: auto;
  padding: 12px 14px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.assistant-bubble {
  max-width: 100%;
  border-radius: 10px;
  padding: 8px 10px;
  font-size: 13px;
  line-height: 1.5;
}
.assistant-bubble--user {
  align-self: flex-end;
  background: #eff6ff;
  border: 1px solid #bfdbfe;
}
.assistant-bubble--assistant {
  align-self: flex-start;
  background: #f8fafc;
  border: 1px solid var(--border-primary, #e5e7eb);
}
.assistant-bubble-role {
  display: block;
  font-size: 11px;
  font-weight: 600;
  color: var(--text-tertiary, #8f959e);
  margin-bottom: 4px;
}
.assistant-bubble-text {
  white-space: pre-wrap;
  word-break: break-word;
}
.assistant-input-row {
  padding: 10px 12px;
  border-top: 1px solid var(--border-primary, #e5e7eb);
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.assistant-input {
  width: 100%;
  box-sizing: border-box;
  resize: vertical;
  min-height: 52px;
  border-radius: 8px;
  border: 1px solid var(--border-primary, #dee0e3);
  padding: 8px 10px;
  font: inherit;
}
.assistant-send {
  align-self: flex-end;
}
.assistant-foot {
  padding: 8px 14px 14px;
  border-top: 1px solid var(--border-primary, #eef0f3);
}
.assistant-backdrop-fade-enter-active,
.assistant-backdrop-fade-leave-active {
  transition: opacity 0.2s ease;
}
.assistant-backdrop-fade-enter,
.assistant-backdrop-fade-leave-to {
  opacity: 0;
}
.assistant-drawer-slide-enter-active,
.assistant-drawer-slide-leave-active {
  transition: transform 0.22s ease;
}
.assistant-drawer-slide-enter,
.assistant-drawer-slide-leave-to {
  transform: translateX(100%);
}
@media (max-width: 640px) {
  .assistant-drawer {
    width: 100vw;
    max-width: none;
    min-width: 0;
  }
}

.link-button {
  border: none;
  background: none;
  color: var(--brand-blue, #1456f0);
  cursor: pointer;
  font-size: 13px;
  padding: 0;
  text-decoration: underline;
}
.link-button:hover {
  opacity: 0.9;
}
</style>
