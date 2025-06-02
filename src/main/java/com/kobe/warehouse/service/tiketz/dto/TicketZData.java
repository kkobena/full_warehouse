package com.kobe.warehouse.service.tiketz.dto;

import com.kobe.warehouse.service.utils.NumberUtil;
import jakarta.validation.constraints.NotNull;

public record TicketZData(@NotNull String libelle, @NotNull Long value, @NotNull Long secondValue, int sortOrder) {
    public String montant() {
        return NumberUtil.formatToString(value);
    }

    public String montantSansArrondi() {
        return NumberUtil.formatToString(secondValue);
    }
}
