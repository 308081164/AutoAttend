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
 */
@Configuration
public class RestTemplateConfig {

    private static final Logger log = LoggerFactory.getLogger(RestTemplateConfig.class);

    @Bean
    @Primary
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    /**
     * 仅用于拉取 GitHub commit diff。若配置了 GITHUB_API_PROXY（如 http://127.0.0.1:7890），则经代理访问 api.github.com。
     */
    @Bean("githubApiRestTemplate")
    public RestTemplate githubApiRestTemplate(@Value("${github.api.proxy:}") String proxyUrl) {
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
