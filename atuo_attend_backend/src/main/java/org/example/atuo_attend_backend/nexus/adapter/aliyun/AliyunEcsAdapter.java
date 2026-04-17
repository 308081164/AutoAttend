package org.example.atuo_attend_backend.nexus.adapter.aliyun;

import com.aliyun.ecs20140526.Client;
import com.aliyun.ecs20140526.models.DescribeInstanceMonitorDataRequest;
import com.aliyun.ecs20140526.models.DescribeInstanceMonitorDataResponse;
import com.aliyun.ecs20140526.models.DescribeInstancesRequest;
import com.aliyun.ecs20140526.models.DescribeInstancesResponse;
import com.aliyun.ecs20140526.models.AuthorizeSecurityGroupEgressRequest;
import com.aliyun.ecs20140526.models.AuthorizeSecurityGroupRequest;
import com.aliyun.ecs20140526.models.DescribeSecurityGroupAttributeRequest;
import com.aliyun.ecs20140526.models.DescribeSecurityGroupAttributeResponse;
import com.aliyun.ecs20140526.models.DescribeSecurityGroupsRequest;
import com.aliyun.ecs20140526.models.DescribeSecurityGroupsResponse;
import com.aliyun.ecs20140526.models.ModifySecurityGroupEgressRuleRequest;
import com.aliyun.ecs20140526.models.ModifySecurityGroupRuleRequest;
import com.aliyun.ecs20140526.models.RebootInstanceRequest;
import com.aliyun.ecs20140526.models.RevokeSecurityGroupEgressRequest;
import com.aliyun.ecs20140526.models.RevokeSecurityGroupRequest;
import com.aliyun.ecs20140526.models.StartInstanceRequest;
import com.aliyun.ecs20140526.models.StopInstanceRequest;
import com.aliyun.teaopenapi.models.Config;
import com.aliyun.teautil.models.RuntimeOptions;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 阿里云 ECS（TeaOpenAPI SDK V2）适配器：
 * - DescribeInstances：拉取实例元数据（含 Memory 容量）
 * - DescribeInstanceMonitorData：拉取 CPU 利用率（先按 CPU 字段落库）
 *
 * 注意：Tea SDK 返回体可转成 toMap()，降低强类型字段访问带来的编译风险。
 */
public class AliyunEcsAdapter {

    private static final ZoneId STORE_ZONE = ZoneId.of("Asia/Shanghai");

    private Client createClient(String accessKeyId, String accessKeySecret, String regionId) throws Exception {
        String endpoint = "ecs." + regionId + ".aliyuncs.com";
        Config config = new Config()
                .setAccessKeyId(accessKeyId)
                .setAccessKeySecret(accessKeySecret)
                .setEndpoint(endpoint);
        return new Client(config);
    }

    public List<InstanceInfo> listInstances(String accessKeyId, String accessKeySecret, String regionId, int pageSize) throws Exception {
        Client client = createClient(accessKeyId, accessKeySecret, regionId);
        RuntimeOptions runtime = new RuntimeOptions();

        DescribeInstancesRequest request = new DescribeInstancesRequest().setRegionId(regionId);
        // 仅 MVP：优先拿一页。由于不同 SDK 版本可能字段名不同，这里用反射设置分页参数，避免编译失败。
        request = tryInvoke(request, "setPageSize", new Class[]{Integer.class}, new Object[]{pageSize});
        request = tryInvoke(request, "setPageNumber", new Class[]{Integer.class}, new Object[]{1});

        DescribeInstancesResponse response = client.describeInstancesWithOptions(request, runtime);
        Map<String, Object> bodyMap = response.body.toMap();
        Object instancesObj = bodyMap.get("Instances");
        if (!(instancesObj instanceof Map<?, ?> instancesMap)) return Collections.emptyList();
        Object listObj = instancesMap.get("Instance");
        if (!(listObj instanceof List<?> list)) return Collections.emptyList();

        List<InstanceInfo> result = new ArrayList<>();
        for (Object item : list) {
            if (!(item instanceof Map<?, ?> m)) continue;
            InstanceInfo info = new InstanceInfo();
            info.instanceId = asString(m.get("InstanceId"));
            info.instanceName = asString(m.get("InstanceName"));
            info.status = asString(m.get("Status"));
            info.instanceType = asString(m.get("InstanceType"));
            info.zoneId = asString(m.get("ZoneId"));
            info.osName = firstNonBlank(asString(m.get("OSNameEn")), asString(m.get("OSName")));
            info.memoryMb = asLong(m.get("Memory"));
            info.privateIp = firstIpFromInner(m);
            info.publicIp = firstIpFromPublic(m);
            if (info.instanceId != null && !info.instanceId.isBlank()) {
                result.add(info);
            }
        }
        return result;
    }

