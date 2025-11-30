package com.kobe.warehouse.service.report;

import com.kobe.warehouse.service.dto.enumeration.TypeVenteDTO;
import com.kobe.warehouse.service.dto.report.DailySalesSummaryDTO;
import java.time.LocalDate;
import java.util.List;

/**
 * Service interface for sales summary reports using mv_daily_sales_summary
 */
public interface SalesSummaryReportService {
    /**
     * Get daily sales summary for a date range
     *
     * @param startDate start date (inclusive)
     * @param endDate end date (inclusive)
     * @return list of daily summaries
     */
    List<DailySalesSummaryDTO> getDailySalesSummary(LocalDate startDate, LocalDate endDate);

    /**
     * Get daily sales summary for a specific date
     *
     * @param date the date
     * @return list of summaries grouped by sale type
     */
    List<DailySalesSummaryDTO> getDailySalesSummaryByDate(LocalDate date);

    /**
     * Get daily sales summary filtered by sale type
     *
     * @param startDate start date (inclusive)
     * @param endDate end date (inclusive)
     * @param typeVente sale type (e.g., "VO", "VNO")
     * @return list of daily summaries for that type
     */
    List<DailySalesSummaryDTO> getDailySalesSummaryByType(LocalDate startDate, LocalDate endDate, TypeVenteDTO typeVente);
}
