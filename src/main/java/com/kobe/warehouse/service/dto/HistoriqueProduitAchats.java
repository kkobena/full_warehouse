package com.kobe.warehouse.service.dto;

import java.time.LocalDateTime;

public interface HistoriqueProduitAchats {
    LocalDateTime getMvtDate();

    String getReference();

    int getQuantite();

    int getPrixAchat();

    String getUser();
}
