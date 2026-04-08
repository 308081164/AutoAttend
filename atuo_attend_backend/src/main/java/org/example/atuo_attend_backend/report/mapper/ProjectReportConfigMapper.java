package org.example.atuo_attend_backend.report.mapper;

import org.apache.ibatis.annotations.*;
import org.example.atuo_attend_backend.report.domain.ProjectReportConfig;

import java.util.List;

@Mapper
public interface ProjectReportConfigMapper {

    @Select("""
            SELECT id, project_id AS projectId, repo_full_name AS repoFullName,
                   enabled, send_to_managers AS sendToManagers, send_to_developers AS sendToDevelopers,
                   manager_emails AS managerEmails,
                   created_at AS createdAt, updated_at AS updatedAt
            FROM aa_project_report_config
            WHERE project_id = #{projectId}
            LIMIT 1
            """)
    ProjectReportConfig findByProjectId(@Param("projectId") long projectId);

    @Select("""
            SELECT id, project_id AS projectId, repo_full_name AS repoFullName,
                   enabled, send_to_managers AS sendToManagers, send_to_developers AS sendToDevelopers,
                   manager_emails AS managerEmails,
                   created_at AS createdAt, updated_at AS updatedAt
            FROM aa_project_report_config
            WHERE enabled = 1
            ORDER BY project_id ASC
            """)
    List<ProjectReportConfig> listEnabled();

    @Insert("""
            INSERT INTO aa_project_report_config (project_id, repo_full_name, enabled, send_to_managers, send_to_developers, manager_emails)
            VALUES (#{projectId}, #{repoFullName}, #{enabled}, #{sendToManagers}, #{sendToDevelopers}, #{managerEmails})
            ON DUPLICATE KEY UPDATE
                repo_full_name = VALUES(repo_full_name),
                enabled = VALUES(enabled),
                send_to_managers = VALUES(send_to_managers),
                send_to_developers = VALUES(send_to_developers),
                manager_emails = VALUES(manager_emails),
                updated_at = CURRENT_TIMESTAMP
            """)
    int upsert(ProjectReportConfig cfg);
}

