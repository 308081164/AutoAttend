<template>
  <div class="ai-config-page ai-config-shell">
    <header class="ai-config-hero">
      <h1 class="page-title">{{ $t('aiConfig.title') }}</h1>
      <p class="page-lead">{{ $t('aiConfig.pageLead') }}</p>
    </header>

    <section class="ai-guide-card" aria-labelledby="ai-guide-title">
      <div class="ai-guide-head">
        <span class="ai-guide-kicker">{{ $t('aiConfig.guideKicker') }}</span>
        <h2 id="ai-guide-title" class="ai-guide-title">{{ $t('aiConfig.guideTitle') }}</h2>
      </div>
      <ol class="ai-guide-steps">
        <li><span class="ai-step-num" aria-hidden="true">1</span><span class="ai-step-text">{{ $t('aiConfig.guideStep1') }}</span></li>
        <li><span class="ai-step-num" aria-hidden="true">2</span><span class="ai-step-text">{{ $t('aiConfig.guideStep2') }}</span></li>
        <li><span class="ai-step-num" aria-hidden="true">3</span><span class="ai-step-text">{{ $t('aiConfig.guideStep3') }}</span></li>
        <li><span class="ai-step-num" aria-hidden="true">4</span><span class="ai-step-text">{{ $t('aiConfig.guideStep4') }}</span></li>
      </ol>
      <div class="ai-guide-cta">
        <router-link :to="{ name: 'test' }" class="ai-cta-btn ai-cta-btn--primary">{{ $t('aiConfig.guideOpenTest') }}</router-link>
        <span class="ai-guide-cta-hint">{{ $t('aiConfig.guideOpenTestHint') }}</span>
      </div>
    </section>

    <section class="ai-section ai-section--guard" aria-labelledby="export-guard-heading">
      <div class="ai-section-head">
        <h2 id="export-guard-heading" class="ai-section-title">{{ $t('aiConfig.exportGuardTitle') }}</h2>
        <p class="ai-section-desc">{{ $t('aiConfig.exportGuardDesc') }}</p>
      </div>
      <div class="config-card ai-section-card">
        <p v-if="exportGuardEnabled" class="export-guard-status export-guard-status--on">{{ $t('aiConfig.exportGuardEnabledHint') }}</p>
        <p v-else class="export-guard-status">{{ $t('aiConfig.exportGuardDisabledHint') }}</p>

        <template v-if="!exportGuardEnabled">
          <div class="form-row">
            <label class="form-label">{{ $t('aiConfig.exportGuardPassword') }}</label>
            <input v-model="exportGuardNew1" type="password" class="form-input" autocomplete="new-password">
          </div>
          <div class="form-row">
            <label class="form-label">{{ $t('aiConfig.exportGuardPasswordConfirm') }}</label>
            <input v-model="exportGuardNew2" type="password" class="form-input" autocomplete="new-password">
          </div>
          <div class="form-actions form-actions--compact">
            <button type="button" class="primary-button" :disabled="exportGuardSaving" @click="saveExportGuard">{{ exportGuardSaving ? $t('aiConfig.saving') : $t('aiConfig.exportGuardSave') }}</button>
            <span v-if="exportGuardMessage" class="save-message" :class="exportGuardMessageOk ? 'success' : 'error'">{{ exportGuardMessage }}</span>
          </div>
        </template>

        <template v-else>
          <div class="form-row">
            <label class="form-label">{{ $t('aiConfig.exportGuardClearHint') }}</label>
            <input v-model="exportGuardClearPwd" type="password" class="form-input" autocomplete="off">
          </div>
          <div class="form-actions form-actions--compact guard-actions-row">
            <button type="button" class="primary-button" @click="openExportModal">{{ $t('aiConfig.exportOpenModal') }}</button>
            <button type="button" class="primary-button primary-button-outline" :disabled="exportGuardSaving" @click="clearExportGuard">{{ exportGuardSaving ? $t('aiConfig.saving') : $t('aiConfig.exportGuardClear') }}</button>
            <span v-if="exportGuardMessage" class="save-message" :class="exportGuardMessageOk ? 'success' : 'error'">{{ exportGuardMessage }}</span>
          </div>
        </template>
      </div>
    </section>

    <div class="usage-detail-modal" v-if="exportModalOpen" @click.self="closeExportModal">
      <div class="usage-detail-dialog export-modal-dialog">
        <div class="usage-detail-header">
          <h3>{{ $t('aiConfig.exportModalTitle') }}</h3>
          <button type="button" class="usage-detail-close" :aria-label="$t('aiConfig.exportModalClose')" @click="closeExportModal">×</button>
        </div>
        <div class="export-modal-body">
          <p class="ai-section-desc export-modal-lead">{{ $t('aiConfig.exportModalDesc') }}</p>
          <label class="form-label">{{ $t('aiConfig.exportModalPassword') }}</label>
          <input v-model="exportModalPwd" type="password" class="form-input" autocomplete="off" @keyup.enter="submitSensitiveExport">
          <div class="form-actions form-actions--compact">
            <button type="button" class="primary-button" :disabled="exportSubmitting" @click="submitSensitiveExport">{{ exportSubmitting ? $t('aiConfig.exportVerifying') : $t('aiConfig.exportModalSubmit') }}</button>
          </div>
          <p v-if="exportSubmitError" class="save-message error">{{ exportSubmitError }}</p>
          <template v-if="exportResultJson">
            <textarea class="export-json-out" readonly rows="10" :value="exportResultJson"></textarea>
            <button type="button" class="primary-button primary-button-outline export-copy-btn" @click="copyExportJson">{{ exportCopied ? $t('aiConfig.exportCopied') : $t('aiConfig.exportCopyJson') }}</button>
          </template>
        </div>
      </div>
    </div>

    <section class="ai-section" aria-labelledby="ai-models-heading">
      <div class="ai-section-head">
        <h2 id="ai-models-heading" class="ai-section-title">{{ $t('aiConfig.sectionModelsTitle') }}</h2>
        <p class="ai-section-desc">{{ $t('aiConfig.sectionModelsDesc') }}</p>
      </div>
      <div class="provider-grid ai-models-grid">
      <div class="provider-panel">
        <div class="ai-column-head">
          <div class="ai-column-head-row">
            <h3 class="ai-column-title">{{ $t('aiConfig.deepseekBlockTitle') }}</h3>
            <div class="ai-external-links">
              <a href="https://platform.deepseek.com/" target="_blank" rel="noopener noreferrer" class="ai-doc-link">{{ $t('aiConfig.openDeepseekConsole') }} ↗</a>
              <a href="https://platform.deepseek.com/api_keys" target="_blank" rel="noopener noreferrer" class="ai-doc-link">{{ $t('aiConfig.openDeepseekKeys') }} ↗</a>
            </div>
          </div>
        </div>
        <div class="config-card" v-if="config">
          <p v-if="configLoadedHint" class="config-loaded-hint">{{ $t('aiConfig.configLoadedHint') }}</p>
          <form @submit.prevent="saveConfig">
            <div class="form-row">
              <label class="form-label">{{ $t('aiConfig.provider') }}</label>
              <span class="form-value">DeepSeek</span>
            </div>
            <div class="form-row">
              <label class="form-label">{{ $t('aiConfig.apiKey') }}</label>
              <p v-if="config.apiKeyMasked" class="api-key-set">{{ $t('aiConfig.apiKeySetHint') }}（{{ config.apiKeyMasked }}）</p>
              <input
                v-model="form.apiKey"
                type="password"
                autocomplete="off"
                :placeholder="config.apiKeyMasked ? $t('aiConfig.apiKeyPlaceholderKeep') : $t('aiConfig.apiKeyPlaceholder')"
                class="form-input"
              >
              <p class="form-hint">{{ $t('aiConfig.apiKeyHint') }}</p>
            </div>
            <div class="form-row">
              <label class="form-label">{{ $t('aiConfig.enabled') }}</label>
              <label class="checkbox-label">
                <input type="checkbox" v-model="form.enabled">
                <span>{{ $t('aiConfig.enabledLabel') }}</span>
              </label>
            </div>
            <div class="form-row">
              <label class="form-label">{{ $t('aiConfig.dailySummaryEnabled') }}</label>
              <label class="checkbox-label">
                <input type="checkbox" v-model="form.dailySummaryEnabled">
                <span>{{ $t('aiConfig.dailySummaryEnabledLabel') }}</span>
              </label>
              <p class="form-hint">{{ $t('aiConfig.dailySummaryHint') }}</p>
              <p class="form-hint subtle">{{ $t('aiConfig.toggleAutoSaveHint') }}</p>
            </div>
            <div class="form-row daily-run-row">
              <button type="button" class="link-button" :disabled="dailyRunLoading" @click="runDailySummaryNow">
                {{ dailyRunLoading ? '…' : $t('aiConfig.runDailySummaryNow') }}
              </button>
              <span v-if="dailyRunMessage" class="save-message inline-msg" :class="dailyRunOk ? 'success' : 'error'">{{ dailyRunMessage }}</span>
            </div>
            <div class="form-row">
              <label class="form-label">{{ $t('aiConfig.model') }}</label>
              <input v-model="form.model" type="text" class="form-input" placeholder="deepseek-chat">
            </div>
            <div class="form-actions">
              <button type="submit" class="primary-button" :disabled="saving">{{ saving ? $t('aiConfig.saving') : $t('aiConfig.save') }}</button>
              <span v-if="saveMessage" class="save-message" :class="saveSuccess ? 'success' : 'error'">{{ saveMessage }}</span>
            </div>
          </form>
        </div>
        <div v-else class="placeholder">{{ $t('aiConfig.loading') }}</div>

        <div class="config-card token-usage-card">
          <h3 class="config-card-title config-card-title--usage">{{ $t('aiConfig.tokenUsageHeadingDeepseek') }}</h3>
          <p class="config-card-desc">{{ $t('aiConfig.tokenUsageDesc') }}</p>
          <div v-if="usageLoading" class="placeholder small">{{ $t('collab.loading') }}</div>
          <template v-else-if="usage">
            <div class="usage-summary">
              <div class="usage-summary-item">
                <span class="usage-summary-value">{{ formatNum(usage.summary.callCount) }}</span>
                <span class="usage-summary-label">{{ $t('aiConfig.usageCallCount') }}</span>
              </div>
              <div class="usage-summary-item">
                <span class="usage-summary-value">{{ formatNum(usage.summary.inputTokens) }}</span>
                <span class="usage-summary-label">{{ $t('aiConfig.usageInputTokens') }}</span>
              </div>
              <div class="usage-summary-item">
                <span class="usage-summary-value">{{ formatNum(usage.summary.outputTokens) }}</span>
                <span class="usage-summary-label">{{ $t('aiConfig.usageOutputTokens') }}</span>
              </div>
              <div class="usage-summary-item">
                <span class="usage-summary-value">{{ formatNum(usage.summary.totalTokens) }}</span>
                <span class="usage-summary-label">{{ $t('aiConfig.usageTotalTokens') }}</span>
              </div>
            </div>
            <p class="usage-since">{{ $t('aiConfig.usageSince', { days: usageDays }) }}</p>
            <div class="usage-actions">
              <select v-model.number="usageDays" @change="onUsageDaysChange('deepseek')" class="usage-days-select">
                <option :value="7">7 {{ $t('aiConfig.usageDays') }}</option>
                <option :value="30">30 {{ $t('aiConfig.usageDays') }}</option>
                <option :value="90">90 {{ $t('aiConfig.usageDays') }}</option>
              </select>
              <button type="button" class="link-button" @click="refreshUsageDeepseek">{{ $t('dashboard.refresh') }}</button>
            </div>
            <div class="usage-charts">
              <div class="usage-chart-block">
                <div class="usage-chart-title">API 请求次数</div>
                <div class="usage-chart-wrap">
                  <canvas ref="deepseekCall"></canvas>
                </div>
              </div>
              <div class="usage-chart-block">
                <div class="usage-chart-title">{{ $t('aiConfig.usageTotalTokens') }}</div>
                <div class="usage-chart-wrap">
                  <canvas ref="deepseekToken"></canvas>
                </div>
              </div>
            </div>
            <div class="usage-detail-actions">
              <button type="button" class="primary-button primary-button-outline" @click="openUsageDetail('deepseek', usageDays)">查看详情</button>
            </div>
          </template>
        </div>
      </div>

      <div class="provider-panel">
        <div class="ai-column-head">
          <div class="ai-column-head-row">
            <h3 class="ai-column-title">{{ $t('aiConfig.qwenBlockTitle') }}</h3>
            <div class="ai-external-links">
              <a href="https://dashscope.console.aliyun.com/" target="_blank" rel="noopener noreferrer" class="ai-doc-link">{{ $t('aiConfig.openDashscopeConsole') }} ↗</a>
              <a href="https://dashscope.console.aliyun.com/apiKey" target="_blank" rel="noopener noreferrer" class="ai-doc-link">{{ $t('aiConfig.openDashscopeKeys') }} ↗</a>
              <a href="https://help.aliyun.com/zh/dashscope/developer-reference/api-details" target="_blank" rel="noopener noreferrer" class="ai-doc-link">{{ $t('aiConfig.qwenDocsLink') }} ↗</a>
            </div>
          </div>
          <p class="ai-column-sub">{{ $t('aiConfig.qwenBlockDesc') }}</p>
        </div>
        <div class="config-card qwen-config-card" v-if="qwenConfig">
          <form @submit.prevent="saveQwenConfig">
            <div class="form-row">
              <label class="form-label">{{ $t('aiConfig.qwenProviderLabel') }}</label>
              <span class="form-value form-value--badge">{{ $t('aiConfig.qwenProviderValue') }}</span>
            </div>
            <div class="form-row">
              <label class="form-label">{{ $t('aiConfig.qwenApiKeyLabel') }}</label>
              <p v-if="qwenConfig.apiKeyMasked" class="api-key-set">{{ $t('aiConfig.qwenApiKeySetHint') }}（{{ qwenConfig.apiKeyMasked }}）</p>
              <input
                v-model="qwenForm.apiKey"
                type="password"
                autocomplete="off"
                :placeholder="qwenConfig.apiKeyMasked ? $t('aiConfig.qwenApiKeyPlaceholderKeep') : $t('aiConfig.qwenApiKeyPlaceholder')"
                class="form-input"
              >
              <p class="form-hint">{{ $t('aiConfig.qwenApiKeyHint') }}</p>
            </div>
            <div class="form-row">
              <label class="form-label">{{ $t('aiConfig.qwenEnabledField') }}</label>
              <label class="checkbox-label">
                <input type="checkbox" v-model="qwenForm.enabled">
                <span>{{ $t('aiConfig.qwenEnabledLabel') }}</span>
              </label>
            </div>
            <div class="form-row">
              <label class="form-label">{{ $t('aiConfig.qwenModelLabel') }}</label>
              <input v-model="qwenForm.model" type="text" class="form-input" placeholder="qwen-omni">
            </div>
            <div class="form-actions">
              <button type="submit" class="primary-button" :disabled="qwenSaving">{{ qwenSaving ? $t('aiConfig.saving') : $t('aiConfig.save') }}</button>
              <span v-if="qwenSaveMessage" class="save-message" :class="qwenSaveSuccess ? 'success' : 'error'">{{ qwenSaveMessage }}</span>
            </div>
          </form>
        </div>

        <div class="config-card token-usage-card qwen-usage-card">
          <h3 class="config-card-title config-card-title--usage">{{ $t('aiConfig.tokenUsageHeadingQwen') }}</h3>
          <p class="config-card-desc">{{ $t('aiConfig.qwenUsageDesc') }}</p>
          <div v-if="usageQwenLoading" class="placeholder small">{{ $t('collab.loading') }}</div>
          <template v-else-if="usageQwen">
            <div class="usage-summary">
              <div class="usage-summary-item">
                <span class="usage-summary-value">{{ formatNum(usageQwen.summary.callCount) }}</span>
                <span class="usage-summary-label">{{ $t('aiConfig.usageCallCount') }}</span>
              </div>
              <div class="usage-summary-item">
                <span class="usage-summary-value">{{ formatNum(usageQwen.summary.inputTokens) }}</span>
                <span class="usage-summary-label">{{ $t('aiConfig.usageInputTokens') }}</span>
              </div>
              <div class="usage-summary-item">
                <span class="usage-summary-value">{{ formatNum(usageQwen.summary.outputTokens) }}</span>
                <span class="usage-summary-label">{{ $t('aiConfig.usageOutputTokens') }}</span>
              </div>
              <div class="usage-summary-item">
                <span class="usage-summary-value">{{ formatNum(usageQwen.summary.totalTokens) }}</span>
                <span class="usage-summary-label">{{ $t('aiConfig.usageTotalTokens') }}</span>
              </div>
            </div>
            <p class="usage-since">{{ $t('aiConfig.usageSince', { days: usageDaysQwen }) }}</p>
            <div class="usage-actions">
              <select v-model.number="usageDaysQwen" @change="onUsageDaysChange('qwen')" class="usage-days-select">
                <option :value="7">7 {{ $t('aiConfig.usageDays') }}</option>
                <option :value="30">30 {{ $t('aiConfig.usageDays') }}</option>
                <option :value="90">90 {{ $t('aiConfig.usageDays') }}</option>
              </select>
              <button type="button" class="link-button" @click="refreshUsageQwen">{{ $t('dashboard.refresh') }}</button>
            </div>
            <div class="usage-charts">
              <div class="usage-chart-block">
                <div class="usage-chart-title">API 请求次数</div>
                <div class="usage-chart-wrap">
                  <canvas ref="qwenCall"></canvas>
                </div>
              </div>
              <div class="usage-chart-block">
                <div class="usage-chart-title">{{ $t('aiConfig.usageTotalTokens') }}</div>
                <div class="usage-chart-wrap">
                  <canvas ref="qwenToken"></canvas>
                </div>
              </div>
            </div>
            <div class="usage-detail-actions">
              <button type="button" class="primary-button primary-button-outline" @click="openUsageDetail('qwen', usageDaysQwen)">查看详情</button>
            </div>
          </template>
        </div>
      </div>
    </div>
    </section>

    <div class="usage-detail-modal" v-if="usageDetailOpen" @click.self="closeUsageDetail">
      <div class="usage-detail-dialog">
        <div class="usage-detail-header">
          <h3>{{ usageDetailTitle }}</h3>
          <button type="button" class="usage-detail-close" @click="closeUsageDetail" aria-label="关闭">×</button>
        </div>
        <div v-if="usageDetailLoading" class="placeholder small">加载中…</div>
        <template v-else>
          <div class="usage-table-wrap">
            <table class="usage-table">
              <thead>
                <tr>
                  <th>{{ $t('aiConfig.usageTime') }}</th>
                  <th>Repo</th>
                  <th>Commit</th>
                  <th>Model</th>
                  <th class="num">{{ $t('aiConfig.usageInputTokens') }}</th>
                  <th class="num">{{ $t('aiConfig.usageOutputTokens') }}</th>
                  <th class="num">{{ $t('aiConfig.usageTotalTokens') }}</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="(row, i) in usageDetailItems" :key="'detail-' + i">
                  <td>{{ formatUsageTime(row.usedAt) }}</td>
                  <td class="repo-cell">{{ row.repoFullName || '—' }}</td>
                  <td class="mono">{{ shortSha(row.commitSha) }}</td>
                  <td>{{ row.model || '—' }}</td>
                  <td class="num">{{ formatNum(row.inputTokens) }}</td>
                  <td class="num">{{ formatNum(row.outputTokens) }}</td>
                  <td class="num">{{ formatNum(row.totalTokens) }}</td>
                </tr>
              </tbody>
            </table>
          </div>
          <p v-if="!usageDetailItems.length" class="usage-empty">{{ $t('aiConfig.usageEmpty') }}</p>
          <div v-else class="usage-pagination">
            <button type="button" class="link-button" :disabled="usageDetailPage <= 1" @click="goUsageDetailPage(usageDetailPage - 1)">上一页</button>
            <span class="usage-pagination-info">第 {{ usageDetailPage }} / {{ usageDetailTotalPages }} 页，共 {{ usageDetailTotal }} 条</span>
            <button type="button" class="link-button" :disabled="usageDetailPage >= usageDetailTotalPages" @click="goUsageDetailPage(usageDetailPage + 1)">下一页</button>
          </div>
        </template>
      </div>
    </div>

    <section class="ai-section" aria-labelledby="ai-github-heading" v-if="githubConfig !== undefined">
      <div class="ai-section-head">
        <h2 id="ai-github-heading" class="ai-section-title">{{ $t('aiConfig.sectionGithubTitle') }}</h2>
        <p class="ai-section-desc">{{ $t('aiConfig.sectionGithubDesc') }}</p>
        <div class="ai-external-links ai-section-links">
          <a href="https://github.com/settings/tokens" target="_blank" rel="noopener noreferrer" class="ai-doc-link">{{ $t('aiConfig.openGithubTokens') }} ↗</a>
          <a href="https://docs.github.com/en/authentication/keeping-your-account-and-data-secure/managing-your-personal-access-tokens" target="_blank" rel="noopener noreferrer" class="ai-doc-link">{{ $t('aiConfig.githubPatDoc') }} ↗</a>
        </div>
      </div>
      <div class="config-card github-config-card ai-section-card">
      <h3 class="config-card-title config-card-title--sub">{{ $t('githubConfig.title') }}</h3>
      <p class="config-card-desc">{{ $t('githubConfig.desc') }}</p>
      <form @submit.prevent="saveGitHubConfig">
        <div class="form-row">
          <label class="form-label">{{ $t('githubConfig.tokenLabel') }}</label>
          <p v-if="githubConfig.githubTokenMasked" class="api-key-set">{{ $t('githubConfig.tokenSetHint') }}（{{ githubConfig.githubTokenMasked }}）</p>
          <input
            v-model="githubForm.githubToken"
            type="password"
            autocomplete="off"
            :placeholder="githubConfig.hasGitHubToken ? $t('githubConfig.tokenPlaceholderKeep') : $t('githubConfig.tokenPlaceholder')"
            class="form-input"
          >
          <p class="form-hint">{{ $t('githubConfig.tokenHint') }}</p>
        </div>
        <div class="form-row">
          <label class="form-label">{{ $t('githubConfig.proxyLabel') }}</label>
          <input
            v-model="githubForm.githubApiProxy"
            type="text"
            autocomplete="off"
            :placeholder="$t('githubConfig.proxyPlaceholder')"
            class="form-input"
          >
        </div>
        <div class="form-actions">
          <button type="submit" class="primary-button" :disabled="githubSaving">{{ githubSaving ? $t('githubConfig.saving') : $t('githubConfig.save') }}</button>
          <span v-if="githubSaveMessage" class="save-message" :class="githubSaveSuccess ? 'success' : 'error'">{{ githubSaveMessage }}</span>
        </div>
      </form>
    </div>
    </section>

    <section class="ai-section" aria-labelledby="ai-mail-heading" v-if="mailConfig !== undefined">
      <div class="ai-section-head">
        <h2 id="ai-mail-heading" class="ai-section-title">{{ $t('aiConfig.sectionMailTitle') }}</h2>
        <p class="ai-section-desc">{{ $t('aiConfig.sectionMailDesc') }}</p>
        <p class="ai-inline-hint">{{ $t('aiConfig.smtpVendorHint') }}</p>
      </div>
      <div class="config-card mail-config-card ai-section-card">
      <h3 class="config-card-title config-card-title--sub">{{ $t('mailConfig.title') }}</h3>
      <p class="config-card-desc">{{ $t('mailConfig.desc') }}</p>
      <form @submit.prevent="saveMailConfig">
        <div class="form-row">
          <label class="form-label">{{ $t('mailConfig.publicBaseUrl') }}</label>
          <input
            v-model="mailForm.publicBaseUrl"
            type="text"
            autocomplete="off"
            :placeholder="$t('mailConfig.publicBaseUrlPh')"
            class="form-input"
          >
          <p class="form-hint">{{ $t('mailConfig.publicBaseUrlHint') }}</p>
        </div>
        <div class="form-row">
          <label class="form-label">{{ $t('mailConfig.smtpHost') }}</label>
          <input v-model="mailForm.smtpHost" type="text" autocomplete="off" class="form-input" placeholder="smtp.example.com">
        </div>
        <div class="form-row">
          <label class="form-label">{{ $t('mailConfig.smtpPort') }}</label>
          <input v-model.number="mailForm.smtpPort" type="number" autocomplete="off" class="form-input" placeholder="587">
        </div>
        <div class="form-row">
          <label class="form-label">{{ $t('mailConfig.smtpUsername') }}</label>
          <input v-model="mailForm.smtpUsername" type="text" autocomplete="off" class="form-input" placeholder="user@example.com">
        </div>
        <div class="form-row">
          <label class="form-label">{{ $t('mailConfig.smtpPassword') }}</label>
          <p v-if="mailConfig.smtpPasswordMasked" class="api-key-set">{{ $t('mailConfig.passwordSetHint') }}</p>
          <input
            v-model="mailForm.smtpPassword"
            type="password"
            autocomplete="off"
            :placeholder="mailConfig.smtpPasswordMasked ? $t('mailConfig.passwordKeepPh') : $t('mailConfig.passwordPh')"
            class="form-input"
          >
        </div>
        <div class="form-row">
          <label class="form-label">{{ $t('mailConfig.fromAddress') }}</label>
          <input v-model="mailForm.fromAddress" type="text" autocomplete="off" class="form-input" placeholder="noreply@example.com">
        </div>
        <div class="form-row">
          <label class="form-label">{{ $t('mailConfig.fromName') }}</label>
          <input v-model="mailForm.fromName" type="text" autocomplete="off" class="form-input" :placeholder="$t('mailConfig.fromNamePh')">
        </div>
        <div class="form-actions">
          <button type="submit" class="primary-button" :disabled="mailSaving">{{ mailSaving ? $t('mailConfig.saving') : $t('mailConfig.save') }}</button>
          <span v-if="mailSaveMessage" class="save-message" :class="mailSaveSuccess ? 'success' : 'error'">{{ mailSaveMessage }}</span>
          <span v-if="mailConfig && mailConfig.configured" class="save-message success">{{ $t('mailConfig.configured') }}</span>
        </div>
      </form>
    </div>
    </section>
  </div>
