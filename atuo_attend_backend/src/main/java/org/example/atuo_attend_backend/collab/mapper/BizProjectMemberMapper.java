package org.example.atuo_attend_backend.collab.mapper;

import org.apache.ibatis.annotations.*;
import org.example.atuo_attend_backend.collab.domain.BizProjectMember;

import java.util.List;

@Mapper
public interface BizProjectMemberMapper {

    @Select("SELECT * FROM biz_project_member WHERE project_id = #{projectId} AND user_id = #{userId}")
    BizProjectMember findByProjectAndUser(@Param("projectId") long projectId, @Param("userId") long userId);

    @Select("SELECT * FROM biz_project_member WHERE project_id = #{projectId}")
    List<BizProjectMember> listByProjectId(@Param("projectId") long projectId);

    @Select("SELECT project_id FROM biz_project_member WHERE user_id = #{userId}")
    List<Long> listProjectIdsByUserId(@Param("userId") long userId);

    @Select("SELECT id, project_id AS projectId, user_id AS userId, role, source, created_at AS createdAt FROM biz_project_member WHERE user_id = #{userId}")
    List<BizProjectMember> listByUserId(@Param("userId") long userId);

    @Select("SELECT project_id FROM biz_project_member WHERE user_id = #{userId} AND role = #{role}")
    List<Long> listProjectIdsByUserIdAndRole(@Param("userId") long userId, @Param("role") String role);

    @Insert("INSERT INTO biz_project_member (project_id, user_id, role, source) VALUES (#{projectId}, #{userId}, #{role}, #{source})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(BizProjectMember member);

    @Update("UPDATE biz_project_member SET role = #{role} WHERE id = #{id}")
    int updateRole(@Param("id") long id, @Param("role") String role);

    @Delete("DELETE FROM biz_project_member WHERE project_id = #{projectId} AND user_id = #{userId}")
    int delete(@Param("projectId") long projectId, @Param("userId") long userId);
}
