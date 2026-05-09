package org.example.atuo_attend_backend.xianyu.mapper;

import org.apache.ibatis.annotations.*;
import org.example.atuo_attend_backend.xianyu.domain.XianyuAccount;

import java.util.List;

@Mapper
public interface XianyuAccountMapper {

    @Insert("INSERT INTO aa_xianyu_account (tenant_id, nickname, avatar_url, cookie_encrypted, status, last_login_at, last_active_at, created_at, updated_at) " +
            "VALUES (#{tenantId}, #{nickname}, #{avatarUrl}, #{cookieEncrypted}, #{status}, #{lastLoginAt}, #{lastActiveAt}, NOW(), NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(XianyuAccount account);

    @Update("UPDATE aa_xianyu_account SET nickname=#{nickname}, avatar_url=#{avatarUrl}, cookie_encrypted=#{cookieEncrypted}, " +
            "status=#{status}, last_login_at=#{lastLoginAt}, last_active_at=#{lastActiveAt}, updated_at=NOW() WHERE id=#{id}")
    int update(XianyuAccount account);

    @Update("UPDATE aa_xianyu_account SET status=#{status}, updated_at=NOW() WHERE id=#{id}")
    int updateStatus(@Param("id") Long id, @Param("status") String status);

    @Update("UPDATE aa_xianyu_account SET last_active_at=NOW(), updated_at=NOW() WHERE id=#{id}")
    int updateActiveTime(@Param("id") Long id);

    @Delete("DELETE FROM aa_xianyu_account WHERE id=#{id}")
    int deleteById(@Param("id") Long id);

    @Select("SELECT * FROM aa_xianyu_account WHERE id=#{id}")
    XianyuAccount selectById(@Param("id") Long id);

    @Select("SELECT * FROM aa_xianyu_account WHERE tenant_id=#{tenantId} ORDER BY created_at DESC")
    List<XianyuAccount> selectByTenantId(@Param("tenantId") Long tenantId);

    @Select("SELECT * FROM aa_xianyu_account WHERE tenant_id=#{tenantId} AND status=#{status} ORDER BY created_at DESC")
    List<XianyuAccount> selectByTenantIdAndStatus(@Param("tenantId") Long tenantId, @Param("status") String status);
}
