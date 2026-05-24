package com.kobe.warehouse.service.dto;

public enum TemporalEnum {
    DAILY(0),
    MONTHLY(1),
    SEMESTERLY(2),
    YEARLY(4);

    private final int order;

    TemporalEnum(int order) {
        this.order = order;
    }

    public int getOrder() {
        return order;
    }
}
