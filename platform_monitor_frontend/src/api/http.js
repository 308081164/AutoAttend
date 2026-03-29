import axios from 'axios'

const TOKEN_KEY = 'platform_ops_token'

const http = axios.create({
  baseURL: '/api',
  timeout: 30000
})

http.interceptors.request.use((config) => {
  const t = localStorage.getItem(TOKEN_KEY)
  if (t) {
    config.headers.Authorization = 'Bearer ' + t
  }
  return config
})

export { http, TOKEN_KEY }
