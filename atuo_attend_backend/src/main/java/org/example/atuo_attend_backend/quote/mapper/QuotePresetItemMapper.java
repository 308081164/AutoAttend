package org.example.atuo_attend_backend.quote.mapper;

import org.apache.ibatis.annotations.*;
import org.example.atuo_attend_backend.quote.domain.QuotePresetItem;

import java.util.List;

@Mapper
public interface QuotePresetItemMapper {

    @Select("SELECT id, tenant_id AS tenantId, name, complexity, category, sort_order AS sortOrder, enabled, created_at AS createdAt, updated_at AS updatedAt " +
            "FROM biz_quote_preset_item WHERE tenant_id = #{tenantId} AND enabled = 1 ORDER BY sort_order ASC, id ASC")
    List<QuotePresetItem> listEnabled(@Param("tenantId") long tenantId);

    @Select("SELECT id, tenant_id AS tenantId, name, complexity, category, sort_order AS sortOrder, enabled, created_at AS createdAt, updated_at AS updatedAt " +
            "FROM biz_quote_preset_item WHERE tenant_id = #{tenantId} ORDER BY sort_order ASC, id ASC")
    List<QuotePresetItem> listAll(@Param("tenantId") long tenantId);

    @Select("SELECT id, tenant_id AS tenantId, name, complexity, category, sort_order AS sortOrder, enabled, created_at AS createdAt, updated_at AS updatedAt " +
            "FROM biz_quote_preset_item WHERE tenant_id = #{tenantId} AND id = #{id}")
    QuotePresetItem findById(@Param("tenantId") long tenantId, @Param("id") long id);

    @Insert("INSERT INTO biz_quote_preset_item (tenant_id, name, complexity, category, sort_order, enabled) " +
            "VALUES (#{tenantId}, #{name}, #{complexity}, #{category}, #{sortOrder}, #{enabled})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(QuotePresetItem row);

    @Update("UPDATE biz_quote_preset_item SET name=#{name}, complexity=#{complexity}, category=#{category}, " +
            "sort_order=#{sortOrder}, enabled=#{enabled}, updated_at=CURRENT_TIMESTAMP WHERE tenant_id=#{tenantId} AND id=#{id}")
    int update(QuotePresetItem row);

    @Delete("DELETE FROM biz_quote_preset_item WHERE tenant_id=#{tenantId} AND id=#{id}")
    int deleteById(@Param("tenantId") long tenantId, @Param("id") long id);
}
