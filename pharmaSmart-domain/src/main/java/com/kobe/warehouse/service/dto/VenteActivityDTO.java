package com.kobe.warehouse.service.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class VenteActivityDTO extends AbstractProduitActivity {

    private final boolean canceled;

    public VenteActivityDTO(LocalDate dateMvt, Integer qtyMvt, boolean canceled, LocalDateTime min, LocalDateTime max) {
        super(dateMvt, qtyMvt, min, max);
        this.canceled = canceled;
    }

    public boolean isCanceled() {
        return canceled;
    }
}
