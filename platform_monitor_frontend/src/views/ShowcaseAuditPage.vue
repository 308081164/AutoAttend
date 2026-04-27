<template>
  <div class="showcase-audit-page">
    <div class="page-header">
      <h2>🏢 展示页内容审核</h2>
      <p class="muted">查看所有租户的展示页配置，可内嵌预览实际效果。发现违规内容可一键关停。</p>
    </div>

    <div v-if="loading" class="muted">加载中…</div>
    <template v-else>
      <div v-if="!list.length" class="muted">暂无租户数据</div>

      <div v-for="item in list" :key="item.tenantId" class="tenant-card">
        <div class="tenant-header">
          <div class="tenant-info">
            <strong>{{ item.tenantName }}</strong>
            <span class="slug">@{{ item.slug }}</span>
            <span class="badge" :class="item.status === 'active' ? 'ok' : 'err'">{{ item.status }}</span>
          </div>
          <div class="tenant-actions">
            <span v-if="item.showcaseEnabled && !item.platformDisabled" class="badge ok">展示已开启</span>
            <span v-else-if="item.platformDisabled" class="badge err">平台已关停</span>
            <span v-else class="badge muted">未开启</span>
            <span class="mode-badge">{{ modeLabel(item.mode) }} / {{ templateLabel(item.templateId) }}</span>
            <button v-if="item.platformDisabled" class="btn btn-sm" @click="toggle(item.tenantId, false)">恢复展示</button>
            <button v-else-if="item.showcaseEnabled" class="btn btn-sm btn-danger" @click="toggle(item.tenantId, true)">关停展示</button>
            <button class="btn btn-sm" @click="openPreview(item)">预览</button>
          </div>
        </div>

        <!-- 内嵌预览 -->
        <div v-if="previewingId === item.tenantId" class="preview-wrap">
          <div class="preview-bar">
            <span>{{ item.tenantName }} 的展示页预览</span>
            <button class="btn btn-sm" @click="previewingId = null">关闭预览</button>
          </div>
          <iframe :src="previewUrl(item.slug)" class="preview-frame" sandbox="allow-scripts" />
        </div>
      </div>
    </template>
  </div>
</template>

<script>
import http from '../http'

export default {
  name: 'ShowcaseAuditPage',
  data () {
    return {
      loading: true,
      list: [],
      previewingId: null
    }
  },
  async mounted () {
    await this.load()
  },
  methods: {
    async load () {
      this.loading = true
      try {
        const { data } = await http.get('/platform/settings/showcase/audit-list')
        if (data.code === 0 && data.data) {
          this.list = data.data
        }
      } catch (e) { void e }
      finally { this.loading = false }
    },
    async toggle (tenantId, disable) {
      try {
        const url = disable
          ? '/platform/settings/showcase/' + tenantId + '/disable'
          : '/platform/settings/showcase/' + tenantId + '/enable'
        const { data } = await http.put(url)
        if (data.code === 0) {
          await this.load()
        } else {
          alert((data && data.message) || '操作失败')
        }
      } catch (e) {
        alert('请求失败')
      }
    },
    openPreview (item) {
      this.previewingId = this.previewingId === item.tenantId ? null : item.tenantId
    },
    previewUrl (slug) {
      return window.location.origin.replace(/\/monitor$/, '') + '/quick-quote/' + encodeURIComponent(slug)
    },
    modeLabel (mode) {
      if (mode === 'template') return '模板模式'
      if (mode === 'custom_html') return '自定义HTML'
      return mode || '未配置'
    },
    templateLabel (id) {
      const map = { enterprise: '企业版', studio: '工作室版', freelancer: '自由职业版' }
      return map[id] || id || '-'
    }
  }
}
</script>

<style scoped>
.showcase-audit-page {
  max-width: 960px;
  margin: 0 auto;
  padding: 24px 16px;
}
.page-header h2 {
  margin: 0 0 4px;
}
.muted {
  color: #888;
}
.tenant-card {
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  margin-bottom: 16px;
  overflow: hidden;
}
.tenant-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  background: #fafafa;
  flex-wrap: wrap;
  gap: 8px;
}
.tenant-info {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}
.slug {
  color: #888;
  font-size: 13px;
}
.badge {
  display: inline-block;
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 600;
}
.badge.ok {
  background: #e8f5e9;
  color: #2e7d32;
}
.badge.err {
  background: #ffebee;
  color: #c62828;
}
.badge.muted {
  background: #f5f5f5;
  color: #999;
}
.mode-badge {
  font-size: 12px;
  color: #666;
  background: #eee;
  padding: 2px 8px;
  border-radius: 4px;
}
.tenant-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}
.btn {
  padding: 6px 14px;
  border: 1px solid #d0d0d0;
  border-radius: 6px;
  cursor: pointer;
  font-size: 13px;
  background: #fff;
}
.btn:hover {
  background: #f5f5f5;
}
.btn-sm {
  padding: 4px 10px;
  font-size: 12px;
}
.btn-danger {
  background: #ffebee;
  border-color: #ef9a9a;
  color: #c62828;
}
.btn-danger:hover {
  background: #ffcdd2;
}
.preview-wrap {
  border-top: 1px solid #e0e0e0;
}
.preview-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 16px;
  background: #f5f5f5;
  font-size: 13px;
}
.preview-frame {
  width: 100%;
  height: 600px;
  border: none;
}
</style>
