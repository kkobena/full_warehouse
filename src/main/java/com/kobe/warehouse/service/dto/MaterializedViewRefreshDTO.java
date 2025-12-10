package com.kobe.warehouse.service.dto;

import java.time.LocalDateTime;

public record MaterializedViewRefreshDTO(
    String viewName,
    String tier,
    String status,
    LocalDateTime refreshStartedAt,
    LocalDateTime refreshCompletedAt,
    Long durationMillis,
    String errorMessage
) {
    public static MaterializedViewRefreshDTO success(String viewName, String tier, LocalDateTime startTime, LocalDateTime endTime) {
        long duration = java.time.Duration.between(startTime, endTime).toMillis();
        return new MaterializedViewRefreshDTO(viewName, tier, "SUCCESS", startTime, endTime, duration, null);
    }

    public static MaterializedViewRefreshDTO failed(String viewName, String tier, LocalDateTime startTime, String errorMessage) {
        return new MaterializedViewRefreshDTO(viewName, tier, "FAILED", startTime, LocalDateTime.now(), null, errorMessage);
    }

    public static MaterializedViewRefreshDTO running(String viewName, String tier, LocalDateTime startTime) {
        return new MaterializedViewRefreshDTO(viewName, tier, "RUNNING", startTime, null, null, null);
    }
}
