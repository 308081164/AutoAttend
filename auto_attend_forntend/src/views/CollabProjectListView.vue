<template>
  <div class="collab-page">
    <div class="page-header">
      <h1>项目协作</h1>
      <div class="header-actions">
        <span class="user-info">{{ userEmail }}</span>
        <button class="link-button" @click="logout">退出</button>
      </div>
    </div>
    <p class="page-desc">选择有权限的项目进入多维表格</p>
    <div v-if="loading" class="placeholder">加载中...</div>
    <div v-else-if="!projects.length" class="placeholder">
      暂无有权限的项目。请先通过 GitHub 推送代码，系统将根据仓库自动创建项目并同步成员。
    </div>
    <ul v-else class="project-list">
      <li v-for="p in projects" :key="p.id" class="project-item">
        <router-link :to="{ name: 'collab-table', params: { projectId: p.id } }" class="project-link">
          <span class="project-name">{{ p.name }}</span>
          <span v-if="p.description" class="project-desc">{{ p.description }}</span>
          <span class="project-repo">仓库：{{ p.repoId }}</span>
        </router-link>
      </li>
    </ul>
    <div class="back-link">
      <router-link to="/">返回首页</router-link>
    </div>
  </div>
</template>

<script>
export default {
  name: 'CollabProjectListView',
  data () {
    return {
      projects: [],
      loading: true,
      userEmail: ''
    }
  },
  created () {
    this.loadMe()
    this.loadProjects()
  },
  methods: {
    async loadMe () {
      try {
        const resp = await this.$http.get('/collab/auth/me')
        if (resp.data && resp.data.code === 0) {
          this.userEmail = resp.data.data.email || ''
        }
      } catch (e) {
        this.$router.push({ name: 'collab-login' })
      }
    },
    async loadProjects () {
      this.loading = true
      try {
        const resp = await this.$http.get('/collab/projects')
        if (resp.data && resp.data.code === 0) {
          this.projects = resp.data.data.items || []
        }
      } catch (e) {
        if (e.response && e.response.status === 401) {
          this.$router.push({ name: 'collab-login' })
        }
      } finally {
        this.loading = false
      }
    },
    logout () {
      window.localStorage.removeItem('autoattend_collab_token')
      this.$router.push({ name: 'collab-login' })
    }
  }
}
</script>

<style scoped>
.collab-page {
  max-width: 640px;
  margin: 0 auto;
}

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}

.page-header h1 {
  margin: 0;
  font-size: 20px;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.user-info {
  font-size: 13px;
  color: #6b7280;
}

.page-desc {
  margin: 0 0 16px;
  font-size: 14px;
  color: #6b7280;
}

.placeholder {
  padding: 24px;
  text-align: center;
  color: #6b7280;
}

.project-list {
  list-style: none;
  padding: 0;
  margin: 0;
}

.project-item {
  margin-bottom: 8px;
}

.project-link {
  display: block;
  padding: 12px 16px;
  border-radius: 8px;
  background: #fff;
  border: 1px solid #e5e7eb;
  text-decoration: none;
  color: inherit;
}

.project-link:hover {
  border-color: #2563eb;
  background: #eff6ff;
}

.project-name {
  font-weight: 600;
  display: block;
}

.project-desc, .project-repo {
  font-size: 12px;
  color: #6b7280;
  display: block;
  margin-top: 4px;
}

.back-link {
  margin-top: 24px;
  font-size: 13px;
}

.back-link a {
  color: #2563eb;
}

.link-button {
  border: none;
  background: transparent;
  color: #2563eb;
  cursor: pointer;
  font-size: 13px;
  padding: 4px 8px;
}
</style>
