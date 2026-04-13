<template>
  <div class="console-shell" :class="{ 'dashboard-embedded-compact': embeddedCompact }">
    <div class="console-inner">
      <div class="dashboard" :class="{ 'dash-modern': !collabDataBoardOnly }">
        <!-- 指挥中心：统筹监测类 KPI + 状态条 + 主体信息 -->
        <section v-if="!collabDataBoardOnly" class="dash-command console-elevated">
          <div class="dash-command-bg" aria-hidden="true"></div>
          <div class="dash-command-top">
            <div class="dash-command-intro">
              <p class="dash-eyebrow">{{ $t('dashboard.workbenchEyebrow') }}</p>
              <h1 class="dash-title">{{ $t('dashboard.workbenchTitle') }}</h1>
              <p class="dash-mission">{{ $t('dashboard.workbenchMission') }}</p>
              <div class="dash-status-strip" role="status">
                <router-link to="/api-config" class="dash-chip dash-chip--link">{{ $t('dashboard.statusStripApi') }}</router-link>
                <span class="dash-chip" :class="{ 'dash-chip--ok': aiHubDeepseekOk, 'dash-chip--warn': !aiHub.loading && !aiHubDeepseekOk }">
                  <span class="dash-chip-glow" aria-hidden="true"></span>
                  {{ $t('dashboard.hubApiDeepSeek') }} · {{ aiHub.loading ? '…' : (aiHubDeepseekOk ? $t('dashboard.statusOk') : $t('dashboard.statusCheck')) }}
                </span>
                <span class="dash-chip" :class="{ 'dash-chip--ok': aiHubQwenOk, 'dash-chip--warn': !aiHub.loading && !aiHubQwenOk }">
                  <span class="dash-chip-glow" aria-hidden="true"></span>
                  {{ $t('dashboard.hubApiQwen') }} · {{ aiHub.loading ? '…' : (aiHubQwenOk ? $t('dashboard.statusOk') : $t('dashboard.statusCheck')) }}
                </span>
                <span class="dash-chip dash-chip--neutral">
                  {{ $t('dashboard.statusStripTeam', { active: teamActiveTodayCount, total: teamTotalLabel }) }}
                </span>
                <router-link to="/nexus" class="dash-chip dash-chip--link dash-chip--neutral">
                  {{ nexusHub.loading ? '…' : $t('dashboard.statusStripNexus', { n: nexusHub.accountCount }) }}
                </router-link>
                <router-link to="/cloud-dev" class="dash-chip dash-chip--link dash-chip--neutral">{{ $t('dashboard.statusStripCloudDev') }}</router-link>
                <router-link
                  v-if="workspaceSummary"
                  to="/subscription"
                  class="dash-chip dash-chip--link dash-chip--ent"
                  :title="$t('dashboard.entitlementChipHint')"
                >
                  {{ workspaceSummary.planDisplayLabel }}
                  <template v-if="workspaceSummary.subscriptionEndsAt"> · {{ $t('dashboard.entitlementUntil', { date: formatWsDate(workspaceSummary.subscriptionEndsAt) }) }}</template>
                </router-link>
              </div>
            </div>
            <div class="dash-command-identity">
              <div class="dash-identity-avatar" aria-hidden="true">
                <img v-if="companyAvatarLogoUrl" :src="companyAvatarLogoUrl" class="identity-avatar-img" alt="">
                <template v-else>{{ companyAvatarInitial }}</template>
              </div>
              <div class="dash-identity-body">
                <div class="dash-identity-title-row">
                  <span class="dash-identity-name">{{ companyDisplayName }}</span>
                  <button type="button" class="identity-edit-subject dash-identity-edit" @click="openSubjectModal">
                    {{ $t('dashboard.consoleEditSubject') }}
                  </button>
                </div>
                <div class="identity-meta-row dash-identity-meta">
                  <span class="identity-id-label">{{ $t('dashboard.consoleLoginId') }}</span>
                  <code class="identity-id-value">{{ consoleLoginIdDisplay }}</code>
                  <button type="button" class="icon-text-btn" :disabled="!consoleLoginIdRaw" @click="copyConsoleIdentity" :title="$t('dashboard.consoleCopyId')">
                    {{ $t('dashboard.consoleCopyId') }}
                  </button>
                  <span v-if="copyIdentityMsg" class="copy-toast">{{ copyIdentityMsg }}</span>
                </div>
                <div class="identity-tags dash-identity-tags">
                  <span class="identity-tag">{{ $t('dashboard.consoleTagWorkspace') }}</span>
                  <span v-if="enterpriseVerified" class="identity-tag identity-tag-success">{{ $t('dashboard.consoleEnterpriseVerified') }}</span>
                  <span v-else class="identity-tag identity-tag-warn">{{ $t('dashboard.consoleEnterprisePending') }}</span>
                  <span class="identity-tag identity-tag-muted">{{ $t('dashboard.consoleTagProduct') }}</span>
                </div>
              </div>
            </div>
          </div>

          <div class="dash-kpi-grid" role="list">
            <div class="dash-kpi" role="listitem">
              <span class="dash-kpi-value">{{ kpiRepoCount }}</span>
              <span class="dash-kpi-label">{{ $t('dashboard.repoCount') }}</span>
              <span class="dash-kpi-hint">{{ $t('dashboard.kpiHintCode') }}</span>
            </div>
            <div class="dash-kpi" role="listitem">
              <span class="dash-kpi-value">{{ kpiCommitCount }}</span>
              <span class="dash-kpi-label">{{ $t('dashboard.totalCommits') }}</span>
              <span class="dash-kpi-hint">{{ $t('dashboard.kpiHintWebhook') }}</span>
            </div>
            <div class="dash-kpi" role="listitem">
              <span class="dash-kpi-value">{{ kpiAuthorCount }}</span>
              <span class="dash-kpi-label">{{ $t('dashboard.authorCount') }}</span>
              <span class="dash-kpi-hint">{{ $t('dashboard.kpiHintPeople') }}</span>
            </div>
            <div class="dash-kpi" role="listitem">
              <span class="dash-kpi-value">{{ quoteHub.loading ? '—' : quoteHub.total }}</span>
              <span class="dash-kpi-label">{{ $t('dashboard.hubQuoteTotal') }}</span>
              <router-link to="/quote" class="dash-kpi-link">{{ $t('dashboard.hubQuoteViewAll') }} →</router-link>
            </div>
            <div class="dash-kpi" role="listitem">
              <span class="dash-kpi-value">{{ prototypeHub.loading ? '—' : prototypeHub.total }}</span>
              <span class="dash-kpi-label">{{ $t('dashboard.hubPrototypeTotal') }}</span>
              <router-link to="/prototype" class="dash-kpi-link">{{ $t('dashboard.hubPrototypeViewAll') }} →</router-link>
            </div>
            <div class="dash-kpi dash-kpi--accent" role="listitem">
              <span class="dash-kpi-value">{{ nexusHub.loading ? '—' : nexusHub.accountCount }}</span>
              <span class="dash-kpi-label">{{ $t('dashboard.kpiNexusAccounts') }}</span>
              <router-link to="/nexus" class="dash-kpi-link">{{ $t('dashboard.kpiNexusLink') }} →</router-link>
            </div>
          </div>
        </section>

        <!-- 仅编辑「主体信息」：复用报价配置里的 party-b-profile 接口 -->
        <div v-if="showSubjectModal" class="subject-modal-mask" @click.self="closeSubjectModal">
          <div class="subject-modal-card">
            <div class="subject-modal-head">
              <h2 class="subject-modal-title">{{ $t('dashboard.consoleEditSubject') }}</h2>
              <button type="button" class="subject-modal-close" @click="closeSubjectModal">×</button>
            </div>

            <div class="subject-logo-row">
              <div v-if="subjectEditLegal.enterpriseLogo" class="subject-logo-preview">
                <img :src="enterpriseLogoPreviewUrl" alt="enterprise logo">
              </div>
              <button type="button" class="secondary-button" :disabled="enterpriseLogoUploading" @click="pickEnterpriseLogo">
                {{ enterpriseLogoUploading ? '上传中…' : '添加企业logo' }}
              </button>
              <input ref="enterpriseLogoFileInput" type="file"
                     accept="image/png,image/jpeg,image/gif,image/webp"
                     class="subject-hidden-input"
                     @change="onEnterpriseLogoFileChange">
            </div>

            <section class="subject-edit-section">
              <h3 class="subject-section-title">{{ $t('quote.partyBLegalSubtitle') }}</h3>
              <div class="subject-grid">
                <label class="subject-label">
                  {{ $t('quote.partyBLegalName') }}
                  <input v-model="subjectEditLegal.legalName" type="text" class="subject-input">
                </label>
                <label class="subject-label">
                  {{ $t('quote.partyBCreditCode') }}
                  <input v-model="subjectEditLegal.creditCode" type="text" class="subject-input">
                </label>
                <label class="subject-label subject-full">
                  {{ $t('quote.partyBAddress') }}
                  <input v-model="subjectEditLegal.address" type="text" class="subject-input">
                </label>
                <label class="subject-label">
                  {{ $t('quote.partyBContactName') }}
                  <input v-model="subjectEditLegal.contactName" type="text" class="subject-input">
                </label>
                <label class="subject-label">
                  {{ $t('quote.partyBContactPhone') }}
                  <input v-model="subjectEditLegal.contactPhone" type="text" class="subject-input">
                </label>
                <label class="subject-label">
                  {{ $t('quote.partyBBankName') }}
                  <input v-model="subjectEditLegal.bankName" type="text" class="subject-input">
                </label>
                <label class="subject-label subject-full">
                  {{ $t('quote.partyBBankAccount') }}
                  <input v-model="subjectEditLegal.bankAccount" type="text" class="subject-input">
                </label>
              </div>

              <div class="subject-section-actions">
                <button type="button" class="primary-button" :disabled="subjectLegalSaving" @click="saveSubjectLegal">
                  {{ subjectLegalSaving ? '…' : $t('quote.partyBSave') }}
                </button>
                <span v-if="subjectLegalMsg" :class="subjectLegalOk ? 'subject-msg-ok' : 'subject-msg-err'">
                  {{ subjectLegalMsg }}
                </span>
              </div>
            </section>

            <section class="subject-edit-section">
              <h3 class="subject-section-title">{{ $t('quote.partyBNaturalSectionTitle') }}</h3>
              <p class="subject-section-hint">{{ $t('quote.partyBNaturalSectionHint') }}</p>
              <div class="subject-grid">
                <label class="subject-label subject-full">
                  {{ $t('quote.partyBNaturalFullName') }}
                  <input v-model="subjectEditNatural.fullName" type="text" class="subject-input">
                </label>
                <label class="subject-label subject-full">
                  {{ $t('quote.partyBNaturalIdNumber') }}
                  <input v-model="subjectEditNatural.idNumber" type="text" class="subject-input">
                </label>
                <label class="subject-label subject-full">
                  {{ $t('quote.partyBNaturalAddress') }}
                  <input v-model="subjectEditNatural.address" type="text" class="subject-input">
                </label>
                <label class="subject-label">
                  {{ $t('quote.partyBNaturalContactPhone') }}
                  <input v-model="subjectEditNatural.contactPhone" type="text" class="subject-input">
                </label>
                <label class="subject-label">
                  {{ $t('quote.partyBNaturalEmail') }}
                  <input v-model="subjectEditNatural.email" type="email" class="subject-input" autocomplete="off">
                </label>
                <label class="subject-label">
                  {{ $t('quote.partyBBankName') }}
                  <input v-model="subjectEditNatural.bankName" type="text" class="subject-input">
                </label>
                <label class="subject-label subject-full">
                  {{ $t('quote.partyBBankAccount') }}
                  <input v-model="subjectEditNatural.bankAccount" type="text" class="subject-input">
                </label>
              </div>

              <div class="subject-section-actions">
                <button type="button" class="primary-button" :disabled="subjectNaturalSaving" @click="saveSubjectNatural">
                  {{ subjectNaturalSaving ? '…' : $t('quote.partyBNaturalSave') }}
                </button>
                <span v-if="subjectNaturalMsg" :class="subjectNaturalOk ? 'subject-msg-ok' : 'subject-msg-err'">
                  {{ subjectNaturalMsg }}
                </span>
              </div>
            </section>

          </div>
        </div>

        <!-- 查看 webhook 链接弹窗（仅用于首页“协作多维表项目”卡片） -->
        <div v-if="showWebhookModal" class="webhook-modal-mask" @click.self="closeWebhookModal">
          <div class="webhook-modal-card">
            <div class="webhook-modal-head">
              <h2 class="webhook-modal-title">{{ $t('dashboard.webhookModalTitle') }}</h2>
              <button type="button" class="webhook-modal-close" @click="closeWebhookModal">×</button>
            </div>

            <p class="webhook-modal-desc">{{ $t('dashboard.webhookModalHint') }}</p>

            <div v-if="webhookModalLoading" class="placeholder">{{ $t('collab.loading') }}</div>
            <template v-else>
              <div class="webhook-url-row">
                <code class="mono webhook-url-code">{{ webhookUrl || '—' }}</code>
                <button type="button" class="link-button" :disabled="!webhookUrl" @click="copyWebhookUrl">
                  {{ $t('dashboard.consoleCopyId') }}
                </button>
              </div>
              <span v-if="webhookCopiedMsg" class="webhook-copy-toast">{{ webhookCopiedMsg }}</span>
            </template>
          </div>
        </div>

        <!-- 团队成员展示选择弹窗 -->
        <div v-if="showTeamPickerModal" class="team-picker-modal-mask" @click.self="closeTeamPickerModal">
          <div class="team-picker-modal-card">
            <div class="team-picker-modal-head">
              <h2 class="team-picker-modal-title">{{ $t('dashboard.teamPickerTitle') }}</h2>
              <button type="button" class="webhook-modal-close" @click="closeTeamPickerModal">×</button>
            </div>

            <p class="webhook-modal-desc">{{ $t('dashboard.teamPickerHint') }}</p>

            <div v-if="teamPickerLoading" class="placeholder">{{ $t('collab.loading') }}</div>
            <template v-else>
              <div class="team-picker-toolbar">
                <label class="team-picker-select-all">
                  <input type="checkbox" :checked="pickerSelectAll" @change="onPickerSelectAllChange">
                  {{ $t('dashboard.teamPickerSelectAll') }}
                </label>
              </div>

              <div class="team-picker-list">
                <div v-for="m in teamPickerPageMembers" :key="m.id" class="team-picker-row">
                  <label class="team-picker-check">
                    <input type="checkbox" :value="m.id" v-model="teamPickerSelectedIds">
                    <span class="activity-dot"
                          :class="m.activeToday ? 'dot-active' : 'dot-inactive'"></span>
                    <img v-if="m.avatar" :src="avatarDisplayUrl(m.avatar)" class="member-avatar member-avatar-xs" alt="">
                    <span v-else class="member-avatar member-avatar-placeholder member-avatar-xs">{{ memberInitial(m) }}</span>
                    <span class="team-picker-name">
                      {{ teamRoleLabel(m.role) }} {{ m.name || m.email || '—' }}
                    </span>
                  </label>
                </div>
              </div>

              <div v-if="teamPickerTotalPages > 1" class="team-hub-pagination">
                <button type="button" class="link-button page-btn" :disabled="teamPickerPage <= 1" @click="teamPickerPrev">
                  {{ $t('dashboard.pagePrev') || '上一页' }}
                </button>
                <span class="page-info">{{ teamPickerPage }} / {{ teamPickerTotalPages }}</span>
                <button type="button" class="link-button page-btn" :disabled="teamPickerPage >= teamPickerTotalPages" @click="teamPickerNext">
                  {{ $t('dashboard.pageNext') || '下一页' }}
                </button>
              </div>

              <div class="team-picker-actions">
                <button type="button" class="primary-button" :disabled="teamPickerSaving" @click="saveTeamPicker">
                  {{ teamPickerSaving ? '…' : $t('teamManage.save') }}
                </button>
                <button type="button" class="secondary-button" @click="closeTeamPickerModal">
                  {{ $t('teamManage.cancel') }}
                </button>
              </div>
            </template>
          </div>
        </div>

        <div v-if="!collabDataBoardOnly" class="hub-modules-head">
          <h2 class="hub-modules-title">{{ $t('dashboard.workbenchModulesTitle') }}</h2>
          <p class="hub-modules-lead">{{ $t('dashboard.workbenchModulesLead') }}</p>
        </div>
        <div v-if="!collabDataBoardOnly" class="hub-grid dash-module-grid">
          <section class="hub-card console-elevated hub-quote">
            <div class="hub-card-head">
              <span class="hub-icon hub-icon-quote" aria-hidden="true"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" width="18" height="18"><path d="M14 2H6a2 2 0 00-2 2v16a2 2 0 002 2h12a2 2 0 002-2V8z"/><polyline points="14 2 14 8 20 8"/><line x1="16" y1="13" x2="8" y2="13"/><line x1="16" y1="17" x2="8" y2="17"/></svg></span>
              <h2 class="hub-card-title">{{ $t('dashboard.hubQuoteTitle') }}</h2>
              <router-link to="/quote" class="hub-card-more">{{ $t('dashboard.hubQuoteViewAll') }} →</router-link>
            </div>
            <div class="hub-metric-row">
              <div class="hub-metric">
                <span class="hub-metric-value">{{ quoteHub.loading ? '—' : quoteHub.total }}</span>
                <span class="hub-metric-label">{{ $t('dashboard.hubQuoteTotal') }}</span>
              </div>
              <div class="hub-quick-actions">
                <router-link to="/quote/new" class="hub-pill hub-pill-primary">{{ $t('dashboard.hubQuoteNew') }}</router-link>
                <router-link to="/quote/config" class="hub-pill">{{ $t('dashboard.hubQuoteConfig') }}</router-link>
              </div>
            </div>
            <div class="hub-subhead">
              <span>{{ $t('dashboard.hubQuoteRecent') }}</span>
            </div>
            <div v-if="quoteHub.loading" class="hub-placeholder">{{ $t('collab.loading') }}</div>
            <ul v-else-if="quoteHub.items.length" class="hub-list">
              <li v-for="row in quoteHub.items" :key="row.id">
                <router-link :to="'/quote/' + row.id" class="hub-list-link">
                  <span class="hub-list-name">{{ row.name || ('#' + row.id) }}</span>
                  <span class="hub-list-meta">#{{ row.id }}<template v-if="row.techStack"> · {{ row.techStack }}</template></span>
                </router-link>
              </li>
            </ul>
            <p v-else class="hub-placeholder muted">{{ $t('dashboard.hubQuoteEmpty') }}</p>
          </section>

          <section class="hub-card console-elevated hub-api">
            <div class="hub-card-head">
              <span class="hub-icon hub-icon-api" aria-hidden="true"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" width="18" height="18"><polygon points="13 2 3 14 12 14 11 22 21 10 12 10 13 2"/></svg></span>
              <h2 class="hub-card-title">{{ $t('dashboard.hubApiTitle') }}</h2>
              <router-link to="/api-config" class="hub-card-more">{{ $t('dashboard.hubApiOpenConfig') }} →</router-link>
            </div>
            <div v-if="aiHub.loading" class="hub-placeholder">{{ $t('dashboard.hubApiLoading') }}</div>
            <template v-else>
              <div class="hub-provider-rows">
                <div class="hub-provider-row">
                  <span class="hub-provider-name">{{ $t('dashboard.hubApiDeepSeek') }}</span>
                  <span class="hub-provider-status" :class="{ ok: aiHubDeepseekOk }">{{ aiHubDeepseekLine }}</span>
                </div>
                <div class="hub-provider-row">
                  <span class="hub-provider-name">{{ $t('dashboard.hubApiQwen') }}</span>
                  <span class="hub-provider-status" :class="{ ok: aiHubQwenOk }">{{ aiHubQwenLine }}</span>
                </div>
              </div>
              <div class="hub-quick-actions hub-quick-actions-foot">
                <router-link to="/test" class="hub-pill hub-pill-primary">{{ $t('dashboard.hubApiOpenTest') }}</router-link>
                <router-link to="/api-config" class="hub-pill">{{ $t('aiConfig.navTitle') }}</router-link>
              </div>
            </template>
          </section>

          <section class="hub-card console-elevated hub-team">
            <div class="hub-card-head">
              <span class="hub-icon hub-icon-team" aria-hidden="true"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" width="18" height="18"><path d="M17 21v-2a4 4 0 00-4-4H5a4 4 0 00-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M23 21v-2a4 4 0 00-3-3.87"/><path d="M16 3.13a4 4 0 010 7.75"/></svg></span>
              <h2 class="hub-card-title">{{ $t('dashboard.hubTeamTitle') }}</h2>
              <router-link to="/team" class="hub-card-more">{{ $t('dashboard.hubTeamOpen') }} →</router-link>
            </div>
            <div class="hub-metric-row">
              <div class="hub-metric">
                <span class="hub-metric-value">{{ teamHubDisplay }}</span>
                <span class="hub-metric-label">{{ $t('dashboard.hubTeamMembers') }}</span>
              </div>
            </div>
            <p class="hub-desc">{{ $t('dashboard.hubTeamDesc') }}</p>
            <div class="team-hub-toolbar">
              <button type="button" class="link-button" @click="openTeamPickerModal">
                展示设置
              </button>
              <div class="team-activity-legend">
                <span class="legend-item"><span class="activity-dot dot-active"></span>{{ $t('dashboard.todayActive') }}</span>
                <span class="legend-item"><span class="activity-dot dot-inactive"></span>{{ $t('dashboard.todayInactive') }}</span>
              </div>
            </div>

            <div v-if="teamMembersLoading" class="placeholder">{{ $t('collab.loading') }}</div>
            <div v-else class="team-hub-members">
              <div v-if="!displayedTeamMembers.length" class="placeholder">{{ $t('dashboard.teamEmpty') }}</div>
              <div v-else class="team-hub-member-list">
                <div v-for="m in displayedTeamMembers" :key="m.id" class="team-hub-member">
                  <span class="activity-dot"
                        :class="m.activeToday ? 'dot-active' : 'dot-inactive'"></span>
                  <div class="member-avatar-wrap">
                    <img
                      v-if="memberAvatarUrl(m)"
                      :src="memberAvatarUrl(m)"
                      class="member-avatar"
                      alt=""
                    >
                    <span v-else class="member-avatar member-avatar-placeholder">{{ memberInitial(m) }}</span>
                  </div>
                  <div class="member-meta">
                    <div class="member-name">
                      {{ (m.name || m.email || '—') }}
                    </div>
                    <div class="member-subline">
                      <span class="member-role-tag" :class="teamRoleTagClass(m.role)">{{ teamRoleLabel(m.role) }}</span>
                      <span v-if="m.jobTitle" class="member-job-title-inline">{{ m.jobTitle }}</span>
                    </div>
                  </div>
                </div>
              </div>
              <div v-if="teamMembersTotalPages > 1" class="team-hub-pagination">
                <button type="button" class="link-button page-btn" :disabled="teamMembersDisplayPage <= 1" @click="teamMembersDisplayPrev">
                  {{ $t('dashboard.pagePrev') || '上一页' }}
                </button>
                <span class="page-info">{{ teamMembersDisplayPage }} / {{ teamMembersTotalPages }}</span>
                <button type="button" class="link-button page-btn" :disabled="teamMembersDisplayPage >= teamMembersTotalPages" @click="teamMembersDisplayNext">
                  {{ $t('dashboard.pageNext') || '下一页' }}
                </button>
              </div>
            </div>

          </section>

          <section class="hub-card console-elevated hub-project">
            <div class="hub-card-head">
              <span class="hub-icon hub-icon-project" aria-hidden="true"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" width="18" height="18"><path d="M22 19a2 2 0 01-2 2H4a2 2 0 01-2-2V5a2 2 0 012-2h5l2 3h9a2 2 0 012 2z"/></svg></span>
              <h2 class="hub-card-title">{{ $t('dashboard.hubProjectTitle') }}</h2>
              <router-link to="/collab/projects" class="hub-card-more">{{ $t('dashboard.hubProjectCollab') }} →</router-link>
            </div>
            <p class="hub-desc">{{ $t('dashboard.hubProjectHint') }}</p>

            <div class="repo-filter hub-repo-filter">
              <button
                type="button"
                class="link-button"
                :disabled="webhookModalLoading"
                @click="openWebhookModal"
              >
                {{ webhookModalLoading ? '…' : $t('dashboard.viewWebhookLink') }}
              </button>
              <label v-if="!fixedRepoFullName">{{ $t('dashboard.project') }}</label>
              <select v-if="!fixedRepoFullName" v-model="selectedRepo" @change="onRepoChange">
                <option value="">{{ $t('dashboard.allProjects') }}</option>
                <option v-for="repo in repos" :key="repo" :value="repo">{{ repo }}</option>
              </select>
            </div>
            <div v-if="!selectedRepo && statsOverview" class="hub-mini-overview">
              <span class="hub-subhead-text">{{ $t('dashboard.hubProjectMiniOverview') }}</span>
              <div class="hub-mini-metrics">
                <div class="hub-mini-metric">
                  <span class="hub-mini-val">{{ statsOverview.repoCount ?? 0 }}</span>
                  <span class="hub-mini-lbl">{{ $t('dashboard.repoCount') }}</span>
                </div>
                <div class="hub-mini-metric">
                  <span class="hub-mini-val">{{ statsOverview.totalCommits ?? 0 }}</span>
                  <span class="hub-mini-lbl">{{ $t('dashboard.totalCommits') }}</span>
                </div>
                <div class="hub-mini-metric">
                  <span class="hub-mini-val">{{ statsOverview.authorCount ?? 0 }}</span>
                  <span class="hub-mini-lbl">{{ $t('dashboard.authorCount') }}</span>
                </div>
              </div>
            </div>
            <div v-else-if="!selectedRepo && !statsOverview" class="hub-placeholder">{{ $t('collab.loading') }}</div>
          </section>

          <section class="hub-card console-elevated hub-ops hub-card-placeholder">
            <div class="hub-card-head">
              <span class="hub-icon hub-icon-ops" aria-hidden="true"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" width="18" height="18"><circle cx="12" cy="12" r="3"/><path d="M19.4 15a1.65 1.65 0 00.33 1.82l.06.06a2 2 0 010 2.83 2 2 0 01-2.83 0l-.06-.06a1.65 1.65 0 00-1.82-.33 1.65 1.65 0 00-1 1.51V21a2 2 0 01-4 0v-.09A1.65 1.65 0 009 19.4a1.65 1.65 0 00-1.82.33l-.06.06a2 2 0 01-2.83-2.83l.06-.06A1.65 1.65 0 004.68 15a1.65 1.65 0 00-1.51-1H3a2 2 0 010-4h.09A1.65 1.65 0 004.6 9a1.65 1.65 0 00-.33-1.82l-.06-.06a2 2 0 012.83-2.83l.06.06A1.65 1.65 0 009 4.68a1.65 1.65 0 001-1.51V3a2 2 0 014 0v.09a1.65 1.65 0 001 1.51 1.65 1.65 0 001.82-.33l.06-.06a2 2 0 012.83 2.83l-.06.06a1.65 1.65 0 00-.33 1.82V9c.26.604.852.997 1.51 1H21a2 2 0 010 4h-.09c-.658.003-1.25.396-1.51 1z"/></svg></span>
              <h2 class="hub-card-title">{{ $t('dashboard.hubOpsTitle') }}</h2>
            </div>
            <p class="hub-desc">{{ $t('dashboard.hubOpsPlaceholder') }}</p>
            <router-link to="/nexus" class="hub-card-more">进入 →</router-link>
          </section>

          <section class="hub-card console-elevated hub-cloud-dev">
            <div class="hub-card-head">
              <span class="hub-icon hub-icon-clouddev" aria-hidden="true"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" width="18" height="18"><path d="M18 10h-1.26A8 8 0 1 0 9 20h9a5 5 0 0 0 0-10z"/></svg></span>
              <h2 class="hub-card-title">{{ $t('dashboard.hubCloudDevTitle') }}</h2>
            </div>
            <p class="hub-desc">{{ $t('dashboard.hubCloudDevDesc') }}</p>
            <router-link to="/cloud-dev" class="hub-card-more">{{ $t('dashboard.hubCloudDevLink') }}</router-link>
          </section>

          <section class="hub-card console-elevated hub-market hub-card-placeholder">
            <div class="hub-card-head">
              <span class="hub-icon hub-icon-market" aria-hidden="true"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" width="18" height="18"><circle cx="12" cy="12" r="10"/><polygon points="10 8 16 12 10 16 10 8"/></svg></span>
              <h2 class="hub-card-title">增效实验室</h2>
            </div>
            <p class="hub-desc">探索提升效率的前沿实验项目，帮助团队在真实场景中快速试用与验证。</p>
            <div class="lab-mini-grid">
              <div
                v-for="t in labTabs"
                :key="t.key"
                class="lab-mini-card"
              >
                <div class="lab-mini-title">
                  {{ t.label }}
                </div>
                <div class="lab-mini-tag">
                  尽情期待
                </div>
              </div>
            </div>
          </section>

          <section class="hub-card console-elevated hub-prototype">
            <div class="hub-card-head">
              <span class="hub-icon hub-icon-prototype" aria-hidden="true"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" width="18" height="18"><rect x="3" y="3" width="18" height="18" rx="2" ry="2"/><circle cx="8.5" cy="8.5" r="1.5"/><polyline points="21 15 16 10 5 21"/></svg></span>
              <h2 class="hub-card-title">{{ $t('dashboard.hubPrototypeTitle') }}</h2>
              <router-link to="/prototype" class="hub-card-more">{{ $t('dashboard.hubPrototypeViewAll') }} →</router-link>
            </div>
            <div class="hub-metric-row">
              <div class="hub-metric">
                <span class="hub-metric-value">{{ prototypeHub.loading ? '—' : prototypeHub.total }}</span>
                <span class="hub-metric-label">{{ $t('dashboard.hubPrototypeTotal') }}</span>
              </div>
              <div class="hub-quick-actions">
                <router-link to="/prototype" class="hub-pill hub-pill-primary">{{ $t('dashboard.hubPrototypeOpenList') }}</router-link>
                <router-link to="/prototype?create=1" class="hub-pill">{{ $t('dashboard.hubPrototypeNew') }}</router-link>
              </div>
            </div>
            <p class="hub-desc">{{ $t('dashboard.hubPrototypeDesc') }}</p>
            <div class="hub-subhead hub-subhead-filter">
              <span>{{ $t('dashboard.hubPrototypeRecent') }}</span>
              <input
                v-model="prototypeHubFilter"
                type="search"
                class="hub-list-filter"
                :placeholder="$t('dashboard.hubPrototypeFilterPlaceholder')"
                autocomplete="off"
              >
            </div>
            <div v-if="prototypeHub.loading" class="hub-placeholder">{{ $t('collab.loading') }}</div>
            <ul v-else-if="prototypeHubListFiltered.length" class="hub-list">
              <li v-for="row in prototypeHubListFiltered" :key="row.id">
                <router-link :to="'/prototype/' + row.id" class="hub-list-link">
                  <span class="hub-list-name">{{ row.name || ('#' + row.id) }}</span>
                  <span class="hub-list-meta">#{{ row.id }}<template v-if="row.currentSpecVersion != null"> · v{{ row.currentSpecVersion }}</template></span>
                </router-link>
              </li>
            </ul>
            <p v-else-if="prototypeHub.items.length && prototypeHubFilter.trim()" class="hub-placeholder muted">{{ $t('dashboard.hubPrototypeNoMatch') }}</p>
            <p v-else class="hub-placeholder muted">{{ $t('dashboard.hubPrototypeEmpty') }}</p>
          </section>

          <section class="hub-card console-elevated hub-chance hub-card-placeholder">
            <div class="hub-card-head">
              <span class="hub-icon hub-icon-chance" aria-hidden="true">◇</span>
              <h2 class="hub-card-title">机会角</h2>
            </div>
            <p class="hub-desc"></p>
            <span class="hub-soon">尽情期待</span>
          </section>
        </div>

        <template v-if="!suppressBoardHeader">
          <header class="dash-board-header">
            <p class="dash-board-eyebrow">{{ $t('dashboard.consoleDataBoardHint') }}</p>
            <h2 class="dash-board-title">{{ $t('dashboard.consoleDataBoardTitle') }}</h2>
          </header>
        </template>

        <!-- 研发遥测：选中仓库时展示项目档案；全局指标已在指挥中心 KPI 呈现 -->
    <section class="section overview-section dash-glass-panel">
      <h2 class="section-title dash-section-title">{{ selectedRepo ? $t('dashboard.projectInfoTitle') : $t('dashboard.telemetrySectionTitle') }}</h2>
      <div v-if="selectedRepo" class="project-info-cards" :class="{ 'project-info-cards-compact': embeddedCompact }">
        <div v-if="repoInfoLoading" class="placeholder">{{ $t('collab.loading') }}</div>
        <template v-else>
          <div class="project-info-row" :class="{ 'project-info-top': embeddedCompact }">
            <span class="project-info-label">{{ $t('dashboard.projectName') }}：</span>
            <a v-if="repoInfo.htmlUrl" :href="repoInfo.htmlUrl" target="_blank" rel="noopener noreferrer" class="project-info-name">{{ repoInfo.name || repoInfo.fullName || selectedRepo }}</a>
            <span v-else class="project-info-name">{{ repoInfo.name || repoInfo.fullName || selectedRepo }}</span>
          </div>
          <div v-if="repoInfo.description" class="project-info-row" :class="{ 'project-info-desc': embeddedCompact }">
            <span class="project-info-label">{{ $t('dashboard.about') }}：</span>
            <span class="project-info-desc">{{ repoInfo.description }}</span>
          </div>
          <div class="project-info-row" :class="{ 'project-info-dev': embeddedCompact }">
            <span class="project-info-label">{{ $t('dashboard.projectDevelopers') }}：</span>
            <span class="project-info-developers">{{ developerListText }}</span>
          </div>
          <div v-if="languageList.length" class="project-info-row" :class="{ 'project-info-lang': embeddedCompact }">
            <span class="project-info-label">{{ $t('dashboard.techStack') }}：</span>
            <span class="project-info-languages">{{ languageList.join('、') }}</span>
          </div>
        </template>
      </div>
      <template v-else>
        <p v-if="statsOverview" class="dash-telemetry-hint">{{ $t('dashboard.telemetryAggregateHint') }}</p>
        <div v-else class="placeholder">{{ $t('collab.loading') }}</div>
      </template>
    </section>

    <!-- 图表区：提交趋势 + 开发者排名（合并为单卡片，Tab 切换） -->
    <div class="charts-row">
      <section class="section chart-section chart-section-merged dash-glass-panel">
        <div class="chart-merge-head">
          <div class="chart-tab-strip" role="tablist">
            <button
              type="button"
              role="tab"
              :aria-selected="chartTab === 'trend'"
              :class="{ active: chartTab === 'trend' }"
              @click="setChartTab('trend')"
            >{{ $t('dashboard.commitTrend') }}</button>
            <button
              type="button"
              role="tab"
              :aria-selected="chartTab === 'author'"
              :class="{ active: chartTab === 'author' }"
              @click="setChartTab('author')"
            >{{ $t('dashboard.authorRanking') }}</button>
          </div>
        </div>
        <div v-if="chartTab === 'trend'" class="chart-panel chart-panel-trend">
          <div class="chart-wrap">
            <canvas ref="trendChart"></canvas>
          </div>
          <div class="chart-legend">
            <button :class="{ active: trendRange === '7d' }" @click="trendRange = '7d'; loadStatsCommitsByDay()">{{ $t('dashboard.commitTrendRange') }}</button>
            <button :class="{ active: trendRange === '30d' }" @click="trendRange = '30d'; loadStatsCommitsByDay()">{{ $t('dashboard.commitTrendRange30') }}</button>
          </div>
        </div>
        <div v-if="chartTab === 'author'" class="chart-panel chart-panel-author">
          <div class="author-rank-toolbar">
            <div class="chart-legend author-rank-modes">
              <button type="button" :class="{ active: authorRankPeriod === 'week' }" @click="setAuthorRankPeriod('week')">{{ $t('dashboard.authorRankWeek') }}</button>
              <button type="button" :class="{ active: authorRankPeriod === 'month' }" @click="setAuthorRankPeriod('month')">{{ $t('dashboard.authorRankMonth') }}</button>
              <button type="button" :class="{ active: authorRankPeriod === 'year' }" @click="setAuthorRankPeriod('year')">{{ $t('dashboard.authorRankYear') }}</button>
              <button type="button" :class="{ active: authorRankPeriod === 'total' }" @click="setAuthorRankPeriod('total')">{{ $t('dashboard.authorRankTotal') }}</button>
            </div>
            <div class="author-rank-nav" v-if="authorRankPeriod !== 'total'">
              <button type="button" class="link-button" :disabled="authorRankLoading" @click="authorRankPrev">{{ $t('dashboard.authorRankPrevPeriod') }}</button>
              <span class="author-rank-range">{{ authorRankRangeText }}</span>
              <button type="button" class="link-button" :disabled="authorRankLoading || !authorRankMeta.canGoNext" @click="authorRankNext">{{ $t('dashboard.authorRankNextPeriod') }}</button>
            </div>
            <div class="author-rank-nav" v-else>
              <span class="author-rank-range muted">{{ authorRankRangeText }}</span>
            </div>
          </div>
          <div class="chart-wrap chart-wrap-bar">
            <canvas ref="authorChart"></canvas>
          </div>
        </div>
      </section>
    </div>

    <!-- 项目每日进展总结（按仓库） -->
    <section class="section dash-glass-panel" v-if="selectedRepo">
      <div class="section-header">
        <h2 class="section-title dash-section-title">{{ $t('dashboard.dailySummaryTitle') }}</h2>
        <div class="daily-summary-toolbar">
          <button type="button" class="link-button primary-action" @click="runDailySummaryForRepo" :disabled="dailySummaryRunLoading || dailySummaryLoading">
            {{ dailySummaryRunLoading ? '…' : $t('dashboard.dailySummaryGenerateYesterday') }}
          </button>
          <button type="button" class="link-button" @click="loadDailySummaries" :disabled="dailySummaryLoading">{{ $t('dashboard.refresh') }}</button>
        </div>
      </div>
      <p class="daily-summary-desc">{{ $t('dashboard.dailySummaryDesc') }}</p>
      <p v-if="dailySummaryRunMessage" :class="dailySummaryRunOk ? 'daily-summary-feedback ok' : 'daily-summary-feedback err'">{{ dailySummaryRunMessage }}</p>
      <div class="daily-summary-history-head">
        <h3 class="daily-summary-subtitle">{{ $t('dashboard.dailySummaryHistoryTitle') }}</h3>
        <label class="page-size-label">
          {{ $t('dashboard.commitsPerPage') }}
          <select v-model.number="dailySummaryPageSize" class="page-size-select" @change="onDailySummaryPageSizeChange">
            <option :value="10">10</option>
            <option :value="20">20</option>
            <option :value="50">50</option>
          </select>
        </label>
      </div>
      <div v-if="dailySummaryLoading" class="placeholder">{{ $t('collab.loading') }}</div>
      <ul v-else-if="dailySummaries.length" class="daily-summary-list">
        <li v-for="s in dailySummaries" :key="s.id" class="daily-summary-item" @click="openDailySummaryDetail(s.id)">
          <span class="ds-date">{{ s.summaryDate }}</span>
          <span class="ds-title">{{ s.title || '—' }}</span>
          <span class="ds-meta">{{ $t('dashboard.dailySummaryCommits', { n: s.commitCount }) }} · {{ s.status }}</span>
        </li>
      </ul>
      <div v-else class="placeholder">{{ $t('dashboard.dailySummaryEmpty') }}</div>
      <div v-if="dailySummaryTotal > 0" class="commits-pagination daily-summary-pagination">
        <span class="page-info">{{ $t('dashboard.pageInfo', { total: dailySummaryTotal, page: dailySummaryPage, pages: dailySummaryTotalPages }) }}</span>
        <div class="page-buttons">
          <button type="button" class="link-button page-btn" :disabled="dailySummaryLoading || dailySummaryPage <= 1" @click="goDailySummaryPrev">{{ $t('dashboard.pagePrev') }}</button>
          <button type="button" class="link-button page-btn" :disabled="dailySummaryLoading || dailySummaryPage >= dailySummaryTotalPages" @click="goDailySummaryNext">{{ $t('dashboard.pageNext') }}</button>
        </div>
      </div>
    </section>

    <!-- 最近提交表格（分页） -->
    <section class="section commits-section dash-glass-panel">
      <div class="section-header">
        <h2 class="section-title dash-section-title">{{ $t('dashboard.recentCommits') }}</h2>
        <div class="commits-toolbar">
          <label class="page-size-label">
            {{ $t('dashboard.commitsPerPage') }}
            <select v-model.number="commitsPageSize" class="page-size-select" @change="onCommitsPageSizeChange">
              <option :value="10">10</option>
              <option :value="20">20</option>
              <option :value="50">50</option>
            </select>
          </label>
          <button type="button" class="link-button" @click="loadCommits" :disabled="commitsLoading">{{ $t('dashboard.refresh') }}</button>
        </div>
      </div>
      <div v-if="commitsLoading && !commits.length" class="placeholder">{{ $t('collab.loading') }}</div>
      <template v-else>
        <div class="table-wrapper commits-table-scroll" v-if="commits.length">
          <table class="table">
            <thead>
              <tr>
                <th>{{ $t('dashboard.repo') }}</th>
                <th>{{ $t('dashboard.commit') }}</th>
                <th>{{ $t('dashboard.author') }}</th>
                <th>{{ $t('dashboard.time') }}</th>
                <th class="num">{{ $t('dashboard.filesChanged') }}</th>
                <th class="num">{{ $t('dashboard.insertions') }}</th>
                <th class="num">{{ $t('dashboard.deletions') }}</th>
                <th>{{ $t('dashboard.message') }}</th>
                <th>{{ $t('dashboard.actions') }}</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="item in commits" :key="item.repoFullName + ':' + item.commitSha">
                <td>{{ item.repoFullName }}</td>
                <td class="mono">{{ shortSha(item.commitSha) }}</td>
                <td>{{ item.authorName || '-' }}</td>
                <td>{{ formatTime(item.committedAt) }}</td>
                <td class="num">{{ item.filesChanged != null ? item.filesChanged : '-' }}</td>
                <td class="num add">{{ item.insertions != null ? item.insertions : '-' }}</td>
                <td class="num del">{{ item.deletions != null ? item.deletions : '-' }}</td>
                <td>{{ item.message }}</td>
                <td>
                  <button class="link-button" @click="viewDiff(item)">{{ $t('dashboard.viewDiff') }}</button>
                  <router-link :to="commitAnalysisRoute(item)" class="link-button commit-analysis-link">{{ $t('dashboard.commitAnalysisBoard') }}</router-link>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
        <div v-else class="placeholder">{{ $t('dashboard.noCommits') }}</div>
        <div v-if="commitsTotal > 0" class="commits-pagination">
          <span class="page-info">{{ $t('dashboard.pageInfo', { total: commitsTotal, page: commitsPage, pages: commitsTotalPages }) }}</span>
          <div class="page-buttons">
            <button type="button" class="link-button page-btn" :disabled="commitsLoading || commitsPage <= 1" @click="goCommitsPrev">{{ $t('dashboard.pagePrev') }}</button>
            <button type="button" class="link-button page-btn" :disabled="commitsLoading || commitsPage >= commitsTotalPages" @click="goCommitsNext">{{ $t('dashboard.pageNext') }}</button>
          </div>
        </div>
      </template>
    </section>

    <section class="section dash-glass-panel" v-if="authors.length">
      <h2 class="section-title dash-section-title">{{ $t('dashboard.developers') }}</h2>
      <div class="table-wrapper">
        <table class="table">
          <thead>
          <tr>
            <th>{{ $t('dashboard.author') }}</th>
            <th>{{ $t('dashboard.email') }}</th>
            <th>{{ $t('dashboard.commits') }}</th>
            <th>{{ $t('dashboard.lastCommit') }}</th>
          </tr>
          </thead>
          <tbody>
          <tr v-for="a in authors" :key="a.authorEmail || a.authorName">
            <td>{{ a.authorName || '-' }}</td>
            <td>{{ a.authorEmail || '-' }}</td>
            <td>{{ a.commitCount }}</td>
            <td>{{ formatTime(a.lastCommittedAt) }}</td>
          </tr>
          </tbody>
        </table>
      </div>
    </section>

    <!-- Diff + AI 分析弹窗 -->
    <div v-if="dailySummaryDetail" class="diff-modal-backdrop" @click="dailySummaryDetail = null">
      <div class="diff-modal daily-summary-modal" @click.stop>
        <div class="diff-modal-header">
          <h2 class="diff-modal-title">{{ dailySummaryDetail.title || $t('dashboard.dailySummaryTitle') }}</h2>
          <button type="button" class="link-button diff-modal-close" @click="dailySummaryDetail = null">×</button>
        </div>
        <div class="diff-modal-body">
          <p class="ds-detail-meta">{{ dailySummaryDetail.summaryDate }} · {{ dailySummaryDetail.repoFullName }} · {{ dailySummaryDetail.status }}</p>
          <div v-if="dailyDetailLoading" class="placeholder">{{ $t('collab.loading') }}</div>
          <!-- eslint-disable-next-line vue/no-v-html -- 受信 AI 生成 Markdown，已关闭 HTML 标签解析 -->
          <div v-else class="daily-summary-body markdown-body" v-html="dailySummaryRenderedHtml"></div>
        </div>
      </div>
    </div>

    <div v-if="selectedCommit" class="diff-modal-backdrop" @click="selectedCommit = null">
      <div class="diff-modal" @click.stop>
        <div class="diff-modal-header">
          <h2 class="diff-modal-title">
            {{ $t('dashboard.diffTitle') }}：{{ selectedCommit.repoFullName }} @ {{ shortSha(selectedCommit.commitSha) }}
          </h2>
          <div class="diff-actions">
            <router-link v-if="selectedCommit" :to="commitAnalysisRoute(selectedCommit)" class="link-button">{{ $t('dashboard.commitAnalysisBoard') }}</router-link>
            <button v-if="diffText" class="link-button" @click="copyDiffForAi">{{ $t('dashboard.copyForAi') }}</button>
            <button type="button" class="link-button diff-modal-close" @click="selectedCommit = null" :aria-label="$t('dashboard.collapse')">{{ $t('dashboard.collapse') }}</button>
          </div>
        </div>
        <div class="diff-modal-body">
          <div class="diff-box" v-if="diffLoading">
            {{ $t('dashboard.loadingDiff') }}
          </div>
          <div v-else-if="diffText" class="diff-box-wrapper">
            <div class="diff-box diff-content" v-html="diffHtml"></div>
            <div v-if="isDiffPlaceholder(diffText)" class="diff-placeholder-actions">
              <button type="button" class="primary-button small" :disabled="diffLoading" @click="retryFetchDiff">{{ $t('dashboard.retryFetchDiff') }}</button>
              <a :href="githubCommitUrl" target="_blank" rel="noopener noreferrer" class="view-on-github-link">{{ $t('dashboard.viewOnGitHub') }}</a>
            </div>
          </div>
          <pre v-else class="diff-box"><code>{{ $t('dashboard.diffNotReady') }}</code></pre>

          <div class="ai-analysis-section">
            <h3 class="ai-analysis-title">{{ $t('dashboard.aiAnalysisTitle') }}</h3>
            <div v-if="aiAnalysisLoading" class="ai-analysis-loading">{{ $t('dashboard.loading') }}...</div>
            <div v-else-if="aiAnalysisResult" class="ai-analysis-result">
              <div class="ai-analysis-row">
                <span class="ai-analysis-label">{{ $t('dashboard.aiWorkSummary') }}</span>
                <span>{{ aiAnalysisResult.workSummary || '—' }}</span>
              </div>
              <div class="ai-analysis-row">
                <span class="ai-analysis-label">{{ $t('dashboard.aiWorkType') }}</span>
                <span>{{ aiAnalysisResult.workType || '—' }}</span>
              </div>
              <div class="ai-analysis-row">
                <span class="ai-analysis-label">{{ $t('dashboard.aiIsEffective') }}</span>
                <span>{{ aiAnalysisResult.isEffective || '—' }}</span>
              </div>
              <div class="ai-analysis-row" v-if="aiAnalysisResult.effectiveReason">
                <span class="ai-analysis-label">{{ $t('dashboard.aiEffectiveReason') }}</span>
                <span>{{ aiAnalysisResult.effectiveReason }}</span>
              </div>
              <div class="ai-analysis-row">
                <span class="ai-analysis-label">{{ $t('dashboard.aiQualityLevel') }}</span>
                <span>{{ aiAnalysisResult.qualityLevel || '—' }}</span>
              </div>
              <div class="ai-analysis-row" v-if="aiAnalysisResult.qualityComment">
                <span class="ai-analysis-label">{{ $t('dashboard.aiQualityComment') }}</span>
                <span>{{ aiAnalysisResult.qualityComment }}</span>
              </div>
            </div>
            <div v-else class="ai-analysis-actions">
              <button type="button" class="primary-button small" :disabled="aiAnalysisRunning" @click="runAiAnalysis">
                {{ aiAnalysisRunning ? $t('dashboard.aiRunning') : $t('dashboard.runAiAnalysis') }}
              </button>
              <p class="ai-analysis-hint">{{ $t('dashboard.aiAnalysisHint') }}</p>
            </div>
          </div>
        </div>
      </div>
    </div>
      </div>
    </div>
  </div>
