package org.example.atuo_attend_backend.commit;

import org.example.atuo_attend_backend.config.SystemConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

/**
 * 通过本地 Git（clone/fetch + git show）拉取指定 commit 的 diff，作为 GitHub API 的兜底方式。
 * 适用于国内网络 API 不稳定时；超时时间可配置并已针对国内慢网络做较长默认值。
 */
@Component
public class LocalGitDiffFetcher {

    private static final Logger log = LoggerFactory.getLogger(LocalGitDiffFetcher.class);
    private static final String GITHUB_REPO_URL = "https://github.com/%s.git";
    private static final int MAX_DIFF_BYTES = 2 * 1024 * 1024; // 与 GithubDiffFetcher 一致 2MB

    private final String workspaceRoot;
    private final long cloneTimeoutSeconds;
    private final long fetchTimeoutSeconds;
    private final long showTimeoutSeconds;
    private final boolean enabled;
    private final SystemConfigService systemConfigService;

    public LocalGitDiffFetcher(
            @Value("${git.workspace:}") String workspaceRoot,
            @Value("${git.clone.timeout.seconds:600}") long cloneTimeoutSeconds,
            @Value("${git.fetch.timeout.seconds:300}") long fetchTimeoutSeconds,
            @Value("${git.show.timeout.seconds:60}") long showTimeoutSeconds,
            @Value("${git.diff.enabled:true}") boolean enabled,
            SystemConfigService systemConfigService) {
        this.workspaceRoot = workspaceRoot != null ? workspaceRoot.trim() : "";
        this.cloneTimeoutSeconds = Math.max(60, cloneTimeoutSeconds);
        this.fetchTimeoutSeconds = Math.max(30, fetchTimeoutSeconds);
        this.showTimeoutSeconds = Math.max(10, showTimeoutSeconds);
        this.enabled = enabled && !this.workspaceRoot.isEmpty();
        this.systemConfigService = systemConfigService;
    }

    public boolean isEnabled() {
        return enabled;
    }

    /**
     * 拉取该 commit 的 diff 文本；未启用、失败或超限返回 null。
     */
    public String fetchDiff(String repoFullName, String commitSha) {
        if (!enabled || repoFullName == null || repoFullName.isBlank() || commitSha == null || commitSha.isBlank()) {
            return null;
        }
        try {
            Path repoDir = ensureRepo(repoFullName);
            if (repoDir == null) return null;
            return runGitShow(repoDir, commitSha);
        } catch (Exception e) {
            log.warn("Local git diff failed for {}@{}: {} - {}", repoFullName, commitSha, e.getClass().getSimpleName(), e.getMessage());
            return null;
        }
    }

    private Path repoDir(String repoFullName) {
        String safe = repoFullName.replace("/", "_").replaceAll("[^a-zA-Z0-9_.-]", "_");
        return Path.of(workspaceRoot).resolve(safe).normalize();
    }

    private Path ensureRepo(String repoFullName) throws IOException, InterruptedException {
        Path dir = repoDir(repoFullName);
        if (Files.isDirectory(dir.resolve(".git"))) {
            int code = runProcess(dir, fetchTimeoutSeconds, "git", "fetch", "--depth", "100", "origin");
            if (code != 0) log.debug("git fetch exited {} for {}", code, repoFullName);
            return dir;
        }
        if (Files.exists(dir)) {
            log.debug("Path exists but not a git repo: {}", dir);
            return null;
        }
        Files.createDirectories(dir.getParent());
        String url = buildCloneUrl(repoFullName);
        int code = runProcess(dir.getParent(), cloneTimeoutSeconds, "git", "clone", "--depth", "100", url, dir.getFileName().toString());
        if (code != 0) return null;
        return dir;
    }

    private String buildCloneUrl(String repoFullName) {
        String token = systemConfigService != null ? systemConfigService.getGitHubToken() : null;
        String base = String.format(GITHUB_REPO_URL, repoFullName);
        if (token != null && !token.isBlank()) {
            return "https://" + token + "@github.com/" + repoFullName + ".git";
        }
        return base;
    }

    private String runGitShow(Path repoDir, String commitSha) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder("git", "show", commitSha, "--format=", "--no-color");
        pb.directory(repoDir.toFile());
        pb.redirectErrorStream(true);
        Process p = pb.start();
        try (InputStream in = p.getInputStream();
             Reader r = new InputStreamReader(in, StandardCharsets.UTF_8);
             BufferedReader br = new BufferedReader(r)) {
            StringBuilder sb = new StringBuilder();
            char[] buf = new char[8192];
            int total = 0;
            int n;
            while ((n = br.read(buf)) != -1 && total < MAX_DIFF_BYTES) {
                sb.append(buf, 0, n);
                total += n;
            }
            boolean ok = p.waitFor(showTimeoutSeconds, TimeUnit.SECONDS);
            if (!ok) {
                p.destroyForcibly();
                log.warn("git show timed out for {} after {}s", commitSha, showTimeoutSeconds);
                return null;
            }
            if (p.exitValue() != 0) return null;
            String out = sb.toString();
            if (total >= MAX_DIFF_BYTES) {
                out = out.substring(0, Math.min(out.length(), MAX_DIFF_BYTES)) + "\n\n... (diff 已截断，超过 " + (MAX_DIFF_BYTES / 1024) + "KB)";
            }
            return out.isBlank() ? null : out;
        }
    }

    /** 执行 Git 命令并消费标准输出，避免管道满阻塞；返回退出码。 */
    private int runProcess(Path cwd, long timeoutSeconds, String... command) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.directory(cwd.toFile());
        pb.redirectErrorStream(true);
        Process p = pb.start();
        try (InputStream in = p.getInputStream()) {
            byte[] buf = new byte[4096];
            while (in.read(buf) != -1) { /* consume */ }
        }
        boolean ok = p.waitFor(timeoutSeconds, TimeUnit.SECONDS);
        if (!ok) {
            p.destroyForcibly();
            log.warn("Git command timed out after {}s: {}", timeoutSeconds, String.join(" ", command));
            return -1;
        }
        return p.exitValue();
    }
}

