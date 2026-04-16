package org.example.atuo_attend_backend.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 对携带 X-Client-Version 的 /api/* 请求校验平台壳版本策略；纯 Web 浏览器无该头则不拦截。
 */
public class ClientShellEnforcementFilter implements Filter {

    public static final String HEADER_VERSION = "X-Client-Version";
    public static final String HEADER_BUILD = "X-Client-Build";
    public static final String HEADER_PLATFORM = "X-Client-Platform";

    private final ClientVersionPolicyService policyService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ClientShellEnforcementFilter(ClientVersionPolicyService policyService) {
        this.policyService = policyService;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;

        if ("OPTIONS".equalsIgnoreCase(req.getMethod())) {
            chain.doFilter(request, response);
            return;
        }

        String uri = req.getRequestURI();
        String ctx = req.getContextPath();
        if (ctx != null && !ctx.isEmpty() && uri.startsWith(ctx)) {
            uri = uri.substring(ctx.length());
        }

        if (!uri.startsWith("/api/")) {
            chain.doFilter(request, response);
            return;
        }
        if (uri.startsWith("/api/public/")) {
            chain.doFilter(request, response);
            return;
        }

        String ver = req.getHeader(HEADER_VERSION);
        if (ver == null || ver.isBlank()) {
            chain.doFilter(request, response);
            return;
        }

        Integer build = null;
        String b = req.getHeader(HEADER_BUILD);
        if (b != null && !b.isBlank()) {
            try {
                build = Integer.parseInt(b.trim());
            } catch (NumberFormatException ignored) {
                build = null;
            }
        }

        ClientVersionPolicyService.BlockResult block = policyService.evaluate(ver.trim(), build);
        if (block == null) {
            chain.doFilter(request, response);
            return;
        }

        resp.setStatus(426);
        resp.setCharacterEncoding(StandardCharsets.UTF_8.name());
        resp.setContentType("application/json;charset=UTF-8");
        var node = objectMapper.createObjectNode();
        node.put("code", 42601);
        node.put("message", block.getMessage() != null ? block.getMessage() : "客户端版本不可用");
        if (block.getUpgradeUrl() != null && !block.getUpgradeUrl().isBlank()) {
            node.put("upgradeUrl", block.getUpgradeUrl());
        }
        resp.getWriter().write(objectMapper.writeValueAsString(node));
    }
}
