package org.example.atuo_attend_backend.tenant.web;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import org.example.atuo_attend_backend.tenant.context.TenantContext;
import java.io.IOException;

/**
 * 请求链最外层在 finally 中清理 {@link TenantContext}，避免线程池复用导致串租。
 * 由 {@link TenantWebConfig} 注册为最高优先级、映射 {@code /*}。
 */
public class TenantClearFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        try {
            chain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }
}
