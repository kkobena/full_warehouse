package com.kobe.warehouse.service.report;

import com.kobe.warehouse.service.dto.report.StockValuationDTO;
import com.kobe.warehouse.service.dto.report.StockValuationSummaryDTO;
import java.util.List;

public interface StockValuationReportService {
    /**
     * Get all stock valuation data
     *
     * @return List of stock valuation records
     */
    List<StockValuationDTO> getAllStockValuation();

    /**
     * Get stock valuation filtered by category
     *
     * @param categorie The category name to filter by
     * @return List of stock valuation records for the category
     */
    List<StockValuationDTO> getStockValuationByCategory(String categorie);

    /**
     * Get stock valuation filtered by storage location
     *
     * @param storageLocation The storage location name to filter by
     * @return List of stock valuation records for the storage location
     */
    List<StockValuationDTO> getStockValuationByStorage(String storageLocation);

    /**
     * Get aggregated stock valuation summary
     *
     * @return Summary with total values and averages
     */
    StockValuationSummaryDTO getStockValuationSummary();

    /**
     * Export stock valuation report as PDF
     *
     * @return PDF bytes
     */
    byte[] exportStockValuationToPdf();
}
