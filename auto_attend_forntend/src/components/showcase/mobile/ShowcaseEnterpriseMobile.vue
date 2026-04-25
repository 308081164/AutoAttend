<template>
  <div class="sc-ent-m">
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
          <div class="sc-cap-title">{{ c.title }}</div>
        </div>
      </div>
    </div>
    <div class="sc-section sc-section-alt" v-if="cases && cases.length">
      <h2 class="sc-section-title">成功案例</h2>
      <div class="sc-cases">
        <div class="sc-case-card" v-for="(c, i) in cases" :key="i">
          <span class="sc-case-tag" v-if="c.industry">{{ c.industry }}</span>
          <div class="sc-case-title">{{ c.title }}</div>
          <div class="sc-case-desc" v-if="c.desc">{{ c.desc }}</div>
        </div>
      </div>
    </div>
    <div class="sc-cta-banner">
      <p>💡 了解了？立即需求梳理 →</p>
      <button class="sc-cta-btn" @click="$emit('scroll-to-form')">开始需求梳理</button>
    </div>
  </div>
</template>

<script>
export default {
  name: 'ShowcaseEnterpriseMobile',
  props: { content: { type: Object, default: () => ({}) }, teamName: { type: String, default: '' } },
  computed: {
    stats () { return this.content.stats || [] },
    capabilities () { return this.content.capabilities || [] },
    cases () { return this.content.cases || [] }
  }
}
</script>

<style scoped>
.sc-ent-m { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; color: #1e293b; }
.sc-hero { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: #fff; padding: 40px 16px 32px; text-align: center; }
.sc-hero-title { font-size: 22px; font-weight: 700; margin: 0 0 8px; }
.sc-hero-desc { font-size: 14px; opacity: 0.9; margin: 0 auto 24px; line-height: 1.5; }
.sc-stats { display: flex; gap: 8px; }
.sc-stat { flex: 1; text-align: center; background: rgba(255,255,255,0.15); border-radius: 8px; padding: 12px 4px; }
.sc-stat-value { font-size: 18px; font-weight: 700; }
.sc-stat-label { font-size: 11px; opacity: 0.8; margin-top: 2px; }
.sc-section { padding: 24px 16px; }
.sc-section-alt { background: #f8fafc; }
.sc-section-title { font-size: 17px; font-weight: 600; margin: 0 0 16px; color: #667eea; }
.sc-cap-grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 10px; }
.sc-cap-card { background: #fff; border: 1px solid #e2e8f0; border-radius: 10px; padding: 16px 12px; text-align: center; }
.sc-cap-title { font-size: 13px; font-weight: 600; }
.sc-cases { display: flex; flex-direction: column; gap: 10px; }
.sc-case-card { background: #fff; border: 1px solid #e2e8f0; border-radius: 10px; padding: 14px; }
.sc-case-tag { display: inline-block; background: #dbeafe; color: #1e40af; font-size: 11px; padding: 2px 6px; border-radius: 4px; margin-bottom: 6px; }
.sc-case-title { font-size: 14px; font-weight: 600; margin-bottom: 2px; }
.sc-case-desc { font-size: 12px; color: #64748b; }
.sc-cta-banner { background: #f0f0ff; padding: 24px 16px; text-align: center; }
.sc-cta-banner p { margin: 0 0 12px; font-size: 14px; color: #667eea; font-weight: 600; }
.sc-cta-btn { background: #667eea; color: #fff; border: none; padding: 12px 24px; border-radius: 8px; font-size: 15px; font-weight: 600; cursor: pointer; width: 100%; min-height: 44px; }
</style>
