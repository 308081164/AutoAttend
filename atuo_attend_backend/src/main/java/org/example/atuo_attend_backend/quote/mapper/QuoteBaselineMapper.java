package org.example.atuo_attend_backend.quote.mapper;

import org.apache.ibatis.annotations.*;
import org.example.atuo_attend_backend.quote.domain.QuoteBaseline;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Mapper
public interface QuoteBaselineMapper {

    @Select("SELECT days FROM biz_quote_baseline WHERE tenant_id = #{tenantId} AND tech_stack = #{techStack} AND complexity = #{complexity} LIMIT 1")
    BigDecimal findDays(@Param("tenantId") long tenantId, @Param("techStack") String techStack, @Param("complexity") String complexity);

    @Select("SELECT id, tech_stack AS techStack, complexity, days FROM biz_quote_baseline WHERE tenant_id = #{tenantId} ORDER BY tech_stack, FIELD(complexity,'simple','standard','medium','complex','extreme'), id")
    List<Map<String, Object>> listAll(@Param("tenantId") long tenantId);

    @Select("SELECT id, tech_stack AS techStack, complexity, days FROM biz_quote_baseline WHERE tenant_id = #{tenantId} AND id = #{id}")
    Map<String, Object> findById(@Param("tenantId") long tenantId, @Param("id") long id);

    @Select("SELECT COUNT(*) FROM biz_quote_baseline WHERE tenant_id = #{tenantId} AND tech_stack = #{ts} AND complexity = #{c}")
    int countByStackAndComplexity(@Param("tenantId") long tenantId, @Param("ts") String ts, @Param("c") String c);

    @Select("SELECT COUNT(*) FROM biz_quote_baseline WHERE tenant_id = #{tenantId} AND tech_stack = #{ts} AND complexity = #{c} AND id <> #{excludeId}")
    int countByStackAndComplexityExcluding(@Param("tenantId") long tenantId, @Param("ts") String ts, @Param("c") String c, @Param("excludeId") long excludeId);

    @Insert("INSERT INTO biz_quote_baseline (tenant_id, tech_stack, complexity, days) VALUES (#{tenantId}, #{techStack}, #{complexity}, #{days})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(QuoteBaseline row);

    @Update("UPDATE biz_quote_baseline SET tech_stack = #{techStack}, complexity = #{complexity}, days = #{days} WHERE tenant_id = #{tenantId} AND id = #{id}")
    int update(QuoteBaseline row);

    @Delete("DELETE FROM biz_quote_baseline WHERE tenant_id = #{tenantId} AND id = #{id}")
    int deleteById(@Param("tenantId") long tenantId, @Param("id") long id);
}
