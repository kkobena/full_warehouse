package com.kobe.warehouse.service.dto.records;

import static java.util.Objects.nonNull;

import java.math.BigDecimal;
import java.math.RoundingMode;

public record ProductStatRecord(
    Long id,
    Long produitCount,
    String codeCip,
    String codeEan,
    String libelle,
    Integer quantitySold,
    Integer costAmount,
    Integer salesAmount,
    Integer discountAmount,
    Integer amountToBeTakenIntoAaccount,
    Double montantHt,
    Integer netAmount
) {
    public Integer htAmount() {
        return nonNull(montantHt) ? BigDecimal.valueOf(montantHt).setScale(0, RoundingMode.HALF_UP).intValue() : null;
    }

    public Integer taxAmount() {
        Integer ht = htAmount();
        return nonNull(salesAmount) && nonNull(ht) ? salesAmount - ht : null;
    }
}
