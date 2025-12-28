package com.kobe.warehouse.service.dto.mobile;

/**
 * DTO for supplier purchase data in mobile reports.
 * Represents a supplier's purchase totals for a given period.
 */
public record FournisseurAchatMobileDTO(
    int id,
    String libelle,
    long montantNet,
    long montantTtc,
    long montantHt,
    long montantTaxe,
    long montantRemise,
    double percentTotal
) {
    /**
     * Creates a new DTO with calculated percentage.
     */
    public static FournisseurAchatMobileDTO of(
        int id,
        String libelle,
        long montantNet,
        long montantTtc,
        long montantHt,
        long montantTaxe,
        long montantRemise,
        long totalAchats
    ) {
        double percent = totalAchats > 0 ? (montantNet * 100.0) / totalAchats : 0.0;
        return new FournisseurAchatMobileDTO(
            id,
            libelle,
            montantNet,
            montantTtc,
            montantHt,
            montantTaxe,
            montantRemise,
            Math.round(percent * 100.0) / 100.0
        );
    }
}