</template>

<script>
import { Chart as ChartJS, registerables } from 'chart.js'

ChartJS.register(...registerables)

const PAGE_SIZE = 20

export default {
  name: 'AiConfigView',
  data () {
    return {
      config: null,
      form: {
        apiKey: '',
        enabled: false,
        dailySummaryEnabled: false,
        model: 'deepseek-chat'
      },
      dailyRunLoading: false,
      dailyRunMessage: '',
      dailyRunOk: false,
      saving: false,
      saveMessage: '',
      saveSuccess: false,
      qwenConfig: null,
      qwenForm: {
        apiKey: '',
        enabled: false,
        model: 'qwen-omni'
      },
      qwenSaving: false,
      qwenSaveMessage: '',
      qwenSaveSuccess: false,
      githubConfig: undefined,
      githubForm: {
        githubToken: '',
        githubApiProxy: ''
      },
      githubSaving: false,
      githubSaveMessage: '',
      githubSaveSuccess: false,
      mailConfig: undefined,
      mailForm: {
        publicBaseUrl: '',
        smtpHost: '',
        smtpPort: 587,
        smtpUsername: '',
        smtpPassword: '',
        fromAddress: '',
        fromName: ''
      },
      mailSaving: false,
      mailSaveMessage: '',
      mailSaveSuccess: false,
      usage: null,
      usageLoading: false,
      usageDays: 30,
      usageDaily: [],
      usageQwen: null,
      usageQwenLoading: false,
      usageDaysQwen: 30,
      usageQwenDaily: [],
      chartInstances: { deepseekCall: null, deepseekToken: null, qwenCall: null, qwenToken: null },
      usageDetailOpen: false,
      usageDetailProvider: null,
      usageDetailDays: 30,
      usageDetailPage: 1,
      usageDetailPageSize: PAGE_SIZE,
      usageDetailTotal: 0,
      usageDetailItems: [],
      usageDetailLoading: false,
      /** 从服务端回填表单时跳过「开关自动保存」 */
      aiConfigHydrating: false,
      aiConfigAutoSaveTimer: null,
      exportGuardEnabled: false,
      exportGuardNew1: '',
      exportGuardNew2: '',
      exportGuardClearPwd: '',
      exportGuardSaving: false,
      exportGuardMessage: '',
      exportGuardMessageOk: false,
      exportModalOpen: false,
      exportModalPwd: '',
      exportSubmitting: false,
      exportSubmitError: '',
      exportResultJson: '',
      exportCopied: false
    }
  },
  created () {
    this.loadConfig()
    this.loadQwenConfig()
    this.loadGitHubConfig()
    this.loadMailConfig()
    this.loadExportGuard()
    this.loadUsage()
    this.loadUsageQwen()
    this.loadUsageDaily()
    this.loadUsageQwenDaily()
  },
  mounted () {
    this.$nextTick(() => {
      setTimeout(() => this.renderUsageCharts(), 150)
    })
  },
  beforeDestroy () {
    if (this.aiConfigAutoSaveTimer) {
      clearTimeout(this.aiConfigAutoSaveTimer)
      this.aiConfigAutoSaveTimer = null
    }
    const chartKeys = ['deepseekCall', 'deepseekToken', 'qwenCall', 'qwenToken']
    chartKeys.forEach(k => {
      if (this.chartInstances[k]) {
        this.chartInstances[k].destroy()
        this.chartInstances[k] = null
      }
    })
  },
  computed: {
    configLoadedHint () {
      if (!this.config) return false
      const hasKey = this.config.hasApiKey === true || !!(this.config.apiKeyMasked && String(this.config.apiKeyMasked).trim())
      const isEnabled = this.config.enabled === true || this.config.enabled === 'true' || this.config.enabled === 1
      return hasKey || isEnabled
    },
    usageDetailTitle () {
      const name = this.usageDetailProvider === 'qwen' ? '千问多模态' : 'DeepSeek'
      return `${name} 用量详情（近 ${this.usageDetailDays || 30} 天）`
    },
    usageDetailTotalPages () {
      if (this.usageDetailTotal <= 0) return 1
      return Math.ceil(this.usageDetailTotal / this.usageDetailPageSize) || 1
    }
  },
  watch: {
    /** Canvas 在 v-else-if="usage" 内，须等 summary 加载完成才挂载；仅监听 daily 会先渲染但 refs 为空 */
    usage () { this.$nextTick(() => this.renderUsageCharts()) },
    usageQwen () { this.$nextTick(() => this.renderUsageCharts()) },
    usageDaily () { this.$nextTick(() => this.renderUsageCharts()) },
    usageQwenDaily () { this.$nextTick(() => this.renderUsageCharts()) },
    'form.enabled' () { this.scheduleDeepSeekToggleAutoSave() },
    'form.dailySummaryEnabled' () { this.scheduleDeepSeekToggleAutoSave() }
  },
  methods: {
    coerceBool (v) {
      if (v === true || v === 1) return true
      if (v === false || v === 0) return false
      if (typeof v === 'string') {
        const s = v.trim().toLowerCase()
        if (s === 'true' || s === '1') return true
        if (s === 'false' || s === '0') return false
      }
      return false
    },
    buildDeepSeekSavePayload () {
      const payload = {
        enabled: !!this.form.enabled,
        dailySummaryEnabled: !!this.form.dailySummaryEnabled,
        model: (this.form.model && String(this.form.model).trim()) || 'deepseek-chat'
      }
      if (this.form.apiKey && !this.form.apiKey.includes('****')) {
        payload.apiKey = this.form.apiKey
      }
      return payload
    },
    scheduleDeepSeekToggleAutoSave () {
      if (this.aiConfigHydrating || !this.config) return
      if (this.aiConfigAutoSaveTimer) clearTimeout(this.aiConfigAutoSaveTimer)
      this.aiConfigAutoSaveTimer = setTimeout(() => this.persistDeepSeekTogglesSilent(), 450)
    },
    async persistDeepSeekTogglesSilent () {
      this.aiConfigAutoSaveTimer = null
      if (this.aiConfigHydrating || !this.config) return
      try {
        const resp = await this.$http.put('/admin/ai-analysis/config', this.buildDeepSeekSavePayload())
        if (resp.data && resp.data.code === 0) {
          await this.loadConfig()
        }
      } catch (e) { /* 静默：可稍后手动点保存 */ }
    },
    async loadConfig () {
      this.aiConfigHydrating = true
      try {
        const resp = await this.$http.get('/admin/ai-analysis/config')
        if (resp.data && resp.data.code === 0 && resp.data.data) {
          const data = resp.data.data
          this.config = data
          this.form.enabled = this.coerceBool(data.enabled)
          this.form.dailySummaryEnabled = this.coerceBool(data.dailySummaryEnabled)
          this.form.model = (data.model && String(data.model).trim()) || 'deepseek-chat'
          this.form.apiKey = ''
        }
      } catch (e) {
        if (e.response && e.response.status === 401) {
          this.$router.push({ name: 'login' })
        }
      } finally {
        this.$nextTick(() => { this.aiConfigHydrating = false })
      }
    },
    async loadQwenConfig () {
      try {
        const resp = await this.$http.get('/admin/ai-analysis/qwen-config')
        if (resp.data && resp.data.code === 0 && resp.data.data) {
          const data = resp.data.data
          this.qwenConfig = data
          this.qwenForm.enabled = data.enabled === true || data.enabled === 'true' || data.enabled === 1
          this.qwenForm.model = (data.model && String(data.model).trim()) || 'qwen-omni'
          this.qwenForm.apiKey = ''
        }
      } catch (e) {
        // 忽略，保留 qwenConfig 为空
      }
    },
    async saveConfig () {
      this.saving = true
      this.saveMessage = ''
      try {
        if (this.aiConfigAutoSaveTimer) {
          clearTimeout(this.aiConfigAutoSaveTimer)
          this.aiConfigAutoSaveTimer = null
        }
        const resp = await this.$http.put('/admin/ai-analysis/config', this.buildDeepSeekSavePayload())
        if (resp.data && resp.data.code === 0) {
          this.saveMessage = this.$t('aiConfig.saveSuccess')
          this.saveSuccess = true
          this.loadConfig()
          this.form.apiKey = ''
        } else {
          this.saveMessage = (resp.data && resp.data.message) || this.$t('aiConfig.saveFailed')
          this.saveSuccess = false
        }
      } catch (e) {
        this.saveMessage = (e.response && e.response.data && e.response.data.message) || this.$t('aiConfig.saveFailed')
        this.saveSuccess = false
      } finally {
        this.saving = false
      }
    },
    async runDailySummaryNow () {
      this.dailyRunLoading = true
      this.dailyRunMessage = ''
      try {
        const resp = await this.$http.post('/admin/ai-analysis/daily-summary/run')
        if (resp.data && resp.data.code === 0 && resp.data.data) {
          const n = resp.data.data.reposProcessed != null ? resp.data.data.reposProcessed : 0
          const d = resp.data.data.summaryDate || ''
          this.dailyRunOk = true
          this.dailyRunMessage = this.$t('aiConfig.runDailySummaryOk', { date: d, n })
        } else {
          this.dailyRunOk = false
          this.dailyRunMessage = (resp.data && resp.data.message) || this.$t('aiConfig.runDailySummaryFail')
        }
      } catch (e) {
        this.dailyRunOk = false
        this.dailyRunMessage = (e.response && e.response.data && e.response.data.message) || this.$t('aiConfig.runDailySummaryFail')
      } finally {
        this.dailyRunLoading = false
      }
    },
    async saveQwenConfig () {
      this.qwenSaving = true
      this.qwenSaveMessage = ''
      try {
        const payload = {
          enabled: this.qwenForm.enabled,
          model: this.qwenForm.model || 'qwen-omni'
        }
        if (this.qwenForm.apiKey && !this.qwenForm.apiKey.includes('****')) {
          payload.apiKey = this.qwenForm.apiKey
        }
        const resp = await this.$http.put('/admin/ai-analysis/qwen-config', payload)
        if (resp.data && resp.data.code === 0) {
          this.qwenSaveMessage = this.$t('aiConfig.saveSuccess')
          this.qwenSaveSuccess = true
          this.loadQwenConfig()
          this.qwenForm.apiKey = ''
        } else {
          this.qwenSaveMessage = (resp.data && resp.data.message) || this.$t('aiConfig.saveFailed')
          this.qwenSaveSuccess = false
        }
      } catch (e) {
        this.qwenSaveMessage = (e.response && e.response.data && e.response.data.message) || this.$t('aiConfig.saveFailed')
        this.qwenSaveSuccess = false
      } finally {
        this.qwenSaving = false
      }
    },
    async loadGitHubConfig () {
      try {
        const resp = await this.$http.get('/admin/config/github')
        if (resp.data && resp.data.code === 0 && resp.data.data) {
          const d = resp.data.data
          this.githubConfig = d
          this.githubForm.githubToken = ''
          this.githubForm.githubApiProxy = d.githubApiProxy || ''
        } else {
          this.githubConfig = {}
        }
      } catch (e) {
        if (e.response && e.response.status === 401) this.$router.push({ name: 'login' })
        else this.githubConfig = {}
      }
    },
    async loadMailConfig () {
      try {
        const resp = await this.$http.get('/admin/config/mail')
        if (resp.data && resp.data.code === 0 && resp.data.data) {
          const d = resp.data.data
          this.mailConfig = d
          this.mailForm.publicBaseUrl = d.publicBaseUrl || ''
          this.mailForm.smtpHost = d.smtpHost || ''
          this.mailForm.smtpPort = d.smtpPort != null ? Number(d.smtpPort) : 587
          this.mailForm.smtpUsername = d.smtpUsername || ''
          this.mailForm.smtpPassword = ''
          this.mailForm.fromAddress = d.fromAddress || ''
          this.mailForm.fromName = d.fromName || ''
        } else {
          this.mailConfig = {}
        }
      } catch (e) {
        if (e.response && e.response.status === 401) this.$router.push({ name: 'login' })
        else this.mailConfig = {}
      }
    },
    async loadUsage () {
      this.usageLoading = true
      try {
        const resp = await this.$http.get('/admin/ai-analysis/usage', { params: { days: this.usageDays, provider: 'deepseek' } })
        if (resp.data && resp.data.code === 0 && resp.data.data) {
          this.usage = resp.data.data
          if (!this.usage.summary) this.usage.summary = {}
        } else {
          this.usage = { summary: {}, items: [] }
        }
      } catch (e) {
        this.usage = { summary: {}, items: [] }
      } finally {
        this.usageLoading = false
      }
    },
    async loadUsageQwen () {
      this.usageQwenLoading = true
      try {
        const resp = await this.$http.get('/admin/ai-analysis/usage', { params: { days: this.usageDaysQwen, provider: 'qwen' } })
        if (resp.data && resp.data.code === 0 && resp.data.data) {
          this.usageQwen = resp.data.data
          if (!this.usageQwen.summary) this.usageQwen.summary = {}
        } else {
          this.usageQwen = { summary: {}, items: [] }
        }
      } catch (e) {
        this.usageQwen = { summary: {}, items: [] }
      } finally {
        this.usageQwenLoading = false
      }
    },
    async loadUsageDaily () {
      try {
        const resp = await this.$http.get('/admin/ai-analysis/usage/daily', { params: { days: this.usageDays, provider: 'deepseek' } })
        if (resp.data && resp.data.code === 0 && resp.data.data && Array.isArray(resp.data.data.daily)) {
          this.usageDaily = this.fillDailyRange(resp.data.data.daily, this.usageDays)
        } else {
          this.usageDaily = this.fillDailyRange([], this.usageDays)
        }
      } catch (e) {
        this.usageDaily = this.fillDailyRange([], this.usageDays)
      }
    },
    async loadUsageQwenDaily () {
      try {
        const resp = await this.$http.get('/admin/ai-analysis/usage/daily', { params: { days: this.usageDaysQwen, provider: 'qwen' } })
        if (resp.data && resp.data.code === 0 && resp.data.data && Array.isArray(resp.data.data.daily)) {
          this.usageQwenDaily = this.fillDailyRange(resp.data.data.daily, this.usageDaysQwen)
        } else {
          this.usageQwenDaily = this.fillDailyRange([], this.usageDaysQwen)
        }
      } catch (e) {
        this.usageQwenDaily = this.fillDailyRange([], this.usageDaysQwen)
      }
    },
    fillDailyRange (daily, days) {
      const map = {}
      ;(daily || []).forEach(d => {
        const dateStr = d.date != null ? String(d.date).slice(0, 10) : ''
        if (dateStr) map[dateStr] = { date: dateStr, callCount: Number(d.callCount) || 0, totalTokens: Number(d.totalTokens) || 0, inputTokens: Number(d.inputTokens) || 0, outputTokens: Number(d.outputTokens) || 0 }
      })
      const result = []
      const end = new Date()
      for (let i = days - 1; i >= 0; i--) {
        const d = new Date(end)
        d.setDate(d.getDate() - i)
        const dateStr = d.getFullYear() + '-' + String(d.getMonth() + 1).padStart(2, '0') + '-' + String(d.getDate()).padStart(2, '0')
        result.push(map[dateStr] || { date: dateStr, callCount: 0, totalTokens: 0, inputTokens: 0, outputTokens: 0 })
      }
      return result
    },
    onUsageDaysChange (provider) {
      if (provider === 'deepseek') {
        this.loadUsage()
        this.loadUsageDaily()
      } else {
        this.loadUsageQwen()
        this.loadUsageQwenDaily()
      }
    },
    refreshUsageDeepseek () {
      this.loadUsage()
      this.loadUsageDaily()
    },
    refreshUsageQwen () {
      this.loadUsageQwen()
      this.loadUsageQwenDaily()
    },
    openUsageDetail (provider, days) {
      this.usageDetailProvider = provider
      this.usageDetailDays = days
      this.usageDetailPage = 1
      this.usageDetailTotal = 0
      this.usageDetailItems = []
      this.usageDetailOpen = true
      this.loadUsageDetailPage()
    },
    closeUsageDetail () {
      this.usageDetailOpen = false
    },
    async loadUsageDetailPage () {
      if (!this.usageDetailProvider) return
      this.usageDetailLoading = true
      try {
        const resp = await this.$http.get('/admin/ai-analysis/usage', {
          params: { days: this.usageDetailDays, provider: this.usageDetailProvider, page: this.usageDetailPage, pageSize: this.usageDetailPageSize }
        })
        if (resp.data && resp.data.code === 0 && resp.data.data) {
          const d = resp.data.data
          this.usageDetailItems = d.items || []
          this.usageDetailTotal = Number(d.total) || 0
        } else {
          this.usageDetailItems = []
          this.usageDetailTotal = 0
        }
      } catch (e) {
        this.usageDetailItems = []
        this.usageDetailTotal = 0
      } finally {
        this.usageDetailLoading = false
      }
    },
    goUsageDetailPage (page) {
      if (page < 1 || page > this.usageDetailTotalPages) return
      this.usageDetailPage = page
      this.loadUsageDetailPage()
    },
    /** Vue 2 单节点 ref 为元素；极少数情况下为数组 */
    resolveChartCanvasRef (name) {
      const r = this.$refs[name]
      if (!r) return null
      return r instanceof HTMLCanvasElement ? r : (Array.isArray(r) ? r[0] : r)
    },
    renderUsageCharts () {
      this.renderOneUsageChart('deepseek', this.usageDaily, 'deepseekCall', 'deepseekToken')
      this.renderOneUsageChart('qwen', this.usageQwenDaily, 'qwenCall', 'qwenToken')
    },
    renderOneUsageChart (provider, daily, callRef, tokenRef) {
      this.$nextTick(() => {
        const days = daily || []
        const formatLabel = (d) => {
          const v = d.date
          if (!v) return ''
          const s = String(v).slice(0, 10)
          return s.length >= 10 ? s.slice(5) : s
        }
        const labels = days.map(formatLabel)
        const callData = days.map(d => d.callCount || 0)
        const tokenData = days.map(d => d.totalTokens || 0)
        const isQwen = provider === 'qwen'
        const blue = 'rgba(37, 99, 235, 0.7)'
        const blueBorder = 'rgb(37, 99, 235)'
        const teal = 'rgba(20, 184, 166, 0.7)'
        const tealBorder = 'rgb(20, 184, 166)'
        const callEl = this.resolveChartCanvasRef(callRef)
        const tokenEl = this.resolveChartCanvasRef(tokenRef)
        if (callEl) {
          if (this.chartInstances[callRef]) this.chartInstances[callRef].destroy()
          this.chartInstances[callRef] = new ChartJS(callEl, {
            type: 'line',
            data: { labels, datasets: [{ label: 'API 请求次数', data: callData, borderColor: blueBorder, backgroundColor: blue, fill: true, tension: 0.2 }] },
            options: {
              responsive: true,
              maintainAspectRatio: false,
              plugins: { legend: { display: false } },
              scales: { y: { beginAtZero: true } }
            }
          })
        }
        if (tokenEl) {
          if (this.chartInstances[tokenRef]) this.chartInstances[tokenRef].destroy()
          this.chartInstances[tokenRef] = new ChartJS(tokenEl, {
            type: 'bar',
            data: { labels, datasets: [{ label: 'Tokens', data: tokenData, backgroundColor: isQwen ? teal : blue, borderColor: isQwen ? tealBorder : blueBorder, borderWidth: 1 }] },
            options: {
              responsive: true,
              maintainAspectRatio: false,
              plugins: { legend: { display: false } },
              scales: { y: { beginAtZero: true } }
            }
          })
        }
      })
    },
    formatNum (v) {
      if (v == null || v === '') return '0'
      const n = Number(v)
      if (isNaN(n)) return String(v)
      return n.toLocaleString()
    },
    formatUsageTime (t) {
      if (!t) return '—'
      try {
        return new Date(t).toLocaleString()
      } catch (e) {
        return String(t)
      }
    },
    shortSha (sha) {
      if (!sha || sha.length < 8) return sha || '—'
      return sha.substring(0, 7)
    },
    async loadExportGuard () {
      try {
        const resp = await this.$http.get('/admin/config/export-guard')
        if (resp.data && resp.data.code === 0 && resp.data.data) {
          this.exportGuardEnabled = !!resp.data.data.enabled
        }
      } catch (e) {
        // 静默
      }
    },
    async saveExportGuard () {
      this.exportGuardMessage = ''
      this.exportGuardMessageOk = false
      if (this.exportGuardNew1 !== this.exportGuardNew2) {
        this.exportGuardMessage = this.$t('aiConfig.exportGuardMismatch')
        return
      }
      if (!this.exportGuardNew1 || String(this.exportGuardNew1).length < 8) {
        this.exportGuardMessage = this.$t('aiConfig.exportGuardMinLen')
        return
      }
      this.exportGuardSaving = true
      try {
        const resp = await this.$http.put('/admin/config/export-guard', {
          password: this.exportGuardNew1,
          passwordConfirm: this.exportGuardNew2
        })
        if (resp.data && resp.data.code === 0) {
          this.exportGuardMessageOk = true
          this.exportGuardMessage = this.$t('aiConfig.exportGuardSaveOk')
          this.exportGuardNew1 = ''
          this.exportGuardNew2 = ''
          await this.loadExportGuard()
        } else {
          this.exportGuardMessage = (resp.data && resp.data.message) || this.$t('aiConfig.saveFailed')
        }
      } catch (e) {
        this.exportGuardMessage = (e.response && e.response.data && e.response.data.message) || this.$t('aiConfig.saveFailed')
      } finally {
        this.exportGuardSaving = false
      }
    },
    async clearExportGuard () {
      this.exportGuardMessage = ''
      this.exportGuardMessageOk = false
      if (!this.exportGuardClearPwd) {
        this.exportGuardMessage = this.$t('aiConfig.exportGuardNeedCurrent')
        return
      }
      this.exportGuardSaving = true
      try {
        const resp = await this.$http.put('/admin/config/export-guard', {
          clear: true,
          currentPassword: this.exportGuardClearPwd
        })
        if (resp.data && resp.data.code === 0) {
          this.exportGuardMessageOk = true
          this.exportGuardMessage = this.$t('aiConfig.exportGuardClearOk')
          this.exportGuardClearPwd = ''
          await this.loadExportGuard()
        } else {
          this.exportGuardMessage = (resp.data && resp.data.message) || this.$t('aiConfig.saveFailed')
        }
      } catch (e) {
        this.exportGuardMessage = (e.response && e.response.data && e.response.data.message) || this.$t('aiConfig.saveFailed')
      } finally {
        this.exportGuardSaving = false
      }
    },
    openExportModal () {
      if (!this.exportGuardEnabled) return
      this.exportModalOpen = true
      this.exportModalPwd = ''
      this.exportSubmitError = ''
      this.exportResultJson = ''
      this.exportCopied = false
    },
    closeExportModal () {
      this.exportModalOpen = false
      this.exportModalPwd = ''
      this.exportSubmitError = ''
      this.exportResultJson = ''
      this.exportCopied = false
    },
    async submitSensitiveExport () {
      this.exportSubmitError = ''
      this.exportResultJson = ''
      this.exportCopied = false
      if (!this.exportModalPwd) {
        this.exportSubmitError = this.$t('aiConfig.exportModalNeedPassword')
        return
      }
      this.exportSubmitting = true
      try {
        const resp = await this.$http.post('/admin/config/sensitive-export', {
          secondaryPassword: this.exportModalPwd
        })
        if (resp.data && resp.data.code === 0 && resp.data.data) {
          this.exportResultJson = JSON.stringify(resp.data.data, null, 2)
        } else {
          this.exportSubmitError = (resp.data && resp.data.message) || this.$t('aiConfig.exportLoadFail')
        }
      } catch (e) {
        this.exportSubmitError = (e.response && e.response.data && e.response.data.message) || this.$t('aiConfig.exportLoadFail')
      } finally {
        this.exportSubmitting = false
      }
    },
    async copyExportJson () {
      if (!this.exportResultJson) return
      try {
        await navigator.clipboard.writeText(this.exportResultJson)
        this.exportCopied = true
        setTimeout(() => { this.exportCopied = false }, 2000)
      } catch (err) {
        this.exportSubmitError = this.$t('aiConfig.exportCopyFail')
      }
    },
    async saveGitHubConfig () {
      this.githubSaving = true
      this.githubSaveMessage = ''
      try {
        const payload = {
          githubApiProxy: this.githubForm.githubApiProxy || ''
        }
        if (this.githubForm.githubToken && !this.githubForm.githubToken.includes('****')) {
          payload.githubToken = this.githubForm.githubToken
        }
        const resp = await this.$http.put('/admin/config/github', payload)
        if (resp.data && resp.data.code === 0) {
          this.githubSaveMessage = this.$t('githubConfig.saveSuccess')
          this.githubSaveSuccess = true
          this.loadGitHubConfig()
          this.githubForm.githubToken = ''
        } else {
          this.githubSaveMessage = (resp.data && resp.data.message) || this.$t('githubConfig.saveFailed')
          this.githubSaveSuccess = false
        }
      } catch (e) {
        this.githubSaveMessage = (e.response && e.response.data && e.response.data.message) || this.$t('githubConfig.saveFailed')
        this.githubSaveSuccess = false
      } finally {
        this.githubSaving = false
      }
    }
    ,
    async saveMailConfig () {
      this.mailSaving = true
      this.mailSaveMessage = ''
      try {
        const payload = {
          publicBaseUrl: this.mailForm.publicBaseUrl || '',
          smtpHost: this.mailForm.smtpHost || '',
          smtpPort: this.mailForm.smtpPort != null ? Number(this.mailForm.smtpPort) : null,
          smtpUsername: this.mailForm.smtpUsername || '',
          fromAddress: this.mailForm.fromAddress || '',
          fromName: this.mailForm.fromName || ''
        }
        if (this.mailForm.smtpPassword && !this.mailForm.smtpPassword.includes('****')) {
          payload.smtpPassword = this.mailForm.smtpPassword
        }
        const resp = await this.$http.put('/admin/config/mail', payload)
        if (resp.data && resp.data.code === 0) {
          this.mailSaveMessage = this.$t('mailConfig.saveSuccess')
          this.mailSaveSuccess = true
          this.loadMailConfig()
          this.mailForm.smtpPassword = ''
        } else {
          this.mailSaveMessage = (resp.data && resp.data.message) || this.$t('mailConfig.saveFailed')
          this.mailSaveSuccess = false
        }
      } catch (e) {
        this.mailSaveMessage = (e.response && e.response.data && e.response.data.message) || this.$t('mailConfig.saveFailed')
        this.mailSaveSuccess = false
      } finally {
        this.mailSaving = false
      }
    }
  }
}
</script>

