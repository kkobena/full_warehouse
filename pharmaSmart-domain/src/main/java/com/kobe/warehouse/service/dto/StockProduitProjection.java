package com.kobe.warehouse.service.dto;

import java.math.BigDecimal;

public interface StockProduitProjection {
    BigDecimal getTotalStock();

    default int getStock() {
        return getTotalStock().intValue();
    }

    long getProduitId();
}
