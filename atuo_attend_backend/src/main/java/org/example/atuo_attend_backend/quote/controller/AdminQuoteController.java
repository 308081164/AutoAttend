package org.example.atuo_attend_backend.quote.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.example.atuo_attend_backend.common.ApiResponse;
import org.example.atuo_attend_backend.quote.dto.*;
import org.example.atuo_attend_backend.quote.mapper.QuoteBaselineMapper;
import org.example.atuo_attend_backend.quote.mapper.QuotePriceConfigMapper;
import org.example.atuo_attend_backend.quote.mapper.QuoteRiskConfigMapper;
import org.example.atuo_attend_backend.quote.service.QuoteDocumentExportService;
import org.example.atuo_attend_backend.quote.service.QuoteCollabLinkService;
import org.example.atuo_attend_backend.quote.service.QuoteProvisionService;
import org.example.atuo_attend_backend.quote.service.QuoteService;
import org.example.atuo_attend_backend.quote.service.QuoteAiAcceptanceTestCasesJobService;
import org.example.atuo_attend_backend.quote.service.QuoteAiContractGenerateJobService;
import org.example.atuo_attend_backend.admin.auth.AdminAuthFilter;
import org.example.atuo_attend_backend.platform.service.PlatformComponentEventService;
import org.example.atuo_attend_backend.tenant.context.TenantConstants;
import org.example.atuo_attend_backend.tenant.context.TenantContext;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/admin/quote")
public class AdminQuoteController {

    private final QuoteService quoteService;
    private final QuoteAiAcceptanceTestCasesJobService acceptanceJobService;
    private final QuoteAiContractGenerateJobService contractJobService;
    private final QuoteDocumentExportService quoteDocumentExportService;
    private final QuoteBaselineMapper baselineMapper;
    private final QuoteRiskConfigMapper riskConfigMapper;
    private final QuotePriceConfigMapper priceConfigMapper;
    private final QuoteProvisionService quoteProvisionService;
    private final QuoteCollabLinkService quoteCollabLinkService;
    private final PlatformComponentEventService componentEventService;

    public AdminQuoteController(QuoteService quoteService, QuoteDocumentExportService quoteDocumentExportService,
                                QuoteBaselineMapper baselineMapper,
                                QuoteRiskConfigMapper riskConfigMapper, QuotePriceConfigMapper priceConfigMapper,
                                QuoteProvisionService quoteProvisionService,
                                QuoteCollabLinkService quoteCollabLinkService,
                                QuoteAiAcceptanceTestCasesJobService acceptanceJobService,
                                QuoteAiContractGenerateJobService contractJobService,
                                PlatformComponentEventService componentEventService) {
        this.quoteService = quoteService;
        this.acceptanceJobService = acceptanceJobService;
        this.contractJobService = contractJobService;
        this.quoteDocumentExportService = quoteDocumentExportService;
        this.baselineMapper = baselineMapper;
        this.riskConfigMapper = riskConfigMapper;
        this.priceConfigMapper = priceConfigMapper;
        this.quoteProvisionService = quoteProvisionService;
        this.quoteCollabLinkService = quoteCollabLinkService;
        this.componentEventService = componentEventService;
    }

    private static long tid() {
        return TenantContext.getTenantIdOrDefault(TenantConstants.DEFAULT_TENANT_ID);
    }

    @PostMapping("/projects")
    public ApiResponse<Map<String, Object>> createProject(@RequestBody QuoteProjectSaveDto body) {
        try {
            long id = quoteService.createProject(body);
            Map<String, Object> data = new HashMap<>();
            data.put("id", id);
            return ApiResponse.ok(data);
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(40000, e.getMessage());
        }
    }

    /**
     * 将自然语言需求交由 DeepSeek 解析为「功能模块 + 功能点（复杂度、数量）」JSON，供前端填入表单。
     * 使用管理后台「AI 配置」中的 DeepSeek Key；不落库。
     */
    @PostMapping("/ai/parse-modules")
    public ApiResponse<Map<String, Object>> parseModulesWithAi(@RequestBody(required = false) QuoteAiModulesParseRequest body) {
        if (body == null) {
            body = new QuoteAiModulesParseRequest();
        }
        try {
            return ApiResponse.ok(quoteService.parseQuoteModulesWithAi(body));
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(40000, e.getMessage());
        } catch (IllegalStateException e) {
            return ApiResponse.error(50000, e.getMessage());
        } catch (Exception e) {
            return ApiResponse.error(50000, e.getMessage() != null ? e.getMessage() : "解析失败");
        }
    }

