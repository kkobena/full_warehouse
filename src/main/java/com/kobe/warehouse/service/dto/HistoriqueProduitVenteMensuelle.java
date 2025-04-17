package com.kobe.warehouse.service.dto;

import java.time.Year;

public interface HistoriqueProduitVenteMensuelle {
    Year getAnnee();

    Integer getMois();

    int getQuantite();
}
