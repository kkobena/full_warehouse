package com.kobe.warehouse.service.report;

import com.kobe.warehouse.domain.enumeration.StockAlertType;
import com.kobe.warehouse.service.dto.report.StockAlertDTO;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface StockAlertReportService {
    /**
     * Get stock alerts (out of stock, low stock, near expiration)
     *
     * @param alertTypes Optional list of alert types to filter by
     * @param pageable   Pagination information
     * @return Page of stock alerts
     */
    Page<StockAlertDTO> getStockAlerts(List<StockAlertType> alertTypes, Pageable pageable);

    /**
     * Get count of stock alerts by type
     *
     * @return Map of alert type to count
     */
    Map<StockAlertType, Long> getStockAlertsCount();

}
