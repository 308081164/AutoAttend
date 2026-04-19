import Vue from 'vue'
import VueRouter from 'vue-router'
import LoginPage from '../views/LoginPage.vue'
import DashboardPage from '../views/DashboardPage.vue'
import TenantsPage from '../views/TenantsPage.vue'
import TenantDetailPage from '../views/TenantDetailPage.vue'
import SystemSettingsPage from '../views/SystemSettingsPage.vue'
import LabFeedbackPage from '../views/LabFeedbackPage.vue'

Vue.use(VueRouter)

const TOKEN_KEY = 'platform_ops_token'

const router = new VueRouter({
  mode: 'history',
  base: import.meta.env.BASE_URL,
  routes: [
    { path: '/login', name: 'login', component: LoginPage, meta: { public: true } },
    { path: '/', name: 'dashboard', component: DashboardPage },
    { path: '/tenants', name: 'tenants', component: TenantsPage },
    { path: '/tenants/:id', name: 'tenant-detail', component: TenantDetailPage },
    { path: '/system', name: 'system-settings', component: SystemSettingsPage },
    { path: '/lab-feedback', name: 'lab-feedback', component: LabFeedbackPage }
  ]
})

router.beforeEach((to, from, next) => {
  const token = localStorage.getItem(TOKEN_KEY)
  if (to.meta.public) {
    if (token && to.name === 'login') {
      next({ name: 'dashboard' })
      return
    }
    next()
    return
  }
  if (!token) {
    next({ name: 'login', query: { redirect: to.fullPath } })
    return
  }
  next()
})

export default router
