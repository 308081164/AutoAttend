<template>
  <div class="agent-chat-page">
    <!-- 顶部导航栏 -->
    <header class="chat-header">
      <div class="header-inner">
        <div class="header-left">
          <img class="logo-icon" src="/brand-logo.svg" width="28" height="28" alt="" aria-hidden="true">
          <span class="header-title">流帮 Project</span>
          <span v-if="tenantName" class="header-tenant">{{ tenantName }}</span>
        </div>
        <button
          v-if="session"
          class="btn-finish-header"
          @click="requestFinish"
          :disabled="sending || messages.length === 0"
        >
          提交需求
        </button>
      </div>
    </header>

    <!-- 项目标题区 -->
    <div class="project-banner" v-if="projectName">
      <div class="banner-inner">
        <h2 class="project-name">{{ projectName }}</h2>
        <span class="project-tag">需求收集</span>
      </div>
    </div>

    <!-- 错误提示 -->
    <div class="error-banner" v-if="error">
      <span class="error-text">{{ error }}</span>
      <button class="error-close" @click="error = ''">&times;</button>
    </div>

    <!-- 加载状态 -->
    <div class="loading-container" v-if="loading && messages.length === 0">
      <div class="loading-spinner"></div>
      <p class="loading-text">正在加载会话...</p>
    </div>

    <!-- 聊天消息区域 -->
    <div class="chat-body" ref="chatBody" v-show="!loading || messages.length > 0">
      <div
        v-for="(msg, index) in messages"
        :key="msg.id || index"
        :class="['message-row', msg.role === 'user' ? 'message-row--user' : 'message-row--assistant']"
      >
        <!-- Assistant 头像 -->
        <div class="avatar avatar--assistant" v-if="msg.role === 'assistant'">
          <svg viewBox="0 0 24 24" width="20" height="20" fill="none">
            <rect width="24" height="24" rx="6" fill="#3370ff" />
            <path d="M7 8h10M7 12h7M7 16h10" stroke="#fff" stroke-width="1.8" stroke-linecap="round" />
          </svg>
        </div>

        <!-- 消息气泡 -->
        <div :class="['message-bubble', msg.role === 'user' ? 'bubble--user' : 'bubble--assistant']">
          <!-- 文本内容 -->
          <div class="bubble-text" v-if="msg.content" v-html="renderContent(msg.content)"></div>

          <!-- 图片消息 -->
          <div class="bubble-image" v-if="msg.attachments && msg.attachments.length">
            <img
              v-for="att in msg.attachments.filter(a => a.type && a.type.startsWith('image/'))"
              :key="att.id"
              :src="att.url || att.thumbnailUrl"
              :alt="att.fileName"
              class="chat-image-thumb"
              @click="openPreview(att.url || att.thumbnailUrl)"
            />
          </div>

          <!-- 文件附件 -->
          <div class="bubble-files" v-if="msg.attachments && msg.attachments.filter(a => !a.type || !a.type.startsWith('image/')).length">
            <div
              v-for="att in msg.attachments.filter(a => !a.type || !a.type.startsWith('image/'))"
              :key="att.id"
              class="file-item"
            >
              <svg class="file-icon" viewBox="0 0 24 24" width="18" height="18" fill="none">
                <path d="M14 2H6a2 2 0 00-2 2v16a2 2 0 002 2h12a2 2 0 002-2V8l-6-6z" stroke="#646a73" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/>
                <path d="M14 2v6h6M16 13H8M16 17H8M10 9H8" stroke="#646a73" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/>
              </svg>
              <span class="file-name">{{ att.fileName }}</span>
              <span class="file-size" v-if="att.fileSize">{{ formatFileSize(att.fileSize) }}</span>
            </div>
          </div>

          <!-- 时间戳 -->
          <div class="bubble-time">{{ formatTime(msg.createdAt) }}</div>
        </div>

        <!-- User 头像 -->
        <div class="avatar avatar--user" v-if="msg.role === 'user'">
          <svg viewBox="0 0 24 24" width="20" height="20" fill="none">
            <circle cx="12" cy="8" r="4" stroke="#fff" stroke-width="1.5"/>
            <path d="M4 20c0-3.3 3.6-6 8-6s8 2.7 8 6" stroke="#fff" stroke-width="1.5" stroke-linecap="round"/>
          </svg>
        </div>
      </div>

      <!-- 正在思考动画 -->
      <div class="message-row message-row--assistant" v-if="sending">
        <div class="avatar avatar--assistant">
          <svg viewBox="0 0 24 24" width="20" height="20" fill="none">
            <rect width="24" height="24" rx="6" fill="#3370ff" />
            <path d="M7 8h10M7 12h7M7 16h10" stroke="#fff" stroke-width="1.8" stroke-linecap="round" />
          </svg>
        </div>
        <div class="message-bubble bubble--assistant bubble--thinking">
          <span class="dot"></span>
          <span class="dot"></span>
          <span class="dot"></span>
        </div>
      </div>
    </div>

    <!-- 底部输入区 -->
    <div class="chat-footer" v-if="session">
      <!-- 已上传附件预览条 -->
      <div class="attachment-bar" v-if="attachments.length">
        <div class="attachment-bar-inner">
          <div class="attachment-chip" v-for="(att, idx) in attachments" :key="att.tempId || idx">
            <img v-if="att.thumbnailUrl" :src="att.thumbnailUrl" class="att-thumb" />
            <svg v-else class="file-icon-sm" viewBox="0 0 24 24" width="16" height="16" fill="none">
              <path d="M14 2H6a2 2 0 00-2 2v16a2 2 0 002 2h12a2 2 0 002-2V8l-6-6z" stroke="#646a73" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/>
              <path d="M14 2v6h6M16 13H8M16 17H8M10 9H8" stroke="#646a73" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
            <span class="att-name">{{ att.fileName }}</span>
            <span class="att-size" v-if="att.fileSize">{{ formatFileSize(att.fileSize) }}</span>
            <button class="att-remove" @click="removeAttachment(idx)">&times;</button>
          </div>
        </div>
      </div>

      <!-- 输入行 -->
      <div class="input-row">
        <button class="btn-attach" @click="$refs.fileInput.click()" :disabled="uploading" title="上传附件">
          <svg viewBox="0 0 24 24" width="22" height="22" fill="none">
            <path d="M12 16V8M12 8l-3 3M12 8l3 3" stroke="#646a73" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"/>
            <path d="M3 16.5V18a2 2 0 002 2h14a2 2 0 002-2v-1.5" stroke="#646a73" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"/>
          </svg>
        </button>
        <input
          ref="fileInput"
          type="file"
          class="hidden-input"
          :accept="'image/*,.pdf,.doc,.docx,.txt,.md'"
          multiple
          @change="onFileSelect"
        />
        <textarea
          ref="messageInput"
          v-model="newMessage"
          class="message-input"
          placeholder="请描述您的需求..."
          rows="1"
          @keydown.enter.exact="handleEnterKey"
          @input="autoResize"
          @paste="onPaste"
          :disabled="sending"
        ></textarea>
        <button
          class="btn-send"
          :class="{ 'btn-send--active': newMessage.trim() || selectedAttachmentIds.length }"
          @click="sendMessage"
          :disabled="sending || (!newMessage.trim() && !selectedAttachmentIds.length)"
          title="发送"
        >
          <svg viewBox="0 0 24 24" width="20" height="20" fill="none">
            <path d="M22 2L11 13" stroke="#fff" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"/>
            <path d="M22 2L15 22l-4-9-9-4L22 2z" stroke="#fff" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"/>
          </svg>
        </button>
      </div>
    </div>

    <!-- 图片全屏预览 -->
    <div class="preview-overlay" v-if="previewImageUrl" @click.self="previewImageUrl = ''">
      <button class="preview-close" @click="previewImageUrl = ''">&times;</button>
      <img :src="previewImageUrl" class="preview-image" />
    </div>

    <!-- 二次确认弹窗 -->
    <div class="modal-overlay" v-if="showFinishModal" @click.self="showFinishModal = false">
      <div class="modal-card">
        <div class="modal-header">
          <h3 class="modal-title">确认提交需求</h3>
          <button class="modal-close" @click="showFinishModal = false">&times;</button>
        </div>
        <div class="modal-body">
          <p class="modal-desc">以下是 Agent 为您整理的需求摘要，请确认后提交：</p>
          <div class="summary-preview" v-if="finishPreview">
            <div class="summary-content" v-html="renderContent(finishPreview.summary || finishPreview)"></div>
          </div>
        </div>
        <div class="modal-footer">
          <button class="btn-modal btn-modal--secondary" @click="showFinishModal = false">
            继续描述
          </button>
          <button class="btn-modal btn-modal--primary" @click="confirmFinish" :disabled="sending">
            确认提交
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { compressImageFile, IMAGE_COMPRESS_PRESETS } from '@/utils/imageCompress'

