package com.kobe.warehouse.service.dto.records;

import java.math.BigDecimal;

public record StoreInventorySummaryByGroupRecord(
    String groupKey,
    String groupLabel,
    long lineCount,
    BigDecimal costBefore,
    BigDecimal costAfter,
    BigDecimal amountBefore,
    BigDecimal amountAfter,
    BigDecimal gapCost,
    BigDecimal gapAmount
) {}
