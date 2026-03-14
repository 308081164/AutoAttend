import Vue from 'vue'
import VueRouter from 'vue-router'
import LoginView from '../views/LoginView.vue'
import DashboardView from '../views/DashboardView.vue'
import TestView from '../views/TestView.vue'
import CollabLoginView from '../views/CollabLoginView.vue'
import CollabProjectListView from '../views/CollabProjectListView.vue'
import CollabTableView from '../views/CollabTableView.vue'

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
    path: '/test',
    name: 'test',
    component: TestView
  },
  {
    path: '/collab-login',
    name: 'collab-login',
    component: CollabLoginView
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

router.beforeEach((to, from, next) => {
  const isCollab = to.path.indexOf('/collab') === 0 && to.name !== 'collab-login'
  const token = isCollab
    ? window.localStorage.getItem('autoattend_collab_token')
    : window.localStorage.getItem('autoattend_token')

  if (to.name === 'login' && !isCollab) {
    if (token) next({ name: 'dashboard' })
    else next()
    return
  }
  if (isCollab) {
    if (!token && to.name !== 'collab-login') {
      next({ name: 'collab-login' })
    } else {
      next()
    }
    return
  }
  if (to.name !== 'login' && !token) {
    next({ name: 'login' })
  } else {
    next()
  }
})

export default router
