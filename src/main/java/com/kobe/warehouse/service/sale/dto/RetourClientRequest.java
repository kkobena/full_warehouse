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
    List<RetourLineRequest> lines
) {
    public record RetourLineRequest(
        Long salesLineId,
        LocalDate salesLineDate,
        int quantite
    ) {}
}
