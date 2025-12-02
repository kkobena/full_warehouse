package com.kobe.warehouse.service.stock.dto;

public record RecapProduitVenduSummary(  Integer quantitySold, Integer quantityAvoir,
                                       Long totalSalesAmount, Long totalPurchaseAmount
) {

}