</template>

<script>
import { Chart as ChartJS, registerables } from 'chart.js'
import MarkdownIt from 'markdown-it'
import { compressImageFile, IMAGE_COMPRESS_PRESETS } from '@/utils/imageCompress'

ChartJS.register(...registerables)

/** 每日进展总结：安全渲染（禁止 Markdown 内嵌原始 HTML，减轻 XSS 风险） */
const dailySummaryMarkdown = new MarkdownIt({
  html: false,
  breaks: true,
  linkify: true,
  typographer: true
})

export default {
  name: 'DashboardView',
  props: {
    /**
     * 当从其它页面嵌入“看板”时，固定当前仓库（隐藏仓库下拉框）。
     * 例如：项目协作页点击“开发与数据看板”时复用首页内容。
     */
    fixedRepoFullName: {
      type: String,
      default: ''
    },
    /**
     * 多维表「开发与数据看板」嵌入：仅展示下方与当前仓库相关的数据区，隐藏企业信息卡片与运营 hub 卡片（报价/API/团队等）。
     */
    collabDataBoardOnly: {
      type: Boolean,
      default: false
    },
    /** 嵌入协作「开发与数据看板」时收紧间距与版式 */
    embeddedCompact: {
      type: Boolean,
      default: false
    },
    /** 隐藏「数据看板」大标题与说明（由外层区块标题承接） */
    suppressBoardHeader: {
      type: Boolean,
      default: false
    }
  },
  data () {
    return {
      dashboard: null,
      commits: [],
      commitsLoading: false,
      commitsPage: 1,
      commitsPageSize: 10,
      commitsTotal: 0,
      selectedCommit: null,
      diffText: '',
      diffMeta: null,
      diffLoading: false,
      repos: [],
      selectedRepo: '',
      authors: [],
      aiAnalysisResult: null,
      aiAnalysisLoading: false,
      aiAnalysisRunning: false,
      statsOverview: null,
      commitsByDay: [],
      authorsStats: [],
      authorRankPeriod: 'week',
      authorRankOffset: 0,
      authorRankMeta: { canGoNext: false, canGoPrevious: true },
      authorRankLoading: false,
      trendRange: '7d',
      /** 合并图表卡片：trend=提交趋势，author=开发者排名 */
      chartTab: 'trend',
      chartInstances: { trend: null, author: null },
      repoInfo: {},
      repoInfoLoading: false,
      dailySummaries: [],
      dailySummaryLoading: false,
      dailySummaryPage: 1,
      dailySummaryPageSize: 10,
      dailySummaryTotal: 0,
      dailySummaryRunLoading: false,
      dailySummaryRunMessage: '',
      dailySummaryRunOk: false,
      dailySummaryDetail: null,
      dailyDetailLoading: false,
      quoteHub: { loading: false, items: [], total: 0 },
      nexusHub: { loading: false, accountCount: 0 },
      workspaceSummary: null,
      workspaceSummaryLoading: false,
      prototypeHub: { loading: false, items: [], total: 0 },
      prototypeHubFilter: '',
      teamHubCount: null,
      teamHubLoading: false,
      aiHub: { loading: false, deepseek: null, qwen: null },
      copyIdentityMsg: '',
      partyBProfile: null,
      partyBProfileLoading: false,
      // 仅主体信息弹窗：复用 /admin/quote/party-b-profile 的同一套数据源
      showSubjectModal: false,
      subjectEditLegal: {
        legalName: '',
        creditCode: '',
        address: '',
        contactName: '',
        contactPhone: '',
        bankName: '',
        bankAccount: '',
        enterpriseLogo: ''
      },
      subjectEditNatural: {
        fullName: '',
        idNumber: '',
        address: '',
        contactPhone: '',
        email: '',
        bankName: '',
        bankAccount: ''
      },
      enterpriseLogoUploading: false,
      subjectLegalSaving: false,
      subjectLegalMsg: '',
      subjectLegalOk: false,
      subjectNaturalSaving: false,
      subjectNaturalMsg: '',
      subjectNaturalOk: false,

      // 查看 webhook 链接弹窗：仅用于首页“协作多维表项目”卡片
      showWebhookModal: false,
      webhookModalLoading: false,
      webhookCopiedMsg: '',
      tenantSlug: '',
      webhookUrl: '',

      // 团队管理卡片：头像+姓名+活跃状态（今日活跃/不活跃）
      teamMembersLoading: false,
      teamMembers: [],
      teamSelectedMemberIds: null, // null 表示全选展示
      teamMembersDisplayPage: 1,
      teamMembersDisplayPageSize: 6,

      showTeamPickerModal: false,
      teamPickerLoading: false,
      teamPickerSaving: false,
      teamPickerPage: 1,
      teamPickerPageSize: 10,
      teamPickerSelectedIds: [],

      // 首页：增效实验室（小卡片列表）
      labTabs: [
        { key: 'finance', label: '智能财会' },
        { key: 'geo', label: 'GEO推流' },
        { key: 'dashboard', label: '客户看板' },
        { key: 'lobster', label: '龙虾智能体' }
      ],

      /** 当前版本默认视为已认证；后续可对接企业认证接口 */
      enterpriseVerified: true
    }
  },
  watch: {
    selectedCommit (v) {
      this.aiAnalysisResult = null
      if (v) {
        this.loadAiAnalysisResult()
        document.body.style.overflow = 'hidden'
        this._onEscape = (e) => { if (e.key === 'Escape') this.selectedCommit = null }
        document.addEventListener('keydown', this._onEscape)
      } else {
        document.body.style.overflow = ''
        if (this._onEscape) {
          document.removeEventListener('keydown', this._onEscape)
          this._onEscape = null
        }
      }
    },
    selectedRepo () {
      if (this.selectedRepo) this.loadRepoInfo()
      else this.repoInfo = {}
    },
    commitsByDay () {
      if (this.chartTab === 'trend') this.renderTrendChart()
    },
    authorsStats () {
      if (this.chartTab === 'author') this.renderAuthorChart()
    },
    '$route.query' () {
      this.$nextTick(() => this.tryOpenCommitFromQuery())
    }
  },
  computed: {
    developerListText () {
      const list = this.authors || []
      if (!list.length) return '—'
      return list.map(a => a.authorName || a.authorEmail || '—').join('、')
    },
    languageList () {
      const lang = this.repoInfo.languages
      if (!lang || typeof lang !== 'object') return []
      return Object.keys(lang)
    },
    githubCommitUrl () {
      if (!this.selectedCommit) return ''
      const repo = this.selectedCommit.repoFullName || ''
      const sha = this.selectedCommit.commitSha || ''
      if (!repo || !sha) return ''
      return `https://github.com/${repo}/commit/${sha}`
    },
    commitsTotalPages () {
      if (!this.commitsTotal || !this.commitsPageSize) return 1
      return Math.max(1, Math.ceil(this.commitsTotal / this.commitsPageSize))
    },
    dailySummaryTotalPages () {
      if (!this.dailySummaryTotal || !this.dailySummaryPageSize) return 1
      return Math.max(1, Math.ceil(this.dailySummaryTotal / this.dailySummaryPageSize))
    },
    dailySummaryRenderedHtml () {
      if (!this.dailySummaryDetail || this.dailyDetailLoading) return ''
      const raw = this.dailySummaryDetail.content
      if (raw == null || String(raw).trim() === '') {
        return '<p class="markdown-empty">—</p>'
      }
      try {
        return dailySummaryMarkdown.render(String(raw))
      } catch (e) {
        return '<pre class="markdown-fallback">' + this.escapeHtml(String(raw)) + '</pre>'
      }
    },
    authorRankRangeText () {
      if (this.authorRankPeriod === 'total') return this.$t('dashboard.authorRankAllTime')
      const m = this.authorRankMeta
      if (!m || !m.periodStart) return '—'
      return this.$t('dashboard.authorRankRange', {
        start: m.periodStart,
        end: m.periodEndInclusive || m.periodStart
      })
    },
    consoleLoginIdRaw () {
      if (typeof localStorage === 'undefined') return ''
      return (localStorage.getItem('autoattend_username') || '').trim()
    },
    companyDisplayName () {
      if (this.partyBProfileLoading) return '…'
      const p = this.partyBProfile || {}
      const legal = (p.legalName && String(p.legalName).trim()) || ''
      if (legal) return legal
      const np = p.naturalPerson || {}
      const nat = (np.fullName && String(np.fullName).trim()) || ''
      if (nat) return nat
      return this.$t('dashboard.consoleCompanyPlaceholder')
    },
    companyAvatarInitial () {
      const n = this.companyDisplayName
      if (!n || n === '…') return '?'
      return String(n).charAt(0).toUpperCase()
    },
    companyAvatarLogoUrl () {
      const p = this.partyBProfile || {}
      const key = p.enterpriseLogo
      if (!key) return ''
      const s = String(key).trim()
      if (!s) return ''
      if (s.startsWith('http://') || s.startsWith('https://')) return s
      const base = this.$http && this.$http.defaults && this.$http.defaults.baseURL ? this.$http.defaults.baseURL : '/api'
      return base + '/admin/team/avatar?key=' + encodeURIComponent(s)
    },
    enterpriseLogoPreviewUrl () {
      const key = this.subjectEditLegal && this.subjectEditLegal.enterpriseLogo
      if (!key) return ''
      const s = String(key).trim()
      if (!s) return ''
      const base = this.$http && this.$http.defaults && this.$http.defaults.baseURL ? this.$http.defaults.baseURL : '/api'
      return base + '/admin/team/avatar?key=' + encodeURIComponent(s)
    },
    consoleLoginIdDisplay () {
      return this.consoleLoginIdRaw || '—'
    },
    teamHubDisplay () {
      if (this.teamHubLoading) return '…'
      if (this.teamHubCount == null) return '—'
      return this.teamHubCount
    },
    teamActiveTodayCount () {
      const list = this.teamMembers || []
      return list.filter(m => m.activeToday).length
    },
    teamTotalLabel () {
      return String(this.teamHubDisplay)
    },
    kpiRepoCount () {
      if (!this.statsOverview) return '—'
      const n = this.statsOverview.repoCount
      return n != null ? n : '—'
    },
    kpiCommitCount () {
      if (!this.statsOverview) return '—'
      const n = this.statsOverview.totalCommits
      return n != null ? n : '—'
    },
    kpiAuthorCount () {
      if (!this.statsOverview) return '—'
      const n = this.statsOverview.authorCount
      return n != null ? n : '—'
    },
    /** 无筛选时与报价卡片一致展示最近 5 条；有筛选时在全部项目中匹配，最多 20 条 */
    prototypeHubListFiltered () {
      const items = Array.isArray(this.prototypeHub.items) ? this.prototypeHub.items : []
      const q = (this.prototypeHubFilter || '').trim().toLowerCase()
      if (!q) {
        return items.slice(0, 5)
      }
      return items.filter((row) => {
        const name = (row && row.name != null ? String(row.name) : '').toLowerCase()
        const id = row && row.id != null ? String(row.id) : ''
        return name.includes(q) || id.includes(q)
      }).slice(0, 20)
    },
    teamMembersAllSorted () {
      const list = Array.isArray(this.teamMembers) ? this.teamMembers.slice() : []
      list.sort((a, b) => {
        const aa = !!a.activeToday
        const bb = !!b.activeToday
        if (aa !== bb) return aa ? -1 : 1
        const na = (a.name || a.email || '').toString().toLowerCase()
        const nb = (b.name || b.email || '').toString().toLowerCase()
        if (na === nb) return 0
        return na < nb ? -1 : 1
      })
      return list
    },
    teamMembersEffective () {
      const all = this.teamMembersAllSorted
      if (!Array.isArray(all) || !all.length) return []
      if (this.teamSelectedMemberIds == null) return all
      const set = new Set(this.teamSelectedMemberIds)
      return all.filter(m => set.has(m.id))
    },
    teamMembersTotalPages () {
      const n = this.teamMembersEffective.length
      const ps = this.teamMembersDisplayPageSize || 6
      return Math.max(1, Math.ceil(n / ps))
    },
    displayedTeamMembers () {
      const list = this.teamMembersEffective
      const ps = this.teamMembersDisplayPageSize || 6
      const totalPages = Math.max(1, Math.ceil((list.length || 0) / ps))
      const page = Math.min(Math.max(1, this.teamMembersDisplayPage || 1), totalPages)
      const start = (page - 1) * ps
      return list.slice(start, start + ps)
    },
    pickerSelectAll () {
      if (!Array.isArray(this.teamMembers) || !this.teamMembers.length) return false
      return Array.isArray(this.teamPickerSelectedIds) && this.teamPickerSelectedIds.length === this.teamMembers.length
    },
    teamPickerPageMembers () {
      const list = this.teamMembersAllSorted
      const ps = this.teamPickerPageSize || 10
      const totalPages = Math.max(1, Math.ceil((list.length || 0) / ps))
      const page = Math.min(Math.max(1, this.teamPickerPage || 1), totalPages)
      const start = (page - 1) * ps
      return list.slice(start, start + ps)
    },
    teamPickerTotalPages () {
      const list = Array.isArray(this.teamMembers) ? this.teamMembers : []
      const ps = this.teamPickerPageSize || 10
      return Math.max(1, Math.ceil(list.length / ps))
    },
    aiHubDeepseekLine () {
      return this.formatAiProviderLine(this.aiHub.deepseek)
    },
    aiHubQwenLine () {
      return this.formatAiProviderLine(this.aiHub.qwen)
    },
    aiHubDeepseekOk () {
      return this.aiProviderReady(this.aiHub.deepseek)
    },
    aiHubQwenOk () {
      return this.aiProviderReady(this.aiHub.qwen)
    },
    diffHtml () {
      if (!this.diffText) return ''
      return this.diffText.split('\n').map(line => {
        const escaped = this.escapeHtml(line || ' ')
        if (line.startsWith('+') && !line.startsWith('+++')) return '<span class="diff-line diff-add">' + escaped + '</span>'
        if (line.startsWith('-') && !line.startsWith('---')) return '<span class="diff-line diff-del">' + escaped + '</span>'
        return '<span class="diff-line">' + escaped + '</span>'
      }).join('\n')
    }
  },
  created () {
    if (this.fixedRepoFullName) {
      this.selectedRepo = String(this.fixedRepoFullName)
    }
    // 团队成员展示设置：从本地持久化中恢复
    try {
      if (typeof localStorage !== 'undefined') {
        const k = 'autoattend_team_display_ids'
        const raw = localStorage.getItem(k)
        if (raw) {
          const parsed = JSON.parse(raw)
          if (parsed === null) {
            this.teamSelectedMemberIds = null
          } else if (Array.isArray(parsed)) {
            this.teamSelectedMemberIds = parsed
          }
        }
      }
    } catch (e) {
      // ignore storage errors
    }

    this.loadDashboard()
    this.loadCommits().then(() => this.tryOpenCommitFromQuery())
    this.loadStatsOverview()
    this.loadStatsCommitsByDay()
    this.loadStatsAuthors()
    this.loadConsoleHub()
    this.loadWorkspaceSummary()
    this.loadRepos().then(() => {
      if (this.selectedRepo) {
        this.loadRepoInfo()
        this.loadDailySummaries()
      }
    }).catch(() => {})
  },
  mounted () {
    this.$nextTick(() => {
      setTimeout(() => {
        if (this.chartTab === 'trend') this.renderTrendChart()
      }, 100)
    })
  },
  beforeDestroy () {
    document.body.style.overflow = ''
    if (this._onEscape) {
      document.removeEventListener('keydown', this._onEscape)
      this._onEscape = null
    }
    const chartKeys = ['trend', 'author']
    chartKeys.forEach(k => {
      if (this.chartInstances[k]) {
        this.chartInstances[k].destroy()
        this.chartInstances[k] = null
      }
    })
  },
  methods: {
    coerceBoolHub (v) {
      return v === true || v === 'true' || v === 1 || v === '1'
    },
    hubHasApiKey (d) {
      if (!d) return false
      if (d.hasApiKey === true) return true
      const m = d.apiKeyMasked
      return !!(m != null && String(m).trim() !== '')
    },
    aiProviderReady (d) {
      return !!(d && this.coerceBoolHub(d.enabled) && this.hubHasApiKey(d))
    },
    formatAiProviderLine (d) {
      if (!d) return this.$t('dashboard.consoleKeyUnset')
      const en = this.coerceBoolHub(d.enabled)
      const key = this.hubHasApiKey(d)
      const enLabel = en ? this.$t('dashboard.consoleEnabled') : this.$t('dashboard.consoleDisabled')
      const keyLabel = key ? this.$t('dashboard.consoleKeySet') : this.$t('dashboard.consoleKeyUnset')
      return `${enLabel} · ${keyLabel}`
    },
    loadConsoleHub () {
      this.loadQuoteHub()
      this.loadPrototypeHub()
      this.loadTeamHub()
      this.loadAiHub()
      this.loadNexusHub()
      this.loadPartyBProfileForHeader()
    },
    async loadNexusHub () {
      this.nexusHub.loading = true
      try {
        const resp = await this.$http.get('/admin/nexus/accounts')
        const items = (resp.data && resp.data.code === 0 && resp.data.data && resp.data.data.items) ? resp.data.data.items : []
        this.nexusHub.accountCount = Array.isArray(items) ? items.length : 0
      } catch (e) {
        this.nexusHub.accountCount = 0
      } finally {
        this.nexusHub.loading = false
      }
    },
    formatWsDate (v) {
      if (v == null || v === '') return ''
      const s = String(v)
      return s.length >= 16 ? s.slice(0, 16).replace('T', ' ') : s
    },
    async loadWorkspaceSummary () {
      this.workspaceSummaryLoading = true
      try {
        const resp = await this.$http.get('/admin/workspace/summary')
        if (resp.data && resp.data.code === 0 && resp.data.data) {
          this.workspaceSummary = resp.data.data
        } else {
          this.workspaceSummary = null
        }
      } catch (e) {
        this.workspaceSummary = null
      } finally {
        this.workspaceSummaryLoading = false
      }
    },
    openSubjectModal () {
      // partyBProfile 由 loadConsoleHub 拉取；若仍在加载，避免覆盖用户编辑
      if (this.partyBProfileLoading) return
      this.syncSubjectEditsFromPartyBProfile()

      this.subjectLegalMsg = ''
      this.subjectNaturalMsg = ''
      this.subjectLegalOk = false
      this.subjectNaturalOk = false
      this.showSubjectModal = true
      if (!this.selectedCommit) document.body.style.overflow = 'hidden'
    },
    closeSubjectModal () {
      this.showSubjectModal = false
      if (!this.selectedCommit) document.body.style.overflow = ''
    },
    async openWebhookModal () {
      this.showWebhookModal = true
      this.webhookCopiedMsg = ''
      this.webhookUrl = ''
      this.webhookModalLoading = true
      try {
        const resp = await this.$http.get('/admin/auth/me')
        if (resp.data && resp.data.code === 0 && resp.data.data && resp.data.data.slug) {
          this.tenantSlug = resp.data.data.slug
        }
        const slug = this.tenantSlug
        if (slug) {
          this.webhookUrl = window.location.origin + '/api/webhooks/github/' + encodeURIComponent(slug)
        }
      } catch (e) {
        this.webhookUrl = ''
      } finally {
        this.webhookModalLoading = false
      }
    },
    closeWebhookModal () {
      this.showWebhookModal = false
      this.webhookCopiedMsg = ''
    },
    openTeamPickerModal () {
      if (!Array.isArray(this.teamMembers) || !this.teamMembers.length) return
      this.showTeamPickerModal = true
      this.teamPickerSaving = false
      this.teamPickerLoading = false
      this.teamPickerPage = 1
      if (this.teamSelectedMemberIds == null) {
        this.teamPickerSelectedIds = this.teamMembersAllSorted.map(m => m.id)
      } else {
        this.teamPickerSelectedIds = this.teamSelectedMemberIds.slice()
      }
    },
    closeTeamPickerModal () {
      this.showTeamPickerModal = false
    },
    onPickerSelectAllChange (e) {
      const checked = e && e.target ? !!e.target.checked : false
      if (checked) {
        this.teamPickerSelectedIds = this.teamMembersAllSorted.map(m => m.id)
      } else {
        this.teamPickerSelectedIds = []
      }
    },
    teamPickerPrev () {
      if (this.teamPickerPage <= 1) return
      this.teamPickerPage -= 1
    },
    teamPickerNext () {
      if (this.teamPickerPage >= this.teamPickerTotalPages) return
      this.teamPickerPage += 1
    },
    saveTeamPicker () {
      this.teamPickerSaving = true
      try {
        const ids = Array.isArray(this.teamPickerSelectedIds) ? this.teamPickerSelectedIds.slice() : []
        // 全选：用 null 表示（避免保存一堆 id）
        if (ids.length === this.teamMembers.length) {
          this.teamSelectedMemberIds = null
        } else {
          this.teamSelectedMemberIds = ids
        }
        this.teamMembersDisplayPage = 1
        // 持久化保存到 localStorage
        try {
          if (typeof localStorage !== 'undefined') {
            const k = 'autoattend_team_display_ids'
            const v = this.teamSelectedMemberIds == null ? 'null' : JSON.stringify(this.teamSelectedMemberIds)
            localStorage.setItem(k, v)
          }
        } catch (e) {
          // ignore storage errors
        }
        this.closeTeamPickerModal()
      } finally {
        this.teamPickerSaving = false
      }
    },
    teamMembersDisplayPrev () {
      if (this.teamMembersDisplayPage <= 1) return
      this.teamMembersDisplayPage -= 1
    },
    teamMembersDisplayNext () {
      if (this.teamMembersDisplayPage >= this.teamMembersTotalPages) return
      this.teamMembersDisplayPage += 1
    },
    memberInitial (m) {
      const name = (m && (m.name || m.email)) ? (m.name || m.email) : ''
      if (!name) return '?'
      return String(name).charAt(0).toUpperCase()
    },
    avatarDisplayUrl (avatarKey) {
      if (!avatarKey || !String(avatarKey).trim()) return ''
      const s = String(avatarKey).trim()
      if (s.startsWith('http://') || s.startsWith('https://')) return s
      const base = this.$http && this.$http.defaults && this.$http.defaults.baseURL ? this.$http.defaults.baseURL : '/api'
      return base + '/admin/team/avatar?key=' + encodeURIComponent(s)
    },
    memberAvatarUrl (m) {
      const key = m && m.avatar
      if (key && String(key).trim()) {
        return this.avatarDisplayUrl(key)
      }
      // 管理员账号：若无个人头像，则默认使用企业 logo
      if (m && m.role === 'super_admin' && this.companyAvatarLogoUrl) {
        return this.companyAvatarLogoUrl
      }
      return ''
    },
    teamRoleLabel (role) {
      if (!role) return '—'
      const map = {
        member: this.$t('teamManage.roleMember'),
        sub_admin: this.$t('teamManage.roleSubAdmin'),
        super_admin: this.$t('teamManage.roleSuperAdmin')
      }
      return map[role] || role
    },
    teamRoleTagClass (role) {
      const r = (role || '').toString()
      if (r === 'super_admin') return 'tag-super-admin'
      if (r === 'sub_admin') return 'tag-sub-admin'
      if (r === 'member') return 'tag-member'
      return 'tag-other'
    },
    async copyWebhookUrl () {
      const url = this.webhookUrl
      if (!url) return
      if (navigator.clipboard && navigator.clipboard.writeText) {
        try {
          await navigator.clipboard.writeText(url)
          this.webhookCopiedMsg = this.$t('dashboard.consoleCopied')
          setTimeout(() => { this.webhookCopiedMsg = '' }, 2000)
          return
        } catch (e) { /* fallback */ }
      }
      const ta = document.createElement('textarea')
      ta.value = url
      document.body.appendChild(ta)
      ta.select()
      try {
        document.execCommand('copy')
        this.webhookCopiedMsg = this.$t('dashboard.consoleCopied')
        setTimeout(() => { this.webhookCopiedMsg = '' }, 2000)
      } catch (e) { /* ignore */ }
      document.body.removeChild(ta)
    },
    syncSubjectEditsFromPartyBProfile () {
      const d = this.partyBProfile || {}
      const np = d.naturalPerson && typeof d.naturalPerson === 'object' ? d.naturalPerson : {}
      this.subjectEditLegal = {
        legalName: d.legalName || '',
        creditCode: d.creditCode || '',
        address: d.address || '',
        contactName: d.contactName || '',
        contactPhone: d.contactPhone || '',
        bankName: d.bankName || '',
        bankAccount: d.bankAccount || '',
        enterpriseLogo: d.enterpriseLogo || ''
      }
      this.subjectEditNatural = {
        fullName: np.fullName || '',
        idNumber: np.idNumber || '',
        address: np.address || '',
        contactPhone: np.contactPhone || '',
        email: np.email || '',
        bankName: np.bankName || '',
        bankAccount: np.bankAccount || ''
      }
    },
    pickEnterpriseLogo () {
      if (!this.$refs.enterpriseLogoFileInput) return
      this.$refs.enterpriseLogoFileInput.click()
    },
    async onEnterpriseLogoFileChange (e) {
      const file = e && e.target && e.target.files && e.target.files[0]
      if (!file) return
      this.enterpriseLogoUploading = true
      try {
        const name = (file.name || '').toLowerCase()
        if (!name.endsWith('.png') && !name.endsWith('.jpg') && !name.endsWith('.jpeg') && !name.endsWith('.gif') && !name.endsWith('.webp')) {
          alert('请选择 PNG/JPG/GIF/WEBP 图片')
          e.target.value = ''
          return
        }
        const uploadFile = await compressImageFile(file, IMAGE_COMPRESS_PRESETS.attachment)
        const form = new FormData()
        form.append('file', uploadFile)
        const r = await this.$http.post('/admin/team/avatar-upload', form, {
          headers: { 'Content-Type': 'multipart/form-data' }
        })
        if (r.data && r.data.code === 0 && r.data.data && r.data.data.key) {
          // 这里复用 avatar-upload：仅负责把图片存入 MinIO 并返回 key
          this.subjectEditLegal.enterpriseLogo = r.data.data.key
          // 不强制立刻保存到 party-b-profile：用户点击保存主体信息才会写入 JSON
        } else {
          alert(r.data && r.data.message ? r.data.message : '上传失败')
        }
      } catch (err) {
        alert(err.response && err.response.data && err.response.data.message ? err.response.data.message : '上传失败')
      } finally {
        this.enterpriseLogoUploading = false
        if (e && e.target) e.target.value = ''
      }
    },
    partyBLegalPayloadForModal () {
      return {
        legalName: this.subjectEditLegal.legalName,
        creditCode: this.subjectEditLegal.creditCode,
        address: this.subjectEditLegal.address,
        contactName: this.subjectEditLegal.contactName,
        contactPhone: this.subjectEditLegal.contactPhone,
        bankName: this.subjectEditLegal.bankName,
        bankAccount: this.subjectEditLegal.bankAccount,
        // 预留：仅由弹窗写入；报价配置页仍会通过同一接口读取并保持同步
        enterpriseLogo: this.subjectEditLegal.enterpriseLogo
      }
    },
    async saveSubjectLegal () {
      this.subjectLegalSaving = true
      this.subjectLegalMsg = ''
      this.subjectLegalOk = false
      try {
        const resp = await this.$http.put('/admin/quote/party-b-profile', this.partyBLegalPayloadForModal())
        if (resp.data && resp.data.code === 0) {
          this.subjectLegalOk = true
          this.subjectLegalMsg = this.$t('quote.partyBSaveOk')
          await this.loadPartyBProfileForHeader()
          // 保存后重新同步一遍，避免用户上传 logo 但保存失败导致显示偏差
          this.syncSubjectEditsFromPartyBProfile()
        } else {
          this.subjectLegalOk = false
          this.subjectLegalMsg = (resp.data && resp.data.message) || this.$t('quote.partyBSaveFail')
        }
      } catch (e) {
        this.subjectLegalOk = false
        this.subjectLegalMsg = (e.response && e.response.data && e.response.data.message) || this.$t('quote.partyBSaveFail')
      } finally {
        this.subjectLegalSaving = false
      }
    },
    async saveSubjectNatural () {
      this.subjectNaturalSaving = true
      this.subjectNaturalMsg = ''
      this.subjectNaturalOk = false
      try {
        const resp = await this.$http.put('/admin/quote/party-b-profile', {
          naturalPerson: { ...this.subjectEditNatural }
        })
        if (resp.data && resp.data.code === 0) {
          this.subjectNaturalOk = true
          this.subjectNaturalMsg = this.$t('quote.partyBSaveOk')
          await this.loadPartyBProfileForHeader()
          this.syncSubjectEditsFromPartyBProfile()
        } else {
          this.subjectNaturalOk = false
          this.subjectNaturalMsg = (resp.data && resp.data.message) || this.$t('quote.partyBSaveFail')
        }
      } catch (e) {
        this.subjectNaturalOk = false
        this.subjectNaturalMsg = (e.response && e.response.data && e.response.data.message) || this.$t('quote.partyBSaveFail')
      } finally {
        this.subjectNaturalSaving = false
      }
    },
    async loadPartyBProfileForHeader () {
      this.partyBProfileLoading = true
      try {
        const resp = await this.$http.get('/admin/quote/party-b-profile')
        if (resp.data && resp.data.code === 0 && resp.data.data) {
          this.partyBProfile = resp.data.data
        } else {
          this.partyBProfile = {}
        }
      } catch (e) {
        this.partyBProfile = {}
      } finally {
        this.partyBProfileLoading = false
      }
    },
    async loadQuoteHub () {
      this.quoteHub.loading = true
      try {
        const resp = await this.$http.get('/admin/quote/projects', { params: { page: 1, pageSize: 5 } })
        if (resp.data && resp.data.code === 0 && resp.data.data) {
          this.quoteHub.items = resp.data.data.items || []
          this.quoteHub.total = Number(resp.data.data.total) || 0
        } else {
          this.quoteHub.items = []
          this.quoteHub.total = 0
        }
      } catch (e) {
        this.quoteHub.items = []
        this.quoteHub.total = 0
      } finally {
        this.quoteHub.loading = false
      }
    },
    async loadPrototypeHub () {
      this.prototypeHub.loading = true
      try {
        const resp = await this.$http.get('/admin/ui-prototype/projects')
        if (resp.data && resp.data.code === 0 && resp.data.data) {
          const items = resp.data.data.items || []
          this.prototypeHub.items = items
          this.prototypeHub.total = items.length
        } else {
          this.prototypeHub.items = []
          this.prototypeHub.total = 0
        }
      } catch (e) {
        this.prototypeHub.items = []
        this.prototypeHub.total = 0
      } finally {
        this.prototypeHub.loading = false
      }
    },
    async loadTeamHub () {
      this.teamHubLoading = true
      this.teamMembersLoading = true
      try {
        const [mResp, aResp] = await Promise.all([
          this.$http.get('/admin/team/members'),
          this.$http.get('/admin/team/members/active-authors-today').catch(() => ({ data: null }))
        ])

        const members = (mResp.data && mResp.data.code === 0 && Array.isArray(mResp.data.data))
          ? mResp.data.data
          : []

        const emailsRaw = aResp.data && aResp.data.code === 0 && aResp.data.data ? aResp.data.data.emails : []
        const activeSet = new Set((emailsRaw || []).map(e => (e || '').toString().trim().toLowerCase()).filter(Boolean))

        this.teamMembers = (members || []).map(m => {
          const email = (m.email || '').toString().trim().toLowerCase()
          return Object.assign({}, m, { activeToday: activeSet.has(email) })
        })
        this.teamHubCount = this.teamMembers.length

        // 初始化：默认全选（teamSelectedMemberIds=null）
        if (this.teamSelectedMemberIds != null && Array.isArray(this.teamSelectedMemberIds)) {
          // 过滤掉已不存在的成员
          const exist = new Set(this.teamMembers.map(x => x.id))
          this.teamSelectedMemberIds = this.teamSelectedMemberIds.filter(id => exist.has(id))
          if (!this.teamSelectedMemberIds.length) this.teamSelectedMemberIds = null
        }
      } catch (e) {
        this.teamHubCount = null
        this.teamMembers = []
      } finally {
        this.teamHubLoading = false
        this.teamMembersLoading = false
      }
    },
    async loadAiHub () {
      this.aiHub.loading = true
      this.aiHub.deepseek = null
      this.aiHub.qwen = null
      try {
        const [ds, qw] = await Promise.all([
          this.$http.get('/admin/ai-analysis/config').catch(() => ({ data: null })),
          this.$http.get('/admin/ai-analysis/qwen-config').catch(() => ({ data: null }))
        ])
        if (ds.data && ds.data.code === 0 && ds.data.data) this.aiHub.deepseek = ds.data.data
        if (qw.data && qw.data.code === 0 && qw.data.data) this.aiHub.qwen = qw.data.data
      } finally {
        this.aiHub.loading = false
      }
    },
    copyConsoleIdentity () {
      const u = this.consoleLoginIdRaw
      if (!u) return
      if (navigator.clipboard && navigator.clipboard.writeText) {
        navigator.clipboard.writeText(u).then(() => {
          this.copyIdentityMsg = this.$t('dashboard.consoleCopied')
          setTimeout(() => { this.copyIdentityMsg = '' }, 2000)
        }).catch(() => this.fallbackCopyUsername(u))
      } else {
        this.fallbackCopyUsername(u)
      }
    },
    fallbackCopyUsername (text) {
      const ta = document.createElement('textarea')
      ta.value = text
      document.body.appendChild(ta)
      ta.select()
      try {
        document.execCommand('copy')
        this.copyIdentityMsg = this.$t('dashboard.consoleCopied')
        setTimeout(() => { this.copyIdentityMsg = '' }, 2000)
      } catch (e) { /* ignore */ }
      document.body.removeChild(ta)
    },
    async loadStatsOverview () {
      try {
        const resp = await this.$http.get('/admin/stats/overview', {
          params: { repoFullName: this.selectedRepo || undefined }
        })
        if (resp.data && resp.data.code === 0) this.statsOverview = resp.data.data
      } catch (e) { console.error('loadStatsOverview failed', e) }
    },
    async loadRepoInfo () {
      if (!this.selectedRepo) return
      this.repoInfoLoading = true
      this.repoInfo = {}
      try {
        const resp = await this.$http.get('/admin/stats/repo-info', {
          params: { repoFullName: this.selectedRepo }
        })
        if (resp.data && resp.data.code === 0 && resp.data.data) this.repoInfo = resp.data.data
      } catch (e) { console.error('loadRepoInfo failed', e) }
      finally { this.repoInfoLoading = false }
    },
    async loadStatsCommitsByDay () {
      try {
        const resp = await this.$http.get('/admin/stats/commits-by-day', {
          params: { range: this.trendRange, repoFullName: this.selectedRepo || undefined }
        })
        if (resp.data && resp.data.code === 0) this.commitsByDay = resp.data.data || []
      } catch (e) { console.error('loadStatsCommitsByDay failed', e) }
    },
    async loadStatsAuthors () {
      this.authorRankLoading = true
      try {
        const params = {
          repoFullName: this.selectedRepo || undefined,
          period: this.authorRankPeriod,
          offset: this.authorRankOffset
        }
        if (this.authorRankPeriod === 'total') {
          params.offset = 0
        }
        const resp = await this.$http.get('/admin/stats/authors', { params })
        const raw = resp.data && resp.data.data
        if (resp.data && resp.data.code === 0 && raw) {
          if (Array.isArray(raw.items)) {
            this.authorsStats = raw.items.slice(0, 15)
            this.authorRankMeta = {
              period: raw.period,
              offset: raw.offset != null ? raw.offset : this.authorRankOffset,
              periodStart: raw.periodStart,
              periodEndInclusive: raw.periodEndInclusive,
              canGoPrevious: raw.canGoPrevious !== false,
              canGoNext: raw.canGoNext === true
            }
            if (raw.offset != null && this.authorRankPeriod !== 'total') {
              this.authorRankOffset = Number(raw.offset) || 0
            }
          } else if (Array.isArray(raw)) {
            this.authorsStats = raw.slice(0, 15)
            this.authorRankMeta = { canGoNext: false, canGoPrevious: false }
          } else {
            this.authorsStats = []
            this.authorRankMeta = { canGoNext: false, canGoPrevious: true }
          }
        } else {
          this.authorsStats = []
        }
      } catch (e) {
        console.error('loadStatsAuthors failed', e)
        this.authorsStats = []
      } finally {
        this.authorRankLoading = false
      }
    },
    setAuthorRankPeriod (p) {
      if (this.authorRankPeriod === p) return
      this.authorRankPeriod = p
      this.authorRankOffset = 0
      this.loadStatsAuthors()
    },
    authorRankPrev () {
      this.authorRankOffset -= 1
      this.loadStatsAuthors()
    },
    authorRankNext () {
      if (!this.authorRankMeta.canGoNext) return
      this.authorRankOffset += 1
      this.loadStatsAuthors()
    },
    setChartTab (tab) {
      if (this.chartTab === tab) return
      if (this.chartInstances.trend) {
        try { this.chartInstances.trend.destroy() } catch (e) { /* noop */ }
        this.chartInstances.trend = null
      }
      if (this.chartInstances.author) {
        try { this.chartInstances.author.destroy() } catch (e) { /* noop */ }
        this.chartInstances.author = null
      }
      this.chartTab = tab
      this.$nextTick(() => {
        setTimeout(() => {
          if (tab === 'trend') this.renderTrendChart()
          else this.renderAuthorChart()
        }, 80)
      })
    },
    renderTrendChart () {
      this.$nextTick(() => {
        const el = this.$refs.trendChart
        if (!el) return
        if (this.chartInstances.trend) this.chartInstances.trend.destroy()
        const days = this.trendRange === '30d' ? 30 : 7
        const formatDayLabel = (d) => {
          const v = d.date
          if (v == null) return ''
          if (typeof v === 'string') return v.slice(0, 10).slice(5)
          if (typeof v === 'number') {
            const t = new Date(v)
            return String(t.getMonth() + 1).padStart(2, '0') + '-' + String(t.getDate()).padStart(2, '0')
          }
          return String(v).slice(0, 10).slice(5)
        }
        let labels = (this.commitsByDay || []).map(formatDayLabel)
        let counts = (this.commitsByDay || []).map(d => d.count || 0)
        if (labels.length === 0) {
          const d = new Date()
          for (let i = days - 1; i >= 0; i--) {
            const t = new Date(d)
            t.setDate(t.getDate() - i)
            const m = String(t.getMonth() + 1).padStart(2, '0')
            const day = String(t.getDate()).padStart(2, '0')
            labels.push(m + '-' + day)
            counts.push(0)
          }
        }
        this.chartInstances.trend = new ChartJS(el, {
          type: 'bar',
          data: {
            labels,
            datasets: [
              { label: this.$t('dashboard.recentCommits'), data: counts, backgroundColor: 'rgba(6, 182, 212, 0.55)', borderColor: 'rgb(34, 211, 238)', borderWidth: 1 }
            ]
          },
          options: {
            responsive: true,
            maintainAspectRatio: true,
            plugins: { legend: { display: false } },
            scales: {
              y: { beginAtZero: true, ticks: { stepSize: 1 } }
            }
          }
        })
      })
    },
    renderAuthorChart () {
      this.$nextTick(() => {
        const el = this.$refs.authorChart
        if (!el) return
        if (this.chartInstances.author) this.chartInstances.author.destroy()
        const data = this.authorsStats || []
        const noDataLabel = this.$t('dashboard.authorRankNoData')
        const labels = data.length ? data.map(a => a.authorName || a.authorEmail || '') : [noDataLabel]
        const counts = data.length ? data.map(a => a.commitCount || 0) : [0]
        this.chartInstances.author = new ChartJS(el, {
          type: 'bar',
          data: {
            labels,
            datasets: [{ label: this.$t('dashboard.commits'), data: counts, backgroundColor: 'rgba(99, 102, 241, 0.55)', borderColor: 'rgb(129, 140, 248)', borderWidth: 1 }]
          },
          options: {
            indexAxis: 'y',
            responsive: true,
            maintainAspectRatio: true,
            plugins: { legend: { display: false } },
            scales: {
              x: { beginAtZero: true, ticks: { stepSize: 1 } }
            }
          }
        })
      })
    },
    async loadRepos () {
      try {
        const resp = await this.$http.get('/admin/repos')
        if (resp.data && resp.data.code === 0) {
          this.repos = resp.data.data.items || []
        }
      } catch (e) {
        console.error('loadRepos failed', e)
      }
    },
    onRepoChange () {
      this.commitsPage = 1
      this.authorRankOffset = 0
      this.dailySummaryPage = 1
      this.dailySummaries = []
      this.dailySummaryTotal = 0
      this.dailySummaryRunMessage = ''
      this.dailySummaryDetail = null
      this.loadDashboard()
      this.loadCommits()
      this.loadDailySummaries()
      this.loadStatsOverview()
      this.loadStatsCommitsByDay()
      this.loadStatsAuthors()
      if (this.selectedRepo) this.loadRepoInfo()
      else this.repoInfo = {}
    },
    async loadDashboard () {
      try {
        const resp = await this.$http.get('/admin/dashboard', {
          params: { range: '24h', repoFullName: this.selectedRepo || undefined }
        })
        const d = resp.data
        if (!d) return
        const ok = d.code === 0 || d.code === '0'
        const data = d.data || d
        if (!ok || !data) return
        this.dashboard = data
        this.authors = data.authors || []
      } catch (e) {
        console.error('loadDashboard failed', e)
      }
    },
    async loadCommits () {
      this.commitsLoading = true
      try {
        const resp = await this.$http.get('/admin/commits', {
          params: {
            page: this.commitsPage,
            pageSize: this.commitsPageSize,
            repoFullName: this.selectedRepo || undefined
          }
        })
        const d = resp.data
        if (!d) { this.commitsLoading = false; return }
        const ok = d.code === 0 || d.code === '0'
        const payload = d.data != null ? d.data : d
        const list = (payload && payload.items) || d.items
        if (ok && Array.isArray(list)) {
          this.commits = list
          const t = payload && payload.total != null ? payload.total : d.total
          if (typeof t === 'number') this.commitsTotal = t
          else if (t != null) this.commitsTotal = Number(t) || 0
          else this.commitsTotal = 0
          const tp = Math.max(1, Math.ceil(this.commitsTotal / this.commitsPageSize))
          if (this.commitsTotal > 0 && this.commitsPage > tp) {
            this.commitsPage = tp
            await this.loadCommits()
            return
          }
        }
      } catch (e) {
        console.error('loadCommits failed', e)
      } finally {
        this.commitsLoading = false
      }
    },
    onCommitsPageSizeChange () {
      this.commitsPage = 1
      this.loadCommits()
    },
    goCommitsPrev () {
      if (this.commitsPage <= 1) return
      this.commitsPage -= 1
      this.loadCommits()
    },
    goCommitsNext () {
      if (this.commitsPage >= this.commitsTotalPages) return
      this.commitsPage += 1
      this.loadCommits()
    },
    async loadDailySummaries () {
      if (!this.selectedRepo) {
        this.dailySummaries = []
        this.dailySummaryTotal = 0
        return
      }
      this.dailySummaryLoading = true
      try {
        const resp = await this.$http.get('/admin/ai-analysis/daily-summaries', {
          params: {
            repoFullName: this.selectedRepo,
            page: this.dailySummaryPage,
            pageSize: this.dailySummaryPageSize
          }
        })
        const d = resp.data && resp.data.data
        if (resp.data && resp.data.code === 0 && d && Array.isArray(d.items)) {
          this.dailySummaries = d.items
          const t = d.total != null ? Number(d.total) : 0
          this.dailySummaryTotal = t
          const tp = Math.max(1, Math.ceil(t / this.dailySummaryPageSize))
          if (t > 0 && this.dailySummaryPage > tp) {
            this.dailySummaryPage = tp
            await this.loadDailySummaries()
            return
          }
        } else {
          this.dailySummaries = []
          this.dailySummaryTotal = 0
        }
      } catch (e) {
        this.dailySummaries = []
        this.dailySummaryTotal = 0
      } finally {
        this.dailySummaryLoading = false
      }
    },
    onDailySummaryPageSizeChange () {
      this.dailySummaryPage = 1
      this.loadDailySummaries()
    },
    goDailySummaryPrev () {
      if (this.dailySummaryPage <= 1) return
      this.dailySummaryPage -= 1
      this.loadDailySummaries()
    },
    goDailySummaryNext () {
      if (this.dailySummaryPage >= this.dailySummaryTotalPages) return
      this.dailySummaryPage += 1
      this.loadDailySummaries()
    },
    async runDailySummaryForRepo () {
      if (!this.selectedRepo) return
      this.dailySummaryRunLoading = true
      this.dailySummaryRunMessage = ''
      try {
        const resp = await this.$http.post('/admin/ai-analysis/daily-summary/run', null, {
          params: { repoFullName: this.selectedRepo }
        })
        if (resp.data && resp.data.code === 0 && resp.data.data) {
          const n = resp.data.data.reposProcessed != null ? resp.data.data.reposProcessed : 0
          const d = resp.data.data.summaryDate || ''
          this.dailySummaryRunOk = true
          this.dailySummaryRunMessage = this.$t('aiConfig.runDailySummaryOk', { date: d, n })
          this.dailySummaryPage = 1
          await this.loadDailySummaries()
        } else {
          this.dailySummaryRunOk = false
          this.dailySummaryRunMessage = (resp.data && resp.data.message) || this.$t('aiConfig.runDailySummaryFail')
        }
      } catch (e) {
        this.dailySummaryRunOk = false
        this.dailySummaryRunMessage = (e.response && e.response.data && e.response.data.message) || this.$t('aiConfig.runDailySummaryFail')
      } finally {
        this.dailySummaryRunLoading = false
      }
    },
    async openDailySummaryDetail (id) {
      this.dailyDetailLoading = true
      this.dailySummaryDetail = {
        id,
        title: this.$t('collab.loading'),
        summaryDate: '',
        repoFullName: this.selectedRepo || '',
        status: '',
        content: ''
      }
      try {
        const resp = await this.$http.get('/admin/ai-analysis/daily-summaries/' + id)
        if (resp.data && resp.data.code === 0 && resp.data.data) {
          this.dailySummaryDetail = resp.data.data
        }
      } catch (e) {
        this.dailySummaryDetail = null
      } finally {
        this.dailyDetailLoading = false
      }
    },
    isDiffPlaceholder (text) {
      return text && (text.indexOf('(Diff 暂不可用') !== -1 || text.indexOf('(Diff will be') !== -1)
    },
    shortSha (sha) {
      if (!sha) return ''
      return sha.substring(0, 8)
    },
    formatTime (t) {
      if (!t) return ''
      try {
        return new Date(t).toLocaleString()
      } catch (e) {
        return t
      }
    },
    tryOpenCommitFromQuery () {
      const q = this.$route.query
      if (!q || !q.commitSha || !q.repoFullName) return
      if (this.selectedCommit && this.selectedCommit.commitSha === q.commitSha && this.selectedCommit.repoFullName === q.repoFullName) return
      const item = this.commits.find(c => c.commitSha === q.commitSha && c.repoFullName === q.repoFullName)
      if (item) {
        this.viewDiff(item)
        return
      }
      // 深链进入时该提交可能不在当前页，仍可根据 query 打开 Diff
      this.viewDiff({
        commitSha: q.commitSha,
        repoFullName: q.repoFullName,
        authorName: '',
        message: '',
        committedAt: null,
        filesChanged: null,
        insertions: null,
        deletions: null
      })
    },
    commitAnalysisRoute (item) {
      if (!item || !item.commitSha) return { path: '/console' }
      return {
        path: `/commit/${item.commitSha}`,
        query: item.repoFullName ? { repoFullName: item.repoFullName } : {}
      }
    },
    retryFetchDiff () {
      if (this.selectedCommit) this.viewDiff(this.selectedCommit)
    },
    async loadAiAnalysisResult () {
      if (!this.selectedCommit) return
      this.aiAnalysisLoading = true
      this.aiAnalysisResult = null
      try {
        const params = {}
        if (this.selectedCommit.repoFullName) params.repoFullName = this.selectedCommit.repoFullName
        const resp = await this.$http.get(`/admin/ai-analysis/commits/${this.selectedCommit.commitSha}/result`, { params })
        if (resp.data && resp.data.code === 0 && resp.data.data) {
          this.aiAnalysisResult = resp.data.data
        }
      } catch (e) {
        if (e.response && e.response.status !== 404) console.error('loadAiAnalysisResult failed', e)
      } finally {
        this.aiAnalysisLoading = false
      }
    },
    async runAiAnalysis () {
      if (!this.selectedCommit || this.aiAnalysisRunning) return
      this.aiAnalysisRunning = true
      try {
        const params = {}
        if (this.selectedCommit.repoFullName) params.repoFullName = this.selectedCommit.repoFullName
        // 组件点击埋点：由“运行 AI 分析”触发
        this.$http.post('/admin/ops/events/component-click', {
          componentKey: 'ai_commit_analysis',
          coreApiKey: 'ai_analysis_run'
        }).catch(() => {})
        const resp = await this.$http.post(`/admin/ai-analysis/commits/${this.selectedCommit.commitSha}/run`, null, { params })
        if (resp.data && resp.data.code === 0 && resp.data.data) {
          this.aiAnalysisResult = resp.data.data
        } else {
          alert((resp.data && resp.data.message) || this.$t('dashboard.aiRunFailed'))
        }
      } catch (e) {
        alert((e.response && e.response.data && e.response.data.message) || this.$t('dashboard.aiRunFailed'))
      } finally {
        this.aiAnalysisRunning = false
      }
    },
    async viewDiff (item) {
      this.selectedCommit = item
      this.diffText = ''
      this.diffMeta = null
      this.diffLoading = true
      try {
        const params = { mode: 'raw' }
        if (item.repoFullName) params.repoFullName = item.repoFullName
        const resp = await this.$http.get(`/admin/commits/${item.commitSha}/diff`, { params })
        if (resp.data && resp.data.code === 0) {
          const d = resp.data.data
          this.diffText = (d && d.diffText) || ''
          this.diffMeta = d ? { repoFullName: d.repoFullName, commitSha: d.commitSha, message: d.message, authorName: d.authorName, authorEmail: d.authorEmail, committedAt: d.committedAt } : null
        } else {
          this.diffText = this.$t('dashboard.diffLoadFailed')
        }
      } catch (e) {
        this.diffText = this.$t('dashboard.diffLoadFailed')
      } finally {
        this.diffLoading = false
        this.loadAiAnalysisResult().then(() => {
          if (!this.aiAnalysisResult && this.diffText && !this.isDiffPlaceholder(this.diffText)) this.runAiAnalysis()
        })
      }
    },
    escapeHtml (s) {
      const div = document.createElement('div')
      div.textContent = s
      return div.innerHTML
    },
    copyDiffForAi () {
      const meta = this.diffMeta || {}
      const header = [
        `仓库: ${meta.repoFullName || this.selectedCommit?.repoFullName || ''}`,
        `提交: ${meta.commitSha || this.selectedCommit?.commitSha || ''}`,
        `说明: ${meta.message || ''}`,
        `作者: ${meta.authorName || ''} <${meta.authorEmail || ''}>`,
        `时间: ${meta.committedAt || ''}`,
        '--- diff ---'
      ].join('\n')
      const text = header + '\n' + (this.diffText || '')
      if (navigator.clipboard && navigator.clipboard.writeText) {
        navigator.clipboard.writeText(text).then(() => {
          alert(this.$t('dashboard.copied'))
        }).catch(() => {
          this.fallbackCopy(text)
        })
      } else {
        this.fallbackCopy(text)
      }
    },
    fallbackCopy (text) {
      const ta = document.createElement('textarea')
      ta.value = text
      document.body.appendChild(ta)
      ta.select()
      try {
        document.execCommand('copy')
        alert(this.$t('dashboard.copied'))
      } catch (e) {
        alert(this.$t('dashboard.copyFailed'))
      }
      document.body.removeChild(ta)
    }
  }
}
</script>

