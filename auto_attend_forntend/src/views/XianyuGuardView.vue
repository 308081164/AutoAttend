<template>
  <div class="xianyu-page">
    <!-- ===== Top Bar ===== -->
    <div class="xianyu-topbar">
      <div class="xianyu-topbar-left">
        <h1 class="xianyu-title">咸鱼值守</h1>
        <div class="xianyu-stats" v-if="dashboardData">
          <span class="xianyu-stat-item">
            <span class="stat-dot stat-dot--online"></span>
            {{ dashboardData.onlineAccounts }}/{{ dashboardData.totalAccounts }} 在线
          </span>
          <span class="xianyu-stat-item" :class="{ 'has-unread': dashboardData.totalUnread > 0 }">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="14" height="14"><path d="M18 8A6 6 0 006 8c0 7-3 9-3 9h18s-3-2-3-9"/><path d="M13.73 21a2 2 0 01-3.46 0"/></svg>
            {{ dashboardData.totalUnread }} 条未读
          </span>
        </div>
      </div>
      <div class="xianyu-topbar-right">
        <button class="xy-btn xy-btn--primary" @click="showAddAccount = true" v-if="!showAddAccount">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="14" height="14"><path d="M12 5v14M5 12h14"/></svg>
          添加账号
        </button>
        <button class="xy-btn xy-btn--ghost" @click="showQuickReplyManager = true">快捷回复管理</button>
        <button class="xy-btn xy-btn--ghost" @click="loadDashboard">刷新</button>
      </div>
    </div>

    <!-- ===== Add Account Panel ===== -->
    <div class="xy-panel xy-panel--add" v-if="showAddAccount">
      <div class="xy-panel-header">
        <h3 class="xy-panel-title">添加咸鱼账号</h3>
        <button class="xy-icon-btn" @click="showAddAccount = false">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="16" height="16"><path d="M18 6L6 18M6 6l12 12"/></svg>
        </button>
      </div>
      <form class="xy-form" @submit.prevent="createAccount">
        <div class="xy-form-grid">
          <div class="xy-form-item">
            <label>咸鱼昵称</label>
            <input v-model="form.nickname" placeholder="例如：我的咸鱼号" required />
          </div>
          <div class="xy-form-item">
            <label>头像 URL（可选）</label>
            <input v-model="form.avatarUrl" placeholder="https://example.com/avatar.jpg" />
          </div>
          <div class="xy-form-item">
            <label>Cookie（加密存储）</label>
            <textarea v-model="form.cookieEncrypted" rows="3" placeholder="粘贴咸鱼网页版 Cookie" required></textarea>
          </div>
        </div>
        <div class="xy-form-actions">
          <button type="button" class="xy-btn xy-btn--ghost" @click="showAddAccount = false">取消</button>
          <button type="submit" class="xy-btn xy-btn--primary" :disabled="creatingAccount">
            {{ creatingAccount ? '保存中...' : '保存账号' }}
          </button>
        </div>
      </form>
    </div>

    <!-- ===== Quick Reply Manager Modal ===== -->
    <div class="xy-modal-overlay" v-if="showQuickReplyManager" @click.self="showQuickReplyManager = false">
      <div class="xy-modal">
        <div class="xy-modal-header">
          <h3>快捷回复管理</h3>
          <button class="xy-icon-btn" @click="showQuickReplyManager = false">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="16" height="16"><path d="M18 6L6 18M6 6l12 12"/></svg>
          </button>
        </div>
        <div class="xy-modal-body">
          <div class="xy-quick-reply-form">
            <input v-model="qrForm.title" placeholder="回复标题" class="xy-input" />
            <input v-model="qrForm.content" placeholder="回复内容" class="xy-input" />
            <button class="xy-btn xy-btn--primary xy-btn--sm" @click="saveQuickReply" :disabled="savingQr">
              {{ editingQrId ? '更新' : '添加' }}
            </button>
          </div>
          <div class="xy-quick-reply-list">
            <div v-for="qr in quickReplies" :key="qr.id" class="xy-qr-item">
              <div class="xy-qr-info">
                <strong>{{ qr.title }}</strong>
                <span class="xy-qr-content">{{ qr.content }}</span>
              </div>
              <div class="xy-qr-actions">
                <button class="xy-icon-btn" @click="editQuickReply(qr)" title="编辑">
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="14" height="14"><path d="M11 4H4a2 2 0 00-2 2v14a2 2 0 002 2h14a2 2 0 002-2v-7"/><path d="M18.5 2.5a2.121 2.121 0 013 3L12 15l-4 1 1-4 9.5-9.5z"/></svg>
                </button>
                <button class="xy-icon-btn xy-icon-btn--danger" @click="deleteQuickReply(qr.id)" title="删除">
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="14" height="14"><polyline points="3 6 5 6 21 6"/><path d="M19 6v14a2 2 0 01-2 2H7a2 2 0 01-2-2V6m3 0V4a2 2 0 012-2h4a2 2 0 012 2v2"/></svg>
                </button>
              </div>
            </div>
            <div v-if="!quickReplies.length" class="xy-empty-hint">暂无快捷回复，在上方添加</div>
          </div>
        </div>
      </div>
    </div>

    <!-- ===== Empty State ===== -->
    <div class="xy-empty-state" v-if="!loading && !accounts.length && !showAddAccount">
      <div class="xy-empty-icon">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" width="48" height="48"><rect x="2" y="2" width="20" height="20" rx="4"/><path d="M8 12h8M12 8v8"/></svg>
      </div>
      <h3 class="xy-empty-title">还没有接入咸鱼账号</h3>
      <p class="xy-empty-desc">添加咸鱼网页版 Cookie 后，即可在平台内查看消息与快捷回复。</p>
      <button class="xy-btn xy-btn--primary" @click="showAddAccount = true">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="14" height="14"><path d="M12 5v14M5 12h14"/></svg>
        添加第一个账号
      </button>
    </div>

    <!-- ===== Loading ===== -->
    <div class="xy-loading" v-if="loading">
      <div class="xy-spinner"></div>
      <span>加载中...</span>
    </div>

    <!-- ===== Master-Detail Layout ===== -->
    <div class="xianyu-master-detail" v-if="accounts.length">
      <!-- Left: Account List -->
      <div class="xianyu-master">
        <div class="xianyu-master-header">
          <span class="xianyu-master-title">咸鱼账号</span>
          <span class="xianyu-master-count">{{ accounts.length }}</span>
        </div>
        <div class="xianyu-account-list">
          <div
            v-for="acc in accounts"
            :key="acc.id"
            class="xianyu-account-item"
            :class="{ 'is-active': selectedAccountId === acc.id }"
            @click="selectAccount(acc)"
          >
            <div class="xy-acc-avatar">
              <img v-if="acc.avatarUrl" :src="acc.avatarUrl" alt="" />
              <span v-else>{{ (acc.nickname || '?').charAt(0) }}</span>
            </div>
            <div class="xy-acc-info">
              <div class="xy-acc-name">{{ acc.nickname || '未命名' }}</div>
              <div class="xy-acc-status" :class="'xy-acc-status--' + acc.status">
                {{ statusLabel(acc.status) }}
              </div>
            </div>
            <div class="xy-acc-actions">
              <button class="xy-icon-btn" @click.stop="deleteAccount(acc.id)" title="删除">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="14" height="14"><polyline points="3 6 5 6 21 6"/><path d="M19 6v14a2 2 0 01-2 2H7a2 2 0 01-2-2V6m3 0V4a2 2 0 012-2h4a2 2 0 012 2v2"/></svg>
              </button>
            </div>
          </div>
        </div>
      </div>

      <!-- Right: Conversation & Messages -->
      <div class="xianyu-detail">
        <template v-if="selectedAccountId">
          <!-- Conversations List -->
          <div class="xy-conversations-panel" v-if="!selectedConversationId">
            <div class="xy-conversations-header">
              <span>会话列表</span>
              <span class="xy-badge" v-if="unreadCount > 0">{{ unreadCount }} 未读</span>
              <button
                type="button"
                class="xy-btn xy-btn--ghost xy-btn--sm"
                :disabled="!selectedAccountId || seedingDemo"
                @click="seedDemoThread"
              >
                {{ seedingDemo ? '插入中…' : '插入测试会话' }}
              </button>
            </div>
            <div class="xy-conversation-list">
              <div
                v-for="conv in conversations"
                :key="conv.id"
                class="xy-conversation-item"
                :class="{ 'has-unread': conv.unreadCount > 0 }"
                @click="selectConversation(conv)"
              >
                <div class="xy-conv-avatar">
                  <img v-if="conv.peerAvatar" :src="conv.peerAvatar" alt="" />
                  <span v-else>{{ (conv.peerNickname || '?').charAt(0) }}</span>
                </div>
                <div class="xy-conv-info">
                  <div class="xy-conv-name">{{ conv.peerNickname || '未知用户' }}</div>
                  <div class="xy-conv-last-msg">{{ conv.lastMessage || '' }}</div>
                </div>
                <div class="xy-conv-meta">
                  <span class="xy-conv-time">{{ formatTime(conv.lastMessageAt) }}</span>
                  <span v-if="conv.unreadCount > 0" class="xy-unread-badge">{{ conv.unreadCount }}</span>
                </div>
              </div>
              <div v-if="!conversations.length" class="xy-empty-hint">暂无会话</div>
            </div>
          </div>

          <!-- Messages Panel -->
          <div class="xy-messages-panel" v-if="selectedConversationId">
            <div class="xy-messages-header">
              <button class="xy-btn xy-btn--ghost xy-btn--sm" @click="selectedConversationId = null">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="14" height="14"><path d="M19 12H5M12 19l-7-7 7-7"/></svg>
                返回
              </button>
              <span class="xy-messages-title">{{ currentConvName }}</span>
            </div>
            <div class="xy-messages-body" ref="messagesBody">
              <div
                v-for="msg in messages"
                :key="msg.id"
                class="xy-message"
                :class="'xy-message--' + msg.direction"
              >
                <div class="xy-msg-bubble">
                  <template v-if="msg.msgType === 'image'">
                    <img :src="msg.fileUrl" alt="" class="xy-msg-image" />
                  </template>
                  <template v-else-if="msg.msgType === 'file'">
                    <a :href="msg.fileUrl" target="_blank" class="xy-msg-file">
                      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="14" height="14"><path d="M14 2H6a2 2 0 00-2 2v16a2 2 0 002 2h12a2 2 0 002-2V8z"/><polyline points="14 2 14 8 20 8"/></svg>
                      {{ msg.content || '文件' }}
                    </a>
                  </template>
                  <template v-else>
                    {{ msg.content }}
                  </template>
                  <div class="xy-msg-time">{{ formatTime(msg.sentAt) }}</div>
                </div>
              </div>
              <div v-if="!messages.length" class="xy-empty-hint">暂无消息</div>
            </div>
            <div class="xy-messages-input">
              <div class="xy-quick-reply-bar" v-if="quickReplies.length">
                <button
                  v-for="qr in quickReplies"
                  :key="qr.id"
                  class="xy-qr-chip"
                  @click="sendQuickReply(qr)"
                  :title="qr.content"
                >
                  {{ qr.title }}
                </button>
              </div>
              <div class="xy-input-row">
                <input
                  v-model="messageText"
                  placeholder="输入消息..."
                  class="xy-input"
                  @keyup.enter="sendMessage"
                />
                <button class="xy-btn xy-btn--primary" @click="sendMessage" :disabled="sendingMsg">
                  {{ sendingMsg ? '发送中...' : '发送' }}
                </button>
              </div>
            </div>
          </div>
        </template>

        <div class="xy-detail-empty" v-else>
          <p>请选择一个咸鱼账号查看会话</p>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'XianyuGuardView',
  data () {
    return {
      loading: false,
      accounts: [],
      selectedAccountId: null,
      conversations: [],
      selectedConversationId: null,
      messages: [],
      unreadCount: 0,
      dashboardData: null,
      showAddAccount: false,
      showQuickReplyManager: false,
      quickReplies: [],
      creatingAccount: false,
      form: {
        nickname: '',
        avatarUrl: '',
        cookieEncrypted: ''
      },
      qrForm: {
        title: '',
        content: ''
      },
      editingQrId: null,
      savingQr: false,
      messageText: '',
      sendingMsg: false,
      seedingDemo: false
    }
  },
  computed: {
    currentConvName () {
      const conv = this.conversations.find(c => c.id === this.selectedConversationId)
      return conv ? conv.peerNickname : ''
    }
  },
  mounted () {
    this.loadAccounts()
    this.loadDashboard()
    this.loadQuickReplies()
  },
  methods: {
    async loadAccounts () {
      this.loading = true
      try {
        const resp = await this.$http.get('/admin/xianyu/accounts')
        if (resp.data && resp.data.code === 0) {
          this.accounts = resp.data.data || []
          if (this.accounts.length && !this.selectedAccountId) {
            await this.selectAccount(this.accounts[0])
          }
        }
      } catch (e) {
        console.error('加载咸鱼账号失败', e)
      } finally {
        this.loading = false
      }
    },
    async seedDemoThread () {
      if (!this.selectedAccountId) return
      this.seedingDemo = true
      try {
        const resp = await this.$http.post('/admin/xianyu/accounts/' + this.selectedAccountId + '/demo-thread')
        if (resp.data && resp.data.code === 0) {
          await this.selectAccount(this.accounts.find(a => a.id === this.selectedAccountId) || { id: this.selectedAccountId })
        }
      } catch (e) {
        console.error('插入测试会话失败', e)
      } finally {
        this.seedingDemo = false
      }
    },
    async loadDashboard () {
      try {
        const resp = await this.$http.get('/admin/xianyu/dashboard')
        if (resp.data && resp.data.code === 0) {
          this.dashboardData = resp.data.data
        }
      } catch (e) {
        console.error('加载看板数据失败', e)
      }
    },
    async loadQuickReplies () {
      try {
        const resp = await this.$http.get('/admin/xianyu/quick-replies')
        if (resp.data && resp.data.code === 0) {
          this.quickReplies = resp.data.data || []
        }
      } catch (e) {
        console.error('加载快捷回复失败', e)
      }
    },
    async createAccount () {
      this.creatingAccount = true
      try {
        const resp = await this.$http.post('/admin/xianyu/accounts', this.form)
        if (resp.data && resp.data.code === 0) {
          this.accounts.unshift(resp.data.data)
          this.showAddAccount = false
          this.form = { nickname: '', avatarUrl: '', cookieEncrypted: '' }
          this.loadDashboard()
        }
      } catch (e) {
        console.error('创建账号失败', e)
      } finally {
        this.creatingAccount = false
      }
    },
    async deleteAccount (id) {
      if (!confirm('确定要删除该咸鱼账号吗？')) return
      try {
        await this.$http.delete('/admin/xianyu/accounts/' + id)
        this.accounts = this.accounts.filter(a => a.id !== id)
        if (this.selectedAccountId === id) {
          this.selectedAccountId = null
          this.conversations = []
          this.selectedConversationId = null
          this.messages = []
        }
        this.loadDashboard()
      } catch (e) {
        console.error('删除账号失败', e)
      }
    },
    async selectAccount (acc) {
      this.selectedAccountId = acc.id
      this.selectedConversationId = null
      this.messages = []
      try {
        const resp = await this.$http.get('/admin/xianyu/accounts/' + acc.id + '/conversations')
        if (resp.data && resp.data.code === 0) {
          this.conversations = resp.data.data || []
        }
        const unreadResp = await this.$http.get('/admin/xianyu/accounts/' + acc.id + '/conversations/unread-count')
        if (unreadResp.data && unreadResp.data.code === 0) {
          this.unreadCount = unreadResp.data.data.unreadCount || 0
        }
      } catch (e) {
        console.error('加载会话失败', e)
      }
    },
    async selectConversation (conv) {
      this.selectedConversationId = conv.id
      try {
        const resp = await this.$http.get('/admin/xianyu/conversations/' + conv.id + '/messages')
        if (resp.data && resp.data.code === 0) {
          this.messages = resp.data.data || []
        }
        // Mark as read
        await this.$http.post('/admin/xianyu/conversations/' + conv.id + '/read')
        conv.unreadCount = 0
        this.loadDashboard()
        this.$nextTick(() => {
          this.scrollToBottom()
        })
      } catch (e) {
        console.error('加载消息失败', e)
      }
    },
    async sendMessage () {
      const text = this.messageText.trim()
      if (!text || !this.selectedConversationId) return
      this.sendingMsg = true
      try {
        const resp = await this.$http.post('/admin/xianyu/conversations/' + this.selectedConversationId + '/messages', {
          content: text,
          msgType: 'text'
        })
        if (resp.data && resp.data.code === 0) {
          this.messages.push(resp.data.data)
          this.messageText = ''
          this.$nextTick(() => {
            this.scrollToBottom()
          })
        }
      } catch (e) {
        console.error('发送消息失败', e)
      } finally {
        this.sendingMsg = false
      }
    },
    sendQuickReply (qr) {
      this.messageText = qr.content
    },
    async saveQuickReply () {
      if (!this.qrForm.title.trim() || !this.qrForm.content.trim()) return
      this.savingQr = true
      try {
        if (this.editingQrId) {
          const resp = await this.$http.put('/admin/xianyu/quick-replies/' + this.editingQrId, this.qrForm)
          if (resp.data && resp.data.code === 0) {
            const idx = this.quickReplies.findIndex(q => q.id === this.editingQrId)
            if (idx >= 0) this.quickReplies.splice(idx, 1, resp.data.data)
          }
        } else {
          const resp = await this.$http.post('/admin/xianyu/quick-replies', this.qrForm)
          if (resp.data && resp.data.code === 0) {
            this.quickReplies.push(resp.data.data)
          }
        }
        this.qrForm = { title: '', content: '' }
        this.editingQrId = null
      } catch (e) {
        console.error('保存快捷回复失败', e)
      } finally {
        this.savingQr = false
      }
    },
    editQuickReply (qr) {
      this.editingQrId = qr.id
      this.qrForm = { title: qr.title, content: qr.content }
    },
    async deleteQuickReply (id) {
      if (!confirm('确定删除该快捷回复？')) return
      try {
        await this.$http.delete('/admin/xianyu/quick-replies/' + id)
        this.quickReplies = this.quickReplies.filter(q => q.id !== id)
      } catch (e) {
        console.error('删除快捷回复失败', e)
      }
    },
    statusLabel (status) {
      const map = { online: '在线', offline: '离线', expired: '已过期' }
      return map[status] || status
    },
    formatTime (dt) {
      if (!dt) return ''
      const d = new Date(dt)
      const pad = n => String(n).padStart(2, '0')
      return pad(d.getHours()) + ':' + pad(d.getMinutes())
    },
    scrollToBottom () {
      const el = this.$refs.messagesBody
      if (el) el.scrollTop = el.scrollHeight
    }
  }
}
</script>

