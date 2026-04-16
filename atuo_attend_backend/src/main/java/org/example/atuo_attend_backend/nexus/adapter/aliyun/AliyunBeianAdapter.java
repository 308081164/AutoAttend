package org.example.atuo_attend_backend.nexus.adapter.aliyun;

import com.aliyun.beian20160810.Client;
import com.aliyun.beian20160810.models.QueryAccessorDomainStatusRequest;
import com.aliyun.beian20160810.models.QueryAccessorDomainStatusResponse;
import com.aliyun.teaopenapi.models.Config;
import com.aliyun.teautil.models.RuntimeOptions;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 阿里云备案服务（Beian）OpenAPI：用于接入场景下的域名状态等查询。
 * <p>
 * 说明：该接口<strong>不是</strong>工信部公开「备案号全文检索」；返回的是接入/核验相关状态字段。
 */
public class AliyunBeianAdapter {

    public Map<String, Object> queryAccessorDomainStatus(
            String accessKeyId,
            String accessKeySecret,
            String regionId,
            String domain,
            String caller
    ) throws Exception {
        Config config = new Config()
                .setAccessKeyId(accessKeyId)
                .setAccessKeySecret(accessKeySecret);
        if (regionId != null && !regionId.isBlank()) {
            config.setRegionId(regionId.trim());
        } else {
            config.setRegionId("cn-hangzhou");
        }
        Client client = new Client(config);
        RuntimeOptions runtime = new RuntimeOptions();
        QueryAccessorDomainStatusRequest req = new QueryAccessorDomainStatusRequest()
                .setDomain(domain.trim());
        if (caller != null && !caller.isBlank()) {
            req.setCaller(caller.trim());
        }
        QueryAccessorDomainStatusResponse resp = client.queryAccessorDomainStatusWithOptions(req, runtime);
        Map<String, Object> out = new LinkedHashMap<>();
        if (resp == null || resp.getBody() == null) {
            out.put("empty", true);
            return out;
        }
        var body = resp.getBody();
        out.put("httpStatus", resp.getStatusCode());
        out.put("code", body.getCode());
        out.put("message", body.getMessage());
        out.put("requestId", body.getRequestId());
        if (body.getData() != null) {
            var d = body.getData();
            out.put("domain", d.getDomain());
            out.put("status", d.getStatus());
            out.put("reason", d.getReason());
            out.put("reasonCode", d.getReasonCode());
        }
        return out;
    }
}
