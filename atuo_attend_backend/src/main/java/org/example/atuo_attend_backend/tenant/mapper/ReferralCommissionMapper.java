package org.example.atuo_attend_backend.tenant.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

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
}
