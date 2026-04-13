package org.example.atuo_attend_backend.platform.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface PlatformOpsAuditMapper {

    @Insert("""
            INSERT INTO aa_platform_ops_audit (platform_session_id, actor_label, action, tenant_id, payload_json)
            VALUES (#{sessionId}, #{actorLabel}, #{action}, #{tenantId}, #{payloadJson})
            """)
    int insert(@Param("sessionId") Long sessionId,
               @Param("actorLabel") String actorLabel,
               @Param("action") String action,
               @Param("tenantId") Long tenantId,
               @Param("payloadJson") String payloadJson);
}
