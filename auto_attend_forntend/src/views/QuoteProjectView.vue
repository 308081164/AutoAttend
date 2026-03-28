<template>
  <div class="quote-project-page">
    <div class="page-head">
      <div class="head-links">
        <router-link to="/quote" class="back-link">← {{ $t('quote.backList') }}</router-link>
        <span class="head-sep" aria-hidden="true">|</span>
        <router-link to="/quote/config" class="head-link-inline">{{ $t('quote.quoteConfigNav') }}</router-link>
      </div>
      <h1>{{ isNew ? $t('quote.newProject') : $t('quote.editTitle') }}</h1>
    </div>

    <div v-if="pageLoading" class="placeholder">{{ $t('quote.loading') }}</div>
    <template v-else>
      <section class="card">
        <h2>项目基础</h2>
        <div class="grid">
          <label>{{ $t('quote.name') }} <input v-model="form.name" class="inp" /></label>
          <label>{{ $t('quote.projectType') }}
            <select v-model="form.projectType" class="inp">
              <option value="website">企业官网</option>
              <option value="ecommerce_miniprogram">电商小程序</option>
              <option value="admin">管理后台</option>
              <option value="app">APP</option>
              <option value="other">其他</option>
            </select>
          </label>
          <label>{{ $t('quote.techStack') }}
            <select v-model="form.techStack" class="inp">
              <option value="vue_node">Vue+Node</option>
              <option value="react_java">React+Java</option>
              <option value="miniprogram">小程序原生</option>
              <option value="flutter">Flutter</option>
              <option value="other">其他</option>
            </select>
          </label>
          <label>{{ $t('quote.designType') }}
            <select v-model="form.designType" class="inp">
              <option value="has_design">已有设计稿</option>
              <option value="need_design">需 UI 设计</option>
              <option value="template">套用模板</option>
            </select>
          </label>
          <label>{{ $t('quote.dataMigration') }}
            <select v-model="form.dataMigration" class="inp">
              <option value="none">无</option>
              <option value="migrate">需从旧系统迁移</option>
              <option value="third_party">需第三方接口对接</option>
            </select>
          </label>
          <label>{{ $t('quote.concurrency') }}
            <select v-model="form.concurrency" class="inp">
              <option value="lt100">&lt;100</option>
              <option value="r100_1000">100–1000</option>
              <option value="r1000_10000">1000–10000</option>
              <option value="gt10000">10000+</option>
            </select>
          </label>
          <label>{{ $t('quote.securityLevel') }}
            <select v-model="form.securityLevel" class="inp">
              <option value="normal">普通</option>
              <option value="financial">金融级</option>
              <option value="government">政府级</option>
            </select>
          </label>
          <label>{{ $t('quote.deployType') }}
            <select v-model="form.deployType" class="inp">
              <option value="single">单机</option>
              <option value="cloud">云服务器</option>
              <option value="k8s">容器化/K8s</option>
            </select>
          </label>
        </div>
        <label class="block">{{ $t('quote.prdSummary') }}</label>
        <textarea v-model="form.prdSummary" class="textarea" rows="4" placeholder="PRD 或需求摘要，有助于提高置信度"></textarea>
      </section>

      <section class="card">
        <h2>{{ $t('quote.quoteDocMetaTitle') }}</h2>
        <p class="hint quote-doc-meta-hint">{{ $t('quote.quoteDocMetaHint') }}</p>
        <div class="quote-subject-mode">
          <span class="mode-label">{{ $t('quote.quoteSubjectModeLabel') }}</span>
          <label class="mode-opt"><input v-model="form.quoteSubjectMode" type="radio" value="legal_entity" /> {{ $t('quote.quoteSubjectLegal') }}</label>
          <label class="mode-opt"><input v-model="form.quoteSubjectMode" type="radio" value="natural_person" /> {{ $t('quote.quoteSubjectNatural') }}</label>
          <label class="mode-opt"><input v-model="form.quoteSubjectMode" type="radio" value="manual" /> {{ $t('quote.quoteSubjectManual') }}</label>
        </div>
        <p v-if="quoteSubjectIncomplete" class="warn-banner">{{ $t('quote.quoteSubjectIncomplete') }}</p>
        <div v-if="form.quoteSubjectMode !== 'manual'" class="quote-header-preview">
          <div class="preview-title">{{ $t('quote.quoteHeaderPreview') }}</div>
          <p v-if="resolvedQuotePreview.vendorName"><strong>{{ $t('quote.quoteVendorName') }}：</strong>{{ resolvedQuotePreview.vendorName }}</p>
          <p v-if="resolvedQuotePreview.contactInfo"><strong>{{ $t('quote.quoteContactInfo') }}：</strong>{{ resolvedQuotePreview.contactInfo }}</p>
          <p v-if="!resolvedQuotePreview.vendorName && !resolvedQuotePreview.contactInfo" class="preview-empty">{{ $t('quote.quoteHeaderPreviewEmpty') }}</p>
        </div>
        <div v-else class="grid">
          <label class="full">{{ $t('quote.quoteVendorName') }} <input v-model="form.quoteVendorName" class="inp wide" :placeholder="$t('quote.quoteVendorNamePh')" /></label>
          <label class="full">{{ $t('quote.quoteContactInfo') }} <input v-model="form.quoteContactInfo" class="inp wide" :placeholder="$t('quote.quoteContactInfoPh')" /></label>
        </div>
        <div class="quote-doc-actions">
          <button type="button" class="btn secondary btn-sm-pad" @click="showPartyBModal = true">{{ $t('quote.editPartyBTemplate') }}</button>
          <router-link to="/quote/config" class="link-config">{{ $t('quote.openQuoteConfig') }}</router-link>
        </div>
        <label class="full block-label">{{ $t('quote.quoteValidityNote') }}</label>
        <input v-model="form.quoteValidityNote" class="inp wide block-inp" :placeholder="$t('quote.quoteValidityNotePh')" />
      </section>

      <party-b-profile-edit-modal :visible="showPartyBModal" @close="showPartyBModal = false" @saved="onPartyBProfileSaved" />

      <section class="card">
        <h2>{{ $t('quote.modules') }}</h2>
        <div class="module-entry-mode-row" role="group" :aria-label="$t('quote.moduleEntryModeLabel')">
          <span class="module-mode-label">{{ $t('quote.moduleEntryModeLabel') }}</span>
          <label class="mode-opt-inline">
            <input v-model="moduleEntryMode" type="radio" value="manual" />
            {{ $t('quote.moduleEntryManual') }}
          </label>
          <label class="mode-opt-inline">
            <input v-model="moduleEntryMode" type="radio" value="ai" />
            {{ $t('quote.moduleEntryAi') }}
          </label>
        </div>

        <div v-show="moduleEntryMode === 'ai'" class="quote-ai-panel">
          <p class="hint">{{ $t('quote.aiModuleHint') }}</p>
          <label class="block">{{ $t('quote.aiModuleRequirementLabel') }}</label>
          <textarea v-model="aiRequirementText" class="textarea" rows="8" :placeholder="$t('quote.aiModulePlaceholder')"></textarea>
          <div class="ai-file-row">
            <label class="btn secondary ai-file-btn">
              {{ $t('quote.aiModuleUploadText') }}
              <input type="file" class="visually-hidden-file" accept=".txt,.md,.markdown,text/plain" @change="onAiRequirementFile" />
            </label>
            <span v-if="aiFileName" class="ai-file-name">{{ aiFileName }}</span>
          </div>
          <div class="ai-merge-row">
            <span class="ai-merge-label">{{ $t('quote.aiModuleMergeLabel') }}</span>
            <label class="mode-opt-inline">
              <input v-model="aiMergeMode" type="radio" value="replace" />
              {{ $t('quote.aiModuleMergeReplace') }}
            </label>
            <label class="mode-opt-inline">
              <input v-model="aiMergeMode" type="radio" value="append" />
              {{ $t('quote.aiModuleMergeAppend') }}
            </label>
          </div>
          <button type="button" class="btn primary" :disabled="aiModuleParsing" @click="runAiParseModules">
            {{ aiModuleParsing ? $t('quote.aiModuleParsing') : $t('quote.aiModuleParseBtn') }}
          </button>
          <p v-if="aiModuleMsg" :class="aiModuleOk ? 'ok' : 'err'" style="margin-top:10px">{{ aiModuleMsg }}</p>
        </div>

        <div v-for="(mod, mi) in modules" :key="'m-' + mi" class="module-block">
          <div class="mod-head">
            <input v-model="mod.name" class="inp mod-name" placeholder="模块名称" />
            <select class="inp preset-select" @change="applyPresetToModule(mi, $event)">
              <option value="">{{ $t('quote.addFromPreset') }}</option>
              <option v-for="p in presetItems" :key="'ps-' + p.id" :value="p.id">{{ formatPresetOption(p) }}</option>
            </select>
            <button type="button" class="btn-sm" @click="addItem(mi)">{{ $t('quote.addItem') }}</button>
            <button type="button" class="btn-sm danger" @click="removeModule(mi)">删模块</button>
          </div>
          <div class="item-table-head" role="row">
            <div class="item-col item-col-name th th-with-hint">
              <span class="th-label-text">{{ $t('quote.itemName') }}</span>
              <button
                type="button"
                class="th-hint-icon"
                :aria-expanded="isTableHintOpen(mi, 'itemName') ? 'true' : 'false'"
                :aria-controls="'quote-th-hint-itemName-' + mi"
                :aria-label="$t('quote.tableHeaderHintAria')"
                @click.stop="toggleTableHint(mi, 'itemName')"
              >?</button>
              <div
                v-show="isTableHintOpen(mi, 'itemName')"
                :id="'quote-th-hint-itemName-' + mi"
                class="th-hint-popover"
                role="tooltip"
              >{{ $t('quote.itemNameHint') }}</div>
            </div>
            <div class="item-col item-col-complexity th th-with-hint">
              <span class="th-label-text">{{ $t('quote.complexity') }}</span>
              <button
                type="button"
                class="th-hint-icon"
                :aria-expanded="isTableHintOpen(mi, 'complexity') ? 'true' : 'false'"
                :aria-controls="'quote-th-hint-complexity-' + mi"
                :aria-label="$t('quote.tableHeaderHintAria')"
                @click.stop="toggleTableHint(mi, 'complexity')"
              >?</button>
              <div
                v-show="isTableHintOpen(mi, 'complexity')"
                :id="'quote-th-hint-complexity-' + mi"
                class="th-hint-popover"
                role="tooltip"
              >{{ $t('quote.complexityHint') }}</div>
            </div>
            <div class="item-col item-col-qty th th-with-hint">
              <span class="th-label-text">{{ $t('quote.quantity') }}</span>
              <button
                type="button"
                class="th-hint-icon"
                :aria-expanded="isTableHintOpen(mi, 'quantity') ? 'true' : 'false'"
                :aria-controls="'quote-th-hint-quantity-' + mi"
                :aria-label="$t('quote.tableHeaderHintAria')"
                @click.stop="toggleTableHint(mi, 'quantity')"
              >?</button>
              <div
                v-show="isTableHintOpen(mi, 'quantity')"
                :id="'quote-th-hint-quantity-' + mi"
                class="th-hint-popover"
                role="tooltip"
              >{{ $t('quote.quantityHint') }}</div>
            </div>
            <div class="item-col item-col-actions th" aria-hidden="true"></div>
          </div>
          <div v-for="(it, ii) in mod.items" :key="'i-' + mi + '-' + ii" class="item-row">
            <input v-model="it.name" class="inp item-col-name" :placeholder="$t('quote.itemName')" />
            <select v-model="it.complexity" class="inp narrow item-col-complexity">
              <option value="simple">简单</option>
              <option value="standard">标准</option>
              <option value="medium">中等</option>
              <option value="complex">复杂</option>
              <option value="extreme">极复杂</option>
            </select>
            <input v-model.number="it.quantity" type="number" min="1" class="inp qty item-col-qty" />
            <div class="item-col-actions">
              <button type="button" class="btn-sm danger" @click="removeItem(mi, ii)">×</button>
            </div>
          </div>
        </div>
        <button type="button" class="btn secondary" @click="addModule">{{ $t('quote.addModule') }}</button>
      </section>

      <div class="actions">
        <button type="button" class="btn primary" :disabled="saving" @click="saveProject">{{ saving ? '…' : $t('quote.save') }}</button>
        <span v-if="autoSaveStatus === 'saved'" class="ok autosave-msg">{{ $t('quote.autoSaveSaved') }}</span>
        <span v-else-if="autoSaveStatus === 'error'" class="err autosave-msg">{{ $t('quote.autoSaveError') }}</span>
        <span v-if="saveMsg" :class="saveOk ? 'ok' : 'err'">{{ saveMsg }}</span>
      </div>

      <section v-if="projectId" class="card">
        <h2>项目创建</h2>
        <p class="hint">一键创建 GitHub 仓库，并将报价需求同步到项目协作多维表，配置 Webhook（push 自动接入）。</p>
        <div v-if="provisionMeta.repoFullName" class="provision-meta">
          <p><strong>仓库：</strong><a :href="provisionMeta.repoHtmlUrl" target="_blank" rel="noopener">{{ provisionMeta.repoFullName }}</a></p>
          <p><strong>状态：</strong>{{ provisionMeta.provisionStatus || '—' }} <span v-if="provisionMeta.provisionLastError" class="err">（{{ provisionMeta.provisionLastError }}）</span></p>
          <p><strong>多维表同步：</strong>{{ provisionMeta.provisionSyncedToCollab ? '已同步' : '未同步' }} <span v-if="provisionMeta.provisionSyncedAt">（{{ provisionMeta.provisionSyncedAt }}）</span></p>
        </div>
        <button type="button" class="btn primary" :disabled="provisioning" @click="openProvisionModal">
          {{ provisioning ? '创建中…' : '创建仓库' }}
        </button>
        <p v-if="provisionMsg" :class="provisionOk ? 'ok' : 'err'">{{ provisionMsg }}</p>
        <div v-if="provisionResult && provisionResult.steps && provisionResult.steps.length" class="provision-steps">
          <h3 class="subh">执行结果</h3>
          <ul>
            <li v-for="(s, i) in provisionResult.steps" :key="'ps-' + i">
              <strong>{{ s.key }}</strong>：<span :class="s.ok ? 'ok' : 'err'">{{ s.message }}</span>
            </li>
          </ul>
        </div>
      </section>

      <section v-if="projectId" class="card">
        <h2>报价计算</h2>
        <div class="risk-grid">
          <label v-for="r in riskConfigsForCalculator" :key="r.riskKey" class="chk">
            <input type="checkbox" :value="r.riskKey" v-model="selectedRisks" />
            {{ r.label }} ({{ formatRiskPct(r.defaultPct) }})
          </label>
          <label v-if="urgencyRiskMeta && urgencyRiskMeta.enabled" class="chk">
            <input type="checkbox" v-model="urgencyRush" />
            {{ urgencyRiskMeta.label }} ({{ formatRiskPct(urgencyRiskMeta.defaultPct) }})
          </label>
        </div>
        <label>{{ $t('quote.priceTier') }}
          <select v-model.number="priceConfigId" class="inp">
            <option v-for="p in priceConfigs" :key="p.id" :value="p.id">{{ formatPriceTierOption(p) }}</option>
          </select>
        </label>
        <h3>{{ $t('quote.auditTitle') }}</h3>
        <div class="risk-grid">
          <label class="chk"><input type="checkbox" v-model="audit.changeLimit" /> 限制需求变更次数</label>
          <label class="chk"><input type="checkbox" v-model="audit.acceptanceClear" /> 验收标准明确</label>
          <label class="chk"><input type="checkbox" v-model="audit.paymentNodes" /> 付款节点约定</label>
          <label class="chk"><input type="checkbox" v-model="audit.deployFee" /> 含部署上线费用</label>
          <label class="chk"><input type="checkbox" v-model="audit.maintenanceScope" /> 维护期及范围明确</label>
        </div>
        <button type="button" class="btn primary" :disabled="calculating" @click="runCalculate">{{ $t('quote.calculate') }}</button>

        <div v-if="calcResult" class="result-box">
          <h3>{{ $t('quote.resultTitle') }}</h3>
          <p>{{ $t('quote.totalDays') }}：{{ calcResult.totalDays }} &nbsp; {{ $t('quote.durationCoefficientUsed') }}：{{ formatCoeff(calcResult.durationCoefficientUsed) }} &nbsp; {{ $t('quote.estimatedDuration') }}：{{ calcResult.estimatedDurationDays != null ? calcResult.estimatedDurationDays : '—' }} {{ $t('quote.durationUnitDays') }} &nbsp; {{ $t('quote.baseAmount') }}：¥{{ calcResult.baseAmount }}</p>
          <p>{{ $t('quote.riskPct') }}：{{ calcResult.riskPctTotal }} &nbsp; 风险金额：¥{{ calcResult.riskAmount }} &nbsp; {{ $t('quote.finalAmount') }}：<strong>¥{{ calcResult.finalAmount }}</strong></p>
          <p>{{ $t('quote.confidence') }}：{{ calcResult.confidenceScore }}（{{ calcResult.confidenceLevel }}）</p>
          <ul v-if="calcResult.riskHints && calcResult.riskHints.length"><li v-for="(h, i) in calcResult.riskHints" :key="i">{{ h }}</li></ul>
          <div class="btn-row export-row">
            <button type="button" class="btn secondary" @click="downloadQuote">{{ $t('quote.genQuote') }}</button>
            <button type="button" class="btn secondary" @click="downloadQuotePdf">{{ $t('quote.genQuotePdf') }}</button>
            <button type="button" class="btn secondary" @click="downloadQuoteDocx">{{ $t('quote.genQuoteWord') }}</button>
          </div>
        </div>
      </section>

      <section v-if="projectId" class="card contract-supplement-card">
        <h2>{{ $t('quote.contractSupplementTitle') }}</h2>
        <p class="hint">{{ $t('quote.contractSupplementHint') }}</p>
        <p class="hint autosave-hint">{{ $t('quote.contractSupplementAutoSaveHint') }}</p>

        <h3 class="subh">{{ $t('quote.paymentPlanTitle') }}</h3>
        <div class="table-wrap">
          <table class="data-table compact-contract">
            <thead>
              <tr>
                <th>{{ $t('quote.paymentPhaseName') }}</th>
                <th>{{ $t('quote.paymentPercent') }}</th>
                <th>{{ $t('quote.paymentTrigger') }}</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="(row, pi) in contractContext.paymentPlan" :key="'pp-' + pi">
                <td><input v-model="row.phaseName" class="inp" /></td>
                <td><input v-model.number="row.percent" type="number" min="0" max="100" step="1" class="inp num narrow" /></td>
                <td><input v-model="row.triggerNote" class="inp wide" /></td>
                <td><button type="button" class="btn-sm danger" @click="removePaymentPhase(pi)">×</button></td>
              </tr>
            </tbody>
          </table>
        </div>
        <button type="button" class="btn secondary btn-sm" @click="addPaymentPhase">{{ $t('quote.addPaymentPhase') }}</button>

        <div class="grid contract-grid-2">
          <label>{{ $t('quote.taxInvoiceNote') }}
            <select v-model="contractContext.taxInvoicePreset" class="inp">
              <option v-for="opt in taxInvoiceOptions" :key="opt.v" :value="opt.v">{{ opt.l }}</option>
            </select>
          </label>
          <label>{{ $t('quote.warrantyMonths') }} <input v-model.number="contractContext.warrantyMonths" type="number" min="0" max="120" class="inp num" /></label>
          <label v-if="contractContext.taxInvoicePreset === 'other'" class="block-full">{{ $t('quote.taxInvoiceCustom') }}
            <input v-model="contractContext.taxInvoiceCustom" class="inp" :placeholder="$t('quote.taxInvoiceCustomPh')" />
          </label>
        </div>

        <div class="sla-block block-full">
          <div class="sla-label-row">
            <span class="sla-label-text">{{ $t('quote.maintenanceSlaNote') }}</span>
            <button
              type="button"
              class="th-hint-icon"
              :aria-expanded="slaHintOpen ? 'true' : 'false'"
              :aria-controls="'quote-sla-hint'"
              :aria-label="$t('quote.slaHintAria')"
              @click.stop="slaHintOpen = !slaHintOpen"
            >?</button>
            <div
              v-show="slaHintOpen"
              id="quote-sla-hint"
              class="th-hint-popover sla-hint-popover"
              role="tooltip"
            >{{ $t('quote.slaHintText') }}</div>
          </div>
          <div class="risk-grid sla-check-grid">
            <label v-for="opt in slaPresetOptions" :key="opt.k" class="chk">
              <input type="checkbox" :checked="contractContext.maintenanceSlaKeys.includes(opt.k)" @change="toggleSlaKey(opt.k, $event)" />
              {{ opt.l }}
            </label>
          </div>
          <label class="block sla-extra-label">{{ $t('quote.slaExtraNote') }}</label>
          <input v-model="contractContext.maintenanceSlaExtra" class="inp block-inp" :placeholder="$t('quote.slaExtraPlaceholder')" />
        </div>

        <h3 class="subh">{{ $t('quote.deliverablesTitle') }}</h3>
        <div class="risk-grid">
          <label v-for="opt in deliverableOptions" :key="opt.k" class="chk">
            <input type="checkbox" :checked="contractContext.deliverables.includes(opt.k)" @change="toggleDeliverable(opt.k, $event)" />
            {{ opt.l }}
          </label>
        </div>

        <h3 class="subh">{{ $t('quote.acceptanceTitle') }}</h3>
        <div class="grid contract-grid-2">
          <label>{{ $t('quote.acceptanceObjectionDays') }} <input v-model.number="contractContext.acceptanceObjectionDays" type="number" min="0" max="60" class="inp num" /></label>
        </div>
        <label class="block">{{ $t('quote.acceptanceCriteriaNote') }}</label>
        <textarea v-model="contractContext.acceptanceCriteriaNote" class="textarea" rows="3" :placeholder="$t('quote.acceptanceCriteriaPlaceholder')"></textarea>

        <h3 class="subh">{{ $t('quote.milestonesTitle') }}</h3>
        <div class="table-wrap">
          <table class="data-table compact-contract">
            <thead>
              <tr>
                <th>{{ $t('quote.milestoneName') }}</th>
                <th>{{ $t('quote.milestoneOffsetDays') }}</th>
                <th>{{ $t('quote.milestoneNote') }}</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="(row, mi) in contractContext.milestones" :key="'ms-' + mi">
                <td><input v-model="row.name" class="inp" /></td>
                <td><input v-model.number="row.offsetDays" type="number" min="0" class="inp num narrow" /></td>
                <td><input v-model="row.note" class="inp wide" /></td>
                <td><button type="button" class="btn-sm danger" @click="removeMilestone(mi)">×</button></td>
              </tr>
            </tbody>
          </table>
        </div>
        <button type="button" class="btn secondary btn-sm" @click="addMilestone">{{ $t('quote.addMilestone') }}</button>

        <label class="block">{{ $t('quote.disputeResolutionNote') }}</label>
        <input v-model="contractContext.disputeResolutionNote" class="inp block-inp" :placeholder="$t('quote.disputePlaceholder')" />

        <h3 class="subh">{{ $t('quote.contractAttachmentsTitle') }}</h3>
        <p class="hint">{{ $t('quote.contractAttachmentsHint') }}</p>
        <div class="btn-row export-row">
          <button type="button" class="btn secondary" @click="downloadAttachmentFunctionList">{{ $t('quote.attachmentFunctionList') }}</button>
          <button type="button" class="btn secondary" @click="downloadAttachmentAcceptance">{{ $t('quote.attachmentAcceptance') }}</button>
          <button type="button" class="btn secondary" @click="downloadAttachmentMilestones">{{ $t('quote.attachmentMilestones') }}</button>
        </div>
        <p class="hint">{{ $t('quote.contractSupplementSaveHint') }}</p>
      </section>

      <!-- 创建仓库弹窗（MVP，内联） -->
      <div v-if="showProvisionModal" class="drawer-mask" @click="closeProvisionModal">
        <div class="drawer" @click.stop>
          <div class="drawer-header">
            <h3>创建 GitHub 仓库</h3>
            <button class="close-btn" @click="closeProvisionModal">×</button>
          </div>
          <div class="drawer-body">
            <div class="grid">
              <label class="full">仓库名 <input v-model="provisionForm.repoName" class="inp wide" placeholder="例如：autoattend-quote-123" /></label>
              <label>可见性
                <select v-model="provisionForm.repoPrivate" class="inp">
                  <option :value="true">Private</option>
                  <option :value="false">Public</option>
                </select>
              </label>
              <label class="full">简介 <input v-model="provisionForm.description" class="inp wide" placeholder="可选" /></label>
            </div>
            <div class="risk-grid">
              <label class="chk"><input type="checkbox" v-model="provisionForm.autoInit" /> 初始化 README</label>
              <label class="chk"><input type="checkbox" v-model="provisionForm.syncMd" /> 写入需求清单 MD</label>
              <label class="chk"><input type="checkbox" v-model="provisionForm.syncCollabTable" /> 同步到多维表</label>
              <label class="chk"><input type="checkbox" v-model="provisionForm.createWebhook" /> 创建 Webhook</label>
            </div>
            <div class="btn-row export-row">
              <button type="button" class="btn primary" :disabled="provisioning" @click="runProvision">
                {{ provisioning ? '创建中…' : '开始创建' }}
              </button>
              <button type="button" class="btn secondary" :disabled="provisioning" @click="closeProvisionModal">取消</button>
            </div>
            <p class="hint" style="margin-top:10px">提示：需要先在「GitHub 集成」配置 GitHub Token，才能创建仓库与 Webhook。</p>
          </div>
        </div>
      </div>

      <section v-if="calcResult && calcResult.id" class="card">
        <h2>{{ $t('quote.contractTitle') }}</h2>
        <div class="grid">
          <label>{{ $t('quote.clientName') }} <input v-model="contract.clientName" class="inp" /></label>
          <label>{{ $t('quote.companyName') }} <input v-model="contract.companyName" class="inp" /></label>
          <p class="hint party-b-hint">{{ $t('quote.companyNamePartyBHint') }}</p>
          <label>{{ $t('quote.templateType') }}
            <select v-model="contract.templateType" class="inp">
              <option value="software_dev">软件开发合同</option>
              <option value="maintenance">维护服务合同</option>
            </select>
          </label>
        </div>
        <button type="button" class="btn primary" :disabled="contractGenLoading" @click="runGenContract">{{ $t('quote.genContract') }}</button>
        <label class="block">合同正文（可编辑）</label>
        <textarea v-model="contract.editedContent" class="textarea contract-ta" rows="16"></textarea>
        <div class="btn-row export-row">
          <button type="button" class="btn secondary" @click="saveContractBody">{{ $t('quote.saveContract') }}</button>
          <button type="button" class="btn secondary" @click="exportContractFile">{{ $t('quote.exportContract') }}</button>
          <button type="button" class="btn secondary" @click="exportContractPdf">{{ $t('quote.exportContractPdf') }}</button>
          <button type="button" class="btn secondary" @click="exportContractDocx">{{ $t('quote.exportContractWord') }}</button>
        </div>
        <p v-if="contractMsg" :class="contractOk ? 'ok' : 'err'">{{ contractMsg }}</p>
      </section>
    </template>
  </div>
