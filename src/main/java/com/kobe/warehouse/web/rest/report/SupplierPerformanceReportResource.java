package com.kobe.warehouse.web.rest.report;

import com.kobe.warehouse.service.dto.report.SupplierPerformanceDTO;
import com.kobe.warehouse.service.dto.report.SupplierPerformanceSummaryDTO;
import com.kobe.warehouse.service.report.SupplierPerformanceReportService;
import com.kobe.warehouse.service.report.pdf.SupplierPerformancePdfReportService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class SupplierPerformanceReportResource {

    private final SupplierPerformanceReportService supplierPerformanceReportService;
    private final SupplierPerformancePdfReportService supplierPerformancePdfReportService;

    public SupplierPerformanceReportResource(SupplierPerformanceReportService supplierPerformanceReportService, SupplierPerformancePdfReportService supplierPerformancePdfReportService) {
        this.supplierPerformanceReportService = supplierPerformanceReportService;
        this.supplierPerformancePdfReportService = supplierPerformancePdfReportService;
    }

    /**
     * GET /supplier-performance : Get all supplier performance data
     *
     * @return List of supplier performance records
     */
    @GetMapping("/supplier-performance")
    public ResponseEntity<List<SupplierPerformanceDTO>> getAllSupplierPerformance() {
        List<SupplierPerformanceDTO> performances = supplierPerformanceReportService.getAllSupplierPerformance();
        return ResponseEntity.ok().body(performances);
    }

    /**
     * GET /supplier-performance/{fournisseurId} : Get supplier performance for a specific supplier
     *
     * @param fournisseurId The supplier ID
     * @return Supplier performance data
     */
    @GetMapping("/supplier-performance/{fournisseurId}")
    public ResponseEntity<SupplierPerformanceDTO> getSupplierPerformance(@PathVariable Integer fournisseurId) {
        SupplierPerformanceDTO performance = supplierPerformanceReportService.getSupplierPerformance(fournisseurId);
        if (performance == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok().body(performance);
    }

    /**
     * GET /supplier-performance/top : Get top suppliers by purchase volume
     *
     * @param limit Maximum number of suppliers to return (default: 10)
     * @return List of top suppliers
     */
    @GetMapping("/supplier-performance/top")
    public ResponseEntity<List<SupplierPerformanceDTO>> getTopSuppliersByVolume(
        @RequestParam(defaultValue = "10") Integer limit
    ) {
        List<SupplierPerformanceDTO> topSuppliers = supplierPerformanceReportService.getTopSuppliersByVolume(limit);
        return ResponseEntity.ok().body(topSuppliers);
    }

    /**
     * GET /supplier-performance/score : Get suppliers by performance score
     *
     * @param minScore Minimum performance score (default: 70)
     * @return List of suppliers with score >= minScore
     */
    @GetMapping("/supplier-performance/score")
    public ResponseEntity<List<SupplierPerformanceDTO>> getSuppliersByPerformanceScore(
        @RequestParam(defaultValue = "70") Double minScore
    ) {
        List<SupplierPerformanceDTO> suppliers = supplierPerformanceReportService.getSuppliersByPerformanceScore(minScore);
        return ResponseEntity.ok().body(suppliers);
    }

    /**
     * GET /supplier-performance/delivery-issues : Get suppliers with delivery issues
     *
     * @return List of suppliers with conformity rate < 95% or avg delivery > 7 days
     */
    @GetMapping("/supplier-performance/delivery-issues")
    public ResponseEntity<List<SupplierPerformanceDTO>> getSuppliersWithDeliveryIssues() {
        List<SupplierPerformanceDTO> suppliers = supplierPerformanceReportService.getSuppliersWithDeliveryIssues();
        return ResponseEntity.ok().body(suppliers);
    }

    /**
     * GET /supplier-performance/summary : Get aggregated supplier performance summary
     *
     * @return Summary with aggregate metrics
     */
    @GetMapping("/supplier-performance/summary")
    public ResponseEntity<SupplierPerformanceSummaryDTO> getSupplierPerformanceSummary() {
        SupplierPerformanceSummaryDTO summary = supplierPerformanceReportService.getSupplierPerformanceSummary();
        return ResponseEntity.ok().body(summary);
    }

    /**
     * GET /supplier-performance/export : Export supplier performance report as PDF
     *
     * @return PDF file
     */
    @GetMapping(value = "/supplier-performance/export", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> exportSupplierPerformanceToPdf() {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Disposition", "attachment; filename=supplier-performance.pdf");
        return ResponseEntity.ok().headers(headers).body(supplierPerformancePdfReportService.export());
    }
}