<style scoped>
/* ============================================================
   DashboardView – Feishu / Lark Design System Styles
   All class names preserved; colours & spacing use theme.css
   CSS custom properties for consistency.
   ============================================================ */

/* --- Shell & Layout --- */
.console-shell {
  min-height: 100%;
  background: var(--bg-page);
  padding: var(--space-xl) var(--space-lg) var(--space-xxxl);
  box-sizing: border-box;
}

.console-inner {
  max-width: 1280px;
  margin: 0 auto;
}

.dashboard {
  display: flex;
  flex-direction: column;
  gap: var(--space-xl);
}

/* —— 工作台指挥中心（监测统筹 + 科技感） —— */
.dash-modern .console-inner {
  max-width: 1320px;
}

.dash-command {
  position: relative;
  overflow: hidden;
  border-radius: 16px;
  padding: var(--space-xl) var(--space-xl) var(--space-lg);
  border: 1px solid rgba(99, 102, 241, 0.22);
  background: linear-gradient(145deg, rgba(15, 23, 42, 0.04) 0%, rgba(255, 255, 255, 0.92) 42%, rgba(240, 249, 255, 0.95) 100%);
  box-shadow:
    0 0 0 1px rgba(255, 255, 255, 0.6) inset,
    0 12px 40px rgba(15, 23, 42, 0.08);
}

