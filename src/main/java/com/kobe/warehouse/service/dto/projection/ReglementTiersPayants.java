package com.kobe.warehouse.service.dto.projection;

public interface ReglementTiersPayants {
    String getLibelle();

    String getType();

    String getFactureNumber();

    Integer getMontantFacture();

    Integer getMontantReglement();

    default Integer getMontantRestant() {
        return getMontantFacture() - getMontantReglement();
    }
}
