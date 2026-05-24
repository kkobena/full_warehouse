package com.kobe.warehouse.service.stock.dto;

public record RecapProduitVendu(Integer id, String libelle, String codeCip, String codeEanLaboratoire, String rayonName, Integer quantitySold, Integer quantityAvoir, Integer totalSalesAmount, Integer totalPurchaseAmount, Integer stock) {

}
