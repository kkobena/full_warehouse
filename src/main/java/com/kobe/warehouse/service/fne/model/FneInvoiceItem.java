package com.kobe.warehouse.service.fne.model;

import jakarta.validation.constraints.NotNull;

public record FneInvoiceItem(String[] taxes, @NotNull String reference, @NotNull String description,
                             @NotNull Double amount) {
}
/*
 private final int quantity = 1;
    private String[] taxes = { "TVAD" };
    private String reference;
    @NotNull
    private String description;
    @NotNull
    private Double amount;// envoie
 */
