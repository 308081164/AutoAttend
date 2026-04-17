/**
 * 侧栏 / 嵌入壳内 router-view 的 keep-alive 排除列表（组件 name，与各 *.vue 的 name 一致）。
 * 调试页、单次分析页等不缓存，避免占满 max 或保留过时状态。
 */
export const SHELL_KEEP_ALIVE_EXCLUDE = ['TestView', 'CommitAnalysisView']
