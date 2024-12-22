package com.kobe.warehouse.service.facturation.dto;

import java.time.LocalDateTime;

public interface DossierFactureProjection {
    long getId();

    String getName();

    int getMontantTotal();

    int getMontantPaye();

    int getItemCount();

    String getNumFacture();

    int getMontantDetailRegle();

    LocalDateTime getFacturationDate();
}
