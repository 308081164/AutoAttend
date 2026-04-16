package org.example.atuo_attend_backend.nexus.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 云市场等第三方 ICP 查询 HTTP 接口（与官方 Beian OpenAPI 不同，通常为 AppCode + URL）。
 */
@Component
@ConfigurationProperties(prefix = "nexus.icp.market")
public class NexusIcpQueryProperties {

    /**
     * 完整请求 URL（含路径），例如云市场商品提供的 HTTPS 地址；未配置则禁用该能力。
     */
    private String apiUrl = "";

    /**
     * 云市场 API 的 AppCode（或文档要求的首部值）；建议使用环境变量注入，勿写入仓库。
     */
    private String appCode = "";

    public String getApiUrl() {
        return apiUrl;
    }

    public void setApiUrl(String apiUrl) {
        this.apiUrl = apiUrl;
    }

    public String getAppCode() {
        return appCode;
    }

    public void setAppCode(String appCode) {
        this.appCode = appCode;
    }

    public boolean isConfigured() {
        return apiUrl != null && !apiUrl.isBlank() && appCode != null && !appCode.isBlank();
    }
}
