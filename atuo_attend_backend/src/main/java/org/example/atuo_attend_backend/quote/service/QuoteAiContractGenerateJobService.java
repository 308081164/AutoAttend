package org.example.atuo_attend_backend.quote.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.atuo_attend_backend.quote.domain.QuoteAiContractGenerateJob;
import org.example.atuo_attend_backend.quote.dto.QuoteAiContractGenerateJobStatus;
import org.example.atuo_attend_backend.quote.dto.ContractGenerateRequest;
import org.example.atuo_attend_backend.quote.mapper.QuoteAiContractGenerateJobMapper;
import org.example.atuo_attend_backend.tenant.context.TenantConstants;
import org.example.atuo_attend_backend.tenant.context.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * 异步生成「合同正文」：入队立即返回 jobId，前端轮询避免 nginx 同步超时。
 */
@Service
public class QuoteAiContractGenerateJobService {

    private static final Logger log = LoggerFactory.getLogger(QuoteAiContractGenerateJobService.class);
    private static final int REQUEST_SNAPSHOT_MAX_CHARS = 8000;

    private final QuoteAiContractGenerateJobMapper jobMapper;
    private final QuoteService quoteService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public QuoteAiContractGenerateJobService(QuoteAiContractGenerateJobMapper jobMapper, QuoteService quoteService) {
        this.jobMapper = jobMapper;
        this.quoteService = quoteService;
    }

    private static long tid() {
        return TenantContext.getTenantIdOrDefault(TenantConstants.DEFAULT_TENANT_ID);
    }

    public long enqueue(long quoteResultId, ContractGenerateRequest req) {
        long tenantId = tid();
        if (req == null) req = new ContractGenerateRequest();

        String snapshotJson;
        try {
            snapshotJson = objectMapper.writeValueAsString(req);
        } catch (Exception e) {
            snapshotJson = null;
        }
        if (snapshotJson != null && snapshotJson.length() > REQUEST_SNAPSHOT_MAX_CHARS) {
            snapshotJson = snapshotJson.substring(0, REQUEST_SNAPSHOT_MAX_CHARS) + "…";
        }

        QuoteAiContractGenerateJob row = new QuoteAiContractGenerateJob();
        row.setTenantId(tenantId);
        row.setQuoteResultId(quoteResultId);
        row.setStatus("pending");
        row.setRequestSnapshot(snapshotJson);
        jobMapper.insert(row);
        Long jobId = row.getId();
        if (jobId == null) throw new IllegalStateException("创建合同生成任务失败");

        long finalTenantId = tenantId;
        long finalJobId = jobId;
        long finalResultId = quoteResultId;
        ContractGenerateRequest finalReq = req;
        java.util.concurrent.CompletableFuture.runAsync(() -> runJobSafe(finalTenantId, finalJobId, finalResultId, finalReq));

        return jobId;
    }

    public QuoteAiContractGenerateJobStatus getStatus(long quoteResultId, long jobId) {
        long tenantId = tid();
        QuoteAiContractGenerateJob j = jobMapper.findById(jobId, tenantId, quoteResultId);
        if (j == null) return null;

        QuoteAiContractGenerateJobStatus s = new QuoteAiContractGenerateJobStatus();
        s.setJobId(j.getId());
        s.setStatus(j.getStatus());
        s.setErrorMessage(j.getErrorMessage());
        if ("success".equals(j.getStatus())) {
            s.setEditedContent(j.getEditedContent());
        }
        return s;
    }

    private void runJobSafe(long tenantId, long jobId, long resultId, ContractGenerateRequest req) {
        try {
            TenantContext.runWithTenantId(tenantId, () -> runJob(jobId, resultId, req));
        } catch (Throwable t) {
            log.error("contract ai job {} crashed", jobId, t);
            try {
                jobMapper.updateFailed(jobId, tenantId, resultId, t.getMessage() != null ? t.getMessage() : "内部错误");
            } catch (Exception e) {
                log.warn("update contract job failed status error: {}", e.getMessage());
            }
        }
    }

    private void runJob(long jobId, long quoteResultId, ContractGenerateRequest req) {
        long tenantId = tid();
        jobMapper.updateStatus(jobId, tenantId, quoteResultId, "running");
        try {
            // 会落库 biz_quote_contract_draft（同步的副作用），但调用方用 jobId 轮询避免网关超时。
            java.util.Map<String, Object> out = quoteService.generateContract(quoteResultId, req);
            String editedContent = out != null ? (String) out.get("editedContent") : null;
            jobMapper.updateSuccess(jobId, tenantId, quoteResultId, editedContent);
        } catch (Exception e) {
            String msg = e.getMessage() != null ? e.getMessage() : "生成失败";
            jobMapper.updateFailed(jobId, tenantId, quoteResultId, msg);
        }
    }
}

