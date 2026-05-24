package com.kobe.warehouse.service.dto.mobile;

import java.time.LocalDate;

/**
 * DTO for TVA breakdown by rate.
 */
public record TvaRateBreakdownDTO(
    int codeTva,           // TVA rate code (0, 5, 10, 18, 20, etc.)
    String rateName,       // Display name e.g., "0%", "18%", "20%"
    long montantHt,
    long montantTva,
    long montantTtc,
    long montantAchat,
    long amountToBeTakenIntoAccount,
    LocalDate date         // Only set when grouping by date
) {
    public static String getRateName(int codeTva) {
        return codeTva + "%";
    }

    public double getPercent(long totalTtc) {
        if (totalTtc == 0) return 0.0;
        return Math.round((montantTtc * 100.0 / totalTtc) * 10.0) / 10.0;
    }
}
