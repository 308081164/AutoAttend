package org.example.atuo_attend_backend.config;

import org.apache.ibatis.annotations.*;

@Mapper
public interface SystemConfigMapper {

    @Select("SELECT config_value FROM aa_system_config WHERE config_key = #{key} LIMIT 1")
    String findByKey(@Param("key") String key);

    @Insert("INSERT INTO aa_system_config (config_key, config_value) VALUES (#{key}, #{value}) ON DUPLICATE KEY UPDATE config_value = VALUES(config_value), updated_at = CURRENT_TIMESTAMP")
    int upsert(@Param("key") String key, @Param("value") String value);
}
