package org.example.atuo_attend_backend.nexus.service;

import org.example.atuo_attend_backend.nexus.adapter.aliyun.AliyunEcsAdapter;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 安全组只读：调用阿里云 ECS OpenAPI（DescribeSecurityGroups / DescribeSecurityGroupAttribute）。
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
}
