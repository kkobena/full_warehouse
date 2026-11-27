package com.kobe.warehouse.service.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record HistoriqueProduitVenteSummary(
        int montantHt, int quantite, int montantAchat, int montantTtc, int montantRemise) {
    /** Le HT est calculé sur le net de ligne : la TVA est le net moins le HT, pas le TTC moins le HT. */
    @JsonProperty("montantTva")
    public int montantTva() {
        return montantTtc - montantRemise - montantHt;
    }

    @JsonProperty("montantNet")
    public int montantNet() {
        return montantTtc - montantRemise;
    }
}
