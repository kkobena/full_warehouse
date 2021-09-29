package com.kobe.warehouse.service.dto;

public class MaxAndMinDate {
    private String minDate,maxDate;

    public String getMinDate() {
        return minDate;
    }

    public MaxAndMinDate setMinDate(String minDate) {
        this.minDate = minDate;
        return this;
    }

    public String getMaxDate() {
        return maxDate;
    }

    public MaxAndMinDate setMaxDate(String maxDate) {
        this.maxDate = maxDate;
        return this;
    }
}
