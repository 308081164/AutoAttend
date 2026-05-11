package org.example.atuo_attend_backend.platform.mapper;

import org.apache.ibatis.annotations.*;

import java.time.OffsetDateTime;
import java.util.List;

@Mapper
public interface PlatformComponentEventMapper {

    @Insert("""
            INSERT INTO aa_platform_component_event(
              tenant_id,
              admin_user_id,
              admin_phone,
              component_key,
              core_api_key,
              event_type
            ) VALUES (
              #{tenantId},
              #{adminUserId},
              #{adminPhone},
              #{componentKey},
              #{coreApiKey},
              #{eventType}
            )
            """)
    int insert(
            @Param("tenantId") long tenantId,
            @Param("adminUserId") Long adminUserId,
            @Param("adminPhone") String adminPhone,
            @Param("componentKey") String componentKey,
            @Param("coreApiKey") String coreApiKey,
            @Param("eventType") String eventType
    );

    @Select("""
            SELECT
              component_key AS componentKey,
              SUM(CASE WHEN event_type = 'click' THEN 1 ELSE 0 END) AS clickCount,
              SUM(CASE WHEN event_type = 'usage' THEN 1 ELSE 0 END) AS usageCount
            FROM aa_platform_component_event
            WHERE created_at >= #{since}
            GROUP BY component_key
            ORDER BY usageCount DESC, clickCount DESC
            """)
    List<ComponentAggRow> listComponentAgg(@Param("since") OffsetDateTime since);

    @Select("""
            SELECT
              component_key AS componentKey,
              core_api_key AS coreApiKey,
              SUM(CASE WHEN event_type = 'click' THEN 1 ELSE 0 END) AS clickCount,
              SUM(CASE WHEN event_type = 'usage' THEN 1 ELSE 0 END) AS usageCount
            FROM aa_platform_component_event
            WHERE created_at >= #{since}
              AND core_api_key IS NOT NULL AND TRIM(core_api_key) <> ''
            GROUP BY component_key, core_api_key
            ORDER BY usageCount DESC, clickCount DESC
            """)
    List<ComponentCoreApiAggRow> listCoreApiAgg(@Param("since") OffsetDateTime since);

    @Select("""
            SELECT
              core_api_key AS coreApiKey,
              component_key AS componentKey,
              SUM(CASE WHEN event_type = 'click' THEN 1 ELSE 0 END) AS clickCount,
              SUM(CASE WHEN event_type = 'usage' THEN 1 ELSE 0 END) AS usageCount,
              COUNT(DISTINCT tenant_id) AS tenantCount,
              COUNT(DISTINCT admin_user_id) AS userCount
            FROM aa_platform_component_event
            WHERE created_at >= #{since}
              AND core_api_key IS NOT NULL AND TRIM(core_api_key) <> ''
            GROUP BY core_api_key, component_key
            ORDER BY clickCount DESC
            LIMIT #{limit}
            """)
    List<HeatRankRow> listHeatRank(@Param("since") OffsetDateTime since, @Param("limit") int limit);

    class ComponentAggRow {
        private String componentKey;
        private long clickCount;
        private long usageCount;

        public String getComponentKey() {
            return componentKey;
        }

        public void setComponentKey(String componentKey) {
            this.componentKey = componentKey;
        }

        public long getClickCount() {
            return clickCount;
        }

        public void setClickCount(long clickCount) {
            this.clickCount = clickCount;
        }

        public long getUsageCount() {
            return usageCount;
        }

        public void setUsageCount(long usageCount) {
            this.usageCount = usageCount;
        }
    }

    class ComponentCoreApiAggRow {
        private String componentKey;
        private String coreApiKey;
        private long clickCount;
        private long usageCount;

        public String getComponentKey() { return componentKey; }
        public void setComponentKey(String componentKey) { this.componentKey = componentKey; }
        public String getCoreApiKey() { return coreApiKey; }
        public void setCoreApiKey(String coreApiKey) { this.coreApiKey = coreApiKey; }
        public long getClickCount() { return clickCount; }
        public void setClickCount(long clickCount) { this.clickCount = clickCount; }
        public long getUsageCount() { return usageCount; }
        public void setUsageCount(long usageCount) { this.usageCount = usageCount; }
    }

    class HeatRankRow {
        private String coreApiKey;
        private String componentKey;
        private long clickCount;
        private long usageCount;
        private long tenantCount;
        private long userCount;

        public String getCoreApiKey() { return coreApiKey; }
        public void setCoreApiKey(String coreApiKey) { this.coreApiKey = coreApiKey; }
        public String getComponentKey() { return componentKey; }
        public void setComponentKey(String componentKey) { this.componentKey = componentKey; }
        public long getClickCount() { return clickCount; }
        public void setClickCount(long clickCount) { this.clickCount = clickCount; }
        public long getUsageCount() { return usageCount; }
        public void setUsageCount(long usageCount) { this.usageCount = usageCount; }
        public long getTenantCount() { return tenantCount; }
        public void setTenantCount(long tenantCount) { this.tenantCount = tenantCount; }
        public long getUserCount() { return userCount; }
        public void setUserCount(long userCount) { this.userCount = userCount; }
    }
}

