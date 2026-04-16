<template>
  <div class="landing" :class="{ 'landing--mobile': layoutMobile }">
    <!-- 手机/小屏提示（媒体查询 + UA，关闭后本会话记住） -->
    <div v-if="showMobileScreenHint" class="landing-mobile-hint" role="status">
      <p class="landing-mobile-hint__text">{{ $t('landing.mobileScreenHint') }}</p>
      <button type="button" class="landing-mobile-hint__btn" @click="dismissMobileHint">
        {{ $t('landing.mobileScreenHintDismiss') }}
      </button>
    </div>

    <div class="landing-bg" aria-hidden="true">
      <div class="landing-bg__mesh"></div>
      <div class="landing-bg__glow landing-bg__glow--1"></div>
      <div class="landing-bg__glow landing-bg__glow--2"></div>
    </div>

    <header class="landing-header">
      <div class="landing-brand">
        <span class="landing-logo" aria-hidden="true">
          <img class="landing-logo-img" src="/brand-logo.svg" width="38" height="38" alt="">
        </span>
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
        <a href="#more" @click="closeMobileNav">{{ $t('landing.navMoreFeatures') }}</a>
        <a href="#integrations" @click="closeMobileNav">{{ $t('landing.navIntegrations') }}</a>
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
        <a href="#more" @click="closeMobileNav">{{ $t('landing.navMoreFeatures') }}</a>
        <a href="#integrations" @click="closeMobileNav">{{ $t('landing.navIntegrations') }}</a>
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
          <div v-if="clientDownloads.releasePageUrl || clientDownloads.windowsUrl" class="hero-downloads">
            <p class="hero-downloads-title">{{ $t('landing.downloadsTitle') }}</p>
            <p class="hero-downloads-ver">{{ $t('landing.downloadsVersion') }} {{ clientDownloads.latestVersion || '—' }}</p>
            <div class="hero-downloads-btns">
              <a v-if="clientDownloads.androidApkUrl" class="btn btn-ghost btn-sm" :href="clientDownloads.androidApkUrl" target="_blank" rel="noopener noreferrer">{{ $t('landing.downloadAndroid') }}</a>
              <a v-if="clientDownloads.iosNoteUrl" class="btn btn-ghost btn-sm" :href="clientDownloads.iosNoteUrl" target="_blank" rel="noopener noreferrer">{{ $t('landing.downloadIos') }}</a>
              <a v-if="clientDownloads.windowsUrl" class="btn btn-ghost btn-sm" :href="clientDownloads.windowsUrl" target="_blank" rel="noopener noreferrer">{{ $t('landing.downloadWindows') }}</a>
              <a v-if="clientDownloads.linuxDebUrl" class="btn btn-ghost btn-sm" :href="clientDownloads.linuxDebUrl" target="_blank" rel="noopener noreferrer">{{ $t('landing.downloadLinux') }}</a>
              <a v-if="clientDownloads.releasePageUrl" class="btn btn-ghost btn-sm" :href="clientDownloads.releasePageUrl" target="_blank" rel="noopener noreferrer">{{ $t('landing.downloadAllReleases') }}</a>
            </div>
          </div>
          <div class="hero-tech-strip">
            <div class="hero-tech-item">
              <span class="hero-tech-val">{{ $t('landing.statReposLabel') }}</span>
              <span class="hero-tech-hint">{{ $t('landing.statReposHint') }}</span>
            </div>
            <div class="hero-tech-item">
              <span class="hero-tech-val">{{ $t('landing.statAiLabel') }}</span>
              <span class="hero-tech-hint">{{ $t('landing.statAiHint') }}</span>
            </div>
            <div class="hero-tech-item">
              <span class="hero-tech-val">{{ $t('landing.statMultiLabel') }}</span>
              <span class="hero-tech-hint">{{ $t('landing.statMultiHint') }}</span>
            </div>
            <div class="hero-tech-item">
              <span class="hero-tech-val">{{ $t('landing.statSecureLabel') }}</span>
              <span class="hero-tech-hint">{{ $t('landing.statSecureHint') }}</span>
            </div>
          </div>
        </div>
        <div class="hero-visual" aria-hidden="true">
          <div class="hero-visual-inner">
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
            <div class="hero-orbit">
              <span class="orbit-dot orbit-dot--a"></span>
              <span class="orbit-dot orbit-dot--b"></span>
              <span class="orbit-dot orbit-dot--c"></span>
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

      <section id="more" class="section section--alt">
        <h2 class="section-title">{{ $t('landing.moreFeaturesTitle') }}</h2>
        <p class="section-desc">{{ $t('landing.moreFeaturesSubtitle') }}</p>
        <div class="feature-grid feature-grid--dense">
          <article v-for="card in moreFeatureCards" :key="card.titleKey" class="feature-card feature-card--glow">
            <div class="feature-icon feature-icon--sm" aria-hidden="true">{{ card.icon }}</div>
            <h3>{{ $t(card.titleKey) }}</h3>
            <p>{{ $t(card.descKey) }}</p>
            <router-link v-if="card.route" class="card-link" :to="card.route">{{ $t('landing.cardAction') }} →</router-link>
          </article>
        </div>
      </section>

      <section id="integrations" class="section">
        <h2 class="section-title">{{ $t('landing.integrationsTitle') }}</h2>
        <p class="section-desc">{{ $t('landing.integrationsSubtitle') }}</p>
        <div class="integration-grid">
          <div v-for="(row, idx) in integrationRows" :key="idx" class="integration-card">
            <div class="integration-icon">{{ row.icon }}</div>
            <h4>{{ $t(row.titleKey) }}</h4>
            <p>{{ $t(row.descKey) }}</p>
          </div>
        </div>
      </section>

      <section class="section tech-banner">
        <div class="tech-banner-inner">
          <h3 class="tech-banner-title">{{ $t('landing.techStripTitle') }}</h3>
          <p class="tech-banner-desc">{{ $t('landing.techStripSubtitle') }}</p>
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
import axios from 'axios'
import { localeOptions, setLocale } from '../locales'

