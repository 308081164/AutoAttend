<template>
  <div class="quote-list-page">
    <div class="page-head">
      <h1>报价项目</h1>
      <p class="desc">结构化需求 → 人天与报价 → 报价单 → AI 合同（半自动化）</p>
      <p class="desc desc-sub">请选择创建方式：<strong>单体项目报价</strong>适合单栈/单交付；<strong>解决方案级报价</strong>适合多端/多交付物统一签约（可先规划交付物再填功能）。</p>
      <div class="head-actions head-actions-split">
        <router-link to="/quote/showcase-config" class="secondary-button">🏢 展示页配置</router-link>
        <router-link to="/quote/config" class="secondary-button">{{ $t('quote.quoteConfigNav') }}</router-link>
        <router-link to="/quote/new" class="primary-button">新建 · 单体项目报价</router-link>
        <router-link to="/quote/solution-wizard" class="primary-button primary-button--alt">新建 · 解决方案级报价</router-link>
      </div>
      <div class="head-actions head-actions-biz">
        <router-link to="/quote/biz-dashboard" class="secondary-button">数据看板</router-link>
        <router-link to="/quote/customers" class="secondary-button">客户管理</router-link>
        <router-link to="/quote/opportunities" class="secondary-button">商机看板</router-link>
        <router-link to="/xianyu" class="secondary-button">咸鱼值守</router-link>
      </div>
      <!-- 团队专属 Agent 链接 -->
      <div v-if="quickQuoteSlug" class="quick-quote-box">
        <div class="quick-quote-info">
          <strong>🔗 团队专属全自动 Agent 链接</strong>
          <span class="hint">发送给客户，客户自助创建项目并开始 AI 需求分析</span>
        </div>
        <div class="quick-quote-actions">
          <code class="quick-quote-link">{{ quickQuoteUrl }}</code>
          <button class="btn-sm primary" @click="copyQuickQuoteLink">{{ quickQuoteCopied ? '已复制 ✓' : '复制链接' }}</button>
          <a :href="quickQuoteUrl" target="_blank" rel="noopener" class="btn-sm secondary">预览</a>
        </div>
      </div>
    </div>
    <div v-if="loading" class="placeholder">{{ $t('quote.loading') }}</div>
    <div v-else-if="!items.length" class="placeholder">{{ $t('quote.emptyList') }}</div>
    <template v-else>
    <table class="data-table data-table--pc">
      <thead>
        <tr>
          <th>ID</th>
          <th>{{ $t('quote.name') }}</th>
          <th>模式</th>
          <th>{{ $t('quote.techStack') }}</th>
          <th>操作</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="row in items" :key="row.id">
          <td>{{ row.id }}</td>
          <td>{{ row.name }}</td>
          <td>{{ row.quoteKind === 'solution' ? '解决方案' : '单体' }}</td>
          <td>{{ row.techStack }}</td>
          <td>
            <router-link :to="'/quote/' + row.id">{{ $t('quote.open') }}</router-link>
            <button
              class="danger-button"
              :disabled="deletingId === row.id"
              @click="deleteProject(row.id)"
            >{{ $t('quote.deleteProject') }}</button>
          </td>
        </tr>
      </tbody>
    </table>
    <div class="card-list card-list--mobile">
      <div v-for="row in items" :key="row.id" class="quote-card">
        <div class="quote-card__header">
          <span class="quote-card__id">#{{ row.id }}</span>
          <span class="quote-card__mode" :class="row.quoteKind === 'solution' ? 'quote-card__mode--solution' : ''">
            {{ row.quoteKind === 'solution' ? '解决方案' : '单体' }}
          </span>
        </div>
        <h3 class="quote-card__name">{{ row.name }}</h3>
        <div class="quote-card__tech">技术栈：{{ row.techStack }}</div>
        <div class="quote-card__actions">
          <router-link :to="'/quote/' + row.id" class="quote-card__btn quote-card__btn--primary">打开</router-link>
          <button
            class="quote-card__btn quote-card__btn--danger"
            :disabled="deletingId === row.id"
            @click="deleteProject(row.id)"
          >删除</button>
        </div>
      </div>
    </div>
    </template>
  </div>
</template>

