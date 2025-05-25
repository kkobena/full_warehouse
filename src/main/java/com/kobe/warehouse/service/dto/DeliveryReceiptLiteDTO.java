package com.kobe.warehouse.service.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class DeliveryReceiptLiteDTO {

    private Long id;

    @NotNull
    private Integer receiptAmount;
    private String receiptRefernce;
    private Integer htAmount;
    @NotNull
    private Integer taxAmount;
    private LocalDate receiptDate;
    private String orderReference;
    private Integer finalAmount;//montant vente

    public Long getId() {
        return id;
    }

    public DeliveryReceiptLiteDTO setId(Long id) {
        this.id = id;
        return this;
    }

    public Integer getHtAmount() {
        return htAmount;
    }

    public DeliveryReceiptLiteDTO setHtAmount(Integer htAmount) {
        this.htAmount = htAmount;
        return this;
    }

    public Integer getFinalAmount() {
        return finalAmount;
    }

    public DeliveryReceiptLiteDTO setFinalAmount(Integer finalAmount) {
        this.finalAmount = finalAmount;
        return this;
    }

    public @NotNull Integer getReceiptAmount() {
        return receiptAmount;
    }

    public DeliveryReceiptLiteDTO setReceiptAmount(@NotNull Integer receiptAmount) {
        this.receiptAmount = receiptAmount;
        return this;
    }



    public String getReceiptRefernce() {
        return receiptRefernce;
    }

    public DeliveryReceiptLiteDTO setReceiptRefernce(String receiptRefernce) {
        this.receiptRefernce = receiptRefernce;
        return this;
    }

    public @NotNull Integer getTaxAmount() {
        return taxAmount;
    }

    public DeliveryReceiptLiteDTO setTaxAmount(@NotNull Integer taxAmount) {
        this.taxAmount = taxAmount;
        return this;
    }

    public LocalDate getReceiptDate() {
        return receiptDate;
    }

    public DeliveryReceiptLiteDTO setReceiptDate(LocalDate receiptDate) {
        this.receiptDate = receiptDate;
        return this;
    }

    public String getOrderReference() {
        return orderReference;
    }

    public DeliveryReceiptLiteDTO setOrderReference(String orderReference) {
        this.orderReference = orderReference;
        return this;
    }


}
