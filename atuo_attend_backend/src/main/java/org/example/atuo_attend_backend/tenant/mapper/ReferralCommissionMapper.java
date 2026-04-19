package org.example.atuo_attend_backend.tenant.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.example.atuo_attend_backend.tenant.dto.ReferralCommissionRow;

import java.util.List;

@Mapper
public interface ReferralCommissionMapper {

    @Insert("""
            INSERT INTO aa_referral_commission (referrer_tenant_id, source_tenant_id, order_amount_cents, commission_cents, subscription_order_id)
            VALUES (#{referrerTenantId}, #{sourceTenantId}, #{orderAmountCents}, #{commissionCents}, #{subscriptionOrderId})
            """)
    int insert(@Param("referrerTenantId") long referrerTenantId,
               @Param("sourceTenantId") long sourceTenantId,
               @Param("orderAmountCents") int orderAmountCents,
               @Param("commissionCents") int commissionCents,
               @Param("subscriptionOrderId") Long subscriptionOrderId);

    @Select("""
            SELECT c.id, c.source_tenant_id AS sourceTenantId, t.name AS sourceTenantName,
                   c.order_amount_cents AS orderAmountCents, c.commission_cents AS commissionCents,
                   c.subscription_order_id AS subscriptionOrderId, c.created_at AS createdAt
            FROM aa_referral_commission c
            LEFT JOIN aa_tenant t ON t.id = c.source_tenant_id
            WHERE c.referrer_tenant_id = #{referrerId}
            ORDER BY c.id DESC
            LIMIT #{limit}
            """)
    List<ReferralCommissionRow> listByReferrer(@Param("referrerId") long referrerId, @Param("limit") int limit);
}
