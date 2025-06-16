package com.kobe.warehouse.service.dto.records;

import java.math.BigDecimal;

public record AchatRecord(
    BigDecimal receiptAmount,
    BigDecimal discountAmount,
    BigDecimal netAmount,
    BigDecimal taxAmount,
    Long achatCount
) {
    public long ttcAmount() {
        return receiptAmount.add(taxAmount).longValue();
    }
}
