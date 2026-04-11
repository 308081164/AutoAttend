package org.example.atuo_attend_backend.tenant.context;

import java.util.function.Supplier;

/**
 * 当前请求/线程绑定的租户 ID，由 Filter 在业务处理前设置，请求结束由 {@link org.example.atuo_attend_backend.tenant.web.TenantClearFilter} 清理。
 */
public final class TenantContext {

    private static final ThreadLocal<Long> CURRENT = new ThreadLocal<>();

    private TenantContext() {
    }

    public static void setTenantId(Long tenantId) {
        if (tenantId == null) {
            CURRENT.remove();
        } else {
            CURRENT.set(tenantId);
        }
    }

    public static Long getTenantId() {
        return CURRENT.get();
    }

    public static long getTenantIdOrDefault(long defaultTenantId) {
        Long v = CURRENT.get();
        return v != null ? v : defaultTenantId;
    }

    public static long requireTenantId() {
        Long v = CURRENT.get();
        if (v == null) {
            throw new IllegalStateException("tenant context is not set");
        }
        return v;
    }

    public static void clear() {
        CURRENT.remove();
    }

    public static void runWithTenantId(long tenantId, Runnable runnable) {
        Long prev = CURRENT.get();
        try {
            CURRENT.set(tenantId);
            runnable.run();
        } finally {
            if (prev == null) {
                CURRENT.remove();
            } else {
                CURRENT.set(prev);
            }
        }
    }

    public static <T> T runWithTenantId(long tenantId, Supplier<T> supplier) {
        Long prev = CURRENT.get();
        try {
            CURRENT.set(tenantId);
            return supplier.get();
        } finally {
            if (prev == null) {
                CURRENT.remove();
            } else {
                CURRENT.set(prev);
            }
        }
    }
}
