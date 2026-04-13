package org.example.atuo_attend_backend.nexus.mapper;

import org.apache.ibatis.annotations.*;
import org.example.atuo_attend_backend.nexus.domain.NexusCloudAccount;

import java.util.List;

@Mapper
public interface NexusCloudAccountMapper {

    @Select("""
            SELECT
              id,
              tenant_id AS tenantId,
              provider,
              display_name AS displayName,
              region_id AS regionId,
              access_key_id_enc AS accessKeyIdEnc,
              access_key_secret_enc AS accessKeySecretEnc,
              auto_sync_interval_seconds AS autoSyncIntervalSeconds,
              last_auto_sync_at AS lastAutoSyncAt,
              created_at AS createdAt,
              updated_at AS updatedAt
            FROM aa_nexus_cloud_account
            WHERE tenant_id = #{tenantId}
            ORDER BY updated_at DESC, id DESC
            """)
    List<NexusCloudAccount> listByTenant(@Param("tenantId") long tenantId);

    @Select("SELECT COUNT(*) FROM aa_nexus_cloud_account WHERE tenant_id = #{tenantId}")
    long countByTenant(@Param("tenantId") long tenantId);

    @Select("""
            SELECT
              id,
              tenant_id AS tenantId,
              provider,
              display_name AS displayName,
              region_id AS regionId,
              access_key_id_enc AS accessKeyIdEnc,
              access_key_secret_enc AS accessKeySecretEnc,
              auto_sync_interval_seconds AS autoSyncIntervalSeconds,
              last_auto_sync_at AS lastAutoSyncAt,
              created_at AS createdAt,
              updated_at AS updatedAt
            FROM aa_nexus_cloud_account
            WHERE tenant_id = #{tenantId} AND id = #{id}
            LIMIT 1
            """)
    NexusCloudAccount findForSync(@Param("tenantId") long tenantId, @Param("id") long id);

    @Insert("""
            INSERT INTO aa_nexus_cloud_account
              (tenant_id, provider, display_name, region_id, access_key_id_enc, access_key_secret_enc, auto_sync_interval_seconds, last_auto_sync_at)
            VALUES
              (#{tenantId}, #{provider}, #{displayName}, #{regionId}, #{accessKeyIdEnc}, #{accessKeySecretEnc}, #{autoSyncIntervalSeconds}, NULL)
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(NexusCloudAccount account);

    @Update("""
            UPDATE aa_nexus_cloud_account
            SET last_auto_sync_at = #{lastAutoSyncAt}
            WHERE tenant_id = #{tenantId} AND id = #{id}
            """)
    int updateLastAutoSyncAt(@Param("tenantId") long tenantId,
                              @Param("id") long id,
                              @Param("lastAutoSyncAt") java.time.LocalDateTime lastAutoSyncAt);

    @Update("""
            UPDATE aa_nexus_cloud_account
            SET display_name = #{displayName},
                updated_at = CURRENT_TIMESTAMP
            WHERE tenant_id = #{tenantId} AND id = #{id}
            """)
    int updateDisplayName(@Param("tenantId") long tenantId,
                          @Param("id") long id,
                          @Param("displayName") String displayName);

    @Update("""
            UPDATE aa_nexus_cloud_account
            SET auto_sync_interval_seconds = #{autoSyncIntervalSeconds},
                updated_at = CURRENT_TIMESTAMP
            WHERE tenant_id = #{tenantId} AND id = #{id}
            """)
    int updateAutoSyncInterval(@Param("tenantId") long tenantId,
                               @Param("id") long id,
                               @Param("autoSyncIntervalSeconds") Integer autoSyncIntervalSeconds);

    @Select("""
            SELECT DISTINCT tenant_id AS tenantId
            FROM aa_nexus_cloud_account
            WHERE provider = #{provider}
            """)
    List<Long> listTenantIdsByProvider(@Param("provider") String provider);

    @Select("""
            SELECT
              id,
              tenant_id AS tenantId,
              provider,
              display_name AS displayName,
              region_id AS regionId,
              access_key_id_enc AS accessKeyIdEnc,
              access_key_secret_enc AS accessKeySecretEnc,
              auto_sync_interval_seconds AS autoSyncIntervalSeconds,
              last_auto_sync_at AS lastAutoSyncAt,
              created_at AS createdAt,
              updated_at AS updatedAt
            FROM aa_nexus_cloud_account
            WHERE provider = #{provider}
            ORDER BY tenant_id ASC, updated_at DESC, id DESC
            """)
    List<NexusCloudAccount> listAllForProvider(@Param("provider") String provider);
}

