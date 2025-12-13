package com.kobe.warehouse.web.rest.stock;

import com.kobe.warehouse.service.stock.StockProduitSearchService;
import com.kobe.warehouse.service.stock.dto.StockProduitSearchDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for searching stock produits for repartition
 */
@RestController
@RequestMapping("/api/stock-produit")
public class StockProduitSearchResource {

    private static final Logger LOG = LoggerFactory.getLogger(StockProduitSearchResource.class);
    private final StockProduitSearchService stockProduitSearchService;

    public StockProduitSearchResource(StockProduitSearchService stockProduitSearchService) {
        this.stockProduitSearchService = stockProduitSearchService;
    }

    /**
     * {@code GET /api/stock-produit/search-for-repartition} : Search stock produits for repartition
     *
     * @param storageId  the storage ID to search in
     * @param searchTerm the search term (product name or code)
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of stock produits in body
     */
    @GetMapping("/search-for-repartition")
    public ResponseEntity<List<StockProduitSearchDTO>> searchForRepartition(
        @RequestParam Integer storageId,
        @RequestParam String searchTerm
    ) {
        LOG.debug("REST request to search stock produits for repartition: storageId={}, searchTerm={}", storageId, searchTerm);
        List<StockProduitSearchDTO> results = stockProduitSearchService.searchStockProduitsForRepartition(storageId, searchTerm);
        return ResponseEntity.ok(results);
    }
}
