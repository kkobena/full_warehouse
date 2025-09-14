package com.kobe.warehouse.service.dto.enumeration;

public enum TypeVenteDTO {
    CashSale("VNO"),
    ThirdPartySales("VO"),
    VenteDepot("VENTES_DEPOTS"),
    VenteDepotAgree("DEPOT_AGREE");

    private final String value;

    TypeVenteDTO(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
