package com.kobe.warehouse.service.dto;

import java.time.Month;
import java.time.Year;

public interface HistoriqueProduitAchatMensuelle {
    Year getAnnee();

    Integer getMois();

    int getQuantite();

    default Month getMonth() {
        if (getMois() == null) {
            return null;
        }
        return Month.of(getMois());
    }
}
