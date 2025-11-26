package com.kobe.warehouse.service.dto.report;

import java.time.LocalDateTime;

public record CashMovementDTO(
    Long id,
    LocalDateTime transactionDate,
    String cashRegisterName,
    String userName,
    String movementType,
    Integer amount,
    String paymentMode,
    String saleNumber,
    String customerName
) {}
