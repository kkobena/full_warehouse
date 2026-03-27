package com.kobe.warehouse.service.scheduler;

import com.kobe.warehouse.service.dto.MaterializedViewRefreshDTO;
import com.kobe.warehouse.service.dto.TierRefreshResultDTO;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service de rafraîchissement des vues matérialisées.
 *
 * <p>Trois niveaux de priorité, chacun avec un cron configurable :
 * <ul>
 *   <li><b>TIER 1</b> — Dashboards temps réel (CA, alertes stock) : toutes les 15 min
 *       pendant les heures d'ouverture</li>
 *   <li><b>TIER 2</b> — Données analytiques (familles, valorisation) : toutes les heures
 *       pendant les heures d'ouverture</li>
 *   <li><b>TIER 3</b> — Analyses complexes (RFM, performance fournisseurs) : 4x/jour</li>
 * </ul>
 *
 * <p>Les méthodes planifiées sont {@code @Async} pour ne pas bloquer le pool scheduling
 * (taille 2) et éviter d'impacter les autres jobs ou le travail des utilisateurs.
 * Le {@code REFRESH MATERIALIZED VIEW CONCURRENTLY} côté PostgreSQL ne bloque pas
 * les lectures non plus.
 *
 * <p>Un {@code refreshAllViews()} est aussi appelé par {@link JobOrchestrationService}
 * en fin de pipeline (démarrage + nocturne) pour garantir des dashboards à jour.
 */
@Service
public class MaterializedViewRefreshService {

    private static final Logger LOG = LoggerFactory.getLogger(MaterializedViewRefreshService.class);

    // ── TIER 1 : Dashboards temps réel ───────────────────────────────────────────
    private static final List<String> TIER1_VIEWS = List.of(
        "mv_dashboard_ca_daily",
        "mv_dashboard_ca_payment_methods",
        "mv_stock_alerts",
        "mv_daily_sales_summary"
    );

    // ── TIER 2 : Données analytiques ─────────────────────────────────────────────
    private static final List<String> TIER2_VIEWS = List.of(
        "mv_dashboard_ca_product_families",
        "mv_monthly_top_products",
        "mv_stock_valuation",
        "mv_stock_valuation_by_rayon"
    );

    // ── TIER 3 : Analyses complexes ──────────────────────────────────────────────
    private static final List<String> TIER3_VIEWS = List.of(
        "mv_customer_rfm",
        "mv_supplier_performance",
        "mv_marge_produit"
    );

    private static final List<String> NON_CONCURRENT_VIEWS = List.of();

    @PersistenceContext
    private EntityManager entityManager;

    // ── Méthodes planifiées (async pour ne pas bloquer le pool scheduling) ────────

    /**
     * TIER 1 : toutes les 15 minutes pendant les heures d'ouverture.
     * Configurable via {@code pharma-smart.views.dashboards-cron}.
     */
    @Async
    @Transactional
    @Scheduled(cron = "${pharma-smart.views.dashboards-cron:0 */15 8-20 * * *}")
    public void refreshTier1ViewsScheduled() {
        LOG.debug("Scheduled TIER 1 refresh");
        refreshViews(TIER1_VIEWS, "TIER1");
    }

    /**
     * TIER 2 : toutes les heures pendant les heures d'ouverture.
     * Configurable via {@code pharma-smart.views.analytique-cron}.
     */
    @Async
    @Transactional
    @Scheduled(cron = "${pharma-smart.views.analytique-cron:0 0 8-20 * * *}")
    public void refreshTier2ViewsScheduled() {
        LOG.debug("Scheduled TIER 2 refresh");
        refreshViews(TIER2_VIEWS, "TIER2");
    }

    /**
     * TIER 3 : 4 fois par jour (9h, 12h, 15h, 18h).
     * Configurable via {@code pharma-smart.views.reporting-cron}.
     */
    @Async
    @Transactional
    @Scheduled(cron = "${pharma-smart.views.reporting-cron:0 0 9,12,15,18 * * *}")
    public void refreshTier3ViewsScheduled() {
        LOG.debug("Scheduled TIER 3 refresh");
        refreshViews(TIER3_VIEWS, "TIER3");
    }

