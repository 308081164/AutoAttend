import Vue from 'vue'
import App from './App.vue'
import router from './router'
import axios from 'axios'
import { i18n } from './locales'

Vue.config.productionTip = false

// VSCode Webview 嵌入模式：允许插件通过 URL 注入 token，实现网页端能力复用
;(function bootstrapEmbedBridge () {
  try {
    const params = new URLSearchParams(window.location.search || '')
    const embed = params.get('embed')
    const adminToken = params.get('adminToken')
    const collabToken = params.get('collabToken')
    const username = params.get('username')
    if (embed === '1') {
      window.__AUTOATTEND_EMBED__ = true
    }
    if (adminToken) window.localStorage.setItem('autoattend_token', adminToken)
    if (collabToken) window.localStorage.setItem('autoattend_collab_token', collabToken)
    if (username) window.localStorage.setItem('autoattend_username', username)

    // 清理敏感 query 参数，避免地址栏长期暴露 token
    if (adminToken || collabToken || username) {
      params.delete('adminToken')
      params.delete('collabToken')
      params.delete('username')
      const qs = params.toString()
      const nextUrl = window.location.pathname + (qs ? `?${qs}` : '') + window.location.hash
      window.history.replaceState({}, document.title, nextUrl)
    }
  } catch (e) {
    // ignore
  }
})()

// 统一使用相对路径 /api，方便本地 devServer 代理和容器内 Nginx 反代
axios.defaults.baseURL = '/api'

// 请求拦截器：自动添加 token（仅 /collab/* 协作 API 用协作 JWT；勿用 url.indexOf('collab')，否则会误匹配 /admin/auth/collab-token）
function isCollabApiPath (url) {
  if (!url) return false
  const p = String(url).split('?')[0]
  return p.startsWith('/collab/') || p.startsWith('collab/')
}

axios.interceptors.request.use(config => {
  const url = config.url || ''
  const token = isCollabApiPath(url)
    ? window.localStorage.getItem('autoattend_collab_token')
    : window.localStorage.getItem('autoattend_token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

// 响应拦截器：统一处理401错误
axios.interceptors.response.use(
  response => {
    return response
  },
  error => {
    if (error.response) {
      const { status } = error.response
      
      // 处理401未授权错误
      if (status === 401) {
        console.warn('检测到401未授权错误，跳转到登录页面')
        
        // 清除本地存储的token
        window.localStorage.removeItem('autoattend_token')
        window.localStorage.removeItem('autoattend_collab_token')
        
        // 延迟跳转到登录页面，避免在Vue实例创建前调用router
        setTimeout(() => {
          const p = window.location.pathname || ''
          if (p === '/login' || p === '/register' || p === '/member-login') return
          const memberArea = p.indexOf('/collab') === 0 || p === '/member' || p.startsWith('/lab') || p.startsWith('/cloud-dev')
          window.location.href = memberArea ? '/member-login' : '/login'
        }, 100)
      }
      
      // 处理403禁止访问错误
      if (status === 403) {
        console.warn('检测到403禁止访问错误')
        // 可以在这里添加特定的403处理逻辑
      }
    }
    
    return Promise.reject(error)
  }
)

Vue.prototype.$http = axios

new Vue({
  router,
  i18n,
  render: h => h(App)
}).$mount('#app')
