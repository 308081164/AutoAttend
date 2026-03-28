package org.example.atuo_attend_backend.quote.mapper;

import org.apache.ibatis.annotations.*;
import org.example.atuo_attend_backend.quote.domain.QuoteContractDraft;

@Mapper
public interface QuoteContractDraftMapper {

    @Insert("""
            INSERT INTO biz_quote_contract_draft (quote_result_id, client_name, company_name, template_type,
                ai_prompt_snapshot, ai_raw_response, edited_content)
            VALUES (#{quoteResultId}, #{clientName}, #{companyName}, #{templateType},
                #{aiPromptSnapshot}, #{aiRawResponse}, #{editedContent})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(QuoteContractDraft d);

    @Update("""
            UPDATE biz_quote_contract_draft SET client_name=#{clientName}, company_name=#{companyName},
                template_type=#{templateType}, ai_prompt_snapshot=#{aiPromptSnapshot}, ai_raw_response=#{aiRawResponse},
                edited_content=#{editedContent}, updated_at=CURRENT_TIMESTAMP
            WHERE quote_result_id = #{quoteResultId}
            """)
    int updateByResultId(QuoteContractDraft d);

    @Select("SELECT id, quote_result_id AS quoteResultId, client_name AS clientName, company_name AS companyName, " +
            "template_type AS templateType, ai_prompt_snapshot AS aiPromptSnapshot, ai_raw_response AS aiRawResponse, " +
            "edited_content AS editedContent, created_at AS createdAt, updated_at AS updatedAt " +
            "FROM biz_quote_contract_draft WHERE quote_result_id = #{resultId} LIMIT 1")
    QuoteContractDraft findByResultId(@Param("resultId") long resultId);

    @Update("UPDATE biz_quote_contract_draft SET edited_content = #{content}, updated_at = CURRENT_TIMESTAMP WHERE quote_result_id = #{resultId}")
    int updateEditedContent(@Param("resultId") long resultId, @Param("content") String content);

    @Delete("DELETE FROM biz_quote_contract_draft WHERE quote_result_id = #{resultId}")
    int deleteByResultId(@Param("resultId") long resultId);
}
