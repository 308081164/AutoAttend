package org.example.atuo_attend_backend.biz.mapper;

import org.apache.ibatis.annotations.*;
import org.example.atuo_attend_backend.biz.domain.CustomerFollowup;

import java.util.List;

@Mapper
public interface CustomerFollowupMapper {

    @Insert("""
            INSERT INTO aa_customer_followup (tenant_id, customer_id, method, content, operator_id)
            VALUES (#{tenantId}, #{customerId}, #{method}, #{content}, #{operatorId})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(CustomerFollowup f);

    @Select("SELECT id, tenant_id AS tenantId, customer_id AS customerId, method, content, operator_id AS operatorId, created_at AS createdAt " +
            "FROM aa_customer_followup WHERE tenant_id = #{tenantId} AND customer_id = #{customerId} ORDER BY created_at DESC LIMIT #{limit} OFFSET #{offset}")
    List<CustomerFollowup> listByCustomer(@Param("tenantId") long tenantId, @Param("customerId") long customerId,
                                          @Param("offset") int offset, @Param("limit") int limit);

    @Select("SELECT COUNT(*) FROM aa_customer_followup WHERE tenant_id = #{tenantId} AND customer_id = #{customerId}")
    long countByCustomer(@Param("tenantId") long tenantId, @Param("customerId") long customerId);
}
