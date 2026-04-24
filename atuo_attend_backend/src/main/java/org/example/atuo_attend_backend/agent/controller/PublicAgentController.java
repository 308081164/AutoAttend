package org.example.atuo_attend_backend.agent.controller;

import org.example.atuo_attend_backend.agent.domain.AgentSession;
import org.example.atuo_attend_backend.agent.dto.AgentModels.BackgroundTextItem;
import org.example.atuo_attend_backend.agent.service.AgentSessionService;
import org.example.atuo_attend_backend.common.ApiResponse;
import org.example.atuo_attend_backend.quote.domain.QuoteProject;
import org.example.atuo_attend_backend.quote.mapper.QuoteProjectMapper;
import org.example.atuo_attend_backend.tenant.domain.Tenant;
import org.example.atuo_attend_backend.tenant.mapper.TenantMapper;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 公开 Agent 接口 - 客户通过 publicToken 访问，无需登录
 */
@RestController
@RequestMapping("/api/public/agent")
public class PublicAgentController {

    private final AgentSessionService sessionService;
    private final TenantMapper tenantMapper;
    private final QuoteProjectMapper projectMapper;

    public PublicAgentController(AgentSessionService sessionService,
                                TenantMapper tenantMapper,
                                QuoteProjectMapper projectMapper) {
        this.sessionService = sessionService;
        this.tenantMapper = tenantMapper;
        this.projectMapper = projectMapper;
    }

    // ========== 团队专属全自动 Agent 链接 ==========

    /**
     * 快速创建报价项目 + Agent 会话。
     * 公开接口，通过 tenant slug 标识团队。
     *
     * 请求体：{ "projectName": "xxx", "quoteKind": "single|solution" }
     * 返回：{ "agentUrl": "/agent/{publicToken}", "projectId": 123, "publicToken": "..." }
     */
    @PostMapping("/quick-start/{slug}")
    public ApiResponse<?> quickStart(@PathVariable String slug,
                                     @RequestBody QuickStartRequest request) {
        // 1. 校验参数
        String projectName = (request.getProjectName() == null) ? "" : request.getProjectName().trim();
        if (projectName.isEmpty()) {
            return ApiResponse.error(40000, "请输入项目名称/代号");
        }
        String quoteKind = request.getQuoteKind();
        if (quoteKind == null || quoteKind.isBlank()) {
            quoteKind = "single";
        }
        if (!"single".equals(quoteKind) && !"solution".equals(quoteKind)) {
            return ApiResponse.error(40000, "quoteKind 只支持 single 或 solution");
        }

        // 2. 查找租户
        Tenant tenant = tenantMapper.findBySlug(slug.trim().toLowerCase(Locale.ROOT));
        if (tenant == null) {
            return ApiResponse.error(40400, "团队不存在");
        }
        if ("suspended".equalsIgnoreCase(tenant.getStatus())) {
            return ApiResponse.error(40300, "团队已暂停");
        }

        Long tenantId = tenant.getId();

        // 3. 创建报价项目
        QuoteProject project = new QuoteProject();
        project.setTenantId(tenantId);
        project.setName(projectName);
        project.setProjectType("other");
        project.setTechStack("vue_node");
        project.setStatus("draft");
        project.setQuoteKind(quoteKind);
        project.setQuoteSubjectMode("legal_entity");
        projectMapper.insert(project);
        Long projectId = project.getId();

        // 4. 创建 Agent 会话
        List<BackgroundTextItem> backgrounds = new ArrayList<>();
        backgrounds.add(new BackgroundTextItem("项目名称", projectName));
        backgrounds.add(new BackgroundTextItem("报价模式", "single".equals(quoteKind) ? "单体应用" : "解决方案级"));

        AgentSession session = sessionService.createSession(
                tenantId, projectId, null, backgrounds, null
        );

        // 5. 返回结果
        Map<String, Object> result = new HashMap<>();
        result.put("projectId", projectId);
        result.put("publicToken", session.getPublicToken());
        result.put("agentUrl", "/agent/" + session.getPublicToken());
        result.put("projectName", projectName);
        result.put("quoteKind", quoteKind);
        return ApiResponse.ok(result);
    }

    /**
     * 验证 slug 是否有效（前端落地页预检）
     */
    @GetMapping("/quick-start/{slug}/check")
    public ApiResponse<?> checkSlug(@PathVariable String slug) {
        Tenant tenant = tenantMapper.findBySlug(slug.trim().toLowerCase(Locale.ROOT));
        if (tenant == null) {
            return ApiResponse.error(40400, "团队不存在");
        }
        if ("suspended".equalsIgnoreCase(tenant.getStatus())) {
            return ApiResponse.error(40300, "团队已暂停");
        }
        Map<String, Object> result = new HashMap<>();
        result.put("teamName", tenant.getName());
        return ApiResponse.ok(result);
    }

