package org.example.atuo_attend_backend.nexus.mapper;

import org.apache.ibatis.annotations.*;
import org.example.atuo_attend_backend.nexus.domain.NexusIcpSite;

import java.util.List;
import java.util.Map;

@Mapper
public interface NexusIcpSiteMapper {

    @Insert("""
            INSERT INTO aa_nexus_icp_site (tenant_id, account_id, domain_name, site_name, icp_license, status_text, remark)
            VALUES (#{tenantId}, #{accountId}, #{domainName}, #{siteName}, #{icpLicense}, #{statusText}, #{remark})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(NexusIcpSite row);

    @Update("""
            UPDATE aa_nexus_icp_site
            SET site_name = #{siteName}, icp_license = #{icpLicense}, status_text = #{statusText}, remark = #{remark}
            WHERE tenant_id = #{tenantId} AND id = #{id}
            """)
    int update(@Param("tenantId") long tenantId,
               @Param("id") long id,
               @Param("siteName") String siteName,
               @Param("icpLicense") String icpLicense,
               @Param("statusText") String statusText,
               @Param("remark") String remark);

    @Delete("DELETE FROM aa_nexus_icp_site WHERE tenant_id = #{tenantId} AND id = #{id}")
    int delete(@Param("tenantId") long tenantId, @Param("id") long id);

    @Select("""
            SELECT id, domain_name AS domainName, site_name AS siteName, icp_license AS icpLicense,
                   status_text AS statusText, remark, created_at AS createdAt, updated_at AS updatedAt
            FROM aa_nexus_icp_site
            WHERE tenant_id = #{tenantId} AND account_id = #{accountId}
            ORDER BY updated_at DESC, id DESC
            """)
    List<Map<String, Object>> listByAccount(@Param("tenantId") long tenantId, @Param("accountId") long accountId);
}
