package com.kobe.warehouse.service.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * DTO for updating PaymentMode entities.
 */
public class PaymentModeUpdateDTO {

    @NotNull
    @Size(max = 50)
    private String code;

    @NotNull
    private String libelle;

    @NotNull
    private Short order;

    public PaymentModeUpdateDTO() {}

    public String getCode() {
        return code;
    }

    public PaymentModeUpdateDTO setCode(String code) {
        this.code = code;
        return this;
    }

    public String getLibelle() {
        return libelle;
    }

    public PaymentModeUpdateDTO setLibelle(String libelle) {
        this.libelle = libelle;
        return this;
    }

    public Short getOrder() {
        return order;
    }

    public PaymentModeUpdateDTO setOrder(Short order) {
        this.order = order;
        return this;
    }
}
