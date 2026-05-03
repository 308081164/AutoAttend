<template>
  <div id="app" class="app-container">
    <!-- 顶部导航栏 -->
    <el-container v-if="hasToken" class="app-layout">
      <el-header class="app-header" height="56px">
        <div class="header-left">
          <img class="brand-logo" src="/brand-logo.svg" width="28" height="28" alt="" aria-hidden="true" />
          <span class="brand-text">AutoAttend 监测台</span>
        </div>
        <el-menu
          :default-active="activeMenu"
          mode="horizontal"
          class="header-menu"
          router
          @select="handleMenuSelect"
        >
          <el-menu-item index="/">
            <i class="el-icon-s-data"></i>
            <span>看板</span>
          </el-menu-item>
          <el-menu-item index="/tenants">
            <i class="el-icon-s-custom"></i>
            <span>租户</span>
          </el-menu-item>
          <el-menu-item index="/system">
            <i class="el-icon-setting"></i>
            <span>系统配置</span>
          </el-menu-item>
          <el-menu-item index="/showcase-audit">
            <i class="el-icon-view"></i>
            <span>展示审核</span>
          </el-menu-item>
          <el-menu-item index="/lab-feedback">
            <i class="el-icon-chat-dot-round"></i>
            <span>实验室反馈</span>
          </el-menu-item>
        </el-menu>
        <div class="header-right">
          <el-button type="text" class="logout-btn" @click="logout">
            <i class="el-icon-switch-button"></i>
            退出
          </el-button>
        </div>
      </el-header>
      <el-main class="app-main">
        <router-view />
      </el-main>
    </el-container>
    <router-view v-else />
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
    },
    activeMenu () {
      return this.$route.path
    }
  },
  methods: {
    handleMenuSelect (index) {
      this.$router.push(index).catch(() => {})
    },
    logout () {
      this.$confirm('确定要退出登录吗？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        localStorage.removeItem(TOKEN_KEY)
        this.$router.push({ name: 'login' }).catch(() => {})
      }).catch(() => {})
    }
  }
}
</script>

<style>
/* 全局样式 - 浅色主题 */
* {
  box-sizing: border-box;
}
body {
  margin: 0;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
  background: #f0f2f5;
  color: #303133;
}
.app-container {
  min-height: 100vh;
}
.app-layout {
  min-height: 100vh;
}
.app-header {
  display: flex;
  align-items: center;
  padding: 0 20px !important;
  background: #fff;
  border-bottom: 1px solid #e4e7ed;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
  position: sticky;
  top: 0;
  z-index: 100;
}
.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-shrink: 0;
}
.brand-logo {
  border-radius: 6px;
  flex-shrink: 0;
}
.brand-text {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  white-space: nowrap;
  margin-right: 8px;
}
.header-menu {
  flex: 1;
  border-bottom: none !important;
  margin-left: 12px;
}
.header-menu .el-menu-item {
  font-size: 14px;
  height: 56px;
  line-height: 56px;
  padding: 0 16px;
}
.header-menu .el-menu-item.is-active {
  color: #409eff;
  border-bottom-color: #409eff;
}
.header-right {
  flex-shrink: 0;
}
.logout-btn {
  font-size: 14px;
  color: #909399;
}
.logout-btn:hover {
  color: #409eff;
}
.app-main {
  padding: 20px;
  background: #f0f2f5;
  min-height: calc(100vh - 56px);
}
</style>
