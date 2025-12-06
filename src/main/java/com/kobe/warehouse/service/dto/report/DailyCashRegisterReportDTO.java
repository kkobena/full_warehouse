package com.kobe.warehouse.service.dto.report;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record DailyCashRegisterReportDTO(
    Integer cashRegisterId,
    String caisseLibelle,
    LocalDate date,
    LocalDateTime openingDate,
    LocalDateTime closingDate,
    Integer openingBalance,
    Integer closingBalance,
    Integer expectedBalance,
    Integer discrepancy,
    Integer totalSales,
    Integer numberOfTransactions,
    List<PaymentModeBreakdown> paymentModeBreakdowns,
    String userName,
    boolean isClosed
) {
    public record PaymentModeBreakdown(
        String modePaiement,
        Integer amount,
        Integer count
    ) {}
}
