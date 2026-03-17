package org.example.atuo_attend_backend.ai.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/**
 * 通义·千问多模态客户端。
 * 使用 DashScope「多模态生成」接口，支持模型：qwen-vl-plus、qwen-vl-max、qwen3-vl-plus 等。
 * 注意：qwen-omni 仅支持 compatible-mode 接口且需流式，此处默认使用 qwen-vl-plus。
 */
@Component
public class QwenClient {

    private static final Logger log = LoggerFactory.getLogger(QwenClient.class);
    private static final String BASE_URL = "https://dashscope.aliyuncs.com";
    /** multimodal-generation 接口支持的默认模型（qwen-omni 仅支持 compatible-mode，此处用 qwen-vl-plus） */
    private static final String DEFAULT_MODEL = "qwen-vl-plus";

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    /** 将不在此接口支持的模型名映射为可用模型，避免历史配置 qwen-omni 导致失败 */
    private static String resolveModel(String model) {
        if (model == null || model.isBlank()) return DEFAULT_MODEL;
        if (model.startsWith("qwen-omni") || "qwen-omni".equalsIgnoreCase(model)) return DEFAULT_MODEL;
        return model;
    }

    /** 仅保留 DashScope 可访问的图片 URL（公网 HTTPS），过滤内网/预签名等避免 400 Invalid URL */
    private static List<String> filterReachableImageUrls(List<String> imageUrls) {
        if (imageUrls == null || imageUrls.isEmpty()) return List.of();
        List<String> out = new ArrayList<>();
        for (String url : imageUrls) {
            if (url == null || url.isBlank()) continue;
            if (isUrlLikelyReachableByDashScope(url)) out.add(url);
            else log.debug("Skip image URL not reachable by DashScope: {}", url.length() > 80 ? url.substring(0, 80) + "..." : url);
        }
        return out;
    }

    private static boolean isUrlLikelyReachableByDashScope(String url) {
        if (url == null || url.isBlank()) return false;
        if (url.startsWith("data:")) return true; // Base64 内联图片，直接传给千问
        if (!url.startsWith("https://")) return false;
        try {
            URI u = URI.create(url);
            String host = u.getHost();
            if (host == null) return false;
            String h = host.toLowerCase();
            if (h.equals("localhost") || h.equals("127.0.0.1") || h.startsWith("minio") || h.equals("minio")) return false;
            if (h.startsWith("10.") || h.startsWith("172.16.") || h.startsWith("172.17.") || h.startsWith("172.18.")
                    || h.startsWith("172.19.") || h.startsWith("172.2") || h.startsWith("172.30.") || h.startsWith("172.31.")
                    || h.startsWith("192.168.")) return false; // 内网地址 DashScope 无法访问
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public ChatResult chat(String apiKey, String model, List<ChatMessage> messages, boolean responseJson) {
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("Qwen API key is empty");
            return ChatResult.error("未配置 API Key");
        }
        String modelStr = resolveModel(model);
        try {
            // DashScope HTTP API：messages 必须放在 input 对象中，且多模态 content 为 [{text},{image:url}] 格式
            ObjectNode body = objectMapper.createObjectNode();
            body.put("model", modelStr);

            ArrayNode arr = objectMapper.createArrayNode();
            for (ChatMessage m : messages) {
                ObjectNode msg = objectMapper.createObjectNode();
                msg.put("role", m.getRole());
                List<String> reachableUrls = filterReachableImageUrls(m.getImageUrls());
                if (!reachableUrls.isEmpty()) {
                    ArrayNode contentArr = objectMapper.createArrayNode();
                    for (String url : reachableUrls) {
                        ObjectNode imgPart = objectMapper.createObjectNode();
                        imgPart.put("image", url);
                        contentArr.add(imgPart);
                    }
                    ObjectNode textPart = objectMapper.createObjectNode();
                    textPart.put("text", m.getContent() != null && !m.getContent().isBlank() ? m.getContent() : "");
                    contentArr.add(textPart);
                    msg.set("content", contentArr);
                } else {
                    String text = m.getContent();
                    msg.put("content", text != null ? text : "");
                }
                arr.add(msg);
            }
            ObjectNode input = body.putObject("input");
            input.set("messages", arr);

            // 始终使用 message 格式，确保返回 output.choices[0].message.content；避免纯文本时返回结构不同导致内容为空
            ObjectNode params = body.putObject("parameters");
            params.put("result_format", "message");

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey.trim());

            HttpEntity<String> entity = new HttpEntity<>(objectMapper.writeValueAsString(body), headers);
            ResponseEntity<String> resp = restTemplate.exchange(
                    BASE_URL + "/api/v1/services/aigc/multimodal-generation/generation",
                    HttpMethod.POST,
                    entity,
                    String.class
            );
            String bodyStr = resp.getBody();
            if (!resp.getStatusCode().is2xxSuccessful()) {
                String err = parseApiError(bodyStr);
                log.warn("Qwen API error {}: {}", resp.getStatusCode(), err);
                return ChatResult.error(err != null ? err : "通义 API 返回 " + resp.getStatusCode());
            }
            if (bodyStr == null || bodyStr.isBlank()) {
                log.warn("Qwen API returned empty body");
                return ChatResult.error("通义 API 返回为空");
            }
            JsonNode root = objectMapper.readTree(bodyStr);
            JsonNode output = root.path("output");
            if (output.isMissingNode()) {
                JsonNode errNode = root.path("message");
                String msg = errNode.isMissingNode() ? "响应中无 output" : errNode.asText("");
                log.warn("Qwen response missing output: {}", bodyStr.length() > 500 ? bodyStr.substring(0, 500) + "..." : bodyStr);
                return ChatResult.error(msg);
            }
            JsonNode choices = output.path("choices");
            String content = null;
            if (choices != null && choices.isArray() && choices.size() > 0) {
                JsonNode msg = choices.get(0).path("message");
                JsonNode c = msg.path("content");
                // DashScope message.content 可能是 string / array[{text,image,...}] / object
                if (c.isTextual()) {
                    content = c.asText(null);
                } else if (c.isArray()) {
                    StringBuilder sb = new StringBuilder();
                    for (JsonNode part : c) {
                        JsonNode t = part.path("text");
                        if (!t.isMissingNode() && t.isTextual() && !t.asText("").isBlank()) {
                            if (!sb.isEmpty()) sb.append("\n");
                            sb.append(t.asText());
                        }
                    }
                    content = sb.isEmpty() ? null : sb.toString();
                } else if (c.isObject()) {
                    JsonNode t = c.path("text");
                    if (!t.isMissingNode() && t.isTextual()) content = t.asText(null);
                } else {
                    content = c.asText(null);
                }
            }
            if (content == null || content.isBlank()) {
                JsonNode textNode = output.path("text");
                if (!textNode.isMissingNode()) content = textNode.asText(null);
            }
            if (content == null || content.isBlank()) {
                log.warn("Qwen response missing content: {}", bodyStr.length() > 500 ? bodyStr.substring(0, 500) + "..." : bodyStr);
                String snippet = bodyStr.length() > 300 ? bodyStr.substring(0, 300) + "..." : bodyStr;
                return ChatResult.error("通义 API 返回内容为空（响应片段: " + snippet + "）");
            }
            int inputTokens = 0, outputTokens = 0;
            JsonNode usage = root.path("usage");
            if (!usage.isMissingNode()) {
                inputTokens = usage.path("input_tokens").asInt(0);
                outputTokens = usage.path("output_tokens").asInt(0);
            }
            return new ChatResult(content, modelStr, null, inputTokens, outputTokens);
        } catch (Exception e) {
            log.warn("Qwen chat failed: {} - {}", e.getClass().getSimpleName(), e.getMessage(), e);
            return ChatResult.error(e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName());
        }
    }

