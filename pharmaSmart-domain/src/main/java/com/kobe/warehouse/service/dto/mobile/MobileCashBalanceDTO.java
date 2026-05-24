package com.kobe.warehouse.service.dto.mobile;

import java.time.LocalDate;
import java.util.List;

/**
 * DTO for mobile cash balance report (Balance Caisse).
 * Provides comprehensive breakdown by payment mode and sales category.
 */
public record MobileCashBalanceDTO(
    LocalDate fromDate,
    LocalDate toDate,
    String periodLabel,

    // Totaux
    int transactionsCount,
    long montantTtc,
    long montantHt,
    long montantNet,
    long montantRemise,
    long montantTva,
    long panierMoyen,

    // Par mode de paiement
    long montantEspeces,
    long montantCartes,
    long montantCheques,
    long montantVirements,
    long montantMobileMoney,
    long montantCredit,
    long montantDiffere,
    long montantTiersPayant,

    // Métriques
    long montantAchats,
    long montantMarge,
    double ratioVenteAchat,
    double ratioAchatVente,

    // Répartition par mode (pour graphique)
    List<PaymentModeBreakdownDTO> paymentBreakdown,

    // Balance par catégorie de vente
    List<CategoryBalanceDTO> categoryBalances,

    // Mouvements de caisse
    List<CashMovementDTO> cashMovements
) {
    public static MobileCashBalanceDTO empty(LocalDate fromDate, LocalDate toDate, String periodLabel) {
        return new MobileCashBalanceDTO(
            fromDate, toDate, periodLabel,
            0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0.0, 0.0,
            List.of(), List.of(), List.of()
        );
    }

    public boolean isEmpty() {
        return transactionsCount == 0 && montantTtc == 0;
    }
}
