package com.kobe.warehouse.service.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record HistoriqueProduitVenteSummary(int montantHt, int quantite,
                                            int montantAchat, int montantTtc, int montantRemise) {
    @JsonProperty("montantTva")
    public int montantTva() {
        return montantTtc - montantHt;
    }

    @JsonProperty("montantNet")
    public int montantNet() {
        return montantTtc - montantRemise;
    }
}
