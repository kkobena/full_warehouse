package com.kobe.warehouse.service.report;

import com.kobe.warehouse.service.dto.report.TopProductDTO;
import java.time.LocalDate;
import java.util.List;

/**
 * Service interface for top products reports using mv_monthly_top_products
 */
public interface TopProductsReportService {
    /**
     * Get top N products by revenue for a specific month
     *
     * @param month the month (first day of month)
     * @param limit number of products to return
     * @return list of top products ordered by revenue DESC
     */
    List<TopProductDTO> getTopProductsByRevenue(LocalDate month, int limit);

    /**
     * Get top N products by quantity sold for a specific month
     *
     * @param month the month (first day of month)
     * @param limit number of products to return
     * @return list of top products ordered by quantity DESC
     */
    List<TopProductDTO> getTopProductsByQuantity(LocalDate month, int limit);

    /**
     * Get all products stats for a specific month
     *
     * @param month the month (first day of month)
     * @return list of all products with stats for that month
     */
    List<TopProductDTO> getAllProductsForMonth(LocalDate month);

    /**
     * Get monthly evolution for a specific product
     *
     * @param produitId the product ID
     * @param nbMonths number of months to retrieve (max 6)
     * @return list of monthly stats for this product
     */
    List<TopProductDTO> getProductMonthlyEvolution(Integer produitId, int nbMonths);
}
