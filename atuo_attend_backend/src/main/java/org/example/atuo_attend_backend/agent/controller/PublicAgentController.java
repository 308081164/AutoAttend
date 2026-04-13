package org.example.atuo_attend_backend.agent.controller;

import org.example.atuo_attend_backend.agent.domain.AgentMessage;
import org.example.atuo_attend_backend.agent.domain.AgentSession;
import org.example.atuo_attend_backend.agent.dto.AgentModels.FinishRequest;
import org.example.atuo_attend_backend.agent.dto.AgentModels.SendMessageRequest;
import org.example.atuo_attend_backend.agent.service.AgentSessionService;
import org.example.atuo_attend_backend.common.ApiResponse;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 公开 Agent 接口 - 客户通过 publicToken 访问，无需登录
 */
@RestController
@RequestMapping("/api/public/agent")
public class PublicAgentController {

    private final AgentSessionService sessionService;

    public PublicAgentController(AgentSessionService sessionService) {
        this.sessionService = sessionService;
    }

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
        List<AgentMessage> messages = sessionService.getMessages(session.getId(), 100, 0);
        result.put("messages", messages);

        return ApiResponse.ok(result);
    }

    /**
     * 发送消息
     */
    @PostMapping("/sessions/{publicToken}/messages")
    public ApiResponse<?> sendMessage(@PathVariable String publicToken,
                                      @RequestBody SendMessageRequest request) {
        AgentSession session = sessionService.getByPublicToken(publicToken);
        if (session == null) {
            return ApiResponse.error(40400, "会话不存在");
        }
        if (!"active".equals(session.getStatus())) {
            return ApiResponse.error(40000, "会话已结束，无法继续发送消息");
        }

        AgentMessage message = sessionService.sendMessage(
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
                                           @RequestParam("file") MultipartFile file) {
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
     * 确认描述完毕
     */
    @PostMapping("/sessions/{publicToken}/finish")
    public ApiResponse<?> finishSession(@PathVariable String publicToken,
                                        @RequestBody(required = false) FinishRequest request) {
        AgentSession session = sessionService.getByPublicToken(publicToken);
        if (session == null) {
            return ApiResponse.error(40400, "会话不存在");
        }

        AgentSession updated = sessionService.confirmAndEnd(session.getId());

        Map<String, Object> result = new HashMap<>();
        result.put("session", updated);
        result.put("summary", updated.getSummaryText());

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
}