<style scoped>
.ai-config-shell {
  max-width: 1120px;
  margin: 0 auto;
  padding: 0 var(--space-md) var(--space-xxl);
  box-sizing: border-box;
}

.ai-config-hero {
  margin-bottom: var(--space-lg);
}

.page-lead {
  margin: var(--space-sm) 0 0;
  font-size: var(--font-size-base);
  line-height: 1.55;
  color: var(--text-secondary);
  max-width: 52rem;
}

.ai-guide-card {
  background: linear-gradient(135deg, rgba(20, 86, 240, 0.06) 0%, var(--bg-card) 48%, var(--bg-card) 100%);
  border: 1px solid rgba(20, 86, 240, 0.14);
  border-radius: 12px;
  padding: var(--space-lg) var(--space-xl);
  margin-bottom: var(--space-xl);
  box-shadow: 0 1px 3px rgba(15, 23, 42, 0.06);
}

.ai-guide-head {
  margin-bottom: var(--space-md);
}

.ai-guide-kicker {
  display: inline-block;
  font-size: 11px;
  font-weight: var(--font-weight-semibold);
  letter-spacing: 0.06em;
  text-transform: uppercase;
  color: var(--brand-blue);
  margin-bottom: var(--space-xs);
}

.ai-guide-title {
  margin: 0;
  font-size: 17px;
  font-weight: var(--font-weight-semibold);
  color: var(--text-primary);
  letter-spacing: -0.02em;
}

