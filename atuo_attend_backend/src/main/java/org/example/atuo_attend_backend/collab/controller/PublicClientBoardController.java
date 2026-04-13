package org.example.atuo_attend_backend.collab.controller;

import org.example.atuo_attend_backend.ai.service.ProjectDailySummaryService;
import org.example.atuo_attend_backend.collab.domain.BizAttachment;
import org.example.atuo_attend_backend.collab.domain.BizProject;
import org.example.atuo_attend_backend.collab.domain.BizProjectClientBoard;
import org.example.atuo_attend_backend.collab.dto.CollabAiTaskModels;
import org.example.atuo_attend_backend.collab.mapper.BizAttachmentMapper;
import org.example.atuo_attend_backend.collab.mapper.BizProjectMapper;
import org.example.atuo_attend_backend.collab.service.ClientBoardShareService;
import org.example.atuo_attend_backend.collab.service.ClientBoardStatsService;
import org.example.atuo_attend_backend.collab.service.CollabAiTaskIngestService;
import org.example.atuo_attend_backend.collab.service.CollabRecordService;
import org.example.atuo_attend_backend.collab.service.CollabTableService;
import org.example.atuo_attend_backend.collab.service.MinioService;
import org.example.atuo_attend_backend.common.ApiResponse;
import org.example.atuo_attend_backend.tenant.context.TenantContext;
import org.example.atuo_attend_backend.tenant.mapper.TenantMapper;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * 客户阅览看板公开接口（仅凭 token，无需登录）。
 * AI 调用使用企业空间已配置的通义 Key，Token 用量计入该企业租户。
 */
@RestController
@RequestMapping("/api/public/client-board")
public class PublicClientBoardController {

    private static final Logger log = LoggerFactory.getLogger(PublicClientBoardController.class);

    private final ClientBoardShareService shareService;
    private final ClientBoardStatsService statsService;
    private final CollabAiTaskIngestService aiIngestService;
    private final BizProjectMapper projectMapper;
    private final TenantMapper tenantMapper;
    private final ProjectDailySummaryService dailySummaryService;
    private final MinioService minioService;
    private final BizAttachmentMapper attachmentMapper;
    private final CollabTableService tableService;
    private final CollabRecordService recordService;

