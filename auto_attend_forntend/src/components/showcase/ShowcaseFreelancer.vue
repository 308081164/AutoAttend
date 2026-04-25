<template>
  <div class="sc-freelancer">
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
      <div class="sc-skill-tags">
        <span class="sc-skill-tag" v-for="(t, i) in techStack" :key="i">{{ t }}</span>
      </div>
    </div>
    <div class="sc-section sc-section-alt" v-if="capabilities && capabilities.length">
      <h2 class="sc-section-title">服务项目</h2>
      <div class="sc-services">
        <div class="sc-service-card" v-for="(c, i) in capabilities" :key="i">
          <div class="sc-service-title">{{ c.title }}</div>
          <div class="sc-service-desc" v-if="c.desc">{{ c.desc }}</div>
        </div>
      </div>
    </div>
    <div class="sc-section" v-if="cases && cases.length">
      <h2 class="sc-section-title">客户评价</h2>
      <div class="sc-reviews">
        <div class="sc-review-card" v-for="(c, i) in cases" :key="i">
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
  name: 'ShowcaseFreelancer',
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
.sc-freelancer { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; color: #1e293b; }
.sc-hero { background: linear-gradient(135deg, #0d9488 0%, #14b8a6 100%); color: #fff; padding: 60px 24px 48px; text-align: center; }
.sc-greeting { font-size: 16px; opacity: 0.9; margin: 0 0 8px; }
.sc-hero-title { font-size: 28px; font-weight: 700; margin: 0 0 12px; }
.sc-hero-desc { font-size: 15px; opacity: 0.9; max-width: 560px; margin: 0 auto; line-height: 1.6; }
.sc-stats-bar { display: flex; justify-content: center; gap: 0; background: #fff; border-bottom: 1px solid #e2e8f0; }
.sc-stat { padding: 16px 32px; text-align: center; border-right: 1px solid #e2e8f0; }
.sc-stat:last-child { border-right: none; }
.sc-stat-value { display: block; font-size: 20px; font-weight: 700; color: #0d9488; }
.sc-stat-label { display: block; font-size: 12px; color: #64748b; margin-top: 2px; }
.sc-section { padding: 40px 24px; max-width: 720px; margin: 0 auto; }
.sc-section-alt { background: #f0fdfa; max-width: 100%; padding: 40px 24px; }
.sc-section-alt .sc-services { max-width: 720px; margin: 0 auto; }
.sc-section-title { font-size: 20px; font-weight: 600; margin: 0 0 20px; text-align: center; color: #0d9488; }
.sc-skill-tags { display: flex; flex-wrap: wrap; gap: 8px; justify-content: center; }
.sc-skill-tag { background: #f0fdfa; color: #0d9488; font-size: 13px; padding: 6px 14px; border-radius: 20px; border: 1px solid #99f6e4; }
.sc-services { display: flex; flex-direction: column; gap: 12px; }
.sc-service-card { background: #fff; border: 1px solid #ccfbf1; border-radius: 12px; padding: 20px; }
.sc-service-title { font-size: 15px; font-weight: 600; color: #0d9488; }
.sc-service-desc { font-size: 13px; color: #64748b; margin-top: 4px; }
.sc-reviews { display: flex; flex-direction: column; gap: 12px; }
.sc-review-card { background: #f0fdfa; border-radius: 12px; padding: 16px; }
.sc-review-stars { color: #f59e0b; font-size: 14px; margin-bottom: 4px; }
.sc-review-title { font-size: 14px; font-weight: 600; margin-bottom: 4px; }
.sc-review-desc { font-size: 13px; color: #64748b; line-height: 1.5; }
.sc-cta-banner { background: #f0fdfa; padding: 32px 24px; text-align: center; }
.sc-cta-banner p { margin: 0 0 16px; font-size: 16px; color: #0d9488; font-weight: 600; }
.sc-cta-btn { background: #0d9488; color: #fff; border: none; padding: 12px 32px; border-radius: 8px; font-size: 15px; font-weight: 600; cursor: pointer; }
.sc-cta-btn:hover { background: #0f766e; }
</style>
