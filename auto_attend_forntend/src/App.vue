<template>
  <div id="app" :class="{ 'embed-mode': isEmbedMode }">
    <!-- bareLayout mode: login/register pages, no sidebar -->
    <template v-if="bareLayout">
      <router-view/>
    </template>

    <!-- embed mode: keep original behavior -->
    <template v-else-if="isEmbedMode">
      <main class="app-main app-main-embed">
        <router-view/>
      </main>
    </template>

    <!-- Normal mode: sidebar + header + content -->
    <template v-else>
      <div class="app-layout">
        <!-- Mobile hamburger toggle -->
        <button
          class="sidebar-toggle"
          :class="{ 'is-active': sidebarOpen }"
          @click="sidebarOpen = !sidebarOpen"
          aria-label="Toggle sidebar"
        >
          <span></span>
          <span></span>
          <span></span>
        </button>

        <!-- Sidebar overlay for mobile -->
        <div
          v-if="sidebarOpen"
          class="sidebar-overlay"
          @click="sidebarOpen = false"
        ></div>

        <!-- LEFT SIDEBAR -->
        <aside class="app-sidebar" :class="{ 'is-open': sidebarOpen }">
          <!-- Logo area -->
          <div class="sidebar-logo">
            <div class="logo-icon">流</div>
            <div class="logo-text">
              <span class="logo-brand">流帮</span>
              <span class="logo-project">Project</span>
            </div>
          </div>

          <!-- Navigation groups -->
          <nav class="sidebar-nav">
            <div class="nav-group">
              <div class="nav-group-label">核心功能</div>
              <router-link
                v-for="item in navGroups.core"
                :key="item.route"
                :to="item.route"
                class="nav-item"
                :class="{ 'is-active': isNavActive(item.route) }"
                @click.native="sidebarOpen = false"
              >
                <span class="nav-icon">{{ item.icon }}</span>
                <span class="nav-label">{{ item.label }}</span>
              </router-link>
            </div>

            <div class="nav-group">
              <div class="nav-group-label">智能工具</div>
              <router-link
                v-for="item in navGroups.smart"
                :key="item.route"
                :to="item.route"
                class="nav-item"
                :class="{ 'is-active': isNavActive(item.route) }"
                @click.native="sidebarOpen = false"
              >
                <span class="nav-icon">{{ item.icon }}</span>
                <span class="nav-label">{{ item.label }}</span>
              </router-link>
            </div>

            <div class="nav-group">
              <div class="nav-group-label">运维与效率</div>
              <router-link
                v-for="item in navGroups.ops"
                :key="item.route"
                :to="item.route"
                class="nav-item"
                :class="{ 'is-active': isNavActive(item.route) }"
                @click.native="sidebarOpen = false"
              >
                <span class="nav-icon">{{ item.icon }}</span>
                <span class="nav-label">{{ item.label }}</span>
              </router-link>
            </div>
          </nav>

          <!-- Sidebar footer -->
          <div class="sidebar-footer">
            <div class="sidebar-user">
              <div class="user-avatar">{{ (username || '?').charAt(0).toUpperCase() }}</div>
              <span class="user-name">{{ username || $t('app.adminLabel') }}</span>
            </div>
            <button v-if="username" class="sidebar-logout" @click="logout">
              {{ $t('app.logout') }}
            </button>
          </div>
        </aside>

        <!-- RIGHT AREA: header + content -->
        <div class="app-right">
          <!-- TOP HEADER BAR -->
          <header class="app-topbar">
            <div class="topbar-left">
              <div class="breadcrumb">
                <span class="breadcrumb-brand">{{ $t('app.title') }}</span>
                <span class="breadcrumb-sep">/</span>
                <span class="breadcrumb-current">{{ currentPageName }}</span>
              </div>
            </div>
            <div class="topbar-right">
              <div class="topbar-search">
                <span class="search-icon">&#128269;</span>
                <span class="search-placeholder">{{ $t('app.search') || '搜索...' }}</span>
              </div>
              <button class="topbar-icon-btn" aria-label="Notifications">
                <span>&#128276;</span>
              </button>
              <select v-model="currentLocale" class="topbar-lang-select" @change="onLocaleChange">
                <option v-for="opt in localeOptions" :key="opt.value" :value="opt.value">{{ opt.label }}</option>
              </select>
              <div class="topbar-avatar">{{ (username || '?').charAt(0).toUpperCase() }}</div>
            </div>
          </header>

          <!-- MAIN CONTENT -->
          <main class="app-main">
            <router-view/>
          </main>
        </div>
      </div>
    </template>
  </div>
</template>

<script>
import { localeOptions, setLocale } from './locales'
import './assets/theme.css'

