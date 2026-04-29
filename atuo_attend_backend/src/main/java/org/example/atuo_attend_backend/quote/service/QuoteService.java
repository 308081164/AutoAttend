package org.example.atuo_attend_backend.quote.service;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.example.atuo_attend_backend.ai.client.DeepSeekClient;
import org.example.atuo_attend_backend.ai.domain.AiAnalysisConfig;
import org.example.atuo_attend_backend.ai.service.AiAnalysisConfigService;
import org.example.atuo_attend_backend.config.SystemConfigService;
import org.example.atuo_attend_backend.quote.domain.*;
import org.example.atuo_attend_backend.quote.dto.*;
import org.example.atuo_attend_backend.quote.mapper.*;
import org.example.atuo_attend_backend.tenant.context.TenantConstants;
import org.example.atuo_attend_backend.tenant.context.TenantContext;
import org.example.atuo_attend_backend.tenant.quota.TenantResourceQuotaService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.Base64;

@Service
public class QuoteService {

    private static final Logger log = LoggerFactory.getLogger(QuoteService.class);

    private static final BigDecimal HUNDRED = new BigDecimal("100");

    private final QuoteProjectMapper projectMapper;
    private final QuoteModuleMapper moduleMapper;
    private final QuoteItemMapper itemMapper;
    private final QuoteBaselineMapper baselineMapper;
    private final QuoteRiskConfigMapper riskConfigMapper;
    private final QuotePriceConfigMapper priceConfigMapper;
    private final QuoteResultMapper resultMapper;
    private final QuoteDocumentMapper documentMapper;
    private final QuoteContractDraftMapper contractDraftMapper;
    private final QuotePresetItemMapper presetItemMapper;
    private final AiAnalysisConfigService aiConfigService;
    private final DeepSeekClient deepSeekClient;
    private final SystemConfigService systemConfigService;
    private final TenantResourceQuotaService tenantResourceQuotaService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    /** 模型偶发在字符串内输出未转义控制字符时，尽量放宽解析（仍须为合法 JSON 结构）。 */
    private final ObjectMapper lenientJsonMapper = new ObjectMapper(
            JsonFactory.builder().enable(JsonReadFeature.ALLOW_UNESCAPED_CONTROL_CHARS).build());

    private static final BigDecimal RISK_PCT_MIN = new BigDecimal("-50");
    private static final BigDecimal RISK_PCT_MAX = new BigDecimal("150");

    public QuoteService(QuoteProjectMapper projectMapper, QuoteModuleMapper moduleMapper, QuoteItemMapper itemMapper,
                        QuoteBaselineMapper baselineMapper, QuoteRiskConfigMapper riskConfigMapper,
                        QuotePriceConfigMapper priceConfigMapper, QuoteResultMapper resultMapper,
                        QuoteDocumentMapper documentMapper, QuoteContractDraftMapper contractDraftMapper,
                        QuotePresetItemMapper presetItemMapper,
                        AiAnalysisConfigService aiConfigService, DeepSeekClient deepSeekClient,
                        SystemConfigService systemConfigService,
                        TenantResourceQuotaService tenantResourceQuotaService) {
        this.projectMapper = projectMapper;
        this.moduleMapper = moduleMapper;
        this.itemMapper = itemMapper;
        this.baselineMapper = baselineMapper;
        this.riskConfigMapper = riskConfigMapper;
        this.priceConfigMapper = priceConfigMapper;
        this.resultMapper = resultMapper;
        this.documentMapper = documentMapper;
        this.contractDraftMapper = contractDraftMapper;
        this.presetItemMapper = presetItemMapper;
        this.aiConfigService = aiConfigService;
        this.deepSeekClient = deepSeekClient;
        this.systemConfigService = systemConfigService;
        this.tenantResourceQuotaService = tenantResourceQuotaService;
    }

    private static long tid() {
        return TenantContext.getTenantIdOrDefault(TenantConstants.DEFAULT_TENANT_ID);
    }

    @Transactional(rollbackFor = Exception.class)
    public long createProject(QuoteProjectSaveDto dto) {
        tenantResourceQuotaService.assertCanCreateQuoteProject(tid());
        QuoteProject p = toProject(dto);
        if (p.getName() == null || p.getName().isBlank()) throw new IllegalArgumentException("项目名称不能为空");
        if (p.getStatus() == null) p.setStatus("draft");
        if (p.getQuoteSubjectMode() == null) {
            p.setQuoteSubjectMode("legal_entity");
        }
        if (p.getQuoteKind() == null) {
            p.setQuoteKind("single");
        }
        p.setTenantId(tid());
        projectMapper.insert(p);
        saveModules(p.getId(), dto.getModules(), null, null);
        return p.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateProject(long id, QuoteProjectSaveDto dto) {
        QuoteProject existing = projectMapper.findById(tid(), id);
        if (existing == null) throw new IllegalArgumentException("项目不存在");
        QuoteProject p = toProject(dto);
        p.setId(id);
        p.setTenantId(tid());
        if (dto.getQuoteCalcPrefs() == null) {
            p.setQuoteCalcPrefsJson(existing.getQuoteCalcPrefsJson());
        }
        if (dto.getQuoteContractContext() == null) {
            p.setQuoteContractContextJson(existing.getQuoteContractContextJson());
        }
        if (dto.getQuoteVendorName() == null) {
            p.setQuoteVendorName(existing.getQuoteVendorName());
        }
        if (dto.getQuoteContactInfo() == null) {
            p.setQuoteContactInfo(existing.getQuoteContactInfo());
        }
        if (dto.getQuoteValidityNote() == null) {
            p.setQuoteValidityNote(existing.getQuoteValidityNote());
        }
        if (dto.getQuoteSubjectMode() == null) {
            p.setQuoteSubjectMode(existing.getQuoteSubjectMode() != null ? existing.getQuoteSubjectMode() : "legal_entity");
        }
        if (dto.getQuoteKind() == null) {
            p.setQuoteKind(existing.getQuoteKind() != null ? existing.getQuoteKind() : "single");
        } else {
            p.setQuoteKind(normalizeQuoteKind(dto.getQuoteKind()));
        }
        projectMapper.update(p);
        // 先缓存行金额，再删除旧模块，避免缓存查询为空
        Map<String, BigDecimal> snapCache = new LinkedHashMap<>();
        Map<String, BigDecimal> adjCache = new LinkedHashMap<>();
        for (QuoteModule m : moduleMapper.listByProjectId(tid(), id)) {
            for (QuoteItem it : itemMapper.listByModuleId(tid(), m.getId())) {
                String key = m.getName() + "||" + it.getName();
                if (it.getLinePriceSnap() != null) snapCache.put(key, it.getLinePriceSnap());
                if (it.getLinePriceAdjusted() != null) adjCache.put(key, it.getLinePriceAdjusted());
            }
        }
        moduleMapper.deleteByProjectId(tid(), id);
        saveModules(id, dto.getModules(), snapCache, adjCache);
    }

    private QuoteProject toProject(QuoteProjectSaveDto dto) {
        QuoteProject p = new QuoteProject();
        p.setName(dto.getName());
        p.setProjectType(nvl(dto.getProjectType(), "other"));
        p.setTechStack(nvl(dto.getTechStack(), "vue_node"));
        p.setDesignType(nvl(dto.getDesignType(), "need_design"));
        p.setDataMigration(nvl(dto.getDataMigration(), "none"));
        p.setConcurrency(nvl(dto.getConcurrency(), "lt100"));
        p.setSecurityLevel(nvl(dto.getSecurityLevel(), "normal"));
        p.setDeployType(nvl(dto.getDeployType(), "cloud"));
        p.setStatus(nvl(dto.getStatus(), "draft"));
        p.setLinkTableId(dto.getLinkTableId());
        p.setPrdSummary(dto.getPrdSummary());
        p.setAiRequirementText(dto.getAiRequirementText());
        p.setQuoteVendorName(trimToNull(dto.getQuoteVendorName()));
        p.setQuoteContactInfo(trimToNull(dto.getQuoteContactInfo()));
        p.setQuoteValidityNote(trimToNull(dto.getQuoteValidityNote()));
        if (dto.getQuoteSubjectMode() != null) {
            p.setQuoteSubjectMode(normalizeQuoteSubjectMode(dto.getQuoteSubjectMode()));
        }
        p.setQuoteKind(normalizeQuoteKind(dto.getQuoteKind()));
        if (dto.getQuoteCalcPrefs() != null && !dto.getQuoteCalcPrefs().isEmpty()) {
            applyQuoteCalcPrefsMapToEntity(p, dto.getQuoteCalcPrefs());
        }
        if (dto.getQuoteContractContext() != null) {
            try {
                p.setQuoteContractContextJson(objectMapper.writeValueAsString(dto.getQuoteContractContext()));
            } catch (JsonProcessingException e) {
                throw new IllegalArgumentException("quoteContractContext 无法序列化");
            }
        }
        return p;
    }

    private void applyQuoteCalcPrefsMapToEntity(QuoteProject p, Map<String, Object> prefs) {
        try {
            p.setQuoteCalcPrefsJson(objectMapper.writeValueAsString(prefs));
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("quoteCalcPrefs 无法序列化");
        }
    }

    private static String nvl(String v, String d) {
        return v == null || v.isBlank() ? d : v;
    }

    private static String trimToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private static String normalizeQuoteSubjectMode(String m) {
        if (m == null || m.isBlank()) return "legal_entity";
        if ("natural_person".equals(m) || "manual".equals(m)) return m;
        return "legal_entity";
    }

    private static String normalizeQuoteKind(String k) {
        if (k == null || k.isBlank()) return "single";
        if ("solution".equals(k)) return "solution";
        return "single";
    }

    private void saveModules(long projectId, List<QuoteModuleSaveDto> modules,
                             Map<String, BigDecimal> snapCache, Map<String, BigDecimal> adjCache) {
        if (modules == null) return;
        if (snapCache == null) snapCache = new LinkedHashMap<>();
        if (adjCache == null) adjCache = new LinkedHashMap<>();
        int mi = 0;
        for (QuoteModuleSaveDto md : modules) {
            if (md.getName() == null || md.getName().isBlank()) continue;
            QuoteModule m = new QuoteModule();
            m.setTenantId(tid());
            m.setQuoteProjectId(projectId);
            m.setName(md.getName().trim());
            m.setSortOrder(md.getSortOrder() != 0 ? md.getSortOrder() : mi++);
            String dk = trimToNull(md.getDeliverableKey());
            if (dk == null) {
                dk = "default";
            }
            m.setDeliverableKey(dk);
            m.setDeliverableLabel(trimToNull(md.getDeliverableLabel()));
            m.setTechStack(trimToNull(md.getTechStack()));
            moduleMapper.insert(m);
            int ii = 0;
            for (QuoteItemSaveDto it : md.getItems()) {
                if (it.getName() == null || it.getName().isBlank()) continue;
                QuoteItem item = new QuoteItem();
                item.setTenantId(tid());
                item.setModuleId(m.getId());
                item.setName(it.getName().trim());
                item.setComplexity(nvl(it.getComplexity(), "standard"));
                item.setQuantity(Math.max(1, it.getQuantity()));
                item.setEstimatedDays(BigDecimal.ZERO);
                item.setSortOrder(ii++);
                item.setExcludedFromScale(Boolean.TRUE.equals(it.getExcludedFromScale()));
                // 恢复缓存的行金额（按模块名+功能名匹配）
                String key = md.getName().trim() + "||" + it.getName().trim();
                item.setLinePriceSnap(snapCache.get(key));
                item.setLinePriceAdjusted(adjCache.get(key));
                itemMapper.insert(item);
            }
        }
    }

    public Map<String, Object> getProjectDetail(long id) {
        QuoteProject p = projectMapper.findById(tid(),id);
        if (p == null) return null;
        Map<String, Object> out = projectToMap(p);
        List<Map<String, Object>> moduleList = new ArrayList<>();
        for (QuoteModule m : moduleMapper.listByProjectId(tid(),id)) {
            Map<String, Object> mm = new LinkedHashMap<>();
            mm.put("id", m.getId());
            mm.put("name", m.getName());
            mm.put("sortOrder", m.getSortOrder());
            mm.put("deliverableKey", m.getDeliverableKey() != null ? m.getDeliverableKey() : "default");
            mm.put("deliverableLabel", m.getDeliverableLabel());
            mm.put("techStack", m.getTechStack());
            List<Map<String, Object>> items = new ArrayList<>();
            for (QuoteItem it : itemMapper.listByModuleId(tid(),m.getId())) {
                Map<String, Object> im = new LinkedHashMap<>();
                im.put("id", it.getId());
                im.put("name", it.getName());
                im.put("complexity", it.getComplexity());
                im.put("quantity", it.getQuantity());
                im.put("estimatedDays", it.getEstimatedDays());
                im.put("excludedFromScale", Boolean.TRUE.equals(it.getExcludedFromScale()));
                im.put("linePriceSnap", it.getLinePriceSnap());
                im.put("linePriceAdjusted", it.getLinePriceAdjusted());
                items.add(im);
            }
            mm.put("items", items);
            moduleList.add(mm);
        }
        out.put("modules", moduleList);
        QuoteResult latest = resultMapper.findLatestByProjectId(tid(),id);
        Map<String, Object> calcPrefs = parseQuoteCalcPrefsJson(p.getQuoteCalcPrefsJson());
        if (calcPrefs == null && latest != null) {
            calcPrefs = inferQuoteCalcPrefsFromLatestResult(latest);
        }
        if (calcPrefs != null) {
            out.put("quoteCalcPrefs", calcPrefs);
        }
        if (latest != null) out.put("latestResult", resultToMap(latest));
        return out;
    }

    private Map<String, Object> parseQuoteCalcPrefsJson(String json) {
        if (json == null || json.isBlank()) return null;
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            return null;
        }
    }

    private Map<String, Object> inferQuoteCalcPrefsFromLatestResult(QuoteResult latest) {
        try {
            Map<String, Object> m = new LinkedHashMap<>();
            List<String> applied = objectMapper.readValue(
                    latest.getSelectedRisksJson() != null ? latest.getSelectedRisksJson() : "[]",
                    new TypeReference<List<String>>() {});
            List<String> riskKeys = new ArrayList<>();
            boolean rush = false;
            if (applied != null) {
                for (String k : applied) {
                    if ("urgency_rush".equals(k)) rush = true;
                    else riskKeys.add(k);
                }
            }
            m.put("riskKeys", riskKeys);
            m.put("urgencyRush", rush);
            @SuppressWarnings("unchecked")
            Map<String, Object> auditRaw = objectMapper.readValue(
                    latest.getAuditChecklistJson() != null ? latest.getAuditChecklistJson() : "{}",
                    new TypeReference<Map<String, Object>>() {});
            m.put("auditChecklist", auditRaw != null ? auditRaw : Map.of());
            m.put("priceConfigId", guessPriceConfigId(latest.getPricePerDayUsed(), latest.getRegionLabelUsed()));
            return m;
        } catch (Exception e) {
            return null;
        }
    }

    private Long guessPriceConfigId(BigDecimal pricePerDay, String regionLabel) {
        if (pricePerDay == null) return null;
        for (Map<String, Object> row : priceConfigMapper.listAll(tid())) {
            BigDecimal pd = toBd(row.get("pricePerDay"));
            String label = Objects.toString(row.get("regionLabel"), "");
            if (pd.compareTo(pricePerDay) == 0 && (regionLabel == null || regionLabel.equals(label))) {
                Object oid = row.get("id");
                if (oid instanceof Number) return ((Number) oid).longValue();
            }
        }
        return null;
    }

    @Transactional
    public void saveQuoteCalcPrefs(long projectId, QuoteCalculateRequest req) {
        if (req == null) req = new QuoteCalculateRequest();
        QuoteProject existing = projectMapper.findById(tid(),projectId);
        if (existing == null) throw new IllegalArgumentException("项目不存在");
        try {
            String json = objectMapper.writeValueAsString(buildQuoteCalcPrefsMap(req));
            projectMapper.updateQuoteCalcPrefs(tid(),projectId, json);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("无法序列化报价偏好");
        }
    }

