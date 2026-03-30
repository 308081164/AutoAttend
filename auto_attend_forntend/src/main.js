import Vue from 'vue'
import App from './App.vue'
import router from './router'
import axios from 'axios'
import { i18n } from './locales'

Vue.config.productionTip = false

// 统一使用相对路径 /api，方便本地 devServer 代理和容器内 Nginx 反代
axios.defaults.baseURL = '/api'

// 请求拦截器：自动添加token
axios.interceptors.request.use(config => {
  const url = config.url || ''
  const token = url.indexOf('/collab') !== -1
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
          if (window.location.pathname !== '/login' && window.location.pathname !== '/register') {
            window.location.href = '/login'
          }
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
