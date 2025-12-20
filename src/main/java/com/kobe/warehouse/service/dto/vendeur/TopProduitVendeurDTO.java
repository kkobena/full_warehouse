package com.kobe.warehouse.service.dto.vendeur;

import java.io.Serializable;

public record TopProduitVendeurDTO(
    Long produitId,
    String produitLibelle,
    String codeCip,
    int quantiteVendue,
    double montantTotal,
    double marge
) implements Serializable {}
