package org.example.atuo_attend_backend.quote.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.atuo_attend_backend.ai.client.DeepSeekClient;
import org.example.atuo_attend_backend.ai.domain.AiAnalysisConfig;
import org.example.atuo_attend_backend.ai.service.AiAnalysisConfigService;
import org.example.atuo_attend_backend.config.SystemConfigService;
import org.example.atuo_attend_backend.quote.domain.*;
import org.example.atuo_attend_backend.quote.dto.*;
import org.example.atuo_attend_backend.quote.mapper.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class QuoteService {

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
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final BigDecimal RISK_PCT_MIN = new BigDecimal("-50");
    private static final BigDecimal RISK_PCT_MAX = new BigDecimal("150");

    public QuoteService(QuoteProjectMapper projectMapper, QuoteModuleMapper moduleMapper, QuoteItemMapper itemMapper,
                        QuoteBaselineMapper baselineMapper, QuoteRiskConfigMapper riskConfigMapper,
                        QuotePriceConfigMapper priceConfigMapper, QuoteResultMapper resultMapper,
                        QuoteDocumentMapper documentMapper, QuoteContractDraftMapper contractDraftMapper,
                        QuotePresetItemMapper presetItemMapper,
                        AiAnalysisConfigService aiConfigService, DeepSeekClient deepSeekClient,
                        SystemConfigService systemConfigService) {
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
    }

    @Transactional
    public long createProject(QuoteProjectSaveDto dto) {
        QuoteProject p = toProject(dto);
        if (p.getName() == null || p.getName().isBlank()) throw new IllegalArgumentException("项目名称不能为空");
        if (p.getStatus() == null) p.setStatus("draft");
        if (p.getQuoteSubjectMode() == null) {
            p.setQuoteSubjectMode("legal_entity");
        }
        projectMapper.insert(p);
        saveModules(p.getId(), dto.getModules());
        return p.getId();
    }

    @Transactional
    public void updateProject(long id, QuoteProjectSaveDto dto) {
        QuoteProject existing = projectMapper.findById(id);
        if (existing == null) throw new IllegalArgumentException("项目不存在");
        QuoteProject p = toProject(dto);
        p.setId(id);
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
        projectMapper.update(p);
        moduleMapper.deleteByProjectId(id);
        saveModules(id, dto.getModules());
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
        p.setQuoteVendorName(trimToNull(dto.getQuoteVendorName()));
        p.setQuoteContactInfo(trimToNull(dto.getQuoteContactInfo()));
        p.setQuoteValidityNote(trimToNull(dto.getQuoteValidityNote()));
        if (dto.getQuoteSubjectMode() != null) {
            p.setQuoteSubjectMode(normalizeQuoteSubjectMode(dto.getQuoteSubjectMode()));
        }
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

    private void saveModules(long projectId, List<QuoteModuleSaveDto> modules) {
        if (modules == null) return;
        int mi = 0;
        for (QuoteModuleSaveDto md : modules) {
            if (md.getName() == null || md.getName().isBlank()) continue;
            QuoteModule m = new QuoteModule();
            m.setQuoteProjectId(projectId);
            m.setName(md.getName().trim());
            m.setSortOrder(md.getSortOrder() != 0 ? md.getSortOrder() : mi++);
            moduleMapper.insert(m);
            int ii = 0;
            for (QuoteItemSaveDto it : md.getItems()) {
                if (it.getName() == null || it.getName().isBlank()) continue;
                QuoteItem item = new QuoteItem();
                item.setModuleId(m.getId());
                item.setName(it.getName().trim());
                item.setComplexity(nvl(it.getComplexity(), "standard"));
                item.setQuantity(Math.max(1, it.getQuantity()));
                item.setEstimatedDays(BigDecimal.ZERO);
                item.setSortOrder(ii++);
                itemMapper.insert(item);
            }
        }
    }

    public Map<String, Object> getProjectDetail(long id) {
        QuoteProject p = projectMapper.findById(id);
        if (p == null) return null;
        Map<String, Object> out = projectToMap(p);
        List<Map<String, Object>> moduleList = new ArrayList<>();
        for (QuoteModule m : moduleMapper.listByProjectId(id)) {
            Map<String, Object> mm = new LinkedHashMap<>();
            mm.put("id", m.getId());
            mm.put("name", m.getName());
            mm.put("sortOrder", m.getSortOrder());
            List<Map<String, Object>> items = new ArrayList<>();
            for (QuoteItem it : itemMapper.listByModuleId(m.getId())) {
                Map<String, Object> im = new LinkedHashMap<>();
                im.put("id", it.getId());
                im.put("name", it.getName());
                im.put("complexity", it.getComplexity());
                im.put("quantity", it.getQuantity());
                im.put("estimatedDays", it.getEstimatedDays());
                items.add(im);
            }
            mm.put("items", items);
            moduleList.add(mm);
        }
        out.put("modules", moduleList);
        QuoteResult latest = resultMapper.findLatestByProjectId(id);
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
        for (Map<String, Object> row : priceConfigMapper.listAll()) {
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
        QuoteProject existing = projectMapper.findById(projectId);
        if (existing == null) throw new IllegalArgumentException("项目不存在");
        try {
            String json = objectMapper.writeValueAsString(buildQuoteCalcPrefsMap(req));
            projectMapper.updateQuoteCalcPrefs(projectId, json);
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
        long total = projectMapper.countAll();
        List<Map<String, Object>> rows = new ArrayList<>();
        for (QuoteProject p : projectMapper.listPaged(offset, pageSize)) {
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
        QuoteProject existing = projectMapper.findById(id);
        if (existing == null) throw new IllegalArgumentException("项目不存在");

        // 先清理业务树：modules/items
        List<QuoteModule> modules = moduleMapper.listByProjectId(id);
        if (modules != null) {
            for (QuoteModule m : modules) {
                itemMapper.deleteByModuleId(m.getId());
            }
        }
        moduleMapper.deleteByProjectId(id);

        // 再清理计算结果：result + document/contractDraft
        List<Long> resultIds = resultMapper.listIdsByProjectId(id);
        if (resultIds != null) {
            for (Long rid : resultIds) {
                documentMapper.deleteByResultId(rid);
                contractDraftMapper.deleteByResultId(rid);
            }
        }
        resultMapper.deleteByProjectId(id);

        // 最后删除项目主表
        projectMapper.deleteById(id);
    }

    @Transactional
    public Map<String, Object> calculate(long projectId, QuoteCalculateRequest req) throws Exception {
        QuoteProject p = projectMapper.findById(projectId);
        if (p == null) throw new IllegalArgumentException("项目不存在");
        List<QuoteModule> modules = moduleMapper.listByProjectId(projectId);
        BigDecimal totalDays = BigDecimal.ZERO;
        for (QuoteModule m : modules) {
            for (QuoteItem it : itemMapper.listByModuleId(m.getId())) {
                BigDecimal base = baselineMapper.findDays(p.getTechStack(), it.getComplexity());
                if (base == null) base = baselineMapper.findDays("other", it.getComplexity());
                if (base == null) base = new BigDecimal("1.5");
                BigDecimal line = base.multiply(new BigDecimal(it.getQuantity())).setScale(2, RoundingMode.HALF_UP);
                itemMapper.updateEstimatedDays(it.getId(), line);
                totalDays = totalDays.add(line);
            }
        }
        Map<String, BigDecimal> riskByKey = new HashMap<>();
        for (Map<String, Object> row : riskConfigMapper.listEnabled()) {
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
            priceRow = priceConfigMapper.findById(req.getPriceConfigId());
        }
        if (priceRow == null) {
            List<Map<String, Object>> list = priceConfigMapper.listEnabled();
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
        resultMapper.insert(r);
        try {
            projectMapper.updateQuoteCalcPrefs(projectId, objectMapper.writeValueAsString(buildQuoteCalcPrefsMap(req)));
        } catch (Exception ignored) {
            // 不因偏好落库失败影响报价结果
        }
        Map<String, Object> data = resultToMap(r);
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
            itemCount += itemMapper.listByModuleId(m.getId()).size();
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
        QuoteProject p = projectMapper.findById(projectId);
        if (p == null) throw new IllegalArgumentException("项目不存在");
        QuoteResult r = quoteResultId != null ? resultMapper.findById(quoteResultId) : resultMapper.findLatestByProjectId(projectId);
        if (r == null || r.getQuoteProjectId() != projectId) throw new IllegalArgumentException("请先计算报价或指定有效的报价结果 ID");
        String html = renderQuoteHtml(p, r);
        documentMapper.insert(r.getId(), "quote", html, 1);
        Map<String, Object> data = new HashMap<>();
        data.put("html", html);
        data.put("filename", "quote-" + projectId + "-" + r.getId() + ".html");
        data.put("quoteResultId", r.getId());
        return data;
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
        sb.append("<table><thead><tr><th>模块</th><th>功能点</th><th>复杂度</th><th>数量</th><th>人天</th></tr></thead><tbody>");
        for (QuoteModule m : moduleMapper.listByProjectId(p.getId())) {
            for (QuoteItem it : itemMapper.listByModuleId(m.getId())) {
                sb.append("<tr><td>").append(esc(m.getName())).append("</td><td>").append(esc(it.getName()))
                        .append("</td><td>").append(esc(it.getComplexity())).append("</td><td>").append(it.getQuantity())
                        .append("</td><td>").append(it.getEstimatedDays() != null ? it.getEstimatedDays().toPlainString() : "0")
                        .append("</td></tr>");
            }
        }
        sb.append("</tbody></table>");
        sb.append("<p style='margin-top:20px'><strong>总人天：</strong>").append(r.getTotalDays().toPlainString());
        sb.append(" &nbsp; <strong>工期系数：</strong>").append(r.getDurationCoefficientUsed() != null ? r.getDurationCoefficientUsed().toPlainString() : "1.2");
        sb.append(" &nbsp; <strong>预计工期（天）：</strong>").append(r.getEstimatedDurationDays() != null ? r.getEstimatedDurationDays().toPlainString() : "0");
        sb.append(" &nbsp; <strong>人天单价：</strong>").append(r.getPricePerDayUsed().toPlainString()).append("（").append(esc(r.getRegionLabelUsed())).append("）</p>");
        sb.append("<p><strong>基础金额：</strong>").append(r.getBaseAmount().toPlainString());
        sb.append(" &nbsp; <strong>风险合计：</strong>+").append(r.getRiskPctTotal().toPlainString()).append("%，风险金额：").append(r.getRiskAmount().toPlainString());
        sb.append(" &nbsp; <strong>最终报价：</strong><span style='color:#059669;font-size:18px'>").append(r.getFinalAmount().toPlainString()).append("</span> 元</p>");
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
        QuoteResult r = resultMapper.findById(quoteResultId);
        if (r == null) throw new IllegalArgumentException("报价结果不存在");
        QuoteProject p = projectMapper.findById(r.getQuoteProjectId());
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
        String content = deepSeekClient.chat(cfg.getApiKey(), model, messages, false);
        if (content == null || content.isBlank()) throw new IllegalStateException("AI 未返回合同内容");
        QuoteContractDraft d = contractDraftMapper.findByResultId(quoteResultId);
        if (d == null) {
            d = new QuoteContractDraft();
            d.setQuoteResultId(quoteResultId);
            d.setClientName(req.getClientName());
            d.setCompanyName(req.getCompanyName());
            d.setTemplateType(req.getTemplateType() != null ? req.getTemplateType() : "software_dev");
            d.setAiPromptSnapshot(userPrompt);
            d.setAiRawResponse(content);
            d.setEditedContent(content);
            contractDraftMapper.insert(d);
        } else {
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
        QuoteContractDraft d = contractDraftMapper.findByResultId(quoteResultId);
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
        if (contractDraftMapper.findByResultId(quoteResultId) == null) {
            throw new IllegalArgumentException("合同草稿不存在，请先生成");
        }
        contractDraftMapper.updateEditedContent(quoteResultId, req.getEditedContent());
    }

    public Map<String, Object> exportContractHtml(long quoteResultId) {
        QuoteContractDraft d = contractDraftMapper.findByResultId(quoteResultId);
        if (d == null) throw new IllegalArgumentException("合同草稿不存在");
        String body = d.getEditedContent() != null ? d.getEditedContent() : d.getAiRawResponse();
        String html = "<!DOCTYPE html><html><head><meta charset=\"UTF-8\"/><title>合同</title></head><body><pre style='white-space:pre-wrap;font-family:sans-serif'>"
                + esc(body != null ? body : "") + "</pre></body></html>";
        documentMapper.insert(quoteResultId, "contract", html, 1);
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
        m.put("quoteVendorName", p.getQuoteVendorName());
        m.put("quoteContactInfo", p.getQuoteContactInfo());
        m.put("quoteValidityNote", p.getQuoteValidityNote());
        m.put("quoteSubjectMode", p.getQuoteSubjectMode() != null ? p.getQuoteSubjectMode() : "legal_entity");
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
        m.put("confidenceScore", r.getConfidenceScore());
        m.put("pricePerDayUsed", r.getPricePerDayUsed());
        m.put("regionLabelUsed", r.getRegionLabelUsed());
        m.put("createdAt", r.getCreatedAt());
        return m;
    }

    public List<Map<String, Object>> listPresetItems(boolean includeDisabled) {
        List<QuotePresetItem> list = includeDisabled ? presetItemMapper.listAll() : presetItemMapper.listEnabled();
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
        presetItemMapper.insert(row);
        return row.getId();
    }

    @Transactional
    public void updatePresetItem(long id, QuotePresetItemSaveDto dto) {
        QuotePresetItem existing = presetItemMapper.findById(id);
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
        if (presetItemMapper.findById(id) == null) throw new IllegalArgumentException("预设项不存在");
        presetItemMapper.deleteById(id);
    }

    @Transactional
    public void updateRiskConfigs(QuoteRiskConfigBatchUpdate batch) {
        if (batch.getItems() == null || batch.getItems().isEmpty()) {
            throw new IllegalArgumentException("请至少提交一条风险配置");
        }
        for (QuoteRiskConfigUpdateItem it : batch.getItems()) {
            if (it.getId() == null) throw new IllegalArgumentException("风险配置 id 不能为空");
            if (riskConfigMapper.countById(it.getId()) == 0) throw new IllegalArgumentException("无效的风险配置 id: " + it.getId());
            if (it.getLabel() == null || it.getLabel().isBlank()) throw new IllegalArgumentException("风险项名称不能为空");
            BigDecimal pct = it.getDefaultPct() != null ? it.getDefaultPct() : BigDecimal.ZERO;
            if (pct.compareTo(RISK_PCT_MIN) < 0 || pct.compareTo(RISK_PCT_MAX) > 0) {
                throw new IllegalArgumentException("风险百分比需在 " + RISK_PCT_MIN + "～" + RISK_PCT_MAX + " 之间（可为负表示降价）");
            }
            int en = Boolean.TRUE.equals(it.getEnabled()) ? 1 : 0;
            riskConfigMapper.updateRow(it.getId(), it.getLabel().trim(), pct.setScale(2, RoundingMode.HALF_UP), en);
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
        if (baselineMapper.countByStackAndComplexity(ts, cx) > 0) {
            throw new IllegalArgumentException("该技术栈与复杂度组合已存在");
        }
        QuoteBaseline row = new QuoteBaseline();
        row.setTechStack(ts);
        row.setComplexity(cx);
        row.setDays(dto.getDays().setScale(2, RoundingMode.HALF_UP));
        baselineMapper.insert(row);
        return row.getId();
    }

    @Transactional
    public void updateBaseline(long id, QuoteBaselineSaveDto dto) {
        if (baselineMapper.findById(id) == null) throw new IllegalArgumentException("人天基准不存在");
        String ts = dto.getTechStack() != null ? dto.getTechStack().trim() : "";
        String cx = dto.getComplexity() != null ? dto.getComplexity().trim() : "";
        if (ts.isEmpty()) throw new IllegalArgumentException("技术栈不能为空");
        if (cx.isEmpty()) throw new IllegalArgumentException("复杂度不能为空");
        validateBaselineDays(dto.getDays());
        if (baselineMapper.countByStackAndComplexityExcluding(ts, cx, id) > 0) {
            throw new IllegalArgumentException("该技术栈与复杂度组合已被其他行占用");
        }
        QuoteBaseline row = new QuoteBaseline();
        row.setId(id);
        row.setTechStack(ts);
        row.setComplexity(cx);
        row.setDays(dto.getDays().setScale(2, RoundingMode.HALF_UP));
        baselineMapper.update(row);
    }

    @Transactional
    public void deleteBaseline(long id) {
        if (baselineMapper.findById(id) == null) throw new IllegalArgumentException("人天基准不存在");
        baselineMapper.deleteById(id);
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
        if (priceConfigMapper.countByRegionLabel(rl) > 0) {
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
        priceConfigMapper.insert(row);
        return row.getId();
    }

    @Transactional
    public void updatePriceConfig(long id, QuotePriceConfigSaveDto dto) {
        Map<String, Object> cur = priceConfigMapper.findById(id);
        if (cur == null) throw new IllegalArgumentException("单价配置不存在");
        String rl = dto.getRegionLabel() != null ? dto.getRegionLabel().trim() : "";
        if (rl.isEmpty()) throw new IllegalArgumentException("地域/档位名称不能为空");
        validatePricePerDay(dto.getPricePerDay());
        if (priceConfigMapper.countByRegionLabelExcluding(rl, id) > 0) {
            throw new IllegalArgumentException("该档位名称已被其他行占用");
        }
        boolean wasEn = mapRowEnabled(cur);
        boolean newEn = dto.getEnabled() == null || dto.getEnabled();
        if (wasEn && !newEn && priceConfigMapper.countEnabled() <= 1) {
            throw new IllegalArgumentException("至少保留一条启用的单价档位");
        }
        QuotePriceConfigRow row = new QuotePriceConfigRow();
        row.setId(id);
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
        Map<String, Object> cur = priceConfigMapper.findById(id);
        if (cur == null) throw new IllegalArgumentException("单价配置不存在");
        if (mapRowEnabled(cur) && priceConfigMapper.countEnabled() <= 1) {
            throw new IllegalArgumentException("至少保留一条启用的单价档位，无法删除");
        }
        priceConfigMapper.deleteById(id);
    }

    // --- 合同：乙方主体模板（系统级）与附件 HTML ---

    public Map<String, Object> getPartyBProfile() {
        return new LinkedHashMap<>(systemConfigService.getQuotePartyBProfile());
    }

    public void savePartyBProfile(Map<String, Object> profile) throws JsonProcessingException {
        systemConfigService.saveQuotePartyBProfile(profile);
    }

    public Map<String, Object> buildContractAttachmentFunctionList(long projectId) {
        QuoteProject p = projectMapper.findById(projectId);
        if (p == null) throw new IllegalArgumentException("项目不存在");
        Map<String, Object> data = new HashMap<>();
        data.put("html", renderAttachmentFunctionListHtml(p));
        data.put("filename", "attachment-1-function-list-" + projectId + ".html");
        return data;
    }

    public Map<String, Object> buildContractAttachmentMilestoneSchedule(long projectId) {
        QuoteProject p = projectMapper.findById(projectId);
        if (p == null) throw new IllegalArgumentException("项目不存在");
        Map<String, Object> ctx = parseContractContextMap(p.getQuoteContractContextJson());
        Map<String, Object> data = new HashMap<>();
        data.put("html", renderAttachmentMilestoneHtml(p, ctx));
        data.put("filename", "attachment-3-milestones-" + projectId + ".html");
        return data;
    }

    /** 附件二：验收标准（草案）HTML */
    public Map<String, Object> buildContractAttachmentAcceptanceStandards(long projectId) {
        QuoteProject p = projectMapper.findById(projectId);
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
        sb.append("- 技术栈：").append(labelTechStack(p.getTechStack())).append("\n");
        sb.append("- 设计：").append(labelDesign(p.getDesignType())).append("；数据/对接：").append(labelDataMigration(p.getDataMigration())).append("\n");
        sb.append("- 并发量级：").append(labelConcurrency(p.getConcurrency())).append("；安全：").append(labelSecurity(p.getSecurityLevel())).append("；部署：").append(labelDeploy(p.getDeployType())).append("\n");
        String prd = p.getPrdSummary();
        if (prd != null && !prd.isBlank()) {
            sb.append("\n### PRD/需求摘要\n").append(prd.trim()).append("\n");
        }
        sb.append("\n### 功能清单（模块-功能点-复杂度-数量-估算人天）\n");
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
        sb.append("| 模块 | 功能点 | 复杂度 | 数量 | 估算人天 |\n| --- | --- | --- | --- | --- |\n");
        for (QuoteModule m : moduleMapper.listByProjectId(p.getId())) {
            for (QuoteItem it : itemMapper.listByModuleId(m.getId())) {
                sb.append("| ").append(escMdCell(m.getName())).append(" | ").append(escMdCell(it.getName())).append(" | ")
                        .append(escMdCell(it.getComplexity())).append(" | ").append(it.getQuantity()).append(" | ")
                        .append(it.getEstimatedDays() != null ? it.getEstimatedDays().toPlainString() : "0").append(" |\n");
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
        for (Map<String, Object> row : riskConfigMapper.listAll()) {
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
        for (QuoteModule m : moduleMapper.listByProjectId(p.getId())) {
            for (QuoteItem it : itemMapper.listByModuleId(m.getId())) {
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
     * 自然语言 → DeepSeek JSON → 与前端/保存接口一致的 modules 结构。
     */
    public Map<String, Object> parseQuoteModulesWithAi(QuoteAiModulesParseRequest req) {
        if (req == null || req.getRequirementText() == null || req.getRequirementText().isBlank()) {
            throw new IllegalArgumentException("需求描述不能为空");
        }
        String text = req.getRequirementText().trim();
        if (text.length() > 20000) {
            throw new IllegalArgumentException("需求描述过长，请控制在 20000 字以内");
        }

        AiAnalysisConfig cfg = aiConfigService.getConfig();
        if (cfg.getApiKey() == null || cfg.getApiKey().isBlank()) {
            throw new IllegalStateException("请先在管理后台「AI 配置」中填写 DeepSeek API Key");
        }
        String model = cfg.getModel() != null && !cfg.getModel().isBlank() ? cfg.getModel() : "deepseek-chat";

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

        String system = """
你是资深软件外包需求分析师。根据用户自然语言需求，拆解为「功能模块 → 功能点」清单，用于人天报价。
必须仅输出一个 JSON 对象，不要 Markdown 代码块，不要其它解释文字。结构严格为：
{"modules":[{"name":"模块中文名","items":[{"name":"功能点中文名","complexity":"standard","quantity":1}]}]}
规则：
- modules：按业务域划分（如用户与权限、订单与支付），至少 1 个模块；名称简洁。
- items：可交付功能粒度，每条一行；大功能可拆成多条。
- complexity 只能是英文枚举：simple、standard、medium、complex、extreme（对应 简单/标准/中等/复杂/极复杂 的工作量档位）。
- quantity：正整数，默认 1。
""";

        String userMsg = ctx + "\n【客户/需求原文】\n" + text;

        List<DeepSeekClient.ChatMessage> messages = List.of(
                new DeepSeekClient.ChatMessage("system", system),
                new DeepSeekClient.ChatMessage("user", userMsg)
        );
        DeepSeekClient.ChatResult result = deepSeekClient.chatWithUsage(cfg.getApiKey(), model, messages, true, 8192);
        String content = result != null ? result.getContent() : null;
        if (content == null || content.isBlank()) {
            throw new IllegalStateException("AI 未返回有效内容，请稍后重试或缩短描述");
        }
        String jsonStr = stripMarkdownCodeFence(content);
        JsonNode root;
        try {
            root = objectMapper.readTree(jsonStr);
        } catch (Exception e) {
            throw new IllegalStateException("AI 返回格式无法解析为 JSON，请重试");
        }
        JsonNode modulesNode = root.get("modules");
        if (modulesNode == null || !modulesNode.isArray()) {
            throw new IllegalStateException("AI 返回缺少 modules 数组");
        }

        List<Map<String, Object>> outModules = new ArrayList<>();
        int mi = 0;
        for (JsonNode mod : modulesNode) {
            String modName = mod.has("name") ? mod.get("name").asText("").trim() : "";
            if (modName.isEmpty()) {
                continue;
            }
            JsonNode itemsNode = mod.get("items");
            List<Map<String, Object>> items = new ArrayList<>();
            if (itemsNode != null && itemsNode.isArray()) {
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
            }
            if (items.isEmpty()) {
                continue;
            }
            Map<String, Object> mm = new LinkedHashMap<>();
            mm.put("name", modName);
            mm.put("sortOrder", mi++);
            mm.put("items", items);
            outModules.add(mm);
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

    private static String stripMarkdownCodeFence(String s) {
        if (s == null) {
            return "";
        }
        String t = s.trim();
        if (t.startsWith("```")) {
            int nl = t.indexOf('\n');
            if (nl > 0) {
                t = t.substring(nl + 1);
            } else {
                t = t.substring(3);
            }
            t = t.trim();
            if (t.endsWith("```")) {
                t = t.substring(0, t.length() - 3).trim();
            }
        }
        return t;
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
        if (req.getPrdSummary() != null && !req.getPrdSummary().isBlank()) {
            String prd = req.getPrdSummary().trim();
            if (prd.length() > 6000) {
                prd = prd.substring(0, 6000) + "…";
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
                1. 至少覆盖每个模块 1～3 条用例，并与功能点名称呼应；总条数建议不超过 80 条。
                2. 不得描述功能清单中不存在的模块或功能，不得与 PRD/验收补充说明矛盾。
                3. JSON 中不得出现除上述字段以外的顶层键（除 acceptanceTestCases 外仅可省略其它键）。
                """;

        List<DeepSeekClient.ChatMessage> messages = List.of(
                new DeepSeekClient.ChatMessage("system", system),
                new DeepSeekClient.ChatMessage("user", userBlock.toString())
        );
        DeepSeekClient.ChatResult result = deepSeekClient.chatWithUsage(cfg.getApiKey(), model, messages, true, 8192);
        String content = result != null ? result.getContent() : null;
        if (content == null || content.isBlank()) {
            throw new IllegalStateException("AI 未返回有效内容，请稍后重试");
        }
        String jsonStr = stripMarkdownCodeFence(content);
        JsonNode root;
        try {
            root = objectMapper.readTree(jsonStr);
        } catch (Exception e) {
            throw new IllegalStateException("AI 返回格式无法解析为 JSON，请重试");
        }
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
