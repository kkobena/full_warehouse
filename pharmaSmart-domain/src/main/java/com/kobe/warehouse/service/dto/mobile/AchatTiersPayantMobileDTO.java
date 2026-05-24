package com.kobe.warehouse.service.dto.mobile;

/**
 * DTO for third-party payer purchase in mobile activity report.
 *
 * @param libelle     Third-party payer name
 * @param categorie   Category (e.g., ASSURANCE, CARNET)
 * @param bonsCount   Number of vouchers
 * @param montant     Total purchase amount
 * @param clientCount Number of clients
 */
public record AchatTiersPayantMobileDTO(
    String libelle,
    String categorie,
    int bonsCount,
    long montant,
    int clientCount
) {}
