package com.kobe.warehouse.web.rest.stock;

import com.kobe.warehouse.service.dto.StockProduitDTO;
import com.kobe.warehouse.service.stock.StockProduitService;
import com.kobe.warehouse.service.stock.dto.StockProduitSearchDTO;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for managing StockProduit
 */
@RestController
@RequestMapping("/api/stock-produit")
public class StockProduitResource {

    private static final Logger LOG = LoggerFactory.getLogger(StockProduitResource.class);
    private final StockProduitService stockProduitService;

    public StockProduitResource(StockProduitService stockProduitService) {
        this.stockProduitService = stockProduitService;
    }

    /**
     * {@code POST /api/stock-produit} : Create a new stock produit (reserve)
     *
     * @param dto the stock produit DTO to create
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and the created stock produit in body
     */
    @PostMapping
    public ResponseEntity<StockProduitDTO> createStockProduit(@Valid @RequestBody StockProduitDTO dto) {
        LOG.debug("REST request to create StockProduit: {}", dto);

        return ResponseEntity.status(HttpStatus.CREATED).body( stockProduitService.createStockProduit(dto));
    }

    /**
     * {@code PUT /api/stock-produit/{id}} : Update an existing stock produit
     *
     * @param id  the ID of the stock produit to update
     * @param dto the stock produit DTO to update
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the updated stock produit in body
     */
    @PutMapping("/{id}")
    public ResponseEntity<StockProduitDTO> updateStockProduit(
        @PathVariable Integer id,
        @Valid @RequestBody StockProduitDTO dto
    ) {
        LOG.debug("REST request to update StockProduit: id={}, dto={}", id, dto);
        dto.setId(id);
        return ResponseEntity.ok(stockProduitService.updateStockProduit(dto));
    }

    /**
     * {@code GET /api/stock-produit/{id}} : Get a stock produit by ID
     *
     * @param id the ID of the stock produit
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the stock produit in body
     */
    @GetMapping("/{id}")
    public ResponseEntity<StockProduitDTO> getStockProduit(@PathVariable Integer id) {
        LOG.debug("REST request to get StockProduit: id={}", id);
        StockProduitDTO result = stockProduitService.getStockProduit(id);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/search-for-repartition")
    public ResponseEntity<List<StockProduitSearchDTO>> searchForRepartition(
        @RequestParam Integer storageId,
        @RequestParam String searchTerm
    ) {
        LOG.debug("REST request to search stock produits for repartition: storageId={}, searchTerm={}", storageId, searchTerm);
        return ResponseEntity.ok(stockProduitService.searchStockProduitsForRepartition(storageId, searchTerm));
    }
}
