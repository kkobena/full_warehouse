package com.kobe.warehouse.service.dto;

import java.time.Year;

public interface HistoriqueProduitAchatMensuelle {
    Year getAnnee();

    Integer getMois();

    int getQuantite();
}
