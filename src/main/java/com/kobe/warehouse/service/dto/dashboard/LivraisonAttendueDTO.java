package com.kobe.warehouse.service.dto.dashboard;


public record LivraisonAttendueDTO(
    Integer commandeId,
    String fournisseurNom,
    Integer nombreReferences
) {}

