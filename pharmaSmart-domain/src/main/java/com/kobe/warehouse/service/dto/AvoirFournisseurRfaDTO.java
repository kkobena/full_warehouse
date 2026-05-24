package com.kobe.warehouse.service.dto;

public record AvoirFournisseurRfaDTO(
    Integer id,
    String fournisseurName,
    String numAvoir,
    String dateAvoir,
    long montant,
    String statut
) {}
