package com.kobe.warehouse.service.cash_register.dto;

import com.kobe.warehouse.domain.enumeration.TypeFinancialTransaction;

public interface CashRegisterVenteSpecialisation extends CashRegisterSpecialisation {
    String getTypeTransaction();

    default TypeFinancialTransaction getTypeVente() {
        return TypeFinancialTransaction.valueOf(getTypeTransaction());
    }
}
