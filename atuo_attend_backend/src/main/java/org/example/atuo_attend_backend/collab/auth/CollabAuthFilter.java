package org.example.atuo_attend_backend.collab.auth;

import org.example.atuo_attend_backend.collab.domain.BizUser;
import org.example.atuo_attend_backend.collab.mapper.BizUserMapper;
import org.example.atuo_attend_backend.collab.service.CollabJwtService;
import org.example.atuo_attend_backend.tenant.context.TenantContext;
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

    public static final String ATTR_COLLAB_USER_ID = "collabUserId";
    public static final String ATTR_COLLAB_JWT_MODE = "collabJwtMode";
    public static final String ATTR_COLLAB_PROJECT_SCOPE = "collabProjectScope";

    private static final String ROLE_SUPER_ADMIN = "super_admin";

    private final CollabJwtService jwtService;
    private final BizUserMapper bizUserMapper;

    public CollabAuthFilter(CollabJwtService jwtService, BizUserMapper bizUserMapper) {
        this.jwtService = jwtService;
        this.bizUserMapper = bizUserMapper;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;

        String path = req.getRequestURI();
        boolean isAnonymousAuth = "POST".equalsIgnoreCase(req.getMethod()) && path != null
                && (path.contains("/auth/login") || path.contains("/auth/register-invite")
                || path.contains("/auth/sms/send-login") || path.contains("/auth/sms/config"));

        String auth = req.getHeader("Authorization");
        String token = null;
        if (auth != null && auth.startsWith("Bearer ")) {
            token = auth.substring(7).trim();
        }
        Long userId = token != null ? jwtService.getUserIdFromToken(token) : null;
        if (userId != null) {
            req.setAttribute(ATTR_COLLAB_USER_ID, userId);
            String mode = token != null ? jwtService.getModeFromToken(token) : null;
            String projectScope = token != null ? jwtService.getProjectScopeFromToken(token) : null;
            req.setAttribute(ATTR_COLLAB_JWT_MODE, mode);
            req.setAttribute(ATTR_COLLAB_PROJECT_SCOPE, projectScope);
            BizUser u = bizUserMapper.findById(userId);
            if (u != null) {
                boolean useTenantContext = shouldSetTenantContext(mode, projectScope, u);
                if (useTenantContext && u.getTenantId() != null) {
                    TenantContext.setTenantId(u.getTenantId());
                } else {
                    TenantContext.clear();
                }
            }
        } else if (!isAnonymousAuth) {
            resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            resp.setContentType("application/json;charset=UTF-8");
            resp.getWriter().write("{\"code\":40101,\"message\":\"未登录或登录已过期\"}");
            return;
        }

        chain.doFilter(request, response);
    }

    private static boolean shouldSetTenantContext(String mode, String projectScope, BizUser u) {
        if (CollabJwtService.PROJECT_SCOPE_ALL.equals(projectScope)) {
            return false;
        }
        if (CollabJwtService.JWT_MODE_ADMIN.equals(mode)) {
            return true;
        }
        if (CollabJwtService.PROJECT_SCOPE_TENANT.equals(projectScope)) {
            return true;
        }
        // 旧 JWT 无 mode/scope：超级管理员按单租户；普通成员不设置（跨租户列表）
        return mode == null && projectScope == null && ROLE_SUPER_ADMIN.equals(u.getRole());
    }

    public static long requireCollabUserId(HttpServletRequest req) {
        Object v = req.getAttribute(ATTR_COLLAB_USER_ID);
        if (!(v instanceof Long)) {
            throw new IllegalStateException("unauthorized");
        }
        return (Long) v;
    }

    public static String projectScopeFrom(HttpServletRequest req) {
        Object v = req.getAttribute(ATTR_COLLAB_PROJECT_SCOPE);
        return v != null ? v.toString() : null;
    }
}
