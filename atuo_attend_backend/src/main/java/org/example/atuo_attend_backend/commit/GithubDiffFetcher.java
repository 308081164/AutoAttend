package org.example.atuo_attend_backend.commit;

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

/**
 * 从 GitHub API 拉取指定 commit 的 diff 文本，用于入库或按需补全。
 * 配置 GITHUB_TOKEN（可选）可提高限流并支持私有仓库。
 * 大陆服务器若无法直连 api.github.com，可配置 GITHUB_API_PROXY（如 http://127.0.0.1:7890）经代理访问。
 */
@Component
public class GithubDiffFetcher {

    private static final Logger log = LoggerFactory.getLogger(GithubDiffFetcher.class);
    private static final String GITHUB_API = "https://api.github.com/repos/%s/commits/%s";
    private static final int MAX_DIFF_BYTES = 2 * 1024 * 1024; // 2MB 上限，避免超大 diff

    private final RestTemplate restTemplate;
    private final String githubToken;

    public GithubDiffFetcher(@Qualifier("githubApiRestTemplate") RestTemplate restTemplate,
                            @Value("${github.token:}") String githubToken) {
        this.restTemplate = restTemplate;
        this.githubToken = (githubToken != null && !githubToken.isBlank()) ? githubToken.trim() : null;
    }

    /**
     * 拉取该 commit 的 diff 文本；失败或超限返回 null。
     */
    public String fetchDiff(String repoFullName, String commitSha) {
        if (repoFullName == null || repoFullName.isBlank() || commitSha == null || commitSha.isBlank()) {
            return null;
        }
        String url = String.format(GITHUB_API, repoFullName, commitSha);
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(java.util.List.of(MediaType.parseMediaType("application/vnd.github.v3.diff")));
        if (githubToken != null) {
            headers.setBearerAuth(githubToken);
        }
        try {
            HttpEntity<Void> entity = new HttpEntity<>(headers);
            String body = restTemplate.exchange(url, HttpMethod.GET, entity, String.class).getBody();
            if (body == null) return null;
            if (body.getBytes().length > MAX_DIFF_BYTES) {
                log.warn("Diff too large for repo={} sha={}, size={}", repoFullName, commitSha, body.getBytes().length);
                int maxChars = Math.min(body.length(), MAX_DIFF_BYTES);
                return body.substring(0, maxChars) + "\n\n... (diff 已截断，超过 " + (MAX_DIFF_BYTES / 1024) + "KB)";
            }
            return body;
        } catch (Exception e) {
            log.warn("Fetch diff failed for repo={} sha={}: {} - {}", repoFullName, commitSha, e.getClass().getSimpleName(), e.getMessage());
            return null;
        }
    }
}
