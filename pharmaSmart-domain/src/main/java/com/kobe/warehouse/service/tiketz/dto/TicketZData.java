package com.kobe.warehouse.service.tiketz.dto;

import com.kobe.warehouse.domain.enumeration.ModePaimentCode;
import com.kobe.warehouse.service.utils.NumberUtil;
import jakarta.validation.constraints.NotNull;

public record TicketZData(
    @NotNull String libelle,
    @NotNull Long value,
    @NotNull Long secondValue,
    int sortOrder,
    ModePaimentCode modePaimentCode
) {
    public TicketZData(String libelle, Long value, Long secondValue, int sortOrder) {
        this(libelle, value, secondValue, sortOrder, null);
    }

    public String montant() {
        return NumberUtil.formatToString(value);
    }

    public String montantSansArrondi() {
        return NumberUtil.formatToString(secondValue);
    }
}