.dash-command-bg {
  position: absolute;
  inset: -40% -20% auto -20%;
  height: 70%;
  background:
    radial-gradient(ellipse 80% 60% at 20% 20%, rgba(34, 211, 238, 0.18), transparent 55%),
    radial-gradient(ellipse 70% 50% at 85% 10%, rgba(99, 102, 241, 0.2), transparent 50%),
    radial-gradient(ellipse 50% 40% at 60% 80%, rgba(20, 86, 240, 0.08), transparent 45%);
  pointer-events: none;
  opacity: 0.95;
}

.dash-command-top {
  position: relative;
  display: grid;
  grid-template-columns: 1fr minmax(220px, 320px);
  gap: var(--space-xl);
  align-items: start;
}

@media (max-width: 960px) {
  .dash-command-top {
    grid-template-columns: 1fr;
  }
}

.dash-command-intro {
  min-width: 0;
}

.dash-eyebrow {
  margin: 0 0 var(--space-xs);
  font-size: 11px;
  font-weight: 600;
  letter-spacing: 0.14em;
  text-transform: uppercase;
  color: #6366f1;
}

.dash-title {
  margin: 0 0 var(--space-sm);
  font-size: clamp(1.35rem, 2.5vw, 1.75rem);
  font-weight: 700;
  letter-spacing: -0.02em;
  color: var(--text-primary);
  line-height: 1.2;
}

