package com.kobe.warehouse.service;

import com.kobe.warehouse.service.dto.MaterializedViewRefreshDTO;
import com.kobe.warehouse.service.dto.TierRefreshResultDTO;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for refreshing materialized views on a scheduled basis.
 *
 * <p>Refresh Schedule:
 * - TIER 1 (High Priority): Every 15 minutes during business hours (8am-8pm) - Dashboard and alerts
 * - TIER 2 (Medium Priority): Every hour - Product and stock analytics
 * - TIER 3 (Low Priority): Every 6 hours - Complex analytical reports
 */
@Service
public class MaterializedViewRefreshService {

    private static final Logger LOG = LoggerFactory.getLogger(MaterializedViewRefreshService.class);
    // =====================================================
    // TIER 1: High Priority Views (Every 15 minutes during business hours)
    // =====================================================
    private static final List<String> TIER1_VIEWS = Arrays.asList(
        "mv_dashboard_ca_daily",
        "mv_dashboard_ca_payment_methods",
        "mv_stock_alerts",
        "mv_daily_sales_summary"
    );
    // =====================================================
    // TIER 2: Medium Priority Views (Every hour)
    // =====================================================
    private static final List<String> TIER2_VIEWS = Arrays.asList(
        "mv_dashboard_ca_product_families",
        "mv_monthly_top_products",
        "mv_stock_valuation"
    );
    // =====================================================
    // TIER 3: Low Priority Views (Every 6 hours)
    // =====================================================
    private static final List<String> TIER3_VIEWS = Arrays.asList(
        "mv_stock_rotation",
        "mv_customer_rfm",
        "mv_abc_pareto_analysis",
        "mv_pareto_summary",
        "mv_supplier_performance",
        "mv_profitability_summary",
        "mv_product_profitability"
    );

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * TIER 1: Refresh high-priority views every 15 minutes during business hours (8am-8pm).
     * These views are used in real-time dashboards and require fresh data.
     */
    @Scheduled(cron = "0 */15 8-20 * * *") // Every 15 minutes between 8am and 8pm
    public void refreshTier1ViewsScheduled() {
        LOG.info("Starting scheduled TIER 1 materialized views refresh");
        refreshTier1Views();
    }

    /**
     * TIER 2: Refresh medium-priority views every hour.
     * These views contain analytical data that can tolerate slight delays.
     */
    @Scheduled(cron = "0 0 * * * *") // Every hour at minute 0
    public void refreshTier2ViewsScheduled() {
        LOG.info("Starting scheduled TIER 2 materialized views refresh");
        refreshTier2Views();
    }

    /**
     * TIER 3: Refresh low-priority views every 6 hours.
     * These are complex analytical views that are less time-sensitive.
     */
    @Scheduled(cron = "0 0 9,12,15,18 * * *") // At 2am, 8am, 2pm, 8pm
    public void refreshTier3ViewsScheduled() {
        LOG.info("Starting scheduled TIER 3 materialized views refresh");
        refreshTier3Views();
    }

    /**
     * Manual refresh of TIER 1 views.
     *
     * @return Result summary with individual view results
     */
    public TierRefreshResultDTO refreshTier1Views() {
        LocalDateTime startTime = LocalDateTime.now();
        List<MaterializedViewRefreshDTO> results = refreshViews(TIER1_VIEWS, "TIER1");
        LocalDateTime endTime = LocalDateTime.now();
        return TierRefreshResultDTO.of("TIER1", results, startTime, endTime);
    }

    /**
     * Manual refresh of TIER 2 views.
     *
     * @return Result summary with individual view results
     */
    public TierRefreshResultDTO refreshTier2Views() {
        LocalDateTime startTime = LocalDateTime.now();
        List<MaterializedViewRefreshDTO> results = refreshViews(TIER2_VIEWS, "TIER2");
        LocalDateTime endTime = LocalDateTime.now();
        return TierRefreshResultDTO.of("TIER2", results, startTime, endTime);
    }

    /**
     * Manual refresh of TIER 3 views.
     *
     * @return Result summary with individual view results
     */
    public TierRefreshResultDTO refreshTier3Views() {
        LocalDateTime startTime = LocalDateTime.now();
        List<MaterializedViewRefreshDTO> results = refreshViews(TIER3_VIEWS, "TIER3");
        LocalDateTime endTime = LocalDateTime.now();
        return TierRefreshResultDTO.of("TIER3", results, startTime, endTime);
    }