const MOBILE_HINT_KEY = 'landing_mobile_screen_hint_dismissed'

export default {
  name: 'LandingView',
  data () {
    return {
      clientDownloads: {
        latestVersion: '',
        releasePageUrl: '',
        windowsUrl: '',
        linuxDebUrl: '',
        androidApkUrl: '',
        iosNoteUrl: ''
      },
      localeOptions,
      locale: this.$i18n.locale,
      mobileNavOpen: false,
      layoutMobile: false,
      showMobileScreenHint: false,
      featureCards: [
        { icon: '◉', titleKey: 'landing.featConsoleTitle', descKey: 'landing.featConsoleDesc', route: { name: 'dashboard' } },
        { icon: '▦', titleKey: 'landing.featCollabTitle', descKey: 'landing.featCollabDesc', route: { name: 'collab-projects' } },
        { icon: '¥', titleKey: 'landing.featQuoteTitle', descKey: 'landing.featQuoteDesc', route: { name: 'quote-list' } },
        { icon: 'AI', titleKey: 'landing.featAiTitle', descKey: 'landing.featAiDesc', route: { name: 'api-config' } },
        { icon: '◇', titleKey: 'landing.featProtoTitle', descKey: 'landing.featProtoDesc', route: { name: 'prototype-list' } },
        { icon: '◎', titleKey: 'landing.featTeamTitle', descKey: 'landing.featTeamDesc', route: { name: 'team-manage' } }
      ],
      moreFeatureCards: [
        { icon: '◎', titleKey: 'landing.featNexusTitle', descKey: 'landing.featNexusDesc', route: { name: 'nexus-console' } },
        { icon: '☁', titleKey: 'landing.featCloudDevTitle', descKey: 'landing.featCloudDevDesc', route: { name: 'cloud-dev-hub' } },
        { icon: '◈', titleKey: 'landing.featClientBoardTitle', descKey: 'landing.featClientBoardDesc', route: { name: 'collab-projects' } },
        { icon: '✦', titleKey: 'landing.featAgentTitle', descKey: 'landing.featAgentDesc', route: { name: 'dashboard' } }
      ],
      integrationRows: [
        { icon: '🔗', titleKey: 'landing.integrationWebhook', descKey: 'landing.integrationWebhookDesc' },
        { icon: '⚡', titleKey: 'landing.integrationAi', descKey: 'landing.integrationAiDesc' },
        { icon: '▣', titleKey: 'landing.integrationCollab', descKey: 'landing.integrationCollabDesc' }
      ],
      stepItems: [
        { titleKey: 'landing.step1Title', descKey: 'landing.step1Desc' },
        { titleKey: 'landing.step2Title', descKey: 'landing.step2Desc' },
        { titleKey: 'landing.step3Title', descKey: 'landing.step3Desc' },
        { titleKey: 'landing.step4Title', descKey: 'landing.step4Desc' }
      ]
    }
  },
  mounted () {
    this.updateLayoutFlags()
    window.addEventListener('resize', this.updateLayoutFlags, { passive: true })
    this.loadClientDownloads()
  },
  beforeDestroy () {
    window.removeEventListener('resize', this.updateLayoutFlags)
  },
  methods: {
    async loadClientDownloads () {
      try {
        const resp = await axios.get('/api/public/client/downloads')
        if (resp.data && resp.data.code === 0 && resp.data.data) {
          const d = resp.data.data
          this.clientDownloads = {
            latestVersion: d.latestVersion || '',
            releasePageUrl: d.releasePageUrl || '',
            windowsUrl: d.windowsUrl || '',
            linuxDebUrl: d.linuxDebUrl || '',
            androidApkUrl: d.androidApkUrl || '',
            iosNoteUrl: d.iosNoteUrl || ''
          }
        }
      } catch (e) { /* ignore */ }
    },
    onLocaleChange () {
      setLocale(this.locale)
    },
    closeMobileNav () {
      this.mobileNavOpen = false
    },
    dismissMobileHint () {
      try {
        sessionStorage.setItem(MOBILE_HINT_KEY, '1')
      } catch (e) { /* ignore */ }
      this.showMobileScreenHint = false
    },
    updateLayoutFlags () {
      const w = typeof window !== 'undefined' ? window.innerWidth : 1200
      this.layoutMobile = w <= 768
      this.computeMobileHint()
    },
    computeMobileHint () {
      if (typeof window === 'undefined') return
      try {
        if (sessionStorage.getItem(MOBILE_HINT_KEY) === '1') {
          this.showMobileScreenHint = false
          return
        }
      } catch (e) { /* ignore */ }
      const ua = navigator.userAgent || ''
      const phoneLike =
        /iPhone|iPod|Android.*Mobile|webOS|BlackBerry|IEMobile|Opera Mini/i.test(ua)
      const narrow = window.innerWidth <= 768
      // 手机 UA + 窄屏时提示；纯窄屏桌面窗口不误判
      this.showMobileScreenHint = phoneLike && narrow
    }
  }
}
</script>

