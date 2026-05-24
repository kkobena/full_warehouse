package com.kobe.warehouse.web.rest.report;

import com.kobe.warehouse.service.dto.report.*;
import com.kobe.warehouse.service.report.ComparativeReportService;
import java.time.LocalDate;
import java.util.List;

import com.kobe.warehouse.service.report.pdf.ComparativePdfReportService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for Comparative Reports (Tableaux Comparatifs)
 */
@RestController
@RequestMapping("/api/comparative-reports")
public class ComparativeReportResource {

    private final ComparativeReportService comparativeReportService;
    private final ComparativePdfReportService comparativePdfReportService;

    public ComparativeReportResource(ComparativeReportService comparativeReportService, ComparativePdfReportService comparativePdfReportService) {
        this.comparativeReportService = comparativeReportService;
        this.comparativePdfReportService = comparativePdfReportService;
    }

    /**
     * GET /api/comparative-reports/monthly : Get monthly CA comparison
     *
     * @param year the year to compare (defaults to current year)
     * @return list of monthly comparisons
     */
    @GetMapping("/monthly")
    public ResponseEntity<List<ComparativeCADTO>> getMonthlyComparison(@RequestParam(required = false) Integer year) {
        int targetYear = year != null ? year : LocalDate.now().getYear();
        List<ComparativeCADTO> result = comparativeReportService.getMonthlyComparison(targetYear);
        return ResponseEntity.ok(result);
    }

    /**
     * GET /api/comparative-reports/quarterly : Get quarterly CA comparison
     *
     * @param year the year to compare (defaults to current year)
     * @return list of quarterly comparisons
     */
    @GetMapping("/quarterly")
    public ResponseEntity<List<ComparativeCADTO>> getQuarterlyComparison(@RequestParam(required = false) Integer year) {
        int targetYear = year != null ? year : LocalDate.now().getYear();
        List<ComparativeCADTO> result = comparativeReportService.getQuarterlyComparison(targetYear);
        return ResponseEntity.ok(result);
    }

    /**
     * GET /api/comparative-reports/yearly : Get yearly CA comparison
     *
     * @param startDate start date
     * @param endDate end date
     * @return list of yearly comparisons
     */
    @GetMapping("/yearly")
    public ResponseEntity<List<ComparativeCADTO>> getYearlyComparison(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        List<ComparativeCADTO> result = comparativeReportService.getYearlyComparison(startDate, endDate);
        return ResponseEntity.ok(result);
    }

    /**
     * GET /api/comparative-reports/by-sales-type : Get CA comparison by sales type
     *
     * @param currentYear current year
     * @param previousYear previous year
     * @return list of comparisons by sales type
     */
    @GetMapping("/by-sales-type")
    public ResponseEntity<List<ComparativeByTypeDTO>> getComparisonBySalesType(
        @RequestParam(required = false) Integer currentYear,
        @RequestParam(required = false) Integer previousYear
    ) {
        int year = currentYear != null ? currentYear : LocalDate.now().getYear();
        int prevYear = previousYear != null ? previousYear : year - 1;
        List<ComparativeByTypeDTO> result = comparativeReportService.getComparisonBySalesType(year, prevYear);
        return ResponseEntity.ok(result);
    }

    /**
     * GET /api/comparative-reports/summary : Get overall comparative summary
     *
     * @return comparative summary
     */
    @GetMapping("/summary")
    public ResponseEntity<ComparativeSummaryDTO> getComparativeSummary() {
        ComparativeSummaryDTO result = comparativeReportService.getComparativeSummary();
        return ResponseEntity.ok(result);
    }

    /**
     * GET /api/comparative-reports/by-family : Get CA comparison by product family (N vs N-1)
     *
     * @param currentYear  current year (defaults to current year)
     * @param previousYear previous year (defaults to currentYear - 1)
     * @return list of comparisons by family, ordered by current CA descending
     */
    @GetMapping("/by-family")
    public ResponseEntity<List<ComparativeByFamilyDTO>> getComparisonByFamily(
        @RequestParam(required = false) Integer currentYear,
        @RequestParam(required = false) Integer previousYear
    ) {
        int year     = currentYear  != null ? currentYear  : LocalDate.now().getYear();
        int prevYear = previousYear != null ? previousYear : year - 1;
        return ResponseEntity.ok(comparativeReportService.getComparisonByFamily(year, prevYear));
    }

    /**
     * GET /api/comparative-reports/by-fournisseur : Get CA comparison by supplier (N vs N-1)
     */
    @GetMapping("/by-fournisseur")
    public ResponseEntity<List<ComparativeByFournisseurDTO>> getComparisonByFournisseur(
        @RequestParam(required = false) Integer currentYear,
        @RequestParam(required = false) Integer previousYear
    ) {
        int year     = currentYear  != null ? currentYear  : LocalDate.now().getYear();
        int prevYear = previousYear != null ? previousYear : year - 1;
        return ResponseEntity.ok(comparativeReportService.getComparisonByFournisseur(year, prevYear));
    }

    /**
     * GET /api/comparative-reports/export : Export comparative report to PDF
     *
     * @param comparisonType type of comparison (MONTHLY, QUARTERLY, YEARLY)
     * @param year the year
     * @return PDF file
     */
    @GetMapping(value = "/export", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> exportComparativeReportToPdf(
        @RequestParam(defaultValue = "MONTHLY") String comparisonType,
        @RequestParam(required = false) Integer year
    ) {
        int targetYear = year != null ? year : LocalDate.now().getYear();
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_PDF).body( comparativePdfReportService.export(comparisonType, targetYear));
    }
}
