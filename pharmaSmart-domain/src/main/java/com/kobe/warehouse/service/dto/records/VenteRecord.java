package com.kobe.warehouse.service.dto.records;

import static java.util.Objects.nonNull;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.kobe.warehouse.domain.enumeration.SalesStatut;
import java.math.BigDecimal;
import java.math.RoundingMode;

public record VenteRecord(
    Integer salesAmount,
    Integer amountToBePaid,
    Integer discountAmount,
    Integer costAmount,
    Integer amountToBeTakenIntoAccount,
    Integer netAmount,
    Double montantHt,
    Integer partAssure,
    Integer partTiersPayant,
    Integer restToPay,
    Integer payrollAmount,
    Integer paidAmount,
    Integer realNetAmount,
    Integer montantttcUg,
    Long saleCount,
    String type,
    SalesStatut statut,
    String group
) {
    public Integer marge() {
        return 0;
    }

    @JsonProperty("taxAmount")
    public Integer taxAmount() {
        if (nonNull(montantHt) && nonNull(salesAmount)) {
            return BigDecimal.valueOf(salesAmount).subtract(BigDecimal.valueOf(montantHt)).setScale(0, RoundingMode.HALF_UP).intValue();
        }
        return 0;
    }

    @JsonProperty("htAmount")
    public BigDecimal htAmount() {
        if (nonNull(montantHt)) return BigDecimal.valueOf(montantHt).setScale(0, RoundingMode.HALF_UP);
        return BigDecimal.ZERO;
    }

    @JsonProperty("panierMoyen")
    public BigDecimal panierMoyen() {
        if (nonNull(montantHt) && nonNull(saleCount) && saleCount > 0) {
            return BigDecimal.valueOf(montantHt).divide(BigDecimal.valueOf(saleCount), 0, RoundingMode.HALF_UP);
        }
        return BigDecimal.ZERO;
    }
}
