package org.example.atuo_attend_backend.tenant.mapper;

import org.apache.ibatis.annotations.*;
import org.example.atuo_attend_backend.tenant.domain.Tenant;

import java.util.List;

@Mapper
public interface TenantMapper {

    @Select("SELECT id, name, slug, referrer_tenant_id AS referrerTenantId, "
            + "invite_code_redeemed AS inviteCodeRedeemed, member_points AS memberPoints, team_first_month_used AS teamFirstMonthUsed, "
            + "plan_code AS planCode, billing_baseline_plan_code AS billingBaselinePlanCode, "
            + "subscription_ends_at AS subscriptionEndsAt, status, created_at AS createdAt FROM aa_tenant WHERE id = #{id}")
    Tenant findById(@Param("id") long id);

    @Select("SELECT id, name, slug, referrer_tenant_id AS referrerTenantId, "
            + "invite_code_redeemed AS inviteCodeRedeemed, member_points AS memberPoints, team_first_month_used AS teamFirstMonthUsed, "
            + "plan_code AS planCode, billing_baseline_plan_code AS billingBaselinePlanCode, "
            + "subscription_ends_at AS subscriptionEndsAt, status, created_at AS createdAt FROM aa_tenant WHERE slug = #{slug}")
    Tenant findBySlug(@Param("slug") String slug);

    @Select("SELECT id, name, slug, referrer_tenant_id AS referrerTenantId, "
            + "invite_code_redeemed AS inviteCodeRedeemed, member_points AS memberPoints, team_first_month_used AS teamFirstMonthUsed, "
            + "plan_code AS planCode, billing_baseline_plan_code AS billingBaselinePlanCode, "
            + "subscription_ends_at AS subscriptionEndsAt, status, created_at AS createdAt FROM aa_tenant ORDER BY id")
    List<Tenant> listAll();

    @Insert("INSERT INTO aa_tenant (name, slug, plan_code, billing_baseline_plan_code, status) "
            + "VALUES (#{name}, #{slug}, #{planCode}, #{billingBaselinePlanCode}, #{status})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Tenant tenant);

    @Update("UPDATE aa_tenant SET plan_code = #{planCode} WHERE id = #{id}")
    int updatePlanCode(@Param("id") long id, @Param("planCode") String planCode);

    @Update("UPDATE aa_tenant SET status = #{status} WHERE id = #{id}")
    int updateStatus(@Param("id") long id, @Param("status") String status);

    @Update("UPDATE aa_tenant SET plan_code = #{planCode}, subscription_ends_at = #{subscriptionEndsAt} WHERE id = #{id}")
    int updatePlanAndSubscriptionEndsAt(@Param("id") long id,
                                        @Param("planCode") String planCode,
                                        @Param("subscriptionEndsAt") java.time.LocalDateTime subscriptionEndsAt);

    @Update("UPDATE aa_tenant SET billing_baseline_plan_code = #{baselinePlanCode} WHERE id = #{id}")
    int updateBillingBaseline(@Param("id") long id, @Param("baselinePlanCode") String baselinePlanCode);

    @Update("UPDATE aa_tenant SET plan_code = #{baselinePlanCode}, subscription_ends_at = NULL "
            + "WHERE id = #{id} AND subscription_ends_at IS NOT NULL AND subscription_ends_at < NOW()")
    int revertExpiredSubscription(@Param("id") long id, @Param("baselinePlanCode") String baselinePlanCode);

    /** 批量回退已过期的订阅窗口（不依赖进程内读库） */
    @Update("UPDATE aa_tenant SET plan_code = billing_baseline_plan_code, subscription_ends_at = NULL "
            + "WHERE subscription_ends_at IS NOT NULL AND subscription_ends_at < NOW()")
    int revertAllExpiredSubscriptions();

    @Update("UPDATE aa_tenant SET plan_code = #{planCode}, subscription_ends_at = #{subscriptionEndsAt}, "
            + "billing_baseline_plan_code = #{billingBaselinePlanCode} WHERE id = #{id}")
    int updatePlanSubscriptionAndBaseline(@Param("id") long id,
                                          @Param("planCode") String planCode,
                                          @Param("subscriptionEndsAt") java.time.LocalDateTime subscriptionEndsAt,
                                          @Param("billingBaselinePlanCode") String billingBaselinePlanCode);

    @Select("SELECT COUNT(*) FROM aa_tenant WHERE slug = #{slug}")
    int countBySlug(@Param("slug") String slug);

    @Select("""
            SELECT COUNT(*) FROM aa_tenant
            WHERE subscription_ends_at IS NOT NULL AND subscription_ends_at > NOW()
            """)
    long countActivePaidSubscriptions();

    @Select("SELECT COUNT(*) FROM aa_tenant WHERE status = 'suspended'")
    long countSuspendedTenants();

    /** 仅查名称，避免依赖 aa_tenant 增量列（公开页在迁移未执行时仍可用） */
    @Select("SELECT name FROM aa_tenant WHERE id = #{id}")
    String findNameById(@Param("id") long id);

    @Update("UPDATE aa_tenant SET referrer_tenant_id = #{referrerTenantId} WHERE id = #{id} AND referrer_tenant_id IS NULL")
    int updateReferrerIfNull(@Param("id") long id, @Param("referrerTenantId") long referrerTenantId);

    @Update("UPDATE aa_tenant SET invite_code_redeemed = 1 WHERE id = #{id}")
    int markInviteRedeemed(@Param("id") long id);

    @Update("UPDATE aa_tenant SET member_points = member_points + #{delta} WHERE id = #{id} AND member_points + #{delta} >= 0")
    int addMemberPoints(@Param("id") long id, @Param("delta") int delta);

    @Update("UPDATE aa_tenant SET team_first_month_used = 1 WHERE id = #{id}")
    int markTeamFirstMonthUsed(@Param("id") long id);
}
