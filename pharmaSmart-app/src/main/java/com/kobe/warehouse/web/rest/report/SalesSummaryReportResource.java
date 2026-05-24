package com.kobe.warehouse.web.rest.report;

import com.kobe.warehouse.security.AuthoritiesConstants;
import com.kobe.warehouse.service.dto.enumeration.TypeVenteDTO;
import com.kobe.warehouse.service.dto.report.DailySalesSummaryDTO;
import com.kobe.warehouse.service.report.SalesSummaryReportService;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for daily sales summary reports
 */
@RestController
@RequestMapping("/api/sales-summary")
public class SalesSummaryReportResource {

    private final SalesSummaryReportService salesSummaryReportService;

    public SalesSummaryReportResource(SalesSummaryReportService salesSummaryReportService) {
        this.salesSummaryReportService = salesSummaryReportService;
    }

    /**
     * GET /api/sales-summary : Get daily sales summary for a date range
     *
     * @param startDate start date (required)
     * @param endDate end date (required)
     * @return ResponseEntity with list of daily summaries
     */
    @GetMapping
    @PreAuthorize("hasAuthority(\"" + AuthoritiesConstants.ADMIN + "\")")
    public ResponseEntity<List<DailySalesSummaryDTO>> getDailySalesSummary(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        List<DailySalesSummaryDTO> result = salesSummaryReportService.getDailySalesSummary(startDate, endDate);
        return ResponseEntity.ok(result);
    }

    /**
     * GET /api/sales-summary/by-date : Get daily sales summary for a specific date
     *
     * @param date the date (required)
     * @return ResponseEntity with list of summaries grouped by sale type
     */
    @GetMapping("/by-date")
    @PreAuthorize("hasAuthority(\"" + AuthoritiesConstants.ADMIN + "\")")
    public ResponseEntity<List<DailySalesSummaryDTO>> getDailySalesSummaryByDate(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        List<DailySalesSummaryDTO> result = salesSummaryReportService.getDailySalesSummaryByDate(date);
        return ResponseEntity.ok(result);
    }

    /**
     * GET /api/sales-summary/by-type : Get daily sales summary filtered by sale type
     *
     * @param startDate start date (required)
     * @param endDate end date (required)
     * @param typeVente sale type (required, e.g., "VO", "VNO")
     * @return ResponseEntity with list of daily summaries for that type
     */
    @GetMapping("/by-type")
    @PreAuthorize("hasAuthority(\"" + AuthoritiesConstants.ADMIN + "\")")
    public ResponseEntity<List<DailySalesSummaryDTO>> getDailySalesSummaryByType(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
        @RequestParam TypeVenteDTO typeVente
    ) {
        List<DailySalesSummaryDTO> result = salesSummaryReportService.getDailySalesSummaryByType(
            startDate,
            endDate,
            typeVente
        );
        return ResponseEntity.ok(result);
    }
}
