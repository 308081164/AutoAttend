package org.example.atuo_attend_backend.tenant.mapper;

import org.apache.ibatis.annotations.*;
import org.example.atuo_attend_backend.tenant.domain.Tenant;

import java.util.List;

@Mapper
public interface TenantMapper {

    @Select("SELECT id, name, slug, created_at AS createdAt FROM aa_tenant WHERE id = #{id}")
    Tenant findById(@Param("id") long id);

    @Select("SELECT id, name, slug, created_at AS createdAt FROM aa_tenant WHERE slug = #{slug}")
    Tenant findBySlug(@Param("slug") String slug);

    @Select("SELECT id, name, slug, created_at AS createdAt FROM aa_tenant ORDER BY id")
    List<Tenant> listAll();

    @Insert("INSERT INTO aa_tenant (name, slug) VALUES (#{name}, #{slug})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Tenant tenant);

    @Select("SELECT COUNT(*) FROM aa_tenant WHERE slug = #{slug}")
    int countBySlug(@Param("slug") String slug);
}
