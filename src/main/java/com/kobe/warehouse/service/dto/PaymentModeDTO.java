package com.kobe.warehouse.service.dto;

import com.kobe.warehouse.domain.PaymentMode;

public class PaymentModeDTO {

    private String code;
    private String libelle;
    private String group;
    private byte[] qrCode;

    public PaymentModeDTO() {}

    public PaymentModeDTO(PaymentMode paymentMode) {
        this.code = paymentMode.getCode();
        this.libelle = paymentMode.getLibelle();
        this.group = paymentMode.getGroup().name();
       // this.qrCode = paymentMode.getQrCode();
    }

    public String getCode() {
        return code;
    }

    public PaymentModeDTO setCode(String code) {
        this.code = code;
        return this;
    }

    public String getLibelle() {
        return libelle;
    }

    public PaymentModeDTO setLibelle(String libelle) {
        this.libelle = libelle;
        return this;
    }

    public String getGroup() {
        return group;
    }

    public PaymentModeDTO setGroup(String group) {
        this.group = group;
        return this;
    }

    public byte[] getQrCode() {
        return qrCode;
    }

    public PaymentModeDTO setQrCode(byte[] qrCode) {
        this.qrCode = qrCode;
        return this;
    }
}
