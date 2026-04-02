package org.example.atuo_attend_backend.nexus.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface NexusAuditLogMapper {

    @Insert("""
            INSERT INTO aa_nexus_audit_log
              (tenant_id, actor_user_id, actor_phone, action, target_type, target_id, result, meta_json)
            VALUES
              (#{tenantId}, #{actorUserId}, #{actorPhone}, #{action}, #{targetType}, #{targetId}, #{result}, #{metaJson})
            """)
    int insert(@Param("tenantId") long tenantId,
               @Param("actorUserId") Long actorUserId,
               @Param("actorPhone") String actorPhone,
               @Param("action") String action,
               @Param("targetType") String targetType,
               @Param("targetId") String targetId,
               @Param("result") String result,
               @Param("metaJson") String metaJson);
}

