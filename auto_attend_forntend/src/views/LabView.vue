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
          key: 'finance',
          label: '智能财会',
          description: '基于 AI 的智能财务分析与报表生成，自动识别票据、对账、生成财务摘要。',
          iconBg: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
          iconPath: '<path d="M12 1v22M17 5H9.5a3.5 3.5 0 000 7h5a3.5 3.5 0 010 7H6"/>',
          status: 'coming'
        },
        {
          key: 'geo',
          label: 'GEO 推流',
          description: '全球化智能流量调度与分发，支持多区域容灾、灰度发布和实时流量监控。',
          iconBg: 'linear-gradient(135deg, #f093fb 0%, #f5576c 100%)',
          iconPath: '<circle cx="12" cy="12" r="10"/><path d="M2 12h20M12 2a15.3 15.3 0 014 10 15.3 15.3 0 01-4 10 15.3 15.3 0 01-4-10 15.3 15.3 0 014-10z"/>',
          status: 'coming'
        },
        {
          key: 'dashboard',
          label: '客户看板',
          description: '面向客户的自助数据看板，支持实时数据展示、自定义指标和分享链接。',
          iconBg: 'linear-gradient(135deg, #4facfe 0%, #00f2fe 100%)',
          iconPath: '<rect x="3" y="3" width="18" height="18" rx="2"/><path d="M3 9h18M9 21V9"/>',
          status: 'coming'
        },
        {
          key: 'lobster',
          label: '龙虾智能体',
          description: '基于大语言模型的多功能 AI 智能体，支持任务编排、知识库问答和自动化工作流。',
          iconBg: 'linear-gradient(135deg, #43e97b 0%, #38f9d7 100%)',
          iconPath: '<path d="M12 2a4 4 0 014 4c0 1.95-1.4 3.58-3.25 3.93"/><path d="M8 6a4 4 0 014-4"/><path d="M12 9.93V22"/><path d="M8 14s1.5 2 4 2 4-2 4-2"/>',
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
