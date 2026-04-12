package org.example.atuo_attend_backend.nexus.task;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.atuo_attend_backend.nexus.domain.NexusAlertRule;
import org.example.atuo_attend_backend.nexus.domain.NexusCloudAccount;
import org.example.atuo_attend_backend.nexus.domain.NexusCloudInstance;
import org.example.atuo_attend_backend.nexus.mapper.*;
import org.example.atuo_attend_backend.nexus.service.NexusAlertNotifyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class NexusAlertEvaluationTask {

    private static final Logger log = LoggerFactory.getLogger(NexusAlertEvaluationTask.class);

    private final NexusAlertRuleMapper alertRuleMapper;
    private final NexusCloudAccountMapper accountMapper;
    private final NexusInstanceMapper instanceMapper;
    private final NexusCpuMetricMapper cpuMetricMapper;
    private final NexusMemoryMetricMapper memoryMetricMapper;
    private final NexusAlertNotifyService notifyService;
    /** 与其它类一致使用本地实例：Spring Boot 4 下未必注册可注入的 ObjectMapper Bean */
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final AtomicBoolean running = new AtomicBoolean(false);

    @Value("${nexus.alert.evaluation-enabled:true}")
    private boolean evaluationEnabled;

    public NexusAlertEvaluationTask(
            NexusAlertRuleMapper alertRuleMapper,
            NexusCloudAccountMapper accountMapper,
            NexusInstanceMapper instanceMapper,
            NexusCpuMetricMapper cpuMetricMapper,
            NexusMemoryMetricMapper memoryMetricMapper,
            NexusAlertNotifyService notifyService
    ) {
        this.alertRuleMapper = alertRuleMapper;
        this.accountMapper = accountMapper;
        this.instanceMapper = instanceMapper;
        this.cpuMetricMapper = cpuMetricMapper;
        this.memoryMetricMapper = memoryMetricMapper;
        this.notifyService = notifyService;
    }

    @Scheduled(fixedDelayString = "${nexus.alert.evaluation-delay-ms:120000}")
    public void tick() {
        if (!evaluationEnabled) return;
        if (!running.compareAndSet(false, true)) return;
        try {
            List<NexusAlertRule> rules = alertRuleMapper.listAllEnabled();
            if (rules == null || rules.isEmpty()) return;
            LocalDateTime now = LocalDateTime.now();
            for (NexusAlertRule rule : rules) {
                try {
                    evaluateOne(rule, now);
                } catch (Exception e) {
                    log.warn("[nexus] alert rule {} eval error: {}", rule.getId(), e.getMessage());
                }
            }
        } finally {
            running.set(false);
        }
    }

    private void evaluateOne(NexusAlertRule rule, LocalDateTime now) throws Exception {
        if (rule.getLastTriggeredAt() != null && rule.getSilenceSeconds() != null && rule.getSilenceSeconds() > 0) {
            long sec = Duration.between(rule.getLastTriggeredAt(), now).getSeconds();
            if (sec < rule.getSilenceSeconds()) {
                return;
            }
        }
        int dur = rule.getDurationMinutes() != null && rule.getDurationMinutes() > 0 ? rule.getDurationMinutes() : 5;
        LocalDateTime since = now.minusMinutes(dur);

        List<InstanceTarget> targets = resolveTargets(rule);
        if (targets.isEmpty()) {
            return;
        }

        List<Map<String, Object>> breached = new ArrayList<>();
        for (InstanceTarget t : targets) {
            if (sustainedBreach(rule, t.tenantId, t.accountId, t.instanceId, since)) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("accountId", t.accountId);
                row.put("instanceId", t.instanceId);
                breached.add(row);
            }
        }
        if (breached.isEmpty()) {
            return;
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("source", "nexus");
        payload.put("ruleId", rule.getId());
        payload.put("ruleName", rule.getName());
        payload.put("metricType", rule.getMetricType());
        payload.put("op", rule.getOp());
        payload.put("threshold", rule.getThreshold());
        payload.put("durationMinutes", dur);
        payload.put("breachedInstances", breached);
        payload.put("time", now.toString());
        String json = objectMapper.writeValueAsString(payload);
        notifyService.notifyAlert(rule, json);
        alertRuleMapper.updateLastTriggered(rule.getTenantId(), rule.getId(), now);
    }

    private boolean sustainedBreach(NexusAlertRule rule, long tenantId, long accountId, String instanceId, LocalDateTime since) {
        String mt = rule.getMetricType() == null ? "" : rule.getMetricType().trim().toLowerCase();
        List<NexusCpuMetricMapper.MetricRow> cpuRows;
        List<NexusMemoryMetricMapper.MetricRow> memRows;
        if ("memory".equals(mt)) {
            memRows = memoryMetricMapper.listMemoryPointsSince(tenantId, accountId, instanceId, since);
            if (memRows == null || memRows.isEmpty()) return false;
            for (NexusMemoryMetricMapper.MetricRow r : memRows) {
                if (r.getValue() == null || !match(r.getValue(), rule.getThreshold(), rule.getOp())) {
                    return false;
                }
            }
            return true;
        }
        cpuRows = cpuMetricMapper.listCpuPointsSince(tenantId, accountId, instanceId, since);
        if (cpuRows == null || cpuRows.isEmpty()) return false;
        for (NexusCpuMetricMapper.MetricRow r : cpuRows) {
            if (r.getValue() == null || !match(r.getValue(), rule.getThreshold(), rule.getOp())) {
                return false;
            }
        }
        return true;
    }

    private static boolean match(double value, Double threshold, String op) {
        if (threshold == null) return false;
        String o = op == null ? "gt" : op.trim().toLowerCase();
        return switch (o) {
            case "gt" -> value > threshold;
            case "gte" -> value >= threshold;
            case "lt" -> value < threshold;
            case "lte" -> value <= threshold;
            default -> false;
        };
    }

    private List<InstanceTarget> resolveTargets(NexusAlertRule rule) {
        List<InstanceTarget> out = new ArrayList<>();
        long tenantId = rule.getTenantId();
        if (rule.getAccountId() != null) {
            if (rule.getInstanceId() != null && !rule.getInstanceId().isBlank()) {
                out.add(new InstanceTarget(tenantId, rule.getAccountId(), rule.getInstanceId().trim()));
            } else {
                List<NexusCloudInstance> list = instanceMapper.listByAccount(tenantId, rule.getAccountId());
                for (NexusCloudInstance i : list) {
                    out.add(new InstanceTarget(tenantId, rule.getAccountId(), i.getInstanceId()));
                }
            }
            return out;
        }
        List<NexusCloudAccount> accs = accountMapper.listByTenant(tenantId);
        for (NexusCloudAccount a : accs) {
            if (!"aliyun".equalsIgnoreCase(a.getProvider())) continue;
            if (rule.getInstanceId() != null && !rule.getInstanceId().isBlank()) {
                List<NexusCloudInstance> list = instanceMapper.listByAccount(tenantId, a.getId());
                String want = rule.getInstanceId().trim();
                for (NexusCloudInstance i : list) {
                    if (want.equals(i.getInstanceId())) {
                        out.add(new InstanceTarget(tenantId, a.getId(), i.getInstanceId()));
                    }
                }
            } else {
                List<NexusCloudInstance> list = instanceMapper.listByAccount(tenantId, a.getId());
                for (NexusCloudInstance i : list) {
                    out.add(new InstanceTarget(tenantId, a.getId(), i.getInstanceId()));
                }
            }
        }
        return out;
    }

    private record InstanceTarget(long tenantId, long accountId, String instanceId) {
    }
}