    // ── API publique (synchrone, pour appels manuels et pipeline) ─────────────────

    @Transactional
    public TierRefreshResultDTO refreshTier1Views() {
        LocalDateTime startTime = LocalDateTime.now();
        return TierRefreshResultDTO.of("TIER1", refreshViews(TIER1_VIEWS, "TIER1"), startTime, LocalDateTime.now());
    }

    @Transactional
    public TierRefreshResultDTO refreshTier2Views() {
        LocalDateTime startTime = LocalDateTime.now();
        return TierRefreshResultDTO.of("TIER2", refreshViews(TIER2_VIEWS, "TIER2"), startTime, LocalDateTime.now());
    }

    @Transactional
    public TierRefreshResultDTO refreshTier3Views() {
        LocalDateTime startTime = LocalDateTime.now();
        return TierRefreshResultDTO.of("TIER3", refreshViews(TIER3_VIEWS, "TIER3"), startTime, LocalDateTime.now());
    }

    @Transactional
    public TierRefreshResultDTO refreshAllViews() {
        LocalDateTime startTime = LocalDateTime.now();
        List<MaterializedViewRefreshDTO> allResults = new ArrayList<>();

        LOG.info("Refresh ALL materialized views");

        allResults.addAll(refreshViews(TIER1_VIEWS, "TIER1"));
        allResults.addAll(refreshViews(TIER2_VIEWS, "TIER2"));
        allResults.addAll(refreshViews(TIER3_VIEWS, "TIER3"));

        TierRefreshResultDTO result = TierRefreshResultDTO.of("ALL", allResults, startTime, LocalDateTime.now());

        LOG.info("ALL views refreshed: {} OK, {} KO en {}ms",
            result.successCount(), result.failedCount(), result.totalDurationMillis());

        return result;
    }

    @Transactional
    public MaterializedViewRefreshDTO refreshView(String viewName) {
        return refreshSingleView(viewName, "MANUAL");
    }

    public List<String> getAllManagedViews() {
        List<String> allViews = new ArrayList<>();
        allViews.addAll(TIER1_VIEWS);
        allViews.addAll(TIER2_VIEWS);
        allViews.addAll(TIER3_VIEWS);
        return allViews;
    }

    public Map<String, List<String>> getViewsByTier() {
        return Map.of("TIER1", TIER1_VIEWS, "TIER2", TIER2_VIEWS, "TIER3", TIER3_VIEWS);
    }

    // ── Méthodes internes ────────────────────────────────────────────────────────

    private List<MaterializedViewRefreshDTO> refreshViews(List<String> viewNames, String tier) {
        List<MaterializedViewRefreshDTO> results = new ArrayList<>();

        for (String viewName : viewNames) {
            results.add(refreshSingleView(viewName, tier));
        }

        long successCount = results.stream().filter(r -> "SUCCESS".equals(r.status())).count();
        long failedCount = results.stream().filter(r -> "FAILED".equals(r.status())).count();

        LOG.info("{} refresh: {} OK, {} KO", tier, successCount, failedCount);

        return results;
    }

    private MaterializedViewRefreshDTO refreshSingleView(String viewName, String tier) {
        LocalDateTime startTime = LocalDateTime.now();

        try {
            boolean useConcurrent = !NON_CONCURRENT_VIEWS.contains(viewName);
            String sql = useConcurrent
                ? "REFRESH MATERIALIZED VIEW CONCURRENTLY " + viewName
                : "REFRESH MATERIALIZED VIEW " + viewName;

            entityManager.createNativeQuery(sql).executeUpdate();

            LocalDateTime endTime = LocalDateTime.now();
            long duration = java.time.Duration.between(startTime, endTime).toMillis();

            LOG.debug("Refreshed {} in {}ms (concurrent={})", viewName, duration, useConcurrent);

            return MaterializedViewRefreshDTO.success(viewName, tier, startTime, endTime);
        } catch (Exception e) {
            LOG.error("Failed to refresh {}", viewName, e);
            return MaterializedViewRefreshDTO.failed(viewName, tier, startTime, e.getMessage());
        }
    }
}
