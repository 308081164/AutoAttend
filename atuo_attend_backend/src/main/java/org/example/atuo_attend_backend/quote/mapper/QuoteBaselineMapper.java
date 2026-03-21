package org.example.atuo_attend_backend.quote.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Mapper
public interface QuoteBaselineMapper {

    @Select("SELECT days FROM biz_quote_baseline WHERE tech_stack = #{techStack} AND complexity = #{complexity} LIMIT 1")
    BigDecimal findDays(@Param("techStack") String techStack, @Param("complexity") String complexity);

    @Select("SELECT tech_stack AS techStack, complexity, days FROM biz_quote_baseline ORDER BY tech_stack, complexity")
    List<Map<String, Object>> listAll();
}
