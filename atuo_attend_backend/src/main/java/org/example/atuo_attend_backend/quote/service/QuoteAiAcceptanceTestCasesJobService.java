package org.example.atuo_attend_backend.quote.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.atuo_attend_backend.ai.client.DeepSeekClient;
import org.example.atuo_attend_backend.ai.domain.AiAnalysisConfig;
import org.example.atuo_attend_backend.ai.service.AiAnalysisConfigService;
import org.example.atuo_attend_backend.quote.domain.QuoteAiAcceptanceTestCasesJob;
import org.example.atuo_attend_backend.quote.dto.QuoteAiAcceptanceTestCasesJobStatus;
import org.example.atuo_attend_backend.quote.dto.QuoteAiAcceptanceTestCasesRequest;
import org.example.atuo_attend_backend.quote.mapper.QuoteAiAcceptanceTestCasesJobMapper;
import org.example.atuo_attend_backend.quote.service.QuoteService;
import org.example.atuo_attend_backend.tenant.context.TenantConstants;
import org.example.atuo_attend_backend.tenant.context.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 异步生成「验收测试用例/测试清单」：入队立即返回 jobId，前端轮询避免 nginx 同步超时。
 */
@Service
public class QuoteAiAcceptanceTestCasesJobService {

    private static final Logger log = LoggerFactory.getLogger(QuoteAiAcceptanceTestCasesJobService.class);
    private static final int REQUEST_SNAPSHOT_MAX_CHARS = 8000;

    private final QuoteAiAcceptanceTestCasesJobMapper jobMapper;
    private final QuoteService quoteService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public QuoteAiAcceptanceTestCasesJobService(QuoteAiAcceptanceTestCasesJobMapper jobMapper,
                                                 QuoteService quoteService) {
        this.jobMapper = jobMapper;
        this.quoteService = quoteService;
    }

    private static long tid() {
        return TenantContext.getTenantIdOrDefault(TenantConstants.DEFAULT_TENANT_ID);
    }

    public long enqueue(long quoteProjectId, QuoteAiAcceptanceTestCasesRequest req) {
        long tenantId = tid();
        if (req == null) req = new QuoteAiAcceptanceTestCasesRequest();

        String snapshotJson;
        try {
            snapshotJson = objectMapper.writeValueAsString(req);
        } catch (Exception e) {
            snapshotJson = null;
        }
        if (snapshotJson != null && snapshotJson.length() > REQUEST_SNAPSHOT_MAX_CHARS) {
            snapshotJson = snapshotJson.substring(0, REQUEST_SNAPSHOT_MAX_CHARS) + "…";
        }

        QuoteAiAcceptanceTestCasesJob row = new QuoteAiAcceptanceTestCasesJob();
        row.setTenantId(tenantId);
        row.setQuoteProjectId(quoteProjectId);
        row.setStatus("pending");
        row.setRequestSnapshot(snapshotJson);
        jobMapper.insert(row);
        Long jobId = row.getId();
        if (jobId == null) throw new IllegalStateException("创建验收用例生成任务失败");

        // 后台执行：避免占用请求线程导致 504
        Long finalJobId = jobId;
        QuoteAiAcceptanceTestCasesRequest finalReq = req;
        Long finalTenantId = tenantId;
        Long finalProjectId = quoteProjectId;
        java.util.concurrent.CompletableFuture.runAsync(() -> runJobSafe(finalTenantId, finalJobId, finalProjectId, finalReq));

        return jobId;
    }

    public QuoteAiAcceptanceTestCasesJobStatus getStatus(long quoteProjectId, long jobId) {
        long tenantId = tid();
        QuoteAiAcceptanceTestCasesJob j = jobMapper.findById(jobId, tenantId, quoteProjectId);
        if (j == null) return null;

        QuoteAiAcceptanceTestCasesJobStatus s = new QuoteAiAcceptanceTestCasesJobStatus();
        s.setJobId(j.getId());
        s.setStatus(j.getStatus());
        s.setErrorMessage(j.getErrorMessage());

        if ("success".equals(j.getStatus()) && j.getResultJson() != null && !j.getResultJson().isBlank()) {
            try {
                Map<String, Object> out = objectMapper.readValue(j.getResultJson(), new TypeReference<Map<String, Object>>() {});
                Object atc = out.get("acceptanceTestCases");
                if (atc instanceof List<?> list) {
                    s.setAcceptanceTestCases((List<Map<String, Object>>) list);
                }
                Object usage = out.get("usage");
                if (usage instanceof Map<?, ?> m) {
                    //noinspection unchecked
                    s.setUsage((Map<String, Object>) m);
                }
            } catch (Exception e) {
                log.warn("parse acceptance job result json failed: {}", e.getMessage());
            }
        }
        return s;
    }

    private void runJobSafe(long tenantId, long jobId, long projectId, QuoteAiAcceptanceTestCasesRequest req) {
        try {
            TenantContext.runWithTenantId(tenantId, () -> runJob(jobId, projectId, req));
        } catch (Throwable t) {
            log.error("acceptance ai job {} crashed", jobId, t);
            try {
                jobMapper.updateFailed(jobId, tenantId, projectId, t.getMessage() != null ? t.getMessage() : "内部错误");
            } catch (Exception e) {
                log.warn("update acceptance job failed status error: {}", e.getMessage());
            }
        }
    }

    private void runJob(long jobId, long projectId, QuoteAiAcceptanceTestCasesRequest req) {
        long tenantId = tid();
        jobMapper.updateStatus(jobId, tenantId, projectId, "running");
        try {
            Map<String, Object> out = quoteService.generateAcceptanceTestCasesWithAi(req);
            String resultJson = objectMapper.writeValueAsString(out);
            jobMapper.updateSuccess(jobId, tenantId, projectId, resultJson);
        } catch (Exception e) {
            String msg = e.getMessage() != null ? e.getMessage() : "生成失败";
            jobMapper.updateFailed(jobId, tenantId, projectId, msg);
        }
    }
}

