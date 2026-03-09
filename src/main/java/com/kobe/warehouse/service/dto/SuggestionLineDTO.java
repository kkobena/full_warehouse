package com.kobe.warehouse.service.dto;

import com.kobe.warehouse.service.dto.enumeration.Mois;
import java.time.LocalDateTime;
import java.util.Map;

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
    int prixVente,
    Map<Mois, Integer> consommationMensuelle
) {

}
