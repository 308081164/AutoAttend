<template>
  <div id="app" :class="{ 'embed-mode': isEmbedMode, 'cloud-dev-embed-max': cloudDevEmbedActive }">
    <!-- bareLayout mode: login/register pages, no sidebar -->
    <template v-if="bareLayout">
      <router-view/>
    </template>

    <!-- embed mode: keep original behavior -->
    <template v-else-if="isEmbedMode">
      <main class="app-main app-main-embed">
        <keep-alive :max="8" :exclude="shellKeepAliveExclude">
          <router-view :key="$route.fullPath"/>
        </keep-alive>
      </main>
    </template>

    <!-- Normal mode: sidebar + header + content -->
    <template v-else>
      <div class="app-layout">
        <transition name="app-toast-fade">
          <div
            v-if="appToastMessage"
            class="app-global-toast"
            :class="'app-global-toast--' + appToastKind"
            role="status"
          >
            {{ appToastMessage }}
          </div>
        </transition>
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
        <div class="sidebar-wrapper">
          <aside class="app-sidebar" :class="{ 'is-open': sidebarOpen, 'is-collapsed': sidebarCollapsed }">
            <!-- Logo area -->
            <div class="sidebar-logo">
              <div class="logo-icon" aria-hidden="true">
                <img class="logo-icon-img" src="/brand-logo.svg" width="36" height="36" alt="">
              </div>
              <div class="logo-text" v-show="!sidebarCollapsed">
                <span class="logo-brand">流帮</span>
                <span class="logo-project">Project</span>
              </div>
            </div>

          <!-- Navigation groups -->
          <nav class="sidebar-nav">
            <!-- 成员账号：精简导航 -->
            <template v-if="memberLayout">
              <div class="nav-group">
                <div class="nav-group-label" v-show="!sidebarCollapsed">{{ $t('app.sidebarGroupCore') }}</div>
                <router-link to="/member" class="nav-item" :class="{ 'is-active': isNavActive('/member') }" @click.native="onNavClick">
                  <span class="nav-icon"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" width="18" height="18"><path d="M3 9l9-7 9 7v11a2 2 0 01-2 2H5a2 2 0 01-2-2z"/><polyline points="9 22 9 12 15 12 15 22"/></svg></span>
                  <span class="nav-label" v-show="!sidebarCollapsed">{{ $t('app.sidebarMemberHome') }}</span>
                </router-link>
                <router-link to="/collab/projects" class="nav-item" :class="{ 'is-active': isNavActive('/collab/projects') }" @click.native="onNavClick">
                  <span class="nav-icon"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" width="18" height="18"><path d="M22 19a2 2 0 01-2 2H4a2 2 0 01-2-2V5a2 2 0 012-2h5l2 3h9a2 2 0 012 2z"/></svg></span>
                  <span class="nav-label" v-show="!sidebarCollapsed">{{ $t('app.sidebarProjects') }}</span>
                </router-link>
              </div>
              <div class="nav-group">
                <div class="nav-group-label" v-show="!sidebarCollapsed">{{ $t('app.sidebarGroupTools') }}</div>
                <router-link to="/cloud-dev" class="nav-item" :class="{ 'is-active': isNavActive('/cloud-dev') }" @click.native="onNavClick">
                  <span class="nav-icon"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" width="18" height="18"><path d="M18 10h-1.26A8 8 0 1 0 9 20h9a5 5 0 0 0 0-10z"/></svg></span>
                  <span class="nav-label" v-show="!sidebarCollapsed">{{ $t('cloudDev.navTitle') }}</span>
                </router-link>
              </div>
              <div class="nav-group">
                <div class="nav-group-label" v-show="!sidebarCollapsed">{{ $t('app.sidebarGroupOps') }}</div>
                <button type="button" class="nav-item nav-item--btn" @click="onMemberNexusClick">
                  <span class="nav-icon"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" width="18" height="18"><circle cx="12" cy="12" r="3"/><path d="M19.4 15a1.65 1.65 0 00.33 1.82l.06.06a2 2 0 010 2.83 2 2 0 01-2.83 0l-.06-.06a1.65 1.65 0 00-1.82-.33 1.65 1.65 0 00-1 1.51V21a2 2 0 01-2 2 2 2 0 01-2-2v-.09A1.65 1.65 0 009 19.4a1.65 1.65 0 00-1.82.33l-.06.06a2 2 0 01-2.83 0 2 2 0 010-2.83l.06-.06A1.65 1.65 0 004.68 15a1.65 1.65 0 00-1.51-1H3a2 2 0 01-2-2 2 2 0 012-2h.09A1.65 1.65 0 004.6 9a1.65 1.65 0 00-.33-1.82l-.06-.06a2 2 0 010-2.83 2 2 0 012.83 0l.06.06A1.65 1.65 0 009 4.68a1.65 1.65 0 001-1.51V3a2 2 0 012-2 2 2 0 012 2v.09a1.65 1.65 0 001 1.51 1.65 1.65 0 001.82-.33l.06-.06a2 2 0 012.83 0 2 2 0 010 2.83l-.06.06a1.65 1.65 0 00-.33 1.82V9a1.65 1.65 0 001.51 1H21a2 2 0 012 2 2 2 0 01-2 2h-.09a1.65 1.65 0 00-1.51 1z"/></svg></span>
                  <span class="nav-label" v-show="!sidebarCollapsed">{{ $t('app.sidebarNexus') }}</span>
                </button>
                <router-link to="/lab" class="nav-item" :class="{ 'is-active': isNavActive('/lab') }" @click.native="onNavClick">
                  <span class="nav-icon"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" width="18" height="18"><circle cx="12" cy="12" r="10"/><polygon points="10 8 16 12 10 16 10 8"/></svg></span>
                  <span class="nav-label" v-show="!sidebarCollapsed">{{ $t('app.sidebarLab') }}</span>
                </router-link>
              </div>
            </template>

            <!-- 管理员：完整导航 -->
            <template v-else>
            <!-- Core features -->
            <div class="nav-group">
              <div class="nav-group-label" v-show="!sidebarCollapsed">核心功能</div>
              <router-link to="/console" class="nav-item" :class="{ 'is-active': isNavActive('/console') }" @click.native="onNavClick">
                <span class="nav-icon"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" width="18" height="18"><path d="M3 9l9-7 9 7v11a2 2 0 01-2 2H5a2 2 0 01-2-2z"/><polyline points="9 22 9 12 15 12 15 22"/></svg></span>
                <span class="nav-label" v-show="!sidebarCollapsed">工作台</span>
              </router-link>
              <router-link to="/quote" class="nav-item" :class="{ 'is-active': isNavActive('/quote') }" @click.native="onNavClick">
                <span class="nav-icon"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" width="18" height="18"><path d="M14 2H6a2 2 0 00-2 2v16a2 2 0 002 2h12a2 2 0 002-2V8z"/><polyline points="14 2 14 8 20 8"/><line x1="16" y1="13" x2="8" y2="13"/><line x1="16" y1="17" x2="8" y2="17"/><polyline points="10 9 9 9 8 9"/></svg></span>
                <span class="nav-label" v-show="!sidebarCollapsed">报价系统</span>
              </router-link>
              <router-link to="/collab/projects" class="nav-item" :class="{ 'is-active': isNavActive('/collab/projects') }" @click.native="onNavClick">
                <span class="nav-icon"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" width="18" height="18"><path d="M22 19a2 2 0 01-2 2H4a2 2 0 01-2-2V5a2 2 0 012-2h5l2 3h9a2 2 0 012 2z"/></svg></span>
                <span class="nav-label" v-show="!sidebarCollapsed">项目管理</span>
              </router-link>
              <router-link to="/team" class="nav-item" :class="{ 'is-active': isNavActive('/team') }" @click.native="onNavClick">
                <span class="nav-icon"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" width="18" height="18"><path d="M17 21v-2a4 4 0 00-4-4H5a4 4 0 00-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M23 21v-2a4 4 0 00-3-3.87"/><path d="M16 3.13a4 4 0 010 7.75"/></svg></span>
                <span class="nav-label" v-show="!sidebarCollapsed">团队管理</span>
              </router-link>
              <router-link to="/subscription" class="nav-item" :class="{ 'is-active': isNavActive('/subscription') }" @click.native="onNavClick">
                <span class="nav-icon"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" width="18" height="18"><rect x="2" y="5" width="20" height="14" rx="2"/><path d="M2 10h20"/></svg></span>
                <span class="nav-label" v-show="!sidebarCollapsed">{{ $t('subscriptionPage.navTitle') }}</span>
              </router-link>
            </div>

            <!-- Smart tools -->
            <div class="nav-group">
              <div class="nav-group-label" v-show="!sidebarCollapsed">智能工具</div>
              <router-link to="/api-config" class="nav-item" :class="{ 'is-active': isNavActive('/api-config') }" @click.native="onNavClick">
                <span class="nav-icon"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" width="18" height="18"><polygon points="13 2 3 14 12 14 11 22 21 10 12 10 13 2"/></svg></span>
                <span class="nav-label" v-show="!sidebarCollapsed">{{ $t('aiConfig.navTitle') }}</span>
              </router-link>
              <router-link to="/prototype" class="nav-item" :class="{ 'is-active': isNavActive('/prototype') }" @click.native="onNavClick">
                <span class="nav-icon"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" width="18" height="18"><rect x="3" y="3" width="18" height="18" rx="2" ry="2"/><circle cx="8.5" cy="8.5" r="1.5"/><polyline points="21 15 16 10 5 21"/></svg></span>
                <span class="nav-label" v-show="!sidebarCollapsed">快原型</span>
              </router-link>
              <router-link to="/cloud-dev" class="nav-item" :class="{ 'is-active': isNavActive('/cloud-dev') }" @click.native="onNavClick">
                <span class="nav-icon"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" width="18" height="18"><path d="M18 10h-1.26A8 8 0 1 0 9 20h9a5 5 0 0 0 0-10z"/></svg></span>
                <span class="nav-label" v-show="!sidebarCollapsed">{{ $t('cloudDev.navTitle') }}</span>
              </router-link>
            </div>

            <!-- Ops & Efficiency -->
            <div class="nav-group">
              <div class="nav-group-label" v-show="!sidebarCollapsed">运维与效率</div>
              <router-link to="/nexus" class="nav-item" :class="{ 'is-active': isNavActive('/nexus') }" @click.native="onNavClick">
                <span class="nav-icon"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" width="18" height="18"><circle cx="12" cy="12" r="3"/><path d="M19.4 15a1.65 1.65 0 00.33 1.82l.06.06a2 2 0 010 2.83 2 2 0 01-2.83 0l-.06-.06a1.65 1.65 0 00-1.82-.33 1.65 1.65 0 00-1 1.51V21a2 2 0 01-2 2 2 2 0 01-2-2v-.09A1.65 1.65 0 009 19.4a1.65 1.65 0 00-1.82.33l-.06.06a2 2 0 01-2.83 0 2 2 0 010-2.83l.06-.06A1.65 1.65 0 004.68 15a1.65 1.65 0 00-1.51-1H3a2 2 0 01-2-2 2 2 0 012-2h.09A1.65 1.65 0 004.6 9a1.65 1.65 0 00-.33-1.82l-.06-.06a2 2 0 010-2.83 2 2 0 012.83 0l.06.06A1.65 1.65 0 009 4.68a1.65 1.65 0 001-1.51V3a2 2 0 012-2 2 2 0 012 2v.09a1.65 1.65 0 001 1.51 1.65 1.65 0 001.82-.33l.06-.06a2 2 0 012.83 0 2 2 0 010 2.83l-.06.06a1.65 1.65 0 00-.33 1.82V9a1.65 1.65 0 001.51 1H21a2 2 0 012 2 2 2 0 01-2 2h-.09a1.65 1.65 0 00-1.51 1z"/></svg></span>
                <span class="nav-label" v-show="!sidebarCollapsed">快捷运维</span>
              </router-link>
              <router-link to="/lab" class="nav-item" :class="{ 'is-active': isNavActive('/lab') }" @click.native="onNavClick">
                <span class="nav-icon"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" width="18" height="18"><circle cx="12" cy="12" r="10"/><polygon points="10 8 16 12 10 16 10 8"/></svg></span>
                <span class="nav-label" v-show="!sidebarCollapsed">增效实验室</span>
              </router-link>
            </div>
            </template>
          </nav>

          <!-- Sidebar footer -->
          <div class="sidebar-footer">
            <div class="sidebar-user">
              <div class="user-avatar" :class="{ 'user-avatar--logo': isAdmin }">
                <template v-if="isAdmin">
                  <img class="user-avatar-brand" src="/brand-logo.svg" alt="">
                </template>
                <template v-else>{{ (username || '?').charAt(0).toUpperCase() }}</template>
              </div>
              <span class="user-name" v-show="!sidebarCollapsed">{{ sidebarUserLabel }}</span>
            </div>
            <button v-if="username && !sidebarCollapsed" class="sidebar-logout" @click="logout">
              {{ $t('app.logout') }}
            </button>
          </div>
        </aside>
        <!-- Collapse toggle button (desktop only) -->
        <button
          class="sidebar-collapse-btn"
          :aria-label="sidebarCollapsed ? 'Expand sidebar' : 'Collapse sidebar'"
          @click="toggleSidebar"
        >
          <span class="collapse-icon" :class="{ 'is-flipped': sidebarCollapsed }">&#9654;</span>
        </button>
        </div>

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
                <svg class="search-icon-svg" viewBox="0 0 20 20" fill="none" stroke="currentColor" stroke-width="1.5" width="16" height="16"><circle cx="9" cy="9" r="6"/><path d="M13.5 13.5L18 18" stroke-linecap="round"/></svg>
                <span class="search-placeholder">{{ $t('app.search') || '搜索...' }}</span>
              </div>
              <button class="topbar-icon-btn" aria-label="Notifications">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" width="18" height="18"><path d="M18 8A6 6 0 006 8c0 7-3 9-3 9h18s-3-2-3-9"/><path d="M13.73 21a2 2 0 01-3.46 0"/></svg>
              </button>
              <select v-model="currentLocale" class="topbar-lang-select" @change="onLocaleChange">
                <option v-for="opt in localeOptions" :key="opt.value" :value="opt.value">{{ opt.label }}</option>
              </select>
              <div class="topbar-avatar" :class="{ 'topbar-avatar--logo': isAdmin }">
                <template v-if="isAdmin">
                  <img class="user-avatar-brand" src="/brand-logo.svg" alt="">
                </template>
                <template v-else>{{ (username || '?').charAt(0).toUpperCase() }}</template>
              </div>
            </div>
          </header>

          <!-- MAIN CONTENT：keep-alive 缓存侧栏内页面实例，减少重复请求；动态路由用 fullPath 区分 -->
          <main class="app-main" :class="{ 'app-main-clouddev-embed': cloudDevEmbedActive }">
            <keep-alive :max="8" :exclude="shellKeepAliveExclude">
              <router-view :key="$route.fullPath"/>
            </keep-alive>
          </main>
        </div>
      </div>
    </template>
  </div>
