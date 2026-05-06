package com.kobe.warehouse.service.dto;

public record LigneFournisseurAPDTO(
    Integer commandeId,
    String numBon,
    String dateCommande,
    String dateEcheance,
    long montant,
    long montantRegle,
    long restantDu,
    String statut
) {}
