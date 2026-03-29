package org.example.atuo_attend_backend.quote.mapper;

import org.apache.ibatis.annotations.*;
import org.example.atuo_attend_backend.quote.domain.QuoteModule;

import java.util.List;

@Mapper
public interface QuoteModuleMapper {

    @Insert("INSERT INTO biz_quote_module (tenant_id, quote_project_id, name, sort_order) VALUES (#{tenantId}, #{quoteProjectId}, #{name}, #{sortOrder})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(QuoteModule m);

    @Delete("DELETE FROM biz_quote_module WHERE tenant_id = #{tenantId} AND quote_project_id = #{projectId}")
    int deleteByProjectId(@Param("tenantId") long tenantId, @Param("projectId") long projectId);

    @Select("SELECT id, tenant_id AS tenantId, quote_project_id AS quoteProjectId, name, sort_order AS sortOrder FROM biz_quote_module " +
            "WHERE tenant_id = #{tenantId} AND quote_project_id = #{projectId} ORDER BY sort_order, id")
    List<QuoteModule> listByProjectId(@Param("tenantId") long tenantId, @Param("projectId") long projectId);
}
