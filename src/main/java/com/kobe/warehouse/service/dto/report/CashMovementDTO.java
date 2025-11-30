package com.kobe.warehouse.service.dto.report;

import java.time.LocalDate;

public record CashMovementDTO(
    Long id,
    LocalDate transactionDate,
    String cashRegisterName,
    String userName,
    String movementType,
    Integer amount,
    String paymentMode,
    String saleNumber,
    String customerName
) {
}
