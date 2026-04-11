<template>
  <div class="landing">
    <header class="landing-header">
      <div class="landing-brand">
        <span class="landing-logo" aria-hidden="true">{{ $t('app.title').charAt(0) }}</span>
        <div class="landing-brand-text">
          <strong>{{ $t('app.title') }}</strong>
          <span>{{ $t('app.slogan') }}</span>
        </div>
      </div>
      <button type="button" class="landing-menu-btn" aria-label="Menu" @click="mobileNavOpen = !mobileNavOpen">
        {{ mobileNavOpen ? '×' : '☰' }}
      </button>
      <nav class="landing-nav landing-nav-desktop" aria-label="primary">
        <a href="#features" @click="closeMobileNav">{{ $t('landing.navFeatures') }}</a>
        <a href="#workflow" @click="closeMobileNav">{{ $t('landing.navWorkflow') }}</a>
        <a href="#cta" @click="closeMobileNav">{{ $t('landing.navCta') }}</a>
      </nav>
      <div class="landing-actions landing-nav-desktop">
        <select v-model="locale" class="landing-lang" @change="onLocaleChange">
          <option v-for="opt in localeOptions" :key="opt.value" :value="opt.value">{{ opt.label }}</option>
        </select>
        <router-link class="btn btn-ghost" :to="{ name: 'login' }">{{ $t('landing.login') }}</router-link>
        <router-link class="btn btn-primary" :to="{ name: 'register' }">{{ $t('landing.register') }}</router-link>
      </div>
    </header>

    <div v-show="mobileNavOpen" class="landing-mobile-panel">
      <nav class="landing-nav-mobile" aria-label="mobile">
        <a href="#features" @click="closeMobileNav">{{ $t('landing.navFeatures') }}</a>
        <a href="#workflow" @click="closeMobileNav">{{ $t('landing.navWorkflow') }}</a>
        <a href="#cta" @click="closeMobileNav">{{ $t('landing.navCta') }}</a>
        <select v-model="locale" class="landing-lang" @change="onLocaleChange">
          <option v-for="opt in localeOptions" :key="'m-' + opt.value" :value="opt.value">{{ opt.label }}</option>
        </select>
        <router-link class="btn btn-ghost" :to="{ name: 'login' }" @click.native="closeMobileNav">{{ $t('landing.login') }}</router-link>
        <router-link class="btn btn-primary" :to="{ name: 'register' }" @click.native="closeMobileNav">{{ $t('landing.register') }}</router-link>
      </nav>
    </div>

    <main class="landing-main">
      <section class="hero">
        <div class="hero-copy">
          <p class="hero-eyebrow">{{ $t('landing.heroEyebrow') }}</p>
          <h1>{{ $t('landing.heroTitle') }}</h1>
          <p class="hero-lead">{{ $t('landing.heroLead') }}</p>
          <div class="hero-cta">
            <router-link class="btn btn-primary btn-lg" :to="{ name: 'register' }">{{ $t('landing.ctaPrimary') }}</router-link>
            <router-link class="btn btn-ghost btn-lg" :to="{ name: 'login' }">{{ $t('landing.ctaSecondary') }}</router-link>
          </div>
          <p class="hero-trust">{{ $t('landing.trustLine') }}</p>
        </div>
        <div class="hero-visual" aria-hidden="true">
          <div class="flow-row">
            <span class="flow-node">{{ $t('landing.visualFlow1') }}</span>
            <span class="flow-arrow">→</span>
            <span class="flow-node">{{ $t('landing.visualFlow2') }}</span>
            <span class="flow-arrow">→</span>
            <span class="flow-node">{{ $t('landing.visualFlow3') }}</span>
            <span class="flow-arrow">→</span>
            <span class="flow-node">{{ $t('landing.visualFlow4') }}</span>
          </div>
          <div class="hero-chart">
            <div class="chart-label">{{ $t('landing.chartHint') }}</div>
            <div class="chart-bars">
              <span v-for="i in 5" :key="i" :style="{ animationDelay: (i * 0.05) + 's' }" />
            </div>
          </div>
        </div>
      </section>

      <section id="features" class="section">
        <h2 class="section-title">{{ $t('landing.featuresTitle') }}</h2>
        <p class="section-desc">{{ $t('landing.featuresSubtitle') }}</p>
        <div class="feature-grid">
          <article v-for="card in featureCards" :key="card.titleKey" class="feature-card">
            <div class="feature-icon" aria-hidden="true">{{ card.icon }}</div>
            <h3>{{ $t(card.titleKey) }}</h3>
            <p>{{ $t(card.descKey) }}</p>
            <router-link v-if="card.route" class="card-link" :to="card.route">{{ $t('landing.cardAction') }} →</router-link>
          </article>
        </div>
      </section>

      <section id="workflow" class="section steps-wrap">
        <h2 class="section-title">{{ $t('landing.stepsTitle') }}</h2>
        <div class="steps">
          <div v-for="(s, idx) in stepItems" :key="idx" class="step">
            <div class="step-num">{{ idx + 1 }}</div>
            <h4>{{ $t(s.titleKey) }}</h4>
            <p>{{ $t(s.descKey) }}</p>
          </div>
        </div>
      </section>

      <section id="cta" class="section cta-block">
        <h2 class="section-title">{{ $t('landing.finalCtaTitle') }}</h2>
        <p class="section-desc">{{ $t('landing.finalCtaDesc') }}</p>
        <div class="hero-cta cta-center">
          <router-link class="btn btn-primary btn-lg" :to="{ name: 'register' }">{{ $t('landing.finalCtaRegister') }}</router-link>
          <router-link class="btn btn-ghost btn-lg" :to="{ name: 'login' }">{{ $t('landing.finalCtaLogin') }}</router-link>
        </div>
      </section>
    </main>

    <footer class="landing-footer">
      <p>{{ $t('landing.footerNote') }}</p>
    </footer>
  </div>