<script>
export default {
  name: 'QuoteListView',
  data () {
    return {
      items: [],
      loading: true,
      deletingId: null,
      quickQuoteSlug: '',
      quickQuoteCopied: false
    }
  },
  computed: {
    quickQuoteUrl () {
      if (!this.quickQuoteSlug) return ''
      return window.location.origin + '/quick-quote/' + this.quickQuoteSlug
    }
  },
  created () {
    this.load()
    this.loadQuickQuoteSlug()
  },
  methods: {
    async load () {
      this.loading = true
      try {
        const resp = await this.$http.get('/admin/quote/projects', { params: { page: 1, pageSize: 100 } })
        if (resp.data && resp.data.code === 0 && resp.data.data) {
          this.items = resp.data.data.items || []
        } else {
          this.items = []
        }
      } catch (e) {
        this.items = []
      } finally {
        this.loading = false
      }
    },
    async loadQuickQuoteSlug () {
      try {
        const resp = await this.$http.get('/admin/auth/me')
        if (resp.data && resp.data.code === 0 && resp.data.data && resp.data.data.slug) {
          this.quickQuoteSlug = resp.data.data.slug
        }
      } catch (e) { void e }
    },
    async copyQuickQuoteLink () {
      if (!this.quickQuoteUrl) return
      try {
        await navigator.clipboard.writeText(this.quickQuoteUrl)
        this.quickQuoteCopied = true
        setTimeout(() => { this.quickQuoteCopied = false }, 3000)
      } catch (e) {
        const ta = document.createElement('textarea')
        ta.value = this.quickQuoteUrl
        document.body.appendChild(ta)
        ta.select()
        document.execCommand('copy')
        document.body.removeChild(ta)
        this.quickQuoteCopied = true
        setTimeout(() => { this.quickQuoteCopied = false }, 3000)
      }
    },
    async deleteProject (id) {
      if (!confirm(this.$t('quote.confirmDeleteQuoteProject'))) return
      this.deletingId = id
      try {
        const resp = await this.$http.delete('/admin/quote/projects/' + id)
        if (resp.data && resp.data.code === 0) {
          await this.load()
        } else {
          alert((resp.data && resp.data.message) || '删除失败')
        }
      } catch (e) {
        alert((e.response && e.response.data && e.response.data.message) || '删除失败')
      } finally {
        this.deletingId = null
      }
    }
  }
}
</script>

<style scoped>
/* 团队专属链接 */
.quick-quote-box {
  margin-top: 16px;
  padding: 14px 16px;
  background: var(--bg-muted, #f0f0ff);
  border: 1px solid #d4d4f7;
  border-radius: 10px;
}
.quick-quote-info {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  margin-bottom: 10px;
}
.quick-quote-info strong { font-size: 14px; }
.quick-quote-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}
.quick-quote-link {
  font-size: 12px;
  padding: 4px 8px;
  background: #fff;
  border: 1px solid #ddd;
  border-radius: 4px;
  word-break: break-all;
  flex: 1;
  min-width: 0;
  color: #667eea;
}
.btn-sm {
  padding: 5px 12px;
  font-size: 12px;
  border-radius: 6px;
  border: none;
  cursor: pointer;
  text-decoration: none;
  white-space: nowrap;
}
.btn-sm.primary { background: #667eea; color: #fff; }
.btn-sm.secondary { background: #f3f4f6; color: #333; border: 1px solid #ddd; }
.hint { color: var(--text-secondary); font-size: 12px; }

.quote-list-page {
  max-width: 900px;
  margin: 0 auto;
  padding: var(--space-xl);
  color: var(--text-primary);
  background: var(--bg-page);
  min-height: 100%;
  box-sizing: border-box;
}
.page-head {
  margin-bottom: var(--space-xl);
}
.page-head h1 {
  margin: 0 0 var(--space-sm);
  font-size: var(--font-size-xxl);
  font-weight: var(--font-weight-bold);
  color: var(--text-primary);
}
.desc {
  color: var(--text-secondary);
  font-size: var(--font-size-base);
  margin-bottom: var(--space-md);
  line-height: var(--line-height-normal);
  font-weight: var(--font-weight-medium);
}
.desc-sub {
  margin-top: -8px;
  margin-bottom: var(--space-md);
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-normal);
  opacity: 0.95;
}
.head-actions-split {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  align-items: center;
}
.head-actions-biz {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
  margin-bottom: var(--space-md);
}
.primary-button--alt {
  background: linear-gradient(135deg, #0d9488, #0f766e);
  border: none;
}

.head-actions {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-sm);
  align-items: center;
  margin-bottom: var(--space-md);
}
.primary-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: var(--space-sm) var(--space-lg);
  background: var(--brand-blue);
  color: #fff;
  border-radius: var(--radius-sm);
  text-decoration: none;
  font-size: var(--font-size-base);
  font-weight: var(--font-weight-semibold);
  transition: var(--transition-fast);
  box-shadow: var(--shadow-btn);
}
.primary-button:hover {
  background: var(--brand-blue-hover);
}
.secondary-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: var(--space-sm) var(--space-lg);
  background: var(--bg-card);
  color: var(--text-primary);
  border-radius: var(--radius-sm);
  text-decoration: none;
  font-size: var(--font-size-base);
  border: 1px solid var(--border-primary);
  font-weight: var(--font-weight-medium);
  transition: var(--transition-fast);
}
.secondary-button:hover {
  background: var(--bg-hover);
  border-color: var(--border-input-hover);
}
.danger-button {
  margin-left: var(--space-sm);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: var(--space-xs) var(--space-md);
  background: var(--danger);
  color: #fff;
  border-radius: var(--radius-sm);
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-semibold);
  border: 1px solid var(--danger);
  cursor: pointer;
  transition: var(--transition-fast);
}
.danger-button:hover {
  background: #E03E3A;
  border-color: #E03E3A;
}
.danger-button:disabled {
  opacity: 0.45;
  cursor: not-allowed;
}
.data-table {
  width: 100%;
  border-collapse: collapse;
  font-size: var(--font-size-base);
  background: var(--bg-card);
  border: 1px solid var(--border-primary);
  border-radius: var(--radius-md);
  overflow: hidden;
}
.data-table th,
.data-table td {
  padding: var(--space-sm) var(--space-md);
  text-align: left;
  color: var(--text-primary);
  border-bottom: 1px solid var(--border-primary);
}
.data-table th {
  background: var(--bg-hover);
  color: var(--text-primary);
  font-weight: var(--font-weight-semibold);
  font-size: var(--font-size-sm);
}
.data-table tbody tr:hover {
  background: var(--bg-hover);
}
.data-table tbody tr:nth-child(even) {
  background: #FAFBFC;
}
.data-table tbody tr:nth-child(even):hover {
  background: var(--bg-hover);
}
.placeholder {
  padding: var(--space-xxl);
  color: var(--text-secondary);
  font-weight: var(--font-weight-medium);
  text-align: center;
}