export default {
  name: 'App',
  data () {
    return {
      localeOptions,
      currentLocale: this.$i18n.locale,
      sidebarOpen: false,
      navGroups: {
        core: [
          { icon: '\u{1F3E0}', label: '工作台', route: '/console' },
          { icon: '\u{1F4CB}', label: '报价系统', route: '/quote' },
          { icon: '\u{1F4C1}', label: '项目管理', route: '/console#projects' },
          { icon: '\u{1F465}', label: '团队管理', route: '/team' }
        ],
        smart: [
          { icon: '\u{1F916}', label: 'AI 配置', route: '/ai-config' },
          { icon: '\u{1F3A8}', label: '快原型', route: '/prototype' }
        ],
        ops: [
          { icon: '\u{1F527}', label: '快捷运维', route: '/nexus' },
          { icon: '\u{1F9EA}', label: '增效实验室', route: '/console#lab' }
        ]
      }
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
    },
    currentPageName () {
      const path = this.$route.path
      const hash = this.$route.hash
      const full = path + hash
      const allNav = [
        ...this.navGroups.core,
        ...this.navGroups.smart,
        ...this.navGroups.ops
      ]
      const match = allNav.find(item => {
        if (item.route.includes('#')) {
          return full === item.route
        }
        return path === item.route
      })
      return match ? match.label : this.$t('app.title')
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
    },
    isNavActive (route) {
      const path = this.$route.path
      const hash = this.$route.hash
      if (route.includes('#')) {
        return (path + hash) === route
      }
      return path === route
    }
  }
}
</script>

<style>
/* ========== Reset & Base ========== */
* {
  box-sizing: border-box;
}

