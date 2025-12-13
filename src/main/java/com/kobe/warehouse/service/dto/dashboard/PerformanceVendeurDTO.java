package com.kobe.warehouse.service.dto.dashboard;

public record PerformanceVendeurDTO(
    Long vendeurId,
    String vendeurNom,
    Integer nombreVentes,
    Long montantTotal,
    Long ticketMoyen,
    Double tauxRemise
) {}
