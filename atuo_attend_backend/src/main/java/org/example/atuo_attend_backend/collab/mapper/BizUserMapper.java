package org.example.atuo_attend_backend.collab.mapper;

import org.apache.ibatis.annotations.*;
import org.example.atuo_attend_backend.collab.domain.BizUser;

@Mapper
public interface BizUserMapper {

    @Select("SELECT * FROM biz_user WHERE id = #{id}")
    BizUser findById(@Param("id") long id);

    @Select("SELECT * FROM biz_user WHERE email = #{email}")
    BizUser findByEmail(@Param("email") String email);

    @Insert("INSERT INTO biz_user (email, name, password_hash, role) VALUES (#{email}, #{name}, #{passwordHash}, #{role})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(BizUser user);

    @Update("UPDATE biz_user SET name = #{name}, password_hash = #{passwordHash}, role = #{role} WHERE id = #{id}")
    int update(BizUser user);
}
