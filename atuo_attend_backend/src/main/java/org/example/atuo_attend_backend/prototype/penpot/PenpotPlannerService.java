package org.example.atuo_attend_backend.prototype.penpot;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.atuo_attend_backend.ai.client.DeepSeekClient;
import org.example.atuo_attend_backend.ai.domain.AiAnalysisConfig;
import org.example.atuo_attend_backend.ai.service.AiAnalysisConfigService;
import org.example.atuo_attend_backend.prototype.penpot.dto.PenpotLayoutPlan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 使用平台 LLM 生成 Penpot 布局计划（JSON）。
 */
@Service
public class PenpotPlannerService {

    private static final Logger log = LoggerFactory.getLogger(PenpotPlannerService.class);
    private static final Pattern FENCED_JSON = Pattern.compile("```json\\s*([\\s\\S]*?)\\s*```", Pattern.CASE_INSENSITIVE);

    private final AiAnalysisConfigService configService;
    private final DeepSeekClient deepSeekClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public PenpotPlannerService(AiAnalysisConfigService configService, DeepSeekClient deepSeekClient) {
        this.configService = configService;
        this.deepSeekClient = deepSeekClient;
    }

    public PenpotLayoutPlan plan(String userPrompt, String projectName) {
        AiAnalysisConfig cfg = configService.getConfig();
        if (cfg == null || !Boolean.TRUE.equals(cfg.getEnabled()) || cfg.getApiKey() == null || cfg.getApiKey().isBlank()) {
            throw new IllegalStateException("AI 未启用或未配置 API Key");
        }
        String model = cfg.getModel() != null ? cfg.getModel() : "deepseek-chat";
        String system = ""
                + "你是 UI 信息架构助手，输出 Penpot 画布布局计划。\n"
                + "只输出一个 JSON 对象，禁止 Markdown、禁止代码块围栏外的文字。\n"
                + "字段：title（短标题，<=40字）、subtitle（可选，<=80字）、boardWidth（数字，建议1440）、boardHeight（数字，建议900）、"
                + "blocks（数组，3～8项）。\n"
                + "blocks[]：label（区块标题，<=24字）、body（该区块说明，可多行，<=500字）。\n"
                + "根据用户需求归纳信息架构；若需求过长，提炼要点写入 blocks。";

        String user = "项目名称：" + (projectName != null ? projectName : "") + "\n用户需求：\n" + (userPrompt != null ? userPrompt : "");

        String raw = null;
        String lastErr = null;
        for (int attempt = 1; attempt <= 3; attempt++) {
            // 构建消息列表：首次仅 system+user；重试时追加 assistant+user 对话历史以传递错误反馈
            List<DeepSeekClient.ChatMessage> messages;
            if (attempt == 1) {
                messages = List.of(
                        new DeepSeekClient.ChatMessage("system", system),
                        new DeepSeekClient.ChatMessage("user", user)
                );
            } else {
                messages = List.of(
                        new DeepSeekClient.ChatMessage("system", system),
                        new DeepSeekClient.ChatMessage("user", user),
                        new DeepSeekClient.ChatMessage("assistant", raw != null ? raw : ""),
                        new DeepSeekClient.ChatMessage("user", "上一次 JSON 无效，错误：" + lastErr + "。请严格只输出合法 JSON 对象，不要包含任何 Markdown 代码块围栏。")
                );
            }
            DeepSeekClient.ChatResult res = deepSeekClient.chatWithUsage(cfg.getApiKey(), model, messages, true);
            raw = res != null ? res.getContent() : null;
            if (raw == null || raw.isBlank()) {
                lastErr = "AI 返回为空";
                continue;
            }
            try {
                JsonNode node = parseJsonObjectLenient(raw);
                return objectMapper.treeToValue(node, PenpotLayoutPlan.class);
            } catch (Exception e) {
                lastErr = e.getMessage() != null ? e.getMessage() : e.toString();
            }
        }
        throw new IllegalStateException("布局规划失败：" + (lastErr != null ? lastErr : "未知错误"));
    }

    private JsonNode parseJsonObjectLenient(String text) throws Exception {
        String trimmed = text != null ? text.trim() : "";
        if (trimmed.isEmpty()) throw new IllegalArgumentException("空输出");
        try {
            JsonNode node = objectMapper.readTree(trimmed);
            if (node != null && node.isObject()) return node;
        } catch (Exception ignore) { }
        Matcher fenced = FENCED_JSON.matcher(trimmed);
        if (fenced.find() && fenced.group(1) != null) {
            JsonNode node = objectMapper.readTree(fenced.group(1).trim());
            if (node != null && node.isObject()) return node;
        }
        int first = trimmed.indexOf('{');
        int last = trimmed.lastIndexOf('}');
        if (first >= 0 && last > first) {
            JsonNode node = objectMapper.readTree(trimmed.substring(first, last + 1));
            if (node != null && node.isObject()) return node;
        }
        throw new IllegalArgumentException("无法解析 JSON 对象");
    }
}