    public List<CpuPointInfo> fetchCpuPoints(
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

        String start = formatUtc(startTimeUtc);
        String end = formatUtc(endTimeUtc);

        DescribeInstanceMonitorDataRequest request = new DescribeInstanceMonitorDataRequest()
                .setInstanceId(instanceId)
                .setStartTime(start)
                .setEndTime(end)
                .setPeriod(periodSeconds);

        DescribeInstanceMonitorDataResponse response = client.describeInstanceMonitorDataWithOptions(request, runtime);
        Map<String, Object> bodyMap = response.body.toMap();
        Object monitorDataObj = bodyMap.get("MonitorData");
        if (!(monitorDataObj instanceof Map<?, ?> monitorDataMap)) return Collections.emptyList();
        Object listObj = monitorDataMap.get("InstanceMonitorData");
        if (!(listObj instanceof List<?> list)) return Collections.emptyList();

        List<CpuPointInfo> points = new ArrayList<>();
        for (Object item : list) {
            if (!(item instanceof Map<?, ?> m)) continue;
            CpuPointInfo p = new CpuPointInfo();
            p.instanceId = asString(m.get("InstanceId"));
            String ts = asString(m.get("TimeStamp"));
            if (ts == null) continue;
            p.ts = parseToStoreZone(ts);
            p.cpuValue = asDouble(m.get("CPU"));
            points.add(p);
        }
        return points;
    }

    public void startInstance(String accessKeyId, String accessKeySecret, String regionId, String instanceId) throws Exception {
        Client client = createClient(accessKeyId, accessKeySecret, regionId);
        RuntimeOptions runtime = new RuntimeOptions();
        StartInstanceRequest req = new StartInstanceRequest()
                .setInstanceId(instanceId);
        client.startInstanceWithOptions(req, runtime);
    }

    public void stopInstance(String accessKeyId, String accessKeySecret, String regionId, String instanceId, boolean forceStop) throws Exception {
        Client client = createClient(accessKeyId, accessKeySecret, regionId);
        RuntimeOptions runtime = new RuntimeOptions();
        StopInstanceRequest req = new StopInstanceRequest()
                .setInstanceId(instanceId)
                .setForceStop(forceStop);
        client.stopInstanceWithOptions(req, runtime);
    }

    /**
     * 分页列举安全组（仅元数据，不含规则明细）。
     */
    public List<SecurityGroupSummary> listSecurityGroups(
            String accessKeyId,
            String accessKeySecret,
            String regionId,
            int pageSize,
            int pageNumber
    ) throws Exception {
        Client client = createClient(accessKeyId, accessKeySecret, regionId);
        RuntimeOptions runtime = new RuntimeOptions();
        DescribeSecurityGroupsRequest request = new DescribeSecurityGroupsRequest()
                .setRegionId(regionId)
                .setPageSize(pageSize)
                .setPageNumber(pageNumber);
        DescribeSecurityGroupsResponse response = client.describeSecurityGroupsWithOptions(request, runtime);
        if (response.body == null || response.body.getSecurityGroups() == null) {
            return Collections.emptyList();
        }
        var groups = response.body.getSecurityGroups().getSecurityGroup();
        if (groups == null || groups.isEmpty()) {
            return Collections.emptyList();
        }
        List<SecurityGroupSummary> out = new ArrayList<>();
        for (var g : groups) {
            if (g == null) continue;
            SecurityGroupSummary s = new SecurityGroupSummary();
            s.securityGroupId = g.getSecurityGroupId();
            s.securityGroupName = g.getSecurityGroupName();
            s.vpcId = g.getVpcId();
            s.description = g.getDescription();
            s.ecsCount = g.getEcsCount();
            s.ruleCount = g.getRuleCount();
            s.creationTime = g.getCreationTime();
            if (s.securityGroupId != null && !s.securityGroupId.isBlank()) {
                out.add(s);
            }
        }
        return out;
    }

