package com.kobe.warehouse.service.dto.records;

import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.domain.StockProduit;

public record QuantitySuggestion(int quantitySold, StockProduit stockProduit, Produit produit) {}
