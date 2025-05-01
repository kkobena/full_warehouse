package com.kobe.warehouse.service.cash_register.dto;

import java.math.BigDecimal;

public interface CashRegisterSpecialisation {
    BigDecimal getPaidAmount();

    BigDecimal getReelAmount();

    String getPaymentModeCode();

    String getPaymentModeLibelle();
}
