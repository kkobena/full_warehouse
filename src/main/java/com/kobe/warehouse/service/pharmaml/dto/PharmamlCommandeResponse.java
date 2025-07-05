package com.kobe.warehouse.service.pharmaml.dto;

public record PharmamlCommandeResponse(boolean success, int totalProduit, int successCount, int outOfStockCount) {}
