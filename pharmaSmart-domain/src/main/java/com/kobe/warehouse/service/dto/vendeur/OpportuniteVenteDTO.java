package com.kobe.warehouse.service.dto.vendeur;

import java.io.Serializable;

public record OpportuniteVenteDTO(
    String type,
    String titre,
    String description,
    int nombreClients,
    double potentielCA
) implements Serializable {}
