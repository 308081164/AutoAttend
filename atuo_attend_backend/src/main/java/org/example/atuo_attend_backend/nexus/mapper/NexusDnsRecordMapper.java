package org.example.atuo_attend_backend.nexus.mapper;

import org.apache.ibatis.annotations.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface NexusDnsRecordMapper {

    @Delete("DELETE FROM aa_nexus_dns_record WHERE tenant_id = #{tenantId} AND account_id = #{accountId}")
    int deleteByAccount(@Param("tenantId") long tenantId, @Param("accountId") long accountId);

    @Insert("""
            INSERT INTO aa_nexus_dns_record (tenant_id, account_id, domain_name, aliyun_record_id, rr, record_type, record_value, ttl, line, synced_at)
            VALUES (#{tenantId}, #{accountId}, #{domainName}, #{aliyunRecordId}, #{rr}, #{recordType}, #{recordValue}, #{ttl}, #{line}, #{syncedAt})
            """)
    int insert(@Param("tenantId") long tenantId,
               @Param("accountId") long accountId,
               @Param("domainName") String domainName,
               @Param("aliyunRecordId") String aliyunRecordId,
               @Param("rr") String rr,
               @Param("recordType") String recordType,
               @Param("recordValue") String recordValue,
               @Param("ttl") Integer ttl,
               @Param("line") String line,
               @Param("syncedAt") LocalDateTime syncedAt);

    @Select("""
            SELECT aliyun_record_id AS recordId, rr, record_type AS recordType, record_value AS recordValue, ttl, line
            FROM aa_nexus_dns_record
            WHERE tenant_id = #{tenantId} AND account_id = #{accountId} AND domain_name = #{domainName}
            ORDER BY rr ASC, record_type ASC
            """)
    List<Map<String, Object>> listByDomain(@Param("tenantId") long tenantId,
                                           @Param("accountId") long accountId,
                                           @Param("domainName") String domainName);
}
