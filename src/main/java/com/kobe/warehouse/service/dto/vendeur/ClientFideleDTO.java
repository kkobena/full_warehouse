package com.kobe.warehouse.service.dto.vendeur;

import java.io.Serializable;
import java.time.LocalDate;

public record ClientFideleDTO(
    Long clientId,
    String nom,
    String categorie,
    double montantTotal,
    int nombreVisites,
    LocalDate derniereVisite
) implements Serializable {}
