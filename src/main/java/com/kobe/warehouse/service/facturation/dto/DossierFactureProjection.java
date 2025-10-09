package com.kobe.warehouse.service.facturation.dto;

import com.kobe.warehouse.domain.FactureItemId;
import java.time.LocalDate;
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

    LocalDate getInvoiceDate();

    default FactureItemId getFactureItemId() {
        return new FactureItemId(getId(), getInvoiceDate());
    }
}