</template>

<script>
import { localeOptions, setLocale } from '../locales'

export default {
  name: 'LandingView',
  data () {
    return {
      localeOptions,
      locale: this.$i18n.locale,
      mobileNavOpen: false,
      featureCards: [
        { icon: '◉', titleKey: 'landing.featConsoleTitle', descKey: 'landing.featConsoleDesc', route: { name: 'dashboard' } },
        { icon: '▦', titleKey: 'landing.featCollabTitle', descKey: 'landing.featCollabDesc', route: { name: 'collab-projects' } },
        { icon: '¥', titleKey: 'landing.featQuoteTitle', descKey: 'landing.featQuoteDesc', route: { name: 'quote-list' } },
        { icon: 'AI', titleKey: 'landing.featAiTitle', descKey: 'landing.featAiDesc', route: { name: 'api-config' } },
        { icon: '◇', titleKey: 'landing.featProtoTitle', descKey: 'landing.featProtoDesc', route: { name: 'prototype-list' } },
        { icon: '◎', titleKey: 'landing.featTeamTitle', descKey: 'landing.featTeamDesc', route: { name: 'team-manage' } }
      ],
      stepItems: [
        { titleKey: 'landing.step1Title', descKey: 'landing.step1Desc' },
        { titleKey: 'landing.step2Title', descKey: 'landing.step2Desc' },
        { titleKey: 'landing.step3Title', descKey: 'landing.step3Desc' },
        { titleKey: 'landing.step4Title', descKey: 'landing.step4Desc' }
      ]
    }
  },
  methods: {
    onLocaleChange () {
      setLocale(this.locale)
    },
    closeMobileNav () {
      this.mobileNavOpen = false
    }
  }
}
</script>

<style scoped>
.landing {
  min-height: 100vh;
  background: #f7f8fa;
  color: #1f2329;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', 'Noto Sans SC', sans-serif;
}

.landing-header {
  position: sticky;
  top: 0;
  z-index: 40;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  min-height: 64px;
  padding: 0 24px;
  background: #fff;
  border-bottom: 1px solid #dee0e3;
}

.landing-brand {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 0;
}

