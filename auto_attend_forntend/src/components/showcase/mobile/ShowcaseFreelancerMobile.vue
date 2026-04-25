<template>
  <div class="sc-fre-m">
    <div class="sc-hero">
      <p class="sc-greeting">你好，我是 {{ content.teamName || teamName }}</p>
      <h1 class="sc-hero-title">{{ content.slogan || '全栈独立开发者' }}</h1>
      <p class="sc-hero-desc" v-if="content.description">{{ content.description }}</p>
    </div>
    <div class="sc-stats-bar" v-if="stats && stats.length">
      <div class="sc-stat" v-for="(s, i) in stats" :key="i">
        <span class="sc-stat-value">{{ s.value }}</span>
        <span class="sc-stat-label">{{ s.label }}</span>
      </div>
    </div>
    <div class="sc-section" v-if="techStack && techStack.length">
      <h2 class="sc-section-title">专业技能</h2>
      <div class="sc-tags">
        <span class="sc-tag" v-for="(t, i) in techStack" :key="i">{{ t }}</span>
      </div>
    </div>
    <div class="sc-section sc-section-alt" v-if="capabilities && capabilities.length">
      <h2 class="sc-section-title">服务项目</h2>
      <div class="sc-services">
        <div class="sc-svc" v-for="(c, i) in capabilities" :key="i">
          <div class="sc-svc-title">{{ c.title }}</div>
          <div class="sc-svc-desc" v-if="c.desc">{{ c.desc }}</div>
        </div>
      </div>
    </div>
    <div class="sc-section" v-if="cases && cases.length">
      <h2 class="sc-section-title">客户评价</h2>
      <div class="sc-reviews">
        <div class="sc-review" v-for="(c, i) in cases" :key="i">
          <div class="sc-review-stars">★★★★★</div>
          <div class="sc-review-title">{{ c.title }}</div>
          <div class="sc-review-desc" v-if="c.desc">{{ c.desc }}</div>
        </div>
      </div>
    </div>
    <div class="sc-cta-banner">
      <p>💡 有项目想聊聊？</p>
      <button class="sc-cta-btn" @click="$emit('scroll-to-form')">开始需求梳理</button>
    </div>
  </div>
</template>

<script>
export default {
  name: 'ShowcaseFreelancerMobile',
  props: { content: { type: Object, default: () => ({}) }, teamName: { type: String, default: '' } },
  computed: {
    stats () { return this.content.stats || [] },
    capabilities () { return this.content.capabilities || [] },
    cases () { return this.content.cases || [] },
    techStack () { return this.content.techStack || [] }
  }
}
</script>

<style scoped>
.sc-fre-m { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; color: #1e293b; }
.sc-hero { background: linear-gradient(135deg, #0d9488 0%, #14b8a6 100%); color: #fff; padding: 40px 16px 32px; text-align: center; }
.sc-greeting { font-size: 14px; opacity: 0.9; margin: 0 0 4px; }
.sc-hero-title { font-size: 22px; font-weight: 700; margin: 0 0 8px; }
.sc-hero-desc { font-size: 14px; opacity: 0.9; line-height: 1.5; }
.sc-stats-bar { display: flex; background: #fff; border-bottom: 1px solid #e2e8f0; }
.sc-stat { flex: 1; text-align: center; padding: 14px 8px; border-right: 1px solid #e2e8f0; }
.sc-stat:last-child { border-right: none; }
.sc-stat-value { display: block; font-size: 17px; font-weight: 700; color: #0d9488; }
.sc-stat-label { display: block; font-size: 11px; color: #64748b; margin-top: 2px; }
.sc-section { padding: 24px 16px; }
.sc-section-alt { background: #f0fdfa; }
.sc-section-title { font-size: 17px; font-weight: 600; margin: 0 0 14px; color: #0d9488; }
.sc-tags { display: flex; flex-wrap: wrap; gap: 6px; }
.sc-tag { background: #f0fdfa; color: #0d9488; font-size: 12px; padding: 5px 10px; border-radius: 16px; border: 1px solid #99f6e4; }
.sc-services { display: flex; flex-direction: column; gap: 10px; }
.sc-svc { background: #fff; border: 1px solid #ccfbf1; border-radius: 10px; padding: 14px; }
.sc-svc-title { font-size: 14px; font-weight: 600; color: #0d9488; }
.sc-svc-desc { font-size: 12px; color: #64748b; margin-top: 2px; }
.sc-reviews { display: flex; flex-direction: column; gap: 10px; }
.sc-review { background: #f0fdfa; border-radius: 10px; padding: 14px; }
.sc-review-stars { color: #f59e0b; font-size: 13px; margin-bottom: 4px; }
.sc-review-title { font-size: 13px; font-weight: 600; margin-bottom: 2px; }
.sc-review-desc { font-size: 12px; color: #64748b; line-height: 1.4; }
.sc-cta-banner { background: #f0fdfa; padding: 24px 16px; text-align: center; }
.sc-cta-banner p { margin: 0 0 12px; font-size: 14px; color: #0d9488; font-weight: 600; }
.sc-cta-btn { background: #0d9488; color: #fff; border: none; padding: 12px 24px; border-radius: 8px; font-size: 15px; font-weight: 600; cursor: pointer; width: 100%; min-height: 44px; }
</style>
