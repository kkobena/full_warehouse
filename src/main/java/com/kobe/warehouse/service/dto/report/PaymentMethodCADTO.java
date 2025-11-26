package com.kobe.warehouse.service.dto.report;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO for CA by payment method
 */
public record PaymentMethodCADTO(
    LocalDate paymentDate,
    String paymentMethod,
    String paymentCode,
    Integer nbPayments,
    Long montantTotal,
    Long montantAvoirs,
    BigDecimal montantMoyen
) {}
