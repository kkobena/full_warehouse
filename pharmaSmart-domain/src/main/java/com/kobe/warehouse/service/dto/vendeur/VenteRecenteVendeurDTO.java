package com.kobe.warehouse.service.dto.vendeur;

import java.io.Serializable;
import java.time.LocalDateTime;

public record VenteRecenteVendeurDTO(
    Long saleId,
    LocalDateTime dateVente,
    String clientNom,
    double montant,
    String typeVente
) implements Serializable {}
