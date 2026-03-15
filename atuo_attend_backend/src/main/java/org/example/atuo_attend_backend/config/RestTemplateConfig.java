package org.example.atuo_attend_backend.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URI;

/**
 * 默认 RestTemplate 与供 GitHub API 使用的 RestTemplate（可选代理，便于大陆服务器访问 api.github.com）。
 * 依赖 systemConfigTableReady 确保 aa_system_config 表在读取前已创建（兼容已有库未执行迁移的部署）。
 */
@Configuration
@org.springframework.context.annotation.DependsOn("systemConfigTableReady")
public class RestTemplateConfig {

    private static final Logger log = LoggerFactory.getLogger(RestTemplateConfig.class);

    @Bean
    @Primary
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    /**
     * 仅用于拉取 GitHub commit diff。代理优先使用环境变量 GITHUB_API_PROXY，为空时使用管理后台配置的 GitHub 代理（需重启生效）。
     */
    @Bean("githubApiRestTemplate")
    public RestTemplate githubApiRestTemplate(@Value("${github.api.proxy:}") String envProxy,
                                              SystemConfigService systemConfigService) {
        String proxyUrl = (envProxy != null && !envProxy.isBlank()) ? envProxy.trim() : systemConfigService.getGitHubApiProxy();
        if (proxyUrl != null && !proxyUrl.isBlank()) {
            try {
                URI u = URI.create(proxyUrl.trim());
                String scheme = u.getScheme() == null ? "http" : u.getScheme().toLowerCase();
                String host = u.getHost();
                int port = u.getPort() > 0 ? u.getPort() : (scheme.startsWith("socks") ? 1080 : 80);
                Proxy.Type type = scheme.startsWith("socks") ? Proxy.Type.SOCKS : Proxy.Type.HTTP;
                Proxy proxy = new Proxy(type, new InetSocketAddress(host, port));
                ClientHttpRequestFactory factory = new ProxiedClientHttpRequestFactory(proxy);
                log.info("GitHub API RestTemplate using proxy: {}://{}:{}", scheme, host, port);
                return new RestTemplate(factory);
            } catch (Exception e) {
                log.warn("Invalid GITHUB_API_PROXY '{}', using direct connection: {}", proxyUrl, e.getMessage());
            }
        }
        return new RestTemplate();
    }
}
