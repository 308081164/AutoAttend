package org.example.atuo_attend_backend.report.mapper;

import org.apache.ibatis.annotations.*;
import org.example.atuo_attend_backend.report.domain.ReportBlacklistItem;

import java.util.List;

@Mapper
public interface ReportBlacklistMapper {

    @Select("SELECT id, email, created_at AS createdAt FROM aa_report_blacklist ORDER BY id DESC")
    List<ReportBlacklistItem> listAll();

    @Select("SELECT id, email, created_at AS createdAt FROM aa_report_blacklist WHERE email = #{email} LIMIT 1")
    ReportBlacklistItem findByEmail(@Param("email") String email);

    @Insert("INSERT INTO aa_report_blacklist (email) VALUES (#{email}) ON DUPLICATE KEY UPDATE email = VALUES(email)")
    int upsert(@Param("email") String email);

    @Delete("DELETE FROM aa_report_blacklist WHERE email = #{email}")
    int deleteByEmail(@Param("email") String email);
}

