package com.kobe.warehouse.service.fne.model;

import jakarta.validation.constraints.NotNull;

public record FneInvoiceItem(Integer quantity, String[] taxes, @NotNull String reference, @NotNull String description,
                             @NotNull Double amount, Double discount) {
}
