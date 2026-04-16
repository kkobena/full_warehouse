package com.kobe.warehouse.service;

import com.kobe.warehouse.domain.enumeration.StockAlertType;
import com.kobe.warehouse.repository.SemoisSuggestionViewRepository;
import com.kobe.warehouse.service.dto.DashboardAlertCountDTO;
import com.kobe.warehouse.service.report.StockAlertReportService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for retrieving dashboard alert counts for pharmacy management
 */
@Service
@Transactional(readOnly = true)
public class DashboardAlertService {

    @PersistenceContext
    private EntityManager entityManager;

    private final StockAlertReportService stockAlertReportService;
    private final SemoisSuggestionViewRepository semoisSuggestionViewRepository;

    public DashboardAlertService(StockAlertReportService stockAlertReportService,
                                  SemoisSuggestionViewRepository semoisSuggestionViewRepository) {
        this.stockAlertReportService = stockAlertReportService;
        this.semoisSuggestionViewRepository = semoisSuggestionViewRepository;
    }

    /**
     * Get all alert counts for the dashboard
     * Results are cached for 5 minutes to improve performance
     *
     * @return DashboardAlertCountDTO with all alert counts
     */
    @Cacheable(value = "dashboardAlertCounts", unless = "#result == null")
    public DashboardAlertCountDTO getAlertCounts() {
        // Get stock alerts counts (péremptions, ruptures)
        Map<StockAlertType, Long> stockAlertCounts = stockAlertReportService.getStockAlertsCount();

        Long peremptionCount = stockAlertCounts.getOrDefault(StockAlertType.PEREMPTION, 0L);
        Long ruptureCount = stockAlertCounts.getOrDefault(StockAlertType.RUPTURE, 0L);

        // Get recent stock entries count (last 24 hours)
        Long entreeCount = getRecentStockEntriesCount();

        // Get recent adjustments count (last 24 hours)
        Long ajustementCount = getRecentAdjustmentsCount();

        // Get recent price modifications count (last 24 hours)
        Long prixModifCount = getRecentPriceModificationsCount();

        // Get SEMOIS urgent products count (rupture + sous seuil — produits à commander)
        Long urgentCount = getUrgentProductsCount();

        // Factures tiers-payant dont l'échéance de règlement est dépassée
        Long facturationOverdueCount = getFacturationOverdueCount();

        return new DashboardAlertCountDTO(peremptionCount, ruptureCount, entreeCount, ajustementCount, prixModifCount, urgentCount, facturationOverdueCount);
    }

    /**
     * Count SEMOIS urgent products (rupture + sous seuil = stock_actuel < stock_objectif, vmm > 0)
     * Uses v_semois_suggestion view via repository
     */
    private Long getUrgentProductsCount() {
        try {
            Long count = semoisSuggestionViewRepository.countUrgentProducts();
            return count != null ? count : 0L;
        } catch (Exception e) {
            return 0L;
        }
    }

    /**
     * Count stock entries from the last 24 hours
     * Uses InventoryTransaction table with mouvement_type = 'ENTREE_STOCK'
     */
    private Long getRecentStockEntriesCount() {
        LocalDate yesterday = LocalDate.now().minusDays(1);

        String sql =
            "SELECT COUNT(DISTINCT it.entity_id) " +
            "FROM inventory_transaction it " +
            "WHERE it.transaction_date >= :yesterday " +
            "AND it.mouvement_type = 'ENTREE_STOCK'";

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("yesterday", yesterday);

        Number result = (Number) query.getSingleResult();
        return result != null ? result.longValue() : 0L;
    }

    /**
     * Count stock adjustments from the last 24 hours
     * Uses InventoryTransaction table with mouvement_type IN ('AJUSTEMENT_IN', 'AJUSTEMENT_OUT')
     */
    private Long getRecentAdjustmentsCount() {
        LocalDate yesterday = LocalDate.now().minusDays(1);

        String sql =
            "SELECT COUNT(DISTINCT it.entity_id) " +
            "FROM inventory_transaction it " +
            "WHERE it.transaction_date >= :yesterday " +
            "AND it.mouvement_type IN ('AJUSTEMENT_IN', 'AJUSTEMENT_OUT')";

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("yesterday", yesterday);

        Number result = (Number) query.getSingleResult();
        return result != null ? result.longValue() : 0L;
    }

    /**
     * Count factures tiers-payant whose settlement deadline has passed.
     * Deadline = invoice_date + delai_reglement days (default 30 if not set).
     * Only NOT_PAID and PARTIALLY_PAID invoices are counted.
     */
    private Long getFacturationOverdueCount() {
        String sql =
            "SELECT COUNT(*) " +
            "FROM facture_tiers_payant f " +
            "LEFT JOIN tiers_payant tp ON tp.id = f.tiers_payant_id " +
            "WHERE f.statut IN ('NOT_PAID', 'PARTIALLY_PAID') " +
            "  AND f.invoice_date + make_interval(days => COALESCE(tp.delai_reglement, 30)) < CURRENT_DATE";

        Query query = entityManager.createNativeQuery(sql);
        Number result = (Number) query.getSingleResult();
        return result != null ? result.longValue() : 0L;
    }

    /**
     * Count price modifications at point of sale from the last 24 hours
     * Uses Logs table with transaction_type = 'MODIFICATION_PRIX_PRODUCT_A_LA_VENTE'
     */
    private Long getRecentPriceModificationsCount() {
        LocalDateTime yesterday = LocalDateTime.now().minusHours(24);

        String sql =
            "SELECT COUNT(*) " +
            "FROM logs l " +
            "WHERE l.created_at >= :yesterday " +
            "AND l.transaction_type = 'MODIFICATION_PRIX_PRODUCT_A_LA_VENTE'";

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("yesterday", yesterday);

        Number result = (Number) query.getSingleResult();
        return result != null ? result.longValue() : 0L;
    }
}
