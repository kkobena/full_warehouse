package com.kobe.warehouse.service.financiel_transaction.dto;

import com.kobe.warehouse.domain.enumeration.TypeFinancialTransaction;

public record MvtCaisseSumProjection(
    Long amount,
    TypeFinancialTransaction typeTransaction,
    String codeModeReglement,
    String libelleModeReglement
) {}
