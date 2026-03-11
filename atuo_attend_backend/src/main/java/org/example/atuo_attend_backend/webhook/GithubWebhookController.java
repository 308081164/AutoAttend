package org.example.atuo_attend_backend.webhook;

import org.example.atuo_attend_backend.commit.CommitService;
import org.example.atuo_attend_backend.common.ApiResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.*;

import java.util.Objects;

@RestController
@RequestMapping("/api/webhooks")
public class GithubWebhookController {

    private final CommitService commitService;

    public GithubWebhookController(CommitService commitService) {
        this.commitService = commitService;
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
        if (payload.getCommits() != null) {
            for (GithubPushPayload.GithubPushCommit c : payload.getCommits()) {
                String authorName = c.getAuthor() != null ? c.getAuthor().getName() : null;
                String authorEmail = c.getAuthor() != null ? c.getAuthor().getEmail() : null;
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

