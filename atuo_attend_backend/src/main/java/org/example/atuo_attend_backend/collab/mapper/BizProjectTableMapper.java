package org.example.atuo_attend_backend.collab.mapper;

import org.apache.ibatis.annotations.*;
import org.example.atuo_attend_backend.collab.domain.BizProjectTable;

@Mapper
public interface BizProjectTableMapper {

    @Select("SELECT * FROM biz_project_table WHERE id = #{id}")
    BizProjectTable findById(@Param("id") long id);

    @Select("SELECT * FROM biz_project_table WHERE project_id = #{projectId}")
    BizProjectTable findByProjectId(@Param("projectId") long projectId);

    @Insert("INSERT INTO biz_project_table (project_id, name) VALUES (#{projectId}, #{name})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(BizProjectTable table);
}
