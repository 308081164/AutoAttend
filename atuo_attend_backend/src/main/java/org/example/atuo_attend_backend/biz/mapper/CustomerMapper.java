package org.example.atuo_attend_backend.biz.mapper;

import org.apache.ibatis.annotations.*;
import org.example.atuo_attend_backend.biz.domain.Customer;

import java.util.List;

@Mapper
public interface CustomerMapper {

    @Insert("""
            INSERT INTO aa_customer (tenant_id, name, company, phone, email, source, stage, tags, assigned_to, last_contacted_at)
            VALUES (#{tenantId}, #{name}, #{company}, #{phone}, #{email}, #{source}, #{stage}, #{tags}, #{assignedTo}, #{lastContactedAt})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    int insert(Customer c);

    @Update("""
            UPDATE aa_customer SET name=#{name}, company=#{company}, phone=#{phone}, email=#{email},
                source=#{source}, stage=#{stage}, tags=#{tags}, assigned_to=#{assignedTo},
                last_contacted_at=#{lastContactedAt}, updated_at=CURRENT_TIMESTAMP
            WHERE tenant_id=#{tenantId} AND id=#{id}
            """)
    int update(Customer c);

    @Select("SELECT id, tenant_id AS tenantId, name, company, phone, email, source, stage, tags, assigned_to AS assignedTo, " +
            "last_contacted_at AS lastContactedAt, created_at AS createdAt, updated_at AS updatedAt " +
            "FROM aa_customer WHERE tenant_id = #{tenantId} AND id = #{id}")
    Customer findById(@Param("tenantId") long tenantId, @Param("id") long id);

    @Select("SELECT id, tenant_id AS tenantId, name, company, phone, email, source, stage, tags, assigned_to AS assignedTo, " +
            "last_contacted_at AS lastContactedAt, created_at AS createdAt, updated_at AS updatedAt " +
            "FROM aa_customer WHERE tenant_id = #{tenantId} ORDER BY updated_at DESC LIMIT #{limit} OFFSET #{offset}")
    List<Customer> listPaged(@Param("tenantId") long tenantId, @Param("offset") int offset, @Param("limit") int limit);

    @Select("SELECT COUNT(*) FROM aa_customer WHERE tenant_id = #{tenantId}")
    long countAll(@Param("tenantId") long tenantId);

    @Select("SELECT id, tenant_id AS tenantId, name, company, phone, email, source, stage, tags, assigned_to AS assignedTo, " +
            "last_contacted_at AS lastContactedAt, created_at AS createdAt, updated_at AS updatedAt " +
            "FROM aa_customer WHERE tenant_id = #{tenantId} AND (name LIKE #{keyword} OR company LIKE #{keyword} OR phone LIKE #{keyword} OR email LIKE #{keyword}) " +
            "ORDER BY updated_at DESC LIMIT #{limit} OFFSET #{offset}")
    List<Customer> search(@Param("tenantId") long tenantId, @Param("keyword") String keyword,
                          @Param("offset") int offset, @Param("limit") int limit);

    @Select("SELECT COUNT(*) FROM aa_customer WHERE tenant_id = #{tenantId} AND (name LIKE #{keyword} OR company LIKE #{keyword} OR phone LIKE #{keyword} OR email LIKE #{keyword})")
    long countSearch(@Param("tenantId") long tenantId, @Param("keyword") String keyword);

    @Delete("DELETE FROM aa_customer WHERE tenant_id = #{tenantId} AND id = #{id}")
    int deleteById(@Param("tenantId") long tenantId, @Param("id") long id);
}
