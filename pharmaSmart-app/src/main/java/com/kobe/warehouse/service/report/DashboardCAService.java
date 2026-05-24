package com.kobe.warehouse.service.report;

import com.kobe.warehouse.service.dto.FinancesSummaryDTO;
import com.kobe.warehouse.service.dto.dashboard.PerformanceVendeurDTO;
import com.kobe.warehouse.service.dto.report.*;
import java.time.LocalDate;
import java.util.List;



/**
 * Service interface for Dashboard Chiffre d'Affaires (CA)
 */
public interface DashboardCAService {

    /**
     * Get daily CA summary for a date range
     */
    List<DailyCADTO> getDailySummary(LocalDate startDate, LocalDate endDate);

    /**
     * Get overall summary with KPIs for today, week, month, year
     */
    DashboardCASummaryDTO getOverallSummary();

    /**
     * Get evolution data for charts
     * @param period "daily", "weekly", "monthly"
     * @param startDate start date
     * @param endDate end date
     */
    DashboardCAEvolutionDTO getEvolutionData(String period, LocalDate startDate, LocalDate endDate);

    /**
     * Get CA distribution by payment method for a date range
     */
    List<PaymentMethodCADTO> getPaymentMethodDistribution(LocalDate startDate, LocalDate endDate);

    /**
     * Get CA distribution by product family for a date range
     */
    List<ProductFamilyCADTO> getProductFamilyDistribution(LocalDate startDate, LocalDate endDate);

    /**
     * Get top 10 products by CA for a date range
     */
    List<TopProductDTO> getTopProducts(LocalDate startDate, LocalDate endDate, Integer limit);


    /**
     * Get finances summary KPIs (supplier debt + third-party receivables)
     */
    FinancesSummaryDTO getSummaryFinances();

    /**
     * Get average basket evolution over the last 12 months
     */
    BasketEvolutionDTO getBasketEvolution();

    /**
     * Refresh materialized views
     */
    void refreshViews();

    /**
     * Aggregate sales performance by staff member (vendeur) for a date range
     */
    List<PerformanceVendeurDTO> getSalesByStaff(LocalDate startDate, LocalDate endDate);

    /**
     * Get generics vs branded substitution statistics for a date range
     */
    GenericsSubstitutionDTO getGenericsSubstitution(LocalDate startDate, LocalDate endDate);

    /**
     * Get discount (remise) KPIs for a date range
     */
    RemisesAnalysisKpiDTO getRemisesKpi(LocalDate startDate, LocalDate endDate);

    /**
     * Get top N products by discount amount for a date range
     */
    List<TopRemiseProduitDTO> getRemisesTopProducts(LocalDate startDate, LocalDate endDate, int limit);

}
