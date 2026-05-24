package com.kobe.warehouse.service.dto.report;

public record TopRemiseProduitDTO(
    String libelle,
    long montantRemise,
    int nbVentes
) {}
