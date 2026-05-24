package com.kobe.warehouse.service.dto;

import java.time.LocalDateTime;

public interface HistoriqueProduitAchats {
    LocalDateTime getMvtDate();

    String getReference();

    int getQuantite();

    int getPrixAchat();

    default String getUser() {
        return getFirstName() + " " + getLastName();
    }

    String getFirstName();

    String getLastName();

    default int getMontantAchat() {
        return getPrixAchat() * getQuantite();
    }
}
