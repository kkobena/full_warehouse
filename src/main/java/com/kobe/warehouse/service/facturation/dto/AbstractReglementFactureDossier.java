package com.kobe.warehouse.service.facturation.dto;

public interface AbstractReglementFactureDossier {
    long getId();

    int getMontantPaye();

    int getMontantTotal();

    boolean isGroupe();

    long getParentId();
}
