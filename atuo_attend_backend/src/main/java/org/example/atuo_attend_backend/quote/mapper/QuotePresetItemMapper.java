package org.example.atuo_attend_backend.quote.mapper;

import org.apache.ibatis.annotations.*;
import org.example.atuo_attend_backend.quote.domain.QuotePresetItem;

import java.util.List;

@Mapper
public interface QuotePresetItemMapper {

    @Select("SELECT id, name, complexity, category, sort_order AS sortOrder, enabled, created_at AS createdAt, updated_at AS updatedAt " +
            "FROM biz_quote_preset_item WHERE enabled = 1 ORDER BY sort_order ASC, id ASC")
    List<QuotePresetItem> listEnabled();

    @Select("SELECT id, name, complexity, category, sort_order AS sortOrder, enabled, created_at AS createdAt, updated_at AS updatedAt " +
            "FROM biz_quote_preset_item ORDER BY sort_order ASC, id ASC")
    List<QuotePresetItem> listAll();

    @Select("SELECT id, name, complexity, category, sort_order AS sortOrder, enabled, created_at AS createdAt, updated_at AS updatedAt " +
            "FROM biz_quote_preset_item WHERE id = #{id}")
    QuotePresetItem findById(long id);

    @Insert("INSERT INTO biz_quote_preset_item (name, complexity, category, sort_order, enabled) " +
            "VALUES (#{name}, #{complexity}, #{category}, #{sortOrder}, #{enabled})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(QuotePresetItem row);

    @Update("UPDATE biz_quote_preset_item SET name=#{name}, complexity=#{complexity}, category=#{category}, " +
            "sort_order=#{sortOrder}, enabled=#{enabled}, updated_at=CURRENT_TIMESTAMP WHERE id=#{id}")
    int update(QuotePresetItem row);

    @Delete("DELETE FROM biz_quote_preset_item WHERE id=#{id}")
    int deleteById(long id);
}
