package org.example.atuo_attend_backend.collab.controller;

import org.example.atuo_attend_backend.collab.domain.BizProject;
import org.example.atuo_attend_backend.collab.domain.BizProjectClientBoard;
import org.example.atuo_attend_backend.collab.dto.CollabAiTaskModels;
import org.example.atuo_attend_backend.collab.mapper.BizProjectMapper;
import org.example.atuo_attend_backend.collab.service.ClientBoardShareService;
import org.example.atuo_attend_backend.collab.service.ClientBoardStatsService;
import org.example.atuo_attend_backend.collab.service.CollabAiTaskIngestService;
import org.example.atuo_attend_backend.common.ApiResponse;
import org.example.atuo_attend_backend.tenant.context.TenantContext;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 客户阅览看板公开接口（仅凭 token，无需登录）。
 * AI 调用使用企业空间已配置的通义 Key，Token 用量计入该企业租户。
 */
@RestController
@RequestMapping("/api/public/client-board")
public class PublicClientBoardController {

    private final ClientBoardShareService shareService;
    private final ClientBoardStatsService statsService;
    private final CollabAiTaskIngestService aiIngestService;
    private final BizProjectMapper projectMapper;

    public PublicClientBoardController(ClientBoardShareService shareService,
                                       ClientBoardStatsService statsService,
                                       CollabAiTaskIngestService aiIngestService,
                                       BizProjectMapper projectMapper) {
        this.shareService = shareService;
        this.statsService = statsService;
        this.aiIngestService = aiIngestService;
        this.projectMapper = projectMapper;
    }

    @GetMapping("/{token}")
    public ApiResponse<?> getBoard(@PathVariable String token) {
        BizProjectClientBoard board = shareService.requireEnabledBoard(token);
        if (board == null) {
            return ApiResponse.error(40400, "看板不存在或未启用");
        }
        BizProject project = projectMapper.findById(board.getProjectId());
        if (project == null) {
            return ApiResponse.error(40400, "项目不存在");
        }
        long tenantId = board.getTenantId();
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("projectName", project.getName() != null ? project.getName() : "");
        data.put("repoId", project.getRepoId() != null ? project.getRepoId() : "");
        data.put("showProgressDashboard", board.getShowProgressDashboard() == null || board.getShowProgressDashboard());
        data.put("showFeatureBacklog", Boolean.TRUE.equals(board.getShowFeatureBacklog()));
        data.put("showAiTableEntry", Boolean.TRUE.equals(board.getShowAiTableEntry()));
        data.put("aiPurpose", board.getAiPurpose() != null ? board.getAiPurpose() : "issue_tracking");

        if (Boolean.TRUE.equals(data.get("showProgressDashboard"))) {
            data.put("progress", TenantContext.runWithTenantId(tenantId, () -> statsService.buildIssueProgressStats(board.getProjectId())));
        }
        if (Boolean.TRUE.equals(data.get("showFeatureBacklog"))) {
            data.put("featureSummary", TenantContext.runWithTenantId(tenantId, () -> statsService.buildFeatureBacklogSummary(board.getProjectId(), 100)));
        }
        return ApiResponse.ok(data);
    }

    @PostMapping("/{token}/ai-preview")
    public ApiResponse<?> aiPreview(@PathVariable String token,
                                    @RequestBody CollabAiTaskModels.AiTaskPreviewRequest body) {
        BizProjectClientBoard board = shareService.requireEnabledBoard(token);
        if (board == null) {
            return ApiResponse.error(40400, "看板不存在或未启用");
        }
        if (!Boolean.TRUE.equals(board.getShowAiTableEntry())) {
            return ApiResponse.error(40300, "未开放 AI 录入");
        }
        String purpose = board.getAiPurpose() != null ? board.getAiPurpose() : "issue_tracking";
        if (!"issue_tracking".equals(purpose)) {
            return ApiResponse.error(40000, "当前阅览看板仅支持对「项目调整」表使用 AI 录入；请在配置中将 AI 目标表设为项目调整。");
        }
        long tenantId = board.getTenantId();
        return TenantContext.runWithTenantId(tenantId, () -> aiIngestService.preview(tenantId, board.getProjectId(), purpose, body));
    }

    @PostMapping("/{token}/ai-commit")
    public ApiResponse<?> aiCommit(@PathVariable String token,
                                   @RequestBody CollabAiTaskModels.AiTaskCommitRequest body) {
        BizProjectClientBoard board = shareService.requireEnabledBoard(token);
        if (board == null) {
            return ApiResponse.error(40400, "看板不存在或未启用");
        }
        if (!Boolean.TRUE.equals(board.getShowAiTableEntry())) {
            return ApiResponse.error(40300, "未开放 AI 录入");
        }
        String purpose = board.getAiPurpose() != null ? board.getAiPurpose() : "issue_tracking";
        if (!"issue_tracking".equals(purpose)) {
            return ApiResponse.error(40000, "当前阅览看板仅支持对「项目调整」表使用 AI 录入；请在配置中将 AI 目标表设为项目调整。");
        }
        if (body.getTasks() != null) {
            for (CollabAiTaskModels.AiTaskDraft t : body.getTasks()) {
                if (t != null) {
                    t.setAttachmentIds(null);
                }
            }
        }
        long tenantId = board.getTenantId();
        return TenantContext.runWithTenantId(tenantId, () ->
                aiIngestService.commit(tenantId, board.getProjectId(), purpose, null, "客户（阅览看板）", body));
    }
}
