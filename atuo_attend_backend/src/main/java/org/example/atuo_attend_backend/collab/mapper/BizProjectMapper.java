package org.example.atuo_attend_backend.collab.mapper;

import org.apache.ibatis.annotations.*;
import org.example.atuo_attend_backend.collab.domain.BizProject;

import java.util.List;

@Mapper
public interface BizProjectMapper {

    @Select("SELECT * FROM biz_project WHERE id = #{id}")
    BizProject findById(@Param("id") long id);

    @Select("SELECT * FROM biz_project WHERE repo_id = #{repoId}")
    BizProject findByRepoId(@Param("repoId") String repoId);

    @Select("SELECT * FROM biz_project ORDER BY id")
    List<BizProject> listAll();

    @Insert("INSERT INTO biz_project (name, description, repo_id, status) VALUES (#{name}, #{description}, #{repoId}, #{status})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(BizProject project);

    @Update("UPDATE biz_project SET name = #{name}, description = #{description}, status = #{status} WHERE id = #{id}")
    int update(BizProject project);
}
