package org.example.atuo_attend_backend.platform.auth;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.atuo_attend_backend.platform.domain.PlatformSession;
import org.example.atuo_attend_backend.platform.mapper.PlatformSessionMapper;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class PlatformAuthFilter implements Filter {

    private final PlatformSessionMapper platformSessionMapper;

    public PlatformAuthFilter(PlatformSessionMapper platformSessionMapper) {
        this.platformSessionMapper = platformSessionMapper;
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

        if (uri.equals("/api/platform/auth/login") && "POST".equalsIgnoreCase(req.getMethod())) {
            chain.doFilter(request, response);
            return;
        }

        if (!uri.startsWith("/api/platform/")) {
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

        PlatformSession session = platformSessionMapper.findValidByToken(token);
        if (session == null) {
            writeUnauthorized(resp);
            return;
        }

        chain.doFilter(request, response);
    }

    private void writeUnauthorized(HttpServletResponse resp) throws IOException {
        resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        resp.setContentType("application/json;charset=UTF-8");
        resp.getWriter().write("{\"code\":40101,\"message\":\"未登录或登录已过期\"}");
    }
}
