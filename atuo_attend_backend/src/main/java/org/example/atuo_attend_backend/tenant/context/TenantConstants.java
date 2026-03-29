package org.example.atuo_attend_backend.tenant.context;

/**
 * 单租户历史数据与种子默认租户的主键，对应 {@code aa_tenant.id = 1}（slug 一般为 {@code default}）。
 */
public final class TenantConstants {

    public static final long DEFAULT_TENANT_ID = 1L;

    private TenantConstants() {
    }
}
