/**
 * 客户端图片压缩（Canvas），在上传前降低体积与带宽。
 *
 * 约定（常见 Web 实践）：
 * - 长边限制：附件类 1920px（与多数站点「全宽图」上限接近）；头像类 512px。
 * - 有损格式：JPEG quality ≈ 0.82（约等于质量 82%，体积与观感折中）。
 * - 含透明通道：输出 PNG（仅缩放，仍为无损），避免 JPEG 吞掉 alpha。
 * - GIF：保留动图，不压缩；SVG/HEIC 等：不处理，原样上传。
 */

/** @typedef {{ maxWidth: number, maxHeight: number, quality: number }} ImageCompressOptions */

export const IMAGE_COMPRESS_PRESETS = {
  /** 头像、小缩略图 */
  avatar: { maxWidth: 512, maxHeight: 512, quality: 0.82 },
  /** 表格附件、协作图片等 */
  attachment: { maxWidth: 1920, maxHeight: 1920, quality: 0.82 }
}

/**
 * 是否应对该文件做栅格压缩（GIF/SVG/HEIC 等排除）。
 */
export function shouldCompressAsRasterImage (file) {
  if (!file || !file.type || !file.type.startsWith('image/')) return false
  const t = file.type.toLowerCase()
  if (t === 'image/gif') return false
  if (t === 'image/svg+xml') return false
  if (t === 'image/heic' || t === 'image/heif') return false
  return true
}

/**
 * @param {File|Blob} file
 * @param {ImageCompressOptions} [options]
 * @returns {Promise<File>}
 */
export async function compressImageFile (file, options = {}) {
  const maxWidth = options.maxWidth ?? IMAGE_COMPRESS_PRESETS.attachment.maxWidth
  const maxHeight = options.maxHeight ?? IMAGE_COMPRESS_PRESETS.attachment.maxHeight
  const quality = options.quality ?? IMAGE_COMPRESS_PRESETS.attachment.quality

  if (!shouldCompressAsRasterImage(file)) {
    return file instanceof File ? file : new File([file], 'image', { type: file.type })
  }

  let source = null
  try {
    source = await decodeToDrawable(file)
    const sw = source.width
    const sh = source.height
    const { width: tw, height: th } = fitInside(sw, sh, maxWidth, maxHeight)

    const canvas = document.createElement('canvas')
    canvas.width = tw
    canvas.height = th
    const ctx = canvas.getContext('2d')
    if (!ctx) {
      return file instanceof File ? file : new File([file], 'image', { type: file.type })
    }
    ctx.imageSmoothingEnabled = true
    ctx.imageSmoothingQuality = 'high'
    ctx.drawImage(source, 0, 0, tw, th)

    const transparent = imageHasTransparency(ctx, tw, th)
    const outMime = transparent ? 'image/png' : 'image/jpeg'
    const outBlob = outMime === 'image/png'
      ? await canvasToBlob(canvas, 'image/png')
      : await canvasToBlob(canvas, 'image/jpeg', quality)

    const originalName = file instanceof File ? file.name : 'image'
    const outName = buildOutputFilename(originalName, outMime)

    if (outBlob.size >= file.size && tw === sw && th === sh) {
      return file instanceof File ? file : new File([file], originalName, { type: file.type })
    }

    return new File([outBlob], outName, {
      type: outMime,
      lastModified: Date.now()
    })
  } catch (err) {
    console.warn('[imageCompress] skipped:', err)
    return file instanceof File ? file : new File([file], 'image', { type: file.type })
  } finally {
    if (source && typeof source.close === 'function') {
      try {
        source.close()
      } catch (_e) {
        /* ignore */
      }
    }
  }
}

function fitInside (sw, sh, maxW, maxH) {
  if (sw <= maxW && sh <= maxH) {
    return { width: sw, height: sh }
  }
  const r = Math.min(maxW / sw, maxH / sh)
  return {
    width: Math.max(1, Math.round(sw * r)),
    height: Math.max(1, Math.round(sh * r))
  }
}

/**
 * @param {File|Blob} file
 * @returns {Promise<HTMLImageElement|ImageBitmap>}
 */
async function decodeToDrawable (file) {
  if (typeof createImageBitmap === 'function') {
    try {
      return await createImageBitmap(file, { imageOrientation: 'from-image' })
    } catch (_e) {
      /* 部分环境不支持 orientation，退回 Image */
    }
  }
  return loadImageElement(file)
}

function loadImageElement (file) {
  return new Promise((resolve, reject) => {
    const url = URL.createObjectURL(file)
    const img = new Image()
    img.onload = () => {
      URL.revokeObjectURL(url)
      resolve(img)
    }
    img.onerror = () => {
      URL.revokeObjectURL(url)
      reject(new Error('load image failed'))
    }
    img.src = url
  })
}

function imageHasTransparency (ctx, w, h) {
  try {
    const { data } = ctx.getImageData(0, 0, w, h)
    const step = Math.max(4, Math.floor((data.length / 4) / 6000) * 4)
    for (let i = 3; i < data.length; i += step) {
      if (data[i] < 255) return true
    }
  } catch (_e) {
    /* 跨域等 */
  }
  return false
}

function canvasToBlob (canvas, type, quality) {
  return new Promise((resolve, reject) => {
    canvas.toBlob(
      (blob) => {
        if (blob) resolve(blob)
        else reject(new Error('canvas.toBlob failed'))
      },
      type,
      quality
    )
  })
}

function buildOutputFilename (originalName, mime) {
  const base = String(originalName || 'image').replace(/\.[^.]+$/, '')
  const ext = mime === 'image/png' ? '.png' : '.jpg'
  return base + ext
}
