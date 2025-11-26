package com.kobe.warehouse.service.report;

import com.kobe.warehouse.service.dto.report.SupplierPerformanceDTO;
import com.kobe.warehouse.service.dto.report.SupplierPerformanceSummaryDTO;
import java.util.List;

public interface SupplierPerformanceReportService {
    /**
     * Get all supplier performance data
     *
     * @return List of supplier performance records
     */
    List<SupplierPerformanceDTO> getAllSupplierPerformance();

    /**
     * Get supplier performance for a specific supplier
     *
     * @param fournisseurId The supplier ID
     * @return Supplier performance data
     */
    SupplierPerformanceDTO getSupplierPerformance(Integer fournisseurId);

    /**
     * Get top suppliers by purchase volume (last 12 months)
     *
     * @param limit Maximum number of suppliers to return
     * @return List of top suppliers
     */
    List<SupplierPerformanceDTO> getTopSuppliersByVolume(Integer limit);

    /**
     * Get suppliers by performance score
     *
     * @param minScore Minimum performance score (0-100)
     * @return List of suppliers with score >= minScore
     */
    List<SupplierPerformanceDTO> getSuppliersByPerformanceScore(Double minScore);

    /**
     * Get suppliers with delivery issues (conformity rate < 95% or avg delivery > 7 days)
     *
     * @return List of suppliers with delivery issues
     */
    List<SupplierPerformanceDTO> getSuppliersWithDeliveryIssues();

    /**
     * Get aggregated supplier performance summary
     *
     * @return Summary with aggregate metrics
     */
    SupplierPerformanceSummaryDTO getSupplierPerformanceSummary();

    /**
     * Export supplier performance report as PDF
     *
     * @return PDF bytes
     */
    byte[] exportSupplierPerformanceToPdf();
}