.ai-guide-steps {
  margin: 0;
  padding: 0;
  list-style: none;
  counter-reset: ai-step;
}

.ai-guide-steps li {
  display: flex;
  gap: var(--space-md);
  align-items: flex-start;
  padding: var(--space-sm) 0;
  border-bottom: 1px solid var(--border-primary);
  font-size: var(--font-size-sm);
  line-height: 1.55;
  color: var(--text-secondary);
}

.ai-guide-steps li:last-child {
  border-bottom: none;
  padding-bottom: 0;
}

.ai-step-num {
  flex-shrink: 0;
  width: 26px;
  height: 26px;
  border-radius: 8px;
  background: var(--brand-blue);
  color: #fff;
  font-size: 13px;
  font-weight: var(--font-weight-semibold);
  display: flex;
  align-items: center;
  justify-content: center;
  margin-top: 1px;
}

.ai-step-text {
  flex: 1;
  min-width: 0;
}

.ai-guide-cta {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: var(--space-md);
  margin-top: var(--space-lg);
  padding-top: var(--space-md);
  border-top: 1px dashed var(--border-primary);
}

.ai-guide-cta-hint {
  font-size: var(--font-size-sm);
  color: var(--text-tertiary);
}

.ai-cta-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 8px 18px;
  border-radius: 8px;
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-medium);
  text-decoration: none;
  transition: var(--transition-fast);
}