</template>

<script>
import PartyBProfileEditModal from '../components/PartyBProfileEditModal.vue'

function emptyModule () {
  return { name: '', sortOrder: 0, items: [{ name: '', complexity: 'standard', quantity: 1 }] }
}

const DEFAULT_DISPUTE_RESOLUTION = '协商不成，提交被告住所地有管辖权的人民法院诉讼'

const TAX_INVOICE_OPTIONS = [
  { v: 'vat_special_13', l: '含税价（增值税专票，税率 13%）' },
  { v: 'vat_normal_13', l: '含税价（增值税普票，税率 13%）' },
  { v: 'vat_special_6', l: '含税价（增值税专票，税率 6%）' },
  { v: 'vat_normal_6', l: '含税价（增值税普票，税率 6%）' },
  { v: 'vat_special_1', l: '含税价（增值税专票，税率 1%）' },
  { v: 'vat_normal_1', l: '含税价（增值税普票，税率 1%）' },
  { v: 'vat_normal_exempt', l: '含税价（增值税普票，免税）' },
  { v: 'receipt', l: '含收据（非增值税发票）' },
  { v: 'price_excl_tax', l: '报价不含税，发票类型另行约定' },
  { v: 'other', l: '其他（见补充说明）' }
]

const SLA_PRESET_OPTIONS = [
  { k: 'workday_4h', l: '工作日 4 小时内响应' },
  { k: 'workday_8h', l: '工作日 8 小时内响应' },
  { k: 'urgent_2h', l: '紧急故障 2 小时内响应' },
  { k: 'fix_48h', l: '一般缺陷 48 小时内提供修复方案或补丁' },
  { k: 'fix_5d', l: '非紧急问题 5 个工作日内处理' },
  { k: 'remote_9_18', l: '远程协助支持（工作日 9:00–18:00）' },
  { k: 'exclude_disaster', l: '不含数据灾难恢复（另行约定）' }
]

