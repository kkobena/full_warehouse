package com.kobe.warehouse.service.report;

import com.kobe.warehouse.service.dto.report.StockAlertDTO;
import java.util.List;
import java.util.Map;

public interface StockAlertReportService {
    /**
     * Get all stock alerts (out of stock, low stock, near expiration)
     *
     * @param alertTypes Optional list of alert types to filter by
     * @return List of stock alerts
     */
    List<StockAlertDTO> getStockAlerts(List<StockAlertDTO.StockAlertType> alertTypes);

    /**
     * Get count of stock alerts by type
     *
     * @return Map of alert type to count
     */
    Map<StockAlertDTO.StockAlertType, Long> getStockAlertsCount();

    /**
     * Export stock alerts to Excel
     *
     * @param alertTypes Optional list of alert types to filter by
     * @return Excel file as byte array
     */
    byte[] exportToExcel(List<StockAlertDTO.StockAlertType> alertTypes) throws Exception;

    /**
     * Export stock alerts to CSV
     *
     * @param alertTypes Optional list of alert types to filter by
     * @return CSV file as byte array
     */
    byte[] exportToCsv(List<StockAlertDTO.StockAlertType> alertTypes) throws Exception;
}
