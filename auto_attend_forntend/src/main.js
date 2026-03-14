import Vue from 'vue'
import App from './App.vue'
import router from './router'
import axios from 'axios'

Vue.config.productionTip = false

// 统一使用相对路径 /api，方便本地 devServer 代理和容器内 Nginx 反代
axios.defaults.baseURL = '/api'
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

Vue.prototype.$http = axios

new Vue({
  router,
  render: h => h(App)
}).$mount('#app')
