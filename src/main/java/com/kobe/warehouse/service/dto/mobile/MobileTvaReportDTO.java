package com.kobe.warehouse.service.dto.mobile;

import java.time.LocalDate;
import java.util.List;

/**
 * DTO for mobile TVA report.
 * Provides VAT breakdown by rate with totals.
 */
public record MobileTvaReportDTO(
    LocalDate fromDate,
    LocalDate toDate,
    String periodLabel,

    // Totaux
    long montantHt,
    long montantTva,
    long montantTtc,
    long montantNet,
    long montantRemise,
    long montantAchat,

    // UG (Unites Gratuites) if applicable
    long montantTvaUg,
    long montantTtcUg,
    long montantRemiseUg,

    // Amount for accounting
    long amountToBeTakenIntoAccount,

    // Breakdown by TVA rate
    List<TvaRateBreakdownDTO> tvaBreakdown,

    // Chart data
    List<TvaChartDataDTO> chartData
) {
    public static MobileTvaReportDTO empty(LocalDate fromDate, LocalDate toDate, String periodLabel) {
        return new MobileTvaReportDTO(
            fromDate, toDate, periodLabel,
            0, 0, 0, 0, 0, 0,
            0, 0, 0,
            0,
            List.of(), List.of()
        );
    }

    public boolean isEmpty() {
        return montantTtc == 0 && tvaBreakdown.isEmpty();
    }
}
