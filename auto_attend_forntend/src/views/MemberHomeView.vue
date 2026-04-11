<template>
  <div class="member-home">
    <div class="page-header">
      <h1 class="page-title">{{ $t('memberHome.title') }}</h1>
      <div class="header-actions">
        <span class="user-info">{{ userEmail }}</span>
        <button class="link-button" @click="logout">{{ $t('app.logout') }}</button>
      </div>
    </div>

    <section class="section stats-section">
      <h2 class="section-title">{{ $t('memberHome.myStats') }}</h2>
      <div v-if="statsLoading" class="placeholder">{{ $t('collab.loading') }}</div>
      <div v-else class="stats-cards">
        <div class="stat-card">
          <div class="stat-value">{{ overview.projectCount ?? 0 }}</div>
          <div class="stat-label">{{ $t('memberHome.projectCount') }}</div>
        </div>
        <div class="stat-card">
          <div class="stat-value">{{ overview.commitCountTotal ?? 0 }}</div>
          <div class="stat-label">{{ $t('memberHome.commitCountTotal') }}</div>
        </div>
        <div class="stat-card">
          <div class="stat-value">{{ overview.commitCountLast7Days ?? 0 }}</div>
          <div class="stat-label">{{ $t('memberHome.commitCountLast7Days') }}</div>
        </div>
      </div>
    </section>

    <section class="section">
      <h2 class="section-title">{{ $t('memberHome.myProjects') }}</h2>
      <p class="section-desc">{{ $t('collab.selectProject') }}</p>
      <router-link to="/collab/projects" class="primary-button">{{ $t('memberHome.goToProjects') }}</router-link>
    </section>

    <div class="back-link">
      <router-link to="/login">{{ $t('memberHome.backToLogin') }}</router-link>
    </div>
  </div>
</template>

<script>
export default {
  name: 'MemberHomeView',
  data () {
    return {
      userEmail: '',
      overview: {},
      statsLoading: true
    }
  },
  created () {
    this.loadMe()
    this.loadOverview()
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
    async loadOverview () {
      this.statsLoading = true
      try {
        const resp = await this.$http.get('/collab/stats/overview')
        if (resp.data && resp.data.code === 0) {
          this.overview = resp.data.data || {}
        }
      } catch (e) {
        if (e.response && e.response.status === 401) this.$router.push({ name: 'login' })
      } finally {
        this.statsLoading = false
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
.member-home {
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
  margin-bottom: var(--space-xxl);
}

.page-title {
  margin: 0;
  font-size: 22px;
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
  text-decoration: underline;
}

.section {
  margin-bottom: var(--space-xxl);
}

.section-title {
  margin: 0 0 var(--space-md);
  font-size: 16px;
  font-weight: 600;
  color: var(--text-primary);
}

.section-desc {
  margin: 0 0 var(--space-md);
  font-size: 14px;
  color: var(--text-secondary);
}

.stats-cards {
  display: flex;
  gap: var(--space-lg);
  flex-wrap: wrap;
}

.stat-card {
  flex: 1;
  min-width: 120px;
  padding: var(--space-lg);
  border-radius: var(--radius-lg);
  background: var(--bg-card);
  border: 1px solid var(--border-primary);
  box-shadow: var(--shadow-sm);
  transition: box-shadow 0.2s;
}

.stat-card:hover {
  box-shadow: var(--shadow-md);
}

.stat-value {
  font-size: 24px;
  font-weight: 600;
  color: var(--text-primary);
}

.stat-label {
  font-size: 13px;
  color: var(--text-tertiary);
  margin-top: 4px;
}

.primary-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: var(--space-sm) var(--space-xl);
  border-radius: var(--radius-md);
  background: var(--brand-blue);
  color: #fff;
  text-decoration: none;
  font-size: 14px;
  font-weight: 500;
  transition: opacity 0.2s;
  box-shadow: var(--shadow-sm);
}

.primary-button:hover {
  opacity: 0.85;
}

.placeholder {
  padding: var(--space-lg);
  color: var(--text-tertiary);
  font-size: 14px;
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
</style>
