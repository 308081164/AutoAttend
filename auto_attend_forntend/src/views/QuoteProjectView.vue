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
      <div class="quote-project-layout">
        <div class="quote-project-main">
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
        <div class="quote-validity-row">
          <input
            v-model.number="quoteValidityDays"
            type="number"
            min="1"
            step="1"
            class="inp quote-validity-days"
            :placeholder="$t('quote.quoteValidityDaysPh')"
          />
          <select v-model="quoteValidityDayKind" class="inp quote-validity-kind">
            <option value="natural">{{ $t('quote.quoteValidityNatural') }}</option>
            <option value="workday">{{ $t('quote.quoteValidityWorkday') }}</option>
          </select>
        </div>
        <p class="hint quote-validity-hint">{{ $t('quote.quoteValidityStructuredHint') }}</p>
        <p v-if="quoteValidityLegacyNote" class="warn-banner quote-validity-legacy">{{ $t('quote.quoteValidityLegacyHint') }}{{ quoteValidityLegacyNote }}</p>
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
          <p class="hint">{{ $t('quote.quoteExportInSidebarHint') }}</p>
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

        <h4 class="subh acceptance-tc-subh">{{ $t('quote.acceptanceTestCasesTitle') }}</h4>
        <p class="hint">{{ $t('quote.acceptanceTestCasesHint') }}</p>
        <div class="btn-row export-row acceptance-tc-toolbar">
          <button type="button" class="btn primary" :disabled="aiAcceptanceParsing" @click="runAiAcceptanceTestCases">
            {{ aiAcceptanceParsing ? $t('quote.aiAcceptanceParsing') : $t('quote.aiAcceptanceBtn') }}
          </button>
          <span class="ai-merge-label">{{ $t('quote.aiAcceptanceMergeLabel') }}</span>
          <label class="chk"><input type="radio" v-model="aiAcceptanceMergeMode" value="replace" /> {{ $t('quote.aiAcceptanceMergeReplace') }}</label>
          <label class="chk"><input type="radio" v-model="aiAcceptanceMergeMode" value="append" /> {{ $t('quote.aiAcceptanceMergeAppend') }}</label>
          <button type="button" class="btn secondary btn-sm" @click="addAcceptanceTestCase">{{ $t('quote.addAcceptanceTestCase') }}</button>
        </div>
        <p v-if="aiAcceptanceMsg" :class="aiAcceptanceOk ? 'ok' : 'err'">{{ aiAcceptanceMsg }}</p>
        <div class="table-wrap acceptance-tc-wrap">
          <table class="data-table compact-contract acceptance-tc-table">
            <thead>
              <tr>
                <th class="atc-th">{{ $t('quote.tcCaseName') }}</th>
                <th class="atc-th">{{ $t('quote.tcModule') }}</th>
                <th class="atc-th atc-th-narrow">{{ $t('quote.tcPriority') }}</th>
                <th class="atc-th">{{ $t('quote.tcPreconditions') }}</th>
                <th class="atc-th">{{ $t('quote.tcTestRole') }}</th>
                <th class="atc-th">{{ $t('quote.tcSteps') }}</th>
                <th class="atc-th">{{ $t('quote.tcExpected') }}</th>
                <th class="atc-th atc-th-narrow">{{ $t('quote.tcResult') }}</th>
                <th class="atc-th">{{ $t('quote.tcRemark') }}</th>
                <th class="atc-th atc-th-action"></th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="(trow, ti) in contractContext.acceptanceTestCases" :key="'atc-' + ti" class="acceptance-tc-tr">
                <td class="atc-td">
                  <input v-model="trow.caseName" class="atc-cell-input" :title="trow.caseName" :placeholder="$t('quote.tcCaseNamePh')" />
                </td>
                <td class="atc-td">
                  <select v-model="trow.module" class="atc-select" :title="trow.module">
                    <option value="">{{ $t('quote.tcModulePh') }}</option>
                    <option v-for="mn in quoteModuleNameOptions" :key="'mn-' + mn" :value="mn">{{ mn }}</option>
                  </select>
                </td>
                <td class="atc-td atc-td-narrow">
                  <select v-model="trow.priority" class="atc-select">
                    <option value="高">{{ $t('quote.tcPriorityHigh') }}</option>
                    <option value="中">{{ $t('quote.tcPriorityMed') }}</option>
                    <option value="低">{{ $t('quote.tcPriorityLow') }}</option>
                  </select>
                </td>
                <td class="atc-td">
                  <input v-model="trow.preconditions" class="atc-cell-input" :title="trow.preconditions" :placeholder="$t('quote.tcPreconditionsPh')" />
                </td>
                <td class="atc-td">
                  <input v-model="trow.testRole" class="atc-cell-input" :title="trow.testRole" :placeholder="$t('quote.tcTestRolePh')" />
                </td>
                <td class="atc-td">
                  <input v-model="trow.steps" class="atc-cell-input" :title="trow.steps" :placeholder="$t('quote.tcStepsPh')" />
                </td>
                <td class="atc-td">
                  <input v-model="trow.expectedResult" class="atc-cell-input" :title="trow.expectedResult" :placeholder="$t('quote.tcExpectedPh')" />
                </td>
                <td class="atc-td atc-td-narrow">
                  <select v-model="trow.testResult" class="atc-select">
                    <option value="待测试">{{ $t('quote.tcResultPending') }}</option>
                    <option value="通过">{{ $t('quote.tcResultPass') }}</option>
                    <option value="驳回">{{ $t('quote.tcResultReject') }}</option>
                  </select>
                </td>
                <td class="atc-td">
                  <input v-model="trow.remark" class="atc-cell-input" :title="trow.remark" />
                </td>
                <td class="atc-td atc-td-action"><button type="button" class="btn-sm danger" @click="removeAcceptanceTestCase(ti)">×</button></td>
              </tr>
            </tbody>
          </table>
        </div>
        <p v-if="!contractContext.acceptanceTestCases.length" class="hint">{{ $t('quote.acceptanceTestCasesEmpty') }}</p>

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
        <p class="hint">{{ $t('quote.contractAttachmentsInSidebarHint') }}</p>
        <p class="hint">{{ $t('quote.contractSupplementSaveHint') }}</p>
      </section>

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
        <p class="hint">{{ $t('quote.contractBodyInSidebarHint') }}</p>
        <label class="block">合同正文（可编辑）</label>
        <textarea v-model="contract.editedContent" class="textarea contract-ta" rows="16"></textarea>
        <p v-if="contractMsg" :class="contractOk ? 'ok' : 'err'">{{ contractMsg }}</p>
      </section>
        </div>

        <aside v-if="projectId" class="quote-output-sidebar" :class="{ collapsed: outputSidebarCollapsed }">
          <button
            type="button"
            class="sidebar-edge-toggle"
            :title="outputSidebarCollapsed ? $t('quote.outputSidebarExpand') : $t('quote.outputSidebarCollapse')"
            @click="outputSidebarCollapsed = !outputSidebarCollapsed"
          >{{ outputSidebarCollapsed ? '◀' : '▶' }}</button>
          <div v-show="!outputSidebarCollapsed" class="quote-output-sidebar-body">
            <h3 class="output-sidebar-title">{{ $t('quote.outputSidebarTitle') }}</h3>
            <p class="hint output-sidebar-hint">{{ $t('quote.outputSidebarHint') }}</p>

            <div v-if="calcResult && calcResult.id" class="output-sidebar-section">
              <h4 class="output-sidebar-subtitle">{{ $t('quote.outputSectionQuote') }}</h4>
              <div
                class="output-file-row"
                :class="{ 'is-ready': artifactReady.quoteHtml || artifactReady.quotePdf || artifactReady.quoteDocx }"
              >
                <span class="output-file-label">{{ $t('quote.outputSectionQuote') }} 导出（HTML/PDF/Word）</span>
                <div class="output-file-actions">
                  <select class="output-file-type-select" v-model="quoteDocType" :disabled="!calcResult">
                    <option value="quoteHtml">{{ $t('quote.outputQuoteHtml') }}</option>
                    <option value="quotePdf">{{ $t('quote.outputQuotePdf') }}</option>
                    <option value="quoteDocx">{{ $t('quote.outputQuoteDocx') }}</option>
                  </select>
                  <button
                    v-if="quotePreviewable"
                    type="button"
                    class="btn-tiny secondary"
                    :disabled="!calcResult"
                    @click="previewSelectedQuoteDoc"
                  >{{ $t('quote.outputPreview') }}</button>
                  <button
                    type="button"
                    class="btn-tiny primary"
                    :disabled="!calcResult"
                    @click="downloadSelectedQuoteDoc"
                  >{{ $t('quote.outputDownload') }}</button>
                </div>
              </div>
            </div>

            <div class="output-sidebar-section">
              <h4 class="output-sidebar-subtitle">{{ $t('quote.outputSectionAttachments') }}</h4>
              <div class="output-file-row" :class="{ 'is-ready': artifactReady.attFunction }">
                <span class="output-file-label">{{ $t('quote.attachmentFunctionList') }}</span>
                <div class="output-file-actions">
                  <button type="button" class="btn-tiny secondary" @click="previewAttachmentFunctionList">{{ $t('quote.outputPreview') }}</button>
                  <button type="button" class="btn-tiny primary" @click="downloadAttachmentFunctionList">{{ $t('quote.outputDownload') }}</button>
                </div>
              </div>
              <div class="output-file-row" :class="{ 'is-ready': artifactReady.attAcceptance }">
                <span class="output-file-label">{{ $t('quote.attachmentAcceptance') }}</span>
                <div class="output-file-actions">
                  <button type="button" class="btn-tiny secondary" @click="previewAttachmentAcceptance">{{ $t('quote.outputPreview') }}</button>
                  <button type="button" class="btn-tiny primary" @click="downloadAttachmentAcceptance">{{ $t('quote.outputDownload') }}</button>
                </div>
              </div>
              <div class="output-file-row" :class="{ 'is-ready': artifactReady.attMilestones }">
                <span class="output-file-label">{{ $t('quote.attachmentMilestones') }}</span>
                <div class="output-file-actions">
                  <button type="button" class="btn-tiny secondary" @click="previewAttachmentMilestones">{{ $t('quote.outputPreview') }}</button>
                  <button type="button" class="btn-tiny primary" @click="downloadAttachmentMilestones">{{ $t('quote.outputDownload') }}</button>
                </div>
              </div>
            </div>

            <div v-if="calcResult && calcResult.id" class="output-sidebar-section">
              <h4 class="output-sidebar-subtitle">{{ $t('quote.outputSectionContract') }}</h4>
              <div class="output-file-row output-file-row--full" :class="{ 'is-ready': artifactReady.contractAi }">
                <span class="output-file-label">{{ $t('quote.genContract') }}</span>
                <button type="button" class="btn-tiny primary" :disabled="contractGenLoading" @click="runGenContract">{{ contractGenLoading ? '…' : $t('quote.outputRun') }}</button>
              </div>
              <div class="output-file-row output-file-row--full" :class="{ 'is-ready': artifactReady.contractBodySaved }">
                <span class="output-file-label">{{ $t('quote.saveContract') }}</span>
                <button type="button" class="btn-tiny secondary" @click="saveContractBody">{{ $t('quote.outputRun') }}</button>
              </div>
              <div
                class="output-file-row output-file-row--full"
                :class="{ 'is-ready': artifactReady.contractHtml || artifactReady.contractPdf || artifactReady.contractDocx }"
              >
                <span class="output-file-label">
                  {{ $t('quote.outputSectionContract') }} 导出（HTML/PDF/Word）
                </span>
                <div class="output-file-actions">
                  <select class="output-file-type-select" v-model="contractDocType">
                    <option value="contractHtml">{{ $t('quote.exportContract') }}</option>
                    <option value="contractPdf">{{ $t('quote.exportContractPdf') }}</option>
                    <option value="contractDocx">{{ $t('quote.exportContractWord') }}</option>
                  </select>
                  <button
                    v-if="contractPreviewable"
                    type="button"
                    class="btn-tiny secondary"
                    @click="previewSelectedContractDoc"
                  >{{ $t('quote.outputPreview') }}</button>
                  <button
                    type="button"
                    class="btn-tiny primary"
                    @click="downloadSelectedContractDoc"
                  >{{ $t('quote.outputDownload') }}</button>
                </div>
              </div>
            </div>

            <div class="output-sidebar-section output-sidebar-section--provision">
              <h4 class="output-sidebar-subtitle">{{ $t('quote.outputSectionRepo') }}</h4>
              <p class="hint sidebar-provision-hint">{{ $t('quote.outputRepoHint') }}</p>
              <div v-if="provisionMeta.repoFullName" class="provision-meta sidebar-provision-meta">
                <p><strong>{{ $t('quote.outputRepoLink') }}</strong><a :href="provisionMeta.repoHtmlUrl" target="_blank" rel="noopener">{{ provisionMeta.repoFullName }}</a></p>
                <p><strong>{{ $t('quote.outputRepoStatus') }}</strong>{{ provisionMeta.provisionStatus || '—' }} <span v-if="provisionMeta.provisionLastError" class="err">（{{ provisionMeta.provisionLastError }}）</span></p>
                <p><strong>{{ $t('quote.outputRepoCollab') }}</strong>{{ provisionMeta.provisionSyncedToCollab ? $t('quote.outputRepoCollabYes') : $t('quote.outputRepoCollabNo') }} <span v-if="provisionMeta.provisionSyncedAt">（{{ provisionMeta.provisionSyncedAt }}）</span></p>
              </div>
              <button
                v-if="provisionMeta.repoFullName && cloneCommandText"
                type="button"
                class="btn primary btn-block-sidebar"
                @click="copyCloneCommand"
              >
                {{ provisionCloneCopied ? $t('quote.outputCopyCloneDone') : $t('quote.outputCopyClone') }}
              </button>
              <button
                v-else
                type="button"
                class="btn primary btn-block-sidebar"
                :disabled="provisioning"
                @click="openProvisionModal"
              >
                {{ provisioning ? $t('quote.outputRepoProvisioning') : $t('quote.outputRepoOpen') }}
              </button>
              <p v-if="provisionMsg" :class="provisionOk ? 'ok' : 'err'">{{ provisionMsg }}</p>
              <div v-if="provisionResult && provisionResult.steps && provisionResult.steps.length" class="provision-steps">
                <h5 class="output-sidebar-subtitle small">{{ $t('quote.outputRepoSteps') }}</h5>
                <ul>
                  <li v-for="(s, i) in provisionResult.steps" :key="'pss-' + i">
                    <strong>{{ s.key }}</strong>：<span :class="s.ok ? 'ok' : 'err'">{{ s.message }}</span>
                  </li>
                </ul>
              </div>
            </div>
          </div>
        </aside>
      </div>

      <div class="quote-save-bar">
        <div class="quote-save-bar-inner">
          <div class="quote-save-bar-left">
            <span v-if="autoSaveStatus === 'saved'" class="ok autosave-msg">{{ $t('quote.autoSaveSaved') }}</span>
            <span v-else-if="autoSaveStatus === 'error'" class="err autosave-msg">{{ $t('quote.autoSaveError') }}</span>
            <span v-if="saveMsg && !saveAllMsg" :class="saveOk ? 'ok' : 'err'">{{ saveMsg }}</span>
            <span v-if="saveAllMsg" :class="saveAllOk ? 'ok' : 'err'">{{ saveAllMsg }}</span>
          </div>
          <button type="button" class="btn secondary quote-save-all-btn" :disabled="saveAllLoading || saving || importFeatureLoading" @click="importCollabFeatureTable">
            {{ importFeatureLoading ? '…' : $t('quote.importCollabFeatureTable') }}
          </button>
          <button type="button" class="btn secondary quote-save-all-btn" :disabled="saveAllLoading || saving || linkTableLoading" @click="openLinkTableModal">
            {{ $t('quote.importFromCollabTable') }}
          </button>
          <button type="button" class="btn primary quote-save-all-btn" :disabled="saveAllLoading || saving" @click="saveAll">
            {{ saveAllLoading ? '…' : $t('quote.saveAll') }}
          </button>
        </div>
      </div>

      <!-- 从多维表带入需求 -->
      <div v-if="showLinkTableModal" class="drawer-mask" @click.self="closeLinkTableModal">
        <div class="drawer drawer-wide" @click.stop>
          <div class="drawer-header">
            <h3>{{ $t('quote.linkTableModalTitle') }}</h3>
            <button class="close-btn" @click="closeLinkTableModal">×</button>
          </div>
          <div class="drawer-body">
            <p class="hint">{{ $t('quote.linkTableModalHint') }}</p>
            <div v-if="linkTableLoading" class="placeholder">{{ $t('quote.loading') }}</div>
            <div v-else-if="!linkTableRows.length" class="hint">{{ $t('quote.linkTableEmpty') }}</div>
            <div v-else class="link-table-list">
              <label v-for="row in linkTableRows" :key="'ltr-' + row.recordId" class="link-table-row">
                <input type="checkbox" :value="row.recordId" v-model="linkTableSelectedIds" />
                <span class="link-table-title">{{ row.title }}</span>
                <span v-if="row.module" class="link-table-mod">{{ row.module }}</span>
              </label>
            </div>
            <div class="btn-row export-row" style="margin-top:16px">
              <button type="button" class="btn primary" :disabled="linkTableApplying || !linkTableSelectedCount" @click="applyLinkTableImport">
                {{ linkTableApplying ? '…' : $t('quote.linkTableApply') }}
              </button>
              <button type="button" class="btn secondary" @click="closeLinkTableModal">{{ $t('quote.provisionCancel') }}</button>
            </div>
          </div>
        </div>
      </div>

      <!-- 创建仓库弹窗 -->
      <div v-if="showProvisionModal" class="drawer-mask" @click="closeProvisionModal">
        <div class="drawer" @click.stop>
          <div class="drawer-header">
            <h3>{{ $t('quote.provisionModalTitle') }}</h3>
            <button class="close-btn" @click="closeProvisionModal">×</button>
          </div>
          <div class="drawer-body">
            <div class="grid">
              <label class="full">{{ $t('quote.provisionRepoName') }} <input v-model="provisionForm.repoName" class="inp wide" :placeholder="$t('quote.provisionRepoNamePh')" /></label>
              <label>{{ $t('quote.provisionVisibility') }}
                <select v-model="provisionForm.repoPrivate" class="inp">
                  <option :value="true">Private</option>
                  <option :value="false">Public</option>
                </select>
              </label>
              <label class="full">{{ $t('quote.provisionDesc') }} <input v-model="provisionForm.description" class="inp wide" :placeholder="$t('quote.provisionDescPh')" /></label>
            </div>
            <div class="risk-grid">
              <label class="chk"><input type="checkbox" v-model="provisionForm.autoInit" /> {{ $t('quote.provisionOptReadme') }}</label>
              <label class="chk"><input type="checkbox" v-model="provisionForm.syncMd" /> {{ $t('quote.provisionOptMd') }}</label>
              <label class="chk"><input type="checkbox" v-model="provisionForm.syncCollabTable" /> {{ $t('quote.provisionOptCollab') }}</label>
              <label class="chk"><input type="checkbox" v-model="provisionForm.createWebhook" /> {{ $t('quote.provisionOptWebhook') }}</label>
            </div>
            <div class="btn-row export-row">
              <button type="button" class="btn primary" :disabled="provisioning" @click="runProvision">
                {{ provisioning ? $t('quote.provisionRunning') : $t('quote.provisionStart') }}
              </button>
              <button type="button" class="btn secondary" :disabled="provisioning" @click="closeProvisionModal">{{ $t('quote.provisionCancel') }}</button>
            </div>
            <p class="hint" style="margin-top:10px">{{ $t('quote.provisionFootnote') }}</p>
          </div>
        </div>
      </div>
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

