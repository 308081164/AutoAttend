package org.example.atuo_attend_backend.report.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;

@Mapper
public interface ReportSendLogMapper {

    @Insert("""
            INSERT INTO aa_report_send_log (email, report_date, status, error_message, report_type, project_id, repo_full_name)
            VALUES (#{email}, #{reportDate}, #{status}, #{errorMessage}, #{reportType}, #{projectId}, #{repoFullName})
            """)
    int insert(@Param("email") String email,
               @Param("reportDate") LocalDate reportDate,
               @Param("status") String status,
               @Param("errorMessage") String errorMessage,
               @Param("reportType") String reportType,
               @Param("projectId") Long projectId,
               @Param("repoFullName") String repoFullName);
}

