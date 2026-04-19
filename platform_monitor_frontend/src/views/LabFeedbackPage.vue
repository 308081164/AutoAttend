<template>
  <div class="wrap">
    <h2 class="title">增效实验室 · 问题反馈</h2>
    <p class="lead">租户管理员在「增效实验室」提交的反馈与附图。</p>

    <p v-if="loading" class="muted">加载中…</p>
    <p v-else-if="error" class="err">{{ error }}</p>

    <template v-else>
      <div class="toolbar">
        <span class="muted">共 {{ total }} 条</span>
        <div class="pager">
          <button type="button" class="btn" :disabled="page <= 1" @click="goPage(page - 1)">上一页</button>
          <span class="muted">第 {{ page }} / {{ totalPages }} 页</span>
          <button type="button" class="btn" :disabled="page >= totalPages" @click="goPage(page + 1)">下一页</button>
        </div>
      </div>

      <div v-if="items.length" class="list">
        <article v-for="row in items" :key="row.id" class="card">
          <div class="card-head">
            <span class="mono">#{{ row.id }}</span>
            <span class="muted">{{ formatTime(row.createdAt) }}</span>
          </div>
          <div class="card-meta">
            <span><strong>租户</strong> {{ row.tenantName || '—' }}</span>
            <span class="mono">tenant_id={{ row.tenantId }}</span>
          </div>
          <pre class="content">{{ row.content }}</pre>
          <div v-if="row.imageKey" class="img-wrap">
            <a :href="imageUrl(row.imageKey)" target="_blank" rel="noopener" class="img-link">
              <img :src="imageUrl(row.imageKey)" alt="附图" class="thumb" />
            </a>
            <div class="muted small">{{ row.imageKey }}</div>
          </div>
        </article>
      </div>
      <p v-else class="muted">暂无反馈记录</p>
    </template>
  </div>
</template>

<script>
import { http } from '../api/http.js'

export default {
  name: 'LabFeedbackPage',
  data () {
    return {
      loading: true,
      error: '',
      items: [],
      total: 0,
      page: 1,
      pageSize: 20
    }
  },
  computed: {
    totalPages () {
      if (!this.total || !this.pageSize) return 1
      return Math.max(1, Math.ceil(this.total / this.pageSize))
    }
  },
  created () {
    this.load()
  },
  methods: {
    imageUrl (key) {
      if (!key) return ''
      return '/api/platform/lab/feedback/image?key=' + encodeURIComponent(key)
    },
    formatTime (t) {
      if (!t) return '—'
      return String(t).replace('T', ' ').slice(0, 19)
    },
    async load () {
      this.loading = true
      this.error = ''
      try {
        const r = await http.get('/platform/lab/feedback', {
          params: { page: this.page, pageSize: this.pageSize }
        })
        const body = r.data
        if (body && body.code === 0 && body.data) {
          this.items = Array.isArray(body.data.items) ? body.data.items : []
          this.total = typeof body.data.total === 'number' ? body.data.total : 0
        } else {
          this.error = (body && body.message) || '加载失败'
          this.items = []
          this.total = 0
        }
      } catch (e) {
        this.error = (e.response && e.response.data && e.response.data.message) || '加载失败'
        this.items = []
        this.total = 0
      } finally {
        this.loading = false
      }
    },
    goPage (p) {
      if (p < 1 || p > this.totalPages) return
      this.page = p
      this.load()
    }
  }
}
</script>

<style scoped>
.wrap {
  max-width: 960px;
}
.title {
  margin: 0 0 8px;
  font-size: 22px;
  font-weight: 600;
}
.lead {
  margin: 0 0 20px;
  color: #94a3b8;
  font-size: 14px;
}
.muted {
  color: #94a3b8;
  font-size: 14px;
}
.err {
  color: #f87171;
}
.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
  flex-wrap: wrap;
  gap: 12px;
}
.pager {
  display: flex;
  align-items: center;
  gap: 10px;
}
.btn {
  background: #1e293b;
  border: 1px solid #334155;
  color: #e2e8f0;
  padding: 6px 12px;
  border-radius: 6px;
  cursor: pointer;
  font-size: 13px;
}
.btn:disabled {
  opacity: 0.45;
  cursor: not-allowed;
}
.list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.card {
  background: #1e293b;
  border: 1px solid #334155;
  border-radius: 10px;
  padding: 16px;
}
.card-head {
  display: flex;
  justify-content: space-between;
  margin-bottom: 8px;
  font-size: 13px;
}
.card-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  font-size: 13px;
  color: #cbd5e1;
  margin-bottom: 10px;
}
.mono {
  font-family: ui-monospace, monospace;
  font-size: 12px;
}
.content {
  margin: 0;
  white-space: pre-wrap;
  word-break: break-word;
  font-size: 14px;
  line-height: 1.6;
  color: #e2e8f0;
  font-family: inherit;
}
.img-wrap {
  margin-top: 12px;
}
.thumb {
  max-width: 100%;
  max-height: 360px;
  border-radius: 8px;
  border: 1px solid #334155;
}
.img-link {
  display: inline-block;
}
.small {
  font-size: 12px;
  margin-top: 6px;
}
</style>
