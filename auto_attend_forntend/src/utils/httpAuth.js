/**
 * 判断 axios 请求的 URL（相对 /api）是否属于协作端 /collab/*
 */
export function isCollabApiRequestUrl (url) {
  if (!url) return false
  const p = String(url).split('?')[0]
  return p.startsWith('/collab/') || p.startsWith('collab/')
}

/**
 * 401 时是否应执行「清空 token + 整页跳转登录」。
 * 未携带 Authorization 的 401（例如成员误触仅管理员可调用的接口）不应清空协作 JWT。
 */
export function shouldHandleUnauthorizedSession (error) {
  const cfg = error && error.config
  if (!cfg) return false
  const auth = cfg.headers && cfg.headers.Authorization
  if (!auth) return false
  return true
}

/** 协作页 401：按是否存在管理员会话跳转对应登录页（勿写死 login） */
export function redirectCollabUnauthorized (router) {
  if (!router) return
  const admin = !!(window.localStorage.getItem('autoattend_token') || '').trim()
  router.push({ name: admin ? 'login' : 'member-login' })
}
