package com.kobe.warehouse.service.sale.dto;

import com.kobe.warehouse.domain.enumeration.ModeReglementRetour;
import com.kobe.warehouse.domain.enumeration.MotifRetourClient;
import java.time.LocalDate;
import java.util.List;

public record RetourClientRequest(
    Long saleId,
    LocalDate saleDate,
    MotifRetourClient motif,
    ModeReglementRetour modeReglement,
    String commentaire,
    List<RetourLineRequest> lines,
    boolean avecEchange
) {
    public record RetourLineRequest(
        Long salesLineId,
        LocalDate salesLineDate,
        int quantite,
        Boolean emballageIntact,
        Boolean numLotLisible,
        Boolean datePeremptionValide
    ) {
        public boolean etatProduitOk() {
            return (emballageIntact == null || emballageIntact)
                && (numLotLisible == null || numLotLisible)
                && (datePeremptionValide == null || datePeremptionValide);
        }
    }
}
