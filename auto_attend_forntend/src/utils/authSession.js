/**
 * Vue 2 中仅读取 localStorage 的 computed 不会建立响应式依赖，token 变更后侧栏等 UI 可能不更新。
 * 在写入/清除 autoattend_* 会话键后调用 notifyAuthSessionChanged；需要响应的组件可 subscribeAuthSession。
 */
const subscribers = new Set()

export function subscribeAuthSession (fn) {
  if (typeof fn !== 'function') return function noop () {}
  subscribers.add(fn)
  return function unsubscribe () {
    subscribers.delete(fn)
  }
}

export function notifyAuthSessionChanged () {
  subscribers.forEach(fn => {
    try {
      fn()
    } catch (e) {
      // ignore
    }
  })
}
