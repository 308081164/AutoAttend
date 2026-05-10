package org.example.atuo_attend_backend.webhook.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface WebhookDeliveryMapper {

    @Select("SELECT COUNT(*) FROM aa_webhook_delivery WHERE delivery_id = #{deliveryId}")
    long countByDeliveryId(@Param("deliveryId") String deliveryId);

    @Insert("""
            INSERT INTO aa_webhook_delivery(delivery_id, event_type)
            VALUES(#{deliveryId}, #{eventType})
            """)
    int insert(@Param("deliveryId") String deliveryId, @Param("eventType") String eventType);

    /** 幂等：已存在相同 delivery_id 时忽略，返回 0 */
    @Insert("""
            INSERT IGNORE INTO aa_webhook_delivery(delivery_id, event_type)
            VALUES(#{deliveryId}, #{eventType})
            """)
    int insertIgnore(@Param("deliveryId") String deliveryId, @Param("eventType") String eventType);
}

