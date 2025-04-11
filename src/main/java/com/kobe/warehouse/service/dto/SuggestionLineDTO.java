package com.kobe.warehouse.service.dto;

import java.time.LocalDateTime;

public record SuggestionLineDTO(
    long id,
    int quantity,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    String fournisseurProduitLibelle,
    String fournisseurProduitCip,
    String fournisseurProduitCodeEan,
    long produitId,
    long fournisseurProduitId,
    int currentStock,
    EtatProduit etatProduit,
    int prixAchat,
    int prixVente
) {}
