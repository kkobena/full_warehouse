package com.kobe.warehouse.service.dto.report;

public record GenericsSubstitutionDTO(
    long totalProduits,
    long produitsGeneriques,
    long princepsAvecGenerique,
    long caTotal,
    long caGeneriques,
    long caPrincepsAvecGenerique
) {
    public double tauxGeneriques() {
        return totalProduits > 0 ? (produitsGeneriques * 100.0) / totalProduits : 0.0;
    }

    public double tauxPrincepsSubstituables() {
        return totalProduits > 0 ? (princepsAvecGenerique * 100.0) / totalProduits : 0.0;
    }

    public double tauxCaGeneriques() {
        return caTotal > 0 ? (caGeneriques * 100.0) / caTotal : 0.0;
    }
}
