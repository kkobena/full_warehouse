package com.kobe.warehouse.service.dto.report;

public record DemarqueByMotifDTO(
    String motif,
    int nbLignes,
    long totalQty,
    long valeur
) {}
