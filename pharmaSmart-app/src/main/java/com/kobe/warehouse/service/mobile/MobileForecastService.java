package com.kobe.warehouse.service.mobile;

import com.kobe.warehouse.domain.enumeration.SalesStatut;
import com.kobe.warehouse.service.dto.mobile.DailySalesDTO;
import com.kobe.warehouse.service.dto.mobile.ForecastRequestDTO;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for mobile ML forecasting data (Phase 4).
 * Provides historical sales data for machine learning predictions.
 */
@Service
@Transactional(readOnly = true)
public class MobileForecastService {

    private static final Logger LOG = LoggerFactory.getLogger(MobileForecastService.class);
    private static final String SALES_CA_TYPE = "CA";

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Get daily sales history for ML forecasting.
     * Returns aggregated daily sales for the specified number of days.
     *
     * @param request Forecast request with parameters
     * @return List of daily sales data
     */
    public List<DailySalesDTO> getDailySalesHistory(ForecastRequestDTO request) {
        int days = request.historicalDays() != null ? request.historicalDays() : 30;

        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(days);

        LOG.debug("Getting daily sales history from {} to {} ({} days)", startDate, endDate, days);

        return getDailySalesForPeriod(startDate, endDate);
    }

    /**
     * Get daily sales history for a specific date range.
     *
     * @param startDate Start date (inclusive)
     * @param endDate End date (inclusive)
     * @return List of daily sales data
     */
    public List<DailySalesDTO> getDailySalesHistory(LocalDate startDate, LocalDate endDate) {
        LOG.debug("Getting daily sales history from {} to {}", startDate, endDate);
        return getDailySalesForPeriod(startDate, endDate);
    }

    /**
     * Get daily sales for a period with full details.
     */
    private List<DailySalesDTO> getDailySalesForPeriod(LocalDate startDate, LocalDate endDate) {
        String sql = """
            SELECT
                s.sale_date as date,
                COALESCE(SUM(s.sales_amount), 0) as sales_amount,
                COUNT(DISTINCT s.id) as transactions_count,
                COUNT(DISTINCT s.customer_id) as customers_count
            FROM sales s
            WHERE s.sale_date BETWEEN :startDate AND :endDate
              AND s.statut = :statut
              AND s.canceled = false
              AND s.ca = :caType
            GROUP BY s.sale_date
            ORDER BY s.sale_date
            """;

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("startDate", startDate);
        query.setParameter("endDate", endDate);
        query.setParameter("statut", SalesStatut.CLOSED.name());
        query.setParameter("caType", SALES_CA_TYPE);

        @SuppressWarnings("unchecked")
        List<Object[]> results = query.getResultList();

        List<DailySalesDTO> dailySales = new ArrayList<>();
        for (Object[] row : results) {
            dailySales.add(DailySalesDTO.fromQueryResult(row));
        }

        // Fill missing dates with zero sales
        return fillMissingDates(dailySales, startDate, endDate);
    }

    /**
     * Fill missing dates in the sales data with zero amounts.
     * Important for ML models that expect continuous data.
     */
    private List<DailySalesDTO> fillMissingDates(
        List<DailySalesDTO> existingSales,
        LocalDate startDate,
        LocalDate endDate
    ) {
        List<DailySalesDTO> completeSales = new ArrayList<>();
        LocalDate currentDate = startDate;

        int existingIndex = 0;

        while (!currentDate.isAfter(endDate)) {
            if (existingIndex < existingSales.size() &&
                existingSales.get(existingIndex).date().equals(currentDate)) {
                // Use existing data
                completeSales.add(existingSales.get(existingIndex));
                existingIndex++;
            } else {
                // Fill with zero for missing date
                completeSales.add(new DailySalesDTO(
                    currentDate,
                    currentDate.toString(),
                    0L,
                    0,
                    0L,
                    0
                ));
            }
            currentDate = currentDate.plusDays(1);
        }

        return completeSales;
    }

    /**
     * Get sales statistics for validation purposes.
     *
     * @param days Number of days to analyze
     * @return Statistics summary
     */
    public SalesStatistics getSalesStatistics(int days) {
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(days);

        String sql = """
            SELECT
                COUNT(DISTINCT s.sale_date) as days_with_sales,
                COALESCE(AVG(daily.sales_amount), 0) as avg_daily_sales,
                COALESCE(MIN(daily.sales_amount), 0) as min_daily_sales,
                COALESCE(MAX(daily.sales_amount), 0) as max_daily_sales,
                COALESCE(STDDEV(daily.sales_amount), 0) as stddev_sales
            FROM (
                SELECT
                    s.sale_date,
                    SUM(s.sales_amount) as sales_amount
                FROM sales s
                WHERE s.sale_date BETWEEN :startDate AND :endDate
                  AND s.statut = :statut
                  AND s.canceled = false
                  AND s.ca = :caType
                GROUP BY s.sale_date
            ) daily
            """;

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("startDate", startDate);
        query.setParameter("endDate", endDate);
        query.setParameter("statut", SalesStatut.CLOSED.name());
        query.setParameter("caType", SALES_CA_TYPE);

        Object[] row = (Object[]) query.getSingleResult();

        return new SalesStatistics(
            ((Number) row[0]).intValue(),
            ((Number) row[1]).doubleValue(),
            ((Number) row[2]).longValue(),
            ((Number) row[3]).longValue(),
            ((Number) row[4]).doubleValue()
        );
    }

    /**
     * Sales statistics record for data quality validation
     */
    public record SalesStatistics(
        int daysWithSales,
        double avgDailySales,
        long minDailySales,
        long maxDailySales,
        double stdDevSales
    ) {

        /**
         * Calculate coefficient of variation
         * (lower is better for forecasting accuracy)
         */
        public double coefficientOfVariation() {
            return avgDailySales > 0 ? stdDevSales / avgDailySales : 0;
        }

        /**
         * Assess if data is suitable for forecasting
         */
        public boolean isSuitableForForecasting() {
            // Need at least 14 days of data
            if (daysWithSales < 14) {
                return false;
            }

            // Coefficient of variation should be < 2 for reasonable predictions
            return coefficientOfVariation() < 2.0;
        }
    }
}
