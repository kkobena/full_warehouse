package com.kobe.warehouse.service.dto.records;

import java.math.BigDecimal;
import java.math.BigInteger;

public record AchatRecord(
    BigDecimal receiptAmount,
    BigDecimal discountAmount,
    BigDecimal netAmount,
    BigDecimal taxAmount, BigInteger achatCount) {}
