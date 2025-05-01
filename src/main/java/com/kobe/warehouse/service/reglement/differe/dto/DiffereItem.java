package com.kobe.warehouse.service.reglement.differe.dto;

import java.time.LocalDateTime;

public record DiffereItem(
    String firsName,
    String lastName,
    String reference,
    int amount,
    int paidAmount,
    int restAmount,
    LocalDateTime mvtDate,
    Long saleId,
    Long customerId
) {
    public String fullName() {
        return firsName + " " + lastName;
    }
}
