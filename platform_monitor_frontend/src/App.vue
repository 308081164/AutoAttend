<template>
  <div id="app" class="root">
    <header class="top">
      <span class="brand">AutoAttend 监测台</span>
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
.brand {
  font-weight: 600;
  letter-spacing: 0.02em;
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
