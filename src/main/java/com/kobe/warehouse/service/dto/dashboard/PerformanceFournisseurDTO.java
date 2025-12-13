package com.kobe.warehouse.service.dto.dashboard;

public record PerformanceFournisseurDTO(
    Long fournisseurId,
    String fournisseurName,
    Integer nombreCommandes,
    Double delaiMoyenJours,
    Double tauxConformite,
    Long caAnnuel,
    Integer note  // 1-5 étoiles
) {
}
