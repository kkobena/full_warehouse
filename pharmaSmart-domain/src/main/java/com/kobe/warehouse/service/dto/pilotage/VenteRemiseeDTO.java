package com.kobe.warehouse.service.dto.pilotage;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDate;

/** Une vente remisée ; vendeur et autorisant restent vides sans le droit « Clients & équipe ». */
@JsonInclude(JsonInclude.Include.ALWAYS)
public record VenteRemiseeDTO(Long id, LocalDate saleDate, String numero, String vendeur, long montant, long remise, Double taux, String autorisePar) {
    public VenteRemiseeDTO masquerPersonnes() {
        return new VenteRemiseeDTO(id, saleDate, numero, null, montant, remise, taux, null);
    }
}