    /**
     * 根据当前功能清单与项目上下文生成「验收测试用例/测试清单」JSON（不落库）；与 DeepSeek、AI 配置同源。
     */
    @PostMapping("/ai/acceptance-test-cases")
    public ApiResponse<Map<String, Object>> generateAcceptanceTestCasesWithAi(@RequestBody(required = false) QuoteAiAcceptanceTestCasesRequest body) {
        if (body == null) {
            body = new QuoteAiAcceptanceTestCasesRequest();
        }
        try {
            return ApiResponse.ok(quoteService.generateAcceptanceTestCasesWithAi(body));
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(40000, e.getMessage());
        } catch (IllegalStateException e) {
            return ApiResponse.error(50000, e.getMessage());
        } catch (Exception e) {
            return ApiResponse.error(50000, e.getMessage() != null ? e.getMessage() : "生成失败");
        }
    }

    /**
     * 异步生成「验收测试用例/测试清单」：入队立即返回 jobId，前端轮询避免 nginx 同步超时。
     */
    @PostMapping("/projects/{id}/ai/acceptance-test-cases/jobs")
    public ApiResponse<Map<String, Object>> enqueueAcceptanceTestCasesJob(@PathVariable long id,
                                                                               @RequestBody(required = false) QuoteAiAcceptanceTestCasesRequest body) {
        if (body == null) body = new QuoteAiAcceptanceTestCasesRequest();
        try {
            long jobId = acceptanceJobService.enqueue(id, body);
            Map<String, Object> out = new HashMap<>();
            out.put("jobId", jobId);
            return ApiResponse.ok(out);
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(40000, e.getMessage());
        } catch (IllegalStateException e) {
            return ApiResponse.error(50000, e.getMessage());
        } catch (Exception e) {
            return ApiResponse.error(50000, e.getMessage() != null ? e.getMessage() : "入队失败");
        }
    }

    @GetMapping("/projects/{id}/ai/acceptance-test-cases/jobs/{jobId}")
    public ApiResponse<QuoteAiAcceptanceTestCasesJobStatus> getAcceptanceTestCasesJobStatus(@PathVariable long id,
                                                                                               @PathVariable long jobId) {
        try {
            QuoteAiAcceptanceTestCasesJobStatus s = acceptanceJobService.getStatus(id, jobId);
            if (s == null) return ApiResponse.error(40400, "任务不存在");
            return ApiResponse.ok(s);
        } catch (Exception e) {
            return ApiResponse.error(50000, e.getMessage() != null ? e.getMessage() : "获取任务状态失败");
        }
    }

