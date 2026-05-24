package com.kobe.warehouse.service.dto;

import com.kobe.warehouse.domain.enumeration.AjustType;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class AjustementActivityDTO extends AbstractProduitActivity {

    private final AjustType ajustType;

    public AjustementActivityDTO(LocalDate dateMvt, Integer qtyMvt, AjustType ajustType, LocalDateTime min, LocalDateTime max) {
        super(dateMvt, qtyMvt, min, max);
        this.ajustType = ajustType;
    }

    public AjustType getAjustType() {
        return ajustType;
    }
}
