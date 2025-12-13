package com.kobe.warehouse.service.stock;

import com.kobe.warehouse.service.stock.dto.StockProduitSearchDTO;

import java.util.List;

public interface StockProduitSearchService {
    /**
     * Search stock produits by storage and search term for repartition
     *
     * @param storageId  the storage ID to search in
     * @param searchTerm the search term (product name or code)
     * @return list of stock produits with all their associated stocks
     */
    List<StockProduitSearchDTO> searchStockProduitsForRepartition(Integer storageId, String searchTerm);
}
