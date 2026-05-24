package com.kobe.warehouse.service.dto.mobile;

/**
 * DTO for category balance in cash balance report.
 * Represents balance for a sales category (VO, VNO, etc.)
 */
public record CategoryBalanceDTO(
    String categoryCode,
    String categoryLabel,
    int count,
    long montantTtc,
    long montantHt,
    long montantNet,
    long montantRemise,
    long montantTva,
    long montantAchat,
    long montantMarge,
    long panierMoyen,

    // Payment breakdown for this category
    long montantCash,
    long montantCard,
    long montantCheque,
    long montantVirement,
    long montantMobileMoney,
    long montantCredit,
    long montantDiffere,
    long montantTiersPayant
) {

}