    private String parseApiError(String body) {
        if (body == null || body.isBlank()) return null;
        try {
            JsonNode root = objectMapper.readTree(body);
            JsonNode msg = root.path("message");
            if (!msg.isMissingNode()) return msg.asText(null);
            JsonNode code = root.path("code");
            if (!code.isMissingNode()) return "code: " + code.asText();
        } catch (Exception ignored) { }
        return body.length() > 200 ? body.substring(0, 200) + "..." : body;
    }

    public static class ChatMessage {
        private final String role;
        private final String content;
        private final List<String> imageUrls;

        public ChatMessage(String role, String content) {
            this(role, content, null);
        }

        public ChatMessage(String role, String content, List<String> imageUrls) {
            this.role = role;
            this.content = content;
            this.imageUrls = imageUrls;
        }

        public String getRole() {
            return role;
        }

        public String getContent() {
            return content;
        }

        public List<String> getImageUrls() {
            return imageUrls;
        }
    }

    public static class ChatResult {
        private final String content;
        private final String model;
        private final String errorMessage;
        private final int inputTokens;
        private final int outputTokens;

        public ChatResult(String content, String model) {
            this(content, model, null, 0, 0);
        }

        public ChatResult(String content, String model, String errorMessage) {
            this(content, model, errorMessage, 0, 0);
        }

        public ChatResult(String content, String model, String errorMessage, int inputTokens, int outputTokens) {
            this.content = content;
            this.model = model;
            this.errorMessage = errorMessage;
            this.inputTokens = inputTokens;
            this.outputTokens = outputTokens;
        }

        public static ChatResult error(String message) {
            return new ChatResult(null, null, message, 0, 0);
        }

        public String getContent() { return content; }
        public String getModel() { return model; }
        public String getErrorMessage() { return errorMessage; }
        public int getInputTokens() { return inputTokens; }
        public int getOutputTokens() { return outputTokens; }
        public int getTotalTokens() { return inputTokens + outputTokens; }
        public boolean isError() {
            return errorMessage != null && !errorMessage.isBlank();
        }
    }
}

