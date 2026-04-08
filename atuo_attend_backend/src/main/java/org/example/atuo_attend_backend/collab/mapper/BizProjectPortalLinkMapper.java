package org.example.atuo_attend_backend.collab.mapper;

import org.apache.ibatis.annotations.*;
import org.example.atuo_attend_backend.collab.domain.BizProjectPortalLink;

import java.util.List;

@Mapper
public interface BizProjectPortalLinkMapper {

    @Select("""
            SELECT id, project_id AS projectId, label, url, sort_order AS sortOrder,
                   created_at AS createdAt, updated_at AS updatedAt
            FROM biz_project_portal_link
            WHERE project_id = #{projectId}
            ORDER BY sort_order ASC, id ASC
            """)
    List<BizProjectPortalLink> listByProjectId(@Param("projectId") long projectId);

    @Select("""
            SELECT id, project_id AS projectId, label, url, sort_order AS sortOrder,
                   created_at AS createdAt, updated_at AS updatedAt
            FROM biz_project_portal_link
            WHERE id = #{id}
            LIMIT 1
            """)
    BizProjectPortalLink findById(@Param("id") long id);

    @Insert("""
            INSERT INTO biz_project_portal_link (project_id, label, url, sort_order)
            VALUES (#{projectId}, #{label}, #{url}, #{sortOrder})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(BizProjectPortalLink link);

    @Update("""
            UPDATE biz_project_portal_link
            SET label = #{label}, url = #{url}, sort_order = #{sortOrder}, updated_at = CURRENT_TIMESTAMP
            WHERE id = #{id}
            """)
    int update(BizProjectPortalLink link);

    @Delete("DELETE FROM biz_project_portal_link WHERE id = #{id}")
    int deleteById(@Param("id") long id);

    @Delete("DELETE FROM biz_project_portal_link WHERE project_id = #{projectId}")
    int deleteByProjectId(@Param("projectId") long projectId);
}