export default {
  name: 'AgentChatView',

  data() {
    return {
      token: '',
      session: null,
      messages: [],
      newMessage: '',
      attachments: [],
      selectedAttachmentIds: [],
      loading: false,
      sending: false,
      uploading: false,
      finishPreview: null,
      showFinishModal: false,
      previewImageUrl: '',
      tenantName: '',
      projectName: '',
      error: ''
    }
  },

  created() {
    this.token = this.$route.params.publicToken
    this.fetchSession()
  },

  mounted() {
    document.addEventListener('paste', this.globalPaste)
  },

  beforeDestroy() {
    document.removeEventListener('paste', this.globalPaste)
  },

  methods: {
    /**
     * 获取会话信息和历史消息
     */
    async fetchSession() {
      this.loading = true
      this.error = ''
      try {
        const resp = await this.$http.get(`/public/agent/sessions/${encodeURIComponent(this.token)}`)
        if (resp.data && resp.data.code === 0 && resp.data.data) {
          const data = resp.data.data
          this.session = data.session || data
          this.messages = data.messages || []
          this.tenantName = data.tenantName || (this.session && this.session.tenantName) || ''
          this.projectName = data.projectName || (this.session && this.session.projectName) || ''
        }
        this.$nextTick(() => this.scrollToBottom())
      } catch (err) {
        const msg = (err.response && err.response.data && err.response.data.message) || err.message || '加载会话失败'
        this.error = msg
      } finally {
        this.loading = false
      }
    },

    /**
     * 处理回车键：移动端回车换行，PC 端回车发送
     */
    handleEnterKey(e) {
      // 移动端回车换行，PC 端回车发送
      if (window.innerWidth <= 768) return
      e.preventDefault()
      this.sendMessage()
    },

    /**
     * 发送消息
     */
    async sendMessage() {
      const content = this.newMessage.trim()
      if ((!content && !this.selectedAttachmentIds.length) || this.sending) return

      this.sending = true
      this.error = ''

      // 乐观添加用户消息到列表
      const optimisticMsg = {
        id: 'temp-' + Date.now(),
        role: 'user',
        content,
        attachments: this.attachments.filter(a => this.selectedAttachmentIds.includes(a.id)).map(a => ({
          id: a.id,
          fileName: a.fileName,
          fileSize: a.fileSize,
          type: a.type,
          url: a.url,
          thumbnailUrl: a.thumbnailUrl
        })),
        createdAt: new Date().toISOString()
      }
      this.messages.push(optimisticMsg)
      this.newMessage = ''
      this.attachments = []
      this.selectedAttachmentIds = []
      this.$nextTick(() => this.scrollToBottom())

      // 重置输入框高度
      if (this.$refs.messageInput) {
        this.$refs.messageInput.style.height = 'auto'
      }

      try {
        const payload = { content }
        if (optimisticMsg.attachments.length) {
          payload.attachmentIds = optimisticMsg.attachments.map(a => a.id)
        }
        const resp = await this.$http.post(
          `/public/agent/sessions/${encodeURIComponent(this.token)}/messages`,
          payload
        )
        if (resp.data && resp.data.code === 0 && resp.data.data) {
          const data = resp.data.data

          // 替换乐观消息为服务端消息
          const idx = this.messages.findIndex(m => m.id === optimisticMsg.id)
          if (idx !== -1) {
            this.messages.splice(idx, 1, data.userMessage || optimisticMsg)
          }

          // 添加 assistant 回复
          if (data.assistantMessage) {
            this.messages.push(data.assistantMessage)
          } else if (data.messages) {
            // 如果返回的是完整消息列表，则更新
            this.messages = data.messages
            this.$nextTick(() => this.scrollToBottom())
          }
        }

        this.$nextTick(() => this.scrollToBottom())
      } catch (err) {
        const msg = (err.response && err.response.data && err.response.data.message) || err.message || '发送失败'
        this.error = msg
        // 移除乐观消息
        const idx = this.messages.findIndex(m => m.id === optimisticMsg.id)
        if (idx !== -1) {
          this.messages.splice(idx, 1)
        }
      } finally {
        this.sending = false
        this.$nextTick(() => this.scrollToBottom())
      }
    },

    /**
     * 上传附件（图片/视频上传前自动压缩）
     */
    async uploadFile(file) {
      if (this.uploading) return
      this.uploading = true
      this.error = ''

      try {
        // 图片压缩：使用已有的 Canvas 压缩工具
        if (file.type.startsWith('image/')) {
          file = await compressImageFile(file, IMAGE_COMPRESS_PRESETS.attachment)
        }
        // 视频压缩：使用浏览器原生 MediaRecorder API（仅支持 MP4/WebM）
        else if (file.type.startsWith('video/')) {
          file = await this.compressVideoFile(file)
        }

        const formData = new FormData()
        formData.append('file', file)

        const resp = await this.$http.post(
          `/public/agent/sessions/${encodeURIComponent(this.token)}/attachments`,
          formData,
          { headers: { 'Content-Type': 'multipart/form-data' } }
        )
        if (resp.data && resp.data.code === 0 && resp.data.data) {
          const data = resp.data.data
          this.attachments.push({
            id: data.id || data.attachmentId,
            fileName: data.fileName || file.name,
            fileSize: data.fileSize || file.size,
            type: data.type || file.type,
            url: data.url,
            thumbnailUrl: data.thumbnailUrl,
            tempId: 'att-' + Date.now()
          })
          this.selectedAttachmentIds.push(data.id || data.attachmentId)
        }
      } catch (err) {
        const msg = (err.response && err.response.data && err.response.data.message) || err.message || '上传失败'
        this.error = msg
      } finally {
        this.uploading = false
      }
    },

    /**
     * 视频压缩：使用浏览器原生 MediaRecorder API
     * 通过 Canvas 捕获视频帧并重新录制为 WebM（VP8/VP9），降低分辨率和码率
     * 如果浏览器不支持 MediaRecorder，则原样返回
     */
    async compressVideoFile(file) {
      // 小于 10MB 的视频不压缩
      if (file.size < 10 * 1024 * 1024) return file

      try {
        if (typeof MediaRecorder === 'undefined') return file

        const video = document.createElement('video')
        video.muted = true
        video.playsInline = true
        video.preload = 'auto'
        video.src = URL.createObjectURL(file)

        await new Promise((resolve, reject) => {
          video.onloadedmetadata = resolve
          video.onerror = reject
        })

        // 限制最大分辨率：720p
        const maxW = 1280
        const maxH = 720
        let w = video.videoWidth
        let h = video.videoHeight
        if (w > maxW || h > maxH) {
          const scale = Math.min(maxW / w, maxH / h)
          w = Math.round(w * scale)
          h = Math.round(h * scale)
        }

        const canvas = document.createElement('canvas')
        canvas.width = w
        canvas.height = h
        const ctx = canvas.getContext('2d')

        // 使用 MediaRecorder 录制
        const stream = canvas.captureStream(30) // 30fps
        const mimeType = MediaRecorder.isTypeSupported('video/webm;codecs=vp9')
          ? 'video/webm;codecs=vp9'
          : MediaRecorder.isTypeSupported('video/webm;codecs=vp8')
            ? 'video/webm;codecs=vp8'
            : 'video/webm'

        const recorder = new MediaRecorder(stream, {
          mimeType,
          videoBitsPerSecond: 1500000 // 1.5 Mbps
        })
        const chunks = []
        recorder.ondataavailable = e => { if (e.data.size > 0) chunks.push(e.data) }

        const compressed = await new Promise((resolve) => {
          recorder.onstop = () => {
            const blob = new Blob(chunks, { type: mimeType })
            // 如果压缩后反而更大，返回原文件
            resolve(blob.size < file.size ? new File([blob], file.name.replace(/\.[^.]+$/, '.webm'), { type: mimeType }) : file)
          }
          recorder.onerror = () => resolve(file) // 压缩失败返回原文件
          recorder.start()

          video.currentTime = 0
          video.play()

          const drawFrame = () => {
            if (video.paused || video.ended) {
              recorder.stop()
              video.pause()
              URL.revokeObjectURL(video.src)
              return
            }
            ctx.drawImage(video, 0, 0, w, h)
            requestAnimationFrame(drawFrame)
          }
          drawFrame()

          // 安全超时：60秒后强制停止
          setTimeout(() => {
            if (recorder.state === 'recording') {
              recorder.stop()
              video.pause()
              URL.revokeObjectURL(video.src)
            }
          }, 60000)
        })

        return compressed
      } catch (e) {
        console.warn('Video compression failed, uploading original:', e)
        return file
      }
    },

    /**
     * 文件选择回调
     */
    onFileSelect(e) {
      const files = e.target.files
      if (!files || !files.length) return
      for (let i = 0; i < files.length; i++) {
        this.uploadFile(files[i])
      }
      // 重置 input 以允许重复选择同一文件
      e.target.value = ''
    },

    /**
     * 处理粘贴事件（支持 Ctrl+V 粘贴图片）
     */
    onPaste(e) {
      const items = e.clipboardData && e.clipboardData.items
      if (!items) return

      for (let i = 0; i < items.length; i++) {
        const item = items[i]
        if (item.type && item.type.startsWith('image/')) {
          e.preventDefault()
          const file = item.getAsFile()
          if (file) {
            this.uploadFile(file)
          }
          return
        }
      }
    },

    /**
     * 全局粘贴监听（确保在任意焦点下都能粘贴图片）
     */
    globalPaste(e) {
      // 仅处理图片粘贴
      const items = e.clipboardData && e.clipboardData.items
      if (!items) return
      let hasImage = false
      for (let i = 0; i < items.length; i++) {
        if (items[i].type && items[i].type.startsWith('image/')) {
          hasImage = true
          break
        }
      }
      if (hasImage) {
        this.onPaste(e)
      }
    },

    /**
     * 移除已上传附件
     */
    removeAttachment(idx) {
      const att = this.attachments[idx]
      if (att) {
        const idIdx = this.selectedAttachmentIds.indexOf(att.id)
        if (idIdx !== -1) {
          this.selectedAttachmentIds.splice(idIdx, 1)
        }
        this.attachments.splice(idx, 1)
      }
    },

    /**
     * 请求结束（获取需求摘要预览）
     */
    async requestFinish() {
      if (this.sending || this.messages.length === 0) return
      this.sending = true
      this.error = ''

      try {
        const resp = await this.$http.post(
          `/public/agent/sessions/${encodeURIComponent(this.token)}/finish`,
          { confirmed: false }
        )
        if (resp.data && resp.data.code === 0 && resp.data.data) {
          const data = resp.data.data
          this.finishPreview = data.summary || data
          this.showFinishModal = true
        }
      } catch (err) {
        const status = err.response && err.response.status
        const serverMsg = (err.response && err.response.data && err.response.data.message) || ''
        if (status === 500) {
          this.error = '服务器内部错误，请稍后重试。如果问题持续存在，请联系管理员。'
        } else {
          this.error = serverMsg || err.message || '获取摘要失败'
        }
      } finally {
        this.sending = false
      }
    },

    /**
     * 确认提交需求
     */
    async confirmFinish() {
      this.sending = true
      this.error = ''

      try {
        const resp = await this.$http.post(
          `/public/agent/sessions/${encodeURIComponent(this.token)}/finish`,
          { confirmed: true }
        )
        if (resp.data && resp.data.code === 0) {
          this.showFinishModal = false
          this.finishPreview = null

          // 添加系统消息
          this.messages.push({
            id: 'sys-' + Date.now(),
            role: 'assistant',
            content: '您的需求已成功提交，我们会尽快为您处理。感谢您的配合！',
            createdAt: new Date().toISOString()
          })
          this.$nextTick(() => this.scrollToBottom())
        }
      } catch (err) {
        const status = err.response && err.response.status
        const serverMsg = (err.response && err.response.data && err.response.data.message) || ''
        if (status === 500) {
          this.error = '服务器内部错误，请稍后重试。如果问题持续存在，请联系管理员。'
        } else {
          this.error = serverMsg || err.message || '提交失败'
        }
      } finally {
        this.sending = false
      }
    },

    /**
     * 滚动聊天区域到底部
     */
    scrollToBottom() {
      const el = this.$refs.chatBody
      if (el) {
        el.scrollTop = el.scrollHeight
      }
    },

    /**
     * 自动调整输入框高度
     */
    autoResize() {
      const el = this.$refs.messageInput
      if (!el) return
      el.style.height = 'auto'
      el.style.height = Math.min(el.scrollHeight, 120) + 'px'
    },

    /**
     * 打开图片全屏预览
     */
    openPreview(url) {
      this.previewImageUrl = url
    },

    /**
     * 格式化时间显示
     */
    formatTime(dateStr) {
      if (!dateStr) return ''
      const date = new Date(dateStr)
      const now = new Date()
      const isToday = date.toDateString() === now.toDateString()

      const pad = (n) => String(n).padStart(2, '0')
      const time = pad(date.getHours()) + ':' + pad(date.getMinutes())

      if (isToday) return time

      const yesterday = new Date(now)
      yesterday.setDate(yesterday.getDate() - 1)
      if (date.toDateString() === yesterday.toDateString()) return '昨天 ' + time

      const month = pad(date.getMonth() + 1)
      const day = pad(date.getDate())
      return month + '-' + day + ' ' + time
    },

    /**
     * 格式化文件大小
     */
    formatFileSize(bytes) {
      if (!bytes) return ''
      if (bytes < 1024) return bytes + ' B'
      if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
      return (bytes / (1024 * 1024)).toFixed(1) + ' MB'
    },

    /**
     * 渲染消息内容（支持基础 Markdown 语法）
     */
    renderContent(content) {
      if (!content) return ''
      // 转义 HTML
      let text = content
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')

      // 围栏代码块 (``` ... ```) —— 必须在行内 code 之前处理
      text = text.replace(/```([\s\S]*?)```/g, function (match, code) {
        return '<pre><code>' + code.trim() + '</code></pre>'
      })

      // 标题 h3-h6
      text = text.replace(/^#{3,6}\s+(.+)$/gm, function (match, heading) {
        const level = match.trimStart().indexOf(' ')
        return '<h' + level + '>' + heading.trim() + '</h' + level + '>'
      })

      // 粗体 **bold**
      text = text.replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>')

      // 斜体 *italic*
      text = text.replace(/\*(.+?)\*/g, '<em>$1</em>')

      // 行内代码 `code`
      text = text.replace(/`([^`]+)`/g, '<code>$1</code>')

      // 无序列表（连续的 - 开头行）
      text = text.replace(/((?:^- .+\n?)+)/gm, function (match) {
        const items = match.trim().split('\n').map(function (line) {
          return '<li>' + line.replace(/^- /, '') + '</li>'
        }).join('')
        return '<ul>' + items + '</ul>'
      })

      // 有序列表（连续的 数字. 开头行）
      text = text.replace(/((?:^\d+\.\s.+\n?)+)/gm, function (match) {
        const items = match.trim().split('\n').map(function (line) {
          return '<li>' + line.replace(/^\d+\.\s/, '') + '</li>'
        }).join('')
        return '<ol>' + items + '</ol>'
      })

      // 换行转 <br>（排除已在块级元素内的换行）
      text = text.replace(/\n/g, '<br>')

      return text
    }
  }
}
</script>

<style scoped>
/* ========== Design Tokens ========== */
.agent-chat-page {
  --lb-blue: #3370ff;
  --lb-green: #00b42a;
  --lb-bg: #f5f6f7;
  --lb-bg-card: #ffffff;
  --lb-border: #e5e6eb;
  --lb-text-1: #1f2329;
  --lb-text-2: #646a73;
  --lb-text-3: #8f959e;
  --lb-radius: 8px;
  --lb-radius-lg: 12px;
}

/* ========== Page Layout ========== */
.agent-chat-page {
  display: flex;
  flex-direction: column;
  height: 100vh;
  height: 100dvh;
  background: var(--lb-bg);
  font-family: -apple-system, BlinkMacSystemFont, 'PingFang SC', 'Hiragino Sans GB',
    'Microsoft YaHei', 'Helvetica Neue', Arial, sans-serif;
  color: var(--lb-text-1);
  -webkit-font-smoothing: antialiased;
  overflow: hidden;
}

/* ========== Header ========== */
.chat-header {
  position: sticky;
  top: 0;
  z-index: 100;
  background: var(--lb-bg-card);
  border-bottom: 1px solid var(--lb-border);
  flex-shrink: 0;
}

.header-inner {
  max-width: 800px;
  margin: 0 auto;
  padding: 12px 20px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 10px;
}

.logo-icon {
  flex-shrink: 0;
  display: block;
  border-radius: 7px;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.08);
}

.header-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--lb-text-1);
}

.header-tenant {
  font-size: 13px;
  color: var(--lb-text-3);
  padding: 2px 8px;
  background: var(--lb-bg);
  border-radius: 4px;
}

.btn-finish-header {
  padding: 5px 14px;
  background: var(--lb-blue);
  color: #ffffff;
  border: none;
  border-radius: 6px;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
  font-family: inherit;
  white-space: nowrap;
  flex-shrink: 0;
}

.btn-finish-header:hover:not(:disabled) {
  background: #2860e1;
}

.btn-finish-header:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

/* ========== Project Banner ========== */
.project-banner {
  flex-shrink: 0;
  background: var(--lb-bg-card);
  border-bottom: 1px solid var(--lb-border);
}

.banner-inner {
  max-width: 800px;
  margin: 0 auto;
  padding: 14px 20px;
  display: flex;
  align-items: center;
  gap: 10px;
}

.project-name {
  font-size: 15px;
  font-weight: 500;
  color: var(--lb-text-1);
  margin: 0;
}

.project-tag {
  font-size: 12px;
  color: var(--lb-blue);
  background: rgba(51, 112, 255, 0.08);
  padding: 2px 8px;
  border-radius: 4px;
  font-weight: 500;
  flex-shrink: 0;
}

/* ========== Error Banner ========== */
.error-banner {
  flex-shrink: 0;
  background: #fff2f0;
  border-bottom: 1px solid #ffccc7;
  padding: 8px 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  max-width: 800px;
  margin: 0 auto;
  width: 100%;
  box-sizing: border-box;
}

.error-text {
  font-size: 13px;
  color: #cb2634;
}

.error-close {
  background: none;
  border: none;
  font-size: 18px;
  color: #cb2634;
  cursor: pointer;
  padding: 0 4px;
  line-height: 1;
}

/* ========== Loading ========== */
.loading-container {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 16px;
}

.loading-spinner {
  width: 32px;
  height: 32px;
  border: 3px solid var(--lb-border);
  border-top-color: var(--lb-blue);
  border-radius: 50%;
  animation: lb-spin 0.8s linear infinite;
}

@keyframes lb-spin {
  to { transform: rotate(360deg); }
}

.loading-text {
  font-size: 14px;
  color: var(--lb-text-3);
  margin: 0;
}

/* ========== Chat Body ========== */
.chat-body {
  flex: 1;
  overflow-y: auto;
  overflow-x: hidden;
  padding: 20px 0;
  scroll-behavior: smooth;
}

.chat-body::-webkit-scrollbar {
  width: 6px;
}

.chat-body::-webkit-scrollbar-thumb {
  background: var(--lb-border);
  border-radius: 3px;
}

/* ========== Message Row ========== */
.message-row {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  max-width: 800px;
  margin: 0 auto;
  padding: 6px 20px;
}

.message-row + .message-row {
  margin-top: 8px;
}

.message-row--user {
  flex-direction: row-reverse;
}

/* ========== Avatar ========== */
.avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  margin-top: 2px;
}

.avatar--assistant {
  background: var(--lb-blue);
}

.avatar--user {
  background: #8f959e;
}

/* ========== Message Bubble ========== */
.message-bubble {
  max-width: 70%;
  min-width: 40px;
  padding: 10px 14px;
  border-radius: var(--lb-radius-lg);
  word-break: break-word;
  line-height: 1.6;
  font-size: 14px;
  position: relative;
}

.bubble--assistant {
  background: var(--lb-bg-card);
  color: var(--lb-text-1);
  border-top-left-radius: 4px;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.04);
}

.bubble--user {
  background: var(--lb-blue);
  color: #ffffff;
  border-top-right-radius: 4px;
}

.bubble-text {
  white-space: pre-wrap;
}

.bubble-time {
  font-size: 11px;
  color: var(--lb-text-3);
  margin-top: 6px;
  text-align: right;
}

.bubble--user .bubble-time {
  color: rgba(255, 255, 255, 0.65);
}

/* ========== Thinking Animation ========== */
.bubble--thinking {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 14px 18px;
}

.dot {
  width: 8px;
  height: 8px;
  background: var(--lb-blue);
  border-radius: 50%;
  animation: lb-bounce 1.4s ease-in-out infinite;
}

.dot:nth-child(1) { animation-delay: 0s; }
.dot:nth-child(2) { animation-delay: 0.2s; }
.dot:nth-child(3) { animation-delay: 0.4s; }

@keyframes lb-bounce {
  0%, 60%, 100% { transform: translateY(0); opacity: 0.4; }
  30% { transform: translateY(-8px); opacity: 1; }
}

/* ========== Image & File in Bubble ========== */
.bubble-image {
  margin-top: 8px;
}

.chat-image-thumb {
  max-width: 200px;
  max-height: 160px;
  border-radius: var(--lb-radius);
  cursor: pointer;
  object-fit: cover;
  display: block;
  transition: opacity 0.2s;
}

.chat-image-thumb:hover {
  opacity: 0.85;
}

.bubble-files {
  margin-top: 8px;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.file-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 10px;
  background: rgba(0, 0, 0, 0.03);
  border-radius: 6px;
  font-size: 13px;
}

.file-icon {
  flex-shrink: 0;
}

.file-name {
  color: var(--lb-blue);
  text-decoration: underline;
  word-break: break-all;
}

.file-size {
  color: var(--lb-text-3);
  font-size: 12px;
  flex-shrink: 0;
}

/* ========== Chat Footer ========== */
.chat-footer {
  flex-shrink: 0;
  background: var(--lb-bg-card);
  border-top: 1px solid var(--lb-border);
  padding: 0;
}

/* ========== Attachment Bar ========== */
.attachment-bar {
  max-width: 800px;
  margin: 0 auto;
  padding: 8px 20px 0;
}

.attachment-bar-inner {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.attachment-chip {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 4px 8px;
  background: var(--lb-bg);
  border: 1px solid var(--lb-border);
  border-radius: 6px;
  font-size: 12px;
  color: var(--lb-text-2);
  max-width: 220px;
}

.att-thumb {
  width: 28px;
  height: 28px;
  border-radius: 4px;
  object-fit: cover;
  flex-shrink: 0;
}

.file-icon-sm {
  flex-shrink: 0;
}

.att-name {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 100px;
}

.att-size {
  color: var(--lb-text-3);
  font-size: 11px;
  flex-shrink: 0;
}

.att-remove {
  background: none;
  border: none;
  font-size: 16px;
  color: var(--lb-text-3);
  cursor: pointer;
  padding: 0 2px;
  line-height: 1;
  flex-shrink: 0;
}

.att-remove:hover {
  color: #cb2634;
}

/* ========== Input Row ========== */
.input-row {
  display: flex;
  align-items: flex-end;
  gap: 8px;
  max-width: 800px;
  margin: 0 auto;
  padding: 12px 20px 8px;
}

.hidden-input {
  display: none;
}

.btn-attach {
  width: 36px;
  height: 36px;
  border: none;
  background: none;
  border-radius: var(--lb-radius);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  transition: background 0.2s;
}

.btn-attach:hover {
  background: var(--lb-bg);
}

.btn-attach:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.message-input {
  flex: 1;
  border: 1px solid var(--lb-border);
  border-radius: var(--lb-radius-lg);
  padding: 8px 14px;
  font-size: 14px;
  line-height: 1.5;
  resize: none;
  outline: none;
  font-family: inherit;
  color: var(--lb-text-1);
  background: var(--lb-bg);
  transition: border-color 0.2s;
  max-height: 120px;
  overflow-y: auto;
}

.message-input:focus {
  border-color: var(--lb-blue);
  background: var(--lb-bg-card);
}

.message-input::placeholder {
  color: var(--lb-text-3);
}

.message-input:disabled {
  opacity: 0.6;
}

.btn-send {
  width: 36px;
  height: 36px;
  border: none;
  background: var(--lb-border);
  border-radius: var(--lb-radius);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  transition: background 0.2s;
}

.btn-send--active {
  background: var(--lb-blue);
}

.btn-send:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

/* ========== Finish Row ========== */
.finish-row {
  max-width: 800px;
  margin: 0 auto;
  padding: 4px 20px 14px;
  display: flex;
  justify-content: center;
}

.btn-finish {
  padding: 10px 32px;
  background: var(--lb-bg-card);
  color: var(--lb-blue);
  border: 1px solid var(--lb-blue);
  border-radius: var(--lb-radius-lg);
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
  font-family: inherit;
}

.btn-finish:hover:not(:disabled) {
  background: var(--lb-blue);
  color: #ffffff;
}

.btn-finish:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

/* ========== Image Preview Overlay ========== */
.preview-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 1000;
  background: rgba(0, 0, 0, 0.85);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
}

.preview-close {
  position: absolute;
  top: 20px;
  right: 20px;
  width: 40px;
  height: 40px;
  border: none;
  background: rgba(255, 255, 255, 0.15);
  color: #ffffff;
  font-size: 24px;
  border-radius: 50%;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: background 0.2s;
}

.preview-close:hover {
  background: rgba(255, 255, 255, 0.3);
}

.preview-image {
  max-width: 90vw;
  max-height: 90vh;
  object-fit: contain;
  border-radius: var(--lb-radius);
}

/* ========== Modal ========== */
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 1000;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
}

.modal-card {
  background: var(--lb-bg-card);
  border-radius: var(--lb-radius-lg);
  width: 100%;
  max-width: 520px;
  max-height: 80vh;
  display: flex;
  flex-direction: column;
  box-shadow: 0 8px 40px rgba(0, 0, 0, 0.12);
}

.modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20px 24px 0;
}

.modal-title {
  font-size: 17px;
  font-weight: 600;
  color: var(--lb-text-1);
  margin: 0;
}

.modal-close {
  width: 32px;
  height: 32px;
  border: none;
  background: none;
  font-size: 20px;
  color: var(--lb-text-3);
  cursor: pointer;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: background 0.2s;
}

.modal-close:hover {
  background: var(--lb-bg);
}

.modal-body {
  padding: 16px 24px;
  overflow-y: auto;
  flex: 1;
}

.modal-desc {
  font-size: 14px;
  color: var(--lb-text-2);
  margin: 0 0 12px;
}

.summary-preview {
  background: var(--lb-bg);
  border: 1px solid var(--lb-border);
  border-radius: var(--lb-radius);
  padding: 16px;
  max-height: 50vh;
  overflow-y: auto;
}

.summary-content {
  font-size: 14px;
  line-height: 1.7;
  color: var(--lb-text-1);
  white-space: pre-wrap;
}

.modal-footer {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 12px;
  padding: 16px 24px 20px;
  border-top: 1px solid var(--lb-border);
}

.btn-modal {
  padding: 8px 20px;
  border-radius: var(--lb-radius);
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
  font-family: inherit;
  border: none;
}

.btn-modal--secondary {
  background: var(--lb-bg);
  color: var(--lb-text-2);
  border: 1px solid var(--lb-border);
}

.btn-modal--secondary:hover {
  background: var(--lb-border);
}

.btn-modal--primary {
  background: var(--lb-blue);
  color: #ffffff;
}

.btn-modal--primary:hover:not(:disabled) {
  background: #2860e1;
}

.btn-modal--primary:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

/* ========== Responsive ========== */
@media (max-width: 600px) {
  .header-inner {
    padding: 10px 14px;
  }

  .banner-inner {
    padding: 12px 14px;
  }

  .message-row {
    padding: 6px 14px;
  }

  .message-bubble {
    max-width: 82%;
  }

  .input-row {
    padding: 10px 14px 6px;
  }

  .attachment-bar {
    padding: 8px 14px 0;
  }

  .finish-row {
    padding: 4px 14px 12px;
  }

  .modal-card {
    max-width: 100%;
    margin: 0 10px;
  }

  .modal-header {
    padding: 16px 18px 0;
  }

  .modal-body {
    padding: 12px 18px;
  }

  .modal-footer {
    padding: 14px 18px 16px;
  }
}
</style>