    public Integer totalCountSecurityGroups(String accessKeyId, String accessKeySecret, String regionId, int pageSize, int pageNumber) throws Exception {
        Client client = createClient(accessKeyId, accessKeySecret, regionId);
        RuntimeOptions runtime = new RuntimeOptions();
        DescribeSecurityGroupsRequest request = new DescribeSecurityGroupsRequest()
                .setRegionId(regionId)
                .setPageSize(pageSize)
                .setPageNumber(pageNumber);
        DescribeSecurityGroupsResponse response = client.describeSecurityGroupsWithOptions(request, runtime);
        if (response.body == null) {
            return 0;
        }
        return response.body.getTotalCount();
    }

    /**
     * 拉取某安全组全部入/出方向规则（分页 nextToken 聚合）。
     */
    public List<Map<String, Object>> listSecurityGroupRulesMerged(
            String accessKeyId,
            String accessKeySecret,
            String regionId,
            String securityGroupId
    ) throws Exception {
        Client client = createClient(accessKeyId, accessKeySecret, regionId);
        RuntimeOptions runtime = new RuntimeOptions();
        List<Map<String, Object>> merged = new ArrayList<>();
        String nextToken = null;
        int guard = 0;
        while (guard++ < 80) {
            DescribeSecurityGroupAttributeRequest req = new DescribeSecurityGroupAttributeRequest()
                    .setRegionId(regionId)
                    .setSecurityGroupId(securityGroupId)
                    .setDirection("all");
            if (nextToken != null && !nextToken.isBlank()) {
                req.setNextToken(nextToken);
            }
            DescribeSecurityGroupAttributeResponse resp = client.describeSecurityGroupAttributeWithOptions(req, runtime);
            if (resp.body == null) {
                break;
            }
            if (resp.body.getPermissions() != null && resp.body.getPermissions().getPermission() != null) {
                for (var p : resp.body.getPermissions().getPermission()) {
                    if (p == null) continue;
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("securityGroupRuleId", p.getSecurityGroupRuleId());
                    m.put("direction", p.getDirection());
                    m.put("ipProtocol", p.getIpProtocol());
                    m.put("portRange", p.getPortRange());
                    m.put("sourcePortRange", p.getSourcePortRange());
                    m.put("policy", p.getPolicy());
                    m.put("priority", p.getPriority());
                    m.put("nicType", p.getNicType());
                    m.put("sourceCidrIp", p.getSourceCidrIp());
                    m.put("destCidrIp", p.getDestCidrIp());
                    m.put("sourceGroupId", p.getSourceGroupId());
                    m.put("destGroupId", p.getDestGroupId());
                    m.put("description", p.getDescription());
                    m.put("createTime", p.getCreateTime());
                    merged.add(m);
                }
            }
            nextToken = resp.body.getNextToken();
            if (nextToken == null || nextToken.isBlank()) {
                break;
            }
        }
        return merged;
    }

    public void authorizeIngressRule(
            String accessKeyId,
            String accessKeySecret,
            String regionId,
            String securityGroupId,
            String ipProtocol,
            String portRange,
            String sourceCidrIp,
            String destCidrIp,
            String policy,
            String priority,
            String nicType,
            String description
    ) throws Exception {
        Client client = createClient(accessKeyId, accessKeySecret, regionId);
        RuntimeOptions runtime = new RuntimeOptions();
        AuthorizeSecurityGroupRequest req = new AuthorizeSecurityGroupRequest()
                .setRegionId(regionId)
                .setSecurityGroupId(securityGroupId)
                .setIpProtocol(ipProtocol)
                .setPortRange(portRange)
                .setPolicy(policy != null ? policy : "accept");
        if (sourceCidrIp != null && !sourceCidrIp.isBlank()) {
            req.setSourceCidrIp(sourceCidrIp);
        }
        if (destCidrIp != null && !destCidrIp.isBlank()) {
            req.setDestCidrIp(destCidrIp);
        }
        if (priority != null && !priority.isBlank()) {
            req.setPriority(priority);
        }
        if (nicType != null && !nicType.isBlank()) {
            req.setNicType(nicType);
        }
        if (description != null && !description.isBlank()) {
            req.setDescription(description);
        }
        client.authorizeSecurityGroupWithOptions(req, runtime);
    }

