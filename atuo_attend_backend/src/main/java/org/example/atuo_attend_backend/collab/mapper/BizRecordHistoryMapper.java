package org.example.atuo_attend_backend.collab.mapper;

import org.apache.ibatis.annotations.*;
import org.example.atuo_attend_backend.collab.domain.BizRecordHistory;

import java.util.List;
import java.util.Map;

@Mapper
public interface BizRecordHistoryMapper {

    @Insert("INSERT INTO biz_record_history (" +
            "project_id, table_id, record_id, column_id, action, old_value, new_value, " +
            "operator_user_id, operator_system_role, operator_project_role, source" +
            ") VALUES (" +
            "#{projectId}, #{tableId}, #{recordId}, #{columnId}, #{action}, #{oldValue}, #{newValue}, " +
            "#{operatorUserId}, #{operatorSystemRole}, #{operatorProjectRole}, #{source}" +
            ")")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(BizRecordHistory item);

    @Select("SELECT h.id, h.record_id AS recordId, h.column_id AS columnId, c.name AS columnName, " +
            "h.action, h.old_value AS oldValue, h.new_value AS newValue, h.operator_user_id AS operatorUserId, " +
            "u.name AS operatorName, u.email AS operatorEmail, h.operator_system_role AS operatorSystemRole, " +
            "h.operator_project_role AS operatorProjectRole, h.source, h.created_at AS createdAt " +
            "FROM biz_record_history h " +
            "LEFT JOIN biz_user u ON u.id = h.operator_user_id " +
            "LEFT JOIN biz_table_column c ON c.id = h.column_id " +
            "WHERE h.record_id = #{recordId} " +
            "ORDER BY h.id DESC LIMIT #{limit} OFFSET #{offset}")
    List<Map<String, Object>> listByRecordId(@Param("recordId") long recordId,
                                             @Param("offset") int offset,
                                             @Param("limit") int limit);

    @Select("SELECT COUNT(1) FROM biz_record_history WHERE record_id = #{recordId}")
    long countByRecordId(@Param("recordId") long recordId);
}
