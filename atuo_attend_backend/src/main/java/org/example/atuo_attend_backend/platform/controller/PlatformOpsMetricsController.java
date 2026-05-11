package org.example.atuo_attend_backend.platform.controller;

import org.example.atuo_attend_backend.common.ApiResponse;
import org.example.atuo_attend_backend.platform.mapper.PlatformComponentEventMapper;
import org.example.atuo_attend_backend.platform.mapper.PlatformOpsMetricsMapper;
import org.example.atuo_attend_backend.tenant.mapper.SubscriptionOrderMapper;
import org.example.atuo_attend_backend.tenant.mapper.TenantMapper;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.text.SimpleDateFormat;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.*;

@RestController
@RequestMapping("/api/platform/ops/metrics")
public class PlatformOpsMetricsController {

    private final PlatformOpsMetricsMapper metricsMapper;
    private final PlatformComponentEventMapper componentEventMapper;
    private final SubscriptionOrderMapper subscriptionOrderMapper;
    private final TenantMapper tenantMapper;

    public PlatformOpsMetricsController(PlatformOpsMetricsMapper metricsMapper,
                                        PlatformComponentEventMapper componentEventMapper,
                                        SubscriptionOrderMapper subscriptionOrderMapper,
                                        TenantMapper tenantMapper) {
        this.metricsMapper = metricsMapper;
        this.componentEventMapper = componentEventMapper;
        this.subscriptionOrderMapper = subscriptionOrderMapper;
        this.tenantMapper = tenantMapper;
    }

    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");

    private OffsetDateTime todayStart() {
        return OffsetDateTime.now(ZONE).toLocalDate().atStartOfDay(ZONE).toOffsetDateTime();
    }

    @GetMapping("/overview")
    public ApiResponse<Map<String, Object>> overview() {
        OffsetDateTime startToday = todayStart();
        OffsetDateTime startTomorrow = startToday.plusDays(1);

        long dauToday = metricsMapper.countDistinctAuthorsBetween(startToday, startTomorrow);

        // MAU：按当前自然月统计（Asia/Shanghai）
        YearMonth ym = YearMonth.from(startToday.toLocalDate());
        OffsetDateTime startMonth = ym.atDay(1).atStartOfDay(ZONE).toOffsetDateTime();
        OffsetDateTime endMonth = ym.plusMonths(1).atDay(1).atStartOfDay(ZONE).toOffsetDateTime();
        long mauMonth = metricsMapper.countDistinctAuthorsBetween(startMonth, endMonth);

        long totalUsers = metricsMapper.countTotalUsers();
        long totalTenants = metricsMapper.countTenants();

        long configuredApiKeyTenants = metricsMapper.countAiApiKeyConfiguredTenants();
        long configuredGithubTokenTenants = metricsMapper.countGithubTokenConfiguredTenants();

        double apiKeyRatio = totalTenants > 0 ? (configuredApiKeyTenants * 100.0d / totalTenants) : 0d;
        double githubTokenRatio = totalTenants > 0 ? (configuredGithubTokenTenants * 100.0d / totalTenants) : 0d;

        long mockRevenueCents30d = subscriptionOrderMapper.sumAmountCentsLast30Days();
        long activePaidSubscriptions = tenantMapper.countActivePaidSubscriptions();
        long suspendedTenants = tenantMapper.countSuspendedTenants();

        Map<String, Object> data = new HashMap<>();
        data.put("dauToday", dauToday);
        data.put("mauMonth", mauMonth);
        data.put("totalUsers", totalUsers);
        data.put("totalTenants", totalTenants);
        data.put("apiKeyConfiguredTenants", configuredApiKeyTenants);
        data.put("apiKeyConfiguredRatioPercent", apiKeyRatio);
        data.put("githubTokenConfiguredTenants", configuredGithubTokenTenants);
        data.put("githubTokenConfiguredRatioPercent", githubTokenRatio);
        data.put("mockRevenueCents30d", mockRevenueCents30d);
        data.put("mrrApproxCents", mockRevenueCents30d);
        data.put("activePaidSubscriptions", activePaidSubscriptions);
        data.put("suspendedTenants", suspendedTenants);
        return ApiResponse.ok(data);
    }

    @GetMapping("/dau-trend")
    public ApiResponse<List<Map<String, Object>>> dauTrend(@RequestParam(value = "days", defaultValue = "30") int days) {
        days = Math.min(Math.max(days, 1), 90);
        OffsetDateTime startToday = todayStart();
        OffsetDateTime start = startToday.minusDays(days - 1L);
        OffsetDateTime end = startToday.plusDays(1L);

        List<PlatformOpsMetricsMapper.DauTrendRow> list = metricsMapper.listDauTrend(start, end);
        SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd");
        List<Map<String, Object>> data = new ArrayList<>();
        if (list != null) {
            for (PlatformOpsMetricsMapper.DauTrendRow r : list) {
                Map<String, Object> m = new HashMap<>();
                m.put("date", r.getDay() != null ? fmt.format(r.getDay()) : null);
                m.put("count", r.getCount());
                data.add(m);
            }
        }
        return ApiResponse.ok(data);
    }

