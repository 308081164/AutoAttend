package org.example.atuo_attend_backend.nexus.mapper;

import org.apache.ibatis.annotations.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface NexusDnsDomainMapper {

    @Delete("DELETE FROM aa_nexus_dns_domain WHERE tenant_id = #{tenantId} AND account_id = #{accountId}")
    int deleteByAccount(@Param("tenantId") long tenantId, @Param("accountId") long accountId);

    @Insert("""
            INSERT INTO aa_nexus_dns_domain (tenant_id, account_id, domain_name, remark, synced_at)
            VALUES (#{tenantId}, #{accountId}, #{domainName}, #{remark}, #{syncedAt})
            """)
    int insert(@Param("tenantId") long tenantId,
               @Param("accountId") long accountId,
               @Param("domainName") String domainName,
               @Param("remark") String remark,
               @Param("syncedAt") LocalDateTime syncedAt);

    @Select("""
            SELECT domain_name AS domainName, remark, synced_at AS syncedAt
            FROM aa_nexus_dns_domain
            WHERE tenant_id = #{tenantId} AND account_id = #{accountId}
            ORDER BY domain_name ASC
            """)
    List<Map<String, Object>> listByAccount(@Param("tenantId") long tenantId, @Param("accountId") long accountId);
}
