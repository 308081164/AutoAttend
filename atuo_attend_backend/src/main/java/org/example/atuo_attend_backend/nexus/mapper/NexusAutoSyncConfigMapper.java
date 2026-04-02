package org.example.atuo_attend_backend.nexus.mapper;

import org.apache.ibatis.annotations.*;
import org.example.atuo_attend_backend.nexus.domain.NexusAutoSyncConfig;

@Mapper
public interface NexusAutoSyncConfigMapper {

    @Select("""
            SELECT
              tenant_id AS tenantId,
              enabled,
              global_interval_seconds AS globalIntervalSeconds,
              cpu_period_seconds AS cpuPeriodSeconds,
              cpu_window_minutes AS cpuWindowMinutes
            FROM aa_nexus_auto_sync_config
            WHERE tenant_id = #{tenantId}
            LIMIT 1
            """)
    NexusAutoSyncConfig findByTenant(@Param("tenantId") long tenantId);

    @Insert("""
            INSERT IGNORE INTO aa_nexus_auto_sync_config (tenant_id, enabled, global_interval_seconds, cpu_period_seconds, cpu_window_minutes)
            VALUES (#{tenantId}, 1, 60, 60, 60)
            """)
    int ensureDefault(@Param("tenantId") long tenantId);
}