    @GetMapping("/projects")
    public ApiResponse<Map<String, Object>> listProjects(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "pageSize", defaultValue = "20") int pageSize) {
        return ApiResponse.ok(quoteService.listProjects(page, pageSize));
    }

    @GetMapping("/projects/{id}")
    public ApiResponse<Map<String, Object>> getProject(@PathVariable long id) {
        Map<String, Object> d = quoteService.getProjectDetail(id);
        if (d == null) return ApiResponse.error(40400, "项目不存在");
        return ApiResponse.ok(d);
    }

    /**
     * 报价 -> 项目创建（MVP）：创建个人 GitHub 仓库、写入需求清单 MD、同步到协作多维表、配置 Webhook。
     */
    @PostMapping("/projects/{id}/provision")
    public ApiResponse<Map<String, Object>> provision(@PathVariable long id,
                                                      @RequestBody(required = false) QuoteProvisionRequest body,
                                                      HttpServletRequest req) {
        if (quoteProvisionService == null) {
            return ApiResponse.error(50000, "provision 服务未就绪");
        }
        try {
            Map<String, Object> r = quoteProvisionService.provision(id, body, req);
            // usage：实际调用核心接口（即“报价项目创建/Provision”）
            Long adminUserId = (Long) req.getAttribute(AdminAuthFilter.ATTR_USER_ID);
            String adminPhone = (String) req.getAttribute(AdminAuthFilter.ATTR_PHONE);
            componentEventService.recordUsage(adminUserId, adminPhone, "hub_quote", "quote_provision");
            return ApiResponse.ok(r);
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(40000, e.getMessage());
        } catch (IllegalStateException e) {
            return ApiResponse.error(50000, e.getMessage());
        } catch (Exception e) {
            return ApiResponse.error(50000, e.getMessage() != null ? e.getMessage() : "provision 失败");
        }
    }

    @PutMapping("/projects/{id}")
    public ApiResponse<Void> updateProject(@PathVariable long id, @RequestBody QuoteProjectSaveDto body) {
        try {
            quoteService.updateProject(id, body);
            return ApiResponse.ok(null);
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(40000, e.getMessage());
        }
    }

    @DeleteMapping("/projects/{id}")
    public ApiResponse<Void> deleteProject(@PathVariable long id) {
        try {
            quoteService.deleteProject(id);
            return ApiResponse.ok(null);
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(40000, e.getMessage());
        }
    }

    /** 仅更新报价计算勾选与审核清单（自动保存，避免用户未点「保存项目」时丢失） */
    @PatchMapping("/projects/{id}/calc-prefs")
    public ApiResponse<Void> patchQuoteCalcPrefs(@PathVariable long id, @RequestBody(required = false) QuoteCalculateRequest body) {
        try {
            quoteService.saveQuoteCalcPrefs(id, body);
            return ApiResponse.ok(null);
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(40000, e.getMessage());
        } catch (IllegalStateException e) {
            return ApiResponse.error(50000, e.getMessage());
        }
    }

    /**
     * 商务调价：对「最近一次计算」结果做等比例缩放（或按目标总价反算系数）。
     */
    @PostMapping("/projects/{id}/price-adjust")
    public ApiResponse<Map<String, Object>> priceAdjust(@PathVariable long id,
                                                        @RequestBody(required = false) QuotePriceAdjustRequest body) {
        try {
            return ApiResponse.ok(quoteService.applyPriceAdjustment(id, body != null ? body : new QuotePriceAdjustRequest()));
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(40000, e.getMessage());
        } catch (IllegalStateException e) {
            return ApiResponse.error(50000, e.getMessage());
        }
    }

    @PostMapping("/projects/{id}/calculate")
    public ApiResponse<Map<String, Object>> calculate(@PathVariable long id,
                                                        @RequestBody(required = false) QuoteCalculateRequest calcReq,
                                                        HttpServletRequest req) {
        if (calcReq == null) calcReq = new QuoteCalculateRequest();
        try {
            Map<String, Object> r = quoteService.calculate(id, calcReq);
            // usage：实际调用核心接口（即“报价计算/Calculate”）
            Long adminUserId = (Long) req.getAttribute(AdminAuthFilter.ATTR_USER_ID);
            String adminPhone = (String) req.getAttribute(AdminAuthFilter.ATTR_PHONE);
            componentEventService.recordUsage(adminUserId, adminPhone, "hub_quote", "quote_calculate");
            return ApiResponse.ok(r);
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(40000, e.getMessage());
        } catch (IllegalStateException e) {
            return ApiResponse.error(50000, e.getMessage());
        } catch (Exception e) {
            return ApiResponse.error(50000, e.getMessage());
        }
    }

    @PostMapping("/projects/{id}/quote-doc")
    public ApiResponse<Map<String, Object>> quoteDoc(@PathVariable long id, @RequestBody(required = false) Map<String, Object> body) {
        Long resultId = null;
        if (body != null && body.get("quoteResultId") != null) {
            resultId = ((Number) body.get("quoteResultId")).longValue();
        }
        try {
            return ApiResponse.ok(quoteService.buildQuoteDocument(id, resultId));
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(40000, e.getMessage());
        }
    }

    /** 报价单 PDF（先落库 HTML 文档再转换） */
    @GetMapping("/projects/{id}/quote-doc.pdf")
    public ResponseEntity<byte[]> quoteDocPdf(@PathVariable long id, @RequestParam(required = false) Long quoteResultId) {
        try {
            byte[] pdf = quoteDocumentExportService.exportQuotePdf(id, quoteResultId);
            String filename = "quote-" + id + ".pdf";
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                            .filename(filename, StandardCharsets.UTF_8).build().toString())
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(pdf);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /** 报价单 Word（.docx） */
    @GetMapping("/projects/{id}/quote-doc.docx")
    public ResponseEntity<byte[]> quoteDocDocx(@PathVariable long id, @RequestParam(required = false) Long quoteResultId) {
        try {
            byte[] docx = quoteDocumentExportService.exportQuoteDocx(id, quoteResultId);
            String filename = "quote-" + id + ".docx";
            MediaType mt = MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                            .filename(filename, StandardCharsets.UTF_8).build().toString())
                    .contentType(mt)
                    .body(docx);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/baselines")
    public ApiResponse<List<Map<String, Object>>> baselines() {
        return ApiResponse.ok(baselineMapper.listAll(tid()));
    }

    @PostMapping("/baselines")
    public ApiResponse<Map<String, Object>> createBaseline(@RequestBody QuoteBaselineSaveDto body) {
        try {
            long id = quoteService.createBaseline(body);
            Map<String, Object> data = new HashMap<>();
            data.put("id", id);
            return ApiResponse.ok(data);
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(40000, e.getMessage());
        }
    }

    @PutMapping("/baselines/{id}")
    public ApiResponse<Void> updateBaseline(@PathVariable long id, @RequestBody QuoteBaselineSaveDto body) {
        try {
            quoteService.updateBaseline(id, body);
            return ApiResponse.ok(null);
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(40000, e.getMessage());
        }
    }

    @DeleteMapping("/baselines/{id}")
    public ApiResponse<Void> deleteBaseline(@PathVariable long id) {
        try {
            quoteService.deleteBaseline(id);
            return ApiResponse.ok(null);
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(40000, e.getMessage());
        }
    }

    @GetMapping("/risk-config")
    public ApiResponse<List<Map<String, Object>>> riskConfig() {
        return ApiResponse.ok(riskConfigMapper.listAll(tid()));
    }

    /** 批量更新风险系数（标签、百分比可为负、是否启用）；risk_key 不可改 */
    @PutMapping("/risk-config")
    public ApiResponse<Void> updateRiskConfig(@RequestBody QuoteRiskConfigBatchUpdate body) {
        try {
            quoteService.updateRiskConfigs(body);
            return ApiResponse.ok(null);
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(40000, e.getMessage());
        }
    }

    @GetMapping("/preset-items")
    public ApiResponse<List<Map<String, Object>>> listPresetItems(
            @RequestParam(value = "all", defaultValue = "false") boolean all) {
        return ApiResponse.ok(quoteService.listPresetItems(all));
    }

    @PostMapping("/preset-items")
    public ApiResponse<Map<String, Object>> createPresetItem(@RequestBody QuotePresetItemSaveDto body) {
        try {
            long id = quoteService.createPresetItem(body);
            Map<String, Object> data = new HashMap<>();
            data.put("id", id);
            return ApiResponse.ok(data);
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(40000, e.getMessage());
        }
    }

    @PutMapping("/preset-items/{id}")
    public ApiResponse<Void> updatePresetItem(@PathVariable long id, @RequestBody QuotePresetItemSaveDto body) {
        try {
            quoteService.updatePresetItem(id, body);
            return ApiResponse.ok(null);
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(40000, e.getMessage());
        }
    }

    @DeleteMapping("/preset-items/{id}")
    public ApiResponse<Void> deletePresetItem(@PathVariable long id) {
        try {
            quoteService.deletePresetItem(id);
            return ApiResponse.ok(null);
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(40000, e.getMessage());
        }
    }

    /** 报价页下拉默认仅启用项；管理页传 all=true */
    @GetMapping("/price-config")
    public ApiResponse<List<Map<String, Object>>> priceConfig(
            @RequestParam(value = "all", defaultValue = "false") boolean all) {
        return ApiResponse.ok(all ? priceConfigMapper.listAll(tid()) : priceConfigMapper.listEnabled(tid()));
    }

    @PostMapping("/price-config")
    public ApiResponse<Map<String, Object>> createPriceConfig(@RequestBody QuotePriceConfigSaveDto body) {
        try {
            long id = quoteService.createPriceConfig(body);
            Map<String, Object> data = new HashMap<>();
            data.put("id", id);
            return ApiResponse.ok(data);
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(40000, e.getMessage());
        }
    }

    @PutMapping("/price-config/{id}")
    public ApiResponse<Void> updatePriceConfig(@PathVariable long id, @RequestBody QuotePriceConfigSaveDto body) {
        try {
            quoteService.updatePriceConfig(id, body);
            return ApiResponse.ok(null);
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(40000, e.getMessage());
        }
    }

    @DeleteMapping("/price-config/{id}")
    public ApiResponse<Void> deletePriceConfig(@PathVariable long id) {
        try {
            quoteService.deletePriceConfig(id);
            return ApiResponse.ok(null);
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(40000, e.getMessage());
        }
    }

    @GetMapping("/projects/{id}/link-table-requirements")
    public ApiResponse<List<Map<String, Object>>> linkTableRequirements(@PathVariable long id) {
        try {
            return ApiResponse.ok(quoteCollabLinkService.listLinkTableRequirements(id));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ApiResponse.error(40000, e.getMessage());
        }
    }

    @PostMapping("/projects/{id}/link-table-requirements/apply")
    public ApiResponse<Map<String, Object>> applyLinkTableRequirements(@PathVariable long id,
                                                                       @RequestBody(required = false) QuoteApplyLinkTableRequest body) {
        try {
            int n = quoteCollabLinkService.applyLinkTableRequirements(id, body != null ? body.getRecordIds() : null);
            Map<String, Object> data = new HashMap<>();
            data.put("importedCount", n);
            return ApiResponse.ok(data);
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ApiResponse.error(40000, e.getMessage());
        }
    }

    /** 为关联仓库的协作项目创建「待开发功能清单」多维表（幂等） */
    @PostMapping("/projects/{id}/collab/import-feature-table")
    public ApiResponse<Map<String, Object>> importCollabFeatureTable(@PathVariable long id) {
        try {
            return ApiResponse.ok(quoteCollabLinkService.ensureCollabFeatureTable(id));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ApiResponse.error(40000, e.getMessage());
        }
    }

    /** 乙方（受托方）主体与收款信息模板（含法人/组织字段与嵌套 naturalPerson），全系统共用，供合同 AI 与商务引用 */
    @GetMapping("/party-b-profile")
    public ApiResponse<Map<String, Object>> getPartyBProfile() {
        return ApiResponse.ok(quoteService.getPartyBProfile());
    }

    @PutMapping("/party-b-profile")
    public ApiResponse<Void> savePartyBProfile(@RequestBody(required = false) Map<String, Object> body) {
        try {
            quoteService.savePartyBProfile(body != null ? body : Map.of());
            return ApiResponse.ok(null);
        } catch (JsonProcessingException e) {
            return ApiResponse.error(50000, "保存失败");
        }
    }

    /** 附件一：功能清单 HTML（与主合同引用） */
    @PostMapping("/projects/{id}/contract-attachments/function-list")
    public ApiResponse<Map<String, Object>> contractAttachmentFunctionList(@PathVariable long id) {
        try {
            return ApiResponse.ok(quoteService.buildContractAttachmentFunctionList(id));
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(40000, e.getMessage());
        }
    }

    /** 附件三：里程碑计划 HTML */
    @PostMapping("/projects/{id}/contract-attachments/milestones")
    public ApiResponse<Map<String, Object>> contractAttachmentMilestones(@PathVariable long id) {
        try {
            return ApiResponse.ok(quoteService.buildContractAttachmentMilestoneSchedule(id));
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(40000, e.getMessage());
        }
    }

    /** 附件二：验收标准（草案）HTML */
    @PostMapping("/projects/{id}/contract-attachments/acceptance")
    public ApiResponse<Map<String, Object>> contractAttachmentAcceptance(@PathVariable long id) {
        try {
            return ApiResponse.ok(quoteService.buildContractAttachmentAcceptanceStandards(id));
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(40000, e.getMessage());
        }
    }

    /** 附件一：功能清单 PDF */
    @GetMapping("/projects/{id}/contract-attachments/function-list.pdf")
    public ResponseEntity<byte[]> contractAttachmentFunctionListPdf(@PathVariable long id) {
        try {
            Map<String, Object> doc = quoteService.buildContractAttachmentFunctionList(id);
            String html = doc.get("html") != null ? doc.get("html").toString() : "";
            byte[] pdf = quoteDocumentExportService.htmlToPdf(html);
            String filename = "attachment-1-function-list-" + id + ".pdf";
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                            .filename(filename, StandardCharsets.UTF_8).build().toString())
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(pdf);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /** 附件一：功能清单 Word（.docx） */
    @GetMapping("/projects/{id}/contract-attachments/function-list.docx")
    public ResponseEntity<byte[]> contractAttachmentFunctionListDocx(@PathVariable long id) {
        try {
            Map<String, Object> doc = quoteService.buildContractAttachmentFunctionList(id);
            String html = doc.get("html") != null ? doc.get("html").toString() : "";
            byte[] docx = quoteDocumentExportService.htmlToDocx(html);
            String filename = "attachment-1-function-list-" + id + ".docx";
            MediaType mt = MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                            .filename(filename, StandardCharsets.UTF_8).build().toString())
                    .contentType(mt)
                    .body(docx);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /** 附件二：验收标准（草案） PDF */
    @GetMapping("/projects/{id}/contract-attachments/acceptance.pdf")
    public ResponseEntity<byte[]> contractAttachmentAcceptancePdf(@PathVariable long id) {
        try {
            Map<String, Object> doc = quoteService.buildContractAttachmentAcceptanceStandards(id);
            String html = doc.get("html") != null ? doc.get("html").toString() : "";
            byte[] pdf = quoteDocumentExportService.htmlToPdf(html);
            String filename = "attachment-2-acceptance-" + id + ".pdf";
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                            .filename(filename, StandardCharsets.UTF_8).build().toString())
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(pdf);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /** 附件二：验收标准（草案） Word（.docx） */
    @GetMapping("/projects/{id}/contract-attachments/acceptance.docx")
    public ResponseEntity<byte[]> contractAttachmentAcceptanceDocx(@PathVariable long id) {
        try {
            Map<String, Object> doc = quoteService.buildContractAttachmentAcceptanceStandards(id);
            String html = doc.get("html") != null ? doc.get("html").toString() : "";
            byte[] docx = quoteDocumentExportService.htmlToDocx(html);
            String filename = "attachment-2-acceptance-" + id + ".docx";
            MediaType mt = MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                            .filename(filename, StandardCharsets.UTF_8).build().toString())
                    .contentType(mt)
                    .body(docx);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /** 附件三：里程碑计划 PDF */
    @GetMapping("/projects/{id}/contract-attachments/milestones.pdf")
    public ResponseEntity<byte[]> contractAttachmentMilestonesPdf(@PathVariable long id) {
        try {
            Map<String, Object> doc = quoteService.buildContractAttachmentMilestoneSchedule(id);
            String html = doc.get("html") != null ? doc.get("html").toString() : "";
            byte[] pdf = quoteDocumentExportService.htmlToPdf(html);
            String filename = "attachment-3-milestones-" + id + ".pdf";
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                            .filename(filename, StandardCharsets.UTF_8).build().toString())
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(pdf);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /** 附件三：里程碑计划 Word（.docx） */
    @GetMapping("/projects/{id}/contract-attachments/milestones.docx")
    public ResponseEntity<byte[]> contractAttachmentMilestonesDocx(@PathVariable long id) {
        try {
            Map<String, Object> doc = quoteService.buildContractAttachmentMilestoneSchedule(id);
            String html = doc.get("html") != null ? doc.get("html").toString() : "";
            byte[] docx = quoteDocumentExportService.htmlToDocx(html);
            String filename = "attachment-3-milestones-" + id + ".docx";
            MediaType mt = MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                            .filename(filename, StandardCharsets.UTF_8).build().toString())
                    .contentType(mt)
                    .body(docx);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/results/{resultId}/contract/generate")
    public ApiResponse<Map<String, Object>> generateContract(@PathVariable long resultId, @RequestBody ContractGenerateRequest req) {
        if (req == null) req = new ContractGenerateRequest();
        try {
            return ApiResponse.ok(quoteService.generateContract(resultId, req));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ApiResponse.error(40000, e.getMessage());
        } catch (Exception e) {
            return ApiResponse.error(50000, e.getMessage());
        }
    }

    /**
     * 异步生成「合同正文」：入队立即返回 jobId，前端轮询避免 nginx 同步超时。
     */
    @PostMapping("/results/{resultId}/contract/generate/jobs")
    public ApiResponse<Map<String, Object>> enqueueContractGenerateJob(@PathVariable long resultId,
                                                                         @RequestBody(required = false) ContractGenerateRequest req) {
        if (req == null) req = new ContractGenerateRequest();
        try {
            long jobId = contractJobService.enqueue(resultId, req);
            Map<String, Object> out = new HashMap<>();
            out.put("jobId", jobId);
            return ApiResponse.ok(out);
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(40000, e.getMessage());
        } catch (IllegalStateException e) {
            return ApiResponse.error(50000, e.getMessage());
        } catch (Exception e) {
            return ApiResponse.error(50000, e.getMessage() != null ? e.getMessage() : "入队失败");
        }
    }

    @GetMapping("/results/{resultId}/contract/generate/jobs/{jobId}")
    public ApiResponse<QuoteAiContractGenerateJobStatus> getContractGenerateJobStatus(@PathVariable long resultId,
                                                                                         @PathVariable long jobId) {
        try {
            QuoteAiContractGenerateJobStatus s = contractJobService.getStatus(resultId, jobId);
            if (s == null) return ApiResponse.error(40400, "任务不存在");
            return ApiResponse.ok(s);
        } catch (Exception e) {
            return ApiResponse.error(50000, e.getMessage() != null ? e.getMessage() : "获取任务状态失败");
        }
    }

    @GetMapping("/results/{resultId}/contract")
    public ApiResponse<Map<String, Object>> getContract(@PathVariable long resultId) {
        Map<String, Object> d = quoteService.getContract(resultId);
        if (d == null) return ApiResponse.error(40400, "暂无合同草稿");
        return ApiResponse.ok(d);
    }

    @PutMapping("/results/{resultId}/contract")
    public ApiResponse<Void> saveContract(@PathVariable long resultId, @RequestBody ContractUpdateRequest req) {
        try {
            quoteService.updateContract(resultId, req);
            return ApiResponse.ok(null);
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(40000, e.getMessage());
        }
    }

    @PostMapping("/results/{resultId}/contract/export")
    public ApiResponse<Map<String, Object>> exportContract(@PathVariable long resultId) {
        try {
            return ApiResponse.ok(quoteService.exportContractHtml(resultId));
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(40000, e.getMessage());
        }
    }

    @GetMapping("/results/{resultId}/contract.pdf")
    public ResponseEntity<byte[]> contractPdf(@PathVariable long resultId) {
        try {
            byte[] pdf = quoteDocumentExportService.exportContractPdf(resultId);
            String filename = "contract-" + resultId + ".pdf";
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                            .filename(filename, StandardCharsets.UTF_8).build().toString())
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(pdf);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/results/{resultId}/contract.docx")
    public ResponseEntity<byte[]> contractDocx(@PathVariable long resultId) {
        try {
            byte[] docx = quoteDocumentExportService.exportContractDocx(resultId);
            String filename = "contract-" + resultId + ".docx";
            MediaType mt = MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                            .filename(filename, StandardCharsets.UTF_8).build().toString())
                    .contentType(mt)
                    .body(docx);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 返回 quoteResultId 维度已落库产出物状态（用于驱动 is-ready 绿色状态，刷新后仍可恢复）。
     */
    @GetMapping("/results/{resultId}/artifacts")
    public ApiResponse<Map<String, Object>> getArtifacts(@PathVariable long resultId) {
        return ApiResponse.ok(quoteService.getQuoteArtifactStatus(resultId));
    }
}
