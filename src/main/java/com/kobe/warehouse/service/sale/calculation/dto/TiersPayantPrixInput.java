package com.kobe.warehouse.service.sale.calculation.dto;

import com.kobe.warehouse.domain.enumeration.OptionPrixType;

public class TiersPayantPrixInput {
    private Integer price;
    private OptionPrixType optionPrixType;
    private float rate ;
    private Long compteTiersPayantId;

    public float getRate() {
        return rate;
    }

    public void setRate(float rate) {
        this.rate = rate;
    }

    public Long getCompteTiersPayantId() {
        return compteTiersPayantId;
    }

    public void setCompteTiersPayantId(Long compteTiersPayantId) {
        this.compteTiersPayantId = compteTiersPayantId;
    }



    public Integer getPrice() {
        return price;
    }

    public void setPrice(Integer price) {
        this.price = price;
    }

    public OptionPrixType getOptionPrixType() {
        return optionPrixType;
    }

    public void setOptionPrixType(OptionPrixType optionPrixType) {
        this.optionPrixType = optionPrixType;
    }
}
