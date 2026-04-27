<template>
  <div class="showcase-wrapper" v-if="config && config.enabled && config.mode !== 'off'">
    <!-- 自定义 HTML 模式 -->
    <iframe v-if="config.mode === 'custom_html' && config.customHtml"
            :srcdoc="config.customHtml"
            class="showcase-iframe"
            sandbox="allow-same-origin"
            @load="onIframeLoad"></iframe>
    <!-- 官方模板模式 -->
    <template v-else-if="config.mode === 'template'">
      <component :is="currentTemplate" :content="config.content || {}" :teamName="config.teamName || ''" />
    </template>
  </div>
</template>

<script>
// Vue 2 异步组件：直接使用 import() 返回函数
const desktopTemplates = {
  enterprise: () => import('./ShowcaseEnterprise.vue'),
  studio: () => import('./ShowcaseStudio.vue'),
  freelancer: () => import('./ShowcaseFreelancer.vue')
}

const mobileTemplates = {
  enterprise: () => import('./mobile/ShowcaseEnterpriseMobile.vue'),
  studio: () => import('./mobile/ShowcaseStudioMobile.vue'),
  freelancer: () => import('./mobile/ShowcaseFreelancerMobile.vue')
}

export default {
  name: 'ShowcaseSection',
  props: {
    config: { type: Object, default: null }
  },
  data () {
    return { isMobile: false }
  },
  computed: {
    currentTemplate () {
      const tid = (this.config && this.config.templateId) || 'enterprise'
      const pool = this.isMobile ? mobileTemplates : desktopTemplates
      return pool[tid] || desktopTemplates.enterprise
    }
  },
  methods: {
    checkMobile () {
      this.isMobile = window.innerWidth <= 768
    },
    onIframeLoad () {
      // iframe 加载完成
    }
  },
  mounted () {
    this.checkMobile()
    window.addEventListener('resize', this.checkMobile)
  },
  beforeUnmount () {
    window.removeEventListener('resize', this.checkMobile)
  }
}
</script>

<style scoped>
.showcase-wrapper {
  width: 100%;
}
.showcase-iframe {
  width: 100%;
  min-height: 400px;
  border: none;
  display: block;
}
</style>
