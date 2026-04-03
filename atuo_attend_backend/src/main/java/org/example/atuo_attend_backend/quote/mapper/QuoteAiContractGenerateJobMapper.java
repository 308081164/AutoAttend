package org.example.atuo_attend_backend.quote.mapper;

import org.apache.ibatis.annotations.*;
import org.example.atuo_attend_backend.quote.domain.QuoteAiContractGenerateJob;

@Mapper
public interface QuoteAiContractGenerateJobMapper {

    @Insert("""
            INSERT INTO aa_quote_ai_contract_generate_job (tenant_id, quote_result_id, status, request_snapshot)
            VALUES (#{tenantId}, #{quoteResultId}, #{status}, #{requestSnapshot})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    int insert(QuoteAiContractGenerateJob row);

    @Update("""
            UPDATE aa_quote_ai_contract_generate_job
            SET status=#{status}, updated_at=CURRENT_TIMESTAMP
            WHERE id=#{id} AND tenant_id=#{tenantId} AND quote_result_id=#{quoteResultId}
            """)
    int updateStatus(@Param("id") long id, @Param("tenantId") long tenantId,
                      @Param("quoteResultId") long quoteResultId, @Param("status") String status);

    @Update("""
            UPDATE aa_quote_ai_contract_generate_job
            SET status='success', edited_content=#{editedContent}, error_message=NULL, updated_at=CURRENT_TIMESTAMP
            WHERE id=#{id} AND tenant_id=#{tenantId} AND quote_result_id=#{quoteResultId}
            """)
    int updateSuccess(@Param("id") long id, @Param("tenantId") long tenantId,
                       @Param("quoteResultId") long quoteResultId, @Param("editedContent") String editedContent);

    @Update("""
            UPDATE aa_quote_ai_contract_generate_job
            SET status='failed', error_message=#{errorMessage}, updated_at=CURRENT_TIMESTAMP
            WHERE id=#{id} AND tenant_id=#{tenantId} AND quote_result_id=#{quoteResultId}
            """)
    int updateFailed(@Param("id") long id, @Param("tenantId") long tenantId,
                      @Param("quoteResultId") long quoteResultId, @Param("errorMessage") String errorMessage);

    @Select("""
            SELECT id, tenant_id AS tenantId, quote_result_id AS quoteResultId, status,
                   request_snapshot AS requestSnapshot, edited_content AS editedContent,
                   error_message AS errorMessage,
                   created_at AS createdAt, updated_at AS updatedAt
            FROM aa_quote_ai_contract_generate_job
            WHERE id=#{id} AND tenant_id=#{tenantId} AND quote_result_id=#{quoteResultId}
            LIMIT 1
            """)
    QuoteAiContractGenerateJob findById(@Param("id") long id, @Param("tenantId") long tenantId,
                                         @Param("quoteResultId") long quoteResultId);
}

