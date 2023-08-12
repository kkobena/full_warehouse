package com.kobe.warehouse.service.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Getter;

@Getter
public abstract class AbstractProduitActivity {
    private final LocalDate dateMvt;
    private final Integer qtyMvt;
    private final LocalDateTime min;
    private final LocalDateTime max;
    public AbstractProduitActivity(LocalDate dateMvt, Integer qtyMvt, LocalDateTime min,
        LocalDateTime max) {
        this.dateMvt = dateMvt;
        this.qtyMvt = qtyMvt;
        this.min = min;
        this.max = max;
    }
}
