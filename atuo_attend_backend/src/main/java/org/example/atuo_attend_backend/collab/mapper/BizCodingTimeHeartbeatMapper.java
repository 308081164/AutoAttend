package org.example.atuo_attend_backend.collab.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.sql.Timestamp;

@Mapper
public interface BizCodingTimeHeartbeatMapper {

    @Insert("INSERT INTO biz_coding_time_heartbeat (tenant_id, project_id, user_id, client_event_id, heartbeat_at, duration_seconds, language, file_fingerprint) "
            + "VALUES (#{tenantId}, #{projectId}, #{userId}, #{clientEventId}, #{heartbeatAt}, #{durationSeconds}, #{language}, #{fileFingerprint}) "
            + "ON DUPLICATE KEY UPDATE id = id")
    int insertIgnoreDuplicate(@Param("tenantId") long tenantId,
                              @Param("projectId") long projectId,
                              @Param("userId") long userId,
                              @Param("clientEventId") String clientEventId,
                              @Param("heartbeatAt") Timestamp heartbeatAt,
                              @Param("durationSeconds") int durationSeconds,
                              @Param("language") String language,
                              @Param("fileFingerprint") String fileFingerprint);

    @Select("SELECT COALESCE(SUM(duration_seconds), 0) FROM biz_coding_time_heartbeat "
            + "WHERE project_id = #{projectId} AND user_id = #{userId} "
            + "AND heartbeat_at >= #{from} AND heartbeat_at < #{to}")
    long sumDurationSeconds(@Param("projectId") long projectId,
                            @Param("userId") long userId,
                            @Param("from") Timestamp from,
                            @Param("to") Timestamp to);
}
