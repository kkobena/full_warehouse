package com.kobe.warehouse.service.dto.mobile;

/**
 * DTO for revenue summary (Chiffre d'Affaires) in mobile activity report.
 *
 * @param montantTtc        Total TTC amount
 * @param montantTva        VAT amount
 * @param montantHt         HT amount (before VAT)
 * @param montantRemise     Discount amount
 * @param montantNet        Net amount
 * @param montantEspece     Cash amount
 * @param montantAutreMode  Other payment modes amount
 * @param montantCredit     Credit sales amount
 * @param montantRegle      Settled amount
 * @param marge             Margin amount
 * @param margePercent      Margin percentage
 */
public record ChiffreAffaireMobileDTO(
    long montantTtc,
    long montantTva,
    long montantHt,
    long montantRemise,
    long montantNet,
    long montantEspece,
    long montantAutreMode,
    long montantCredit,
    long montantRegle,
    long marge,
    double margePercent
) {
    public static ChiffreAffaireMobileDTO empty() {
        return new ChiffreAffaireMobileDTO(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0.0);
    }
}
