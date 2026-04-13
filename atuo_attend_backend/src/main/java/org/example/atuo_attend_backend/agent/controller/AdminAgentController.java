package org.example.atuo_attend_backend.agent.controller;

import lombok.extern.slf4j.Slf4j;
import org.example.atuo_attend_backend.agent.domain.AgentMessage;
import org.example.atuo_attend_backend.agent.domain.AgentSession;
import org.example.atuo_attend_backend.agent.dto.AgentModels.CreateSessionRequest;
import org.example.atuo_attend_backend.agent.service.AgentSessionService;
import org.example.atuo_attend_backend.common.ApiResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 管理 Agent 接口 - 运营方使用，需要登录认证
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/agent")
public class AdminAgentController {

    @Autowired
    private AgentSessionService sessionService;

    /**
     * 创建会话
     */
    @PostMapping("/quote/projects/{projectId}/agent-sessions")
    public ApiResponse<?> createSession(@PathVariable Long projectId,
                                        @RequestBody(required = false) CreateSessionRequest request) {
        try {
            // 从认证上下文中获取 tenantId 和 userId
            Long tenantId = requireUserId();
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
        } catch (Exception e) {
            log.error("Failed to create session: projectId={}", projectId, e);
            return ApiResponse.error("创建会话失败: " + e.getMessage());
        }
    }

    /**
     * 获取项目下的会话列表
     */
    @GetMapping("/quote/projects/{projectId}/agent-sessions")
    public ApiResponse<?> listSessions(@PathVariable Long projectId) {
        try {
            requireUserId();
            List<AgentSession> sessions = sessionService.listByProject(projectId);
            return ApiResponse.ok(sessions);
        } catch (Exception e) {
            log.error("Failed to list sessions: projectId={}", projectId, e);
            return ApiResponse.error("获取会话列表失败: " + e.getMessage());
        }
    }

    /**
     * 获取会话详情
     */
    @GetMapping("/sessions/{sessionId}")
    public ApiResponse<?> getSessionDetail(@PathVariable Long sessionId) {
        try {
            requireUserId();
            AgentSession session = sessionService.getSession(sessionId);
            if (session == null) {
                return ApiResponse.error("会话不存在");
            }

            Map<String, Object> result = new HashMap<>();
            result.put("session", session);

            List<AgentMessage> messages = sessionService.getMessages(sessionId, 100, 0);
            result.put("messages", messages);

            return ApiResponse.ok(result);
        } catch (Exception e) {
            log.error("Failed to get session detail: sessionId={}", sessionId, e);
            return ApiResponse.error("获取会话详情失败: " + e.getMessage());
        }
    }

    /**
     * 终止会话
     */
    @PostMapping("/sessions/{sessionId}/terminate")
    public ApiResponse<?> terminateSession(@PathVariable Long sessionId) {
        try {
            requireUserId();
            AgentSession session = sessionService.terminateSession(sessionId);
            return ApiResponse.ok(session);
        } catch (Exception e) {
            log.error("Failed to terminate session: sessionId={}", sessionId, e);
            return ApiResponse.error("终止会话失败: " + e.getMessage());
        }
    }

    /**
     * 获取会话摘要
     */
    @GetMapping("/sessions/{sessionId}/summary")
    public ApiResponse<?> getSummary(@PathVariable Long sessionId) {
        try {
            requireUserId();
            AgentSession session = sessionService.getSession(sessionId);
            if (session == null) {
                return ApiResponse.error("会话不存在");
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
                    log.warn("Failed to generate summary preview", e);
                    result.put("summary", "");
                    result.put("isPreview", true);
                }
            }

            return ApiResponse.ok(result);
        } catch (Exception e) {
            log.error("Failed to get summary: sessionId={}", sessionId, e);
            return ApiResponse.error("获取摘要失败: " + e.getMessage());
        }
    }

    // ==================== 辅助方法 ====================

    /**
     * 获取当前登录用户 ID（模拟实现，实际项目中从 SecurityContext 获取）
     */
    private Long requireUserId() {
        // TODO: 实际项目中从 SecurityContext / JWT Token 中获取当前用户信息
        // 当前为模拟实现，返回默认 tenantId
        // 示例:
        // Long userId = SecurityContextHolder.getContext().getAuthentication().getUserId();
        // Long tenantId = userMapper.getTenantId(userId);
        // return tenantId;
        return 1L;
    }
}
