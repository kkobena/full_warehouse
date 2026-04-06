package com.kobe.warehouse.service.facturation.dto;

import java.time.LocalDate;

public record ReglementDto(
    Long id,
    LocalDate transactionDate,
    Integer paidAmount,
    String transactionNumber,
    String paymentMode,
    String banque,
    String commentaire
) {}
