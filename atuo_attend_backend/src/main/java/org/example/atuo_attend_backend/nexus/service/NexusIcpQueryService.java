package org.example.atuo_attend_backend.nexus.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.atuo_attend_backend.nexus.adapter.aliyun.AliyunBeianAdapter;
import org.example.atuo_attend_backend.nexus.config.NexusIcpQueryProperties;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * 备案相关查询：官方 Beian OpenAPI + 可选云市场 HTTP。
 */
@Service
public class NexusIcpQueryService {

    private static final String DEFAULT_CALLER = "nexus-console";

    private final AliyunBeianAdapter beianAdapter = new AliyunBeianAdapter();
    private final NexusIcpQueryProperties marketProperties;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public NexusIcpQueryService(NexusIcpQueryProperties marketProperties) {
        this.marketProperties = marketProperties;
    }

    public Map<String, Object> queryOfficialDomainStatus(
            String accessKeyId,
            String accessKeySecret,
            String regionId,
            String domain
    ) throws Exception {
        return beianAdapter.queryAccessorDomainStatus(
                accessKeyId, accessKeySecret, regionId, domain, DEFAULT_CALLER);
    }

    /**
     * 云市场第三方 ICP 查询：将 URL 中 {domain} 替换，或使用 query 参数 domain=。
     */
    public Map<String, Object> queryMarketByDomain(String domain) {
        Map<String, Object> result = new HashMap<>();
        if (!marketProperties.isConfigured()) {
            result.put("configured", false);
            result.put("message", "未配置 nexus.icp.market.api-url 与 nexus.icp.market.app-code，云市场查询不可用。");
            return result;
        }
        String baseUrl = marketProperties.getApiUrl().trim();
        String enc = URLEncoder.encode(domain.trim(), StandardCharsets.UTF_8);
        String url;
        if (baseUrl.contains("{domain}")) {
            url = baseUrl.replace("{domain}", enc);
        } else if (baseUrl.contains("?")) {
            url = baseUrl + "&domain=" + enc;
        } else {
            url = baseUrl + "?domain=" + enc;
        }
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "APPCODE " + marketProperties.getAppCode().trim());
        HttpEntity<Void> entity = new HttpEntity<>(headers);
        try {
            ResponseEntity<String> resp = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            result.put("configured", true);
            result.put("httpStatus", resp.getStatusCode().value());
            String raw = resp.getBody();
            result.put("rawBody", raw);
            try {
                if (raw != null && raw.trim().startsWith("{")) {
                    result.put("json", objectMapper.readValue(raw, new TypeReference<Map<String, Object>>() { }));
                }
            } catch (Exception ignored) {
                // 保留 rawBody 即可
            }
        } catch (HttpStatusCodeException ex) {
            result.put("configured", true);
            result.put("httpStatus", ex.getStatusCode().value());
            result.put("error", ex.getStatusCode().toString());
            result.put("rawBody", ex.getResponseBodyAsString());
        }
        return result;
    }
}
