package org.example.atuo_attend_backend.nexus.mapper;

import org.apache.ibatis.annotations.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface NexusOssBucketMapper {

    @Delete("DELETE FROM aa_nexus_oss_bucket WHERE tenant_id = #{tenantId} AND account_id = #{accountId}")
    int deleteByAccount(@Param("tenantId") long tenantId, @Param("accountId") long accountId);

    @Insert("""
            INSERT INTO aa_nexus_oss_bucket (tenant_id, account_id, bucket_name, region, location, synced_at)
            VALUES (#{tenantId}, #{accountId}, #{bucketName}, #{region}, #{location}, #{syncedAt})
            """)
    int insert(@Param("tenantId") long tenantId,
               @Param("accountId") long accountId,
               @Param("bucketName") String bucketName,
               @Param("region") String region,
               @Param("location") String location,
               @Param("syncedAt") LocalDateTime syncedAt);

    @Select("""
            SELECT bucket_name AS bucketName, region, location, synced_at AS syncedAt
            FROM aa_nexus_oss_bucket
            WHERE tenant_id = #{tenantId} AND account_id = #{accountId}
            ORDER BY bucket_name ASC
            """)
    List<Map<String, Object>> listByAccount(@Param("tenantId") long tenantId, @Param("accountId") long accountId);
}
