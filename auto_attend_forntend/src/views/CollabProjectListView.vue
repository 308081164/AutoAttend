<template>
  <div class="collab-page">
    <div class="page-header">
      <h1>{{ $t('collab.projectsTitle') }}</h1>
      <div class="header-actions">
        <span class="user-info">{{ userEmail }}</span>
        <button class="link-button" @click="logout">{{ $t('app.logout') }}</button>
      </div>
    </div>
    <div v-if="showIdentitySwitcher" class="identity-bar">
      <label class="identity-label" for="collab-acting-select">{{ $t('collab.identityLabel') }}</label>
      <select
        id="collab-acting-select"
        v-model.number="actingUserId"
        class="identity-select"
        @change="onActingIdentityChange"
      >
        <option v-for="it in linkedIdentities" :key="it.id" :value="it.id">
          {{ formatIdentityOption(it) }}
        </option>
      </select>
      <span class="identity-hint">{{ $t('collab.identityHint') }}</span>
    </div>
    <div v-if="isAdmin" class="collab-tabs" role="tablist">
      <button
        type="button"
        class="collab-tab"
        :class="{ active: projectTab === 'tenant' }"
        role="tab"
        @click="projectTab = 'tenant'"
      >
        {{ $t('collab.tabOrgProjects') }}
      </button>
      <button
        type="button"
        class="collab-tab"
        :class="{ active: projectTab === 'external' }"
        role="tab"
        @click="projectTab = 'external'"
      >
        {{ $t('collab.tabParticipationProjects') }}
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
          <span v-if="isAdmin && p.projectParticipation" class="project-part-badge" :class="'pp-' + p.projectParticipation">
            {{ p.projectParticipation === 'organization' ? $t('collab.badgeOrganization') : $t('collab.badgeParticipation') }}
          </span>
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
import { getStoredCollabActingUserId, setStoredCollabActingUserId } from '@/utils/collabActingUser'

export default {
  name: 'CollabProjectListView',
  data () {
    return {
      projects: [],
      projectTab: 'tenant',
      projectSearch: '',
      loading: true,
      userEmail: '',
      sessionUserId: null,
      linkedIdentities: [],
      actingUserId: null
    }
  },
  computed: {
    isAdmin () {
      return !!window.localStorage.getItem('autoattend_token')
    },
    showIdentitySwitcher () {
      return this.linkedIdentities.length > 1
    },
    displayProjects () {
      if (!this.isAdmin) {
        return this.projects
      }
      const org = this.projects.filter(p => p.projectParticipation === 'organization')
      const part = this.projects.filter(p => p.projectParticipation === 'participation')
      return this.projectTab === 'external' ? part : org
    },
    activeTabDesc () {
      if (!this.isAdmin) return this.$t('collab.selectProject')
      if (this.projectTab === 'external') {
        return this.$t('collab.selectProjectParticipation')
      }
      return this.$t('collab.selectProjectOrg')
    },
    emptyTabMessage () {
      if (!this.isAdmin) {
        return this.$t('collab.noProjects')
      }
      if (this.projectTab === 'external') {
        return this.$t('collab.noParticipationProjects')
      }
      return this.$t('collab.noOrgProjects')
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
      .then(() => this.loadLinkedIdentities())
      .then(() => this.loadProjects())
  },
  methods: {
    formatIdentityOption (it) {
      const em = (it.email || '').trim()
      const name = (it.name || '').trim()
      if (name && em && name !== em) return `${name} (${em})`
      return em || name || ('#' + it.id)
    },
    async loadLinkedIdentities () {
      try {
        const resp = await this.$http.get('/collab/auth/linked-identities')
        if (resp.data && resp.data.code === 0) {
          const d = resp.data.data || {}
          this.linkedIdentities = Array.isArray(d.items) ? d.items : []
          const apiActing = d.actingUserId != null ? Number(d.actingUserId) : null
          const stored = getStoredCollabActingUserId()
          const storedNum = stored != null ? Number(stored) : null
          const ids = new Set(this.linkedIdentities.map(x => x.id))
          if (storedNum != null && ids.has(storedNum)) {
            this.actingUserId = storedNum
          } else if (apiActing != null && ids.has(apiActing)) {
            this.actingUserId = apiActing
          } else if (this.sessionUserId != null && ids.has(this.sessionUserId)) {
            this.actingUserId = this.sessionUserId
          } else if (this.linkedIdentities.length) {
            this.actingUserId = this.linkedIdentities[0].id
          }
          if (this.actingUserId != null) {
            setStoredCollabActingUserId(this.actingUserId)
          }
        }
      } catch (e) { /* ignore */ }
    },
    onActingIdentityChange () {
      setStoredCollabActingUserId(this.actingUserId)
      this.loadProjects()
    },
    async loadMe () {
      try {
        const resp = await this.$http.get('/collab/auth/me')
        if (resp.data && resp.data.code === 0) {
          const d = resp.data.data || {}
          this.userEmail = d.email || ''
          this.sessionUserId = d.id != null ? Number(d.id) : null
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
    logout () {
      const admin = this.isAdmin
      window.localStorage.removeItem('autoattend_collab_token')
      window.localStorage.removeItem('autoattend_token')
      window.localStorage.removeItem('autoattend_username')
      this.$router.push({ name: admin ? 'login' : 'member-login' })
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

.identity-bar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: var(--space-sm);
  margin-bottom: var(--space-md);
  padding: var(--space-sm) var(--space-md);
  background: var(--bg-card);
  border: 1px solid var(--border-primary);
  border-radius: var(--radius-lg);
  font-size: 13px;
}

.identity-label {
  color: var(--text-secondary);
  margin: 0;
}

.identity-select {
  min-width: 220px;
  padding: 6px 10px;
  border-radius: var(--radius-md);
  border: 1px solid var(--border-primary);
  background: var(--bg-page);
  color: var(--text-primary);
}

.identity-hint {
  color: var(--text-tertiary);
  font-size: 12px;
}

.project-part-badge {
  display: inline-block;
  font-size: 11px;
  font-weight: 600;
  padding: 2px 8px;
  border-radius: 999px;
  margin-bottom: 6px;
}

.project-part-badge.pp-organization {
  background: rgba(20, 86, 240, 0.12);
  color: var(--brand-blue, #1456f0);
}

.project-part-badge.pp-participation {
  background: rgba(16, 185, 129, 0.14);
  color: #059669;
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
