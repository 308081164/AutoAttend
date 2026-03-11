<template>
  <div id="app">
    <header class="app-header">
      <div class="app-title">AutoAttend 管理后台（MVP）</div>
      <div class="app-header-right">
        <span v-if="username" class="app-username">管理员：{{ username }}</span>
        <button v-if="username" class="link-button" @click="logout">退出</button>
      </div>
    </header>
    <main class="app-main">
      <router-view/>
    </main>
  </div>
</template>

<script>
export default {
  name: 'App',
  computed: {
    username () {
      return window.localStorage.getItem('autoattend_username') || ''
    }
  },
  methods: {
    logout () {
      window.localStorage.removeItem('autoattend_token')
      window.localStorage.removeItem('autoattend_username')
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
