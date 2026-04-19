package org.example.atuo_attend_backend.nexus.adapter.aliyun;

import com.aliyun.cms20190101.Client;
import com.aliyun.cms20190101.models.DescribeMetricListRequest;
import com.aliyun.cms20190101.models.DescribeMetricListResponse;
import com.aliyun.teaopenapi.models.Config;
import com.aliyun.teautil.models.RuntimeOptions;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.example.atuo_attend_backend.nexus.domain.NexusMemoryMetricPoint;

/**
 * 阿里云 CloudMonitor（CMS）适配器：
 * - 从 acs_ecs_dashboard / memory_usedutilization 拉取内存利用率
 */
public class AliyunCmsAdapter {

    private static final ZoneId STORE_ZONE = ZoneId.of("Asia/Shanghai");
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private Client createClient(String accessKeyId, String accessKeySecret, String regionId) throws Exception {
        String endpoint = "cms." + regionId + ".aliyuncs.com";
        Config config = new Config()
                .setAccessKeyId(accessKeyId)
                .setAccessKeySecret(accessKeySecret)
                .setEndpoint(endpoint);
        return new Client(config);
    }

    public List<NexusMemoryMetricPoint> fetchMemoryPoints(
            String accessKeyId,
            String accessKeySecret,
            String regionId,
            String instanceId,
            Instant startTimeUtc,
            Instant endTimeUtc,
            int periodSeconds
    ) throws Exception {
        // 不同账号/地域下 Dimensions 形态不一致：依次尝试（含 regionId、单对象非数组）
        String escId = escapeJson(instanceId);
        String escRg = escapeJson(regionId);
        String[] dimCandidates = new String[] {
                "{\"instanceId\":\"" + escId + "\",\"regionId\":\"" + escRg + "\"}",
                "{\"instanceId\":\"" + escId + "\"}",
                "[{\"instanceId\":\"" + escId + "\",\"regionId\":\"" + escRg + "\"}]",
                "[{\"instanceId\":\"" + escId + "\"}]"
        };
        for (String dimensions : dimCandidates) {
            List<NexusMemoryMetricPoint> pts = fetchMemoryPointsOnce(
                    accessKeyId, accessKeySecret, regionId,
                    instanceId, startTimeUtc, endTimeUtc, periodSeconds, dimensions
            );
            if (pts != null && !pts.isEmpty()) {
                return pts;
            }
        }
        return Collections.emptyList();
    }

    private List<NexusMemoryMetricPoint> fetchMemoryPointsOnce(
            String accessKeyId,
            String accessKeySecret,
            String regionId,
            String instanceId,
            Instant startTimeUtc,
            Instant endTimeUtc,
            int periodSeconds,
            String dimensions
    ) throws Exception {

        Client client = createClient(accessKeyId, accessKeySecret, regionId);
        RuntimeOptions runtime = new RuntimeOptions();

        String start = AliyunOpenApiTimeUtil.utcSecondZ(startTimeUtc);
        String end = AliyunOpenApiTimeUtil.utcSecondZ(endTimeUtc);

        DescribeMetricListRequest request = new DescribeMetricListRequest()
                .setNamespace("acs_ecs_dashboard")
                .setMetricName("memory_usedutilization")
                .setPeriod(String.valueOf(periodSeconds))
                .setStartTime(start)
                .setEndTime(end)
                .setDimensions(dimensions)
                .setLength("1440");

        DescribeMetricListResponse response = client.describeMetricListWithOptions(request, runtime);

        if (response == null || response.body == null) return Collections.emptyList();

        Map<String, Object> bodyMap = response.body.toMap();
        Object datapointsObj = bodyMap.getOrDefault("Datapoints", bodyMap.get("datapoints"));
        String datapoints = datapointsObj instanceof String ? (String) datapointsObj : null;
        if (datapoints == null || datapoints.isBlank()) return Collections.emptyList();

        List<Map<String, Object>> rows = objectMapper.readValue(datapoints, new TypeReference<List<Map<String, Object>>>() {});
        if (rows == null || rows.isEmpty()) return Collections.emptyList();

        List<NexusMemoryMetricPoint> points = new ArrayList<>();
        for (Map<String, Object> r : rows) {
            Object tsObj = firstNonNull(r, "timestamp", "Timestamp", "time");
            if (tsObj == null) continue;
            Object avgObj = firstNonNull(r, "Average", "Value", "value", "Maximum", "Minimum");
            Double v = toDouble(avgObj);
            if (v == null) v = 0d;

            long tsMs = toEpochMillis(tsObj);
            if (tsMs <= 0) continue;

            LocalDateTime ts = Instant.ofEpochMilli(tsMs).atZone(STORE_ZONE).toLocalDateTime();
            NexusMemoryMetricPoint p = new NexusMemoryMetricPoint();
            p.setInstanceId(instanceId);
            p.setTs(ts);
            p.setValue(v);
            points.add(p);
        }
        return points;
    }

    private static String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static Object firstNonNull(Map<String, Object> m, String... keys) {
        for (String k : keys) {
            Object v = m.get(k);
            if (v != null) return v;
        }
        return null;
    }

    /** CMS 可能返回毫秒或秒级时间戳 */
    private static long toEpochMillis(Object tsObj) {
        long raw;
        if (tsObj instanceof Number n) {
            raw = n.longValue();
        } else {
            try {
                raw = Long.parseLong(String.valueOf(tsObj));
            } catch (Exception e) {
                return -1L;
            }
        }
        // 秒级（10 位左右）
        if (raw > 0 && raw < 1_000_000_000_000L) {
            return raw * 1000L;
        }
        return raw;
    }

    private static Double toDouble(Object obj) {
        if (obj == null) return null;
        if (obj instanceof Number n) return n.doubleValue();
        try {
            return Double.parseDouble(String.valueOf(obj));
        } catch (Exception ignored) {
            return null;
        }
    }
}

