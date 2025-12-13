package com.kobe.warehouse.service.dto.dashboard;

public record StatistiquesRapidesDTO(
    Integer ventesEnCours,
    Integer clientsServis,
    Integer produitsVendus,
    Integer tempsMoyenVente // in minutes
) {}
