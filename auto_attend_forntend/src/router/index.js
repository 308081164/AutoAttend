import Vue from 'vue'
import VueRouter from 'vue-router'
import LoginView from '../views/LoginView.vue'
import DashboardView from '../views/DashboardView.vue'
import TestView from '../views/TestView.vue'
import AiConfigView from '../views/AiConfigView.vue'
import CollabProjectListView from '../views/CollabProjectListView.vue'
import CollabTableView from '../views/CollabTableView.vue'
import MemberHomeView from '../views/MemberHomeView.vue'
import TeamManageView from '../views/TeamManageView.vue'
import CommitAnalysisView from '../views/CommitAnalysisView.vue'

Vue.use(VueRouter)

const routes = [
  {
    path: '/login',
    name: 'login',
    component: LoginView
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
    path: '/ai-config',
    name: 'ai-config',
    component: AiConfigView
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

  if (to.name === 'login') {
    if (adminToken) next({ name: 'dashboard' })
    else if (collabToken) next({ name: 'member-home' })
    else next()
    return
  }
  if (isCollabLoginPath) {
    if (adminToken) next({ name: 'collab-projects' })
    else next()
    return
  }
  if (isCollabPath || isMemberHome) {
    if (adminToken && !collabToken) {
      try {
        const r = await fetch('/api/admin/auth/collab-token', { headers: { Authorization: 'Bearer ' + adminToken } })
        const data = await r.json()
        if (data.data && data.data.collabToken) {
          window.localStorage.setItem('autoattend_collab_token', data.data.collabToken)
          next()
        } else next({ name: 'login' })
      } catch (e) {
        next({ name: 'login' })
      }
      return
    }
    if (!collabToken && !adminToken) {
      next({ name: 'login' })
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
      next({ name: 'member-home' })
      return
    }
  }
  if (to.name !== 'login' && !adminToken) {
    next({ name: 'login' })
  } else {
    next()
  }
})

export default router
