package com.kobe.warehouse.service.dto.records;

import com.kobe.warehouse.domain.enumeration.SalesStatut;

import java.lang.Integer;


public record VenteRecord(
    Integer salesAmount,
    Integer amountToBePaid,
    Integer discountAmount,
    Integer costAmount,
    Integer marge,
    Integer amountToBeTakenIntoAccount,
    Integer netAmount,
    Integer htAmount,
    Integer partAssure,
    Integer partTiersPayant,
    Integer taxAmount,
    Integer restToPay,
    Integer htAmountUg,
    Integer discountAmountHorsUg,
    Integer discountAmountUg,
    Integer netUgAmount,
    Integer margeUg,
    Integer montantttcUg,
    Integer payrollAmount,
    Integer montantTvaUg,
    Integer montantnetUg,
    Integer paidAmount,
    Integer realNetAmount,
    Long saleCount,
    Long panierMoyen,
    String type,
    SalesStatut statut,
    String group
) {}
