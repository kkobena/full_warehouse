package com.kobe.warehouse.service.dto.mobile;

import java.util.List;

/**
 * DTO for custom report metric data (Phase 4)
 */
public record CustomReportMetricDTO(
    String metricCode,
    String metricName,
    String value,
    Double trend,
    List<ChartDataPointDTO> chartData,
    String details
) {

    /**
     * Chart data point for metrics
     */
    public record ChartDataPointDTO(
        String label,
        Double value,
        String color
    ) {}
}
