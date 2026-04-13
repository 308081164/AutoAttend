<template>
  <div id="app" class="root">
    <header class="top">
      <div class="left">
        <img class="brand-logo" src="/brand-logo.svg" width="28" height="28" alt="" aria-hidden="true" />
        <span class="brand">AutoAttend 监测台</span>
        <nav v-if="hasToken" class="nav">
          <router-link :to="{ name: 'dashboard' }" class="nav-link" exact-active-class="active">看板</router-link>
          <router-link :to="{ name: 'tenants' }" class="nav-link" exact-active-class="active">租户</router-link>
        </nav>
      </div>
      <button v-if="hasToken" type="button" class="link" @click="logout">退出</button>
    </header>
    <main class="main">
      <router-view />
    </main>
  </div>
</template>

<script>
const TOKEN_KEY = 'platform_ops_token'

export default {
  name: 'App',
  computed: {
    hasToken () {
      void this.$route.fullPath
      return typeof localStorage !== 'undefined' && !!localStorage.getItem(TOKEN_KEY)
    }
  },
  methods: {
    logout () {
      localStorage.removeItem(TOKEN_KEY)
      this.$router.push({ name: 'login' }).catch(() => {})
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
  font-family: system-ui, -apple-system, Segoe UI, Roboto, sans-serif;
  background: #0f172a;
  color: #e2e8f0;
}
.root {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}
.top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 20px;
  background: #1e293b;
  border-bottom: 1px solid #334155;
}
.left {
  display: flex;
  align-items: center;
  gap: 18px;
}
.brand-logo {
  border-radius: 7px;
  flex-shrink: 0;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.25);
}
.brand {
  font-weight: 600;
  letter-spacing: 0.02em;
}
.nav {
  display: flex;
  align-items: center;
  gap: 12px;
}
.nav-link {
  color: #93c5fd;
  text-decoration: none;
  font-size: 14px;
}
.nav-link.active {
  color: #fff;
  border-bottom: 2px solid #60a5fa;
  padding-bottom: 2px;
}
.link {
  background: transparent;
  border: none;
  color: #93c5fd;
  cursor: pointer;
  font-size: 14px;
}
.link:hover {
  text-decoration: underline;
}
.main {
  flex: 1;
  padding: 24px 20px;
}
</style>