<style scoped>
.landing {
  --lb-ink: #0a0e1a;
  --lb-muted: #64748b;
  --lb-accent: #3b82f6;
  --lb-accent2: #06b6d4;
  --lb-card: rgba(255, 255, 255, 0.78);
  --lb-border: rgba(148, 163, 184, 0.35);
  position: relative;
  min-height: 100vh;
  color: var(--lb-ink);
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', 'Noto Sans SC', sans-serif;
  overflow-x: hidden;
}

.landing-bg {
  position: fixed;
  inset: 0;
  z-index: 0;
  pointer-events: none;
  background: linear-gradient(165deg, #f0f7ff 0%, #eef2ff 35%, #f8fafc 70%, #ecfeff 100%);
}

.landing-bg__mesh {
  position: absolute;
  inset: 0;
  opacity: 0.45;
  background-image:
    linear-gradient(rgba(59, 130, 246, 0.06) 1px, transparent 1px),
    linear-gradient(90deg, rgba(59, 130, 246, 0.06) 1px, transparent 1px);
  background-size: 48px 48px;
}

.landing-bg__glow {
  position: absolute;
  border-radius: 50%;
  filter: blur(80px);
  opacity: 0.55;
}
.landing-bg__glow--1 {
  width: min(520px, 90vw);
  height: min(520px, 90vw);
  top: -120px;
  right: -80px;
  background: radial-gradient(circle, rgba(59, 130, 246, 0.45) 0%, transparent 70%);
}
.landing-bg__glow--2 {
  width: min(400px, 80vw);
  height: min(400px, 80vw);
  bottom: 10%;
  left: -100px;
  background: radial-gradient(circle, rgba(6, 182, 212, 0.35) 0%, transparent 70%);
}

.landing-mobile-hint {
  position: relative;
  z-index: 50;
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 12px 20px;
  background: linear-gradient(90deg, rgba(251, 191, 36, 0.2) 0%, rgba(251, 146, 60, 0.15) 100%);
  border-bottom: 1px solid rgba(245, 158, 11, 0.35);
  backdrop-filter: blur(8px);
}

.landing-mobile-hint__text {
  margin: 0;
  flex: 1;
  min-width: 200px;
  font-size: 13px;
  line-height: 1.55;
  color: #78350f;
}

.landing-mobile-hint__btn {
  flex-shrink: 0;
  padding: 8px 16px;
  border-radius: 8px;
  border: 1px solid rgba(120, 53, 15, 0.35);
  background: rgba(255, 255, 255, 0.85);
  color: #78350f;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
}

.landing-mobile-hint__btn:hover {
  background: #fff;
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
  background: rgba(255, 255, 255, 0.72);
  border-bottom: 1px solid var(--lb-border);
  backdrop-filter: blur(14px);
}

.landing-brand {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 0;
}

.landing-logo {
  width: 38px;
  height: 38px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  overflow: hidden;
  box-shadow: 0 4px 20px rgba(37, 99, 235, 0.35);
}

.landing-logo-img {
  width: 100%;
  height: 100%;
  display: block;
  object-fit: cover;
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
  letter-spacing: -0.02em;
}

.landing-brand-text span {
  font-size: 12px;
  color: var(--lb-muted);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.landing-nav {
  display: flex;
  align-items: center;
  gap: 4px;
}

.landing-nav a {
  padding: 8px 12px;
  border-radius: 8px;
  font-size: 13px;
  font-weight: 500;
  color: #475569;
  text-decoration: none;
  transition: background 0.15s, color 0.15s;
}

.landing-nav a:hover {
  background: rgba(59, 130, 246, 0.1);
  color: #1d4ed8;
}

.landing-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.landing-lang {
  padding: 6px 10px;
  border: 1px solid var(--lb-border);
  border-radius: 8px;
  font-size: 13px;
  background: rgba(255, 255, 255, 0.9);
  color: var(--lb-ink);
  cursor: pointer;
}

.btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 0 16px;
  height: 38px;
  border-radius: 10px;
  font-size: 14px;
  font-weight: 600;
  text-decoration: none;
  border: none;
  cursor: pointer;
  transition: background 0.15s, transform 0.1s, box-shadow 0.15s;
}

.btn:active {
  transform: scale(0.98);
}

.btn-lg {
  height: 44px;
  padding: 0 22px;
}

.btn-ghost {
  background: rgba(255, 255, 255, 0.85);
  color: #1e293b;
  border: 1px solid var(--lb-border);
}

.btn-ghost:hover {
  background: #fff;
  border-color: #cbd5e1;
}

.btn-primary {
  background: linear-gradient(135deg, #2563eb 0%, #0891b2 100%);
  color: #fff;
  box-shadow: 0 4px 16px rgba(37, 99, 235, 0.35);
}

.btn-primary:hover {
  filter: brightness(1.05);
}

.landing-menu-btn {
  display: none;
  width: 42px;
  height: 42px;
  border: 1px solid var(--lb-border);
  border-radius: 10px;
  background: rgba(255, 255, 255, 0.9);
  font-size: 18px;
  cursor: pointer;
}

.landing-mobile-panel {
  display: none;
  position: relative;
  z-index: 39;
  border-bottom: 1px solid var(--lb-border);
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(10px);
  padding: 16px;
}

.landing-nav-mobile {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.landing-nav-mobile a {
  color: #475569;
  text-decoration: none;
  padding: 8px 0;
  font-weight: 500;
}

.landing-main {
  position: relative;
  z-index: 1;
  max-width: 1140px;
  margin: 0 auto;
  padding: 40px 24px 72px;
}

.hero {
  display: grid;
  grid-template-columns: 1.05fr 0.95fr;
  gap: 48px;
  align-items: center;
  margin-bottom: 80px;
}

.hero-eyebrow {
  font-size: 12px;
  letter-spacing: 0.12em;
  text-transform: uppercase;
  color: #2563eb;
  font-weight: 600;
  margin: 0 0 14px;
}

.hero h1 {
  margin: 0 0 18px;
  font-size: clamp(28px, 4vw, 42px);
  font-weight: 700;
  line-height: 1.15;
  letter-spacing: -0.03em;
  background: linear-gradient(120deg, #0f172a 0%, #1e3a5f 50%, #0e7490 100%);
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
}

.hero-lead {
  margin: 0 0 28px;
  font-size: 16px;
  line-height: 1.7;
  color: #475569;
}

.hero-cta {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-bottom: 18px;
}

.cta-center {
  justify-content: center;
}

.hero-trust {
  margin: 0 0 24px;
  font-size: 12px;
  color: #94a3b8;
}

.hero-downloads {
  margin: 0 0 20px;
  padding: 14px 16px;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.72);
  border: 1px solid var(--lb-border);
}
.hero-downloads-title {
  margin: 0 0 6px;
  font-size: 13px;
  font-weight: 600;
  color: #0f172a;
}
.hero-downloads-ver {
  margin: 0 0 10px;
  font-size: 12px;
  color: #64748b;
}
.hero-downloads-btns {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
.hero-downloads .btn-sm {
  padding: 6px 12px;
  font-size: 12px;
}

.hero-tech-strip {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
}

.hero-tech-item {
  padding: 12px 14px;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.65);
  border: 1px solid var(--lb-border);
  box-shadow: 0 2px 12px rgba(15, 23, 42, 0.04);
}

.hero-tech-val {
  display: block;
  font-size: 13px;
  font-weight: 700;
  color: #0f172a;
  margin-bottom: 4px;
}

.hero-tech-hint {
  font-size: 11px;
  color: #94a3b8;
}

.hero-visual-inner {
  position: relative;
  min-height: 300px;
  border-radius: 20px;
  background: linear-gradient(145deg, rgba(255, 255, 255, 0.95) 0%, rgba(241, 245, 249, 0.9) 100%);
  border: 1px solid rgba(148, 163, 184, 0.4);
  padding: 28px;
  box-shadow:
    0 24px 48px rgba(15, 23, 42, 0.08),
    inset 0 1px 0 rgba(255, 255, 255, 0.9);
  overflow: hidden;
}

.flow-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
  margin-bottom: 20px;
}

.flow-node {
  padding: 8px 14px;
  background: linear-gradient(180deg, #fff 0%, #f1f5f9 100%);
  border-radius: 10px;
  font-size: 12px;
  font-weight: 600;
  border: 1px solid rgba(59, 130, 246, 0.25);
  box-shadow: 0 2px 8px rgba(15, 23, 42, 0.06);
}

.flow-arrow {
  color: #2563eb;
  font-size: 14px;
  font-weight: 700;
}

.hero-chart {
  background: rgba(255, 255, 255, 0.9);
  border-radius: 14px;
  padding: 18px;
  border: 1px solid var(--lb-border);
}

.chart-label {
  font-size: 12px;
  color: #64748b;
  margin-bottom: 10px;
}

.chart-bars {
  display: flex;
  align-items: flex-end;
  gap: 8px;
  height: 100px;
}

.chart-bars span {
  flex: 1;
  background: linear-gradient(180deg, #2563eb 0%, #38bdf8 100%);
  border-radius: 4px 4px 0 0;
  animation: barUp 1s ease-out backwards;
  box-shadow: 0 -4px 16px rgba(37, 99, 235, 0.25);
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

.hero-orbit {
  position: absolute;
  right: -20px;
  bottom: -20px;
  width: 140px;
  height: 140px;
  pointer-events: none;
}
.orbit-dot {
  position: absolute;
  width: 10px;
  height: 10px;
  border-radius: 50%;
  animation: orbitPulse 2.4s ease-in-out infinite;
}
.orbit-dot--a {
  background: #3b82f6;
  top: 20%;
  left: 30%;
}
.orbit-dot--b {
  background: #06b6d4;
  top: 55%;
  right: 25%;
  animation-delay: 0.4s;
}
.orbit-dot--c {
  background: #8b5cf6;
  bottom: 15%;
  left: 45%;
  animation-delay: 0.8s;
}

@keyframes orbitPulse {
  0%, 100% { opacity: 0.5; transform: scale(1); }
  50% { opacity: 1; transform: scale(1.15); }
}

.section {
  margin-bottom: 72px;
}

.section--alt {
  padding: 8px 0;
}

.section-title {
  margin: 0 0 10px;
  font-size: clamp(22px, 2.5vw, 28px);
  font-weight: 700;
  text-align: center;
  letter-spacing: -0.02em;
  color: #0f172a;
}

.section-desc {
  margin: 0 0 32px;
  text-align: center;
  font-size: 15px;
  color: #64748b;
  line-height: 1.65;
  max-width: 640px;
  margin-left: auto;
  margin-right: auto;
}

.feature-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
}

.feature-grid--dense {
  grid-template-columns: repeat(2, 1fr);
}

.feature-card {
  position: relative;
  background: var(--lb-card);
  border-radius: 16px;
  padding: 24px;
  border: 1px solid var(--lb-border);
  box-shadow: 0 4px 24px rgba(15, 23, 42, 0.06);
  transition: transform 0.2s, box-shadow 0.2s, border-color 0.2s;
  backdrop-filter: blur(8px);
}

.feature-card::before {
  content: '';
  position: absolute;
  inset: 0;
  border-radius: 16px;
  padding: 1px;
  background: linear-gradient(135deg, rgba(59, 130, 246, 0.25), transparent 40%, rgba(6, 182, 212, 0.15));
  -webkit-mask: linear-gradient(#fff 0 0) content-box, linear-gradient(#fff 0 0);
  mask: linear-gradient(#fff 0 0) content-box, linear-gradient(#fff 0 0);
  -webkit-mask-composite: xor;
  mask-composite: exclude;
  pointer-events: none;
  opacity: 0;
  transition: opacity 0.2s;
}

.feature-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 16px 40px rgba(37, 99, 235, 0.12);
  border-color: rgba(59, 130, 246, 0.35);
}

.feature-card:hover::before {
  opacity: 1;
}

.feature-card--glow {
  background: linear-gradient(165deg, rgba(255, 255, 255, 0.92) 0%, rgba(238, 242, 255, 0.85) 100%);
}

.feature-icon {
  width: 44px;
  height: 44px;
  border-radius: 12px;
  background: linear-gradient(135deg, rgba(59, 130, 246, 0.15) 0%, rgba(6, 182, 212, 0.12) 100%);
  display: grid;
  place-items: center;
  font-size: 17px;
  margin-bottom: 14px;
  border: 1px solid rgba(59, 130, 246, 0.2);
}

.feature-icon--sm {
  width: 40px;
  height: 40px;
  font-size: 15px;
}

.feature-card h3 {
  margin: 0 0 10px;
  font-size: 16px;
  font-weight: 600;
  color: #0f172a;
}

.feature-card p {
  margin: 0 0 14px;
  font-size: 14px;
  line-height: 1.6;
  color: #64748b;
}

.card-link {
  font-size: 13px;
  font-weight: 600;
  color: #2563eb;
  text-decoration: none;
}

.card-link:hover {
  color: #1d4ed8;
  text-decoration: underline;
}

.integration-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
}

.integration-card {
  padding: 24px;
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.75);
  border: 1px solid var(--lb-border);
  box-shadow: 0 4px 20px rgba(15, 23, 42, 0.05);
}

.integration-icon {
  font-size: 24px;
  margin-bottom: 12px;
}

.integration-card h4 {
  margin: 0 0 8px;
  font-size: 15px;
  font-weight: 600;
  color: #0f172a;
}

.integration-card p {
  margin: 0;
  font-size: 13px;
  line-height: 1.6;
  color: #64748b;
}

.tech-banner {
  margin-bottom: 64px;
}

.tech-banner-inner {
  text-align: center;
  padding: 36px 28px;
  border-radius: 20px;
  background: linear-gradient(120deg, rgba(37, 99, 235, 0.08) 0%, rgba(6, 182, 212, 0.08) 50%, rgba(139, 92, 246, 0.06) 100%);
  border: 1px solid rgba(59, 130, 246, 0.2);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.8);
}

.tech-banner-title {
  margin: 0 0 8px;
  font-size: 20px;
  font-weight: 700;
  color: #0f172a;
}

.tech-banner-desc {
  margin: 0;
  font-size: 14px;
  color: #64748b;
  line-height: 1.6;
}

.steps-wrap {
  background: var(--lb-card);
  border-radius: 20px;
  padding: 36px 28px;
  border: 1px solid var(--lb-border);
  box-shadow: 0 8px 32px rgba(15, 23, 42, 0.06);
  backdrop-filter: blur(10px);
}

.steps {
  display: flex;
  flex-wrap: wrap;
  gap: 24px;
  justify-content: space-between;
}

.step {
  flex: 1;
  min-width: 140px;
  text-align: center;
}

.step-num {
  width: 40px;
  height: 40px;
  margin: 0 auto 12px;
  border-radius: 12px;
  background: linear-gradient(135deg, #2563eb 0%, #0891b2 100%);
  color: #fff;
  font-weight: 700;
  font-size: 16px;
  display: grid;
  place-items: center;
  box-shadow: 0 4px 14px rgba(37, 99, 235, 0.35);
}

.step h4 {
  margin: 0 0 8px;
  font-size: 14px;
  font-weight: 600;
  color: #0f172a;
}

.step p {
  margin: 0;
  font-size: 12px;
  color: #64748b;
  line-height: 1.5;
}

.cta-block {
  padding: 48px 0 16px;
}

.landing-footer {
  position: relative;
  z-index: 1;
  padding: 28px 24px;
  text-align: center;
  font-size: 12px;
  color: #94a3b8;
  background: rgba(248, 250, 252, 0.9);
  border-top: 1px solid var(--lb-border);
}

/* —— 平板 —— */
@media (max-width: 1024px) {
  .hero-tech-strip {
    grid-template-columns: repeat(2, 1fr);
  }
  .feature-grid {
    grid-template-columns: repeat(2, 1fr);
  }
  .feature-grid--dense {
    grid-template-columns: 1fr;
  }
  .integration-grid {
    grid-template-columns: 1fr;
  }
}

/* —— 手机 / 小屏 —— */
@media (max-width: 960px) {
  .hero {
    grid-template-columns: 1fr;
    gap: 36px;
    margin-bottom: 56px;
  }
  .hero-visual {
    order: -1;
  }
  .hero-visual-inner {
    min-height: 260px;
  }
  .feature-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .landing-main {
    padding: 28px 16px 56px;
  }
  .landing-header {
    padding: 0 16px;
  }
  .hero h1 {
    font-size: clamp(24px, 7vw, 32px);
  }
  .hero-tech-strip {
    grid-template-columns: 1fr 1fr;
    gap: 10px;
  }
  .hero-tech-item {
    padding: 10px 12px;
  }
  .section {
    margin-bottom: 56px;
  }
  .steps {
    flex-direction: column;
  }
  .flow-row {
    justify-content: center;
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

@media (max-width: 480px) {
  .hero-cta {
    flex-direction: column;
  }
  .hero-cta .btn {
    width: 100%;
  }
  .hero-tech-strip {
    grid-template-columns: 1fr;
  }
}
</style>
