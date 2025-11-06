package com.kobe.warehouse.service.reglement.dto;

import com.kobe.warehouse.domain.enumeration.TypeFinancialTransaction;

import java.time.LocalDateTime;

public record DefaultPaymentRecord(String operateur, LocalDateTime mvtDate, String modeReglement,
                                   int amount,  String reference,
                                   TypeFinancialTransaction typeFinancialTransaction) {
}
