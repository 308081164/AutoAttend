package org.example.atuo_attend_backend.collab.auth;

import org.example.atuo_attend_backend.collab.service.CollabJwtService;
import org.springframework.stereotype.Component;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 对 /api/collab/* 请求校验 JWT，将 userId 放入 request 属性 "collabUserId"。
 * 登录、注册等白名单路径在 Controller 层放行（不挂在此 Filter 的 urlPattern 内的可匿名访问）。
 */
@Component
public class CollabAuthFilter implements Filter {

    private final CollabJwtService jwtService;

    public CollabAuthFilter(CollabJwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;

        String path = req.getRequestURI();
        boolean isLogin = "POST".equalsIgnoreCase(req.getMethod()) && path != null && path.contains("/auth/login");

        String auth = req.getHeader("Authorization");
        String token = null;
        if (auth != null && auth.startsWith("Bearer ")) {
            token = auth.substring(7).trim();
        }
        Long userId = token != null ? jwtService.getUserIdFromToken(token) : null;
        if (userId != null) {
            req.setAttribute("collabUserId", userId);
        } else if (!isLogin) {
            resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            resp.setContentType("application/json;charset=UTF-8");
            resp.getWriter().write("{\"code\":40101,\"message\":\"未登录或登录已过期\"}");
            return;
        }

        chain.doFilter(request, response);
    }
}
