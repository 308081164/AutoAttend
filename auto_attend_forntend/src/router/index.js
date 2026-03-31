import Vue from 'vue'
import VueRouter from 'vue-router'
import LoginView from '../views/LoginView.vue'
import RegisterView from '../views/RegisterView.vue'
import DashboardView from '../views/DashboardView.vue'
import TestView from '../views/TestView.vue'
import AiConfigView from '../views/AiConfigView.vue'
import CollabProjectListView from '../views/CollabProjectListView.vue'
import CollabTableView from '../views/CollabTableView.vue'
import MemberHomeView from '../views/MemberHomeView.vue'
import TeamManageView from '../views/TeamManageView.vue'
import TenantAdminManageView from '../views/TenantAdminManageView.vue'
import CommitAnalysisView from '../views/CommitAnalysisView.vue'
import QuoteListView from '../views/QuoteListView.vue'
import QuoteProjectView from '../views/QuoteProjectView.vue'
import QuoteConfigView from '../views/QuoteConfigView.vue'
import PrototypeListView from '../views/PrototypeListView.vue'
import PrototypeProjectView from '../views/PrototypeProjectView.vue'

Vue.use(VueRouter)

const routes = [
  {
    path: '/login',
    name: 'login',
    component: LoginView
  },
  {
    path: '/register',
    name: 'register',
    component: RegisterView
  },
  {
    path: '/',
    name: 'dashboard',
    component: DashboardView
  },
  {
    path: '/member',
    name: 'member-home',
    component: MemberHomeView
  },
  {
    path: '/test',
    name: 'test',
    component: TestView
  },
  {
    path: '/team',
    name: 'team-manage',
    component: TeamManageView
  },
  {
    path: '/tenant-admins',
    name: 'tenant-admins',
    component: TenantAdminManageView
  },
  {
    path: '/ai-config',
    name: 'ai-config',
    component: AiConfigView
  },
  {
    path: '/quote',
    name: 'quote-list',
    component: QuoteListView
  },
  {
    path: '/quote/config',
    name: 'quote-config',
    component: QuoteConfigView
  },
  {
    path: '/quote/baseline-price',
    redirect: { name: 'quote-config' }
  },
  {
    path: '/quote/:id',
    name: 'quote-project',
    component: QuoteProjectView
  },
  {
    path: '/commit/:commitSha',
    name: 'commit-analysis',
    component: CommitAnalysisView
  },
  {
    path: '/collab-login',
    redirect: { name: 'login' }
  },
  {
    path: '/collab/projects',
    name: 'collab-projects',
    component: CollabProjectListView
  },
  {
    path: '/collab/projects/:projectId/table',
    name: 'collab-table',
    component: CollabTableView
  },
  {
    path: '/prototype',
    name: 'prototype-list',
    component: PrototypeListView
  },
  {
    path: '/prototype/:projectId',
    name: 'prototype-project',
    component: PrototypeProjectView
  }
]

const router = new VueRouter({
  mode: 'history',
  routes
})

router.beforeEach(async (to, from, next) => {
  const isCollabPath = to.path.indexOf('/collab') === 0
  const isMemberHome = to.name === 'member-home'
  const isCollabLoginPath = to.path === '/collab-login'
  const adminToken = window.localStorage.getItem('autoattend_token')
  const collabToken = window.localStorage.getItem('autoattend_collab_token')

  if (to.name === 'login' || to.name === 'register') {
    if (adminToken) next({ name: 'dashboard' }).catch(() => {})
    else if (collabToken) next({ name: 'member-home' }).catch(() => {})
    else next()
    return
  }
  if (isCollabLoginPath) {
    if (adminToken) next({ name: 'collab-projects' }).catch(() => {})
    else next()
    return
  }
  if (isCollabPath || isMemberHome) {
    // 有管理员 token 时始终拉取并覆盖协作 token，避免使用过期的 collab token 导致 401
    if (adminToken) {
      try {
        const r = await fetch('/api/admin/auth/collab-token', { headers: { Authorization: 'Bearer ' + adminToken } })
        const data = await r.json()
        if (data.data && data.data.collabToken) {
          window.localStorage.setItem('autoattend_collab_token', data.data.collabToken)
          next()
        } else {
          next({ name: 'login' }).catch(() => {})
        }
      } catch (e) {
        next({ name: 'login' }).catch(() => {})
      }
      return
    }
    if (!collabToken) {
      next({ name: 'login' }).catch(() => {})
      return
    }
    next()
    return
  }
  if (to.name === 'dashboard' || to.path === '/') {
    if (adminToken) {
      next()
      return
    }
    if (collabToken && !adminToken) {
      next({ name: 'member-home' }).catch(() => {})
      return
    }
  }
  if (to.name !== 'login' && to.name !== 'register' && !adminToken) {
    next({ name: 'login' }).catch(() => {})
  } else {
    next()
  }
})

export default router