/** 工作日 4h / 8h 响应互斥：只保留数组中先出现的那一项 */
function dedupeMutexWorkdayResponseKeys (keys) {
  const out = [...keys]
  const i4 = out.indexOf('workday_4h')
  const i8 = out.indexOf('workday_8h')
  if (i4 >= 0 && i8 >= 0) {
    if (i4 < i8) out.splice(i8, 1)
    else out.splice(i4, 1)
  }
  return out
}

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
    return { maintenanceSlaKeys: dedupeMutexWorkdayResponseKeys([...DEFAULT_SLA_KEYS]), maintenanceSlaExtra: extra }
  }
  return { maintenanceSlaKeys: dedupeMutexWorkdayResponseKeys(keys), maintenanceSlaExtra: extra }
}

function emptyAcceptanceTestCaseRow () {
  return {
    caseName: '',
    module: '',
    priority: '中',
    preconditions: '',
    testRole: '',
    steps: '',
    expectedResult: '',
    testResult: '待测试',
    remark: ''
  }
}

function normalizeAcceptanceTestCaseRow (raw) {
  if (!raw || typeof raw !== 'object') return emptyAcceptanceTestCaseRow()
  const trOk = ['待测试', '通过', '驳回']
  let testResult = raw.testResult != null ? String(raw.testResult).trim() : '待测试'
  if (!trOk.includes(testResult)) testResult = '待测试'
  const prOk = ['高', '中', '低']
  let priority = raw.priority != null ? String(raw.priority).trim() : '中'
  if (!prOk.includes(priority)) priority = '中'
  return {
    caseName: raw.caseName != null ? String(raw.caseName) : '',
    module: raw.module != null ? String(raw.module) : '',
    priority,
    preconditions: raw.preconditions != null ? String(raw.preconditions) : '',
    testRole: raw.testRole != null ? String(raw.testRole) : '',
    steps: raw.steps != null ? String(raw.steps) : '',
    expectedResult: raw.expectedResult != null ? String(raw.expectedResult) : '',
    testResult,
    remark: raw.remark != null ? String(raw.remark) : ''
  }
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
    acceptanceTestCases: [],
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
  const atc = Array.isArray(raw.acceptanceTestCases)
    ? raw.acceptanceTestCases.map(normalizeAcceptanceTestCaseRow)
    : (base.acceptanceTestCases || [])
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
    acceptanceTestCases: atc,
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
        quoteContactInfo: ''
      },
      quoteValidityDays: '',
      quoteValidityDayKind: 'natural',
      quoteValidityLegacyNote: '',
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
      provisionCloneCopied: false,
      outputSidebarCollapsed: false,
      artifactReady: {
        quoteHtml: false,
        quotePdf: false,
        quoteDocx: false,
        attFunction: false,
        attAcceptance: false,
        attMilestones: false,
        contractAi: false,
        contractBodySaved: false,
        contractHtml: false,
        contractPdf: false,
        contractDocx: false
      },
      quoteDocType: 'quoteHtml',
      contractDocType: 'contractHtml',
      saveAllLoading: false,
      saveAllMsg: '',
      saveAllOk: false,
      showLinkTableModal: false,
      linkTableLoading: false,
      linkTableRows: [],
      linkTableSelectedIds: [],
      linkTableApplying: false,
      importFeatureLoading: false,
      importFeatureMsg: '',
      contractContext: defaultContractContext(),
      /** 功能模块：manual | ai */
      moduleEntryMode: 'manual',
      // textarea 内显示的“本次要提交给 AI 的输入”（replace 模式下为完整原文；append 模式下仅显示新增部分）
      aiRequirementText: '',
      // 后端持久化保存的完整 AI 原文（append 模式下会追加到末尾）
      aiRequirementPersistedText: '',
      aiMergeMode: 'replace',
      aiModuleParsing: false,
      aiModuleMsg: '',
      aiModuleOk: false,
      aiAcceptanceParsing: false,
      aiAcceptanceMsg: '',
      aiAcceptanceOk: false,
      aiAcceptanceMergeMode: 'replace',
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
    quotePreviewable () {
      return this.quoteDocType === 'quoteHtml' || this.quoteDocType === 'quotePdf'
    },
    contractPreviewable () {
      return this.contractDocType === 'contractHtml' || this.contractDocType === 'contractPdf'
    },
    linkTableSelectedCount () {
      return Array.isArray(this.linkTableSelectedIds) ? this.linkTableSelectedIds.length : 0
    },
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
    },
    /** 验收测试用例「归属模块」下拉：来自当前功能模块名称 */
    quoteModuleNameOptions () {
      const names = []
      const seen = new Set()
      for (const m of this.modules || []) {
        const n = (m.name && String(m.name).trim()) ? String(m.name).trim() : ''
        if (n && !seen.has(n)) {
          seen.add(n)
          names.push(n)
        }
      }
      return names
    },
    /** git clone 指令（HTTPS），用于侧栏一键复制 */
    cloneCommandText () {
      const name = (this.provisionMeta.repoFullName || '').trim()
      let u = (this.provisionMeta.repoHtmlUrl || '').trim()
      if (!u && name) u = 'https://github.com/' + name
      if (!u) return ''
      const base = u.endsWith('.git') ? u.replace(/\.git$/i, '') : u.replace(/\/$/, '')
      return 'git clone ' + base + '.git'
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
    aiMergeMode (val) {
      // append 模式下不展示历史原文：只展示新增部分
      if (val === 'append') {
        this.aiRequirementText = ''
      } else {
        this.aiRequirementText = this.aiRequirementPersistedText || ''
      }
    },
    aiRequirementText (val) {
      // replace 模式下：textarea 内容即为完整原文，需同步到 persisted，保证后续 append 的“原有文本”准确
      if (this.aiMergeMode === 'replace') {
        this.aiRequirementPersistedText = val || ''
      }
    },
    quoteValidityDays (val) {
      const n = Number(val)
      if (Number.isFinite(n) && n > 0) this.quoteValidityLegacyNote = ''
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
            quoteContactInfo: ''
          }
          this.quoteValidityDays = ''
          this.quoteValidityDayKind = 'natural'
          this.quoteValidityLegacyNote = ''
          this.modules = [emptyModule()]
          this.contractContext = normalizeContractContext(null)
          this.aiRequirementText = ''
          this.aiRequirementPersistedText = ''
          this.aiMergeMode = 'replace'
          this.resetCalcPrefsUi()
          this.resetArtifactReady()
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
          this.resetArtifactReady()
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
            this.aiRequirementPersistedText = d.aiRequirementText || ''
            this.aiRequirementText = this.aiMergeMode === 'append' ? '' : (this.aiRequirementPersistedText || '')
            this.form.quoteSubjectMode = d.quoteSubjectMode || 'legal_entity'
            this.form.quoteVendorName = d.quoteVendorName || ''
            this.form.quoteContactInfo = d.quoteContactInfo || ''
            this.applyQuoteValidityFromServer(d.quoteValidityNote || '')
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
            await this.syncArtifactReadyFromServer()
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
      return name ? `流帮Project 报价项目：${name}` : '流帮Project 报价项目'
    },
    resetArtifactReady () {
      const keys = Object.keys(this.artifactReady || {})
      keys.forEach(k => this.$set(this.artifactReady, k, false))
    },
    markArtifactReady (key) {
      if (!key || this.artifactReady == null || !Object.prototype.hasOwnProperty.call(this.artifactReady, key)) return
      this.$set(this.artifactReady, key, true)
    },
    async syncArtifactReadyFromServer () {
      const hasEdited = (this.contract.editedContent || '').trim().length > 0
      if (hasEdited) {
        // contractAi / contractBodySaved 来自「合同草稿」而不是文件导出落库
        this.markArtifactReady('contractAi')
        this.markArtifactReady('contractBodySaved')
      }

      // quote / contract 的 HTML/PDF/DOCX 落库状态：由服务端基于 biz_quote_document 归档结果驱动
      if (this.calcResult && this.calcResult.id) {
        try {
          const resp = await this.$http.get(`/admin/quote/results/${this.calcResult.id}/artifacts`)
          if (resp.data && resp.data.code === 0 && resp.data.data) {
            const d = resp.data.data
            if (d.quoteHtml) this.markArtifactReady('quoteHtml')
            if (d.quotePdf) this.markArtifactReady('quotePdf')
            if (d.quoteDocx) this.markArtifactReady('quoteDocx')

            if (d.contractHtml) this.markArtifactReady('contractHtml')
            if (d.contractPdf) this.markArtifactReady('contractPdf')
            if (d.contractDocx) this.markArtifactReady('contractDocx')
          }
        } catch (e) {
          // 失败不阻断页面：最多影响绿色状态判断
        }
      }
    },
    async openLinkTableModal () {
      if (!this.projectId) return
      this.showLinkTableModal = true
      this.linkTableLoading = true
      this.linkTableRows = []
      this.linkTableSelectedIds = []
      try {
        const resp = await this.$http.get('/admin/quote/projects/' + this.projectId + '/link-table-requirements')
        if (resp.data && resp.data.code === 0 && Array.isArray(resp.data.data)) {
          this.linkTableRows = resp.data.data
        } else {
          this.linkTableRows = []
        }
      } catch (e) {
        this.linkTableRows = []
        alert((e.response && e.response.data && e.response.data.message) || this.$t('quote.linkTableLoadFail'))
      } finally {
        this.linkTableLoading = false
      }
    },
    closeLinkTableModal () {
      this.showLinkTableModal = false
    },
    async applyLinkTableImport () {
      const ids = (this.linkTableSelectedIds || []).map(id => Number(id)).filter(id => id > 0)
      if (!ids.length || !this.projectId) return
      this.linkTableApplying = true
      try {
        const resp = await this.$http.post('/admin/quote/projects/' + this.projectId + '/link-table-requirements/apply', { recordIds: ids })
        if (resp.data && resp.data.code === 0) {
          const n = (resp.data.data && resp.data.data.importedCount) || 0
          await this.loadProject(this.projectId)
          alert(this.$t('quote.linkTableApplyOk', { n }))
          this.closeLinkTableModal()
        } else {
          alert((resp.data && resp.data.message) || this.$t('quote.linkTableApplyFail'))
        }
      } catch (e) {
        alert((e.response && e.response.data && e.response.data.message) || this.$t('quote.linkTableApplyFail'))
      } finally {
        this.linkTableApplying = false
      }
    },
    async importCollabFeatureTable () {
      if (!this.projectId) return
      this.importFeatureLoading = true
      try {
        const resp = await this.$http.post('/admin/quote/projects/' + this.projectId + '/collab/import-feature-table')
        if (resp.data && resp.data.code === 0 && resp.data.data) {
          const cp = resp.data.data.collabProjectId
          alert(this.$t('quote.importFeatureOk'))
          this.$router.push({ name: 'collab-table', params: { projectId: String(cp) }, query: { purpose: 'feature_backlog' } })
        } else {
          alert((resp.data && resp.data.message) || this.$t('quote.importFeatureFail'))
        }
      } catch (e) {
        alert((e.response && e.response.data && e.response.data.message) || this.$t('quote.importFeatureFail'))
      } finally {
        this.importFeatureLoading = false
      }
    },
    async saveAll () {
      if (this.pageLoading) return
      if (this.autoSaveDebounceTimer) {
        clearTimeout(this.autoSaveDebounceTimer)
        this.autoSaveDebounceTimer = null
      }
      if (this.calcPrefsDebounceTimer) {
        clearTimeout(this.calcPrefsDebounceTimer)
        this.calcPrefsDebounceTimer = null
      }
      this.saveAllLoading = true
      this.saveAllMsg = ''
      this.saveAllOk = false
      this.saveMsg = ''
      try {
        await this.saveProject()
        if (!this.saveOk) {
          this.saveAllOk = false
          this.saveAllMsg = this.saveMsg || this.$t('quote.saveAllFail')
          return
        }
        if (this.projectId) {
          await this.$http.patch('/admin/quote/projects/' + this.projectId + '/calc-prefs', this.buildQuoteCalcPrefsPayload())
        }
        let contractErr = false
        if (this.calcResult && this.calcResult.id) {
          try {
            const resp = await this.$http.put('/admin/quote/results/' + this.calcResult.id + '/contract', {
              editedContent: this.contract.editedContent
            })
            if (!resp.data || resp.data.code !== 0) contractErr = true
            else {
              this.markArtifactReady('contractBodySaved')
            }
          } catch (e) {
            contractErr = true
          }
        }
        this.lastAutoSavedSnapshot = JSON.stringify(this.payload())
        if (contractErr) {
          this.saveAllOk = false
          this.saveAllMsg = this.$t('quote.saveAllPartial')
        } else {
          this.saveAllOk = true
          this.saveAllMsg = this.$t('quote.saveAllOk')
        }
      } catch (e) {
        this.saveAllOk = false
        this.saveAllMsg = (e.response && e.response.data && e.response.data.message) || this.$t('quote.saveAllFail')
      } finally {
        this.saveAllLoading = false
      }
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
    copyCloneCommand () {
      const t = this.cloneCommandText
      if (!t) return
      const done = () => {
        this.provisionCloneCopied = true
        setTimeout(() => { this.provisionCloneCopied = false }, 2000)
      }
      if (navigator.clipboard && navigator.clipboard.writeText) {
        navigator.clipboard.writeText(t).then(done).catch(() => this.fallbackCopyClone(t, done))
      } else {
        this.fallbackCopyClone(t, done)
      }
    },
    fallbackCopyClone (text, done) {
      const ta = document.createElement('textarea')
      ta.value = text
      document.body.appendChild(ta)
      ta.select()
      try {
        document.execCommand('copy')
        if (typeof done === 'function') done()
      } catch (e) { /* ignore */ }
      document.body.removeChild(ta)
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
    applyQuoteValidityFromServer (raw) {
      const s = (raw || '').trim()
      this.quoteValidityLegacyNote = ''
      const zh = s.match(/自本报价单出具之日起\s*(\d+)\s*个\s*(自然日|工作日)/)
      if (zh) {
        this.quoteValidityDays = parseInt(zh[1], 10)
        this.quoteValidityDayKind = zh[2] === '工作日' ? 'workday' : 'natural'
        return
      }
      const en = s.match(/^Valid for\s+(\d+)\s+(calendar days|working days)\s+from the date of this quotation\.?$/i)
      if (en) {
        this.quoteValidityDays = parseInt(en[1], 10)
        this.quoteValidityDayKind = /working/i.test(en[2]) ? 'workday' : 'natural'
        return
      }
      if (s) this.quoteValidityLegacyNote = s
      this.quoteValidityDays = ''
      this.quoteValidityDayKind = 'natural'
    },
    buildQuoteValidityNoteForPayload () {
      const n = Number(this.quoteValidityDays)
      if (Number.isFinite(n) && n > 0) {
        const isEn = this.$i18n && this.$i18n.locale === 'en'
        if (isEn) {
          const unit = this.quoteValidityDayKind === 'workday' ? 'working days' : 'calendar days'
          return `Valid for ${n} ${unit} from the date of this quotation.`
        }
        const unit = this.quoteValidityDayKind === 'workday' ? '工作日' : '自然日'
        return `自本报价单出具之日起 ${n} 个${unit}`
      }
      return (this.quoteValidityLegacyNote || '').trim()
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
        aiRequirementText: this.aiMergeMode === 'append' ? (this.aiRequirementPersistedText || '') : (this.aiRequirementText || ''),
        quoteSubjectMode: this.form.quoteSubjectMode || 'legal_entity',
        quoteVendorName: this.form.quoteVendorName || '',
        quoteContactInfo: this.form.quoteContactInfo || '',
        quoteValidityNote: this.buildQuoteValidityNoteForPayload(),
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
          acceptanceTestCases: (this.contractContext.acceptanceTestCases || []).map(normalizeAcceptanceTestCaseRow),
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
        if (key === 'workday_4h' || key === 'workday_8h') {
          const other = key === 'workday_4h' ? 'workday_8h' : 'workday_4h'
          const j = arr.indexOf(other)
          if (j >= 0) arr.splice(j, 1)
        }
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
    addAcceptanceTestCase () {
      const row = emptyAcceptanceTestCaseRow()
      const opts = this.quoteModuleNameOptions
      if (opts.length) row.module = opts[0]
      if (!Array.isArray(this.contractContext.acceptanceTestCases)) {
        this.$set(this.contractContext, 'acceptanceTestCases', [])
      }
      this.contractContext.acceptanceTestCases.push(row)
    },
    removeAcceptanceTestCase (ti) {
      if (!Array.isArray(this.contractContext.acceptanceTestCases)) return
      this.contractContext.acceptanceTestCases.splice(ti, 1)
    },
    /** 与 payload().aiRequirementText 对齐；追加模式下合并已持久化与当前输入框中的未提交片段 */
    buildAiModuleOriginalTextForAi () {
      if (this.aiMergeMode === 'append') {
        const p = (this.aiRequirementPersistedText || '').trim()
        const t = (this.aiRequirementText || '').trim()
        if (p && t) return p + '\n\n' + t
        return p || t
      }
      return (this.aiRequirementText || '').trim()
    },
    modulesPayloadForAi () {
      return this.modules.map((m, mi) => ({
        name: m.name,
        sortOrder: m.sortOrder != null ? m.sortOrder : mi,
        items: (m.items || []).filter(it => it.name && String(it.name).trim()).map(it => ({
          name: String(it.name).trim(),
          complexity: it.complexity || 'standard',
          quantity: Math.max(1, Number(it.quantity) || 1)
        }))
      })).filter(m => m.name && String(m.name).trim() && m.items.length)
    },
    async runAiAcceptanceTestCases () {
      const modules = this.modulesPayloadForAi()
      if (!modules.length) {
        this.aiAcceptanceOk = false
        this.aiAcceptanceMsg = this.$t('quote.aiAcceptanceNoModules')
        return
      }
      if (!this.projectId) {
        this.aiAcceptanceOk = false
        this.aiAcceptanceMsg = this.$t('quote.aiAcceptanceFail')
        return
      }

      this.aiAcceptanceParsing = true
      this.aiAcceptanceMsg = ''
      this.aiAcceptanceOk = false
      try {
        const body = {
          projectType: this.form.projectType,
          techStack: this.form.techStack,
          designType: this.form.designType,
          dataMigration: this.form.dataMigration,
          concurrency: this.form.concurrency,
          securityLevel: this.form.securityLevel,
          deployType: this.form.deployType,
          prdSummary: this.form.prdSummary || '',
          aiRequirementText: this.buildAiModuleOriginalTextForAi(),
          acceptanceCriteriaNote: (this.contractContext.acceptanceCriteriaNote || '').trim(),
          modules
        }

        // 1) 入队：返回 jobId（避免 nginx 504）
        const enqueueResp = await this.$http.post(`/admin/quote/projects/${this.projectId}/ai/acceptance-test-cases/jobs`, body)
        const jobId = enqueueResp && enqueueResp.data && enqueueResp.data.code === 0 && enqueueResp.data.data
          ? enqueueResp.data.data.jobId
          : null
        if (!jobId) {
          this.aiAcceptanceOk = false
          this.aiAcceptanceMsg = (enqueueResp && enqueueResp.data && enqueueResp.data.message) || this.$t('quote.aiAcceptanceFail')
          return
        }

        // 2) 轮询：直到成功/失败
        const jobBase = `/admin/quote/projects/${this.projectId}/ai/acceptance-test-cases/jobs/`
        const maxAttempts = 180
        const intervalMs = 1500
        for (let i = 0; i < maxAttempts; i++) {
          const sResp = await this.$http.get(jobBase + jobId)
          const payload = sResp && sResp.data && sResp.data.code === 0 ? sResp.data.data : null
          if (payload) {
            if (payload.status === 'success') {
              const rawList = Array.isArray(payload.acceptanceTestCases) ? payload.acceptanceTestCases : []
              const list = rawList.map(normalizeAcceptanceTestCaseRow)
                .filter(r => r.caseName && String(r.caseName).trim())
              if (!list.length) {
                this.aiAcceptanceOk = false
                this.aiAcceptanceMsg = this.$t('quote.aiAcceptanceNoResult')
                return
              }
              if (!Array.isArray(this.contractContext.acceptanceTestCases)) {
                this.$set(this.contractContext, 'acceptanceTestCases', [])
              }
              if (this.aiAcceptanceMergeMode === 'append') {
                this.contractContext.acceptanceTestCases = this.contractContext.acceptanceTestCases.concat(list)
              } else {
                this.contractContext.acceptanceTestCases = list
              }
              this.aiAcceptanceOk = true
              this.aiAcceptanceMsg = this.$t('quote.aiAcceptanceOk', { n: this.contractContext.acceptanceTestCases.length })
              return
            }
            if (payload.status === 'failed') {
              this.aiAcceptanceOk = false
              this.aiAcceptanceMsg = payload.errorMessage || this.$t('quote.aiAcceptanceFail')
              return
            }
          }
          await new Promise(resolve => setTimeout(resolve, intervalMs))
        }
        this.aiAcceptanceOk = false
        this.aiAcceptanceMsg = this.$t('quote.aiAcceptanceFail') + '（轮询超时）'
      } catch (e) {
        this.aiAcceptanceOk = false
        this.aiAcceptanceMsg = (e.response && e.response.data && e.response.data.message) || this.$t('quote.aiAcceptanceFail')
      } finally {
        this.aiAcceptanceParsing = false
      }
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
            // append：把“新增输入”追加到已持久化原文后面；并且执行后不展示历史原文（textarea 清空）
            const base = (this.aiRequirementPersistedText || '').trim()
            const combined = base ? (base + '\n\n' + text) : text
            this.aiRequirementPersistedText = combined.trim()
            this.aiRequirementText = ''
            const baseModules = this.modules.filter(m => (m.name && String(m.name).trim()) || (m.items || []).some(it => it.name && String(it.name).trim()))
            this.modules = baseModules.concat(normalized)
          } else {
            // replace：直接用当前 textarea 作为新的完整原文
            this.aiRequirementPersistedText = text
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
          await this.syncArtifactReadyFromServer()
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
          await this.syncArtifactReadyFromServer()
        }
      } catch (e) {
        alert('生成失败')
      }
    },
    async previewQuoteHtml () {
      if (!this.projectId || !this.calcResult) return
      try {
        const resp = await this.$http.post('/admin/quote/projects/' + this.projectId + '/quote-doc', {
          quoteResultId: this.calcResult.id
        })
        if (resp.data && resp.data.code === 0 && resp.data.data && resp.data.data.html) {
          const blob = new Blob([resp.data.data.html], { type: 'text/html;charset=utf-8' })
          const u = URL.createObjectURL(blob)
          window.open(u, '_blank', 'noopener')
          setTimeout(() => URL.revokeObjectURL(u), 120000)
          await this.syncArtifactReadyFromServer()
        }
      } catch (e) {
        alert('生成失败')
      }
    },
    async downloadBinaryGet (path, defaultFilename, artifactKey) {
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
        if (artifactKey) this.markArtifactReady(artifactKey)
      } catch (e) {
        alert((e.response && e.response.data && e.response.data.message) || this.$t('quote.exportFail'))
      }
    },
    async previewQuotePdf () {
      if (!this.projectId || !this.calcResult) return
      try {
        const resp = await this.$http.get('/admin/quote/projects/' + this.projectId + '/quote-doc.pdf', {
          params: { quoteResultId: this.calcResult.id },
          responseType: 'blob'
        })
        const u = URL.createObjectURL(resp.data)
        window.open(u, '_blank', 'noopener')
        setTimeout(() => URL.revokeObjectURL(u), 120000)
        await this.syncArtifactReadyFromServer()
      } catch (e) {
        alert((e.response && e.response.data && e.response.data.message) || this.$t('quote.exportFail'))
      }
    },
    async downloadQuotePdf () {
      if (!this.projectId || !this.calcResult) return
      await this.downloadBinaryGet(
        '/admin/quote/projects/' + this.projectId + '/quote-doc.pdf',
        'quote-' + this.projectId + '.pdf',
        null
      )
      await this.syncArtifactReadyFromServer()
    },
    async downloadQuoteDocx () {
      if (!this.projectId || !this.calcResult) return
      await this.downloadBinaryGet(
        '/admin/quote/projects/' + this.projectId + '/quote-doc.docx',
        'quote-' + this.projectId + '.docx',
        null
      )
      await this.syncArtifactReadyFromServer()
    },
    previewSelectedQuoteDoc () {
      if (this.quoteDocType === 'quoteHtml') return this.previewQuoteHtml()
      if (this.quoteDocType === 'quotePdf') return this.previewQuotePdf()
      return null
    },
    downloadSelectedQuoteDoc () {
      if (this.quoteDocType === 'quoteHtml') return this.downloadQuote()
      if (this.quoteDocType === 'quotePdf') return this.downloadQuotePdf()
      if (this.quoteDocType === 'quoteDocx') return this.downloadQuoteDocx()
      return null
    },
    previewSelectedContractDoc () {
      if (this.contractDocType === 'contractHtml') return this.previewContractHtml()
      if (this.contractDocType === 'contractPdf') return this.previewContractPdf()
      return null
    },
    downloadSelectedContractDoc () {
      if (this.contractDocType === 'contractHtml') return this.exportContractFile()
      if (this.contractDocType === 'contractPdf') return this.exportContractPdf()
      if (this.contractDocType === 'contractDocx') return this.exportContractDocx()
      return null
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
    async downloadAttachmentHtml (url, fallbackName, artifactKey) {
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
          if (artifactKey) this.markArtifactReady(artifactKey)
        } else {
          alert(this.$t('quote.attachmentFail'))
        }
      } catch (e) {
        alert(this.$t('quote.attachmentFail'))
      }
    },
    async previewAttachmentHtml (url, artifactKey) {
      if (!this.projectId) return
      try {
        const resp = await this.$http.post(url)
        if (resp.data && resp.data.code === 0 && resp.data.data && resp.data.data.html) {
          const blob = new Blob([resp.data.data.html], { type: 'text/html;charset=utf-8' })
          const u = URL.createObjectURL(blob)
          window.open(u, '_blank', 'noopener')
          setTimeout(() => URL.revokeObjectURL(u), 120000)
          if (artifactKey) this.markArtifactReady(artifactKey)
        } else {
          alert(this.$t('quote.attachmentFail'))
        }
      } catch (e) {
        alert(this.$t('quote.attachmentFail'))
      }
    },
    downloadAttachmentFunctionList () {
      this.downloadAttachmentHtml('/admin/quote/projects/' + this.projectId + '/contract-attachments/function-list', 'attachment-1.html', 'attFunction')
    },
    previewAttachmentFunctionList () {
      this.previewAttachmentHtml('/admin/quote/projects/' + this.projectId + '/contract-attachments/function-list', 'attFunction')
    },
    downloadAttachmentMilestones () {
      this.downloadAttachmentHtml('/admin/quote/projects/' + this.projectId + '/contract-attachments/milestones', 'attachment-3.html', 'attMilestones')
    },
    previewAttachmentMilestones () {
      this.previewAttachmentHtml('/admin/quote/projects/' + this.projectId + '/contract-attachments/milestones', 'attMilestones')
    },
    downloadAttachmentAcceptance () {
      this.downloadAttachmentHtml('/admin/quote/projects/' + this.projectId + '/contract-attachments/acceptance', 'attachment-2.html', 'attAcceptance')
    },
    previewAttachmentAcceptance () {
      this.previewAttachmentHtml('/admin/quote/projects/' + this.projectId + '/contract-attachments/acceptance', 'attAcceptance')
    },
    async runGenContract () {
      if (!this.calcResult || !this.calcResult.id) return
      this.contractGenLoading = true
      this.contractMsg = ''
      try {
        // 1) 入队：返回 jobId（避免 nginx 504）
        const enqueueResp = await this.$http.post(`/admin/quote/results/${this.calcResult.id}/contract/generate/jobs`, {
          clientName: this.contract.clientName,
          companyName: this.contract.companyName,
          templateType: this.contract.templateType
        })
        const jobId = enqueueResp && enqueueResp.data && enqueueResp.data.code === 0 && enqueueResp.data.data
          ? enqueueResp.data.data.jobId
          : null
        if (!jobId) {
          this.contractOk = false
          this.contractMsg = (enqueueResp && enqueueResp.data && enqueueResp.data.message) || '失败'
          return
        }

        // 2) 轮询：直到成功/失败
        const jobBase = `/admin/quote/results/${this.calcResult.id}/contract/generate/jobs/`
        const maxAttempts = 180
        const intervalMs = 1500
        for (let i = 0; i < maxAttempts; i++) {
          const sResp = await this.$http.get(jobBase + jobId)
          const payload = sResp && sResp.data && sResp.data.code === 0 ? sResp.data.data : null
          if (payload) {
            if (payload.status === 'success') {
              this.contract.editedContent = payload.editedContent || ''
              this.contractOk = true
              this.contractMsg = '已生成'
              this.markArtifactReady('contractAi')
              return
            }
            if (payload.status === 'failed') {
              this.contractOk = false
              this.contractMsg = payload.errorMessage || '失败'
              return
            }
          }
          await new Promise(resolve => setTimeout(resolve, intervalMs))
        }
        this.contractOk = false
        this.contractMsg = '失败（轮询超时）'
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
          this.markArtifactReady('contractBodySaved')
        } else {
          this.contractOk = false
          this.contractMsg = (resp.data && resp.data.message) || '失败'
        }
      } catch (e) {
        this.contractOk = false
        this.contractMsg = '失败'
      }
    },
    async previewContractHtml () {
      if (!this.calcResult || !this.calcResult.id) return
      try {
        const resp = await this.$http.post('/admin/quote/results/' + this.calcResult.id + '/contract/export')
        if (resp.data && resp.data.code === 0 && resp.data.data && resp.data.data.html) {
          const blob = new Blob([resp.data.data.html], { type: 'text/html;charset=utf-8' })
          const u = URL.createObjectURL(blob)
          window.open(u, '_blank', 'noopener')
          setTimeout(() => URL.revokeObjectURL(u), 120000)
          await this.syncArtifactReadyFromServer()
        }
      } catch (e) {
        alert('导出失败')
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
          await this.syncArtifactReadyFromServer()
        }
      } catch (e) {
        alert('导出失败')
      }
    },
    async previewContractPdf () {
      if (!this.calcResult || !this.calcResult.id) return
      try {
        const resp = await this.$http.get('/admin/quote/results/' + this.calcResult.id + '/contract.pdf', {
          responseType: 'blob'
        })
        const u = URL.createObjectURL(resp.data)
        window.open(u, '_blank', 'noopener')
        setTimeout(() => URL.revokeObjectURL(u), 120000)
        await this.syncArtifactReadyFromServer()
      } catch (e) {
        alert((e.response && e.response.data && e.response.data.message) || this.$t('quote.exportFail'))
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
        await this.syncArtifactReadyFromServer()
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
        await this.syncArtifactReadyFromServer()
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
  padding-bottom: clamp(88px, 12vh, 120px);
  box-sizing: border-box;
  color: #0f172a;
  background: #f1f5f9;
}
.quote-project-layout {
  display: flex;
  align-items: flex-start;
  gap: 20px;
  width: 100%;
}
.quote-project-main {
  flex: 1;
  min-width: 0;
}
.quote-output-sidebar {
  position: sticky;
  top: 16px;
  width: 300px;
  flex-shrink: 0;
  max-height: calc(100vh - 100px);
  overflow: hidden;
  display: flex;
  flex-direction: column;
  background: #fff;
  border: 1px solid #cbd5e1;
  border-radius: 10px;
  box-shadow: 0 1px 3px rgba(15, 23, 42, 0.08);
  z-index: 2;
}
.quote-output-sidebar.collapsed {
  width: 44px;
  min-width: 44px;
}
.quote-output-sidebar.collapsed .sidebar-edge-toggle {
  border-radius: 10px;
  width: 100%;
  min-height: 120px;
}
.sidebar-edge-toggle {
  flex-shrink: 0;
  width: 100%;
  padding: 8px 4px;
  border: none;
  border-bottom: 1px solid #e2e8f0;
  background: #f8fafc;
  cursor: pointer;
  font-size: 14px;
  color: #475569;
}
.sidebar-edge-toggle:hover {
  background: #e2e8f0;
  color: #0f172a;
}
.quote-output-sidebar-body {
  overflow-y: auto;
  padding: 12px 14px 16px;
  flex: 1;
  min-height: 0;
}
.output-sidebar-title {
  margin: 0 0 6px;
  font-size: 1rem;
  font-weight: 700;
  color: #020617;
}
.output-sidebar-hint {
  margin: 0 0 14px;
  font-size: 12px;
  line-height: 1.45;
}
.output-sidebar-section {
  margin-bottom: 18px;
  padding-bottom: 14px;
  border-bottom: 1px solid #e2e8f0;
}
.output-sidebar-section:last-child {
  margin-bottom: 0;
  padding-bottom: 0;
  border-bottom: none;
}
.output-sidebar-subtitle {
  margin: 0 0 10px;
  font-size: 13px;
  font-weight: 600;
  color: #334155;
}
.output-sidebar-subtitle.small {
  font-size: 12px;
  margin-top: 8px;
}
.output-file-row {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  padding: 8px 10px;
  margin-bottom: 8px;
  background: #f8fafc;
  border-radius: 8px;
  border: 1px solid #e2e8f0;
}
.output-file-row--full {
  justify-content: flex-start;
  gap: 12px;
}
.output-file-row .output-file-label {
  font-size: 12px;
  font-weight: 500;
  color: #94a3b8;
  flex: 1;
  min-width: 120px;
  line-height: 1.35;
}
.output-file-row.is-ready .output-file-label {
  color: #15803d;
}
.output-file-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  justify-content: flex-end;
}
.output-file-type-select {
  height: 26px;
  padding: 4px 8px;
  font-size: 12px;
  border: 1px solid #cbd5e1;
  border-radius: 6px;
  background: #fff;
  color: #334155;
  cursor: pointer;
  white-space: nowrap;
}
.btn-tiny {
  padding: 4px 10px;
  font-size: 12px;
  border-radius: 6px;
  border: 1px solid #cbd5e1;
  cursor: pointer;
  background: #fff;
  color: #334155;
  white-space: nowrap;
}
.btn-tiny:hover:not(:disabled) {
  background: #f1f5f9;
}
.btn-tiny:disabled {
  opacity: 0.45;
  cursor: not-allowed;
}
.btn-tiny.primary {
  background: #2563eb;
  border-color: #2563eb;
  color: #fff;
}
.btn-tiny.primary:hover:not(:disabled) {
  background: #1d4ed8;
}
.btn-tiny.secondary {
  background: #fff;
}
.btn-block-sidebar {
  width: 100%;
  margin-top: 8px;
}
.sidebar-provision-hint {
  margin-bottom: 8px;
}
.sidebar-provision-meta p {
  margin: 4px 0;
  font-size: 12px;
}
.quote-save-bar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  z-index: 40;
  background: rgba(255, 255, 255, 0.97);
  border-top: 1px solid #cbd5e1;
  box-shadow: 0 -6px 24px rgba(15, 23, 42, 0.08);
  padding: 12px clamp(16px, 3vw, 36px);
  box-sizing: border-box;
}
.quote-save-bar-inner {
  max-width: none;
  margin: 0 auto;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}