.dash-mission {
  margin: 0 0 var(--space-lg);
  font-size: var(--font-size-sm);
  line-height: 1.55;
  color: var(--text-secondary);
  max-width: 36rem;
}

.dash-status-strip {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
}

.dash-chip {
  position: relative;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 500;
  color: var(--text-secondary);
  background: rgba(255, 255, 255, 0.75);
  border: 1px solid rgba(148, 163, 184, 0.35);
  backdrop-filter: blur(8px);
}

.dash-chip-glow {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #94a3b8;
  flex-shrink: 0;
}

.dash-chip--ok .dash-chip-glow {
  background: #22c55e;
  box-shadow: 0 0 8px rgba(34, 197, 94, 0.7);
}

.dash-chip--warn .dash-chip-glow {
  background: #f59e0b;
  box-shadow: 0 0 8px rgba(245, 158, 11, 0.55);
}

.dash-chip--neutral {
  background: rgba(248, 250, 252, 0.85);
}

.dash-chip--ent {
  font-weight: 600;
  color: #0f172a;
  background: linear-gradient(135deg, #e0e7ff, #cffafe);
  border-color: rgba(99, 102, 241, 0.35);
}

.dash-chip--link {
  text-decoration: none;
  cursor: pointer;
  transition: border-color 0.15s, color 0.15s, box-shadow 0.15s;
}

.dash-chip--link:hover {
  border-color: rgba(99, 102, 241, 0.45);
  color: #4f46e5;
  box-shadow: 0 0 0 3px rgba(99, 102, 241, 0.12);
}

.dash-command-identity {
  position: relative;
  display: flex;
  gap: var(--space-md);
  padding: var(--space-md);
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.55);
  border: 1px solid rgba(226, 232, 240, 0.9);
  backdrop-filter: blur(10px);
}

