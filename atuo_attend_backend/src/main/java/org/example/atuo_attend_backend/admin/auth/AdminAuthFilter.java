package org.example.atuo_attend_backend.admin.auth;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.atuo_attend_backend.tenant.domain.AdminSession;
import org.example.atuo_attend_backend.tenant.domain.Tenant;
import org.example.atuo_attend_backend.tenant.domain.TenantAdminUser;
import org.example.atuo_attend_backend.tenant.context.TenantContext;
import org.example.atuo_attend_backend.tenant.mapper.AdminSessionMapper;
import org.example.atuo_attend_backend.tenant.mapper.TenantAdminUserMapper;
import org.example.atuo_attend_backend.tenant.mapper.TenantMapper;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 校验 /api/admin/* 的 Bearer Token（aa_admin_session），白名单：登录、注册。
 */
@Component
public class AdminAuthFilter implements Filter {

    public static final String ATTR_USER_ID = "adminUserId";
    public static final String ATTR_TENANT_ID = "adminTenantId";
    public static final String ATTR_PHONE = "adminPhone";

    private final AdminSessionMapper adminSessionMapper;
    private final TenantAdminUserMapper tenantAdminUserMapper;
    private final TenantMapper tenantMapper;

    public AdminAuthFilter(AdminSessionMapper adminSessionMapper,
                           TenantAdminUserMapper tenantAdminUserMapper,
                           TenantMapper tenantMapper) {
        this.adminSessionMapper = adminSessionMapper;
        this.tenantAdminUserMapper = tenantAdminUserMapper;
        this.tenantMapper = tenantMapper;
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

        boolean anonymous = uri.equals("/api/admin/auth/login") && "POST".equalsIgnoreCase(req.getMethod())
                || uri.equals("/api/admin/auth/register") && "POST".equalsIgnoreCase(req.getMethod())
                || uri.equals("/api/admin/auth/sms/config") && "GET".equalsIgnoreCase(req.getMethod())
                || uri.equals("/api/admin/auth/sms/send") && "POST".equalsIgnoreCase(req.getMethod());

        if (anonymous) {
            chain.doFilter(request, response);
            return;
        }

        if (!uri.startsWith("/api/admin/")) {
            chain.doFilter(request, response);
            return;
        }

        // 头像用于 <img src="...">：浏览器图片请求无法携带 Authorization 头
        // 因此对 GET /api/admin/team/avatar 做白名单放行；MinIO key 仍在接口内校验 avatars/ 前缀。
        if ("/api/admin/team/avatar".equals(uri) && "GET".equalsIgnoreCase(req.getMethod())) {
            chain.doFilter(request, response);
            return;
        }

        String auth = req.getHeader("Authorization");
        String token = null;
        if (auth != null && auth.startsWith("Bearer ")) {
            token = auth.substring(7).trim();
        }
        if (token == null || token.isEmpty()) {
            writeUnauthorized(resp);
            return;
        }

        AdminSession session = adminSessionMapper.findValidByToken(token);
        if (session == null) {
            writeUnauthorized(resp);
            return;
        }

        Tenant tenant = tenantMapper.findById(session.getTenantId());
        if (tenant != null && "suspended".equalsIgnoreCase(tenant.getStatus())) {
            writeForbidden(resp);
            return;
        }

        req.setAttribute(ATTR_USER_ID, session.getUserId());
        req.setAttribute(ATTR_TENANT_ID, session.getTenantId());
        TenantAdminUser tau = tenantAdminUserMapper.findById(session.getUserId());
        if (tau != null) {
            req.setAttribute(ATTR_PHONE, tau.getPhone());
        }

        TenantContext.setTenantId(session.getTenantId());

        chain.doFilter(request, response);
    }

    private void writeUnauthorized(HttpServletResponse resp) throws IOException {
        resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        resp.setContentType("application/json;charset=UTF-8");
        resp.getWriter().write("{\"code\":40101,\"message\":\"未登录或登录已过期\"}");
    }

    private void writeForbidden(HttpServletResponse resp) throws IOException {
        resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
        resp.setContentType("application/json;charset=UTF-8");
        resp.getWriter().write("{\"code\":40301,\"message\":\"组织已暂停服务，请联系平台支持\"}");
    }
}
