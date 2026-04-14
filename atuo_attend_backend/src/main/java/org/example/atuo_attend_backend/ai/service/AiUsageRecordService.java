package org.example.atuo_attend_backend.ai.service;

import org.example.atuo_attend_backend.ai.mapper.AiTokenUsageMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
public class AiUsageRecordService {

    private static final Logger log = LoggerFactory.getLogger(AiUsageRecordService.class);

    private final AiTokenUsageMapper tokenUsageMapper;

    public AiUsageRecordService(AiTokenUsageMapper tokenUsageMapper) {
        this.tokenUsageMapper = tokenUsageMapper;
    }

    public void recordPersonal(long tenantId, String provider, String model, int inTok, int outTok,
                               String repoFullName, String commitSha) {
        record(tenantId, provider, model, inTok, outTok, repoFullName, commitSha, "personal", null);
    }

    public void recordOfficial(long tenantId, String provider, String model, int inTok, int outTok,
                               String repoFullName, String commitSha, BigDecimal costYuan) {
        record(tenantId, provider, model, inTok, outTok, repoFullName, commitSha, "official", costYuan);
    }

    private void record(long tenantId, String provider, String model, int inTok, int outTok,
                        String repoFullName, String commitSha, String usageSource, BigDecimal costYuan) {
        if (tokenUsageMapper == null) return;
        try {
            int total = Math.max(0, inTok) + Math.max(0, outTok);
            tokenUsageMapper.insert(tenantId, LocalDateTime.now(), provider, model,
                    inTok, outTok, total, repoFullName, commitSha, usageSource, costYuan);
        } catch (Exception e) {
            log.warn("record ai usage failed: {}", e.getMessage());
        }
    }
}