const DEFAULT_SLA_KEYS = ['workday_4h', 'fix_48h', 'remote_9_18']

function normalizeTaxInvoiceFields (raw) {
  if (!raw || typeof raw !== 'object') {
    return { taxInvoicePreset: 'vat_special_13', taxInvoiceCustom: '' }
  }
  const preset = raw.taxInvoicePreset
  if (typeof preset === 'string' && TAX_INVOICE_OPTIONS.some(o => o.v === preset)) {
    return {
      taxInvoicePreset: preset,
      taxInvoiceCustom: raw.taxInvoiceCustom != null ? String(raw.taxInvoiceCustom) : ''
    }
  }
  const oldNote = raw.taxInvoiceNote != null ? String(raw.taxInvoiceNote).trim() : ''
  if (!oldNote) return { taxInvoicePreset: 'vat_special_13', taxInvoiceCustom: '' }
  const hit = TAX_INVOICE_OPTIONS.find(o => o.l === oldNote)
  if (hit) return { taxInvoicePreset: hit.v, taxInvoiceCustom: '' }
  return { taxInvoicePreset: 'other', taxInvoiceCustom: oldNote }
}

function normalizeMaintenanceSlaFields (raw) {
  const keys = Array.isArray(raw.maintenanceSlaKeys)
    ? raw.maintenanceSlaKeys.filter(k => typeof k === 'string' && SLA_PRESET_OPTIONS.some(o => o.k === k))
    : []
  const extra = raw.maintenanceSlaExtra != null ? String(raw.maintenanceSlaExtra) : ''
  const legacyNote = raw.maintenanceSlaNote != null ? String(raw.maintenanceSlaNote).trim() : ''
  if (!keys.length && legacyNote) {
    return { maintenanceSlaKeys: [], maintenanceSlaExtra: legacyNote }
  }
  if (!keys.length) {
    return { maintenanceSlaKeys: [...DEFAULT_SLA_KEYS], maintenanceSlaExtra: extra }
  }
  return { maintenanceSlaKeys: keys, maintenanceSlaExtra: extra }
}

