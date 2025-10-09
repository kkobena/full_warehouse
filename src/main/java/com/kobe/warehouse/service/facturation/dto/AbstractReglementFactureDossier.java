package com.kobe.warehouse.service.facturation.dto;

import java.time.LocalDate;

public interface AbstractReglementFactureDossier {
    long getId();

    int getMontantPaye();

    int getMontantTotal();

    boolean isGroupe();

    long getParentId();

    LocalDate getInvoiceDate();
    LocalDate getParentInvoiceDate();
}