    // ========== 原有 Agent 接口 ==========

    /**
     * 获取会话信息及历史消息
     */
    @GetMapping("/sessions/{publicToken}")
    public ApiResponse<?> getSession(@PathVariable String publicToken) {
        AgentSession session = sessionService.getByPublicToken(publicToken);
        if (session == null) {
            return ApiResponse.error(40400, "会话不存在");
        }

        Map<String, Object> result = new HashMap<>();
        result.put("session", session);

        // 获取历史消息
        List<org.example.atuo_attend_backend.agent.domain.AgentMessage> messages = sessionService.getMessages(session.getId(), 100, 0);
        result.put("messages", messages);

        return ApiResponse.ok(result);
    }

    /**
     * 发送消息
     */
    @PostMapping("/sessions/{publicToken}/messages")
    public ApiResponse<?> sendMessage(@PathVariable String publicToken,
                                      @RequestBody org.example.atuo_attend_backend.agent.dto.AgentModels.SendMessageRequest request) {
        AgentSession session = sessionService.getByPublicToken(publicToken);
        if (session == null) {
            return ApiResponse.error(40400, "会话不存在");
        }
        if (!"active".equals(session.getStatus())) {
            return ApiResponse.error(40000, "会话已结束，无法继续发送消息");
        }

        org.example.atuo_attend_backend.agent.domain.AgentMessage message = sessionService.sendMessage(
                session.getId(),
                request.getContent(),
                request.getAttachmentIds()
        );

        return ApiResponse.ok(message);
    }

    /**
     * 上传附件
     */
    @PostMapping("/sessions/{publicToken}/attachments")
    public ApiResponse<?> uploadAttachment(@PathVariable String publicToken,
                                           @RequestParam("file") org.springframework.web.multipart.MultipartFile file) {
        AgentSession session = sessionService.getByPublicToken(publicToken);
        if (session == null) {
            return ApiResponse.error(40400, "会话不存在");
        }
        if (!"active".equals(session.getStatus())) {
            return ApiResponse.error(40000, "会话已结束，无法上传附件");
        }

        if (file.isEmpty()) {
            return ApiResponse.error(40000, "上传文件不能为空");
        }

        Map<String, Object> result = sessionService.uploadAttachment(
                session.getId(), session.getProjectId(),
                file.getOriginalFilename(), file.getContentType(),
                file.getSize(), file
        );

        return ApiResponse.ok(result);
    }

    /**
     * 描述完毕：confirmed=false 时仅预览摘要，confirmed=true 时确认结束会话。
     */
    @PostMapping("/sessions/{publicToken}/finish")
    public ApiResponse<?> finishSession(@PathVariable String publicToken,
                                        @RequestBody(required = false) org.example.atuo_attend_backend.agent.dto.AgentModels.FinishRequest request) {
        AgentSession session = sessionService.getByPublicToken(publicToken);
        if (session == null) {
            return ApiResponse.error(40400, "会话不存在");
        }

        boolean confirmed = request != null && Boolean.TRUE.equals(request.getConfirmed());

        Map<String, Object> result = new HashMap<>();
        if (confirmed) {
            // 确认提交：结束会话
            AgentSession updated = sessionService.confirmAndEnd(session.getId());
            result.put("session", updated);
            result.put("summary", updated.getSummaryText());
        } else {
            // 预览模式：仅生成摘要预览，不结束会话
            try {
                String summary = sessionService.generateSummaryPreview(session.getId());
                result.put("summary", summary);
            } catch (Exception e) {
                return ApiResponse.error(50000, "生成摘要失败：" + (e.getMessage() != null ? e.getMessage() : "未知错误"));
            }
        }

        return ApiResponse.ok(result);
    }

    /**
     * 附件预览
     */
    @GetMapping("/attachments/{id}/preview")
    public ApiResponse<?> getAttachmentPreview(@PathVariable Long id) {
        Map<String, Object> result = sessionService.getAttachmentPreview(id);
        if (result == null) {
            return ApiResponse.error(40400, "附件不存在");
        }

        return ApiResponse.ok(result);
    }

    // ========== DTO ==========

    public static class QuickStartRequest {
        private String projectName;
        private String quoteKind;

        public String getProjectName() { return projectName; }
        public void setProjectName(String projectName) { this.projectName = projectName; }
        public String getQuoteKind() { return quoteKind; }
        public void setQuoteKind(String quoteKind) { this.quoteKind = quoteKind; }
    }
}
