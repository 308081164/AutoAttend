package org.example.atuo_attend_backend.nexus.service;

import com.aliyun.tea.TeaException;
import org.example.atuo_attend_backend.nexus.adapter.aliyun.AliyunEcsAdapter;
import org.example.atuo_attend_backend.nexus.aliyun.AliyunEcsOpenApiSupport;
import org.example.atuo_attend_backend.nexus.dto.NexusSecurityGroupRuleWriteRequest;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 安全组：Describe + Authorize / Revoke / Modify（阿里云 ECS OpenAPI）。
 */
@Service
public class NexusSecurityGroupService {

    private static final int MAX_PAGE_SIZE = 50;

    private final AliyunEcsAdapter ecsAdapter = new AliyunEcsAdapter();

    public Map<String, Object> listSecurityGroups(
            String accessKeyId,
            String accessKeySecret,
            String regionId,
            int page,
            int pageSize
    ) throws Exception {
        int ps = Math.min(Math.max(pageSize, 1), MAX_PAGE_SIZE);
        int pn = Math.max(page, 1);
        List<AliyunEcsAdapter.SecurityGroupSummary> items = ecsAdapter.listSecurityGroups(accessKeyId, accessKeySecret, regionId, ps, pn);
        Integer total = ecsAdapter.totalCountSecurityGroups(accessKeyId, accessKeySecret, regionId, ps, pn);
        Map<String, Object> out = new HashMap<>();
        out.put("items", items);
        out.put("totalCount", total != null ? total : 0);
        out.put("pageNumber", pn);
        out.put("pageSize", ps);
        return out;
    }

    public List<Map<String, Object>> listRules(
            String accessKeyId,
            String accessKeySecret,
            String regionId,
            String securityGroupId
    ) throws Exception {
        return ecsAdapter.listSecurityGroupRulesMerged(accessKeyId, accessKeySecret, regionId, securityGroupId);
    }

    public void addRule(
            String accessKeyId,
            String accessKeySecret,
            String regionId,
            String securityGroupId,
            NexusSecurityGroupRuleWriteRequest req
    ) throws NexusSecurityGroupWriteException {
        validateWriteCommon(req, true);
        String dir = req.getDirection().trim().toLowerCase(Locale.ROOT);
        try {
            if ("ingress".equals(dir)) {
                ecsAdapter.authorizeIngressRule(
                        accessKeyId, accessKeySecret, regionId, securityGroupId,
                        req.getIpProtocol().trim(),
                        req.getPortRange().trim(),
                        trimToNull(req.getSourceCidrIp()),
                        trimToNull(req.getDestCidrIp()),
                        trimToNull(req.getPolicy()),
                        trimToNull(req.getPriority()),
                        defaultNicType(req.getNicType()),
                        trimToNull(req.getDescription())
                );
            } else {
                ecsAdapter.authorizeEgressRule(
                        accessKeyId, accessKeySecret, regionId, securityGroupId,
                        req.getIpProtocol().trim(),
                        req.getPortRange().trim(),
                        trimToNull(req.getSourceCidrIp()),
                        trimToNull(req.getDestCidrIp()),
                        trimToNull(req.getPolicy()),
                        trimToNull(req.getPriority()),
                        defaultNicType(req.getNicType()),
                        trimToNull(req.getDescription())
                );
            }
        } catch (Exception e) {
            throw wrapAliyun(e);
        }
    }

    public void updateRule(
            String accessKeyId,
            String accessKeySecret,
            String regionId,
            String securityGroupId,
            String ruleId,
            NexusSecurityGroupRuleWriteRequest req
    ) throws NexusSecurityGroupWriteException {
        if (ruleId == null || ruleId.isBlank()) {
            throw new NexusSecurityGroupWriteException(40000, "securityGroupRuleId required", null);
        }
        validateWriteCommon(req, false);
        String dir = req.getDirection().trim().toLowerCase(Locale.ROOT);
        try {
            if ("ingress".equals(dir)) {
                ecsAdapter.modifyIngressRule(
                        accessKeyId, accessKeySecret, regionId, securityGroupId, ruleId.trim(),
                        req.getIpProtocol().trim(),
                        req.getPortRange().trim(),
                        trimToNull(req.getSourceCidrIp()),
                        trimToNull(req.getDestCidrIp()),
                        trimToNull(req.getPolicy()),
                        trimToNull(req.getPriority()),
                        defaultNicType(req.getNicType()),
                        trimToNull(req.getDescription())
                );
            } else {
                ecsAdapter.modifyEgressRule(
                        accessKeyId, accessKeySecret, regionId, securityGroupId, ruleId.trim(),
                        req.getIpProtocol().trim(),
                        req.getPortRange().trim(),
                        trimToNull(req.getSourceCidrIp()),
                        trimToNull(req.getDestCidrIp()),
                        trimToNull(req.getPolicy()),
                        trimToNull(req.getPriority()),
                        defaultNicType(req.getNicType()),
                        trimToNull(req.getDescription())
                );
            }
        } catch (Exception e) {
            throw wrapAliyun(e);
        }
    }