    @GetMapping("/active-authors")
    public ApiResponse<List<Map<String, Object>>> activeAuthors(
            @RequestParam(value = "limit", defaultValue = "20") int limit) {
        limit = Math.min(Math.max(limit, 5), 50);

        OffsetDateTime startToday = todayStart();
        OffsetDateTime end = startToday.plusDays(1L);

        List<PlatformOpsMetricsMapper.ActiveAuthorRow> list = metricsMapper.listActiveAuthors(startToday, end, limit);

        List<Map<String, Object>> data = new ArrayList<>();
        if (list != null) {
            for (PlatformOpsMetricsMapper.ActiveAuthorRow r : list) {
                Map<String, Object> m = new HashMap<>();
                m.put("authorEmail", r.getAuthorEmail());
                m.put("authorName", r.getAuthorName());
                m.put("commitCount", r.getCommitCount());
                m.put("lastCommittedAt", r.getLastCommittedAt() != null ? r.getLastCommittedAt().toString() : null);
                data.add(m);
            }
        }
        return ApiResponse.ok(data);
    }

    @GetMapping("/component-usage")
    public ApiResponse<Map<String, Object>> componentUsage(
            @RequestParam(value = "days", defaultValue = "30") int days) {
        days = Math.min(Math.max(days, 1), 180);
        OffsetDateTime startToday = todayStart();
        OffsetDateTime since = startToday.minusDays(days - 1L);

        List<PlatformComponentEventMapper.ComponentAggRow> compAgg = componentEventMapper.listComponentAgg(since);
        List<PlatformComponentEventMapper.ComponentCoreApiAggRow> coreAgg = componentEventMapper.listCoreApiAgg(since);

        Map<String, Object> out = new HashMap<>();
        out.put("since", since.toString());

        List<Map<String, Object>> components = new ArrayList<>();
        Map<String, Map<String, Object>> byComponent = new LinkedHashMap<>();

        if (compAgg != null) {
            for (PlatformComponentEventMapper.ComponentAggRow r : compAgg) {
                Map<String, Object> m = new HashMap<>();
                m.put("componentKey", r.getComponentKey());
                m.put("clickCount", r.getClickCount());
                m.put("usageCount", r.getUsageCount());
                m.put("coreApis", new ArrayList<>());
                byComponent.put(r.getComponentKey(), m);
            }
        }

        if (coreAgg != null) {
            for (PlatformComponentEventMapper.ComponentCoreApiAggRow r : coreAgg) {
                Map<String, Object> comp = byComponent.get(r.getComponentKey());
                if (comp == null) continue;
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> coreApis = (List<Map<String, Object>>) comp.get("coreApis");
                Map<String, Object> c = new HashMap<>();
                c.put("coreApiKey", r.getCoreApiKey());
                c.put("clickCount", r.getClickCount());
                c.put("usageCount", r.getUsageCount());
                coreApis.add(c);
            }
        }

        components.addAll(byComponent.values());
        out.put("components", components);
        return ApiResponse.ok(out);
    }

    @GetMapping("/heat-rank")
    public ApiResponse<Map<String, Object>> heatRank(
            @RequestParam(value = "days", defaultValue = "30") int days,
            @RequestParam(value = "limit", defaultValue = "50") int limit) {
        days = Math.min(Math.max(days, 1), 180);
        limit = Math.min(Math.max(limit, 5), 200);
        OffsetDateTime startToday = todayStart();
        OffsetDateTime since = startToday.minusDays(days - 1L);

        List<PlatformComponentEventMapper.HeatRankRow> rows = componentEventMapper.listHeatRank(since, limit);

        List<Map<String, Object>> items = new ArrayList<>();
        if (rows != null) {
            int rank = 0;
            for (PlatformComponentEventMapper.HeatRankRow r : rows) {
                rank++;
                Map<String, Object> m = new HashMap<>();
                m.put("rank", rank);
                m.put("coreApiKey", r.getCoreApiKey());
                m.put("componentKey", r.getComponentKey());
                m.put("clickCount", r.getClickCount());
                m.put("usageCount", r.getUsageCount());
                m.put("tenantCount", r.getTenantCount());
                m.put("userCount", r.getUserCount());
                items.add(m);
            }
        }

        Map<String, Object> out = new HashMap<>();
        out.put("since", since.toString());
        out.put("total", items.size());
        out.put("items", items);
        return ApiResponse.ok(out);
    }
}

