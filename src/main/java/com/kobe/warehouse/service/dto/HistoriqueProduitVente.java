package com.kobe.warehouse.service.dto;

import java.time.LocalDateTime;

public interface HistoriqueProduitVente {
    LocalDateTime getMvtDate();

    String getReference();

    int getQuantite();

    int getPrixUnitaire();

    int getMontantNet();

    int getMontantRemise();

    int getMontantTtc();

    int getMontantTva();

    int getMontantHt();

    default String getUser() {
        return getFirstName() + " " + getLastName();
    }

    String getFirstName();

    String getLastName();
}
