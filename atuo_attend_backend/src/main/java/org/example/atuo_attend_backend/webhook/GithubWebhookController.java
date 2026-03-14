package org.example.atuo_attend_backend.webhook;

import org.example.atuo_attend_backend.collab.domain.BizProject;
import org.example.atuo_attend_backend.collab.domain.BizUser;
import org.example.atuo_attend_backend.collab.service.CollabSyncService;
import org.example.atuo_attend_backend.commit.CommitService;
import org.example.atuo_attend_backend.common.ApiResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.*;

import java.util.Objects;

@RestController
@RequestMapping("/api/webhooks")
public class GithubWebhookController {

    private final CommitService commitService;
    private final CollabSyncService collabSyncService;

    public GithubWebhookController(CommitService commitService, CollabSyncService collabSyncService) {
        this.commitService = commitService;
        this.collabSyncService = collabSyncService;
    }

    @PostMapping("/github")
    public ApiResponse<?> onGithubWebhook(@RequestHeader("X-GitHub-Event") String event,
                                          @RequestHeader(value = "X-GitHub-Delivery", required = false) String deliveryId,
                                          @RequestHeader HttpHeaders headers,
                                          @RequestBody(required = false) GithubPushPayload payload) {
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