function defaultContractContext () {
  return {
    paymentPlan: [
      { phaseName: '签约', percent: 30, triggerNote: '合同签订后5个工作日内' },
      { phaseName: '阶段验收', percent: 40, triggerNote: '里程碑达成并通过阶段验收后' },
      { phaseName: '终验', percent: 30, triggerNote: '项目终验通过后7日内' }
    ],
    taxInvoicePreset: 'vat_special_13',
    taxInvoiceCustom: '',
    warrantyMonths: 3,
    maintenanceSlaKeys: [...DEFAULT_SLA_KEYS],
    maintenanceSlaExtra: '',
    deliverables: ['source_code', 'deploy_doc', 'api_doc'],
    acceptanceObjectionDays: 5,
    acceptanceCriteriaNote: '',
    milestones: [
      { name: '需求/范围确认', offsetDays: 7, note: '' },
      { name: '开发与联调', offsetDays: 30, note: '' },
      { name: 'UAT 与终验', offsetDays: 45, note: '' }
    ],
    disputeResolutionNote: DEFAULT_DISPUTE_RESOLUTION
  }
}

function normalizeContractContext (raw) {
  const base = defaultContractContext()
  if (!raw || typeof raw !== 'object') return JSON.parse(JSON.stringify(base))
  const pay = Array.isArray(raw.paymentPlan) && raw.paymentPlan.length
    ? raw.paymentPlan.map(p => ({
      phaseName: p.phaseName != null ? String(p.phaseName) : '',
      percent: p.percent != null ? Number(p.percent) : 0,
      triggerNote: p.triggerNote != null ? String(p.triggerNote) : ''
    }))
    : base.paymentPlan
  const del = Array.isArray(raw.deliverables) ? raw.deliverables.filter(Boolean) : base.deliverables
  const ms = Array.isArray(raw.milestones) && raw.milestones.length
    ? raw.milestones.map(m => ({
      name: m.name != null ? String(m.name) : '',
      offsetDays: m.offsetDays != null ? Number(m.offsetDays) : 0,
      note: m.note != null ? String(m.note) : ''
    }))
    : base.milestones
  const tax = normalizeTaxInvoiceFields(raw)
  const sla = normalizeMaintenanceSlaFields(raw)
  const dr = raw.disputeResolutionNote != null ? String(raw.disputeResolutionNote).trim() : ''
  return {
    paymentPlan: pay,
    taxInvoicePreset: tax.taxInvoicePreset,
    taxInvoiceCustom: tax.taxInvoiceCustom,
    warrantyMonths: raw.warrantyMonths != null ? Math.max(0, Number(raw.warrantyMonths) || 0) : base.warrantyMonths,
    maintenanceSlaKeys: sla.maintenanceSlaKeys,
    maintenanceSlaExtra: sla.maintenanceSlaExtra,
    deliverables: del.length ? del : [...base.deliverables],
    acceptanceObjectionDays: raw.acceptanceObjectionDays != null ? Math.max(0, Number(raw.acceptanceObjectionDays) || 0) : base.acceptanceObjectionDays,
    acceptanceCriteriaNote: raw.acceptanceCriteriaNote != null ? String(raw.acceptanceCriteriaNote) : '',
    milestones: ms,
    disputeResolutionNote: dr || DEFAULT_DISPUTE_RESOLUTION
  }
}

