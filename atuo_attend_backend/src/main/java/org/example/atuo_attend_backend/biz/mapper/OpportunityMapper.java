package org.example.atuo_attend_backend.biz.mapper;

import org.apache.ibatis.annotations.*;
import org.example.atuo_attend_backend.biz.domain.Opportunity;

import java.util.List;

@Mapper
public interface OpportunityMapper {

    @Insert("""
            INSERT INTO aa_opportunity (tenant_id, customer_id, quote_project_id, name, expected_amount, stage, win_probability, expected_close_date, assigned_to)
            VALUES (#{tenantId}, #{customerId}, #{quoteProjectId}, #{name}, #{expectedAmount}, #{stage}, #{winProbability}, #{expectedCloseDate}, #{assignedTo})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Opportunity o);

    @Update("""
            UPDATE aa_opportunity SET name=#{name}, expected_amount=#{expectedAmount}, stage=#{stage},
                win_probability=#{winProbability}, expected_close_date=#{expectedCloseDate},
                assigned_to=#{assignedTo}, updated_at=CURRENT_TIMESTAMP
            WHERE tenant_id=#{tenantId} AND id=#{id}
            """)
    int update(Opportunity o);

    @Select("SELECT id, tenant_id AS tenantId, customer_id AS customerId, quote_project_id AS quoteProjectId, name, " +
            "expected_amount AS expectedAmount, stage, win_probability AS winProbability, " +
            "expected_close_date AS expectedCloseDate, assigned_to AS assignedTo, " +
            "created_at AS createdAt, updated_at AS updatedAt " +
            "FROM aa_opportunity WHERE tenant_id = #{tenantId} AND id = #{id}")
    Opportunity findById(@Param("tenantId") long tenantId, @Param("id") long id);

    @Select("SELECT id, tenant_id AS tenantId, customer_id AS customerId, quote_project_id AS quoteProjectId, name, " +
            "expected_amount AS expectedAmount, stage, win_probability AS winProbability, " +
            "expected_close_date AS expectedCloseDate, assigned_to AS assignedTo, " +
            "created_at AS createdAt, updated_at AS updatedAt " +
            "FROM aa_opportunity WHERE tenant_id = #{tenantId} ORDER BY updated_at DESC LIMIT #{limit} OFFSET #{offset}")
    List<Opportunity> listPaged(@Param("tenantId") long tenantId, @Param("offset") int offset, @Param("limit") int limit);

    @Select("SELECT COUNT(*) FROM aa_opportunity WHERE tenant_id = #{tenantId}")
    long countAll(@Param("tenantId") long tenantId);

    @Select("SELECT id, tenant_id AS tenantId, customer_id AS customerId, quote_project_id AS quoteProjectId, name, " +
            "expected_amount AS expectedAmount, stage, win_probability AS winProbability, " +
            "expected_close_date AS expectedCloseDate, assigned_to AS assignedTo, " +
            "created_at AS createdAt, updated_at AS updatedAt " +
            "FROM aa_opportunity WHERE tenant_id = #{tenantId} AND stage = #{stage} ORDER BY updated_at DESC")
    List<Opportunity> listByStage(@Param("tenantId") long tenantId, @Param("stage") String stage);

    @Select("SELECT stage, COUNT(*) AS cnt, SUM(expected_amount) AS totalAmount FROM aa_opportunity " +
            "WHERE tenant_id = #{tenantId} GROUP BY stage")
    List<java.util.Map<String, Object>> groupByStage(@Param("tenantId") long tenantId);

    @Delete("DELETE FROM aa_opportunity WHERE tenant_id = #{tenantId} AND id = #{id}")
    int deleteById(@Param("tenantId") long tenantId, @Param("id") long id);
}
