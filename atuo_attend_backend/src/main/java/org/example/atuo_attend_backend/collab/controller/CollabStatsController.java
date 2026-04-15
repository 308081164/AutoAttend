package org.example.atuo_attend_backend.collab.controller;

import org.example.atuo_attend_backend.collab.auth.CollabAccessContext;
import org.example.atuo_attend_backend.collab.domain.BizProject;
import org.example.atuo_attend_backend.collab.domain.BizUser;
import org.example.atuo_attend_backend.collab.service.CollabAuthService;
import org.example.atuo_attend_backend.collab.service.CollabProjectService;
import org.example.atuo_attend_backend.commit.CommitService;
import org.example.atuo_attend_backend.common.ApiResponse;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 协作用户（员工）工作统计：参与项目数、本人提交数等，用于工作台展示。
 */
@RestController
@RequestMapping("/api/collab/stats")
public class CollabStatsController {

    private final CollabAuthService authService;
    private final CollabProjectService projectService;
    private final CommitService commitService;

    public CollabStatsController(CollabAuthService authService,
                                CollabProjectService projectService,
                                CommitService commitService) {
        this.authService = authService;
        this.projectService = projectService;
        this.commitService = commitService;
    }

    /**
     * 当前用户工作概览：参与项目数、总提交数、近 7 天提交数。
     */
    @GetMapping("/overview")
    public ApiResponse<Map<String, Object>> overview(HttpServletRequest req) {
        CollabAccessContext ctx = CollabAccessContext.from(req);
        BizUser statsUser = authService.getCurrentUser(ctx.getEffectiveUserId());
        if (statsUser == null) {
            return ApiResponse.error(40400, "用户不存在");
        }

        List<BizProject> projects = projectService.listProjectsForAccess(ctx);
        long projectCount = projects.size();
        String email = statsUser.getEmail();
        long commitCountTotal = commitService.countByAuthorEmailInProjects(email, projects);
        OffsetDateTime since7d = OffsetDateTime.now().minus(7, ChronoUnit.DAYS);
        long commitCount7d = commitService.countByAuthorEmailSinceInProjects(email, projects, since7d);

        Map<String, Object> data = new HashMap<>();
        data.put("projectCount", projectCount);
        data.put("commitCountTotal", commitCountTotal);
        data.put("commitCountLast7Days", commitCount7d);
        data.put("statsUserId", statsUser.getId());
        data.put("statsEmail", email);
        return ApiResponse.ok(data);
    }
}
