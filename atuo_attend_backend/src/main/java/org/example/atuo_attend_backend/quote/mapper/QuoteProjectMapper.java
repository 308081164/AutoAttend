package org.example.atuo_attend_backend.quote.mapper;

import org.apache.ibatis.annotations.*;
import org.example.atuo_attend_backend.quote.domain.QuoteProject;

import java.util.List;

@Mapper
public interface QuoteProjectMapper {

    @Insert("""
            INSERT INTO biz_quote_project (name, project_type, tech_stack, design_type, data_migration,
                concurrency, security_level, deploy_type, status, link_table_id, prd_summary, quote_calc_prefs_json)
            VALUES (#{name}, #{projectType}, #{techStack}, #{designType}, #{dataMigration},
                #{concurrency}, #{securityLevel}, #{deployType}, #{status}, #{linkTableId}, #{prdSummary}, #{quoteCalcPrefsJson})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(QuoteProject p);

    @Update("""
            UPDATE biz_quote_project SET name=#{name}, project_type=#{projectType}, tech_stack=#{techStack},
                design_type=#{designType}, data_migration=#{dataMigration}, concurrency=#{concurrency},
                security_level=#{securityLevel}, deploy_type=#{deployType}, status=#{status},
                link_table_id=#{linkTableId}, prd_summary=#{prdSummary}, quote_calc_prefs_json=#{quoteCalcPrefsJson},
                updated_at=CURRENT_TIMESTAMP
            WHERE id=#{id}
            """)
    int update(QuoteProject p);

    @Select("SELECT id, name, project_type AS projectType, tech_stack AS techStack, design_type AS designType, " +
            "data_migration AS dataMigration, concurrency, security_level AS securityLevel, deploy_type AS deployType, " +
            "status, link_table_id AS linkTableId, prd_summary AS prdSummary, quote_calc_prefs_json AS quoteCalcPrefsJson, " +
            "created_at AS createdAt, updated_at AS updatedAt " +
            "FROM biz_quote_project WHERE id = #{id}")
    QuoteProject findById(@Param("id") long id);

    @Select("SELECT id, name, project_type AS projectType, tech_stack AS techStack, design_type AS designType, " +
            "data_migration AS dataMigration, concurrency, security_level AS securityLevel, deploy_type AS deployType, " +
            "status, link_table_id AS linkTableId, prd_summary AS prdSummary, quote_calc_prefs_json AS quoteCalcPrefsJson, " +
            "created_at AS createdAt, updated_at AS updatedAt " +
            "FROM biz_quote_project ORDER BY updated_at DESC LIMIT #{limit} OFFSET #{offset}")
    List<QuoteProject> listPaged(@Param("offset") int offset, @Param("limit") int limit);

    @Select("SELECT COUNT(*) FROM biz_quote_project")
    long countAll();

    @Delete("DELETE FROM biz_quote_project WHERE id = #{id}")
    int deleteById(@Param("id") long id);

    @Update("UPDATE biz_quote_project SET quote_calc_prefs_json = #{json}, updated_at = CURRENT_TIMESTAMP WHERE id = #{id}")
    int updateQuoteCalcPrefs(@Param("id") long id, @Param("json") String json);
}
