package org.example.atuo_attend_backend.webhook;

import org.example.atuo_attend_backend.collab.domain.BizProject;
import org.example.atuo_attend_backend.collab.domain.BizUser;
import org.example.atuo_attend_backend.collab.service.CollabSyncService;
import org.example.atuo_attend_backend.commit.CommitService;
import org.example.atuo_attend_backend.common.ApiResponse;
import org.example.atuo_attend_backend.tenant.context.TenantConstants;
import org.example.atuo_attend_backend.tenant.context.TenantContext;
import org.example.atuo_attend_backend.tenant.domain.Tenant;
import org.example.atuo_attend_backend.tenant.mapper.TenantMapper;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Locale;
import java.util.Objects;

@RestController
@RequestMapping("/api/webhooks")
public class GithubWebhookController {

    private final CommitService commitService;
    private final CollabSyncService collabSyncService;
    private final TenantMapper tenantMapper;

    public GithubWebhookController(CommitService commitService, CollabSyncService collabSyncService,
                                   TenantMapper tenantMapper) {
        this.commitService = commitService;
        this.collabSyncService = collabSyncService;
        this.tenantMapper = tenantMapper;
    }

    /**
     * 按组织 slug 路由（推荐）：Webhook URL 形如 {@code .../api/webhooks/github/{slug}}，无需配置 X-Tenant-Id。
     */
    @PostMapping("/github/{slug}")
    public ResponseEntity<ApiResponse<?>> onGithubWebhookBySlug(@PathVariable("slug") String slug,
                                                                @RequestHeader("X-GitHub-Event") String event,
                                                                @RequestHeader(value = "X-GitHub-Delivery", required = false) String deliveryId,
                                                                @RequestHeader HttpHeaders headers,
                                                                @RequestBody(required = false) GithubPushPayload payload) {
        if (slug == null || slug.isBlank()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(40400, "unknown slug"));
        }
        Tenant t = tenantMapper.findBySlug(slug.trim().toLowerCase(Locale.ROOT));
        if (t == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(40400, "unknown slug"));
        }
        return ResponseEntity.ok(handleGithubPush(t.getId(), event, deliveryId, headers, payload));
    }

    /**
     * 兼容旧版：通过请求头 {@code X-Tenant-Id} 指定租户；未传时默认为租户 1。
     */
    @PostMapping("/github")
    public ApiResponse<?> onGithubWebhook(@RequestHeader("X-GitHub-Event") String event,
                                          @RequestHeader(value = "X-GitHub-Delivery", required = false) String deliveryId,
                                          @RequestHeader(value = "X-Tenant-Id", required = false) String tenantIdHeader,
                                          @RequestHeader HttpHeaders headers,
                                          @RequestBody(required = false) GithubPushPayload payload) {
        long tenantId = TenantConstants.DEFAULT_TENANT_ID;
        if (tenantIdHeader != null && !tenantIdHeader.isBlank()) {
            try {
                tenantId = Long.parseLong(tenantIdHeader.trim());
            } catch (NumberFormatException ignored) {
                tenantId = TenantConstants.DEFAULT_TENANT_ID;
            }
        }
        return handleGithubPush(tenantId, event, deliveryId, headers, payload);
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

        // 协作模块：项目随仓库自动创建，用户与成员同步
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
