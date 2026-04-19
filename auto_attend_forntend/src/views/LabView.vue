<template>
  <div class="lab-page">
    <!-- Page Header -->
    <div class="lab-header">
      <div class="lab-header-left">
        <h1 class="lab-title">增效实验室</h1>
        <p class="lab-subtitle">探索提升效率的前沿实验项目，帮助团队在真实场景中快速试用与验证。</p>
      </div>
    </div>

    <!-- Lab Projects Grid -->
    <div class="lab-grid">
      <div
        v-for="project in labProjects"
        :key="project.key"
        class="lab-card"
        :class="{ 'lab-card--coming': project.status === 'coming' }"
      >
        <div class="lab-card-icon" :style="{ background: project.iconBg }">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" width="28" height="28" v-html="project.iconPath"></svg>
        </div>
        <div class="lab-card-body">
          <div class="lab-card-name">{{ project.label }}</div>
          <div class="lab-card-desc">{{ project.description }}</div>
        </div>
        <div class="lab-card-footer">
          <span class="lab-card-tag" :class="'lab-card-tag--' + project.status">
            {{ project.status === 'coming' ? '敬请期待' : '可用' }}
          </span>
          <button
            v-if="project.status === 'available'"
            class="lab-card-btn"
            @click="openProject(project)"
          >
            进入
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="14" height="14"><path d="M5 12h14M12 5l7 7-7 7"/></svg>
          </button>
        </div>
      </div>
    </div>

    <!-- Empty state hint -->
    <div class="lab-empty-hint">
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" width="40" height="40" style="color: var(--text-disabled);"><path d="M9.663 17h4.673M12 3v1m6.364 1.636l-.707.707M21 12h-1M4 12H3m3.343-5.657l-.707-.707m2.828 9.9a5 5 0 117.072 0l-.548.547A3.374 3.374 0 0014 18.469V19a2 2 0 11-4 0v-.531c0-.895-.356-1.754-.988-2.386l-.548-.547z"/></svg>
      <p>更多实验项目正在孵化中，敬请期待。</p>
    </div>
  </div>
</template>

<script>
export default {
  name: 'LabView',
  data () {
    return {
      labProjects: [
        {
          key: 'ai_native',
          label: 'AI原生应用和开放API',
          description: '面向业务场景的 AI 原生能力与开放接口，统一接入与治理。',
          iconBg: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
          iconPath: '<polygon points="13 2 3 14 12 14 11 22 21 10 12 10 13 2"/>',
          status: 'coming'
        },
        {
          key: 'cloud_coding',
          label: '流帮 Cloud Coding',
          description: '云端开发与协作环境，与项目资产、协作流一站式衔接。',
          iconBg: 'linear-gradient(135deg, #4facfe 0%, #00f2fe 100%)',
          iconPath: '<path d="M18 10h-1.26A8 8 0 1 0 9 20h9a5 5 0 0 0 0-10z"/>',
          status: 'coming'
        },
        {
          key: 'xianyu',
          label: '咸鱼/淘宝管家',
          description: '电商侧订单与消息聚合、自动化跟进与经营辅助。',
          iconBg: 'linear-gradient(135deg, #f093fb 0%, #f5576c 100%)',
          iconPath: '<circle cx="9" cy="21" r="1"/><circle cx="20" cy="21" r="1"/><path d="M1 1h4l2.68 13.39a2 2 0 0 0 2 1.61h9.72a2 2 0 0 0 2-1.61L23 6H6"/>',
          status: 'coming'
        },
        {
          key: 'dr',
          label: '项目容灾恢复',
          description: '关键项目与数据的备份、演练与一键恢复编排。',
          iconBg: 'linear-gradient(135deg, #43e97b 0%, #38f9d7 100%)',
          iconPath: '<path d="M21 16V8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73l7 4a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16z"/><polyline points="3.27 6.96 12 12.01 20.73 6.96"/><line x1="12" y1="22.08" x2="12" y2="12"/>',
          status: 'coming'
        },
        {
          key: 'migrate',
          label: '项目迁移',
          description: '跨仓库、跨环境的项目与数据迁移工具链与校验。',
          iconBg: 'linear-gradient(135deg, #fa709a 0%, #fee140 100%)',
          iconPath: '<polyline points="17 1 21 5 17 9"/><path d="M3 11V9a4 4 0 0 1 4-4h14"/><polyline points="7 23 3 19 7 15"/><path d="M21 13v2a4 4 0 0 1-4 4H3"/>',
          status: 'coming'
        }
      ]
    }
  },
  methods: {
    openProject (project) {
      // Future: navigate to project-specific route
      this.$message && this.$message.info(`${project.label} 即将上线`)
    }
  }
}
</script>

<style scoped>
.lab-page {
  max-width: 1200px;
  margin: 0 auto;
  padding: 32px;
}

/* ========== Header ========== */
.lab-header {
  margin-bottom: 32px;
}

.lab-header-left {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.lab-title {
  margin: 0;
  font-size: 24px;
  font-weight: 700;
  color: var(--text-primary, #1F2329);
}

.lab-subtitle {
  margin: 0;
  font-size: 14px;
  color: var(--text-secondary, #646A73);
  line-height: 1.6;
}

/* ========== Grid ========== */
.lab-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 20px;
}

@media (max-width: 768px) {
  .lab-grid {
    grid-template-columns: 1fr;
  }
}

/* ========== Card ========== */
.lab-card {
  background: #fff;
  border: 1px solid var(--border-primary, #dee0e3);
  border-radius: 12px;
  padding: 24px;
  display: flex;
  flex-direction: column;
  gap: 16px;
  transition: box-shadow 0.25s ease, border-color 0.25s ease;
  cursor: default;
}

.lab-card:hover {
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.06);
  border-color: var(--brand-blue, #1456F0);
}

.lab-card--coming {
  opacity: 0.85;
}

.lab-card--coming:hover {
  opacity: 1;
}

/* ========== Card Icon ========== */
.lab-card-icon {
  width: 52px;
  height: 52px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  flex-shrink: 0;
}

/* ========== Card Body ========== */
.lab-card-body {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.lab-card-name {
  font-size: 17px;
  font-weight: 600;
  color: var(--text-primary, #1F2329);
}

.lab-card-desc {
  font-size: 13px;
  color: var(--text-secondary, #646A73);
  line-height: 1.7;
}

/* ========== Card Footer ========== */
.lab-card-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 4px;
}

.lab-card-tag {
  display: inline-flex;
  align-items: center;
  font-size: 12px;
  font-weight: 500;
  padding: 3px 10px;
  border-radius: 100px;
}

.lab-card-tag--coming {
  background: #F0F5FF;
  color: var(--brand-blue, #1456F0);
}

.lab-card-tag--available {
  background: #ECFDF5;
  color: #059669;
}

.lab-card-btn {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 6px 14px;
  border: none;
  border-radius: 8px;
  background: var(--brand-blue, #1456F0);
  color: #fff;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: opacity 0.2s, background 0.2s;
}

.lab-card-btn:hover {
  opacity: 0.85;
}

/* ========== Empty Hint ========== */
.lab-empty-hint {
  margin-top: 48px;
  text-align: center;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
}

.lab-empty-hint p {
  margin: 0;
  font-size: 14px;
  color: var(--text-disabled, #8F959E);
}
</style>
