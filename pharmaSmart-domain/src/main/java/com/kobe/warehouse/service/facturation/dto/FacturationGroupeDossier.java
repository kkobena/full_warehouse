package com.kobe.warehouse.service.facturation.dto;

import java.time.LocalDate;

public interface FacturationGroupeDossier extends AbstractReglementFactureDossier {
    String getNumFacture();

    String getOrganismeName();

    int getItemsCount();

    LocalDate getDebutPeriode();

    LocalDate getFinPeriode();

    int getMontantDetailRegle();

    default int getMontantVerse() {
        return getMontantTotal() - getMontantDetailRegle();
    }

    @Override
    default boolean isGroupe() {
        return true;
    }
}
