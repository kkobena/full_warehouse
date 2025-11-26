package com.kobe.warehouse.web.rest.report;

import com.kobe.warehouse.service.dto.report.*;
import com.kobe.warehouse.service.report.DashboardCAService;
import java.time.LocalDate;
import java.util.List;

import com.kobe.warehouse.service.report.pdf.DashboardCAPdfExportService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for Dashboard Chiffre d'Affaires (CA)
 */
@RestController
@RequestMapping("/api/dashboard-ca")
public class DashboardCAResource {

    private final DashboardCAService dashboardCAService;
    private final DashboardCAPdfExportService dashboardCAPdfExportService;

    public DashboardCAResource(DashboardCAService dashboardCAService, DashboardCAPdfExportService dashboardCAPdfExportService) {
        this.dashboardCAService = dashboardCAService;
        this.dashboardCAPdfExportService = dashboardCAPdfExportService;
    }

    /**
     * GET /api/dashboard-ca/daily : Get daily CA summary
     *
     * @param startDate start date
     * @param endDate end date
     * @return list of daily CA data
     */
    @GetMapping("/daily")
    public ResponseEntity<List<DailyCADTO>> getDailySummary(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        List<DailyCADTO> result = dashboardCAService.getDailySummary(startDate, endDate);
        return ResponseEntity.ok(result);
    }

    /**
     * GET /api/dashboard-ca/summary : Get overall summary with KPIs
     *
     * @return dashboard summary
     */
    @GetMapping("/summary")
    public ResponseEntity<DashboardCASummaryDTO> getOverallSummary() {
        DashboardCASummaryDTO result = dashboardCAService.getOverallSummary();
        return ResponseEntity.ok(result);
    }

    /**
     * GET /api/dashboard-ca/evolution : Get evolution data for charts
     *
     * @param period "daily", "weekly", "monthly"
     * @param startDate start date
     * @param endDate end date
     * @return evolution data
     */
    @GetMapping("/evolution")
    public ResponseEntity<DashboardCAEvolutionDTO> getEvolutionData(
        @RequestParam(defaultValue = "daily") String period,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        DashboardCAEvolutionDTO result = dashboardCAService.getEvolutionData(period, startDate, endDate);
        return ResponseEntity.ok(result);
    }

    /**
     * GET /api/dashboard-ca/payment-methods : Get CA distribution by payment method
     *
     * @param startDate start date
     * @param endDate end date
     * @return list of payment method CA data
     */
    @GetMapping("/payment-methods")
    public ResponseEntity<List<PaymentMethodCADTO>> getPaymentMethodDistribution(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        List<PaymentMethodCADTO> result = dashboardCAService.getPaymentMethodDistribution(startDate, endDate);
        return ResponseEntity.ok(result);
    }

    /**
     * GET /api/dashboard-ca/product-families : Get CA distribution by product family
     *
     * @param startDate start date
     * @param endDate end date
     * @return list of product family CA data
     */
    @GetMapping("/product-families")
    public ResponseEntity<List<ProductFamilyCADTO>> getProductFamilyDistribution(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        List<ProductFamilyCADTO> result = dashboardCAService.getProductFamilyDistribution(startDate, endDate);
        return ResponseEntity.ok(result);
    }

    /**
     * GET /api/dashboard-ca/top-products : Get top products by CA
     *
     * @param startDate start date
     * @param endDate end date
     * @param limit number of products (default 10)
     * @return list of top products
     */
    @GetMapping("/top-products")
    public ResponseEntity<List<TopProductDTO>> getTopProducts(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
        @RequestParam(defaultValue = "10") Integer limit
    ) {
        List<TopProductDTO> result = dashboardCAService.getTopProducts(startDate, endDate, limit);
        return ResponseEntity.ok(result);
    }

    /**
     * GET /api/dashboard-ca/export : Export dashboard to PDF
     *
     * @param startDate start date
     * @param endDate end date
     * @return PDF file
     */
    @GetMapping(value = "/export", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> exportDashboardToPdf(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {

        return ResponseEntity.ok().contentType(MediaType.APPLICATION_PDF).body(dashboardCAPdfExportService.export(startDate, endDate));
    }

    /**
     * GET /api/dashboard-ca/export/excel : Export daily summary to Excel
     *
     * @param startDate start date
     * @param endDate end date
     * @return Excel file (.xlsx)
     */
    @GetMapping(value = "/export/excel", produces = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    public ResponseEntity<byte[]> exportDailySummaryToExcel(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        try {
            byte[] excelData = dashboardCAService.exportDailySummaryToExcel(startDate, endDate);
            String filename = "dashboard_ca_" + startDate + "_" + endDate + ".xlsx";

            return ResponseEntity
                .ok()
                .header("Content-Disposition", "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(excelData);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * GET /api/dashboard-ca/export/csv : Export daily summary to CSV
     *
     * @param startDate start date
     * @param endDate end date
     * @return CSV file
     */
    @GetMapping(value = "/export/csv", produces = "text/csv")
    public ResponseEntity<byte[]> exportDailySummaryToCsv(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        try {
            byte[] csvData = dashboardCAService.exportDailySummaryToCsv(startDate, endDate);
            String filename = "dashboard_ca_" + startDate + "_" + endDate + ".csv";

            return ResponseEntity
                .ok()
                .header("Content-Disposition", "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(csvData);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * GET /api/dashboard-ca/export/top-products/excel : Export top products to Excel
     *
     * @param startDate start date
     * @param endDate end date
     * @return Excel file (.xlsx)
     */
    @GetMapping(value = "/export/top-products/excel", produces = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    public ResponseEntity<byte[]> exportTopProductsToExcel(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        try {
            byte[] excelData = dashboardCAService.exportTopProductsToExcel(startDate, endDate);
            String filename = "top_products_" + startDate + "_" + endDate + ".xlsx";

            return ResponseEntity
                .ok()
                .header("Content-Disposition", "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(excelData);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * GET /api/dashboard-ca/export/top-products/csv : Export top products to CSV
     *
     * @param startDate start date
     * @param endDate end date
     * @return CSV file
     */
    @GetMapping(value = "/export/top-products/csv", produces = "text/csv")
    public ResponseEntity<byte[]> exportTopProductsToCsv(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        try {
            byte[] csvData = dashboardCAService.exportTopProductsToCsv(startDate, endDate);
            String filename = "top_products_" + startDate + "_" + endDate + ".csv";

            return ResponseEntity
                .ok()
                .header("Content-Disposition", "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(csvData);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * POST /api/dashboard-ca/refresh : Refresh materialized views
     *
     * @return success status
     */
    @PostMapping("/refresh")
    public ResponseEntity<Void> refreshViews() {
        dashboardCAService.refreshViews();
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
