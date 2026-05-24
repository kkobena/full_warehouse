package com.kobe.warehouse.service.stock.dto;

import com.kobe.warehouse.domain.enumeration.StorageType;

public record ProduitStockSearch(int quantite, int qteUg, Long storage, StorageType storageType, Integer stockReassort,
                                 Integer seuilMini,Integer id) {
}
