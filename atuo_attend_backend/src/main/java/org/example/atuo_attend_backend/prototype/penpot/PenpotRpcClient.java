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
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import jakarta.annotation.PostConstruct;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * 调用 Penpot HTTP RPC。
 * Penpot 2.12+ 使用 {@code /api/main/methods/:method}；旧版为 {@code /api/rpc/command/:method}。
 * {@code app.penpot.rpc-path-style=auto} 时：启动时探测路径并锁定；请求时遇「路径不存在」类状态码再尝试另一路径。
 */
@Component
public class PenpotRpcClient {

    private static final Logger log = LoggerFactory.getLogger(PenpotRpcClient.class);

    private final PenpotProperties props;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /** 平台账号：login-with-password 成功后复用 */
    private volatile String platformSessionCookie;

    /** auto 模式下解析出的路径：new = /api/main/methods，legacy = /api/rpc/command */
    private volatile String autoResolvedRpcKind;

    public PenpotRpcClient(PenpotProperties props, RestTemplate restTemplate) {
        this.props = props;
        this.restTemplate = restTemplate;
    }

    /**
     * 启动时探测 Penpot 版本对应 RPC 前缀，避免首请求才回退、也减少错误日志。
     * 使用无需鉴权即可区分「路由存在」的轻量 RPC（get-teams 未登录通常返回 401/403，而非 404）。
     */
    @PostConstruct
    public void probeAndLockRpcPathOnStartup() {
        if (!props.isEnabled() || !isRpcAutoMode()) {
            return;
        }
        String base = trimSlash(props.getEffectiveRpcBaseUrl());
        if (!StringUtils.hasText(base)) {
            return;
        }
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        h.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        applyPenpotClientHeader(h);
        HttpEntity<String> entity = new HttpEntity<>("{}", h);
        String[] kinds = new String[] { "new", "legacy" };
        for (String kind : kinds) {
            String url = "new".equals(kind)
                    ? base + "/api/main/methods/get-teams"
                    : base + "/api/rpc/command/get-teams";
            try {
                restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
                noteSuccessFromKind(kind);
                log.info("Penpot RPC path probe: locked to {} ({} responded without 404)", kind, url);
                return;
            } catch (HttpStatusCodeException e) {
                int code = e.getStatusCode().value();
                if (code == 404 || code == 405) {
                    log.debug("Penpot RPC probe {} -> HTTP {} (try next path)", url, code);
                    continue;
                }
                noteSuccessFromKind(kind);
                log.info("Penpot RPC path probe: locked to {} ({} -> HTTP {}, route exists)", kind, url, code);
                return;
            } catch (RestClientException e) {
                log.warn("Penpot RPC path probe network error for {}: {}", url, e.getMessage());
                return;
            }
        }
        log.warn("Penpot RPC path probe: could not distinguish new vs legacy (Penpot down or unreachable); will try both on first RPC");
    }

    private void noteSuccessFromKind(String kind) {
        if (!isRpcAutoMode()) {
            return;
        }
        synchronized (this) {
            if (autoResolvedRpcKind != null) {
                return;
            }
            autoResolvedRpcKind = "new".equals(kind) ? "new" : "legacy";
        }
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
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        applyPenpotClientHeader(headers);
        applyAuth(headers, tenantAccessToken);
        String jsonBody = body == null || body.isEmpty() ? "{}" : safeWriteJson(body);
        return postCommand(methodName, headers, jsonBody, true);
    }

    /** 无需登录的 RPC（如 prepare-register-profile）。 */
    public JsonNode commandNoAuth(String methodName, Map<String, Object> body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        applyPenpotClientHeader(headers);
        String jsonBody = body == null || body.isEmpty() ? "{}" : safeWriteJson(body);
        return postCommand(methodName, headers, jsonBody, false);
    }