    public void authorizeEgressRule(
            String accessKeyId,
            String accessKeySecret,
            String regionId,
            String securityGroupId,
            String ipProtocol,
            String portRange,
            String sourceCidrIp,
            String destCidrIp,
            String policy,
            String priority,
            String nicType,
            String description
    ) throws Exception {
        Client client = createClient(accessKeyId, accessKeySecret, regionId);
        RuntimeOptions runtime = new RuntimeOptions();
        AuthorizeSecurityGroupEgressRequest req = new AuthorizeSecurityGroupEgressRequest()
                .setRegionId(regionId)
                .setSecurityGroupId(securityGroupId)
                .setIpProtocol(ipProtocol)
                .setPortRange(portRange)
                .setPolicy(policy != null ? policy : "accept");
        if (sourceCidrIp != null && !sourceCidrIp.isBlank()) {
            req.setSourceCidrIp(sourceCidrIp);
        }
        if (destCidrIp != null && !destCidrIp.isBlank()) {
            req.setDestCidrIp(destCidrIp);
        }
        if (priority != null && !priority.isBlank()) {
            req.setPriority(priority);
        }
        if (nicType != null && !nicType.isBlank()) {
            req.setNicType(nicType);
        }
        if (description != null && !description.isBlank()) {
            req.setDescription(description);
        }
        client.authorizeSecurityGroupEgressWithOptions(req, runtime);
    }

    public void revokeIngressRule(
            String accessKeyId,
            String accessKeySecret,
            String regionId,
            String securityGroupId,
            String ruleId
    ) throws Exception {
        Client client = createClient(accessKeyId, accessKeySecret, regionId);
        RuntimeOptions runtime = new RuntimeOptions();
        RevokeSecurityGroupRequest req = new RevokeSecurityGroupRequest()
                .setRegionId(regionId)
                .setSecurityGroupId(securityGroupId)
                .setSecurityGroupRuleId(java.util.List.of(ruleId));
        client.revokeSecurityGroupWithOptions(req, runtime);
    }

    public void revokeEgressRule(
            String accessKeyId,
            String accessKeySecret,
            String regionId,
            String securityGroupId,
            String ruleId
    ) throws Exception {
        Client client = createClient(accessKeyId, accessKeySecret, regionId);
        RuntimeOptions runtime = new RuntimeOptions();
        RevokeSecurityGroupEgressRequest req = new RevokeSecurityGroupEgressRequest()
                .setRegionId(regionId)
                .setSecurityGroupId(securityGroupId)
                .setSecurityGroupRuleId(java.util.List.of(ruleId));
        client.revokeSecurityGroupEgressWithOptions(req, runtime);
    }

    public void modifyIngressRule(
            String accessKeyId,
            String accessKeySecret,
            String regionId,
            String securityGroupId,
            String securityGroupRuleId,
            String ipProtocol,
            String portRange,
            String sourceCidrIp,
            String destCidrIp,
            String policy,
            String priority,
            String nicType,
            String description
    ) throws Exception {
        Client client = createClient(accessKeyId, accessKeySecret, regionId);
        RuntimeOptions runtime = new RuntimeOptions();
        ModifySecurityGroupRuleRequest req = new ModifySecurityGroupRuleRequest()
                .setRegionId(regionId)
                .setSecurityGroupId(securityGroupId)
                .setSecurityGroupRuleId(securityGroupRuleId)
                .setIpProtocol(ipProtocol)
                .setPortRange(portRange)
                .setPolicy(policy != null ? policy : "accept");
        if (sourceCidrIp != null && !sourceCidrIp.isBlank()) {
            req.setSourceCidrIp(sourceCidrIp);
        }
        if (destCidrIp != null && !destCidrIp.isBlank()) {
            req.setDestCidrIp(destCidrIp);
        }
        if (priority != null && !priority.isBlank()) {
            req.setPriority(priority);
        }
        if (nicType != null && !nicType.isBlank()) {
            req.setNicType(nicType);
        }
        if (description != null && !description.isBlank()) {
            req.setDescription(description);
        }
        client.modifySecurityGroupRuleWithOptions(req, runtime);
    }

    public void modifyEgressRule(
            String accessKeyId,
            String accessKeySecret,
            String regionId,
            String securityGroupId,
            String securityGroupRuleId,
            String ipProtocol,
            String portRange,
            String sourceCidrIp,
            String destCidrIp,
            String policy,
            String priority,
            String nicType,
            String description
    ) throws Exception {
        Client client = createClient(accessKeyId, accessKeySecret, regionId);
        RuntimeOptions runtime = new RuntimeOptions();
        ModifySecurityGroupEgressRuleRequest req = new ModifySecurityGroupEgressRuleRequest()
                .setRegionId(regionId)
                .setSecurityGroupId(securityGroupId)
                .setSecurityGroupRuleId(securityGroupRuleId)
                .setIpProtocol(ipProtocol)
                .setPortRange(portRange)
                .setPolicy(policy != null ? policy : "accept");
        if (sourceCidrIp != null && !sourceCidrIp.isBlank()) {
            req.setSourceCidrIp(sourceCidrIp);
        }
        if (destCidrIp != null && !destCidrIp.isBlank()) {
            req.setDestCidrIp(destCidrIp);
        }
        if (priority != null && !priority.isBlank()) {
            req.setPriority(priority);
        }
        if (nicType != null && !nicType.isBlank()) {
            req.setNicType(nicType);
        }
        if (description != null && !description.isBlank()) {
            req.setDescription(description);
        }
        client.modifySecurityGroupEgressRuleWithOptions(req, runtime);
    }

