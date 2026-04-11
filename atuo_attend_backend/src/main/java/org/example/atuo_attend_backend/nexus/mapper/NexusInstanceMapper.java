package org.example.atuo_attend_backend.nexus.mapper;

import org.apache.ibatis.annotations.*;
import org.example.atuo_attend_backend.nexus.domain.NexusCloudInstance;

import java.util.List;

@Mapper
public interface NexusInstanceMapper {

    @Select("""
            SELECT
              tenant_id AS tenantId,
              account_id AS accountId,
              instance_id AS instanceId,
              instance_name AS instanceName,
              status,
              instance_type AS instanceType,
              zone_id AS zoneId,
              public_ip AS publicIp,
              private_ip AS privateIp,
              os_name AS osName,
              memory_mb AS memoryMb,
              bt_panel_url AS btPanelUrl,
              created_at AS createdAt,
              updated_at AS updatedAt
            FROM aa_nexus_cloud_instance
            WHERE tenant_id = #{tenantId} AND account_id = #{accountId}
            ORDER BY updated_at DESC, instance_id DESC
            LIMIT 500
            """)
    List<NexusCloudInstance> listByAccount(@Param("tenantId") long tenantId, @Param("accountId") long accountId);

    @Insert("""
            INSERT INTO aa_nexus_cloud_instance
              (tenant_id, account_id, instance_id, instance_name, status, instance_type, zone_id, public_ip, private_ip, os_name, memory_mb, bt_panel_url)
            VALUES
              (#{tenantId}, #{accountId}, #{instanceId}, #{instanceName}, #{status}, #{instanceType}, #{zoneId}, #{publicIp}, #{privateIp}, #{osName}, #{memoryMb}, NULL)
            ON DUPLICATE KEY UPDATE
              instance_name = VALUES(instance_name),
              status = VALUES(status),
              instance_type = VALUES(instance_type),
              zone_id = VALUES(zone_id),
              public_ip = VALUES(public_ip),
              private_ip = VALUES(private_ip),
              os_name = VALUES(os_name),
              memory_mb = VALUES(memory_mb),
              bt_panel_url = COALESCE(aa_nexus_cloud_instance.bt_panel_url, VALUES(bt_panel_url)),
              updated_at = CURRENT_TIMESTAMP
            """)
    int upsert(@Param("tenantId") long tenantId, @Param("accountId") long accountId,
                @Param("instanceId") String instanceId,
                @Param("instanceName") String instanceName,
                @Param("status") String status,
                @Param("instanceType") String instanceType,
                @Param("zoneId") String zoneId,
                @Param("publicIp") String publicIp,
                @Param("privateIp") String privateIp,
                @Param("osName") String osName,
                @Param("memoryMb") Long memoryMb);

    @Update("""
            UPDATE aa_nexus_cloud_instance
            SET bt_panel_url = #{btPanelUrl},
                updated_at = CURRENT_TIMESTAMP
            WHERE tenant_id = #{tenantId} AND account_id = #{accountId} AND instance_id = #{instanceId}
            """)
    int updateBtPanelUrl(@Param("tenantId") long tenantId,
                         @Param("accountId") long accountId,
                         @Param("instanceId") String instanceId,
                         @Param("btPanelUrl") String btPanelUrl);
}
