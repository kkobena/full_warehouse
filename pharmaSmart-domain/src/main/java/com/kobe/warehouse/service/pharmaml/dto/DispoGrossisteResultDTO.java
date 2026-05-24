package com.kobe.warehouse.service.pharmaml.dto;

import java.util.List;

/**
 * Résultat de disponibilité pour un grossiste donné.
 */
public record DispoGrossisteResultDTO(
    Integer grossisteId,
    String fournisseurLibelle,
    List<InfoProduitDTO> produits
) {}
