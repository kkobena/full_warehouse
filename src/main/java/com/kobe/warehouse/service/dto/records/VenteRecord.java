package com.kobe.warehouse.service.dto.records;

import com.kobe.warehouse.domain.enumeration.SalesStatut;


public record VenteRecord(
    Integer salesAmount,
    Integer amountToBePaid,
    Integer discountAmount,
    Integer costAmount,
    Integer amountToBeTakenIntoAccount,
    Integer netAmount,
    Integer htAmount,
    Integer partAssure,
    Integer partTiersPayant,
    Integer restToPay,
    Integer payrollAmount,
    Integer paidAmount,
    Integer realNetAmount,
    Integer montantttcUg,
    Long saleCount,
    Long panierMoyen,
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
