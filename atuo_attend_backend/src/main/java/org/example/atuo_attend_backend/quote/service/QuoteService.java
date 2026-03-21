package org.example.atuo_attend_backend.quote.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.atuo_attend_backend.ai.client.DeepSeekClient;
import org.example.atuo_attend_backend.ai.domain.AiAnalysisConfig;
import org.example.atuo_attend_backend.ai.service.AiAnalysisConfigService;
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
    private final AiAnalysisConfigService aiConfigService;
    private final DeepSeekClient deepSeekClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public QuoteService(QuoteProjectMapper projectMapper, QuoteModuleMapper moduleMapper, QuoteItemMapper itemMapper,
                        QuoteBaselineMapper baselineMapper, QuoteRiskConfigMapper riskConfigMapper,
                        QuotePriceConfigMapper priceConfigMapper, QuoteResultMapper resultMapper,
                        QuoteDocumentMapper documentMapper, QuoteContractDraftMapper contractDraftMapper,
                        AiAnalysisConfigService aiConfigService, DeepSeekClient deepSeekClient) {
        this.projectMapper = projectMapper;
        this.moduleMapper = moduleMapper;
        this.itemMapper = itemMapper;
        this.baselineMapper = baselineMapper;
        this.riskConfigMapper = riskConfigMapper;
        this.priceConfigMapper = priceConfigMapper;
        this.resultMapper = resultMapper;
        this.documentMapper = documentMapper;
        this.contractDraftMapper = contractDraftMapper;
        this.aiConfigService = aiConfigService;
        this.deepSeekClient = deepSeekClient;
    }

    @Transactional
    public long createProject(QuoteProjectSaveDto dto) {
        QuoteProject p = toProject(dto);
        if (p.getName() == null || p.getName().isBlank()) throw new IllegalArgumentException("项目名称不能为空");
        if (p.getStatus() == null) p.setStatus("draft");
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
        return p;
    }

    private static String nvl(String v, String d) {
        return v == null || v.isBlank() ? d : v;
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
        if (latest != null) out.put("latestResult", resultToMap(latest));
        return out;
    }

    public Map<String, Object> listProjects(int page, int pageSize) {
        page = Math.max(1, page);
        pageSize = Math.min(Math.max(pageSize, 1), 100);
        int offset = (page - 1) * pageSize;
        long total = projectMapper.countAll();
        List<Map<String, Object>> rows = new ArrayList<>();
        for (QuoteProject p : projectMapper.listPaged(offset, pageSize)) {
            rows.add(projectToMap(p));
        }
        Map<String, Object> data = new HashMap<>();
        data.put("items", rows);
        data.put("total", total);
        data.put("page", page);
        data.put("pageSize", pageSize);
        return data;
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
        BigDecimal baseAmount = totalDays.multiply(pricePerDay).setScale(2, RoundingMode.HALF_UP);
        BigDecimal mult = BigDecimal.ONE.add(riskPct.divide(HUNDRED, 6, RoundingMode.HALF_UP));
        BigDecimal finalAmount = baseAmount.multiply(mult).setScale(2, RoundingMode.HALF_UP);
        BigDecimal riskAmount = finalAmount.subtract(baseAmount).setScale(2, RoundingMode.HALF_UP);
        int confidence = computeConfidence(p, modules, totalDays);
        List<String> hints = buildRiskHints(confidence, req.getAuditChecklist());
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
        r.setRegionLabelUsed(regionLabel);
        resultMapper.insert(r);
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
        sb.append("<!DOCTYPE html><html><head><meta charset='UTF-8'><title>报价单</title>");
        sb.append("<style>body{font-family:sans-serif;padding:24px;color:#111} h1{font-size:20px} table{border-collapse:collapse;width:100%;margin-top:16px} th,td{border:1px solid #ddd;padding:8px;font-size:13px} th{background:#f3f4f6}</style>");
        sb.append("</head><body>");
        sb.append("<h1>项目报价单</h1>");
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

    @Transactional
    public Map<String, Object> generateContract(long quoteResultId, ContractGenerateRequest req) throws Exception {
        QuoteResult r = resultMapper.findById(quoteResultId);
        if (r == null) throw new IllegalArgumentException("报价结果不存在");
        QuoteProject p = projectMapper.findById(r.getQuoteProjectId());
        AiAnalysisConfig cfg = aiConfigService.getConfig();
        if (cfg.getApiKey() == null || cfg.getApiKey().isBlank()) {
            throw new IllegalStateException("请先在 AI 配置中填写 DeepSeek API Key");
        }
        if (!Boolean.TRUE.equals(cfg.getEnabled())) {
            throw new IllegalStateException("请先在 AI 配置中开启单次提交 AI 分析（共用 DeepSeek）");
        }
        String model = cfg.getModel() != null && !cfg.getModel().isBlank() ? cfg.getModel() : "deepseek-chat";
        String templateHint = "software_dev".equals(req.getTemplateType())
                ? "中国大陆软件开发外包合同，需包含：标的与交付范围、交付与验收、价款与支付、知识产权、保密、违约责任、争议解决等条款。"
                : "软件维护服务合同，明确维护范围、响应时间、费用与期限。";
        String cn = req.getClientName() != null ? req.getClientName() : "甲方";
        String yn = req.getCompanyName() != null ? req.getCompanyName() : "乙方";
        String userPrompt = """
                请根据以下结构化信息起草一份合同正文（使用中文），条理清晰，用 Markdown 标题分级，不要编造双方具体地址除非必要可用占位。
                甲方（委托方/客户）：%s
                乙方（受托方/开发方）：%s
                项目：%s
                最终报价金额（人民币）：%s 元
                总人天：%s
                合同类型要求：%s
                
                报价摘要：基础金额 %s 元，风险调整后 %s 元，人天单价 %s。
                """.formatted(
                cn,
                yn,
                p.getName() != null ? p.getName() : "",
                r.getFinalAmount().toPlainString(),
                r.getTotalDays().toPlainString(),
                templateHint,
                r.getBaseAmount().toPlainString(),
                r.getFinalAmount().toPlainString(),
                r.getPricePerDayUsed().toPlainString()
        );
        List<DeepSeekClient.ChatMessage> messages = List.of(
                new DeepSeekClient.ChatMessage("system", "你是资深法务助理，擅长 IT 外包合同起草。输出可直接给商务使用的合同正文。"),
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
        String html = "<!DOCTYPE html><html><head><meta charset='UTF-8'><title>合同</title></head><body><pre style='white-space:pre-wrap;font-family:sans-serif'>"
                + esc(body != null ? body : "") + "</pre></body></html>";
        documentMapper.insert(quoteResultId, "contract", html, 1);
        Map<String, Object> data = new HashMap<>();
        data.put("html", html);
        data.put("filename", "contract-" + quoteResultId + ".html");
        return data;
    }

    private Map<String, Object> projectToMap(QuoteProject p) {
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
        m.put("createdAt", p.getCreatedAt());
        m.put("updatedAt", p.getUpdatedAt());
        return m;
    }

    private Map<String, Object> resultToMap(QuoteResult r) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", r.getId());
        m.put("quoteProjectId", r.getQuoteProjectId());
        m.put("totalDays", r.getTotalDays());
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
}
