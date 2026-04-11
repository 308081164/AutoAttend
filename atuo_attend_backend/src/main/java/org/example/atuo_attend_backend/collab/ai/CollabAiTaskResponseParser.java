package org.example.atuo_attend_backend.collab.ai;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.atuo_attend_backend.collab.dto.CollabAiTaskModels;

import java.util.List;

/**
 * 解析通义/DeepSeek 返回的任务草稿 JSON（数组或 items/tasks 包装）。
 */
public final class CollabAiTaskResponseParser {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private CollabAiTaskResponseParser() {
    }

    public static List<CollabAiTaskModels.AiTaskDraft> parseDrafts(String content) throws Exception {
        if (content == null || content.isBlank()) {
            return List.of();
        }
        String json = stripMarkdownFence(content.trim());
        json = json.replaceAll(",\\s*]", "]");
        try {
            JsonNode root = MAPPER.readTree(json);
            if (root.isArray()) {
                return MAPPER.convertValue(root, new TypeReference<List<CollabAiTaskModels.AiTaskDraft>>() {});
            }
            if (root.isObject()) {
                if (root.has("items") && root.get("items").isArray()) {
                    return MAPPER.convertValue(root.get("items"), new TypeReference<List<CollabAiTaskModels.AiTaskDraft>>() {});
                }
                if (root.has("tasks") && root.get("tasks").isArray()) {
                    return MAPPER.convertValue(root.get("tasks"), new TypeReference<List<CollabAiTaskModels.AiTaskDraft>>() {});
                }
            }
        } catch (Exception ignored) {
            // 回退：截取首个 [...] 片段（兼容模型多输出解释性文字）
        }
        int arrStart = json.indexOf('[');
        int arrEnd = json.lastIndexOf(']');
        if (arrStart >= 0 && arrEnd > arrStart) {
            String slice = json.substring(arrStart, arrEnd + 1).replaceAll(",\\s*]", "]");
            JsonNode root = MAPPER.readTree(slice);
            if (root.isArray()) {
                return MAPPER.convertValue(root, new TypeReference<List<CollabAiTaskModels.AiTaskDraft>>() {});
            }
        }
        throw new IllegalArgumentException("AI 返回中未找到 JSON 数组（需为 [...] 或 { items/tasks: [...] }）");
    }

    private static String stripMarkdownFence(String s) {
        if (!s.startsWith("```")) {
            return s;
        }
        int start = s.indexOf('\n');
        String t = start > 0 ? s.substring(start + 1) : s.substring(3);
        t = t.trim();
        int end = t.lastIndexOf("```");
        if (end > 0) {
            t = t.substring(0, end).trim();
        }
        return t;
    }
}
