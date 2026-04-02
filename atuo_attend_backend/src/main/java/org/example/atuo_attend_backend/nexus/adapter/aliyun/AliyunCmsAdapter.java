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
import java.time.format.DateTimeFormatter;
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

        Client client = createClient(accessKeyId, accessKeySecret, regionId);
        RuntimeOptions runtime = new RuntimeOptions();

        // 先按一段时间窗拉取（MVP：不做 NextToken 循环）
        String start = DateTimeFormatter.ISO_INSTANT.format(startTimeUtc);
        String end = DateTimeFormatter.ISO_INSTANT.format(endTimeUtc);

        // dimensions：{\"instanceId\":\"...\"} 的字符串形式，SDK 参数要求 String
        String dimensions = "[{\"instanceId\":\"" + instanceId + "\"}]";

        DescribeMetricListRequest request = new DescribeMetricListRequest()
                .setNamespace("acs_ecs_dashboard")
                .setMetricName("memory_usedutilization")
                .setPeriod(String.valueOf(periodSeconds))
                .setStartTime(start)
                .setEndTime(end)
                .setDimensions(dimensions)
                // 长度限制：DescribeMetricList 最大允许 1440；此处用 limit 的安全上限
                .setLength("1440");

        DescribeMetricListResponse response = client.describeMetricListWithOptions(request, runtime);

        if (response == null || response.body == null) return Collections.emptyList();

        // Tea SDK 响应体是 TeaModel，可通过 toMap() 兼容不同版本字段名大小写
        Map<String, Object> bodyMap = response.body.toMap();
        Object datapointsObj = bodyMap.getOrDefault("Datapoints", bodyMap.get("datapoints"));
        String datapoints = datapointsObj instanceof String ? (String) datapointsObj : null;
        if (datapoints == null || datapoints.isBlank()) return Collections.emptyList();

        List<Map<String, Object>> rows = objectMapper.readValue(datapoints, new TypeReference<List<Map<String, Object>>>() {});
        if (rows == null || rows.isEmpty()) return Collections.emptyList();

        List<NexusMemoryMetricPoint> points = new ArrayList<>();
        for (Map<String, Object> r : rows) {
            Object tsObj = r.get("timestamp");
            Object avgObj = r.get("Average");
            if (tsObj == null) continue;
            Double v = toDouble(avgObj);
            if (v == null) v = 0d;

            long tsMs;
            if (tsObj instanceof Number n) {
                tsMs = n.longValue();
            } else {
                try {
                    tsMs = Long.parseLong(String.valueOf(tsObj));
                } catch (Exception ignored) {
                    continue;
                }
            }

            LocalDateTime ts = Instant.ofEpochMilli(tsMs).atZone(STORE_ZONE).toLocalDateTime();
            NexusMemoryMetricPoint p = new NexusMemoryMetricPoint();
            p.setInstanceId(instanceId);
            p.setTs(ts);
            p.setValue(v);
            points.add(p);
        }
        return points;
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

