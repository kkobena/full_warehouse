package com.kobe.warehouse.service.report;

import com.kobe.warehouse.service.dto.report.ComparativeByTypeDTO;
import com.kobe.warehouse.service.dto.report.ComparativeCADTO;
import com.kobe.warehouse.service.dto.report.ComparativeSummaryDTO;

import java.time.LocalDate;
import java.util.List;

/**
 * Service interface for Comparative Reports (Tableaux Comparatifs)
 */
public interface ComparativeReportService {

    /**
     * Get monthly CA comparison (current year vs previous year)
     */
    List<ComparativeCADTO> getMonthlyComparison(Integer year);

    /**
     * Get quarterly CA comparison
     */
    List<ComparativeCADTO> getQuarterlyComparison(Integer year);

    /**
     * Get yearly CA comparison over multiple years
     */
    List<ComparativeCADTO> getYearlyComparison(LocalDate startDate, LocalDate endDate);

    /**
     * Get CA comparison by sales type
     */
    List<ComparativeByTypeDTO> getComparisonBySalesType(Integer currentYear, Integer previousYear);

    /**
     * Get overall comparative summary
     */
    ComparativeSummaryDTO getComparativeSummary();


}
