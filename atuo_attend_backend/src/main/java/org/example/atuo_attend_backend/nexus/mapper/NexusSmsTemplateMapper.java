package org.example.atuo_attend_backend.nexus.mapper;

import org.apache.ibatis.annotations.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface NexusSmsTemplateMapper {

    @Delete("DELETE FROM aa_nexus_sms_template WHERE tenant_id = #{tenantId} AND account_id = #{accountId}")
    int deleteByAccount(@Param("tenantId") long tenantId, @Param("accountId") long accountId);

    @Insert("""
            INSERT INTO aa_nexus_sms_template (tenant_id, account_id, template_code, template_name, template_type, audit_status, synced_at)
            VALUES (#{tenantId}, #{accountId}, #{templateCode}, #{templateName}, #{templateType}, #{auditStatus}, #{syncedAt})
            """)
    int insert(@Param("tenantId") long tenantId,
               @Param("accountId") long accountId,
               @Param("templateCode") String templateCode,
               @Param("templateName") String templateName,
               @Param("templateType") String templateType,
               @Param("auditStatus") String auditStatus,
               @Param("syncedAt") LocalDateTime syncedAt);

    @Select("""
            SELECT template_code AS templateCode, template_name AS templateName, template_type AS templateType,
                   audit_status AS auditStatus, synced_at AS syncedAt
            FROM aa_nexus_sms_template
            WHERE tenant_id = #{tenantId} AND account_id = #{accountId}
            ORDER BY template_code ASC
            """)
    List<Map<String, Object>> listByAccount(@Param("tenantId") long tenantId, @Param("accountId") long accountId);
}
