package org.example.atuo_attend_backend.tenant.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.example.atuo_attend_backend.tenant.domain.SubscriptionOrder;

@Mapper
public interface SubscriptionOrderMapper {

    @Insert("""
            INSERT INTO aa_subscription_order (tenant_id, plan_code, amount_cents, currency, provider, status)
            VALUES (#{tenantId}, #{planCode}, #{amountCents}, #{currency}, #{provider}, #{status})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertEntity(SubscriptionOrder order);

    @Select("""
            SELECT COALESCE(SUM(amount_cents), 0) FROM aa_subscription_order
            WHERE status = 'completed'
              AND created_at >= DATE_SUB(NOW(), INTERVAL 30 DAY)
            """)
    long sumAmountCentsLast30Days();
}
