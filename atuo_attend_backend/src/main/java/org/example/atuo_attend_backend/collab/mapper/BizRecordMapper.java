package org.example.atuo_attend_backend.collab.mapper;

import org.apache.ibatis.annotations.*;
import org.example.atuo_attend_backend.collab.domain.BizRecord;
import org.example.atuo_attend_backend.collab.dto.CollabRecordFilterRule;

import java.util.List;

@Mapper
public interface BizRecordMapper {

    @Select("SELECT * FROM biz_record WHERE id = #{id}")
    BizRecord findById(@Param("id") long id);

    @Select("SELECT * FROM biz_record WHERE table_id = #{tableId} ORDER BY id DESC LIMIT #{limit} OFFSET #{offset}")
    List<BizRecord> listByTableId(@Param("tableId") long tableId, @Param("offset") int offset, @Param("limit") int limit);

    @Select("SELECT COUNT(1) FROM biz_record WHERE table_id = #{tableId}")
    long countByTableId(@Param("tableId") long tableId);

    @Select("<script>"
        + "SELECT * FROM biz_record r "
        + "WHERE r.table_id = #{tableId} "
        + "<if test='rules != null and rules.size() &gt; 0'>"
        + "  <foreach collection='rules' item='rule' separator=' '>"
        + "    AND ("
        + "      <choose>"
        + "        <when test=\"rule.op == 'eq'\">"
        + "          EXISTS (SELECT 1 FROM biz_record_field rf "
        + "                  WHERE rf.record_id = r.id "
        + "                    AND rf.column_id = #{rule.columnId} "
        + "                    AND LOWER(rf.value_text) = LOWER(#{rule.value}))"
        + "        </when>"
        + "        <when test=\"rule.op == 'ne'\">"
        + "          ( NOT EXISTS (SELECT 1 FROM biz_record_field rf "
        + "                         WHERE rf.record_id = r.id "
        + "                           AND rf.column_id = #{rule.columnId}) "
        + "            OR EXISTS (SELECT 1 FROM biz_record_field rf "
        + "                        WHERE rf.record_id = r.id "
        + "                          AND rf.column_id = #{rule.columnId} "
        + "                          AND (rf.value_text IS NULL OR LOWER(rf.value_text) &lt;&gt; LOWER(#{rule.value})))"
        + "        </when>"
        + "        <when test=\"rule.op == 'contains'\">"
        + "          EXISTS (SELECT 1 FROM biz_record_field rf "
        + "                  WHERE rf.record_id = r.id "
        + "                    AND rf.column_id = #{rule.columnId} "
        + "                    AND rf.value_text IS NOT NULL "
        + "                    AND LOWER(rf.value_text) LIKE CONCAT('%', LOWER(#{rule.value}), '%'))"
        + "        </when>"
        + "        <when test=\"rule.op == 'not_contains'\">"
        + "          ( NOT EXISTS (SELECT 1 FROM biz_record_field rf "
        + "                         WHERE rf.record_id = r.id "
        + "                           AND rf.column_id = #{rule.columnId}) "
        + "            OR EXISTS (SELECT 1 FROM biz_record_field rf "
        + "                        WHERE rf.record_id = r.id "
        + "                          AND rf.column_id = #{rule.columnId} "
        + "                          AND (rf.value_text IS NULL "
        + "                               OR LOWER(rf.value_text) NOT LIKE CONCAT('%', LOWER(#{rule.value}), '%'))))"
        + "        </when>"
        + "        <when test=\"rule.op == 'empty'\">"
        + "          ( NOT EXISTS (SELECT 1 FROM biz_record_field rf "
        + "                         WHERE rf.record_id = r.id "
        + "                           AND rf.column_id = #{rule.columnId}) "
        + "            OR EXISTS (SELECT 1 FROM biz_record_field rf "
        + "                        WHERE rf.record_id = r.id "
        + "                          AND rf.column_id = #{rule.columnId} "
        + "                          AND (rf.value_text IS NULL OR rf.value_text = ''))) "
        + "        </when>"
        + "        <when test=\"rule.op == 'not_empty'\">"
        + "          EXISTS (SELECT 1 FROM biz_record_field rf "
        + "                  WHERE rf.record_id = r.id "
        + "                    AND rf.column_id = #{rule.columnId} "
        + "                    AND rf.value_text IS NOT NULL "
        + "                    AND rf.value_text != '')"
        + "        </when>"
        + "        <otherwise>"
        + "          1 = 0"
        + "        </otherwise>"
        + "      </choose>"
        + "    )"
        + "  </foreach>"
        + "</if>"
        + "ORDER BY r.id DESC LIMIT #{limit} OFFSET #{offset}"
        + "</script>")
    List<BizRecord> listByTableIdWithFilters(@Param("tableId") long tableId,
                                              @Param("offset") int offset,
                                              @Param("limit") int limit,
                                              @Param("rules") List<CollabRecordFilterRule> rules);

