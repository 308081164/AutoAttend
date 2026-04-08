package org.example.atuo_attend_backend.report.mapper;

import org.apache.ibatis.annotations.*;
import org.example.atuo_attend_backend.report.domain.ReportConfig;

@Mapper
public interface ReportConfigMapper {

    @Select("""
            SELECT id, enabled, schedule_type AS scheduleType, schedule_param AS scheduleParam,
                   cron_expression AS cronExpression, company_name AS companyName, daily_extra_message AS dailyExtraMessage,
                   created_at AS createdAt, updated_at AS updatedAt
            FROM aa_report_config
            ORDER BY id DESC
            LIMIT 1
            """)
    ReportConfig getLatest();

    @Insert("""
            INSERT INTO aa_report_config (enabled, schedule_type, schedule_param, cron_expression, company_name, daily_extra_message)
            VALUES (#{enabled}, #{scheduleType}, #{scheduleParam}, #{cronExpression}, #{companyName}, #{dailyExtraMessage})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(ReportConfig cfg);

    @Update("""
            UPDATE aa_report_config
            SET enabled = #{enabled},
                schedule_type = #{scheduleType},
                schedule_param = #{scheduleParam},
                cron_expression = #{cronExpression},
                company_name = #{companyName},
                daily_extra_message = #{dailyExtraMessage},
                updated_at = CURRENT_TIMESTAMP
            WHERE id = #{id}
            """)
    int update(ReportConfig cfg);
}

