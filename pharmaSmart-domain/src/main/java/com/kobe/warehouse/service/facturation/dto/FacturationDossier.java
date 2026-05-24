package com.kobe.warehouse.service.facturation.dto;

import java.time.LocalDateTime;

public interface FacturationDossier extends AbstractReglementFactureDossier {
    LocalDateTime getFacturationDate();

    LocalDateTime getSaleDate();

    String getMatricule();

    String getCustomerFullName();

    String getBonNumber();

    @Override
    default boolean isGroupe() {
        return false;
    }
}
