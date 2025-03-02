package com.kobe.warehouse.service.dto;

public enum OrderBy {
    QUANTITY_SOLD("quantity_sold"),
    AMOUNT("sales_amount");

    private final String value;

    OrderBy(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
