package com.kobe.warehouse.service.dto.mobile;

/**
 * DTO for supplier group purchases in mobile activity report.
 *
 * @param libelle      Supplier group name
 * @param montantTtc   Total TTC amount
 * @param montantTva   VAT amount
 * @param montantHt    HT amount (before VAT)
 * @param percentTotal Percentage of total purchases
 */
public record GroupeFournisseurAchatMobileDTO(
    String libelle,
    long montantTtc,
    long montantTva,
    long montantHt,
    double percentTotal
) {}
