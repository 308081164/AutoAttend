import Vue from 'vue'
import VueRouter from 'vue-router'
import { notifyAuthSessionChanged } from '../utils/authSession'
import LoginView from '../views/LoginView.vue'
import RegisterView from '../views/RegisterView.vue'
import LandingView from '../views/LandingView.vue'
import LandingDetailView from '../views/LandingDetailView.vue'
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
import ShowcaseConfigView from '../views/ShowcaseConfigView.vue'
import QuoteProjectView from '../views/QuoteProjectView.vue'
import QuoteSolutionWizardView from '../views/QuoteSolutionWizardView.vue'
import QuoteConfigView from '../views/QuoteConfigView.vue'
import PrototypeListView from '../views/PrototypeListView.vue'
import PrototypeProjectView from '../views/PrototypeProjectView.vue'
import PenpotDiagnoseView from '../views/PenpotDiagnoseView.vue'
import NexusConsoleView from '../views/NexusConsoleView.vue'
import LabView from '../views/LabView.vue'
import CloudDevHubView from '../views/CloudDevHubView.vue'
import ClientBoardView from '../views/ClientBoardView.vue'
import AgentChatView from '../views/AgentChatView.vue'
import QuickQuoteLanding from '../views/QuickQuoteLanding.vue'
import SubscriptionBillingView from '../views/SubscriptionBillingView.vue'
import XianyuGuardView from '../views/XianyuGuardView.vue'
import BizDashboardView from '../views/BizDashboardView.vue'
import BizCustomersView from '../views/BizCustomersView.vue'
import BizCustomerDetailView from '../views/BizCustomerDetailView.vue'
import BizOpportunitiesView from '../views/BizOpportunitiesView.vue'
import MarketplaceListView from '../views/MarketplaceListView.vue'
import MarketplaceDetailView from '../views/MarketplaceDetailView.vue'
import MarketplaceMineView from '../views/MarketplaceMineView.vue'
import MarketplacePendingView from '../views/MarketplacePendingView.vue'

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
    path: '/features/:id',
    name: 'landing-detail',
    component: LandingDetailView,
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
    path: '/marketplace',
    name: 'marketplace-list',
    component: MarketplaceListView
  },
  {
    path: '/marketplace/mine',
    name: 'marketplace-mine',
    component: MarketplaceMineView
  },
  {
    path: '/marketplace/pending',
    name: 'marketplace-pending',
    component: MarketplacePendingView
  },
  {
    path: '/marketplace/:id',
    name: 'marketplace-detail',
    component: MarketplaceDetailView
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
    path: '/quote/showcase-config',
    name: 'showcase-config',
    component: ShowcaseConfigView
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
    path: '/quote/solution-wizard',
    name: 'quote-solution-wizard',
    component: QuoteSolutionWizardView
  },
  {
    path: '/quote/biz-dashboard',
    name: 'biz-dashboard',
    component: BizDashboardView
  },
  {
    path: '/quote/customers',
    name: 'biz-customers',
    component: BizCustomersView
  },
  {
    path: '/quote/customers/:id',
    name: 'biz-customer-detail',
    component: BizCustomerDetailView
  },
  {
    path: '/quote/opportunities',
    name: 'biz-opportunities',
    component: BizOpportunitiesView
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
    path: '/prototype/penpot-diagnose',
    name: 'penpot-diagnose',
    component: PenpotDiagnoseView
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
  },
  {
    path: '/quick-quote/:slug',
    name: 'quick-quote-landing',
    component: QuickQuoteLanding,
    meta: { public: true, bareLayout: true }
  },
  {
    path: '/xianyu',
    name: 'xianyu-guard',
    component: XianyuGuardView
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
  const adminToken = (window.localStorage.getItem('autoattend_token') || '').trim()
  const collabToken = (window.localStorage.getItem('autoattend_collab_token') || '').trim()
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
      p.startsWith('/marketplace') ||
      p.startsWith('/nexus') ||
      p.startsWith('/tenant-admins') ||
      p.startsWith('/xianyu')
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
          notifyAuthSessionChanged()
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
