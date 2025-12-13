package com.kobe.warehouse.service.dto.dashboard;

public record SuggestionReapproDTO(
    Long produitId,
    String produitLibelle,
    String codeCip,
    Integer stockActuel,
    Integer consommationMoyenne,
    Integer quantiteSuggeree,
    Long fournisseurId,
    String fournisseurName,
    Integer delaiLivraison,
    Long prixUnitaire
) {
}
