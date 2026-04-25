<template>
  <div class="sc-studio">
    <div class="sc-hero">
      <h1 class="sc-hero-title">{{ content.slogan || teamName }}</h1>
      <p class="sc-hero-desc" v-if="content.description">{{ content.description }}</p>
    </div>
    <div class="sc-section" v-if="capabilities && capabilities.length">
      <h2 class="sc-section-title">服务范围</h2>
      <div class="sc-cap-grid">
        <div class="sc-cap-card" v-for="(c, i) in capabilities" :key="i">
          <div class="sc-cap-icon">✨</div>
          <div class="sc-cap-title">{{ c.title }}</div>
          <div class="sc-cap-desc" v-if="c.desc">{{ c.desc }}</div>
        </div>
      </div>
    </div>
    <div class="sc-section sc-section-alt" v-if="cases && cases.length">
      <h2 class="sc-section-title">精选作品</h2>
      <div class="sc-cases-grid">
        <div class="sc-case-card" v-for="(c, i) in cases" :key="i">
          <span class="sc-case-tag" v-if="c.industry">{{ c.industry }}</span>
          <div class="sc-case-title">{{ c.title }}</div>
          <div class="sc-case-desc" v-if="c.desc">{{ c.desc }}</div>
        </div>
      </div>
    </div>
    <div class="sc-section" v-if="stats && stats.length">
      <h2 class="sc-section-title">为什么选择我们</h2>
      <div class="sc-highlights">
        <div class="sc-highlight" v-for="(s, i) in stats" :key="i">
          <div class="sc-hl-icon">✓</div>
          <div class="sc-hl-label">{{ s.label }}</div>
        </div>
      </div>
    </div>
    <div class="sc-cta-banner">
      <p>💡 准备好开始了吗？</p>
      <button class="sc-cta-btn" @click="$emit('scroll-to-form')">开始需求梳理</button>
    </div>
  </div>
</template>

<script>
export default {
  name: 'ShowcaseStudio',
  props: { content: { type: Object, default: () => ({}) }, teamName: { type: String, default: '' } },
  computed: {
    stats () { return this.content.stats || [] },
    capabilities () { return this.content.capabilities || [] },
    cases () { return this.content.cases || [] }
  }
}
</script>

<style scoped>
.sc-studio { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; color: #1e293b; }
.sc-hero { background: linear-gradient(135deg, #7c3aed 0%, #a78bfa 100%); color: #fff; padding: 60px 24px 48px; text-align: center; }
.sc-hero-title { font-size: 30px; font-weight: 700; margin: 0 0 12px; }
.sc-hero-desc { font-size: 16px; opacity: 0.9; max-width: 560px; margin: 0 auto; line-height: 1.6; }
.sc-section { padding: 40px 24px; max-width: 960px; margin: 0 auto; }
.sc-section-alt { background: #faf5ff; max-width: 100%; padding: 40px 24px; }
.sc-section-alt .sc-cases-grid { max-width: 960px; margin: 0 auto; }
.sc-section-title { font-size: 22px; font-weight: 600; margin: 0 0 24px; text-align: center; color: #7c3aed; }
.sc-cap-grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 16px; }
.sc-cap-card { background: #fff; border: 1px solid #e9d5ff; border-radius: 12px; padding: 24px; text-align: center; }
.sc-cap-icon { font-size: 28px; margin-bottom: 8px; }
.sc-cap-title { font-size: 15px; font-weight: 600; margin-bottom: 4px; }
.sc-cap-desc { font-size: 13px; color: #64748b; }
.sc-cases-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 16px; }
.sc-case-card { background: #fff; border: 1px solid #e9d5ff; border-radius: 12px; padding: 20px; }
.sc-case-tag { display: inline-block; background: #f3e8ff; color: #7c3aed; font-size: 12px; padding: 2px 8px; border-radius: 4px; margin-bottom: 8px; }
.sc-case-title { font-size: 15px; font-weight: 600; margin-bottom: 4px; }
.sc-case-desc { font-size: 13px; color: #64748b; line-height: 1.5; }
.sc-highlights { display: flex; justify-content: center; gap: 32px; flex-wrap: wrap; }
.sc-highlight { text-align: center; }
.sc-hl-icon { width: 40px; height: 40px; background: #f3e8ff; color: #7c3aed; border-radius: 50%; display: flex; align-items: center; justify-content: center; margin: 0 auto 8px; font-weight: 700; }
.sc-hl-label { font-size: 14px; font-weight: 500; }
.sc-cta-banner { background: #f5f3ff; padding: 32px 24px; text-align: center; }
.sc-cta-banner p { margin: 0 0 16px; font-size: 16px; color: #7c3aed; font-weight: 600; }
.sc-cta-btn { background: #7c3aed; color: #fff; border: none; padding: 12px 32px; border-radius: 8px; font-size: 15px; font-weight: 600; cursor: pointer; }
.sc-cta-btn:hover { background: #6d28d9; }
</style>
