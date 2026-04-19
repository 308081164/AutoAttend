<template>
  <div class="mp-wrap">
    <router-link to="/marketplace" class="back">{{ $t('marketplace.backList') }}</router-link>
    <div v-if="loading" class="placeholder">{{ $t('marketplace.loading') }}</div>
    <div v-else-if="!detail" class="placeholder">{{ $t('marketplace.notFound') }}</div>
    <article v-else class="article">
      <p class="banner-warn">{{ $t('marketplace.detailBanner') }}</p>
      <h1>{{ detail.title }}</h1>
      <div class="meta">
        <span>{{ $t('marketplace.publisher') }}：{{ detail.publisherDisplayName }}</span>
        <span>{{ $t('marketplace.statusLabel') }}：{{ detail.status }}</span>
        <span v-if="detail.publishTime">{{ formatTime(detail.publishTime) }}</span>
      </div>
      <section class="body-md" v-html="renderedDescription"></section>
      <div v-if="detail.requirementImageUrls && detail.requirementImageUrls.length" class="req-gallery">
        <img v-for="(u, gi) in detail.requirementImageUrls" :key="gi" :src="u" alt="" class="req-img">
      </div>
      <ul v-if="detail.techStack && detail.techStack.length" class="tags">
        <li v-for="(t, i) in detail.techStack" :key="i">{{ t }}</li>
      </ul>
      <dl class="facts">
        <dt>{{ $t('marketplace.fieldBudget') }}</dt><dd>{{ detail.budgetRange || '—' }}</dd>
        <dt>{{ $t('marketplace.fieldDuration') }}</dt><dd>{{ detail.duration || '—' }}</dd>
        <dt>{{ $t('marketplace.fieldLocation') }}</dt><dd>{{ detail.location || '—' }}</dd>
        <dt>{{ $t('marketplace.fieldValidity') }}</dt><dd>{{ validityText }}</dd>
        <dt>{{ $t('marketplace.fieldContactType') }}</dt><dd>{{ contactTypeLabel }}</dd>
        <dt>{{ $t('marketplace.fieldContact') }}</dt><dd><code>{{ detail.contactValue || '—' }}</code></dd>
        <dt v-if="detail.contactAttachmentUrl">{{ $t('marketplace.fieldContactQr') }}</dt>
        <dd v-if="detail.contactAttachmentUrl" class="qr-cell"><img :src="detail.contactAttachmentUrl" alt="" class="qr-img"></dd>
      </dl>
      <div v-if="canPublish && isMine && detail.status === 'open'" class="actions">
        <button type="button" class="secondary-button" @click="closeProject">{{ $t('marketplace.closeProject') }}</button>
      </div>
    </article>
  </div>
</template>

<script>
import MarkdownIt from 'markdown-it'

const md = new MarkdownIt({ html: false, linkify: true, breaks: true })

export default {
  name: 'MarketplaceDetailView',
  data () {
    return {
      loading: true,
      detail: null,
      canPublish: false,
      myUserId: null
    }
  },
  computed: {
    renderedDescription () {
      const raw = this.detail && this.detail.description
      if (!raw) return ''
      try {
        return md.render(String(raw))
      } catch (e) {
        return '<p>' + String(raw) + '</p>'
      }
    },
    isMine () {
      return this.detail && this.myUserId && this.detail.publisherAdminUserId === this.myUserId
    },
    contactTypeLabel () {
      const t = this.detail && this.detail.contactType
      const m = {
        phone: this.$t('marketplace.contactPhone'),
        email: this.$t('marketplace.contactEmail'),
        internal: this.$t('marketplace.contactInternal'),
        wechat: this.$t('marketplace.contactWechat'),
        qq: this.$t('marketplace.contactQq'),
        feishu: this.$t('marketplace.contactFeishu')
      }
      return m[t] || t || '—'
    },
    validityText () {
      const d = this.detail
      if (!d) return '—'
      if (d.effectiveNeverExpires) return this.$t('marketplace.longTerm')
      if (d.expireTime) return this.formatTime(d.expireTime)
      return '—'
    }
  },
  async mounted () {
    await this.loadMe()
    await this.loadDetail()
  },
  methods: {
    async loadMe () {
      try {
        const r = await this.$http.get('/admin/auth/me')
        if (r.data && r.data.code === 0 && r.data.data) {
          this.myUserId = r.data.data.userId
          this.canPublish = !!r.data.data.canPublishProjectInfo
        }
      } catch (e) { /* ignore */ }
    },
    async loadDetail () {
      this.loading = true
      const id = this.$route.params.id
      try {
        const r = await this.$http.get('/admin/marketplace/projects/' + id)
        if (r.data && r.data.code === 0) {
          this.detail = r.data.data
        } else {
          this.detail = null
        }
      } catch (e) {
        this.detail = null
      } finally {
        this.loading = false
      }
    },
    formatTime (v) {
      if (!v) return ''
      return String(v).replace('T', ' ').slice(0, 19)
    },
    async closeProject () {
      if (!this.detail) return
      if (!window.confirm(this.$t('marketplace.confirmClose'))) return
      try {
        const r = await this.$http.post('/admin/marketplace/projects/' + this.detail.projectId + '/close')
        if (r.data && r.data.code === 0) {
          await this.loadDetail()
        } else {
          alert(r.data.message || 'Error')
        }
      } catch (e) {
        alert('Error')
      }
    }
  }
}
</script>

<style scoped>
.mp-wrap { max-width: 800px; margin: 0 auto; padding: var(--space-xl); min-height: 100vh; }
.back { display: inline-block; margin-bottom: 16px; font-size: 14px; }
.banner-warn { background: #fff8e6; border: 1px solid #f0e0b0; padding: 10px 12px; border-radius: 8px; font-size: 13px; margin: 0 0 16px; }
.article h1 { font-size: 24px; margin: 0 0 12px; }
.meta { font-size: 13px; color: var(--text-secondary); display: flex; flex-wrap: wrap; gap: 12px; margin-bottom: 16px; }
.body-md { font-size: 15px; line-height: 1.65; margin-bottom: 16px; }
.req-gallery { display: flex; flex-wrap: wrap; gap: 10px; margin-bottom: 16px; }
.req-img { max-width: 100%; max-height: 240px; border-radius: 8px; border: 1px solid var(--border-primary); object-fit: contain; }
.tags { list-style: none; padding: 0; margin: 0 0 16px; display: flex; flex-wrap: wrap; gap: 8px; }
.tags li { background: var(--bg-muted); padding: 4px 10px; border-radius: 999px; font-size: 13px; }
.facts { display: grid; grid-template-columns: 140px 1fr; gap: 8px 12px; font-size: 14px; }
.facts dt { color: var(--text-secondary); margin: 0; }
.facts dd { margin: 0; }
.qr-cell { grid-column: 2; }
.qr-img { max-width: 200px; max-height: 200px; border-radius: 8px; border: 1px solid var(--border-primary); }
.actions { margin-top: 24px; }
.secondary-button { padding: 8px 16px; border-radius: 8px; border: 1px solid var(--border-primary); background: transparent; cursor: pointer; }
.placeholder { color: var(--text-secondary); }
</style>
