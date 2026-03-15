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
      <router-link to="/collab-login">{{ $t('memberHome.backToLogin') }}</router-link>
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
        this.$router.push({ name: 'collab-login' })
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
        if (e.response && e.response.status === 401) {
          this.$router.push({ name: 'collab-login' })
        }
      } finally {
        this.statsLoading = false
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
.member-home {
  max-width: 640px;
  margin: 0 auto;
}

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 24px;
}

.page-title {
  margin: 0;
  font-size: 22px;
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

.link-button {
  border: none;
  background: transparent;
  color: #2563eb;
  cursor: pointer;
  font-size: 13px;
  padding: 4px 8px;
}

.link-button:hover {
  text-decoration: underline;
}

.section {
  margin-bottom: 28px;
}

.section-title {
  margin: 0 0 12px;
  font-size: 16px;
  font-weight: 600;
}

.section-desc {
  margin: 0 0 12px;
  font-size: 14px;
  color: #6b7280;
}

.stats-cards {
  display: flex;
  gap: 16px;
  flex-wrap: wrap;
}

.stat-card {
  flex: 1;
  min-width: 120px;
  padding: 16px;
  border-radius: 8px;
  background: #fff;
  border: 1px solid #e5e7eb;
}

.stat-value {
  font-size: 24px;
  font-weight: 600;
  color: #111827;
}

.stat-label {
  font-size: 13px;
  color: #6b7280;
  margin-top: 4px;
}

.primary-button {
  display: inline-block;
  padding: 10px 20px;
  border-radius: 6px;
  background: #2563eb;
  color: #fff;
  text-decoration: none;
  font-size: 14px;
}

.primary-button:hover {
  background: #1d4ed8;
}

.placeholder {
  padding: 16px;
  color: #6b7280;
  font-size: 14px;
}

.back-link {
  margin-top: 24px;
  font-size: 13px;
}

.back-link a {
  color: #2563eb;
}
</style>
