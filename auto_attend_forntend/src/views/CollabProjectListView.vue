<template>
  <div class="collab-page">
    <div class="page-header">
      <h1>{{ $t('collab.projectsTitle') }}</h1>
      <div class="header-actions">
        <span class="user-info">{{ userEmail }}</span>
        <button class="link-button" @click="logout">{{ $t('app.logout') }}</button>
      </div>
    </div>
    <div v-if="isAdmin" class="collab-tabs" role="tablist">
      <button
        type="button"
        class="collab-tab"
        :class="{ active: projectTab === 'tenant' }"
        role="tab"
        @click="projectTab = 'tenant'"
      >
        {{ $t('collab.tabTenantProjects') }}
      </button>
      <button
        type="button"
        class="collab-tab"
        :class="{ active: projectTab === 'external' }"
        role="tab"
        @click="projectTab = 'external'"
      >
        {{ $t('collab.tabExternalProjects') }}
      </button>
    </div>
    <p class="page-desc">{{ activeTabDesc }}</p>
    <div v-if="loading" class="placeholder">{{ $t('collab.loading') }}</div>
    <div v-else-if="!displayProjects.length" class="placeholder">
      {{ emptyTabMessage }}
    </div>
    <template v-else>
      <div class="project-search-wrap">
        <input
          v-model.trim="projectSearch"
          type="search"
          class="project-search-input"
          :placeholder="$t('collab.searchProjectsPlaceholder')"
          :aria-label="$t('collab.searchProjectsPlaceholder')"
          autocomplete="off"
        >
      </div>
      <div v-if="!filteredProjects.length" class="placeholder">
        {{ $t('collab.noSearchResults') }}
      </div>
      <ul v-else class="project-list">
      <li v-for="p in filteredProjects" :key="p.id" class="project-item">
        <router-link :to="{ name: 'collab-table', params: { projectId: p.id } }" class="project-link">
          <span v-if="p.tenantName" class="project-tenant">{{ $t('collab.tenantLabel') }}：{{ p.tenantName }}</span>
          <span class="project-name">{{ p.name }}</span>
          <span v-if="p.description" class="project-desc">{{ p.description }}</span>
          <span class="project-repo">{{ $t('collab.repoLabel') }}：{{ p.repoId }}</span>
        </router-link>
      </li>
    </ul>
    </template>
  </div>
</template>

<script>
export default {
  name: 'CollabProjectListView',
  data () {
    return {
      projects: [],
      externalProjects: [],
      projectTab: 'tenant',
      projectSearch: '',
      loading: true,
      userEmail: ''
    }
  },
  computed: {
    isAdmin () {
      return !!window.localStorage.getItem('autoattend_token')
    },
    displayProjects () {
      return this.projectTab === 'external' ? this.externalProjects : this.projects
    },
    activeTabDesc () {
      if (this.projectTab === 'external') {
        return this.$t('collab.selectProjectExternal')
      }
      return this.$t('collab.selectProject')
    },
    emptyTabMessage () {
      if (this.projectTab === 'external') {
        return this.$t('collab.noExternalProjects')
      }
      return this.$t('collab.noProjects')
    },
    filteredProjects () {
      const q = (this.projectSearch || '').trim().toLowerCase()
      if (!q) return this.displayProjects
      return this.displayProjects.filter(p => {
        const name = String(p.name != null ? p.name : '').toLowerCase()
        const desc = String(p.description != null ? p.description : '').toLowerCase()
        const repo = String(p.repoId != null ? p.repoId : '').toLowerCase()
        const tenant = String(p.tenantName != null ? p.tenantName : '').toLowerCase()
        return name.includes(q) || desc.includes(q) || repo.includes(q) || tenant.includes(q)
      })
    }
  },
  watch: {
    projectTab () {
      this.projectSearch = ''
    }
  },
  created () {
    this.loadMe()
    this.loadProjects()
    this.loadExternalIfAdmin()
  },
  methods: {
    async loadMe () {
      try {
        const resp = await this.$http.get('/collab/auth/me')
        if (resp.data && resp.data.code === 0) {
          this.userEmail = resp.data.data.email || ''
        }
      } catch (e) {
        if (e.response && e.response.status === 401) {
          this.$router.push(this.isAdmin ? { name: 'login' } : { name: 'member-login' })
        }
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
          this.$router.push(this.isAdmin ? { name: 'login' } : { name: 'member-login' })
        }
      } finally {
        this.loading = false
      }
    },
    async loadExternalIfAdmin () {
      if (!this.isAdmin) return
      try {
        const resp = await this.$http.get('/admin/collab/external-projects')
        if (resp.data && resp.data.code === 0) {
          this.externalProjects = resp.data.data.items || []
        }
      } catch (e) { /* ignore */ }
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
  max-width: 920px;
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

.project-search-wrap {
  margin-bottom: var(--space-md);
}

.project-search-input {
  width: 100%;
  box-sizing: border-box;
  padding: 10px var(--space-md);
  font-size: 14px;
  color: var(--text-primary);
  background: var(--bg-card);
  border: 1px solid var(--border-primary);
  border-radius: var(--radius-lg);
  transition: border-color 0.2s, box-shadow 0.2s;
}

.project-search-input::placeholder {
  color: var(--text-tertiary);
}

.project-search-input:hover {
  border-color: var(--text-tertiary);
}

.project-search-input:focus {
  outline: none;
  border-color: var(--brand-blue);
  box-shadow: 0 0 0 3px rgba(20, 86, 240, 0.12);
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
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: var(--space-sm);
  align-items: stretch;
}

.project-item {
  display: flex;
  margin: 0;
  min-width: 0;
}

@media (max-width: 640px) {
  .project-list {
    grid-template-columns: 1fr;
  }
}

.project-link {
  display: block;
  flex: 1;
  min-width: 0;
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

.project-tenant {
  display: block;
  font-size: 11px;
  font-weight: 600;
  color: var(--brand-blue);
  margin-bottom: 4px;
}

.collab-tabs {
  display: flex;
  gap: 8px;
  margin-bottom: var(--space-sm);
}
.collab-tab {
  padding: 8px 14px;
  border-radius: 8px;
  border: 1px solid var(--border-primary);
  background: var(--bg-card);
  cursor: pointer;
  font-size: 14px;
}
.collab-tab.active {
  border-color: var(--brand-blue);
  background: #f0f5ff;
  color: var(--brand-blue);
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