.ai-cta-btn--primary {
  background: var(--brand-blue);
  color: #fff;
  box-shadow: 0 1px 2px rgba(20, 86, 240, 0.25);
}

.ai-cta-btn--primary:hover {
  background: var(--brand-blue-hover);
  color: #fff;
}

.ai-section {
  margin-bottom: var(--space-xl);
}

.ai-section-head {
  margin-bottom: var(--space-md);
}

.ai-section-title {
  margin: 0 0 var(--space-xs);
  font-size: 18px;
  font-weight: var(--font-weight-semibold);
  color: var(--text-primary);
  letter-spacing: -0.02em;
}

.ai-section-desc {
  margin: 0 0 var(--space-sm);
  font-size: var(--font-size-sm);
  line-height: 1.5;
  color: var(--text-secondary);
  max-width: 48rem;
}

.ai-section-links {
  margin-top: var(--space-sm);
}

.ai-inline-hint {
  margin: 0;
  font-size: var(--font-size-sm);
  color: var(--text-tertiary);
  line-height: 1.45;
  max-width: 48rem;
}

.ai-section-card {
  margin-top: 0;
}

.ai-column-head {
  padding: var(--space-md) var(--space-lg);
  background: var(--bg-page);
  border-bottom: 1px solid var(--border-primary);
}