</template>

<script>
import { localeOptions, setLocale } from './locales'
import { subscribeAuthSession, notifyAuthSessionChanged } from './utils/authSession'
import { SHELL_KEEP_ALIVE_EXCLUDE } from './utils/keepAliveShell'
import './assets/theme.css'

export default {
  name: 'App',
  data () {
    return {
      /** 与 localStorage 会话键联动，使 isAdmin/memberLayout 等计算属性在 token 变更后重新求值 */
      authSessionTick: 0,
      localeOptions,
      currentLocale: this.$i18n.locale,
      sidebarOpen: false,
      sidebarCollapsed: false,
      /** 云开发「站内嵌入」开启时由 CloudDevHubView 通知，用于隐藏主导航侧栏、放大 iframe 区域 */
      cloudDevEmbedActive: false,
      appToastMessage: '',
      appToastKind: 'info',
      appToastTimer: null,
      shellKeepAliveExclude: SHELL_KEEP_ALIVE_EXCLUDE
    }
  },
  computed: {
    isEmbedMode () {
      return String(this.$route.query.embed || '') === '1' || window.__AUTOATTEND_EMBED__ === true
    },
    username () {
      void this.authSessionTick
      return window.localStorage.getItem('autoattend_username') || ''
    },
    isAdmin () {
      void this.authSessionTick
      return !!(window.localStorage.getItem('autoattend_token') || '').trim()
    },
    /** 仅成员 JWT、无管理员 JWT：侧栏与路由走精简模式 */
    memberLayout () {
      void this.authSessionTick
      const admin = (window.localStorage.getItem('autoattend_token') || '').trim()
      return !!(window.localStorage.getItem('autoattend_collab_token') || '').trim() && !admin
    },
    sidebarUserLabel () {
      if (this.isAdmin) return this.$t('app.adminLabel') || '管理员'
      if (this.memberLayout) return this.$t('app.memberLabel')
      return this.username || ''
    },
    bareLayout () {
      return !!(this.$route.meta && this.$route.meta.bareLayout)
    },
    currentPageName () {
      const path = this.$route.path
      if (path === '/cloud-dev') {
        return this.$t('cloudDev.navTitle')
      }
      const map = {
        '/console': '工作台',
        '/quote': '报价系统',
        '/quote/config': '报价配置',
        '/collab/projects': this.memberLayout ? this.$t('app.sidebarProjects') : '项目管理',
        '/team': '团队管理',
        '/subscription': this.$t('subscriptionPage.navTitle'),
        '/tenant-admins': '租户管理',
        '/api-config': 'API 配置',
        '/prototype': '快原型',
        '/nexus': '快捷运维',
        '/lab': this.memberLayout ? this.$t('app.sidebarLab') : '增效实验室',
        '/member': this.$t('memberHome.title')
      }
      for (const [prefix, name] of Object.entries(map)) {
        if (path === prefix || path.startsWith(prefix + '/')) return name
      }
      return this.$t('app.title')
    }
  },
  mounted () {
    this._unsubAuthSession = subscribeAuthSession(() => {
      this.authSessionTick++
    })
    this.initSidebarState()
    window.addEventListener('resize', this.onResize)
    this._onCloudDevEmbed = (e) => {
      const active = !!(e && e.detail && e.detail.active)
      this.cloudDevEmbedActive = active
    }
    window.addEventListener('autoattend-clouddev-embed', this._onCloudDevEmbed)
  },
  beforeDestroy () {
    if (this._unsubAuthSession) {
      this._unsubAuthSession()
      this._unsubAuthSession = null
    }
    window.removeEventListener('resize', this.onResize)
    if (this._onCloudDevEmbed) {
      window.removeEventListener('autoattend-clouddev-embed', this._onCloudDevEmbed)
    }
    if (this.appToastTimer) {
      clearTimeout(this.appToastTimer)
      this.appToastTimer = null
    }
  },
  methods: {
    showAppToast (message, kind = 'info') {
      const k = kind === 'error' ? 'error' : 'info'
      if (this.appToastTimer) {
        clearTimeout(this.appToastTimer)
        this.appToastTimer = null
      }
      this.appToastKind = k
      this.appToastMessage = message || ''
      this.appToastTimer = setTimeout(() => {
        this.appToastMessage = ''
        this.appToastTimer = null
      }, 3200)
    },
    onMemberNexusClick () {
      if (window.innerWidth < 768) {
        this.sidebarOpen = false
      }
      this.showAppToast(this.$t('app.memberNexusDesigningToast'), 'info')
    },
    initSidebarState () {
      if (window.innerWidth < 1024) {
        this.sidebarCollapsed = false
      } else {
        this.sidebarCollapsed = false
      }
    },
    onResize () {
      if (window.innerWidth < 768) {
        this.sidebarCollapsed = false
      }
    },
    toggleSidebar () {
      this.sidebarCollapsed = !this.sidebarCollapsed
    },
    onNavClick () {
      if (window.innerWidth < 768) {
        this.sidebarOpen = false
      }
    },
    onLocaleChange () {
      setLocale(this.currentLocale)
    },
    async logout () {
      const adminTok = window.localStorage.getItem('autoattend_token')
      const collabTok = window.localStorage.getItem('autoattend_collab_token')
      const memberOnly = !!collabTok && !adminTok
      try {
        if (adminTok) {
          await this.$http.post('/admin/auth/logout', null, {
            headers: { Authorization: 'Bearer ' + adminTok }
          })
        }
      } catch (e) {
        // ignore backend logout error, still clear local state
      } finally {
        window.localStorage.removeItem('autoattend_token')
        window.localStorage.removeItem('autoattend_username')
        window.localStorage.removeItem('autoattend_collab_token')
        notifyAuthSessionChanged()
        if (memberOnly) {
          this.$router.push({ name: 'member-login' })
        } else {
          this.$router.push({ name: 'login' })
        }
      }
    },
    isNavActive (route) {
      const path = this.$route.path
      if (route.includes('#')) {
        return (path + this.$route.hash) === route
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
.sidebar-wrapper {
  position: relative;
  flex-shrink: 0;
}

.app-sidebar {
  width: var(--sidebar-width, 240px);
  min-width: var(--sidebar-width, 240px);
  height: 100vh;
  position: sticky;
  top: 0;
  background: var(--bg-sidebar, #1F2329);
  display: flex;
  flex-direction: column;
  overflow-y: auto;
  overflow-x: hidden;
  z-index: 999;
  transition: width var(--transition-slow, all 0.3s ease),
              min-width var(--transition-slow, all 0.3s ease),
              transform var(--transition-slow, all 0.3s ease);
}

.app-sidebar.is-collapsed {
  width: var(--sidebar-collapsed-width, 64px);
  min-width: var(--sidebar-collapsed-width, 64px);
}

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
  position: relative;
  min-height: 72px;
}

.app-sidebar.is-collapsed .sidebar-logo {
  justify-content: center;
  padding: 20px 0 16px;
  gap: 0;
}

.logo-icon {
  width: 36px;
  height: 36px;
  min-width: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 10px;
  overflow: hidden;
  flex-shrink: 0;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.2);
}
.logo-icon-img {
  width: 100%;
  height: 100%;
  display: block;
  object-fit: cover;
}

.logo-text {
  display: flex;
  flex-direction: column;
  line-height: 1.2;
  white-space: nowrap;
  overflow: hidden;
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

/* --- Collapse toggle button --- */
.sidebar-collapse-btn {
  display: none;
  position: absolute;
  right: -14px;
  top: 28px;
  width: 24px;
  height: 24px;
  background: var(--bg-sidebar, #1F2329);
  border: 2px solid rgba(255, 255, 255, 0.15);
  border-radius: 50%;
  cursor: pointer;
  align-items: center;
  justify-content: center;
  padding: 0;
  z-index: 1000;
  transition: background 0.2s, border-color 0.2s;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.15);
}

.sidebar-collapse-btn:hover {
  background: rgba(255, 255, 255, 0.1);
  border-color: rgba(255, 255, 255, 0.3);
}

.collapse-icon {
  display: block;
  font-size: 10px;
  color: rgba(255, 255, 255, 0.7);
  transition: transform 0.3s ease;
  line-height: 1;
}

.collapse-icon.is-flipped {
  transform: rotate(180deg);
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
  white-space: nowrap;
  overflow: hidden;
}

.app-sidebar.is-collapsed .nav-group-label {
  padding: 8px 0 4px;
  text-align: center;
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
  width: 100%;
  box-sizing: border-box;
  border: none;
  background: transparent;
  font-family: inherit;
  text-align: left;
}

.app-sidebar.is-collapsed .nav-item {
  justify-content: center;
  padding: 9px 0;
}

.nav-item::before {
  content: '';
  position: absolute;
  left: 0;
  top: 4px;
  bottom: 4px;
  width: 3px;
  background: var(--brand-blue, #1456F0);
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
  background: rgba(20, 86, 240, 0.15);
  color: #fff;
  font-weight: 500;
}

.nav-item.is-active::before {
  transform: scaleY(1);
}

.nav-icon {
  width: 24px;
  min-width: 24px;
  height: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.nav-icon svg {
  width: 18px;
  height: 18px;
  opacity: 0.75;
}

.nav-item:hover .nav-icon svg,
.nav-item.is-active .nav-icon svg {
  opacity: 1;
}

.nav-label {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.app-global-toast {
  position: fixed;
  top: 16px;
  left: 50%;
  transform: translateX(-50%);
  z-index: 10060;
  max-width: min(420px, calc(100vw - 32px));
  padding: 12px 18px;
  border-radius: 999px;
  font-size: 14px;
  font-weight: 500;
  line-height: 1.4;
  text-align: center;
  box-shadow: 0 10px 40px rgba(15, 23, 42, 0.35);
  pointer-events: none;
}
.app-global-toast--info {
  background: linear-gradient(145deg, #1e3a5f, #1e293b);
  color: #e2e8f0;
  border: 1px solid rgba(148, 163, 184, 0.35);
}
.app-global-toast--error {
  background: linear-gradient(145deg, #7f1d1d, #991b1b);
  color: #fef2f2;
  border: 1px solid rgba(252, 165, 165, 0.45);
}
.app-toast-fade-enter-active,
.app-toast-fade-leave-active {
  transition: opacity 0.2s ease, transform 0.2s ease;
}
.app-toast-fade-enter,
.app-toast-fade-leave-to {
  opacity: 0;
  transform: translateX(-50%) translateY(-8px);
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

.app-sidebar.is-collapsed .sidebar-footer {
  justify-content: center;
  padding: 12px 8px;
}

.sidebar-user {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
  flex: 1;
}

.app-sidebar.is-collapsed .sidebar-user {
  justify-content: center;
}

.user-avatar {
  width: 30px;
  height: 30px;
  min-width: 30px;
  border-radius: 50%;
  background: var(--brand-blue, #1456F0);
  color: #fff;
  font-size: 13px;
  font-weight: 600;
  display: flex;
  align-items: center;
  justify-content: center;
}

.user-avatar--logo {
  border-radius: 8px;
  padding: 0;
  overflow: hidden;
}

.user-avatar-brand {
  width: 100%;
  height: 100%;
  display: block;
  object-fit: cover;
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
  border-bottom: 1px solid var(--border-primary, #e5e7eb);
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

.search-icon-svg {
  color: var(--text-disabled, #9ca3af);
  flex-shrink: 0;
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
  transition: background 0.2s;
  position: relative;
  color: var(--text-secondary, #6b7280);
}

.topbar-icon-btn:hover {
  background: var(--bg-page, #f0f2f5);
  color: var(--text-primary, #1f2937);
}

.topbar-lang-select {
  padding: 4px 8px;
  border-radius: 4px;
  border: 1px solid var(--border-primary, #e5e7eb);
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
  background: var(--brand-blue, #1456F0);
  color: #fff;
  font-size: 14px;
  font-weight: 600;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
}

.topbar-avatar--logo {
  border-radius: 8px;
  padding: 0;
  overflow: hidden;
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

/* ========== Responsive: Desktop (>=1024px) ========== */
@media (min-width: 1024px) {
  .sidebar-collapse-btn {
    display: flex;
  }
}

/* ========== Responsive: Tablet (768px - 1023px) ========== */
@media (min-width: 768px) and (max-width: 1023px) {
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
    width: var(--sidebar-width, 240px);
    min-width: var(--sidebar-width, 240px);
  }

  .app-sidebar.is-collapsed {
    width: var(--sidebar-width, 240px);
    min-width: var(--sidebar-width, 240px);
  }

  .app-sidebar.is-open {
    transform: translateX(0);
    box-shadow: 4px 0 24px rgba(0, 0, 0, 0.15);
  }

  .app-topbar {
    padding-left: 56px;
  }

  .sidebar-collapse-btn {
    display: none;
  }
}

/* ========== Responsive: Mobile (<768px) ========== */
@media (max-width: 767px) {
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
    width: var(--sidebar-width, 240px);
    min-width: var(--sidebar-width, 240px);
  }

  .app-sidebar.is-collapsed {
    width: var(--sidebar-width, 240px);
    min-width: var(--sidebar-width, 240px);
  }

  .app-sidebar.is-open {
    transform: translateX(0);
    box-shadow: 4px 0 24px rgba(0, 0, 0, 0.15);
  }

  .app-topbar {
    padding-left: 56px;
  }

  .sidebar-collapse-btn {
    display: none;
  }

  .topbar-search {
    display: none;
  }

  .app-main {
    padding: 16px;
  }
}

/* 云开发中心：站内嵌入开启时隐藏主导航侧栏、收紧顶栏与主区内边距，放大 iframe 可用宽度 */
.cloud-dev-embed-max .sidebar-wrapper {
  display: none;
}

.cloud-dev-embed-max .app-topbar {
  min-height: 48px;
  height: auto;
  padding: 8px 16px;
}

.cloud-dev-embed-max .topbar-search {
  display: none;
}

.cloud-dev-embed-max .app-main-clouddev-embed {
  padding: 8px 10px 12px;
  display: flex;
  flex-direction: column;
  flex: 1;
  min-height: 0;
  overflow: auto;
}

@media (max-width: 767px) {
  .cloud-dev-embed-max .app-topbar {
    padding-left: 16px;
  }
}
</style>
