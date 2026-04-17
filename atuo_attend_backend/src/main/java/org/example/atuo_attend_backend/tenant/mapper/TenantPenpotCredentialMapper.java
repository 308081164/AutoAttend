package org.example.atuo_attend_backend.tenant.mapper;

import org.apache.ibatis.annotations.*;
import org.example.atuo_attend_backend.tenant.domain.TenantPenpotCredential;

@Mapper
public interface TenantPenpotCredentialMapper {

    @Select("SELECT tenant_id AS tenantId, penpot_email AS penpotEmail, password_enc AS passwordEnc, "
            + "access_token_enc AS accessTokenEnc, created_at AS createdAt, updated_at AS updatedAt "
            + "FROM aa_tenant_penpot_credential WHERE tenant_id = #{tenantId}")
    TenantPenpotCredential findByTenantId(@Param("tenantId") long tenantId);

    @Insert("INSERT INTO aa_tenant_penpot_credential (tenant_id, penpot_email, password_enc, access_token_enc) "
            + "VALUES (#{tenantId}, #{penpotEmail}, #{passwordEnc}, #{accessTokenEnc})")
    int insert(TenantPenpotCredential row);

    @Update("UPDATE aa_tenant_penpot_credential SET access_token_enc = #{accessTokenEnc}, updated_at = CURRENT_TIMESTAMP "
            + "WHERE tenant_id = #{tenantId}")
    int updateAccessToken(@Param("tenantId") long tenantId, @Param("accessTokenEnc") String accessTokenEnc);
}
