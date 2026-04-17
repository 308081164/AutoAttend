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
 * 调用 Penpot HTTP RPC（/api/rpc/command/:method）。
 * 支持：平台级 Access Token / 密码会话；租户级 Access Token（按请求传入）；以及无认证注册流程。
 */
@Component
public class PenpotRpcClient {

    private static final Logger log = LoggerFactory.getLogger(PenpotRpcClient.class);

    private final PenpotProperties props;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /** 平台账号：login-with-password 成功后复用 */
    private volatile String platformSessionCookie;

    public PenpotRpcClient(PenpotProperties props, RestTemplate restTemplate) {
        this.props = props;
        this.restTemplate = restTemplate;
    }

    public boolean hasPlatformCredentials() {
        if (!props.isEnabled()) {
            return false;
        }
        if (StringUtils.hasText(props.getAccessToken())) {
            return true;
        }
        return StringUtils.hasText(props.getEmail()) && StringUtils.hasText(props.getPassword());
    }

    /**
     * @deprecated 使用 {@link #hasPlatformCredentials()}；保留旧调用处兼容。
     */
    @Deprecated
    public boolean isConfigured() {
        return hasPlatformCredentials();
    }

    /**
     * 部分 RPC（如 export-binfile）返回 JSON 字符串字面量。
     */
    public String commandForStringResult(String methodName, Map<String, Object> body) {
        return commandForStringResult(methodName, body, null);
    }

    public String commandForStringResult(String methodName, Map<String, Object> body, String tenantAccessToken) {
        JsonNode n = command(methodName, body, tenantAccessToken);
        if (n == null || n.isNull()) {
            return null;
        }
        if (n.isTextual()) {
            return n.asText();
        }
        return n.toString();
    }

    public JsonNode command(String methodName, Map<String, Object> body) {
        return command(methodName, body, null);
    }

    /**
     * @param tenantAccessToken 非空时以租户 Token 调用；为空时使用平台凭据。
     */
    public JsonNode command(String methodName, Map<String, Object> body, String tenantAccessToken) {
        ensurePlatformSession(tenantAccessToken);
        String url = trimSlash(props.getEffectiveRpcBaseUrl()) + "/api/rpc/command/" + methodName;
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        applyAuth(headers, tenantAccessToken);

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

    /** 无需登录的 RPC（如 prepare-register-profile）。 */
    public JsonNode commandNoAuth(String methodName, Map<String, Object> body) {
        String url = trimSlash(props.getEffectiveRpcBaseUrl()) + "/api/rpc/command/" + methodName;
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
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
            throw new IllegalStateException("Penpot RPC 失败: " + methodName + " HTTP " + e.getStatusCode().value()
                    + (hint != null && !hint.isBlank() ? (": " + truncate(hint, 500)) : ""));
        } catch (Exception e) {
            if (e instanceof IllegalStateException) throw (IllegalStateException) e;
            throw new IllegalStateException("Penpot RPC 异常: " + methodName + ": " + e.getMessage(), e);
        }
    }

    /**
     * 使用 Cookie 会话调用（如 create-access-token）。
     * @param cookieHeader 形如 auth-token=...; Path=... 的首段或完整 Cookie
     */
    public JsonNode commandWithCookie(String methodName, Map<String, Object> body, String cookieHeader) {
        String url = trimSlash(props.getEffectiveRpcBaseUrl()) + "/api/rpc/command/" + methodName;
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        if (StringUtils.hasText(cookieHeader)) {
            headers.add(HttpHeaders.COOKIE, cookieHeader);
        }
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
            throw new IllegalStateException("Penpot RPC 失败: " + methodName + " HTTP " + e.getStatusCode().value()
                    + (hint != null && !hint.isBlank() ? (": " + truncate(hint, 500)) : ""));
        } catch (Exception e) {
            if (e instanceof IllegalStateException) throw (IllegalStateException) e;
            throw new IllegalStateException("Penpot RPC 异常: " + methodName + ": " + e.getMessage(), e);
        }
    }

    /**
     * 登录并返回 Set-Cookie 中的 auth-token 段（供后续 RPC 使用）。
     */
    public String loginFetchAuthCookie(String email, String password) {
        String url = trimSlash(props.getEffectiveRpcBaseUrl()) + "/api/rpc/command/login-with-password";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        Map<String, Object> body = new HashMap<>();
        body.put("email", email.trim());
        body.put("password", password);
        HttpEntity<String> entity = new HttpEntity<>(safeWriteJson(body), headers);
        try {
            ResponseEntity<String> resp = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
            String cookie = resp.getHeaders().getFirst(HttpHeaders.SET_COOKIE);
            if (cookie == null || !cookie.contains("auth-token=")) {
                throw new IllegalStateException("Penpot 登录未返回 auth-token Cookie，请检查账号或 PENPOT_PUBLIC_URI");
            }
            return cookie.split(";", 2)[0].trim();
        } catch (HttpStatusCodeException e) {
            String hint = e.getResponseBodyAsString();
            throw new IllegalStateException("Penpot 登录失败 HTTP " + e.getStatusCode().value()
                    + (hint != null ? (": " + truncate(hint, 400)) : ""));
        }
    }

    private void ensurePlatformSession(String tenantAccessToken) {
        if (StringUtils.hasText(tenantAccessToken)) {
            return;
        }
        if (StringUtils.hasText(props.getAccessToken())) {
            return;
        }
        if (platformSessionCookie != null) {
            return;
        }
        synchronized (this) {
            if (platformSessionCookie != null) {
                return;
            }
            if (!StringUtils.hasText(props.getEmail()) || !StringUtils.hasText(props.getPassword())) {
                throw new IllegalStateException("Penpot 未配置：请设置租户自动开户，或配置 app.penpot.access-token，或同时设置 email 与 password");
            }
            platformSessionCookie = loginFetchAuthCookie(props.getEmail(), props.getPassword());
        }
    }

    private void applyAuth(HttpHeaders headers, String tenantAccessToken) {
        if (StringUtils.hasText(tenantAccessToken)) {
            headers.set(HttpHeaders.AUTHORIZATION, "Token " + tenantAccessToken.trim());
            return;
        }
        if (StringUtils.hasText(props.getAccessToken())) {
            headers.set(HttpHeaders.AUTHORIZATION, "Token " + props.getAccessToken().trim());
            return;
        }
        if (platformSessionCookie != null) {
            headers.add(HttpHeaders.COOKIE, platformSessionCookie);
        }
    }

    private String safeWriteJson(Map<String, Object> body) {
        try {
            return objectMapper.writeValueAsString(body);
        } catch (Exception e) {
            throw new IllegalStateException("JSON 序列化失败", e);
        }
    }

    private static String trimSlash(String u) {
        if (u == null) return "";
        return u.endsWith("/") ? u.substring(0, u.length() - 1) : u;
    }

    private static String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max) + "…";
    }
}
