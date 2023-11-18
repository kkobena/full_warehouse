package com.kobe.warehouse.service.dto.records;

import java.math.BigDecimal;

public record StoreInventorySummaryRecord(
    BigDecimal costValueBegin,
    BigDecimal costValueAfter,
    BigDecimal amountValueBegin,
    BigDecimal amountValueAfter,
    BigDecimal gapCost,
    BigDecimal gapAmount) {}
