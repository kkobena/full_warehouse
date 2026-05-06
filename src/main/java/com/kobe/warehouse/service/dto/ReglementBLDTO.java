package com.kobe.warehouse.service.dto;

public record ReglementBLDTO(
    Long id,
    String dateReglement,
    long montant,
    String reference,
    String commentaire,
    String operateur
) {}
