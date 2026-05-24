package com.kobe.warehouse.service.dto.report;

import java.util.List;

/**
 * DTO for Dashboard CA evolution chart data
 */
public record DashboardCAEvolutionDTO(
    List<String> labels,
    List<Long> caValues,
    List<Long> caPreviousValues,
    List<Integer> transactionCounts,
    String period  // "daily", "weekly", "monthly"
) {}
