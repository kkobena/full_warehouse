package com.kobe.warehouse.service.dto;

import java.time.LocalDateTime;

public class LotSoldDTO {

    private Long id;

    private LocalDateTime createdDate;

    private String saleReference;

    private Integer quantity;

    public LotSoldDTO() {}

    public Long getId() {
        return id;
    }

    public LotSoldDTO setId(Long id) {
        this.id = id;
        return this;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public LotSoldDTO setCreatedDate(LocalDateTime createdDate) {
        this.createdDate = createdDate;
        return this;
    }

    public String getSaleReference() {
        return saleReference;
    }

    public LotSoldDTO setSaleReference(String saleReference) {
        this.saleReference = saleReference;
        return this;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public LotSoldDTO setQuantity(Integer quantity) {
        this.quantity = quantity;
        return this;
    }
}
