package org.example.atuo_attend_backend.quote.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface QuoteDocumentMapper {

    @Insert("INSERT INTO biz_quote_document (quote_result_id, doc_type, content, version) VALUES (#{resultId}, #{docType}, #{content}, #{version})")
    int insert(@Param("resultId") long resultId, @Param("docType") String docType, @Param("content") String content, @Param("version") int version);

    @Delete("DELETE FROM biz_quote_document WHERE quote_result_id = #{resultId}")
    int deleteByResultId(@Param("resultId") long resultId);
}
