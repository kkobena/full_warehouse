package com.kobe.warehouse.service.dto;

import java.time.Month;

public interface HistoriqueProduitVenteMensuelle {
    Integer getAnnee();

    Integer getMois();

    int getQuantite();

    default Month getMonth() {
        if (getMois() == null) {
            return null;
        }
        return Month.of(getMois());
    }
}
