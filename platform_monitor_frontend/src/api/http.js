import axios from 'axios'

const TOKEN_KEY = 'platform_ops_token'

const http = axios.create({
  baseURL: '/api',
  timeout: 30000
})

// 请求拦截器：自动添加token
http.interceptors.request.use((config) => {
  const t = localStorage.getItem(TOKEN_KEY)
  if (t) {
    config.headers.Authorization = 'Bearer ' + t
  }
  return config
})

// 响应拦截器：统一处理401错误
http.interceptors.response.use(
  response => {
    return response
  },
  error => {
    if (error.response) {
      const { status } = error.response
      
      // 处理401未授权错误
      if (status === 401) {
        console.warn('平台监控后台：检测到401未授权错误，跳转到登录页面')
        
        // 清除本地存储的token
        localStorage.removeItem(TOKEN_KEY)
        
        // 延迟跳转到登录页面
        setTimeout(() => {
          if (window.location.pathname !== '/login') {
            window.location.href = '/login'
          }
        }, 100)
      }
    }
    
    return Promise.reject(error)
  }
)

export { http, TOKEN_KEY }