.landing-logo {
  width: 36px;
  height: 36px;
  border-radius: 10px;
  background: linear-gradient(135deg, #3370ff 0%, #5b9cff 100%);
  color: #fff;
  font-weight: 700;
  font-size: 15px;
  display: grid;
  place-items: center;
  flex-shrink: 0;
}

.landing-brand-text {
  display: flex;
  flex-direction: column;
  gap: 0;
  min-width: 0;
}

.landing-brand-text strong {
  font-size: 16px;
  font-weight: 600;
}

.landing-brand-text span {
  font-size: 12px;
  color: #646a73;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.landing-nav {
  display: flex;
  align-items: center;
  gap: 8px;
}

.landing-nav a {
  padding: 8px 14px;
  border-radius: 8px;
  font-size: 14px;
  color: #646a73;
  text-decoration: none;
}

.landing-nav a:hover {
  background: #f7f8fa;
  color: #1f2329;
}

.landing-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.landing-lang {
  padding: 6px 10px;
  border: 1px solid #dee0e3;
  border-radius: 8px;
  font-size: 13px;
  background: #fff;
  color: #1f2329;
  cursor: pointer;
}

.btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 0 16px;
  height: 38px;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  text-decoration: none;
  border: none;
  cursor: pointer;
  transition: background 0.15s, transform 0.1s;
}

.btn:active {
  transform: scale(0.98);
}

.btn-lg {
  height: 42px;
  padding: 0 20px;
}

.btn-ghost {
  background: #fff;
  color: #1f2329;
  border: 1px solid #dee0e3;
}

.btn-ghost:hover {
  background: #f7f8fa;
}

.btn-primary {
  background: #3370ff;
  color: #fff;
}

.btn-primary:hover {
  background: #2860e1;
}

.landing-menu-btn {
  display: none;
  width: 40px;
  height: 40px;
  border: 1px solid #dee0e3;
  border-radius: 8px;
  background: #fff;
  font-size: 18px;
  cursor: pointer;
}

.landing-mobile-panel {
  display: none;
  border-bottom: 1px solid #dee0e3;
  background: #fff;
  padding: 16px;
}

