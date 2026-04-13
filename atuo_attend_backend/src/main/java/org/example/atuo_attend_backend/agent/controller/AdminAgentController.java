package org.example.atuo_attend_backend.agent.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.example.atuo_attend_backend.admin.auth.AdminAuthFilter;
import org.example.atuo_attend_backend.agent.domain.AgentMessage;
import org.example.atuo_attend_backend.agent.domain.AgentSession;
import org.example.atuo_attend_backend.agent.dto.AgentModels.CreateSessionRequest;
import org.example.atuo_attend_backend.agent.service.AgentSessionService;
import org.example.atuo_attend_backend.common.ApiResponse;
import org.example.atuo_attend_backend.tenant.context.TenantContext;
import org.example.atuo_attend_backend.tenant.context.TenantConstants;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 管理 Agent 接口 - 运营方使用，需要登录认证
 */
@RestController
@RequestMapping("/api/admin/agent")
public class AdminAgentController {

    private final AgentSessionService sessionService;

    public AdminAgentController(AgentSessionService sessionService) {
        this.sessionService = sessionService;
    }

    /**
     * 管理端接口应优先使用会话中的租户 ID。{@link org.example.atuo_attend_backend.tenant.web.TenantClearFilter} 先于 {@link AdminAuthFilter} 执行，
     * 会在进入 Controller 前清空 {@link TenantContext}，因此不能仅依赖 ThreadLocal。
     */
    private static long tenantIdFrom(HttpServletRequest request) {
        Object tid = request != null ? request.getAttribute(AdminAuthFilter.ATTR_TENANT_ID) : null;
        if (tid instanceof Long l) {
            return l;
        }
        return TenantContext.getTenantIdOrDefault(TenantConstants.DEFAULT_TENANT_ID);
    }

    /**
     * 创建会话
     */
    @PostMapping("/quote/projects/{projectId}/agent-sessions")
    public ApiResponse<?> createSession(@PathVariable Long projectId,
                                        @RequestBody(required = false) CreateSessionRequest request,
                                        HttpServletRequest httpRequest) {
        long tenantId = tenantIdFrom(httpRequest);
        String projectContext = null;
        List<org.example.atuo_attend_backend.agent.dto.AgentModels.BackgroundTextItem> backgroundTexts = null;
        List<Long> backgroundAttachmentIds = null;

        if (request != null) {
            backgroundTexts = request.getBackgroundTexts();
            backgroundAttachmentIds = request.getBackgroundAttachmentIds();
        }

        AgentSession session = sessionService.createSession(
                tenantId, projectId, projectContext,
                backgroundTexts, backgroundAttachmentIds
        );

        return ApiResponse.ok(session);
    }

    /**
     * 获取项目下的会话列表
     */
    @GetMapping("/quote/projects/{projectId}/agent-sessions")
    public ApiResponse<?> listSessions(@PathVariable Long projectId,
                                       HttpServletRequest httpRequest) {
        tenantIdFrom(httpRequest);
        List<AgentSession> sessions = sessionService.listByProject(projectId);
        return ApiResponse.ok(sessions);
    }

    /**
     * 获取会话详情
     */
    @GetMapping("/sessions/{sessionId}")
    public ApiResponse<?> getSessionDetail(@PathVariable Long sessionId,
                                           HttpServletRequest httpRequest) {
        tenantIdFrom(httpRequest);
        AgentSession session = sessionService.getSession(sessionId);
        if (session == null) {
            return ApiResponse.error(40400, "会话不存在");
        }

        Map<String, Object> result = new HashMap<>();
        result.put("session", session);

        List<AgentMessage> messages = sessionService.getMessages(sessionId, 100, 0);
        result.put("messages", messages);

        return ApiResponse.ok(result);
    }

    /**
     * 终止会话
     */
    @PostMapping("/sessions/{sessionId}/terminate")
    public ApiResponse<?> terminateSession(@PathVariable Long sessionId,
                                           HttpServletRequest httpRequest) {
        tenantIdFrom(httpRequest);
        AgentSession session = sessionService.terminateSession(sessionId);
        return ApiResponse.ok(session);
    }

    /**
     * 获取会话摘要
     */
    @GetMapping("/sessions/{sessionId}/summary")
    public ApiResponse<?> getSummary(@PathVariable Long sessionId,
                                     HttpServletRequest httpRequest) {
        tenantIdFrom(httpRequest);
        AgentSession session = sessionService.getSession(sessionId);
        if (session == null) {
            return ApiResponse.error(40400, "会话不存在");
        }

        Map<String, Object> result = new HashMap<>();
        result.put("sessionId", sessionId);
        result.put("status", session.getStatus());

        // 如果已有摘要，直接返回
        if (session.getSummaryText() != null && !session.getSummaryText().isEmpty()) {
            result.put("summary", session.getSummaryText());
        } else {
            // 否则生成预览摘要
            try {
                String preview = sessionService.generateSummaryPreview(sessionId);
                result.put("summary", preview);
                result.put("isPreview", true);
            } catch (Exception e) {
                result.put("summary", "");
                result.put("isPreview", true);
            }
        }

        return ApiResponse.ok(result);
    }
}