export default {
  name: 'QuoteProjectView',
  components: { PartyBProfileEditModal },
  data () {
    return {
      pageLoading: true,
      isNew: false,
      projectId: null,
      form: {
        name: '',
        projectType: 'website',
        techStack: 'vue_node',
        designType: 'need_design',
        dataMigration: 'none',
        concurrency: 'lt100',
        securityLevel: 'normal',
        deployType: 'cloud',
        status: 'draft',
        linkTableId: null,
        prdSummary: '',
        quoteSubjectMode: 'legal_entity',
        quoteVendorName: '',
        quoteContactInfo: '',
        quoteValidityNote: ''
      },
      partyBProfile: {},
      showPartyBModal: false,
      modules: [emptyModule()],
      riskConfigs: [],
      priceConfigs: [],
      selectedRisks: [],
      urgencyRush: false,
      priceConfigId: null,
      audit: {
        changeLimit: false,
        acceptanceClear: false,
        paymentNodes: false,
        deployFee: false,
        maintenanceScope: false
      },
      calcResult: null,
      calculating: false,
      saving: false,
      saveMsg: '',
      saveOk: false,
      contract: { clientName: '', companyName: '', templateType: 'software_dev', editedContent: '' },
      contractGenLoading: false,
      contractMsg: '',
      contractOk: false,
      /** 仅启用项，供「从预设添加」下拉（在「报价配置」页维护） */
      presetItems: [],
      restoringCalcPrefs: false,
      restoringProject: false,
      lastAutoSavedSnapshot: '',
      autoSaveDebounceTimer: null,
      autoSaveStatus: '',
      slaHintOpen: false,
      taxInvoiceOptions: TAX_INVOICE_OPTIONS,
      slaPresetOptions: SLA_PRESET_OPTIONS,
      calcPrefsDebounceTimer: null,
      /** 功能点表头「?」说明：`field:moduleIndex` 或 null */
      openTableHint: null,
      // 项目创建（GitHub provision）
      showProvisionModal: false,
      provisioning: false,
      provisionMsg: '',
      provisionOk: false,
      provisionResult: null,
      provisionMeta: {
        repoFullName: '',
        repoHtmlUrl: '',
        provisionStatus: '',
        provisionLastError: '',
        provisionSyncedToCollab: false,
        provisionSyncedAt: ''
      },
      provisionForm: {
        repoName: '',
        repoPrivate: true,
        description: '',
        autoInit: true,
        syncMd: true,
        syncCollabTable: true,
        createWebhook: true
      },
      contractContext: defaultContractContext(),
      /** 功能模块：manual | ai */
      moduleEntryMode: 'manual',
      aiRequirementText: '',
      aiMergeMode: 'replace',
      aiModuleParsing: false,
      aiModuleMsg: '',
      aiModuleOk: false,
      aiFileName: '',
      deliverableOptions: [
        { k: 'source_code', l: '源代码' },
        { k: 'deploy_doc', l: '部署文档' },
        { k: 'api_doc', l: 'API 文档' },
        { k: 'db_script', l: '数据库脚本' },
        { k: 'test_report', l: '测试报告' },
        { k: 'user_manual', l: '操作说明' },
        { k: 'training', l: '远程培训/讲解' }
      ]
    }
  },
  computed: {
    /** 用于侦听报价偏好变化并防抖落库 */
    calcPrefsSnapshot () {
      return JSON.stringify({
        rk: this.selectedRisks,
        ur: this.urgencyRush,
        pc: this.priceConfigId,
        au: this.audit
      })
    },
    /** 用于整页自动保存（已存在项目） */
    projectPayloadSnapshot () {
      if (this.pageLoading || !this.projectId) return ''
      return JSON.stringify(this.payload())
    },
    riskConfigsForCalculator () {
      return (this.riskConfigs || []).filter(r => r.riskKey !== 'urgency_rush' && r.enabled)
    },
    urgencyRiskMeta () {
      return (this.riskConfigs || []).find(r => r.riskKey === 'urgency_rush')
    },
    resolvedQuotePreview () {
      const mode = this.form.quoteSubjectMode || 'legal_entity'
      const prof = this.partyBProfile || {}
      if (mode === 'manual') {
        return {
          vendorName: (this.form.quoteVendorName || '').trim(),
          contactInfo: (this.form.quoteContactInfo || '').trim()
        }
      }
      if (mode === 'natural_person') {
        const np = prof.naturalPerson && typeof prof.naturalPerson === 'object' ? prof.naturalPerson : {}
        const parts = []
        if (np.contactPhone) parts.push('电话：' + String(np.contactPhone).trim())
        if (np.email) parts.push('邮箱：' + String(np.email).trim())
        if (np.address) parts.push('地址：' + String(np.address).trim())
        return {
          vendorName: (np.fullName || '').trim(),
          contactInfo: parts.join('；')
        }
      }
      const parts = []
      if (prof.contactName) parts.push('联系人：' + String(prof.contactName).trim())
      if (prof.contactPhone) parts.push('电话：' + String(prof.contactPhone).trim())
      if (prof.address) parts.push('地址：' + String(prof.address).trim())
      return {
        vendorName: (prof.legalName || '').trim(),
        contactInfo: parts.join('；')
      }
    },
    quoteSubjectIncomplete () {
      const mode = this.form.quoteSubjectMode || 'legal_entity'
      if (mode === 'manual') return false
      const r = this.resolvedQuotePreview
      return !r.vendorName && !r.contactInfo
    }
  },
  created () {
    this.init()
  },
  mounted () {
    document.addEventListener('click', this.closeTableHintOnOutside)
  },
  beforeDestroy () {
    document.removeEventListener('click', this.closeTableHintOnOutside)
    if (this.calcPrefsDebounceTimer) clearTimeout(this.calcPrefsDebounceTimer)
    if (this.autoSaveDebounceTimer) clearTimeout(this.autoSaveDebounceTimer)
  },
  watch: {
    '$route.params.id' () {
      this.init()
    },
    calcPrefsSnapshot () {
      if (this.pageLoading || this.restoringCalcPrefs) return
      this.scheduleQuoteCalcPrefsSave()
    },
    projectPayloadSnapshot: {
      handler (val) {
        if (this.pageLoading || this.restoringProject || !this.projectId) return
        if (!val || val === this.lastAutoSavedSnapshot) return
        this.scheduleProjectAutoSave()
      }
    }
  },
  methods: {
    tableHintKey (moduleIndex, field) {
      return String(field) + ':' + String(moduleIndex)
    },
    isTableHintOpen (moduleIndex, field) {
      return this.openTableHint === this.tableHintKey(moduleIndex, field)
    },
    toggleTableHint (moduleIndex, field) {
      const k = this.tableHintKey(moduleIndex, field)
      this.openTableHint = this.openTableHint === k ? null : k
    },
    closeTableHintOnOutside () {
      this.openTableHint = null
      this.slaHintOpen = false
    },
    async loadPartyBProfile () {
      try {
        const resp = await this.$http.get('/admin/quote/party-b-profile')
        if (resp.data && resp.data.code === 0 && resp.data.data) {
          this.partyBProfile = { ...resp.data.data }
        } else {
          this.partyBProfile = {}
        }
      } catch (e) {
        this.partyBProfile = {}
      }
    },
    onPartyBProfileSaved () {
      this.loadPartyBProfile()
    },
    applyRiskConfigPayload (arr) {
      this.riskConfigs = (arr || []).map(r => ({
        id: r.id,
        riskKey: r.riskKey,
        label: r.label,
        defaultPct: Number(r.defaultPct),
        enabled: r.enabled === true || r.enabled === 1
      }))
    },
    applyPresetPickerPayload (arr) {
      this.presetItems = (arr || []).map(p => ({
        id: p.id,
        name: p.name,
        complexity: p.complexity || 'standard',
        category: p.category || '',
        enabled: p.enabled === true || p.enabled === 1
      })).filter(p => p.enabled)
    },
    formatRiskPct (n) {
      const x = Number(n)
      if (Number.isNaN(x)) return '0%'
      if (x >= 0) return '+' + x + '%'
      return x + '%'
    },
    formatCoeff (c) {
      if (c == null || c === '') return '1.2'
      const x = Number(c)
      return Number.isFinite(x) ? String(x) : '1.2'
    },
    formatPriceTierOption (p) {
      const coeff = p.durationCoefficient != null ? Number(p.durationCoefficient) : 1.2
      const c = Number.isFinite(coeff) && coeff > 0 ? coeff : 1.2
      return `${p.regionLabel} — ¥${p.pricePerDay}/天 ×${c}`
    },
    formatPresetOption (p) {
      const c = p.category ? p.category + ' · ' : ''
      return c + p.name
    },
    resetCalcPrefsUi () {
      this.selectedRisks = []
      this.urgencyRush = false
      this.audit = {
        changeLimit: false,
        acceptanceClear: false,
        paymentNodes: false,
        deployFee: false,
        maintenanceScope: false
      }
      if (this.priceConfigs.length) this.priceConfigId = this.priceConfigs[0].id
      else this.priceConfigId = null
    },
    buildQuoteCalcPrefsPayload () {
      return {
        riskKeys: Array.isArray(this.selectedRisks) ? [...this.selectedRisks] : [],
        urgencyRush: !!this.urgencyRush,
        priceConfigId: this.priceConfigId,
        auditChecklist: { ...this.audit }
      }
    },
    applyQuoteCalcPrefs (prefs) {
      if (!prefs || typeof prefs !== 'object') return
      if (Array.isArray(prefs.riskKeys)) {
        this.selectedRisks = prefs.riskKeys.filter(k => typeof k === 'string')
      }
      if (typeof prefs.urgencyRush === 'boolean') this.urgencyRush = prefs.urgencyRush
      const pid = prefs.priceConfigId
      if (pid != null && this.priceConfigs.some(p => Number(p.id) === Number(pid))) {
        this.priceConfigId = Number(pid)
      }
      const ac = prefs.auditChecklist
      if (ac && typeof ac === 'object') {
        const keys = ['changeLimit', 'acceptanceClear', 'paymentNodes', 'deployFee', 'maintenanceScope']
        keys.forEach(k => {
          if (Object.prototype.hasOwnProperty.call(ac, k)) this.audit[k] = !!ac[k]
        })
      }
    },
    scheduleQuoteCalcPrefsSave () {
      if (this.calcPrefsDebounceTimer) clearTimeout(this.calcPrefsDebounceTimer)
      this.calcPrefsDebounceTimer = setTimeout(() => this.flushQuoteCalcPrefs(), 650)
    },
    async flushQuoteCalcPrefs () {
      if (this.pageLoading || this.restoringCalcPrefs || !this.projectId) return
      const body = this.buildQuoteCalcPrefsPayload()
      try {
        await this.$http.patch('/admin/quote/projects/' + this.projectId + '/calc-prefs', body)
      } catch (e) { /* 静默失败，避免打断编辑 */ }
    },
    applyPresetToModule (mi, ev) {
      const v = ev.target.value
      ev.target.value = ''
      if (!v) return
      const p = this.presetItems.find(x => String(x.id) === String(v))
      if (!p) return
      this.modules[mi].items.push({
        name: p.name,
        complexity: p.complexity || 'standard',
        quantity: 1
      })
    },
    async init () {
      const raw = this.$route.params.id
      this.isNew = raw === 'new'
      this.pageLoading = true
      this.calcResult = null
      this.saveMsg = ''
      try {
        const [r1, r2, r3] = await Promise.all([
          this.$http.get('/admin/quote/risk-config'),
          this.$http.get('/admin/quote/price-config'),
          this.$http.get('/admin/quote/preset-items')
        ])
        if (r1.data && r1.data.code === 0) this.applyRiskConfigPayload(r1.data.data)
        if (r2.data && r2.data.code === 0) {
          this.priceConfigs = r2.data.data || []
          if (this.priceConfigs.length && !this.priceConfigId) this.priceConfigId = this.priceConfigs[0].id
        }
        if (r3.data && r3.data.code === 0) this.applyPresetPickerPayload(r3.data.data)
        else this.presetItems = []
        await this.loadPartyBProfile()
        if (this.isNew) {
          this.projectId = null
          this.form = {
            ...this.form,
            name: '',
            prdSummary: '',
            quoteSubjectMode: 'legal_entity',
            quoteVendorName: '',
            quoteContactInfo: '',
            quoteValidityNote: ''
          }
          this.modules = [emptyModule()]
          this.contractContext = normalizeContractContext(null)
          this.resetCalcPrefsUi()
        } else {
          this.projectId = Number(raw)
          await this.loadProject(this.projectId)
        }
      } catch (e) {
        if (e.response && e.response.status === 401) this.$router.push({ name: 'login' })
      } finally {
        this.pageLoading = false
      }
    },
    async loadProject (id) {
      this.restoringProject = true
      try {
        const resp = await this.$http.get('/admin/quote/projects/' + id)
        if (resp.data && resp.data.code === 0 && resp.data.data) {
          const d = resp.data.data
          this.restoringCalcPrefs = true
          try {
            this.form.name = d.name || ''
            this.form.projectType = d.projectType || 'other'
            this.form.techStack = d.techStack || 'vue_node'
            this.form.designType = d.designType || 'need_design'
            this.form.dataMigration = d.dataMigration || 'none'
            this.form.concurrency = d.concurrency || 'lt100'
            this.form.securityLevel = d.securityLevel || 'normal'
            this.form.deployType = d.deployType || 'cloud'
            this.form.status = d.status || 'draft'
            this.form.linkTableId = d.linkTableId
            this.form.prdSummary = d.prdSummary || ''
            this.form.quoteSubjectMode = d.quoteSubjectMode || 'legal_entity'
            this.form.quoteVendorName = d.quoteVendorName || ''
            this.form.quoteContactInfo = d.quoteContactInfo || ''
            this.form.quoteValidityNote = d.quoteValidityNote || ''
            this.contractContext = normalizeContractContext(d.quoteContractContext)
            const mods = d.modules || []
            if (!mods.length) this.modules = [emptyModule()]
            else {
              this.modules = mods.map((m, idx) => ({
                name: m.name,
                sortOrder: m.sortOrder != null ? m.sortOrder : idx,
                items: (m.items || []).map(it => ({
                  name: it.name,
                  complexity: it.complexity || 'standard',
                  quantity: it.quantity || 1
                }))
              }))
            }
            if (d.quoteCalcPrefs) {
              this.applyQuoteCalcPrefs(d.quoteCalcPrefs)
            }
            if (d.latestResult) {
              this.calcResult = { ...d.latestResult, riskHints: [], confidenceLevel: '' }
            }
            this.provisionMeta = {
              repoFullName: d.githubRepoFullName || '',
              repoHtmlUrl: d.githubRepoHtmlUrl || '',
              provisionStatus: d.provisionStatus || '',
              provisionLastError: d.provisionLastError || '',
              provisionSyncedToCollab: d.provisionSyncedToCollab === true,
              provisionSyncedAt: d.provisionSyncedAt || ''
            }
            if (!this.provisionForm.repoName) {
              this.provisionForm.repoName = this.suggestRepoName()
              this.provisionForm.description = this.suggestRepoDesc()
            }
            await this.loadContractIfAny()
          } finally {
            this.$nextTick(() => {
              this.restoringCalcPrefs = false
              this.lastAutoSavedSnapshot = JSON.stringify(this.payload())
              this.restoringProject = false
            })
          }
        } else {
          this.restoringProject = false
        }
      } catch (e) {
        this.restoringProject = false
        throw e
      }
    },
    suggestRepoName () {
      const base = (this.form.name || '').trim()
      const clean = base
        .toLowerCase()
        .replace(/[^a-z0-9\\-\\s_]+/g, '')
        .replace(/\\s+/g, '-')
        .replace(/_+/g, '-')
        .replace(/-+/g, '-')
        .replace(/^-|-$/g, '')
      if (clean) return `autoattend-${clean}`.slice(0, 80)
      return `autoattend-quote-${this.projectId || 'new'}`
    },
    suggestRepoDesc () {
      const name = (this.form.name || '').trim()
      return name ? `AutoAttend 报价项目：${name}` : 'AutoAttend 报价项目'
    },
    openProvisionModal () {
      this.provisionMsg = ''
      this.provisionOk = false
      this.provisionResult = null
      if (!this.provisionForm.repoName) {
        this.provisionForm.repoName = this.suggestRepoName()
      }
      if (!this.provisionForm.description) {
        this.provisionForm.description = this.suggestRepoDesc()
      }
      this.showProvisionModal = true
    },
    closeProvisionModal () {
      if (this.provisioning) return
      this.showProvisionModal = false
    },
    async runProvision () {
      if (!this.projectId) return
      const repoName = (this.provisionForm.repoName || '').trim()
      if (!repoName) {
        this.provisionOk = false
        this.provisionMsg = '请填写仓库名'
        return
      }
      this.provisioning = true
      this.provisionMsg = ''
      this.provisionOk = false
      this.provisionResult = null
      try {
        const body = {
          repoName: repoName,
          repoPrivate: this.provisionForm.repoPrivate === true,
          description: (this.provisionForm.description || '').trim(),
          autoInit: this.provisionForm.autoInit === true,
          syncMd: this.provisionForm.syncMd === true,
          syncCollabTable: this.provisionForm.syncCollabTable === true,
          createWebhook: this.provisionForm.createWebhook === true
        }
        const resp = await this.$http.post(`/admin/quote/projects/${this.projectId}/provision`, body, { timeout: 120000 })
        if (resp.data && resp.data.code === 0 && resp.data.data) {
          this.provisionOk = true
          this.provisionMsg = '已提交创建'
          this.provisionResult = resp.data.data
          await this.loadProject(this.projectId)
          this.showProvisionModal = false
        } else {
          this.provisionOk = false
          this.provisionMsg = (resp.data && resp.data.message) || '创建失败'
        }
      } catch (e) {
        this.provisionOk = false
        this.provisionMsg = (e.response && e.response.data && e.response.data.message) || '网络错误'
      } finally {
        this.provisioning = false
      }
    },
    async loadContractIfAny () {
      if (!this.calcResult || !this.calcResult.id) return
      try {
        const resp = await this.$http.get('/admin/quote/results/' + this.calcResult.id + '/contract')
        if (resp.data && resp.data.code === 0 && resp.data.data) {
          const c = resp.data.data
          this.contract.clientName = c.clientName || ''
          this.contract.companyName = c.companyName || ''
          this.contract.templateType = c.templateType || 'software_dev'
          this.contract.editedContent = c.editedContent || c.aiRawResponse || ''
        }
      } catch (e) { /* 404 ok */ }
    },
    payload () {
      return {
        name: this.form.name,
        projectType: this.form.projectType,
        techStack: this.form.techStack,
        designType: this.form.designType,
        dataMigration: this.form.dataMigration,
        concurrency: this.form.concurrency,
        securityLevel: this.form.securityLevel,
        deployType: this.form.deployType,
        status: this.form.status,
        linkTableId: this.form.linkTableId,
        prdSummary: this.form.prdSummary,
        quoteSubjectMode: this.form.quoteSubjectMode || 'legal_entity',
        quoteVendorName: this.form.quoteVendorName || '',
        quoteContactInfo: this.form.quoteContactInfo || '',
        quoteValidityNote: this.form.quoteValidityNote || '',
        modules: this.modules.map((m, mi) => ({
          name: m.name,
          sortOrder: m.sortOrder != null ? m.sortOrder : mi,
          items: (m.items || []).filter(it => it.name && String(it.name).trim()).map(it => ({
            name: String(it.name).trim(),
            complexity: it.complexity || 'standard',
            quantity: Math.max(1, Number(it.quantity) || 1)
          }))
        })).filter(m => m.name && String(m.name).trim() && m.items.length),
        quoteCalcPrefs: this.buildQuoteCalcPrefsPayload(),
        quoteContractContext: {
          paymentPlan: (this.contractContext.paymentPlan || []).map(p => ({
            phaseName: p.phaseName,
            percent: Math.max(0, Math.min(100, Number(p.percent) || 0)),
            triggerNote: p.triggerNote || ''
          })),
          taxInvoiceNote: this.buildTaxInvoiceNote(),
          taxInvoicePreset: this.contractContext.taxInvoicePreset,
          taxInvoiceCustom: this.contractContext.taxInvoiceCustom || '',
          warrantyMonths: Math.max(0, Number(this.contractContext.warrantyMonths) || 0),
          maintenanceSlaNote: this.buildMaintenanceSlaNote(),
          maintenanceSlaKeys: [...(this.contractContext.maintenanceSlaKeys || [])],
          maintenanceSlaExtra: (this.contractContext.maintenanceSlaExtra || '').trim(),
          deliverables: [...(this.contractContext.deliverables || [])],
          acceptanceObjectionDays: Math.max(0, Number(this.contractContext.acceptanceObjectionDays) || 0),
          acceptanceCriteriaNote: this.contractContext.acceptanceCriteriaNote || '',
          milestones: (this.contractContext.milestones || []).map(m => ({
            name: m.name || '',
            offsetDays: Math.max(0, Number(m.offsetDays) || 0),
            note: m.note || ''
          })),
          disputeResolutionNote: this.contractContext.disputeResolutionNote || ''
        }
      }
    },
    buildTaxInvoiceNote () {
      const ctx = this.contractContext
      if (ctx.taxInvoicePreset === 'other') {
        return (ctx.taxInvoiceCustom || '').trim() || '其他（待补充）'
      }
      const opt = TAX_INVOICE_OPTIONS.find(o => o.v === ctx.taxInvoicePreset)
      return opt ? opt.l : (ctx.taxInvoiceCustom || '').trim()
    },
    buildMaintenanceSlaNote () {
      const parts = (this.contractContext.maintenanceSlaKeys || []).map(k => {
        const o = SLA_PRESET_OPTIONS.find(x => x.k === k)
        return o ? o.l : ''
      }).filter(Boolean)
      const extra = (this.contractContext.maintenanceSlaExtra || '').trim()
      if (extra) parts.push(extra)
      return parts.join('；')
    },
    scheduleProjectAutoSave () {
      if (this.autoSaveDebounceTimer) clearTimeout(this.autoSaveDebounceTimer)
      this.autoSaveDebounceTimer = setTimeout(() => this.flushProjectAutoSave(), 1200)
    },
    async flushProjectAutoSave () {
      if (this.pageLoading || this.restoringProject || !this.projectId || this.saving) return
      const snap = JSON.stringify(this.payload())
      if (snap === this.lastAutoSavedSnapshot) return
      try {
        await this.$http.put('/admin/quote/projects/' + this.projectId, this.payload())
        this.lastAutoSavedSnapshot = JSON.stringify(this.payload())
        this.autoSaveStatus = 'saved'
        setTimeout(() => {
          if (this.autoSaveStatus === 'saved') this.autoSaveStatus = ''
        }, 2500)
      } catch (e) {
        this.autoSaveStatus = 'error'
      }
    },
    toggleSlaKey (key, ev) {
      const arr = this.contractContext.maintenanceSlaKeys
      if (ev.target.checked) {
        if (!arr.includes(key)) arr.push(key)
      } else {
        const i = arr.indexOf(key)
        if (i >= 0) arr.splice(i, 1)
      }
    },
    async saveProject () {
      if (this.autoSaveDebounceTimer) {
        clearTimeout(this.autoSaveDebounceTimer)
        this.autoSaveDebounceTimer = null
      }
      this.saving = true
      this.saveMsg = ''
      try {
        const body = this.payload()
        if (this.isNew) {
          const resp = await this.$http.post('/admin/quote/projects', body)
          if (resp.data && resp.data.code === 0 && resp.data.data && resp.data.data.id) {
            this.saveOk = true
            this.saveMsg = '已创建'
            const newId = resp.data.data.id
            this.projectId = newId
            this.isNew = false
            await this.$router.replace({ name: 'quote-project', params: { id: String(newId) } })
            this.$nextTick(() => {
              this.lastAutoSavedSnapshot = JSON.stringify(this.payload())
            })
          } else {
            this.saveOk = false
            this.saveMsg = (resp.data && resp.data.message) || '失败'
          }
        } else {
          const resp = await this.$http.put('/admin/quote/projects/' + this.projectId, body)
          if (resp.data && resp.data.code === 0) {
            this.saveOk = true
            this.saveMsg = '已保存'
            this.$nextTick(() => {
              this.lastAutoSavedSnapshot = JSON.stringify(this.payload())
            })
          } else {
            this.saveOk = false
            this.saveMsg = (resp.data && resp.data.message) || '失败'
          }
        }
      } catch (e) {
        this.saveOk = false
        this.saveMsg = (e.response && e.response.data && e.response.data.message) || '网络错误'
      } finally {
        this.saving = false
      }
    },
    onAiRequirementFile (ev) {
      const f = ev.target && ev.target.files && ev.target.files[0]
      if (!f) return
      this.aiFileName = f.name
      const reader = new FileReader()
      reader.onload = () => {
        const t = typeof reader.result === 'string' ? reader.result : ''
        if (t) {
          this.aiRequirementText = (this.aiRequirementText ? this.aiRequirementText + '\n\n' : '') + t
        }
      }
      reader.readAsText(f, 'UTF-8')
      ev.target.value = ''
    },
    normalizeAiModules (list) {
      const allowed = ['simple', 'standard', 'medium', 'complex', 'extreme']
      if (!Array.isArray(list)) return []
      return list.map((m, mi) => {
        const items = (Array.isArray(m.items) ? m.items : []).map(it => {
          const cx = (it.complexity && allowed.includes(it.complexity)) ? it.complexity : 'standard'
          return {
            name: String(it.name || '').trim(),
            complexity: cx,
            quantity: Math.max(1, Number(it.quantity) || 1)
          }
        }).filter(it => it.name)
        return {
          name: String(m.name || '').trim(),
          sortOrder: typeof m.sortOrder === 'number' ? m.sortOrder : mi,
          items: items.length ? items : [{ name: '', complexity: 'standard', quantity: 1 }]
        }
      }).filter(m => m.name && m.items.some(it => it.name))
    },
    async runAiParseModules () {
      const text = (this.aiRequirementText || '').trim()
      if (!text) {
        this.aiModuleOk = false
        this.aiModuleMsg = this.$t('quote.aiModuleEmpty')
        return
      }
      this.aiModuleParsing = true
      this.aiModuleMsg = ''
      this.aiModuleOk = false
      try {
        const body = {
          requirementText: text,
          projectType: this.form.projectType,
          techStack: this.form.techStack,
          designType: this.form.designType,
          dataMigration: this.form.dataMigration,
          concurrency: this.form.concurrency,
          securityLevel: this.form.securityLevel,
          deployType: this.form.deployType,
          prdSummary: this.form.prdSummary || ''
        }
        const resp = await this.$http.post('/admin/quote/ai/parse-modules', body)
        if (resp.data && resp.data.code === 0 && resp.data.data && resp.data.data.modules) {
          const normalized = this.normalizeAiModules(resp.data.data.modules)
          if (!normalized.length) {
            this.aiModuleOk = false
            this.aiModuleMsg = this.$t('quote.aiModuleNoResult')
            return
          }
          if (this.aiMergeMode === 'append') {
            const base = this.modules.filter(m => (m.name && String(m.name).trim()) || (m.items || []).some(it => it.name && String(it.name).trim()))
            this.modules = base.concat(normalized)
          } else {
            this.modules = normalized
          }
          this.aiModuleOk = true
          this.aiModuleMsg = this.$t('quote.aiModuleOk')
          this.moduleEntryMode = 'manual'
        } else {
          this.aiModuleOk = false
          this.aiModuleMsg = (resp.data && resp.data.message) || this.$t('quote.aiModuleFail')
        }
      } catch (e) {
        this.aiModuleOk = false
        this.aiModuleMsg = (e.response && e.response.data && e.response.data.message) || this.$t('quote.aiModuleFail')
      } finally {
        this.aiModuleParsing = false
      }
    },
    addModule () {
      this.modules.push(emptyModule())
    },
    removeModule (mi) {
      if (this.modules.length <= 1) return
      this.modules.splice(mi, 1)
    },
    addItem (mi) {
      this.modules[mi].items.push({ name: '', complexity: 'standard', quantity: 1 })
    },
    removeItem (mi, ii) {
      if (this.modules[mi].items.length <= 1) return
      this.modules[mi].items.splice(ii, 1)
    },
    async runCalculate () {
      let pid = this.projectId || (this.$route.params.id !== 'new' ? Number(this.$route.params.id) : null)
      if (!pid) {
        await this.saveProject()
        pid = this.projectId || (this.$route.params.id !== 'new' ? Number(this.$route.params.id) : null)
      }
      if (!pid) {
        alert('请先填写项目名称并保存')
        return
      }
      this.projectId = pid
      this.calculating = true
      try {
        const body = {
          riskKeys: this.selectedRisks,
          urgencyRush: this.urgencyRush,
          priceConfigId: this.priceConfigId,
          auditChecklist: { ...this.audit }
        }
        const resp = await this.$http.post('/admin/quote/projects/' + pid + '/calculate', body)
        if (resp.data && resp.data.code === 0 && resp.data.data) {
          this.calcResult = resp.data.data
          await this.loadContractIfAny()
        } else {
          alert((resp.data && resp.data.message) || '计算失败')
        }
      } catch (e) {
        alert((e.response && e.response.data && e.response.data.message) || '计算失败')
      } finally {
        this.calculating = false
      }
    },
    async downloadQuote () {
      if (!this.projectId || !this.calcResult) return
      try {
        const resp = await this.$http.post('/admin/quote/projects/' + this.projectId + '/quote-doc', {
          quoteResultId: this.calcResult.id
        })
        if (resp.data && resp.data.code === 0 && resp.data.data && resp.data.data.html) {
          const blob = new Blob([resp.data.data.html], { type: 'text/html;charset=utf-8' })
          const a = document.createElement('a')
          a.href = URL.createObjectURL(blob)
          a.download = resp.data.data.filename || 'quote.html'
          a.click()
          URL.revokeObjectURL(a.href)
        }
      } catch (e) {
        alert('生成失败')
      }
    },
    async downloadBinaryGet (path, defaultFilename) {
      try {
        const resp = await this.$http.get(path, {
          params: { quoteResultId: this.calcResult.id },
          responseType: 'blob'
        })
        const blob = resp.data
        const a = document.createElement('a')
        a.href = URL.createObjectURL(blob)
        a.download = defaultFilename
        a.click()
        URL.revokeObjectURL(a.href)
      } catch (e) {
        alert((e.response && e.response.data && e.response.data.message) || this.$t('quote.exportFail'))
      }
    },
    async downloadQuotePdf () {
      if (!this.projectId || !this.calcResult) return
      await this.downloadBinaryGet(
        '/admin/quote/projects/' + this.projectId + '/quote-doc.pdf',
        'quote-' + this.projectId + '.pdf'
      )
    },
    async downloadQuoteDocx () {
      if (!this.projectId || !this.calcResult) return
      await this.downloadBinaryGet(
        '/admin/quote/projects/' + this.projectId + '/quote-doc.docx',
        'quote-' + this.projectId + '.docx'
      )
    },
    addPaymentPhase () {
      this.contractContext.paymentPlan.push({ phaseName: '', percent: 0, triggerNote: '' })
    },
    removePaymentPhase (pi) {
      if (this.contractContext.paymentPlan.length <= 1) return
      this.contractContext.paymentPlan.splice(pi, 1)
    },
    toggleDeliverable (key, ev) {
      const arr = this.contractContext.deliverables
      if (ev.target.checked) {
        if (!arr.includes(key)) arr.push(key)
      } else {
        const i = arr.indexOf(key)
        if (i >= 0) arr.splice(i, 1)
      }
    },
    addMilestone () {
      this.contractContext.milestones.push({ name: '', offsetDays: 0, note: '' })
    },
    removeMilestone (mi) {
      if (this.contractContext.milestones.length <= 1) return
      this.contractContext.milestones.splice(mi, 1)
    },
    async downloadAttachmentHtml (url, fallbackName) {
      if (!this.projectId) return
      try {
        const resp = await this.$http.post(url)
        if (resp.data && resp.data.code === 0 && resp.data.data && resp.data.data.html) {
          const blob = new Blob([resp.data.data.html], { type: 'text/html;charset=utf-8' })
          const a = document.createElement('a')
          a.href = URL.createObjectURL(blob)
          a.download = resp.data.data.filename || fallbackName
          a.click()
          URL.revokeObjectURL(a.href)
        } else {
          alert(this.$t('quote.attachmentFail'))
        }
      } catch (e) {
        alert(this.$t('quote.attachmentFail'))
      }
    },
    downloadAttachmentFunctionList () {
      this.downloadAttachmentHtml('/admin/quote/projects/' + this.projectId + '/contract-attachments/function-list', 'attachment-1.html')
    },
    downloadAttachmentMilestones () {
      this.downloadAttachmentHtml('/admin/quote/projects/' + this.projectId + '/contract-attachments/milestones', 'attachment-3.html')
    },
    downloadAttachmentAcceptance () {
      this.downloadAttachmentHtml('/admin/quote/projects/' + this.projectId + '/contract-attachments/acceptance', 'attachment-2.html')
    },
    async runGenContract () {
      if (!this.calcResult || !this.calcResult.id) return
      this.contractGenLoading = true
      this.contractMsg = ''
      try {
        const resp = await this.$http.post('/admin/quote/results/' + this.calcResult.id + '/contract/generate', {
          clientName: this.contract.clientName,
          companyName: this.contract.companyName,
          templateType: this.contract.templateType
        })
        if (resp.data && resp.data.code === 0 && resp.data.data) {
          this.contract.editedContent = resp.data.data.editedContent || ''
          this.contractOk = true
          this.contractMsg = '已生成'
        } else {
          this.contractOk = false
          this.contractMsg = (resp.data && resp.data.message) || '失败'
        }
      } catch (e) {
        this.contractOk = false
        this.contractMsg = (e.response && e.response.data && e.response.data.message) || '失败'
      } finally {
        this.contractGenLoading = false
      }
    },
    async saveContractBody () {
      if (!this.calcResult || !this.calcResult.id) return
      this.contractMsg = ''
      try {
        const resp = await this.$http.put('/admin/quote/results/' + this.calcResult.id + '/contract', {
          editedContent: this.contract.editedContent
        })
        if (resp.data && resp.data.code === 0) {
          this.contractOk = true
          this.contractMsg = '已保存'
        } else {
          this.contractOk = false
          this.contractMsg = (resp.data && resp.data.message) || '失败'
        }
      } catch (e) {
        this.contractOk = false
        this.contractMsg = '失败'
      }
    },
    async exportContractFile () {
      if (!this.calcResult || !this.calcResult.id) return
      try {
        const resp = await this.$http.post('/admin/quote/results/' + this.calcResult.id + '/contract/export')
        if (resp.data && resp.data.code === 0 && resp.data.data && resp.data.data.html) {
          const blob = new Blob([resp.data.data.html], { type: 'text/html;charset=utf-8' })
          const a = document.createElement('a')
          a.href = URL.createObjectURL(blob)
          a.download = resp.data.data.filename || 'contract.html'
          a.click()
          URL.revokeObjectURL(a.href)
        }
      } catch (e) {
        alert('导出失败')
      }
    },
    async exportContractPdf () {
      if (!this.calcResult || !this.calcResult.id) return
      try {
        const resp = await this.$http.get('/admin/quote/results/' + this.calcResult.id + '/contract.pdf', {
          responseType: 'blob'
        })
        const blob = resp.data
        const a = document.createElement('a')
        a.href = URL.createObjectURL(blob)
        a.download = 'contract-' + this.calcResult.id + '.pdf'
        a.click()
        URL.revokeObjectURL(a.href)
      } catch (e) {
        alert((e.response && e.response.data && e.response.data.message) || this.$t('quote.exportFail'))
      }
    },
    async exportContractDocx () {
      if (!this.calcResult || !this.calcResult.id) return
      try {
        const resp = await this.$http.get('/admin/quote/results/' + this.calcResult.id + '/contract.docx', {
          responseType: 'blob'
        })
        const blob = resp.data
        const a = document.createElement('a')
        a.href = URL.createObjectURL(blob)
        a.download = 'contract-' + this.calcResult.id + '.docx'
        a.click()
        URL.revokeObjectURL(a.href)
      } catch (e) {
        alert((e.response && e.response.data && e.response.data.message) || this.$t('quote.exportFail'))
      }
    }
  }
}
</script>

