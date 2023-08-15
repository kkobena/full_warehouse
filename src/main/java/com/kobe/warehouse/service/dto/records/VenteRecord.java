package com.kobe.warehouse.service.dto.records;

import java.math.BigDecimal;
import java.math.BigInteger;

public record VenteRecord(
    BigDecimal salesAmount,
    BigDecimal amountToBePaid,
    BigDecimal discountAmount,
    BigDecimal costAmount,
    BigDecimal marge,
    BigDecimal amountToBeTakenIntoAccount,
    BigDecimal netAmount,
    BigDecimal htAmount,
    BigDecimal partAssure,
    BigDecimal partTiersPayant,
    BigDecimal taxAmount,
    BigDecimal restToPay,
    BigDecimal htAmountUg,
    BigDecimal discountAmountHorsUg,
    BigDecimal discountAmountUg,
    BigDecimal netUgAmount,
    BigDecimal margeUg,
    BigDecimal montantttcUg,
    BigDecimal payrollAmount,
    BigDecimal montantTvaUg,
    BigDecimal montantnetUg,
    BigDecimal paidAmount,
    BigDecimal realNetAmount,
    BigInteger saleCount,
    double panierMoyen) {}