<style scoped>
/* ===== Page Layout ===== */
.xianyu-page {
  padding: 20px 24px;
  height: calc(100vh - 56px - 40px);
  display: flex;
  flex-direction: column;
}

/* ===== Top Bar ===== */
.xianyu-topbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
  flex-shrink: 0;
}
.xianyu-topbar-left {
  display: flex;
  align-items: center;
  gap: 16px;
}
.xianyu-title {
  font-size: 20px;
  font-weight: 700;
  margin: 0;
  color: var(--text-primary, #1f2937);
}
.xianyu-stats {
  display: flex;
  gap: 12px;
  font-size: 13px;
  color: var(--text-secondary, #6b7280);
}
.xianyu-stat-item {
  display: flex;
  align-items: center;
  gap: 4px;
}
.stat-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  display: inline-block;
}
.stat-dot--online { background: #10b981; }
.has-unread { color: var(--brand-blue, #1456F0); font-weight: 500; }
.xianyu-topbar-right {
  display: flex;
  gap: 8px;
}

/* ===== Buttons ===== */
.xy-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 7px 14px;
  border-radius: 6px;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  border: 1px solid transparent;
  transition: all 0.2s;
  font-family: inherit;
  white-space: nowrap;
}
.xy-btn--primary {
  background: var(--brand-blue, #1456F0);
  color: #fff;
  border-color: var(--brand-blue, #1456F0);
}
.xy-btn--primary:hover { background: #0f4ad0; }
.xy-btn--primary:disabled { opacity: 0.5; cursor: not-allowed; }
.xy-btn--ghost {
  background: transparent;
  color: var(--text-secondary, #6b7280);
  border-color: var(--border-primary, #e5e7eb);
}
.xy-btn--ghost:hover { background: var(--bg-page, #f0f2f5); color: var(--text-primary, #1f2937); }
.xy-btn--sm { padding: 4px 10px; font-size: 12px; }
.xy-icon-btn {
  width: 28px;
  height: 28px;
  border: none;
  background: transparent;
  border-radius: 4px;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: var(--text-disabled, #9ca3af);
  transition: all 0.2s;
}
.xy-icon-btn:hover { background: rgba(0,0,0,0.06); color: var(--text-primary, #1f2937); }
.xy-icon-btn--danger:hover { background: #fef2f2; color: #dc2626; }

/* ===== Panel ===== */
.xy-panel {
  background: #fff;
  border: 1px solid var(--border-primary, #e5e7eb);
  border-radius: 8px;
  margin-bottom: 16px;
  flex-shrink: 0;
}
.xy-panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  border-bottom: 1px solid var(--border-primary, #e5e7eb);
}
.xy-panel-title { margin: 0; font-size: 15px; font-weight: 600; }
.xy-form { padding: 16px; }
.xy-form-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; }
.xy-form-item { display: flex; flex-direction: column; gap: 4px; }
.xy-form-item:last-child { grid-column: 1 / -1; }
.xy-form-item label { font-size: 12px; font-weight: 500; color: var(--text-secondary, #6b7280); }
.xy-form-item input,
.xy-form-item textarea {
  padding: 8px 10px;
  border: 1px solid var(--border-primary, #e5e7eb);
  border-radius: 6px;
  font-size: 13px;
  font-family: inherit;
  outline: none;
  transition: border-color 0.2s;
}
.xy-form-item input:focus,
.xy-form-item textarea:focus { border-color: var(--brand-blue, #1456F0); }
.xy-form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 12px;
}

/* ===== Empty State ===== */
.xy-empty-state {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: var(--text-disabled, #9ca3af);
}
.xy-empty-icon { margin-bottom: 12px; opacity: 0.4; }
.xy-empty-title { margin: 0 0 6px; font-size: 16px; font-weight: 600; color: var(--text-secondary, #6b7280); }
.xy-empty-desc { margin: 0 0 16px; font-size: 13px; }
.xy-empty-hint { padding: 24px; text-align: center; color: var(--text-disabled, #9ca3af); font-size: 13px; }

/* ===== Loading ===== */
.xy-loading {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  color: var(--text-secondary, #6b7280);
  font-size: 14px;
}
.xy-spinner {
  width: 20px;
  height: 20px;
  border: 2px solid var(--border-primary, #e5e7eb);
  border-top-color: var(--brand-blue, #1456F0);
  border-radius: 50%;
  animation: spin 0.6s linear infinite;
}
@keyframes spin { to { transform: rotate(360deg); } }

/* ===== Master-Detail Layout ===== */
.xianyu-master-detail {
  flex: 1;
  display: flex;
  gap: 16px;
  min-height: 0;
  overflow: hidden;
}

/* ===== Left: Account List ===== */
.xianyu-master {
  width: 280px;
  min-width: 280px;
  background: #fff;
  border: 1px solid var(--border-primary, #e5e7eb);
  border-radius: 8px;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}
.xianyu-master-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  border-bottom: 1px solid var(--border-primary, #e5e7eb);
  flex-shrink: 0;
}
.xianyu-master-title { font-size: 14px; font-weight: 600; }
.xianyu-master-count {
  font-size: 12px;
  background: var(--bg-page, #f0f2f5);
  padding: 2px 8px;
  border-radius: 10px;
  color: var(--text-secondary, #6b7280);
}
.xianyu-account-list { flex: 1; overflow-y: auto; }
.xianyu-account-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 16px;
  cursor: pointer;
  transition: background 0.15s;
  border-bottom: 1px solid var(--border-primary, #e5e7eb);
}
.xianyu-account-item:hover { background: var(--bg-page, #f0f2f5); }
.xianyu-account-item.is-active { background: rgba(20, 86, 240, 0.08); }
.xy-acc-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: var(--brand-blue, #1456F0);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: 600;
  flex-shrink: 0;
  overflow: hidden;
}
.xy-acc-avatar img { width: 100%; height: 100%; object-fit: cover; }
.xy-acc-info { flex: 1; min-width: 0; }
.xy-acc-name { font-size: 13px; font-weight: 500; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.xy-acc-status { font-size: 11px; margin-top: 2px; }
.xy-acc-status--online { color: #10b981; }
.xy-acc-status--offline { color: var(--text-disabled, #9ca3af); }
.xy-acc-status--expired { color: #f59e0b; }
.xy-acc-actions { flex-shrink: 0; }

/* ===== Right: Detail ===== */
.xianyu-detail {
  flex: 1;
  background: #fff;
  border: 1px solid var(--border-primary, #e5e7eb);
  border-radius: 8px;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}
.xy-detail-empty {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--text-disabled, #9ca3af);
  font-size: 14px;
}

/* ===== Conversations ===== */
.xy-conversations-panel { flex: 1; display: flex; flex-direction: column; overflow: hidden; }
.xy-conversations-header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 16px;
  border-bottom: 1px solid var(--border-primary, #e5e7eb);
  font-size: 14px;
  font-weight: 600;
  flex-shrink: 0;
}
.xy-badge {
  font-size: 11px;
  background: var(--brand-blue, #1456F0);
  color: #fff;
  padding: 1px 8px;
  border-radius: 10px;
  font-weight: 500;
}
.xy-conversation-list { flex: 1; overflow-y: auto; }
.xy-conversation-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 16px;
  cursor: pointer;
  transition: background 0.15s;
  border-bottom: 1px solid var(--border-primary, #e5e7eb);
}
.xy-conversation-item:hover { background: var(--bg-page, #f0f2f5); }
.xy-conversation-item.has-unread { background: rgba(20, 86, 240, 0.04); }
.xy-conv-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: var(--text-disabled, #9ca3af);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: 600;
  flex-shrink: 0;
  overflow: hidden;
}
.xy-conv-avatar img { width: 100%; height: 100%; object-fit: cover; }
.xy-conv-info { flex: 1; min-width: 0; }
.xy-conv-name { font-size: 13px; font-weight: 500; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.xy-conv-last-msg { font-size: 12px; color: var(--text-disabled, #9ca3af); white-space: nowrap; overflow: hidden; text-overflow: ellipsis; margin-top: 2px; }
.xy-conv-meta { flex-shrink: 0; display: flex; flex-direction: column; align-items: flex-end; gap: 2px; }
.xy-conv-time { font-size: 11px; color: var(--text-disabled, #9ca3af); }
.xy-unread-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 18px;
  height: 18px;
  padding: 0 5px;
  border-radius: 9px;
  background: var(--brand-blue, #1456F0);
  color: #fff;
  font-size: 11px;
  font-weight: 600;
}

/* ===== Messages ===== */
.xy-messages-panel { flex: 1; display: flex; flex-direction: column; overflow: hidden; }
.xy-messages-header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 16px;
  border-bottom: 1px solid var(--border-primary, #e5e7eb);
  flex-shrink: 0;
}
.xy-messages-title { font-size: 14px; font-weight: 600; }
.xy-messages-body {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.xy-message {
  display: flex;
  max-width: 75%;
}
.xy-message--inbound { align-self: flex-start; }
.xy-message--outbound { align-self: flex-end; }
.xy-msg-bubble {
  padding: 8px 12px;
  border-radius: 12px;
  font-size: 13px;
  line-height: 1.5;
  word-break: break-word;
  position: relative;
}
.xy-message--inbound .xy-msg-bubble {
  background: var(--bg-page, #f0f2f5);
  color: var(--text-primary, #1f2937);
  border-bottom-left-radius: 4px;
}
.xy-message--outbound .xy-msg-bubble {
  background: var(--brand-blue, #1456F0);
  color: #fff;
  border-bottom-right-radius: 4px;
}
.xy-msg-image {
  max-width: 200px;
  max-height: 200px;
  border-radius: 8px;
  display: block;
}
.xy-msg-file {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  color: inherit;
  text-decoration: underline;
}
.xy-msg-time {
  font-size: 10px;
  opacity: 0.6;
  margin-top: 4px;
  text-align: right;
}
.xy-messages-input {
  border-top: 1px solid var(--border-primary, #e5e7eb);
  padding: 10px 16px;
  flex-shrink: 0;
}
.xy-quick-reply-bar {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-bottom: 8px;
}
.xy-qr-chip {
  padding: 4px 10px;
  border-radius: 12px;
  border: 1px solid var(--border-primary, #e5e7eb);
  background: #fff;
  font-size: 12px;
  cursor: pointer;
  transition: all 0.15s;
  color: var(--text-secondary, #6b7280);
}
.xy-qr-chip:hover {
  border-color: var(--brand-blue, #1456F0);
  color: var(--brand-blue, #1456F0);
}
.xy-input-row {
  display: flex;
  gap: 8px;
}
.xy-input-row .xy-input {
  flex: 1;
  padding: 8px 12px;
  border: 1px solid var(--border-primary, #e5e7eb);
  border-radius: 6px;
  font-size: 13px;
  font-family: inherit;
  outline: none;
  transition: border-color 0.2s;
}
.xy-input-row .xy-input:focus { border-color: var(--brand-blue, #1456F0); }

/* ===== Modal ===== */
.xy-modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.4);
  z-index: 2000;
  display: flex;
  align-items: center;
  justify-content: center;
}
.xy-modal {
  background: #fff;
  border-radius: 12px;
  width: 480px;
  max-width: 90vw;
  max-height: 80vh;
  display: flex;
  flex-direction: column;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.15);
}
.xy-modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px;
  border-bottom: 1px solid var(--border-primary, #e5e7eb);
}
.xy-modal-header h3 { margin: 0; font-size: 16px; font-weight: 600; }
.xy-modal-body {
  padding: 16px 20px;
  overflow-y: auto;
  flex: 1;
}
.xy-quick-reply-form {
  display: flex;
  gap: 8px;
  margin-bottom: 16px;
}
.xy-quick-reply-form .xy-input {
  flex: 1;
  padding: 8px 10px;
  border: 1px solid var(--border-primary, #e5e7eb);
  border-radius: 6px;
  font-size: 13px;
  font-family: inherit;
  outline: none;
}
.xy-quick-reply-form .xy-input:focus { border-color: var(--brand-blue, #1456F0); }
.xy-quick-reply-list { display: flex; flex-direction: column; gap: 6px; }
.xy-qr-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 12px;
  border-radius: 6px;
  background: var(--bg-page, #f0f2f5);
}
.xy-qr-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
  flex: 1;
}
.xy-qr-info strong { font-size: 13px; }
.xy-qr-content { font-size: 12px; color: var(--text-secondary, #6b7280); white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.xy-qr-actions { display: flex; gap: 4px; flex-shrink: 0; }
