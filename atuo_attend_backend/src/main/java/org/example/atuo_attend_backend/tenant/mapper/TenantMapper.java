package org.example.atuo_attend_backend.tenant.mapper;

import org.apache.ibatis.annotations.*;
import org.example.atuo_attend_backend.tenant.domain.Tenant;

import java.util.List;

@Mapper
public interface TenantMapper {

    @Select("SELECT id, name, slug, plan_code AS planCode, status, created_at AS createdAt FROM aa_tenant WHERE id = #{id}")
    Tenant findById(@Param("id") long id);

    @Select("SELECT id, name, slug, plan_code AS planCode, status, created_at AS createdAt FROM aa_tenant WHERE slug = #{slug}")
    Tenant findBySlug(@Param("slug") String slug);

    @Select("SELECT id, name, slug, plan_code AS planCode, status, created_at AS createdAt FROM aa_tenant ORDER BY id")
    List<Tenant> listAll();

    @Insert("INSERT INTO aa_tenant (name, slug, plan_code, status) VALUES (#{name}, #{slug}, #{planCode}, #{status})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Tenant tenant);

    @Update("UPDATE aa_tenant SET plan_code = #{planCode} WHERE id = #{id}")
    int updatePlanCode(@Param("id") long id, @Param("planCode") String planCode);

    @Select("SELECT COUNT(*) FROM aa_tenant WHERE slug = #{slug}")
    int countBySlug(@Param("slug") String slug);
}
