package com.kobe.warehouse.service.report;

import com.kobe.warehouse.service.dto.report.CashMovementDTO;
import com.kobe.warehouse.service.dto.report.DailyCashRegisterReportDTO;

import java.time.LocalDate;
import java.util.List;

public interface CashRegisterReportService {
    /**
     * Get daily cash register report for a specific date
     *
     * @param date The date for the report
     * @return Daily cash register report
     */
    List<DailyCashRegisterReportDTO> getDailyReport(LocalDate date);

    /**
     * Get cash movements history with optional filters
     *
     * @param startDate      Start date
     * @param endDate        End date
     * @param userId         Optional user ID filter
     * @param cashRegisterId Optional cash register ID filter
     * @return List of cash movements
     */
    List<CashMovementDTO> getCashMovements(LocalDate startDate, LocalDate endDate, Long userId, Long cashRegisterId);

    /**
     * Get weekly or monthly summary of cash registers
     *
     * @param startDate Start date
     * @param endDate   End date
     * @return List of daily cash register summaries
     */
    List<DailyCashRegisterReportDTO> getCashRegisterSummary(LocalDate startDate, LocalDate endDate);
}
