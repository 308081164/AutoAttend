package org.example.atuo_attend_backend.admin.mapper;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.example.atuo_attend_backend.admin.domain.AdminSmsCode;

import java.time.LocalDateTime;

@Mapper
public interface AdminSmsCodeMapper {

    @Insert("""
            INSERT INTO aa_admin_sms_code (phone, purpose, code_hash, expires_at, verify_attempts)
            VALUES (#{phone}, #{purpose}, #{codeHash}, #{expiresAt}, 0)
            """)
    int insert(@Param("phone") String phone,
               @Param("purpose") String purpose,
               @Param("codeHash") String codeHash,
               @Param("expiresAt") LocalDateTime expiresAt);

    @Select("""
            SELECT id, phone, purpose, code_hash AS codeHash, expires_at AS expiresAt, used_at AS usedAt,
                   verify_attempts AS verifyAttempts, created_at AS createdAt
            FROM aa_admin_sms_code
            WHERE phone = #{phone} AND purpose = #{purpose} AND used_at IS NULL AND expires_at > #{now}
            ORDER BY id DESC LIMIT 1
            """)
    AdminSmsCode findLatestValid(@Param("phone") String phone,
                                 @Param("purpose") String purpose,
                                 @Param("now") LocalDateTime now);

    @Update("UPDATE aa_admin_sms_code SET used_at = #{usedAt} WHERE id = #{id} AND used_at IS NULL")
    int markUsed(@Param("id") long id, @Param("usedAt") LocalDateTime usedAt);

    @Update("UPDATE aa_admin_sms_code SET verify_attempts = verify_attempts + 1 WHERE id = #{id}")
    int incrementAttempts(@Param("id") long id);

    @Select("""
            SELECT created_at FROM aa_admin_sms_code
            WHERE phone = #{phone} AND purpose = #{purpose}
            ORDER BY id DESC LIMIT 1
            """)
    LocalDateTime findLastSendTime(@Param("phone") String phone, @Param("purpose") String purpose);

    @Delete("""
            DELETE FROM aa_admin_sms_code
            WHERE phone = #{phone} AND purpose = #{purpose} AND used_at IS NULL
            """)
    int deleteUnusedForPhonePurpose(@Param("phone") String phone, @Param("purpose") String purpose);
}
