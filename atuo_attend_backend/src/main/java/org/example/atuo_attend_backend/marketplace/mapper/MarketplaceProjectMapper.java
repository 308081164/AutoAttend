package org.example.atuo_attend_backend.marketplace.mapper;

import org.apache.ibatis.annotations.*;
import org.example.atuo_attend_backend.marketplace.domain.MarketplaceProject;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface MarketplaceProjectMapper {

    @Insert("INSERT INTO aa_marketplace_project (tenant_id, publisher_user_id, title, description, tech_stack_json, "
            + "budget_range, duration, location, contact_type, contact_value_enc, status, reject_reason, view_count, "
            + "publish_time, expire_time) VALUES (#{tenantId}, #{publisherUserId}, #{title}, #{description}, #{techStackJson}, "
            + "#{budgetRange}, #{duration}, #{location}, #{contactType}, #{contactValueEnc}, #{status}, #{rejectReason}, "
            + "#{viewCount}, #{publishTime}, #{expireTime})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(MarketplaceProject row);

    @Select("SELECT id, tenant_id AS tenantId, publisher_user_id AS publisherUserId, title, description, "
            + "tech_stack_json AS techStackJson, budget_range AS budgetRange, duration, location, contact_type AS contactType, "
            + "contact_value_enc AS contactValueEnc, status, reject_reason AS rejectReason, view_count AS viewCount, "
            + "publish_time AS publishTime, expire_time AS expireTime, created_at AS createdAt, updated_at AS updatedAt "
            + "FROM aa_marketplace_project WHERE id = #{id}")
    MarketplaceProject findById(@Param("id") long id);

    @Update("UPDATE aa_marketplace_project SET title = #{title}, description = #{description}, tech_stack_json = #{techStackJson}, "
            + "budget_range = #{budgetRange}, duration = #{duration}, location = #{location}, contact_type = #{contactType}, "
            + "contact_value_enc = #{contactValueEnc}, expire_time = #{expireTime}, updated_at = CURRENT_TIMESTAMP "
            + "WHERE id = #{id} AND tenant_id = #{tenantId}")
    int updateEditable(MarketplaceProject row);

    @Update("UPDATE aa_marketplace_project SET status = #{status}, reject_reason = #{rejectReason}, updated_at = CURRENT_TIMESTAMP "
            + "WHERE id = #{id} AND tenant_id = #{tenantId}")
    int updateStatus(@Param("id") long id, @Param("tenantId") long tenantId,
                     @Param("status") String status, @Param("rejectReason") String rejectReason);

    @Update("UPDATE aa_marketplace_project SET status = #{status}, publish_time = #{publishTime}, expire_time = #{expireTime}, "
            + "reject_reason = NULL, updated_at = CURRENT_TIMESTAMP WHERE id = #{id} AND tenant_id = #{tenantId}")
    int approve(@Param("id") long id, @Param("tenantId") long tenantId, @Param("status") String status,
                @Param("publishTime") LocalDateTime publishTime, @Param("expireTime") LocalDateTime expireTime);

    @Update("UPDATE aa_marketplace_project SET view_count = view_count + 1 WHERE id = #{id} AND tenant_id = #{tenantId}")
    int incrementViewCount(@Param("id") long id, @Param("tenantId") long tenantId);

    @Update("UPDATE aa_marketplace_project SET status = 'closed', updated_at = CURRENT_TIMESTAMP "
            + "WHERE status = 'open' AND expire_time IS NOT NULL AND expire_time < NOW()")
    int closeExpiredOpen();

    @Select("SELECT COUNT(*) FROM aa_marketplace_project WHERE tenant_id = #{tenantId} "
            + "AND (#{statusFilter} IS NULL OR #{statusFilter} = '' "
            + "  AND status IN ('open','closed','completed') "
            + " OR (#{statusFilter} IS NOT NULL AND #{statusFilter} <> '' AND status = #{statusFilter})) "
            + "AND (#{q} IS NULL OR #{q} = '' OR title LIKE CONCAT('%', #{q}, '%') OR description LIKE CONCAT('%', #{q}, '%')) "
            + "AND (#{tech} IS NULL OR #{tech} = '' OR tech_stack_json LIKE CONCAT('%', #{tech}, '%')) "
            + "AND (#{location} IS NULL OR #{location} = '' OR location LIKE CONCAT('%', #{location}, '%'))")
    long countList(@Param("tenantId") long tenantId, @Param("q") String q, @Param("tech") String tech,
                   @Param("location") String location, @Param("statusFilter") String statusFilter);

    @Select("SELECT id, tenant_id AS tenantId, publisher_user_id AS publisherUserId, title, description, "
            + "tech_stack_json AS techStackJson, budget_range AS budgetRange, duration, location, contact_type AS contactType, "
            + "contact_value_enc AS contactValueEnc, status, reject_reason AS rejectReason, view_count AS viewCount, "
            + "publish_time AS publishTime, expire_time AS expireTime, created_at AS createdAt, updated_at AS updatedAt "
            + "FROM aa_marketplace_project WHERE tenant_id = #{tenantId} "
            + "AND (#{statusFilter} IS NULL OR #{statusFilter} = '' "
            + "  AND status IN ('open','closed','completed') "
            + " OR (#{statusFilter} IS NOT NULL AND #{statusFilter} <> '' AND status = #{statusFilter})) "
            + "AND (#{q} IS NULL OR #{q} = '' OR title LIKE CONCAT('%', #{q}, '%') OR description LIKE CONCAT('%', #{q}, '%')) "
            + "AND (#{tech} IS NULL OR #{tech} = '' OR tech_stack_json LIKE CONCAT('%', #{tech}, '%')) "
            + "AND (#{location} IS NULL OR #{location} = '' OR location LIKE CONCAT('%', #{location}, '%')) "
            + "ORDER BY COALESCE(publish_time, created_at) DESC LIMIT #{limit} OFFSET #{offset}")
    List<MarketplaceProject> listPageNewest(@Param("tenantId") long tenantId, @Param("q") String q, @Param("tech") String tech,
                                            @Param("location") String location, @Param("statusFilter") String statusFilter,
                                            @Param("offset") int offset, @Param("limit") int limit);

    @Select("SELECT id, tenant_id AS tenantId, publisher_user_id AS publisherUserId, title, description, "
            + "tech_stack_json AS techStackJson, budget_range AS budgetRange, duration, location, contact_type AS contactType, "
            + "contact_value_enc AS contactValueEnc, status, reject_reason AS rejectReason, view_count AS viewCount, "
            + "publish_time AS publishTime, expire_time AS expireTime, created_at AS createdAt, updated_at AS updatedAt "
            + "FROM aa_marketplace_project WHERE tenant_id = #{tenantId} "
            + "AND (#{statusFilter} IS NULL OR #{statusFilter} = '' "
            + "  AND status IN ('open','closed','completed') "
            + " OR (#{statusFilter} IS NOT NULL AND #{statusFilter} <> '' AND status = #{statusFilter})) "
            + "AND (#{q} IS NULL OR #{q} = '' OR title LIKE CONCAT('%', #{q}, '%') OR description LIKE CONCAT('%', #{q}, '%')) "
            + "AND (#{tech} IS NULL OR #{tech} = '' OR tech_stack_json LIKE CONCAT('%', #{tech}, '%')) "
            + "AND (#{location} IS NULL OR #{location} = '' OR location LIKE CONCAT('%', #{location}, '%')) "
            + "ORDER BY view_count DESC, COALESCE(publish_time, created_at) DESC LIMIT #{limit} OFFSET #{offset}")
    List<MarketplaceProject> listPageHot(@Param("tenantId") long tenantId, @Param("q") String q, @Param("tech") String tech,
                                         @Param("location") String location, @Param("statusFilter") String statusFilter,
                                         @Param("offset") int offset, @Param("limit") int limit);

    @Select("SELECT id, tenant_id AS tenantId, publisher_user_id AS publisherUserId, title, description, "
            + "tech_stack_json AS techStackJson, budget_range AS budgetRange, duration, location, contact_type AS contactType, "
            + "contact_value_enc AS contactValueEnc, status, reject_reason AS rejectReason, view_count AS viewCount, "
            + "publish_time AS publishTime, expire_time AS expireTime, created_at AS createdAt, updated_at AS updatedAt "
            + "FROM aa_marketplace_project WHERE tenant_id = #{tenantId} "
            + "AND (#{statusFilter} IS NULL OR #{statusFilter} = '' "
            + "  AND status IN ('open','closed','completed') "
            + " OR (#{statusFilter} IS NOT NULL AND #{statusFilter} <> '' AND status = #{statusFilter})) "
            + "AND (#{q} IS NULL OR #{q} = '' OR title LIKE CONCAT('%', #{q}, '%') OR description LIKE CONCAT('%', #{q}, '%')) "
            + "AND (#{tech} IS NULL OR #{tech} = '' OR tech_stack_json LIKE CONCAT('%', #{tech}, '%')) "
            + "AND (#{location} IS NULL OR #{location} = '' OR location LIKE CONCAT('%', #{location}, '%')) "
            + "ORDER BY budget_range DESC, COALESCE(publish_time, created_at) DESC LIMIT #{limit} OFFSET #{offset}")
    List<MarketplaceProject> listPageBudget(@Param("tenantId") long tenantId, @Param("q") String q, @Param("tech") String tech,
                                           @Param("location") String location, @Param("statusFilter") String statusFilter,
                                           @Param("offset") int offset, @Param("limit") int limit);

    @Select("SELECT id, tenant_id AS tenantId, publisher_user_id AS publisherUserId, title, description, "
            + "tech_stack_json AS techStackJson, budget_range AS budgetRange, duration, location, contact_type AS contactType, "
            + "contact_value_enc AS contactValueEnc, status, reject_reason AS rejectReason, view_count AS viewCount, "
            + "publish_time AS publishTime, expire_time AS expireTime, created_at AS createdAt, updated_at AS updatedAt "
            + "FROM aa_marketplace_project WHERE tenant_id = #{tenantId} AND publisher_user_id = #{publisherUserId} "
            + "ORDER BY id DESC LIMIT #{limit} OFFSET #{offset}")
    List<MarketplaceProject> listMine(@Param("tenantId") long tenantId, @Param("publisherUserId") long publisherUserId,
                                      @Param("offset") int offset, @Param("limit") int limit);

    @Select("SELECT COUNT(*) FROM aa_marketplace_project WHERE tenant_id = #{tenantId} AND publisher_user_id = #{publisherUserId}")
    long countMine(@Param("tenantId") long tenantId, @Param("publisherUserId") long publisherUserId);

    @Select("SELECT id, tenant_id AS tenantId, publisher_user_id AS publisherUserId, title, description, "
            + "tech_stack_json AS techStackJson, budget_range AS budgetRange, duration, location, contact_type AS contactType, "
            + "contact_value_enc AS contactValueEnc, status, reject_reason AS rejectReason, view_count AS viewCount, "
            + "publish_time AS publishTime, expire_time AS expireTime, created_at AS createdAt, updated_at AS updatedAt "
            + "FROM aa_marketplace_project WHERE tenant_id = #{tenantId} AND status = 'pending_review' ORDER BY id ASC")
    List<MarketplaceProject> listPending(@Param("tenantId") long tenantId);
}
