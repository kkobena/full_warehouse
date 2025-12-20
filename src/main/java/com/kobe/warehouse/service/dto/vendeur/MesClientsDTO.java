package com.kobe.warehouse.service.dto.vendeur;

import java.io.Serializable;

public record MesClientsDTO(
    int clientsServis,
    int nouveauxClients,
    int clientsFideles,
    double tauxFidelisation
) implements Serializable {}
