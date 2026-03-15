package org.example.atuo_attend_backend.ai.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;

/**
 * 调用 DeepSeek 对话 API（OpenAI 兼容格式）。
 * 文档：https://api-docs.deepseek.com/api/create-chat-completion
 */
@Component
public class DeepSeekClient {

    private static final Logger log = LoggerFactory.getLogger(DeepSeekClient.class);
    private static final String BASE_URL = "https://api.deepseek.com";

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 发送 chat completion 请求，返回助手消息的 content；失败返回 null。
     */
    public String chat(String apiKey, String model, List<ChatMessage> messages, boolean requestJson) {
        ChatResult result = chatWithUsage(apiKey, model, messages, requestJson);
        return result != null ? result.getContent() : null;
    }

    /**
     * 发送 chat 请求并解析 usage，用于记录 Token 消耗。
     */
    public ChatResult chatWithUsage(String apiKey, String model, List<ChatMessage> messages, boolean requestJson) {
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("DeepSeek API key is empty");
            return null;
        }
        try {
            String modelStr = model != null && !model.isBlank() ? model : "deepseek-chat";
            ObjectNode body = objectMapper.createObjectNode();
            body.put("model", modelStr);
            ArrayNode msgArray = objectMapper.createArrayNode();
            for (ChatMessage m : messages) {
                ObjectNode msg = objectMapper.createObjectNode();
                msg.put("role", m.getRole());
                msg.put("content", m.getContent());
                msgArray.add(msg);
            }
            body.set("messages", msgArray);
            body.put("max_tokens", 4096);
            if (requestJson) {
                body.putObject("response_format").put("type", "json_object");
            }
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey.trim());
            HttpEntity<String> entity = new HttpEntity<>(objectMapper.writeValueAsString(body), headers);
            ResponseEntity<String> resp = restTemplate.exchange(BASE_URL + "/v1/chat/completions", HttpMethod.POST, entity, String.class);
            if (resp.getStatusCode().is2xxSuccessful() && resp.getBody() != null) {
                JsonNode root = objectMapper.readTree(resp.getBody());
                JsonNode choices = root.get("choices");
                if (choices != null && choices.isArray() && choices.size() > 0) {
                    JsonNode msg = choices.get(0).get("message");
                    if (msg != null && msg.has("content")) {
                        String content = msg.get("content").asText();
                        int inputTokens = 0, outputTokens = 0;
                        JsonNode usage = root.get("usage");
                        if (usage != null) {
                            inputTokens = usage.has("prompt_tokens") ? usage.get("prompt_tokens").asInt(0) : 0;
                            outputTokens = usage.has("completion_tokens") ? usage.get("completion_tokens").asInt(0) : 0;
                        }
                        return new ChatResult(content, inputTokens, outputTokens, modelStr);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("DeepSeek chat failed: {} - {}", e.getClass().getSimpleName(), e.getMessage());
        }
        return null;
    }

    public static class ChatResult {
        private final String content;
        private final int inputTokens;
        private final int outputTokens;
        private final String model;

        public ChatResult(String content, int inputTokens, int outputTokens, String model) {
            this.content = content;
            this.inputTokens = inputTokens;
            this.outputTokens = outputTokens;
            this.model = model;
        }
        public String getContent() { return content; }
        public int getInputTokens() { return inputTokens; }
        public int getOutputTokens() { return outputTokens; }
        public String getModel() { return model; }
        public int getTotalTokens() { return inputTokens + outputTokens; }
    }

    public static class ChatMessage {
        private String role;
        private String content;

        public ChatMessage(String role, String content) {
            this.role = role;
            this.content = content;
        }
        public String getRole() { return role; }
        public String getContent() { return content; }
    }
}
