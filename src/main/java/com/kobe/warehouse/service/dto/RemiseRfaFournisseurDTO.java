package com.kobe.warehouse.service.dto;

public record RemiseRfaFournisseurDTO(
    Integer fournisseurId,
    String fournisseurName,
    Long palierRfa,
    long caCommandeN,
    double pourcentageAtteint,
    long rfaEstimee,
    long rfaRecue,
    String alerte
) {}