    public void deleteRule(
            String accessKeyId,
            String accessKeySecret,
            String regionId,
            String securityGroupId,
            String ruleId,
            String direction
    ) throws NexusSecurityGroupWriteException {
        if (ruleId == null || ruleId.isBlank()) {
            throw new NexusSecurityGroupWriteException(40000, "securityGroupRuleId required", null);
        }
        if (direction == null || direction.isBlank()) {
            throw new NexusSecurityGroupWriteException(40000, "direction required (ingress|egress)", null);
        }
        String dir = direction.trim().toLowerCase(Locale.ROOT);
        if (!"ingress".equals(dir) && !"egress".equals(dir)) {
            throw new NexusSecurityGroupWriteException(40000, "direction must be ingress or egress", null);
        }
        try {
            if ("ingress".equals(dir)) {
                ecsAdapter.revokeIngressRule(accessKeyId, accessKeySecret, regionId, securityGroupId, ruleId.trim());
            } else {
                ecsAdapter.revokeEgressRule(accessKeyId, accessKeySecret, regionId, securityGroupId, ruleId.trim());
            }
        } catch (Exception e) {
            throw wrapAliyun(e);
        }
    }

    private static void validateWriteCommon(NexusSecurityGroupRuleWriteRequest req, boolean isAdd)
            throws NexusSecurityGroupWriteException {
        if (req == null) {
            throw new NexusSecurityGroupWriteException(40000, "body required", null);
        }
        if (req.getDirection() == null || req.getDirection().isBlank()) {
            throw new NexusSecurityGroupWriteException(40000, "direction required (ingress|egress)", null);
        }
        String dir = req.getDirection().trim().toLowerCase(Locale.ROOT);
        if (!"ingress".equals(dir) && !"egress".equals(dir)) {
            throw new NexusSecurityGroupWriteException(40000, "direction must be ingress or egress", null);
        }
        if (req.getIpProtocol() == null || req.getIpProtocol().isBlank()) {
            throw new NexusSecurityGroupWriteException(40000, "ipProtocol required (e.g. tcp, udp, icmp, all)", null);
        }
        if (req.getPortRange() == null || req.getPortRange().isBlank()) {
            throw new NexusSecurityGroupWriteException(40000, "portRange required (e.g. 22/22 or 1/65535)", null);
        }
        if (isAdd && "ingress".equals(dir)) {
            if (req.getSourceCidrIp() == null || req.getSourceCidrIp().isBlank()) {
                throw new NexusSecurityGroupWriteException(40000, "入方向需填写授权对象 sourceCidrIp（CIDR 或 0.0.0.0/0）", null);
            }
        }
        if (isAdd && "egress".equals(dir)) {
            if (req.getDestCidrIp() == null || req.getDestCidrIp().isBlank()) {
                throw new NexusSecurityGroupWriteException(40000, "出方向需填写授权对象 destCidrIp（CIDR 或 0.0.0.0/0）", null);
            }
        }
    }

    private static String defaultNicType(String nicType) {
        String t = trimToNull(nicType);
        return t != null ? t : "internet";
    }

    private static String trimToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private static NexusSecurityGroupWriteException wrapAliyun(Throwable e) {
        TeaException te = AliyunEcsOpenApiSupport.unwrapTea(e);
        if (te != null) {
            int code = AliyunEcsOpenApiSupport.mapBusinessCode(te);
            String msg = AliyunEcsOpenApiSupport.friendlyMessage(te);
            return new NexusSecurityGroupWriteException(code, msg, te);
        }
        String msg = e.getMessage() != null ? e.getMessage() : "aliyun api error";
        return new NexusSecurityGroupWriteException(50000, msg, e);
    }
}
