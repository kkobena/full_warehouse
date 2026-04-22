package com.kobe.warehouse.service.dto;

import java.util.List;

public record AvoirFournisseurCommand(
    Integer retourBonId,
    String commentaire,
    List<AvoirLigneCommand> lignes
) {
    public record AvoirLigneCommand(
        Integer retourBonItemId,
        Integer qtyAcceptee,
        Integer prixAchat      // nullable — defaults to RetourBonItem.prixAchat
    ) {}
}
