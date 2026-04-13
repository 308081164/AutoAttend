package org.example.atuo_attend_backend.agent.controller;

import lombok.extern.slf4j.Slf4j;
import org.example.atuo_attend_backend.agent.domain.AgentMessage;
import org.example.atuo_attend_backend.agent.domain.AgentSession;
import org.example.atuo_attend_backend.agent.dto.AgentModels.FinishRequest;
import org.example.atuo_attend_backend.agent.dto.AgentModels.SendMessageRequest;
import org.example.atuo_attend_backend.agent.service.AgentSessionService;
import org.example.atuo_attend_backend.collab.mapper.BizAttachmentMapper;
import org.example.atuo_attend_backend.collab.service.MinioService;
import org.example.atuo_attend_backend.common.ApiResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 公开 Agent 接口 - 客户通过 publicToken 访问，无需登录
 */
@Slf4j
@RestController
@RequestMapping("/api/public/agent")
public class PublicAgentController {

    @Autowired
    private AgentSessionService sessionService;

    @Autowired
    private BizAttachmentMapper attachmentMapper;

    @Autowired
    private MinioService minioService;

    /**
     * 获取会话信息及历史消息
     */
    @GetMapping("/sessions/{publicToken}")
    public ApiResponse<?> getSession(@PathVariable String publicToken) {
        try {
            AgentSession session = sessionService.getByPublicToken(publicToken);
            if (session == null) {
                return ApiResponse.error("会话不存在");
            }

            Map<String, Object> result = new HashMap<>();
            result.put("session", session);

            // 获取历史消息
            List<AgentMessage> messages = sessionService.getMessages(session.getId(), 100, 0);
            result.put("messages", messages);

            return ApiResponse.ok(result);
        } catch (Exception e) {
            log.error("Failed to get session: publicToken={}", publicToken, e);
            return ApiResponse.error("获取会话信息失败: " + e.getMessage());
        }
    }

    /**
     * 发送消息
     */
    @PostMapping("/sessions/{publicToken}/messages")
    public ApiResponse<?> sendMessage(@PathVariable String publicToken,
                                      @RequestBody SendMessageRequest request) {
        try {
            AgentSession session = sessionService.getByPublicToken(publicToken);
            if (session == null) {
                return ApiResponse.error("会话不存在");
            }
            if (!"active".equals(session.getStatus())) {
                return ApiResponse.error("会话已结束，无法继续发送消息");
            }

            AgentMessage message = sessionService.sendMessage(
                    session.getId(),
                    request.getContent(),
                    request.getAttachmentIds()
            );

            return ApiResponse.ok(message);
        } catch (Exception e) {
            log.error("Failed to send message: publicToken={}", publicToken, e);
            return ApiResponse.error("发送消息失败: " + e.getMessage());
        }
    }

    /**
     * 上传附件
     */
    @PostMapping("/sessions/{publicToken}/attachments")
    public ApiResponse<?> uploadAttachment(@PathVariable String publicToken,
                                           @RequestParam("file") MultipartFile file) {
        try {
            AgentSession session = sessionService.getByPublicToken(publicToken);
            if (session == null) {
                return ApiResponse.error("会话不存在");
            }
            if (!"active".equals(session.getStatus())) {
                return ApiResponse.error("会话已结束，无法上传附件");
            }

            if (file.isEmpty()) {
                return ApiResponse.error("上传文件不能为空");
            }

            // 上传到 MinIO
            String objectName = "agent/" + session.getId() + "/" + System.currentTimeMillis() + "_" + file.getOriginalFilename();
            String url = minioService.uploadFile(objectName, file.getInputStream(), file.getContentType(), file.getSize());

            // 保存附件记录
            Map<String, Object> attachment = new HashMap<>();
            attachment.put("sessionId", session.getId());
            attachment.put("fileName", file.getOriginalFilename());
            attachment.put("fileSize", file.getSize());
            attachment.put("fileType", file.getContentType());
            attachment.put("objectName", objectName);
            attachment.put("url", url);
            attachmentMapper.insert(attachment);

            Map<String, Object> result = new HashMap<>();
            result.put("id", attachment.get("id"));
            result.put("fileName", file.getOriginalFilename());
            result.put("fileSize", file.getSize());
            result.put("fileType", file.getContentType());
            result.put("url", url);

            return ApiResponse.ok(result);
        } catch (Exception e) {
            log.error("Failed to upload attachment: publicToken={}", publicToken, e);
            return ApiResponse.error("上传附件失败: " + e.getMessage());
        }
    }

    /**
     * 确认描述完毕
     */
    @PostMapping("/sessions/{publicToken}/finish")
    public ApiResponse<?> finishSession(@PathVariable String publicToken,
                                        @RequestBody(required = false) FinishRequest request) {
        try {
            AgentSession session = sessionService.getByPublicToken(publicToken);
            if (session == null) {
                return ApiResponse.error("会话不存在");
            }

            AgentSession updated = sessionService.confirmAndEnd(session.getId());

            Map<String, Object> result = new HashMap<>();
            result.put("session", updated);
            result.put("summary", updated.getSummaryText());

            return ApiResponse.ok(result);
        } catch (Exception e) {
            log.error("Failed to finish session: publicToken={}", publicToken, e);
            return ApiResponse.error("结束会话失败: " + e.getMessage());
        }
    }

    /**
     * 附件预览（通过 MinIO）
     */
    @GetMapping("/attachments/{id}/preview")
    public ApiResponse<?> getAttachmentPreview(@PathVariable Long id) {
        try {
            Map<String, Object> attachment = attachmentMapper.findById(id);
            if (attachment == null) {
                return ApiResponse.error("附件不存在");
            }

            String objectName = (String) attachment.get("objectName");
            String previewUrl = minioService.getPresignedUrl(objectName);

            Map<String, Object> result = new HashMap<>();
            result.put("id", attachment.get("id"));
            result.put("fileName", attachment.get("fileName"));
            result.put("fileType", attachment.get("fileType"));
            result.put("previewUrl", previewUrl);

            return ApiResponse.ok(result);
        } catch (Exception e) {
            log.error("Failed to get attachment preview: id={}", id, e);
            return ApiResponse.error("获取附件预览失败: " + e.getMessage());
        }
    }
}
