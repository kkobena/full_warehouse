package com.kobe.warehouse.service.dto;

import java.time.LocalDateTime;
import java.util.List;

public record TierRefreshResultDTO(
    String tier,
    int totalViews,
    int successCount,
    int failedCount,
    LocalDateTime startedAt,
    LocalDateTime completedAt,
    Long totalDurationMillis,
    List<MaterializedViewRefreshDTO> results
) {
    public static TierRefreshResultDTO of(String tier, List<MaterializedViewRefreshDTO> results, LocalDateTime startTime, LocalDateTime endTime) {
        long successCount = results.stream().filter(r -> "SUCCESS".equals(r.status())).count();
        long failedCount = results.stream().filter(r -> "FAILED".equals(r.status())).count();
        long duration = java.time.Duration.between(startTime, endTime).toMillis();

        return new TierRefreshResultDTO(
            tier,
            results.size(),
            (int) successCount,
            (int) failedCount,
            startTime,
            endTime,
            duration,
            results
        );
    }
}
