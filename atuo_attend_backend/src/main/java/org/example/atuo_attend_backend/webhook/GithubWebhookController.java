package org.example.atuo_attend_backend.webhook;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.atuo_attend_backend.collab.domain.BizProject;
import org.example.atuo_attend_backend.collab.domain.BizUser;
import org.example.atuo_attend_backend.collab.service.CollabSyncService;
import org.example.atuo_attend_backend.commit.CommitService;
import org.example.atuo_attend_backend.common.ApiResponse;
import org.example.atuo_attend_backend.tenant.context.TenantConstants;
import org.example.atuo_attend_backend.tenant.context.TenantContext;
import org.example.atuo_attend_backend.tenant.domain.Tenant;
import org.example.atuo_attend_backend.tenant.mapper.TenantMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Objects;

/**
 * GitHub Webhook：{@code push} 同步提交；{@code workflow_run} 在 CI 非成功时写入「项目调整」表。
 */
@RestController
@RequestMapping("/api/webhooks")
public class GithubWebhookController {

    private static final Logger log = LoggerFactory.getLogger(GithubWebhookController.class);

    private final CommitService commitService;
    private final CollabSyncService collabSyncService;
    private final TenantMapper tenantMapper;
    private final ObjectMapper objectMapper;
    private final GithubActionsCiWebhookService githubActionsCiWebhookService;

    /** 与 GitHub Webhook Secret 一致时校验 {@code X-Hub-Signature-256}；未配置则不校验 */
    private final String githubWebhookHmacSecret;

    public GithubWebhookController(CommitService commitService,
                                   CollabSyncService collabSyncService,
                                   TenantMapper tenantMapper,
                                   ObjectMapper objectMapper,
                                   GithubActionsCiWebhookService githubActionsCiWebhookService,
                                   @Value("${app.github.webhook.hmac-secret:}") String githubWebhookHmacSecret) {
        this.commitService = commitService;
        this.collabSyncService = collabSyncService;
        this.tenantMapper = tenantMapper;
        this.objectMapper = objectMapper;
        this.githubActionsCiWebhookService = githubActionsCiWebhookService;
        this.githubWebhookHmacSecret = githubWebhookHmacSecret;
    }

    /**
     * 按组织 slug 路由（推荐）：Webhook URL 形如 {@code .../api/webhooks/github/{slug}}，无需配置 X-Tenant-Id。
     */
    @PostMapping("/github/{slug}")
    public ResponseEntity<ApiResponse<?>> onGithubWebhookBySlug(@PathVariable("slug") String slug,
                                                                  @RequestHeader("X-GitHub-Event") String event,
                                                                  @RequestHeader(value = "X-GitHub-Delivery", required = false) String deliveryId,
                                                                  @RequestHeader(value = "X-Hub-Signature-256", required = false) String signature256,
                                                                  @RequestHeader HttpHeaders headers,
                                                                  @RequestBody(required = false) byte[] rawBody) {
        if (slug == null || slug.isBlank()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(40400, "unknown slug"));
        }
        Tenant t = tenantMapper.findBySlug(slug.trim().toLowerCase(Locale.ROOT));
        if (t == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(40400, "unknown slug"));
        }
        return dispatchGithubEvent(t.getId(), event, deliveryId, signature256, headers, rawBody);
    }

    /**
     * 兼容旧版：通过请求头 {@code X-Tenant-Id} 指定租户；未传时默认为租户 1。
     */
    @PostMapping("/github")
    public ResponseEntity<ApiResponse<?>> onGithubWebhook(@RequestHeader("X-GitHub-Event") String event,
                                                          @RequestHeader(value = "X-GitHub-Delivery", required = false) String deliveryId,
                                                          @RequestHeader(value = "X-Hub-Signature-256", required = false) String signature256,
                                                          @RequestHeader(value = "X-Tenant-Id", required = false) String tenantIdHeader,
                                                          @RequestHeader HttpHeaders headers,
                                                          @RequestBody(required = false) byte[] rawBody) {
        long tenantId = TenantConstants.DEFAULT_TENANT_ID;
        if (tenantIdHeader != null && !tenantIdHeader.isBlank()) {
            try {
                tenantId = Long.parseLong(tenantIdHeader.trim());
            } catch (NumberFormatException ignored) {
                tenantId = TenantConstants.DEFAULT_TENANT_ID;
            }
        }
        return dispatchGithubEvent(tenantId, event, deliveryId, signature256, headers, rawBody);
    }

    private ResponseEntity<ApiResponse<?>> dispatchGithubEvent(long tenantId,
                                                             String event,
                                                             String deliveryId,
                                                             String signature256,
                                                             HttpHeaders headers,
                                                             byte[] rawBody) {
        byte[] body = rawBody != null ? rawBody : new byte[0];
        if (!GithubWebhookSignatureUtil.isValid(body, signature256, githubWebhookHmacSecret)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error(40100, "invalid webhook signature"));
        }
        String json = body.length == 0 ? "{}" : new String(body, StandardCharsets.UTF_8);
        try {
            if ("push".equals(event)) {
                GithubPushPayload payload = objectMapper.readValue(json, GithubPushPayload.class);
                return ResponseEntity.ok(handleGithubPush(tenantId, event, deliveryId, headers, payload));
            }
            if ("workflow_run".equals(event)) {
                JsonNode root = objectMapper.readTree(json);
                githubActionsCiWebhookService.handleWorkflowRunCompleted(tenantId, deliveryId, root);
                return ResponseEntity.ok(ApiResponse.ok(null));
            }
        } catch (Exception e) {
            log.warn("[github-webhook] event={} parse or handle failed: {}", event, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(40000, "invalid payload"));
        }
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    private ApiResponse<?> handleGithubPush(long tenantId,
                                            String event,
                                            String deliveryId,
                                            HttpHeaders headers,
                                            GithubPushPayload payload) {
        TenantContext.setTenantId(tenantId);

        if (!Objects.equals(event, "push") || payload == null || payload.getRepository() == null) {
            return ApiResponse.ok(null);
        }
        String repoFullName = payload.getRepository().getFull_name();

        BizProject project = collabSyncService.ensureProjectAndTable(repoFullName);

        if (payload.getCommits() != null) {
            for (GithubPushPayload.GithubPushCommit c : payload.getCommits()) {
                String authorName = c.getAuthor() != null ? c.getAuthor().getName() : null;
                String authorEmail = c.getAuthor() != null ? c.getAuthor().getEmail() : null;

                BizUser user = collabSyncService.ensureUser(authorEmail, authorName);
                if (project != null && user != null) {
                    collabSyncService.ensureProjectMember(project.getId(), user.getId());
                }

                commitService.saveCommit(repoFullName,
                        c.getId(),
                        authorName,
                        authorEmail,
                        c.getTimestamp(),
                        c.getMessage(),
                        "");
                commitService.fetchAndSaveDiff(repoFullName, c.getId());
            }
        }
        return ApiResponse.ok(null);
    }
}
