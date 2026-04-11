package org.example.atuo_attend_backend.nexus.mapper;

import org.apache.ibatis.annotations.*;
import org.example.atuo_attend_backend.nexus.domain.NexusAlertRule;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface NexusAlertRuleMapper {

    @Select("""
            SELECT
              id,
              tenant_id AS tenantId,
              account_id AS accountId,
              instance_id AS instanceId,
              name,
              metric_type AS metricType,
              op,
              threshold,
              duration_minutes AS durationMinutes,
              webhook_url AS webhookUrl,
              notify_email AS notifyEmail,
              silence_seconds AS silenceSeconds,
              enabled,
              last_triggered_at AS lastTriggeredAt,
              created_at AS createdAt,
              updated_at AS updatedAt
            FROM aa_nexus_alert_rule
            WHERE tenant_id = #{tenantId}
            ORDER BY id DESC
            """)
    List<NexusAlertRule> listByTenant(@Param("tenantId") long tenantId);

    @Select("""
            SELECT
              id,
              tenant_id AS tenantId,
              account_id AS accountId,
              instance_id AS instanceId,
              name,
              metric_type AS metricType,
              op,
              threshold,
              duration_minutes AS durationMinutes,
              webhook_url AS webhookUrl,
              notify_email AS notifyEmail,
              silence_seconds AS silenceSeconds,
              enabled,
              last_triggered_at AS lastTriggeredAt,
              created_at AS createdAt,
              updated_at AS updatedAt
            FROM aa_nexus_alert_rule
            WHERE enabled = 1
            ORDER BY tenant_id, id
            """)
    List<NexusAlertRule> listAllEnabled();

    @Select("""
            SELECT
              id,
              tenant_id AS tenantId,
              account_id AS accountId,
              instance_id AS instanceId,
              name,
              metric_type AS metricType,
              op,
              threshold,
              duration_minutes AS durationMinutes,
              webhook_url AS webhookUrl,
              notify_email AS notifyEmail,
              silence_seconds AS silenceSeconds,
              enabled,
              last_triggered_at AS lastTriggeredAt,
              created_at AS createdAt,
              updated_at AS updatedAt
            FROM aa_nexus_alert_rule
            WHERE tenant_id = #{tenantId} AND id = #{id}
            LIMIT 1
            """)
    NexusAlertRule findById(@Param("tenantId") long tenantId, @Param("id") long id);

    @Insert("""
            INSERT INTO aa_nexus_alert_rule
              (tenant_id, account_id, instance_id, name, metric_type, op, threshold, duration_minutes,
               webhook_url, notify_email, silence_seconds, enabled)
            VALUES
              (#{tenantId}, #{accountId}, #{instanceId}, #{name}, #{metricType}, #{op}, #{threshold}, #{durationMinutes},
               #{webhookUrl}, #{notifyEmail}, #{silenceSeconds}, #{enabled})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(NexusAlertRule r);

    @Update("""
            UPDATE aa_nexus_alert_rule
            SET account_id = #{accountId},
                instance_id = #{instanceId},
                name = #{name},
                metric_type = #{metricType},
                op = #{op},
                threshold = #{threshold},
                duration_minutes = #{durationMinutes},
                webhook_url = #{webhookUrl},
                notify_email = #{notifyEmail},
                silence_seconds = #{silenceSeconds},
                enabled = #{enabled},
                updated_at = CURRENT_TIMESTAMP
            WHERE tenant_id = #{tenantId} AND id = #{id}
            """)
    int update(NexusAlertRule r);

    @Update("""
            UPDATE aa_nexus_alert_rule
            SET last_triggered_at = #{lastTriggeredAt},
                updated_at = CURRENT_TIMESTAMP
            WHERE tenant_id = #{tenantId} AND id = #{id}
            """)
    int updateLastTriggered(@Param("tenantId") long tenantId,
                            @Param("id") long id,
                            @Param("lastTriggeredAt") LocalDateTime lastTriggeredAt);

    @Delete("""
            DELETE FROM aa_nexus_alert_rule
            WHERE tenant_id = #{tenantId} AND id = #{id}
            """)
    int delete(@Param("tenantId") long tenantId, @Param("id") long id);
}
