package org.example.atuo_attend_backend.nexus.mapper;

import org.apache.ibatis.annotations.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface NexusSmsSignatureMapper {

    @Delete("DELETE FROM aa_nexus_sms_signature WHERE tenant_id = #{tenantId} AND account_id = #{accountId}")
    int deleteByAccount(@Param("tenantId") long tenantId, @Param("accountId") long accountId);

    @Insert("""
            INSERT INTO aa_nexus_sms_signature (tenant_id, account_id, sign_name, audit_status, sign_type, synced_at)
            VALUES (#{tenantId}, #{accountId}, #{signName}, #{auditStatus}, #{signType}, #{syncedAt})
            """)
    int insert(@Param("tenantId") long tenantId,
               @Param("accountId") long accountId,
               @Param("signName") String signName,
               @Param("auditStatus") String auditStatus,
               @Param("signType") String signType,
               @Param("syncedAt") LocalDateTime syncedAt);

    @Select("""
            SELECT sign_name AS signName, audit_status AS auditStatus, sign_type AS signType, synced_at AS syncedAt
            FROM aa_nexus_sms_signature
            WHERE tenant_id = #{tenantId} AND account_id = #{accountId}
            ORDER BY sign_name ASC
            """)
    List<Map<String, Object>> listByAccount(@Param("tenantId") long tenantId, @Param("accountId") long accountId);
}
