package com.kobe.warehouse.service.dto.projection;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.kobe.warehouse.domain.enumeration.TiersPayantCategorie;

public record ReglementTiersPayants(
    String libelle,
    TiersPayantCategorie type,
    String numFacture,
    Long montantReglement,
    Long montantFacture
) {
    @JsonProperty("montantRestant")
    public Long montantRestant() {
        return montantFacture - montantReglement;
    }
}
