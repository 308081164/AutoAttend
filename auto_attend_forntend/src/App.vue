<template>
  <div id="app" :class="{ 'embed-mode': isEmbedMode }">
    <header v-if="!isEmbedMode && !bareLayout" class="app-header">
      <div class="app-brand">
        <div class="app-title">{{ $t('app.title') }}</div>
        <div class="app-slogan">{{ $t('app.slogan') }}</div>
      </div>
      <div class="app-header-right">
        <router-link
          v-if="showBackToHome"
          :to="{ name: 'dashboard' }"
          class="back-home-link"
        >{{ $t('app.backToHome') }}</router-link>
        <select v-model="currentLocale" class="lang-select" @change="onLocaleChange">
          <option v-for="opt in localeOptions" :key="opt.value" :value="opt.value">{{ opt.label }}</option>
        </select>
        <span v-if="username" class="app-username">{{ $t('app.adminLabel') }}：{{ username }}</span>
        <button v-if="username" class="link-button" @click="logout">{{ $t('app.logout') }}</button>
      </div>
    </header>
    <main class="app-main" :class="{ 'app-main-embed': isEmbedMode, 'app-main-bare': bareLayout }">
      <router-view/>
    </main>
  </div>
</template>

<script>
import { localeOptions, setLocale } from './locales'

export default {
  name: 'App',
  data () {
    return {
      localeOptions,
      currentLocale: this.$i18n.locale
    }
  },
  computed: {
    isEmbedMode () {
      return String(this.$route.query.embed || '') === '1' || window.__AUTOATTEND_EMBED__ === true
    },
    username () {
      return window.localStorage.getItem('autoattend_username') || ''
    },
    /** 管理员已登录且不在控制台首页时显示「返回首页」 */
    showBackToHome () {
      if (!this.username) return false
      return this.$route.name !== 'dashboard' && this.$route.name !== 'landing'
    },
    bareLayout () {
      return !!(this.$route.meta && this.$route.meta.bareLayout)
    }
  },
  methods: {
    onLocaleChange () {
      setLocale(this.currentLocale)
    },
    async logout () {
      try {
        const token = window.localStorage.getItem('autoattend_token')
        if (token) {
          await this.$http.post('/admin/auth/logout', null, {
            headers: { Authorization: 'Bearer ' + token }
          })
        }
      } catch (e) {
        // ignore backend logout error, still clear local state
      } finally {
        window.localStorage.removeItem('autoattend_token')
        window.localStorage.removeItem('autoattend_username')
        window.localStorage.removeItem('autoattend_collab_token')
        this.$router.push({ name: 'login' })
      }
    }
  }
}
</script>

<style>
* {
  box-sizing: border-box;
}

body {
  margin: 0;
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, "Noto Sans", sans-serif;
  background-color: #f5f5f5;
}

#app {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

.app-header {
  min-height: 56px;
  background-color: #1f2937;
  color: #ffffff;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 24px;
  gap: 16px;
}

.app-brand {
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 2px;
  min-width: 0;
}

.app-title {
  font-size: 16px;
  font-weight: 600;
  line-height: 1.25;
}

.app-slogan {
  font-size: 12px;
  font-weight: 400;
  color: #9ca3af;
  line-height: 1.25;
}

.app-header-right {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.back-home-link {
  display: inline-flex;
  align-items: center;
  padding: 6px 12px;
  font-size: 13px;
  font-weight: 600;
  color: #fff;
  text-decoration: none;
  background: rgba(255, 255, 255, 0.12);
  border: 1px solid rgba(255, 255, 255, 0.35);
  border-radius: 6px;
  transition: background 0.15s, border-color 0.15s;
}

.back-home-link:hover {
  background: rgba(255, 255, 255, 0.2);
  border-color: rgba(255, 255, 255, 0.5);
  text-decoration: none;
}

.lang-select {
  padding: 4px 8px;
  border-radius: 4px;
  border: 1px solid rgba(255,255,255,0.3);
  background: rgba(255,255,255,0.1);
  color: #fff;
  font-size: 13px;
  cursor: pointer;
}

/* 下拉选项：浅色背景 + 深色文字，保证可读性 */
.lang-select option {
  background: #ffffff;
  color: #1f2937;
}

.app-username {
  font-size: 13px;
  opacity: 0.85;
}

.link-button {
  border: none;
  background: transparent;
  color: #93c5fd;
  cursor: pointer;
  font-size: 13px;
  padding: 4px 8px;
}

.link-button:hover {
  text-decoration: underline;
}

.app-main {
  flex: 1;
  padding: 24px;
}

.app-main-embed {
  padding: 0;
}

.app-main-bare {
  padding: 0;
}

.embed-mode {
  background-color: #fff;
}
</style>
