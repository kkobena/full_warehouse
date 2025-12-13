package com.kobe.warehouse.service.dashboard.mapper;

import com.kobe.warehouse.service.dto.dashboard.*;
import com.kobe.warehouse.service.dto.report.*;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

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
            .filter(alert -> alert.alertType() == StockAlertDTO.StockAlertType.RUPTURE)
            .count();

        long alerte = alertsByType.stream()
            .filter(alert -> alert.alertType() == StockAlertDTO.StockAlertType.ALERTE)
            .count();

        long peremption = alertsByType.stream()
            .filter(alert -> alert.alertType() == StockAlertDTO.StockAlertType.PEREMPTION)
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
     * Convertit DailyCashRegisterReportDTO vers VentesJourDTO
     *
     * @param cashRegisterReports Rapports de caisse du jour
     * @return VentesJourDTO pour le dashboard caissier
     */
    public VentesJourDTO toVentesJourDTO(List<DailyCashRegisterReportDTO> cashRegisterReports) {
        if (cashRegisterReports == null || cashRegisterReports.isEmpty()) {
            return new VentesJourDTO(0L, 0, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0.0);
        }

        long montantTotal = 0L;
        int nombreVentes = 0;
        long montantEspeces = 0L;
        long montantCB = 0L;
        long montantCheque = 0L;
        long montantMobileMoney = 0L;
        long montantVirement = 0L;
        long montantAssurance = 0L;

        for (DailyCashRegisterReportDTO report : cashRegisterReports) {
            montantTotal += report.totalSales() != null ? report.totalSales() : 0;
            nombreVentes += report.numberOfTransactions() != null ? report.numberOfTransactions() : 0;

            if (report.paymentModeBreakdowns() != null) {
                for (DailyCashRegisterReportDTO.PaymentModeBreakdown breakdown : report.paymentModeBreakdowns()) {
                    long amount = breakdown.amount() != null ? breakdown.amount() : 0L;

                    switch (breakdown.modePaiement().toUpperCase()) {
                        case "CASH" -> montantEspeces += amount;
                        case "CB", "CARD" -> montantCB += amount;
                        case "CHECK", "CHEQUE" -> montantCheque += amount;
                        case "MOBILE_PAYMENT", "MOBILE" -> montantMobileMoney += amount;
                        case "VIREMENT", "TRANSFER" -> montantVirement += amount;
                        case "INSURANCE", "ASSURANCE" -> montantAssurance += amount;
                    }
                }
            }
        }

        long ticketMoyen = nombreVentes > 0 ? montantTotal / nombreVentes : 0L;

        // Objectif jour (à configurer ou récupérer d'une autre source)
        Long objectifJour = 1000000L; // 1 million XOF par défaut
        Double tauxAtteinte = objectifJour > 0 ? (montantTotal * 100.0 / objectifJour) : 0.0;

        return new VentesJourDTO(
            montantTotal,
            nombreVentes,
            montantEspeces,
            montantCB,
            montantCheque,
            montantMobileMoney,
            montantVirement,
            montantAssurance,
            ticketMoyen,
            objectifJour,
            tauxAtteinte
        );
    }

    /**
     * Convertit DailyCashRegisterReportDTO vers CaisseStatusDTO
     *
     * @param cashRegisterReports Rapports de caisse du jour
     * @return CaisseStatusDTO pour le dashboard caissier
     */
    public CaisseStatusDTO toCaisseStatusDTO(List<DailyCashRegisterReportDTO> cashRegisterReports) {
        if (cashRegisterReports == null || cashRegisterReports.isEmpty()) {
            return new CaisseStatusDTO(0L, 0L, 0L, 0L, null, "FERMEE");
        }

        // Prendre le premier rapport (ou le plus récent)
        DailyCashRegisterReportDTO report = cashRegisterReports.get(0);

        return new CaisseStatusDTO(
            report.openingBalance() != null ? report.openingBalance().longValue() : 0L,
            report.closingBalance() != null ? report.closingBalance().longValue() : 0L,
            report.expectedBalance() != null ? report.expectedBalance().longValue() : 0L,
            report.discrepancy() != null ? report.discrepancy().longValue() : 0L,
            report.closingDate(),
            report.isClosed() ? "FERMEE" : "OUVERTE"
        );
    }

    /**
     * Convertit TopProductDTO vers TopProduitDTO (dashboard caissier)
     *
     * @param topProduct TopProductDTO du service report
     * @return TopProduitDTO pour le dashboard
     */
    public TopProduitDTO toTopProduitDTO(TopProductDTO topProduct) {
        if (topProduct == null) {
            return null;
        }

        return new TopProduitDTO(
            topProduct.produitId().longValue(),
            topProduct.libelle(),
            topProduct.codeCip(),
            topProduct.qteVendue(),
            topProduct.caGenere().longValue(),
            topProduct.nbVentes().intValue()
        );
    }

    /**
     * Convertit une liste de TopProductDTO vers une liste de TopProduitDTO
     */
    public List<TopProduitDTO> toTopProduitDTOList(List<TopProductDTO> topProducts) {
        return topProducts.stream()
            .map(this::toTopProduitDTO)
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
