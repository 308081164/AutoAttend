package org.example.atuo_attend_backend.admin;

import org.example.atuo_attend_backend.ai.client.QwenClient;
import org.example.atuo_attend_backend.ai.domain.AiAnalysisConfig;
import org.example.atuo_attend_backend.ai.official.OfficialAiPoolService;
import org.example.atuo_attend_backend.ai.service.AiAnalysisConfigService;
import org.example.atuo_attend_backend.common.ApiResponse;
import org.example.atuo_attend_backend.tenant.context.TenantConstants;
import org.example.atuo_attend_backend.tenant.context.TenantContext;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 控制台「小助理」：基于租户已配置的通义（DashScope）进行多轮文本对话。
 */
@RestController
@RequestMapping("/api/admin/assistant")
public class AdminAssistantController {

    private static final int MAX_MESSAGES = 24;
    private static final int MAX_CONTENT_LEN = 8000;

    private final AiAnalysisConfigService aiAnalysisConfigService;
    private final OfficialAiPoolService officialAiPoolService;
    private final QwenClient qwenClient;

    public AdminAssistantController(AiAnalysisConfigService aiAnalysisConfigService,
                                      OfficialAiPoolService officialAiPoolService,
                                      QwenClient qwenClient) {
        this.aiAnalysisConfigService = aiAnalysisConfigService;
        this.officialAiPoolService = officialAiPoolService;
        this.qwenClient = qwenClient;
    }

    @SuppressWarnings("unchecked")
    @PostMapping("/qwen-chat")
    public ApiResponse<Map<String, Object>> qwenChat(@RequestBody Map<String, Object> body) {
        AiAnalysisConfig qwen = aiAnalysisConfigService.getQwenConfig();
        if (qwen == null || !Boolean.TRUE.equals(qwen.getEnabled()) || qwen.getApiKey() == null || qwen.getApiKey().isBlank()) {
            return ApiResponse.error(40001, "请先在「API 配置」中启用并保存通义千问（DashScope）API Key");
        }
        List<Map<String, Object>> raw = body != null && body.get("messages") instanceof List
                ? (List<Map<String, Object>>) body.get("messages")
                : List.of();
        if (raw.isEmpty()) {
            return ApiResponse.error(40000, "messages 不能为空");
        }
        List<QwenClient.ChatMessage> msgs = new ArrayList<>();
        msgs.add(new QwenClient.ChatMessage("system",
                "你是 AutoAttend 控制台内的「小助理」，用简洁中文回答用户关于研发协作、项目管理与产品使用的问题。"
                        + "若问题超出能力，请如实说明。", null));
        int n = 0;
        for (Map<String, Object> m : raw) {
            if (n++ >= MAX_MESSAGES) break;
            String role = m.get("role") != null ? String.valueOf(m.get("role")).trim().toLowerCase() : "";
            String content = m.get("content") != null ? String.valueOf(m.get("content")) : "";
            if (content.length() > MAX_CONTENT_LEN) {
                content = content.substring(0, MAX_CONTENT_LEN);
            }
            if (!"user".equals(role) && !"assistant".equals(role)) {
                continue;
            }
            if (content.isBlank()) continue;
            msgs.add(new QwenClient.ChatMessage(role, content, null));
        }
        if (msgs.size() <= 1) {
            return ApiResponse.error(40000, "请至少发送一条用户消息");
        }
        long tenantId = TenantContext.getTenantIdOrDefault(TenantConstants.DEFAULT_TENANT_ID);
        String model = qwen.getModel() != null && !qwen.getModel().isBlank() ? qwen.getModel() : "qwen-vl-plus";
        OfficialAiPoolService.QwenChatOutcome out = officialAiPoolService.chatQwen(
                qwenClient, tenantId, qwen.getApiKey(), model, msgs, false);
        if (out == null || out.result() == null) {
            return ApiResponse.error(50001, "模型调用失败");
        }
        QwenClient.ChatResult res = out.result();
        if (res.isError()) {
            return ApiResponse.error(50002, res.getErrorMessage() != null ? res.getErrorMessage() : "模型返回错误");
        }
        officialAiPoolService.recordQwenUsage(tenantId, res, out.officialPool());
        Map<String, Object> data = new HashMap<>();
        data.put("content", res.getContent() != null ? res.getContent() : "");
        data.put("model", res.getModel());
        return ApiResponse.ok(data);
    }
}
