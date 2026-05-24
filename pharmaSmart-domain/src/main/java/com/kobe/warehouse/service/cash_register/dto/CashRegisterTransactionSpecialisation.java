package com.kobe.warehouse.service.cash_register.dto;

import com.kobe.warehouse.domain.enumeration.TypeFinancialTransaction;

public interface CashRegisterTransactionSpecialisation extends CashRegisterSpecialisation {
    String getTypeTransaction();

    default TypeFinancialTransaction getTypeFinancialTransaction() {
        return TypeFinancialTransaction.valueOf(getTypeTransaction());
    }
}
