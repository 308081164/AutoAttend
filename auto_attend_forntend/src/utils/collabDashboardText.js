/**
 * 多维表仪表盘：从「问题/标题」等文本列抽取词频，供词云展示。
 * 使用 Intl.Segmenter（中文分词）+ 常见停用词过滤；无 Segmenter 时按标点粗分。
 */

const ZH_STOP = new Set([
  '的', '了', '和', '是', '在', '也', '有', '就', '不', '与', '及', '或', '等', '为', '以', '对', '中', '上', '到', '将', '从', '但', '而', '被', '把', '让', '向', '于', '由', '可', '能', '会', '要', '可以', '这个', '一个', '进行', '通过', '如果', '因为', '所以', '没有', '已经', '需要', '问题', '无法', '出现', '目前', '相关', '是否', '时候', '之后', '以及', '还是', '或者', '这样', '什么', '怎么', '如何', '用户', '系统', '数据', '功能', '页面', '接口', '服务', '项目', '任务', '时间', '版本', '开发', '测试', '使用', '处理', '完成', '提交', '反馈', '确认', '修改', '添加', '删除', '查看', '显示', '设置', '配置', '错误', '失败', '成功', '正常', '异常', '无法', '不能', '应该', '必须', '可能', '建议', '希望', '谢谢', '您好'
])

const EN_STOP = new Set([
  'the', 'a', 'an', 'and', 'or', 'to', 'of', 'in', 'on', 'for', 'is', 'are', 'was', 'were', 'be', 'been', 'it', 'this', 'that', 'with', 'as', 'at', 'by', 'from', 'not', 'but', 'if', 'when', 'we', 'you', 'they', 'he', 'she', 'i', 'has', 'have', 'had', 'do', 'does', 'did', 'can', 'could', 'will', 'would', 'should', 'may', 'might', 'the', 'our', 'your', 'their', 'its'
])

function isStopWord (w) {
  const s = String(w || '').trim().toLowerCase()
  if (s.length < 2) return true
  if (ZH_STOP.has(s)) return true
  if (/^[a-z]{2,}$/i.test(s) && EN_STOP.has(s)) return true
  return false
}

/**
 * @param {string} text
 * @returns {string[]}
 */
export function tokenizeForWordCloud (text) {
  if (!text || typeof text !== 'string') return []
  const raw = text.replace(/\s+/g, ' ').trim()
  if (!raw) return []

  if (typeof Intl !== 'undefined' && typeof Intl.Segmenter === 'function') {
    try {
      const seg = new Intl.Segmenter('zh', { granularity: 'word' })
      const out = []
      for (const { segment, isWordLike } of seg.segment(raw)) {
        const t = String(segment || '').trim()
        if (!t || !isWordLike) continue
        if (isStopWord(t)) continue
        if (t.length === 1 && /[\u4e00-\u9fff]/.test(t)) continue
        out.push(t)
      }
      if (out.length) return out
    } catch (_e) {
      /* 部分环境无 zh Segmenter */
    }
  }

  const fallback = []
  raw.split(/[\s\n\r\t，。！？、；：""''（）【】《》<>[\]{}|\\/]+/).forEach((p) => {
    const t = p.trim()
    if (t.length >= 2 && !isStopWord(t)) fallback.push(t)
  })
  return fallback
}

function collectTextColumnIds (columns) {
  const ids = []
  const hints = ['问题', '标题', '描述', '备注', '详情', '内容']
  const list = Array.isArray(columns) ? columns : []
  list.forEach((c) => {
    const name = String(c.name || '').trim()
    const ct = String(c.columnType || '').toLowerCase()
    if (ct === 'attachment' || ct === 'multi_user') return
    if (hints.some((h) => name.includes(h))) {
      ids.push(c.id)
    }
  })
  if (ids.length) return ids
  list.forEach((c) => {
    const ct = String(c.columnType || '').toLowerCase()
    if (ct === 'text') ids.push(c.id)
  })
  return ids
}

/**
 * @param {object[]} records
 * @param {object[]} columns
 * @param {number} [maxWords]
 * @returns {{ text: string, weight: number, max: number }[]}
 */
export function buildWordCloudItems (records, columns, maxWords = 48) {
  const colIds = collectTextColumnIds(columns)
  if (!colIds.length) return []

  const chunks = []
  const rows = Array.isArray(records) ? records : []
  rows.forEach((row) => {
    colIds.forEach((cid) => {
      const v = row['c' + cid]
      if (v == null) return
      const s = String(v).trim()
      if (s) chunks.push(s)
    })
  })
  const blob = chunks.join('\n')
  if (!blob.trim()) return []

  const freq = new Map()
  tokenizeForWordCloud(blob).forEach((tok) => {
    freq.set(tok, (freq.get(tok) || 0) + 1)
  })

  const sorted = [...freq.entries()]
    .filter(([w]) => w && w.length >= 2)
    .sort((a, b) => b[1] - a[1])
    .slice(0, maxWords)

  const max = sorted.length ? sorted[0][1] : 1
  return sorted.map(([text, weight]) => ({ text, weight, max }))
}
