package org.example.atuo_attend_backend.report.mapper;

import org.apache.ibatis.annotations.*;
import org.example.atuo_attend_backend.report.domain.ReportRecipient;

import java.util.List;

@Mapper
public interface ReportRecipientMapper {

    @Select("""
            SELECT id, email, display_name AS displayName, unsubscribed_at AS unsubscribedAt,
                   unsubscribe_token AS unsubscribeToken, created_at AS createdAt, updated_at AS updatedAt
            FROM aa_report_recipient
            WHERE email = #{email}
            LIMIT 1
            """)
    ReportRecipient findByEmail(@Param("email") String email);

    @Select("""
            SELECT id, email, display_name AS displayName, unsubscribed_at AS unsubscribedAt,
                   unsubscribe_token AS unsubscribeToken, created_at AS createdAt, updated_at AS updatedAt
            FROM aa_report_recipient
            ORDER BY id DESC
            LIMIT #{limit} OFFSET #{offset}
            """)
    List<ReportRecipient> listPaged(@Param("offset") int offset, @Param("limit") int limit);

    @Select("SELECT COUNT(1) FROM aa_report_recipient")
    long countAll();

    @Insert("""
            INSERT INTO aa_report_recipient (email, display_name, unsubscribed_at, unsubscribe_token)
            VALUES (#{email}, #{displayName}, #{unsubscribedAt}, #{unsubscribeToken})
            ON DUPLICATE KEY UPDATE
                display_name = VALUES(display_name),
                unsubscribe_token = COALESCE(VALUES(unsubscribe_token), unsubscribe_token),
                updated_at = CURRENT_TIMESTAMP
            """)
    int upsert(ReportRecipient r);

    @Update("UPDATE aa_report_recipient SET unsubscribed_at = CURRENT_TIMESTAMP, updated_at = CURRENT_TIMESTAMP WHERE email = #{email}")
    int markUnsubscribed(@Param("email") String email);

    @Update("UPDATE aa_report_recipient SET unsubscribed_at = NULL, updated_at = CURRENT_TIMESTAMP WHERE email = #{email}")
    int clearUnsubscribed(@Param("email") String email);

    @Select("""
            SELECT id, email, display_name AS displayName, unsubscribed_at AS unsubscribedAt,
                   unsubscribe_token AS unsubscribeToken, created_at AS createdAt, updated_at AS updatedAt
            FROM aa_report_recipient
            WHERE unsubscribe_token = #{token}
            LIMIT 1
            """)
    ReportRecipient findByToken(@Param("token") String token);
}