.ai-column-head-row {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-start;
  justify-content: space-between;
  gap: var(--space-sm) var(--space-md);
}

.ai-column-title {
  margin: 0;
  font-size: 15px;
  font-weight: var(--font-weight-semibold);
  color: var(--text-primary);
}

.ai-column-sub {
  margin: var(--space-sm) 0 0;
  font-size: var(--font-size-sm);
  color: var(--text-tertiary);
  line-height: 1.45;
}

.ai-external-links {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-sm) var(--space-md);
  align-items: center;
}

.ai-doc-link {
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-medium);
  color: var(--brand-blue);
  text-decoration: none;
  white-space: nowrap;
}

.ai-doc-link:hover {
  text-decoration: underline;
  color: var(--brand-blue-hover);
}

.provider-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: var(--space-lg);
  margin-bottom: 0;
}

@media (max-width: 900px) {
  .provider-grid {
    grid-template-columns: 1fr;
  }
}

.provider-panel {
  background: var(--bg-card);
  border: 1px solid var(--border-primary);
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 1px 3px rgba(15, 23, 42, 0.06);
  display: flex;
  flex-direction: column;
  min-height: 0;
}

.provider-panel .config-card {
  border: none;
  border-radius: 0;
  margin-top: 0;
  box-shadow: none;
}