.dash-identity-avatar {
  width: 52px;
  height: 52px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  font-weight: 700;
  color: #fff;
  background: linear-gradient(135deg, #6366f1, #22d3ee);
  flex-shrink: 0;
  overflow: hidden;
}

.dash-identity-avatar .identity-avatar-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.dash-identity-body {
  min-width: 0;
  flex: 1;
}

.dash-identity-title-row {
  display: flex;
  align-items: center;
  gap: var(--space-sm);
  flex-wrap: wrap;
  margin-bottom: 4px;
}

.dash-identity-name {
  font-size: var(--font-size-base);
  font-weight: 600;
  color: var(--text-primary);
}

.dash-identity-edit {
  font-size: var(--font-size-xs);
  padding: 2px 8px;
}

.dash-identity-meta {
  margin-bottom: 6px;
}

.dash-identity-tags {
  flex-wrap: wrap;
}

.dash-kpi-grid {
  position: relative;
  display: grid;
  grid-template-columns: repeat(6, minmax(0, 1fr));
  gap: 12px;
  margin-top: var(--space-lg);
  padding-top: var(--space-lg);
  border-top: 1px solid rgba(148, 163, 184, 0.25);
}

@media (max-width: 1100px) {
  .dash-kpi-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@media (max-width: 520px) {
  .dash-kpi-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

.dash-kpi {
  padding: 14px 14px 12px;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.72);
  border: 1px solid rgba(226, 232, 240, 0.95);
  transition: border-color 0.15s, box-shadow 0.15s;
}

.dash-kpi:hover {
  border-color: rgba(99, 102, 241, 0.35);
  box-shadow: 0 4px 20px rgba(99, 102, 241, 0.08);
}

.dash-kpi--accent {
  background: linear-gradient(135deg, rgba(238, 242, 255, 0.95), rgba(224, 242, 254, 0.9));
  border-color: rgba(99, 102, 241, 0.28);
}

.dash-kpi-value {
  display: block;
  font-size: 1.5rem;
  font-weight: 700;
  font-variant-numeric: tabular-nums;
  letter-spacing: -0.02em;
  color: var(--text-primary);
  line-height: 1.15;
}

.dash-kpi-label {
  display: block;
  margin-top: 4px;
  font-size: 11px;
  font-weight: 500;
  color: var(--text-tertiary);
  text-transform: uppercase;
  letter-spacing: 0.06em;
}

.dash-kpi-hint {
  display: block;
  margin-top: 6px;
  font-size: 11px;
  color: var(--text-disabled);
  line-height: 1.35;
}

.dash-kpi-link {
  display: inline-block;
  margin-top: 8px;
  font-size: 12px;
  font-weight: 500;
  color: #4f46e5;
  text-decoration: none;
}

.dash-kpi-link:hover {
  text-decoration: underline;
}

.hub-modules-head {
  margin-bottom: calc(var(--space-sm) * -1);
}

.hub-modules-title {
  margin: 0 0 var(--space-xs);
  font-size: var(--font-size-lg);
  font-weight: 700;
  color: var(--text-primary);
  letter-spacing: -0.02em;
}

.hub-modules-lead {
  margin: 0 0 var(--space-md);
  font-size: var(--font-size-sm);
  color: var(--text-secondary);
  line-height: 1.5;
  max-width: 48rem;
}

.dash-module-grid .hub-card {
  min-height: 0;
  padding: var(--space-md) var(--space-lg) var(--space-lg);
  border-radius: 14px;
  border: 1px solid rgba(226, 232, 240, 0.95);
  transition: box-shadow 0.2s, border-color 0.2s;
}

.dash-module-grid .hub-card:hover {
  border-color: rgba(99, 102, 241, 0.22);
  box-shadow: 0 8px 28px rgba(15, 23, 42, 0.06);
}

.dash-module-grid .hub-card-head {
  margin-bottom: var(--space-md);
}

.dash-board-header {
  margin-bottom: var(--space-xs);
}

.dash-board-eyebrow {
  margin: 0 0 4px;
  font-size: 11px;
  font-weight: 600;
  letter-spacing: 0.12em;
  text-transform: uppercase;
  color: #64748b;
}

.dash-board-title {
  margin: 0;
  font-size: var(--font-size-xl);
  font-weight: 700;
  color: var(--text-primary);
  letter-spacing: -0.02em;
}

.dash-glass-panel {
  border-radius: 14px !important;
  border: 1px solid rgba(226, 232, 240, 0.95) !important;
  background: rgba(255, 255, 255, 0.78) !important;
  backdrop-filter: blur(10px);
  box-shadow: 0 4px 24px rgba(15, 23, 42, 0.04) !important;
}

.dash-modern .dash-glass-panel {
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.92) 0%, rgba(248, 250, 252, 0.88) 100%) !important;
}

.dash-section-title::before {
  background: linear-gradient(180deg, #22d3ee, #6366f1) !important;
}

.dash-telemetry-hint {
  margin: 0;
  font-size: var(--font-size-sm);
  color: var(--text-secondary);
  line-height: 1.55;
  padding: var(--space-sm) 0;
}

.dash-modern .chart-tab-strip button.active {
  background: linear-gradient(135deg, rgba(99, 102, 241, 0.15), rgba(34, 211, 238, 0.12));
  border-color: rgba(99, 102, 241, 0.35);
  color: #4338ca;
}

/* 嵌入协作「开发与数据看板」：飞书式紧凑数据区 */
.console-shell.dashboard-embedded-compact {
  padding: 0;
  background: transparent;
  min-height: 0;
}

.dashboard-embedded-compact .console-inner {
  max-width: none;
  margin: 0;
}

.dashboard-embedded-compact .dashboard {
  gap: var(--space-md);
}

.dashboard-embedded-compact .section {
  padding: var(--space-md) var(--space-lg);
  border-radius: 8px;
  box-shadow: none;
  border: 1px solid var(--border-primary);
  background: var(--bg-card);
}

.dashboard-embedded-compact .section-header {
  margin-bottom: var(--space-xs);
}

.dashboard-embedded-compact .section-header .section-title {
  margin-bottom: 0;
}

.dashboard-embedded-compact .overview-section .section-title {
  margin-bottom: var(--space-sm);
}

.dashboard-embedded-compact .section-title {
  font-size: var(--font-size-base);
  font-weight: var(--font-weight-semibold);
  margin-bottom: var(--space-sm);
  padding-left: var(--space-sm);
}

.dashboard-embedded-compact .section-title::before {
  width: 3px;
  height: 14px;
  border-radius: 2px;
}

.dashboard-embedded-compact .charts-row {
  gap: var(--space-md);
}

.dashboard-embedded-compact .chart-merge-head {
  padding-bottom: var(--space-sm);
  margin-bottom: 0;
}

.dashboard-embedded-compact .chart-tab-strip button {
  padding: 6px 14px;
  font-size: var(--font-size-sm);
  border-radius: 6px;
}

.dashboard-embedded-compact .chart-wrap {
  height: 200px;
}

.dashboard-embedded-compact .chart-wrap-bar {
  height: 280px;
}

.dashboard-embedded-compact .author-rank-toolbar {
  margin-bottom: var(--space-sm);
}

.project-info-cards.project-info-cards-compact {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 1fr);
  gap: var(--space-sm) var(--space-lg);
  align-items: start;
}

.project-info-cards.project-info-cards-compact .project-info-top,
.project-info-cards.project-info-cards-compact .project-info-desc {
  grid-column: 1 / -1;
}

.project-info-cards.project-info-cards-compact .project-info-dev {
  grid-column: 1;
}

.project-info-cards.project-info-cards-compact .project-info-lang {
  grid-column: 2;
}

.project-info-cards.project-info-cards-compact .project-info-label {
  min-width: 72px;
  font-size: var(--font-size-sm);
}

.project-info-cards.project-info-cards-compact .project-info-row {
  font-size: var(--font-size-sm);
}

@media (max-width: 720px) {
  .project-info-cards.project-info-cards-compact .project-info-dev,
  .project-info-cards.project-info-cards-compact .project-info-lang {
    grid-column: 1 / -1;
  }
}

/* --- Elevated Card Base --- */
.console-elevated {
  background: var(--bg-card);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-md);
  border: 1px solid var(--border-primary);
  transition: var(--transition-normal);
}

.console-elevated:hover {
  box-shadow: var(--shadow-lg);
}

/* ============================================================
   Identity Card
   ============================================================ */
.identity-card {
  padding: var(--space-xl) var(--space-xl);
  margin-bottom: var(--space-xl);
}

.identity-main {
  display: flex;
  align-items: flex-start;
  gap: var(--space-lg);
}

.identity-avatar {
  width: 52px;
  height: 52px;
  border-radius: 50%;
  background: var(--brand-gradient);
  color: #fff;
  font-size: var(--font-size-xxl);
  font-weight: var(--font-weight-bold);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  box-shadow: 0 0 0 3px rgba(20, 86, 240, 0.15);
}

.identity-avatar-img {
  width: 100%;
  height: 100%;
  border-radius: 50%;
  object-fit: cover;
  display: block;
}

.identity-title-row {
  display: flex;
  flex-wrap: wrap;
  align-items: baseline;
  gap: var(--space-sm) var(--space-md);
  margin-bottom: var(--space-sm);
}

.identity-name {
  margin: 0;
  font-size: var(--font-size-xxl);
  font-weight: var(--font-weight-bold);
  color: var(--text-primary);
  letter-spacing: -0.02em;
}

.identity-text {
  flex: 1;
  min-width: 0;
}

.identity-edit-subject {
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-medium);
  color: var(--brand-blue);
  text-decoration: none;
  background: transparent;
  border: none;
  padding: 0;
  cursor: pointer;
  transition: var(--transition-fast);
}

.identity-edit-subject:hover {
  color: var(--brand-blue-hover);
  text-decoration: underline;
}

.identity-meta-row {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: var(--space-sm) var(--space-md);
  margin-bottom: var(--space-md);
  font-size: var(--font-size-sm);
}

.identity-id-label {
  color: var(--text-tertiary);
}

.identity-id-value {
  background: var(--bg-hover);
  padding: 2px var(--space-sm);
  border-radius: var(--radius-sm);
  font-size: var(--font-size-xs);
  color: var(--text-secondary);
  font-family: var(--font-mono);
}

.icon-text-btn {
  border: none;
  background: transparent;
  color: var(--brand-blue);
  font-size: var(--font-size-sm);
  cursor: pointer;
  padding: 0;
  transition: var(--transition-fast);
}

.icon-text-btn:hover {
  color: var(--brand-blue-hover);
  text-decoration: underline;
}

.icon-text-btn:disabled {
  color: var(--text-disabled);
  cursor: not-allowed;
  text-decoration: none;
}

.copy-toast {
  font-size: var(--font-size-xs);
  color: var(--success);
}

.identity-tags {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-sm);
}

.identity-tag {
  font-size: var(--font-size-xs);
  padding: var(--space-xs) var(--space-md);
  border-radius: var(--radius-full);
  background: var(--brand-blue-light);
  color: var(--brand-blue);
  font-weight: var(--font-weight-medium);
  white-space: nowrap;
}

.identity-tag-muted {
  background: var(--bg-hover);
  color: var(--text-secondary);
}

.identity-tag-success {
  background: var(--success-bg);
  color: var(--success);
  border: 1px solid rgba(52, 199, 89, 0.25);
}

.identity-tag-warn {
  background: var(--warning-bg);
  color: #92400e;
  border: 1px solid rgba(245, 158, 11, 0.25);
}

/* ============================================================
   Subject Edit Modal
   ============================================================ */
.subject-modal-mask {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.45);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: var(--z-modal);
  padding: var(--space-xl);
  backdrop-filter: blur(2px);
}

.subject-modal-card {
  width: min(980px, 100%);
  max-height: 90vh;
  overflow: auto;
  background: var(--bg-card);
  border-radius: var(--radius-lg);
  padding: var(--space-xl);
  box-shadow: var(--shadow-lg);
  border: 1px solid var(--border-primary);
}

.subject-modal-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-md);
  margin-bottom: var(--space-lg);
}

.subject-modal-title {
  margin: 0;
  font-size: var(--font-size-xl);
  font-weight: var(--font-weight-semibold);
  color: var(--text-primary);
}

.subject-modal-close {
  width: 34px;
  height: 34px;
  border-radius: var(--radius-sm);
  border: 1px solid var(--border-primary);
  background: var(--bg-hover);
  color: var(--text-secondary);
  cursor: pointer;
  font-size: var(--font-size-xl);
  line-height: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: var(--transition-fast);
}

.subject-modal-close:hover {
  background: var(--border-primary);
  color: var(--text-primary);
}

.subject-logo-row {
  display: flex;
  align-items: center;
  gap: var(--space-md);
  flex-wrap: wrap;
  margin-bottom: var(--space-lg);
}

.subject-logo-preview {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 40px;
  max-width: 180px;
}

.subject-logo-preview img {
  max-height: 56px;
  max-width: 180px;
  object-fit: contain;
  border-radius: var(--radius-sm);
}

.subject-hidden-input {
  display: none;
}

.subject-edit-section {
  border: 1px solid var(--border-primary);
  border-radius: var(--radius-md);
  padding: var(--space-lg);
  margin-bottom: var(--space-lg);
}

.subject-section-title {
  margin: 0 0 var(--space-md);
  font-size: var(--font-size-base);
  font-weight: var(--font-weight-semibold);
  color: var(--text-primary);
}

.subject-section-hint {
  margin: calc(var(--space-sm) * -1) 0 var(--space-md);
  font-size: var(--font-size-sm);
  color: var(--text-tertiary);
}

.subject-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: var(--space-md);
}

.subject-full {
  grid-column: 1 / -1;
}

.subject-label {
  font-size: var(--font-size-sm);
  color: var(--text-secondary);
  display: block;
}

.subject-input {
  width: 100%;
  margin-top: var(--space-xs);
  padding: var(--space-sm) var(--space-md);
  border: 1px solid var(--border-input);
  border-radius: var(--radius-sm);
  font-size: var(--font-size-base);
  box-sizing: border-box;
  background: var(--bg-input);
  color: var(--text-primary);
  transition: var(--transition-fast);
}

