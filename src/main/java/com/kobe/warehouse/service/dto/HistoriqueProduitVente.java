package com.kobe.warehouse.service.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;

public record HistoriqueProduitVente(LocalDateTime mvtDate, String reference, int quantite, int prixUnitaire,
                                     int montantNet, int montantRemise, int montantTtc, int montantTva,
                                     int montantHt, String firstName, String lastName) {
    @JsonProperty("user")
    public String getUser() {
        return firstName + " " + lastName;
    }


}
