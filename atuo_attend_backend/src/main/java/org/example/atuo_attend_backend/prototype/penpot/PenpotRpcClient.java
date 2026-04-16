package org.example.atuo_attend_backend.prototype.penpot;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.atuo_attend_backend.prototype.config.PenpotProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * 调用 Penpot HTTP RPC（/api/rpc/command/:method），支持 Access Token 或 Cookie 会话。
 */
@Component
public class PenpotRpcClient {

    private static final Logger log = LoggerFactory.getLogger(PenpotRpcClient.class);

    private final PenpotProperties props;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /** 缓存 Cookie：login-with-password 成功后复用 */
    private volatile String sessionCookie;

    public PenpotRpcClient(PenpotProperties props, RestTemplate restTemplate) {
        this.props = props;
        this.restTemplate = restTemplate;
    }

    public boolean isConfigured() {
        if (!props.isEnabled()) {
            return false;
        }
        if (StringUtils.hasText(props.getAccessToken())) {
            return true;
        }
        return StringUtils.hasText(props.getEmail()) && StringUtils.hasText(props.getPassword());
    }

    public JsonNode command(String methodName, Map<String, Object> body) {
        ensureSession();
        String url = trimSlash(props.getBaseUrl()) + "/api/rpc/command/" + methodName;
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        applyAuth(headers);

        String jsonBody = body == null || body.isEmpty() ? "{}" : safeWriteJson(body);
        HttpEntity<String> entity = new HttpEntity<>(jsonBody, headers);
        try {
            ResponseEntity<String> resp = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
            String raw = resp.getBody();
            if (raw == null || raw.isBlank()) {
                throw new IllegalStateException("Penpot 返回空响应: " + methodName);
            }
            return objectMapper.readTree(raw);
        } catch (HttpStatusCodeException e) {
            String hint = e.getResponseBodyAsString();
            log.warn("Penpot RPC {} failed: {} body={}", methodName, e.getStatusCode(), truncate(hint, 800));
            throw new IllegalStateException("Penpot RPC 失败: " + methodName + " HTTP " + e.getStatusCode().value()
                    + (hint != null && !hint.isBlank() ? (": " + truncate(hint, 500)) : ""));
        } catch (Exception e) {
            if (e instanceof IllegalStateException) throw (IllegalStateException) e;
            throw new IllegalStateException("Penpot RPC 异常: " + methodName + ": " + e.getMessage(), e);
        }
    }

    private void ensureSession() {
        if (StringUtils.hasText(props.getAccessToken())) {
            return;
        }
        if (sessionCookie != null) {
            return;
        }
        synchronized (this) {
            if (sessionCookie != null) {
                return;
            }
            if (!StringUtils.hasText(props.getEmail()) || !StringUtils.hasText(props.getPassword())) {
                throw new IllegalStateException("Penpot 未配置：请设置 app.penpot.access-token，或同时设置 email 与 password");
            }
            loginPassword();
        }
    }

    private void loginPassword() {
        String url = trimSlash(props.getBaseUrl()) + "/api/rpc/command/login-with-password";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        Map<String, Object> body = new HashMap<>();
        body.put("email", props.getEmail().trim());
        body.put("password", props.getPassword());
        HttpEntity<String> entity = new HttpEntity<>(safeWriteJson(body), headers);
        try {
            ResponseEntity<String> resp = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
            String cookie = resp.getHeaders().getFirst(HttpHeaders.SET_COOKIE);
            if (cookie == null || !cookie.contains("auth-token=")) {
                throw new IllegalStateException("Penpot 登录未返回 auth-token Cookie，请检查账号或 PENPOT_PUBLIC_URI");
            }
            // 只取第一条 Cookie（auth-token=...; Path=...）
            String part = cookie.split(";", 2)[0].trim();
            this.sessionCookie = part;
        } catch (HttpStatusCodeException e) {
            String hint = e.getResponseBodyAsString();
            throw new IllegalStateException("Penpot 登录失败 HTTP " + e.getStatusCode().value()
                    + (hint != null ? (": " + truncate(hint, 400)) : ""));
        }
    }

    private void applyAuth(HttpHeaders headers) {
        if (StringUtils.hasText(props.getAccessToken())) {
            headers.set(HttpHeaders.AUTHORIZATION, "Token " + props.getAccessToken().trim());
            return;
        }
        if (sessionCookie != null) {
            headers.add(HttpHeaders.COOKIE, sessionCookie);
        }
    }

    private static String trimSlash(String u) {
        if (u == null) return "";
        return u.endsWith("/") ? u.substring(0, u.length() - 1) : u;
    }

    private String safeWriteJson(Map<String, Object> body) {
        try {
            return objectMapper.writeValueAsString(body);
        } catch (Exception e) {
            throw new IllegalStateException("JSON 序列化失败", e);
        }
    }

    private static String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max) + "…";
    }
}
