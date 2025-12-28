package com.kobe.warehouse.service.dto.mobile;

import java.util.List;

/**
 * DTO for third-party payer summary in mobile activity report.
 *
 * @param reglements         List of third-party payments received
 * @param totalFacture       Total invoiced amount
 * @param totalRegle         Total settled amount
 * @param totalRestant       Total remaining amount
 * @param achats             List of third-party purchases
 * @param totalBons          Total number of vouchers
 * @param totalMontantAchats Total purchase amount
 * @param totalClients       Total number of clients
 */
public record TiersPayantSummaryMobileDTO(
    // Règlements reçus
    List<ReglementTiersPayantMobileDTO> reglements,
    long totalFacture,
    long totalRegle,
    long totalRestant,

    // Achats/Bons
    List<AchatTiersPayantMobileDTO> achats,
    int totalBons,
    long totalMontantAchats,
    int totalClients
) {
    public static TiersPayantSummaryMobileDTO empty() {
        return new TiersPayantSummaryMobileDTO(
            List.of(), 0, 0, 0,
            List.of(), 0, 0, 0
        );
    }
}
