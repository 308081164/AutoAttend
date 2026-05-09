/**
 * 协作多维表附件缩略图 blob URL 缓存（跨表格刷新复用，减少重复拉取与解码）。
 * 仅缓存「附件 id -> object URL」；组件销毁时应 clear。
 */
const MAX_ENTRIES = 400
const cache = new Map()

function cacheKey (id) {
  return Number(id)
}

export function getCollabAttachmentPreviewUrl (id) {
  const k = cacheKey(id)
  if (!cache.has(k)) return null
  const v = cache.get(k)
  cache.delete(k)
  cache.set(k, v)
  return v
}

export function rememberCollabAttachmentPreview (id, blobUrl) {
  const k = cacheKey(id)
  if (cache.has(k)) {
    const old = cache.get(k)
    if (old !== blobUrl) {
      try { URL.revokeObjectURL(old) } catch (_e) { /* ignore */ }
    }
    cache.delete(k)
  }
  while (cache.size >= MAX_ENTRIES) {
    const oldest = cache.keys().next().value
    const u = cache.get(oldest)
    try { URL.revokeObjectURL(u) } catch (_e) { /* ignore */ }
    cache.delete(oldest)
  }
  cache.set(k, blobUrl)
}

export function invalidateCollabAttachmentPreview (id) {
  const k = cacheKey(id)
  if (!cache.has(k)) return
  const u = cache.get(k)
  try { URL.revokeObjectURL(u) } catch (_e) { /* ignore */ }
  cache.delete(k)
}

export function clearCollabAttachmentPreviewCache () {
  for (const u of cache.values()) {
    try { URL.revokeObjectURL(u) } catch (_e) { /* ignore */ }
  }
  cache.clear()
}
