package com.kobe.warehouse.service.pharmaml.dto;

import java.util.List;

/**
 * Requête pour la vérification de disponibilité multi-grossistes.
 */
public record DispoMultiRequestDTO(
    Integer commandeId,
    String orderDate,
    Integer suggestionId,
    List<Integer> grossisteIds
) {}
