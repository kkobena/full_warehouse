package com.kobe.warehouse.service.dashboard.mapper;

import com.kobe.warehouse.domain.enumeration.StockAlertType;
import com.kobe.warehouse.service.dto.dashboard.AnalyseABCDTO;
import com.kobe.warehouse.service.dto.dashboard.PerformanceFournisseurDTO;
import com.kobe.warehouse.service.dto.dashboard.StockAlertsDTO;
import com.kobe.warehouse.service.dto.report.ABCParetoSummaryDTO;
import com.kobe.warehouse.service.dto.report.StockAlertDTO;
import com.kobe.warehouse.service.dto.report.SupplierPerformanceDTO;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Mapper pour convertir les DTOs des services report vers les DTOs des dashboards
 */
@Component
public class DashboardDTOMapper {

    /**
     * Convertit un Map de compteurs d'alertes vers StockAlertsDTO
     *
     * @param alertsByType List d'alertes groupées par type
     * @return StockAlertsDTO pour le dashboard
     */
    public StockAlertsDTO toStockAlertsDTO(List<StockAlertDTO> alertsByType) {
        long rupture = alertsByType.stream()
            .filter(alert -> alert.alertType() == StockAlertType.RUPTURE)
            .count();

        long alerte = alertsByType.stream()
            .filter(alert -> alert.alertType() == StockAlertType.ALERTE)
            .count();

        long peremption = alertsByType.stream()
            .filter(alert -> alert.alertType() == StockAlertType.PEREMPTION)
            .count();

        // Pour bientôt en rupture, on compte les produits avec stock < seuil * 1.5
        // Cette info n'est pas dans StockAlertDTO, on la laisse à 0 pour l'instant
        // ou on peut la calculer séparément
        Integer bientotEnRupture = 0;

        return new StockAlertsDTO(
            (int) rupture,
            (int) alerte,
            bientotEnRupture,
            (int) peremption  // réassort stock rayon (approximation avec péremption)
        );
    }

    /**
     * Convertit ABCParetoSummaryDTO vers AnalyseABCDTO
     *
     * @param summary ABCParetoSummaryDTO du service report
     * @return AnalyseABCDTO pour le dashboard
     */
    public AnalyseABCDTO toAnalyseABCDTO(ABCParetoSummaryDTO summary) {
        if (summary == null) {
            return null;
        }

        return new AnalyseABCDTO(
            new AnalyseABCDTO.ClasseABCItem(
                summary.nbProduitsA(),
                summary.pctCaClasseA() != null ? summary.pctCaClasseA().doubleValue() : 0.0,
                80.0, // Pourcentage CA classe A (fixe selon Pareto)
                summary.caClasseA()
            ),
            new AnalyseABCDTO.ClasseABCItem(
                summary.nbProduitsB(),
                summary.pctCaClasseB() != null ? summary.pctCaClasseB().doubleValue() : 0.0,
                15.0, // Pourcentage CA classe B
                summary.caClasseB()
            ),
            new AnalyseABCDTO.ClasseABCItem(
                summary.nbProduitsC(),
                summary.pctCaClasseC() != null ? summary.pctCaClasseC().doubleValue() : 0.0,
                5.0, // Pourcentage CA classe C
                summary.caClasseC()
            )
        );
    }

    /**
     * Convertit SupplierPerformanceDTO vers PerformanceFournisseurDTO
     *
     * @param supplierPerf SupplierPerformanceDTO du service report
     * @return PerformanceFournisseurDTO pour le dashboard
     */
    public PerformanceFournisseurDTO toPerformanceFournisseurDTO(SupplierPerformanceDTO supplierPerf) {
        if (supplierPerf == null) {
            return null;
        }

        // Calculer une note sur 5 basée sur le score de performance
        int note = calculateRating(supplierPerf.performanceScore());

        return new PerformanceFournisseurDTO(
            supplierPerf.fournisseurId().longValue(),
            supplierPerf.fournisseurName(),
            supplierPerf.nbOrdersLast12Months(),
            supplierPerf.avgDeliveryDays() != null ? supplierPerf.avgDeliveryDays().doubleValue() : 0.0,
            supplierPerf.conformityRatePct() != null ? supplierPerf.conformityRatePct().doubleValue() : 0.0,
            supplierPerf.purchaseAmountLast12Months(),
            note
        );
    }

    /**
     * Convertit une liste de SupplierPerformanceDTO vers une liste de PerformanceFournisseurDTO
     */
    public List<PerformanceFournisseurDTO> toPerformanceFournisseurDTOList(List<SupplierPerformanceDTO> suppliers) {
        return suppliers.stream()
            .map(this::toPerformanceFournisseurDTO)
            .toList();
    }


    /**
     * Calcule une note sur 5 basée sur un score de performance
     *
     * @param performanceScore Score de performance (0-100)
     * @return Note de 1 à 5
     */
    private int calculateRating(java.math.BigDecimal performanceScore) {
        if (performanceScore == null) {
            return 3; // Note moyenne par défaut
        }

        double score = performanceScore.doubleValue();
        if (score >= 90) return 5;
        if (score >= 75) return 4;
        if (score >= 60) return 3;
        if (score >= 40) return 2;
        return 1;
    }
}
