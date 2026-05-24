package com.kobe.warehouse.service.dto;

public record BedImportLigneDTO(
    Integer fournisseurProduitId,
    int quantite,
    int prixAchat,
    int prixVente
) {}