    /**
     * Refresh all materialized views across all tiers.
     *
     * @return Result summary with all view results
     */
    public TierRefreshResultDTO refreshAllViews() {
        LocalDateTime startTime = LocalDateTime.now();
        List<MaterializedViewRefreshDTO> allResults = new ArrayList<>();

        LOG.info("Starting refresh of ALL materialized views");

        allResults.addAll(refreshViews(TIER1_VIEWS, "TIER1"));
        allResults.addAll(refreshViews(TIER2_VIEWS, "TIER2"));
        allResults.addAll(refreshViews(TIER3_VIEWS, "TIER3"));

        LocalDateTime endTime = LocalDateTime.now();
        TierRefreshResultDTO result = TierRefreshResultDTO.of("ALL", allResults, startTime, endTime);

        LOG.info(
            "Completed refresh of ALL materialized views: {} successful, {} failed in {}ms",
            result.successCount(),
            result.failedCount(),
            result.totalDurationMillis()
        );

        return result;
    }

    /**
     * Refresh a specific materialized view by name.
     *
     * @param viewName The name of the materialized view to refresh
     * @return Refresh result for the view
     */
    public MaterializedViewRefreshDTO refreshView(String viewName) {
        return refreshSingleView(viewName, "MANUAL");
    }

    /**
     * Refresh a list of views for a specific tier.
     *
     * @param viewNames List of view names to refresh
     * @param tier The tier identifier (TIER1, TIER2, TIER3)
     * @return List of refresh results
     */
    private List<MaterializedViewRefreshDTO> refreshViews(List<String> viewNames, String tier) {
        List<MaterializedViewRefreshDTO> results = new ArrayList<>();

        for (String viewName : viewNames) {
            MaterializedViewRefreshDTO result = refreshSingleView(viewName, tier);
            results.add(result);
        }

        long successCount = results.stream().filter(r -> "SUCCESS".equals(r.status())).count();
        long failedCount = results.stream().filter(r -> "FAILED".equals(r.status())).count();

        LOG.info("{} refresh completed: {} successful, {} failed", tier, successCount, failedCount);

        return results;
    }

    /**
     * Refresh a single materialized view with error handling.
     *
     * @param viewName The view name to refresh
     * @param tier The tier/source of the refresh request
     * @return Refresh result
     */
    @Transactional
    public MaterializedViewRefreshDTO refreshSingleView(String viewName, String tier) {
        LocalDateTime startTime = LocalDateTime.now();

        LOG.debug("Refreshing materialized view: {} ({})", viewName, tier);

        try {
            // Use REFRESH MATERIALIZED VIEW CONCURRENTLY to avoid locking
            // This requires a unique index on the view (which all our views have)
            String sql = "REFRESH MATERIALIZED VIEW CONCURRENTLY " + viewName;
            entityManager.createNativeQuery(sql).executeUpdate();

            LocalDateTime endTime = LocalDateTime.now();
            long duration = java.time.Duration.between(startTime, endTime).toMillis();

            LOG.info("Successfully refreshed {} in {}ms", viewName, duration);

            return MaterializedViewRefreshDTO.success(viewName, tier, startTime, endTime);
        } catch (Exception e) {
            LOG.error("Failed to refresh materialized view: {}", viewName, e);
            return MaterializedViewRefreshDTO.failed(viewName, tier, startTime, e.getMessage());
        }
    }

    /**
     * Get list of all materialized views managed by this service.
     *
     * @return List of all view names
     */
    public List<String> getAllManagedViews() {
        List<String> allViews = new ArrayList<>();
        allViews.addAll(TIER1_VIEWS);
        allViews.addAll(TIER2_VIEWS);
        allViews.addAll(TIER3_VIEWS);
        return allViews;
    }

    /**
     * Get views organized by tier.
     *
     * @return Map of tier names to view lists
     */
    public java.util.Map<String, List<String>> getViewsByTier() {
        return java.util.Map.of("TIER1", TIER1_VIEWS, "TIER2", TIER2_VIEWS, "TIER3", TIER3_VIEWS);
    }
}
