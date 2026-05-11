/**
 * 通用埋点 mixin —— 项目协作模块细化点击监测
 *
 * 使用方式：
 *   在 CollabTableView.vue 等组件中混入此 mixin，
 *   然后在需要埋点的地方调用 this.$trackClick('core_api_key')
 *
 * 埋点数据会通过 POST /admin/ops/events/component-click 发送到后端，
 * 所有错误静默忽略，不影响主业务流程。
 */
export default {
  methods: {
    /**
     * 记录一次功能点击事件
     * @param {string} coreApiKey - 核心功能标识（如 collab.record.create）
     * @param {string} [componentKey] - 组件标识（默认 'collab'）
     */
    $trackClick (coreApiKey, componentKey = 'collab') {
      if (!coreApiKey || !String(coreApiKey).trim()) return
      try {
        this.$http.post('/admin/ops/events/component-click', {
          componentKey,
          coreApiKey: String(coreApiKey).trim()
        }).catch(() => {})
      } catch (_) {
        // 静默忽略
      }
    }
  }
}
