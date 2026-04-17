package org.example.atuo_attend_backend.nexus.dto;

/**
 * 监控图表单点（与库内列表格式一致：ts + value，便于前端共用渲染）。
 */
public class NexusMetricChartPoint {

    private String ts;
    private Double value;

    public NexusMetricChartPoint() {
    }

    public NexusMetricChartPoint(String ts, Double value) {
        this.ts = ts;
        this.value = value;
    }

    public String getTs() {
        return ts;
    }

    public void setTs(String ts) {
        this.ts = ts;
    }

    public Double getValue() {
        return value;
    }

    public void setValue(Double value) {
        this.value = value;
    }
}
