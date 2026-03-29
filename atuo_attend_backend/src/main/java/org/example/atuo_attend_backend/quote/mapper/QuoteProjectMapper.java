package org.example.atuo_attend_backend.quote.mapper;

import org.apache.ibatis.annotations.*;
import org.example.atuo_attend_backend.quote.domain.QuoteProject;

import java.util.List;

@Mapper
public interface QuoteProjectMapper {

    @Insert("""
            INSERT INTO biz_quote_project (tenant_id, name, project_type, tech_stack, design_type, data_migration,
                concurrency, security_level, deploy_type, status, link_table_id, prd_summary, quote_calc_prefs_json, quote_contract_context_json,
                quote_vendor_name, quote_contact_info, quote_validity_note, quote_subject_mode)
            VALUES (#{tenantId}, #{name}, #{projectType}, #{techStack}, #{designType}, #{dataMigration},
                #{concurrency}, #{securityLevel}, #{deployType}, #{status}, #{linkTableId}, #{prdSummary}, #{quoteCalcPrefsJson}, #{quoteContractContextJson},
                #{quoteVendorName}, #{quoteContactInfo}, #{quoteValidityNote}, #{quoteSubjectMode})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(QuoteProject p);

    @Update("""
            UPDATE biz_quote_project SET name=#{name}, project_type=#{projectType}, tech_stack=#{techStack},
                design_type=#{designType}, data_migration=#{dataMigration}, concurrency=#{concurrency},
                security_level=#{securityLevel}, deploy_type=#{deployType}, status=#{status},
                link_table_id=#{linkTableId}, prd_summary=#{prdSummary}, quote_calc_prefs_json=#{quoteCalcPrefsJson},
                quote_contract_context_json=#{quoteContractContextJson},
                quote_vendor_name=#{quoteVendorName}, quote_contact_info=#{quoteContactInfo}, quote_validity_note=#{quoteValidityNote},
                quote_subject_mode=#{quoteSubjectMode},
                updated_at=CURRENT_TIMESTAMP
            WHERE tenant_id=#{tenantId} AND id=#{id}
            """)
    int update(QuoteProject p);

    @Select("SELECT id, tenant_id AS tenantId, name, project_type AS projectType, tech_stack AS techStack, design_type AS designType, " +
            "data_migration AS dataMigration, concurrency, security_level AS securityLevel, deploy_type AS deployType, " +
            "status, link_table_id AS linkTableId, prd_summary AS prdSummary, quote_calc_prefs_json AS quoteCalcPrefsJson, " +
            "quote_contract_context_json AS quoteContractContextJson, " +
            "quote_vendor_name AS quoteVendorName, quote_contact_info AS quoteContactInfo, quote_validity_note AS quoteValidityNote, " +
            "quote_subject_mode AS quoteSubjectMode, " +
            "github_repo_full_name AS githubRepoFullName, github_repo_html_url AS githubRepoHtmlUrl, " +
            "github_webhook_id AS githubWebhookId, github_webhook_secret AS githubWebhookSecret, " +
            "provision_status AS provisionStatus, provision_last_error AS provisionLastError, " +
            "provision_synced_to_collab AS provisionSyncedToCollab, provision_synced_at AS provisionSyncedAt, " +
            "created_at AS createdAt, updated_at AS updatedAt " +
            "FROM biz_quote_project WHERE tenant_id = #{tenantId} AND id = #{id}")
    QuoteProject findById(@Param("tenantId") long tenantId, @Param("id") long id);

    @Select("SELECT id, tenant_id AS tenantId, name, project_type AS projectType, tech_stack AS techStack, design_type AS designType, " +
            "data_migration AS dataMigration, concurrency, security_level AS securityLevel, deploy_type AS deployType, " +
            "status, link_table_id AS linkTableId, prd_summary AS prdSummary, quote_calc_prefs_json AS quoteCalcPrefsJson, " +
            "quote_contract_context_json AS quoteContractContextJson, " +
            "quote_vendor_name AS quoteVendorName, quote_contact_info AS quoteContactInfo, quote_validity_note AS quoteValidityNote, " +
            "quote_subject_mode AS quoteSubjectMode, " +
            "github_repo_full_name AS githubRepoFullName, github_repo_html_url AS githubRepoHtmlUrl, " +
            "github_webhook_id AS githubWebhookId, github_webhook_secret AS githubWebhookSecret, " +
            "provision_status AS provisionStatus, provision_last_error AS provisionLastError, " +
            "provision_synced_to_collab AS provisionSyncedToCollab, provision_synced_at AS provisionSyncedAt, " +
            "created_at AS createdAt, updated_at AS updatedAt " +
            "FROM biz_quote_project WHERE tenant_id = #{tenantId} ORDER BY updated_at DESC LIMIT #{limit} OFFSET #{offset}")
    List<QuoteProject> listPaged(@Param("tenantId") long tenantId, @Param("offset") int offset, @Param("limit") int limit);

    @Update("""
            UPDATE biz_quote_project
            SET github_repo_full_name=#{repoFullName},
                github_repo_html_url=#{repoHtmlUrl},
                github_webhook_id=#{webhookId},
                github_webhook_secret=#{webhookSecret},
                provision_status=#{status},
                provision_last_error=#{lastError},
                provision_synced_to_collab=#{syncedToCollab},
                provision_synced_at=#{syncedAt},
                updated_at=CURRENT_TIMESTAMP
            WHERE tenant_id=#{tenantId} AND id=#{id}
            """)
    int updateProvisionState(@Param("tenantId") long tenantId, @Param("id") long id,
                             @Param("repoFullName") String repoFullName,
                             @Param("repoHtmlUrl") String repoHtmlUrl,
                             @Param("webhookId") Long webhookId,
                             @Param("webhookSecret") String webhookSecret,
                             @Param("status") String status,
                             @Param("lastError") String lastError,
                             @Param("syncedToCollab") Integer syncedToCollab,
                             @Param("syncedAt") java.time.LocalDateTime syncedAt);

    @Select("SELECT COUNT(*) FROM biz_quote_project WHERE tenant_id = #{tenantId}")
    long countAll(@Param("tenantId") long tenantId);

    /** 已绑定 GitHub 仓库的报价项目数（用于套餐 GitHub 仓库配额） */
    @Select("SELECT COUNT(*) FROM biz_quote_project WHERE tenant_id = #{tenantId} AND github_repo_full_name IS NOT NULL AND TRIM(github_repo_full_name) <> ''")
    long countGithubLinkedByTenant(@Param("tenantId") long tenantId);

    @Delete("DELETE FROM biz_quote_project WHERE tenant_id = #{tenantId} AND id = #{id}")
    int deleteById(@Param("tenantId") long tenantId, @Param("id") long id);

    @Update("UPDATE biz_quote_project SET quote_calc_prefs_json = #{json}, updated_at = CURRENT_TIMESTAMP WHERE tenant_id = #{tenantId} AND id = #{id}")
    int updateQuoteCalcPrefs(@Param("tenantId") long tenantId, @Param("id") long id, @Param("json") String json);
}