.subject-input:hover {
  border-color: var(--border-input-hover);
}

.subject-input:focus {
  outline: none;
  border-color: var(--border-input-focus);
  box-shadow: 0 0 0 2px rgba(20, 86, 240, 0.15);
}

.subject-section-actions {
  margin-top: var(--space-lg);
  display: flex;
  align-items: center;
  gap: var(--space-md);
  flex-wrap: wrap;
}

.subject-msg-ok {
  color: var(--success);
  font-size: var(--font-size-sm);
}

.subject-msg-err {
  color: var(--danger);
  font-size: var(--font-size-sm);
}

/* ============================================================
   Webhook URL Modal
   ============================================================ */
.webhook-modal-mask {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.45);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: var(--z-modal);
  padding: var(--space-xl);
  backdrop-filter: blur(2px);
}

.webhook-modal-card {
  width: min(860px, 100%);
  background: var(--bg-card);
  border-radius: var(--radius-lg);
  padding: var(--space-xl);
  box-shadow: var(--shadow-lg);
  border: 1px solid var(--border-primary);
}

.webhook-modal-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-md);
  margin-bottom: var(--space-lg);
}

.webhook-modal-title {
  margin: 0;
  font-size: var(--font-size-xl);
  font-weight: var(--font-weight-semibold);
  color: var(--text-primary);
}

.webhook-modal-close {
  width: 34px;
  height: 34px;
  border-radius: var(--radius-sm);
  border: 1px solid var(--border-primary);
  background: var(--bg-hover);
  color: var(--text-secondary);
  cursor: pointer;
  font-size: var(--font-size-xl);
  line-height: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: var(--transition-fast);
}

.webhook-modal-close:hover {
  background: var(--border-primary);
  color: var(--text-primary);
}

.webhook-modal-desc {
  margin: 0 0 var(--space-md);
  font-size: var(--font-size-sm);
  color: var(--text-tertiary);
}

.webhook-url-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-md);
  flex-wrap: wrap;
  padding: var(--space-md);
  border: 1px solid var(--border-primary);
  border-radius: var(--radius-md);
  background: var(--bg-hover);
}

.webhook-url-code {
  flex: 1;
  min-width: 420px;
  word-break: break-all;
  font-size: var(--font-size-xs);
  font-family: var(--font-mono);
}

.webhook-copy-toast {
  display: inline-block;
  margin-top: var(--space-md);
  color: var(--brand-blue);
  font-size: var(--font-size-sm);
}

/* ============================================================
   Team Picker Modal
   ============================================================ */
.team-picker-modal-mask {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.45);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: calc(var(--z-modal) + 100);
  padding: var(--space-xl);
  backdrop-filter: blur(2px);
}

.team-picker-modal-card {
  width: min(920px, 100%);
  max-height: 90vh;
  overflow: auto;
  background: var(--bg-card);
  border-radius: var(--radius-lg);
  padding: var(--space-xl);
  box-shadow: var(--shadow-lg);
  border: 1px solid var(--border-primary);
}

.team-picker-modal-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-md);
  margin-bottom: var(--space-lg);
}

.team-picker-modal-title {
  margin: 0;
  font-size: var(--font-size-xl);
  font-weight: var(--font-weight-semibold);
  color: var(--text-primary);
}

.team-picker-toolbar {
  margin-bottom: var(--space-md);
}

.team-picker-select-all {
  display: inline-flex;
  align-items: center;
  gap: var(--space-sm);
  font-size: var(--font-size-sm);
  color: var(--text-secondary);
  font-weight: var(--font-weight-medium);
}

.team-picker-list {
  border: 1px solid var(--border-primary);
  border-radius: var(--radius-md);
  padding: var(--space-md);
  background: var(--bg-hover);
}

.team-picker-row {
  margin-bottom: var(--space-md);
}

.team-picker-row:last-child {
  margin-bottom: 0;
}

.team-picker-check {
  display: flex;
  align-items: center;
  gap: var(--space-sm);
  cursor: pointer;
}

.team-picker-check input[type="checkbox"] {
  transform: translateY(1px);
  accent-color: var(--brand-blue);
}

.team-picker-actions {
  margin-top: var(--space-lg);
  display: flex;
  justify-content: flex-end;
  gap: var(--space-md);
}

.team-picker-name {
  font-size: var(--font-size-sm);
  color: var(--text-primary);
}

/* ============================================================
   Activity Dots
   ============================================================ */
.activity-dot {
  width: 8px;
  height: 8px;
  border-radius: var(--radius-full);
  display: inline-block;
  flex-shrink: 0;
}

.activity-dot.dot-active {
  background: var(--success);
  box-shadow: 0 0 0 2px rgba(52, 199, 89, 0.25);
}

.activity-dot.dot-inactive {
  background: var(--text-disabled);
}

/* ============================================================
   Hub Grid & Cards
   ============================================================ */
.hub-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: var(--space-lg);
  margin-bottom: var(--space-sm);
}

@media (max-width: 1100px) {
  .hub-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 640px) {
  .hub-grid {
    grid-template-columns: 1fr;
  }
}

.hub-card {
  padding: var(--space-lg) var(--space-xl) var(--space-xl);
  min-height: 200px;
  display: flex;
  flex-direction: column;
}

.hub-card-placeholder {
  opacity: 0.92;
}

.hub-card-placeholder .hub-desc {
  flex: 1;
}

/* Hub card type modifiers (used for conditional styling) */
.hub-quote {}
.hub-api {}
.hub-team {}
.hub-project {}
.hub-ops {}
.hub-market {}
.hub-prototype {}
.hub-chance {}

.hub-soon {
  font-size: var(--font-size-xs);
  font-weight: var(--font-weight-medium);
  color: var(--text-disabled);
  margin-top: auto;
  padding-top: var(--space-sm);
}

.hub-card-head {
  display: flex;
  align-items: center;
  gap: var(--space-sm);
  margin-bottom: var(--space-lg);
}

.hub-icon {
  width: 28px;
  height: 28px;
  border-radius: var(--radius-sm);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: var(--font-size-sm);
  color: #fff;
  flex-shrink: 0;
}