/* ========== 移动端卡片列表 ========== */
.card-list--mobile {
  display: none;
}

.quote-card {
  background: var(--bg-card);
  border: 1px solid var(--border-primary);
  border-radius: var(--radius-md);
  padding: var(--space-md);
  margin-bottom: var(--space-md);
}

.quote-card__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: var(--space-sm);
}

.quote-card__id {
  font-size: var(--font-size-sm);
  color: var(--text-secondary);
  font-weight: var(--font-weight-medium);
}

.quote-card__mode {
  font-size: var(--font-size-xs);
  padding: 2px 8px;
  border-radius: 4px;
  background: var(--bg-hover);
  color: var(--text-secondary);
}

.quote-card__mode--solution {
  background: rgba(13, 148, 136, 0.1);
  color: #0d9488;
}

.quote-card__name {
  margin: 0 0 var(--space-sm);
  font-size: var(--font-size-lg);
  font-weight: var(--font-weight-semibold);
  color: var(--text-primary);
  line-height: 1.4;
}

.quote-card__tech {
  font-size: var(--font-size-sm);
  color: var(--text-secondary);
  margin-bottom: var(--space-md);
}

.quote-card__actions {
  display: flex;
  gap: var(--space-sm);
}

.quote-card__btn {
  flex: 1;
  padding: var(--space-sm) var(--space-md);
  border-radius: var(--radius-sm);
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-medium);
  text-align: center;
  text-decoration: none;
  cursor: pointer;
  transition: var(--transition-fast);
}

.quote-card__btn--primary {
  background: var(--brand-blue);
  color: #fff;
  border: none;
}

.quote-card__btn--primary:hover {
  background: var(--brand-blue-hover);
}

.quote-card__btn--danger {
  background: #fff;
  color: var(--danger);
  border: 1px solid var(--danger);
}

.quote-card__btn--danger:hover {
  background: var(--danger);
  color: #fff;
}

.quote-card__btn--danger:disabled {
  opacity: 0.45;
  cursor: not-allowed;
}

/* ========== 响应式：移动端 ========== */
@media (max-width: 768px) {
  .quote-list-page {
    padding: var(--space-md);
    padding-bottom: calc(var(--space-xl) + env(safe-area-inset-bottom, 0px));
  }

  .page-head h1 {
    font-size: var(--font-size-xl);
  }

  .desc {
    font-size: var(--font-size-sm);
    line-height: 1.5;
  }

  .desc-sub {
    display: none;
  }

  .head-actions-split {
    flex-direction: column;
    gap: var(--space-sm);
  }

  .head-actions-split .secondary-button,
  .head-actions-split .primary-button,
  .head-actions-biz .secondary-button {
    width: 100%;
    text-align: center;
    font-size: var(--font-size-sm);
    padding: var(--space-sm) var(--space-md);
  }

  .data-table--pc {
    display: none;
  }

  .card-list--mobile {
    display: block;
  }
}
</style>
