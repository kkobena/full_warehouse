package com.kobe.warehouse.service.dto.pilotage;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDate;

/** Une vente et sa marge (HT) ; le vendeur reste vide sans le droit « Clients & équipe ». */
@JsonInclude(JsonInclude.Include.ALWAYS)
public record VenteMargeDTO(Long id, LocalDate saleDate, String numero, String vendeur, Double caHt, Double coutHt, Double marge) {
    public VenteMargeDTO masquerVendeur() {
        return new VenteMargeDTO(id, saleDate, numero, null, caHt, coutHt, marge);
    }
}