    public PublicClientBoardController(ClientBoardShareService shareService,
                                       ClientBoardStatsService statsService,
                                       CollabAiTaskIngestService aiIngestService,
                                       BizProjectMapper projectMapper,
                                       TenantMapper tenantMapper,
                                       ProjectDailySummaryService dailySummaryService,
                                       MinioService minioService,
                                       BizAttachmentMapper attachmentMapper,
                                       CollabTableService tableService,
                                       CollabRecordService recordService) {
        this.shareService = shareService;
        this.statsService = statsService;
        this.aiIngestService = aiIngestService;
        this.projectMapper = projectMapper;
        this.tenantMapper = tenantMapper;
        this.dailySummaryService = dailySummaryService;
        this.minioService = minioService;
        this.attachmentMapper = attachmentMapper;
        this.tableService = tableService;
        this.recordService = recordService;
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

        // 仅用 name 列，避免 aa_tenant 未跑迁移时 findById 因缺列失败导致 502
        String tenantName = tenantMapper.findNameById(tenantId);
        if (tenantName == null) {
            tenantName = "";
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("tenantName", tenantName);
        data.put("projectName", project.getName() != null ? project.getName() : "");
        data.put("repoId", project.getRepoId() != null ? project.getRepoId() : "");
        data.put("showProgressDashboard", board.getShowProgressDashboard() == null || board.getShowProgressDashboard());
        data.put("showFeatureBacklog", Boolean.TRUE.equals(board.getShowFeatureBacklog()));
        data.put("showAiTableEntry", Boolean.TRUE.equals(board.getShowAiTableEntry()));
        data.put("aiPurpose", board.getAiPurpose() != null ? board.getAiPurpose() : "issue_tracking");

        if (Boolean.TRUE.equals(data.get("showProgressDashboard"))) {
            data.put("progress", safeIssueProgress(tenantId, board.getProjectId()));
        }
        if (Boolean.TRUE.equals(data.get("showFeatureBacklog"))) {
            data.put("featureSummary", safeFeatureSummary(tenantId, board.getProjectId()));
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
        long tenantId = board.getTenantId();
        return TenantContext.runWithTenantId(tenantId, () ->
                aiIngestService.commit(tenantId, board.getProjectId(), purpose, null, "客户（阅览看板）", body));
    }

    // ==================== 开发日报 ====================

    /** 开发日报列表 */
    @GetMapping("/{token}/daily-summaries")
    public ApiResponse<?> listDailySummaries(
            @PathVariable String token,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "pageSize", defaultValue = "10") int pageSize) {
        BizProjectClientBoard board = shareService.requireEnabledBoard(token);
        if (board == null) {
            return ApiResponse.error(40400, "看板不存在或未启用");
        }
        BizProject project = projectMapper.findById(board.getProjectId());
        if (project == null) {
            return ApiResponse.error(40400, "项目不存在");
        }
        String repoId = project.getRepoId();
        if (repoId == null || repoId.isBlank()) {
            return ApiResponse.error(40000, "项目未关联仓库");
        }
        long tenantId = board.getTenantId();
        Map<String, Object> result = TenantContext.runWithTenantId(tenantId,
                () -> dailySummaryService.listByRepo(repoId, page, pageSize));
        return ApiResponse.ok(result);
    }

    /** 开发日报详情 */
    @GetMapping("/{token}/daily-summaries/{id}")
    public ApiResponse<?> getDailySummary(@PathVariable String token,
                                          @PathVariable long id) {
        BizProjectClientBoard board = shareService.requireEnabledBoard(token);
        if (board == null) {
            return ApiResponse.error(40400, "看板不存在或未启用");
        }
        long tenantId = board.getTenantId();
        Optional<?> opt = TenantContext.runWithTenantId(tenantId,
                () -> dailySummaryService.findById(id));
        return opt.map(s -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", ((org.example.atuo_attend_backend.ai.domain.ProjectDailySummary) s).getId());
            m.put("repoFullName", ((org.example.atuo_attend_backend.ai.domain.ProjectDailySummary) s).getRepoFullName());
            m.put("summaryDate", ((org.example.atuo_attend_backend.ai.domain.ProjectDailySummary) s).getSummaryDate() != null
                    ? ((org.example.atuo_attend_backend.ai.domain.ProjectDailySummary) s).getSummaryDate().toString() : null);
            m.put("title", ((org.example.atuo_attend_backend.ai.domain.ProjectDailySummary) s).getTitle());
            m.put("content", ((org.example.atuo_attend_backend.ai.domain.ProjectDailySummary) s).getContent());
            m.put("commitCount", ((org.example.atuo_attend_backend.ai.domain.ProjectDailySummary) s).getCommitCount());
            m.put("model", ((org.example.atuo_attend_backend.ai.domain.ProjectDailySummary) s).getModel());
            m.put("status", ((org.example.atuo_attend_backend.ai.domain.ProjectDailySummary) s).getStatus());
            m.put("errorMessage", ((org.example.atuo_attend_backend.ai.domain.ProjectDailySummary) s).getErrorMessage());
            m.put("createdAt", ((org.example.atuo_attend_backend.ai.domain.ProjectDailySummary) s).getCreatedAt());
            m.put("updatedAt", ((org.example.atuo_attend_backend.ai.domain.ProjectDailySummary) s).getUpdatedAt());
            return ApiResponse.ok(m);
        }).orElseGet(() -> ApiResponse.error(40400, "总结不存在"));
    }

    // ==================== 附件上传 ====================

    /** public 附件上传（用于 AI 录入场景） */
    @PostMapping("/{token}/attachments")
    public ApiResponse<?> uploadAttachment(@PathVariable String token,
                                           @RequestParam("file") MultipartFile file) {
        BizProjectClientBoard board = shareService.requireEnabledBoard(token);
        if (board == null) {
            return ApiResponse.error(40400, "看板不存在或未启用");
        }
        long projectId = board.getProjectId();
        if (file == null || file.isEmpty()) {
            return ApiResponse.error(40000, "请选择文件");
        }
        try {
            String key = minioService.upload(projectId, 0L, file.getOriginalFilename(), file.getInputStream(), file.getSize());
            BizAttachment att = new BizAttachment();
            att.setProjectId(projectId);
            att.setRecordId(null);
            att.setFileName(file.getOriginalFilename() != null ? file.getOriginalFilename() : "file");
            att.setFileSize(file.getSize());
            att.setStorageKey(key);
            att.setUploadedBy(0L);
            attachmentMapper.insert(att);
            Map<String, Object> data = new HashMap<>();
            data.put("id", att.getId());
            data.put("fileName", att.getFileName());
            data.put("fileSize", att.getFileSize());
            data.put("createdAt", att.getCreatedAt());
            data.put("isImage", isImageFileName(att.getFileName()));
            return ApiResponse.ok(data);
        } catch (Exception e) {
            return ApiResponse.error(50000, "上传失败: " + e.getMessage());
        }
    }

