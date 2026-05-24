package com.kobe.warehouse.service.reassort.dto;

import com.kobe.warehouse.domain.StockProduit;

public record ReassortRecord(StockProduit stockProduit, Integer availableQuantity) {
}
