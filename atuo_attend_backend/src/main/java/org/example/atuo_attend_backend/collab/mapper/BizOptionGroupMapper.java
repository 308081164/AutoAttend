package org.example.atuo_attend_backend.collab.mapper;

import org.apache.ibatis.annotations.*;
import org.example.atuo_attend_backend.collab.domain.BizOptionGroup;

import java.util.List;

@Mapper
public interface BizOptionGroupMapper {

    @Select("SELECT * FROM biz_option_group WHERE id = #{id}")
    BizOptionGroup findById(@Param("id") long id);

    @Select("SELECT * FROM biz_option_group WHERE scope = 'global' OR (scope = 'project' AND project_id = #{projectId})")
    List<BizOptionGroup> listByProject(@Param("projectId") long projectId);

    @Insert("INSERT INTO biz_option_group (name, options, scope, project_id) VALUES (#{name}, #{options}, #{scope}, #{projectId})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(BizOptionGroup group);

    @Update("UPDATE biz_option_group SET name = #{name}, options = #{options} WHERE id = #{id}")
    int update(BizOptionGroup group);
}
