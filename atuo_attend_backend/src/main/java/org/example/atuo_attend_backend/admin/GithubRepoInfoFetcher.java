package org.example.atuo_attend_backend.admin;

import org.example.atuo_attend_backend.config.SystemConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 从 GitHub API 拉取仓库基本信息（名称、描述、技术栈）。
 * Token/代理与 GithubDiffFetcher 一致（库内配置或环境变量）。
 */
@Component
public class GithubRepoInfoFetcher {

    private static final Logger log = LoggerFactory.getLogger(GithubRepoInfoFetcher.class);
    private static final String REPO_URL = "https://api.github.com/repos/%s";
    private static final String LANGUAGES_URL = "https://api.github.com/repos/%s/languages";

    private final RestTemplate restTemplate;
    private final SystemConfigService systemConfigService;
    private final String envGitHubToken;

    public GithubRepoInfoFetcher(@Qualifier("githubApiRestTemplate") RestTemplate restTemplate,
                                 SystemConfigService systemConfigService,
                                 @Value("${github.token:}") String envGitHubToken) {
        this.restTemplate = restTemplate;
        this.systemConfigService = systemConfigService;
        this.envGitHubToken = (envGitHubToken != null && !envGitHubToken.isBlank()) ? envGitHubToken.trim() : null;
    }

    private String resolveToken() {
        String fromDb = systemConfigService.getGitHubToken();
        if (fromDb != null) return fromDb;
        return envGitHubToken;
    }

    /**
     * 拉取仓库基本信息与语言占比；失败返回 null。
     */
    public Map<String, Object> fetchRepoInfo(String repoFullName) {
        if (repoFullName == null || repoFullName.isBlank()) return null;
        String token = resolveToken();
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        if (token != null) headers.setBearerAuth(token);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            String repoUrl = String.format(REPO_URL, repoFullName.trim());
            Map<?, ?> repo = restTemplate.exchange(repoUrl, HttpMethod.GET, entity, Map.class).getBody();
            if (repo == null) return null;

            Map<String, Object> out = new LinkedHashMap<>();
            out.put("name", repo.get("name"));
            out.put("fullName", repo.get("full_name"));
            out.put("description", repo.get("description"));
            out.put("htmlUrl", repo.get("html_url"));

            Map<String, Long> languages = fetchLanguages(repoFullName.trim(), headers);
            out.put("languages", languages != null ? languages : Collections.emptyMap());
            return out;
        } catch (Exception e) {
            log.warn("Fetch repo info failed for {}: {} - {}", repoFullName, e.getClass().getSimpleName(), e.getMessage());
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Long> fetchLanguages(String repoFullName, HttpHeaders headers) {
        try {
            String url = String.format(LANGUAGES_URL, repoFullName);
            HttpEntity<Void> entity = new HttpEntity<>(headers);
            Map<String, Number> raw = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class).getBody();
            if (raw == null) return null;
            Map<String, Long> out = new LinkedHashMap<>();
            raw.forEach((k, v) -> out.put(k, v != null ? v.longValue() : 0L));
            return out;
        } catch (Exception e) {
            log.debug("Fetch languages failed for {}: {}", repoFullName, e.getMessage());
            return null;
        }
    }
}
