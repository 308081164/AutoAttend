package org.example.atuo_attend_backend.webhook.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface WebhookDeliveryMapper {

    @Insert("""
            INSERT INTO aa_webhook_delivery(delivery_id, event_type)
            VALUES(#{deliveryId}, #{eventType})
            """)
    int insert(@Param("deliveryId") String deliveryId, @Param("eventType") String eventType);
}

