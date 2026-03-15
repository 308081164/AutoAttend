<template>
  <div id="app">
    <header class="app-header">
      <div class="app-title">{{ $t('app.title') }}</div>
      <div class="app-header-right">
        <select v-model="currentLocale" class="lang-select" @change="onLocaleChange">
          <option v-for="opt in localeOptions" :key="opt.value" :value="opt.value">{{ opt.label }}</option>
        </select>
        <span v-if="username" class="app-username">{{ $t('app.adminLabel') }}：{{ username }}</span>
        <router-link v-if="username" to="/team" class="link-button">{{ $t('teamManage.navTitle') }}</router-link>
        <router-link v-if="username" to="/ai-config" class="link-button">{{ $t('aiConfig.navTitle') }}</router-link>
        <button v-if="username" class="link-button" @click="logout">{{ $t('app.logout') }}</button>
      </div>
    </header>
    <main class="app-main">
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
    username () {
      return window.localStorage.getItem('autoattend_username') || ''
    }
  },
  methods: {
    onLocaleChange () {
      setLocale(this.currentLocale)
    },
    logout () {
      window.localStorage.removeItem('autoattend_token')
      window.localStorage.removeItem('autoattend_username')
      window.localStorage.removeItem('autoattend_collab_token')
      this.$router.push({ name: 'login' })
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
  height: 56px;
  background-color: #1f2937;
  color: #ffffff;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
}

.app-title {
  font-size: 16px;
  font-weight: 600;
}

.app-header-right {
  display: flex;
  align-items: center;
  gap: 12px;
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
</style>
