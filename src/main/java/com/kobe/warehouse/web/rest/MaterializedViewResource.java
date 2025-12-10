package com.kobe.warehouse.web.rest;

import com.kobe.warehouse.security.AuthoritiesConstants;
import com.kobe.warehouse.service.MaterializedViewRefreshService;
import com.kobe.warehouse.service.dto.MaterializedViewRefreshDTO;
import com.kobe.warehouse.service.dto.TierRefreshResultDTO;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for managing materialized view refreshes.
 *
 * <p>Provides endpoints for:
 * - Manual refresh of materialized views by tier or individually
 * - Viewing refresh schedules and managed views
 * - Monitoring view refresh status
 *
 * <p>All endpoints require ADMIN authority.
 */
@RestController
@RequestMapping("/api/admin/materialized-views")
@Secured(AuthoritiesConstants.ADMIN)
public class MaterializedViewResource {

    private static final Logger LOG = LoggerFactory.getLogger(MaterializedViewResource.class);

    private final MaterializedViewRefreshService refreshService;

    public MaterializedViewResource(MaterializedViewRefreshService refreshService) {
        this.refreshService = refreshService;
    }

    /**
     * POST /api/admin/materialized-views/refresh/tier1
     * Manually refresh TIER 1 (high-priority) views.
     *
     * @return Refresh result with success/failure counts and individual view results
     */
    @PostMapping("/refresh/tier1")
    public ResponseEntity<TierRefreshResultDTO> refreshTier1() {
        LOG.info("Manual refresh requested for TIER 1 views");
        TierRefreshResultDTO result = refreshService.refreshTier1Views();
        return ResponseEntity.ok(result);
    }

    /**
     * POST /api/admin/materialized-views/refresh/tier2
     * Manually refresh TIER 2 (medium-priority) views.
     *
     * @return Refresh result with success/failure counts and individual view results
     */
    @PostMapping("/refresh/tier2")
    public ResponseEntity<TierRefreshResultDTO> refreshTier2() {
        LOG.info("Manual refresh requested for TIER 2 views");
        TierRefreshResultDTO result = refreshService.refreshTier2Views();
        return ResponseEntity.ok(result);
    }

    /**
     * POST /api/admin/materialized-views/refresh/tier3
     * Manually refresh TIER 3 (low-priority) views.
     *
     * @return Refresh result with success/failure counts and individual view results
     */
    @PostMapping("/refresh/tier3")
    public ResponseEntity<TierRefreshResultDTO> refreshTier3() {
        LOG.info("Manual refresh requested for TIER 3 views");
        TierRefreshResultDTO result = refreshService.refreshTier3Views();
        return ResponseEntity.ok(result);
    }

    /**
     * POST /api/admin/materialized-views/refresh/all
     * Manually refresh ALL materialized views across all tiers.
     *
     * <p>Warning: This operation may take several minutes to complete.
     *
     * @return Refresh result with success/failure counts and all view results
     */
    @PostMapping("/refresh/all")
    public ResponseEntity<TierRefreshResultDTO> refreshAll() {
        LOG.info("Manual refresh requested for ALL materialized views");
        TierRefreshResultDTO result = refreshService.refreshAllViews();
        return ResponseEntity.ok(result);
    }

    /**
     * POST /api/admin/materialized-views/refresh/{viewName}
     * Manually refresh a specific materialized view by name.
     *
     * @param viewName The name of the materialized view to refresh
     * @return Refresh result for the specific view
     */
    @PostMapping("/refresh/{viewName}")
    public ResponseEntity<MaterializedViewRefreshDTO> refreshView(@PathVariable String viewName) {
        LOG.info("Manual refresh requested for view: {}", viewName);

        // Validate that the view is managed by the service
        if (!refreshService.getAllManagedViews().contains(viewName)) {
            LOG.warn("Attempted to refresh unknown view: {}", viewName);
            return ResponseEntity.badRequest().build();
        }

        MaterializedViewRefreshDTO result = refreshService.refreshView(viewName);
        return ResponseEntity.ok(result);
    }

    /**
     * GET /api/admin/materialized-views/managed
     * Get list of all materialized views managed by the refresh service.
     *
     * @return List of all managed view names
     */
    @GetMapping("/managed")
    public ResponseEntity<List<String>> getManagedViews() {
        return ResponseEntity.ok(refreshService.getAllManagedViews());
    }

    /**
     * GET /api/admin/materialized-views/tiers
     * Get all views organized by tier with their refresh schedules.
     *
     * @return Map of tier information including view names and schedules
     */
    @GetMapping("/tiers")
    public ResponseEntity<Map<String, Object>> getViewsByTier() {
        Map<String, List<String>> viewsByTier = refreshService.getViewsByTier();

        Map<String, Object> response = Map.of(
            "TIER1", Map.of(
                "views", viewsByTier.get("TIER1"),
                "schedule", "Every 15 minutes (8am-8pm)",
                "cron", "0 */15 8-20 * * *",
                "priority", "HIGH"
            ),
            "TIER2", Map.of(
                "views", viewsByTier.get("TIER2"),
                "schedule", "Every hour",
                "cron", "0 0 * * * *",
                "priority", "MEDIUM"
            ),
            "TIER3", Map.of(
                "views", viewsByTier.get("TIER3"),
                "schedule", "Every 6 hours (2am, 8am, 2pm, 8pm)",
                "cron", "0 0 2,8,14,20 * * *",
                "priority", "LOW"
            )
        );

        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/admin/materialized-views/info
     * Get general information about the materialized view refresh system.
     *
     * @return System information and configuration
     */
    @GetMapping("/info")
    public ResponseEntity<Map<String, Object>> getSystemInfo() {
        Map<String, List<String>> viewsByTier = refreshService.getViewsByTier();

        Map<String, Object> info = Map.of(
            "totalManagedViews", refreshService.getAllManagedViews().size(),
            "tier1Count", viewsByTier.get("TIER1").size(),
            "tier2Count", viewsByTier.get("TIER2").size(),
            "tier3Count", viewsByTier.get("TIER3").size(),
            "refreshMethod", "CONCURRENT (non-blocking)",
            "schedulingEnabled", true,
            "description", "Automated materialized view refresh system with 3-tier priority scheduling"
        );

        return ResponseEntity.ok(info);
    }
}