    private Map<String, Object> buildQuoteCalcPrefsMap(QuoteCalculateRequest req) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("riskKeys", req.getRiskKeys() != null ? new ArrayList<>(req.getRiskKeys()) : new ArrayList<String>());
        map.put("urgencyRush", req.isUrgencyRush());
        map.put("priceConfigId", req.getPriceConfigId());
        map.put("auditChecklist", req.getAuditChecklist() != null ? new LinkedHashMap<>(req.getAuditChecklist()) : Map.of());
        return map;
    }

    public Map<String, Object> listProjects(int page, int pageSize) {
        page = Math.max(1, page);
        pageSize = Math.min(Math.max(pageSize, 1), 100);
        int offset = (page - 1) * pageSize;
        long total = projectMapper.countAll(tid());
        List<Map<String, Object>> rows = new ArrayList<>();
        for (QuoteProject p : projectMapper.listPaged(tid(),offset, pageSize)) {
            rows.add(projectToListItemMap(p));
        }
        Map<String, Object> data = new HashMap<>();
        data.put("items", rows);
        data.put("total", total);
        data.put("page", page);
        data.put("pageSize", pageSize);
        return data;
    }

    /**
     * 删除报价项目：清理该项目下的模块/功能点/计算结果与相关文档草稿，再删除项目本身。
     * 注意：此删除不包含“已同步到协作多维表”的数据清理（尚未在 MVP 里约定回滚）。
     */
    @Transactional
    public void deleteProject(long id) {
        QuoteProject existing = projectMapper.findById(tid(),id);
        if (existing == null) throw new IllegalArgumentException("项目不存在");

        // 先清理业务树：modules/items
        List<QuoteModule> modules = moduleMapper.listByProjectId(tid(),id);
        if (modules != null) {
            for (QuoteModule m : modules) {
                itemMapper.deleteByModuleId(tid(),m.getId());
            }
        }
        moduleMapper.deleteByProjectId(tid(),id);

        // 再清理计算结果：result + document/contractDraft
        List<Long> resultIds = resultMapper.listIdsByProjectId(tid(),id);
        if (resultIds != null) {
            for (Long rid : resultIds) {
                documentMapper.deleteByResultId(tid(),rid);
                contractDraftMapper.deleteByResultId(tid(),rid);
            }
        }
        resultMapper.deleteByProjectId(tid(),id);

        // 最后删除项目主表
        projectMapper.deleteById(tid(),id);
    }

    @Transactional
    public Map<String, Object> calculate(long projectId, QuoteCalculateRequest req) throws Exception {
        QuoteProject p = projectMapper.findById(tid(),projectId);
        if (p == null) throw new IllegalArgumentException("项目不存在");
        List<QuoteModule> modules = moduleMapper.listByProjectId(tid(),projectId);
        BigDecimal totalDays = BigDecimal.ZERO;
        for (QuoteModule m : modules) {
            for (QuoteItem it : itemMapper.listByModuleId(tid(),m.getId())) {
                String stackForLine = trimToNull(m.getTechStack());
                if (stackForLine == null) {
                    stackForLine = p.getTechStack();
                }
                BigDecimal base = baselineMapper.findDays(tid(), stackForLine, it.getComplexity());
                if (base == null) base = baselineMapper.findDays(tid(),"other", it.getComplexity());
                if (base == null) base = new BigDecimal("1.5");
                BigDecimal line = base.multiply(new BigDecimal(it.getQuantity())).setScale(2, RoundingMode.HALF_UP);
                itemMapper.updateEstimatedDays(tid(),it.getId(), line);
                totalDays = totalDays.add(line);
            }
        }
        Map<String, BigDecimal> riskByKey = new HashMap<>();
        for (Map<String, Object> row : riskConfigMapper.listEnabled(tid())) {
            String key = (String) row.get("riskKey");
            Object pct = row.get("defaultPct");
            BigDecimal d = toBd(pct);
            riskByKey.put(key, d);
        }
        BigDecimal riskPct = BigDecimal.ZERO;
        List<String> applied = new ArrayList<>();
        if (req.getRiskKeys() != null) {
            for (String k : req.getRiskKeys()) {
                BigDecimal d = riskByKey.get(k);
                if (d != null) {
                    riskPct = riskPct.add(d);
                    applied.add(k);
                }
            }
        }
        if (req.isUrgencyRush()) {
            BigDecimal rush = riskByKey.get("urgency_rush");
            if (rush != null) {
                riskPct = riskPct.add(rush);
                applied.add("urgency_rush");
            }
        }
        Map<String, Object> priceRow = null;
        if (req.getPriceConfigId() != null) {
            priceRow = priceConfigMapper.findById(tid(),req.getPriceConfigId());
        }
        if (priceRow == null) {
            List<Map<String, Object>> list = priceConfigMapper.listEnabled(tid());
            if (!list.isEmpty()) priceRow = list.get(0);
        }
        if (priceRow == null) throw new IllegalStateException("未配置人天单价，请先执行 schema_quote_mysql.sql 或新增单价配置");
        BigDecimal pricePerDay = toBd(priceRow.get("pricePerDay"));
        String regionLabel = Objects.toString(priceRow.get("regionLabel"), "");
        BigDecimal durationCoeff = normalizeDurationCoefficientForCalc(priceRow.get("durationCoefficient"));
        BigDecimal estimatedDuration = totalDays.multiply(durationCoeff).setScale(2, RoundingMode.HALF_UP);
        BigDecimal baseAmount = totalDays.multiply(pricePerDay).setScale(2, RoundingMode.HALF_UP);
        BigDecimal mult = BigDecimal.ONE.add(riskPct.divide(HUNDRED, 6, RoundingMode.HALF_UP));
        BigDecimal finalAmount = baseAmount.multiply(mult).setScale(2, RoundingMode.HALF_UP);
        BigDecimal riskAmount = finalAmount.subtract(baseAmount).setScale(2, RoundingMode.HALF_UP);
        int confidence = computeConfidence(p, modules, totalDays);
        List<String> hints = buildRiskHints(confidence, req.getAuditChecklist());
        if (req.getRiskKeys() != null && req.getRiskKeys().contains("standard_cycle") && req.isUrgencyRush()) {
            hints.add(0, "已同时勾选「标准交付周期（降价）」与「加急」，逻辑上可能矛盾，请人工复核最终报价。");
        }
        QuoteResult r = new QuoteResult();
        r.setQuoteProjectId(projectId);
        r.setTotalDays(totalDays);
        r.setBaseAmount(baseAmount);
        r.setRiskPctTotal(riskPct);
        r.setRiskAmount(riskAmount);
        r.setFinalAmount(finalAmount);
        r.setConfidenceScore(confidence);
        r.setAuditChecklistJson(objectMapper.writeValueAsString(req.getAuditChecklist() != null ? req.getAuditChecklist() : Map.of()));
        r.setSelectedRisksJson(objectMapper.writeValueAsString(applied));
        r.setPricePerDayUsed(pricePerDay);
        r.setDurationCoefficientUsed(durationCoeff);
        r.setEstimatedDurationDays(estimatedDuration);
        r.setRegionLabelUsed(regionLabel);
        r.setBaselineFinalAmount(finalAmount);
        r.setPriceScaleFactor(BigDecimal.ONE);
        r.setAdjustedFinalAmount(finalAmount);
        r.setPriceAdjustNote(null);
        r.setTenantId(tid());
        resultMapper.insert(r);
        distributeBaselineLinePrices(projectId, finalAmount);
        try {
            projectMapper.updateQuoteCalcPrefs(tid(),projectId, objectMapper.writeValueAsString(buildQuoteCalcPrefsMap(req)));
        } catch (Exception ignored) {
            // 不因偏好落库失败影响报价结果
        }
        QuoteResult persisted = resultMapper.findById(tid(), r.getId());
        Map<String, Object> data = resultToMap(persisted != null ? persisted : r);
        data.put("riskHints", hints);
        data.put("confidenceLevel", confidenceLevel(confidence));
        return data;
    }

    private static BigDecimal toBd(Object o) {
        if (o == null) return BigDecimal.ZERO;
        if (o instanceof BigDecimal) return (BigDecimal) o;
        return new BigDecimal(o.toString());
    }

    private int computeConfidence(QuoteProject p, List<QuoteModule> modules, BigDecimal totalDays) {
        int score = 78;
        int itemCount = 0;
        for (QuoteModule m : modules) {
            itemCount += itemMapper.listByModuleId(tid(),m.getId()).size();
        }
        if (itemCount == 0) score -= 25;
        else if (itemCount < 3) score -= 10;
        else score += Math.min(8, itemCount / 2);
        if (modules.size() >= 2) score += 5;
        else score -= 5;
        if (p.getPrdSummary() != null && p.getPrdSummary().trim().length() > 30) score += 10;
        else score -= 12;
        if (totalDays.compareTo(BigDecimal.ZERO) <= 0) score -= 15;
        return Math.max(0, Math.min(100, score));
    }

    private String confidenceLevel(int c) {
        if (c >= 90) return "high";
        if (c >= 70) return "medium";
        if (c >= 50) return "low";
        return "very_low";
    }

    private List<String> buildRiskHints(int confidence, Map<String, Boolean> checklist) {
        List<String> hints = new ArrayList<>();
        if (confidence < 70) hints.add("置信度偏低，建议补充 PRD/功能点后再报价。");
        if (checklist == null || !Boolean.TRUE.equals(checklist.get("acceptanceClear"))) {
            hints.add("建议勾选并确认：验收标准是否明确。");
        }
        if (checklist == null || !Boolean.TRUE.equals(checklist.get("paymentNodes"))) {
            hints.add("建议约定付款节点（如 3-4-3）。");
        }
        return hints;
    }

    public Map<String, Object> buildQuoteDocument(long projectId, Long quoteResultId) {
        QuoteProject p = projectMapper.findById(tid(),projectId);
        if (p == null) throw new IllegalArgumentException("项目不存在");
        QuoteResult r = quoteResultId != null ? resultMapper.findById(tid(),quoteResultId) : resultMapper.findLatestByProjectId(tid(),projectId);
        if (r == null || r.getQuoteProjectId() != projectId) throw new IllegalArgumentException("请先计算报价或指定有效的报价结果 ID");

        // 自动检测：若所有功能点 estimatedDays=0 且 linePriceSnap=null，说明尚未计算，自动执行一次
        List<QuoteModule> modules = moduleMapper.listByProjectId(tid(), projectId);
        boolean needsRecalc = false;
        if (!modules.isEmpty()) {
            for (QuoteModule m : modules) {
                for (QuoteItem it : itemMapper.listByModuleId(tid(), m.getId())) {
                    BigDecimal days = it.getEstimatedDays();
                    if (days != null && days.compareTo(BigDecimal.ZERO) > 0) {
                        needsRecalc = false;
                        break;
                    }
                    if (it.getLinePriceSnap() != null) {
                        needsRecalc = false;
                        break;
                    }
                }
                if (!needsRecalc) break;
            }
            if (needsRecalc) {
                log.info("buildQuoteDocument: detected uncalculated items, auto-calculating for project {}", projectId);
                try {
                    QuoteCalculateRequest autoReq = new QuoteCalculateRequest();
                    autoReq.setRiskKeys(r.getSelectedRisksJson() != null
                            ? objectMapper.readValue(r.getSelectedRisksJson(), List.class) : null);
                    Map<String, Object> calcResult = calculate(projectId, autoReq);
                    r = resultMapper.findLatestByProjectId(tid(), projectId);
                    if (r == null) {
                        throw new IllegalStateException("自动计算报价失败");
                    }
                } catch (Exception e) {
                    log.warn("buildQuoteDocument: auto-calculate failed, using existing result: {}", e.getMessage());
                }
            }
        }

        String html = renderQuoteHtml(p, r);
        documentMapper.insert(tid(), r.getId(), "quote", html, 1);
        Map<String, Object> data = new HashMap<>();
        data.put("html", html);
        data.put("filename", "quote-" + projectId + "-" + r.getId() + ".html");
        data.put("quoteResultId", r.getId());
        return data;
    }

    /**
     * 将二进制产出（PDF/DOCX）落库为 base64 字符串，写入 biz_quote_document.content（MEDIUMTEXT）。
     * 用于驱动「刷新后仍能判断步骤进度」的绿色状态。
     */
    public void persistBinaryDocument(long quoteResultId, String docType, byte[] bytes) {
        if (bytes == null) return;
        String b64 = Base64.getEncoder().encodeToString(bytes);
        documentMapper.insert(tid(), quoteResultId, docType, b64, 1);
    }

    /**
     * 返回“哪些产出物已经落库”的布尔状态，供前端驱动 is-ready 绿标。
     */
    public Map<String, Object> getQuoteArtifactStatus(long quoteResultId) {
        List<String> docTypes = documentMapper.listDocTypesByResultId(tid(), quoteResultId);
        Set<String> set = new HashSet<>(docTypes != null ? docTypes : List.of());
        Map<String, Object> out = new LinkedHashMap<>();
        // quote html：历史上落库 doc_type=quote
        out.put("quoteHtml", set.contains("quote"));
        // quote pdf/docx：新落库 doc_type=quote_pdf / quote_docx
        out.put("quotePdf", set.contains("quote_pdf"));
        out.put("quoteDocx", set.contains("quote_docx"));

        // contract html：历史上落库 doc_type=contract
        out.put("contractHtml", set.contains("contract"));
        // contract pdf/docx：新落库 doc_type=contract_pdf / contract_docx
        out.put("contractPdf", set.contains("contract_pdf"));
        out.put("contractDocx", set.contains("contract_docx"));
        return out;
    }

    private String renderQuoteHtml(QuoteProject p, QuoteResult r) {
        StringBuilder sb = new StringBuilder();
        // OpenHTMLToPDF 按 XML 解析：void 元素须自闭合，否则报 meta 未闭合
        sb.append("<!DOCTYPE html><html><head><meta charset=\"UTF-8\"/><title>报价单</title>");
        sb.append("<style>body{font-family:sans-serif;padding:24px;color:#111} h1{font-size:20px} .quote-doc-meta{border:1px solid #e5e7eb;background:#f9fafb;padding:12px 14px;border-radius:8px;margin:12px 0 16px;line-height:1.55} .quote-doc-meta p{margin:4px 0;font-size:13px} table{border-collapse:collapse;width:100%;margin-top:16px} th,td{border:1px solid #ddd;padding:8px;font-size:13px} th{background:#f3f4f6}</style>");
        sb.append("</head><body>");
        sb.append("<h1>项目报价单</h1>");
        Map<String, String> header = resolveQuoteDocHeader(p);
        String vendor = header.getOrDefault("vendorName", "");
        String contact = header.getOrDefault("contactInfo", "");
        boolean hasMeta = !vendor.isBlank() || !contact.isBlank()
                || (p.getQuoteValidityNote() != null && !p.getQuoteValidityNote().isBlank());
        if (hasMeta) {
            sb.append("<div class=\"quote-doc-meta\">");
            if (!vendor.isBlank()) {
                sb.append("<p><strong>报价单位：</strong>").append(esc(vendor)).append("</p>");
            }
            if (!contact.isBlank()) {
                sb.append("<p><strong>联系方式：</strong>").append(esc(contact)).append("</p>");
            }
            if (p.getQuoteValidityNote() != null && !p.getQuoteValidityNote().isBlank()) {
                sb.append("<p><strong>报价有效期：</strong>").append(esc(p.getQuoteValidityNote())).append("</p>");
            }
            sb.append("</div>");
        }
        sb.append("<p><strong>项目名称：</strong>").append(esc(p.getName())).append("</p>");
        sb.append("<p><strong>生成时间：</strong>").append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))).append("</p>");
        sb.append("<p><strong>技术栈：</strong>").append(esc(p.getTechStack())).append(" &nbsp; <strong>项目类型：</strong>").append(esc(p.getProjectType())).append("</p>");
        boolean sol = "solution".equals(p.getQuoteKind());
        if (sol) {
            sb.append("<table><thead><tr><th>交付物</th><th>计价技术栈</th><th>模块</th><th>功能点</th><th>复杂度</th><th>数量</th><th>人天</th><th>金额(元)</th></tr></thead><tbody>");
        } else {
            sb.append("<table><thead><tr><th>模块</th><th>功能点</th><th>复杂度</th><th>数量</th><th>人天</th><th>金额(元)</th></tr></thead><tbody>");
        }
        for (QuoteModule m : moduleMapper.listByProjectId(tid(),p.getId())) {
            String dlab = m.getDeliverableLabel();
            String dk = m.getDeliverableKey() != null ? m.getDeliverableKey() : "default";
            String delCell = (dlab != null && !dlab.isBlank()) ? esc(dlab) : esc(dk);
            String mts = trimToNull(m.getTechStack());
            String stackCell = mts != null ? esc(mts) : esc(p.getTechStack());
            for (QuoteItem it : itemMapper.listByModuleId(tid(),m.getId())) {
                String amt = it.getLinePriceAdjusted() != null ? it.getLinePriceAdjusted().toPlainString()
                        : (it.getLinePriceSnap() != null ? it.getLinePriceSnap().toPlainString() : "—");
                if (sol) {
                    sb.append("<tr><td>").append(delCell).append("</td><td>").append(stackCell).append("</td><td>").append(esc(m.getName())).append("</td><td>").append(esc(it.getName()))
                            .append("</td><td>").append(esc(it.getComplexity())).append("</td><td>").append(it.getQuantity())
                            .append("</td><td>").append(it.getEstimatedDays() != null ? it.getEstimatedDays().toPlainString() : "0")
                            .append("</td><td>").append(amt)
                            .append("</td></tr>");
                } else {
                    sb.append("<tr><td>").append(esc(m.getName())).append("</td><td>").append(esc(it.getName()))
                            .append("</td><td>").append(esc(it.getComplexity())).append("</td><td>").append(it.getQuantity())
                            .append("</td><td>").append(it.getEstimatedDays() != null ? it.getEstimatedDays().toPlainString() : "0")
                            .append("</td><td>").append(amt)
                            .append("</td></tr>");
                }
            }
        }
        sb.append("</tbody></table>");
        Map<String, BigDecimal> subByKey = new LinkedHashMap<>();
        for (QuoteModule m : moduleMapper.listByProjectId(tid(), p.getId())) {
            String dk = m.getDeliverableKey() != null ? m.getDeliverableKey() : "default";
            String label = (m.getDeliverableLabel() != null && !m.getDeliverableLabel().isBlank()) ? m.getDeliverableLabel() : dk;
            for (QuoteItem it : itemMapper.listByModuleId(tid(), m.getId())) {
                BigDecimal line = it.getLinePriceAdjusted() != null ? it.getLinePriceAdjusted()
                        : (it.getLinePriceSnap() != null ? it.getLinePriceSnap() : BigDecimal.ZERO);
                subByKey.merge(label, line, BigDecimal::add);
            }
        }
        if (subByKey.size() > 1) {
            sb.append("<p style='margin-top:12px'><strong>分交付物小计：</strong></p><ul>");
            for (Map.Entry<String, BigDecimal> e : subByKey.entrySet()) {
                sb.append("<li>").append(esc(e.getKey())).append("：").append(e.getValue().toPlainString()).append(" 元</li>");
            }
            sb.append("</ul>");
        }
        sb.append("<p style='margin-top:20px'><strong>总人天：</strong>").append(r.getTotalDays().toPlainString());
        sb.append(" &nbsp; <strong>工期系数：</strong>").append(r.getDurationCoefficientUsed() != null ? r.getDurationCoefficientUsed().toPlainString() : "1.2");
        sb.append(" &nbsp; <strong>预计工期（天）：</strong>").append(r.getEstimatedDurationDays() != null ? r.getEstimatedDurationDays().toPlainString() : "0");
        sb.append(" &nbsp; <strong>人天单价：</strong>").append(r.getPricePerDayUsed().toPlainString()).append("（").append(esc(r.getRegionLabelUsed())).append("）</p>");
        sb.append("<p><strong>基础金额：</strong>").append(r.getBaseAmount().toPlainString());
        sb.append(" &nbsp; <strong>风险合计：</strong>+").append(r.getRiskPctTotal().toPlainString()).append("%，风险金额：").append(r.getRiskAmount().toPlainString());
        BigDecimal baseLine = r.getBaselineFinalAmount() != null ? r.getBaselineFinalAmount() : r.getFinalAmount();
        sb.append(" &nbsp; <strong>模型基线总价：</strong>").append(baseLine.toPlainString()).append(" 元");
        if (r.getPriceScaleFactor() != null && r.getPriceScaleFactor().compareTo(BigDecimal.ONE) != 0) {
            sb.append(" &nbsp; <strong>商务系数：</strong>").append(r.getPriceScaleFactor().stripTrailingZeros().toPlainString());
        }
        sb.append("</p>");
        if (r.getPriceAdjustNote() != null && !r.getPriceAdjustNote().isBlank()) {
            sb.append("<p><strong>调价说明：</strong>").append(esc(r.getPriceAdjustNote())).append("</p>");
        }
        sb.append("<p><strong>最终对外报价：</strong><span style='color:#059669;font-size:18px'>").append(r.getFinalAmount().toPlainString()).append("</span> 元</p>");
        sb.append("<p><strong>置信度评分：</strong>").append(r.getConfidenceScore()).append("/100</p>");
        sb.append("</body></html>");
        return sb.toString();
    }

    private static String esc(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    /**
     * 报价单抬头：报价单位、联系方式。manual 用项目字段；否则从系统乙方主体模板（报价配置）解析。
     */
    private Map<String, String> resolveQuoteDocHeader(QuoteProject p) {
        Map<String, String> out = new LinkedHashMap<>();
        String mode = p.getQuoteSubjectMode();
        if (mode == null || mode.isBlank()) {
            mode = "legal_entity";
        }
        if ("manual".equals(mode)) {
            out.put("vendorName", nz(p.getQuoteVendorName()));
            out.put("contactInfo", nz(p.getQuoteContactInfo()));
            return out;
        }
        Map<String, Object> profile = systemConfigService.getQuotePartyBProfile();
        if ("natural_person".equals(mode)) {
            Object npObj = profile.get("naturalPerson");
            if (npObj instanceof Map<?, ?>) {
                @SuppressWarnings("unchecked")
                Map<String, Object> n = (Map<String, Object>) npObj;
                out.put("vendorName", nz(strVal(n.get("fullName"))));
                out.put("contactInfo", formatNaturalPersonContactLine(n));
            } else {
                out.put("vendorName", "");
                out.put("contactInfo", "");
            }
            return out;
        }
        out.put("vendorName", nz(strVal(profile.get("legalName"))));
        out.put("contactInfo", formatLegalEntityContactLine(profile));
        return out;
    }

    private static String strVal(Object o) {
        if (o == null) return "";
        return o.toString().trim();
    }

    private static String nz(String s) {
        return s == null ? "" : s.trim();
    }

    private List<Map<String, Object>> parseQuoteModuleItemsJson(JsonNode itemsNode) {
        List<Map<String, Object>> items = new ArrayList<>();
        if (itemsNode == null || !itemsNode.isArray()) {
            return items;
        }
        for (JsonNode it : itemsNode) {
            String itemName = it.has("name") ? it.get("name").asText("").trim() : "";
            if (itemName.isEmpty()) {
                continue;
            }
            String complexity = normalizeComplexityForQuote(it.has("complexity") ? it.get("complexity").asText() : "standard");
            int qty = 1;
            if (it.has("quantity") && it.get("quantity").isNumber()) {
                qty = Math.max(1, it.get("quantity").intValue());
            }
            Map<String, Object> im = new LinkedHashMap<>();
            im.put("name", itemName);
            im.put("complexity", complexity);
            im.put("quantity", qty);
            items.add(im);
        }
        return items;
    }

    private static String formatLegalEntityContactLine(Map<String, Object> profile) {
        List<String> parts = new ArrayList<>();
        String cn = strVal(profile.get("contactName"));
        if (!cn.isEmpty()) {
            parts.add("联系人：" + cn);
        }
        String ph = strVal(profile.get("contactPhone"));
        if (!ph.isEmpty()) {
            parts.add("电话：" + ph);
        }
        String addr = strVal(profile.get("address"));
        if (!addr.isEmpty()) {
            parts.add("地址：" + addr);
        }
        return String.join("；", parts);
    }

    private static String formatNaturalPersonContactLine(Map<String, Object> n) {
        List<String> parts = new ArrayList<>();
        String ph = strVal(n.get("contactPhone"));
        if (!ph.isEmpty()) {
            parts.add("电话：" + ph);
        }
        String em = strVal(n.get("email"));
        if (!em.isEmpty()) {
            parts.add("邮箱：" + em);
        }
        String addr = strVal(n.get("address"));
        if (!addr.isEmpty()) {
            parts.add("地址：" + addr);
        }
        return String.join("；", parts);
    }

    @Transactional
    public Map<String, Object> generateContract(long quoteResultId, ContractGenerateRequest req) throws Exception {
        QuoteResult r = resultMapper.findById(tid(),quoteResultId);
        if (r == null) throw new IllegalArgumentException("报价结果不存在");
        QuoteProject p = projectMapper.findById(tid(),r.getQuoteProjectId());
        if (p == null) throw new IllegalArgumentException("报价关联项目不存在");
        AiAnalysisConfig cfg = aiConfigService.getConfig();
        if (cfg.getApiKey() == null || cfg.getApiKey().isBlank()) {
            throw new IllegalStateException("请先在 AI 配置中填写 DeepSeek API Key");
        }
        if (!Boolean.TRUE.equals(cfg.getEnabled())) {
            throw new IllegalStateException("请先在 AI 配置中开启单次提交 AI 分析（共用 DeepSeek）");
        }
        String model = cfg.getModel() != null && !cfg.getModel().isBlank() ? cfg.getModel() : "deepseek-chat";
        String templateHint = "software_dev".equals(req.getTemplateType())
                ? "中国大陆软件开发外包合同，需包含：标的与交付范围、交付与验收、价款与支付、知识产权、保密、违约责任、争议解决等条款；须引用下文「结构化事实」中的付款、交付物、验收、质保、里程碑等，与事实矛盾处以事实为准。"
                : "软件维护服务合同，明确维护范围、响应时间、费用与期限；须结合下文结构化事实。";
        String userPrompt = buildContractUserPrompt(p, r, req, templateHint);
        List<DeepSeekClient.ChatMessage> messages = List.of(
                new DeepSeekClient.ChatMessage("system", "你是资深法务助理，擅长 IT 外包合同起草。必须基于用户提供的结构化事实撰写正文：付款阶段、交付物清单、验收与异议期、免费质保、里程碑、争议解决方式等须与事实一致；事实未提供处用「待双方确认」占位，勿编造与上文金额、人天、风险合计相矛盾的条款。输出可直接给商务使用的合同正文（Markdown）。"),
                new DeepSeekClient.ChatMessage("user", userPrompt)
        );
        // 合同正文单次生成可能较长：这里显式收紧输出预算，降低 nginx 侧超时概率。
        DeepSeekClient.ChatResult result = deepSeekClient.chatWithUsage(cfg.getApiKey(), model, messages, false, 2048);
        String content = result != null ? result.getContent() : null;
        if (content == null || content.isBlank()) throw new IllegalStateException("AI 未返回合同内容");
        QuoteContractDraft d = contractDraftMapper.findByResultId(tid(),quoteResultId);
        if (d == null) {
            d = new QuoteContractDraft();
            d.setQuoteResultId(quoteResultId);
            d.setClientName(req.getClientName());
            d.setCompanyName(req.getCompanyName());
            d.setTemplateType(req.getTemplateType() != null ? req.getTemplateType() : "software_dev");
            d.setAiPromptSnapshot(userPrompt);
            d.setAiRawResponse(content);
            d.setEditedContent(content);
            d.setTenantId(tid());
            contractDraftMapper.insert(d);
        } else {
            d.setTenantId(tid());
            d.setClientName(req.getClientName());
            d.setCompanyName(req.getCompanyName());
            d.setTemplateType(req.getTemplateType() != null ? req.getTemplateType() : "software_dev");
            d.setAiPromptSnapshot(userPrompt);
            d.setAiRawResponse(content);
            d.setEditedContent(content);
            contractDraftMapper.updateByResultId(d);
        }
        Map<String, Object> out = new HashMap<>();
        out.put("editedContent", content);
        out.put("quoteResultId", quoteResultId);
        return out;
    }

    public Map<String, Object> getContract(long quoteResultId) {
        QuoteContractDraft d = contractDraftMapper.findByResultId(tid(),quoteResultId);
        if (d == null) return null;
        Map<String, Object> m = new HashMap<>();
        m.put("clientName", d.getClientName());
        m.put("companyName", d.getCompanyName());
        m.put("templateType", d.getTemplateType());
        m.put("aiRawResponse", d.getAiRawResponse());
        m.put("editedContent", d.getEditedContent());
        return m;
    }

    @Transactional
    public void updateContract(long quoteResultId, ContractUpdateRequest req) {
        if (contractDraftMapper.findByResultId(tid(),quoteResultId) == null) {
            throw new IllegalArgumentException("合同草稿不存在，请先生成");
        }
        contractDraftMapper.updateEditedContent(tid(),quoteResultId, req.getEditedContent());
    }

    public Map<String, Object> exportContractHtml(long quoteResultId) {
        QuoteContractDraft d = contractDraftMapper.findByResultId(tid(),quoteResultId);
        if (d == null) throw new IllegalArgumentException("合同草稿不存在");
        String body = d.getEditedContent() != null ? d.getEditedContent() : d.getAiRawResponse();
        String html = "<!DOCTYPE html><html><head><meta charset=\"UTF-8\"/><title>合同</title></head><body><pre style='white-space:pre-wrap;font-family:sans-serif'>"
                + esc(body != null ? body : "") + "</pre></body></html>";
        documentMapper.insert(tid(), quoteResultId, "contract", html, 1);
        Map<String, Object> data = new HashMap<>();
        data.put("html", html);
        data.put("filename", "contract-" + quoteResultId + ".html");
        return data;
    }

    private Map<String, Object> projectToListItemMap(QuoteProject p) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", p.getId());
        m.put("name", p.getName());
        m.put("projectType", p.getProjectType());
        m.put("techStack", p.getTechStack());
        m.put("designType", p.getDesignType());
        m.put("dataMigration", p.getDataMigration());
        m.put("concurrency", p.getConcurrency());
        m.put("securityLevel", p.getSecurityLevel());
        m.put("deployType", p.getDeployType());
        m.put("status", p.getStatus());
        m.put("linkTableId", p.getLinkTableId());
        m.put("prdSummary", p.getPrdSummary());
        m.put("aiRequirementText", p.getAiRequirementText());
        m.put("quoteVendorName", p.getQuoteVendorName());
        m.put("quoteContactInfo", p.getQuoteContactInfo());
        m.put("quoteValidityNote", p.getQuoteValidityNote());
        m.put("quoteSubjectMode", p.getQuoteSubjectMode() != null ? p.getQuoteSubjectMode() : "legal_entity");
        m.put("quoteKind", p.getQuoteKind() != null ? p.getQuoteKind() : "single");
        m.put("githubRepoFullName", p.getGithubRepoFullName());
        m.put("githubRepoHtmlUrl", p.getGithubRepoHtmlUrl());
        m.put("githubWebhookId", p.getGithubWebhookId());
        m.put("provisionStatus", p.getProvisionStatus());
        m.put("provisionLastError", p.getProvisionLastError());
        m.put("provisionSyncedToCollab", p.getProvisionSyncedToCollab());
        m.put("provisionSyncedAt", p.getProvisionSyncedAt());
        m.put("createdAt", p.getCreatedAt());
        m.put("updatedAt", p.getUpdatedAt());
        return m;
    }

    private Map<String, Object> projectToMap(QuoteProject p) {
        Map<String, Object> m = projectToListItemMap(p);
        m.put("quoteContractContext", parseContractContextMap(p.getQuoteContractContextJson()));
        return m;
    }

    private Map<String, Object> parseContractContextMap(String json) {
        if (json == null || json.isBlank()) return new LinkedHashMap<>();
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            return new LinkedHashMap<>();
        }
    }

    private Map<String, Object> resultToMap(QuoteResult r) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", r.getId());
        m.put("quoteProjectId", r.getQuoteProjectId());
        m.put("totalDays", r.getTotalDays());
        m.put("estimatedDurationDays", r.getEstimatedDurationDays());
        m.put("durationCoefficientUsed", r.getDurationCoefficientUsed());
        m.put("baseAmount", r.getBaseAmount());
        m.put("riskPctTotal", r.getRiskPctTotal());
        m.put("riskAmount", r.getRiskAmount());
        m.put("finalAmount", r.getFinalAmount());
        m.put("baselineFinalAmount", r.getBaselineFinalAmount());
        m.put("priceScaleFactor", r.getPriceScaleFactor());
        m.put("adjustedFinalAmount", r.getAdjustedFinalAmount());
        m.put("priceAdjustNote", r.getPriceAdjustNote());
        m.put("confidenceScore", r.getConfidenceScore());
        m.put("pricePerDayUsed", r.getPricePerDayUsed());
        m.put("regionLabelUsed", r.getRegionLabelUsed());
        m.put("createdAt", r.getCreatedAt());
        return m;
    }

    /**
     * 将模型总价按人天权重分摊到各行，作为基线行金额（调价前）。
     */
    private void distributeBaselineLinePrices(long projectId, BigDecimal modelTotal) {
        if (modelTotal == null || modelTotal.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        List<QuoteModule> modules = moduleMapper.listByProjectId(tid(), projectId);
        List<QuoteItem> all = new ArrayList<>();
        BigDecimal totalWeight = BigDecimal.ZERO;
        for (QuoteModule mo : modules) {
            for (QuoteItem it : itemMapper.listByModuleId(tid(), mo.getId())) {
                BigDecimal w = it.getEstimatedDays() != null ? it.getEstimatedDays() : BigDecimal.ZERO;
                if (w.compareTo(BigDecimal.ZERO) <= 0) {
                    w = new BigDecimal("0.01");
                }
                all.add(it);
                totalWeight = totalWeight.add(w);
            }
        }
        if (all.isEmpty() || totalWeight.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        BigDecimal acc = BigDecimal.ZERO;
        for (int i = 0; i < all.size(); i++) {
            QuoteItem it = all.get(i);
            BigDecimal w = it.getEstimatedDays() != null ? it.getEstimatedDays() : BigDecimal.ZERO;
            if (w.compareTo(BigDecimal.ZERO) <= 0) {
                w = new BigDecimal("0.01");
            }
            BigDecimal snap;
            if (i < all.size() - 1) {
                snap = modelTotal.multiply(w).divide(totalWeight, 2, RoundingMode.HALF_UP);
                acc = acc.add(snap);
            } else {
                snap = modelTotal.subtract(acc).setScale(2, RoundingMode.HALF_UP);
            }
            itemMapper.updateLinePrices(tid(), it.getId(), snap, snap);
        }
    }

    /**
     * 对最新报价结果做商务调价：可缩放行按同一系数等比例调整；excluded 行保持基线行金额。
     */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> applyPriceAdjustment(long projectId, QuotePriceAdjustRequest req) {
        if (req == null) {
            throw new IllegalArgumentException("请求体不能为空");
        }
        QuoteResult r = resultMapper.findLatestByProjectId(tid(), projectId);
        if (r == null) {
            throw new IllegalArgumentException("请先计算报价");
        }
        BigDecimal baseline = r.getBaselineFinalAmount() != null ? r.getBaselineFinalAmount() : r.getFinalAmount();
        if (baseline == null) {
            baseline = r.getFinalAmount();
        }

        List<QuoteModule> modules = moduleMapper.listByProjectId(tid(), projectId);
        List<QuoteItem> all = new ArrayList<>();
        BigDecimal scalablePool = BigDecimal.ZERO;
        BigDecimal frozenSum = BigDecimal.ZERO;
        for (QuoteModule mo : modules) {
            for (QuoteItem it : itemMapper.listByModuleId(tid(), mo.getId())) {
                all.add(it);
                BigDecimal snap = it.getLinePriceSnap();
                if (snap == null) {
                    throw new IllegalStateException("行金额未初始化，请先重新「计算报价」后再调价");
                }
                if (Boolean.TRUE.equals(it.getExcludedFromScale())) {
                    frozenSum = frozenSum.add(snap);
                } else {
                    scalablePool = scalablePool.add(snap);
                }
            }
        }
        if (all.isEmpty()) {
            throw new IllegalStateException("无功能行，无法调价");
        }

        BigDecimal k;
        BigDecimal scalableTarget;
        if (req.getTargetAmount() != null && req.getTargetAmount().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal target = req.getTargetAmount().setScale(2, RoundingMode.HALF_UP);
            scalableTarget = target.subtract(frozenSum).setScale(2, RoundingMode.HALF_UP);
            if (scalablePool.compareTo(BigDecimal.ZERO) <= 0) {
                if (scalableTarget.abs().compareTo(new BigDecimal("0.02")) > 0) {
                    throw new IllegalArgumentException("当前全部为不参与缩放行，无法匹配目标总价");
                }
                k = BigDecimal.ONE;
                scalableTarget = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
            } else {
                k = scalableTarget.divide(scalablePool, 8, RoundingMode.HALF_UP);
            }
        } else if (req.getScaleFactor() != null && req.getScaleFactor().compareTo(BigDecimal.ZERO) > 0) {
            k = req.getScaleFactor();
            scalableTarget = scalablePool.multiply(k).setScale(2, RoundingMode.HALF_UP);
        } else {
            throw new IllegalArgumentException("请提供 targetAmount 或 scaleFactor");
        }
        if (k.compareTo(new BigDecimal("0.01")) < 0 || k.compareTo(new BigDecimal("100")) > 0) {
            throw new IllegalArgumentException("调价系数需在 0.01～100 之间");
        }

        int scCount = (int) all.stream().filter(x -> !Boolean.TRUE.equals(x.getExcludedFromScale())).count();
        BigDecimal acc = BigDecimal.ZERO;
        int sIdx = 0;
        BigDecimal newTotal = BigDecimal.ZERO;
        for (QuoteItem it : all) {
            BigDecimal snap = it.getLinePriceSnap();
            BigDecimal adj;
            if (Boolean.TRUE.equals(it.getExcludedFromScale())) {
                adj = snap;
            } else {
                if (scalablePool.compareTo(BigDecimal.ZERO) <= 0) {
                    adj = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
                } else if (sIdx < scCount - 1) {
                    adj = scalableTarget.multiply(snap).divide(scalablePool, 2, RoundingMode.HALF_UP);
                    acc = acc.add(adj);
                } else {
                    adj = scalableTarget.subtract(acc).setScale(2, RoundingMode.HALF_UP);
                }
                sIdx++;
            }
            itemMapper.updateLinePrices(tid(), it.getId(), snap, adj);
            newTotal = newTotal.add(adj);
        }

        r.setBaselineFinalAmount(baseline);
        r.setPriceScaleFactor(k);
        r.setAdjustedFinalAmount(newTotal);
        r.setPriceAdjustNote(trimToNull(req.getNote()));
        r.setFinalAmount(newTotal);
        resultMapper.updatePriceAdjustment(tid(), r.getId(), newTotal, baseline, k, newTotal, r.getPriceAdjustNote());
        QuoteResult out = resultMapper.findById(tid(), r.getId());
        return resultToMap(out != null ? out : r);
    }

    public List<Map<String, Object>> listPresetItems(boolean includeDisabled) {
        List<QuotePresetItem> list = includeDisabled ? presetItemMapper.listAll(tid()) : presetItemMapper.listEnabled(tid());
        List<Map<String, Object>> out = new ArrayList<>();
        for (QuotePresetItem it : list) {
            out.add(presetItemToMap(it));
        }
        return out;
    }

    private Map<String, Object> presetItemToMap(QuotePresetItem it) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", it.getId());
        m.put("name", it.getName());
        m.put("complexity", it.getComplexity());
        m.put("category", it.getCategory());
        m.put("sortOrder", it.getSortOrder());
        m.put("enabled", Boolean.TRUE.equals(it.getEnabled()));
        m.put("createdAt", it.getCreatedAt());
        m.put("updatedAt", it.getUpdatedAt());
        return m;
    }

    @Transactional
    public long createPresetItem(QuotePresetItemSaveDto dto) {
        if (dto.getName() == null || dto.getName().isBlank()) {
            throw new IllegalArgumentException("预设功能点名称不能为空");
        }
        QuotePresetItem row = new QuotePresetItem();
        row.setName(dto.getName().trim());
        row.setComplexity(nvl(dto.getComplexity(), "standard"));
        row.setCategory(dto.getCategory() != null && !dto.getCategory().isBlank() ? dto.getCategory().trim() : null);
        row.setSortOrder(dto.getSortOrder() != null ? dto.getSortOrder() : 0);
        row.setEnabled(dto.getEnabled() == null || dto.getEnabled());
        row.setTenantId(tid());
        presetItemMapper.insert(row);
        return row.getId();
    }

    @Transactional
    public void updatePresetItem(long id, QuotePresetItemSaveDto dto) {
        QuotePresetItem existing = presetItemMapper.findById(tid(), id);
        if (existing == null) throw new IllegalArgumentException("预设项不存在");
        if (dto.getName() == null || dto.getName().isBlank()) {
            throw new IllegalArgumentException("预设功能点名称不能为空");
        }
        existing.setName(dto.getName().trim());
        existing.setComplexity(nvl(dto.getComplexity(), "standard"));
        existing.setCategory(dto.getCategory() != null && !dto.getCategory().isBlank() ? dto.getCategory().trim() : null);
        if (dto.getSortOrder() != null) existing.setSortOrder(dto.getSortOrder());
        if (dto.getEnabled() != null) existing.setEnabled(dto.getEnabled());
        presetItemMapper.update(existing);
    }

    @Transactional
    public void deletePresetItem(long id) {
        if (presetItemMapper.findById(tid(), id) == null) throw new IllegalArgumentException("预设项不存在");
        presetItemMapper.deleteById(tid(), id);
    }

    @Transactional
    public void updateRiskConfigs(QuoteRiskConfigBatchUpdate batch) {
        if (batch.getItems() == null || batch.getItems().isEmpty()) {
            throw new IllegalArgumentException("请至少提交一条风险配置");
        }
        for (QuoteRiskConfigUpdateItem it : batch.getItems()) {
            if (it.getId() == null) throw new IllegalArgumentException("风险配置 id 不能为空");
            if (riskConfigMapper.countById(tid(), it.getId()) == 0) throw new IllegalArgumentException("无效的风险配置 id: " + it.getId());
            if (it.getLabel() == null || it.getLabel().isBlank()) throw new IllegalArgumentException("风险项名称不能为空");
            BigDecimal pct = it.getDefaultPct() != null ? it.getDefaultPct() : BigDecimal.ZERO;
            if (pct.compareTo(RISK_PCT_MIN) < 0 || pct.compareTo(RISK_PCT_MAX) > 0) {
                throw new IllegalArgumentException("风险百分比需在 " + RISK_PCT_MIN + "～" + RISK_PCT_MAX + " 之间（可为负表示降价）");
            }
            int en = Boolean.TRUE.equals(it.getEnabled()) ? 1 : 0;
            riskConfigMapper.updateRow(tid(), it.getId(), it.getLabel().trim(), pct.setScale(2, RoundingMode.HALF_UP), en);
        }
    }

    private static boolean mapRowEnabled(Map<String, Object> m) {
        if (m == null) return false;
        Object o = m.get("enabled");
        return Boolean.TRUE.equals(o) || (o instanceof Number && ((Number) o).intValue() == 1);
    }

    private void validateBaselineDays(BigDecimal d) {
        if (d == null) throw new IllegalArgumentException("人天数不能为空");
        if (d.compareTo(new BigDecimal("0.01")) < 0 || d.compareTo(new BigDecimal("999.99")) > 0) {
            throw new IllegalArgumentException("人天需在 0.01～999.99 之间");
        }
    }

    @Transactional
    public long createBaseline(QuoteBaselineSaveDto dto) {
        String ts = dto.getTechStack() != null ? dto.getTechStack().trim() : "";
        String cx = dto.getComplexity() != null ? dto.getComplexity().trim() : "";
        if (ts.isEmpty()) throw new IllegalArgumentException("技术栈不能为空");
        if (cx.isEmpty()) throw new IllegalArgumentException("复杂度不能为空");
        validateBaselineDays(dto.getDays());
        if (baselineMapper.countByStackAndComplexity(tid(),ts, cx) > 0) {
            throw new IllegalArgumentException("该技术栈与复杂度组合已存在");
        }
        QuoteBaseline row = new QuoteBaseline();
        row.setTechStack(ts);
        row.setComplexity(cx);
        row.setDays(dto.getDays().setScale(2, RoundingMode.HALF_UP));
        row.setTenantId(tid());
        baselineMapper.insert(row);
        return row.getId();
    }

    @Transactional
    public void updateBaseline(long id, QuoteBaselineSaveDto dto) {
        if (baselineMapper.findById(tid(),id) == null) throw new IllegalArgumentException("人天基准不存在");
        String ts = dto.getTechStack() != null ? dto.getTechStack().trim() : "";
        String cx = dto.getComplexity() != null ? dto.getComplexity().trim() : "";
        if (ts.isEmpty()) throw new IllegalArgumentException("技术栈不能为空");
        if (cx.isEmpty()) throw new IllegalArgumentException("复杂度不能为空");
        validateBaselineDays(dto.getDays());
        if (baselineMapper.countByStackAndComplexityExcluding(tid(),ts, cx, id) > 0) {
            throw new IllegalArgumentException("该技术栈与复杂度组合已被其他行占用");
        }
        QuoteBaseline row = new QuoteBaseline();
        row.setId(id);
        row.setTenantId(tid());
        row.setTechStack(ts);
        row.setComplexity(cx);
        row.setDays(dto.getDays().setScale(2, RoundingMode.HALF_UP));
        baselineMapper.update(row);
    }

    @Transactional
    public void deleteBaseline(long id) {
        if (baselineMapper.findById(tid(),id) == null) throw new IllegalArgumentException("人天基准不存在");
        baselineMapper.deleteById(tid(), id);
    }

    private void validatePricePerDay(BigDecimal p) {
        if (p == null) throw new IllegalArgumentException("单价不能为空");
        if (p.compareTo(new BigDecimal("1")) < 0 || p.compareTo(new BigDecimal("999999.99")) > 0) {
            throw new IllegalArgumentException("人天单价需在 1～999999.99 之间");
        }
    }

    private static final BigDecimal DEFAULT_DURATION_COEFFICIENT = new BigDecimal("1.2000");

    /** 计算报价时：空或非法则回退默认 1.2 */
    private BigDecimal normalizeDurationCoefficientForCalc(Object raw) {
        if (raw == null) return DEFAULT_DURATION_COEFFICIENT;
        BigDecimal c = toBd(raw);
        if (c.compareTo(new BigDecimal("0.01")) < 0 || c.compareTo(new BigDecimal("100")) > 0) {
            return DEFAULT_DURATION_COEFFICIENT;
        }
        return c.setScale(4, RoundingMode.HALF_UP);
    }

    private BigDecimal resolveDurationCoefficientForSave(BigDecimal fromDto) {
        BigDecimal c = fromDto != null ? fromDto : DEFAULT_DURATION_COEFFICIENT;
        if (c.compareTo(new BigDecimal("0.01")) < 0 || c.compareTo(new BigDecimal("100")) > 0) {
            throw new IllegalArgumentException("工期系数需在 0.01～100 之间");
        }
        return c.setScale(4, RoundingMode.HALF_UP);
    }

    @Transactional
    public long createPriceConfig(QuotePriceConfigSaveDto dto) {
        String rl = dto.getRegionLabel() != null ? dto.getRegionLabel().trim() : "";
        if (rl.isEmpty()) throw new IllegalArgumentException("地域/档位名称不能为空");
        validatePricePerDay(dto.getPricePerDay());
        if (priceConfigMapper.countByRegionLabel(tid(), rl) > 0) {
            throw new IllegalArgumentException("该档位名称已存在");
        }
        QuotePriceConfigRow row = new QuotePriceConfigRow();
        row.setRegionLabel(rl);
        row.setPricePerDay(dto.getPricePerDay().setScale(2, RoundingMode.HALF_UP));
        String cur = dto.getCurrency() != null && !dto.getCurrency().isBlank() ? dto.getCurrency().trim().toUpperCase() : "CNY";
        if (cur.length() > 8) throw new IllegalArgumentException("币种代码过长");
        row.setCurrency(cur);
        row.setDurationCoefficient(resolveDurationCoefficientForSave(dto.getDurationCoefficient()));
        row.setEnabled(dto.getEnabled() == null || dto.getEnabled());
        row.setTenantId(tid());
        priceConfigMapper.insert(row);
        return row.getId();
    }

    @Transactional
    public void updatePriceConfig(long id, QuotePriceConfigSaveDto dto) {
        Map<String, Object> cur = priceConfigMapper.findById(tid(),id);
        if (cur == null) throw new IllegalArgumentException("单价配置不存在");
        String rl = dto.getRegionLabel() != null ? dto.getRegionLabel().trim() : "";
        if (rl.isEmpty()) throw new IllegalArgumentException("地域/档位名称不能为空");
        validatePricePerDay(dto.getPricePerDay());
        if (priceConfigMapper.countByRegionLabelExcluding(tid(), rl, id) > 0) {
            throw new IllegalArgumentException("该档位名称已被其他行占用");
        }
        boolean wasEn = mapRowEnabled(cur);
        boolean newEn = dto.getEnabled() == null || dto.getEnabled();
        if (wasEn && !newEn && priceConfigMapper.countEnabled(tid()) <= 1) {
            throw new IllegalArgumentException("至少保留一条启用的单价档位");
        }
        QuotePriceConfigRow row = new QuotePriceConfigRow();
        row.setId(id);
        row.setTenantId(tid());
        row.setRegionLabel(rl);
        row.setPricePerDay(dto.getPricePerDay().setScale(2, RoundingMode.HALF_UP));
        String curc = dto.getCurrency() != null && !dto.getCurrency().isBlank() ? dto.getCurrency().trim().toUpperCase() : "CNY";
        if (curc.length() > 8) throw new IllegalArgumentException("币种代码过长");
        row.setCurrency(curc);
        row.setDurationCoefficient(resolveDurationCoefficientForSave(dto.getDurationCoefficient()));
        row.setEnabled(newEn);
        priceConfigMapper.update(row);
    }

    @Transactional
    public void deletePriceConfig(long id) {
        Map<String, Object> cur = priceConfigMapper.findById(tid(),id);
        if (cur == null) throw new IllegalArgumentException("单价配置不存在");
        if (mapRowEnabled(cur) && priceConfigMapper.countEnabled(tid()) <= 1) {
            throw new IllegalArgumentException("至少保留一条启用的单价档位，无法删除");
        }
        priceConfigMapper.deleteById(tid(), id);
    }

    // --- 合同：乙方主体模板（系统级）与附件 HTML ---

    public Map<String, Object> getPartyBProfile() {
        return new LinkedHashMap<>(systemConfigService.getQuotePartyBProfile());
    }

    public void savePartyBProfile(Map<String, Object> profile) throws JsonProcessingException {
        systemConfigService.saveQuotePartyBProfile(profile);
    }

    public Map<String, Object> buildContractAttachmentFunctionList(long projectId) {
        QuoteProject p = projectMapper.findById(tid(),projectId);
        if (p == null) throw new IllegalArgumentException("项目不存在");
        Map<String, Object> data = new HashMap<>();
        data.put("html", renderAttachmentFunctionListHtml(p));
        data.put("filename", "attachment-1-function-list-" + projectId + ".html");
        return data;
    }

    public Map<String, Object> buildContractAttachmentMilestoneSchedule(long projectId) {
        QuoteProject p = projectMapper.findById(tid(),projectId);
        if (p == null) throw new IllegalArgumentException("项目不存在");
        Map<String, Object> ctx = parseContractContextMap(p.getQuoteContractContextJson());
        Map<String, Object> data = new HashMap<>();
        data.put("html", renderAttachmentMilestoneHtml(p, ctx));
        data.put("filename", "attachment-3-milestones-" + projectId + ".html");
        return data;
    }

    /** 附件二：验收标准（草案）HTML */
    public Map<String, Object> buildContractAttachmentAcceptanceStandards(long projectId) {
        QuoteProject p = projectMapper.findById(tid(),projectId);
        if (p == null) throw new IllegalArgumentException("项目不存在");
        Map<String, Object> ctx = parseContractContextMap(p.getQuoteContractContextJson());
        Map<String, Object> data = new HashMap<>();
        data.put("html", renderAttachmentAcceptanceHtml(p, ctx));
        data.put("filename", "attachment-2-acceptance-" + projectId + ".html");
        return data;
    }

    private static final Map<String, String> DELIVERABLE_LABELS = new LinkedHashMap<>();

    static {
        DELIVERABLE_LABELS.put("source_code", "源代码（约定交付形式，如 Git 仓库只读权限或压缩包）");
        DELIVERABLE_LABELS.put("deploy_doc", "部署说明与环境配置文档");
        DELIVERABLE_LABELS.put("api_doc", "接口/API 文档");
        DELIVERABLE_LABELS.put("db_script", "数据库脚本与迁移说明");
        DELIVERABLE_LABELS.put("test_report", "测试报告或测试要点说明");
        DELIVERABLE_LABELS.put("user_manual", "用户/管理员操作说明（简版）");
        DELIVERABLE_LABELS.put("training", "远程培训或交付讲解（场次待确认）");
    }

    private static final int CONTRACT_FACTS_MAX_CHARS = 12000;

    private String buildContractUserPrompt(QuoteProject p, QuoteResult r, ContractGenerateRequest req, String templateHint) {
        String cn = req.getClientName() != null && !req.getClientName().isBlank() ? req.getClientName().trim() : "甲方";
        String yn = req.getCompanyName() != null && !req.getCompanyName().isBlank() ? req.getCompanyName().trim() : "乙方";
        String facts = buildContractStructuredFactsBlock(p, r);
        if (facts.length() > CONTRACT_FACTS_MAX_CHARS) {
            facts = facts.substring(0, CONTRACT_FACTS_MAX_CHARS) + "\n\n…（结构化事实过长已截断，完整以系统项目与附件为准）";
        }
        return """
                请根据以下信息起草合同正文（中文，Markdown 标题分级）。可在首部增加「附件清单」，其中附件一《功能清单》、附件三《里程碑计划》可由双方另行确认签章。
                
                【合同类型要求】
                %s
                
                【甲乙方称呼（正文抬头可用）】
                甲方（委托方/客户）：%s
                乙方（受托方/开发方）：%s
                
                【项目与经济核心数据】
                项目名称：%s
                最终报价金额（人民币）：%s 元
                总人天：%s
                预计工期（天，总人天×工期系数）：%s
                基础金额：%s 元；风险调整后含税前报价逻辑对应金额：%s 元；人天单价（本次计价采用）：%s 元/人天；风险合计百分比（本次已勾选）：%s%%
                
                【以下为结构化事实，请写入对应条款，勿与数字矛盾】
                %s
                """.formatted(
                templateHint,
                cn,
                yn,
                p.getName() != null ? p.getName() : "",
                r.getFinalAmount().toPlainString(),
                r.getTotalDays().toPlainString(),
                r.getEstimatedDurationDays() != null ? r.getEstimatedDurationDays().toPlainString() : "0",
                r.getBaseAmount().toPlainString(),
                r.getFinalAmount().toPlainString(),
                r.getPricePerDayUsed().toPlainString(),
                r.getRiskPctTotal() != null ? r.getRiskPctTotal().toPlainString() : "0",
                facts
        );
    }

    private String buildContractStructuredFactsBlock(QuoteProject p, QuoteResult r) {
        StringBuilder sb = new StringBuilder();
        sb.append("### 项目属性\n");
        sb.append("- 项目类型：").append(labelProjectType(p.getProjectType())).append("\n");
        sb.append("- 报价模式：").append("solution".equals(p.getQuoteKind()) ? "解决方案级（多交付物统一签约）" : "单体项目").append("\n");
        sb.append("- 技术栈（项目默认/共用）：").append(labelTechStack(p.getTechStack())).append("\n");
        sb.append("- 设计：").append(labelDesign(p.getDesignType())).append("；数据/对接：").append(labelDataMigration(p.getDataMigration())).append("\n");
        sb.append("- 并发量级：").append(labelConcurrency(p.getConcurrency())).append("；安全：").append(labelSecurity(p.getSecurityLevel())).append("；部署：").append(labelDeploy(p.getDeployType())).append("\n");
        String prd = p.getPrdSummary();
        if (prd != null && !prd.isBlank()) {
            sb.append("\n### PRD/需求摘要\n").append(prd.trim()).append("\n");
        }
        sb.append("\n### 功能清单（模块-功能点-复杂度-数量-估算人天）\n");
        if ("solution".equals(p.getQuoteKind())) {
            sb.append("（解决方案级：按交付物分组；人天基准可能因交付物技术栈不同而不同）\n");
        }
        sb.append(buildFunctionListMarkdownForPrompt(p));
        sb.append("\n### 本次报价勾选的风险项（已计入风险合计）\n");
        sb.append(formatSelectedRisksForPrompt(r));
        sb.append("\n### 报价时审核清单勾选结果\n");
        sb.append(formatAuditChecklistForPrompt(r));
        sb.append("\n### 合同补充（付款/质保/验收/交付物/里程碑/争议）\n");
        sb.append(formatContractContextForPrompt(parseContractContextMap(p.getQuoteContractContextJson())));
        sb.append("\n### 乙方（受托方）主体与收款信息（系统模板：含法人/组织与自然人两套，按实际签约主体选用对应小节；若与抬头乙方名称不一致以本段为准）\n");
        sb.append(formatPartyBProfileForPrompt(systemConfigService.getQuotePartyBProfile()));
        return sb.toString();
    }

    private String buildFunctionListMarkdownForPrompt(QuoteProject p) {
        StringBuilder sb = new StringBuilder();
        if ("solution".equals(p.getQuoteKind())) {
            sb.append("| 交付物 | 技术栈(计价) | 模块 | 功能点 | 复杂度 | 数量 | 估算人天 |\n| --- | --- | --- | --- | --- | --- | --- |\n");
            for (QuoteModule m : moduleMapper.listByProjectId(tid(), p.getId())) {
                String dk = m.getDeliverableKey() != null ? m.getDeliverableKey() : "default";
                String dlab = m.getDeliverableLabel() != null && !m.getDeliverableLabel().isBlank()
                        ? m.getDeliverableLabel() : dk;
                String mts = trimToNull(m.getTechStack());
                String stackLabel = mts != null ? labelTechStack(mts) : ("（同项目默认 " + labelTechStack(p.getTechStack()) + "）");
                for (QuoteItem it : itemMapper.listByModuleId(tid(), m.getId())) {
                    sb.append("| ").append(escMdCell(dlab + " (" + dk + ")")).append(" | ")
                            .append(escMdCell(stackLabel)).append(" | ")
                            .append(escMdCell(m.getName())).append(" | ").append(escMdCell(it.getName())).append(" | ")
                            .append(escMdCell(it.getComplexity())).append(" | ").append(it.getQuantity()).append(" | ")
                            .append(it.getEstimatedDays() != null ? it.getEstimatedDays().toPlainString() : "0").append(" |\n");
                }
            }
        } else {
            sb.append("| 模块 | 功能点 | 复杂度 | 数量 | 估算人天 |\n| --- | --- | --- | --- | --- |\n");
            for (QuoteModule m : moduleMapper.listByProjectId(tid(), p.getId())) {
                for (QuoteItem it : itemMapper.listByModuleId(tid(), m.getId())) {
                    sb.append("| ").append(escMdCell(m.getName())).append(" | ").append(escMdCell(it.getName())).append(" | ")
                            .append(escMdCell(it.getComplexity())).append(" | ").append(it.getQuantity()).append(" | ")
                            .append(it.getEstimatedDays() != null ? it.getEstimatedDays().toPlainString() : "0").append(" |\n");
                }
            }
        }
        return sb.toString();
    }

    private static String escMdCell(String s) {
        if (s == null) return "";
        return s.replace("|", "\\|").replace("\n", " ");
    }

    private String formatSelectedRisksForPrompt(QuoteResult r) {
        Map<String, String> keyToLabel = new LinkedHashMap<>();
        for (Map<String, Object> row : riskConfigMapper.listAll(tid())) {
            Object k = row.get("riskKey");
            if (k != null) keyToLabel.put(k.toString(), Objects.toString(row.get("label"), k.toString()));
        }
        List<String> keys;
        try {
            keys = objectMapper.readValue(
                    r.getSelectedRisksJson() != null ? r.getSelectedRisksJson() : "[]",
                    new TypeReference<List<String>>() {});
        } catch (Exception e) {
            keys = List.of();
        }
        if (keys == null || keys.isEmpty()) return "（本次未勾选额外风险项，或仅使用加急等内置逻辑）\n";
        StringBuilder sb = new StringBuilder();
        for (String k : keys) {
            sb.append("- ").append(keyToLabel.getOrDefault(k, k)).append("（risk_key=").append(k).append("）\n");
        }
        sb.append("- 风险合计百分比（本次计算结果）：").append(r.getRiskPctTotal() != null ? r.getRiskPctTotal().toPlainString() : "0").append("%\n");
        return sb.toString();
    }

    private String formatAuditChecklistForPrompt(QuoteResult r) {
        Map<String, Object> ac;
        try {
            ac = objectMapper.readValue(
                    r.getAuditChecklistJson() != null ? r.getAuditChecklistJson() : "{}",
                    new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            ac = Map.of();
        }
        if (ac.isEmpty()) return "（无勾选记录）\n";
        StringBuilder sb = new StringBuilder();
        sb.append("- 限制需求变更次数：").append(boolCn(ac.get("changeLimit"))).append("\n");
        sb.append("- 验收标准明确：").append(boolCn(ac.get("acceptanceClear"))).append("\n");
        sb.append("- 付款节点约定：").append(boolCn(ac.get("paymentNodes"))).append("\n");
        sb.append("- 含部署上线费用：").append(boolCn(ac.get("deployFee"))).append("\n");
        sb.append("- 维护期及范围明确：").append(boolCn(ac.get("maintenanceScope"))).append("\n");
        return sb.toString();
    }

    private static String boolCn(Object o) {
        return Boolean.TRUE.equals(o) || (o instanceof Number && ((Number) o).intValue() == 1) ? "是" : "否";
    }

    @SuppressWarnings("unchecked")
    private String formatContractContextForPrompt(Map<String, Object> ctx) {
        if (ctx == null || ctx.isEmpty()) return "（未填写合同补充信息，相关条款请用「待双方确认」）\n";
        StringBuilder sb = new StringBuilder();
        Object plan = ctx.get("paymentPlan");
        if (plan instanceof List<?> list && !list.isEmpty()) {
            sb.append("付款计划（阶段、比例%、触发条件）：\n");
            for (Object o : list) {
                if (o instanceof Map) {
                    Map<String, Object> m = (Map<String, Object>) o;
                    sb.append("- ").append(Objects.toString(m.get("phaseName"), "阶段")).append("：")
                            .append(m.get("percent") != null ? m.get("percent") + "%" : "?%").append("，")
                            .append(Objects.toString(m.get("triggerNote"), "")).append("\n");
                }
            }
        }
        Object w = ctx.get("warrantyMonths");
        if (w != null) sb.append("免费质保月数：").append(w).append("\n");
        Object tax = ctx.get("taxInvoiceNote");
        if (tax != null && !tax.toString().isBlank()) sb.append("含税/发票说明：").append(tax).append("\n");
        Object sla = ctx.get("maintenanceSlaNote");
        if (sla != null && !sla.toString().isBlank()) sb.append("维护响应/SLA 说明：").append(sla).append("\n");
        Object days = ctx.get("acceptanceObjectionDays");
        if (days != null) sb.append("验收异议期（工作日）：").append(days).append("\n");
        Object acn = ctx.get("acceptanceCriteriaNote");
        if (acn != null && !acn.toString().isBlank()) sb.append("验收标准补充说明：").append(acn).append("\n");
        Object atc = ctx.get("acceptanceTestCases");
        if (atc instanceof List<?> list && !list.isEmpty()) {
            sb.append("验收测试用例/测试清单（结构化，共 ").append(list.size()).append(" 条，与附件二一致）：\n");
            int shown = 0;
            for (Object o : list) {
                if (shown >= 18) {
                    sb.append("…（以下条目略）\n");
                    break;
                }
                if (o instanceof Map<?, ?> mm) {
                    Map<String, Object> m = (Map<String, Object>) mm;
                    sb.append("- ").append(Objects.toString(m.get("caseName"), ""))
                            .append("｜模块：").append(Objects.toString(m.get("module"), ""))
                            .append("｜优先级：").append(Objects.toString(m.get("priority"), ""))
                            .append("｜结果：").append(Objects.toString(m.get("testResult"), "")).append("\n");
                }
                shown++;
            }
        }
        Object del = ctx.get("deliverables");
        if (del instanceof List<?> dlist && !dlist.isEmpty()) {
            sb.append("约定交付物：\n");
            for (Object k : dlist) {
                String key = k != null ? k.toString() : "";
                sb.append("- ").append(DELIVERABLE_LABELS.getOrDefault(key, key)).append("\n");
            }
        }
        Object ms = ctx.get("milestones");
        if (ms instanceof List<?> mlist && !mlist.isEmpty()) {
            sb.append("里程碑（阶段名、相对天数、备注）：\n");
            for (Object o : mlist) {
                if (o instanceof Map) {
                    Map<String, Object> m = (Map<String, Object>) o;
                    sb.append("- ").append(Objects.toString(m.get("name"), ""))
                            .append("：合同生效后约 ").append(Objects.toString(m.get("offsetDays"), "?")).append(" 日；")
                            .append(Objects.toString(m.get("note"), "")).append("\n");
                }
            }
        }
        Object dr = ctx.get("disputeResolutionNote");
        if (dr != null && !dr.toString().isBlank()) sb.append("争议解决（双方意向）：").append(dr).append("\n");
        return sb.toString();
    }

    @SuppressWarnings("unchecked")
    private String formatPartyBProfileForPrompt(Map<String, Object> m) {
        if (m == null || m.isEmpty()) return "（未配置乙方主体模板，请在管理端「报价配置」中填写）\n";
        StringBuilder sb = new StringBuilder();
        sb.append("#### 乙方为法人或其他组织时\n");
        putLine(sb, "法定名称", m.get("legalName"));
        putLine(sb, "统一社会信用代码", m.get("creditCode"));
        putLine(sb, "住所", m.get("address"));
        putLine(sb, "联系人", m.get("contactName"));
        putLine(sb, "联系电话", m.get("contactPhone"));
        putLine(sb, "开户行", m.get("bankName"));
        putLine(sb, "银行账号", m.get("bankAccount"));
        sb.append("\n#### 乙方为自然人时\n");
        Object np = m.get("naturalPerson");
        if (np instanceof Map) {
            Map<String, Object> n = (Map<String, Object>) np;
            putLine(sb, "姓名", n.get("fullName"));
            putLine(sb, "身份证件号码", n.get("idNumber"));
            putLine(sb, "住所（经常居住地）", n.get("address"));
            putLine(sb, "联系电话", n.get("contactPhone"));
            putLine(sb, "电子邮箱", n.get("email"));
            putLine(sb, "开户行", n.get("bankName"));
            putLine(sb, "银行账号", n.get("bankAccount"));
        } else {
            sb.append("（未填写自然人主体模板）\n");
        }
        return sb.toString();
    }

    private static void putLine(StringBuilder sb, String label, Object v) {
        if (v == null || v.toString().isBlank()) return;
        sb.append("- ").append(label).append("：").append(v.toString().trim()).append("\n");
    }

    private String renderAttachmentFunctionListHtml(QuoteProject p) {
        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html><html><head><meta charset=\"UTF-8\"/><title>附件一 功能清单</title>");
        sb.append("<style>body{font-family:sans-serif;padding:24px} table{border-collapse:collapse;width:100%} th,td{border:1px solid #ccc;padding:8px;font-size:13px}</style></head><body>");
        sb.append("<h1>附件一：功能清单（报价项目）</h1>");
        sb.append("<p><strong>项目名称：</strong>").append(esc(p.getName())).append("</p>");
        sb.append("<p>说明：本附件与主合同「开发范围」或「验收依据」挂钩，以双方确认版本为准。</p>");
        sb.append("<table><thead><tr><th>模块</th><th>功能点</th><th>复杂度</th><th>数量</th><th>估算人天</th></tr></thead><tbody>");
        for (QuoteModule m : moduleMapper.listByProjectId(tid(),p.getId())) {
            for (QuoteItem it : itemMapper.listByModuleId(tid(),m.getId())) {
                sb.append("<tr><td>").append(esc(m.getName())).append("</td><td>").append(esc(it.getName()))
                        .append("</td><td>").append(esc(it.getComplexity())).append("</td><td>").append(it.getQuantity())
                        .append("</td><td>").append(it.getEstimatedDays() != null ? esc(it.getEstimatedDays().toPlainString()) : "0")
                        .append("</td></tr>");
            }
        }
        sb.append("</tbody></table></body></html>");
        return sb.toString();
    }

    @SuppressWarnings("unchecked")
    private String renderAttachmentMilestoneHtml(QuoteProject p, Map<String, Object> ctx) {
        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html><html><head><meta charset=\"UTF-8\"/><title>附件三 里程碑计划</title>");
        sb.append("<style>body{font-family:sans-serif;padding:24px} table{border-collapse:collapse;width:100%} th,td{border:1px solid #ccc;padding:8px;font-size:13px}</style></head><body>");
        sb.append("<h1>附件三：项目实施计划 / 里程碑（草案）</h1>");
        sb.append("<p><strong>项目名称：</strong>").append(esc(p.getName())).append("</p>");
        sb.append("<p>说明：日期为相对「合同生效日」的参考天数，正式计划以双方签章为准。</p>");
        sb.append("<table><thead><tr><th>序号</th><th>阶段名称</th><th>参考时间（生效后约 X 日）</th><th>备注</th></tr></thead><tbody>");
        Object ms = ctx != null ? ctx.get("milestones") : null;
        int i = 1;
        if (ms instanceof List<?> mlist) {
            for (Object o : mlist) {
                if (o instanceof Map) {
                    Map<String, Object> m = (Map<String, Object>) o;
                    sb.append("<tr><td>").append(i++).append("</td><td>").append(esc(Objects.toString(m.get("name"), "")))
                            .append("</td><td>").append(esc(Objects.toString(m.get("offsetDays"), "")))
                            .append("</td><td>").append(esc(Objects.toString(m.get("note"), "")))
                            .append("</td></tr>");
                }
            }
        }
        if (i == 1) {
            sb.append("<tr><td colspan=\"4\">（未在项目「合同补充信息」中填写里程碑，请补充后重新导出）</td></tr>");
        }
        sb.append("</tbody></table></body></html>");
        return sb.toString();
    }

    @SuppressWarnings("unchecked")
    private String renderAttachmentAcceptanceHtml(QuoteProject p, Map<String, Object> ctx) {
        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html><html><head><meta charset=\"UTF-8\"/><title>附件二 验收标准与测试用例</title>");
        sb.append("<style>body{font-family:sans-serif;padding:24px;line-height:1.6} h2{font-size:16px;margin-top:20px} ol{padding-left:20px} table{border-collapse:collapse;width:100%} th,td{border:1px solid #ccc;padding:8px;font-size:13px}.tc-table th,.tc-table td{font-size:12px;vertical-align:top;word-break:break-word}</style></head><body>");
        sb.append("<h1>附件二：验收标准与测试用例清单（草案）</h1>");
        sb.append("<p><strong>项目名称：</strong>").append(esc(p.getName())).append("</p>");
        int od = 5;
        Object days = ctx != null ? ctx.get("acceptanceObjectionDays") : null;
        if (days instanceof Number) {
            od = Math.max(0, ((Number) days).intValue());
        } else if (days != null) {
            try {
                od = Math.max(0, Integer.parseInt(days.toString()));
            } catch (Exception ignored) {
                od = 5;
            }
        }
        sb.append("<h2>一、验收与异议期</h2>");
        sb.append("<ol>");
        sb.append("<li>乙方完成开发、联调及内部测试后，可向甲方提交验收申请，并交付本附件第三条所列交付物。</li>");
        sb.append("<li>甲方应在收到验收申请之日起 <strong>").append(od).append("</strong> 个工作日内完成验收；逾期未提出书面异议的，视为该阶段验收通过（双方另有书面约定的除外）。</li>");
        sb.append("<li>甲方提出的异议应具体、可复现；乙方应在合理期限内予以修复或说明。重复验收次数及范围以主合同约定为准。</li>");
        sb.append("</ol>");
        sb.append("<h2>二、功能验收测试用例清单</h2>");
        Object acn = ctx != null ? ctx.get("acceptanceCriteriaNote") : null;
        Object atc = ctx != null ? ctx.get("acceptanceTestCases") : null;
        if (atc instanceof List<?> tcl && !tcl.isEmpty()) {
            sb.append("<p>下列用例与<strong>附件一《功能清单》</strong>中模块及功能点对应，作为交付验收的操作依据；「测试结果」在验收执行中填写，以双方确认的通过/驳回为准。</p>");
            if (acn != null && !acn.toString().isBlank()) {
                sb.append("<p><strong>补充说明（与用例一并作为验收参考）：</strong>").append(esc(acn.toString().trim())).append("</p>");
            }
            sb.append("<table class=\"tc-table\"><thead><tr>");
            sb.append("<th>序号</th><th>用例名称</th><th>归属模块</th><th>优先级</th><th>前置条件</th>");
            sb.append("<th>测试角色（系统角色）</th><th>测试步骤</th><th>预期结果</th><th>测试结果</th><th>备注</th>");
            sb.append("</tr></thead><tbody>");
            int ti = 1;
            for (Object o : tcl) {
                if (o instanceof Map<?, ?> raw) {
                    Map<String, Object> m = (Map<String, Object>) raw;
                    sb.append("<tr><td>").append(ti++).append("</td><td>").append(esc(Objects.toString(m.get("caseName"), "")))
                            .append("</td><td>").append(esc(Objects.toString(m.get("module"), "")))
                            .append("</td><td>").append(esc(Objects.toString(m.get("priority"), "")))
                            .append("</td><td>").append(esc(Objects.toString(m.get("preconditions"), "")))
                            .append("</td><td>").append(esc(Objects.toString(m.get("testRole"), "")))
                            .append("</td><td>").append(esc(Objects.toString(m.get("steps"), "")).replace("\n", "<br/>"))
                            .append("</td><td>").append(esc(Objects.toString(m.get("expectedResult"), "")))
                            .append("</td><td>").append(esc(Objects.toString(m.get("testResult"), "待测试")))
                            .append("</td><td>").append(esc(Objects.toString(m.get("remark"), "")))
                            .append("</td></tr>");
                }
            }
            sb.append("</tbody></table>");
        } else {
            if (acn != null && !acn.toString().isBlank()) {
                sb.append("<p>").append(esc(acn.toString().trim())).append("</p>");
            } else {
                sb.append("<p>双方确认：以主合同及<strong>附件一《功能清单》</strong>中所列功能及双方确认的用例/界面为准；功能完整、主要流程可用、无明显缺陷即视为达到交付验收标准。</p>");
            }
            sb.append("<p style=\"color:#666\">提示：可在报价项目「合同补充信息」中维护结构化测试用例清单，或使用「AI 生成测试清单」基于当前功能清单生成，核对后再导出本附件。</p>");
        }
        sb.append("<h2>三、交付物核对</h2>");
        sb.append("<p>乙方应按下列清单交付（以项目「合同补充信息」中勾选为准）：</p>");
        sb.append("<table><thead><tr><th>序号</th><th>交付物</th></tr></thead><tbody>");
        Object del = ctx != null ? ctx.get("deliverables") : null;
        int di = 1;
        if (del instanceof List<?> dlist && !dlist.isEmpty()) {
            for (Object k : dlist) {
                String key = k != null ? k.toString() : "";
                String label = DELIVERABLE_LABELS.getOrDefault(key, key);
                sb.append("<tr><td>").append(di++).append("</td><td>").append(esc(label)).append("</td></tr>");
            }
        }
        if (di == 1) {
            sb.append("<tr><td colspan=\"2\">（未勾选约定交付物，请在报价项目中补充后重新导出）</td></tr>");
        }
        sb.append("</tbody></table>");
        sb.append("<h2>四、里程碑与阶段验收（参考）</h2>");
        sb.append("<p>下列计划与<strong>附件三《里程碑计划》</strong>一致，供阶段验收与付款节点对照使用。</p>");
        sb.append("<table><thead><tr><th>序号</th><th>阶段名称</th><th>参考时间（生效后约 X 日）</th><th>备注</th></tr></thead><tbody>");
        Object ms = ctx != null ? ctx.get("milestones") : null;
        int mi = 1;
        if (ms instanceof List<?> mlist) {
            for (Object o : mlist) {
                if (o instanceof Map) {
                    Map<String, Object> m = (Map<String, Object>) o;
                    sb.append("<tr><td>").append(mi++).append("</td><td>").append(esc(Objects.toString(m.get("name"), "")))
                            .append("</td><td>").append(esc(Objects.toString(m.get("offsetDays"), "")))
                            .append("</td><td>").append(esc(Objects.toString(m.get("note"), "")))
                            .append("</td></tr>");
                }
            }
        }
        if (mi == 1) {
            sb.append("<tr><td colspan=\"4\">（未填写里程碑，可仅按终验执行）</td></tr>");
        }
        sb.append("</tbody></table>");
        sb.append("<p style=\"margin-top:24px;color:#666;font-size:12px\">说明：本附件由系统根据报价项目「合同补充信息」按预设规则自动生成，仅供签约前讨论使用，最终以双方签章版本为准。</p>");
        sb.append("</body></html>");
        return sb.toString();
    }

    /**
     * 解决方案向导第一步：仅拆分交付物骨架（键、名称、建议技术栈、范围摘要），不含功能点。
     */
    public Map<String, Object> parseDeliverablesOutlineWithAi(QuoteAiDeliverablesOutlineRequest req) {
        if (req == null || req.getRequirementText() == null || req.getRequirementText().isBlank()) {
            throw new IllegalArgumentException("需求描述不能为空");
        }
        String text = req.getRequirementText().trim();
        if (text.length() > 100000) {
            throw new IllegalArgumentException("需求描述过长，请控制在 100,000 字以内");
        }
        // 交付物骨架解析：文本过长时截取前10000字（骨架解析不需要全文细节）
        if (text.length() > 10000) {
            text = text.substring(0, 10000) + "\n\n…（需求文档过长，仅截取前 10000 字用于交付物骨架分析）";
        }
        AiAnalysisConfig cfg = aiConfigService.getConfig();
        if (cfg.getApiKey() == null || cfg.getApiKey().isBlank()) {
            throw new IllegalStateException("请先在管理后台「AI 配置」中填写 DeepSeek API Key");
        }
        String model = cfg.getModel() != null && !cfg.getModel().isBlank() ? cfg.getModel() : "deepseek-chat";

        StringBuilder ctx = new StringBuilder();
        ctx.append("【项目上下文】\n");
        ctx.append("项目类型：").append(labelProjectType(req.getProjectType()))
                .append(" (code: ").append(nvl(req.getProjectType(), "")).append(")\n");
        ctx.append("默认技术栈（未指定的交付物可参考）：").append(labelTechStack(req.getTechStack()))
                .append(" (code: ").append(nvl(req.getTechStack(), "vue_node")).append(")\n");
        if (req.getPrdSummary() != null && !req.getPrdSummary().isBlank()) {
            String prd = req.getPrdSummary().trim();
            if (prd.length() > 4000) {
                prd = prd.substring(0, 4000) + "…";
            }
            ctx.append("PRD/摘要（节选）：\n").append(prd).append("\n");
        }

        String system = """
你是资深软件外包需求分析师。用户要做「解决方案级」统一报价，请先只做交付物拆分骨架，不要输出功能点或模块明细。
必须仅输出一个 JSON 对象，不要 Markdown 代码块，不要其它解释文字。结构严格为：
{"deliverables":[{"deliverableKey":"web","deliverableLabel":"Web 管理端","techStack":"vue_node","scopeSummary":"本交付物负责…"}]}
规则：
- deliverables：至少 2 个、通常不超过 6 个；每个代表可独立验收的子系统（如 Web 管理端、用户 App、后端 API）。
- deliverableKey：小写英文短码，唯一（web、app、api、mini_program、admin 等）。
- deliverableLabel：中文展示名。
- techStack：必须是以下 code 之一：vue_node、react_java、miniprogram、flutter、other；按该交付物主要实现技术选择；不确定时用项目默认或 other。
- scopeSummary：一两句话概括该交付物职责边界，勿写功能清单。
""";

        String userMsg = ctx + "\n【客户/需求原文】\n" + text;
        List<DeepSeekClient.ChatMessage> messages = List.of(
                new DeepSeekClient.ChatMessage("system", system),
                new DeepSeekClient.ChatMessage("user", userMsg)
        );
        DeepSeekClient.ChatResult result = deepSeekClient.chatWithUsage(cfg.getApiKey(), model, messages, true, 4096);
        String content = result != null ? result.getContent() : null;
        if (content == null || content.isBlank()) {
            throw new IllegalStateException("AI 未返回有效内容，请稍后重试或缩短描述");
        }
        JsonNode root = parseAiJsonOrThrow(content, "parseDeliverablesOutline");
        List<Map<String, Object>> out = new ArrayList<>();
        if (!root.has("deliverables") || !root.get("deliverables").isArray()) {
            throw new IllegalStateException("AI 返回缺少 deliverables 数组");
        }
        Set<String> seen = new HashSet<>();
        for (JsonNode del : root.get("deliverables")) {
            String dKey = del.has("deliverableKey") ? del.get("deliverableKey").asText("").trim().toLowerCase() : "";
            if (dKey.isEmpty() || seen.contains(dKey)) {
                continue;
            }
            seen.add(dKey);
            String dLabel = del.has("deliverableLabel") ? del.get("deliverableLabel").asText("").trim() : "";
            String ts = del.has("techStack") ? del.get("techStack").asText("").trim() : "";
            if (ts.isEmpty()) {
                ts = nvl(req.getTechStack(), "vue_node");
            }
            String sum = del.has("scopeSummary") ? del.get("scopeSummary").asText("").trim() : "";
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("deliverableKey", dKey);
            row.put("deliverableLabel", dLabel.isEmpty() ? dKey : dLabel);
            row.put("techStack", normalizeTechStackCode(ts));
            row.put("scopeSummary", sum);
            out.add(row);
        }
        if (out.size() < 2) {
            throw new IllegalStateException("交付物拆分过少，请补充更具体的多端/多子系统需求描述");
        }
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("deliverables", out);
        if (result != null) {
            Map<String, Object> usage = new LinkedHashMap<>();
            usage.put("inputTokens", result.getInputTokens());
            usage.put("outputTokens", result.getOutputTokens());
            usage.put("model", result.getModel() != null ? result.getModel() : model);
            resp.put("usage", usage);
        }
        return resp;
    }

    private static String normalizeTechStackCode(String ts) {
        if (ts == null) return "other";
        String t = ts.trim().toLowerCase();
        return switch (t) {
            case "vue_node", "react_java", "miniprogram", "flutter", "other" -> t;
            default -> "other";
        };
    }

    /**
     * 自然语言 → DeepSeek JSON → 与前端/保存接口一致的 modules 结构。
     * 支持超长文本：超过 10000 字时自动切片为多条消息喂给 AI，AI 看到全部内容后返回一份统一结果。
     */
    public Map<String, Object> parseQuoteModulesWithAi(QuoteAiModulesParseRequest req) {
        if (req == null || req.getRequirementText() == null || req.getRequirementText().isBlank()) {
            throw new IllegalArgumentException("需求描述不能为空");
        }
        String text = req.getRequirementText().trim();
        if (text.length() > 100000) {
            throw new IllegalArgumentException("需求描述过长，请控制在 100,000 字以内");
        }

        AiAnalysisConfig cfg = aiConfigService.getConfig();
        if (cfg.getApiKey() == null || cfg.getApiKey().isBlank()) {
            throw new IllegalStateException("请先在管理后台「AI 配置」中填写 DeepSeek API Key");
        }
        String model = cfg.getModel() != null && !cfg.getModel().isBlank() ? cfg.getModel() : "deepseek-chat";

        // 构建项目上下文
        String ctx = buildProjectContext(req);

        // 构建 system prompt
        String system = buildModuleSystemPrompt(req);

        // 构建 user messages：超长文本切片为多条消息
        List<DeepSeekClient.ChatMessage> messages = new ArrayList<>();
        messages.add(new DeepSeekClient.ChatMessage("system", system));

        if (text.length() <= 10000) {
            // 短文本：单条消息
            messages.add(new DeepSeekClient.ChatMessage("user", ctx + "\n【客户/需求原文】\n" + text));
        } else {
            // 长文本：切片为多条 user 消息，AI 一次看到全部内容
            List<String> chunks = splitTextByParagraph(text, 10000);
            // 第一条消息带项目上下文 + 第一片
            messages.add(new DeepSeekClient.ChatMessage("user",
                    ctx + "\n【客户/需求原文（第 1/" + chunks.size() + " 部分）】\n" + chunks.get(0)));
            // 后续每片作为独立 user 消息
            for (int i = 1; i < chunks.size(); i++) {
                messages.add(new DeepSeekClient.ChatMessage("user",
                        "【客户/需求原文（第 " + (i + 1) + "/" + chunks.size() + " 部分）】\n" + chunks.get(i)));
            }
            // 最后一条消息要求 AI 综合所有部分输出结果
            messages.add(new DeepSeekClient.ChatMessage("user",
                    "以上是完整的客户需求文档（共 " + chunks.size() + " 部分）。请综合所有内容，输出一份完整的模块拆解 JSON。"));
        }

        DeepSeekClient.ChatResult result = deepSeekClient.chatWithUsage(cfg.getApiKey(), model, messages, true, 8192);
        String content = result != null ? result.getContent() : null;
        if (content == null || content.isBlank()) {
            throw new IllegalStateException("AI 未返回有效内容，请稍后重试或缩短描述");
        }

        return parseModulesResult(content, req, result, model);
    }

    /**
     * 构建项目上下文信息（项目类型、技术栈等维度）
     */
    private String buildProjectContext(QuoteAiModulesParseRequest req) {
        StringBuilder ctx = new StringBuilder();
        ctx.append("【项目上下文】下列为当前报价表单已选维度（辅助拆解模块；客户原文未提及处勿臆造具体业务功能）\n");
        ctx.append("项目类型：").append(labelProjectType(req.getProjectType()))
                .append(" (code: ").append(nvl(req.getProjectType(), "")).append(")\n");
        ctx.append("技术栈：").append(labelTechStack(req.getTechStack()))
                .append(" (code: ").append(nvl(req.getTechStack(), "")).append(")\n");
        ctx.append("设计：").append(labelDesign(req.getDesignType()))
                .append(" (code: ").append(nvl(req.getDesignType(), "")).append(")\n");
        ctx.append("数据迁移/对接：").append(labelDataMigration(req.getDataMigration()))
                .append(" (code: ").append(nvl(req.getDataMigration(), "")).append(")\n");
        ctx.append("并发量级：").append(labelConcurrency(req.getConcurrency()))
                .append(" (code: ").append(nvl(req.getConcurrency(), "")).append(")\n");
        ctx.append("安全：").append(labelSecurity(req.getSecurityLevel()))
                .append(" (code: ").append(nvl(req.getSecurityLevel(), "")).append(")\n");
        ctx.append("部署：").append(labelDeploy(req.getDeployType()))
                .append(" (code: ").append(nvl(req.getDeployType(), "")).append(")\n");
        if (req.getPrdSummary() != null && !req.getPrdSummary().isBlank()) {
            String prd = req.getPrdSummary().trim();
            if (prd.length() > 4000) {
                prd = prd.substring(0, 4000) + "…";
            }
            ctx.append("PRD/摘要（节选）：\n").append(prd).append("\n");
        }

        boolean multiDel = Boolean.TRUE.equals(req.getMultiDeliverableMode());
        List<QuoteDeliverableHintDto> hints = req.getDeliverableHints();
        boolean hintMode = multiDel && hints != null && !hints.isEmpty();
        if (hintMode) {
            ctx.append("\n【用户已确认的交付物分组（必须严格遵守，不得新增或改名 deliverableKey）】\n");
            for (QuoteDeliverableHintDto h : hints) {
                if (h == null || h.getDeliverableKey() == null || h.getDeliverableKey().isBlank()) {
                    continue;
                }
                ctx.append("- deliverableKey=").append(h.getDeliverableKey().trim().toLowerCase());
                if (h.getDeliverableLabel() != null && !h.getDeliverableLabel().isBlank()) {
                    ctx.append("，展示名=").append(h.getDeliverableLabel().trim());
                }
                if (h.getTechStack() != null && !h.getTechStack().isBlank()) {
                    ctx.append("，建议计价技术栈 code=").append(h.getTechStack().trim());
                }
                ctx.append("\n");
            }
        }
        return ctx.toString();
    }

    /**
     * 构建 system prompt（根据模式选择）
     */
    private String buildModuleSystemPrompt(QuoteAiModulesParseRequest req) {
        boolean multiDel = Boolean.TRUE.equals(req.getMultiDeliverableMode());
        List<QuoteDeliverableHintDto> hints = req.getDeliverableHints();
        boolean hintMode = multiDel && hints != null && !hints.isEmpty();

        if (hintMode) {
            return """
你是资深软件外包需求分析师。用户已固定交付物分组与技术栈倾向，请仅在各分组下拆解「模块 → 功能点」。
必须仅输出一个 JSON 对象，不要 Markdown 代码块，不要其它解释文字。结构严格为：
{"deliverables":[{"deliverableKey":"web","deliverableLabel":"Web 管理端","modules":[{"name":"模块中文名","items":[{"name":"功能点中文名","complexity":"standard","quantity":1}]}]}]}
规则：
- deliverables 数组必须与用户给出的分组一一对应：每个 deliverableKey 必须与用户列表中的键完全一致（小写），不得增删交付物。
- deliverableLabel 与用户展示名一致（若用户未给则用键）。
- 每个交付物下至少 1 个模块；modules 按业务域划分。
- items：complexity 只能是 simple、standard、medium、complex、extreme；quantity 为正整数。
""";
        } else if (multiDel) {
            return """
你是资深软件外包需求分析师。用户要做「一整套系统」的统一报价（可能含 Web、App、后端等多子系统）。
必须仅输出一个 JSON 对象，不要 Markdown 代码块，不要其它解释文字。结构严格为：
{"deliverables":[{"deliverableKey":"web","deliverableLabel":"Web 管理端","modules":[{"name":"模块中文名","items":[{"name":"功能点中文名","complexity":"standard","quantity":1}]}]}]}
规则：
- deliverables：至少 1 个交付物；deliverableKey 用小写英文短码（web、app、api、mini_program 等）；deliverableLabel 为中文展示名。
- 每个交付物下 modules：按业务域划分，至少 1 个模块。
- items：可交付功能粒度；complexity 只能是：simple、standard、medium、complex、extreme；quantity 为正整数。
""";
        } else {
            return """
你是资深软件外包需求分析师。根据用户自然语言需求，拆解为「功能模块 → 功能点」清单，用于人天报价。
必须仅输出一个 JSON 对象，不要 Markdown 代码块，不要其它解释文字。结构严格为：
{"modules":[{"name":"模块中文名","items":[{"name":"功能点中文名","complexity":"standard","quantity":1}]}]}
规则：
- modules：按业务域划分（如用户与权限、订单与支付），至少 1 个模块；名称简洁。
- items：可交付功能粒度，每条一行；大功能可拆成多条。
- complexity 只能是英文枚举：simple、standard、medium、complex、extreme（对应 简单/标准/中等/复杂/极复杂 的工作量档位）。
- quantity：正整数，默认 1。
""";
        }
    }

    /**
     * 解析 AI 返回的 JSON 为统一的 modules 结构
     */
    private Map<String, Object> parseModulesResult(String content, QuoteAiModulesParseRequest req,
                                                    DeepSeekClient.ChatResult result, String model) {
        boolean multiDel = Boolean.TRUE.equals(req.getMultiDeliverableMode());
        List<QuoteDeliverableHintDto> hints = req.getDeliverableHints();
        boolean hintMode = multiDel && hints != null && !hints.isEmpty();

        JsonNode root = parseAiJsonOrThrow(content, "parseQuoteModules");
        List<Map<String, Object>> outModules = new ArrayList<>();
        int mi = 0;

        if (multiDel && root.has("deliverables") && root.get("deliverables").isArray()) {
            for (JsonNode del : root.get("deliverables")) {
                String dKey = del.has("deliverableKey") ? del.get("deliverableKey").asText("").trim().toLowerCase() : "";
                if (dKey.isEmpty()) {
                    dKey = "part_" + mi;
                }
                String dLabel = del.has("deliverableLabel") ? del.get("deliverableLabel").asText("").trim() : "";
                JsonNode modulesNode = del.get("modules");
                if (modulesNode == null || !modulesNode.isArray()) {
                    continue;
                }
                for (JsonNode mod : modulesNode) {
                    String modName = mod.has("name") ? mod.get("name").asText("").trim() : "";
                    if (modName.isEmpty()) {
                        continue;
                    }
                    JsonNode itemsNode = mod.get("items");
                    List<Map<String, Object>> items = parseQuoteModuleItemsJson(itemsNode);
                    if (items.isEmpty()) {
                        continue;
                    }
                    Map<String, Object> mm = new LinkedHashMap<>();
                    mm.put("name", modName);
                    mm.put("sortOrder", mi++);
                    mm.put("deliverableKey", dKey);
                    mm.put("deliverableLabel", dLabel.isEmpty() ? null : dLabel);
                    if (hintMode && hints != null) {
                        for (QuoteDeliverableHintDto h : hints) {
                            if (h == null || h.getDeliverableKey() == null) continue;
                            if (dKey.equalsIgnoreCase(h.getDeliverableKey().trim())) {
                                if (h.getTechStack() != null && !h.getTechStack().isBlank()) {
                                    mm.put("techStack", h.getTechStack().trim());
                                }
                                break;
                            }
                        }
                    }
                    mm.put("items", items);
                    outModules.add(mm);
                }
            }
        } else {
            JsonNode modulesNode = root.get("modules");
            if (modulesNode == null || !modulesNode.isArray()) {
                throw new IllegalStateException("AI 返回缺少 modules 数组");
            }
            for (JsonNode mod : modulesNode) {
                String modName = mod.has("name") ? mod.get("name").asText("").trim() : "";
                if (modName.isEmpty()) {
                    continue;
                }
                JsonNode itemsNode = mod.get("items");
                List<Map<String, Object>> items = parseQuoteModuleItemsJson(itemsNode);
                if (items.isEmpty()) {
                    continue;
                }
                Map<String, Object> mm = new LinkedHashMap<>();
                mm.put("name", modName);
                mm.put("sortOrder", mi++);
                mm.put("deliverableKey", "default");
                mm.put("deliverableLabel", null);
                mm.put("items", items);
                outModules.add(mm);
            }
        }

        if (outModules.isEmpty() && multiDel && root.has("modules") && root.get("modules").isArray()) {
            JsonNode modulesNode = root.get("modules");
            for (JsonNode mod : modulesNode) {
                String modName = mod.has("name") ? mod.get("name").asText("").trim() : "";
                if (modName.isEmpty()) {
                    continue;
                }
                JsonNode itemsNode = mod.get("items");
                List<Map<String, Object>> items = parseQuoteModuleItemsJson(itemsNode);
                if (items.isEmpty()) {
                    continue;
                }
                Map<String, Object> mm = new LinkedHashMap<>();
                mm.put("name", modName);
                mm.put("sortOrder", mi++);
                mm.put("deliverableKey", "default");
                mm.put("deliverableLabel", null);
                mm.put("items", items);
                outModules.add(mm);
            }
        }

        if (outModules.isEmpty()) {
            throw new IllegalStateException("未解析出有效功能模块，请补充更具体的需求描述");
        }

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("modules", outModules);
        if (result != null) {
            Map<String, Object> usage = new LinkedHashMap<>();
            usage.put("inputTokens", result.getInputTokens());
            usage.put("outputTokens", result.getOutputTokens());
            usage.put("model", result.getModel() != null ? result.getModel() : model);
            out.put("usage", usage);
        }
        return out;
    }

    private static long toLong(Object v) {
        if (v == null) return 0;
        if (v instanceof Number) return ((Number) v).longValue();
        try { return Long.parseLong(v.toString()); } catch (Exception e) { return 0; }
    }

    /**
     * 按段落边界切分文本，每片不超过 maxChunkSize 字。
     */
    private List<String> splitTextByParagraph(String text, int maxChunkSize) {
        List<String> chunks = new ArrayList<>();
        String[] paragraphs = text.split("\\n\\n+");
        StringBuilder current = new StringBuilder();
        for (String para : paragraphs) {
            if (para.isBlank()) continue;
            if (current.length() > 0 && current.length() + 2 + para.length() > maxChunkSize) {
                chunks.add(current.toString());
                current = new StringBuilder();
            }
            if (para.length() > maxChunkSize) {
                if (current.length() > 0) {
                    chunks.add(current.toString());
                    current = new StringBuilder();
                }
                String[] lines = para.split("\\n");
                for (String line : lines) {
                    if (current.length() > 0 && current.length() + 1 + line.length() > maxChunkSize) {
                        chunks.add(current.toString());
                        current = new StringBuilder();
                    }
                    if (current.length() > 0) current.append('\n');
                    current.append(line);
                }
            } else {
                if (current.length() > 0) current.append("\n\n");
                current.append(para);
            }
        }
        if (current.length() > 0) {
            chunks.add(current.toString());
        }
        return chunks;
    }

    /**
     * 快原型从报价导入：由后端判别传入 AI 的纯文本内容，两种形态互斥。
     * <ul>
     *   <li>存在 PRD 或 AI 录入原文任一非空：输出<strong>只基于该叙述推导</strong>的《页面设计文档》导入稿（含信息架构/页面蓝图/布局与字段声明写法要求），
     *       但<strong>不附带</strong>任何功能清单、附件/附录表格或“按模块对应”的提示词。</li>
     *   <li>二者皆空：<strong>仅输出</strong>报价「功能清单」Markdown 表格作为唯一需求替代。</li>
     * </ul>
     */
    public Map<String, Object> buildPrototypeRequirementFromQuote(long quoteProjectId) {
        Map<String, Object> detail = getProjectDetail(quoteProjectId);
        if (detail == null) {
            throw new IllegalArgumentException("报价项目不存在");
        }
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> modules = (List<Map<String, Object>>) detail.get("modules");
        if (modules == null) {
            modules = Collections.emptyList();
        }

        String projectName = Objects.toString(detail.get("name"), "").trim();
        String displayName = !projectName.isEmpty() ? projectName : ("报价项目 #" + quoteProjectId);
        String prdSummary = Objects.toString(detail.get("prdSummary"), "").trim();
        String aiRequirementText = Objects.toString(detail.get("aiRequirementText"), "").trim();

        String prdBlock = prdSummary.isEmpty() ? "" : (prdSummary.length() > 6000 ? prdSummary.substring(0, 6000) + "…" : prdSummary);
        String aiBlock = aiRequirementText.isEmpty() ? "" : (aiRequirementText.length() > 12000 ? aiRequirementText.substring(0, 12000) + "…" : aiRequirementText);
        final boolean hasUserNarrative = !prdBlock.isEmpty() || !aiBlock.isEmpty();

        StringBuilder doc = new StringBuilder();
        if (hasUserNarrative) {
            appendNarrativeOnlyPageDesignDoc(doc, displayName, quoteProjectId, prdBlock, aiBlock);
        } else {
            if (modules.isEmpty()) {
                throw new IllegalArgumentException("无用户原始需求时需至少有一条报价功能清单，请先填写功能模块与功能点");
            }
            appendFeatureListBasedPageDesignDoc(doc, displayName, quoteProjectId, modules);
        }

        String requirementText = doc.toString().trim();
        if (requirementText.isEmpty()) {
            throw new IllegalStateException("导入结果为空，请重试");
        }

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("quoteProjectId", quoteProjectId);
        out.put("requirementText", requirementText);
        out.put("sourceModuleCount", modules.size());
        return out;
    }

    /**
     * 有用户原始叙述时：生成“页面设计文档导入稿”（仅由原文推导），用于提升快原型生成稳定性。
     *
     * 注意：此处<strong>不允许</strong>附带功能清单/附录/附件式表格；也<strong>不允许</strong>出现“按模块编写/与模块对应”等指令。
     */
    private static void appendNarrativeOnlyPageDesignDoc(StringBuilder doc,
                                                         String displayName,
                                                         long quoteProjectId,
                                                         String prdBlock,
                                                         String aiBlock) {
        doc.append("# 《").append(displayName).append("》页面设计文档（导入稿）\n\n");
        doc.append("> **生成规则（强约束）**：\n");
        doc.append("> - 本文档的页面划分、导航层级、状态池命名、字段与按钮文案，必须**只依据下方「用户原始需求」**推导；\n");
        doc.append("> - **禁止**引入任何“附件/附录/功能清单/模块对应关系”来组织页面；\n");
        doc.append("> - 输出时要**增强**：页面划分、组件内字段声明、页面内组件划分与空间布局（分区比例/顺序）的表述。\n\n");

        doc.append("## 1. 文档元信息\n");
        doc.append("- **项目名称**：").append(displayName).append("\n");
        doc.append("- **关联报价项目**：#").append(quoteProjectId).append("\n");
        doc.append("- **项目类型**：管理后台\n\n");

        doc.append("## 2. 用户原始需求（唯一依据）\n\n");
        if (prdBlock != null && !prdBlock.isBlank()) {
            doc.append("### 2.1 PRD / 需求摘要\n");
            doc.append(prdBlock).append("\n\n");
        }
        if (aiBlock != null && !aiBlock.isBlank()) {
            doc.append("### 2.2 AI 智能录入时的用户原文\n");
            doc.append(aiBlock).append("\n\n");
        }

        doc.append("## 3. 产品目标与范围（由原文归纳）\n");
        doc.append("用 2～4 句概括：系统解决什么问题、主要服务对象、成功使用时用户能完成什么。表述克制，不引入原文未提及的品牌/政策。\n\n");

        doc.append("## 4. 信息架构与导航（由原文推导）\n");
        doc.append("- **主导航形态**：建议「侧边分组 + 工作台落地页」，并在工作台内用 Tabs 表达状态池（若原文存在“待…”栏目）。\n");
        doc.append("- **核心用户路径**：列出 1～3 条最高频路径（如：创建订单 → 对接分配 → 设计/建模/评审 → 报价/待生产 → 已完成）。\n");
        doc.append("- **页面职责**：列表页做检索/筛选/批量；详情/抽屉做单实体编辑；避免单屏堆满无关模块。\n\n");

        doc.append("## 5. 核心页面详细设计（必须写到可直接落地原型）\n");
        doc.append("请对每个核心页面按以下模板输出（页面数量与命名**由原文自然推导**，不要机械分页）：\n");
        doc.append("- **页面名称**与在导航中的位置\n");
        doc.append("- **空间布局**：顶栏/筛选区/主内容/侧栏或抽屉/底部操作区；写清上下/左右分区与大致比例（如 6:4、上列表下详情）。\n");
        doc.append("- **组件划分**：表格/表单/卡片/步骤条/Tabs/Panel 等，写清摆放顺序。\n");
        doc.append("- **字段声明**：逐字段列出中文标签、控件类型（文本/多行/下拉/数字/日期/上传）、必填/只读/条件显示。\n");
        doc.append("- **状态与交互**：Tab 切换（setTab）、抽屉/面板展开（togglePanel）、行内操作、危险操作（取消/驳回）的样式区分。\n\n");

        doc.append("### 5.1 示例（仅示范格式与详略，禁止照抄为真实需求）\n\n");
        doc.append("""
                **页面：售前客服 — 新建订单**
                - **导航位置**：侧栏「订单」→「新建」。
                - **空间布局**：页头（标题+说明）→ 主区左右 **6:4**（左表单/右侧卡片）→ 底部固定操作栏。
                - **组件划分**：左侧表单分组「订单信息」「客户需求」；右侧卡片「财务与材质」。
                - **字段声明**：
                  - 订单编号（只读 文本）
                  - 订单来源（下拉；当选“达人推荐”时显示“达人昵称”文本）
                  - 下单时间（日期时间）
                  - 定金金额（数字）
                  - 基础需求（多行）
                  - 客户联系方式（必填 文本）
                - **操作**：主按钮【提交创建】；次要【保存草稿】（如原文未提及可不写）。

                **页面：售中客服 — 订单工作台（多状态池）**
                - **导航位置**：侧栏「工作台」。
                - **空间布局**：顶部统计卡片区（约 15～20% 高度）+ 下方「Tabs + 表格」；点击行内【去对接】打开右侧抽屉（约 30～40% 屏宽）。
                - **组件划分**：统计卡片组；状态 Tabs；订单表格；抽屉内分组表单（对接字段+分配人员）。
                - **字段声明**：表格列（订单编号/状态/更新时间/操作）；抽屉字段（字印/材质/手寸或链长/选择设计师等）。
                - **交互**：Tabs 用 setTab；抽屉用 togglePanel；危险操作【已取消】红色文字按钮并锁定后续操作。
                """);
        doc.append("\n\n");

        doc.append("## 6. 视觉与交互规范（必须遵守）\n");
        doc.append("- 风格：简洁现代中后台；主操作明确；中文业务术语与原文一致。\n");
        doc.append("- 约束：仅使用原型引擎支持的交互（setTab/togglePanel）；禁止图片/外链/iframe。\n");
        doc.append("- 文案：按钮与状态名尽量使用原文用语（如“去对接”“待…栏目”“已取消”等）。\n");
    }

    /**
     * 无用户原文时：基于功能清单生成同结构的页面设计文档导入稿。
     * “用户原始需求”小节由功能清单转述而来，明确说明是唯一依据。
     */
    private static void appendFeatureListBasedPageDesignDoc(StringBuilder doc,
                                                            String displayName,
                                                            long quoteProjectId,
                                                            List<Map<String, Object>> modules) {
        doc.append("# 《").append(displayName).append("》页面设计文档（导入稿）\n\n");
        doc.append("> **生成规则（强约束）**：\n");
        doc.append("> - 本文档的页面划分、导航层级、状态池命名、字段与按钮文案，必须**只依据下方「功能清单整理出的需求」**推导；\n");
        doc.append("> - **禁止**引用其他“模块说明/测试用例/外部附件”来组织页面；\n");
        doc.append("> - 输出时要**增强**：页面划分、组件内字段声明、页面内组件划分与空间布局（分区比例/顺序）的表述。\n\n");

        doc.append("## 1. 文档元信息\n");
        doc.append("- **项目名称**：").append(displayName).append("\n");
        doc.append("- **关联报价项目**：#").append(quoteProjectId).append("\n");
        doc.append("- **项目类型**：管理后台\n\n");

        doc.append("## 2. 功能清单整理出的需求（唯一依据）\n\n");
        doc.append("下表为本项目在报价阶段已确认的功能清单（模块/功能点/复杂度/数量），视作本次页面设计的唯一需求来源。\n\n");
        appendQuoteFeatureListMarkdownTable(doc, modules, 200);

        doc.append("## 3. 产品目标与范围（由功能清单归纳）\n");
        doc.append("用 2～4 句概括：系统大致解决的问题、主要服务对象、关键能力范围（例如订单全生命周期、权限管理、统计分析等）。不要虚构表中不存在的业务域。\n\n");

        doc.append("## 4. 信息架构与导航（由功能清单推导）\n");
        doc.append("- **主导航形态**：从功能清单中抽取 3～6 个业务域（如订单管理、流程设计/建模、工艺与报价、用户与权限、统计分析）作为一级菜单；\n");
        doc.append("- **核心用户路径**：根据“创建/编辑/上传/导出/统计”等功能点，列出 1～3 条代表性路径（如：创建订单 → 分配设计/建模 → 工艺评审 → 报价/待生产 → 导出/统计）；\n");
        doc.append("- **页面职责**：将“创建/编辑类”能力集中到表单/详情，“查看/导出/批量操作类”集中到列表/工作台，避免为每一条功能点单独做页面。\n\n");

        doc.append("## 5. 核心页面详细设计（必须写到可直接落地原型）\n");
        doc.append("请基于功能清单自行归纳页面，而非“每行一个页面”。对每个核心页面按以下模板输出：\n");
        doc.append("- **页面名称**与在导航中的位置\n");
        doc.append("- **空间布局**：顶栏/筛选区/主内容/侧栏或抽屉/底部操作区；写清上下/左右分区与大致比例（如 6:4、上列表下详情）。\n");
        doc.append("- **组件划分**：表格/表单/卡片/步骤条/Tabs/Panel 等，写清摆放顺序。\n");
        doc.append("- **字段声明**：根据表格中的功能点，列出每页涉及的字段标签、控件类型、必填/只读/条件显示等。\n");
        doc.append("- **状态与交互**：根据“待…/已完成/已取消”等功能语义设计 Tab 与按钮，并说明 setTab/togglePanel 的触发位置。\n\n");

        doc.append("## 6. 视觉与交互规范（必须遵守）\n");
        doc.append("- 风格：简洁现代中后台；主操作明确；中文业务术语尽量沿用功能点文案。\n");
        doc.append("- 约束：仅使用原型引擎支持的交互（setTab/togglePanel）；禁止图片/外链/iframe。\n");
        doc.append("- 文案：按钮与状态名应能在功能清单语义中找到依据，不新增无关业务对象。\n");
    }

    /** 报价功能清单 Markdown 表（仅用于无用户原文时的导入正文）。 */
    private static void appendQuoteFeatureListMarkdownTable(StringBuilder doc, List<Map<String, Object>> modules, int maxRows) {
        doc.append("| 模块 | 功能点 | 复杂度 | 数量 |\n");
        doc.append("| --- | --- | --- | --- |\n");
        int rowCount = 0;
        outer:
        for (Map<String, Object> mod : modules) {
            if (mod == null) continue;
            String modName = escMdTableCell(Objects.toString(mod.get("name"), ""));
            if (modName.isEmpty()) continue;
            Object itemsObj = mod.get("items");
            if (!(itemsObj instanceof List<?> items) || items.isEmpty()) {
                if (rowCount >= maxRows) break;
                doc.append("| ").append(modName).append(" | （无子功能点） |  |  |\n");
                rowCount++;
                continue;
            }
            for (Object io : items) {
                if (rowCount >= maxRows) {
                    doc.append("| … | （其余行已省略） |  |  |\n");
                    break outer;
                }
                if (!(io instanceof Map<?, ?> rm)) continue;
                String itemName = escMdTableCell(Objects.toString(rm.get("name"), ""));
                if (itemName.isEmpty()) continue;
                String cx = escMdTableCell(Objects.toString(rm.get("complexity"), "standard"));
                String qty = escMdTableCell(Objects.toString(rm.get("quantity"), "1"));
                doc.append("| ").append(modName).append(" | ").append(itemName).append(" | ").append(cx).append(" | ").append(qty).append(" |\n");
                rowCount++;
            }
        }
        doc.append('\n');
    }

    /** Markdown 表格单元格内避免破坏表格结构 */
    private static String escMdTableCell(String s) {
        if (s == null) return "";
        return s.replace('|', '｜').replace('\r', ' ').replace('\n', ' ').trim();
    }

    /**
     * 从模型回复中解析 JSON：去除 BOM、抽取任意位置的 Markdown 代码块、截取首个平衡 {...}。
     * 解决「前面有说明文字」「JSON 在中间的 ```json 块里」等仅 strip 首尾 fence 无法处理的情况。
     */
    private JsonNode parseAiJsonOrThrow(String rawContent, String logContext) {
        if (rawContent == null || rawContent.isBlank()) {
            throw new IllegalStateException("AI 未返回有效内容，请稍后重试");
        }
        String jsonStr = normalizeAiJsonContent(rawContent);
        if (jsonStr.isBlank()) {
            throw new IllegalStateException("AI 返回格式无法解析为 JSON，请重试");
        }
        try {
            return objectMapper.readTree(jsonStr);
        } catch (Exception e1) {
            try {
                return lenientJsonMapper.readTree(jsonStr);
            } catch (Exception e2) {
                log.warn("{}: JSON parse failed: {} | snippet: {}", logContext, e1.getMessage(), truncateForLog(jsonStr, 700));
                throw new IllegalStateException("AI 返回格式无法解析为 JSON，请重试。若多次失败，请减少功能清单条目或缩短补充说明后重试。");
            }
        }
    }

    private static String truncateForLog(String s, int max) {
        if (s == null) return "";
        String t = s.replace('\r', ' ').replace('\n', ' ');
        if (t.length() <= max) return t;
        return t.substring(0, max) + "…";
    }

    private static String normalizeAiJsonContent(String raw) {
        if (raw == null) {
            return "";
        }
        String t = raw.trim();
        if (t.startsWith("\uFEFF")) {
            t = t.substring(1).trim();
        }
        t = extractMarkdownFenceContent(t);
        t = t.trim();
        int start = t.indexOf('{');
        if (start < 0) {
            return t;
        }
        int end = findMatchingClosingBrace(t, start);
        if (end > start) {
            return t.substring(start, end + 1);
        }
        return t.substring(start);
    }

    /** 取首个 ``` … ``` 代码块内部（支持全文任意位置，不要求以 fence 开头）。 */
    private static String extractMarkdownFenceContent(String t) {
        int fence = t.indexOf("```");
        if (fence < 0) {
            return t;
        }
        int nl = t.indexOf('\n', fence);
        int contentStart = (nl >= 0) ? nl + 1 : fence + 3;
        int close = t.indexOf("```", contentStart);
        if (close > contentStart) {
            return t.substring(contentStart, close).trim();
        }
        return t.substring(contentStart).trim();
    }

    /**
     * 从 openIdx 处的 '{' 起，找到与之匹配的 '}'（忽略字符串内的括号）。
     */
    private static int findMatchingClosingBrace(String s, int openIdx) {
        if (openIdx < 0 || openIdx >= s.length() || s.charAt(openIdx) != '{') {
            return -1;
        }
        int depth = 0;
        boolean inStr = false;
        boolean escape = false;
        for (int i = openIdx; i < s.length(); i++) {
            char c = s.charAt(i);
            if (escape) {
                escape = false;
                continue;
            }
            if (inStr) {
                if (c == '\\') {
                    escape = true;
                } else if (c == '"') {
                    inStr = false;
                }
                continue;
            }
            if (c == '"') {
                inStr = true;
                continue;
            }
            if (c == '{') {
                depth++;
            } else if (c == '}') {
                depth--;
                if (depth == 0) {
                    return i;
                }
            }
        }
        return -1;
    }

    /** 与前端选项一致：simple / standard / medium / complex / extreme */
    private static String normalizeComplexityForQuote(String raw) {
        if (raw == null || raw.isBlank()) {
            return "standard";
        }
        String x = raw.trim();
        String lower = x.toLowerCase();
        return switch (lower) {
            case "simple", "s", "简单" -> "simple";
            case "standard", "norm", "标准" -> "standard";
            case "medium", "med", "中等", "中" -> "medium";
            case "complex", "hard", "复杂" -> "complex";
            case "extreme", "very_complex", "极复杂" -> "extreme";
            default -> {
                if (x.contains("极") || x.contains("extreme")) {
                    yield "extreme";
                }
                if (x.contains("复杂") && !x.contains("极")) {
                    yield "complex";
                }
                if (x.contains("中等") || (x.contains("中") && x.length() <= 3)) {
                    yield "medium";
                }
                if (x.contains("简单")) {
                    yield "simple";
                }
                if (x.contains("标准")) {
                    yield "standard";
                }
                yield "standard";
            }
        };
    }

    private static String labelProjectType(String t) {
        if (t == null) return "";
        return switch (t) {
            case "website" -> "企业官网";
            case "ecommerce_miniprogram" -> "电商小程序";
            case "admin" -> "管理后台";
            case "app" -> "APP";
            default -> t;
        };
    }

    private static String labelTechStack(String t) {
        if (t == null) return "";
        return switch (t) {
            case "vue_node" -> "Vue+Node";
            case "react_java" -> "React+Java";
            case "miniprogram" -> "小程序原生";
            case "flutter" -> "Flutter";
            default -> t;
        };
    }

    private static String labelDesign(String t) {
        if (t == null) return "";
        return switch (t) {
            case "has_design" -> "已有设计稿";
            case "need_design" -> "需 UI 设计";
            case "template" -> "套用模板";
            default -> t;
        };
    }

    private static String labelDataMigration(String t) {
        if (t == null) return "";
        return switch (t) {
            case "none" -> "无";
            case "migrate" -> "需从旧系统迁移";
            case "third_party" -> "需第三方接口对接";
            default -> t;
        };
    }

    private static String labelConcurrency(String t) {
        if (t == null) return "";
        return switch (t) {
            case "lt100" -> "<100";
            case "r100_1000" -> "100–1000";
            case "r1000_10000" -> "1000–10000";
            case "gt10000" -> "10000+";
            default -> t;
        };
    }

    private static String labelSecurity(String t) {
        if (t == null) return "";
        return switch (t) {
            case "normal" -> "普通";
            case "financial" -> "金融级";
            case "government" -> "政府级";
            default -> t;
        };
    }

    private static String labelDeploy(String t) {
        if (t == null) return "";
        return switch (t) {
            case "single" -> "单机";
            case "cloud" -> "云服务器";
            case "k8s" -> "容器化/K8s";
            default -> t;
        };
    }

    /**
     * 根据当前功能清单与项目上下文生成「验收测试用例/测试清单」JSON（不落库）。
     */
    public Map<String, Object> generateAcceptanceTestCasesWithAi(QuoteAiAcceptanceTestCasesRequest req) {
        if (req == null) {
            throw new IllegalArgumentException("请求体不能为空");
        }
        List<String> moduleOrder = new ArrayList<>();
        if (req.getModules() != null) {
            for (Map<String, Object> mod : req.getModules()) {
                if (mod == null) continue;
                Object mn = mod.get("name");
                if (mn == null || mn.toString().isBlank()) continue;
                String name = mn.toString().trim();
                moduleOrder.add(name);
            }
        }
        if (moduleOrder.isEmpty()) {
            throw new IllegalArgumentException("请先填写并保存至少一个功能模块后再生成测试清单");
        }

        AiAnalysisConfig cfg = aiConfigService.getConfig();
        if (cfg.getApiKey() == null || cfg.getApiKey().isBlank()) {
            throw new IllegalStateException("请先在管理后台「AI 配置」中填写 DeepSeek API Key");
        }
        String model = cfg.getModel() != null && !cfg.getModel().isBlank() ? cfg.getModel() : "deepseek-chat";

        StringBuilder userBlock = new StringBuilder();
        userBlock.append("【项目上下文】下列维度与报价表单一致；生成用例时不得与下文功能清单或 PRD 矛盾。\n");
        userBlock.append("项目类型：").append(labelProjectType(req.getProjectType()))
                .append(" (code: ").append(nvl(req.getProjectType(), "")).append(")\n");
        userBlock.append("技术栈：").append(labelTechStack(req.getTechStack()))
                .append(" (code: ").append(nvl(req.getTechStack(), "")).append(")\n");
        userBlock.append("设计：").append(labelDesign(req.getDesignType()))
                .append("；数据/对接：").append(labelDataMigration(req.getDataMigration())).append("\n");
        userBlock.append("并发：").append(labelConcurrency(req.getConcurrency()))
                .append("；安全：").append(labelSecurity(req.getSecurityLevel()))
                .append("；部署：").append(labelDeploy(req.getDeployType())).append("\n");
        if (req.getAiRequirementText() != null && !req.getAiRequirementText().isBlank()) {
            String air = req.getAiRequirementText().trim();
            // 单次 prompt 过长会导致 LLM 生成耗时过长进而触发 nginx 504：
            // 这里收紧输入预算；若用户确实需要超大文本，可考虑“分模块/分批生成”。
            if (air.length() > 6000) {
                air = air.substring(0, 6000) + "…";
            }
            userBlock.append("\n【AI 录入功能模块时的用户原文】（可与 PRD 互补；用例须与下文功能清单一致，不得以原文为由虚构清单外功能）\n")
                    .append(air).append("\n");
        }
        if (req.getPrdSummary() != null && !req.getPrdSummary().isBlank()) {
            String prd = req.getPrdSummary().trim();
            if (prd.length() > 4000) {
                prd = prd.substring(0, 4000) + "…";
            }
            userBlock.append("\n【PRD/需求摘要】\n").append(prd).append("\n");
        }
        if (req.getAcceptanceCriteriaNote() != null && !req.getAcceptanceCriteriaNote().isBlank()) {
            userBlock.append("\n【验收标准补充说明（用例须与此一致、不得抵触）】\n")
                    .append(req.getAcceptanceCriteriaNote().trim()).append("\n");
        }
        userBlock.append("\n【功能清单（归属模块必须严格使用下列「模块」列中已有名称，禁止新增模块名；用例应覆盖主要功能点）】\n");
        userBlock.append(buildModulesMarkdownFromWireMaps(req.getModules()));

        String system = """
                你是资深软件测试工程师，负责编写可执行的合同/交付验收级「测试用例清单」。
                必须仅输出一个 JSON 对象，不要 Markdown 代码块，不要其它解释文字。结构严格为：
                {"acceptanceTestCases":[{"caseName":"","module":"","priority":"","preconditions":"","testRole":"","steps":"","expectedResult":"","testResult":"","remark":""}]}
                
                字段要求（全部为字符串；无内容时 remark 可为 ""，preconditions 至少写「无」或简短说明）：
                - caseName：简洁明确的用例名称，建议能呼应具体功能点。
                - module：必须与用户给出的功能清单表格中「模块」列某一名称完全一致，禁止自造模块名。
                - priority：只能是「高」「中」「低」之一；参考功能点复杂度（extreme/complex→高，medium→中，simple/standard→低）。
                - preconditions：账号/角色/数据/环境等前置条件。
                - testRole：测试时所使用的系统角色（如「未登录访客」「普通用户」「管理员」等），须与业务相符。
                - steps：测试步骤，使用编号「1.」「2.」分行描述。
                - expectedResult：可观察、可判定的预期结果。
                - testResult：新生成的用例必须固定为「待测试」三个字，禁止填「通过」或「驳回」。
                - remark：补充说明；可空字符串。
                
                规则：
                1. 至少覆盖每个模块 1～2 条用例，并与功能点名称呼应；总条数建议不超过 60 条（必须尽量遵守以避免超时）。
                2. 不得描述功能清单中不存在的模块或功能，不得与 PRD/验收补充说明矛盾；若用户提供了「AI 录入功能模块时的用户原文」，可据此细化步骤与预期，但不得以原文为由虚构清单外功能。
                3. JSON 中不得出现除上述字段以外的顶层键（除 acceptanceTestCases 外仅可省略其它键）。
                4. 必须输出合法 JSON：字符串值内如需换行请使用 \\n 转义，禁止在引号对之间直接插入未转义的换行符（否则会导致解析失败）。
                """;

        List<DeepSeekClient.ChatMessage> messages = List.of(
                new DeepSeekClient.ChatMessage("system", system),
                new DeepSeekClient.ChatMessage("user", userBlock.toString())
        );
        // 输出太长会显著增加生成耗时，触发 nginx 504 的概率也更高。
        // 用例较多时 4096 易截断导致 JSON 不完整；与功能模块解析一致放宽到 8192
        DeepSeekClient.ChatResult result = deepSeekClient.chatWithUsage(cfg.getApiKey(), model, messages, true, 8192);
        String content = result != null ? result.getContent() : null;
        if (content == null || content.isBlank()) {
            throw new IllegalStateException("AI 未返回有效内容，请稍后重试");
        }
        JsonNode root = parseAiJsonOrThrow(content, "acceptanceTestCases");
        JsonNode arr = root.get("acceptanceTestCases");
        if (arr == null || !arr.isArray()) {
            throw new IllegalStateException("AI 返回缺少 acceptanceTestCases 数组");
        }
        List<Map<String, Object>> normalized = normalizeAcceptanceTestCasesJsonArray(arr, moduleOrder);
        if (normalized.isEmpty()) {
            throw new IllegalStateException("未解析出有效测试用例，请检查功能清单后重试");
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("acceptanceTestCases", normalized);
        if (result != null) {
            Map<String, Object> usage = new LinkedHashMap<>();
            usage.put("inputTokens", result.getInputTokens());
            usage.put("outputTokens", result.getOutputTokens());
            usage.put("model", result.getModel() != null ? result.getModel() : model);
            out.put("usage", usage);
        }
        return out;
    }

    @SuppressWarnings("unchecked")
    private String buildModulesMarkdownFromWireMaps(List<Map<String, Object>> modules) {
        if (modules == null || modules.isEmpty()) {
            return "（无模块）\n";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("| 模块 | 功能点 | 复杂度 | 数量 |\n| --- | --- | --- | --- |\n");
        final int MAX_ITEMS_PER_MODULE = 8; // 控制 prompt 体积，避免单次请求过长触发超时
        for (Map<String, Object> mod : modules) {
            if (mod == null) continue;
            Object mn = mod.get("name");
            String modName = mn != null ? mn.toString().trim() : "";
            if (modName.isEmpty()) continue;
            Object itemsObj = mod.get("items");
            if (!(itemsObj instanceof List<?> itemList) || itemList.isEmpty()) {
                sb.append("| ").append(escMdCell(modName)).append(" | （无功能点） |  |  |\n");
                continue;
            }
            int itemCount = 0;
            for (Object io : itemList) {
                if (!(io instanceof Map)) continue;
                Map<String, Object> it = (Map<String, Object>) io;
                Object in = it.get("name");
                String itemName = in != null ? in.toString().trim() : "";
                if (itemName.isEmpty()) continue;
                String cx = Objects.toString(it.get("complexity"), "standard");
                int qty = 1;
                if (it.get("quantity") instanceof Number n) {
                    qty = Math.max(1, n.intValue());
                }
                sb.append("| ").append(escMdCell(modName)).append(" | ").append(escMdCell(itemName)).append(" | ")
                        .append(escMdCell(cx)).append(" | ").append(qty).append(" |\n");
                itemCount++;
                if (itemCount >= MAX_ITEMS_PER_MODULE) break;
            }
        }
        return sb.toString();
    }

    private List<Map<String, Object>> normalizeAcceptanceTestCasesJsonArray(JsonNode arr, List<String> moduleOrder) {
        List<Map<String, Object>> out = new ArrayList<>();
        LinkedHashSet<String> allowed = new LinkedHashSet<>(moduleOrder);
        for (JsonNode node : arr) {
            if (node == null || !node.isObject()) continue;
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("caseName", textOrEmpty(node, "caseName"));
            String rawMod = textOrEmpty(node, "module");
            row.put("module", snapModuleToAllowed(rawMod, moduleOrder, allowed));
            row.put("priority", normalizeAcceptancePriority(textOrEmpty(node, "priority")));
            row.put("preconditions", blankToHint(textOrEmpty(node, "preconditions"), "无"));
            row.put("testRole", blankToHint(textOrEmpty(node, "testRole"), "普通用户"));
            row.put("steps", textOrEmpty(node, "steps"));
            row.put("expectedResult", textOrEmpty(node, "expectedResult"));
            row.put("testResult", "待测试");
            row.put("remark", textOrEmpty(node, "remark"));
            if (row.get("caseName").toString().isBlank()) {
                continue;
            }
            out.add(row);
            if (out.size() >= 100) {
                break;
            }
        }
        return out;
    }

    private static String textOrEmpty(JsonNode node, String field) {
        JsonNode n = node.get(field);
        if (n == null || n.isNull()) return "";
        return n.asText("").trim();
    }

    private static String blankToHint(String s, String hint) {
        return (s == null || s.isBlank()) ? hint : s.trim();
    }

    private static String snapModuleToAllowed(String rawMod, List<String> moduleOrder, Set<String> allowed) {
        if (rawMod != null && allowed.contains(rawMod.trim())) {
            return rawMod.trim();
        }
        if (rawMod == null || rawMod.isBlank()) {
            return moduleOrder.isEmpty() ? "" : moduleOrder.get(0);
        }
        String t = rawMod.trim();
        for (String m : moduleOrder) {
            if (t.equalsIgnoreCase(m) || t.contains(m) || m.contains(t)) {
                return m;
            }
        }
        return moduleOrder.isEmpty() ? t : moduleOrder.get(0);
    }

    private static String normalizeAcceptancePriority(String p) {
        if (p == null || p.isBlank()) return "中";
        String x = p.trim();
        if (x.equals("高") || x.equalsIgnoreCase("high") || x.toUpperCase().contains("P0") || x.contains("紧急")) {
            return "高";
        }
        if (x.equals("低") || x.equalsIgnoreCase("low") || x.toUpperCase().contains("P3")) {
            return "低";
        }
        return "中";
    }

}
