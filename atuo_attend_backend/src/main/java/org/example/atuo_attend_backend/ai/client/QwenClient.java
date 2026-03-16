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

import java.util.List;

/**
 * 通义·千问多模态简单客户端。
 * 说明：具体 API 路径和字段以阿里云百炼最新文档为准，如有出入可在此处微调。
 */
@Component
public class QwenClient {

    private static final Logger log = LoggerFactory.getLogger(QwenClient.class);
    private static final String BASE_URL = "https://dashscope.aliyuncs.com";

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ChatResult chat(String apiKey, String model, List<ChatMessage> messages, boolean responseJson) {
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("Qwen API key is empty");
            return null;
        }
        try {
            String modelStr = (model != null && !model.isBlank()) ? model : "qwen-omni";
            ObjectNode body = objectMapper.createObjectNode();
            body.put("model", modelStr);

            ArrayNode arr = objectMapper.createArrayNode();
            for (ChatMessage m : messages) {
                ObjectNode msg = objectMapper.createObjectNode();
                msg.put("role", m.getRole());
                if (m.getImageUrls() != null && !m.getImageUrls().isEmpty()) {
                    // 多模态：组合文本 + 图片 URL
                    ArrayNode contentArr = objectMapper.createArrayNode();
                    if (m.getContent() != null && !m.getContent().isBlank()) {
                        ObjectNode textPart = objectMapper.createObjectNode();
                        textPart.put("type", "text");
                        textPart.put("text", m.getContent());
                        contentArr.add(textPart);
                    }
                    for (String url : m.getImageUrls()) {
                        if (url == null || url.isBlank()) continue;
                        ObjectNode imgPart = objectMapper.createObjectNode();
                        imgPart.put("type", "image_url");
                        ObjectNode imgUrl = objectMapper.createObjectNode();
                        imgUrl.put("url", url);
                        imgPart.set("image_url", imgUrl);
                        contentArr.add(imgPart);
                    }
                    msg.set("content", contentArr);
                } else {
                    msg.put("content", m.getContent());
                }
                arr.add(msg);
            }
            body.set("messages", arr);

            if (responseJson) {
                ObjectNode fmt = body.putObject("response_format");
                fmt.put("type", "json_object");
            }

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
            if (resp.getStatusCode().is2xxSuccessful() && resp.getBody() != null) {
                JsonNode root = objectMapper.readTree(resp.getBody());
                JsonNode output = root.path("output");
                JsonNode choices = output.path("choices");
                if (choices != null && choices.isArray() && choices.size() > 0) {
                    JsonNode msg = choices.get(0).path("message");
                    String content = msg.path("content").asText(null);
                    return new ChatResult(content, modelStr);
                }
            }
        } catch (Exception e) {
            log.warn("Qwen chat failed: {} - {}", e.getClass().getSimpleName(), e.getMessage());
        }
        return null;
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

        public ChatResult(String content, String model) {
            this.content = content;
            this.model = model;
        }

        public String getContent() {
            return content;
        }

        public String getModel() {
            return model;
        }
    }
}