    /**
     * 公开预览附件（仅当附件属于该 token 对应项目时允许，供客户看板多维表与 AI 缩略图使用）。
     */
    @GetMapping("/{token}/attachments/{id}/preview")
    public ResponseEntity<Resource> previewAttachment(@PathVariable String token,
                                                      @PathVariable long id) {
        BizProjectClientBoard board = shareService.requireEnabledBoard(token);
        if (board == null) {
            return ResponseEntity.status(404).build();
        }
        long projectId = board.getProjectId();
        BizAttachment att = attachmentMapper.findById(id);
        if (att == null) {
            return ResponseEntity.notFound().build();
        }
        if (!Objects.equals(att.getProjectId(), projectId)) {
            return ResponseEntity.status(403).build();
        }
        try {
            InputStream stream = minioService.download(att.getStorageKey());
            String fn = att.getFileName() != null ? att.getFileName().toLowerCase() : "";
            MediaType mediaType = MediaType.APPLICATION_OCTET_STREAM;
            if (fn.endsWith(".jpg") || fn.endsWith(".jpeg")) {
                mediaType = MediaType.IMAGE_JPEG;
            } else if (fn.endsWith(".png")) {
                mediaType = MediaType.IMAGE_PNG;
            } else if (fn.endsWith(".gif")) {
                mediaType = MediaType.IMAGE_GIF;
            } else if (fn.endsWith(".webp")) {
                mediaType = MediaType.parseMediaType("image/webp");
            } else if (fn.endsWith(".svg")) {
                mediaType = MediaType.parseMediaType("image/svg+xml");
            }
            Resource resource = new InputStreamResource(stream);
            String displayName = att.getFileName() != null ? att.getFileName() : "preview";
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline()
                            .filename(safeFilename(displayName), StandardCharsets.UTF_8).build().toString())
                    .contentType(mediaType)
                    .contentLength(att.getFileSize() != null && att.getFileSize() > 0 ? att.getFileSize() : -1)
                    .body(resource);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    private static String safeFilename(String name) {
        if (name == null || name.isBlank()) {
            return "preview";
        }
        return name.replace("\r", "").replace("\n", "");
    }

    // ==================== 多维表格（只读） ====================

    /** 多维表格数据（只读） */
    @GetMapping("/{token}/table-data")
    public ApiResponse<?> getTableData(
            @PathVariable String token,
            @RequestParam(value = "purpose", defaultValue = "issue_tracking") String purpose,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "pageSize", defaultValue = "20") int pageSize) {
        BizProjectClientBoard board = shareService.requireEnabledBoard(token);
        if (board == null) {
            return ApiResponse.error(40400, "看板不存在或未启用");
        }
        long projectId = board.getProjectId();
        long tenantId = board.getTenantId();
        return TenantContext.runWithTenantId(tenantId, () -> {
            Map<String, Object> tableInfo = tableService.getTableWithColumns(projectId, purpose);
            if (tableInfo == null) {
                return ApiResponse.error(40400, "表格不存在");
            }
            Object tableIdObj = tableInfo.get("id");
            if (tableIdObj == null) {
                return ApiResponse.error(50000, "表格数据异常");
            }
            long tableId = ((Number) tableIdObj).longValue();
            java.util.List<Map<String, Object>> records = recordService.listRecords(tableId, page, pageSize);
            long total = recordService.countRecords(tableId);
            Map<String, Object> result = new HashMap<>();
            result.put("columns", tableInfo.get("columns"));
            result.put("records", records);
            result.put("total", total);
            return ApiResponse.ok(result);
        });
    }

    // ==================== 辅助方法 ====================

    private Map<String, Object> safeIssueProgress(long tenantId, long projectId) {
        try {
            return TenantContext.runWithTenantId(tenantId, () -> statsService.buildIssueProgressStats(projectId));
        } catch (Exception e) {
            log.warn("Client board progress stats failed tenantId={} projectId={}: {}", tenantId, projectId, e.toString());
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("empty", true);
            m.put("message", "统计数据暂不可用");
            return m;
        }
    }

    private java.util.List<Map<String, Object>> safeFeatureSummary(long tenantId, long projectId) {
        try {
            return TenantContext.runWithTenantId(tenantId, () -> statsService.buildFeatureBacklogSummary(projectId, 100));
        } catch (Exception e) {
            log.warn("Client board feature summary failed tenantId={} projectId={}: {}", tenantId, projectId, e.toString());
            return java.util.List.of();
        }
    }

    private static boolean isImageFileName(String name) {
        if (name == null) return false;
        String n = name.toLowerCase();
        return n.endsWith(".jpg") || n.endsWith(".jpeg") || n.endsWith(".png")
                || n.endsWith(".gif") || n.endsWith(".webp") || n.endsWith(".svg");
    }
}
