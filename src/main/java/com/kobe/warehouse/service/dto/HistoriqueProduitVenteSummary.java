package com.kobe.warehouse.service.dto;

public interface HistoriqueProduitVenteSummary {
    int getQuantite();

    int getMontantNet();

    int getMontantRemise();

    int getMontantTtc();

    int getMontantTva();

    int getMontantHt();
}