<style scoped>
/* 全宽占满可视区域（仅左右留少量边距），避免窄栏居中 */
.quote-project-page {
  width: 100%;
  max-width: none;
  margin: 0;
  padding: clamp(16px, 2vw, 28px) clamp(16px, 3vw, 40px);
  box-sizing: border-box;
  color: #0f172a;
  background: #f1f5f9;
}
.head-links { display: flex; flex-wrap: wrap; align-items: center; gap: 10px 14px; margin-bottom: 6px; }
.head-sep { color: #64748b; font-size: 15px; user-select: none; }
.head-link-inline { font-size: 15px; color: #1d4ed8; text-decoration: none; font-weight: 500; }
.head-link-inline:hover { text-decoration: underline; color: #1e40af; }
.back-link { font-size: 15px; color: #1d4ed8; text-decoration: none; font-weight: 500; }
.back-link:hover { text-decoration: underline; color: #1e40af; }
.page-head h1 { margin: 10px 0 20px; font-size: clamp(1.5rem, 2vw, 1.85rem); font-weight: 700; color: #020617; letter-spacing: -0.02em; }
.card {
  background: #fff;
  border: 1px solid #cbd5e1;
  border-radius: 10px;
  padding: clamp(18px, 2vw, 28px);
  margin-bottom: 20px;
  box-shadow: 0 1px 2px rgba(15, 23, 42, 0.06);
}
.card h2 { margin: 0 0 14px; font-size: 1.125rem; font-weight: 700; color: #020617; }
.card h3 { margin: 18px 0 10px; font-size: 1rem; font-weight: 600; color: #0f172a; }
/* 宽屏下多列铺开，提高横向利用率 */
.grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(min(100%, 260px), 1fr)); gap: 14px 18px; margin-bottom: 14px; }
.grid label.full { grid-column: 1 / -1; }
.quote-doc-meta-hint { margin-top: -4px; }
.quote-subject-mode {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 12px 18px;
  margin-bottom: 14px;
  font-size: 15px;
  color: #1e293b;
}
.quote-subject-mode .mode-label { font-weight: 700; width: 100%; }
.quote-subject-mode .mode-opt { display: inline-flex; align-items: center; gap: 6px; font-weight: 500; cursor: pointer; }
.warn-banner {
  background: #fffbeb;
  border: 1px solid #fcd34d;
  color: #92400e;
  padding: 10px 12px;
  border-radius: 8px;
  font-size: 14px;
  margin-bottom: 12px;
  line-height: 1.45;
}
.quote-header-preview {
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  padding: 12px 14px;
  margin-bottom: 14px;
  font-size: 14px;
  line-height: 1.5;
  color: #334155;
}
.quote-header-preview .preview-title { font-weight: 700; color: #0f172a; margin-bottom: 8px; font-size: 13px; }
.quote-header-preview p { margin: 4px 0; }
.preview-empty { color: #64748b; font-style: italic; margin: 0 !important; }
.quote-doc-actions {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 14px;
  margin-bottom: 16px;
}
.btn-sm-pad { padding: 8px 14px; font-size: 14px; margin-top: 0; }
.link-config { font-size: 14px; color: #1d4ed8; font-weight: 600; text-decoration: none; }
.link-config:hover { text-decoration: underline; }
.block-label { display: block; margin-top: 4px; margin-bottom: 6px; font-weight: 600; }
.block-inp { width: 100%; max-width: 720px; box-sizing: border-box; }
label { display: flex; flex-direction: column; gap: 6px; font-size: 15px; color: #1e293b; font-weight: 500; }
label.block { display: block; margin-top: 10px; }
.inp {
  padding: 8px 12px;
  border: 1px solid #94a3b8;
  border-radius: 8px;
  font-size: 16px;
  line-height: 1.4;
  color: #0f172a;
  background: #fff;
}
.inp:focus,
.textarea:focus {
  outline: 2px solid #3b82f6;
  outline-offset: 1px;
  border-color: #2563eb;
}
.inp.narrow { max-width: 160px; }
.inp.qty { max-width: 88px; }
.textarea {
  width: 100%;
  box-sizing: border-box;
  padding: 12px;
  border: 1px solid #94a3b8;
  border-radius: 8px;
  font-size: 16px;
  line-height: 1.5;
  min-height: 6.5em;
  color: #0f172a;
  background: #fff;
}
.contract-ta { font-family: ui-monospace, monospace; font-size: 14px; }
/* 功能模块：区块与表头对比度加强，减轻视疲劳 */
.module-block {
  border: 1px solid #94a3b8;
  background: #f8fafc;
  padding: 16px;
  margin-bottom: 14px;
  border-radius: 8px;
}
.mod-head { display: flex; flex-wrap: wrap; gap: 10px; align-items: center; margin-bottom: 10px; }
.mod-name { flex: 1; min-width: 200px; }
.item-table-head,
.item-row {
  display: grid;
  grid-template-columns: minmax(140px, 1fr) minmax(160px, 200px) 88px 44px;
  gap: 10px;
  align-items: center;
  margin-bottom: 8px;
}
.item-table-head {
  margin-top: 6px;
  padding: 10px 4px 12px;
  border-bottom: 2px solid #64748b;
  background: linear-gradient(to bottom, #f1f5f9, #fff);
  border-radius: 6px 6px 0 0;
}
.th { font-size: 15px; font-weight: 700; color: #020617; line-height: 1.2; }
.th-label-text { flex: 0 1 auto; }
.th-with-hint {
  position: relative;
  display: flex;
  flex-direction: row;
  align-items: center;
  flex-wrap: wrap;
  gap: 6px;
}
.th-hint-icon {
  flex-shrink: 0;
  width: 22px;
  height: 22px;
  margin: 0;
  padding: 0;
  border: 1px solid #64748b;
  border-radius: 50%;
  background: #fff;
  color: #475569;
  font-size: 13px;
  font-weight: 700;
  line-height: 1;
  cursor: pointer;
  box-sizing: border-box;
}
.th-hint-icon:hover,
.th-hint-icon:focus {
  outline: none;
  border-color: #2563eb;
  color: #1d4ed8;
  box-shadow: 0 0 0 2px rgba(37, 99, 235, 0.25);
}
.th-hint-popover {
  position: absolute;
  top: 100%;
  left: 0;
  z-index: 40;
  margin-top: 6px;
  max-width: min(288px, calc(100vw - 48px));
  padding: 10px 12px;
  font-size: 13px;
  font-weight: 500;
  color: #334155;
  line-height: 1.45;
  background: #fff;
  border: 1px solid #cbd5e1;
  border-radius: 8px;
  box-shadow: 0 8px 24px rgba(15, 23, 42, 0.12);
}
.item-col-qty .th-hint-popover {
  left: auto;
  right: 0;
}
.item-row { padding: 2px 0; }
.item-col-actions { display: flex; align-items: flex-start; justify-content: center; padding-top: 2px; }
.item-row .inp.narrow { max-width: none; width: 100%; }
.item-row .inp { min-width: 0; }
.btn { padding: 10px 18px; border-radius: 8px; border: none; cursor: pointer; font-size: 16px; margin-right: 8px; margin-top: 8px; }
.btn.primary { background: #1d4ed8; color: #fff; }
.btn.primary:hover { background: #1e40af; }
.btn.secondary {
  background: #e2e8f0;
  color: #0f172a;
  border: 1px solid #94a3b8;
  font-weight: 500;
}
.btn.secondary:hover { background: #cbd5e1; }
.btn-sm {
  padding: 6px 12px;
  font-size: 14px;
  border-radius: 6px;
  border: 1px solid #64748b;
  background: #fff;
  color: #1e293b;
  cursor: pointer;
  font-weight: 500;
}
.btn-sm:hover { background: #f1f5f9; border-color: #475569; }
.btn-sm.danger { color: #991b1b; border-color: #fca5a5; background: #fff; }
.btn-sm.danger:hover { background: #fef2f2; border-color: #f87171; }
.actions { margin-bottom: 18px; }
.provision-meta p { margin: 6px 0; }
.provision-steps ul { margin: 8px 0 0; padding-left: 18px; }
.provision-steps li { margin: 4px 0; }

/* 弹窗（与协作页抽屉风格对齐的轻量实现） */
.drawer-mask {
  position: fixed;
  inset: 0;
  background: rgba(15, 23, 42, 0.55);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 3000;
  padding: 16px;
  box-sizing: border-box;
}
.drawer {
  width: min(760px, 96vw);
  max-height: 90vh;
  overflow: auto;
  background: #fff;
  border-radius: 10px;
  border: 1px solid #e2e8f0;
}
.drawer-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  padding: 12px 14px;
  border-bottom: 1px solid #e2e8f0;
}
.drawer-header h3 { margin: 0; font-size: 16px; color: #0f172a; }
.drawer-body { padding: 14px; }
.close-btn {
  border: none;
  background: transparent;
  font-size: 22px;
  line-height: 1;
  cursor: pointer;
  color: #334155;
}
.risk-grid { display: flex; flex-wrap: wrap; gap: 14px 18px; margin: 14px 0; font-size: 15px; color: #1e293b; }
.chk { flex-direction: row; align-items: center; font-weight: 500; }
.result-box {
  margin-top: 18px;
  padding: 16px;
  background: #ecfdf5;
  border: 1px solid #6ee7b7;
  border-radius: 8px;
  font-size: 16px;
  line-height: 1.55;
  color: #064e3b;
}
.result-box h3 { color: #022c22; }
.result-box strong { color: #022c22; }
.btn-row { margin-top: 10px; }
.export-row { display: flex; flex-wrap: wrap; gap: 10px; align-items: center; }
.export-row .btn { margin-top: 0; }
.ok { color: #047857; font-size: 15px; font-weight: 600; }
.err { color: #b91c1c; font-size: 15px; font-weight: 600; }
.placeholder { padding: 28px; color: #475569; font-size: 16px; font-weight: 500; }
.preset-select { min-width: 200px; max-width: min(100%, 400px); flex: 1; font-size: 15px; }
.contract-supplement-card .subh { margin: 18px 0 10px; font-size: 1rem; color: #0f172a; }
.contract-grid-2 { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; margin-top: 10px; align-items: end; }
@media (max-width: 720px) { .contract-grid-2 { grid-template-columns: 1fr; } }
.sla-block { margin-top: 4px; }
.sla-label-row { position: relative; display: inline-flex; align-items: flex-start; gap: 8px; margin-bottom: 6px; flex-wrap: wrap; }
.sla-label-text { font-weight: 600; color: #0f172a; font-size: 15px; }
.sla-hint-popover { max-width: min(360px, calc(100vw - 48px)); }
.sla-check-grid { margin-top: 4px; margin-bottom: 8px; }
.sla-extra-label { margin-top: 8px; }
.autosave-msg { margin-left: 10px; font-size: 14px; font-weight: 500; }
.autosave-hint { margin-top: 8px; }
.block-full { grid-column: 1 / -1; }
.block-inp { width: 100%; max-width: 720px; box-sizing: border-box; margin-top: 6px; }
.compact-contract .inp.wide { min-width: 160px; }
.party-b-hint { margin: -6px 0 12px; font-size: 13px; max-width: 520px; }
.module-entry-mode-row {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 12px 18px;
  margin-bottom: 14px;
  padding: 10px 12px;
  background: #f1f5f9;
  border-radius: 8px;
  border: 1px solid #e2e8f0;
}
.module-mode-label { font-weight: 600; color: #0f172a; margin-right: 4px; }
.mode-opt-inline { display: inline-flex; align-items: center; gap: 6px; font-size: 15px; color: #334155; cursor: pointer; }
.quote-ai-panel {
  margin-bottom: 18px;
  padding: 14px 16px;
  background: #eff6ff;
  border: 1px solid #93c5fd;
  border-radius: 8px;
}
.quote-ai-panel .hint { margin-top: 0; }
.ai-file-row { display: flex; flex-wrap: wrap; align-items: center; gap: 12px; margin: 10px 0; }
.ai-file-btn { position: relative; cursor: pointer; display: inline-block; margin-top: 0; }
.visually-hidden-file {
  position: absolute;
  width: 0.01px;
  height: 0.01px;
  opacity: 0;
  overflow: hidden;
  z-index: -1;
}
.ai-file-name { font-size: 14px; color: #475569; }
.ai-merge-row {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px 16px;
  margin: 12px 0;
  font-size: 15px;
  color: #1e293b;
}
.ai-merge-label { font-weight: 600; }
</style>