.landing-nav-mobile {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.landing-nav-mobile a {
  color: #646a73;
  text-decoration: none;
  padding: 8px 0;
}

.landing-main {
  max-width: 1120px;
  margin: 0 auto;
  padding: 48px 24px 64px;
}

.hero {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 40px;
  align-items: center;
  margin-bottom: 72px;
}

.hero-eyebrow {
  font-size: 12px;
  letter-spacing: 0.06em;
  text-transform: uppercase;
  color: #646a73;
  margin: 0 0 12px;
}

.hero h1 {
  margin: 0 0 16px;
  font-size: clamp(26px, 3.5vw, 38px);
  font-weight: 600;
  line-height: 1.2;
  letter-spacing: -0.02em;
}

.hero-lead {
  margin: 0 0 28px;
  font-size: 16px;
  line-height: 1.65;
  color: #646a73;
}

.hero-cta {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-bottom: 20px;
}

.cta-center {
  justify-content: center;
}

.hero-trust {
  margin: 0;
  font-size: 12px;
  color: #8f959e;
}

.hero-visual {
  min-height: 280px;
  border-radius: 16px;
  background: linear-gradient(145deg, #eef3ff 0%, #f7f8fa 50%, #e8f0ff 100%);
  border: 1px solid #dee0e3;
  padding: 24px;
  box-shadow: 0 8px 24px rgba(31, 35, 41, 0.08);
}

.flow-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
  margin-bottom: 16px;
}

.flow-node {
  padding: 8px 12px;
  background: #fff;
  border-radius: 8px;
  font-size: 12px;
  font-weight: 500;
  border: 1px solid rgba(51, 112, 255, 0.2);
  box-shadow: 0 2px 8px rgba(31, 35, 41, 0.06);
}

.flow-arrow {
  color: #3370ff;
  font-size: 14px;
}

.hero-chart {
  background: #fff;
  border-radius: 12px;
  padding: 16px;
  border: 1px solid #dee0e3;
}

.chart-label {
  font-size: 12px;
  color: #646a73;
  margin-bottom: 8px;
}

.chart-bars {
  display: flex;
  align-items: flex-end;
  gap: 8px;
  height: 96px;
}

.chart-bars span {
  flex: 1;
  background: linear-gradient(180deg, #3370ff 0%, #8eb7ff 100%);
  border-radius: 4px 4px 0 0;
  animation: barUp 1s ease-out backwards;
}

.chart-bars span:nth-child(1) { height: 40%; }
.chart-bars span:nth-child(2) { height: 65%; }
.chart-bars span:nth-child(3) { height: 85%; }
.chart-bars span:nth-child(4) { height: 55%; }
.chart-bars span:nth-child(5) { height: 72%; }

@keyframes barUp {
  from {
    transform: scaleY(0);
    transform-origin: bottom;
  }
  to {
    transform: scaleY(1);
    transform-origin: bottom;
  }
}

.section {
  margin-bottom: 64px;
}

.section-title {
  margin: 0 0 8px;
  font-size: 22px;
  font-weight: 600;
  text-align: center;
}

.section-desc {
  margin: 0 0 28px;
  text-align: center;
  font-size: 14px;
  color: #646a73;
}

.feature-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
}

.feature-card {
  background: #fff;
  border-radius: 12px;
  padding: 22px;
  border: 1px solid #dee0e3;
  box-shadow: 0 2px 8px rgba(31, 35, 41, 0.06);
  transition: box-shadow 0.2s, transform 0.2s;
}

.feature-card:hover {
  box-shadow: 0 8px 24px rgba(31, 35, 41, 0.1);
  transform: translateY(-2px);
}

.feature-icon {
  width: 40px;
  height: 40px;
  border-radius: 10px;
  background: rgba(51, 112, 255, 0.12);
  display: grid;
  place-items: center;
  font-size: 16px;
  margin-bottom: 12px;
}

.feature-card h3 {
  margin: 0 0 8px;
  font-size: 16px;
  font-weight: 600;
}

.feature-card p {
  margin: 0 0 12px;
  font-size: 14px;
  line-height: 1.55;
  color: #646a73;
}

.card-link {
  font-size: 13px;
  font-weight: 500;
  color: #3370ff;
  text-decoration: none;
}

.card-link:hover {
  text-decoration: underline;
}

.steps-wrap {
  background: #fff;
  border-radius: 12px;
  padding: 32px 24px;
  border: 1px solid #dee0e3;
}

.steps {
  display: flex;
  flex-wrap: wrap;
  gap: 20px;
  justify-content: space-between;
}

.step {
  flex: 1;
  min-width: 140px;
  text-align: center;
}

.step-num {
  width: 36px;
  height: 36px;
  margin: 0 auto 10px;
  border-radius: 50%;
  background: #3370ff;
  color: #fff;
  font-weight: 600;
  font-size: 15px;
  display: grid;
  place-items: center;
}

.step h4 {
  margin: 0 0 6px;
  font-size: 14px;
  font-weight: 600;
}

.step p {
  margin: 0;
  font-size: 12px;
  color: #646a73;
  line-height: 1.45;
}

.cta-block {
  padding: 40px 0;
}

.landing-footer {
  padding: 24px;
  text-align: center;
  font-size: 12px;
  color: #8f959e;
  background: #f2f3f5;
  border-top: 1px solid #dee0e3;
}

@media (max-width: 960px) {
  .hero {
    grid-template-columns: 1fr;
  }

  .hero-visual {
    order: -1;
    min-height: 220px;
  }

  .feature-grid {
    grid-template-columns: 1fr;
  }

  .steps {
    flex-direction: column;
  }
}

@media (max-width: 720px) {
  .landing-nav-desktop {
    display: none !important;
  }

  .landing-menu-btn {
    display: grid;
    place-items: center;
  }

  .landing-mobile-panel {
    display: block;
  }
}

@media (min-width: 721px) {
  .landing-menu-btn {
    display: none;
  }

  .landing-mobile-panel {
    display: none !important;
  }
}
</style>
