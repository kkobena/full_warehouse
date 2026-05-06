package com.kobe.warehouse.service.dto;

public record CompteFournisseurAPDTO(
    Integer fournisseurId,
    String fournisseurName,
    String fournisseurCode,
    String phone,
    long totalCommande,
    long totalRegle,
    long solde,
    long nbCommandesEnAttente,
    String prochaineEcheance,
    String statut
) {}
