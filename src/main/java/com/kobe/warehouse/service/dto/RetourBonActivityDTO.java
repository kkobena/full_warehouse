package com.kobe.warehouse.service.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class RetourBonActivityDTO extends AbstractProduitActivity {

    public RetourBonActivityDTO(LocalDate dateMvt, Integer qtyMvt, LocalDateTime min, LocalDateTime max) {
        super(dateMvt, qtyMvt, min, max);
    }
}
