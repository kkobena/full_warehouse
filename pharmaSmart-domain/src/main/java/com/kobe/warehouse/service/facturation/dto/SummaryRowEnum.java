package com.kobe.warehouse.service.facturation.dto;

public enum SummaryRowEnum {
    REMISE_FORFAITAIRE("Remise forfaitaire"),
    TOTAL_GENERAL("Total général"),
    ARRETE_A_PAYER("ARRETE LA PRESENTE FACTURE A LA SOMME DE (en lettres) :");

    private final String description;

    SummaryRowEnum(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