    /**
     * 使用 Cookie 会话调用（如 create-access-token）。
     * @param cookieHeader 形如 auth-token=...; Path=... 的首段或完整 Cookie
     */
    public JsonNode commandWithCookie(String methodName, Map<String, Object> body, String cookieHeader) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        applyPenpotClientHeader(headers);
        if (StringUtils.hasText(cookieHeader)) {
            headers.add(HttpHeaders.COOKIE, cookieHeader);
        }
        String jsonBody = body == null || body.isEmpty() ? "{}" : safeWriteJson(body);
        return postCommand(methodName, headers, jsonBody, false);
    }

    /**
     * 登录并返回 Set-Cookie 中的 auth-token 段（供后续 RPC 使用）。
     */
    public String loginFetchAuthCookie(String email, String password) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        applyPenpotClientHeader(headers);
        Map<String, Object> body = new HashMap<>();
        body.put("email", email.trim());
        body.put("password", password);
        String jsonBody = safeWriteJson(body);
        HttpEntity<String> entity = new HttpEntity<>(jsonBody, headers);
        String base = trimSlash(props.getEffectiveRpcBaseUrl());
        String[] tryUrls = loginTryUrls(base);
        HttpStatusCodeException last = null;
        for (String url : tryUrls) {
            try {
                ResponseEntity<String> resp = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
                String cookie = resp.getHeaders().getFirst(HttpHeaders.SET_COOKIE);
                if (cookie == null || !cookie.contains("auth-token=")) {
                    throw new IllegalStateException("Penpot 登录未返回 auth-token Cookie，请检查账号或 PENPOT_PUBLIC_URI");
                }
                noteSuccessFromUrl(url);
                return cookie.split(";", 2)[0].trim();
            } catch (HttpStatusCodeException e) {
                last = e;
                if (!shouldTryAlternateRpcPath(e) || !isRpcAutoMode() || autoResolvedRpcKind != null) {
                    break;
                }
            }
        }
        String hint = last != null ? last.getResponseBodyAsString() : "";
        throw new IllegalStateException("Penpot 登录失败 HTTP "
                + (last != null ? last.getStatusCode().value() : "?")
                + (hint != null && !hint.isBlank() ? (": " + truncate(hint, 400)) : ""));
    }

    /** 登录在 auto 下先试新路径再试旧路径（与 postCommand 一致）。 */
    private String[] loginTryUrls(String base) {
        String style = normalizedRpcPathStyle();
        if ("legacy".equals(style)) {
            return new String[] { base + "/api/rpc/command/login-with-password" };
        }
        if ("new".equals(style)) {
            return new String[] { base + "/api/main/methods/login-with-password" };
        }
        if ("legacy".equals(autoResolvedRpcKind)) {
            return new String[] { base + "/api/rpc/command/login-with-password" };
        }
        if ("new".equals(autoResolvedRpcKind)) {
            return new String[] { base + "/api/main/methods/login-with-password" };
        }
        return new String[] {
                base + "/api/main/methods/login-with-password",
                base + "/api/rpc/command/login-with-password"
        };
    }

    private JsonNode postCommand(String methodName, HttpHeaders headers, String jsonBody, boolean noteSuccess) {
        HttpEntity<String> entity = new HttpEntity<>(jsonBody, headers);
        String base = trimSlash(props.getEffectiveRpcBaseUrl());
        String[] urls = rpcTryUrls(base, methodName);
        HttpStatusCodeException last = null;
        for (String url : urls) {
            try {
                ResponseEntity<String> resp = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
                if (noteSuccess) {
                    noteSuccessFromUrl(url);
                }
                return parseBody(methodName, resp.getBody());
            } catch (HttpStatusCodeException e) {
                last = e;
                if (!shouldTryAlternateRpcPath(e) || !isRpcAutoMode() || autoResolvedRpcKind != null) {
                    log.warn("Penpot RPC {} failed: {} body={}", methodName, e.getStatusCode(), truncate(e.getResponseBodyAsString(), 800));
                    throw rpcException(methodName, e);
                }
            } catch (IllegalStateException ex) {
                throw ex;
            } catch (Exception e) {
                throw new IllegalStateException("Penpot RPC 异常: " + methodName + ": " + e.getMessage(), e);
            }
        }
        if (last != null) {
            log.warn("Penpot RPC {} all paths failed, last: {} body={}", methodName, last.getStatusCode(),
                    truncate(last.getResponseBodyAsString(), 800));
            throw rpcException(methodName, last);
        }
        throw new IllegalStateException("Penpot RPC 失败: " + methodName);
    }

    private String[] rpcTryUrls(String base, String methodName) {
        String style = normalizedRpcPathStyle();
        if ("legacy".equals(style)) {
            return new String[] { base + "/api/rpc/command/" + methodName };
        }
        if ("new".equals(style)) {
            return new String[] { base + "/api/main/methods/" + methodName };
        }
        if ("legacy".equals(autoResolvedRpcKind)) {
            return new String[] { base + "/api/rpc/command/" + methodName };
        }
        if ("new".equals(autoResolvedRpcKind)) {
            return new String[] { base + "/api/main/methods/" + methodName };
        }
        return new String[] {
                base + "/api/main/methods/" + methodName,
                base + "/api/rpc/command/" + methodName
        };
    }

    private JsonNode parseBody(String methodName, String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalStateException("Penpot 返回空响应: " + methodName);
        }
        try {
            return objectMapper.readTree(raw);
        } catch (Exception e) {
            throw new IllegalStateException("Penpot JSON 解析失败: " + methodName, e);
        }
    }

    private static IllegalStateException rpcException(String methodName, HttpStatusCodeException e) {
        String hint = e.getResponseBodyAsString();
        return new IllegalStateException("Penpot RPC 失败: " + methodName + " HTTP " + e.getStatusCode().value()
                + (hint != null && !hint.isBlank() ? (": " + truncate(hint, 500)) : ""));
    }

    private void noteSuccessFromUrl(String url) {
        if (!isRpcAutoMode()) {
            return;
        }
        synchronized (this) {
            if (autoResolvedRpcKind != null) {
                return;
            }
            autoResolvedRpcKind = url != null && url.contains("/api/main/methods/") ? "new" : "legacy";
            log.info("Penpot RPC path locked to {} (first successful request)", autoResolvedRpcKind);
        }
    }

    /**
     * 路径错误时常为 404/405；网关/瞬时故障可能为 502/503/504，可尝试另一套路径（兼容反代配置差异）。
     */
    private static boolean shouldTryAlternateRpcPath(HttpStatusCodeException e) {
        int code = e.getStatusCode().value();
        return code == 404 || code == 405 || code == 502 || code == 503 || code == 504;
    }

    private boolean isRpcAutoMode() {
        return "auto".equals(normalizedRpcPathStyle());
    }

    private String normalizedRpcPathStyle() {
        String s = props.getRpcPathStyle();
        if (s == null || s.isBlank()) {
            return "auto";
        }
        String t = s.trim().toLowerCase();
        if ("v212".equals(t) || "main".equals(t)) {
            return "new";
        }
        if ("old".equals(t)) {
            return "legacy";
        }
        return t;
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

    /**
     * Penpot {@code sec/client-header-check}：无此头则拒绝 RPC（浏览器端由官方前端注入 {@code x-client}）。
     */
    private void applyPenpotClientHeader(HttpHeaders headers) {
        String v = props.getClientHeader();
        if (StringUtils.hasText(v)) {
            headers.set("x-client", v.trim());
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
