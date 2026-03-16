package org.example.atuo_attend_backend.collab.mapper;

import org.apache.ibatis.annotations.*;
import org.example.atuo_attend_backend.collab.domain.BizAttachment;

import java.util.List;

@Mapper
public interface BizAttachmentMapper {

    @Select("SELECT * FROM biz_attachment WHERE id = #{id}")
    BizAttachment findById(@Param("id") long id);

    @Select("SELECT * FROM biz_attachment WHERE record_id = #{recordId}")
    List<BizAttachment> listByRecordId(@Param("recordId") long recordId);

    @Insert("INSERT INTO biz_attachment (project_id, record_id, file_name, file_size, storage_key, uploaded_by) VALUES (#{projectId}, #{recordId}, #{fileName}, #{fileSize}, #{storageKey}, #{uploadedBy})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(BizAttachment attachment);

    @Delete("DELETE FROM biz_attachment WHERE id = #{id}")
    int deleteById(@Param("id") long id);

    @Select("SELECT * FROM biz_attachment WHERE project_id = #{projectId} ORDER BY id DESC")
    List<BizAttachment> listByProjectId(@Param("projectId") long projectId);

    @Update("UPDATE biz_attachment SET record_id = #{recordId} WHERE id = #{id}")
    int updateRecordId(@Param("id") long id, @Param("recordId") long recordId);
}
