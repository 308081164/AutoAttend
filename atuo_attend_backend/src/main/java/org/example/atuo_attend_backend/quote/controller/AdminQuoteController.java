package org.example.atuo_attend_backend.quote.controller;

import org.example.atuo_attend_backend.common.ApiResponse;
import org.example.atuo_attend_backend.quote.dto.*;
import org.example.atuo_attend_backend.quote.mapper.QuoteBaselineMapper;
import org.example.atuo_attend_backend.quote.mapper.QuotePriceConfigMapper;
import org.example.atuo_attend_backend.quote.mapper.QuoteRiskConfigMapper;
import org.example.atuo_attend_backend.quote.service.QuoteDocumentExportService;
import org.example.atuo_attend_backend.quote.service.QuoteService;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/quote")
public class AdminQuoteController {

    private final QuoteService quoteService;
    private final QuoteDocumentExportService quoteDocumentExportService;
    private final QuoteBaselineMapper baselineMapper;
    private final QuoteRiskConfigMapper riskConfigMapper;
    private final QuotePriceConfigMapper priceConfigMapper;

    public AdminQuoteController(QuoteService quoteService, QuoteDocumentExportService quoteDocumentExportService,
                                QuoteBaselineMapper baselineMapper,
                                QuoteRiskConfigMapper riskConfigMapper, QuotePriceConfigMapper priceConfigMapper) {
        this.quoteService = quoteService;
        this.quoteDocumentExportService = quoteDocumentExportService;
        this.baselineMapper = baselineMapper;
        this.riskConfigMapper = riskConfigMapper;
        this.priceConfigMapper = priceConfigMapper;
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

    @PutMapping("/projects/{id}")
    public ApiResponse<Void> updateProject(@PathVariable long id, @RequestBody QuoteProjectSaveDto body) {
        try {
            quoteService.updateProject(id, body);
            return ApiResponse.ok(null);
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(40000, e.getMessage());
        }
    }

    @PostMapping("/projects/{id}/calculate")
    public ApiResponse<Map<String, Object>> calculate(@PathVariable long id, @RequestBody(required = false) QuoteCalculateRequest req) {
        if (req == null) req = new QuoteCalculateRequest();
        try {
            return ApiResponse.ok(quoteService.calculate(id, req));
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
        return ApiResponse.ok(baselineMapper.listAll());
    }

    @GetMapping("/risk-config")
    public ApiResponse<List<Map<String, Object>>> riskConfig() {
        return ApiResponse.ok(riskConfigMapper.listAll());
    }

    @GetMapping("/price-config")
    public ApiResponse<List<Map<String, Object>>> priceConfig() {
        return ApiResponse.ok(priceConfigMapper.listEnabled());
    }

    @GetMapping("/projects/{id}/link-table-requirements")
    public ApiResponse<List<Map<String, Object>>> linkTableRequirements(@PathVariable long id) {
        // 可选：后续与多维表需求记录对接
        return ApiResponse.ok(List.of());
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
}
