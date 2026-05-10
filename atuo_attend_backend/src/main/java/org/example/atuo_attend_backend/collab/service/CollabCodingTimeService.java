package org.example.atuo_attend_backend.collab.service;

import org.example.atuo_attend_backend.collab.auth.CollabAccessContext;
import org.example.atuo_attend_backend.collab.domain.BizProject;
import org.example.atuo_attend_backend.collab.dto.CollabCodingHeartbeatBatchRequest;
import org.example.atuo_attend_backend.collab.dto.CollabCodingHeartbeatItemRequest;
import org.example.atuo_attend_backend.collab.mapper.BizCodingTimeHeartbeatMapper;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

@Service
public class CollabCodingTimeService {

    private static final int MAX_BATCH = 100;
    private static final int MIN_DURATION = 1;
    private static final int MAX_DURATION = 7200;

    private final CollabProjectService projectService;
    private final BizCodingTimeHeartbeatMapper heartbeatMapper;

    public CollabCodingTimeService(CollabProjectService projectService,
                                   BizCodingTimeHeartbeatMapper heartbeatMapper) {
        this.projectService = projectService;
        this.heartbeatMapper = heartbeatMapper;
    }

    public int ingestHeartbeats(CollabAccessContext ctx, CollabCodingHeartbeatBatchRequest body) {
        if (body == null || body.getItems() == null || body.getItems().isEmpty()) {
            return 0;
        }
        List<CollabCodingHeartbeatItemRequest> items = body.getItems();
        if (items.size() > MAX_BATCH) {
            throw new IllegalArgumentException("单次最多提交 " + MAX_BATCH + " 条心跳");
        }
        long userId = ctx.getEffectiveUserId();
        int n = 0;
        for (CollabCodingHeartbeatItemRequest it : items) {
            if (it.getClientEventId() == null || it.getClientEventId().isBlank()) {
                continue;
            }
            if (it.getClientEventId().length() > 80) {
                throw new IllegalArgumentException("clientEventId 过长");
            }
            if (!projectService.canAccessProject(ctx, it.getProjectId())) {
                throw new IllegalArgumentException("无权限向该项目上报编码统计");
            }
            BizProject p = projectService.getById(it.getProjectId());
            if (p == null || p.getTenantId() == null) {
                throw new IllegalArgumentException("项目不存在");
            }
            int d = it.getDurationSeconds();
            if (d < MIN_DURATION) {
                continue;
            }
            d = Math.min(d, MAX_DURATION);
            Instant at = it.getHeartbeatAt() != null ? it.getHeartbeatAt() : Instant.now();
            String lang = it.getLanguage();
            if (lang != null && lang.length() > 64) {
                lang = lang.substring(0, 64);
            }
            String fp = it.getFileFingerprint();
            if (fp != null && fp.length() > 128) {
                fp = fp.substring(0, 128);
            }
            heartbeatMapper.insertIgnoreDuplicate(
                    p.getTenantId(),
                    it.getProjectId(),
                    userId,
                    it.getClientEventId().trim(),
                    Timestamp.from(at),
                    d,
                    lang,
                    fp
            );
            n++;
        }
        return n;
    }

    public long sumSecondsForUserProjectDay(CollabAccessContext ctx, long projectId, LocalDate day) {
        if (!projectService.canAccessProject(ctx, projectId)) {
            throw new IllegalArgumentException("无权限查询该项目编码统计");
        }
        Instant start = day.atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant end = day.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);
        return heartbeatMapper.sumDurationSeconds(
                projectId,
                ctx.getEffectiveUserId(),
                Timestamp.from(start),
                Timestamp.from(end)
        );
    }
}
