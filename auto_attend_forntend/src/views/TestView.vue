<template>
  <div class="test-page">
    <h1 class="page-title">{{ $t('test.title') }}</h1>
    <p class="page-desc">{{ $t('test.desc') }}</p>

    <section class="test-section">
      <div class="test-header">
        <h2 class="test-title">{{ $t('test.diffTitle') }}</h2>
        <button class="primary-button" :disabled="diffLoading" @click="runDiffTest">
          {{ diffLoading ? $t('test.testing') : $t('test.runTest') }}
        </button>
      </div>
      <p class="test-desc">{{ $t('test.diffDesc') }}</p>
      <div v-if="diffResult" class="test-result" :class="diffResult.hasDiff ? 'success' : 'fail'">
        <div class="result-row">
          <span class="result-label">{{ $t('test.result') }}：</span>
          <span>{{ diffResult.message }}</span>
        </div>
        <div class="result-row">
          <span class="result-label">{{ $t('test.latency') }}：</span>
          <span>{{ diffResult.latencyMs }} ms</span>
        </div>
        <div v-if="diffResult.hasDiff && diffResult.diffLength" class="result-row">
          <span class="result-label">Diff 长度：</span>
          <span>{{ diffResult.diffLength }} 字符</span>
        </div>
      </div>
    </section>

    <section class="test-section">
      <div class="test-header">
        <h2 class="test-title">{{ $t('test.aiTitle') }}</h2>
        <button class="primary-button" :disabled="aiLoading" @click="runAiTest">
          {{ aiLoading ? $t('test.testing') : $t('test.runTest') }}
        </button>
      </div>
      <p class="test-desc">{{ $t('test.aiDesc') }}</p>
      <div v-if="aiResult" class="test-result" :class="aiResult.available ? 'success' : 'fail'">
        <div class="result-row">
          <span class="result-label">{{ $t('test.result') }}：</span>
          <span>{{ aiResult.message }}</span>
        </div>
        <div v-if="aiResult.latencyMs" class="result-row">
          <span class="result-label">{{ $t('test.latency') }}：</span>
          <span>{{ aiResult.latencyMs }} ms</span>
        </div>
      </div>
    </section>

    <section class="test-section">
      <div class="test-header">
        <h2 class="test-title">{{ $t('test.emailTitle') }}</h2>
        <button class="primary-button" :disabled="emailLoading" @click="runEmailTest">
          {{ emailLoading ? $t('test.testing') : $t('test.runTest') }}
        </button>
      </div>
      <p class="test-desc">{{ $t('test.emailDesc') }}</p>
      <div v-if="emailResult" class="test-result" :class="emailResult.available ? 'success' : 'fail'">
        <div class="result-row">
          <span class="result-label">{{ $t('test.result') }}：</span>
          <span>{{ emailResult.message }}</span>
        </div>
        <div v-if="emailResult.latencyMs" class="result-row">
          <span class="result-label">{{ $t('test.latency') }}：</span>
          <span>{{ emailResult.latencyMs }} ms</span>
        </div>
      </div>
    </section>

    <div class="back-link">
      <router-link to="/">{{ $t('collab.backHome') }}</router-link>
    </div>
  </div>
</template>

<script>
export default {
  name: 'TestView',
  data () {
    return {
      diffLoading: false,
      diffResult: null,
      aiLoading: false,
      aiResult: null,
      emailLoading: false,
      emailResult: null
    }
  },
  methods: {
    async runDiffTest () {
      this.diffResult = null
      this.diffLoading = true
      try {
        const resp = await this.$http.get('/admin/test/diff')
        if (resp.data && resp.data.code === 0) {
          this.diffResult = resp.data.data
        } else {
          this.diffResult = { hasDiff: false, message: (resp.data && resp.data.message) || '请求失败', latencyMs: 0 }
        }
      } catch (e) {
        this.diffResult = {
          hasDiff: false,
          message: (e.response && e.response.data && e.response.data.message) || '网络或后端错误',
          latencyMs: 0
        }
      } finally {
        this.diffLoading = false
      }
    },
    async runAiTest () {
      this.aiResult = null
      this.aiLoading = true
      try {
        const resp = await this.$http.get('/admin/test/ai')
        if (resp.data && resp.data.code === 0) {
          this.aiResult = resp.data.data
        } else {
          this.aiResult = { available: false, message: (resp.data && resp.data.message) || '请求失败', latencyMs: 0 }
        }
      } catch (e) {
        this.aiResult = {
          available: false,
          message: (e.response && e.response.data && e.response.data.message) || '网络或后端错误',
          latencyMs: 0
        }
      } finally {
        this.aiLoading = false
      }
    },
    async runEmailTest () {
      this.emailResult = null
      this.emailLoading = true
      try {
        const resp = await this.$http.get('/admin/test/email')
        if (resp.data && resp.data.code === 0) {
          this.emailResult = resp.data.data
        } else {
          this.emailResult = { available: false, message: (resp.data && resp.data.message) || '请求失败', latencyMs: 0 }
        }
      } catch (e) {
        this.emailResult = {
          available: false,
          message: (e.response && e.response.data && e.response.data.message) || '网络或后端错误',
          latencyMs: 0
        }
      } finally {
        this.emailLoading = false
      }
    }
  }
}
</script>

<style scoped>
.test-page {
  max-width: 720px;
}

.page-title {
  margin: 0 0 8px;
  font-size: 22px;
}

.page-desc {
  margin: 0 0 24px;
  font-size: 14px;
  color: #6b7280;
}

.test-section {
  margin-bottom: 28px;
  padding: 16px 20px;
  border-radius: 8px;
  background: #fff;
  border: 1px solid #e5e7eb;
}

.test-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 8px;
}

.test-title {
  margin: 0;
  font-size: 16px;
}

.test-desc {
  margin: 0 0 12px;
  font-size: 13px;
  color: #6b7280;
}

.test-result {
  margin-top: 12px;
  padding: 12px;
  border-radius: 6px;
  font-size: 13px;
}

.test-result.success {
  background: #ecfdf5;
  border: 1px solid #a7f3d0;
  color: #065f46;
}

.test-result.fail {
  background: #fef2f2;
  border: 1px solid #fecaca;
  color: #991b1b;
}

.result-row {
  margin-bottom: 4px;
}

.result-row:last-child {
  margin-bottom: 0;
}

.result-label {
  font-weight: 600;
  margin-right: 4px;
}

.primary-button {
  padding: 6px 14px;
  border-radius: 6px;
  border: none;
  background: #2563eb;
  color: #fff;
  font-size: 13px;
  cursor: pointer;
}

.primary-button:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}

.back-link {
  margin-top: 24px;
  font-size: 13px;
}

.back-link a {
  color: #2563eb;
}
</style>
