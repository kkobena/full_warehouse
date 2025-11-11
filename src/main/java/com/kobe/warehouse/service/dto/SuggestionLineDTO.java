package com.kobe.warehouse.service.dto;

import java.time.LocalDateTime;

public record SuggestionLineDTO(
    Integer id,
    int quantity,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    String fournisseurProduitLibelle,
    String fournisseurProduitCip,
    String fournisseurProduitCodeEan,
    Integer produitId,
    Integer fournisseurProduitId,
    int currentStock,
    EtatProduit etatProduit,
    int prixAchat,
    int prixVente
) {}
