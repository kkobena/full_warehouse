package com.kobe.warehouse.domain;

import jakarta.persistence.Entity;

@Entity
public class UninsuredCustomer extends Customer {

    private Integer caution;

    public Integer getCaution() {
        return caution;
    }

    public void setCaution(Integer caution) {
        this.caution = caution;
    }
}
