package com.kobe.warehouse.service.dto.mobile;

/**
 * DTO for third-party payment in mobile activity report.
 *
 * @param libelle          Third-party payer name
 * @param categorie        Category (e.g., ASSURANCE, CARNET)
 * @param numFacture       Invoice number
 * @param montantFacture   Invoiced amount
 * @param montantReglement Settled amount
 * @param montantRestant   Remaining amount
 */
public record ReglementTiersPayantMobileDTO(
    String libelle,
    String categorie,
    String numFacture,
    long montantFacture,
    long montantReglement,
    long montantRestant
) {}
