package com.kobe.warehouse.service.stock.dto;

public record RecapProduitVenduSummary(Long totalProducts, Integer quantitySold, Integer quantityAvoir,
                                       Long totalSalesAmount, Long totalPurchaseAmount, Long totalStock
) {

}
