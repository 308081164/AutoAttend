<template>
  <div class="sc-enterprise">
    <div class="sc-hero">
      <h1 class="sc-hero-title">{{ content.slogan || teamName }}</h1>
      <p class="sc-hero-desc" v-if="content.description">{{ content.description }}</p>
      <div class="sc-stats" v-if="stats && stats.length">
        <div class="sc-stat" v-for="(s, i) in stats" :key="i">
          <div class="sc-stat-value">{{ s.value }}</div>
          <div class="sc-stat-label">{{ s.label }}</div>
        </div>
      </div>
    </div>
    <div class="sc-section" v-if="capabilities && capabilities.length">
      <h2 class="sc-section-title">核心能力</h2>
      <div class="sc-cap-grid">
        <div class="sc-cap-card" v-for="(c, i) in capabilities" :key="i">
          <div class="sc-cap-icon">⚡</div>
          <div class="sc-cap-title">{{ c.title }}</div>
          <div class="sc-cap-desc" v-if="c.desc">{{ c.desc }}</div>
        </div>
      </div>
    </div>
    <div class="sc-section sc-section-alt" v-if="cases && cases.length">
      <h2 class="sc-section-title">成功案例</h2>
      <div class="sc-cases-grid">
        <div class="sc-case-card" v-for="(c, i) in cases" :key="i">
          <span class="sc-case-tag" v-if="c.industry">{{ c.industry }}</span>
          <div class="sc-case-title">{{ c.title }}</div>
          <div class="sc-case-desc" v-if="c.desc">{{ c.desc }}</div>
        </div>
      </div>
    </div>
    <div class="sc-section" v-if="techStack && techStack.length">
      <h2 class="sc-section-title">技术栈</h2>
      <div class="sc-tech-tags">
        <span class="sc-tech-tag" v-for="(t, i) in techStack" :key="i">{{ t }}</span>
      </div>
    </div>
    <div class="sc-cta-banner">
      <p>💡 了解了我们的实力？</p>
      <p class="sc-cta-sub">立即开始需求梳理，AI 帮您智能拆解项目 →</p>
      <button class="sc-cta-btn" @click="$emit('scroll-to-form')">开始需求梳理</button>
    </div>
  </div>
</template>

<script>
export default {
  name: 'ShowcaseEnterprise',
  props: {
    content: { type: Object, default: () => ({}) },
    teamName: { type: String, default: '' }
  },
  computed: {
    stats () { return this.content.stats || [] },
    capabilities () { return this.content.capabilities || [] },
    cases () { return this.content.cases || [] },
    techStack () { return this.content.techStack || [] }
  }
}
</script>

<style scoped>
.sc-enterprise { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; color: #1e293b; }
.sc-hero { background: linear-gradient(135deg, #1e40af 0%, #3b82f6 100%); color: #fff; padding: 60px 24px 48px; text-align: center; }
.sc-hero-title { font-size: 32px; font-weight: 700; margin: 0 0 12px; }
.sc-hero-desc { font-size: 16px; opacity: 0.9; max-width: 600px; margin: 0 auto 32px; line-height: 1.6; }
.sc-stats { display: flex; justify-content: center; gap: 32px; max-width: 600px; margin: 0 auto; }
.sc-stat { text-align: center; }
.sc-stat-value { font-size: 28px; font-weight: 700; }
.sc-stat-label { font-size: 13px; opacity: 0.8; margin-top: 4px; }
.sc-section { padding: 40px 24px; max-width: 960px; margin: 0 auto; }
.sc-section-alt { background: #f8fafc; max-width: 100%; padding: 40px 24px; }
.sc-section-alt .sc-cases-grid { max-width: 960px; margin: 0 auto; }
.sc-section-title { font-size: 22px; font-weight: 600; margin: 0 0 24px; text-align: center; color: #1e40af; }
.sc-cap-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 16px; }
.sc-cap-card { background: #fff; border: 1px solid #e2e8f0; border-radius: 12px; padding: 24px; text-align: center; }
.sc-cap-icon { font-size: 28px; margin-bottom: 8px; }
.sc-cap-title { font-size: 15px; font-weight: 600; margin-bottom: 4px; }
.sc-cap-desc { font-size: 13px; color: #64748b; }
.sc-cases-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 16px; }
.sc-case-card { background: #fff; border: 1px solid #e2e8f0; border-radius: 12px; padding: 20px; }
.sc-case-tag { display: inline-block; background: #dbeafe; color: #1e40af; font-size: 12px; padding: 2px 8px; border-radius: 4px; margin-bottom: 8px; }
.sc-case-title { font-size: 15px; font-weight: 600; margin-bottom: 4px; }
.sc-case-desc { font-size: 13px; color: #64748b; line-height: 1.5; }
.sc-tech-tags { display: flex; flex-wrap: wrap; gap: 8px; justify-content: center; }
.sc-tech-tag { background: #eff6ff; color: #1e40af; font-size: 13px; padding: 6px 14px; border-radius: 20px; border: 1px solid #bfdbfe; }
.sc-cta-banner { background: #eff6ff; padding: 32px 24px; text-align: center; }
.sc-cta-banner p { margin: 0 0 8px; font-size: 16px; color: #1e40af; font-weight: 600; }
.sc-cta-sub { font-size: 14px; color: #3b82f6; font-weight: 400 !important; margin-bottom: 16px !important; }
.sc-cta-btn { background: #1e40af; color: #fff; border: none; padding: 12px 32px; border-radius: 8px; font-size: 15px; font-weight: 600; cursor: pointer; }
.sc-cta-btn:hover { background: #1e3a8a; }
</style>
