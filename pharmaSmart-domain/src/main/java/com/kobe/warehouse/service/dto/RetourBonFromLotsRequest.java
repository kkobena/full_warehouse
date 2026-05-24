package com.kobe.warehouse.service.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * Requête batch pour créer plusieurs RetourBon depuis une liste de lots périmés.
 * Chaque lot doit avoir son propre motif et quantité.
 *
 */
public class RetourBonFromLotsRequest {

    @NotNull
    @NotEmpty
    @Valid
    private List<RetourBonFromLotRequest> lots;

    public List<RetourBonFromLotRequest> getLots() {
        return lots;
    }

    public RetourBonFromLotsRequest setLots(List<RetourBonFromLotRequest> lots) {
        this.lots = lots;
        return this;
    }
}

