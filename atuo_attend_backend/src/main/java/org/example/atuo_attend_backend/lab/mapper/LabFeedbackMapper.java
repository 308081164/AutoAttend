package org.example.atuo_attend_backend.lab.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.example.atuo_attend_backend.lab.dto.LabFeedbackListItem;

import java.util.List;

@Mapper
public interface LabFeedbackMapper {

    @Insert("""
            INSERT INTO aa_lab_feedback (tenant_id, content, image_key)
            VALUES (#{tenantId}, #{content}, #{imageKey})
            """)
    int insert(@Param("tenantId") long tenantId,
               @Param("content") String content,
               @Param("imageKey") String imageKey);

    @Select("""
            SELECT f.id, f.tenant_id AS tenantId, COALESCE(t.name, '') AS tenantName,
                   f.content, f.image_key AS imageKey, f.created_at AS createdAt
            FROM aa_lab_feedback f
            LEFT JOIN aa_tenant t ON t.id = f.tenant_id
            ORDER BY f.id DESC
            LIMIT #{limit} OFFSET #{offset}
            """)
    List<LabFeedbackListItem> listPage(@Param("offset") int offset, @Param("limit") int limit);

    @Select("SELECT COUNT(*) FROM aa_lab_feedback")
    long countAll();
}
