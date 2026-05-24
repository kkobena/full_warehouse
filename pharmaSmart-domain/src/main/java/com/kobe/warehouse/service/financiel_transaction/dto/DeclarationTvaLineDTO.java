package com.kobe.warehouse.service.financiel_transaction.dto;

public record DeclarationTvaLineDTO(
    int codeTva,
    String tauxLabel,
    long baseHtVentes,
    long tvaCollectee,
    long baseHtAchats,
    long tvaDeductible
) {}
