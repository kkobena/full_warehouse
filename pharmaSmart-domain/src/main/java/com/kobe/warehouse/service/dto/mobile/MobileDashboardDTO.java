package com.kobe.warehouse.service.dto.mobile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO for mobile dashboard data - optimized single payload.
 * Provides all dashboard data in a single API call for mobile efficiency.
 */
public record MobileDashboardDTO(
    // CA du jour
    long dailyCA,
    // CA moyen journalier des 30 derniers jours — référence glissante
    long averageCA30j,

    // Nuls quand il n'y a rien à quoi se comparer : une officine à son premier jour n'a pas
    // progressé de cent pour cent, elle n'a pas de veille. L'écran doit pouvoir afficher un tiret
    // plutôt qu'un chiffre qui se lit comme une performance.
    Double variationPercent,
    // Écart % entre le CA du jour et la moyenne des 30 derniers jours
    Double trendVs30j,

    // Statistiques des ventes
    int transactionsCount,
    long averageBasket,
    int customersCount,

    // Montants
    long amountCollected,
    long amountCredit,
    long marginAmount,
    double marginPercent,

    // Alertes
    List<MobileAlertDTO> alerts,
    int alertsCount,

    // Top produits
    List<TopProductDTO> topProducts,

    // Evolution CA (7 derniers jours)
    List<DailyCASummaryDTO> caTrend,

    // Metadata
    LocalDate date,
    LocalDateTime lastUpdate
) {
    /**
     * Alert summary for mobile dashboard.
     */
    public record MobileAlertDTO(
        String type,          // STOCK_RUPTURE, EXPIRY, CASH_DISCREPANCY, INVOICE_OVERDUE
        String severity,      // CRITICAL, WARNING, INFO
        String message,
        int count,
        String icon,
        String color
    ) {}

    /**
     * Top selling product summary.
     */
    public record TopProductDTO(
        Long id,
        String name,
        String codeCip,
        long salesAmount,
        int quantitySold,
        int rank
    ) {}

    /**
     * Daily CA summary for trend chart.
     */
    public record DailyCASummaryDTO(
        LocalDate date,
        String dayLabel,      // "Lun", "Mar", etc.
        long caTotal,
        int transactionsCount
    ) {}
}