.provider-panel .config-card:hover {
  box-shadow: none;
}

.provider-panel .token-usage-card {
  border-top: 1px solid var(--border-primary);
}
.provider-panel .placeholder {
  border: none;
  margin: 0;
  padding: var(--space-xl);
}
.provider-panel .config-loaded-hint {
  margin: 0 0 var(--space-lg) 0;
}
.config-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: var(--space-xl);
  margin-bottom: var(--space-xl);
}
@media (max-width: 768px) {
  .config-row {
    grid-template-columns: 1fr;
  }
}
.usage-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: var(--space-xl);
  margin-bottom: var(--space-xl);
}
@media (max-width: 768px) {
  .usage-row {
    grid-template-columns: 1fr;
  }
}
.token-usage-card {
  margin-top: 0;
}
.qwen-config-card {
  margin-top: 0;
}
.page-title {
  font-size: var(--font-size-xxl);
  margin: 0;
  color: var(--text-primary);
  font-weight: var(--font-weight-bold);
}
.config-loaded-hint {
  font-size: var(--font-size-sm);
  color: var(--success);
  margin: 0 0 var(--space-lg) 0;
  padding: var(--space-sm) var(--space-md);
  background: var(--success-bg);
  border-radius: var(--radius-sm);
}
.api-key-set {
  font-size: var(--font-size-sm);
  color: var(--success);
  margin: 0 0 var(--space-sm) 0;
}
.config-card {
  background: var(--bg-card);
  border: 1px solid var(--border-primary);
  border-radius: var(--radius-md);
  padding: var(--space-xl);
  box-shadow: var(--shadow-sm);
  transition: var(--transition-normal);
}
.config-card:hover {
  box-shadow: var(--shadow-md);
}
.usage-summary {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-lg) var(--space-xl);
  margin-bottom: var(--space-md);
}
.usage-summary-item {
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.usage-summary-value {
  font-size: var(--font-size-xl);
  font-weight: var(--font-weight-semibold);
  color: var(--text-primary);
}
.usage-summary-label {
  font-size: var(--font-size-xs);
  color: var(--text-secondary);
}
.usage-since {
  font-size: var(--font-size-sm);
  color: var(--text-secondary);
  margin: 0 0 var(--space-md);
}
.usage-actions {
  display: flex;
  align-items: center;
  gap: var(--space-md);
  margin-bottom: var(--space-lg);
}
.usage-days-select {
  padding: var(--space-xs) var(--space-sm);
  border: 1px solid var(--border-primary);
  border-radius: var(--radius-sm);
  font-size: var(--font-size-sm);
  color: var(--text-primary);
  background: var(--bg-input);
  transition: var(--transition-fast);
}
.usage-days-select:hover {
  border-color: var(--border-input-hover);
}
.usage-days-select:focus {
  outline: none;
  border-color: var(--border-input-focus);
  box-shadow: 0 0 0 2px rgba(20, 86, 240, 0.15);
}
.link-button {
  background: none;
  border: none;
  color: var(--text-link);
  cursor: pointer;
  font-size: var(--font-size-sm);
  padding: 0;
  transition: var(--transition-fast);
}
.link-button:hover {
  text-decoration: underline;
  color: var(--text-link-hover);
}
.link-button:disabled {
  color: var(--text-disabled);
  cursor: not-allowed;
  text-decoration: none;
}
.usage-charts {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: var(--space-lg);
  margin-bottom: var(--space-lg);
}
.usage-chart-block {
  min-height: 120px;
}
.usage-chart-title {
  font-size: var(--font-size-xs);
  color: var(--text-secondary);
  margin-bottom: var(--space-xs);
}
.usage-chart-wrap {
  height: 120px;
  position: relative;
}
@media (max-width: 500px) {
  .usage-charts {
    grid-template-columns: 1fr;
  }
}
.usage-detail-actions {
  margin-top: var(--space-sm);
}
.primary-button-outline {
  background: var(--bg-card);
  color: var(--brand-blue);
  border: 1px solid var(--brand-blue);
}
.primary-button-outline:hover {
  background: var(--brand-blue-light);
}
.usage-detail-modal {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.4);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: var(--z-modal);
}
.usage-detail-dialog {
  background: var(--bg-card);
  border-radius: var(--radius-md);
  max-width: 90vw;
  width: 800px;
  max-height: 85vh;
  display: flex;
  flex-direction: column;
  box-shadow: var(--shadow-lg);
}
.export-modal-dialog {
  width: min(520px, 92vw);
  max-height: 90vh;
}
.export-modal-body {
  padding: var(--space-lg) var(--space-xl);
  overflow: auto;
}
.export-modal-lead {
  margin-top: 0;
  margin-bottom: var(--space-md);
}
.export-json-out {
  width: 100%;
  margin-top: var(--space-md);
  padding: var(--space-sm) var(--space-md);
  font-size: var(--font-size-xs);
  font-family: ui-monospace, monospace;
  border: 1px solid var(--border-primary);
  border-radius: var(--radius-sm);
  background: var(--bg-hover);
  color: var(--text-primary);
  resize: vertical;
  box-sizing: border-box;
}
.export-copy-btn {
  margin-top: var(--space-sm);
}
.export-guard-status {
  margin: 0 0 var(--space-md);
  font-size: var(--font-size-sm);
  color: var(--text-secondary);
  line-height: 1.5;
}
.export-guard-status--on {
  color: var(--text-primary);
}
.ai-section--guard .ai-section-card {
  margin-top: 0;
}
.form-actions--compact {
  margin-top: var(--space-md);
  padding-top: var(--space-md);
  border-top: none;
  flex-wrap: wrap;
}
.guard-actions-row {
  align-items: center;
}
.usage-detail-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: var(--space-lg) var(--space-xl);
  border-bottom: 1px solid var(--border-primary);
}
.usage-detail-header h3 {
  margin: 0;
  font-size: var(--font-size-lg);
  color: var(--text-primary);
}
.usage-detail-close {
  background: none;
  border: none;
  font-size: 24px;
  line-height: 1;
  color: var(--text-secondary);
  cursor: pointer;
  padding: 0 var(--space-xs);
  transition: var(--transition-fast);
}
.usage-detail-close:hover {
  color: var(--text-primary);
}
.usage-detail-dialog .usage-table-wrap {
  flex: 1;
  overflow: auto;
  padding: var(--space-lg) var(--space-xl);
}
.usage-pagination {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: var(--space-lg);
  padding: var(--space-md) var(--space-xl);
  border-top: 1px solid var(--border-primary);
}
.usage-pagination-info {
  font-size: var(--font-size-sm);
  color: var(--text-secondary);
}
.usage-table-wrap {
  overflow-x: auto;
}
.usage-table {
  width: 100%;
  border-collapse: collapse;
  font-size: var(--font-size-sm);
}
.usage-table th,
.usage-table td {
  padding: var(--space-sm) var(--space-md);
  text-align: left;
  border-bottom: 1px solid var(--border-primary);
  color: var(--text-primary);
}
.usage-table th {
  background: var(--bg-hover);
  font-weight: var(--font-weight-semibold);
  font-size: var(--font-size-xs);
  color: var(--text-secondary);
}
.usage-table tbody tr:hover {
  background: var(--bg-hover);
}
.usage-table th.num,
.usage-table td.num {
  text-align: right;
}
.usage-table .mono {
  font-family: var(--font-mono);
}
.repo-cell {
  max-width: 180px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.usage-empty {
  font-size: var(--font-size-sm);
  color: var(--text-secondary);
  margin: 0;
}
.placeholder.small {
  padding: var(--space-md);
}
.github-config-card,
.mail-config-card {
  margin-top: 0;
}
.config-card-title {
  font-size: var(--font-size-lg);
  margin: 0 0 var(--space-sm);
  color: var(--text-primary);
  font-weight: var(--font-weight-semibold);
}
.config-card-title--sub {
  font-size: var(--font-size-base);
}
.config-card-title--usage {
  font-size: var(--font-size-base);
  margin-bottom: var(--space-xs);
}
.config-card-desc {
  font-size: var(--font-size-sm);
  color: var(--text-secondary);
  margin: 0 0 var(--space-xl);
}
.form-row {
  margin-bottom: var(--space-xl);
}
.form-label {
  display: block;
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-medium);
  color: var(--text-secondary);
  margin-bottom: var(--space-xs);
}
.form-value {
  font-size: var(--font-size-base);
  color: var(--text-secondary);
}
.form-value--badge {
  display: inline-block;
  padding: 4px 10px;
  border-radius: 6px;
  background: var(--bg-page);
  border: 1px solid var(--border-primary);
  font-size: var(--font-size-sm);
  color: var(--text-primary);
}
.form-input {
  width: 100%;
  padding: var(--space-sm) var(--space-md);
  border: 1px solid var(--border-input);
  border-radius: var(--radius-sm);
  font-size: var(--font-size-base);
  box-sizing: border-box;
  color: var(--text-primary);
  background: var(--bg-input);
  transition: var(--transition-fast);
}
.form-input:hover {
  border-color: var(--border-input-hover);
}
.form-input:focus {
  outline: none;
  border-color: var(--border-input-focus);
  box-shadow: 0 0 0 2px rgba(20, 86, 240, 0.15);
}
.form-hint {
  font-size: var(--font-size-xs);
  color: var(--text-tertiary);
  margin-top: var(--space-xs);
}
.form-hint.subtle {
  color: var(--text-disabled);
  font-style: italic;
}
.daily-run-row {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: var(--space-md);
}
.daily-run-row .inline-msg {
  margin: 0;
}
.checkbox-label {
  display: inline-flex;
  align-items: center;
  gap: var(--space-sm);
  font-size: var(--font-size-base);
  cursor: pointer;
  color: var(--text-primary);
}
.checkbox-label input {
  margin: 0;
}
.form-actions {
  display: flex;
  align-items: center;
  gap: var(--space-md);
  margin-top: var(--space-xl);
  padding-top: var(--space-lg);
  border-top: 1px solid var(--border-primary);
}
.primary-button {
  padding: var(--space-sm) var(--space-xl);
  border-radius: var(--radius-sm);
  border: none;
  background: var(--brand-blue);
  color: #fff;
  font-size: var(--font-size-base);
  cursor: pointer;
  font-weight: var(--font-weight-medium);
  transition: var(--transition-fast);
  box-shadow: var(--shadow-btn);
}
.primary-button:hover {
  background: var(--brand-blue-hover);
}
.primary-button:disabled {
  opacity: 0.45;
  cursor: not-allowed;
}
.save-message {
  font-size: var(--font-size-sm);
}
.save-message.success {
  color: var(--success);
}
.save-message.error {
  color: var(--danger);
}
.placeholder {
  padding: var(--space-xl);
  text-align: center;
  color: var(--text-secondary);
}
</style>
