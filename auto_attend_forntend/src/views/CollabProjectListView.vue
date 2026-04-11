<template>
  <div class="collab-page">
    <div class="page-header">
      <h1>{{ $t('collab.projectsTitle') }}</h1>
      <div class="header-actions">
        <span class="user-info">{{ userEmail }}</span>
        <button class="link-button" @click="logout">{{ $t('app.logout') }}</button>
      </div>
    </div>
    <p class="page-desc">{{ $t('collab.selectProject') }}</p>
    <div v-if="loading" class="placeholder">{{ $t('collab.loading') }}</div>
    <div v-else-if="!projects.length" class="placeholder">
      {{ $t('collab.noProjects') }}
    </div>
    <ul v-else class="project-list">
      <li v-for="p in projects" :key="p.id" class="project-item">
        <router-link :to="{ name: 'collab-table', params: { projectId: p.id } }" class="project-link">
          <span class="project-name">{{ p.name }}</span>
          <span v-if="p.description" class="project-desc">{{ p.description }}</span>
          <span class="project-repo">{{ $t('collab.repoLabel') }}：{{ p.repoId }}</span>
        </router-link>
      </li>
    </ul>
    <div class="back-link">
      <router-link :to="{ name: 'dashboard' }">{{ $t('collab.backHome') }}</router-link>
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
        if (e.response && e.response.status === 401) this.$router.push({ name: 'login' })
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
        if (e.response && e.response.status === 401) this.$router.push({ name: 'login' })
      } finally {
        this.loading = false
      }
    },
    logout () {
      window.localStorage.removeItem('autoattend_collab_token')
      window.localStorage.removeItem('autoattend_token')
      this.$router.push({ name: 'login' })
    }
  }
}
</script>

<style scoped>
.collab-page {
  max-width: 640px;
  margin: 0 auto;
  padding: var(--space-xl);
  background: var(--bg-page);
  min-height: 100vh;
  box-sizing: border-box;
}

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: var(--space-sm);
}

.page-header h1 {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
  color: var(--text-primary);
}

.header-actions {
  display: flex;
  align-items: center;
  gap: var(--space-md);
}

.user-info {
  font-size: 13px;
  color: var(--text-tertiary);
}

.page-desc {
  margin: 0 0 var(--space-lg);
  font-size: 14px;
  color: var(--text-secondary);
}

.placeholder {
  padding: var(--space-xxl);
  text-align: center;
  color: var(--text-tertiary);
  background: var(--bg-card);
  border: 1px solid var(--border-primary);
  border-radius: var(--radius-lg);
}

.project-list {
  list-style: none;
  padding: 0;
  margin: 0;
}

.project-item {
  margin-bottom: var(--space-sm);
}

.project-link {
  display: block;
  padding: var(--space-md) var(--space-lg);
  border-radius: var(--radius-lg);
  background: var(--bg-card);
  border: 1px solid var(--border-primary);
  text-decoration: none;
  color: inherit;
  transition: all 0.2s;
  box-shadow: var(--shadow-sm);
}

.project-link:hover {
  border-color: var(--brand-blue);
  background: #F0F5FF;
  box-shadow: var(--shadow-md);
}

.project-name {
  font-weight: 600;
  display: block;
  color: var(--text-primary);
}

.project-desc,
.project-repo {
  font-size: 12px;
  color: var(--text-tertiary);
  display: block;
  margin-top: 4px;
}

.back-link {
  margin-top: var(--space-xxl);
  font-size: 13px;
}

.back-link a {
  color: var(--brand-blue);
  text-decoration: none;
  transition: opacity 0.2s;
}

.back-link a:hover {
  opacity: 0.75;
}

.link-button {
  border: none;
  background: transparent;
  color: var(--brand-blue);
  cursor: pointer;
  font-size: 13px;
  padding: 4px var(--space-sm);
  transition: opacity 0.2s;
}

.link-button:hover {
  opacity: 0.75;
}
</style>
