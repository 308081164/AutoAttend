import Vue from 'vue'
import VueRouter from 'vue-router'
import LoginView from '../views/LoginView.vue'
import RegisterView from '../views/RegisterView.vue'
import LandingView from '../views/LandingView.vue'
import DashboardView from '../views/DashboardView.vue'
import TestView from '../views/TestView.vue'
import AiConfigView from '../views/AiConfigView.vue'
import CollabProjectListView from '../views/CollabProjectListView.vue'
import CollabTableView from '../views/CollabTableView.vue'
import MemberHomeView from '../views/MemberHomeView.vue'
import MemberLoginView from '../views/MemberLoginView.vue'
import TeamManageView from '../views/TeamManageView.vue'
import TenantAdminManageView from '../views/TenantAdminManageView.vue'
import CommitAnalysisView from '../views/CommitAnalysisView.vue'
import QuoteListView from '../views/QuoteListView.vue'
import QuoteProjectView from '../views/QuoteProjectView.vue'
import QuoteConfigView from '../views/QuoteConfigView.vue'
import PrototypeListView from '../views/PrototypeListView.vue'
import PrototypeProjectView from '../views/PrototypeProjectView.vue'
import NexusConsoleView from '../views/NexusConsoleView.vue'
import LabView from '../views/LabView.vue'
import CloudDevHubView from '../views/CloudDevHubView.vue'
import ClientBoardView from '../views/ClientBoardView.vue'
import AgentChatView from '../views/AgentChatView.vue'
import SubscriptionBillingView from '../views/SubscriptionBillingView.vue'

Vue.use(VueRouter)

const routes = [
  {
    path: '/login',
    name: 'login',
    component: LoginView,
    meta: { bareLayout: true }
  },
  {
    path: '/member-login',
    name: 'member-login',
    component: MemberLoginView,
    meta: { bareLayout: true }
  },
  {
    path: '/register',
    name: 'register',
    component: RegisterView,
    meta: { bareLayout: true }
  },
  {
    path: '/',
    name: 'landing',
    component: LandingView,
    meta: { public: true, bareLayout: true }
  },
  {
    path: '/console',
    name: 'dashboard',
    component: DashboardView
  },
  {
    path: '/subscription',
    name: 'subscription-billing',
    component: SubscriptionBillingView
  },
  {
    path: '/member',
    name: 'member-home',
    component: MemberHomeView,
    meta: { memberArea: true }
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
    path: '/api-config',
    name: 'api-config',
    component: AiConfigView
  },
  {
    path: '/ai-config',
    redirect: '/api-config'
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
    component: CollabProjectListView,
    meta: { memberArea: true }
  },
  {
    path: '/collab/projects/:projectId/table',
    name: 'collab-table',
    component: CollabTableView,
    meta: { memberArea: true }
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
  },
  {
    path: '/nexus',
    name: 'nexus-console',
    component: NexusConsoleView
  },
  {
    path: '/lab',
    name: 'lab',
    component: LabView,
    meta: { memberArea: true }
  },
  {
    path: '/cloud-dev',
    name: 'cloud-dev-hub',
    component: CloudDevHubView,
    meta: { memberArea: true }
  },
  {
    path: '/client-board/:token',
    name: 'client-board',
    component: ClientBoardView,
    meta: { public: true, bareLayout: true }
  },
  {
    path: '/agent/:publicToken',
    name: 'agent-chat',
    component: AgentChatView,
    meta: { public: true, bareLayout: true }
  }
]

const router = new VueRouter({
  mode: 'history',
  routes
})

// Vue Router 3: next() returns undefined, never call .catch() on it
router.beforeEach(async (to, from, next) => {
  if (to.meta && to.meta.public) {
    next()
    return
  }
  const adminToken = window.localStorage.getItem('autoattend_token')
  const collabToken = window.localStorage.getItem('autoattend_collab_token')
  const memberOnly = !!collabToken && !adminToken
  if (memberOnly) {
    const p = to.path
    if (
      p === '/console' || p.startsWith('/console/') ||
      p.startsWith('/quote') ||
      p === '/team' || p.startsWith('/team/') ||
      p === '/subscription' || p.startsWith('/subscription/') ||
      p.startsWith('/api-config') || p.startsWith('/ai-config') ||
      p.startsWith('/prototype') ||
      p.startsWith('/nexus') ||
      p.startsWith('/tenant-admins')
    ) {
      next({ name: 'member-home' })
      return
    }
  }
  const isCollabPath = to.path.indexOf('/collab') === 0
  const isMemberHome = to.name === 'member-home'
  const isCollabLoginPath = to.path === '/collab-login'

  if (to.name === 'login' || to.name === 'register' || to.name === 'member-login') {
    if (to.name === 'member-login' && adminToken) { next({ name: 'dashboard' }); return }
    if (to.name === 'member-login' && collabToken) { next({ name: 'member-home' }); return }
    if (to.name === 'login' || to.name === 'register') {
      if (adminToken) { next({ name: 'dashboard' }); return }
      if (collabToken) { next({ name: 'member-home' }); return }
    }
    next()
    return
  }
  if (to.name === 'landing') {
    next()
    return
  }
  if (isCollabLoginPath) {
    if (adminToken) { next({ name: 'collab-projects' }); return }
    next()
    return
  }
  if (isCollabPath || isMemberHome) {
    if (adminToken) {
      // Admin user: try to get collab token, but always allow access
      try {
        const r = await fetch('/api/admin/auth/collab-token', { headers: { Authorization: 'Bearer ' + adminToken } })
        const data = await r.json()
        if (data.data && data.data.collabToken) {
          window.localStorage.setItem('autoattend_collab_token', data.data.collabToken)
        }
      } catch (e) {
        // Ignore API errors, admin is still authenticated
      }
      next()
      return
    }
    if (!collabToken) {
      next({ name: 'member-login' })
      return
    }
    next()
    return
  }
  if (to.name === 'dashboard') {
    if (adminToken) {
      next()
      return
    }
    if (collabToken && !adminToken) {
      next({ name: 'member-home' })
      return
    }
  }
  if (to.name !== 'login' && to.name !== 'register' && to.name !== 'member-login' && to.name !== 'landing' && !adminToken) {
    next({ name: 'login' })
  } else {
    next()
  }
})

export default router
