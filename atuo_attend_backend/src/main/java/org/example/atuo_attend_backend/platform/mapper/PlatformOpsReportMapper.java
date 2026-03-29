package org.example.atuo_attend_backend.platform.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.example.atuo_attend_backend.platform.dto.PlatformTenantOpsRow;

import java.util.List;

@Mapper
public interface PlatformOpsReportMapper {

    @Select("""
            SELECT
              t.id AS tenantId,
              t.name AS tenantName,
              t.slug AS slug,
              t.plan_code AS planCode,
              t.status AS status,
              t.created_at AS tenantCreatedAt,
              (SELECT COUNT(*) FROM biz_user u WHERE u.tenant_id = t.id) AS memberCount,
              (SELECT COUNT(*) FROM biz_quote_project q
                 WHERE q.tenant_id = t.id
                   AND q.github_repo_full_name IS NOT NULL
                   AND TRIM(q.github_repo_full_name) <> '') AS githubLinkedProjects,
              (SELECT COUNT(*) FROM aa_commit c
                 WHERE c.tenant_id = t.id
                   AND c.committed_at >= DATE_SUB(NOW(), INTERVAL 1 DAY)) AS commits24h,
              (SELECT COUNT(*) FROM aa_commit c2
                 WHERE c2.tenant_id = t.id
                   AND c2.committed_at >= DATE_SUB(NOW(), INTERVAL 7 DAY)) AS commits7d,
              (SELECT COUNT(DISTINCT author_email) FROM aa_commit c3
                 WHERE c3.tenant_id = t.id
                   AND c3.committed_at >= DATE_SUB(NOW(), INTERVAL 1 DAY)
                   AND c3.author_email IS NOT NULL
                   AND TRIM(c3.author_email) <> '') AS dauEmails24h,
              (SELECT COALESCE(SUM(diff_size_bytes), 0) FROM aa_commit_diff d
                 WHERE d.tenant_id = t.id) AS storageDiffBytesApprox
            FROM aa_tenant t
            ORDER BY t.id
            """)
    List<PlatformTenantOpsRow> listTenantOpsRows();
}
