package com.kobe.warehouse.service.dashboard.impl;

import com.kobe.warehouse.service.dashboard.ResponsableCommandeDashboardService;
import com.kobe.warehouse.service.dashboard.mapper.DashboardDTOMapper;
import com.kobe.warehouse.service.dto.dashboard.*;
import com.kobe.warehouse.service.dto.report.StockAlertDTO;
import com.kobe.warehouse.service.report.ABCParetoReportService;
import com.kobe.warehouse.service.report.StockAlertReportService;
import com.kobe.warehouse.service.report.SupplierPerformanceReportService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class ResponsableCommandeDashboardServiceImpl implements ResponsableCommandeDashboardService {

    @PersistenceContext
    private EntityManager entityManager;

    private final StockAlertReportService stockAlertReportService;
    private final ABCParetoReportService abcParetoReportService;
    private final SupplierPerformanceReportService supplierPerformanceReportService;
    private final DashboardDTOMapper mapper;

    public ResponsableCommandeDashboardServiceImpl(
        StockAlertReportService stockAlertReportService,
        ABCParetoReportService abcParetoReportService,
        SupplierPerformanceReportService supplierPerformanceReportService,
        DashboardDTOMapper mapper
    ) {
        this.stockAlertReportService = stockAlertReportService;
        this.abcParetoReportService = abcParetoReportService;
        this.supplierPerformanceReportService = supplierPerformanceReportService;
        this.mapper = mapper;
    }

    @Override
    public ResponsableCommandeDashboardDTO getDashboardData() {
        return new ResponsableCommandeDashboardDTO(
            getStockAlerts(),
            getCommandesEnCours(),
            getPeremptions(),
            getRotationStock(),
            getSuggestionsReappro(),
            getAnalyseABC(),
            getPerformanceFournisseurs(5)
        );
    }

    @Override
    public StockAlertsDTO getStockAlerts() {
        List<StockAlertDTO> stockAlerts = stockAlertReportService.getStockAlerts(null, Pageable.unpaged()).getContent();
        return mapper.toStockAlertsDTO(stockAlerts);
    }

    @Override
    public CommandesEnCoursDTO getCommandesEnCours() {
        String queryEnAttente = """
            SELECT COUNT(*) FROM commande
            WHERE statut = 'REQUESTED' OR statut = 'PASSED'
        """;

        String queryAReceptionner = """
            SELECT COUNT(*) FROM commande
            WHERE statut = 'IN_PROGRESS'
        """;

        String queryMontantTotal = """
            SELECT COALESCE(SUM(c.order_amount), 0)
            FROM commande c
            WHERE c.statut IN ('REQUESTED', 'PASSED', 'IN_PROGRESS')
        """;

        Integer enAttente = ((Number) entityManager.createNativeQuery(queryEnAttente).getSingleResult()).intValue();
        Integer aReceptionner = ((Number) entityManager.createNativeQuery(queryAReceptionner).getSingleResult()).intValue();
        Long totalMontant = ((Number) entityManager.createNativeQuery(queryMontantTotal).getSingleResult()).longValue();

        return new CommandesEnCoursDTO(enAttente, aReceptionner, totalMontant);
    }

    @Override
    public PeremptionsDTO getPeremptions() {
        LocalDate now = LocalDate.now();
        LocalDate unMoisPlus = now.plusMonths(1);
        LocalDate troisMoisPlus = now.plusMonths(3);
        LocalDate sixMoisPlus = now.plusMonths(6);

        String queryUnMois = """
            SELECT COUNT(DISTINCT p.id)
            FROM produit p
            JOIN stock_produit sp ON sp.produit_id = p.id
            WHERE p.peremption_date IS NOT NULL
            AND p.peremption_date BETWEEN :now AND :unMoisPlus
        """;

        String queryUnATroisMois = """
            SELECT COUNT(DISTINCT p.id)
            FROM produit p
            JOIN stock_produit sp ON sp.produit_id = p.id
            WHERE p.peremption_date IS NOT NULL
            AND p.peremption_date BETWEEN :unMoisPlus AND :troisMoisPlus
        """;

        String queryTroisASixMois = """
            SELECT COUNT(DISTINCT p.id)
            FROM produit p
            JOIN stock_produit sp ON sp.produit_id = p.id
            WHERE p.peremption_date IS NOT NULL
            AND p.peremption_date BETWEEN :troisMoisPlus AND :sixMoisPlus
        """;

        String queryValeur = """
            SELECT COALESCE(SUM((sp.qty_stock + sp.qty_ug) * p.regular_unit_price), 0)
            FROM produit p
            JOIN stock_produit sp ON sp.produit_id = p.id
            WHERE p.peremption_date IS NOT NULL
            AND p.peremption_date <= :sixMoisPlus
        """;

        Integer unMois = ((Number) entityManager.createNativeQuery(queryUnMois)
            .setParameter("now", now)
            .setParameter("unMoisPlus", unMoisPlus)
            .getSingleResult()).intValue();

        Integer unATroisMois = ((Number) entityManager.createNativeQuery(queryUnATroisMois)
            .setParameter("unMoisPlus", unMoisPlus)
            .setParameter("troisMoisPlus", troisMoisPlus)
            .getSingleResult()).intValue();

        Integer troisASixMois = ((Number) entityManager.createNativeQuery(queryTroisASixMois)
            .setParameter("troisMoisPlus", troisMoisPlus)
            .setParameter("sixMoisPlus", sixMoisPlus)
            .getSingleResult()).intValue();

        Long valeurTotale = ((Number) entityManager.createNativeQuery(queryValeur)
            .setParameter("sixMoisPlus", sixMoisPlus)
            .getSingleResult()).longValue();

        return new PeremptionsDTO(unMois, unATroisMois, troisASixMois, valeurTotale);
    }

    @Override
    public RotationStockDTO getRotationStock() {
        // Calcul de la rotation moyenne sur 30 jours
        String queryRotation = """
            SELECT
                COALESCE(
                    ROUND(
                        SUM(sl.quantity_sold) /
                        NULLIF(AVG(sp.qty_stock + sp.qty_ug), 0)
                    , 2)
                , 0) as rotation_moyenne
            FROM sales_line sl
            JOIN stock_produit sp ON sp.produit_id = sl.produit_id
            WHERE sl.created_at >= CURRENT_DATE - INTERVAL '30 days'
        """;

        // Rotation rapide: >= 4 (ventes rapides)
        String queryRapide = """
            SELECT COUNT(*)
            FROM (
                SELECT p.id
                FROM produit p
                JOIN stock_produit sp ON sp.produit_id = p.id
                LEFT JOIN sales_line sl ON sl.produit_id = p.id
                    AND sl.created_at >= CURRENT_DATE - INTERVAL '30 days'
                WHERE (sp.qty_stock + sp.qty_ug) > 0
                GROUP BY p.id, sp.qty_stock, sp.qty_ug
                HAVING COALESCE(SUM(sl.quantity_sold), 0) / NULLIF((sp.qty_stock + sp.qty_ug), 0) >= 4
            ) AS rapide_products
        """;

        // Rotation normale: >= 2 et < 4
        String queryNormal = """
            SELECT COUNT(*)
            FROM (
                SELECT p.id
                FROM produit p
                JOIN stock_produit sp ON sp.produit_id = p.id
                LEFT JOIN sales_line sl ON sl.produit_id = p.id
                    AND sl.created_at >= CURRENT_DATE - INTERVAL '30 days'
                WHERE (sp.qty_stock + sp.qty_ug) > 0
                GROUP BY p.id, sp.qty_stock, sp.qty_ug
                HAVING COALESCE(SUM(sl.quantity_sold), 0) / NULLIF((sp.qty_stock + sp.qty_ug), 0) >= 2
                AND COALESCE(SUM(sl.quantity_sold), 0) / NULLIF((sp.qty_stock + sp.qty_ug), 0) < 4
            ) AS normal_products
        """;

        // Rotation lente: < 2
        String queryLent = """
            SELECT COUNT(*)
            FROM (
                SELECT p.id
                FROM produit p
                JOIN stock_produit sp ON sp.produit_id = p.id
                LEFT JOIN sales_line sl ON sl.produit_id = p.id
                    AND sl.created_at >= CURRENT_DATE - INTERVAL '30 days'
                WHERE (sp.qty_stock + sp.qty_ug) > 0
                GROUP BY p.id, sp.qty_stock, sp.qty_ug
                HAVING COALESCE(SUM(sl.quantity_sold), 0) / NULLIF((sp.qty_stock + sp.qty_ug), 0) < 2
            ) AS lent_products
        """;

        Double rotationMoyenne = ((Number) entityManager.createNativeQuery(queryRotation)
            .getSingleResult()).doubleValue();

        Integer rapide = ((Number) entityManager.createNativeQuery(queryRapide)
            .getSingleResult()).intValue();

        Integer normal = ((Number) entityManager.createNativeQuery(queryNormal)
            .getSingleResult()).intValue();

        Integer lent = ((Number) entityManager.createNativeQuery(queryLent)
            .getSingleResult()).intValue();

        return new RotationStockDTO(rotationMoyenne != null ? rotationMoyenne : 0.0, rapide, normal, lent);
    }

    @Override
    public List<SuggestionReapproDTO> getSuggestionsReappro() {
        String query = """
            SELECT
                p.id as produit_id,
                p.libelle as produit_libelle,
                p.code_cip,
                (sp.qty_stock + sp.qty_ug) as stock_actuel,
                COALESCE(ROUND(SUM(sl.quantity_sold) / 90.0), 0) as consommation_moyenne,
                GREATEST(
                    p.qty_seuil_mini * 2,
                    COALESCE(ROUND(SUM(sl.quantity_sold) / 90.0 * 30), 0)
                ) as quantite_suggeree,
                f.id as fournisseur_id,
                f.libelle as fournisseur_name,
                3 as delai_livraison,
                p.regular_unit_price as prix_unitaire
            FROM produit p
            JOIN stock_produit sp ON sp.produit_id = p.id
            LEFT JOIN sales_line sl ON sl.produit_id = p.id
                AND sl.created_at >= CURRENT_DATE - INTERVAL '90 days'
            LEFT JOIN fournisseur_produit fp ON fp.produit_id = p.id
            LEFT JOIN fournisseur f ON f.id = fp.fournisseur_id
            WHERE (sp.qty_stock + sp.qty_ug) < p.qty_seuil_mini
            GROUP BY p.id, p.libelle, p.code_cip, sp.qty_stock, sp.qty_ug, p.qty_seuil_mini,
                     p.regular_unit_price, f.id, f.libelle
            ORDER BY stock_actuel ASC
            LIMIT 50
        """;

        @SuppressWarnings("unchecked")
        List<Object[]> results = entityManager.createNativeQuery(query).getResultList();

        List<SuggestionReapproDTO> suggestions = new ArrayList<>();
        for (Object[] row : results) {
            suggestions.add(new SuggestionReapproDTO(
                ((Number) row[0]).longValue(),    // produitId
                (String) row[1],                   // produitLibelle
                (String) row[2],                   // codeCip
                ((Number) row[3]).intValue(),      // stockActuel
                ((Number) row[4]).intValue(),      // consommationMoyenne
                ((Number) row[5]).intValue(),      // quantiteSuggeree
                row[6] != null ? ((Number) row[6]).longValue() : null,  // fournisseurId
                (String) row[7],                   // fournisseurName
                ((Number) row[8]).intValue(),      // delaiLivraison
                ((Number) row[9]).longValue()      // prixUnitaire
            ));
        }

        return suggestions;
    }

    @Override
    public AnalyseABCDTO getAnalyseABC() {
        return mapper.toAnalyseABCDTO(abcParetoReportService.getABCParetoSummary());
    }

    @Override
    public List<PerformanceFournisseurDTO> getPerformanceFournisseurs(Integer top) {
        return mapper.toPerformanceFournisseurDTOList(
            supplierPerformanceReportService.getTopSuppliersByVolume(top != null ? top : 5)
        );
    }
}