body {
  margin: 0;
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, "Noto Sans", sans-serif;
  background-color: var(--bg-page, #f0f2f5);
  color: var(--text-primary, #1f2937);
}

#app {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

/* ========== Embed Mode ========== */
.embed-mode {
  background-color: #fff;
}

/* ========== App Layout (normal mode) ========== */
.app-layout {
  display: flex;
  min-height: 100vh;
  width: 100%;
}

/* ========== Mobile Hamburger Toggle ========== */
.sidebar-toggle {
  display: none;
  position: fixed;
  top: 12px;
  left: 12px;
  z-index: 1001;
  width: 36px;
  height: 36px;
  background: var(--bg-sidebar, #1e293b);
  border: none;
  border-radius: 8px;
  cursor: pointer;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 4px;
  padding: 0;
}

.sidebar-toggle span {
  display: block;
  width: 18px;
  height: 2px;
  background: #fff;
  border-radius: 1px;
  transition: transform 0.3s, opacity 0.3s;
}

.sidebar-toggle.is-active span:nth-child(1) {
  transform: translateY(6px) rotate(45deg);
}

.sidebar-toggle.is-active span:nth-child(2) {
  opacity: 0;
}

.sidebar-toggle.is-active span:nth-child(3) {
  transform: translateY(-6px) rotate(-45deg);
}

/* ========== Sidebar Overlay (mobile) ========== */
.sidebar-overlay {
  display: none;
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.45);
  z-index: 998;
}

/* ========== LEFT SIDEBAR ========== */
.app-sidebar {
  width: 240px;
  min-width: 240px;
  height: 100vh;
  position: sticky;
  top: 0;
  background: var(--bg-sidebar, #1e293b);
  display: flex;
  flex-direction: column;
  overflow-y: auto;
  overflow-x: hidden;
  z-index: 999;
  transition: transform 0.3s ease;
}

/* Sidebar scrollbar */
.app-sidebar::-webkit-scrollbar {
  width: 4px;
}

.app-sidebar::-webkit-scrollbar-thumb {
  background: rgba(255, 255, 255, 0.15);
  border-radius: 2px;
}

/* ========== Sidebar Logo ========== */
.sidebar-logo {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 20px 20px 16px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
}

.logo-icon {
  width: 36px;
  height: 36px;
  min-width: 36px;
  background: var(--brand-blue, #3b82f6);
  color: #fff;
  font-size: 18px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 8px;
}

.logo-text {
  display: flex;
  flex-direction: column;
  line-height: 1.2;
}

.logo-brand {
  font-size: 16px;
  font-weight: 700;
  color: #fff;
}

.logo-project {
  font-size: 11px;
  font-weight: 400;
  color: rgba(255, 255, 255, 0.5);
}

/* ========== Sidebar Navigation ========== */
.sidebar-nav {
  flex: 1;
  padding: 12px 0;
  overflow-y: auto;
}

.nav-group {
  margin-bottom: 8px;
}

.nav-group-label {
  padding: 8px 20px 4px;
  font-size: 11px;
  font-weight: 600;
  color: rgba(255, 255, 255, 0.35);
  text-transform: uppercase;
  letter-spacing: 0.5px;
  user-select: none;
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 9px 20px;
  color: rgba(255, 255, 255, 0.7);
  text-decoration: none;
  font-size: 14px;
  font-weight: 400;
  transition: background 0.2s, color 0.2s;
  position: relative;
  cursor: pointer;
  border: none;
  background: none;
  width: 100%;
  text-align: left;
}

.nav-item::before {
  content: '';
  position: absolute;
  left: 0;
  top: 4px;
  bottom: 4px;
  width: 3px;
  background: var(--brand-blue, #3b82f6);
  border-radius: 0 2px 2px 0;
  transform: scaleY(0);
  transition: transform 0.2s ease;
}

.nav-item:hover {
  background: rgba(255, 255, 255, 0.06);
  color: #fff;
  text-decoration: none;
}

.nav-item.is-active {
  background: rgba(59, 130, 246, 0.12);
  color: #fff;
  font-weight: 500;
}

.nav-item.is-active::before {
  transform: scaleY(1);
}

.nav-icon {
  font-size: 18px;
  width: 24px;
  text-align: center;
  flex-shrink: 0;
}

.nav-label {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* ========== Sidebar Footer ========== */
.sidebar-footer {
  padding: 12px 16px;
  border-top: 1px solid rgba(255, 255, 255, 0.08);
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.sidebar-user {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
  flex: 1;
}

.user-avatar {
  width: 30px;
  height: 30px;
  min-width: 30px;
  border-radius: 50%;
  background: var(--brand-blue, #3b82f6);
  color: #fff;
  font-size: 13px;
  font-weight: 600;
  display: flex;
  align-items: center;
  justify-content: center;
}

.user-name {
  font-size: 13px;
  color: rgba(255, 255, 255, 0.8);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.sidebar-logout {
  border: none;
  background: rgba(255, 255, 255, 0.08);
  color: rgba(255, 255, 255, 0.6);
  font-size: 12px;
  padding: 4px 10px;
  border-radius: 4px;
  cursor: pointer;
  transition: background 0.2s, color 0.2s;
  white-space: nowrap;
}

.sidebar-logout:hover {
  background: rgba(255, 255, 255, 0.15);
  color: #fff;
}

/* ========== RIGHT AREA ========== */
.app-right {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
  min-height: 100vh;
}

/* ========== TOP HEADER BAR ========== */
.app-topbar {
  position: sticky;
  top: 0;
  z-index: 100;
  height: 56px;
  min-height: 56px;
  background: #fff;
  border-bottom: 1px solid var(--border-color, #e5e7eb);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  gap: 16px;
}

.topbar-left {
  display: flex;
  align-items: center;
  min-width: 0;
}

.breadcrumb {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
}

.breadcrumb-brand {
  color: var(--text-secondary, #6b7280);
  font-weight: 400;
}

.breadcrumb-sep {
  color: var(--text-disabled, #9ca3af);
}

.breadcrumb-current {
  color: var(--text-primary, #1f2937);
  font-weight: 600;
}

.topbar-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.topbar-search {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  background: var(--bg-page, #f0f2f5);
  border-radius: 6px;
  cursor: pointer;
  min-width: 180px;
  transition: background 0.2s;
}

.topbar-search:hover {
  background: #e5e7eb;
}

.search-icon {
  font-size: 14px;
  color: var(--text-disabled, #9ca3af);
}

.search-placeholder {
  font-size: 13px;
  color: var(--text-disabled, #9ca3af);
}

.topbar-icon-btn {
  width: 36px;
  height: 36px;
  border: none;
  background: transparent;
  border-radius: 8px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  transition: background 0.2s;
  position: relative;
}

.topbar-icon-btn:hover {
  background: var(--bg-page, #f0f2f5);
}

.topbar-lang-select {
  padding: 4px 8px;
  border-radius: 4px;
  border: 1px solid var(--border-color, #e5e7eb);
  background: #fff;
  color: var(--text-primary, #1f2937);
  font-size: 13px;
  cursor: pointer;
}

.topbar-lang-select option {
  background: #ffffff;
  color: #1f2937;
}

.topbar-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: var(--brand-blue, #3b82f6);
  color: #fff;
  font-size: 14px;
  font-weight: 600;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
}

/* ========== MAIN CONTENT ========== */
.app-main {
  flex: 1;
  padding: 24px;
  overflow-y: auto;
  background: var(--bg-page, #f0f2f5);
}

.app-main-embed {
  padding: 0;
}

.app-main-bare {
  padding: 0;
}

/* ========== Responsive: < 1024px ========== */
@media (max-width: 1023px) {
  .sidebar-toggle {
    display: flex;
  }

  .sidebar-overlay {
    display: block;
  }

  .app-sidebar {
    position: fixed;
    top: 0;
    left: 0;
    height: 100vh;
    transform: translateX(-100%);
    z-index: 999;
    box-shadow: none;
  }

  .app-sidebar.is-open {
    transform: translateX(0);
    box-shadow: 4px 0 24px rgba(0, 0, 0, 0.15);
  }

  .app-topbar {
    padding-left: 56px;
  }
}
</style>