.hub-icon-quote { background: linear-gradient(135deg, var(--brand-blue), var(--brand-purple)); }
.hub-icon-api { background: linear-gradient(135deg, #0891b2, var(--brand-blue)); }
.hub-icon-team { background: linear-gradient(135deg, #059669, #10b981); }
.hub-icon-project { background: linear-gradient(135deg, var(--brand-purple), #a855f7); }
.hub-icon-ops { background: linear-gradient(135deg, #475569, #64748b); }
.hub-icon-clouddev { background: linear-gradient(135deg, #0ea5e9, #6366f1); }
.hub-icon-market { background: linear-gradient(135deg, #ea580c, #f97316); }
.hub-icon-prototype { background: linear-gradient(135deg, var(--brand-blue), #22c55e); }
.hub-icon-chance { background: linear-gradient(135deg, #0ea5e9, #6366f1); }

.hub-card-title {
  margin: 0;
  flex: 1;
  font-size: var(--font-size-lg);
  font-weight: var(--font-weight-semibold);
  color: var(--text-primary);
}

.hub-card-more {
  font-size: var(--font-size-sm);
  color: var(--brand-blue);
  text-decoration: none;
  white-space: nowrap;
  transition: var(--transition-fast);
}

.hub-card-more:hover {
  color: var(--brand-blue-hover);
  text-decoration: underline;
}

/* --- Hub Metrics --- */
.hub-metric-row {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-end;
  gap: var(--space-md) var(--space-xl);
  margin-bottom: var(--space-md);
}

.hub-metric {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.hub-metric-value {
  font-size: var(--font-size-xxxl);
  font-weight: var(--font-weight-bold);
  color: var(--text-primary);
  line-height: 1.1;
  font-variant-numeric: tabular-nums;
}

.hub-metric-label {
  font-size: var(--font-size-xs);
  color: var(--text-tertiary);
}

/* --- Hub Quick Actions & Pills --- */
.hub-quick-actions {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-sm);
  align-items: center;
}

.hub-quick-actions-foot {
  margin-top: auto;
  padding-top: var(--space-lg);
}

.hub-pill {
  display: inline-block;
  padding: var(--space-sm) var(--space-md);
  border-radius: var(--radius-full);
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-medium);
  text-decoration: none;
  background: var(--bg-hover);
  color: var(--text-primary);
  border: 1px solid var(--border-primary);
  transition: var(--transition-fast);
}

.hub-pill:hover {
  background: var(--border-primary);
  border-color: var(--border-input-hover);
}

.hub-pill-primary {
  background: var(--brand-blue);
  color: #fff;
  border-color: var(--brand-blue);
  box-shadow: var(--shadow-btn);
}

.hub-pill-primary:hover {
  background: var(--brand-blue-hover);
  border-color: var(--brand-blue-hover);
}

/* --- Hub Sub-head, List, Filter --- */
.hub-subhead {
  font-size: var(--font-size-xs);
  font-weight: var(--font-weight-medium);
  color: var(--text-tertiary);
  margin-bottom: var(--space-sm);
}

.hub-subhead-filter {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: var(--space-sm) var(--space-md);
  justify-content: space-between;
}

.hub-list-filter {
  flex: 1;
  min-width: 120px;
  max-width: 220px;
  padding: var(--space-sm) var(--space-md);
  font-size: var(--font-size-sm);
  border: 1px solid var(--border-primary);
  border-radius: var(--radius-sm);
  background: var(--bg-input);
  color: var(--text-primary);
  box-sizing: border-box;
  transition: var(--transition-fast);
}

.hub-list-filter::placeholder {
  color: var(--text-disabled);
}

.hub-list-filter:focus {
  outline: none;
  border-color: var(--border-input-focus);
  box-shadow: 0 0 0 2px rgba(20, 86, 240, 0.15);
}

.hub-list {
  list-style: none;
  margin: 0;
  padding: 0;
  border-top: 1px solid var(--border-primary);
}

.hub-list li {
  border-bottom: 1px solid var(--bg-hover);
}

.hub-list li:last-child {
  border-bottom: none;
}

.hub-list-link {
  display: flex;
  flex-direction: column;
  gap: 2px;
  padding: var(--space-md) 0;
  text-decoration: none;
  color: inherit;
  transition: var(--transition-fast);
}

.hub-list-link:hover .hub-list-name {
  color: var(--brand-blue);
}

.hub-list-name {
  font-size: var(--font-size-base);
  font-weight: var(--font-weight-medium);
  color: var(--text-primary);
}

.hub-list-meta {
  font-size: var(--font-size-xs);
  color: var(--text-tertiary);
}

.hub-placeholder {
  font-size: var(--font-size-sm);
  color: var(--text-tertiary);
}

.hub-placeholder.muted {
  margin: 0;
  padding: var(--space-sm) 0 0;
}

/* --- Hub Provider Rows (API Hub) --- */
.hub-provider-rows {
  display: flex;
  flex-direction: column;
  gap: var(--space-sm);
  flex: 1;
}

.hub-provider-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: var(--space-md);
  padding: var(--space-md);
  background: var(--bg-hover);
  border-radius: var(--radius-md);
  border: 1px solid var(--border-primary);
}

.hub-provider-name {
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-medium);
  color: var(--text-secondary);
}

.hub-provider-status {
  font-size: var(--font-size-xs);
  color: var(--text-tertiary);
  text-align: right;
}

.hub-provider-status.ok {
  color: var(--success);
  font-weight: var(--font-weight-medium);
}

.hub-desc {
  font-size: var(--font-size-sm);
  color: var(--text-tertiary);
  line-height: var(--line-height-normal);
  margin: 0 0 var(--space-md);
}

.hub-block-link {
  margin-top: auto;
  font-size: var(--font-size-base);
  font-weight: var(--font-weight-medium);
  color: var(--brand-blue);
  text-decoration: none;
}

.hub-block-link:hover {
  text-decoration: underline;
}

.hub-block-link-sub {
  margin-top: var(--space-sm);
  font-weight: var(--font-weight-normal);
  font-size: var(--font-size-sm);
  color: var(--text-tertiary);
}

.hub-repo-filter {
  margin-bottom: var(--space-lg);
  display: flex;
  align-items: center;
  gap: var(--space-sm);
  flex-wrap: wrap;
}

.hub-repo-filter label {
  margin-right: 0;
}

/* --- Hub Mini Overview (Project Hub) --- */
.hub-mini-overview {
  margin-top: var(--space-xs);
}

.hub-subhead-text {
  display: block;
  font-size: var(--font-size-xs);
  font-weight: var(--font-weight-medium);
  color: var(--text-disabled);
  text-transform: uppercase;
  letter-spacing: 0.04em;
  margin-bottom: var(--space-md);
}

.hub-mini-metrics {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: var(--space-sm);
}

.hub-mini-metric {
  background: var(--bg-hover);
  border-radius: var(--radius-md);
  padding: var(--space-md) var(--space-sm);
  text-align: center;
  border: 1px solid var(--border-primary);
}

.hub-mini-val {
  display: block;
  font-size: var(--font-size-xxl);
  font-weight: var(--font-weight-bold);
  color: var(--text-primary);
}

.hub-mini-lbl {
  font-size: var(--font-size-xs);
  color: var(--text-tertiary);
  margin-top: var(--space-xs);
  display: block;
}

/* ============================================================
   Lab Mini Grid (Market / Lab Hub)
   ============================================================ */
.lab-mini-grid {
  margin-top: var(--space-md);
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: var(--space-sm) var(--space-md);
}

.lab-mini-card {
  border-radius: var(--radius-md);
  border: 1px solid var(--border-primary);
  background: var(--bg-hover);
  padding: var(--space-md);
  display: flex;
  flex-direction: column;
  gap: var(--space-xs);
}

.lab-mini-title {
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-semibold);
  color: var(--text-primary);
}

.lab-mini-tag {
  align-self: flex-start;
  font-size: var(--font-size-xs);
  font-weight: var(--font-weight-medium);
  padding: 2px var(--space-sm);
  border-radius: var(--radius-full);
  background: var(--brand-blue-light);
  color: var(--brand-blue);
}

@media (max-width: 640px) {
  .lab-mini-grid {
    grid-template-columns: 1fr;
  }
}

/* ============================================================
   Team Hub Members
   ============================================================ */
.team-activity-legend {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: var(--space-sm) var(--space-lg);
  margin-bottom: 0;
  margin-top: 0;
}

.team-activity-legend .legend-item {
  display: inline-flex;
  align-items: center;
  gap: var(--space-sm);
  font-size: var(--font-size-xs);
  color: var(--text-secondary);
  font-weight: var(--font-weight-medium);
}

.team-hub-toolbar {
  margin-bottom: var(--space-md);
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-md);
}

.team-hub-members {
  margin-top: var(--space-xs);
}

.team-hub-member-list {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: var(--space-sm) var(--space-md);
}

@media (max-width: 520px) {
  .team-hub-member-list {
    grid-template-columns: 1fr;
  }
}

.team-hub-member {
  display: flex;
  align-items: flex-start;
  gap: var(--space-sm);
}

.member-avatar-wrap {
  width: 34px;
  height: 34px;
  border-radius: var(--radius-sm);
  background: var(--bg-hover);
  border: 1px solid var(--border-primary);
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  flex-shrink: 0;
}

.member-avatar {
  width: 34px;
  height: 34px;
  object-fit: cover;
  display: block;
}

.member-avatar-placeholder {
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-bold);
  color: var(--text-secondary);
}

.member-avatar-xs {
  width: 24px;
  height: 24px;
  border-radius: var(--radius-xs);
  object-fit: cover;
}

.member-meta {
  min-width: 0;
}

.member-name {
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-semibold);
  color: var(--text-primary);
  display: flex;
  align-items: baseline;
  gap: var(--space-sm);
  flex-wrap: wrap;
}

.member-subline {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: var(--space-sm);
  margin-top: var(--space-xs);
}

.member-role-tag {
  font-size: var(--font-size-xs);
  font-weight: var(--font-weight-medium);
  padding: 2px var(--space-sm);
  border-radius: var(--radius-full);
  line-height: 16px;
  border: 1px solid transparent;
  white-space: nowrap;
}

.member-role-tag.tag-super-admin {
  background: var(--danger-bg);
  color: #991b1b;
  border-color: rgba(245, 74, 69, 0.2);
}

.member-role-tag.tag-sub-admin {
  background: var(--brand-blue-light);
  color: var(--brand-blue);
  border-color: rgba(20, 86, 240, 0.15);
}

.member-role-tag.tag-member {
  background: var(--success-bg);
  color: #166534;
  border-color: rgba(52, 199, 89, 0.2);
}

.member-role-tag.tag-other {
  background: var(--bg-hover);
  color: var(--text-secondary);
  border-color: var(--border-primary);
}

.member-job-title-inline {
  font-size: var(--font-size-xs);
  color: var(--text-tertiary);
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.team-hub-pagination {
  margin-top: var(--space-md);
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-md);
}

.team-hub-pagination .page-info {
  font-size: var(--font-size-sm);
  color: var(--text-secondary);
  font-weight: var(--font-weight-medium);
}

/* ============================================================
   Console Board Title & Hint
   ============================================================ */
.console-board-hint {
  font-size: var(--font-size-sm);
  color: var(--text-tertiary);
  margin: var(--space-xxl) 0 var(--space-sm);
}

.console-board-title {
  margin: 0 0 var(--space-lg);
  font-size: var(--font-size-xl);
  font-weight: var(--font-weight-semibold);
  color: var(--text-primary);
}

/* ============================================================
   Section (Data Board)
   ============================================================ */
.section {
  background-color: var(--bg-card);
  border-radius: var(--radius-lg);
  padding: var(--space-lg) var(--space-xl) var(--space-xl);
  box-shadow: var(--shadow-sm);
  border: 1px solid var(--border-primary);
}

.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: var(--space-sm);
  flex-wrap: wrap;
  gap: var(--space-sm);
}

.section-title {
  margin: 0 0 var(--space-md);
  font-size: var(--font-size-lg);
  font-weight: var(--font-weight-semibold);
  color: var(--text-primary);
  display: flex;
  align-items: center;
  padding-left: var(--space-md);
  position: relative;
  line-height: var(--line-height-normal);
}

.section-title::before {
  content: "";
  position: absolute;
  left: 0;
  top: 50%;
  transform: translateY(-50%);
  width: 3px;
  height: 18px;
  background-color: var(--brand-blue);
  border-radius: var(--radius-full);
}

.section-header-bar {
  padding-top: var(--space-sm);
  padding-bottom: var(--space-sm);
}

.header-actions {
  display: flex;
  align-items: center;
  gap: var(--space-md);
}

.collab-entry {
  color: var(--brand-blue);
  text-decoration: none;
  font-size: var(--font-size-base);
}

.collab-entry:hover {
  text-decoration: underline;
}

/* ============================================================
   Overview Cards
   ============================================================ */
.overview-section .section-title {
  margin-bottom: var(--space-md);
}

.overview-cards {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: var(--space-lg);
}

@media (max-width: 900px) {
  .overview-cards {
    grid-template-columns: 1fr;
  }
}

.overview-card {
  background: var(--brand-blue-light);
  border-radius: var(--radius-md);
  padding: var(--space-xl);
  text-align: center;
  border: 1px solid rgba(20, 86, 240, 0.12);
  transition: var(--transition-normal);
}

.overview-card:nth-child(2) {
  background: var(--success-bg);
  border-color: rgba(52, 199, 89, 0.15);
}

.overview-card:nth-child(3) {
  background: rgba(124, 58, 237, 0.06);
  border-color: rgba(124, 58, 237, 0.12);
}

.overview-card:hover {
  box-shadow: var(--shadow-md);
  transform: translateY(-1px);
}

.overview-value {
  font-size: var(--font-size-xxxl);
  font-weight: var(--font-weight-bold);
  color: var(--text-primary);
  line-height: 1.2;
}

.overview-label {
  font-size: var(--font-size-sm);
  color: var(--text-tertiary);
  margin-top: var(--space-xs);
}

/* ============================================================
   Project Info (selected repo)
   ============================================================ */
.project-info-cards {
  display: flex;
  flex-direction: column;
  gap: var(--space-sm);
}

.project-info-row {
  display: flex;
  align-items: flex-start;
  gap: var(--space-sm);
  font-size: var(--font-size-base);
}

.project-info-label {
  flex-shrink: 0;
  color: var(--text-tertiary);
  min-width: 80px;
}

.project-info-name {
  font-weight: var(--font-weight-medium);
  color: var(--brand-blue);
  text-decoration: none;
}

.project-info-name:hover {
  text-decoration: underline;
}

.project-info-desc {
  color: var(--text-secondary);
  line-height: var(--line-height-normal);
}

.project-info-developers,
.project-info-languages {
  color: var(--text-secondary);
}

/* ============================================================
   Charts
   ============================================================ */
.charts-row {
  display: grid;
  grid-template-columns: 1fr;
  gap: var(--space-xl);
}

.chart-section {
  min-height: 260px;
}

.chart-section-merged {
  min-height: 0;
}

.chart-section-full {
  grid-column: 1 / -1;
}

.chart-section-wide {
  grid-column: 1 / -1;
}

.chart-merge-head {
  margin-bottom: var(--space-xs);
  padding-bottom: var(--space-md);
  border-bottom: 1px solid var(--border-primary);
}

.chart-tab-strip {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-sm);
  align-items: center;
}

.chart-tab-strip button {
  padding: var(--space-sm) var(--space-lg);
  font-size: var(--font-size-base);
  font-weight: var(--font-weight-medium);
  border-radius: var(--radius-sm);
  border: 1px solid var(--border-primary);
  background: var(--bg-hover);
  color: var(--text-secondary);
  cursor: pointer;
  transition: var(--transition-fast);
}

.chart-tab-strip button:hover {
  background: var(--border-primary);
  color: var(--text-primary);
}

.chart-tab-strip button.active {
  background: var(--brand-blue);
  color: #fff;
  border-color: var(--brand-blue);
}

.chart-panel {
  /* base panel style, panels are shown/hidden via v-if */
}

.chart-panel-author {
  /* author ranking panel */
}

.chart-panel-trend .chart-wrap {
  margin-top: 0;
}

.chart-wrap {
  height: 220px;
  position: relative;
}

.chart-wrap-pie {
  height: 240px;
}

.chart-section-full .chart-wrap {
  height: 260px;
}

.chart-wrap-bar {
  height: 360px;
}

.chart-legend {
  margin-top: var(--space-sm);
  display: flex;
  gap: var(--space-sm);
}

.chart-legend button {
  padding: var(--space-xs) var(--space-sm);
  font-size: var(--font-size-xs);
  border-radius: var(--radius-xs);
  border: 1px solid var(--border-primary);
  background: var(--bg-hover);
  color: var(--text-secondary);
  cursor: pointer;
  transition: var(--transition-fast);
}

.chart-legend button:hover {
  background: var(--border-primary);
}

.chart-legend button.active {
  background: var(--brand-blue);
  color: #fff;
  border-color: var(--brand-blue);
}

.author-rank-header {
  margin-bottom: var(--space-xs);
}

.author-rank-toolbar {
  margin-bottom: var(--space-md);
  display: flex;
  flex-direction: column;
  gap: var(--space-sm);
}

.author-rank-modes {
  flex-wrap: wrap;
}

.author-rank-nav {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: var(--space-md) var(--space-lg);
  font-size: var(--font-size-sm);
}

.author-rank-range {
  color: var(--text-secondary);
  font-weight: var(--font-weight-medium);
}

.author-rank-range.muted {
  color: var(--text-tertiary);
  font-weight: var(--font-weight-normal);
}

/* ============================================================
   Tables
   ============================================================ */
.table-wrapper {
  width: 100%;
  overflow-x: auto;
}

.table {
  width: 100%;
  border-collapse: collapse;
  font-size: var(--font-size-sm);
}

.table th,
.table td {
  padding: var(--space-sm) var(--space-md);
  border-bottom: 1px solid var(--border-primary);
  text-align: left;
}

.table th {
  background-color: var(--bg-hover);
  font-weight: var(--font-weight-medium);
  color: var(--text-secondary);
  white-space: nowrap;
}

.table tbody tr {
  transition: var(--transition-fast);
}

.table tbody tr:hover {
  background-color: var(--bg-hover);
}

.table tbody tr:nth-child(even) {
  background-color: rgba(245, 246, 247, 0.5);
}

.table tbody tr:nth-child(even):hover {
  background-color: var(--bg-hover);
}

.table .num {
  text-align: right;
  font-variant-numeric: tabular-nums;
}

.table .num.add { color: var(--success); }
.table .num.del { color: var(--danger); }

/* ============================================================
   Commits Section
   ============================================================ */
.commits-section {
  /* extends .section */
}

.commits-toolbar {
  display: flex;
  align-items: center;
  gap: var(--space-md);
  flex-wrap: wrap;
}

.page-size-label {
  display: inline-flex;
  align-items: center;
  gap: var(--space-xs);
  font-size: var(--font-size-sm);
  color: var(--text-secondary);
}

.page-size-select {
  padding: var(--space-xs) var(--space-sm);
  border-radius: var(--radius-sm);
  border: 1px solid var(--border-primary);
  font-size: var(--font-size-sm);
  background: var(--bg-input);
  color: var(--text-primary);
  transition: var(--transition-fast);
}

.page-size-select:focus {
  outline: none;
  border-color: var(--border-input-focus);
  box-shadow: 0 0 0 2px rgba(20, 86, 240, 0.15);
}

.commits-table-scroll {
  max-height: min(52vh, 520px);
  overflow: auto;
}

.commits-pagination {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: var(--space-sm);
  margin-top: var(--space-md);
  padding-top: var(--space-md);
  border-top: 1px solid var(--border-primary);
  font-size: var(--font-size-sm);
  color: var(--text-tertiary);
}

.page-buttons {
  display: flex;
  gap: var(--space-sm);
}

.page-btn:disabled {
  opacity: 0.45;
  cursor: not-allowed;
}

/* ============================================================
   Repo Filter
   ============================================================ */
.repo-filter {
  display: flex;
  align-items: center;
  gap: var(--space-sm);
  font-size: var(--font-size-sm);
}

.repo-filter select {
  padding: var(--space-xs) var(--space-sm);
  border-radius: var(--radius-xs);
  border: 1px solid var(--border-primary);
  font-size: var(--font-size-sm);
  background: var(--bg-input);
  color: var(--text-primary);
}

/* ============================================================
   Summary Grid
   ============================================================ */
.summary-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: var(--space-md);
}

.summary-card {
  border-radius: var(--radius-sm);
  padding: var(--space-md);
  background-color: var(--bg-hover);
}

.summary-card .label {
  font-size: var(--font-size-xs);
  color: var(--text-tertiary);
}

.summary-card .value {
  margin-top: var(--space-xs);
  font-size: var(--font-size-xl);
  font-weight: var(--font-weight-semibold);
  color: var(--text-primary);
}

/* ============================================================
   Common: Mono, Link Button, Placeholder
   ============================================================ */
.mono {
  font-family: var(--font-mono);
}

.link-button {
  border: none;
  background: transparent;
  color: var(--brand-blue);
  cursor: pointer;
  padding: 0;
  font-size: var(--font-size-sm);
  transition: var(--transition-fast);
}

.link-button:hover {
  color: var(--brand-blue-hover);
  text-decoration: underline;
}

.link-button:disabled {
  color: var(--text-disabled);
  cursor: not-allowed;
  text-decoration: none;
}

.link-button.subtle {
  color: var(--text-tertiary);
  font-size: var(--font-size-xs);
}

.link-button.primary-action {
  font-weight: var(--font-weight-medium);
}

.commit-analysis-link {
  margin-left: var(--space-sm);
}

.placeholder {
  font-size: var(--font-size-sm);
  color: var(--text-tertiary);
}

/* ============================================================
   Primary & Secondary Buttons
   ============================================================ */
.primary-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: var(--space-sm);
  padding: var(--space-sm) var(--space-lg);
  font-size: var(--font-size-base);
  font-weight: var(--font-weight-medium);
  line-height: var(--line-height-normal);
  border-radius: var(--radius-sm);
  border: 1px solid transparent;
  cursor: pointer;
  user-select: none;
  white-space: nowrap;
  background-color: var(--brand-blue);
  color: #FFFFFF;
  border-color: var(--brand-blue);
  box-shadow: var(--shadow-btn);
  transition: var(--transition-fast);
}

.primary-button:hover {
  background-color: var(--brand-blue-hover);
  border-color: var(--brand-blue-hover);
}

.primary-button:active {
  background-color: var(--brand-blue-active);
  border-color: var(--brand-blue-active);
}

.primary-button:disabled {
  opacity: 0.45;
  cursor: not-allowed;
  pointer-events: none;
}

.primary-button:focus-visible {
  box-shadow: 0 0 0 2px rgba(20, 86, 240, 0.25);
}

.primary-button.small {
  padding: var(--space-xs) var(--space-md);
  font-size: var(--font-size-sm);
  border-radius: var(--radius-xs);
}

.secondary-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: var(--space-sm);
  padding: var(--space-sm) var(--space-lg);
  font-size: var(--font-size-base);
  font-weight: var(--font-weight-medium);
  line-height: var(--line-height-normal);
  border-radius: var(--radius-sm);
  border: 1px solid var(--border-primary);
  cursor: pointer;
  user-select: none;
  white-space: nowrap;
  background-color: var(--bg-card);
  color: var(--text-primary);
  transition: var(--transition-fast);
}

.secondary-button:hover {
  background-color: var(--bg-hover);
  border-color: var(--border-input-hover);
}

.secondary-button:active {
  background-color: var(--border-primary);
}

.secondary-button:disabled {
  opacity: 0.45;
  cursor: not-allowed;
  pointer-events: none;
}

.secondary-button:focus-visible {
  box-shadow: 0 0 0 2px rgba(20, 86, 240, 0.25);
}

/* ============================================================
   Daily Summary
   ============================================================ */
.daily-summary-desc {
  font-size: var(--font-size-sm);
  color: var(--text-tertiary);
  margin: 0 0 var(--space-md);
}

.daily-summary-toolbar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: var(--space-md) var(--space-lg);
}

.daily-summary-feedback {
  font-size: var(--font-size-sm);
  margin: 0 0 var(--space-md);
}

.daily-summary-feedback.ok {
  color: var(--success);
}

.daily-summary-feedback.err {
  color: var(--danger);
}

.daily-summary-history-head {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-sm) var(--space-lg);
  margin-bottom: var(--space-md);
}

.daily-summary-subtitle {
  margin: 0;
  font-size: var(--font-size-base);
  font-weight: var(--font-weight-medium);
  color: var(--text-secondary);
}

.daily-summary-pagination {
  margin-top: var(--space-md);
}

.daily-summary-list {
  list-style: none;
  padding: 0;
  margin: 0;
}

.daily-summary-item {
  display: flex;
  flex-wrap: wrap;
  align-items: baseline;
  gap: var(--space-sm) var(--space-lg);
  padding: var(--space-md);
  margin-bottom: var(--space-sm);
  border: 1px solid var(--border-primary);
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: var(--transition-fast);
}

.daily-summary-item:hover {
  background: var(--bg-hover);
  border-color: var(--border-input-hover);
}

.ds-date {
  font-family: var(--font-mono);
  font-size: var(--font-size-sm);
  color: var(--text-secondary);
  flex-shrink: 0;
}

.ds-title {
  flex: 1;
  min-width: 160px;
  font-size: var(--font-size-base);
  color: var(--text-primary);
  font-weight: var(--font-weight-medium);
}

.ds-meta {
  font-size: var(--font-size-xs);
  color: var(--text-disabled);
}

.daily-summary-modal.diff-modal {
  max-width: 920px;
  width: 100%;
}

.ds-detail-meta {
  font-size: var(--font-size-xs);
  color: var(--text-tertiary);
  margin: 0 0 var(--space-md);
}

.daily-summary-body {
  margin: 0;
  word-break: break-word;
  font-size: var(--font-size-base);
  line-height: var(--line-height-relaxed);
  color: var(--text-primary);
  max-height: min(72vh, 640px);
  overflow: auto;
  padding: var(--space-lg);
  background: var(--bg-hover);
  border-radius: var(--radius-md);
  border: 1px solid var(--border-primary);
}

/* Markdown body deep selectors */
.daily-summary-body.markdown-body >>> .markdown-empty {
  margin: 0;
  color: var(--text-disabled);
}

.daily-summary-body.markdown-body >>> .markdown-fallback {
  margin: 0;
  white-space: pre-wrap;
  font-family: var(--font-mono);
  font-size: var(--font-size-sm);
}

.daily-summary-body.markdown-body >>> h1,
.daily-summary-body.markdown-body >>> h2,
.daily-summary-body.markdown-body >>> h3,
.daily-summary-body.markdown-body >>> h4 {
  margin: 1.1em 0 0.5em;
  font-weight: var(--font-weight-semibold);
  color: var(--text-primary);
  line-height: 1.35;
}

.daily-summary-body.markdown-body >>> h1:first-child,
.daily-summary-body.markdown-body >>> h2:first-child,
.daily-summary-body.markdown-body >>> h3:first-child,
.daily-summary-body.markdown-body >>> h4:first-child {
  margin-top: 0;
}

.daily-summary-body.markdown-body >>> h1 { font-size: 1.35rem; }
.daily-summary-body.markdown-body >>> h2 { font-size: 1.2rem; }
.daily-summary-body.markdown-body >>> h3 { font-size: 1.1rem; }
.daily-summary-body.markdown-body >>> h4 { font-size: 1rem; }

.daily-summary-body.markdown-body >>> p {
  margin: 0.55em 0;
  color: var(--text-primary);
}

.daily-summary-body.markdown-body >>> ul,
.daily-summary-body.markdown-body >>> ol {
  margin: 0.5em 0;
  padding-left: 1.35em;
}

.daily-summary-body.markdown-body >>> li {
  margin: 0.25em 0;
}

.daily-summary-body.markdown-body >>> li > p {
  margin: 0.2em 0;
}

.daily-summary-body.markdown-body >>> hr {
  border: none;
  border-top: 1px solid var(--border-primary);
  margin: 1.25em 0;
}

.daily-summary-body.markdown-body >>> blockquote {
  margin: 0.75em 0;
  padding: 0.35em 0 0.35em var(--space-md);
  border-left: 3px solid var(--border-primary);
  color: var(--text-secondary);
  background: rgba(255, 255, 255, 0.6);
  border-radius: 0 var(--radius-sm) var(--radius-sm) 0;
}

.daily-summary-body.markdown-body >>> a {
  color: var(--brand-blue);
  text-decoration: none;
}

.daily-summary-body.markdown-body >>> a:hover {
  text-decoration: underline;
}

.daily-summary-body.markdown-body >>> code {
  font-family: var(--font-mono);
  font-size: 0.88em;
  background: rgba(0, 0, 0, 0.04);
  padding: 0.15em 0.45em;
  border-radius: var(--radius-xs);
  color: #b45309;
}

.daily-summary-body.markdown-body >>> pre {
  margin: 0.75em 0;
  padding: var(--space-md);
  background: var(--bg-sidebar);
  color: #e2e8f0;
  border-radius: var(--radius-md);
  overflow-x: auto;
  font-size: var(--font-size-sm);
  line-height: 1.5;
}

.daily-summary-body.markdown-body >>> pre code {
  background: transparent;
  padding: 0;
  color: inherit;
  font-size: inherit;
}

.daily-summary-body.markdown-body >>> table {
  border-collapse: collapse;
  width: 100%;
  margin: 0.75em 0;
  font-size: var(--font-size-sm);
}

.daily-summary-body.markdown-body >>> th,
.daily-summary-body.markdown-body >>> td {
  border: 1px solid var(--border-primary);
  padding: var(--space-sm) var(--space-md);
  text-align: left;
}

.daily-summary-body.markdown-body >>> th {
  background: var(--bg-hover);
  font-weight: var(--font-weight-semibold);
}

.daily-summary-body.markdown-body >>> strong {
  font-weight: var(--font-weight-semibold);
  color: var(--text-primary);
}

/* ============================================================
   Diff Modal
   ============================================================ */
.diff-modal-backdrop {
  position: fixed;
  inset: 0;
  z-index: var(--z-modal);
  background: rgba(0, 0, 0, 0.45);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: var(--space-xl);
  box-sizing: border-box;
  backdrop-filter: blur(2px);
}

.diff-modal {
  background: var(--bg-card);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-lg);
  width: 100%;
  max-width: 960px;
  max-height: 90vh;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  border: 1px solid var(--border-primary);
}

.diff-modal-header {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: var(--space-lg) var(--space-xl);
  border-bottom: 1px solid var(--border-primary);
  background: var(--bg-hover);
}

.diff-modal-title {
  margin: 0;
  font-size: var(--font-size-md);
  font-weight: var(--font-weight-medium);
  color: var(--text-primary);
  flex: 1;
  min-width: 0;
  padding-right: var(--space-md);
}

.diff-modal-close {
  flex-shrink: 0;
}

.diff-modal-body {
  flex: 1;
  overflow: auto;
  padding: var(--space-xl);
}

.diff-modal-body .diff-box-wrapper {
  margin-top: var(--space-sm);
}

.diff-modal-body .diff-box {
  max-height: 50vh;
}

.diff-box-wrapper {
  margin-top: var(--space-sm);
}

.diff-placeholder-actions {
  display: flex;
  align-items: center;
  gap: var(--space-md);
  margin-top: var(--space-md);
  flex-wrap: wrap;
}

.diff-box-wrapper .view-on-github-link {
  display: inline-block;
  font-size: var(--font-size-sm);
  color: var(--brand-blue);
}

.diff-box-wrapper .view-on-github-link:hover {
  text-decoration: underline;
}

.diff-box {
  margin-top: var(--space-sm);
  max-height: 400px;
  overflow: auto;
  padding: var(--space-md);
  border-radius: var(--radius-sm);
  background-color: var(--bg-sidebar);
  color: #e5e7eb;
  font-size: var(--font-size-xs);
}

.diff-actions {
  display: flex;
  align-items: center;
  gap: var(--space-md);
}

.diff-content {
  white-space: pre-wrap;
  word-break: break-all;
  font-family: var(--font-mono);
}

.diff-line {
  display: block;
}

.diff-add {
  color: #34d399;
  background-color: rgba(52, 211, 153, 0.08);
}

.diff-del {
  color: #f87171;
  background-color: rgba(248, 113, 113, 0.08);
}

/* ============================================================
   AI Analysis Section
   ============================================================ */
.ai-analysis-section {
  margin-top: var(--space-xl);
  padding-top: var(--space-lg);
  border-top: 1px solid var(--border-primary);
}

.ai-analysis-title {
  font-size: var(--font-size-md);
  margin: 0 0 var(--space-md);
  color: var(--text-secondary);
}

.ai-analysis-loading {
  font-size: var(--font-size-sm);
  color: var(--text-tertiary);
}

.ai-analysis-result {
  background: var(--bg-hover);
  border-radius: var(--radius-md);
  padding: var(--space-md) var(--space-lg);
  font-size: var(--font-size-base);
  border: 1px solid var(--border-primary);
}

.ai-analysis-row {
  margin-bottom: var(--space-sm);
  font-size: var(--font-size-base);
}

.ai-analysis-row:last-child {
  margin-bottom: 0;
}

.ai-analysis-label {
  display: inline-block;
  min-width: 100px;
  color: var(--text-tertiary);
  margin-right: var(--space-sm);
}

.ai-analysis-actions {
  margin-top: var(--space-sm);
}

.ai-analysis-hint {
  font-size: var(--font-size-xs);
  color: var(--text-disabled);
  margin: var(--space-sm) 0 0;
}

/* ============================================================
   Page Info (shared)
   ============================================================ */
.page-info {
  font-size: var(--font-size-sm);
  color: var(--text-tertiary);
}

/* ============================================================
   Muted utility
   ============================================================ */
.muted {
  color: var(--text-disabled) !important;
}

</style>

