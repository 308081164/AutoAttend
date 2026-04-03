package org.example.atuo_attend_backend.quote.mapper;

import org.apache.ibatis.annotations.*;
import org.example.atuo_attend_backend.quote.domain.QuoteAiAcceptanceTestCasesJob;

@Mapper
public interface QuoteAiAcceptanceTestCasesJobMapper {

    @Insert("""
            INSERT INTO aa_quote_ai_acceptance_test_cases_job (tenant_id, quote_project_id, status, request_snapshot)
            VALUES (#{tenantId}, #{quoteProjectId}, #{status}, #{requestSnapshot})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    int insert(QuoteAiAcceptanceTestCasesJob row);

    @Update("""
            UPDATE aa_quote_ai_acceptance_test_cases_job
            SET status=#{status}, updated_at=CURRENT_TIMESTAMP
            WHERE id=#{id} AND tenant_id=#{tenantId} AND quote_project_id=#{quoteProjectId}
            """)
    int updateStatus(@Param("id") long id, @Param("tenantId") long tenantId, @Param("quoteProjectId") long quoteProjectId,
                      @Param("status") String status);

    @Update("""
            UPDATE aa_quote_ai_acceptance_test_cases_job
            SET status='success', result_json=#{resultJson}, error_message=NULL, updated_at=CURRENT_TIMESTAMP
            WHERE id=#{id} AND tenant_id=#{tenantId} AND quote_project_id=#{quoteProjectId}
            """)
    int updateSuccess(@Param("id") long id, @Param("tenantId") long tenantId, @Param("quoteProjectId") long quoteProjectId,
                       @Param("resultJson") String resultJson);

    @Update("""
            UPDATE aa_quote_ai_acceptance_test_cases_job
            SET status='failed', error_message=#{errorMessage}, updated_at=CURRENT_TIMESTAMP
            WHERE id=#{id} AND tenant_id=#{tenantId} AND quote_project_id=#{quoteProjectId}
            """)
    int updateFailed(@Param("id") long id, @Param("tenantId") long tenantId, @Param("quoteProjectId") long quoteProjectId,
                       @Param("errorMessage") String errorMessage);

    @Select("""
            SELECT id, tenant_id AS tenantId, quote_project_id AS quoteProjectId, status,
                   request_snapshot AS requestSnapshot, result_json AS resultJson,
                   error_message AS errorMessage, created_at AS createdAt, updated_at AS updatedAt
            FROM aa_quote_ai_acceptance_test_cases_job
            WHERE id=#{id} AND tenant_id=#{tenantId} AND quote_project_id=#{quoteProjectId}
            LIMIT 1
            """)
    QuoteAiAcceptanceTestCasesJob findById(@Param("id") long id, @Param("tenantId") long tenantId,
                                            @Param("quoteProjectId") long quoteProjectId);
}

