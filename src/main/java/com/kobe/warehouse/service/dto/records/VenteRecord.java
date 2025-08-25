package com.kobe.warehouse.service.dto.records;

import static java.util.Objects.nonNull;

import com.kobe.warehouse.domain.enumeration.SalesStatut;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

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

    public Integer taxAmount() {
        return 0;
    }

    public Integer htAmountUg() {
        return 0;
    }
    public BigDecimal htAmount() {
        if (nonNull(montantHt)) return BigDecimal.valueOf(montantHt).setScale(0, RoundingMode.HALF_UP);
        return BigDecimal.ZERO;
    }
    public BigDecimal panierMoyen() {
        return htAmount().divide(BigDecimal.valueOf(Objects.requireNonNullElse(saleCount, 1L)), 0, RoundingMode.HALF_UP);
    }

    public Integer discountAmountHorsUg() {
        return 0;
    }

    public Integer discountAmountUg() {
        return 0;
    }

    public Integer netUgAmount() {
        return 0;
    }

    public Integer margeUg() {
        return 0;
    }

    public Integer montantttcUg() {
        return 0;
    }

    public Integer montantnetUg() {
        return 0;
    }
}
