package org.example.atuo_attend_backend.nexus.mapper;

import org.apache.ibatis.annotations.*;
import org.example.atuo_attend_backend.nexus.domain.NexusCpuMetricPoint;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface NexusCpuMetricMapper {

    @Insert("""
            INSERT INTO aa_nexus_instance_cpu_metric
              (tenant_id, account_id, instance_id, ts, value)
            VALUES
              (#{tenantId}, #{accountId}, #{instanceId}, #{ts}, #{value})
            ON DUPLICATE KEY UPDATE
              value = VALUES(value),
              updated_at = CURRENT_TIMESTAMP
            """)
    int upsertPoint(NexusCpuMetricPoint p);

    @Select("""
            SELECT
              ts, value
            FROM aa_nexus_instance_cpu_metric
            WHERE tenant_id = #{tenantId}
              AND account_id = #{accountId}
              AND instance_id = #{instanceId}
            ORDER BY ts ASC
            LIMIT #{limit}
            """)
    List<MetricRow> listCpuPoints(@Param("tenantId") long tenantId,
                                   @Param("accountId") long accountId,
                                   @Param("instanceId") String instanceId,
                                   @Param("limit") int limit);

    class MetricRow {
        private LocalDateTime ts;
        private Double value;

        public LocalDateTime getTs() {
            return ts;
        }

        public void setTs(LocalDateTime ts) {
            this.ts = ts;
        }

        public Double getValue() {
            return value;
        }

        public void setValue(Double value) {
            this.value = value;
        }
    }
}

