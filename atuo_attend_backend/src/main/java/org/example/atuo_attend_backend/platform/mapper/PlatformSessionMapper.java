package org.example.atuo_attend_backend.platform.mapper;

import org.apache.ibatis.annotations.*;
import org.example.atuo_attend_backend.platform.domain.PlatformSession;

@Mapper
public interface PlatformSessionMapper {

    @Select("SELECT id, token, expires_at AS expiresAt, created_at AS createdAt FROM aa_platform_session "
            + "WHERE token = #{token} AND expires_at > NOW()")
    PlatformSession findValidByToken(@Param("token") String token);

    @Insert("INSERT INTO aa_platform_session (token, expires_at) VALUES (#{token}, NOW() + INTERVAL 7200 SECOND)")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(PlatformSession session);

    @Delete("DELETE FROM aa_platform_session WHERE expires_at < NOW()")
    int deleteExpired();
}
