package com.kobe.warehouse.service.dto.dashboard;

public record TopProduitDTO(
    Long produitId,
    String produitLibelle,
    String codeCip,
    Integer quantiteVendue,
    Long montantTotal,
    Integer nombreVentes
) {}
