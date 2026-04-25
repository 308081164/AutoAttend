<template>
  <div class="sc-stu-m">
    <div class="sc-hero">
      <h1 class="sc-hero-title">{{ content.slogan || teamName }}</h1>
      <p class="sc-hero-desc" v-if="content.description">{{ content.description }}</p>
    </div>
    <div class="sc-section" v-if="capabilities && capabilities.length">
      <h2 class="sc-section-title">服务范围</h2>
      <div class="sc-cap-grid">
        <div class="sc-cap-card" v-for="(c, i) in capabilities" :key="i">
          <div class="sc-cap-title">{{ c.title }}</div>
          <div class="sc-cap-desc" v-if="c.desc">{{ c.desc }}</div>
        </div>
      </div>
    </div>
    <div class="sc-section sc-section-alt" v-if="cases && cases.length">
      <h2 class="sc-section-title">精选作品</h2>
      <div class="sc-cases">
        <div class="sc-case-card" v-for="(c, i) in cases" :key="i">
          <span class="sc-case-tag" v-if="c.industry">{{ c.industry }}</span>
          <div class="sc-case-title">{{ c.title }}</div>
        </div>
      </div>
    </div>
    <div class="sc-section" v-if="stats && stats.length">
      <h2 class="sc-section-title">为什么选择我们</h2>
      <div class="sc-highlights">
        <div class="sc-hl" v-for="(s, i) in stats" :key="i">
          <span class="sc-hl-icon">✓</span> {{ s.label }}
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
  name: 'ShowcaseStudioMobile',
  props: { content: { type: Object, default: () => ({}) }, teamName: { type: String, default: '' } },
  computed: {
    stats () { return this.content.stats || [] },
    capabilities () { return this.content.capabilities || [] },
    cases () { return this.content.cases || [] }
  }
}
</script>

<style scoped>
.sc-stu-m { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; color: #1e293b; }
.sc-hero { background: linear-gradient(135deg, #7c3aed 0%, #a78bfa 100%); color: #fff; padding: 40px 16px 32px; text-align: center; }
.sc-hero-title { font-size: 22px; font-weight: 700; margin: 0 0 8px; }
.sc-hero-desc { font-size: 14px; opacity: 0.9; line-height: 1.5; }
.sc-section { padding: 24px 16px; }
.sc-section-alt { background: #faf5ff; }
.sc-section-title { font-size: 17px; font-weight: 600; margin: 0 0 16px; color: #7c3aed; }
.sc-cap-grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 10px; }
.sc-cap-card { background: #fff; border: 1px solid #e9d5ff; border-radius: 10px; padding: 16px 12px; text-align: center; }
.sc-cap-title { font-size: 13px; font-weight: 600; }
.sc-cap-desc { font-size: 12px; color: #64748b; margin-top: 2px; }
.sc-cases { display: flex; flex-direction: column; gap: 10px; }
.sc-case-card { background: #fff; border: 1px solid #e9d5ff; border-radius: 10px; padding: 14px; }
.sc-case-tag { display: inline-block; background: #f3e8ff; color: #7c3aed; font-size: 11px; padding: 2px 6px; border-radius: 4px; margin-bottom: 4px; }
.sc-case-title { font-size: 14px; font-weight: 600; }
.sc-highlights { display: grid; grid-template-columns: repeat(2, 1fr); gap: 10px; }
.sc-hl { display: flex; align-items: center; gap: 8px; font-size: 13px; font-weight: 500; }
.sc-hl-icon { width: 28px; height: 28px; background: #f3e8ff; color: #7c3aed; border-radius: 50%; display: flex; align-items: center; justify-content: center; font-size: 12px; font-weight: 700; flex-shrink: 0; }
.sc-cta-banner { background: #f5f3ff; padding: 24px 16px; text-align: center; }
.sc-cta-banner p { margin: 0 0 12px; font-size: 14px; color: #7c3aed; font-weight: 600; }
.sc-cta-btn { background: #7c3aed; color: #fff; border: none; padding: 12px 24px; border-radius: 8px; font-size: 15px; font-weight: 600; cursor: pointer; width: 100%; min-height: 44px; }
</style>