.quote-save-bar-left {
  flex: 1;
  min-width: 0;
  font-size: 14px;
}
.quote-save-all-btn {
  flex-shrink: 0;
  margin: 0;
  padding: 10px 22px;
}
@media (max-width: 1100px) {
  .quote-project-layout {
    flex-direction: column;
  }
  .quote-output-sidebar {
    position: relative;
    top: 0;
    width: 100%;
    max-width: none;
    max-height: none;
  }
  .quote-output-sidebar.collapsed {
    width: 100%;
    min-height: 0;
  }
  .quote-output-sidebar.collapsed .sidebar-edge-toggle {
    min-height: auto;
    padding: 10px;
  }
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
.acceptance-tc-subh { margin-top: 20px; margin-bottom: 6px; font-size: 15px; font-weight: 600; color: #0f172a; }
.acceptance-tc-toolbar { flex-wrap: wrap; align-items: center; gap: 10px 14px; margin-bottom: 8px; }
.acceptance-tc-wrap {
  overflow-x: auto;
  margin: 8px 0 12px;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
  background: #fff;
}
.acceptance-tc-table {
  width: max-content;
  min-width: 100%;
  border-collapse: collapse;
  font-size: 13px;
  table-layout: fixed;
}
.acceptance-tc-table thead th.atc-th {
  background: #f1f5f9;
  border: 1px solid #e2e8f0;
  padding: 8px 6px;
  font-weight: 600;
  color: #334155;
  white-space: nowrap;
  position: sticky;
  top: 0;
  z-index: 1;
  width: 15ch;
  min-width: 15ch;
  max-width: 15ch;
  box-sizing: border-box;
  overflow: hidden;
  text-overflow: ellipsis;
}
.acceptance-tc-table thead th.atc-th-narrow {
  width: 6.5rem;
  min-width: 6.5rem;
  max-width: 6.5rem;
}
.acceptance-tc-table thead th.atc-th-action {
  width: 40px;
  min-width: 40px;
  max-width: 40px;
}
.acceptance-tc-tr {
  height: 44px;
}
.acceptance-tc-table tbody td.atc-td {
  vertical-align: middle;
  padding: 4px 6px;
  border: 1px solid #e2e8f0;
  background: #fff;
  width: 15ch;
  max-width: 15ch;
  min-width: 15ch;
  overflow: hidden;
}
.acceptance-tc-table tbody td.atc-td-narrow {
  width: 6.5rem;
  max-width: 6.5rem;
  min-width: 6.5rem;
}
.acceptance-tc-table tbody td.atc-td-action {
  width: 40px;
  max-width: 40px;
  min-width: 40px;
  text-align: center;
}
.atc-cell-input,
.acceptance-tc-table .atc-select {
  display: block;
  width: 100%;
  height: 34px;
  box-sizing: border-box;
  margin: 0;
  padding: 4px 8px;
  font-size: 13px;
  line-height: 1.25;
  border: 1px solid #cbd5e1;
  border-radius: 4px;
  background: #fff;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.acceptance-tc-table .atc-select {
  cursor: pointer;
  max-width: 100%;
}
.atc-cell-input:focus,
.acceptance-tc-table .atc-select:focus {
  outline: none;
  border-color: #2563eb;
  box-shadow: 0 0 0 1px #2563eb;
}

.quote-validity-row {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px;
  margin-top: 6px;
}
.quote-validity-days {
  width: 7rem;
  min-width: 6rem;
}
.quote-validity-kind {
  min-width: 8rem;
}
.quote-validity-hint {
  margin-top: 8px;
  margin-bottom: 0;
}
.quote-validity-legacy {
  margin-top: 10px;
  font-size: 13px;
  word-break: break-word;
}
</style>