    @Select("<script>"
        + "SELECT COUNT(1) FROM biz_record r "
        + "WHERE r.table_id = #{tableId} "
        + "<if test='rules != null and rules.size() &gt; 0'>"
        + "  <foreach collection='rules' item='rule' separator=' '>"
        + "    AND ("
        + "      <choose>"
        + "        <when test=\"rule.op == 'eq'\">"
        + "          EXISTS (SELECT 1 FROM biz_record_field rf "
        + "                  WHERE rf.record_id = r.id "
        + "                    AND rf.column_id = #{rule.columnId} "
        + "                    AND LOWER(rf.value_text) = LOWER(#{rule.value}))"
        + "        </when>"
        + "        <when test=\"rule.op == 'ne'\">"
        + "          ( NOT EXISTS (SELECT 1 FROM biz_record_field rf "
        + "                         WHERE rf.record_id = r.id "
        + "                           AND rf.column_id = #{rule.columnId}) "
        + "            OR EXISTS (SELECT 1 FROM biz_record_field rf "
        + "                        WHERE rf.record_id = r.id "
        + "                          AND rf.column_id = #{rule.columnId} "
        + "                          AND (rf.value_text IS NULL OR LOWER(rf.value_text) &lt;&gt; LOWER(#{rule.value})))"
        + "        </when>"
        + "        <when test=\"rule.op == 'contains'\">"
        + "          EXISTS (SELECT 1 FROM biz_record_field rf "
        + "                  WHERE rf.record_id = r.id "
        + "                    AND rf.column_id = #{rule.columnId} "
        + "                    AND rf.value_text IS NOT NULL "
        + "                    AND LOWER(rf.value_text) LIKE CONCAT('%', LOWER(#{rule.value}), '%'))"
        + "        </when>"
        + "        <when test=\"rule.op == 'not_contains'\">"
        + "          ( NOT EXISTS (SELECT 1 FROM biz_record_field rf "
        + "                         WHERE rf.record_id = r.id "
        + "                           AND rf.column_id = #{rule.columnId}) "
        + "            OR EXISTS (SELECT 1 FROM biz_record_field rf "
        + "                        WHERE rf.record_id = r.id "
        + "                          AND rf.column_id = #{rule.columnId} "
        + "                          AND (rf.value_text IS NULL "
        + "                               OR LOWER(rf.value_text) NOT LIKE CONCAT('%', LOWER(#{rule.value}), '%'))))"
        + "        </when>"
        + "        <when test=\"rule.op == 'empty'\">"
        + "          ( NOT EXISTS (SELECT 1 FROM biz_record_field rf "
        + "                         WHERE rf.record_id = r.id "
        + "                           AND rf.column_id = #{rule.columnId}) "
        + "            OR EXISTS (SELECT 1 FROM biz_record_field rf "
        + "                        WHERE rf.record_id = r.id "
        + "                          AND rf.column_id = #{rule.columnId} "
        + "                          AND (rf.value_text IS NULL OR rf.value_text = ''))) "
        + "        </when>"
        + "        <when test=\"rule.op == 'not_empty'\">"
        + "          EXISTS (SELECT 1 FROM biz_record_field rf "
        + "                  WHERE rf.record_id = r.id "
        + "                    AND rf.column_id = #{rule.columnId} "
        + "                    AND rf.value_text IS NOT NULL "
        + "                    AND rf.value_text != '')"
        + "        </when>"
        + "        <otherwise>"
        + "          1 = 0"
        + "        </otherwise>"
        + "      </choose>"
        + "    )"
        + "  </foreach>"
        + "</if>"
        + "</script>")
    long countByTableIdWithFilters(@Param("tableId") long tableId,
                                     @Param("rules") List<CollabRecordFilterRule> rules);

    @Insert("INSERT INTO biz_record (table_id, created_by) VALUES (#{tableId}, #{createdBy})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(BizRecord record);

    @Update("UPDATE biz_record SET created_by = #{createdBy} WHERE id = #{id}")
    int update(BizRecord record);

    @Delete("DELETE FROM biz_record WHERE id = #{id}")
    int deleteById(@Param("id") long id);
}