    public void rebootInstance(String accessKeyId, String accessKeySecret, String regionId, String instanceId, boolean forceStop) throws Exception {
        Client client = createClient(accessKeyId, accessKeySecret, regionId);
        RuntimeOptions runtime = new RuntimeOptions();
        RebootInstanceRequest req = new RebootInstanceRequest()
                .setInstanceId(instanceId)
                .setForceStop(forceStop);
        client.rebootInstanceWithOptions(req, runtime);
    }

    private static String formatUtc(Instant t) {
        return AliyunOpenApiTimeUtil.utcSecondZ(t);
    }

    private static LocalDateTime parseToStoreZone(String ts) {
        OffsetDateTime odt = OffsetDateTime.parse(ts);
        return odt.toInstant().atZone(STORE_ZONE).toLocalDateTime();
    }

    private static String firstNonBlank(String a, String b) {
        if (a != null && !a.isBlank()) return a;
        return b;
    }

    private static String firstIpFromPublic(Map<?, ?> instanceMap) {
        Object publicIpObj = instanceMap.get("PublicIpAddress");
        if (publicIpObj instanceof Map<?, ?> publicMap) {
            Object ipObj = publicMap.get("IpAddress");
            if (ipObj instanceof List<?> ips && !ips.isEmpty()) return asString(ips.get(0));
            if (ipObj != null) return asString(ipObj);
        }
        return null;
    }

    private static String firstIpFromInner(Map<?, ?> instanceMap) {
        Object innerObj = instanceMap.get("InnerIpAddress");
        if (innerObj instanceof Map<?, ?> innerMap) {
            Object ipObj = innerMap.get("IpAddress");
            if (ipObj instanceof List<?> ips && !ips.isEmpty()) return asString(ips.get(0));
            if (ipObj != null) return asString(ipObj);
        }
        // 兜底：有些场景没有 InnerIpAddress，取 PrimaryIpAddress 也行
        Object niObj = instanceMap.get("NetworkInterfaces");
        if (niObj instanceof Map<?, ?> niMap) {
            Object listObj = niMap.get("NetworkInterface");
            if (listObj instanceof List<?> list && !list.isEmpty() && list.get(0) instanceof Map<?, ?> firstNi) {
                Object primaryIp = firstNi.get("PrimaryIpAddress");
                if (primaryIp != null) return asString(primaryIp);
            }
        }
        return null;
    }

    private static String asString(Object o) {
        return o == null ? null : String.valueOf(o);
    }

    private static Long asLong(Object o) {
        if (o == null) return null;
        if (o instanceof Number n) return n.longValue();
        try {
            return Long.parseLong(String.valueOf(o));
        } catch (Exception ignored) {
            return null;
        }
    }

    private static Double asDouble(Object o) {
        if (o == null) return null;
        if (o instanceof Number n) return n.doubleValue();
        try {
            return Double.parseDouble(String.valueOf(o));
        } catch (Exception ignored) {
            return null;
        }
    }

    public static class InstanceInfo {
        public String instanceId;
        public String instanceName;
        public String status;
        public String instanceType;
        public String zoneId;
        public String publicIp;
        public String privateIp;
        public String osName;
        public Long memoryMb;
    }

    public static class CpuPointInfo {
        public String instanceId;
        public LocalDateTime ts;
        public Double cpuValue;
    }

    public static class SecurityGroupSummary {
        public String securityGroupId;
        public String securityGroupName;
        public String vpcId;
        public String description;
        public Integer ecsCount;
        public Integer ruleCount;
        public String creationTime;
    }

    private static DescribeInstancesRequest tryInvoke(DescribeInstancesRequest req, String method, Class<?>[] pTypes, Object[] args) {
        try {
            var m = req.getClass().getMethod(method, pTypes);
            Object v = m.invoke(req, args);
            if (v instanceof DescribeInstancesRequest dr) return dr;
        } catch (Exception ignored) {
            // ignore
        }
        return req;
    }
}

