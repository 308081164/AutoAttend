package org.example.atuo_attend_backend.nexus.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.example.atuo_attend_backend.nexus.domain.NexusMemoryMetricPoint;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface NexusMemoryMetricMapper {

    @Insert("""
            INSERT INTO aa_nexus_instance_memory_metric
              (tenant_id, account_id, instance_id, ts, value)
            VALUES
              (#{tenantId}, #{accountId}, #{instanceId}, #{ts}, #{value})
            ON DUPLICATE KEY UPDATE
              value = VALUES(value),
              updated_at = CURRENT_TIMESTAMP
            """)
    int upsertPoint(NexusMemoryMetricPoint p);

    @Select("""
            SELECT
              ts, value
            FROM aa_nexus_instance_memory_metric
            WHERE tenant_id = #{tenantId}
              AND account_id = #{accountId}
              AND instance_id = #{instanceId}
            ORDER BY ts ASC
            LIMIT #{limit}
            """)
    List<MetricRow> listMemoryPoints(@Param("tenantId") long tenantId,
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

