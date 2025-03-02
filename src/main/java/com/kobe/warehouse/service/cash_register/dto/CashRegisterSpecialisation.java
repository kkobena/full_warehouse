package com.kobe.warehouse.service.cash_register.dto;

import java.math.BigDecimal;

public interface CashRegisterSpecialisation {
    BigDecimal getPaidAmount();

    String getPaymentModeCode();

    String getPaymentModeLibelle();
}
